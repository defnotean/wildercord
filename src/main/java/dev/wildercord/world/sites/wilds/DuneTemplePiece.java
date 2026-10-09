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
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * A domed temple half sunk in the dunes. The hall floor has a square of loose sand ringed in carved stone: dig one block
 * and the whole square slides into the hidden sanctum below, where a sandstorm adept watches an Earth and Wind seal.
 */
public final class DuneTemplePiece extends WildsPiece {
	public DuneTemplePiece(int x, int z, Direction facing) { super(WildsSites.DUNE_TEMPLE, x, z, 21, 16, 21, facing); }
	public DuneTemplePiece(CompoundTag tag) { super(WildsSites.DUNE_TEMPLE, tag); }
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		return surface(c, new DuneTemplePiece(c.chunkPos().getMinBlockX(), c.chunkPos().getMinBlockZ(), facing(c)), 5);
	}

	/** The loose square: local x and z 9 to 11, at the hall floor (y 0) and the sanctum's ceiling (y -1). */
	static boolean plug(int x, int z) { return x >= 9 && x <= 11 && z >= 9 && z <= 11; }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);
		footing(level, bb, 0, 0, 20, 20, s(Blocks.SANDSTONE));
		fill(level, bb, 0, 0, 0, 20, 0, 20, s(Blocks.SANDSTONE));
		clear(level, bb, 0, 1, 0, 20, 15, 20);
		// The hidden sanctum under the hall.
		room(level, bb, 4, -6, 4, 16, -1, 16, s(Blocks.CUT_SANDSTONE));
		for (int[] p : new int[][]{{6, 6}, {14, 6}, {6, 11}, {14, 11}}) set(level, bb, s(Blocks.OCHRE_FROGLIGHT), p[0], -6, p[1]);
		for (int y = -5; y <= -1; y++) set(level, bb, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.EAST), 5, y, 10);
		// The reliquary behind the seal.
		fill(level, bb, 5, -5, 13, 15, -2, 13, s(Blocks.CHISELED_SANDSTONE));
		sealDoorAcross(level, bb, 13, 9, 11, -5, -3, RuneSealBlock.Element.EARTH, RuneSealBlock.Element.WIND);
		chest(level, bb, 10, -5, 15, WildsSites.DUNE_VAULT, Direction.SOUTH, 2);
		for (int[] p : new int[][]{{5, 14}, {15, 15}, {6, 15}}) brushable(level, bb, p[0], -5, p[1], 30 + p[0]);
		// The hall.
		room(level, bb, 3, 0, 3, 17, 7, 17, s(Blocks.CUT_SANDSTONE));
		for (int x = 3; x <= 17; x++) for (int z = 3; z <= 17; z++)
			if ((x == 3 || x == 17 || z == 3 || z == 17) && noise(x, z) < 30) set(level, bb, s(Blocks.SMOOTH_SANDSTONE), x, 2, z);
		for (int[] p : new int[][]{{5, 5}, {15, 5}, {5, 15}, {15, 15}}) {
			fill(level, bb, p[0], 1, p[1], p[0], 6, p[1], s(Blocks.CHISELED_SANDSTONE));
			set(level, bb, s(Blocks.LANTERN), p[0] + (p[0] < 10 ? 1 : -1), 1, p[1]);
		}
		for (int z : new int[]{7, 13}) { fill(level, bb, 3, 4, z, 3, 5, z, AIR); fill(level, bb, 17, 4, z, 17, 5, z, AIR); }
		// The loose square, ringed in carved stone, and the sand cap over the ladder.
		for (int x = 8; x <= 12; x++) for (int z = 8; z <= 12; z++) set(level, bb, s(plug(x, z) ? Blocks.SAND : Blocks.CHISELED_SANDSTONE), x, 0, z);
		fill(level, bb, 9, -1, 9, 11, -1, 11, s(Blocks.SAND));
		set(level, bb, s(Blocks.SAND), 5, 0, 10);
		// Sand blown in through the door, with a few things buried in it.
		for (int x = 4; x <= 16; x++) for (int z = 4; z <= 16; z++) {
			if (x >= 7 && x <= 13 && z >= 7 && z <= 13 || x >= 8 && x <= 12 && z <= 7) continue;
			int n = noise(x, z);
			if (n < 4) brushable(level, bb, x, 1, z, n);
			else if (n < 16) set(level, bb, s(Blocks.SAND), x, 1, z);
		}
		// The dome, spire and corner posts.
		double[] radius = {6, 5.5, 4.5, 3, 1.5};
		for (int y = 8; y <= 12; y++) for (int x = 3; x <= 17; x++) for (int z = 3; z <= 17; z++)
			if (dist(x, z, 10, 10) <= radius[y - 8]) set(level, bb, s(Blocks.SMOOTH_SANDSTONE), x, y, z);
		fill(level, bb, 10, 13, 10, 10, 14, 10, s(Blocks.CUT_SANDSTONE));
		set(level, bb, s(Blocks.CHISELED_SANDSTONE), 10, 15, 10);
		for (int[] p : new int[][]{{3, 3}, {17, 3}, {3, 17}, {17, 17}}) fill(level, bb, p[0], 8, p[1], p[0], 10, p[1], s(Blocks.CHISELED_SANDSTONE));
		// The door, half full of sand, and the dunes heaped against the walls.
		clear(level, bb, 9, 1, 3, 11, 3, 3);
		fill(level, bb, 9, 1, 3, 11, 1, 3, s(Blocks.SAND));
		fill(level, bb, 8, 4, 3, 12, 4, 3, s(Blocks.CHISELED_SANDSTONE));
		for (int x = 0; x <= 20; x++) for (int z = 0; z <= 20; z++) {
			if (x >= 3 && x <= 17 && z >= 3 && z <= 17 || x >= 8 && x <= 12 && z < 3) continue;
			int n = noise(z, x), high = Math.min(x, Math.min(z, Math.min(20 - x, 20 - z)));
			if (n < 70) set(level, bb, s(Blocks.SAND), x, 1, z);
			if (n < 45 && high >= 1) set(level, bb, s(Blocks.SAND), x, 2, z);
			if (n < 20 && high >= 2) set(level, bb, s(Blocks.SAND), x, 3, z);
		}
		chest(level, bb, 15, 1, 14, WildsSites.DUNE_HALL, Direction.WEST, 1);
		guard(level, bb, EntityTypes.HUSK, 6, 1, 8, List.of(Runes.BOLT, Runes.MIRE), false);
		guard(level, bb, EntityTypes.HUSK, 14, 1, 10, List.of(Runes.BOLT, Runes.MIRE), false);
		guard(level, bb, EntityTypes.HUSK, 13, -5, 7, List.of(Runes.ZONE, Runes.SANDSTORM), true);
	}

	private void brushable(WorldGenLevel level, BoundingBox bb, int x, int y, int z, long salt) {
		BlockPos at = getWorldPos(x, y, z);
		if (!bb.isInside(at)) return;
		set(level, bb, s(Blocks.SUSPICIOUS_SAND), x, y, z);
		if (level.getBlockEntity(at) instanceof BrushableBlockEntity sand)
			sand.setLootTable(WildsSites.DUNE_SAND, getBoundingBox().minX() * 31L + getBoundingBox().minZ() * 17L + salt);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(4, -6, 13, 16, -1, 16)); }
}
