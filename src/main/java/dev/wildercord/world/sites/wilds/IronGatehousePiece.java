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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * The last gate of a fortress the badlands swallowed: two towers and a gate hall with its portcullis still down.
 * Shackle archers hold the rampart; the right tower is the old armoury, shut by an Earth and Storm seal, with a
 * tremor adept inside.
 */
public final class IronGatehousePiece extends WildsPiece {
	public IronGatehousePiece(int x, int z, Direction facing) { super(WildsSites.IRON_GATEHOUSE, x, z, 25, 20, 15, facing); }
	public IronGatehousePiece(CompoundTag tag) { super(WildsSites.IRON_GATEHOUSE, tag); }
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		return surface(c, new IronGatehousePiece(c.chunkPos().getMinBlockX(), c.chunkPos().getMinBlockZ(), facing(c)), 6);
	}

	private static BlockState stone(int x, int y, int z) {
		int n = noise(x, y, z);
		return n < 14 ? Blocks.DYED_TERRACOTTA.red().defaultBlockState() : n < 32 ? s(Blocks.CRACKED_STONE_BRICKS) : s(Blocks.STONE_BRICKS);
	}

	private void walls(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1) {
		for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++)
			if (x == x0 || x == x1 || z == z0 || z == z1 || y == y0 || y == y1) set(level, bb, stone(x, y, z), x, y, z);
			else set(level, bb, AIR, x, y, z);
	}

	private void crenels(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1, int y) {
		for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++)
			if ((x == x0 || x == x1 || z == z0 || z == z1) && (x + z) % 2 == 0) set(level, bb, s(Blocks.STONE_BRICKS), x, y, z);
	}

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);
		footing(level, bb, 0, 0, 24, 14, s(Blocks.TERRACOTTA));
		fill(level, bb, 0, 0, 0, 24, 0, 14, s(Blocks.TERRACOTTA));
		clear(level, bb, 0, 1, 0, 24, 19, 14);
		fill(level, bb, 9, 0, 0, 15, 0, 14, s(Blocks.STONE_BRICKS));
		// The left tower: a button door, a ladder to the upper room and out onto the rampart and the roof.
		walls(level, bb, 0, 0, 1, 6, 16, 9);
		fill(level, bb, 1, 10, 2, 5, 10, 8, s(Blocks.SPRUCE_PLANKS));
		for (int y = 1; y <= 16; y++) set(level, bb, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.EAST), 1, y, 5);
		BlockState door = s(Blocks.IRON_DOOR).setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
		set(level, bb, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 3, 1, 1);
		set(level, bb, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3, 2, 1);
		set(level, bb, s(Blocks.STONE_BUTTON).setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, Direction.SOUTH), 2, 2, 0);
		set(level, bb, s(Blocks.STONE_BUTTON).setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, Direction.NORTH), 2, 2, 2);
		set(level, bb, s(Blocks.LANTERN), 5, 1, 8);
		set(level, bb, s(Blocks.LANTERN), 5, 11, 8);
		crenels(level, bb, 0, 1, 6, 9, 17);
		// The gate hall, portcullis down at the front, an open arch at the back, the rampart on its roof.
		walls(level, bb, 7, 0, 2, 17, 10, 8);
		fill(level, bb, 8, 0, 3, 16, 0, 7, s(Blocks.STONE_BRICKS));
		clear(level, bb, 10, 1, 2, 14, 5, 2);
		fill(level, bb, 10, 1, 2, 14, 5, 2, s(Blocks.IRON_BARS));
		clear(level, bb, 10, 1, 8, 14, 5, 8);
		clear(level, bb, 6, 1, 5, 7, 2, 5);
		clear(level, bb, 6, 11, 5, 6, 12, 5);
		crenels(level, bb, 7, 2, 17, 8, 11);
		set(level, bb, AIR, 7, 11, 5);
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 12, 9, 5);
		set(level, bb, s(Blocks.CHIPPED_ANVIL), 16, 1, 3);
		chest(level, bb, 8, 1, 7, WildsSites.IRON_HALL, Direction.EAST, 1);
		// The right tower, the armoury, behind its seal.
		walls(level, bb, 18, 0, 1, 24, 8, 9);
		fill(level, bb, 18, 9, 1, 24, 12, 9, s(Blocks.STONE_BRICKS));
		crenels(level, bb, 18, 1, 24, 9, 13);
		clear(level, bb, 17, 1, 4, 17, 3, 6);
		sealDoorAlong(level, bb, 18, 4, 6, 1, 3, RuneSealBlock.Element.EARTH, RuneSealBlock.Element.STORM);
		fill(level, bb, 19, 0, 2, 23, 0, 8, s(Blocks.POLISHED_ANDESITE));
		for (int z : new int[]{3, 7}) fill(level, bb, 23, 1, z, 23, 2, z, s(Blocks.IRON_BARS));
		set(level, bb, s(Blocks.CHIPPED_ANVIL), 23, 1, 2);
		set(level, bb, s(Blocks.LANTERN), 23, 1, 8);
		chest(level, bb, 22, 1, 5, WildsSites.IRON_VAULT, Direction.WEST, 2);
		// Rubble in the yard behind.
		for (int x = 0; x <= 24; x++) for (int z = 10; z <= 14; z++) if (noise(x, z) < 12 && (x < 9 || x > 15))
			set(level, bb, stone(x, 1, z), x, 1, z);
		guard(level, bb, EntityTypes.SKELETON, 9, 11, 5, List.of(Runes.BOLT, Runes.SHACKLE), false);
		guard(level, bb, EntityTypes.SKELETON, 15, 11, 5, List.of(Runes.BOLT, Runes.SHACKLE), false);
		guard(level, bb, EntityTypes.VINDICATOR, 20, 1, 3, List.of(Runes.BURST, Runes.TREMOR), true);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(18, 0, 1, 24, 8, 9)); }
}
