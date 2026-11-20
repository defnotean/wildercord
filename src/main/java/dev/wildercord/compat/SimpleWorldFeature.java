package dev.wildercord.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Base class for zero-config features so feature classes share one definition across MC versions
 * ({@code Feature<NoneFeatureConfiguration>} in 26.2 vs codec-unit {@code Feature} interface in 26.3).
 */
public abstract class SimpleWorldFeature extends Feature<NoneFeatureConfiguration> {
	protected SimpleWorldFeature() {
		super(NoneFeatureConfiguration.CODEC);
	}

	@Override
	public final boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		return place(context.level(), context.chunkGenerator(), context.random(), context.origin());
	}

	public abstract boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin);
}
