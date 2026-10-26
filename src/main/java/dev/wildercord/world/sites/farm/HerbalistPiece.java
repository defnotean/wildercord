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
		clearCanopy(level, bb);
		site(level, bb, s(Blocks.GRASS_BLOCK), 1);
		fill(level, bb, 8, 0, 0, 8, 0, 4, s(Blocks.DIRT_PATH));
		// Flower beds either side of the path.
		for (int x = 2; x <= 14; x++) for (int z = 1; z <= 3; z++) {
			if (x >= 7 && x <= 9) continue;
			set(level, bb, s(Blocks.PODZOL), x, 0, z);
			set(level, bb, s(FLOWERS[noise(x, z) % FLOWERS.length]), x, 1, z);
		}
		// A flowering hedge along the front, and a leafy arbour over the path.
		for (int x = 0; x <= 16; x++) if (x < 7 || x > 9) set(level, bb, leaves(noise(x, 0) < 40 ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES), x, 1, 0);
		for (int x : new int[]{7, 9}) fill(level, bb, x, 1, 0, x, 3, 0, s(Blocks.STRIPPED_OAK_LOG));
		fill(level, bb, 6, 4, 0, 10, 4, 0, leaves(Blocks.FLOWERING_AZALEA_LEAVES));
		set(level, bb, leaves(Blocks.AZALEA_LEAVES), 8, 5, 0);
		hangingLantern(level, bb, 8, 3, 0, 0, false);
		for (int z = 5; z <= 13; z += 2) set(level, bb, s(Blocks.SWEET_BERRY_BUSH).setValue(BlockStateProperties.AGE_3, 3), 1, 1, z);
		// Beetroot behind the house, round a little pool.
		for (int x = 3; x <= 13; x++) for (int z = 14; z <= 16; z++) {
			set(level, bb, farmland(true), x, 0, z);
			set(level, bb, s(Blocks.BEETROOTS).setValue(BlockStateProperties.AGE_3, 1 + noise(x, z) % 3), x, 1, z);
		}
		set(level, bb, s(Blocks.WATER), 8, 0, 15);
		set(level, bb, AIR, 8, 1, 15);
		set(level, bb, s(Blocks.LILY_PAD), 8, 1, 15);
		// The cottage: a fieldstone footing, oak infill in a stripped-log frame, a steep spruce roof.
		var planks = s(Blocks.OAK_PLANKS);
		var log = s(Blocks.STRIPPED_OAK_LOG);
		Palette footing = Palette.of(Blocks.COBBLESTONE, 5, Blocks.MOSSY_COBBLESTONE, 4, Blocks.STONE, 1);
		fill(level, bb, 3, 0, 5, 13, 0, 13, s(Blocks.SPRUCE_PLANKS));
		walls(level, bb, 3, 1, 5, 13, 4, 13, planks);
		walls(level, bb, 3, 1, 5, 13, 1, 13, footing);
		for (int x : new int[]{3, 13}) for (int z : new int[]{5, 13}) fill(level, bb, x, 1, z, x, 4, z, log);
		for (int x : new int[]{3, 13}) fill(level, bb, x, 2, 9, x, 4, 9, log);
		for (int x : new int[]{6, 10}) fill(level, bb, x, 2, 13, x, 4, 13, log);
		fill(level, bb, 3, 4, 5, 13, 4, 5, log); fill(level, bb, 3, 4, 13, 13, 4, 13, log);
		fill(level, bb, 3, 4, 5, 3, 4, 13, log); fill(level, bb, 13, 4, 5, 13, 4, 13, log);
		for (int i = 0; i < 6; i++) {
			int y = 5 + i;
			fill(level, bb, 2 + i, y, 4, 2 + i, y, 14, stairs(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
			fill(level, bb, 14 - i, y, 4, 14 - i, y, 14, stairs(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
			fill(level, bb, 3 + i, y, 5, 13 - i, y, 5, planks);
			fill(level, bb, 3 + i, y, 13, 13 - i, y, 13, planks);
		}
		fill(level, bb, 8, 10, 4, 8, 10, 14, s(Blocks.SPRUCE_PLANKS));
		fill(level, bb, 8, 11, 4, 8, 11, 14, s(Blocks.SPRUCE_SLAB));
		set(level, bb, s(Blocks.SPRUCE_FENCE), 8, 12, 4); set(level, bb, s(Blocks.SPRUCE_FENCE), 8, 12, 14);
		// Corbels under the long eaves.
		for (int z = 4; z <= 14; z++) {
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, true), 2, 4, z);
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 14, 4, z);
		}
		// Cross windows in both gables, framed in stripped logs.
		for (int z : new int[]{5, 13}) {
			fill(level, bb, 7, 6, z, 9, 8, z, log);
			fill(level, bb, 8, 6, z, 8, 8, z, s(Blocks.GLASS));
			set(level, bb, s(Blocks.GLASS), 7, 7, z); set(level, bb, s(Blocks.GLASS), 9, 7, z);
		}
		for (int z : new int[]{8, 10}) { set(level, bb, s(Blocks.GLASS), 3, 2, z); set(level, bb, s(Blocks.GLASS), 13, 2, z); }
		for (int x : new int[]{5, 11}) { set(level, bb, s(Blocks.GLASS), x, 2, 5); set(level, bb, s(Blocks.GLASS), x, 2, 13); }
		// Flowering shrubs under the windows.
		for (int x : new int[]{5, 11}) set(level, bb, s(Blocks.FLOWERING_AZALEA), x, 1, 4);
		for (int z : new int[]{8, 10}) { set(level, bb, s(Blocks.FLOWERING_AZALEA), 2, 1, z); set(level, bb, s(Blocks.AZALEA), 14, 1, z); }
		// A fieldstone chimney up the east side, smoking.
		Palette chimney = Palette.of(Blocks.COBBLESTONE, 4, Blocks.MOSSY_COBBLESTONE, 2, Blocks.STONE_BRICKS, 2);
		fill(level, bb, 14, 1, 6, 15, 9, 7, chimney);
		fill(level, bb, 14, 10, 6, 15, 10, 7, s(Blocks.BRICKS));
		set(level, bb, s(Blocks.CAMPFIRE), 15, 11, 6);
		// The door under a little pent hood on posts, a lantern hung beside it.
		for (int x : new int[]{7, 9}) fill(level, bb, x, 1, 4, x, 2, 4, s(Blocks.SPRUCE_FENCE));
		for (int x = 6; x <= 10; x++) set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false), x, 3, 4);
		door(level, bb, Blocks.OAK_DOOR, 8, 1, 5, Direction.NORTH, true);
		set(level, bb, s(Blocks.LANTERN), 6, 1, 4);
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
