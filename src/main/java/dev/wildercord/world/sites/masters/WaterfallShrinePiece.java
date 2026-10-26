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
 * A snowfield shrine of the Rime or Tide school: a frozen waterfall down a cliff over a still pool. Its reward rests in an
 * alcove high in the cliff, reached by ledges hopping up over the pool (a fall lands in the water).
 */
public final class WaterfallShrinePiece extends MasterSitePiece {
	public WaterfallShrinePiece(int x, int y, int z, Direction facing, boolean alt) { super(MasterSites.WATERFALL_SHRINE, x, y, z, 17, 23, 17, facing, alt); }
	public WaterfallShrinePiece(CompoundTag tag) { super(MasterSites.WATERFALL_SHRINE, tag); }
	@Override public String site() { return "master_waterfall_shrine"; }
	@Override public List<String> schools() { return List.of("rime", "tide"); }

	/** The ledges in order, {x, z, height}: seven over the pool's back edge, six along the cliff. */
	public static int[][] ledges() {
		int[][] out = new int[13][];
		for (int i = 0; i < 7; i++) out[i] = new int[]{2 + 2 * i, 10, 1 + i};
		for (int i = 0; i < 6; i++) out[7 + i] = new int[]{13 - 2 * i, 11, 8 + i};
		return out;
	}

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		BlockState rock = alt ? s(Blocks.DARK_PRISMARINE) : s(Blocks.PACKED_ICE), rock2 = alt ? s(Blocks.STONE) : s(Blocks.SNOW_BLOCK);
		BlockState trim = alt ? s(Blocks.PRISMARINE_BRICKS) : s(Blocks.SPRUCE_PLANKS), post = alt ? s(Blocks.DARK_PRISMARINE) : s(Blocks.SPRUCE_LOG);
		BlockState carved = alt ? s(Blocks.PRISMARINE_BRICKS) : s(Blocks.STONE_BRICKS), chiseled = alt ? s(Blocks.DARK_PRISMARINE) : s(Blocks.CHISELED_STONE_BRICKS);
		BlockState rim = alt ? s(Blocks.PRISMARINE_BRICK_SLAB) : s(Blocks.SMOOTH_STONE_SLAB), light = alt ? s(Blocks.SEA_LANTERN) : s(Blocks.SOUL_LANTERN);
		Block roofStairs = alt ? Blocks.DARK_PRISMARINE_STAIRS : Blocks.SPRUCE_STAIRS;
		BlockState roofUnder = alt ? s(Blocks.DARK_PRISMARINE) : s(Blocks.SPRUCE_PLANKS), roofCap = alt ? s(Blocks.DARK_PRISMARINE_SLAB) : s(Blocks.SPRUCE_SLAB);
		Block banner = Blocks.WALL_BANNER.pick(alt ? DyeColor.CYAN : DyeColor.LIGHT_BLUE);
		pad(level, bb, 0, 0, 16, 16, 22, alt ? s(Blocks.PRISMARINE_BRICKS) : s(Blocks.STONE_BRICKS), s(Blocks.STONE));
		// The yard is swept flagstone, worn and cracked here and there, so the shrine stands clear of the drifts around it.
		if (!alt) for (int x = 0; x <= 16; x++) for (int z = 0; z <= 11; z++) {
			int n = noise(x, 3, z);
			if (n < 18) set(level, bb, s(Blocks.CRACKED_STONE_BRICKS), x, 0, z);
			else if (n < 30) set(level, bb, s(Blocks.POLISHED_ANDESITE), x, 0, z);
			else if (n < 36) set(level, bb, s(Blocks.SNOW_BLOCK), x, 0, z);
		}
		// The cliff and its frozen fall. Rime's cliff is grey stone veined with old ice and capped with snow, so the blue fall
		// stands out against it rather than vanishing into a white bank.
		for (int x = 0; x <= 16; x++) for (int y = 1; y <= 16; y++) for (int z = 12; z <= 16; z++) {
			int n = noise(x, y, z);
			BlockState c = alt ? (n < 40 ? rock2 : rock) : y == 16 ? rock2 : n < 22 ? rock : n < 62 ? s(Blocks.STONE) : n < 85 ? s(Blocks.ANDESITE) : s(Blocks.TUFF);
			set(level, bb, c, x, y, z);
		}
		// A carved face on the cliff: pilasters either side of the fall and the alcove, under a frieze.
		for (int x : new int[]{1, 5, 11, 15}) { fill(level, bb, x, 1, 12, x, 15, 12, carved); set(level, bb, chiseled, x, 1, 12); set(level, bb, chiseled, x, 15, 12); }
		fill(level, bb, 1, 16, 12, 15, 16, 12, carved);
		// The fall cuts a channel two deep into the cliff, bulges out over the pool as it nears the bottom, and spreads in a
		// frozen apron at its foot; icicles fringe its lip under the frieze.
		BlockState fall = alt ? s(Blocks.WATER) : s(Blocks.BLUE_ICE), fall2 = alt ? s(Blocks.PRISMARINE) : s(Blocks.PACKED_ICE);
		fill(level, bb, 6, 1, 13, 10, 15, 13, fall2);
		fill(level, bb, 6, 1, 12, 10, 15, 12, s(Blocks.BLUE_ICE));
		fill(level, bb, 7, 16, 12, 9, 16, 13, s(Blocks.BLUE_ICE));
		for (int x = 6; x <= 10; x++) {
			int bulge = 3 + noise(x, 0, 11) % 3 - (x == 6 || x == 10 ? 2 : 0);
			fill(level, bb, x, 1, 11, x, bulge, 11, s(Blocks.BLUE_ICE));
			if (x >= 7 && x <= 9) set(level, bb, fall2, x, 1, 10);
			if (noise(x, 15, 11) < 60) set(level, bb, s(Blocks.ICE), x, 15, 11);
			if (x % 2 == 0) set(level, bb, s(Blocks.ICE), x, 14, 11);
		}
		if (alt) fill(level, bb, 7, 17, 13, 9, 17, 13, fall);
		fill(level, bb, 2, 14, 12, 4, 15, 14, AIR);
		// The still pool: water two deep in a stone basin, a few floes on it in Rime's cold.
		fill(level, bb, 1, -2, 3, 15, 0, 11, rock2.is(Blocks.SNOW_BLOCK) ? s(Blocks.STONE) : rock2);
		fill(level, bb, 2, -1, 4, 14, 0, 10, s(Blocks.WATER));
		if (!alt) for (int x = 2; x <= 14; x++) for (int z = 4; z <= 9; z++) if (noise(x, z) < 18) set(level, bb, s(Blocks.ICE), x, 0, z);
		// Where the fall meets the pool the water churns: a ring of broken ice and pale stones, packed ice floes heaped at the foot.
		for (int x = 4; x <= 12; x++) for (int z = 8; z <= 10; z++) {
			int d = Math.abs(x - 8) + (10 - z);
			if (d < 2 || d > 5) continue;
			int n = noise(x, 9, z);
			if (alt) { if (n < 35) set(level, bb, s(Blocks.SEA_LANTERN), x, -1, z); }
			else if (n < 30) set(level, bb, s(Blocks.PACKED_ICE), x, 0, z);
			else if (n < 50) set(level, bb, s(Blocks.ICE), x, 0, z);
		}
		if (!alt) { set(level, bb, s(Blocks.PACKED_ICE), 6, 1, 10); set(level, bb, s(Blocks.PACKED_ICE), 10, 1, 10); set(level, bb, s(Blocks.BLUE_ICE), 6, 2, 10); }
		// A low slab kerb round the pool, lit at its corners.
		for (int z = 3; z <= 11; z++) { set(level, bb, rim, 1, 1, z); set(level, bb, rim, 15, 1, z); }
		for (int x = 2; x <= 14; x++) set(level, bb, rim, x, 1, 3);
		for (int[] c : new int[][]{{1, 3}, {15, 3}, {1, 11}, {15, 11}}) { set(level, bb, chiseled, c[0], 1, c[1]); set(level, bb, light, c[0], 2, c[1]); }
		var ledge = alt ? s(Blocks.PRISMARINE_BRICKS) : s(Blocks.PACKED_ICE);
		for (int[] l : ledges()) set(level, bb, ledge, l[0], l[2], l[1]);
		chest(level, bb, 3, 14, 13, rewardLoot(), Direction.SOUTH, 2);
		set(level, bb, alt ? s(Blocks.SEA_LANTERN) : s(Blocks.SOUL_LANTERN), 2, 14, 14);
		// Ice spires rising from the cliff's shoulders either side of the pavilion.
		for (int[] sp : new int[][]{{1, 15, 5}, {2, 13, 3}, {15, 14, 6}, {14, 16, 3}}) {
			for (int y = 17; y < 17 + sp[2]; y++) set(level, bb, alt ? s(Blocks.PRISMARINE) : y < 17 + sp[2] - 1 ? s(Blocks.PACKED_ICE) : s(Blocks.BLUE_ICE), sp[0], y, sp[1]);
		}
		// A railed brow on the cliff top and a hipped pavilion over the source of the fall.
		for (int x = 0; x <= 16; x++) {
			boolean pier = x == 0 || x == 3 || x == 13 || x == 16;
			if (pier) { set(level, bb, chiseled, x, 17, 12); set(level, bb, light, x, 18, 12); }
			else set(level, bb, rim, x, 17, 12);
		}
		fill(level, bb, 4, 16, 13, 12, 16, 16, carved);
		for (int[] c : new int[][]{{5, 13}, {11, 13}, {5, 16}, {11, 16}}) pillar(level, bb, c[0], c[1], 17, 19, chiseled, post, chiseled);
		hipRoof(level, bb, 5, 13, 11, 15, 20, roofStairs, roofUnder, roofCap);
		fill(level, bb, 5, 20, 13, 11, 20, 15, roofUnder);
		set(level, bb, alt ? s(Blocks.DARK_PRISMARINE) : s(Blocks.BLUE_ICE), 8, 17, 15);
		set(level, bb, light, 8, 18, 15);
		hangingLantern(level, bb, 8, 19, 14, 0, !alt);
		banner(level, bb, banner, 5, 19, 12, Direction.SOUTH); banner(level, bb, banner, 11, 19, 12, Direction.SOUTH);
		// The gate: a tie beam, a flared top rail, lanterns hung beneath and the school's colours on the posts.
		fill(level, bb, 7, 0, 0, 9, 0, 2, carved);
		fill(level, bb, 4, 1, 1, 4, 4, 1, post); fill(level, bb, 12, 1, 1, 12, 4, 1, post);
		fill(level, bb, 3, 5, 1, 13, 5, 1, trim);
		fill(level, bb, 5, 4, 1, 11, 4, 1, trim);
		fill(level, bb, 2, 6, 1, 14, 6, 1, roofUnder);
		set(level, bb, stair(roofStairs, Direction.EAST, true), 1, 6, 1); set(level, bb, stair(roofStairs, Direction.WEST, true), 15, 6, 1);
		hangingLantern(level, bb, 6, 3, 1, 0, !alt); hangingLantern(level, bb, 10, 3, 1, 0, !alt);
		banner(level, bb, banner, 4, 3, 0, Direction.SOUTH); banner(level, bb, banner, 12, 3, 0, Direction.SOUTH);
		if (alt) { set(level, bb, s(Blocks.SEA_LANTERN), 3, 5, 1); set(level, bb, s(Blocks.SEA_LANTERN), 13, 5, 1); }
		else { set(level, bb, s(Blocks.SOUL_LANTERN), 3, 1, 2); set(level, bb, s(Blocks.SOUL_LANTERN), 13, 1, 2); }
		// Stone lanterns at the outer corners of the yard.
		for (int x : new int[]{0, 16}) { fill(level, bb, x, 1, 2, x, 2, 2, carved); set(level, bb, chiseled, x, 3, 2); set(level, bb, light, x, 4, 2); }
		lectern(level, bb, 6, 1, 2);
		var fence = s(Blocks.SPRUCE_FENCE);
		dummy(level, bb, 0, 1, 5, fence); dummy(level, bb, 16, 1, 5, fence); target(level, bb, 0, 1, 9, fence); target(level, bb, 16, 1, 9, fence);
		chest(level, bb, 15, 1, 1, MasterSites.supplies(site()), Direction.WEST, 1);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(); }
}
