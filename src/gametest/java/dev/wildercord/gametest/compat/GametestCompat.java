package dev.wildercord.gametest.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationThunk;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import dev.wildercord.client.compat.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.function.Predicate;

/** Version-specific gametest harness helpers for Minecraft 26.2. */
public final class GametestCompat {
	private GametestCompat() {}

	/**
	 * Polls a server condition until it holds or the tick budget runs out, returning the ticks
	 * waited. 26.1.2's TestServerContext has no waitFor, and the integrated server only ticks
	 * while the harness is inside a client-side wait, so each attempt advances the world with
	 * ClientGameTestContext.waitTicks before the next check.
	 */
	public static int waitFor(ClientGameTestContext client, TestServerContext server, Predicate<MinecraftServer> condition, int timeoutTicks) {
		int waited = 0;
		try {
			while (waited < timeoutTicks) {
				if (Boolean.TRUE.equals(server.computeOnServer(condition::test))) {
					return waited;
				}
				client.waitTicks(1);
				waited++;
			}
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
		return waited;
	}

	public static InputConstants.Key keyboardKey(int keyCode) {
		return InputConstants.Type.KEYSYM.getOrCreate(keyCode);
	}

	public static void setFullscreen(Window window, boolean fullscreen) {
		if (window.isFullscreen() != fullscreen) {
			window.toggleFullScreen();
		}
	}

	/** 1.21.11 dropped Camera#getViewRotationProjectionMatrix: rebuild it from the camera pose and the perspective. */
	public static Matrix4f viewProjection(Camera camera, Matrix4f out) {
		Vec3 pos = camera.position();
		out.rotation(camera.rotation()).translate((float) -pos.x, (float) -pos.y, (float) -pos.z);
		return Minecraft.getInstance().gameRenderer.getProjectionMatrix(reportedFov(camera)).mul(out, out);
	}

	/** 1.21.11 no longer reports a fov on Camera; the slider is what GameRenderer feeds the projection. */
	public static float reportedFov(Camera camera) {
		return Minecraft.getInstance().options.fov().get();
	}

	public static void renderFrame(Minecraft mc, DeltaTracker delta) {
		mc.gameRenderer.render(delta, true);
	}

	/** 1.21.10 has no GameRenderer.updateCamera: tick() refreshes the main camera from the camera entity. */
	public static void updateCamera(GameRenderer gameRenderer, DeltaTracker delta) {
		gameRenderer.tick();
	}

	/** 1.21.10 dropped the client-loaded tracking, so a ticked, live player stands in for the loaded client. */
	public static boolean clientLoaded(ServerPlayer player) {
		return player.tickCount > 0 && !player.isRemoved();
	}

	public static void extractMainCamera(GameRenderer gameRenderer, CameraRenderState camera, DeltaTracker delta) {
		// 1.21.11 has no MainCamera.extractRenderState: the level render state carries the camera's, filled by the last frame.
		// 1.21.10 has neither, so copy the live camera's pose into the state.
		Camera source = gameRenderer.getMainCamera();
		camera.blockPos = source.getBlockPosition();
		camera.pos = source.getPosition();
		camera.initialized = true;
		camera.entityPos = source.getPosition();
		camera.orientation = source.rotation();
	}

	/** 1.21.8 keeps the particle queues private and has no public clear: empty them by hand. */
	public static void clearParticles(net.minecraft.client.particle.ParticleEngine engine) {
		try {
			java.lang.reflect.Field queues = net.minecraft.client.particle.ParticleEngine.class.getDeclaredField("particles");
			queues.setAccessible(true);
			for (Object queue : ((java.util.Map<?, ?>) queues.get(engine)).values()) {
				((java.util.Collection<?>) queue).clear();
			}
			java.lang.reflect.Field adding = net.minecraft.client.particle.ParticleEngine.class.getDeclaredField("particlesToAdd");
			adding.setAccessible(true);
			((java.util.Collection<?>) adding.get(engine)).clear();
		} catch (ReflectiveOperationException failure) {
			throw new IllegalStateException(failure);
		}
	}

	/** 1.21.8 carries a knock-back packet as three raw axis values, not a movement vector. */
	public static net.minecraft.world.phys.Vec3 motionOf(net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket packet) {
		return new net.minecraft.world.phys.Vec3(packet.getXa(), packet.getYa(), packet.getZa());
	}

	public static BlockPos findNearestStructure(ServerLevel level, Holder<Structure> structure, BlockPos origin,
			int radiusChunks, boolean skipKnownStructures) {
		var hit = level.getChunkSource().getGenerator().findNearestMapStructure(
			level, HolderSet.direct(structure), origin, radiusChunks, skipKnownStructures);
		return hit == null ? null : hit.getFirst();
	}

	public static StructureStart generateStructureStart(ServerLevel level, Holder<Structure> structure, ChunkPos chunk) {
		var cs = level.getChunkSource();
		var gen = cs.getGenerator();
		return structure.value().generate(structure, level.dimension(), level.registryAccess(), gen,
			gen.getBiomeSource(), cs.randomState(), level.getStructureManager(), level.getSeed(),
			chunk, 0, level, structure.value().biomes()::contains);
	}

	public static Pair<BlockPos, Holder<Biome>> findBiomeHorizontal(BiomeSource source, int x, int y, int z,
			int radius, int increment, Predicate<Holder<Biome>> target, RandomSource random, boolean findClosest,
			RandomState randomState) {
		return source.findBiomeHorizontal(x, y, z, radius, increment, target, random, findClosest, randomState.sampler());
	}

	public static boolean biomeHasMob(Biome biome, MobCategory category, EntityType<?> type) {
		return biome.getMobSettings().getMobs(category).unwrap().stream().anyMatch(entry -> entry.value().type() == type);
	}

	public static Feature<?> configuredFeatureValue(PlacedFeature placed) {
		return placed.feature().value().feature();
	}

	public static Structure.GenerationContext dummyStructureContext(ChunkPos chunk, LevelHeightAccessor height) {
		return new Structure.GenerationContext((RegistryAccess) null, null, null, null, null, (WorldgenRandom) null,
			721, chunk, height, biome -> true);
	}

	/**
	 * A real {@link Structure.GenerationContext} backed by the level's actual chunk generator, biome
	 * source and random state, so a site locator can be exercised against genuine terrain heightmaps
	 * (the same inputs {@link #generateStructureStart} feeds through {@code Structure.generate}). The
	 * biome predicate accepts everything; each locator does its own biome eligibility check. 26.2 keeps
	 * the climate sampler off this record - {@code BiomeSource} takes it per call instead.
	 */
	public static Structure.GenerationContext realStructureContext(ServerLevel level, ChunkPos chunk) {
		var cs = level.getChunkSource();
		var gen = cs.getGenerator();
		return new Structure.GenerationContext(level.registryAccess(), gen, gen.getBiomeSource(),
			cs.randomState(), level.getStructureManager(), level.getSeed(), chunk, level, biome -> true);
	}

	public static Vec3 entityPositionSyncTarget(ClientboundEntityPositionSyncPacket sync) {
		return sync.values().position();
	}

	public static Vec3 moveEntityDeltaTarget(ClientboundMoveEntityPacket move, Vec3 base) {
		VecDeltaCodec codec = new VecDeltaCodec();
		codec.setBase(base);
		return codec.decode(move.getXa(), move.getYa(), move.getZa());
	}

	public static boolean syncVelocityFlag(ServerPlayer player) {
		return player.hurtMarked;
	}

	public static NarrationElementOutput narrationOutput(StringBuilder said) {
		return new NarrationElementOutput() {
			@Override
			public void add(NarratedElementType type, NarrationThunk<?> thunk) {
				thunk.getText(text -> said.append(text).append(' '));
			}

			@Override
			public NarrationElementOutput nest() {
				return this;
			}
		};
	}

	public static int particlePacketCount(ClientboundLevelParticlesPacket packet) {
		return Math.max(1, packet.getCount());
	}

	public static void swingMainHandWithItem(LocalPlayer player) {
		player.swing(InteractionHand.MAIN_HAND, true);
	}

	/** Clears the toast stack. 26.3 moved the toast manager from Minecraft onto Gui. */
	public static void clearToasts(Minecraft mc) {
		mc.getToastManager().clear();
	}

	/** Clears the chat line list. 26.3 reaches it through Gui#hud. */
	public static void clearChat(Minecraft mc) {
		mc.gui.getChat().clearMessages(false);
	}

	/** Whether the HUD is hidden (the F3+A state). 26.3 moved it behind Gui#hud. */
	public static boolean hudHidden(Minecraft mc) {
		return mc.options.hideGui;
	}

	/** Flips HUD visibility, so a screenshot can be taken without the hotbar and crosshair. */
	public static void toggleHud(Minecraft mc) {
		mc.options.hideGui = !mc.options.hideGui;
	}

	/**
	 * Recomputes the GUI scale and re-inits the HUD. 26.x calls this Minecraft#resizeGui;
	 * this branch still routes it through the window-resize handler, which is what rebuilds
	 * the panes and the chat/scale state.
	 */
	public static void resizeGui(Minecraft mc) {
		mc.resizeDisplay();
	}

	/**
	 * Pumps the client until queued clientbound packets are processed. 26.3 added
	 * TestSingleplayerContext#getConnection; before it, waiting for the chunk download
	 * to settle is what drains the client's inbound queue.
	 */
	/**
	 * 1.21.5 has no camera-distance attribute: that arrived with 1.21.6. Third person sits at vanilla
	 * 's fixed distance here, so the pull-back the tests ask for is a no-op and the spectacle checks
	 * run at that distance.
	 */
	public static void cameraDistance(ServerPlayer player, double distance) {
	}
	public static void awaitClientPackets(TestSingleplayerContext world) {
		world.getClientWorld().waitForChunksDownload();
	}
}
