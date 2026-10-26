package dev.wildercord.world.sites.water;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * A lighthouse on the shore, banded white and red. The keeper is gone; their log lies open on the ground floor. Up
 * the ladder, the lamp has gone dark: a lever beside it lights it again. The log's last page says where the keeper
 * kept what mattered: under the cracked stone in the floor, in a cellar.
 *
 * In its own frame the tower stands round x 6, z 6; the ground floor is at y {@link #BASE}, the door at z 3.
 */
public class LighthousePiece extends WaterPiece {
	public static final int WIDTH = 13, HEIGHT = 39, DEPTH = 13;
	public static final int BASE = 6;
	/** The lamp room's floor (the gallery): the lamp stands one over it. */
	public static final int GALLERY = BASE + 22;
	static final int CX = 6, CZ = 6;

	private static final BlockState BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
	private static final BlockState WHITE = Blocks.CONCRETE.white().defaultBlockState();
	private static final BlockState RED = Blocks.CONCRETE.red().defaultBlockState();
	private static final BlockState ROOF = Blocks.DEEPSLATE_TILES.defaultBlockState();
	private static final Palette PLINTH = Palette.of(Blocks.STONE_BRICKS, 6, Blocks.MOSSY_STONE_BRICKS, 3, Blocks.CRACKED_STONE_BRICKS, 1);

	public LighthousePiece(int x, int y, int z, Direction facing) {
		super(WaterSites.LIGHTHOUSE, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public LighthousePiece(CompoundTag tag) {
		super(WaterSites.LIGHTHOUSE, tag);
	}

	/** Dry, fairly level ground just over the sea, with open water somewhere close by. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		ChunkPos chunk = c.chunkPos();
		LighthousePiece piece = new LighthousePiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		int sea = c.chunkGenerator().getSeaLevel();
		BlockPos mid = piece.getWorldPos(CX, 0, CZ);
		if (!land(c, mid, sea - 1, sea + 5)) {
			return Optional.empty();
		}
		int ground = surface(c, mid) - 1;
		for (int[] at : new int[][] {{1, 1}, {11, 1}, {1, 11}, {11, 11}}) {
			int h = surface(c, piece.getWorldPos(at[0], 0, at[1])) - 1;
			if (Math.abs(h - ground) > 3) {
				return Optional.empty();
			}
		}
		boolean shore = false;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			shore |= water(c, mid.relative(d, 14), 1, 64);
		}
		if (!shore) {
			return Optional.empty();
		}
		piece.move(0, ground - BASE, 0);
		return stub(piece, new BlockPos(mid.getX(), ground, mid.getZ()));
	}

	private static double r(int x, int z) {
		return dist(x, z, CX, CZ);
	}

	/** The way in toward the tower's axis from (x, z), for stairs that lean on the round wall. */
	private static Direction towardShaft(int x, int z) {
		int dx = CX - x, dz = CZ - z;
		if (Math.abs(dx) >= Math.abs(dz)) {
			return dx > 0 ? Direction.EAST : Direction.WEST;
		}
		return dz > 0 ? Direction.NORTH : Direction.SOUTH;
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos reference) {
		for (int x = 0; x < WIDTH; x++) {
			for (int z = 0; z < DEPTH; z++) {
				double d = r(x, z);
				if (d > 5.5) {
					continue;
				}
				// A stone footing out to the apron's edge, the ground over it cleared.
				post(level, bb, d > 3.4 ? Blocks.COBBLESTONE.defaultBlockState() : BRICKS, x, z, BASE);
				clear(level, bb, x, BASE + 1, z, x, BASE + 4, z);
				if (d <= 3.4) {
					tower(level, bb, x, z, d);
				}
			}
		}
		cellar(level, bb);
		yard(level, bb);
		groundFloor(level, bb);
		lamp(level, bb);
	}

	/** One column of the tower: banded wall or open shaft, the cellar under it, the lamp room and the roof over it. */
	private void tower(WorldGenLevel level, BoundingBox bb, int x, int z, double d) {
		boolean wall = d > 2.4;
		for (int y = BASE - 4; y < BASE; y++) {
			set(level, bb, wall || y == BASE - 4 ? BRICKS : AIR, x, y, z);
		}
		// A weathered stone plinth two courses high, then the painted bands.
		for (int y = BASE + 1; y < GALLERY; y++) {
			BlockState band = y <= BASE + 2 ? PLINTH.at(x, y, z) : (y - BASE - 3) / 4 % 2 == 0 ? WHITE : RED;
			set(level, bb, wall ? band : AIR, x, y, z);
		}
		// The cap: a slate cone in four rings, each edge laid in stairs that climb toward the lightning rod.
		set(level, bb, ROOF, x, GALLERY + 5, z);
		double[] rings = {2.4, 1.5, 0.5};
		for (int i = 0; i < rings.length; i++) {
			double outer = i == 0 ? 3.4 : rings[i - 1];
			if (d <= rings[i]) {
				set(level, bb, ROOF, x, GALLERY + 6 + i, z);
			} else if (d <= outer) {
				set(level, bb, stair(Blocks.DEEPSLATE_TILE_STAIRS, towardShaft(x, z), false), x, GALLERY + 6 + i, z);
			}
		}
	}

	private void cellar(WorldGenLevel level, BoundingBox bb) {
		// Its way down: the cracked stone in the ground floor, a ladder under it.
		set(level, bb, Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 4, BASE, 6);
		for (int y = BASE - 3; y < BASE; y++) {
			set(level, bb, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST), 4, y, 6);
		}
		chest(level, bb, 8, BASE - 3, 6, WaterSites.LIGHTHOUSE_LOOT, Direction.WEST, 302);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 7, BASE - 3, 8);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 7, BASE - 3, 4);
	}

	/** A kerbed apron with lamp posts, a paved path to the door and a porch of dark piers under a stone hood. */
	private void yard(WorldGenLevel level, BoundingBox bb) {
		for (int x = 0; x < WIDTH; x++) {
			for (int z = 0; z < DEPTH; z++) {
				double d = r(x, z);
				boolean front = Math.abs(x - CX) <= 1 && z < CZ;
				if (d > 4.6 && d <= 5.5 && !front) {
					set(level, bb, noise(x, z) < 35 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState(), x, BASE + 1, z);
				}
			}
		}
		for (int[] c : new int[][] {{3, 2}, {9, 2}, {3, 10}, {9, 10}}) {
			fill(level, bb, c[0], BASE + 2, c[1], c[0], BASE + 3, c[1], Blocks.SPRUCE_FENCE.defaultBlockState());
			set(level, bb, Blocks.LANTERN.defaultBlockState(), c[0], BASE + 4, c[1]);
		}
		for (int z = 0; z <= 2; z++) {
			post(level, bb, PLINTH.at(CX, BASE, z), CX, z, BASE);
			clear(level, bb, CX, BASE + 1, z, CX, BASE + 3, z);
		}
		BlockState pier = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
		for (int x : new int[] {CX - 1, CX + 1}) {
			fill(level, bb, x, BASE + 1, 2, x, BASE + 3, 2, pier);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), x, BASE + 4, 2);
			banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.BLUE), x, BASE + 7, 2, Direction.SOUTH);
		}
		set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), CX, BASE + 3, 2);
		// Rope coils and floats left by the door.
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 10, BASE + 1, 4);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.EAST), 10, BASE + 1, 5);
		set(level, bb, Blocks.FLOWER_POT.defaultBlockState(), 10, BASE + 2, 4);
		set(level, bb, Blocks.HAY_BLOCK.defaultBlockState(), 2, BASE + 1, 5);
	}

	private void groundFloor(WorldGenLevel level, BoundingBox bb) {
		// The door, and steps down off the apron.
		fill(level, bb, CX, BASE + 1, 3, CX, BASE + 2, 3, AIR);
		// The ladder up the back wall, through the gallery floor.
		for (int y = BASE + 1; y <= GALLERY; y++) {
			set(level, bb, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH), CX, y, 8);
		}
		lectern(level, bb, 8, BASE + 1, 5, Direction.WEST, "Keeper's Log", "book.wildercord.water_lighthouse", 4);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 5, BASE + 1, 8);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 5, BASE + 2, 8);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 5, BASE + 1, 4);
		// A rug, a pot on a barrel and wall torches up the shaft so the climb is never dark.
		fill(level, bb, 5, BASE + 1, 5, 7, BASE + 1, 6, Blocks.CARPET.blue().defaultBlockState());
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 7, BASE + 1, 8);
		set(level, bb, Blocks.POTTED_ALLIUM.defaultBlockState(), 7, BASE + 2, 8);
		for (int y : new int[] {BASE + 4, BASE + 16}) {
			sconce(level, bb, 4, y, CZ, Direction.EAST, false);
		}
		for (int y : new int[] {BASE + 10, BASE + 20}) {
			sconce(level, bb, 8, y, CZ, Direction.WEST, false);
		}
		// Windows up both flanks and the seaward face, each with a sill under it and a hood over it.
		for (int y = BASE + 7; y < GALLERY; y += 6) {
			set(level, bb, Blocks.GLASS.defaultBlockState(), 9, y, CZ);
			set(level, bb, Blocks.GLASS.defaultBlockState(), 3, y, CZ);
			for (int dy : new int[] {-1, 1}) {
				set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 10, y + dy, CZ);
				set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 2, y + dy, CZ);
			}
		}
		for (int y : new int[] {BASE + 10, BASE + 16}) {
			set(level, bb, Blocks.GLASS.defaultBlockState(), CX, y, 3);
			for (int dy : new int[] {-1, 1}) {
				set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true), CX, y + dy, 2);
			}
		}
	}

	/** The authored lamp, transformed with the piece's orientation for observation in old and new worlds. */
	public BlockPos lampPosition() {
		return localPosition(CX, GALLERY + 1, CZ);
	}

	/** The gallery, its railing, the glass lamp room and the dark lamp with its lever. */
	private void lamp(WorldGenLevel level, BoundingBox bb) {
		for (int x = 0; x < WIDTH; x++) {
			for (int z = 0; z < DEPTH; z++) {
				double d = r(x, z);
				if (d > 5.4) {
					continue;
				}
				set(level, bb, d > 3.4 ? BRICKS : Blocks.SMOOTH_STONE.defaultBlockState(), x, GALLERY, z);
				for (int y = GALLERY + 1; y <= GALLERY + 4; y++) {
					BlockState s = AIR;
					if (d > 4.4) {
						s = y == GALLERY + 1 ? Blocks.IRON_BARS.defaultBlockState() : AIR;
					} else if (d > 3.4) {
						s = AIR;
					} else if (d > 2.4) {
						s = y == GALLERY + 4 ? BRICKS : Blocks.GLASS.defaultBlockState();
					}
					set(level, bb, s, x, y, z);
				}
			}
		}
		// The gallery rides out on two courses of corbels, and the cap flares into eaves over the walk.
		for (int x = 0; x < WIDTH; x++) {
			for (int z = 0; z < DEPTH; z++) {
				double d = r(x, z);
				if (d <= 3.4 || d > 5.4) {
					continue;
				}
				Direction in = towardShaft(x, z);
				if (d <= 4.4) {
					set(level, bb, BRICKS, x, GALLERY - 1, z);
					set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, in, true), x, GALLERY - 2, z);
					set(level, bb, stair(Blocks.DEEPSLATE_TILE_STAIRS, in, false), x, GALLERY + 5, z);
				} else {
					set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, in, true), x, GALLERY - 1, z);
				}
			}
		}
		for (int[] c : new int[][] {{CX, 1}, {CX, 11}, {1, CZ}, {11, CZ}}) {
			set(level, bb, Blocks.LANTERN.defaultBlockState(), c[0], GALLERY + 2, c[1]);
		}
		// The way out onto the gallery, over the door.
		fill(level, bb, CX, GALLERY + 1, 3, CX, GALLERY + 2, 3, AIR);
		// The ladder comes up through the floor.
		set(level, bb, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH), CX, GALLERY, 8);
		set(level, bb, BRICKS, CX, GALLERY, 9);
		// The lamp: dark until its lever is thrown.
		set(level, bb, Blocks.REDSTONE_LAMP.defaultBlockState(), CX, GALLERY + 1, CZ);
		set(level, bb, Blocks.GLOWSTONE.defaultBlockState(), CX, GALLERY + 4, CZ);
		set(level, bb, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, Direction.SOUTH), CX, GALLERY + 1, 7);
		set(level, bb, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), CX, GALLERY + 9, CZ);
		set(level, bb, Blocks.LIGHTNING_ROD.waxed().unaffected().defaultBlockState(), CX, GALLERY + 10, CZ);
	}
}
