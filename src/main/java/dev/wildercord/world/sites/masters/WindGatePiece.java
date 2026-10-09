package dev.wildercord.world.sites.masters;

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

import java.util.List;

/**
 * A tall hilltop gate of the Gale or Thunder school. Its reward sits on the crossbeam, reached by a climbing line of posts:
 * seven along the back row, seven back along the front, then a jump onto the beam.
 */
public final class WindGatePiece extends MasterSitePiece {
	public WindGatePiece(int x, int y, int z, Direction facing, boolean alt) { super(MasterSites.WIND_GATE, x, y, z, 15, 17, 14, facing, alt); }
	public WindGatePiece(CompoundTag tag) { super(MasterSites.WIND_GATE, tag); }
	@Override public String site() { return "master_wind_gate"; }
	@Override public List<String> schools() { return List.of("gale", "thunder"); }

	/** The climbing posts in order, {x, z, height}. */
	public static int[][] posts() {
		int[][] out = new int[14][];
		for (int i = 0; i < 7; i++) out[i] = new int[]{1 + 2 * i, 11, 1 + i};
		for (int i = 0; i < 7; i++) out[7 + i] = new int[]{13 - 2 * i, 9, 8 + i};
		return out;
	}

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		BlockState pillar = alt ? s(Blocks.POLISHED_DEEPSLATE) : s(Blocks.STRIPPED_OAK_LOG);
		BlockState beam = alt ? s(Blocks.CUT_COPPER.waxed().unaffected()) : s(Blocks.SPRUCE_PLANKS);
		BlockState cloth = alt ? s(Blocks.WOOL.yellow()) : s(Blocks.WOOL.white());
		pad(level, bb, 0, 0, 14, 13, 16, s(Blocks.STONE_BRICKS), s(Blocks.STONE));
		for (int x = 0; x <= 14; x++) for (int z = 0; z <= 13; z++) if (noise(x, z) < 25) set(level, bb, s(Blocks.MOSSY_STONE_BRICKS), x, 0, z);
		fill(level, bb, 1, 1, 7, 2, 14, 8, pillar);
		fill(level, bb, 12, 1, 7, 13, 14, 8, pillar);
		fill(level, bb, 0, 15, 6, 14, 15, 8, beam);
		fill(level, bb, 3, 13, 7, 11, 13, 7, beam);
		for (int x = 3; x <= 11; x += 2) { set(level, bb, cloth, x, 14, 6); set(level, bb, cloth, x, 14, 8); }
		var rod = s(Blocks.LIGHTNING_ROD.waxed().unaffected());
		set(level, bb, rod, 0, 16, 7); set(level, bb, rod, 14, 16, 7);
		if (alt) { set(level, bb, rod, 4, 16, 6); set(level, bb, rod, 10, 16, 8); }
		// The climb.
		var stone = s(Blocks.POLISHED_ANDESITE);
		for (int[] p : posts()) fill(level, bb, p[0], 1, p[1], p[0], p[2], p[1], stone);
		chest(level, bb, 7, 16, 7, rewardLoot(), Direction.SOUTH, 2);
		// The practice yard in front of the gate.
		var post = s(Blocks.SPRUCE_FENCE);
		dummy(level, bb, 3, 1, 3, post); dummy(level, bb, 11, 1, 3, post);
		target(level, bb, 9, 1, 1, post);
		lectern(level, bb, 7, 1, 4);
		chest(level, bb, 13, 1, 2, MasterSites.supplies(site()), Direction.WEST, 1);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(); }
}
