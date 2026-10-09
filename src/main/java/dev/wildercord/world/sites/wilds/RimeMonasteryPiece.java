package dev.wildercord.world.sites.wilds;

import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * A snowed-in monastery, long empty: a broken wall around a frozen fountain, monks' cells, a chapel with half its roof
 * gone and a bell tower that still rings. Under the chapel a stair leads to the crypt, where a Frost and Time seal
 * keeps the old reliquary shut.
 */
public final class RimeMonasteryPiece extends WildsPiece {
	public RimeMonasteryPiece(int x, int z, Direction facing) { super(WildsSites.RIME_MONASTERY, x, z, 23, 16, 23, facing); }
	public RimeMonasteryPiece(CompoundTag tag) { super(WildsSites.RIME_MONASTERY, tag); }
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		return surface(c, new RimeMonasteryPiece(c.chunkPos().getMinBlockX(), c.chunkPos().getMinBlockZ(), facing(c)), 4);
	}

	private static BlockState stone(int x, int y, int z) {
		int n = noise(x, y, z);
		return s(n < 25 ? Blocks.CRACKED_STONE_BRICKS : n < 40 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS);
	}

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);
		footing(level, bb, 0, 0, 22, 22, s(Blocks.COBBLESTONE));
		for (int x = 0; x <= 22; x++) for (int z = 0; z <= 22; z++) set(level, bb, noise(x, z) < 50 ? s(Blocks.SNOW_BLOCK) : stone(x, 0, z), x, 0, z);
		clear(level, bb, 0, 1, 0, 22, 15, 22);
		for (int x = 0; x <= 22; x++) for (int z = 0; z <= 22; z++) if (noise(z, x) < 45) set(level, bb, s(Blocks.SNOW), x, 1, z);
		// The broken outer wall, with its gate.
		for (int x = 0; x <= 22; x++) for (int z = 0; z <= 22; z++) {
			if (x != 0 && x != 22 && z != 0 && z != 22) continue;
			int h = 1 + noise(x * 3, z * 5) % 4;
			for (int y = 1; y <= h; y++) set(level, bb, stone(x, y, z), x, y, z);
		}
		clear(level, bb, 10, 1, 0, 12, 4, 0);
		for (int x : new int[]{9, 13}) fill(level, bb, x, 1, 0, x, 5, 0, s(Blocks.STONE_BRICKS));
		// Two monks' cells along the west wall.
		for (int z0 : new int[]{2, 7}) {
			room(level, bb, 1, 0, z0, 5, 4, z0 + 4, s(Blocks.STONE_BRICKS));
			fill(level, bb, 2, 0, z0 + 1, 4, 0, z0 + 3, s(Blocks.SPRUCE_PLANKS));
			clear(level, bb, 5, 1, z0 + 2, 5, 2, z0 + 2);
			for (int x = 2; x <= 4; x++) for (int z = z0 + 1; z <= z0 + 3; z++) if (noise(x, z0, z) < 30) set(level, bb, s(Blocks.SNOW), x, 4, z);
			set(level, bb, s(Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.LIGHT_BLUE)), 2, 1, z0 + 3);
		}
		chest(level, bb, 2, 1, 3, WildsSites.RIME_HALL, Direction.EAST, 1);
		set(level, bb, s(Blocks.BARREL), 2, 1, 8);
		set(level, bb, s(Blocks.LANTERN), 4, 1, 3);
		// The frozen fountain.
		for (int x = 11; x <= 17; x++) for (int z = 4; z <= 10; z++) {
			double d = dist(x, z, 14, 7);
			if (d <= 2.6) set(level, bb, d > 1.6 ? s(Blocks.STONE_BRICK_WALL) : s(Blocks.PACKED_ICE), x, 1, z);
		}
		fill(level, bb, 14, 1, 7, 14, 2, 7, s(Blocks.CHISELED_STONE_BRICKS));
		set(level, bb, s(Blocks.BLUE_ICE), 14, 3, 7);
		// The chapel, roof half fallen in.
		room(level, bb, 2, 0, 13, 12, 7, 21, s(Blocks.STONE_BRICKS));
		for (int x = 2; x <= 12; x++) for (int z = 13; z <= 21; z++) for (int y = 1; y <= 6; y++)
			if (x == 2 || x == 12 || z == 13 || z == 21) set(level, bb, y >= 5 && noise(x, y, z) < 30 ? AIR : stone(x, y, z), x, y, z);
		fill(level, bb, 3, 0, 14, 11, 0, 20, s(Blocks.SPRUCE_PLANKS));
		for (int x = 2; x <= 12; x++) for (int z = 13; z <= 21; z++) {
			if (noise(x, 7, z) < 40) set(level, bb, AIR, x, 7, z);
			else set(level, bb, s(Blocks.SPRUCE_PLANKS), x, 7, z);
		}
		clear(level, bb, 6, 1, 13, 7, 3, 13);
		for (int z : new int[]{16, 18}) clear(level, bb, 12, 3, z, 12, 4, z);
		for (int z : new int[]{15, 17}) fill(level, bb, 4, 1, z, 7, 1, z, s(Blocks.SPRUCE_STAIRS).setValue(StairBlock.FACING, Direction.NORTH));
		set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), 7, 1, 20);
		set(level, bb, s(Blocks.LECTERN).setValue(LecternBlock.FACING, Direction.SOUTH), 6, 1, 20);
		set(level, bb, s(Blocks.LANTERN), 7, 2, 20);
		// The crypt, down a stair under the chapel floor.
		room(level, bb, 3, -6, 14, 11, -1, 20, s(Blocks.DEEPSLATE_BRICKS));
		for (int step = 0; step <= 5; step++) {
			clear(level, bb, 9, -step + 1, 14 + step, 10, -step + 3, 14 + step);
			fill(level, bb, 9, -step, 14 + step, 10, -step, 14 + step, s(Blocks.STONE_BRICK_STAIRS).setValue(StairBlock.FACING, Direction.SOUTH));
		}
		for (int z : new int[]{15, 19}) set(level, bb, s(Blocks.SOUL_LANTERN).setValue(LanternBlock.HANGING, true), 8, -2, z);
		// The reliquary behind the seal.
		fill(level, bb, 6, -5, 15, 6, -2, 19, s(Blocks.DEEPSLATE_TILES));
		sealDoorAlong(level, bb, 6, 16, 18, -5, -3, RuneSealBlock.Element.FROST, RuneSealBlock.Element.TIME);
		chest(level, bb, 4, -5, 17, WildsSites.RIME_VAULT, Direction.EAST, 2);
		set(level, bb, s(Blocks.BLUE_ICE), 4, -5, 15);
		set(level, bb, s(Blocks.BLUE_ICE), 4, -5, 19);
		// The bell tower.
		room(level, bb, 15, 0, 15, 20, 15, 20, s(Blocks.STONE_BRICKS));
		for (int x = 15; x <= 20; x++) for (int z = 15; z <= 20; z++) for (int y = 1; y <= 14; y++)
			if (x == 15 || x == 20 || z == 15 || z == 20) set(level, bb, stone(x, y, z), x, y, z);
		fill(level, bb, 16, 12, 16, 19, 12, 19, s(Blocks.SPRUCE_PLANKS));
		for (int y = 1; y <= 12; y++) set(level, bb, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.EAST), 16, y, 19);
		clear(level, bb, 17, 1, 15, 18, 3, 15);
		for (int y = 13; y <= 14; y++) {
			clear(level, bb, 17, y, 15, 18, y, 15); clear(level, bb, 17, y, 20, 18, y, 20);
			clear(level, bb, 15, y, 17, 15, y, 18); clear(level, bb, 20, y, 17, 20, y, 18);
		}
		set(level, bb, s(Blocks.BELL).setValue(BellBlock.FACING, Direction.NORTH).setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING), 18, 14, 17);
		fill(level, bb, 15, 15, 15, 20, 15, 20, s(Blocks.SPRUCE_PLANKS));
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(3, -6, 14, 6, -1, 20)); }
}
