package dev.wildercord.aura.world;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.wildercord.client.combat.ArticulatedCombat;
import dev.wildercord.client.fx.HitStop;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.client.fx.ScreenEffects;
import dev.wildercord.gametest.MastersNpcCaptureProbe;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Real enrolled-client first-person counter trials and unchanged native framebuffer readbacks.
 * Timeline/health assertions are automatic. These images require manual warning/body/HUD review;
 * they deliberately do not borrow the spectator probe's visibility certification.
 */
public final class EmberKilnOpponentViewTest implements FabricClientGameTest {
	private static final float HEALTH = 200, PARTIAL = .5F;
	private enum Counter {
		HOLD, INWARD, OUTWARD, EARLY_JUMP, TIMED_JUMP;
		boolean escapes() { return this == INWARD || this == OUTWARD || this == TIMED_JUMP; }
	}
	private static final DeltaTracker DELTA = new DeltaTracker() {
		public float getGameTimeDeltaTicks() { return PARTIAL; }
		public float getGameTimeDeltaPartialTick(boolean paused) { return PARTIAL; }
		public float getRealtimeDeltaTicks() { return PARTIAL; }
	};
	private record Observation(long gameTick, long acceptedTick, int attackId, float age, boolean pending,
		boolean windup, boolean guarding, double aura, int participants, Vec3 masterPosition, Vec3 playerPosition, float playerHealth) {}
	private record Receipt(String name, String captureKind, String pixelReview, boolean realEnrolledClient, boolean spectator,
		String answer, String phase, int requestedTick, long clientGameTick, long acceptedTick, float clientAge,
		int width, int height, int fov, float lookYaw, float lookPitch, String camera, String requestedBackend,
		String extractedBackend, boolean reducedFlash, String othersQuality, String particles, Vec3 cameraPosition,
		Observation serverObservation) {}
	private ServerLevel level;
	private ServerPlayer player;
	private SwordMaster master;
	private Vec3 origin;
	private long began;

	@Override public void runTest(ClientGameTestContext context) {
		var saved = context.computeOnClient(MastersNpcCaptureProbe.Options::save);
		MagicQuality.Level own = MagicQuality.own, others = MagicQuality.others;
		MagicQuality.Impact impact = MagicQuality.impact;
		String previous = System.getProperty(ArticulatedCombat.ENABLE_PROPERTY);
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(30);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false",
				"gamerule advance_time false", "time set 3000", "weather clear")) world.getServer().runCommand(command);
			world.getServer().runOnServer(server -> {
				player = server.getPlayerList().getPlayers().getFirst(); level = player.level();
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.world.EmberKilnOpponentViewTest\",\"seed\":\"{}\"}", level.getSeed());
				origin = new Vec3(player.getBlockX() + .5, 181, player.getBlockZ() + .5);
				for (int x = -18; x <= 18; x++) for (int z = -18; z <= 18; z++)
					level.setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
				player.setGameMode(GameType.SURVIVAL); player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
				for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
					EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) player.setItemSlot(slot, ItemStack.EMPTY);
				player.getFoodData().setFoodLevel(20);
			});
			context.runOnClient(MastersNpcCaptureProbe::configure);
			context.waitTicks(10);
			world.getConnection().waitForChunksRender();
			for (boolean articulated : new boolean[] {false, true}) for (int fov : new int[] {70, 90, 110})
				for (boolean reduced : new boolean[] {false, true}) for (boolean inward : new boolean[] {false, true}) {
					System.setProperty(ArticulatedCombat.ENABLE_PROPERTY, Boolean.toString(articulated));
					context.runOnClient(mc -> {
						mc.options.fov().set(fov); mc.options.particles().set(reduced ? ParticleStatus.MINIMAL : ParticleStatus.ALL);
						MagicQuality.own = MagicQuality.others = reduced ? MagicQuality.Level.MINIMAL : MagicQuality.Level.BALANCED;
						MagicQuality.reducedFlash = reduced; MagicQuality.cameraShake = false;
						MagicQuality.impact = reduced ? MagicQuality.Impact.OFF : MagicQuality.Impact.FULL;
						HitStop.clear(); ScreenEffects.clearCameraMotion();
					});
					trial(context, world, articulated, fov, reduced, inward ? Counter.INWARD : Counter.HOLD);
				}
			System.setProperty(ArticulatedCombat.ENABLE_PROPERTY, "false");
			context.runOnClient(mc -> {
				mc.options.fov().set(90); mc.options.particles().set(ParticleStatus.ALL);
				MagicQuality.own = MagicQuality.others = MagicQuality.Level.BALANCED;
				MagicQuality.reducedFlash = false; MagicQuality.impact = MagicQuality.Impact.FULL;
			});
			for (Counter counter : List.of(Counter.OUTWARD, Counter.EARLY_JUMP, Counter.TIMED_JUMP))
				trial(context, world, false, 90, false, counter);
		} finally {
			if (previous == null) System.clearProperty(ArticulatedCombat.ENABLE_PROPERTY); else System.setProperty(ArticulatedCombat.ENABLE_PROPERTY, previous);
			context.getInput().releaseKey(o -> o.keyUp);
			context.getInput().releaseKey(o -> o.keyDown);
			context.getInput().releaseKey(o -> o.keyJump);
			context.runOnClient(mc -> {
				MagicQuality.own = own; MagicQuality.others = others; MagicQuality.impact = impact;
				HitStop.clear(); ScreenEffects.clearCameraMotion(); saved.restore(mc);
			});
		}
	}

	private void trial(ClientGameTestContext context, TestSingleplayerContext world, boolean articulated, int fov, boolean reduced, Counter counter) {
		String prefix = "ember_kiln_opponent_" + (articulated ? "segmented" : "rigid") + "_fov" + fov
			+ (reduced ? "_reduced_" : "_normal_") + counter.name().toLowerCase(java.util.Locale.ROOT);
		try {
			beginNaturally(world);
			if (counter == Counter.EARLY_JUMP) jumpAt(context, world, 6);
			var warning = shotAt(context, world, prefix, "warning", 24, articulated, counter);
			if (counter == Counter.INWARD || counter == Counter.OUTWARD) {
				boolean inward = counter == Counter.INWARD;
				if (inward) context.getInput().holdKey(o -> o.keyUp); else context.getInput().holdKey(o -> o.keyDown);
				try {
					for (int tick = 0; tick < 14; tick++) {
						double radius = world.getServer().computeOnServer(server -> player.position().subtract(origin).horizontalDistance());
						if (inward ? radius < 2.35 : radius > 5.9) break;
						context.waitTick();
					}
				} finally {
					context.getInput().releaseKey(o -> o.keyUp); context.getInput().releaseKey(o -> o.keyDown);
				}
				world.getServer().runOnServer(server -> {
					double radius = player.position().subtract(origin).horizontalDistance();
					check(level.getGameTime() < began + EmberKilnRules.TELL
						&& (inward ? radius < EmberKilnRules.INNER : radius > EmberKilnRules.OUTER),
						"Real movement input reaches its safe boundary before the accepted pulse: " + counter);
				});
			}
			if (counter == Counter.TIMED_JUMP) jumpAt(context, world, 34);
			if (counter == Counter.EARLY_JUMP) at(world, 39, () -> check(player.onGround() && Math.abs(player.getY() - origin.y) < .02,
				"An early native jump really lands before the pulse and fails the counter"));
			var release = shotAt(context, world, prefix, "release", 40, articulated, counter);
			world.getServer().runOnServer(server -> {
				check(Math.abs(player.getHealth() - (counter.escapes() ? HEALTH : HEALTH - EmberKilnRules.DAMAGE)) < .02,
					"The real enrolled player's actual input resolves the authoritative counter: " + counter);
				if (counter == Counter.TIMED_JUMP) check(player.getY() - origin.y > EmberKilnRules.HIGH, "Native jump physics clears the low ring");
			});
			var recovery = shotAt(context, world, prefix, "recovery", 65, articulated, counter);
			var all = CompletableFuture.allOf(warning, release, recovery);
			context.waitFor(mc -> all.isDone(), 50); all.join();
		} finally {
			context.getInput().releaseKey(o -> o.keyUp);
			context.getInput().releaseKey(o -> o.keyDown);
			context.getInput().releaseKey(o -> o.keyJump);
			world.getServer().runOnServer(server -> { if (master != null) { master.discard(); master = null; } });
		}
		context.waitTicks(5);
	}

	private void jumpAt(ClientGameTestContext context, TestSingleplayerContext world, int age) {
		for (int ticks = 0; ticks <= age + 2; ticks++) {
			long now = world.getServer().computeOnServer(server -> level.getGameTime());
			if (now >= began + age) {
				check(now <= began + age + 1, "Jump input remains inside its declared native timing window");
				context.getInput().holdKey(o -> o.keyJump);
				try { context.waitTicks(2); } finally { context.getInput().releaseKey(o -> o.keyJump); }
				return;
			}
			context.waitTick();
		}
		throw new AssertionError("Native jump input missed its accepted timeline");
	}

	private CompletableFuture<Void> shotAt(ClientGameTestContext context, TestSingleplayerContext world, String prefix, String phase,
		int age, boolean articulated, Counter counter) {
		int id = master.getId();
		for (int tick = 0; tick < age + 10; tick++) {
			Observation server = world.getServer().computeOnServer(s -> observation());
			var shot = context.computeOnClient(mc -> {
				check(!mc.isPaused() && mc.gui.screen() == null && !mc.player.isSpectator(), "The real challenger remains unpaused in first person");
				if (!(mc.level.getEntity(id) instanceof SwordMaster live) || live.attackAnimation() != 9) return null;
				if (live.attackElapsed(PARTIAL) < age) return null;
				check(live.attackElapsed(PARTIAL) < age + 1, "Opponent screenshot must match the exact requested phase");
				check(mc.level.getGameTime() - (long) live.attackElapsed(0) == began && Math.abs(mc.level.getGameTime() - server.gameTick) <= 1,
					"Native client and server receipts retain the same accepted source clock");
				return capture(mc, live, prefix + "_" + phase, phase, age, articulated, counter, server);
			});
			if (shot != null) return shot;
			context.waitTick();
		}
		throw new AssertionError("Native opponent view did not observe " + phase);
	}

	private CompletableFuture<Void> capture(Minecraft mc, SwordMaster live, String name, String phase, int tick,
		boolean articulated, Counter counter, Observation server) {
		mc.gameRenderer.update(DELTA); mc.gameRenderer.extract(DELTA, true); mc.gameRenderer.render();
		RenderSystem.getDevice().createCommandEncoder().submit();
		var renderer = (dev.wildercord.client.auraworld.MasterRenderer) mc.getEntityRenderDispatcher().getRenderer(live);
		var state = renderer.createRenderState(live, PARTIAL);
		String actualBackend = ArticulatedCombat.frame(state) == null ? "rigid" : "segmented";
		check(actualBackend.equals(articulated ? "segmented" : "rigid"), "Opponent trial samples the requested renderer backend");
		var receipt = new Receipt(name, "native_first_person_accepted_timeline", "manual_review_pending", true, mc.player.isSpectator(),
			"real_input_" + counter.name().toLowerCase(java.util.Locale.ROOT), phase, tick,
			mc.level.getGameTime(), began, live.attackElapsed(PARTIAL), mc.getWindow().getWidth(), mc.getWindow().getHeight(),
			mc.options.fov().get(), mc.player.getYRot(), mc.player.getXRot(), mc.options.getCameraType().name(),
			articulated ? "segmented" : "rigid", actualBackend, MagicQuality.reducedFlash, MagicQuality.others.name(),
			mc.options.particles().get().name(), mc.gameRenderer.mainCamera().position(), server);
		var result = new CompletableFuture<Void>();
		net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
			try (image) {
				var path = FabricLoader.getInstance().getGameDir().resolve("screenshots").resolve(name + ".png");
				Files.createDirectories(path.getParent()); image.writeToFile(path);
				Files.writeString(path.resolveSibling(name + ".json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(receipt));
				result.complete(null);
			} catch (Throwable failure) { result.completeExceptionally(failure); }
		});
		return result;
	}

	private void beginNaturally(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			place(0, 4.5); player.setHealth(HEALTH); player.setAbsorptionAmount(0);
			master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
			check(master != null, "Registered Ember Master exists");
			master.setDiscipline(MastersRules.EMBER); master.snapTo(origin.x, origin.y, origin.z, 0, 0); level.addFreshEntity(master);
			master.mobInteract(player, InteractionHand.MAIN_HAND); master.mobInteract(player, InteractionHand.MAIN_HAND);
			check(SwordMaster.ready(player) == 1, "Real client explicitly enrolls in the native counter trial"); master.setTarget(player);
		});
		world.getServer().waitFor(server -> {
			if (!master.state(AuraFighter.WINDUP)) return false;
			check(master.attackAnimation() == 5, "Original sequence-zero Ember Wake owns its natural opening");
			began = level.getGameTime() - (long) master.attackElapsed(0); return true;
		}, 45);
		at(world, 10, () -> place(8, 0));
		at(world, EmberWakeRules.TELL + EmberWakeRules.RECOVERY - 1, () -> {
			place(0, 4); player.setHealth(HEALTH); master.setTarget(player);
		});
		world.getServer().waitFor(server -> {
			if (!master.kilnPending()) return false;
			check(master.attackAnimation() == 9 && master.attackElapsed(0) <= 1, "Natural sequence-one AI admits the ring");
			began = level.getGameTime() - (long) master.attackElapsed(0); return true;
		}, 4);
	}
	private Observation observation() {
		check(master.attackAnimation() == 9 && master.started() && !master.isNoAi() && master.challengerCount() == 1,
			"The screenshot retains a live native trial and the real client as its sole challenger");
		check(Math.abs(master.auraRemaining() - 48) < .01 && master.position().distanceToSqr(origin) < .003,
			"Server origin and paid resource budget remain fixed");
		return new Observation(level.getGameTime(), began, master.attackAnimation(), master.attackElapsed(0), master.kilnPending(),
			master.state(AuraFighter.WINDUP), master.guarding(), master.auraRemaining(), master.challengerCount(), master.position(), player.position(), player.getHealth());
	}
	private void place(double x, double z) {
		player.teleportTo(level, origin.x + x, origin.y, origin.z + z, Set.of(), 180, 10, false); player.setDeltaMovement(Vec3.ZERO);
	}
	private void at(TestSingleplayerContext world, int age, Runnable action) {
		long expected = began + age;
		world.getServer().waitFor(server -> {
			if (level.getGameTime() < expected) return false;
			check(level.getGameTime() == expected, "Opening setup uses its exact native source clock"); action.run(); return true;
		}, age + 20);
	}
	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
