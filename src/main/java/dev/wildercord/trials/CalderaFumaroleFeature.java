package dev.wildercord.trials;

import dev.wildercord.compat.SimpleWorldFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;

/** Bounded basalt fumarole mound and slag vent cluster in Cinder Calderas. */
public final class CalderaFumaroleFeature extends SimpleWorldFeature {
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
		int placed = 0;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (Math.abs(dx) + Math.abs(dz) > 2) continue;
				int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX() + dx, origin.getZ() + dz);
				BlockPos sp = new BlockPos(origin.getX() + dx, sy - 1, origin.getZ() + dz);
				if (!writable(level, sp)) continue;
				if (dx == 0 && dz == 0) {
					level.setBlock(sp, Blocks.BASALT.defaultBlockState(), Block.UPDATE_CLIENTS);
					BlockPos ventPos = sp.above();
					if (writable(level, ventPos) && level.isEmptyBlock(ventPos)) {
						level.setBlock(ventPos, BiomeTrialContent.CALDERA_SLAG_VENT.defaultBlockState(), Block.UPDATE_CLIENTS);
						placed++;
					}
				} else if (random.nextInt(2) == 0) {
					level.setBlock(sp, random.nextInt(4) == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
					placed++;
				}
			}
		}
		return placed > 0;
	}

	private static boolean writable(WorldGenLevel level, BlockPos pos) {
		return level.hasChunkAt(pos) && !level.isOutsideBuildHeight(pos)
			&& (!(level instanceof WorldGenRegion region) || region.isWithinWriteZone(pos));
	}
}
