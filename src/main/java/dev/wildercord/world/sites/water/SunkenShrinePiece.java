package dev.wildercord.world.sites.water;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * A shrine to the Tide on the floor of a warm sea. In its court a conduit hangs in a ring of prismarine, alive: swim
 * near and you can breathe. Behind it the shrine's hall is dry, reached the old way: a short tunnel under its front
 * wall and up through a pool in its floor, where water never rises. In its own frame the sea floor is at y
 * {@link #FLOOR}; the court is at z 1-7, the hall at z 8-15.
 */
public class SunkenShrinePiece extends WaterPiece {
	public static final int WIDTH = 17, HEIGHT = 14, DEPTH = 17;
	/** The platform's top, laid on the sea floor. */
	public static final int FLOOR = 2;
	/** The hall's dry floor: you stand at {@code HALL + 1}. */
	public static final int HALL = 5;
	static final int CONDUIT_X = 8, CONDUIT_Y = 5, CONDUIT_Z = 4;
	static final int POOL_X = 8, POOL_Z = 10;

	private static final BlockState BRICKS = Blocks.PRISMARINE_BRICKS.defaultBlockState();
	private static final BlockState DARK = Blocks.DARK_PRISMARINE.defaultBlockState();
	private static final BlockState LIGHT = Blocks.SEA_LANTERN.defaultBlockState();
	private static final BlockState WATER = Blocks.WATER.defaultBlockState();

	public SunkenShrinePiece(int x, int y, int z, Direction facing) {
		super(WaterSites.SUNKEN_SHRINE, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public SunkenShrinePiece(CompoundTag tag) {
		super(WaterSites.SUNKEN_SHRINE, tag);
	}

	/** A sea floor deep enough to cover the whole shrine, and flat enough to lay it on. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		ChunkPos chunk = c.chunkPos();
		SunkenShrinePiece piece = new SunkenShrinePiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		BlockPos mid = piece.getWorldPos(8, 0, 8);
		if (!water(c, mid, HEIGHT, 255)) {
			return Optional.empty();
		}
		int bed = floor(c, mid) - 1;
		for (int[] at : new int[][] {{1, 1}, {15, 1}, {1, 15}, {15, 15}}) {
			BlockPos q = piece.getWorldPos(at[0], 0, at[1]);
			if (!water(c, q, HEIGHT - 3, 255) || Math.abs(floor(c, q) - 1 - bed) > 3) {
				return Optional.empty();
			}
		}
		piece.move(0, bed - FLOOR, 0);
		return stub(piece, new BlockPos(mid.getX(), bed + 1, mid.getZ()));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos reference) {
		// Sea over the platform where the floor rose: water stays water, rock becomes sea.
		for (int x = 0; x < WIDTH; x++) {
			for (int z = 0; z < DEPTH; z++) {
				for (int y = FLOOR + 1; y < HEIGHT; y++) {
					BlockState there = getBlock(level, x, y, z, bb);
					if (!there.isAir() && there.getFluidState().isEmpty()) {
						set(level, bb, WATER, x, y, z);
					}
				}
			}
		}
		for (int x = 1; x <= 15; x++) {
			for (int z = 1; z <= 15; z++) {
				post(level, bb, BRICKS, x, z, FLOOR);
				if (x == 1 || x == 15 || z == 1 || z == 15) {
					set(level, bb, DARK, x, FLOOR, z);
				}
			}
		}
		court(level, bb);
		hall(level, bb);
		sentry(level, bb, EntityTypes.DROWNED, 4, FLOOR + 1, 3);
		sentry(level, bb, EntityTypes.DROWNED, 12, FLOOR + 1, 5);
	}

	/** The court: the conduit in its ring of sixteen, pillars at the corners, coral and sea pickles round it. */
	private void court(WorldGenLevel level, BoundingBox bb) {
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
					set(level, bb, Math.abs(dx) == 2 && Math.abs(dz) == 2 ? LIGHT : BRICKS, CONDUIT_X + dx, CONDUIT_Y, CONDUIT_Z + dz);
				}
			}
		}
		soaked(level, bb, Blocks.CONDUIT.defaultBlockState(), CONDUIT_X, CONDUIT_Y, CONDUIT_Z);
		for (int[] p : new int[][] {{2, 2}, {14, 2}, {2, 7}, {14, 7}}) {
			fill(level, bb, p[0], FLOOR + 1, p[1], p[0], FLOOR + 4, p[1], BRICKS);
			set(level, bb, LIGHT, p[0], FLOOR + 5, p[1]);
		}
		BlockState[] growth = {Blocks.TUBE_CORAL.defaultBlockState(), Blocks.BRAIN_CORAL_FAN.defaultBlockState(), Blocks.HORN_CORAL.defaultBlockState(),
			Blocks.TUBE_CORAL_FAN.defaultBlockState()};
		for (int x = 2; x <= 14; x++) {
			for (int z = 1; z <= 7; z++) {
				int n = noise(x + boundingBox.minX(), z + boundingBox.minZ());
				if (x >= 7 && x <= 9 || n >= 30 || !wetAt(level, bb, x, FLOOR + 1, z)) {
					continue;
				}
				if (n < 8) {
					soaked(level, bb, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, 1 + n % 4), x, FLOOR + 1, z);
				} else {
					soaked(level, bb, growth[n % growth.length], x, FLOOR + 1, z);
				}
			}
		}
	}

	/** The hall: solid prismarine round a dry room, the tunnel under its front wall and the pool up into it. */
	private void hall(WorldGenLevel level, BoundingBox bb) {
		fill(level, bb, 3, FLOOR + 1, 8, 13, HALL + 5, 15, BRICKS);
		for (int y = FLOOR + 1; y <= HALL + 5; y++) {
			for (int x = 3; x <= 13; x++) {
				set(level, bb, DARK, x, y, 8);
				set(level, bb, DARK, x, y, 15);
			}
		}
		fill(level, bb, 4, HALL + 1, 9, 12, HALL + 4, 14, AIR);
		fill(level, bb, 4, HALL, 9, 12, HALL, 14, DARK);
		// The tunnel and the moon pool.
		fill(level, bb, POOL_X, FLOOR + 1, 7, POOL_X, FLOOR + 2, POOL_Z, WATER);
		set(level, bb, WATER, POOL_X, HALL, POOL_Z);
		set(level, bb, LIGHT, POOL_X - 1, HALL, POOL_Z);
		set(level, bb, LIGHT, POOL_X + 1, HALL, POOL_Z);
		set(level, bb, LIGHT, POOL_X, FLOOR + 2, 11);
		// Its roof, stepped, a sea lantern on top for divers to find.
		fill(level, bb, 4, HALL + 6, 9, 12, HALL + 6, 14, Blocks.PRISMARINE_BRICK_SLAB.defaultBlockState());
		fill(level, bb, 6, HALL + 6, 10, 10, HALL + 6, 13, DARK);
		set(level, bb, LIGHT, 8, HALL + 7, 11);
		for (int[] l : new int[][] {{5, 11}, {11, 11}, {8, 13}}) {
			set(level, bb, LIGHT, l[0], HALL + 5, l[1]);
		}
		for (int x : new int[] {6, 10}) {
			set(level, bb, Blocks.GLASS.defaultBlockState(), x, HALL + 2, 8);
		}
		for (int z = 11; z <= 14; z++) {
			set(level, bb, Blocks.CARPET.cyan().defaultBlockState(), 8, HALL + 1, z);
		}
		set(level, bb, Blocks.PRISMARINE_STAIRS.defaultBlockState(), 5, HALL + 1, 9);
		set(level, bb, Blocks.PRISMARINE_STAIRS.defaultBlockState(), 11, HALL + 1, 9);
		lectern(level, bb, 4, HALL + 1, 14, Direction.EAST, "Tide Tablet", "book.wildercord.water_sunken_shrine", 2);
		chest(level, bb, 12, HALL + 1, 14, WaterSites.SHRINE_LOOT, Direction.WEST, 306);
	}
}
