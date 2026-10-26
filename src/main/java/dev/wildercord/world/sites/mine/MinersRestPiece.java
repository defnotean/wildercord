package dev.wildercord.world.sites.mine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
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
	static final int WIDTH = 11, HEIGHT = 21, DEPTH = 17, DROP = 8;

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
		BlockState chiseled = s(Blocks.CHISELED_STONE_BRICKS), polished = s(Blocks.POLISHED_ANDESITE);
		Palette floor = Palette.of(Blocks.STONE_BRICKS, 7, Blocks.MOSSY_STONE_BRICKS, 2, Blocks.CRACKED_STONE_BRICKS, 1);
		fill(level, bb, 1, 2, 0, 9, 20, 8, AIR);
		// The shrine: a worn brick floor on footings with an andesite aisle, and broad steps up from the path.
		fill(level, bb, 2, 1, 1, 8, 1, 7, floor);
		fill(level, bb, 5, 1, 1, 5, 1, 7, polished);
		set(level, bb, chiseled, 5, 1, 3);
		footing(level, bb, 2, 1, 8, 7, 1, s(Blocks.COBBLESTONE));
		for (int x = 4; x <= 6; x++) set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false), x, 1, 0);
		footing(level, bb, 4, 0, 6, 0, 1, s(Blocks.COBBLESTONE));
		// Four pillars on chiseled bases, corbelled out under the eaves.
		for (int x : new int[] {2, 8}) for (int z : new int[] {1, 7}) {
			pillar(level, bb, x, z, 2, 5, chiseled, brick, chiseled);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, x == 2 ? Direction.EAST : Direction.WEST, true), x == 2 ? 1 : 9, 5, z);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, z == 1 ? Direction.NORTH : Direction.SOUTH, true), x, 5, z == 1 ? 0 : 8);
		}
		// Low walls between the pillars, an architrave over them and shallow arches springing from each pillar.
		fill(level, bb, 2, 2, 2, 2, 2, 6, s(Blocks.STONE_BRICK_WALL));
		fill(level, bb, 8, 2, 2, 8, 2, 6, s(Blocks.STONE_BRICK_WALL));
		fill(level, bb, 3, 2, 7, 7, 2, 7, s(Blocks.STONE_BRICK_WALL));
		fill(level, bb, 5, 2, 7, 5, 2, 7, AIR);
		fill(level, bb, 2, 5, 2, 2, 5, 6, brick);
		fill(level, bb, 8, 5, 2, 8, 5, 6, brick);
		fill(level, bb, 3, 5, 1, 7, 5, 1, brick);
		fill(level, bb, 3, 5, 7, 7, 5, 7, brick);
		set(level, bb, chiseled, 5, 5, 1);
		for (int x : new int[] {2, 8}) {
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true), x, 4, 2);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true), x, 4, 6);
		}
		for (int z : new int[] {1, 7}) {
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 3, 4, z);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 7, 4, z);
		}
		// A plank ceiling with log beams, under a hipped roof of brick stairs crowned by a lamp.
		fill(level, bb, 2, 6, 1, 8, 6, 7, plank);
		for (int z : new int[] {2, 6}) fill(level, bb, 3, 6, z, 7, 6, z, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		hipRoof(level, bb, 2, 1, 8, 7, 6, Blocks.STONE_BRICK_STAIRS, brick, chiseled);
		// A belfry rising out of the roof: four posts round the great bell, arched on every face, under a needle spire.
		fill(level, bb, 4, 9, 3, 6, 9, 5, brick);
		fill(level, bb, 4, 10, 3, 6, 12, 5, AIR);
		for (int x : new int[] {4, 6}) for (int z : new int[] {3, 5}) pillar(level, bb, x, z, 10, 12, chiseled, brick, brick);
		set(level, bb, s(Blocks.STONE_BRICK_WALL), 5, 10, 3);
		set(level, bb, s(Blocks.STONE_BRICK_WALL), 5, 10, 5);
		set(level, bb, s(Blocks.STONE_BRICK_WALL), 4, 10, 4);
		set(level, bb, s(Blocks.STONE_BRICK_WALL), 6, 10, 4);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true), 5, 12, 3);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true), 5, 12, 5);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 4, 12, 4);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 6, 12, 4);
		fill(level, bb, 4, 13, 3, 6, 13, 5, chiseled);
		set(level, bb, s(Blocks.BELL).setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING), 5, 12, 4);
		spire(level, bb, 5, 4, 2, 14, Blocks.STONE_BRICK_STAIRS, brick, chiseled, s(Blocks.LIGHTNING_ROD));
		// Lamps hung at the roof's four corners and banners on the front pillars.
		for (int x : new int[] {1, 9}) for (int z : new int[] {0, 8}) hangingLantern(level, bb, x, 5, z, 0, false);
		for (int x : new int[] {2, 8}) banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.ORANGE), x, 4, 0, Direction.NORTH);
		// The bell, the candle altars, the bench and the offering chest.
		set(level, bb, s(Blocks.BELL).setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING), 5, 5, 3);
		set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), 7, 2, 5);
		set(level, bb, s(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true), 7, 3, 5);
		set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), 3, 2, 5);
		set(level, bb, s(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true), 3, 3, 5);
		chest(level, bb, 3, 2, 3, MineSites.REST, Direction.EAST, 481);
		fill(level, bb, 7, 2, 2, 7, 2, 3, s(Blocks.SPRUCE_STAIRS).setValue(StairBlock.FACING, Direction.EAST));
		hangingLantern(level, bb, 5, 5, 5);
		// Ferns potted on the wall tops, and lamps on posts either side of the steps.
		set(level, bb, s(Blocks.POTTED_FERN), 2, 3, 4);
		set(level, bb, s(Blocks.POTTED_FERN), 8, 3, 4);
		for (int x : new int[] {1, 9}) {
			footing(level, bb, x, 0, x, 0, 1, s(Blocks.COBBLESTONE));
			set(level, bb, s(Blocks.COBBLESTONE_WALL), x, 1, 0);
			set(level, bb, fence, x, 2, 0);
			set(level, bb, s(Blocks.LANTERN), x, 3, 0);
		}
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
		// Log joists under the balcony's edges, braced back to the cliff.
		for (int x : new int[] {3, 7}) {
			fill(level, bb, x, 0, 10, x, 0, 15, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, true), x, -1, 10);
		}
		fill(level, bb, 5, -2, 15, 5, 0, 15, s(Blocks.IRON_CHAIN));
		hangingLantern(level, bb, 5, -3, 15);
		set(level, bb, s(Blocks.LANTERN), 3, 3, 15);
		set(level, bb, s(Blocks.LANTERN), 7, 3, 15);
		// Log posts at the balcony's entry carrying a little lintel, so it reads as a gate out over the drop.
		for (int x : new int[] {3, 7}) fill(level, bb, x, 2, 9, x, 4, 9, s(Blocks.SPRUCE_LOG));
		fill(level, bb, 3, 5, 9, 7, 5, 9, s(Blocks.SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 4, 4, 9);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, true), 6, 4, 9);
		hangingLantern(level, bb, 5, 4, 9);
	}
}
