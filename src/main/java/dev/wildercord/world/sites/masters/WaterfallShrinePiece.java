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
 * A snowfield shrine of the Rime or Tide school: a frozen waterfall down a cliff over a still pool. Its reward rests in an
 * alcove high in the cliff, reached by ledges hopping up over the pool (a fall lands in the water).
 */
public final class WaterfallShrinePiece extends MasterSitePiece {
	public WaterfallShrinePiece(int x, int y, int z, Direction facing, boolean alt) { super(MasterSites.WATERFALL_SHRINE, x, y, z, 17, 18, 17, facing, alt); }
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
		pad(level, bb, 0, 0, 16, 16, 17, alt ? s(Blocks.PRISMARINE_BRICKS) : s(Blocks.SNOW_BLOCK), s(Blocks.STONE));
		// The cliff and its frozen fall.
		for (int x = 0; x <= 16; x++) for (int y = 1; y <= 16; y++) for (int z = 12; z <= 16; z++) set(level, bb, noise(x, y, z) < 40 ? rock2 : rock, x, y, z);
		fill(level, bb, 6, 1, 12, 10, 15, 12, s(Blocks.BLUE_ICE));
		fill(level, bb, 7, 1, 11, 9, 2, 11, s(Blocks.BLUE_ICE));
		fill(level, bb, 2, 14, 12, 4, 15, 14, AIR);
		// The still pool: water two deep in a stone basin, a few floes on it in Rime's cold.
		fill(level, bb, 1, -2, 3, 15, 0, 11, rock2.is(Blocks.SNOW_BLOCK) ? s(Blocks.STONE) : rock2);
		fill(level, bb, 2, -1, 4, 14, 0, 10, s(Blocks.WATER));
		if (!alt) for (int x = 2; x <= 14; x++) for (int z = 4; z <= 9; z++) if (noise(x, z) < 18) set(level, bb, s(Blocks.ICE), x, 0, z);
		var ledge = alt ? s(Blocks.PRISMARINE_BRICKS) : s(Blocks.PACKED_ICE);
		for (int[] l : ledges()) set(level, bb, ledge, l[0], l[2], l[1]);
		chest(level, bb, 3, 14, 13, rewardLoot(), Direction.SOUTH, 2);
		set(level, bb, alt ? s(Blocks.SEA_LANTERN) : s(Blocks.SOUL_LANTERN), 2, 14, 14);
		// The gate, the lectern and the practice posts by the pool.
		fill(level, bb, 4, 1, 1, 4, 4, 1, post); fill(level, bb, 12, 1, 1, 12, 4, 1, post);
		fill(level, bb, 3, 5, 1, 13, 5, 1, trim);
		if (alt) { set(level, bb, s(Blocks.SEA_LANTERN), 3, 5, 1); set(level, bb, s(Blocks.SEA_LANTERN), 13, 5, 1); }
		else { set(level, bb, s(Blocks.SOUL_LANTERN), 3, 1, 2); set(level, bb, s(Blocks.SOUL_LANTERN), 13, 1, 2); }
		lectern(level, bb, 6, 1, 2);
		var fence = s(Blocks.SPRUCE_FENCE);
		dummy(level, bb, 0, 1, 5, fence); dummy(level, bb, 16, 1, 5, fence); target(level, bb, 0, 1, 9, fence); target(level, bb, 16, 1, 9, fence);
		chest(level, bb, 15, 1, 1, MasterSites.supplies(site()), Direction.WEST, 1);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(); }
}
