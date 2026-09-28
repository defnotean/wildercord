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
		approach(level, bb);
		foundation(level, bb, 12, 14, 28, 24, F - 1);
		foundation(level, bb, 8, 24, 32, 42, F - 1);
		foundation(level, bb, 15, 42, 25, 48, F - 1);
		foundation(level, bb, 36, 57, 41, 69, F - 1);
		antechamber(level, bb);
		tunnel(level, bb);
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
		// The moon pool: up through the roof and the antechamber's floor. The water stops at the floor.
		fill(level, bb, 19, 7, 16, 21, 8, 18, WATER);
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
		for (int x = 11; x <= 29; x += 6) {
			for (int z = 27; z <= 39; z += 6) {
				set(level, bb, LIGHT, x, F + 9, z);
			}
		}
		for (int z : new int[] {30, 36}) {
			fill(level, bb, 20, F + 7, z, 20, F + 8, z, Blocks.IRON_CHAIN.defaultBlockState());
			set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 20, F + 6, z);
		}
		chest(level, bb, 30, F + 1, 40, DungeonWorldgen.TIDE_HALL, Direction.WEST, 21);
	}

	// ------------------------------------------------------------------ the corridor

	private void corridor(WorldGenLevel level, BoundingBox bb) {
		room(level, bb, 15, F, 42, 25, F + 7, 48, BRICKS);
		fill(level, bb, 16, F, 43, 24, F, 47, DARK);
		set(level, bb, LIGHT, 16, F + 3, 45);
		set(level, bb, LIGHT, 24, F + 3, 45);
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
				// The dome: air under the bowl, and a shell of glass wherever air would touch the sea.
				for (int y = F + 7; y < HEIGHT; y++) {
					if (domeAir(x, y, z)) {
						set(level, bb, AIR, x, y, z);
					} else if (domeAir(x + 1, y, z) || domeAir(x - 1, y, z) || domeAir(x, y, z + 1) || domeAir(x, y, z - 1)
							|| domeAir(x, y - 1, z) || domeAir(x, y + 1, z)) {
						set(level, bb, GLASS, x, y, z);
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
		// Steps down from the balcony, and the way in.
		fill(level, bb, 19, PIT + 1, 51, 21, PIT + 2, 51, BRICKS);
		fill(level, bb, 19, PIT + 1, 52, 21, PIT + 1, 52, BRICKS);
		fill(level, bb, 18, F, 47, 22, F, 50, DARK);
		fill(level, bb, 18, F + 1, 47, 22, F + 5, 50, AIR);
		// The core: the altar on the pit floor, and a conduit hanging high over it.
		altar(level, bb, DungeonAltarBlock.Kind.TIDE, CX, PIT + 1, ARENA_Z);
		set(level, bb, Blocks.CONDUIT.defaultBlockState(), CX, F + 5, ARENA_Z);
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
	}

	// ------------------------------------------------------------------ guards

	private void guards(WorldGenLevel level, BoundingBox bb) {
		guard(level, bb, EntityTypes.DROWNED, 11, F + 1, 30, List.of(Runes.BOLT, Runes.SHOCK), false);
		guard(level, bb, EntityTypes.DROWNED, 29, F + 1, 34, List.of(Runes.WAVE, Runes.FROST), false);
		guard(level, bb, EntityTypes.WITCH, 20, F + 1, 30, List.of(Runes.BOLT, Runes.BUBBLE), false);
		guard(level, bb, EntityTypes.DROWNED, 20, F + 1, 38, List.of(Runes.RING, Runes.SHOCK), true);
	}
}
