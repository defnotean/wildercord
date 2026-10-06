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
public final class StoneMarchPresentationTest implements FabricClientGameTest {
	private static final float HEALTH = 200;
	private static final class Challenger extends FakePlayer {
		Challenger(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "MarchMotionTarget")); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}
	private record Beat(String name, String phase, int age) {}
	private static final List<Beat> BODY = List.of(new Beat("gather", "windup", 12), new Beat("overhead", "windup", 25),
		new Beat("first_strike", "release", 32), new Beat("first_recoil", "recovery", 36), new Beat("second_strike", "release", 40),
		new Beat("third_strike", "release", 48), new Beat("extraction", "recovery", 64), new Beat("reset", "recovery", 84));
	private StoneMarchFixture fixture;
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
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.world.StoneMarchPresentationTest\",\"seed\":\"{}\"}", level.getSeed());
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
				capture(context, world, articulated, "wide", new Beat("spent_first", "recovery", 36));
				capture(context, world, articulated, "wide", new Beat("spent_all", "recovery", 64));
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
			context.runOnClient(mc -> mc.options.fov().set(close ? 50 : 36));
			context.waitTicks(8);
			world.getConnection().waitForChunksRender();
			beginNaturally(world);
			int id = master.getId();
			CompletableFuture<MastersNpcCaptureProbe.Evidence> shot = null;
			for (int tick = 0; tick < beat.age + 10; tick++) {
				var serverFrame = world.getServer().computeOnServer(server -> observe());
				shot = context.computeOnClient(mc -> {
					check(!mc.isPaused() && mc.gui.screen() == null && mc.player.isSpectator(), "Native observer remains unpaused and outside the roster");
					if (!(mc.level.getEntity(id) instanceof SwordMaster live) || live.attackAnimation() != 10) return null;
					float age = live.attackElapsed(MastersNpcCaptureProbe.PARTIAL);
					if (age < beat.age) return null;
					check(age < beat.age + 1, "Capture exact requested March beat, never a neighbomarch phase");
					check(mc.level.getGameTime() - (long) live.attackElapsed(0) == began, "Capture preserves the naturally accepted server activation");
					String name = "stone_fault_march_" + (articulated ? "segmented_" : "rigid_") + view + "_" + beat.name;
					return MastersNpcCaptureProbe.capture(mc, live, name, beat.phase, beat.age,
						18 - (beat.age >= 32 ? 1 : 0) - (beat.age >= 40 ? 2 : 0) - (beat.age >= 48 ? 3 : 0), origin, serverFrame, articulated, close);
				});
				if (shot != null) break;
				context.waitTick();
			}
			check(shot != null, "Native client must observe the requested March frame");
			world.getServer().waitFor(server -> {
				if (level.getGameTime() < began + StoneMarchRules.TELL) return false;
				observe(); return true;
			}, 70);
			var pending = shot;
			context.waitFor(mc -> pending.isDone(), 40);
			var evidence = shot.join();
			check(evidence.captureStatus().equals("passed"), "Native framebuffer, backend, socket and band receipts must pass");
			for (var receipt : evidence.modelReceipts()) {
				float[] root = receipt.modelRootTransform();
				check(root.length == 6 && Math.abs(root[0]) + Math.abs(root[1]) + Math.abs(root[2]) < .0001F,
					"The visual turn cannot translate the model root");
				float expected = 0;
				check(Math.abs(root[4] - expected) < .0001F, "The planted action retains a fixed model root");
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
				case "front" -> origin.add(3, 0, 4.8);
				case "side" -> origin.add(-4.8, 0, 0);
				default -> origin.add(8, 8.5, 7.5);
			};
			// Wide frames preserve the complete fixed lane; front/side frames inspect the original planted blade action.
			Vec3 focus = view.equals("wide") ? origin.add(0, .12, 3.2) : origin.add(0, 1.05, 0);
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
			target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000); target.setHealth(2000);
			target.snapTo(origin.x, origin.y, origin.z + 2, 180, 0); level.addNewPlayer(target);
			fixture = new StoneMarchFixture(level, origin, List.of(target)); master = fixture.master;
		});
		fixture.await(world); began = fixture.began;
	}

	private MasterSchoolMotionChecks.ServerFrame observe() {
		long age = level.getGameTime() - began;
		check(master.isAlive() && !master.isNoAi() && master.started() && master.challengerCount() == 1,
			"Live server AI and one enrolled challenger own each frame");
		check(master.attackAnimation() == 10 && master.marchPending() == (age < StoneMarchRules.THIRD), "The captured form remains the exact accepted march");
		check(master.state(AuraFighter.WINDUP) == (age < StoneMarchRules.THIRD) && !master.guarding(), "The tell and open recovery match the server");
		check(Math.abs(master.auraRemaining() - fixture.paidAura) < .01, "The opening and march are each paid exactly once");
		check(master.position().distanceToSqr(origin) < .003 && master.getDeltaMovement().horizontalDistanceSqr() < .0001,
			"The model turn cannot rotate into server movement");
		check(Math.abs(target.getHealth() - (age < StoneMarchRules.TELL ? HEALTH : HEALTH - 26.4)) < .02,
			"One synchronized server pulse owns damage only at its exact tick");
		return new MasterSchoolMotionChecks.ServerFrame(level.getGameTime(), began, master.attackAnimation(), master.attackElapsed(0),
			master.isNoAi(), master.marchPending(), master.state(AuraFighter.WINDUP), master.guarding(), master.challengerCount(),
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
