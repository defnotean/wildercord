package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import dev.wildercord.client.combat.ArticulatedCombat;
import dev.wildercord.gametest.MastersNpcCaptureProbe;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Native spectator captures, each from a fresh naturally admitted server attack. This fixture
 * does not certify first-person opponent visibility or reduced-effects/FOV coverage.
 */
public final class EmberKilnPresentationTest implements FabricClientGameTest {
	private static final float HEALTH = 200;
	private static final class Challenger extends FakePlayer {
		Challenger(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "KilnMotionTarget")); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}
	private record Beat(String name, String phase, int age) {}
	private static final List<Beat> BODY = List.of(new Beat("coil", "windup", 8), new Beat("half_turn", "windup", 20),
		new Beat("planted", "windup", 32), new Beat("release", "release", 40),
		new Beat("follow", "recovery", 44), new Beat("recovery", "recovery", 65));
	private ServerLevel level;
	private Vec3 origin;
	private SwordMaster master;
	private Challenger target;
	private long began;

	@Override public void runTest(ClientGameTestContext context) {
		var saved = context.computeOnClient(MastersNpcCaptureProbe.Options::save);
		String previous = System.getProperty(ArticulatedCombat.ENABLE_PROPERTY);
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(30);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false",
				"gamerule advance_time false", "time set 3000", "weather clear")) world.getServer().runCommand(command);
			world.getServer().runOnServer(server -> {
				ServerPlayer observer = server.getPlayerList().getPlayers().getFirst();
				level = observer.level(); origin = new Vec3(observer.getBlockX() + .5, 181, observer.getBlockZ() + .5);
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.world.EmberKilnPresentationTest\",\"seed\":\"{}\"}", level.getSeed());
				for (int x = -18; x <= 18; x++) for (int z = -18; z <= 18; z++)
					level.setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
				observer.setGameMode(GameType.SPECTATOR);
			});
			context.runOnClient(MastersNpcCaptureProbe::configure);
			for (boolean articulated : new boolean[] {false, true}) {
				System.setProperty(ArticulatedCombat.ENABLE_PROPERTY, Boolean.toString(articulated));
				for (String view : List.of("front", "side")) for (Beat beat : BODY)
					capture(context, world, articulated, view, beat);
				capture(context, world, articulated, "wide", new Beat("warning", "reply_warning", 20));
				capture(context, world, articulated, "wide", new Beat("late_warning", "reply_warning", 36));
				capture(context, world, articulated, "wide", new Beat("recovery", "recovery", 65));
			}
		} finally {
			if (previous == null) System.clearProperty(ArticulatedCombat.ENABLE_PROPERTY); else System.setProperty(ArticulatedCombat.ENABLE_PROPERTY, previous);
			context.runOnClient(mc -> { MastersNpcCaptureProbe.end(); saved.restore(mc); });
		}
	}

	private void capture(ClientGameTestContext context, TestSingleplayerContext world, boolean articulated, String view, Beat beat) {
		boolean close = !view.equals("wide");
		try {
			frameObserver(world, view);
			context.runOnClient(mc -> mc.options.fov().set(close ? 50 : 43));
			context.waitTicks(8);
			world.getConnection().waitForChunksRender();
			beginNaturally(world);
			int id = master.getId();
			CompletableFuture<MastersNpcCaptureProbe.Evidence> shot = null;
			for (int tick = 0; tick < beat.age + 10; tick++) {
				var serverFrame = world.getServer().computeOnServer(server -> observe());
				shot = context.computeOnClient(mc -> {
					check(!mc.isPaused() && mc.gui.screen() == null && mc.player.isSpectator(), "Native observer remains unpaused and outside the roster");
					if (!(mc.level.getEntity(id) instanceof SwordMaster live) || live.attackAnimation() != 9) return null;
					float age = live.attackElapsed(MastersNpcCaptureProbe.PARTIAL);
					if (age < beat.age) return null;
					check(age < beat.age + 1, "Capture exact requested Kiln beat, never a neighboring phase");
					check(mc.level.getGameTime() - (long) live.attackElapsed(0) == began, "Capture preserves the naturally accepted server activation");
					String name = "ember_kiln_" + (articulated ? "segmented_" : "rigid_") + view + "_" + beat.name;
					return MastersNpcCaptureProbe.capture(mc, live, name, beat.phase, beat.age,
						beat.age < EmberKilnRules.TELL ? EmberKilnRules.SECTORS * 2 : 0, origin, serverFrame, articulated, close);
				});
				if (shot != null) break;
				context.waitTick();
			}
			check(shot != null, "Native client must observe the requested Kiln frame");
			world.getServer().waitFor(server -> {
				if (level.getGameTime() < began + EmberKilnRules.TELL) return false;
				observe(); return true;
			}, 70);
			var pending = shot;
			context.waitFor(mc -> pending.isDone(), 40);
			var evidence = shot.join();
			check(evidence.captureStatus().equals("passed"), "Native framebuffer, backend, socket and ring receipts must pass");
			for (var receipt : evidence.modelReceipts()) {
				float[] root = receipt.modelRootTransform();
				check(root.length == 6 && Math.abs(root[0]) + Math.abs(root[1]) + Math.abs(root[2]) < .0001F,
					"The visual turn cannot translate the model root");
				float expected = articulated ? 0 : MasterAnimationRules.kilnTurn(evidence.renderedTimeline().age(), EmberKilnRules.TELL);
				check(Math.abs(root[4] - expected) < .0001F, "Rigid root owns the complete turn; segmented pelvis owns its own turn");
			}
		} finally {
			world.getServer().runOnServer(server -> {
				if (master != null) { master.discard(); master = null; }
				if (target != null) { target.discard(); target = null; }
			});
		}
		context.waitTicks(5);
	}

	private void frameObserver(TestSingleplayerContext world, String view) {
		world.getServer().runOnServer(server -> {
			ServerPlayer observer = server.getPlayerList().getPlayers().getFirst();
			Vec3 camera = switch (view) {
				case "front" -> origin.add(4.8, 0, 2.2);
				case "side" -> origin.add(0, 0, -4.8);
				default -> origin.add(6, 10.3, 6);
			};
			// High wide framing clears the complete inner rim above the hood. The closer
			// front/side views separately establish the whole-body and blade silhouettes.
			Vec3 focus = view.equals("wide") ? origin.add(.5, .12, .5) : origin.add(0, 1.05, 0);
			Vec3 look = focus.subtract(camera.add(0, observer.getEyeHeight(), 0));
			float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
			float pitch = (float) -Math.toDegrees(Math.atan2(look.y, look.horizontalDistance()));
			observer.teleportTo(level, camera.x, camera.y, camera.z, Set.of(), yaw, pitch, false);
			observer.setDeltaMovement(Vec3.ZERO);
		});
	}

	private void beginNaturally(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			target = new Challenger(level); target.setGameMode(GameType.SURVIVAL);
			target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); target.setHealth(HEALTH);
			target.snapTo(origin.x, origin.y, origin.z + 4.5, 180, 0); level.addNewPlayer(target);
			master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
			check(master != null, "Registered Ember Master exists");
			master.setDiscipline(MastersRules.EMBER); master.snapTo(origin.x, origin.y, origin.z, 0, 0); level.addFreshEntity(master);
			master.mobInteract(target, InteractionHand.MAIN_HAND); master.mobInteract(target, InteractionHand.MAIN_HAND);
			check(SwordMaster.ready(target) == 1, "Fake challenger explicitly enrolls in the native trial"); master.setTarget(target);
		});
		world.getServer().waitFor(server -> {
			if (!master.state(AuraFighter.WINDUP)) return false;
			check(master.attackAnimation() == 5, "Ordinary Ember AI retains its original opening Wake");
			began = level.getGameTime() - (long) master.attackElapsed(0); return true;
		}, 45);
		at(world, 10, () -> placeTarget(8, 0));
		at(world, EmberWakeRules.TELL + EmberWakeRules.RECOVERY - 1, () -> {
			placeTarget(4, 0); target.setHealth(HEALTH); master.setTarget(target);
			check(Math.abs(master.auraRemaining() - 76) < .01, "The opening paid once and completed its recovery");
		});
		world.getServer().waitFor(server -> {
			if (!master.kilnPending()) return false;
			check(master.attackAnimation() == 9 && master.attackElapsed(0) <= 1, "Ordinary sequence-one AI admits Kiln Ring");
			began = level.getGameTime() - (long) master.attackElapsed(0); return true;
		}, 4);
	}

	private MasterSchoolMotionChecks.ServerFrame observe() {
		long age = level.getGameTime() - began;
		check(master.isAlive() && !master.isNoAi() && master.started() && master.challengerCount() == 1,
			"Live server AI and one enrolled challenger own each frame");
		check(master.attackAnimation() == 9 && master.kilnPending() == (age < EmberKilnRules.TELL), "The captured form remains the exact accepted ring");
		check(master.state(AuraFighter.WINDUP) == (age < EmberKilnRules.TELL) && !master.guarding(), "The tell and open recovery match the server");
		check(Math.abs(master.auraRemaining() - 48) < .01, "The opening and ring are each paid exactly once");
		check(master.position().distanceToSqr(origin) < .003 && master.getDeltaMovement().horizontalDistanceSqr() < .0001,
			"The model turn cannot rotate into server movement");
		check(Math.abs(target.getHealth() - (age < EmberKilnRules.TELL ? HEALTH : HEALTH - EmberKilnRules.DAMAGE)) < .02,
			"One synchronized server pulse owns damage only at its exact tick");
		return new MasterSchoolMotionChecks.ServerFrame(level.getGameTime(), began, master.attackAnimation(), master.attackElapsed(0),
			master.isNoAi(), master.kilnPending(), master.state(AuraFighter.WINDUP), master.guarding(), master.challengerCount(),
			master.auraRemaining(), target.getHealth(), master.position(), target.position());
	}
	private void placeTarget(double x, double z) {
		target.teleportTo(level, origin.x + x, origin.y, origin.z + z, Set.of(), 90, 0, false); target.setDeltaMovement(Vec3.ZERO);
	}
	private void at(TestSingleplayerContext world, int age, Runnable action) {
		long expected = began + age;
		world.getServer().waitFor(server -> {
			if (level.getGameTime() < expected) return false;
			check(level.getGameTime() == expected, "Observe the exact natural opening timeline"); action.run(); return true;
		}, age + 20);
	}
	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
