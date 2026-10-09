package dev.wildercord.world.sites.wilds;

import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.spell.Runes;
import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
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
		clear(level, bb, 8, 1, 0, 10, 4, 0);
		for (int x : new int[]{7, 11}) fill(level, bb, x, 1, 0, x, 6, 0, s(Blocks.CHISELED_POLISHED_BLACKSTONE));
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
		chest(level, bb, 3, 1, 15, WildsSites.EMBER_HALL, Direction.EAST, 1);
		// The tower: the strongroom below, a lit room above, the fire on top.
		room(level, bb, 7, 0, 10, 11, 16, 14, s(Blocks.POLISHED_BLACKSTONE_BRICKS));
		for (int y = 1; y <= 15; y++) for (int x = 7; x <= 11; x++) for (int z = 10; z <= 14; z++)
			if ((x == 7 || x == 11) && (z == 10 || z == 14)) set(level, bb, s(Blocks.CHISELED_POLISHED_BLACKSTONE), x, y, z);
		fill(level, bb, 8, 6, 11, 10, 6, 13, s(Blocks.POLISHED_BLACKSTONE));
		set(level, bb, s(Blocks.SHROOMLIGHT), 9, 7, 12);
		for (int y = 9; y <= 10; y++) { set(level, bb, AIR, 9, y, 10); set(level, bb, AIR, 9, y, 14); set(level, bb, AIR, 7, y, 12); set(level, bb, AIR, 11, y, 12); }
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

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(7, 0, 10, 11, 6, 14)); }
}
