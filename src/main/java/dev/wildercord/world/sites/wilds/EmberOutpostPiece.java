package dev.wildercord.world.sites.wilds;

import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.spell.Runes;
import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * A walled blackstone outpost on a Nether cave floor, its tower topped by a fire that never goes out. Wither skeletons
 * carrying ember spells hold the yard; the tower's ground floor is the strongroom, behind a Fire and Blood seal.
 * The wart in its walls matches the forest around it.
 */
public final class EmberOutpostPiece extends WildsPiece {
	public EmberOutpostPiece(int x, int z, Direction facing) { super(WildsSites.EMBER_OUTPOST, x, z, 19, 18, 19, facing); }
	public EmberOutpostPiece(CompoundTag tag) { super(WildsSites.EMBER_OUTPOST, tag); }
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		return cave(c, new EmberOutpostPiece(c.chunkPos().getMinBlockX(), c.chunkPos().getMinBlockZ(), facing(c)), 9, 34, 100, 6);
	}

	private static BlockState stone(int x, int y, int z) {
		int n = noise(x, y, z);
		return s(n < 20 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS : n < 30 ? Blocks.GILDED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS);
	}

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);
		BlockState wart = s(level.getBiome(getWorldPos(9, 1, 9)).is(Biomes.WARPED_FOREST) ? Blocks.WARPED_WART_BLOCK : Blocks.NETHER_WART_BLOCK);
		footing(level, bb, 0, 0, 18, 18, s(Blocks.BLACKSTONE));
		fill(level, bb, 0, 0, 0, 18, 0, 18, s(Blocks.POLISHED_BLACKSTONE));
		clear(level, bb, 0, 1, 0, 18, 9, 18);
		clear(level, bb, 6, 10, 9, 12, 17, 15);
		// The yard wall and gate.
		for (int x = 0; x <= 18; x++) for (int z = 0; z <= 18; z++) {
			if (x != 0 && x != 18 && z != 0 && z != 18) continue;
			for (int y = 1; y <= 4; y++) set(level, bb, stone(x, y, z), x, y, z);
			set(level, bb, (x + z) % 2 == 0 ? wart : s(Blocks.POLISHED_BLACKSTONE_BRICKS), x, 5, z);
		}
		crenellate(level, bb, 0, 0, 18, 18, 6, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
		clear(level, bb, 8, 1, 0, 10, 4, 0);
		for (int x : new int[]{7, 11}) {
			fill(level, bb, x, 1, 0, x, 6, 0, s(Blocks.CHISELED_POLISHED_BLACKSTONE));
			set(level, bb, s(Blocks.LANTERN), x, 7, 0);
		}
		// The gate's shoulders round it into an arch.
		set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true), 8, 4, 0);
		set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, true), 10, 4, 0);
		// Braziers and wart grown over the yard.
		for (int[] p : new int[][]{{3, 3}, {15, 3}, {15, 15}}) {
			set(level, bb, s(Blocks.GILDED_BLACKSTONE), p[0], 1, p[1]);
			set(level, bb, s(Blocks.CAMPFIRE), p[0], 2, p[1]);
		}
		for (int x = 1; x <= 17; x++) for (int z = 1; z <= 17; z++) {
			if (x >= 6 && x <= 12 && z >= 7) continue;
			int n = noise(x, z);
			if (n < 7) set(level, bb, wart, x, 1, z);
			else if (n < 9) set(level, bb, s(Blocks.SHROOMLIGHT), x, 0, z);
		}
		// Counterforts inside the wall, each capped by a stair leaning on it.
		for (int[] p : new int[][]{{1, 5, 4}, {1, 10, 4}, {17, 5, 5}, {17, 10, 5}, {5, 17, 2}, {13, 17, 2}, {4, 1, 3}, {14, 1, 3}}) {
			Direction wall = p[2] == 2 ? Direction.NORTH : p[2] == 3 ? Direction.SOUTH : p[2] == 4 ? Direction.WEST : Direction.EAST;
			fill(level, bb, p[0], 1, p[1], p[0], 3, p[1], s(Blocks.POLISHED_BLACKSTONE_BRICKS));
			set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, wall, false), p[0], 4, p[1]);
		}
		for (int x : new int[]{6, 12}) banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.RED), x, 3, 1, Direction.NORTH);
		// A dressed road from the gate to the tower, swept of wart.
		for (int x = 8; x <= 10; x++) for (int z = 1; z <= 9; z++) {
			set(level, bb, s(Blocks.POLISHED_BLACKSTONE_BRICKS), x, 0, z);
			if (z <= 6) set(level, bb, AIR, x, 1, z);
		}
		chest(level, bb, 3, 1, 15, WildsSites.EMBER_HALL, Direction.EAST, 1);
		// The tower: the strongroom below, a lit room above, the fire on top.
		room(level, bb, 7, 0, 10, 11, 16, 14, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
		for (int y = 1; y <= 15; y++) for (int x = 7; x <= 11; x++) for (int z = 10; z <= 14; z++)
			if ((x == 7 || x == 11) && (z == 10 || z == 14)) set(level, bb, s(Blocks.CHISELED_POLISHED_BLACKSTONE), x, y, z);
		fill(level, bb, 8, 6, 11, 10, 6, 13, s(Blocks.POLISHED_BLACKSTONE));
		set(level, bb, s(Blocks.SHROOMLIGHT), 9, 7, 12);
		for (int y = 9; y <= 10; y++) { set(level, bb, AIR, 9, y, 10); set(level, bb, AIR, 9, y, 14); set(level, bb, AIR, 7, y, 12); set(level, bb, AIR, 11, y, 12); }
		// A string course at the upper floor, hooded windows, and a corbelled, crenellated fire platform.
		towerRing(level, bb, 7, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
		towerRing(level, bb, 15, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
		for (int x = 6; x <= 12; x++) for (int z = 9; z <= 15; z++)
			if (x == 6 || x == 12 || z == 9 || z == 15) set(level, bb, stone(x, 16, z), x, 16, z);
		crenellate(level, bb, 6, 9, 12, 15, 17, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
		for (int y : new int[]{8, 11}) {
			set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH, true), 9, y, 9);
			set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.SOUTH, true), 9, y, 15);
			set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, true), 6, y, 12);
			set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true), 12, y, 12);
		}
		for (int x : new int[]{8, 10}) banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.RED), x, 12, 9, Direction.SOUTH);
		hangingLantern(level, bb, 9, 15, 12, 3, false);
		set(level, bb, s(Blocks.MAGMA_BLOCK), 9, 16, 12);
		set(level, bb, s(Blocks.CAMPFIRE), 9, 17, 12);
		sealDoorAcross(level, bb, 10, 8, 10, 1, 3, RuneSealBlock.Element.FIRE, RuneSealBlock.Element.BLOOD);
		set(level, bb, s(Blocks.SHROOMLIGHT), 8, 0, 11);
		set(level, bb, s(Blocks.SHROOMLIGHT), 10, 0, 11);
		chest(level, bb, 9, 1, 13, WildsSites.EMBER_VAULT, Direction.SOUTH, 2);
		guard(level, bb, EntityTypes.WITHER_SKELETON, 5, 1, 6, List.of(Runes.BOLT, Runes.EMBER), false);
		guard(level, bb, EntityTypes.WITHER_SKELETON, 13, 1, 6, List.of(Runes.BOLT, Runes.EMBER), false);
		guard(level, bb, EntityTypes.WITHER_SKELETON, 9, 1, 8, List.of(Runes.BURST, Runes.FIRE), true);
	}

	/** Upside-down stairs around the tower's outside at {@code y}, each leaning on the wall, solid at the corners. */
	private void towerRing(WorldGenLevel level, BoundingBox bb, int y, Block stairs) {
		for (int x = 7; x <= 11; x++) {
			set(level, bb, stair(stairs, Direction.NORTH, true), x, y, 9);
			set(level, bb, stair(stairs, Direction.SOUTH, true), x, y, 15);
		}
		for (int z = 10; z <= 14; z++) {
			set(level, bb, stair(stairs, Direction.EAST, true), 6, y, z);
			set(level, bb, stair(stairs, Direction.WEST, true), 12, y, z);
		}
		for (int[] c : new int[][]{{6, 9}, {12, 9}, {6, 15}, {12, 15}}) set(level, bb, s(Blocks.POLISHED_BLACKSTONE_BRICKS), c[0], y, c[1]);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(7, 0, 10, 11, 6, 14)); }
}
