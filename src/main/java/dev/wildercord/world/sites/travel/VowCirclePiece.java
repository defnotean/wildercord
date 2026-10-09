package dev.wildercord.world.sites.travel;

import dev.wildercord.spell.CircleVows;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * An open shrine of pillars, one for each Circle Vow, rising taller with the later circles. The book on the
 * centre lectern lists every vow's two sides, written from the vows themselves, and how to take or release one.
 */
public final class VowCirclePiece extends TravelPiece {
	static final int C = 9, R = 7;

	public VowCirclePiece(int x, int y, int z, Direction facing) { super(TravelSites.VOWS_PIECE, x, y, z, 19, 9, 19, facing); }
	public VowCirclePiece(CompoundTag tag) { super(TravelSites.VOWS_PIECE, tag); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		for (int x = 0; x <= 18; x++) for (int z = 0; z <= 18; z++) {
			double r = dist(x, z, C, C);
			if (r > R + 1.5) continue;
			BlockState floor = r <= 2.2 ? b(Blocks.SMOOTH_STONE) : Math.abs(r - R) < 0.6 ? b(Blocks.CHISELED_STONE_BRICKS)
				: noise(x, z) < 20 ? b(Blocks.MOSSY_STONE_BRICKS) : b(Blocks.STONE_BRICKS);
			footing(level, bb, x, z, x, z, r > R + 0.6 ? GRASS : floor, COBBLE, 8);
		}
		// One pillar per vow, the first at the entrance's left, going round; later circles stand taller.
		int count = CircleVows.ALL.size();
		for (int i = 0; i < count; i++) {
			double a = -Math.PI / 2 + (i + 0.5) * Math.PI * 2 / count;
			int x = C + (int) Math.round(R * Math.cos(a)), z = C + (int) Math.round(R * Math.sin(a)), top = 2 + i / 2;
			fill(level, bb, x, 1, z, x, top, z, b(Blocks.QUARTZ_PILLAR));
			set(level, bb, b(Blocks.CHISELED_QUARTZ_BLOCK), x, top + 1, z);
			set(level, bb, b(Blocks.SOUL_LANTERN), x, top + 2, z);
		}
		lectern(level, bb, C, 1, C, Direction.SOUTH, TravelSites.vowBook());
		chest(level, bb, C, 1, C + 2, TravelSites.loot(TravelSites.VOWS), Direction.SOUTH, 9);
		for (int[] d : new int[][]{{-2, 0}, {2, 0}}) set(level, bb, b(Blocks.AMETHYST_CLUSTER), C + d[0], 1, C + d[1]);
	}
}
