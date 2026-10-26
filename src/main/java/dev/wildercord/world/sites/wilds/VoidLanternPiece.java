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
	/** The dressing course just outside the shaft: plinth, bands and the crown overhang. */
	private static boolean skirt(int x, int z) { double d = dist(x, z, C, C); return d > 3.6 && d <= 4.6; }
	/** The horizontal direction from a skirt cell back toward the shaft. */
	private static Direction toward(int x, int z) {
		int dx = C - x, dz = C - z;
		return Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
	}

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
		// A battered plinth, ledges at each floor and a corbelled crown with a parapet round the roof.
		for (int x = 0; x <= 14; x++) for (int z = 0; z <= 14; z++) {
			if (!skirt(x, z)) continue;
			if (x < 6 || x > 8 || z > C) {
				fill(level, bb, x, 1, z, x, 2, z, s(Blocks.END_STONE_BRICKS));
				set(level, bb, stair(Blocks.PURPUR_STAIRS, toward(x, z), false), x, 3, z);
			}
			for (int y : new int[]{11, 22}) set(level, bb, topSlab(Blocks.PURPUR_SLAB), x, y, z);
			set(level, bb, stair(Blocks.PURPUR_STAIRS, toward(x, z), true), x, 29, z);
			set(level, bb, s(Blocks.END_STONE_BRICKS), x, 30, z);
			set(level, bb, s(Blocks.END_STONE_BRICKS), x, 31, z);
			if ((x + z) % 3 == 0) set(level, bb, s(Blocks.PURPUR_PILLAR), x, 32, z);
		}
		// Pillar ribs down the shaft's flanks, clear of the door and the windows.
		for (int[] r : new int[][]{{4, 4}, {10, 4}, {4, 10}, {10, 10}, {11, 7}, {7, 11}}) fill(level, bb, r[0], 1, r[1], r[0], 28, r[1], s(Blocks.PURPUR_PILLAR));
		// Purple carpet on the ground floor; the lantern room's is laid around the ladder hatch.
		for (int x = 0; x <= 14; x++) for (int z = 0; z <= 14; z++) if (inside(x, z)) {
			set(level, bb, Blocks.CARPET.pick(DyeColor.PURPLE).defaultBlockState(), x, 1, z);
			if (x != 7 || z != 9) set(level, bb, Blocks.CARPET.pick(DyeColor.PURPLE).defaultBlockState(), x, 23, z);
		}
		// The door, windows and lights.
		clear(level, bb, 6, 1, 4, 8, 3, 4);
		for (int x : new int[]{5, 9}) fill(level, bb, x, 1, 3, x, 4, 3, s(Blocks.PURPUR_PILLAR));
		for (int x = 6; x <= 8; x++) set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.NORTH, true), x, 4, 3);
		for (int x : new int[]{5, 9}) set(level, bb, s(Blocks.END_ROD), x, 5, 3);
		for (int x : new int[]{6, 8}) banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.PURPLE), x, 7, 3, Direction.SOUTH);
		for (int y : new int[]{6, 7, 16, 17, 26, 27}) {
			set(level, bb, Blocks.STAINED_GLASS.purple().defaultBlockState(), 4, y, 7);
			set(level, bb, Blocks.STAINED_GLASS.purple().defaultBlockState(), 7, y, 4);
		}
		for (int y : new int[]{1, 12, 23}) { set(level, bb, s(Blocks.END_ROD), 5, y, 7); set(level, bb, s(Blocks.END_ROD), 8, y, 9); }
		hangingLantern(level, bb, 7, 10, 7, 0, true);
		hangingLantern(level, bb, 6, 21, 6, 0, true);
		hangingLantern(level, bb, 7, 29, 7, 1, true);
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
		set(level, bb, s(Blocks.END_ROD), C, 35, C);
		guard(level, bb, EntityTypes.ENDERMAN, 6, 1, 6, List.of(Runes.BOLT, Runes.BLIND), false);
		guard(level, bb, EntityTypes.ENDERMAN, 7, 12, 6, List.of(Runes.BOLT, Runes.PULL), true);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(3, 11, 8, 11, 21, 11), worldBox(3, 22, 3, 11, 30, 11)); }
}
