package dev.wildercord.world.dungeons;

import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.content.dungeons.DungeonAltarBlock;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * The Drowned Scriptorium, on the floor of the deep ocean, laid out in its own coordinates (x across,
 * y up, z inward from the entrance). The sea floor at the entrance is at y 2; the building stands on
 * foundations over it, its halls dry inside (floor at y 8, you stand at 9), kept so by the old trick
 * of a way in from below: water never rises through a hole in a floor.
 *
 * <pre>
 *   z 0-5    the approach: a lantern-lit path over the sea floor, kelp either side
 *   z 5-18   the flooded tunnel under the building, two air pockets in its roof, up through the moon pool
 *   z 14-24  the antechamber, dry; a Rune Seal door of Frost and Storm
 *   z 24-42  the Hall of Shelves: kelp in glass tanks in the walls, drowned Runebound, a chest
 *   z 42     a second door, of Life and Wind
 *   z 47-79  the Arena: a balcony round a pit that floods and drains, under a dome of glass; the core
 *   x 36-41  the Vault, off the balcony behind a door of Storm and Arcane
 * </pre>
 */
public class DrownedScriptoriumPiece extends DungeonPiece {
	public static final int WIDTH = 42;
	public static final int HEIGHT = 27;
	public static final int DEPTH = 81;
	private static final int SEABED = 2;
	/** The dry floor's local y. */
	public static final int F = 8;
	/** The arena pit's floor: two blocks of flood over it, the balcony a block above that. */
	private static final int PIT = 5;
	private static final int CX = 20;
	private static final int ARENA_Z = 63;
	private static final int PIT_R = 12;
	private static final int BALCONY_R = 15;
	private static final int WALL_R = 16;
	private static final double RISE = 10.0;

	private static final BlockState PRISMARINE = Blocks.PRISMARINE.defaultBlockState();
	private static final BlockState BRICKS = Blocks.PRISMARINE_BRICKS.defaultBlockState();
	private static final BlockState DARK = Blocks.DARK_PRISMARINE.defaultBlockState();
	private static final BlockState LIGHT = Blocks.SEA_LANTERN.defaultBlockState();
	private static final BlockState GLASS = Blocks.GLASS.defaultBlockState();
	private static final BlockState WATER = Blocks.WATER.defaultBlockState();
	private static final BlockState SHELF = Blocks.BOOKSHELF.defaultBlockState();
	/** Outer masonry: bricks with rough prismarine and dark courses worked in. */
	private static final Palette ROOF = Palette.of(Blocks.PRISMARINE_BRICKS, 6, Blocks.PRISMARINE, 3, Blocks.DARK_PRISMARINE, 1);

	public DrownedScriptoriumPiece(int x, int y, int z, Direction facing) {
		super(DungeonWorldgen.DROWNED_SCRIPTORIUM_PIECE, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public DrownedScriptoriumPiece(CompoundTag tag) {
		super(DungeonWorldgen.DROWNED_SCRIPTORIUM_PIECE, tag);
	}

	/** Finds its footing: the sea floor at the entrance. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext context) {
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
		ChunkPos chunk = context.chunkPos();
		DrownedScriptoriumPiece piece = new DrownedScriptoriumPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), facing);
		BlockPos entrance = piece.getWorldPos(CX, 0, 2);
		int ground = context.chunkGenerator().getFirstOccupiedHeight(entrance.getX(), entrance.getZ(), Heightmap.Types.OCEAN_FLOOR_WG,
			context.heightAccessor(), context.randomState());
		piece.move(0, ground - 1 - SEABED, 0);
		return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(), ground, entrance.getZ()), builder -> builder.addPiece(piece)));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos reference) {
		dev.wildercord.world.dungeons.DungeonWards.remember(level, this);
		approach(level, bb);
		foundation(level, bb, 12, 14, 28, 24, F - 1);
		foundation(level, bb, 8, 24, 32, 42, F - 1);
		foundation(level, bb, 15, 42, 25, 48, F - 1);
		foundation(level, bb, 36, 57, 41, 69, F - 1);
		antechamber(level, bb);
		tunnel(level, bb);
		facade(level, bb);
		hall(level, bb);
		corridor(level, bb);
		arena(level, bb);
		vault(level, bb);
		gate(level, bb, 24, RuneSealBlock.Element.FROST, RuneSealBlock.Element.STORM);
		gate(level, bb, 42, RuneSealBlock.Element.LIFE, RuneSealBlock.Element.WIND);
		guards(level, bb);
	}

	/** Prismarine under a stretch of floor, from {@code top} down to the sea floor. */
	private void foundation(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1, int top) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				column(level, bb, x, z, top);
			}
		}
	}

	private void column(WorldGenLevel level, BoundingBox bb, int x, int z, int top) {
		for (int y = top; y >= 0; y--) {
			BlockState there = getBlock(level, x, y, z, bb);
			if (!there.isAir() && there.getFluidState().isEmpty()) {
				break;
			}
			set(level, bb, y == top ? DARK : PRISMARINE, x, y, z);
		}
	}

	/** The highest solid block of the sea floor in a column, looking down from {@code from}. */
	private int seabed(WorldGenLevel level, BoundingBox bb, int x, int z, int from) {
		for (int y = from; y > 0; y--) {
			BlockState there = getBlock(level, x, y, z, bb);
			if (!there.isAir() && there.getFluidState().isEmpty()) {
				return y;
			}
		}
		return SEABED;
	}

	// ------------------------------------------------------------------ the approach and the tunnel

	private void approach(WorldGenLevel level, BoundingBox bb) {
		for (int z = 0; z <= 5; z++) {
			for (int x = 15; x <= 25; x++) {
				int floor = seabed(level, bb, x, z, 7);
				boolean path = x >= 18 && x <= 22;
				set(level, bb, path ? (x == CX ? DARK : BRICKS) : PRISMARINE, x, floor, z);
				// Kelp either side of the path, and sea pickles glowing among it.
				if ((x == 15 || x == 25) && getBlock(level, x, floor + 1, z, bb).is(Blocks.WATER)) {
					int tall = 2 + noise(x, z) % 3;
					for (int y = floor + 1; y < floor + tall; y++) {
						set(level, bb, Blocks.KELP_PLANT.defaultBlockState(), x, y, z);
					}
					set(level, bb, Blocks.KELP.defaultBlockState(), x, floor + tall, z);
				} else if ((x == 16 || x == 24) && z % 2 == 0 && getBlock(level, x, floor + 1, z, bb).is(Blocks.WATER)) {
					set(level, bb, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, 3), x, floor + 1, z);
				}
			}
			// Lantern pillars flanking the way.
			if (z % 2 == 1) {
				for (int x : new int[] {17, 23}) {
					int floor = seabed(level, bb, x, z, 7);
					for (int y = floor + 1; y <= floor + 3; y++) {
						set(level, bb, BRICKS, x, y, z);
					}
					set(level, bb, LIGHT, x, floor + 4, z);
				}
			}
			// The channel to the tunnel's mouth stays open.
			fill(level, bb, 18, 3, z, 22, 6, z, WATER);
		}
	}

	/** Under the building: a flooded tunnel with two air pockets in its roof, rising through the moon pool. */
	private void tunnel(WorldGenLevel level, BoundingBox bb) {
		fill(level, bb, 17, 2, 5, 23, 7, 18, BRICKS);
		fill(level, bb, 18, 2, 5, 22, 2, 18, DARK);
		fill(level, bb, 18, 3, 5, 22, 6, 18, WATER);
		for (int z : new int[] {7, 10, 13, 16}) {
			set(level, bb, LIGHT, 17, 5, z);
			set(level, bb, LIGHT, 23, 5, z);
		}
		// Air pockets: a breath held in the roof.
		for (int z : new int[] {9, 13}) {
			fill(level, bb, 18, 7, z - 1, 22, 9, z + 1, BRICKS);
			fill(level, bb, 19, 7, z, 21, 8, z, AIR);
			set(level, bb, LIGHT, 20, 9, z);
		}
		// Dark ribs ring the tunnel between the lanterns.
		for (int z : new int[] {6, 11, 15}) {
			fill(level, bb, 17, 3, z, 17, 7, z, DARK);
			fill(level, bb, 23, 3, z, 23, 7, z, DARK);
			fill(level, bb, 18, 7, z, 22, 7, z, DARK);
		}
		// The moon pool: up through the roof and the antechamber's floor. The water stops at the floor.
		fill(level, bb, 19, 7, 16, 21, 8, 18, WATER);
	}

	/** The way in: a great gate over the tunnel's mouth between two crowned pylons, under a stepped pediment. */
	private void facade(WorldGenLevel level, BoundingBox bb) {
		for (int x0 : new int[] {13, 24}) {
			// A pylon on a dark plinth, its crown overhanging the approach on a corbel course.
			for (int x = x0; x <= x0 + 3; x++) {
				for (int z = 4; z <= 6; z++) {
					column(level, bb, x, z, 2);
					for (int y = 3; y <= 15; y++) {
						set(level, bb, y <= 4 ? DARK : ROOF.at(x, y, z), x, y, z);
					}
				}
				set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.NORTH, true), x, 13, 3);
				fill(level, bb, x, 14, 3, x, 15, 3, DARK);
			}
			fill(level, bb, x0, 5, 4, x0, 15, 4, DARK);
			fill(level, bb, x0 + 3, 5, 4, x0 + 3, 15, 4, DARK);
			// A dark panel with a lit eye.
			fill(level, bb, x0 + 1, 6, 4, x0 + 2, 11, 4, DARK);
			fill(level, bb, x0 + 1, 10, 4, x0 + 2, 10, 4, LIGHT);
			crenellate(level, bb, x0, 3, x0 + 3, 6, 16, DARK);
			set(level, bb, LIGHT, x0, 17, 3);
			set(level, bb, LIGHT, x0 + 3, 17, 3);
		}
		// Jambs and an arch head round the channel, which stays open.
		fill(level, bb, 17, 3, 4, 17, 7, 4, DARK);
		fill(level, bb, 23, 3, 4, 23, 7, 4, DARK);
		fill(level, bb, 18, 7, 4, 22, 7, 4, DARK);
		// The lintel, a corbel course under its face, and a stepped pediment over it.
		fill(level, bb, 17, 8, 4, 23, 10, 6, ROOF);
		set(level, bb, LIGHT, CX, 9, 4);
		for (int x = 17; x <= 23; x++) {
			set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.NORTH, true), x, 10, 3);
		}
		for (int k = 0; k <= 3; k++) {
			fill(level, bb, 17 + k, 11 + k, 4, 23 - k, 11 + k, 5, k == 0 ? DARK : BRICKS);
			if (k > 0) {
				for (int z = 4; z <= 5; z++) {
					set(level, bb, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.EAST, false), 16 + k, 11 + k, z);
					set(level, bb, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.WEST, false), 24 - k, 11 + k, z);
				}
			}
		}
		set(level, bb, LIGHT, CX, 12, 4);
		set(level, bb, DARK, CX, 15, 4);
		set(level, bb, LIGHT, CX, 16, 4);
	}

	// ------------------------------------------------------------------ the antechamber

	private void antechamber(WorldGenLevel level, BoundingBox bb) {
		room(level, bb, 12, F, 14, 28, F + 7, 24, BRICKS);
		fill(level, bb, 13, F, 15, 27, F, 23, DARK);
		// Sea lanterns ring the moon pool.
		fill(level, bb, 18, F, 15, 22, F, 19, LIGHT);
		for (int z : new int[] {16, 22}) {
			set(level, bb, LIGHT, 13, F + 3, z);
			set(level, bb, LIGHT, 27, F + 3, z);
		}
		// Dark pilasters in the corners and a corbelled cornice round the ceiling.
		for (int[] c : new int[][] {{13, 15}, {27, 15}, {13, 23}, {27, 23}}) {
			fill(level, bb, c[0], F + 1, c[1], c[0], F + 6, c[1], DARK);
		}
		for (int x = 14; x <= 26; x++) {
			set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.SOUTH, true), x, F + 6, 15);
			set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.NORTH, true), x, F + 6, 23);
		}
		for (int z = 16; z <= 22; z++) {
			set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.WEST, true), 13, F + 6, z);
			set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.EAST, true), 27, F + 6, z);
		}
		// Outside: dark pilasters up the front and merlons round the roof.
		for (int x : new int[] {12, 16, 24, 28}) {
			column(level, bb, x, 13, F - 1);
			fill(level, bb, x, F, 13, x, F + 8, 13, DARK);
		}
		crenellate(level, bb, 12, 14, 28, 24, F + 8, DARK);
	}

	/** A seal door in the wall at {@code z}, framed in dark prismarine with a sea lantern above. */
	private void gate(WorldGenLevel level, BoundingBox bb, int z, RuneSealBlock.Element a, RuneSealBlock.Element b) {
		sealDoorAcross(level, bb, z, 18, 22, F + 1, F + 4, a, b);
		for (int y = F + 1; y <= F + 5; y++) {
			set(level, bb, DARK, 17, y, z);
			set(level, bb, DARK, 23, y, z);
		}
		fill(level, bb, 17, F + 5, z, 23, F + 5, z, DARK);
		set(level, bb, LIGHT, 20, F + 6, z);
	}

	// ------------------------------------------------------------------ the Hall of Shelves

	private void hall(WorldGenLevel level, BoundingBox bb) {
		room(level, bb, 8, F, 24, 32, F + 9, 42, BRICKS);
		fill(level, bb, 9, F, 25, 31, F, 41, DARK);
		fill(level, bb, 19, F + 1, 25, 21, F + 1, 41, Blocks.CARPET.pick(DyeColor.CYAN).defaultBlockState());
		for (int z = 25; z <= 41; z++) {
			boolean tank = z % 4 == 0 && z >= 28;
			for (int y = F + 1; y <= F + 4; y++) {
				set(level, bb, tank && y > F + 1 ? GLASS : SHELF, 9, y, z);
				set(level, bb, tank && y > F + 1 ? GLASS : SHELF, 31, y, z);
			}
			if (tank) {
				// Kelp behind glass, set into the walls.
				for (int x : new int[] {8, 32}) {
					set(level, bb, Blocks.KELP_PLANT.defaultBlockState(), x, F + 2, z);
					set(level, bb, Blocks.KELP_PLANT.defaultBlockState(), x, F + 3, z);
					set(level, bb, Blocks.KELP.defaultBlockState(), x, F + 4, z);
				}
			}
			// Free-standing shelves in two rows, with a gap to pass through.
			if (z >= 27 && z <= 39 && z != 33) {
				for (int x : new int[] {13, 14, 26, 27}) {
					for (int y = F + 1; y <= F + 3; y++) {
						set(level, bb, SHELF, x, y, z);
					}
				}
			}
		}
		// A pointed vault over the hall, carved out of a prismarine shell thick enough to keep the sea out. Three
		// dark transverse ribs cross it, sea lanterns set in each.
		fill(level, bb, 8, F + 9, 24, 32, F + 14, 42, ROOF);
		for (int x = 9; x <= 31; x++) {
			int top = vaultTop(x);
			for (int z = 25; z <= 41; z++) {
				boolean rib = z == 27 || z == 33 || z == 39;
				for (int y = F + 9; y <= top; y++) {
					set(level, bb, rib && y == top ? (x == 14 || x == CX || x == 26 ? LIGHT : DARK) : AIR, x, y, z);
				}
			}
		}
		// The ribs come down onto dark pilasters over the wall shelves, with a lantern set in each and a corbel at the top.
		for (int z : new int[] {27, 33, 39}) {
			fill(level, bb, 9, F + 5, z, 9, F + 8, z, DARK);
			fill(level, bb, 31, F + 5, z, 31, F + 8, z, DARK);
			set(level, bb, LIGHT, 9, F + 6, z);
			set(level, bb, LIGHT, 31, F + 6, z);
			set(level, bb, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.WEST, true), 10, F + 8, z);
			set(level, bb, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.EAST, true), 30, F + 8, z);
		}
		// A cornice along both walls between the pilasters, and windows onto the deep.
		for (int z = 25; z <= 41; z++) {
			if (z == 27 || z == 33 || z == 39) {
				continue;
			}
			set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.WEST, true), 9, F + 8, z);
			set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.EAST, true), 31, F + 8, z);
		}
		for (int z : new int[] {30, 36}) {
			fill(level, bb, 8, F + 6, z, 8, F + 7, z, GLASS);
			fill(level, bb, 32, F + 6, z, 32, F + 7, z, GLASS);
		}
		// Rose windows high in both end walls, a sea lantern at the heart of each.
		for (int z : new int[] {24, 42}) {
			fill(level, bb, 19, F + 10, z, 21, F + 12, z, GLASS);
			set(level, bb, LIGHT, CX, F + 11, z);
		}
		for (int z : new int[] {30, 36}) {
			fill(level, bb, 20, F + 7, z, 20, vaultTop(CX), z, Blocks.IRON_CHAIN.defaultBlockState());
			set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 20, F + 6, z);
		}
		// Outside: stepped buttresses against the long walls, a parapet round the roof and a ridge down its middle.
		for (int z : new int[] {27, 33, 39}) {
			for (int x = 5; x <= 7; x++) {
				column(level, bb, x, z, F - 1);
				column(level, bb, 40 - x, z, F - 1);
			}
			buttress(level, bb, 7, z, F, 12, Direction.WEST, DARK, Blocks.PRISMARINE_BRICK_STAIRS);
			buttress(level, bb, 33, z, F, 12, Direction.EAST, DARK, Blocks.PRISMARINE_BRICK_STAIRS);
		}
		crenellate(level, bb, 8, 24, 32, 42, F + 15, DARK);
		fill(level, bb, 16, F + 15, 26, 24, F + 15, 40, ROOF);
		fill(level, bb, 18, F + 16, 26, 22, F + 16, 40, DARK);
		for (int z = 27; z <= 39; z += 6) {
			set(level, bb, LIGHT, CX, F + 17, z);
		}
		chest(level, bb, 30, F + 1, 40, DungeonWorldgen.TIDE_HALL, Direction.WEST, 21);
	}

	/** The hall vault's highest open block over column x: the old ceiling line at the walls, five higher at the middle. */
	private static int vaultTop(int x) {
		double t = (x - CX) / 11.5;
		return F + 8 + (int) Math.round(5 * (1 - t * t));
	}

	// ------------------------------------------------------------------ the corridor

	private void corridor(WorldGenLevel level, BoundingBox bb) {
		room(level, bb, 15, F, 42, 25, F + 7, 48, BRICKS);
		fill(level, bb, 16, F, 43, 24, F, 47, DARK);
		set(level, bb, LIGHT, 16, F + 3, 45);
		set(level, bb, LIGHT, 24, F + 3, 45);
		// Pilasters at both ends and a dark rib across the ceiling between the lanterns.
		for (int z : new int[] {43, 47}) {
			fill(level, bb, 16, F + 1, z, 16, F + 6, z, DARK);
			fill(level, bb, 24, F + 1, z, 24, F + 6, z, DARK);
		}
		fill(level, bb, 16, F + 7, 45, 24, F + 7, 45, DARK);
		set(level, bb, LIGHT, CX, F + 7, 45);
		for (int z = 44; z <= 46; z++) {
			set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.WEST, true), 16, F + 6, z);
			set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.EAST, true), 24, F + 6, z);
		}
	}

	// ------------------------------------------------------------------ the Arena

	/** Inside the dome, over the walls' tops: an upturned bowl of air. */
	private static boolean domeAir(int x, int y, int z) {
		double d = dist(x, z, CX, ARENA_Z);
		if (y <= F + 6) {
			return y > F && d <= BALCONY_R;
		}
		double h = y - (F + 6);
		double e = (d / (BALCONY_R + 0.5)) * (d / (BALCONY_R + 0.5)) + (h / RISE) * (h / RISE);
		return d <= BALCONY_R && e <= 0.9;
	}

	/** The arena, from under its pit to its glass dome, and the vault off the balcony (with the door between). */
	@Override
	public java.util.List<BoundingBox> wardedBoxes() {
		return java.util.List.of(
			worldBox(CX - WALL_R - 2, 0, ARENA_Z - WALL_R - 2, CX + WALL_R + 2, HEIGHT - 1, Math.min(DEPTH - 1, ARENA_Z + WALL_R + 2)),
			worldBox(32, 0, 56, WIDTH - 1, F + 9, 70));
	}

	private void arena(WorldGenLevel level, BoundingBox bb) {
		for (int x = CX - WALL_R - 1; x <= CX + WALL_R + 1; x++) {
			for (int z = ARENA_Z - WALL_R - 1; z <= ARENA_Z + WALL_R + 1; z++) {
				double d = dist(x, z, CX, ARENA_Z);
				if (d > WALL_R) {
					continue;
				}
				column(level, bb, x, z, PIT - 1);
				if (d > BALCONY_R) {
					// The outer wall.
					for (int y = PIT; y <= F + 6; y++) {
						set(level, bb, y == F + 3 ? LIGHT : BRICKS, x, y, z);
					}
				} else if (d > PIT_R) {
					// The balcony: solid down to the pit's floor, so the flood stays in the pit.
					for (int y = PIT; y < F; y++) {
						set(level, bb, PRISMARINE, x, y, z);
					}
					set(level, bb, Math.abs(d - 14) < 0.5 && noise(x, z) < 40 ? LIGHT : DARK, x, F, z);
					for (int y = F + 1; y <= F + 6; y++) {
						set(level, bb, AIR, x, y, z);
					}
				} else {
					set(level, bb, pitFloor(x, z, d), x, PIT, z);
					for (int y = PIT + 1; y <= F + 6; y++) {
						set(level, bb, AIR, x, y, z);
					}
				}
				// The dome: air under the bowl, and a shell wherever air would touch the sea: glass between eight
				// dark prismarine ribs, a sea lantern at the crown.
				boolean rib = onSpoke(x, z, CX, ARENA_Z, 8, 0.6);
				for (int y = F + 7; y < HEIGHT; y++) {
					if (domeAir(x, y, z)) {
						set(level, bb, AIR, x, y, z);
					} else if (domeAir(x + 1, y, z) || domeAir(x - 1, y, z) || domeAir(x, y, z + 1) || domeAir(x, y, z - 1)
							|| domeAir(x, y - 1, z) || domeAir(x, y + 1, z)) {
						set(level, bb, x == CX && z == ARENA_Z ? LIGHT : rib ? DARK : GLASS, x, y, z);
					}
				}
			}
		}
		// Six pedestals in the pit, a sea lantern on each: somewhere dry to stand when the tide comes in.
		for (int i = 0; i < 6; i++) {
			double a = Math.PI * 2 * i / 6 + Math.PI / 6;
			int px = CX + (int) Math.round(Math.cos(a) * 8);
			int pz = ARENA_Z + (int) Math.round(Math.sin(a) * 8);
			fill(level, bb, px, PIT + 1, pz, px, F - 1, pz, BRICKS);
			set(level, bb, LIGHT, px, F, pz);
		}
		// Dark columns round the balcony carry the dome's ribs down to the floor, a sea lantern in each; none stands in
		// the way in (south) or before the vault door (east).
		for (int k : new int[] {1, 2, 3, 4, 5, 7}) {
			double a = Math.PI / 4 * k;
			int px = CX + (int) Math.round(Math.cos(a) * 14);
			int pz = ARENA_Z + (int) Math.round(Math.sin(a) * 14);
			pillar(level, bb, px, pz, F + 1, F + 8, DARK, BRICKS, DARK);
			set(level, bb, LIGHT, px, F + 4, pz);
		}
		// The conduit hangs on a chain from the crown.
		fill(level, bb, CX, F + 6, ARENA_Z, CX, F + 15, ARENA_Z, Blocks.IRON_CHAIN.defaultBlockState());
		// Steps down from the balcony, and the way in.
		fill(level, bb, 19, PIT + 1, 51, 21, PIT + 2, 51, BRICKS);
		fill(level, bb, 19, PIT + 1, 52, 21, PIT + 1, 52, BRICKS);
		fill(level, bb, 18, F, 47, 22, F, 50, DARK);
		fill(level, bb, 18, F + 1, 47, 22, F + 5, 50, AIR);
		// A dark portal where the way in meets the balcony.
		fill(level, bb, 17, F + 1, 50, 17, F + 6, 50, DARK);
		fill(level, bb, 23, F + 1, 50, 23, F + 6, 50, DARK);
		fill(level, bb, 18, F + 6, 50, 22, F + 6, 50, DARK);
		set(level, bb, LIGHT, CX, F + 6, 50);
		set(level, bb, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.WEST, true), 18, F + 5, 50);
		set(level, bb, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.EAST, true), 22, F + 5, 50);
		// The core: the altar on the pit floor, and a conduit hanging high over it.
		altar(level, bb, DungeonAltarBlock.Kind.TIDE, CX, PIT + 1, ARENA_Z);
		// Dry: a conduit is waterlogged by default, and would pour onto the altar before the tide ever rose.
		set(level, bb, Blocks.CONDUIT.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, false),
			CX, F + 5, ARENA_Z);
	}

	/** The pit's floor: dark prismarine, two rings of bricks, a ring of sea lanterns round the core. */
	private BlockState pitFloor(int x, int z, double d) {
		if (d < 1.6) {
			return BRICKS;
		}
		if (Math.abs(d - 3) < 0.5) {
			return LIGHT;
		}
		if (Math.abs(d - 6) < 0.5 || Math.abs(d - 10.5) < 0.5) {
			return BRICKS;
		}
		return noise(x, z) < 15 ? PRISMARINE : DARK;
	}

	// ------------------------------------------------------------------ the Vault

	private void vault(WorldGenLevel level, BoundingBox bb) {
		room(level, bb, 36, F, 57, 41, F + 7, 69, BRICKS);
		fill(level, bb, 37, F, 58, 40, F, 68, DARK);
		fill(level, bb, 33, F + 1, 61, 36, F + 4, 65, AIR);
		sealDoorAlong(level, bb, 36, 61, 65, F + 1, F + 4, RuneSealBlock.Element.STORM, RuneSealBlock.Element.ARCANE);
		chest(level, bb, 39, F + 1, 59, DungeonWorldgen.TIDE_VAULT, Direction.WEST, 22);
		chest(level, bb, 39, F + 1, 67, DungeonWorldgen.TIDE_VAULT, Direction.WEST, 23);
		set(level, bb, BRICKS, 39, F + 1, 63);
		set(level, bb, LIGHT, 39, F + 2, 63);
		set(level, bb, LIGHT, 38, F + 7, 60);
		set(level, bb, LIGHT, 38, F + 7, 66);
		// Dark pilasters in the far corners and either side of the plinth, under a corbelled cornice.
		for (int z : new int[] {58, 62, 64, 68}) {
			fill(level, bb, 40, F + 1, z, 40, F + 6, z, DARK);
		}
		for (int z = 58; z <= 68; z++) {
			if (z != 58 && z != 62 && z != 64 && z != 68) {
				set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.EAST, true), 40, F + 6, z);
			}
		}
		for (int x = 37; x <= 39; x++) {
			set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.SOUTH, true), x, F + 6, 58);
			set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.NORTH, true), x, F + 6, 68);
		}
		set(level, bb, DARK, 39, F + 3, 63);
		set(level, bb, LIGHT, 40, F + 4, 63);
	}

	// ------------------------------------------------------------------ guards

	private void guards(WorldGenLevel level, BoundingBox bb) {
		guard(level, bb, EntityTypes.DROWNED, 11, F + 1, 30, List.of(Runes.BOLT, Runes.SHOCK), false);
		guard(level, bb, EntityTypes.DROWNED, 29, F + 1, 34, List.of(Runes.WAVE, Runes.FROST), false);
		guard(level, bb, EntityTypes.WITCH, 20, F + 1, 30, List.of(Runes.BOLT, Runes.BUBBLE), false);
		guard(level, bb, EntityTypes.DROWNED, 20, F + 1, 38, List.of(Runes.RING, Runes.SHOCK), true);
	}
}
