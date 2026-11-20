package dev.wildercord.compat;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Base class for zero-config features so feature classes share one definition across MC versions
 * ({@code Feature<NoneFeatureConfiguration>} in 26.2 vs codec-unit {@code Feature} interface in 26.3).
 */
public abstract class SimpleWorldFeature implements Feature {
	private final MapCodec<SimpleWorldFeature> codec = MapCodec.unit(this);

	protected SimpleWorldFeature() {}

	@Override
	public final MapCodec<? extends Feature> codec() {
		return codec;
	}

	@Override
	public abstract boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin);
}
