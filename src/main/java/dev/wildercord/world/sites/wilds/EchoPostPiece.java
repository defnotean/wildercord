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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * A listening post on the deep dark's floor, where someone once sat on wool and wrote down what the sensors heard.
 * There are sensors and crystals but never a shrieker, and the post keeps its distance from ancient cities (see its
 * structure set). Its records sit behind a Wind and Void seal.
 */
public final class EchoPostPiece extends WildsPiece {
	public EchoPostPiece(int x, int z, Direction facing) { super(WildsSites.ECHO_POST, x, z, 17, 10, 17, facing); }
	public EchoPostPiece(CompoundTag tag) { super(WildsSites.ECHO_POST, tag); }
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		return cave(c, new EchoPostPiece(c.chunkPos().getMinBlockX(), c.chunkPos().getMinBlockZ(), facing(c)), 8, -50, -10, 6);
	}

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);
		footing(level, bb, 0, 0, 16, 16, s(Blocks.COBBLED_DEEPSLATE));
		fill(level, bb, 0, 0, 0, 16, 0, 16, s(Blocks.DEEPSLATE_TILES));
		clear(level, bb, 0, 1, 0, 16, 8, 16);
		// The post.
		room(level, bb, 3, 0, 3, 13, 6, 13, s(Blocks.DEEPSLATE_BRICKS));
		fill(level, bb, 4, 0, 4, 12, 0, 12, Blocks.WOOL.gray().defaultBlockState());
		for (int[] p : new int[][]{{3, 3}, {13, 3}, {3, 13}, {13, 13}}) fill(level, bb, p[0], 0, p[1], p[0], 7, p[1], s(Blocks.POLISHED_DEEPSLATE));
		clear(level, bb, 7, 1, 3, 9, 3, 3);
		for (int x : new int[]{6, 10}) set(level, bb, s(Blocks.SOUL_LANTERN), x, 1, 2);
		for (int x = 4; x <= 12; x++) for (int z = 4; z <= 12; z++) if (noise(x, z) < 18) set(level, bb, s(Blocks.AMETHYST_BLOCK), x, 6, z);
		for (int[] p : new int[][]{{5, 5}, {11, 5}, {5, 11}, {11, 11}, {8, 8}}) {
			set(level, bb, s(Blocks.AMETHYST_BLOCK), p[0], 6, p[1]);
			set(level, bb, s(Blocks.AMETHYST_CLUSTER), p[0], 7, p[1]);
		}
		// What they listened with.
		set(level, bb, s(Blocks.SCULK_SENSOR), 4, 1, 4);
		set(level, bb, s(Blocks.SCULK_SENSOR), 12, 1, 4);
		set(level, bb, s(Blocks.CALIBRATED_SCULK_SENSOR), 12, 1, 8);
		set(level, bb, s(Blocks.AMETHYST_CLUSTER), 4, 1, 6);
		set(level, bb, s(Blocks.LECTERN).setValue(LecternBlock.FACING, Direction.SOUTH), 8, 1, 7);
		set(level, bb, s(Blocks.SOUL_LANTERN).setValue(LanternBlock.HANGING, true), 8, 5, 6);
		chest(level, bb, 4, 1, 8, WildsSites.ECHO_HALL, Direction.EAST, 1);
		// The records, behind the seal.
		fill(level, bb, 4, 1, 10, 12, 5, 10, s(Blocks.DEEPSLATE_TILES));
		sealDoorAcross(level, bb, 10, 7, 9, 1, 2, RuneSealBlock.Element.WIND, RuneSealBlock.Element.VOID);
		chest(level, bb, 8, 1, 12, WildsSites.ECHO_VAULT, Direction.SOUTH, 2);
		set(level, bb, s(Blocks.AMETHYST_CLUSTER), 5, 1, 12);
		set(level, bb, s(Blocks.AMETHYST_CLUSTER), 11, 1, 12);
		set(level, bb, s(Blocks.SOUL_LANTERN), 6, 1, 11);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(3, 0, 10, 13, 6, 13)); }
}
