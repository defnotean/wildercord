package dev.wildercord.world.sites.wilds;

import dev.wildercord.wildlife.SakuraVillager;
import dev.wildercord.wildlife.ShrineCat;
import dev.wildercord.wildlife.ShrineCatContent;
import dev.wildercord.wildlife.ShrineCatRules;
import dev.wildercord.world.dungeons.JapaneseArchitecture;
import dev.wildercord.world.dungeons.JapaneseArchitecture.BlockPlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * Peaceful Japanese Sakura Village modeled 1:1 from the village reference sheet: perimeter stone &amp;
 * bamboo garden wall with corner ishidoro lanterns, grand Myojin Torii gate, koi brook crossed by an
 * arched crimson Taiko-bashi drum bridge, Matcha Tea House with raised engawa veranda &amp; chabudai
 * tables, Cat Shrine Pavilion with chozuya basin, shimenawa rope, shide banners, Suzu bell &amp;
 * four elevated cat shrines, weeping sakura blossom trees, Tea Master, Shrine Maiden, and all four cats.
 */
public final class SakuraVillagePiece extends WildsPiece {
	private static final int WIDTH = 45, HEIGHT = 20, DEPTH = 50;

	public SakuraVillagePiece(int x, int z, Direction facing) {
		super(WildsSites.SAKURA_VILLAGE, x, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public SakuraVillagePiece(CompoundTag tag) {
		super(WildsSites.SAKURA_VILLAGE, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		return surface(c, new SakuraVillagePiece(c.chunkPos().getMinBlockX(), c.chunkPos().getMinBlockZ(), facing(c)), 10);
	}

	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of();
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		BlockPlacer p = (x, y, z, st) -> set(level, bb, st, x, y, z);

		footing(level, bb, 0, 0, WIDTH - 1, DEPTH - 1, s(Blocks.DIRT));
		fill(level, bb, 0, 0, 0, WIDTH - 1, 0, DEPTH - 1, s(Blocks.GRASS_BLOCK));
		clear(level, bb, 0, 1, 0, WIDTH - 1, HEIGHT - 1, DEPTH - 1);

		perimeterWall(level, bb, p);
		gardenPathAndStream(level, bb, p);
		JapaneseArchitecture.toriiGate(p, 22, 1, 4, 3, 6, true);
		for (int[] l : new int[][]{{17, 2}, {27, 2}, {17, 8}, {27, 8}}) {
			JapaneseArchitecture.ishidoro(p, l[0], 1, l[1]);
		}

		teaHouse(level, bb, p);
		catShrine(level, bb, p);

		for (int[] t : new int[][]{{6, 6, 4}, {38, 6, 4}, {7, 41, 5}, {22, 43, 5}, {37, 41, 5}}) {
			JapaneseArchitecture.weepingCherryTree(p, t[0], 1, t[1], t[2], true);
		}

		// Peaceful Tea Master, Shrine Maiden & all four cats enjoying the village grounds.
		spawnVillager(level, bb, 10, 2, 28, SakuraVillager.TEA_MASTER);
		spawnVillager(level, bb, 30, 2, 27, SakuraVillager.SHRINE_MAIDEN);
		spawnVillageCat(level, bb, 18, 2, 26, ShrineCatRules.Variant.YUKI);
		spawnVillageCat(level, bb, 11, 2, 25, ShrineCatRules.Variant.ODIE);
		spawnVillageCat(level, bb, 32, 3, 24, ShrineCatRules.Variant.FREYA);
		spawnVillageCat(level, bb, 26, 1, 29, ShrineCatRules.Variant.KURO);
	}

	private void perimeterWall(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState wall = s(Blocks.STONE_BRICKS);
		BlockState mossy = s(Blocks.MOSSY_STONE_BRICKS);
		BlockState cap = JapaneseArchitecture.slab(Blocks.STONE_BRICK_SLAB, SlabType.BOTTOM);
		BlockState bamboo = s(Blocks.BAMBOO_BLOCK);

		for (int x = 1; x <= WIDTH - 2; x++) {
			if (x >= 19 && x <= 25) {
				continue;
			}
			set(level, bb, noise(x, 1) < 35 ? mossy : wall, x, 1, 1);
			set(level, bb, cap, x, 2, 1);
			set(level, bb, noise(x, 48) < 35 ? mossy : wall, x, 1, DEPTH - 2);
			set(level, bb, bamboo, x, 2, DEPTH - 2);
		}
		for (int z = 2; z < DEPTH - 2; z++) {
			set(level, bb, noise(1, z) < 35 ? mossy : wall, 1, 1, z);
			set(level, bb, z % 2 == 0 ? bamboo : cap, 1, 2, z);
			set(level, bb, noise(43, z) < 35 ? mossy : wall, WIDTH - 2, 1, z);
			set(level, bb, z % 2 == 0 ? bamboo : cap, WIDTH - 2, 2, z);
		}
		for (int[] c : new int[][]{{1, 1}, {WIDTH - 2, 1}, {1, DEPTH - 2}, {WIDTH - 2, DEPTH - 2}}) {
			JapaneseArchitecture.ishidoro(p, c[0], 1, c[1]);
		}
		for (int x = 2; x < WIDTH - 2; x++) {
			for (int z = 2; z < DEPTH - 2; z++) {
				if (noise(x, z) < 16) {
					set(level, bb, s(Blocks.PINK_PETALS), x, 1, z);
				} else if (noise(x + 9, z - 5) < 5) {
					set(level, bb, s(Blocks.MOSS_CARPET), x, 1, z);
				}
			}
		}
	}

	private void gardenPathAndStream(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		for (int z = 0; z < DEPTH - 4; z++) {
			for (int x = 20; x <= 24; x++) {
				BlockState stone = noise(x, z) < 30 ? s(Blocks.MOSSY_STONE_BRICKS)
					: (noise(x + 4, z) < 40 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.SMOOTH_STONE));
				set(level, bb, stone, x, 0, z);
			}
		}
		// Landscaped koi stream with mossy boulder banks, sea lanterns, lily pads & bamboo clusters.
		for (int x = 2; x <= WIDTH - 3; x++) {
			for (int z = 11; z <= 15; z++) {
				set(level, bb, s(Blocks.SAND), x, -1, z);
				set(level, bb, s(Blocks.WATER), x, 0, z);
				set(level, bb, AIR, x, 1, z);
				if ((x == 8 || x == 16 || x == 28 || x == 36) && z == 13) {
					set(level, bb, s(Blocks.SEA_LANTERN), x, -1, z);
				} else if (noise(x, z) < 14 && Math.abs(x - 22) > 4) {
					set(level, bb, s(Blocks.LILY_PAD), x, 1, z);
				}
			}
			if (Math.abs(x - 22) > 3 && noise(x * 3, 10) < 32) {
				set(level, bb, noise(x, 2) < 50 ? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.MOSS_BLOCK), x, 1, 10);
			}
			if (Math.abs(x - 22) > 3 && noise(x * 5, 16) < 32) {
				set(level, bb, noise(x, 7) < 50 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.MOSS_BLOCK), x, 1, 16);
			}
		}
		for (int[] b : new int[][]{{9, 10, 4}, {11, 16, 3}, {33, 16, 4}, {36, 10, 4}}) {
			for (int y = 1; y <= b[2]; y++) {
				set(level, bb, s(Blocks.BAMBOO_BLOCK), b[0], y, b[1]);
			}
		}
		// Arched crimson Taiko-bashi drum bridge across z = 11..15.
		for (int x = 20; x <= 24; x++) {
			set(level, bb, stair(Blocks.CRIMSON_STAIRS, Direction.NORTH, false), x, 1, 11);
			set(level, bb, s(Blocks.CRIMSON_PLANKS), x, 1, 12);
			set(level, bb, s(Blocks.CRIMSON_SLAB), x, 2, 13);
			set(level, bb, s(Blocks.CRIMSON_PLANKS), x, 1, 14);
			set(level, bb, stair(Blocks.CRIMSON_STAIRS, Direction.SOUTH, false), x, 1, 15);
		}
		for (int z = 11; z <= 15; z++) {
			set(level, bb, s(Blocks.CRIMSON_FENCE), 20, 2, z);
			set(level, bb, s(Blocks.CRIMSON_FENCE), 24, 2, z);
		}
		for (int[] lp : new int[][]{{20, 11}, {24, 11}, {20, 15}, {24, 15}}) {
			set(level, bb, s(Blocks.STRIPPED_DARK_OAK_LOG), lp[0], 2, lp[1]);
			set(level, bb, s(Blocks.LANTERN), lp[0], 3, lp[1]);
		}
		// Stepping-stone plazas linking bridge to West Tea House and East Cat Shrine.
		for (int x = 18; x <= 27; x++) {
			for (int z = 25; z <= 30; z++) {
				set(level, bb, (x + z) % 2 == 0 ? s(Blocks.SMOOTH_STONE) : s(Blocks.POLISHED_ANDESITE), x, 0, z);
			}
		}
	}

	private void teaHouse(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState spruce = s(Blocks.SPRUCE_PLANKS);
		BlockState bamboo = s(Blocks.BAMBOO_PLANKS);
		BlockState darkLog = s(Blocks.STRIPPED_DARK_OAK_LOG);
		BlockState cherry = s(Blocks.CHERRY_PLANKS);
		BlockState paper = s(Blocks.CALCITE);

		// Raised engawa wooden veranda deck & step from main stone courtyard.
		fill(level, bb, 3, 1, 20, 18, 1, 35, spruce);
		for (int z = 26; z <= 29; z++) {
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false), 18, 1, z);
		}
		fill(level, bb, 5, 1, 22, 16, 1, 33, bamboo);
		for (int z = 22; z <= 33; z++) {
			set(level, bb, darkLog, 10, 1, z);
		}
		// Framing shoji walls with wide open east veranda entry.
		JapaneseArchitecture.shojiPerimeter(p, 4, 2, 21, 17, 6, 34, darkLog, darkLog, spruce, paper, 4);
		fill(level, bb, 17, 2, 25, 17, 4, 30, AIR);
		for (int z : new int[]{24, 31}) {
			set(level, bb, JapaneseArchitecture.openTrapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.EAST), 16, 2, z);
		}
		for (int x : new int[]{7, 11, 14}) {
			set(level, bb, JapaneseArchitecture.openTrapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.SOUTH), x, 3, 21);
			set(level, bb, JapaneseArchitecture.openTrapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.NORTH), x, 3, 34);
		}

		// Two low chabudai tea tables with matcha teacups & cushions + north-west kettle hearth counter.
		for (int tz : new int[]{25, 30}) {
			set(level, bb, JapaneseArchitecture.slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM), 9, 2, tz);
			set(level, bb, s(Blocks.SEA_PICKLE).setValue(SeaPickleBlock.PICKLES, 2), 9, 3, tz);
			set(level, bb, s(Blocks.CARPET.red()), 8, 2, tz);
			set(level, bb, s(Blocks.CARPET.red()), 10, 2, tz);
		}
		fill(level, bb, 5, 2, 22, 7, 2, 24, s(Blocks.STONE_BRICKS));
		set(level, bb, s(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, true), 6, 2, 23);
		set(level, bb, s(Blocks.CAULDRON), 6, 4, 23);
		set(level, bb, s(Blocks.IRON_CHAIN), 6, 5, 23);
		set(level, bb, s(Blocks.BOOKSHELF), 5, 2, 26);
		set(level, bb, s(Blocks.BARREL), 5, 2, 28);
		set(level, bb, s(Blocks.POTTED_FLOWERING_AZALEA), 5, 3, 26);
		set(level, bb, s(Blocks.LANTERN), 17, 3, 23);
		set(level, bb, s(Blocks.LANTERN), 17, 3, 32);
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 11, 6, 28);

		// Curved irimoya roof with upturned corners & warm under-eave cherry trim.
		JapaneseArchitecture.upturnedEaveSkirt(p, 3, 20, 18, 35, 7, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, cherry, true);
		hipRoof(level, bb, 4, 21, 17, 34, 7, Blocks.DARK_OAK_STAIRS, cherry, s(Blocks.STRIPPED_CHERRY_LOG));
		chest(level, bb, 6, 2, 32, WildsSites.SAKURA_HALL, Direction.NORTH, 17L);
	}

	private void catShrine(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState cherry = s(Blocks.CHERRY_PLANKS);
		BlockState red = s(Blocks.DYED_TERRACOTTA.red());
		BlockState darkFloor = s(Blocks.DARK_OAK_PLANKS);
		BlockState andesite = s(Blocks.POLISHED_ANDESITE);

		// Chozuya purification water basin & steps before the shrine approach.
		fill(level, bb, 25, 1, 21, 27, 1, 23, andesite);
		set(level, bb, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL), 26, 2, 22);
		for (int[] cp : new int[][]{{25, 21}, {27, 21}, {25, 23}, {27, 23}}) {
			set(level, bb, s(Blocks.SPRUCE_FENCE), cp[0], 2, cp[1]);
			set(level, bb, s(Blocks.SPRUCE_FENCE), cp[0], 3, cp[1]);
		}
		fill(level, bb, 25, 4, 21, 27, 4, 23, JapaneseArchitecture.slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));

		// Shrine plinth floor, vermilion colonnade & front steps + saisen-bako offering box.
		fill(level, bb, 27, 1, 21, 40, 1, 34, andesite);
		fill(level, bb, 29, 1, 23, 38, 1, 32, darkFloor);
		for (int z = 26; z <= 29; z++) {
			set(level, bb, stair(Blocks.POLISHED_ANDESITE_STAIRS, Direction.EAST, false), 27, 1, z);
		}
		set(level, bb, s(Blocks.BARREL), 28, 2, 28);
		set(level, bb, s(Blocks.POTTED_FLOWERING_AZALEA), 28, 2, 24);
		set(level, bb, s(Blocks.POTTED_FLOWERING_AZALEA), 28, 2, 31);

		for (int[] col : new int[][]{{28, 22}, {39, 22}, {28, 33}, {39, 33}, {28, 25}, {28, 30}}) {
			fill(level, bb, col[0], 2, col[1], col[0], 6, col[1], red);
		}
		// Enclosed north, south & east shrine walls + shimenawa front lintel & white shide banners.
		JapaneseArchitecture.shojiPerimeter(p, 29, 2, 22, 39, 6, 33, red, cherry, andesite, s(Blocks.CALCITE), 5);
		fill(level, bb, 29, 2, 25, 29, 5, 30, AIR);
		for (int z = 25; z <= 30; z++) {
			set(level, bb, s(Blocks.HAY_BLOCK), 28, 5, z);
			if (z == 26 || z == 29) {
				banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.WHITE), 27, 5, z, Direction.WEST);
			}
		}
		banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.PINK), 27, 4, 22, Direction.WEST);
		banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.PINK), 27, 4, 33, Direction.WEST);

		// Four elevated Cat Cushion Shrines + central hanging Suzu bell.
		for (int[] c : new int[][]{{32, 24}, {36, 24}, {32, 31}, {36, 31}}) {
			set(level, bb, andesite, c[0], 2, c[1]);
			set(level, bb, s(Blocks.CARPET.pink()), c[0], 3, c[1]);
		}
		fill(level, bb, 33, 5, 27, 34, 5, 28, cherry);
		set(level, bb, s(Blocks.IRON_CHAIN), 33, 4, 27);
		set(level, bb, s(Blocks.BELL).setValue(BellBlock.FACING, Direction.WEST), 33, 3, 27);
		for (int[] sl : new int[][]{{31, 22}, {37, 22}, {31, 33}, {37, 33}}) {
			set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), sl[0], 6, sl[1]);
		}

		// Curved cherry & deepslate irimoya roof with upturned eaves & gold finials.
		JapaneseArchitecture.upturnedEaveSkirt(p, 27, 20, 41, 35, 7, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, cherry, true);
		hipRoof(level, bb, 27, 21, 40, 34, 7, Blocks.CHERRY_STAIRS, cherry, s(Blocks.GOLD_BLOCK));
		set(level, bb, s(Blocks.GOLD_BLOCK), 30, 14, 27);
		set(level, bb, s(Blocks.GOLD_BLOCK), 37, 14, 27);
		chest(level, bb, 38, 2, 32, WildsSites.SAKURA_VAULT, Direction.NORTH, 91L);
	}

	private void spawnVillager(WorldGenLevel level, BoundingBox bb, int x, int y, int z, int role) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!bb.isInside(pos)) {
			return;
		}
		SakuraVillager v = ShrineCatContent.SAKURA_VILLAGER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (v == null) {
			return;
		}
		v.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
		v.configure(role);
		level.addFreshEntityWithPassengers(v);
	}

	private void spawnVillageCat(WorldGenLevel level, BoundingBox bb, int x, int y, int z, ShrineCatRules.Variant variant) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!bb.isInside(pos)) {
			return;
		}
		ShrineCat cat = ShrineCatContent.SHRINE_CAT.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (cat == null) {
			return;
		}
		cat.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
		cat.configureVillage(variant, pos);
		level.addFreshEntityWithPassengers(cat);
	}
}
