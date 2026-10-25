package dev.wildercord.world.sites.masters;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
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
	public WindGatePiece(int x, int y, int z, Direction facing, boolean alt) { super(MasterSites.WIND_GATE, x, y, z, 15, 22, 14, facing, alt); }
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
		BlockState chiseled = s(Blocks.CHISELED_STONE_BRICKS);
		Block plinth = Blocks.STONE_BRICK_STAIRS, bracket = alt ? Blocks.POLISHED_DEEPSLATE_STAIRS : Blocks.SPRUCE_STAIRS;
		Block roofStairs = alt ? Blocks.DEEPSLATE_TILE_STAIRS : Blocks.SPRUCE_STAIRS;
		BlockState roofUnder = alt ? s(Blocks.DEEPSLATE_TILES) : s(Blocks.SPRUCE_PLANKS), roofCap = alt ? s(Blocks.DEEPSLATE_TILE_SLAB) : s(Blocks.SPRUCE_SLAB);
		pad(level, bb, 0, 0, 14, 13, 21, s(Blocks.STONE_BRICKS), s(Blocks.STONE));
		for (int x = 0; x <= 14; x++) for (int z = 0; z <= 13; z++) if (noise(x, z) < 25) set(level, bb, s(Blocks.MOSSY_STONE_BRICKS), x, 0, z);
		// A flagged processional way up to the gate.
		fill(level, bb, 6, 0, 0, 8, 0, 6, s(Blocks.POLISHED_ANDESITE));
		fill(level, bb, 1, 1, 7, 2, 14, 8, pillar);
		fill(level, bb, 12, 1, 7, 13, 14, 8, pillar);
		// Stepped stone plinths round the feet of both pillars, kept clear of the climbing posts behind.
		for (int[] p : new int[][]{{0, 3}, {11, 14}}) {
			int a = p[0], b = p[1];
			for (int z = 7; z <= 8; z++) { set(level, bb, stair(plinth, Direction.EAST, false), a, 1, z); set(level, bb, stair(plinth, Direction.WEST, false), b, 1, z); }
			for (int x = a + 1; x < b; x++) set(level, bb, stair(plinth, Direction.NORTH, false), x, 1, 6);
			set(level, bb, chiseled, a, 1, 6); set(level, bb, chiseled, b, 1, 6);
		}
		// Brackets under the beam where it meets the pillars and at its overhanging ends.
		for (int z = 7; z <= 8; z++) {
			set(level, bb, stair(bracket, Direction.WEST, true), 3, 14, z); set(level, bb, stair(bracket, Direction.EAST, true), 11, 14, z);
			set(level, bb, stair(bracket, Direction.EAST, true), 0, 14, z); set(level, bb, stair(bracket, Direction.WEST, true), 14, 14, z);
		}
		fill(level, bb, 0, 15, 6, 14, 15, 8, beam);
		fill(level, bb, 3, 13, 7, 11, 13, 7, beam);
		for (int x = 3; x <= 11; x += 2) { set(level, bb, cloth, x, 14, 6); set(level, bb, cloth, x, 14, 8); }
		var rod = s(Blocks.LIGHTNING_ROD.waxed().unaffected());
		set(level, bb, rod, 0, 16, 7); set(level, bb, rod, 14, 16, 7);
		if (alt) { set(level, bb, rod, 4, 16, 6); set(level, bb, rod, 10, 16, 8); }
		// A hipped roof raised over the beam on short posts, lanterns hung beneath, rods on its ridge.
		for (int x : new int[]{1, 13}) fill(level, bb, x, 16, 7, x, 18, 7, pillar);
		hipRoof(level, bb, 1, 7, 13, 7, 19, roofStairs, roofUnder, roofCap);
		fill(level, bb, 1, 19, 7, 13, 19, 7, roofUnder);
		hangingLantern(level, bb, 4, 18, 7, 0, false); hangingLantern(level, bb, 10, 18, 7, 0, false);
		set(level, bb, rod, 1, 21, 7); set(level, bb, rod, 13, 21, 7); set(level, bb, chiseled, 7, 21, 7);
		// The school's colours on the faces of the pillars.
		Block banner = Blocks.WALL_BANNER.pick(alt ? DyeColor.YELLOW : DyeColor.WHITE);
		for (int x : new int[]{1, 2, 12, 13}) banner(level, bb, banner, x, 11, 6, Direction.SOUTH);
		// Stone lanterns at the corners of the yard.
		for (int[] c : new int[][]{{0, 0}, {14, 0}, {0, 13}, {14, 13}}) { fill(level, bb, c[0], 1, c[1], c[0], 2, c[1], s(Blocks.STONE_BRICKS)); set(level, bb, chiseled, c[0], 3, c[1]); set(level, bb, s(Blocks.LANTERN), c[0], 4, c[1]); }
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
