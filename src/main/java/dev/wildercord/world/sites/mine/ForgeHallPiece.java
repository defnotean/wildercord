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
	static final int WIDTH = 23, HEIGHT = 24, DEPTH = 27;

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
		BlockState chiseled = s(Blocks.CHISELED_DEEPSLATE);
		Palette plinth = Palette.of(Blocks.DEEPSLATE_BRICKS, 7, Blocks.CRACKED_DEEPSLATE_BRICKS, 2, Blocks.COBBLED_DEEPSLATE, 1);
		Palette wall = Palette.of(Blocks.STONE_BRICKS, 9, Blocks.CRACKED_STONE_BRICKS, 2, Blocks.MOSSY_STONE_BRICKS, 1);
		Palette paving = Palette.of(Blocks.POLISHED_DEEPSLATE, 7, Blocks.DEEPSLATE_TILES, 2, Blocks.CRACKED_DEEPSLATE_TILES, 1);
		fill(level, bb, 0, 1, 0, 22, 23, 26, AIR);
		footing(level, bb, 0, 0, 22, 26, 0, s(Blocks.COBBLED_DEEPSLATE));
		// The hall: a dark plinth course under worn stone brick walls, deepslate pilasters, a flat ceiling under the roof.
		room(level, bb, 1, 0, 1, 21, 11, 25, stone);
		walls(level, bb, 1, 3, 1, 21, 10, 25, wall);
		walls(level, bb, 1, 0, 1, 21, 2, 25, plinth);
		fill(level, bb, 1, 0, 1, 21, 0, 25, floor);
		fill(level, bb, 2, 0, 2, 20, 0, 24, paving);
		fill(level, bb, 10, 0, 2, 12, 0, 8, tile);
		fill(level, bb, 1, 11, 1, 21, 11, 25, tile);
		for (int z = 1; z <= 25; z += 6) {
			fill(level, bb, 0, 0, z, 0, 9, z, brick);
			fill(level, bb, 22, 0, z, 22, 9, z, brick);
			set(level, bb, chiseled, 0, 3, z);
			set(level, bb, chiseled, 22, 3, z);
		}
		// Tall barred windows between the pilasters, each on a stair sill under a stair hood.
		for (int z = 4; z <= 22; z += 6) {
			fill(level, bb, 1, 5, z, 1, 7, z + 1, s(Blocks.IRON_BARS));
			fill(level, bb, 21, 5, z, 21, 7, z + 1, s(Blocks.IRON_BARS));
			for (int w = z; w <= z + 1; w++) {
				set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.EAST, true), 0, 4, w);
				set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.WEST, true), 22, 4, w);
				set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 0, 8, w);
				set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 22, 8, w);
			}
		}
		// A steep gable roof of deepslate tile running front to back, its gables filled with dark brick and a barred oculus.
		gableRoof(level, bb, 1, 1, 21, 25, 11, false, Blocks.DEEPSLATE_TILE_STAIRS, s(Blocks.DEEPSLATE_TILE_SLAB), plinth);
		for (int z : new int[] {1, 25}) {
			fill(level, bb, 10, 14, z, 12, 14, z, s(Blocks.IRON_BARS));
			set(level, bb, s(Blocks.IRON_BARS), 11, 15, z);
			set(level, bb, chiseled, 11, 17, z);
		}
		set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), 11, 16, 0);
		// The door: a deep portal of polished piers under a lintel and keystone, a corbelled arch and broad steps.
		fill(level, bb, 9, 1, 1, 13, 5, 1, AIR);
		for (int x : new int[] {8, 14}) {
			fill(level, bb, x, 0, 0, x, 6, 0, floor);
			set(level, bb, s(Blocks.LANTERN), x, 8, 0);
		}
		fill(level, bb, 8, 6, 1, 14, 6, 1, brick);
		fill(level, bb, 8, 7, 0, 14, 7, 0, brick);
		set(level, bb, chiseled, 11, 7, 0);
		for (int x = 9; x <= 13; x++) set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), x, 6, 0);
		set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.WEST, true), 9, 5, 1);
		set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.EAST, true), 13, 5, 1);
		fill(level, bb, 9, 0, 0, 13, 0, 0, s(Blocks.DEEPSLATE_BRICK_SLAB));
		hangingLantern(level, bb, 11, 5, 0);
		// Twin chimneys, each over a hearth, standing proud of the roof and smoking from the top.
		for (int cx : new int[] {4, 18}) {
			room(level, bb, cx - 1, 11, 20, cx + 1, 16, 22, brick);
			for (int x = cx - 1; x <= cx + 1; x++) for (int z = 20; z <= 22; z++) {
				if (x == cx && z == 21) continue;
				fill(level, bb, x, 17, z, x, 19, z, plinth);
				set(level, bb, s(Blocks.DEEPSLATE_BRICK_SLAB), x, 20, z);
			}
			set(level, bb, s(Blocks.CAMPFIRE), cx, 15, 21);
			set(level, bb, AIR, cx, 16, 21);
			fill(level, bb, cx - 1, 1, 21, cx + 1, 1, 21, s(Blocks.MAGMA_BLOCK));
			fill(level, bb, cx - 1, 2, 21, cx + 1, 2, 21, s(Blocks.IRON_BARS));
			// The chimney breast inside: a brick back and a corbelled hood over the hearth.
			fill(level, bb, cx - 1, 1, 22, cx + 1, 10, 22, plinth);
			fill(level, bb, cx - 1, 4, 21, cx + 1, 10, 21, plinth);
			for (int x = cx - 1; x <= cx + 1; x++) {
				set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), x, 3, 21);
				set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), x, 6, 20);
			}
		}
		// The smelting line: blast furnaces facing the hall, between brick piers, stock barrels at either end.
		for (int x = 7; x <= 15; x++) set(level, bb, x % 2 == 1 ? s(Blocks.BLAST_FURNACE).setValue(BlastFurnaceBlock.FACING, Direction.SOUTH) : brick, x, 1, 20);
		fill(level, bb, 7, 2, 20, 15, 2, 20, s(Blocks.DEEPSLATE_BRICK_SLAB));
		barrel(level, bb, 6, 1, 20, null, 0);
		barrel(level, bb, 16, 1, 20, null, 0);
		set(level, bb, s(Blocks.COAL_BLOCK), 6, 2, 20);
		set(level, bb, s(Blocks.RAW_IRON_BLOCK), 16, 2, 20);
		// The lava channel at the back, sunk in the floor and railed off.
		fill(level, bb, 5, -1, 23, 17, -1, 24, brick);
		fill(level, bb, 6, 0, 24, 16, 0, 24, s(Blocks.LAVA));
		fill(level, bb, 6, 1, 23, 16, 2, 23, s(Blocks.IRON_BARS));
		// Two rows of deepslate columns carry dark oak tie beams across the hall.
		for (int z : new int[] {4, 10, 16}) {
			fill(level, bb, 2, 10, z, 20, 10, z, s(Blocks.STRIPPED_DARK_OAK_LOG).setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X));
			for (int x : new int[] {5, 17}) {
				pillar(level, bb, x, z, 1, 10, floor, brick, chiseled);
				set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.SOUTH, true), x, 10, z + 1);
				set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), x, 10, z - 1);
			}
		}
		// Anvils round the master's anvil on its chiseled dais; the quench cauldron; grindstone and smithing tables.
		for (int x : new int[] {7, 15}) for (int z : new int[] {7, 13}) set(level, bb, s(Blocks.ANVIL).setValue(AnvilBlock.FACING, Direction.EAST), x, 1, z);
		fill(level, bb, 8, 0, 8, 14, 0, 8, chiseled);
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
		// Torches along both long walls, between the windows.
		for (int z : new int[] {3, 9, 15}) {
			sconce(level, bb, 2, 4, z, Direction.EAST, false);
			sconce(level, bb, 20, 4, z, Direction.WEST, false);
		}
	}
}
