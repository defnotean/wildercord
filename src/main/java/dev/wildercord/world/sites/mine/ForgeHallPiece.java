package dev.wildercord.world.sites.mine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * A mountain forge hall where the Iron breath was taught: a deepslate hall with twin smoking chimneys, four anvils round the
 * master's anvil, a line of blast furnaces, a lava channel behind iron bars and a quench cauldron. Peaceful; its masterwork
 * chest keeps an Iron manual page.
 */
public final class ForgeHallPiece extends MinePiece {
	static final int WIDTH = 23, HEIGHT = 17, DEPTH = 27;

	public ForgeHallPiece(int x, int y, int z, Direction facing) {
		super(MineSites.FORGE_HALL, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public ForgeHallPiece(CompoundTag tag) {
		super(MineSites.FORGE_HALL, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		var chunk = c.chunkPos();
		var piece = new ForgeHallPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		BlockPos entrance = piece.getWorldPos(11, 0, 0), middle = piece.getWorldPos(11, 0, 13);
		if (wet(c, entrance) || wet(c, middle)) return Optional.empty();
		int y = ground(c, middle);
		piece.move(0, y - 1, 0);
		return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(), y, entrance.getZ()), b -> b.addPiece(piece)));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk,
			BlockPos reference) {
		BlockState brick = s(Blocks.DEEPSLATE_BRICKS), tile = s(Blocks.DEEPSLATE_TILES), floor = s(Blocks.POLISHED_DEEPSLATE), stone = s(Blocks.STONE_BRICKS);
		fill(level, bb, 0, 1, 0, 22, 16, 26, AIR);
		// The hall: stone brick walls on deepslate buttresses, a stepped roof.
		room(level, bb, 1, 0, 1, 21, 11, 25, stone);
		fill(level, bb, 1, 0, 1, 21, 0, 25, floor);
		fill(level, bb, 1, 11, 1, 21, 11, 25, tile);
		fill(level, bb, 4, 12, 1, 18, 12, 25, tile);
		fill(level, bb, 8, 13, 1, 14, 13, 25, brick);
		for (int z = 1; z <= 25; z += 6) {
			fill(level, bb, 0, 0, z, 0, 9, z, brick);
			fill(level, bb, 22, 0, z, 22, 9, z, brick);
		}
		for (int z = 4; z <= 22; z += 6) {
			fill(level, bb, 1, 5, z, 1, 7, z + 1, s(Blocks.IRON_BARS));
			fill(level, bb, 21, 5, z, 21, 7, z + 1, s(Blocks.IRON_BARS));
		}
		// The door: a deepslate arch with steps down to the slope.
		fill(level, bb, 9, 1, 1, 13, 5, 1, AIR);
		fill(level, bb, 8, 6, 1, 14, 6, 1, brick);
		fill(level, bb, 8, 0, 0, 14, 0, 0, s(Blocks.DEEPSLATE_BRICK_SLAB));
		// Twin chimneys, each over a hearth, smoking from the top.
		for (int cx : new int[] {4, 18}) {
			room(level, bb, cx - 1, 11, 20, cx + 1, 16, 22, brick);
			set(level, bb, s(Blocks.CAMPFIRE), cx, 15, 21);
			set(level, bb, AIR, cx, 16, 21);
			fill(level, bb, cx - 1, 1, 21, cx + 1, 1, 21, s(Blocks.MAGMA_BLOCK));
			fill(level, bb, cx - 1, 2, 21, cx + 1, 2, 21, s(Blocks.IRON_BARS));
		}
		// The smelting line: blast furnaces facing the hall, between brick piers.
		for (int x = 7; x <= 15; x++) set(level, bb, x % 2 == 1 ? s(Blocks.BLAST_FURNACE).setValue(BlastFurnaceBlock.FACING, Direction.SOUTH) : brick, x, 1, 20);
		fill(level, bb, 7, 2, 20, 15, 2, 20, s(Blocks.DEEPSLATE_BRICK_SLAB));
		// The lava channel at the back, sunk in the floor and railed off.
		fill(level, bb, 5, -1, 23, 17, -1, 24, brick);
		fill(level, bb, 6, 0, 24, 16, 0, 24, s(Blocks.LAVA));
		fill(level, bb, 6, 1, 23, 16, 2, 23, s(Blocks.IRON_BARS));
		// Anvils round the master's anvil; the quench cauldron; grindstone and smithing tables.
		for (int x : new int[] {7, 15}) for (int z : new int[] {7, 13}) set(level, bb, s(Blocks.ANVIL).setValue(AnvilBlock.FACING, Direction.EAST), x, 1, z);
		fill(level, bb, 9, 0, 9, 13, 0, 11, brick);
		set(level, bb, s(Blocks.ANVIL).setValue(AnvilBlock.FACING, Direction.EAST), 11, 1, 10);
		chest(level, bb, 11, 1, 12, MineSites.FORGE_MASTERWORK, Direction.SOUTH, 421);
		set(level, bb, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL), 11, 1, 16);
		set(level, bb, s(Blocks.SMITHING_TABLE), 3, 1, 10);
		set(level, bb, s(Blocks.SMITHING_TABLE), 19, 1, 10);
		set(level, bb, s(Blocks.GRINDSTONE), 3, 1, 14);
		chest(level, bb, 2, 1, 6, MineSites.FORGE, Direction.EAST, 422);
		chest(level, bb, 20, 1, 6, MineSites.FORGE, Direction.WEST, 423);
		barrel(level, bb, 2, 1, 17, null, 0);
		barrel(level, bb, 20, 1, 17, null, 0);
		set(level, bb, s(Blocks.CRAFTING_TABLE), 19, 1, 14);
		for (int x : new int[] {6, 11, 16}) for (int z : new int[] {5, 15}) {
			fill(level, bb, x, 8, z, x, 10, z, s(Blocks.IRON_CHAIN));
			hangingLantern(level, bb, x, 7, z);
		}
	}
}
