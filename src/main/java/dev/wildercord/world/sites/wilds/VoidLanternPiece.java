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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * A round purpur spire on the End's outer islands with a caged light on top, seen from islands away. Endermen bound
 * with blinding and pulling spells walk its floors; a Void and Arcane seal on the second floor hides the ladder to the
 * lantern room.
 */
public final class VoidLanternPiece extends WildsPiece {
	public VoidLanternPiece(int x, int z, Direction facing) { super(WildsSites.VOID_LANTERN, x, z, 15, 36, 15, facing); }
	public VoidLanternPiece(CompoundTag tag) { super(WildsSites.VOID_LANTERN, tag); }
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		return surface(c, new VoidLanternPiece(c.chunkPos().getMinBlockX(), c.chunkPos().getMinBlockZ(), facing(c)), 4, 40);
	}
	static final int C = 7;

	static boolean inside(int x, int z) { return dist(x, z, C, C) <= 2.5; }
	static boolean ring(int x, int z) { double d = dist(x, z, C, C); return d > 2.5 && d <= 3.6; }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);
		clear(level, bb, 0, 1, 0, 14, 35, 14);
		for (int x = 0; x <= 14; x++) for (int z = 0; z <= 14; z++) {
			double d = dist(x, z, C, C);
			if (d > 7.2) continue;
			fill(level, bb, x, -SINK + (int) Math.max(0, d - 3), z, x, -1, z, s(Blocks.END_STONE));
			set(level, bb, s(d <= 4.6 ? Blocks.END_STONE_BRICKS : Blocks.END_STONE), x, 0, z);
			if (ring(x, z)) for (int y = 1; y <= 30; y++)
				set(level, bb, s((y % 10 == 0 || (x + z) % 4 == 0) ? Blocks.PURPUR_PILLAR : Blocks.PURPUR_BLOCK), x, y, z);
			if (inside(x, z)) for (int y : new int[]{11, 22}) set(level, bb, s(Blocks.PURPUR_BLOCK), x, y, z);
			if (d <= 3.6) set(level, bb, s(Blocks.PURPUR_BLOCK), x, 30, z);
		}
		// The door, windows and lights.
		clear(level, bb, 6, 1, 4, 8, 3, 4);
		for (int y : new int[]{6, 7, 16, 17, 26, 27}) {
			set(level, bb, Blocks.STAINED_GLASS.purple().defaultBlockState(), 4, y, 7);
			set(level, bb, Blocks.STAINED_GLASS.purple().defaultBlockState(), 7, y, 4);
		}
		for (int y : new int[]{1, 12, 23}) { set(level, bb, s(Blocks.END_ROD), 5, y, 7); set(level, bb, s(Blocks.END_ROD), 8, y, 9); }
		// Ground floor: a ladder up through the first floor.
		for (int y = 1; y <= 11; y++) set(level, bb, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.WEST), 9, y, 7);
		chest(level, bb, 9, 1, 6, WildsSites.VOID_HALL, Direction.WEST, 1);
		// Second floor: the alcove behind the seal, its ladder up to the lantern room.
		for (int x = 5; x <= 9; x++) for (int y = 12; y <= 21; y++) if (inside(x, 8)) set(level, bb, s(Blocks.PURPUR_BLOCK), x, y, 8);
		sealDoorAcross(level, bb, 8, 6, 8, 12, 14, RuneSealBlock.Element.VOID, RuneSealBlock.Element.ARCANE);
		for (int y = 12; y <= 22; y++) set(level, bb, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH), 7, y, 9);
		// The lantern room and the cage on the roof.
		chest(level, bb, 7, 23, 6, WildsSites.VOID_VAULT, Direction.SOUTH, 2);
		set(level, bb, s(Blocks.CRYING_OBSIDIAN), 7, 22, 6);
		for (int[] p : new int[][]{{5, 5}, {9, 5}, {5, 9}, {9, 9}}) {
			fill(level, bb, p[0], 31, p[1], p[0], 34, p[1], s(Blocks.PURPUR_PILLAR));
			set(level, bb, s(Blocks.END_ROD), p[0], 35, p[1]);
		}
		set(level, bb, s(Blocks.PURPUR_BLOCK), C, 31, C);
		set(level, bb, s(Blocks.PEARLESCENT_FROGLIGHT), C, 32, C);
		set(level, bb, s(Blocks.PEARLESCENT_FROGLIGHT), C, 33, C);
		fill(level, bb, 5, 34, 5, 9, 34, 9, s(Blocks.PURPUR_SLAB));
		guard(level, bb, EntityTypes.ENDERMAN, 6, 1, 6, List.of(Runes.BOLT, Runes.BLIND), false);
		guard(level, bb, EntityTypes.ENDERMAN, 7, 12, 6, List.of(Runes.BOLT, Runes.PULL), true);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(3, 11, 8, 11, 21, 11), worldBox(3, 22, 3, 11, 30, 11)); }
}
