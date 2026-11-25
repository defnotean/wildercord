package dev.wildercord.trials;

import dev.wildercord.compat.SimpleWorldFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

/** Bounded crystalline ley-spire and leybloom cluster formation in Astral Ley-Glades. */
public final class LeySpireFeature extends SimpleWorldFeature {
	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		if (!level.getLevel().dimension().equals(Level.OVERWORLD)) {
			return false;
		}
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
		BlockPos base = new BlockPos(origin.getX(), y, origin.getZ());
		if (!writable(level, base) || level.getBlockState(base.below()).isAir() || !level.getFluidState(base.below()).isEmpty()) {
			return false;
		}
		int height = 3 + random.nextInt(2);
		for (int dy = 0; dy < height; dy++) {
			BlockPos p = base.above(dy);
			if (!writable(level, p)) continue;
			BlockState pillar = dy == 0 ? Blocks.AMETHYST_BLOCK.defaultBlockState()
				: (dy == height - 1 ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.OXIDIZED_COPPER.defaultBlockState());
			level.setBlock(p, pillar, Block.UPDATE_CLIENTS);
		}
		int clusters = 0;
		for (int i = 0; i < 6; i++) {
			int dx = random.nextInt(5) - 2;
			int dz = random.nextInt(5) - 2;
			if (dx == 0 && dz == 0) continue;
			int cy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX() + dx, origin.getZ() + dz);
			BlockPos cp = new BlockPos(origin.getX() + dx, cy, origin.getZ() + dz);
			if (!writable(level, cp) || !level.isEmptyBlock(cp) || !level.getFluidState(cp).isEmpty()) continue;
			BlockState bloom = BiomeTrialContent.LEYBLOOM_CLUSTER.defaultBlockState();
			if (bloom.canSurvive(level, cp) && level.setBlock(cp, bloom, Block.UPDATE_CLIENTS)) {
				clusters++;
			}
		}
		return clusters > 0 || height > 0;
	}

	private static boolean writable(WorldGenLevel level, BlockPos pos) {
		return level.hasChunkAt(pos) && !level.isOutsideBuildHeight(pos)
			&& (!(level instanceof WorldGenRegion region) || region.isWithinWriteZone(pos));
	}
}
