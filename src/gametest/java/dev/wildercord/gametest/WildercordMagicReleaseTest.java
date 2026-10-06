package dev.wildercord.gametest;

import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Exercises the real Cord cast path and photographs the formation and release beats. */
public class WildercordMagicReleaseTest implements FabricClientGameTest {
	private static final BlockPos FLOOR = new BlockPos(0, 159, 0);
	private static final int STAGE_TIMEOUT = 2400;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).create();
				StageChunks stage = new StageChunks(world)) {
			context.waitTicks(60);
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1600, 900);
				mc.options.guiScale().set(2);
				mc.resizeGui();
				mc.gui.toastManager().clear();
				if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.CREATIVE);
				Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
				Spellbook book = Spellbooks.get(player).withStarterGiven();
				for (RuneDef rune : Runes.all()) book = book.learn(rune.id());
				Spellbooks.set(player, book);
				Spellbooks.setMana(player, 400);
				player.setYRot(0);
				player.setXRot(0);
				player.teleportTo(player.level(), 0.5, 160, 0.5, Set.<Relative>of(), 0, 0, false);
			});
			// A fixed delay cannot guarantee that both fill corners, or the interior,
			// are ready. Failed commands otherwise only report an error in chat.
			stage.awaitReady();
			world.getServer().runOnServer(server -> {
				checkedFill(server, "fill -68 159 -8 68 159 40 polished_deepslate");
				checkedFill(server, "fill -68 160 24 68 174 24 black_concrete");
				verifyBlocks(server.overworld(), -68, 159, -8, 68, 159, 40, Blocks.POLISHED_DEEPSLATE);
				verifyBlocks(server.overworld(), -68, 160, 24, 68, 174, 24, Blocks.CONCRETE.black());
			});
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
				.teleportTo(server.overworld(), 0.5, 160, 0.5, Set.<Relative>of(), 0, 0, false));
			context.waitTicks(20);
			awaitCamera(context, world);
			cast(context, world, "beam_fire_wind", Runes.BEAM, Runes.FIRE, Runes.WINDCUT);
			cast(context, world, "bolt_fire_wind", Runes.BOLT, Runes.FIRE, Runes.WINDCUT);
			cast(context, world, "spark_storm", Runes.SPARK, Runes.SHOCK);
			cast(context, world, "cone_fire", Runes.CONE, Runes.FIRE);
			cast(context, world, "ring_frost", Runes.RING, Runes.FROST);
			cast(context, world, "self_wind", Runes.SELF, Runes.SWIFT);
			cast(context, world, "rain_fire_wind", Runes.RAIN, Runes.FIRE, Runes.WINDCUT);
			cast(context, world, "zone_life", Runes.ZONE, Runes.GROW);
			for (RuneDef rune : Runes.all()) if (rune.family() == dev.wildercord.spell.RuneFamily.SHAPE) {
				// Relay has a paid two-input protocol and restricted payload; its dedicated suite records the real focus/release.
				if (rune == Runes.RELAY) {
					if (!dev.wildercord.spell.SpellCompiler.compile(List.of(rune, Runes.FIRE)).isEmpty()) throw new AssertionError("The gallery must never present unsupported Relay + Fire as a successful cast");
					continue;
				}
				world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
					.teleportTo(server.overworld(), 0.5, 160, 0.5, Set.<Relative>of(), 0, 0, false));
				context.waitTicks(3);
				cast(context, world, "shape_" + rune.path(), rune, Runes.FIRE);
			}
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			world.getServer().runOnServer(server -> {
				var player=server.getPlayerList().getPlayers().getFirst();
				SpellCaster.edit(player,0,List.of(Runes.BEAM.id(),Runes.FIRE.id(),Runes.WINDCUT.id()));
				Spellbooks.setReadyAt(player,0,0);Spellbooks.setMana(player,400);SpellCaster.cast(player,0);
			});
			context.waitTicks(2);
			context.runOnClient(mc -> {mc.player.setYRot(180);mc.player.setXRot(-20);});
			world.getServer().runOnServer(server -> {var player=server.getPlayerList().getPlayers().getFirst();player.setYRot(180);player.setXRot(-20);});
			context.waitTicks(2);
			shot(context,"turn_rear_circle");
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			shot(context,"turn_first_person_clearance");
		}
	}

	private static void cast(ClientGameTestContext context, TestSingleplayerContext world, String name, RuneDef... runes) {
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		List<String> ids = Arrays.stream(runes).map(RuneDef::id).toList();
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			if (!player.level().getBlockState(FLOOR).is(Blocks.POLISHED_DEEPSLATE)
					|| player.getY() < 159.5) {
				throw new AssertionError("magic screenshot stage is missing before " + name + ": " + stageState(player));
			}
			SpellCaster.edit(player, 0, ids);
			Spellbooks.setReadyAt(player, 0, 0);
			Spellbooks.setMana(player, 400);
			SpellCaster.cast(player, 0);
		});
		context.waitTicks(2);
		shot(context, name + "_form");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		shot(context, name + "_first_person");
		context.waitTicks(2);
		shot(context, name + "_first_person_release");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		shot(context, name + "_release");
		context.waitTicks(50);
	}

	private static void checkedFill(MinecraftServer server, String command) {
		boolean[] called = {false}, succeeded = {false};
		int[] changed = {0};
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack()
			.withCallback((success, result) -> { called[0] = true; succeeded[0] = success; changed[0] = result; }), command);
		if (!called[0] || !succeeded[0] || changed[0] <= 0) {
			throw new AssertionError("Stage command failed: " + command + "; callback=" + called[0]
				+ ", success=" + succeeded[0] + ", changed=" + changed[0]
				+ ", missing chunks=" + missingChunks(server.overworld()));
		}
	}

	private static void verifyBlocks(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
		String mismatch = blockMismatch(pos -> {
			var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
			return chunk == null ? null : chunk.getBlockState(pos);
		}, x1, y1, z1, x2, y2, z2, block);
		if (mismatch != null) throw new AssertionError(mismatch);
	}

	private static String blockMismatch(Function<BlockPos, BlockState> blocks, int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
		for (BlockPos pos : BlockPos.betweenClosed(x1, y1, z1, x2, y2, z2)) {
			BlockState actual = blocks.apply(pos);
			if (actual == null || !actual.is(block)) {
				return "Stage block mismatch at " + pos + ": expected=" + block
					+ ", actual=" + (actual == null ? "unloaded" : actual);
			}
		}
		return null;
	}

	private static void awaitCamera(ClientGameTestContext context, TestSingleplayerContext world) {
		try {
			world.getServer().waitFor(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				var floor = player.level().getChunkSource().getChunkNow(0, 0);
				return player.onGround() && !player.getAbilities().flying
					&& player.position().distanceToSqr(new Vec3(0.5, 160, 0.5)) < 0.01
					&& floor != null && floor.getBlockState(FLOOR).is(Blocks.POLISHED_DEEPSLATE);
			}, 200);
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitFor(WildercordMagicReleaseTest::cameraReady, STAGE_TIMEOUT);
		} catch (AssertionError failure) {
			String serverState = world.getServer().computeOnServer(server -> stageState(server.getPlayerList().getPlayers().getFirst()));
			String clientState = context.computeOnClient(mc -> "player=" + (mc.player == null ? "missing" : mc.player.position())
				+ ", camera=" + mc.gameRenderer.mainCamera().position() + ", stage=" + clientStageMismatch(mc)
				+ ", ready=" + cameraReady(mc));
			throw new AssertionError("Stage camera did not settle: server=" + serverState + "; client=" + clientState, failure);
		}
	}

	private static boolean cameraReady(Minecraft mc) {
		if (mc.player == null || mc.level == null || mc.getCameraEntity() != mc.player) return false;
		var camera = mc.gameRenderer.mainCamera();
		return clientStageMismatch(mc) == null
			&& mc.player.onGround() && mc.player.position().distanceToSqr(new Vec3(0.5, 160, 0.5)) < 0.01
			&& camera.isInitialized() && camera.entity() == mc.player && camera.isDetached()
			&& Math.abs(camera.xRot()) < 0.01 && Math.abs(camera.yRot()) < 0.01
			&& camera.position().distanceToSqr(mc.player.getEyePosition().add(0, 0, -4)) < 0.01;
	}

	private static String clientStageMismatch(Minecraft mc) {
		if (mc.level == null) return "client level missing";
		Function<BlockPos, BlockState> blocks = pos -> {
			var chunk = mc.level.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
			return chunk == null ? null : chunk.getBlockState(pos);
		};
		String floor = blockMismatch(blocks, -68, 159, -8, 68, 159, 40, Blocks.POLISHED_DEEPSLATE);
		return floor != null ? floor : blockMismatch(blocks, -68, 160, 24, 68, 174, 24, Blocks.CONCRETE.black());
	}

	private static String stageState(ServerPlayer player) {
		var chunk = player.level().getChunkSource().getChunkNow(0, 0);
		return "player=" + player.position() + ", onGround=" + player.onGround() + ", flying=" + player.getAbilities().flying
			+ ", floor=" + (chunk == null ? "unloaded" : chunk.getBlockState(FLOOR))
			+ ", missing chunks=" + missingChunks(player.level());
	}

	private static List<String> missingChunks(ServerLevel level) {
		List<String> missing = new ArrayList<>();
		for (int cx = -68 >> 4; cx <= 68 >> 4; cx++) for (int cz = -8 >> 4; cz <= 40 >> 4; cz++) {
			boolean admitted = level.hasChunk(cx, cz);
			boolean full = level.getChunkSource().getChunkNow(cx, cz) != null;
			if (!admitted || !full) missing.add(cx + "," + cz + "[admitted=" + admitted + ",full=" + full + "]");
		}
		return missing;
	}

	/** Owns only this fixture's additional tickets; polling never causes a chunk load. */
	private static final class StageChunks implements AutoCloseable {
		private final TestSingleplayerContext world;
		private final List<ChunkPos> owned = new ArrayList<>();

		StageChunks(TestSingleplayerContext world) {
			this.world = world;
			try {
				world.getServer().runOnServer(server -> {
					var chunks = server.overworld().getChunkSource();
					for (int cx = -68 >> 4; cx <= 68 >> 4; cx++) for (int cz = -8 >> 4; cz <= 40 >> 4; cz++) {
						var pos = new ChunkPos(cx, cz);
						if (chunks.updateChunkForced(pos, true)) owned.add(pos);
					}
				});
			} catch (RuntimeException | Error failure) {
				try { close(); } catch (RuntimeException | Error cleanup) { failure.addSuppressed(cleanup); }
				throw failure;
			}
		}

		void awaitReady() {
			try {
				world.getServer().waitFor(server -> missingChunks(server.overworld()).isEmpty(), STAGE_TIMEOUT);
			} catch (AssertionError failure) {
				throw new AssertionError("Stage chunks not FULL after " + STAGE_TIMEOUT + " ticks: "
					+ world.getServer().computeOnServer(server -> stageState(server.getPlayerList().getPlayers().getFirst())), failure);
			}
		}

		@Override
		public void close() {
			world.getServer().runOnServer(server -> {
				for (ChunkPos pos : owned) server.overworld().getChunkSource().updateChunkForced(pos, false);
			});
			owned.clear();
		}
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.takeScreenshot(TestScreenshotOptions.of("magic_" + name).disableCounterPrefix());
	}
}
