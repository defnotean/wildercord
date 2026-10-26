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
 * berries along the side and beetroot behind. Round it, in a cleared ring hedged with azalea: a glasshouse of tender
 * herbs, a pergola hung with drying bunches and a stone stillroom tower under a steep pyramid roof.
 */
public final class HerbalistPiece extends FarmPiece {
	private static final Block[] FLOWERS = {Blocks.ALLIUM, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY, Blocks.OXEYE_DAISY, Blocks.POPPY, Blocks.DANDELION,
		Blocks.AZURE_BLUET, Blocks.BLUE_ORCHID};

	private static final int MARGIN = 5;

	public HerbalistPiece(int x, int z, Direction facing) { super(FarmSites.HERBALIST, x, 0, z, 17, 13, 17, facing, MARGIN); }
	public HerbalistPiece(CompoundTag tag) { super(FarmSites.HERBALIST, tag); }

	@Override
	protected int margin() { return MARGIN; }

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
		glasshouse(level, bb);
		pergola(level, bb);
		stillroom(level, bb);
		grounds(level, bb);
	}

	/** The ring: an azalea hedge round the edge, a stone-piered gate and lantern-lit path in, flowering trees at the corners. */
	private void grounds(WorldGenLevel level, BoundingBox bb) {
		int lo = -MARGIN, hi = 16 + MARGIN;
		for (int i = lo; i <= hi; i++) {
			for (int[] at : new int[][]{{i, lo}, {i, hi}, {lo, i}, {hi, i}}) {
				if (at[1] == lo && at[0] >= 6 && at[0] <= 10) continue;
				set(level, bb, leaves(noise(at[0], at[1]) < 35 ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES), at[0], 1, at[1]);
				if (noise(at[1], at[0]) < 30) set(level, bb, leaves(Blocks.AZALEA_LEAVES), at[0], 2, at[1]);
			}
		}
		for (int x : new int[]{6, 10}) {
			fill(level, bb, x, 1, lo, x, 3, lo, s(Blocks.MOSSY_STONE_BRICKS));
			set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), x, 4, lo);
			set(level, bb, s(Blocks.LANTERN), x, 5, lo);
		}
		fill(level, bb, 7, 0, lo, 9, 0, -1, s(Blocks.DIRT_PATH));
		fill(level, bb, 8, 0, lo, 8, 0, -1, s(Blocks.MOSSY_COBBLESTONE));
		for (int x : new int[]{6, 10}) { set(level, bb, s(Blocks.SPRUCE_FENCE), x, 1, -2); set(level, bb, s(Blocks.LANTERN), x, 2, -2); }
		for (int[] t : new int[][]{{-2, -2}, {18, -2}, {-2, 18}})
			tree(level, bb, t[0], 1, t[1], 4, s(Blocks.OAK_LOG), leaves(noise(t[0], t[1]) < 50 ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES));
		for (int x = lo + 1; x < hi; x++) for (int z = lo + 1; z < hi; z++) {
			if (x >= 0 && x <= 16 && z >= 0 && z <= 16) continue;
			if (x >= -4 && x <= -1 && z >= 4 && z <= 12 || x >= 17 && x <= 20 && z >= 4 && z <= 12) continue;
			var at = getWorldPos(x, 1, z);
			if (!bb.isInside(at) || !level.getBlockState(at).isAir()) continue;
			if (noise(x, z) < 12) set(level, bb, s(FLOWERS[noise(z, x) % FLOWERS.length]), x, 1, z);
			else if (noise(x, z) < 20) set(level, bb, s(Blocks.SHORT_GRASS), x, 1, z);
		}
	}

	/** West of the cottage: a glasshouse on a stone sill, framed in stripped oak, ferns and tender herbs in its beds. */
	private void glasshouse(WorldGenLevel level, BoundingBox bb) {
		var frame = s(Blocks.STRIPPED_OAK_LOG);
		var glass = s(Blocks.GLASS);
		fill(level, bb, -4, 0, 4, -1, 0, 12, s(Blocks.STONE_BRICKS));
		fill(level, bb, -4, 1, 4, -1, 5, 12, AIR);
		walls(level, bb, -4, 2, 4, -1, 3, 12, glass);
		walls(level, bb, -4, 1, 4, -1, 1, 12, s(Blocks.MOSSY_STONE_BRICKS));
		for (int z = 4; z <= 12; z += 2) for (int x : new int[]{-4, -1}) fill(level, bb, x, 1, z, x, 3, z, frame);
		for (int z : new int[]{4, 12}) for (int x : new int[]{-3, -2}) fill(level, bb, x, 2, z, x, 3, z, glass);
		fill(level, bb, -4, 4, 4, -4, 4, 12, frame);
		fill(level, bb, -1, 4, 4, -1, 4, 12, frame);
		fill(level, bb, -3, 4, 4, -2, 4, 12, glass);
		fill(level, bb, -3, 5, 4, -2, 5, 12, frame);
		for (int z = 6; z <= 10; z += 2) fill(level, bb, -3, 5, z, -2, 5, z, glass);
		fill(level, bb, -3, 0, 5, -3, 0, 11, s(Blocks.PODZOL));
		fill(level, bb, -2, 0, 5, -2, 0, 11, s(Blocks.MOSS_BLOCK));
		for (int z = 5; z <= 11; z++) {
			int n = noise(-3, z);
			set(level, bb, s(n < 30 ? Blocks.FERN : n < 55 ? FLOWERS[n % FLOWERS.length] : Blocks.AZALEA), -3, 1, z);
			if (z % 3 == 0) set(level, bb, s(Blocks.POTTED_FERN), -2, 1, z);
		}
		set(level, bb, s(Blocks.WATER_CAULDRON).setValue(BlockStateProperties.LEVEL_CAULDRON, 3), -2, 1, 11);
		set(level, bb, hangingLantern(), -3, 4, 8);
		door(level, bb, Blocks.OAK_DOOR, -1, 1, 8, Direction.EAST, false);
		set(level, bb, s(Blocks.DIRT_PATH), 0, 0, 8);
	}

	/** East of the chimney: a pergola of spruce posts under slatted slabs, hung with drying bunches over barrels and racks. */
	private void pergola(WorldGenLevel level, BoundingBox bb) {
		fill(level, bb, 17, 0, 4, 20, 0, 12, s(Blocks.COARSE_DIRT));
		for (int x : new int[]{17, 20}) for (int z : new int[]{4, 8, 12}) {
			set(level, bb, s(Blocks.COBBLESTONE), x, 0, z);
			fill(level, bb, x, 1, z, x, 3, z, s(Blocks.STRIPPED_SPRUCE_LOG));
		}
		var beam = s(Blocks.STRIPPED_SPRUCE_LOG).setValue(BlockStateProperties.AXIS, Direction.Axis.Z);
		for (int z = 4; z <= 12; z++) {
			if (z % 2 == 0) fill(level, bb, 18, 4, z, 19, 4, z, s(Blocks.SPRUCE_SLAB));
			set(level, bb, beam, 17, 4, z);
			set(level, bb, beam, 20, 4, z);
			if (z % 2 == 1) for (int x : new int[]{18, 19}) {
				int n = noise(x, z);
				set(level, bb, n < 40 ? leaves(Blocks.AZALEA_LEAVES) : n < 70 ? leaves(Blocks.FLOWERING_AZALEA_LEAVES) : s(Blocks.DRIED_KELP_BLOCK), x, 3, z);
			}
		}
		set(level, bb, s(Blocks.BARREL).setValue(BlockStateProperties.FACING, Direction.UP), 18, 1, 5);
		set(level, bb, s(Blocks.BARREL).setValue(BlockStateProperties.FACING, Direction.UP), 19, 1, 5);
		set(level, bb, s(Blocks.HAY_BLOCK), 18, 1, 11);
		set(level, bb, s(Blocks.COMPOSTER), 19, 1, 11);
		set(level, bb, s(Blocks.FLETCHING_TABLE), 19, 1, 8);
		set(level, bb, s(Blocks.FLOWER_POT), 18, 1, 8);
		set(level, bb, hangingLantern(), 18, 3, 8);
	}

	/** Behind the beetroot: the stillroom, a stone ground floor and a timber upper storey under a pyramid roof. */
	private void stillroom(WorldGenLevel level, BoundingBox bb) {
		int x0 = 16, z0 = 15, x1 = 20, z1 = 19;
		Palette base = Palette.of(Blocks.COBBLESTONE, 4, Blocks.MOSSY_COBBLESTONE, 3, Blocks.STONE_BRICKS, 2);
		var log = s(Blocks.STRIPPED_SPRUCE_LOG);
		fill(level, bb, x0, 0, z0, x1, 0, z1, s(Blocks.SPRUCE_PLANKS));
		fill(level, bb, x0, 1, z0, x1, 7, z1, AIR);
		walls(level, bb, x0, 1, z0, x1, 3, z1, base);
		walls(level, bb, x0, 4, z0, x1, 7, z1, s(Blocks.SPRUCE_PLANKS));
		for (int x : new int[]{x0, x1}) for (int z : new int[]{z0, z1}) fill(level, bb, x, 1, z, x, 7, z, log);
		fill(level, bb, x0, 4, z0, x1, 4, z0, log); fill(level, bb, x0, 4, z1, x1, 4, z1, log);
		fill(level, bb, x0, 4, z0, x0, 4, z1, log); fill(level, bb, x1, 4, z0, x1, 4, z1, log);
		// Eaves a block out all round, then the pyramid climbing to a spire.
		for (int i = -1; i < 3; i++) {
			int a0 = x0 + i, b0 = z0 + i, a1 = x1 - i, b1 = z1 - i, y = 8 + i + 1;
			for (int x = a0; x <= a1; x++) for (int z = b0; z <= b1; z++) {
				if (x != a0 && x != a1 && z != b0 && z != b1) { set(level, bb, s(Blocks.SPRUCE_PLANKS), x, y, z); continue; }
				Direction up = z == b0 ? Direction.NORTH : z == b1 ? Direction.SOUTH : x == a0 ? Direction.EAST : Direction.WEST;
				set(level, bb, stair(Blocks.SPRUCE_STAIRS, up, false), x, y, z);
			}
		}
		set(level, bb, s(Blocks.SPRUCE_PLANKS), 18, 11, 17);
		set(level, bb, s(Blocks.LIGHTNING_ROD.waxed().unaffected()), 18, 12, 17);
		for (int[] w : new int[][]{{18, z0}, {18, z1}, {x1, 16}}) {
			set(level, bb, s(Blocks.GLASS_PANE), w[0], 2, w[1]);
			set(level, bb, s(Blocks.GLASS_PANE), w[0], 5, w[1]);
			set(level, bb, s(Blocks.GLASS_PANE), w[0], 6, w[1]);
		}
		door(level, bb, Blocks.SPRUCE_DOOR, x0, 1, 17, Direction.WEST, false);
		set(level, bb, s(Blocks.LANTERN), x0 - 1, 1, 16);
		fill(level, bb, 14, 0, 17, x0 - 1, 0, 17, s(Blocks.DIRT_PATH));
		set(level, bb, s(Blocks.BREWING_STAND), 19, 1, 18);
		set(level, bb, s(Blocks.BREWING_STAND), 17, 1, 18);
		set(level, bb, s(Blocks.BOOKSHELF), 19, 1, 16); set(level, bb, s(Blocks.BOOKSHELF), 19, 2, 16);
		set(level, bb, s(Blocks.WATER_CAULDRON).setValue(BlockStateProperties.LEVEL_CAULDRON, 2), 18, 1, 18);
		fill(level, bb, x0 + 1, 4, z0 + 1, x1 - 2, 4, z1 - 1, s(Blocks.SPRUCE_PLANKS));
		ladder(level, bb, 19, 1, 4, 17, Direction.WEST);
		set(level, bb, s(Blocks.HAY_BLOCK), 17, 5, 16);
		set(level, bb, leaves(Blocks.FLOWERING_AZALEA_LEAVES), 17, 5, 18);
		set(level, bb, hangingLantern(), 18, 7, 17);
	}
}
