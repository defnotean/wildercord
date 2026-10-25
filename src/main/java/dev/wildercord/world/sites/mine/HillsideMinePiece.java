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
	static final int WIDTH = 15, HEIGHT = 21, DEPTH = 34, YARD = 4;

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
		BlockState stone = s(Blocks.STONE), cobble = s(Blocks.COBBLESTONE), fence = s(Blocks.SPRUCE_FENCE);
		Palette deck = Palette.of(Blocks.SPRUCE_PLANKS, 9, Blocks.DARK_OAK_PLANKS, 2);
		Palette masonry = Palette.of(Blocks.STONE_BRICKS, 8, Blocks.MOSSY_STONE_BRICKS, 3, Blocks.CRACKED_STONE_BRICKS, 2, Blocks.COBBLESTONE, 2);
		Palette siding = Palette.of(Blocks.SPRUCE_PLANKS, 7, Blocks.STRIPPED_SPRUCE_LOG, 2);
		// The yard, dug flat into the slope's foot, held back by dry-stone walls capped with slabs.
		fill(level, bb, 0, YARD + 1, 0, 14, 20, 11, AIR);
		fill(level, bb, 1, YARD, 0, 13, YARD, 11, deck);
		for (int x : new int[] {1, 7, 13}) for (int z : new int[] {0, 5, 10}) footing(level, bb, x, z, x, z, YARD, log);
		fill(level, bb, 1, YARD - 1, 0, 13, YARD - 1, 0, beam);
		for (int x : new int[] {0, 14}) {
			int z0 = x == 0 ? 3 : 9;
			fill(level, bb, x, YARD - 1, z0, x, YARD + 2, 11, masonry);
			fill(level, bb, x, YARD + 3, z0, x, YARD + 3, 11, s(Blocks.STONE_BRICK_SLAB));
			footing(level, bb, x, z0, x, 11, YARD - 1, cobble);
		}
		fill(level, bb, 0, YARD + 1, 12, 2, YARD + 5, 12, masonry);
		fill(level, bb, 12, YARD + 1, 12, 14, YARD + 5, 12, masonry);
		// Lantern posts at the yard's open corners.
		for (int x : new int[] {1, 13}) {
			fill(level, bb, x, YARD + 1, 0, x, YARD + 2, 0, fence);
			set(level, bb, s(Blocks.LANTERN), x, YARD + 3, 0);
		}
		// The pit and headframe: a chain from the pulley down to a hanging bucket.
		fill(level, bb, 2, 0, 3, 6, YARD, 7, cobble);
		fill(level, bb, 3, 1, 4, 5, YARD, 6, AIR);
		weather(level, bb, 2, 0, 3, 6, YARD, 7, cobble, s(Blocks.MOSSY_COBBLESTONE), 25);
		for (int x : new int[] {2, 6}) for (int z : new int[] {3, 7}) fill(level, bb, x, YARD + 1, z, x, YARD + 9, z, log);
		fill(level, bb, 2, YARD + 10, 3, 6, YARD + 10, 3, beam);
		fill(level, bb, 2, YARD + 10, 7, 6, YARD + 10, 7, beam);
		fill(level, bb, 4, YARD + 10, 3, 4, YARD + 10, 7, s(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, net.minecraft.core.Direction.Axis.Z));
		// Knee braces under the beams and a mid-height tie between the legs.
		for (int z : new int[] {3, 7}) {
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 3, YARD + 9, z);
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, true), 5, YARD + 9, z);
			fill(level, bb, 3, YARD + 5, z, 5, YARD + 5, z, beam);
		}
		set(level, bb, fence, 4, YARD + 11, 5);
		set(level, bb, fence, 3, YARD + 11, 5);
		set(level, bb, fence, 5, YARD + 11, 5);
		// A little hipped sheave house keeps the rain off the pulley.
		for (int x : new int[] {2, 6}) for (int z : new int[] {3, 7}) set(level, bb, fence, x, YARD + 11, z);
		hipRoof(level, bb, 2, 3, 6, 7, YARD + 12, Blocks.SPRUCE_STAIRS, plank, s(Blocks.SPRUCE_SLAB));
		fill(level, bb, 4, 2, 5, 4, YARD + 9, 5, s(Blocks.IRON_CHAIN));
		set(level, bb, s(Blocks.CAULDRON), 4, 1, 5);
		for (int x : new int[] {3, 5}) set(level, bb, fence, x, YARD + 1, 4);
		// Ore waiting for the cart: a spill of raw stone and barrels by the pit.
		set(level, bb, s(Blocks.RAW_IRON_BLOCK), 1, YARD + 1, 8);
		set(level, bb, s(Blocks.COBBLESTONE_SLAB), 2, YARD + 1, 8);
		set(level, bb, s(Blocks.COAL_BLOCK), 1, YARD + 1, 9);
		barrel(level, bb, 1, YARD + 1, 10, null, 0);
		barrel(level, bb, 1, YARD + 2, 10, null, 0);
		fill(level, bb, 2, YARD + 1, 10, 4, YARD + 1, 10, beam);
		// The sorting shed: a stone sill course, timber framing and a steep gable roof.
		room(level, bb, 9, YARD, 2, 13, YARD + 4, 8, plank);
		walls(level, bb, 9, YARD + 1, 2, 13, YARD + 3, 8, siding);
		walls(level, bb, 9, YARD, 2, 13, YARD, 8, masonry);
		for (int x : new int[] {9, 13}) for (int z : new int[] {2, 8}) fill(level, bb, x, YARD, z, x, YARD + 4, z, log);
		fill(level, bb, 9, YARD + 4, 2, 13, YARD + 4, 2, beam);
		fill(level, bb, 9, YARD + 4, 8, 13, YARD + 4, 8, beam);
		gableRoof(level, bb, 9, 2, 13, 8, YARD + 5, false, Blocks.SPRUCE_STAIRS, s(Blocks.SPRUCE_SLAB), siding);
		fill(level, bb, 9, YARD + 1, 5, 9, YARD + 2, 5, AIR);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, true), 8, YARD + 3, 5);
		for (int z : new int[] {4, 6}) set(level, bb, s(Blocks.LANTERN), 8, YARD + 1, z);
		fill(level, bb, 13, YARD + 2, 4, 13, YARD + 2, 6, s(Blocks.GLASS_PANE));
		for (int z = 4; z <= 6; z++) {
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 14, YARD + 1, z);
			set(level, bb, s(Blocks.SPRUCE_TRAPDOOR), 14, YARD + 3, z);
		}
		set(level, bb, s(Blocks.CRAFTING_TABLE), 12, YARD + 1, 3);
		set(level, bb, s(Blocks.STONECUTTER), 11, YARD + 1, 3);
		barrel(level, bb, 12, YARD + 1, 7, MineSites.HILLSIDE_TALLY, 411);
		barrel(level, bb, 11, YARD + 1, 7, null, 0);
		barrel(level, bb, 12, YARD + 2, 7, null, 0);
		chest(level, bb, 12, YARD + 1, 5, MineSites.HILLSIDE, Direction.WEST, 412);
		set(level, bb, s(Blocks.LANTERN), 10, YARD + 1, 3);
		hangingLantern(level, bb, 11, YARD + 3, 5);
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
		// The portal: masonry piers, a lintel with a keystone and a battered cornice, lamps hung under the lintel.
		for (int x : new int[] {4, 10}) {
			fill(level, bb, x, YARD + 1, 11, x, YARD + 4, 11, masonry);
			set(level, bb, s(Blocks.POLISHED_ANDESITE), x, YARD, 11);
		}
		fill(level, bb, 4, YARD + 5, 11, 10, YARD + 5, 11, s(Blocks.STONE_BRICKS));
		set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), 7, YARD + 5, 11);
		for (int x = 3; x <= 11; x++) set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false), x, YARD + 6, 11);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 5, YARD + 4, 11);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 9, YARD + 4, 11);
		hangingLantern(level, bb, 6, YARD + 4, 11);
		hangingLantern(level, bb, 8, YARD + 4, 11);
		fill(level, bb, 7, YARD + 1, 0, 7, YARD + 1, DEPTH - 4, s(Blocks.RAIL));
		set(level, bb, s(Blocks.LANTERN), 4, YARD + 1, DEPTH - 3);
		chest(level, bb, 10, YARD + 1, DEPTH - 4, MineSites.HILLSIDE, Direction.WEST, 413);
		set(level, bb, s(Blocks.IRON_ORE), 7, YARD + 1, DEPTH - 2);
		set(level, bb, s(Blocks.COPPER_ORE), 6, YARD + 2, DEPTH - 2);
	}
}
