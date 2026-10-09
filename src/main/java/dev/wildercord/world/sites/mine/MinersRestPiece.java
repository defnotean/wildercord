package dev.wildercord.world.sites.mine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * A miner's rest on a cliff edge: a small stone shrine with a bell, a candle altar and a bench, and a plank balcony over the
 * drop with a lantern hung below it on a chain, kept lit for those still underground. Peaceful. It only stands where the ground
 * falls away at least 8 blocks past the shrine. Local y 1 is the floor.
 */
public final class MinersRestPiece extends MinePiece {
	static final int WIDTH = 11, HEIGHT = 12, DEPTH = 17, DROP = 8;

	public MinersRestPiece(int x, int y, int z, Direction facing) {
		super(MineSites.MINERS_REST, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public MinersRestPiece(CompoundTag tag) {
		super(MineSites.MINERS_REST, tag);
	}

	/** Turns until the shrine's ground is level and the ground past the balcony is a long way down. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		var chunk = c.chunkPos();
		Direction first = Direction.Plane.HORIZONTAL.getRandomDirection(c.random());
		for (int turn = 0; turn < 4; turn++) {
			Direction facing = first;
			for (int i = 0; i < turn; i++) facing = facing.getClockWise();
			var piece = new MinersRestPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), facing);
			BlockPos entrance = piece.getWorldPos(5, 0, 0), shrine = piece.getWorldPos(5, 0, 4);
			if (wet(c, entrance) || wet(c, shrine)) continue;
			int y = ground(c, shrine);
			boolean level = true;
			for (BlockPos at : new BlockPos[] {piece.getWorldPos(2, 0, 1), piece.getWorldPos(8, 0, 1), piece.getWorldPos(2, 0, 7), piece.getWorldPos(8, 0, 7)}) {
				level &= Math.abs(ground(c, at) - y) <= 3;
			}
			int below = Math.max(ground(c, piece.getWorldPos(5, 0, 14)), ground(c, piece.getWorldPos(5, 0, 18)));
			if (!level || y - below < DROP) continue;
			piece.move(0, y - 1, 0);
			return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(), y, entrance.getZ()), b -> b.addPiece(piece)));
		}
		return Optional.empty();
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk,
			BlockPos reference) {
		BlockState brick = s(Blocks.STONE_BRICKS), plank = s(Blocks.SPRUCE_PLANKS), fence = s(Blocks.SPRUCE_FENCE);
		fill(level, bb, 1, 2, 0, 9, 11, 8, AIR);
		// The shrine: a brick floor on footings, four pillars, low walls and a slab roof.
		fill(level, bb, 2, 1, 1, 8, 1, 7, brick);
		footing(level, bb, 2, 1, 8, 7, 1, s(Blocks.COBBLESTONE));
		for (int x : new int[] {2, 8}) for (int z : new int[] {1, 7}) fill(level, bb, x, 2, z, x, 5, z, s(Blocks.CHISELED_STONE_BRICKS));
		fill(level, bb, 2, 2, 2, 2, 2, 6, s(Blocks.STONE_BRICK_WALL));
		fill(level, bb, 8, 2, 2, 8, 2, 6, s(Blocks.STONE_BRICK_WALL));
		fill(level, bb, 3, 2, 7, 7, 2, 7, s(Blocks.STONE_BRICK_WALL));
		fill(level, bb, 5, 2, 7, 5, 2, 7, AIR);
		fill(level, bb, 1, 6, 0, 9, 6, 8, s(Blocks.STONE_BRICK_SLAB));
		fill(level, bb, 3, 7, 2, 7, 7, 6, s(Blocks.STONE_BRICK_SLAB));
		set(level, bb, s(Blocks.BELL).setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING), 5, 5, 3);
		set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), 7, 2, 5);
		set(level, bb, s(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true), 7, 3, 5);
		set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), 3, 2, 5);
		set(level, bb, s(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true), 3, 3, 5);
		chest(level, bb, 3, 2, 3, MineSites.REST, Direction.EAST, 481);
		fill(level, bb, 7, 2, 2, 7, 2, 3, s(Blocks.SPRUCE_STAIRS).setValue(StairBlock.FACING, Direction.EAST));
		hangingLantern(level, bb, 5, 5, 5);
		// The balcony over the drop, braced at the cliff edge, its lamp hung below.
		fill(level, bb, 2, 2, 9, 8, 6, 16, AIR);
		fill(level, bb, 3, 1, 8, 7, 1, 15, plank);
		for (int z = 8; z <= 15; z++) {
			set(level, bb, fence, 3, 2, z);
			set(level, bb, fence, 7, 2, z);
		}
		fill(level, bb, 4, 2, 15, 6, 2, 15, fence);
		for (int x : new int[] {3, 7}) footing(level, bb, x, 8, x, 9, 1, s(Blocks.SPRUCE_LOG));
		set(level, bb, s(Blocks.SPRUCE_STAIRS).setValue(StairBlock.FACING, Direction.SOUTH).setValue(StairBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP), 5, 0, 10);
		fill(level, bb, 5, -2, 15, 5, 0, 15, s(Blocks.IRON_CHAIN));
		hangingLantern(level, bb, 5, -3, 15);
		set(level, bb, s(Blocks.LANTERN), 3, 3, 15);
		set(level, bb, s(Blocks.LANTERN), 7, 3, 15);
	}
}
