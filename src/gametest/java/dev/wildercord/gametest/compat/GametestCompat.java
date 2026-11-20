package dev.wildercord.gametest.compat;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationThunk;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
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

import java.util.function.Predicate;

/** Version-specific gametest harness helpers for Minecraft 26.2. */
public final class GametestCompat {
	private GametestCompat() {}

	public static InputConstants.Key keyboardKey(int keyCode) {
		return InputConstants.Type.KEYSYM.getOrCreate(keyCode);
	}

	public static void setFullscreen(Window window, boolean fullscreen) {
		if (window.isFullscreen() != fullscreen) {
			window.toggleFullScreen();
		}
	}

	public static void renderFrame(Minecraft mc, DeltaTracker delta) {
		mc.gameRenderer.render(delta, true);
	}

	public static void extractMainCamera(GameRenderer gameRenderer, CameraRenderState camera, DeltaTracker delta) {
		var mainCam = gameRenderer.getMainCamera();
		mainCam.extractRenderState(camera, mainCam.getCameraEntityPartialTicks(delta));
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
}
