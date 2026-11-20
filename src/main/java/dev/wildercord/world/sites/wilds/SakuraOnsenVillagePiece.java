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
 * Sakura Mountain Onsen Resort Village: three-tier terraced hillside retreat featuring a grand
 * two-story wooden Ryokan Inn with wrap-around balconies and blue noren curtains, open-air steaming
 * Rotenburo mineral hot-spring pools and cedar soaking tubs framed by mossy stone boulders and bamboo
 * screens, a cozy Ramen/Dango &amp; Tea Pavilion with red parasol umbrellas, and an elevated
 * Calligraphy &amp; Scroll Gazebo on the upper cliff terrace.
 */
public final class SakuraOnsenVillagePiece extends WildsPiece {
	private static final int WIDTH = 64, HEIGHT = 28, DEPTH = 68;

	public SakuraOnsenVillagePiece(int x, int z, Direction facing) {
		super(WildsSites.SAKURA_ONSEN_VILLAGE, x, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public SakuraOnsenVillagePiece(CompoundTag tag) {
		super(WildsSites.SAKURA_ONSEN_VILLAGE, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		return surface(c, new SakuraOnsenVillagePiece(c.chunkPos().getMinBlockX(), c.chunkPos().getMinBlockZ(), facing(c)), 10);
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
		terracedGround(level, bb, p);
		ryokanInn(level, bb, p);
		rotenburoBathGarden(level, bb, p);
		ramenTeaPavilion(level, bb, p);
		calligraphyGazebo(level, bb, p);

		for (int[] t : new int[][]{{8, 9, 5}, {25, 10, 4}, {55, 6, 4}, {54, 30, 5}, {25, 53, 5}, {58, 56, 5}}) {
			int baseY = t[1] >= 48 ? 6 : (t[1] >= 20 ? 3 : 1);
			JapaneseArchitecture.weepingCherryTree(p, t[0], baseY, t[1], t[2], true);
		}

		SakuraVillageHelper.spawnVillager(level, bb, getWorldPos(37, 3, 35), SakuraVillager.ONSEN_KEEPER);
		SakuraVillageHelper.spawnVillager(level, bb, getWorldPos(46, 3, 18), SakuraVillager.TEA_MASTER);
		SakuraVillageHelper.spawnVillager(level, bb, getWorldPos(27, 3, 30), SakuraVillager.LANTERN_MAKER);
		SakuraVillageHelper.spawnVillager(level, bb, getWorldPos(12, 7, 56), SakuraVillager.SHRINE_MAIDEN);
		SakuraVillageHelper.spawnCat(level, bb, getWorldPos(32, 3, 38), ShrineCatRules.Variant.ODIE);
		SakuraVillageHelper.spawnCat(level, bb, getWorldPos(26, 3, 33), ShrineCatRules.Variant.FREYA);
	}

	private void terracedGround(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState dirt = s(Blocks.DIRT);
		BlockState grass = s(Blocks.GRASS_BLOCK);
		BlockState stone = s(Blocks.STONE_BRICKS);
		BlockState mossy = s(Blocks.MOSSY_STONE_BRICKS);

		// Lower entry terrace (z = 0..19, top y = 0), Mid resort terrace (z = 20..45, top y = 2), Upper mountain terrace (z = 46..67, top y = 5).
		fill(level, bb, 0, 0, 0, WIDTH - 1, 0, DEPTH - 1, dirt);
		fill(level, bb, 0, 0, 0, WIDTH - 1, 0, 19, grass);
		fill(level, bb, 0, 1, 20, WIDTH - 1, 1, DEPTH - 1, dirt);
		fill(level, bb, 0, 2, 20, WIDTH - 1, 2, 45, grass);
		fill(level, bb, 0, 3, 46, WIDTH - 1, 4, DEPTH - 1, dirt);
		fill(level, bb, 0, 5, 46, WIDTH - 1, 5, DEPTH - 1, grass);
		clear(level, bb, 0, 1, 0, WIDTH - 1, HEIGHT - 1, 19);
		clear(level, bb, 0, 3, 20, WIDTH - 1, HEIGHT - 1, 45);
		clear(level, bb, 0, 6, 46, WIDTH - 1, HEIGHT - 1, DEPTH - 1);

		// Mossy retaining cliffs along terrace risers.
		for (int x = 1; x <= WIDTH - 2; x++) {
			set(level, bb, noise(x, 20) < 40 ? mossy : stone, x, 1, 20);
			set(level, bb, noise(x, 20) < 35 ? s(Blocks.MOSS_BLOCK) : stone, x, 2, 20);
			for (int y = 3; y <= 5; y++) {
				set(level, bb, noise(x, y, 46) < 40 ? mossy : s(Blocks.MOSSY_COBBLESTONE), x, y, 46);
			}
		}
		// Central stone approach path, grand Myojin Torii gate & welcoming campfire hearth on lower plaza.
		for (int z = 1; z <= 45; z++) {
			for (int x = 27; x <= 32; x++) {
				int py = z <= 19 ? 0 : 2;
				set(level, bb, (x + z) % 2 == 0 ? s(Blocks.SMOOTH_STONE) : s(Blocks.POLISHED_ANDESITE), x, py, z);
			}
		}
		JapaneseArchitecture.toriiGate(p, 30, 1, 5, 3, 6, true);
		for (int y = 1; y <= 2; y++) {
			for (int x = 27; x <= 32; x++) {
				set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false), x, y, 18 + y);
			}
		}
		// Upper mountain bath stairs (x = 28..31, z = 44..47).
		for (int y = 3; y <= 5; y++) {
			for (int x = 28; x <= 31; x++) {
				set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false), x, y, 41 + y);
			}
		}
		// Cozy welcome bonfire in lower entry garden beside ramen stairs.
		fill(level, bb, 34, 0, 6, 36, 0, 8, s(Blocks.COBBLESTONE));
		set(level, bb, s(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, true), 35, 1, 7);
		JapaneseArchitecture.ishidoro(p, 25, 1, 3);
		JapaneseArchitecture.ishidoro(p, 34, 1, 3);
		JapaneseArchitecture.ishidoro(p, 26, 3, 23);
		JapaneseArchitecture.ishidoro(p, 34, 3, 23);
	}

	private void ryokanInn(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState darkLog = s(Blocks.STRIPPED_DARK_OAK_LOG);
		BlockState spruce = s(Blocks.SPRUCE_PLANKS);
		BlockState bamboo = s(Blocks.BAMBOO_PLANKS);
		BlockState paper = s(Blocks.CALCITE);

		// Raised Ryokan veranda & tatami ground floor (x = 4..25, z = 22..45, floor y = 3).
		fill(level, bb, 4, 2, 22, 26, 2, 45, s(Blocks.STONE_BRICKS));
		fill(level, bb, 4, 3, 22, 26, 3, 45, spruce);
		fill(level, bb, 6, 3, 24, 23, 3, 43, bamboo);
		for (int z = 30; z <= 36; z++) {
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false), 26, 3, z);
		}
		JapaneseArchitecture.shojiPerimeter(p, 5, 4, 23, 24, 7, 44, darkLog, darkLog, spruce, paper, 4);
		fill(level, bb, 24, 4, 30, 24, 6, 36, AIR);
		// Warm paper screen ambient glow anchors + blue noren curtain banners at main Ryokan entry.
		for (int z : new int[]{27, 32, 34, 39}) {
			banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.BLUE), 25, 6, z, Direction.EAST);
		}
		set(level, bb, s(Blocks.OCHRE_FROGLIGHT), 7, 6, 26);
		set(level, bb, s(Blocks.OCHRE_FROGLIGHT), 7, 6, 40);
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 15, 7, 33);

		// Chabudai tea tables with non-waterlogged ceramic cups & cushions inside Ryokan lobby.
		for (int tz : new int[]{28, 34, 39}) {
			set(level, bb, JapaneseArchitecture.slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM), 17, 4, tz);
			set(level, bb, SakuraVillageHelper.dryTeaCups(2), 17, 5, tz);
			set(level, bb, s(Blocks.CARPET.pick(DyeColor.BLUE)), 16, 4, tz);
			set(level, bb, s(Blocks.CARPET.pick(DyeColor.BLUE)), 18, 4, tz);
		}
		set(level, bb, s(Blocks.BARREL), 8, 4, 25);
		chest(level, bb, 9, 4, 25, WildsSites.SAKURA_ONSEN_HALL, Direction.SOUTH, 53L);

		// Interior stairs up to 2nd floor + wrap-around timber balcony overlooking Rotenburo baths!
		fill(level, bb, 4, 8, 22, 26, 8, 45, spruce);
		for (int step = 0; step < 5; step++) {
			int z = 37 + step;
			int sy = 4 + step;
			fill(level, bb, 9, 4, z, 10, sy - 1, z, spruce);
			fill(level, bb, 9, sy, z, 10, sy, z, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
			fill(level, bb, 9, sy + 1, z, 10, 10, z, AIR);
		}
		for (int x = 4; x <= 26; x++) {
			set(level, bb, s(Blocks.SPRUCE_FENCE), x, 9, 22);
			set(level, bb, s(Blocks.SPRUCE_FENCE), x, 9, 45);
		}
		for (int z = 22; z <= 45; z++) {
			set(level, bb, s(Blocks.SPRUCE_FENCE), 4, 9, z);
			set(level, bb, s(Blocks.SPRUCE_FENCE), 26, 9, z);
		}
		JapaneseArchitecture.shojiPerimeter(p, 7, 9, 25, 22, 12, 42, darkLog, darkLog, spruce, paper, 4);
		fill(level, bb, 22, 9, 31, 22, 11, 36, AIR);
		set(level, bb, JapaneseArchitecture.slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM), 15, 9, 32);
		set(level, bb, SakuraVillageHelper.dryTeaCups(2), 15, 10, 32);
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 26, 12, 24);
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 26, 12, 33);
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 26, 12, 43);

		JapaneseArchitecture.upturnedEaveSkirt(p, 3, 21, 27, 46, 8, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, spruce, true);
		JapaneseArchitecture.upturnedEaveSkirt(p, 5, 23, 24, 44, 13, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, darkLog, true);
		hipRoof(level, bb, 6, 24, 23, 43, 14, Blocks.DEEPSLATE_TILE_STAIRS, spruce, s(Blocks.STRIPPED_DARK_OAK_LOG));
	}

	private void rotenburoBathGarden(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState mossy = s(Blocks.MOSSY_COBBLESTONE);
		BlockState moss = s(Blocks.MOSS_BLOCK);
		BlockState stone = s(Blocks.MOSSY_STONE_BRICKS);
		BlockState bambooBlock = s(Blocks.BAMBOO_BLOCK);
		BlockState bambooFence = s(Blocks.BAMBOO_FENCE);

		// 1. Upper Steaming Open-Air Rotenburo Pool (x = 32..46, z = 49..60, enclosed on all sides y = 5..7).
		for (int x = 31; x <= 47; x++) {
			for (int z = 48; z <= 61; z++) {
				boolean rim = x == 31 || x == 47 || z == 48 || z == 61;
				if (rim) {
					BlockState rock = noise(x, z) < 40 ? moss : mossy;
					fill(level, bb, x, 3, z, x, 6, z, rock);
					if (noise(x + 3, z) < 35) {
						set(level, bb, s(Blocks.MOSSY_COBBLESTONE_WALL), x, 7, z);
					}
				} else {
					set(level, bb, s(Blocks.STONE_BRICKS), x, 3, z);
					if ((x == 35 || x == 43) && (z == 52 || z == 57)) {
						set(level, bb, s(Blocks.SEA_LANTERN), x, 3, z);
					} else if ((x == 38 && z == 54) || (x == 40 && z == 55)) {
						set(level, bb, s(Blocks.MAGMA_BLOCK), x, 3, z);
					}
					set(level, bb, s(Blocks.WATER), x, 4, z);
					set(level, bb, s(Blocks.WATER), x, 5, z);
					set(level, bb, AIR, x, 6, z);
				}
			}
		}
		// Concealed dry rim steam vents above stone crater wall producing billowing hot-spring mist over the upper pool.
		for (int[] vent : new int[][]{{31, 6, 54}, {47, 6, 55}}) {
			set(level, bb, s(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, true), vent[0], vent[1], vent[2]);
			set(level, bb, JapaneseArchitecture.slab(Blocks.MOSSY_STONE_BRICK_SLAB, SlabType.TOP), vent[0], vent[1] + 1, vent[2]);
		}
		// Entry steps from stone landing into upper pool.
		set(level, bb, stone, 31, 6, 51);
		set(level, bb, AIR, 31, 7, 51);
		set(level, bb, stair(Blocks.MOSSY_STONE_BRICK_STAIRS, Direction.WEST, false), 30, 5, 51);

		// 2. Cedar Soaking Tub & Bamboo Privacy Screen on Mid Terrace (x = 34..40, z = 36..42).
		for (int x = 34; x <= 40; x++) {
			for (int z = 36; z <= 42; z++) {
				boolean wall = x == 34 || x == 40 || z == 36 || z == 42;
				set(level, bb, s(Blocks.SPRUCE_PLANKS), x, 2, z);
				set(level, bb, wall ? s(Blocks.SPRUCE_PLANKS) : s(Blocks.WATER), x, 3, z);
			}
		}
		for (int x = 33; x <= 41; x++) {
			for (int y = 3; y <= 6; y++) {
				set(level, bb, (x == 33 || x == 41 || y == 3) ? bambooBlock : bambooFence, x, y, 43);
			}
		}
		set(level, bb, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL), 33, 3, 38);
		set(level, bb, s(Blocks.SPRUCE_STAIRS), 37, 4, 42);

		// 3. Lower Bamboo-Screened Mineral Pool & Mossy Waterfall Cliffside (x = 48..58, z = 34..44).
		for (int x = 47; x <= 59; x++) {
			for (int z = 33; z <= 45; z++) {
				boolean rim = x == 47 || x == 59 || z == 33 || z == 45;
				if (rim) {
					set(level, bb, noise(x, z) < 40 ? moss : mossy, x, 2, z);
					set(level, bb, stone, x, 3, z);
				} else {
					set(level, bb, s(Blocks.STONE_BRICKS), x, 1, z);
					if (x == 53 && z == 39) {
						set(level, bb, s(Blocks.SEA_LANTERN), x, 1, z);
					}
					set(level, bb, s(Blocks.WATER), x, 2, z);
					set(level, bb, AIR, x, 3, z);
				}
			}
		}
		// Lush mossy rock cliff & tall bamboo grove framing east bath edge.
		for (int x = 51; x <= 62; x++) {
			for (int z = 49; z <= 64; z++) {
				if (x + z >= 106) {
					fill(level, bb, x, 6, z, x, 7 + noise(x, z) / 40, z, noise(x, z) < 45 ? moss : mossy);
				}
			}
		}
		for (int[] bm : new int[][]{{45, 34, 5}, {45, 40, 6}, {59, 31, 5}, {32, 31, 4}}) {
			for (int y = 3; y < 3 + bm[2]; y++) {
				set(level, bb, bambooBlock, bm[0], y, bm[1]);
			}
		}
		JapaneseArchitecture.ishidoro(p, 44, 3, 46);
		JapaneseArchitecture.ishidoro(p, 45, 3, 33);
	}

	private void ramenTeaPavilion(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState cherry = s(Blocks.CHERRY_PLANKS);
		BlockState darkLog = s(Blocks.STRIPPED_DARK_OAK_LOG);
		BlockState redWool = s(Blocks.WOOL.pick(DyeColor.RED));
		BlockState whiteWool = s(Blocks.WOOL.pick(DyeColor.WHITE));

		// Raised terrace deck on SE lower foreground (x = 37..57, z = 10..29, deck y = 2).
		fill(level, bb, 37, 1, 10, 57, 1, 29, s(Blocks.STONE_BRICKS));
		fill(level, bb, 37, 2, 10, 57, 2, 29, cherry);
		for (int x = 37; x <= 57; x++) {
			if (x < 45 || x > 48) {
				set(level, bb, s(Blocks.CHERRY_FENCE), x, 3, 10);
			}
		}
		for (int z = 10; z <= 29; z++) {
			set(level, bb, s(Blocks.CHERRY_FENCE), 37, 3, z);
			set(level, bb, s(Blocks.CHERRY_FENCE), 57, 3, z);
		}
		for (int x = 45; x <= 48; x++) {
			set(level, bb, stair(Blocks.CHERRY_STAIRS, Direction.NORTH, false), x, 1, 9);
			set(level, bb, stair(Blocks.CHERRY_STAIRS, Direction.NORTH, false), x, 2, 10);
		}

		// Cozy Ramen, Dango & Matcha Stall with red/white awning (x = 40..54, z = 20..28).
		fill(level, bb, 40, 3, 21, 54, 3, 21, s(Blocks.OAK_PLANKS));
		for (int x : new int[]{43, 47, 51}) {
			set(level, bb, SakuraVillageHelper.dryTeaCups(3), x, 4, 21);
			set(level, bb, JapaneseArchitecture.slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM), x, 3, 19);
		}
		set(level, bb, s(Blocks.SMOKER), 43, 3, 26);
		set(level, bb, s(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, true), 47, 3, 26);
		set(level, bb, s(Blocks.CAULDRON), 47, 5, 26);
		set(level, bb, s(Blocks.BARREL), 52, 3, 26);
		for (int[] post : new int[][]{{40, 21}, {54, 21}, {40, 28}, {54, 28}}) {
			fill(level, bb, post[0], 3, post[1], post[0], 6, post[1], darkLog);
		}
		JapaneseArchitecture.shojiPerimeter(p, 40, 3, 24, 54, 6, 28, darkLog, darkLog, s(Blocks.SPRUCE_PLANKS), s(Blocks.CALCITE), 5);
		fill(level, bb, 40, 3, 24, 54, 5, 24, AIR);
		for (int x = 40; x <= 54; x++) {
			set(level, bb, (x % 2 == 0) ? redWool : whiteWool, x, 6, 20);
			set(level, bb, (x % 2 == 0) ? redWool : whiteWool, x, 7, 21);
			if (x % 3 == 0) {
				set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), x, 5, 20);
			}
		}
		hipRoof(level, bb, 39, 21, 55, 29, 7, Blocks.DARK_OAK_STAIRS, cherry, s(Blocks.STRIPPED_DARK_OAK_LOG));

		// Two crimson courtyard parasol umbrellas + outdoor tea tables.
		for (int px : new int[]{41, 53}) {
			set(level, bb, s(Blocks.CRIMSON_FENCE), px, 3, 15);
			set(level, bb, s(Blocks.CRIMSON_FENCE), px, 4, 15);
			set(level, bb, s(Blocks.CRIMSON_FENCE), px, 5, 15);
			fill(level, bb, px - 1, 6, 14, px + 1, 6, 16, redWool);
			fill(level, bb, px - 1, 7, 14, px + 1, 7, 16, s(Blocks.RED_CARPET));
			set(level, bb, JapaneseArchitecture.slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM), px - 1, 3, 14);
			set(level, bb, SakuraVillageHelper.dryTeaCups(2), px - 1, 4, 14);
		}
	}

	private void calligraphyGazebo(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState stone = s(Blocks.MOSSY_STONE_BRICKS);
		BlockState moss = s(Blocks.MOSS_BLOCK);
		BlockState darkLog = s(Blocks.STRIPPED_DARK_OAK_LOG);
		BlockState cherry = s(Blocks.CHERRY_PLANKS);

		// Elevated mossy cliff knoll (x = 4..20, z = 49..63, plinth top y = 6).
		fill(level, bb, 4, 3, 49, 20, 6, 63, stone);
		fill(level, bb, 4, 6, 49, 20, 6, 63, moss);
		fill(level, bb, 7, 6, 51, 17, 6, 61, cherry);
		for (int i = 0; i < 4; i++) {
			fill(level, bb, 21 - i, 3 + i, 53, 21 - i, 3 + i, 57, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false));
		}
		for (int[] gp : new int[][]{{8, 52}, {16, 52}, {8, 60}, {16, 60}}) {
			fill(level, bb, gp[0], 7, gp[1], gp[0], 10, gp[1], darkLog);
		}
		for (int x = 8; x <= 16; x++) {
			if (x < 11 || x > 13) {
				set(level, bb, s(Blocks.SPRUCE_FENCE), x, 7, 52);
				set(level, bb, s(Blocks.SPRUCE_FENCE), x, 7, 60);
			}
		}
		set(level, bb, s(Blocks.LECTERN), 12, 7, 56);
		set(level, bb, s(Blocks.CHISELED_BOOKSHELF).setValue(ChiseledBookShelfBlock.FACING, Direction.SOUTH), 10, 7, 59);
		set(level, bb, s(Blocks.POTTED_CHERRY_SAPLING), 9, 7, 53);
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 12, 10, 56);
		chest(level, bb, 15, 7, 59, WildsSites.SAKURA_ONSEN_VAULT, Direction.NORTH, 67L);
		JapaneseArchitecture.ishidoro(p, 7, 7, 51);
		hipRoof(level, bb, 7, 51, 17, 61, 11, Blocks.DARK_OAK_STAIRS, cherry, s(Blocks.GOLD_BLOCK));
	}
}
