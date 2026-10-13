package dev.wildercord.world.sites.highland;

import dev.wildercord.world.cartographer.CartographerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Highland Survey Ring: an open stone meridian court in the Windscar Highlands where the Glass Cartographer waits.
 * Four dry-stone compass causeways meet at a central meridian pedestal; the entrance shelter holds the Survey
 * Astrolabe, a lectern with The Glass Meridian, and a supply chest.
 */
public final class SurveyRingPiece extends HighlandPiece {
	public static final int DECK = 2;

	public SurveyRingPiece(int x, int y, int z, Direction facing) {
		super(HighlandSites.RING_PIECE, x, y, z, 35, 12, 35, facing);
	}
	public SurveyRingPiece(CompoundTag tag) { super(HighlandSites.RING_PIECE, tag); }

	public BlockPos pedestal() { return world(17, DECK, 17); }
	public BlockPos astrolabe() { return world(17, DECK + 1, 4); }

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox clip, ChunkPos chunk, BlockPos pivot) {
		// Replaying a clipped piece must not erase the saved astrolabe identity or refill an opened chest.
		for (int x = 0; x <= 34; x++) for (int y = DECK + 1; y <= 11; y++) for (int z = 0; z <= 34; z++) {
			if (!furniture(x, y, z)) set(level, clip, AIR, x, y, z);
		}
		// Circular stone survey terrace and four compass causeways, with recessed gravel sectors between them.
		for (int x = 2; x <= 32; x++) for (int z = 2; z <= 32; z++) {
			double dist = Math.hypot(x - 17, z - 17);
			if (dist > 15.5) continue;
			footing(level, clip, x, z, x, z, DECK - 1);
			boolean causeway = Math.abs(x - 17) <= 2 || Math.abs(z - 17) <= 2;
			boolean ring = dist >= 12.5 && dist <= 15.0;
			boolean center = Math.abs(x - 17) <= 3 && Math.abs(z - 17) <= 3;
			int n = noise(x, DECK, z);
			BlockState floor;
			if (center) floor = Blocks.POLISHED_ANDESITE.defaultBlockState();
			else if (causeway || ring) floor = n < 22 ? MOSSY_BRICK : n < 50 ? CRACKED_BRICK : BRICK;
			else floor = n < 35 ? Blocks.GRAVEL.defaultBlockState() : n < 70 ? Blocks.TUFF.defaultBlockState() : ANDESITE;
			set(level, clip, floor, x, DECK, z);
		}
		// Stepped approach on all four compass arms so the ring never drops off sharply.
		for (int i = 15; i <= 19; i++) {
			for (int step : new int[]{1, 33}) {
				footing(level, clip, i, step, i, step, DECK - 1);
				footing(level, clip, step, i, step, i, DECK - 1);
				set(level, clip, COBBLE, i, DECK - 1, step);
				set(level, clip, COBBLE, step, DECK - 1, i);
			}
		}
		// Central Meridian Pedestal (17, DECK, 17).
		for (int x = 15; x <= 19; x++) for (int z = 15; z <= 19; z++) {
			set(level, clip, (x == 15 || x == 19 || z == 15 || z == 19) ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState(), x, DECK, z);
		}
		set(level, clip, Blocks.LODESTONE.defaultBlockState(), 17, DECK, 17);

		// Eight weathered stone-and-copper survey pillars around the perimeter.
		for (int[] c : new int[][]{{6, 6}, {28, 6}, {6, 28}, {28, 28}, {12, 3}, {22, 3}, {12, 31}, {22, 31}}) {
			for (int y = DECK + 1; y <= DECK + 3; y++) set(level, clip, y == DECK + 2 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : BRICK, c[0], y, c[1]);
			set(level, clip, Blocks.STONE_BRICK_WALL.defaultBlockState(), c[0], DECK + 4, c[1]);
			set(level, clip, Blocks.LANTERN.defaultBlockState(), c[0], DECK + 5, c[1]);
		}

		// North entrance shelter holding the Survey Astrolabe, instruction lectern, and supply chest.
		for (int x : new int[]{14, 20}) for (int z : new int[]{2, 6}) {
			for (int y = DECK + 1; y <= DECK + 3; y++) set(level, clip, Blocks.SPRUCE_LOG.defaultBlockState(), x, y, z);
		}
		for (int x = 14; x <= 20; x++) for (int z = 2; z <= 6; z++) {
			set(level, clip, (x == 14 || x == 20 || z == 2 || z == 6) ? Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState() : SPRUCE, x, DECK + 4, z);
		}
		set(level, clip, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 17, DECK, 4);
		set(level, clip, CartographerContent.ASTROLABE.defaultBlockState(), 17, DECK + 1, 4);
		lectern(level, clip, 15, DECK + 1, 4, Direction.SOUTH,
			HighlandSites.book("survey_ring", "The Glass Meridian", "Marchkeeper Survey"));
		chest(level, clip, 19, DECK + 1, 4, Direction.SOUTH, HighlandSites.loot(HighlandSites.SURVEY_RING), 43L);
	}

	private static boolean furniture(int x, int y, int z) {
		return y == DECK + 1 && z == 4 && (x == 15 || x == 17 || x == 19);
	}
}
