package dev.wildercord.world.sites.wilds;

import dev.wildercord.wildlife.SakuraVillager;
import dev.wildercord.wildlife.ShrineCatRules;
import dev.wildercord.world.dungeons.JapaneseArchitecture;
import dev.wildercord.world.dungeons.JapaneseArchitecture.BlockPlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * Detailed Japanese Sakura Village compound modeled after village architecture references:
 * white-plaster perimeter castle wall with twin corner Yagura watchtowers and grand Myojin
 * Torii gate; winding koi river crossed by a crimson Taiko-bashi drum bridge &amp; stepping-stone
 * footbridge; two-story Matcha Tea House &amp; Ryokan Inn; Grand Cat Shrine Pavilion (Neko-Jinja)
 * housing elevated altar shrines and perched {@code Freya}; festival market Yatai stalls with
 * overhead lantern festoons; Artisan Kura storehouse, smithy, Karesansui Zen rock garden, eight
 * weeping sakura trees, Tea Master, Shrine Maiden, Swordsmith, Lantern Artisan, and all four Shrine Cats.
 */
public final class SakuraVillagePiece extends WildsPiece {
	private static final int WIDTH = 64, HEIGHT = 26, DEPTH = 68;

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
		gardenPathAndRiver(level, bb, p);
		JapaneseArchitecture.toriiGate(p, 32, 1, 4, 3, 6, true);
		for (int[] l : new int[][]{{26, 2}, {38, 2}, {26, 9}, {38, 9}, {16, 12}, {46, 12}, {25, 22}, {38, 22}, {25, 39}, {38, 39}, {10, 44}, {54, 44}}) {
			JapaneseArchitecture.ishidoro(p, l[0], 1, l[1]);
		}

		teaHouseInn(level, bb, p);
		catShrine(level, bb, p);
		festivalMarket(level, bb);
		northQuarterAndZenGarden(level, bb, p);

		for (int[] t : new int[][]{{8, 9, 4}, {55, 9, 4}, {7, 45, 5}, {57, 45, 5}, {5, 62, 5}, {32, 62, 5}, {59, 61, 5}, {19, 19, 4}}) {
			JapaneseArchitecture.weepingCherryTree(p, t[0], 1, t[1], t[2], true);
		}

		// Peaceful Tea Master, Shrine Maiden, Swordsmith, Lantern Artisan & all four cats anchored in the village compound.
		spawnVillager(level, bb, 12, 2, 32, SakuraVillager.TEA_MASTER);
		spawnVillager(level, bb, 42, 2, 30, SakuraVillager.SHRINE_MAIDEN);
		spawnVillager(level, bb, 30, 1, 52, SakuraVillager.SWORDSMITH);
		spawnVillager(level, bb, 36, 1, 27, SakuraVillager.LANTERN_MAKER);
		spawnVillageCat(level, bb, 18, 2, 31, ShrineCatRules.Variant.YUKI);
		spawnVillageCat(level, bb, 15, 2, 28, ShrineCatRules.Variant.ODIE);
		spawnVillageCat(level, bb, 41, 3, 32, ShrineCatRules.Variant.FREYA);
		spawnVillageCat(level, bb, 31, 1, 36, ShrineCatRules.Variant.KURO);
	}

	private void perimeterWall(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState plinth = s(Blocks.STONE_BRICKS);
		BlockState mossy = s(Blocks.MOSSY_STONE_BRICKS);
		BlockState plaster = s(Blocks.CALCITE);
		BlockState cap = JapaneseArchitecture.slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM);

		for (int x = 1; x <= WIDTH - 2; x++) {
			if (x < 28 || x > 36) {
				set(level, bb, noise(x, 1) < 35 ? mossy : plinth, x, 1, 1);
				set(level, bb, plaster, x, 2, 1);
				set(level, bb, cap, x, 3, 1);
			}
			set(level, bb, noise(x, 66) < 35 ? mossy : plinth, x, 1, DEPTH - 2);
			set(level, bb, plaster, x, 2, DEPTH - 2);
			set(level, bb, cap, x, 3, DEPTH - 2);
		}
		for (int z = 2; z < DEPTH - 2; z++) {
			set(level, bb, noise(1, z) < 35 ? mossy : plinth, 1, 1, z);
			set(level, bb, plaster, 1, 2, z);
			set(level, bb, cap, 1, 3, z);
			set(level, bb, noise(62, z) < 35 ? mossy : plinth, WIDTH - 2, 1, z);
			set(level, bb, plaster, WIDTH - 2, 2, z);
			set(level, bb, cap, WIDTH - 2, 3, z);
		}
		// Twin South Corner Yagura Watchtowers.
		cornerYagura(level, bb, 1, 1);
		cornerYagura(level, bb, 58, 1);
		for (int x = 2; x < WIDTH - 2; x++) {
			for (int z = 2; z < DEPTH - 2; z++) {
				if (noise(x, z) < 15) {
					set(level, bb, s(Blocks.PINK_PETALS), x, 1, z);
				} else if (noise(x + 9, z - 5) < 5) {
					set(level, bb, s(Blocks.MOSS_CARPET), x, 1, z);
				}
			}
		}
	}

	private void cornerYagura(WorldGenLevel level, BoundingBox bb, int x0, int z0) {
		int x1 = x0 + 4, z1 = z0 + 4;
		BlockState darkLog = s(Blocks.STRIPPED_DARK_OAK_LOG);
		fill(level, bb, x0, 1, z0, x1, 3, z1, s(Blocks.STONE_BRICKS));
		for (int y = 4; y <= 6; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					if (x == x0 || x == x1 || z == z0 || z == z1) {
						boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
						set(level, bb, corner ? darkLog : s(Blocks.CALCITE), x, y, z);
					}
				}
			}
		}
		set(level, bb, s(Blocks.LANTERN), x0 + 2, 4, z0);
		hipRoof(level, bb, x0, z0, x1, z1, 7, Blocks.DEEPSLATE_TILE_STAIRS, s(Blocks.DARK_OAK_PLANKS), s(Blocks.GOLD_BLOCK));
	}

	private void gardenPathAndRiver(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		for (int z = 0; z < DEPTH - 12; z++) {
			for (int x = 29; x <= 35; x++) {
				BlockState stone = noise(x, z) < 30 ? s(Blocks.MOSSY_STONE_BRICKS)
					: (noise(x + 4, z) < 40 ? s(Blocks.POLISHED_ANDESITE) : s(Blocks.SMOOTH_STONE));
				set(level, bb, stone, x, 0, z);
			}
		}
		// Winding Koi River across z = 13..17 with glowing sea lanterns, lily pads, mossy banks & bamboo grove.
		for (int x = 2; x <= WIDTH - 3; x++) {
			for (int z = 13; z <= 17; z++) {
				set(level, bb, s(Blocks.SAND), x, -1, z);
				set(level, bb, s(Blocks.WATER), x, 0, z);
				set(level, bb, AIR, x, 1, z);
				if (x % 8 == 0 && z == 15) {
					set(level, bb, s(Blocks.SEA_LANTERN), x, -1, z);
				} else if (Math.abs(x - 32) > 4 && Math.abs(x - 49) > 2 && noise(x, z) < 14) {
					set(level, bb, s(Blocks.LILY_PAD), x, 1, z);
				}
			}
			if (Math.abs(x - 32) > 4 && Math.abs(x - 49) > 2 && noise(x * 3, 12) < 34) {
				set(level, bb, noise(x, 2) < 50 ? s(Blocks.MOSSY_COBBLESTONE) : s(Blocks.MOSS_BLOCK), x, 1, 12);
			}
			if (Math.abs(x - 32) > 4 && Math.abs(x - 49) > 2 && noise(x * 5, 18) < 34) {
				set(level, bb, noise(x, 7) < 50 ? s(Blocks.MOSSY_STONE_BRICKS) : s(Blocks.MOSS_BLOCK), x, 1, 18);
			}
		}
		for (int[] b : new int[][]{{12, 12, 4}, {14, 18, 3}, {44, 18, 4}, {52, 12, 4}}) {
			for (int y = 1; y <= b[2]; y++) {
				set(level, bb, s(Blocks.BAMBOO_BLOCK), b[0], y, b[1]);
			}
		}
		// Central Arched Crimson Taiko-bashi Drum Bridge across z = 13..17.
		for (int x = 29; x <= 35; x++) {
			set(level, bb, stair(Blocks.CRIMSON_STAIRS, Direction.NORTH, false), x, 1, 13);
			set(level, bb, s(Blocks.CRIMSON_PLANKS), x, 1, 14);
			set(level, bb, s(Blocks.CRIMSON_SLAB), x, 2, 15);
			set(level, bb, s(Blocks.CRIMSON_PLANKS), x, 1, 16);
			set(level, bb, stair(Blocks.CRIMSON_STAIRS, Direction.SOUTH, false), x, 1, 17);
		}
		for (int z = 13; z <= 17; z++) {
			set(level, bb, s(Blocks.CRIMSON_FENCE), 29, 2, z);
			set(level, bb, s(Blocks.CRIMSON_FENCE), 35, 2, z);
		}
		for (int[] lp : new int[][]{{29, 13}, {35, 13}, {29, 17}, {35, 17}}) {
			set(level, bb, s(Blocks.STRIPPED_DARK_OAK_LOG), lp[0], 2, lp[1]);
			set(level, bb, s(Blocks.LANTERN), lp[0], 3, lp[1]);
		}
		// East Stepping-Stone Footbridge toward Cat Shrine.
		for (int z = 13; z <= 17; z++) {
			for (int x = 48; x <= 50; x++) {
				set(level, bb, s(Blocks.POLISHED_ANDESITE_SLAB), x, 1, z);
			}
		}
		// Main courtyard flagstone plaza linking Tea House, Market Stalls & Cat Shrine.
		for (int x = 23; x <= 40; x++) {
			for (int z = 20; z <= 42; z++) {
				set(level, bb, (x + z) % 2 == 0 ? s(Blocks.SMOOTH_STONE) : s(Blocks.POLISHED_ANDESITE), x, 0, z);
			}
		}
	}

	private void teaHouseInn(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState spruce = s(Blocks.SPRUCE_PLANKS);
		BlockState bamboo = s(Blocks.BAMBOO_PLANKS);
		BlockState darkLog = s(Blocks.STRIPPED_DARK_OAK_LOG);
		BlockState cherry = s(Blocks.CHERRY_PLANKS);
		BlockState paper = s(Blocks.CALCITE);

		// Raised engawa wooden veranda deck + eastern veranda steps.
		fill(level, bb, 4, 1, 21, 24, 1, 43, spruce);
		for (int z = 29; z <= 34; z++) {
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false), 24, 1, z);
		}
		fill(level, bb, 6, 1, 23, 22, 1, 41, bamboo);
		for (int z = 23; z <= 41; z++) {
			set(level, bb, darkLog, 13, 1, z);
		}

		// Ground-floor framing shoji walls with open eastern entrance and lattice windows.
		JapaneseArchitecture.shojiPerimeter(p, 5, 2, 22, 23, 6, 42, darkLog, darkLog, spruce, paper, 4);
		fill(level, bb, 23, 2, 28, 23, 5, 35, AIR);
		for (int z : new int[]{27, 36}) {
			set(level, bb, JapaneseArchitecture.openTrapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.EAST), 22, 2, z);
		}
		for (int x : new int[]{8, 12, 16, 20}) {
			set(level, bb, JapaneseArchitecture.openTrapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.SOUTH), x, 3, 22);
			set(level, bb, JapaneseArchitecture.openTrapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.NORTH), x, 3, 42);
		}

		// Three Chabudai Tea Tables with matcha cups & cushions + Irori Kettle Hearth + Tea Bar Counter.
		for (int tz : new int[]{25, 30, 35}) {
			set(level, bb, JapaneseArchitecture.slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM), 17, 2, tz);
			set(level, bb, SakuraVillageHelper.dryTeaCups(2), 17, 3, tz);
			set(level, bb, s(Blocks.CARPET.red()), 16, 2, tz);
			set(level, bb, s(Blocks.CARPET.red()), 18, 2, tz);
		}
		fill(level, bb, 6, 2, 23, 9, 2, 26, s(Blocks.STONE_BRICKS));
		set(level, bb, s(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, true), 7, 2, 24);
		set(level, bb, s(Blocks.CAULDRON), 7, 4, 24);
		set(level, bb, s(Blocks.IRON_CHAIN), 7, 5, 24);
		fill(level, bb, 6, 2, 28, 6, 2, 33, darkLog);
		set(level, bb, s(Blocks.CHISELED_BOOKSHELF).setValue(ChiseledBookShelfBlock.FACING, Direction.EAST), 6, 2, 27);
		set(level, bb, s(Blocks.BARREL), 6, 2, 29);
		set(level, bb, s(Blocks.POTTED_FLOWERING_AZALEA), 6, 3, 28);
		banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.GREEN), 6, 5, 31, Direction.EAST);
		set(level, bb, s(Blocks.LANTERN), 23, 3, 25);
		set(level, bb, s(Blocks.LANTERN), 23, 3, 38);
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 15, 6, 32);

		// Tier 1 Upturned Eave Skirt + 2nd-Story Tatami Tea Balcony (reachable via interior stairs at x = 7..8, z = 34..39).
		JapaneseArchitecture.upturnedEaveSkirt(p, 4, 21, 24, 43, 7, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, cherry, true);
		fill(level, bb, 7, 7, 24, 21, 7, 40, bamboo);
		for (int step = 0; step < 6; step++) {
			int z = 34 + step;
			int sy = 2 + step;
			fill(level, bb, 8, 2, z, 9, sy - 1, z, spruce);
			fill(level, bb, 8, sy, z, 9, sy, z, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
			fill(level, bb, 8, sy + 1, z, 9, 9, z, AIR);
		}
		JapaneseArchitecture.shojiPerimeter(p, 7, 8, 24, 21, 11, 40, darkLog, darkLog, spruce, paper, 4);
		fill(level, bb, 21, 8, 29, 21, 10, 35, AIR);
		for (int z = 25; z <= 39; z++) {
			set(level, bb, s(Blocks.SPRUCE_FENCE), 22, 8, z);
		}
		set(level, bb, JapaneseArchitecture.slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM), 14, 8, 30);
		set(level, bb, SakuraVillageHelper.dryTeaCups(2), 14, 9, 30);
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 14, 11, 30);

		hipRoof(level, bb, 6, 23, 22, 41, 12, Blocks.DARK_OAK_STAIRS, cherry, s(Blocks.STRIPPED_CHERRY_LOG));
		chest(level, bb, 10, 2, 40, WildsSites.SAKURA_HALL, Direction.NORTH, 17L);
	}

	private void catShrine(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState cherry = s(Blocks.CHERRY_PLANKS);
		BlockState red = s(Blocks.DYED_TERRACOTTA.red());
		BlockState darkFloor = s(Blocks.DARK_OAK_PLANKS);
		BlockState andesite = s(Blocks.POLISHED_ANDESITE);

		// Chozuya purification water basin pavilion before the shrine approach.
		fill(level, bb, 36, 1, 19, 38, 1, 21, andesite);
		set(level, bb, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL), 37, 2, 20);
		for (int[] cp : new int[][]{{36, 19}, {38, 19}, {36, 21}, {38, 21}}) {
			set(level, bb, s(Blocks.SPRUCE_FENCE), cp[0], 2, cp[1]);
			set(level, bb, s(Blocks.SPRUCE_FENCE), cp[0], 3, cp[1]);
		}
		fill(level, bb, 36, 4, 19, 38, 4, 21, JapaneseArchitecture.slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));

		// Shrine plinth floor, vermilion colonnade, entry steps & saisen-bako offering box.
		fill(level, bb, 39, 1, 21, 59, 1, 43, andesite);
		fill(level, bb, 42, 1, 24, 57, 1, 40, darkFloor);
		for (int z = 29; z <= 35; z++) {
			set(level, bb, stair(Blocks.POLISHED_ANDESITE_STAIRS, Direction.EAST, false), 39, 1, z);
		}
		set(level, bb, s(Blocks.BARREL), 40, 2, 34);
		set(level, bb, s(Blocks.POTTED_FLOWERING_AZALEA), 40, 2, 27);
		set(level, bb, s(Blocks.POTTED_FLOWERING_AZALEA), 40, 2, 37);

		// Freya's elevated royal pink-cushion shrine dais right at the west shrine threshold (41, 3, 32).
		set(level, bb, andesite, 41, 2, 32);
		set(level, bb, s(Blocks.CARPET.pink()), 41, 3, 32);

		for (int[] col : new int[][]{{40, 22}, {58, 22}, {40, 42}, {58, 42}, {40, 28}, {40, 36}}) {
			fill(level, bb, col[0], 2, col[1], col[0], 6, col[1], red);
		}
		JapaneseArchitecture.shojiPerimeter(p, 42, 2, 22, 58, 6, 42, red, cherry, andesite, s(Blocks.CALCITE), 5);
		fill(level, bb, 42, 2, 28, 42, 5, 36, AIR);
		for (int z = 28; z <= 36; z++) {
			set(level, bb, s(Blocks.HAY_BLOCK), 40, 5, z);
			if (z == 29 || z == 35) {
				banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.WHITE), 39, 5, z, Direction.WEST);
			}
		}
		banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.PINK), 39, 4, 22, Direction.WEST);
		banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.PINK), 39, 4, 42, Direction.WEST);

		// Four elevated Cat Altar Shrines inside Neko-Jinja + central hanging bronze Suzu Bell.
		for (int[] c : new int[][]{{47, 26}, {53, 26}, {47, 37}, {53, 37}}) {
			set(level, bb, andesite, c[0], 2, c[1]);
			set(level, bb, s(Blocks.CARPET.pink()), c[0], 3, c[1]);
			set(level, bb, s(Blocks.LANTERN), c[0] + 1, 2, c[1]);
			set(level, bb, s(Blocks.POTTED_FLOWERING_AZALEA), c[0] - 1, 2, c[1]);
		}
		fill(level, bb, 49, 5, 31, 51, 5, 33, cherry);
		set(level, bb, s(Blocks.IRON_CHAIN), 50, 4, 32);
		set(level, bb, s(Blocks.BELL).setValue(BellBlock.FACING, Direction.WEST), 50, 3, 32);
		for (int[] sl : new int[][]{{45, 24}, {55, 24}, {45, 40}, {55, 40}}) {
			set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), sl[0], 6, sl[1]);
		}

		JapaneseArchitecture.upturnedEaveSkirt(p, 38, 20, 60, 44, 7, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, cherry, true);
		hipRoof(level, bb, 40, 22, 58, 42, 8, Blocks.CHERRY_STAIRS, cherry, s(Blocks.GOLD_BLOCK));
		set(level, bb, s(Blocks.GOLD_BLOCK), 47, 17, 32);
		set(level, bb, s(Blocks.GOLD_BLOCK), 51, 17, 32);
		chest(level, bb, 56, 2, 40, WildsSites.SAKURA_VAULT, Direction.NORTH, 91L);
	}

	private void festivalMarket(WorldGenLevel level, BoundingBox bb) {
		BlockState spruceFence = s(Blocks.SPRUCE_FENCE);
		BlockState redWool = s(Blocks.WOOL.red());
		BlockState whiteWool = s(Blocks.WOOL.white());
		BlockState pinkWool = s(Blocks.WOOL.pink());

		// West Dango & Matcha Yatai Stall (x = 26..28, z = 25..29).
		fill(level, bb, 26, 1, 25, 26, 1, 29, s(Blocks.OAK_PLANKS));
		set(level, bb, s(Blocks.BARREL), 26, 1, 25);
		set(level, bb, SakuraVillageHelper.dryTeaCups(3), 26, 2, 27);
		set(level, bb, s(Blocks.POTTED_FLOWERING_AZALEA), 26, 2, 29);
		for (int[] p : new int[][]{{26, 25}, {28, 25}, {26, 29}, {28, 29}}) {
			fill(level, bb, p[0], 1, p[1], p[0], 3, p[1], spruceFence);
		}
		for (int x = 26; x <= 28; x++) {
			for (int z = 25; z <= 29; z++) {
				set(level, bb, (z % 2 == 0) ? whiteWool : redWool, x, 4, z);
			}
		}
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 28, 3, 27);

		// East Suzu & Charm Yatai Stall (x = 36..38, z = 25..29).
		fill(level, bb, 38, 1, 25, 38, 1, 29, s(Blocks.OAK_PLANKS));
		set(level, bb, s(Blocks.LECTERN), 38, 2, 27);
		set(level, bb, s(Blocks.LANTERN), 38, 2, 25);
		for (int[] p : new int[][]{{36, 25}, {38, 25}, {36, 29}, {38, 29}}) {
			fill(level, bb, p[0], 1, p[1], p[0], 3, p[1], spruceFence);
		}
		for (int x = 36; x <= 38; x++) {
			for (int z = 25; z <= 29; z++) {
				set(level, bb, (z % 2 == 0) ? whiteWool : pinkWool, x, 4, z);
			}
		}
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 36, 3, 27);

		// Overhead Lantern Festoon Arches spanning the central boulevard (x = 26..38, z = 22, 32, 41).
		BlockState beam = s(Blocks.STRIPPED_DARK_OAK_LOG);
		for (int fz : new int[]{22, 32, 41}) {
			fill(level, bb, 26, 1, fz, 26, 5, fz, beam);
			fill(level, bb, 38, 1, fz, 38, 5, fz, beam);
			fill(level, bb, 26, 5, fz, 38, 5, fz, beam);
			for (int lx : new int[]{29, 32, 35}) {
				set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), lx, 4, fz);
			}
		}
	}

	private void northQuarterAndZenGarden(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState deepslate = s(Blocks.DEEPSLATE_TILES);
		BlockState plaster = s(Blocks.CALCITE);
		BlockState darkLog = s(Blocks.STRIPPED_DARK_OAK_LOG);

		// NW Artisan Kura Storehouse (x = 10..22, z = 49..59) with white-plaster walls & dark tile hip roof.
		fill(level, bb, 10, 0, 49, 22, 1, 59, deepslate);
		JapaneseArchitecture.shojiPerimeter(p, 10, 2, 49, 22, 6, 59, darkLog, darkLog, deepslate, plaster, 4);
		fill(level, bb, 22, 2, 53, 22, 4, 55, AIR);
		set(level, bb, s(Blocks.IRON_BARS), 14, 3, 49);
		set(level, bb, s(Blocks.IRON_BARS), 18, 3, 49);
		set(level, bb, s(Blocks.BARREL), 12, 2, 57);
		set(level, bb, s(Blocks.CHEST), 13, 2, 57);
		set(level, bb, s(Blocks.LANTERN), 21, 3, 52);
		hipRoof(level, bb, 9, 48, 23, 60, 7, Blocks.DEEPSLATE_TILE_STAIRS, deepslate, s(Blocks.GOLD_BLOCK));

		// North-Center Open-Air Artisan Smithy Veranda (x = 25..34, z = 50..57).
		fill(level, bb, 25, 0, 50, 34, 0, 57, s(Blocks.POLISHED_ANDESITE));
		for (int[] c : new int[][]{{25, 50}, {34, 50}, {25, 57}, {34, 57}}) {
			fill(level, bb, c[0], 1, c[1], c[0], 4, c[1], darkLog);
		}
		fill(level, bb, 25, 5, 50, 34, 5, 57, s(Blocks.SPRUCE_PLANKS));
		set(level, bb, s(Blocks.BLAST_FURNACE), 27, 1, 55);
		set(level, bb, s(Blocks.ANVIL), 30, 1, 53);
		set(level, bb, s(Blocks.GRINDSTONE), 32, 1, 55);
		set(level, bb, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL), 26, 1, 52);
		set(level, bb, s(Blocks.LANTERN), 29, 2, 50);

		// NE Karesansui Zen Rock Garden (x = 38..56, z = 48..60): raked gravel/diorite waves + mossy boulders.
		for (int x = 38; x <= 56; x++) {
			for (int z = 48; z <= 60; z++) {
				boolean edge = x == 38 || x == 56 || z == 48 || z == 60;
				set(level, bb, edge ? s(Blocks.STONE_BRICKS) : ((x % 2 == 0) ? s(Blocks.GRAVEL) : s(Blocks.DIORITE)), x, 0, z);
			}
		}
		for (int[] rock : new int[][]{{43, 52}, {50, 55}, {45, 57}}) {
			set(level, bb, s(Blocks.MOSS_BLOCK), rock[0], 1, rock[1]);
			set(level, bb, s(Blocks.MOSSY_COBBLESTONE), rock[0] + 1, 1, rock[1]);
			set(level, bb, s(Blocks.MOSSY_STONE_BRICK_WALL), rock[0], 2, rock[1]);
		}
		JapaneseArchitecture.ishidoro(p, 54, 1, 50);
	}

	private void spawnVillager(WorldGenLevel level, BoundingBox bb, int x, int y, int z, int role) {
		SakuraVillageHelper.spawnVillager(level, bb, getWorldPos(x, y, z), role);
	}

	private void spawnVillageCat(WorldGenLevel level, BoundingBox bb, int x, int y, int z, ShrineCatRules.Variant variant) {
		SakuraVillageHelper.spawnCat(level, bb, getWorldPos(x, y, z), variant);
	}
}
