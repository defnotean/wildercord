package dev.wildercord.world.sites.wilds;

import dev.wildercord.content.SacredPigQuest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/** A tiny forest village where a sacred pig is sick and the cure costs everything you own. */
public final class SacredPigVillagePiece extends WildsPiece {
	private static final int W = 24, H = 10, D = 24;

	public SacredPigVillagePiece(int x, int z, Direction facing) {
		super(WildsSites.SACRED_PIG_VILLAGE, x, z, W, H, D, facing);
	}

	public SacredPigVillagePiece(CompoundTag tag) {
		super(WildsSites.SACRED_PIG_VILLAGE, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		return surface(c, new SacredPigVillagePiece(c.chunkPos().getMinBlockX(), c.chunkPos().getMinBlockZ(), facing(c)), 6);
	}

	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of();
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		// Clearing / ground
		footing(level, bb, 0, 0, W - 1, D - 1, s(Blocks.DIRT));
		fill(level, bb, 0, 0, 0, W - 1, 0, D - 1, s(Blocks.GRASS_BLOCK));
		clear(level, bb, 0, 1, 0, W - 1, H - 1, D - 1);
		// dirt path
		for (int z = 4; z <= 19; z++) set(level, bb, s(Blocks.COARSE_DIRT), 12, 0, z);
		for (int x = 6; x <= 18; x++) set(level, bb, s(Blocks.COARSE_DIRT), x, 0, 12);

		// Small hut (priest house)
		hut(level, bb, 3, 1, 3, 9, 5, 9);
		// Small hut (medicine shack)
		hut(level, bb, 14, 1, 3, 20, 5, 8);

		// Pig pen
		pen(level, bb, 4, 1, 14, 10, 1, 20);
		// Campfire / offering spot
		set(level, bb, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 12, 1, 10);
		for (int[] l : new int[][]{{11, 10}, {13, 10}, {12, 9}, {12, 11}}) {
			set(level, bb, s(Blocks.OAK_FENCE), l[0], 1, l[1]);
			set(level, bb, s(Blocks.LANTERN), l[0], 2, l[1]);
		}

		// Flowers and flavor
		for (int[] f : new int[][]{{2, 12}, {3, 11}, {21, 12}, {20, 14}, {11, 3}, {13, 3}, {6, 21}, {18, 21}}) {
			set(level, bb, s((f[0] + f[1]) % 2 == 0 ? Blocks.POPPY : Blocks.DANDELION), f[0], 1, f[1]);
		}
		// A sign of desperation
		set(level, bb, s(Blocks.HAY_BLOCK), 16, 1, 5);
		set(level, bb, s(Blocks.HAY_BLOCK), 17, 1, 5);
		set(level, bb, s(Blocks.BARREL), 18, 1, 6);

		// Loot chest in priest hut
		chest(level, bb, 5, 1, 5, WildsSites.SACRED_PIG_HALL, Direction.SOUTH, 1);

		// Spawn sick sacred pig and priest (once, in the chunk containing the pen center)
		BlockPos pigPos = getWorldPos(7, 1, 17);
		if (bb.isInside(pigPos) && level.getLevel() instanceof ServerLevel sl) {
			SacredPigQuest.spawnNpc(sl, pigPos.getX(), pigPos.getY(), pigPos.getZ());
		}
	}

	private void hut(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1) {
		fill(level, bb, x0, y0, z0, x1, y1, z1, s(Blocks.OAK_PLANKS));
		clear(level, bb, x0 + 1, y0, z0 + 1, x1 - 1, y1 - 1, z1 - 1);
		fill(level, bb, x0, y0 - 1, z0, x1, y0 - 1, z1, s(Blocks.COBBLESTONE));
		// door
		clear(level, bb, (x0 + x1) / 2, y0, z1, (x0 + x1) / 2, y0 + 1, z1);
		// roof
		for (int i = 0; i <= (x1 - x0) / 2 + 1; i++) {
			int ry = y1 + i;
			int rx0 = x0 - 1 + i;
			int rx1 = x1 + 1 - i;
			if (rx0 > rx1) break;
			for (int x = rx0; x <= rx1; x++) {
				for (int z = z0 - 1; z <= z1 + 1; z++) {
					set(level, bb, s(Blocks.SPRUCE_PLANKS), x, ry, z);
				}
			}
		}
		// interior
		set(level, bb, s(Blocks.CRAFTING_TABLE), x0 + 1, y0, z0 + 1);
		set(level, bb, s(Blocks.LANTERN), (x0 + x1) / 2, y1 - 1, (z0 + z1) / 2);
	}

	private void pen(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
				if (edge) set(level, bb, s(Blocks.OAK_FENCE), x, y0, z);
				else if ((x + z) % 3 == 0) set(level, bb, s(Blocks.MUD), x, y0 - 1, z);
			}
		}
		clear(level, bb, (x0 + x1) / 2, y0, z0, (x0 + x1) / 2, y0, z0); // gate gap
		set(level, bb, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.SOUTH), (x0 + x1) / 2, y0, z0);
		set(level, bb, s(Blocks.WATER_CAULDRON), x1 - 1, y0, z1 - 1);
	}
}
