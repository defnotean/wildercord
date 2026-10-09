package dev.wildercord.world.sites.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A steep-roofed cottage among flower beds: a brewing bench and cauldron below, herbs drying in the loft above,
 * berries along the side and beetroot behind.
 */
public final class HerbalistPiece extends FarmPiece {
	private static final Block[] FLOWERS = {Blocks.ALLIUM, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.OXEYE_DAISY, Blocks.POPPY, Blocks.DANDELION,
		Blocks.AZURE_BLUET, Blocks.BLUE_ORCHID};

	public HerbalistPiece(int x, int z, Direction facing) { super(FarmSites.HERBALIST, x, 0, z, 17, 13, 17, facing); }
	public HerbalistPiece(CompoundTag tag) { super(FarmSites.HERBALIST, tag); }

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		site(level, bb, s(Blocks.GRASS_BLOCK), 1);
		fill(level, bb, 8, 0, 0, 8, 0, 4, s(Blocks.DIRT_PATH));
		// Flower beds either side of the path.
		for (int x = 2; x <= 14; x++) for (int z = 1; z <= 3; z++) {
			if (x >= 7 && x <= 9) continue;
			set(level, bb, s(Blocks.PODZOL), x, 0, z);
			set(level, bb, s(FLOWERS[noise(x, z) % FLOWERS.length]), x, 1, z);
		}
		for (int z = 5; z <= 13; z += 2) set(level, bb, s(Blocks.SWEET_BERRY_BUSH).setValue(BlockStateProperties.AGE_3, 3), 1, 1, z);
		// Beetroot behind the house, round a little pool.
		for (int x = 3; x <= 13; x++) for (int z = 14; z <= 16; z++) {
			set(level, bb, farmland(true), x, 0, z);
			set(level, bb, s(Blocks.BEETROOTS).setValue(BlockStateProperties.AGE_3, 1 + noise(x, z) % 3), x, 1, z);
		}
		set(level, bb, s(Blocks.WATER), 8, 0, 15);
		set(level, bb, AIR, 8, 1, 15);
		// The cottage.
		var planks = s(Blocks.OAK_PLANKS);
		fill(level, bb, 3, 0, 5, 13, 0, 13, s(Blocks.SPRUCE_PLANKS));
		walls(level, bb, 3, 1, 5, 13, 4, 13, planks);
		for (int x : new int[]{3, 13}) for (int z : new int[]{5, 13}) fill(level, bb, x, 1, z, x, 4, z, s(Blocks.STRIPPED_OAK_LOG));
		for (int i = 0; i < 6; i++) {
			int y = 5 + i;
			fill(level, bb, 2 + i, y, 4, 2 + i, y, 14, stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
			fill(level, bb, 14 - i, y, 4, 14 - i, y, 14, stairs(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
			fill(level, bb, 3 + i, y, 5, 13 - i, y, 5, planks);
			fill(level, bb, 3 + i, y, 13, 13 - i, y, 13, planks);
		}
		fill(level, bb, 8, 10, 4, 8, 10, 14, s(Blocks.SPRUCE_PLANKS));
		fill(level, bb, 8, 11, 4, 8, 11, 14, s(Blocks.SPRUCE_SLAB));
		set(level, bb, s(Blocks.GLASS), 8, 7, 5);
		set(level, bb, s(Blocks.GLASS), 8, 7, 13);
		for (int z : new int[]{8, 10}) { set(level, bb, s(Blocks.GLASS), 3, 2, z); set(level, bb, s(Blocks.GLASS), 13, 2, z); }
		for (int x : new int[]{5, 11}) { set(level, bb, s(Blocks.GLASS), x, 2, 5); set(level, bb, s(Blocks.GLASS), x, 2, 13); }
		door(level, bb, Blocks.OAK_DOOR, 8, 1, 5, Direction.NORTH, true);
		// The bench: a brewing stand, a full cauldron, the herbalist's chest, a pot of flowers.
		set(level, bb, s(Blocks.BREWING_STAND), 5, 1, 12);
		set(level, bb, s(Blocks.WATER_CAULDRON).setValue(BlockStateProperties.LEVEL_CAULDRON, 3), 4, 1, 12);
		set(level, bb, s(Blocks.CRAFTING_TABLE), 6, 1, 12);
		chest(level, bb, 4, 1, 7, FarmSites.HERBALIST_BENCH, Direction.EAST, 1);
		set(level, bb, s(Blocks.BARREL).setValue(BlockStateProperties.FACING, Direction.UP), 4, 1, 9);
		set(level, bb, s(Blocks.POTTED_ALLIUM), 4, 2, 9);
		set(level, bb, s(Blocks.COMPOSTER), 11, 1, 12);
		set(level, bb, hangingLantern(), 8, 4, 9);
		// The drying loft, up the ladder on the east wall.
		fill(level, bb, 4, 5, 6, 12, 5, 12, s(Blocks.SPRUCE_PLANKS));
		ladder(level, bb, 12, 1, 6, 9, Direction.WEST);
		set(level, bb, s(Blocks.HAY_BLOCK), 5, 6, 7);
		set(level, bb, s(Blocks.DRIED_KELP_BLOCK), 5, 6, 8);
		set(level, bb, leaves(Blocks.AZALEA_LEAVES), 6, 6, 11);
		set(level, bb, leaves(Blocks.FLOWERING_AZALEA_LEAVES), 5, 6, 11);
		set(level, bb, s(Blocks.HAY_BLOCK), 7, 6, 11);
		chest(level, bb, 11, 6, 7, FarmSites.HERBALIST_LOFT, Direction.WEST, 2);
	}
}
