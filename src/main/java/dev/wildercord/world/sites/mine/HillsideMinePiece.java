package dev.wildercord.world.sites.mine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * A mine's mouth in a hillside: a plank yard on posts, a headframe whose chain drops into a pit, a sorting shed with the
 * crew's tally, and a rail line into a framed tunnel whose walls still show ore. Peaceful. Local y 4 is the yard.
 */
public final class HillsideMinePiece extends MinePiece {
	static final int WIDTH = 15, HEIGHT = 17, DEPTH = 34, YARD = 4;

	public HillsideMinePiece(int x, int y, int z, Direction facing) {
		super(MineSites.HILLSIDE_MINE, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public HillsideMinePiece(CompoundTag tag) {
		super(MineSites.HILLSIDE_MINE, tag);
	}

	/** Faces the way the hill rises: the tunnel has to end under ground. A flat or watery spot has no mine. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		var chunk = c.chunkPos();
		Direction first = Direction.Plane.HORIZONTAL.getRandomDirection(c.random());
		for (int turn = 0; turn < 4; turn++) {
			Direction facing = first;
			for (int i = 0; i < turn; i++) facing = facing.getClockWise();
			var piece = new HillsideMinePiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), facing);
			BlockPos entrance = piece.getWorldPos(7, 0, 0);
			if (wet(c, entrance)) continue;
			int yard = ground(c, entrance), inner = ground(c, piece.getWorldPos(7, 0, 26)), mouth = ground(c, piece.getWorldPos(7, 0, 14));
			if (inner - yard < 6 || inner - yard > 40 || mouth < yard + 1) continue;
			piece.move(0, yard - YARD, 0);
			return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(), yard, entrance.getZ()), b -> b.addPiece(piece)));
		}
		return Optional.empty();
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk,
			BlockPos reference) {
		BlockState plank = s(Blocks.SPRUCE_PLANKS), log = s(Blocks.SPRUCE_LOG), beam = s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.X);
		BlockState stone = s(Blocks.STONE), cobble = s(Blocks.COBBLESTONE);
		// The yard, dug flat into the slope's foot.
		fill(level, bb, 0, YARD + 1, 0, 14, 16, 11, AIR);
		fill(level, bb, 1, YARD, 0, 13, YARD, 11, plank);
		for (int x : new int[] {1, 7, 13}) for (int z : new int[] {0, 5, 10}) footing(level, bb, x, z, x, z, YARD, log);
		// The pit and headframe: a chain from the pulley down to a hanging bucket.
		fill(level, bb, 2, 0, 3, 6, YARD, 7, cobble);
		fill(level, bb, 3, 1, 4, 5, YARD, 6, AIR);
		for (int x : new int[] {2, 6}) for (int z : new int[] {3, 7}) fill(level, bb, x, YARD + 1, z, x, YARD + 9, z, log);
		fill(level, bb, 2, YARD + 10, 3, 6, YARD + 10, 3, beam);
		fill(level, bb, 2, YARD + 10, 7, 6, YARD + 10, 7, beam);
		fill(level, bb, 4, YARD + 10, 3, 4, YARD + 10, 7, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Z));
		set(level, bb, s(Blocks.SPRUCE_FENCE), 4, YARD + 11, 5);
		set(level, bb, s(Blocks.SPRUCE_FENCE), 3, YARD + 11, 5);
		set(level, bb, s(Blocks.SPRUCE_FENCE), 5, YARD + 11, 5);
		fill(level, bb, 4, 2, 5, 4, YARD + 9, 5, s(Blocks.IRON_CHAIN));
		set(level, bb, s(Blocks.CAULDRON), 4, 1, 5);
		for (int x : new int[] {3, 5}) set(level, bb, s(Blocks.SPRUCE_FENCE), x, YARD + 1, 4);
		// The sorting shed: the tally barrels, a stonecutter and the crew's chest.
		room(level, bb, 9, YARD, 2, 13, YARD + 4, 8, plank);
		for (int x : new int[] {9, 13}) for (int z : new int[] {2, 8}) fill(level, bb, x, YARD, z, x, YARD + 4, z, log);
		fill(level, bb, 8, YARD + 5, 1, 14, YARD + 5, 9, s(Blocks.SPRUCE_SLAB));
		fill(level, bb, 9, YARD + 1, 5, 9, YARD + 2, 5, AIR);
		fill(level, bb, 13, YARD + 2, 4, 13, YARD + 2, 6, s(Blocks.GLASS_PANE));
		set(level, bb, s(Blocks.CRAFTING_TABLE), 12, YARD + 1, 3);
		set(level, bb, s(Blocks.STONECUTTER), 11, YARD + 1, 3);
		barrel(level, bb, 12, YARD + 1, 7, MineSites.HILLSIDE_TALLY, 411);
		barrel(level, bb, 11, YARD + 1, 7, null, 0);
		barrel(level, bb, 12, YARD + 2, 7, null, 0);
		chest(level, bb, 12, YARD + 1, 5, MineSites.HILLSIDE, Direction.WEST, 412);
		set(level, bb, s(Blocks.LANTERN), 10, YARD + 1, 3);
		// The tunnel: rock (some of it ore) around a framed bore, rails down its middle.
		for (int x = 3; x <= 11; x++) for (int y = YARD - 1; y <= YARD + 6; y++) for (int z = 12; z <= DEPTH - 1; z++) set(level, bb, rock(x, y, z, stone, false), x, y, z);
		fill(level, bb, 5, YARD + 1, 12, 9, YARD + 4, DEPTH - 3, AIR);
		fill(level, bb, 4, YARD + 1, DEPTH - 6, 10, YARD + 4, DEPTH - 3, AIR);
		for (int z = 12; z <= DEPTH - 6; z += 4) {
			fill(level, bb, 5, YARD + 1, z, 5, YARD + 3, z, log);
			fill(level, bb, 9, YARD + 1, z, 9, YARD + 3, z, log);
			fill(level, bb, 5, YARD + 4, z, 9, YARD + 4, z, beam);
			if (z % 8 == 0) hangingLantern(level, bb, 7, YARD + 3, z + 1);
		}
		fill(level, bb, 7, YARD + 1, 0, 7, YARD + 1, DEPTH - 4, s(Blocks.RAIL));
		set(level, bb, s(Blocks.LANTERN), 4, YARD + 1, DEPTH - 3);
		chest(level, bb, 10, YARD + 1, DEPTH - 4, MineSites.HILLSIDE, Direction.WEST, 413);
		set(level, bb, s(Blocks.IRON_ORE), 7, YARD + 1, DEPTH - 2);
		set(level, bb, s(Blocks.COPPER_ORE), 6, YARD + 2, DEPTH - 2);
	}
}
