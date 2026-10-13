package dev.wildercord.world.biome;

import dev.wildercord.wildlife.MoonreedBlock;
import dev.wildercord.wildlife.WetlandGarden;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;

/** A small bank colony in an admitted Reedlands patch; dry ground and existing plants stay intact. */
public final class ReedlandBanks {
	private ReedlandBanks() { }
	public static final int MAX_PLANTS = 4;

	/** At most 64 columns in the source chunk, with no extra random draws or feature registrations. */
	public static int place(WorldGenLevel level, BlockPos origin) {
		if (!level.getBiome(origin).is(ReedlandClimate.REEDLANDS)) return 0;
		var reed = WetlandGarden.REED.defaultBlockState().setValue(MoonreedBlock.AGE, 1);
		int startX = (origin.getX() & ~15) + (origin.getX() & 1);
		int startZ = (origin.getZ() & ~15) + (origin.getZ() & 1);
		int placed = 0;
		for (int dx = 0; dx < 16; dx += 2) for (int dz = 0; dz < 16; dz += 2) {
			int x = startX + dx, z = startZ + dz;
			var at = new BlockPos(x, MoonreedBlock.surfaceHeight(level, x, z), z);
			if (level.getBiome(at).is(ReedlandClimate.REEDLANDS) && level.isEmptyBlock(at)
				&& reed.canSurvive(level, at) && MoonreedBlock.moist(level, at) && MoonreedBlock.openSky(level, at)
				&& level.setBlock(at, reed, Block.UPDATE_CLIENTS) && ++placed == MAX_PLANTS) return placed;
		}
		return placed;
	}
}
