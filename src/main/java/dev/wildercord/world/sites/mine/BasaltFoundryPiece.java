package dev.wildercord.world.sites.mine;

import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * A basalt foundry in the deltas: a blackstone hall on basalt posts, two lava troughs sunk in its floor, crucibles, a line of
 * blast furnaces and a smoking chimney, held by a Runebound wither skeleton with a blaze and a magma cube. The troughs are
 * closed on every side, so their lava never spreads. Local y 0 is the floor.
 */
public final class BasaltFoundryPiece extends MinePiece {
	static final int WIDTH = 19, HEIGHT = 18, DEPTH = 21;

	public BasaltFoundryPiece(int x, int y, int z, Direction facing) {
		super(MineSites.BASALT_FOUNDRY, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public BasaltFoundryPiece(CompoundTag tag) {
		super(MineSites.BASALT_FOUNDRY, tag);
	}

	/** Stands on the deltas' own floor: the highest dry ground with room above it, below the Nether's roof. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		var chunk = c.chunkPos();
		var piece = new BasaltFoundryPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		BlockPos middle = piece.getWorldPos(9, 0, 10), entrance = piece.getWorldPos(9, 0, 0);
		var column = c.chunkGenerator().getBaseColumn(middle.getX(), middle.getZ(), c.heightAccessor(), c.randomState());
		for (int y = 96; y >= 33; y--) {
			BlockState ground = column.getBlock(y);
			if (ground.isAir() || !ground.getFluidState().isEmpty()) continue;
			boolean room = true;
			for (int up = 1; up <= 6 && room; up++) room = column.getBlock(y + up).isAir();
			if (!room) continue;
			piece.move(0, y, 0);
			return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(), y + 1, entrance.getZ()), b -> b.addPiece(piece)));
		}
		return Optional.empty();
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk,
			BlockPos reference) {
		BlockState black = s(Blocks.BLACKSTONE), bricks = s(Blocks.POLISHED_BLACKSTONE_BRICKS), basalt = s(Blocks.POLISHED_BASALT);
		BlockState chiseled = s(Blocks.CHISELED_POLISHED_BLACKSTONE), slab = s(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
		Palette wall = Palette.of(Blocks.BLACKSTONE, 6, Blocks.POLISHED_BLACKSTONE_BRICKS, 3, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, 2, Blocks.GILDED_BLACKSTONE, 1);
		Palette course = Palette.of(Blocks.POLISHED_BLACKSTONE_BRICKS, 5, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, 2);
		Palette paving = Palette.of(Blocks.POLISHED_BLACKSTONE_BRICKS, 6, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, 2, Blocks.POLISHED_BLACKSTONE, 2);
		fill(level, bb, 0, 1, 0, 18, 17, 20, AIR);
		footing(level, bb, 0, 0, 18, 20, 0, s(Blocks.BASALT));
		// The hall: worn blackstone walls over a brick plinth, basalt posts at the corners and bays.
		room(level, bb, 1, 0, 1, 17, 9, 19, black);
		walls(level, bb, 1, 2, 1, 17, 8, 19, wall);
		walls(level, bb, 1, 0, 1, 17, 1, 19, course);
		fill(level, bb, 1, 0, 1, 17, 0, 19, paving);
		for (int x : new int[] {1, 17}) for (int z : new int[] {1, 7, 13, 19}) {
			fill(level, bb, x, 0, z, x, 10, z, basalt);
			set(level, bb, chiseled, x, 11, z);
		}
		// Basalt spurs against the long walls, sloped back at the top.
		for (int z : new int[] {1, 7, 13, 19}) {
			fill(level, bb, 0, 0, z, 0, 4, z, basalt);
			fill(level, bb, 18, 0, z, 18, 4, z, basalt);
			set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, false), 0, 5, z);
			set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, false), 18, 5, z);
		}
		// Barred windows on stair sills, under stair hoods.
		for (int z = 4; z <= 16; z += 6) {
			fill(level, bb, 1, 4, z, 1, 6, z + 1, s(Blocks.IRON_BARS));
			fill(level, bb, 17, 4, z, 17, 6, z + 1, s(Blocks.IRON_BARS));
			for (int w = z; w <= z + 1; w++) {
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, true), 0, 3, w);
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true), 18, 3, w);
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, true), 0, 7, w);
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true), 18, 7, w);
			}
		}
		// A corbelled parapet round the roof, crenellated.
		for (int z = 2; z <= 18; z++) {
			if ((z - 1) % 6 == 0) continue;
			set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, true), 0, 9, z);
			set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true), 18, 9, z);
		}
		for (int x = 2; x <= 16; x++) {
			set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH, true), x, 9, 0);
			set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.SOUTH, true), x, 9, 20);
		}
		walls(level, bb, 1, 10, 1, 17, 10, 19, course);
		crenellate(level, bb, 1, 1, 17, 19, 11, bricks);
		// The door: basalt piers, a gilded lintel under a chiseled keystone, a corbelled arch and a broad step.
		fill(level, bb, 7, 1, 1, 11, 5, 1, AIR);
		fill(level, bb, 6, 6, 1, 12, 6, 1, s(Blocks.GILDED_BLACKSTONE));
		for (int x : new int[] {6, 12}) {
			fill(level, bb, x, 0, 0, x, 6, 0, basalt);
			set(level, bb, s(Blocks.SOUL_LANTERN), x, 8, 0);
		}
		fill(level, bb, 6, 7, 0, 12, 7, 0, bricks);
		set(level, bb, chiseled, 9, 7, 0);
		for (int x = 7; x <= 11; x++) set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH, true), x, 6, 0);
		set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true), 7, 5, 1);
		set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, true), 11, 5, 1);
		fill(level, bb, 7, 0, 0, 11, 0, 0, slab);
		hangingLantern(level, bb, 9, 5, 0, 0, true);
		// The casting-floor lantern: a raised clerestory over the middle of the hall, barred all round, under a hipped roof.
		walls(level, bb, 6, 9, 4, 12, 12, 12, course);
		fill(level, bb, 6, 12, 4, 12, 12, 12, bricks);
		fill(level, bb, 7, 9, 5, 11, 11, 11, AIR);
		for (int x = 7; x <= 11; x += 2) {
			fill(level, bb, x, 10, 4, x, 11, 4, s(Blocks.IRON_BARS));
			fill(level, bb, x, 10, 12, x, 11, 12, s(Blocks.IRON_BARS));
		}
		for (int z = 6; z <= 10; z += 2) {
			fill(level, bb, 6, 10, z, 6, 11, z, s(Blocks.IRON_BARS));
			fill(level, bb, 12, 10, z, 12, 11, z, s(Blocks.IRON_BARS));
		}
		hipRoof(level, bb, 6, 4, 12, 12, 13, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, bricks, chiseled);
		hangingLantern(level, bb, 9, 11, 8, 2, false);
		// The chimney over the back corner, standing proud of the parapet and smoking.
		room(level, bb, 12, 9, 14, 16, 13, 18, bricks);
		for (int x = 12; x <= 16; x++) for (int z = 14; z <= 18; z++) {
			boolean rim = x == 12 || x == 16 || z == 14 || z == 18;
			if (rim) fill(level, bb, x, 14, z, x, 16, z, course);
			else if (x != 14 || z != 16) fill(level, bb, x, 14, z, x, 16, z, bricks);
			if (rim) set(level, bb, slab, x, 17, z);
		}
		set(level, bb, s(Blocks.CAMPFIRE), 14, 12, 16);
		set(level, bb, AIR, 14, 13, 16);
		// Two lava troughs: brick below and on every side, so the lava stays put.
		for (int x : new int[] {4, 14}) {
			fill(level, bb, x - 1, -1, 4, x + 1, -1, 14, bricks);
			fill(level, bb, x - 1, 0, 4, x + 1, 0, 4, chiseled);
			fill(level, bb, x - 1, 0, 14, x + 1, 0, 14, chiseled);
			fill(level, bb, x, 0, 5, x, 0, 13, s(Blocks.LAVA));
			fill(level, bb, x - 1, 1, 5, x - 1, 1, 13, s(Blocks.POLISHED_BLACKSTONE_BRICK_WALL));
			fill(level, bb, x + 1, 1, 5, x + 1, 1, 13, s(Blocks.POLISHED_BLACKSTONE_BRICK_WALL));
		}
		set(level, bb, s(Blocks.LAVA_CAULDRON), 7, 1, 9);
		set(level, bb, s(Blocks.LAVA_CAULDRON), 11, 1, 9);
		set(level, bb, s(Blocks.CAULDRON), 9, 1, 7);
		for (int x = 4; x <= 14; x += 2) if (x != 10) set(level, bb, s(Blocks.BLAST_FURNACE).setValue(BlastFurnaceBlock.FACING, Direction.SOUTH), x, 1, 18);
		// Brick piers between the blast furnaces, a slab shelf over the line, coal and ore stock.
		for (int x = 5; x <= 13; x += 2) {
			set(level, bb, bricks, x, 1, 18);
			set(level, bb, slab, x, 2, 18);
		}
		set(level, bb, s(Blocks.COAL_BLOCK), 3, 1, 18);
		set(level, bb, s(Blocks.MAGMA_BLOCK), 15, 1, 18);
		barrel(level, bb, 16, 1, 18, null, 0);
		barrel(level, bb, 16, 1, 17, null, 0);
		set(level, bb, s(Blocks.RAW_GOLD_BLOCK), 16, 2, 18);
		chest(level, bb, 10, 1, 18, MineSites.FOUNDRY, Direction.SOUTH, 451);
		chest(level, bb, 2, 1, 16, MineSites.FOUNDRY, Direction.EAST, 452);
		set(level, bb, s(Blocks.ANVIL), 9, 1, 14);
		for (int x : new int[] {6, 12}) for (int z : new int[] {6, 13}) hangingLantern(level, bb, x, 8, z);
		// Chains hang from the ceiling over the troughs, where the crucibles were swung.
		for (int x : new int[] {4, 14}) for (int z : new int[] {6, 12}) fill(level, bb, x, 6, z, x, 8, z, s(Blocks.IRON_CHAIN));
		guard(level, bb, EntityTypes.WITHER_SKELETON, 9, 1, 12, List.of(Runes.BOLT, Runes.EMBER), true);
		sentry(level, bb, EntityTypes.BLAZE, 12, 2, 4);
		sentry(level, bb, EntityTypes.MAGMA_CUBE, 6, 1, 15);
	}
}
