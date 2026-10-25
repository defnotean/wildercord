package dev.wildercord.world.sites.highland;

import dev.wildercord.world.cartographer.CartographerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
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
			set(level, clip, BRICK, c[0], DECK + 4, c[1]);
			set(level, clip, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), c[0], DECK + 5, c[1]);
			set(level, clip, Blocks.STONE_BRICK_WALL.defaultBlockState(), c[0], DECK + 6, c[1]);
			set(level, clip, Blocks.LANTERN.defaultBlockState(), c[0], DECK + 7, c[1]);
			for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
				int x = c[0] + (d == Direction.EAST ? -1 : d == Direction.WEST ? 1 : 0);
				int z = c[1] + (d == Direction.NORTH ? -1 : d == Direction.SOUTH ? 1 : 0);
				set(level, clip, st(Blocks.STONE_BRICK_STAIRS, d, true), x, DECK + 4, z);
				if (terrace(x, z)) set(level, clip, st(Blocks.STONE_BRICK_STAIRS, d, false), x, DECK + 1, z);
			}
		}

		// A low dry-stone balustrade round the terrace rim, broken where the causeways run out.
		for (int x = 2; x <= 32; x++) for (int z = 2; z <= 32; z++) {
			double dist = Math.hypot(x - 17, z - 17);
			if (dist <= 14.6 || dist > 15.5 || Math.abs(x - 17) <= 3 || Math.abs(z - 17) <= 3) continue;
			set(level, clip, Blocks.STONE_BRICK_WALL.defaultBlockState(), x, DECK + 1, z);
		}
		// Timber gateways on the south, east and west arms; the entrance shelter is the north gate.
		for (int x : new int[]{14, 20}) fill(level, clip, x, DECK + 1, 31, x, DECK + 3, 31, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
		for (int z : new int[]{14, 20}) {
			fill(level, clip, 3, DECK + 1, z, 3, DECK + 3, z, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
			fill(level, clip, 31, DECK + 1, z, 31, DECK + 3, z, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState());
		}
		fill(level, clip, 14, DECK + 4, 31, 20, DECK + 4, 31, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		fill(level, clip, 3, DECK + 4, 14, 3, DECK + 4, 20, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
		fill(level, clip, 31, DECK + 4, 14, 31, DECK + 4, 20, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
		for (int[] g : new int[][]{{17, 31}, {3, 17}, {31, 17}}) {
			set(level, clip, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), g[0], DECK + 4, g[1]);
			set(level, clip, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState(), g[0], DECK + 5, g[1]);
			set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), g[0], DECK + 3, g[1]);
		}
		for (int[] f : new int[][]{{14, 31}, {20, 31}, {3, 14}, {3, 20}, {31, 14}, {31, 20}}) {
			set(level, clip, Blocks.STONE_BRICK_WALL.defaultBlockState(), f[0], DECK + 5, f[1]);
		}
		// Lamp pedestals between the causeways, one in each gravel sector, lighting the plinth from four sides.
		for (int x : new int[]{13, 21}) for (int z : new int[]{13, 21}) {
			set(level, clip, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), x, DECK + 1, z);
			set(level, clip, Blocks.LANTERN.defaultBlockState(), x, DECK + 2, z);
		}

		// North entrance shelter holding the Survey Astrolabe, instruction lectern, and supply chest.
		for (int x : new int[]{14, 20}) for (int z : new int[]{2, 6}) {
			for (int y = DECK + 1; y <= DECK + 3; y++) set(level, clip, Blocks.SPRUCE_LOG.defaultBlockState(), x, y, z);
		}
		for (int x = 14; x <= 20; x++) for (int z = 2; z <= 6; z++) {
			set(level, clip, (x == 14 || x == 20 || z == 2 || z == 6) ? Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState() : SPRUCE, x, DECK + 4, z);
		}
		hip(level, clip, 15, 3, 19, 5, DECK + 5, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES.defaultBlockState());
		set(level, clip, Blocks.LANTERN.defaultBlockState(), 17, DECK + 8, 4);
		for (int z : new int[]{2, 6}) {
			set(level, clip, st(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 15, DECK + 3, z);
			set(level, clip, st(Blocks.SPRUCE_STAIRS, Direction.EAST, true), 19, DECK + 3, z);
		}
		for (int x : new int[]{14, 20}) {
			set(level, clip, st(Blocks.SPRUCE_STAIRS, Direction.SOUTH, true), x, DECK + 3, 3);
			set(level, clip, st(Blocks.SPRUCE_STAIRS, Direction.NORTH, true), x, DECK + 3, 5);
		}
		set(level, clip, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 17, DECK + 3, 3);
		set(level, clip, Blocks.BARREL.defaultBlockState(), 13, DECK + 1, 3);
		set(level, clip, Blocks.BARREL.defaultBlockState(), 13, DECK + 2, 3);
		set(level, clip, Blocks.BARREL.defaultBlockState(), 21, DECK + 1, 5);
		set(level, clip, Blocks.BARREL.defaultBlockState(), 21, DECK + 1, 3);
		set(level, clip, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 17, DECK, 4);
		set(level, clip, CartographerContent.ASTROLABE.defaultBlockState(), 17, DECK + 1, 4);
		lectern(level, clip, 15, DECK + 1, 4, Direction.SOUTH,
			HighlandSites.book("survey_ring", "The Glass Meridian", "Marchkeeper Survey"));
		chest(level, clip, 19, DECK + 1, 4, Direction.SOUTH, HighlandSites.loot(HighlandSites.SURVEY_RING), 43L);
	}

	private void hip(WorldGenLevel level, BoundingBox clip, int x0, int z0, int x1, int z1, int y, Block stairs, BlockState cap) {
		for (int k = 0; ; k++) {
			int a = x0 - 1 + k, b = x1 + 1 - k, c = z0 - 1 + k, d = z1 + 1 - k, yy = y + k;
			if (a > b || c > d) return;
			if (a == b || c == d) {
				fill(level, clip, a, yy, c, b, yy, d, cap);
				return;
			}
			for (int x = a; x <= b; x++) {
				set(level, clip, st(stairs, Direction.NORTH, false), x, yy, c);
				set(level, clip, st(stairs, Direction.SOUTH, false), x, yy, d);
			}
			for (int z = c + 1; z <= d - 1; z++) {
				set(level, clip, st(stairs, Direction.EAST, false), a, yy, z);
				set(level, clip, st(stairs, Direction.WEST, false), b, yy, z);
			}
		}
	}

	private static boolean terrace(int x, int z) {
		return Math.hypot(x - 17, z - 17) <= 15.5;
	}

	private static BlockState st(Block stairs, Direction facing, boolean top) {
		return stairs.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
	}

	private static boolean furniture(int x, int y, int z) {
		return y == DECK + 1 && z == 4 && (x == 15 || x == 17 || x == 19);
	}
}
