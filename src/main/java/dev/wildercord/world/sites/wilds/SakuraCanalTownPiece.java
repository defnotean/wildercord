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
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * Sakura Canal &amp; Lantern Harbor Village: terraced stone sea-lantern canal crossed by twin arched
 * crimson Taiko-bashi drum bridges and overhead lantern garlands, a two-story Watermill &amp; Tea
 * Brewery with a timber paddle wheel, a Bamboo Dockhouse &amp; Pier with a moored skiff, a Chochin
 * Lantern-Maker's Workshop, and a hillside five-Torii staircase climbing to an Inari Shrine.
 */
public final class SakuraCanalTownPiece extends WildsPiece {
	private static final int WIDTH = 64, HEIGHT = 26, DEPTH = 68;

	public SakuraCanalTownPiece(int x, int z, Direction facing) {
		super(WildsSites.SAKURA_CANAL_TOWN, x, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public SakuraCanalTownPiece(CompoundTag tag) {
		super(WildsSites.SAKURA_CANAL_TOWN, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		return surface(c, new SakuraCanalTownPiece(c.chunkPos().getMinBlockX(), c.chunkPos().getMinBlockZ(), facing(c)), 10);
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
		fill(level, bb, 0, 0, 0, WIDTH - 1, 1, DEPTH - 1, s(Blocks.DIRT));
		fill(level, bb, 0, 2, 0, WIDTH - 1, 2, DEPTH - 1, s(Blocks.GRASS_BLOCK));
		clear(level, bb, 0, 3, 0, WIDTH - 1, HEIGHT - 1, DEPTH - 1);

		canalAndBridges(level, bb, p);
		watermillBrewery(level, bb, p);
		lanternWorkshop(level, bb, p);
		bambooDockhouseAndSkiff(level, bb, p);
		hillsideToriiAndShrine(level, bb, p);

		for (int[] t : new int[][]{{10, 10, 5}, {18, 17, 4}, {48, 10, 5}, {56, 20, 4}, {12, 44, 4}, {44, 41, 4}, {58, 60, 5}}) {
			JapaneseArchitecture.weepingCherryTree(p, t[0], 3, t[1], t[2], true);
		}
		scatterGardenDetails(level, bb, p);

		SakuraVillageHelper.spawnVillager(level, bb, getWorldPos(39, 3, 30), SakuraVillager.FISHERMAN);
		SakuraVillageHelper.spawnVillager(level, bb, getWorldPos(19, 3, 54), SakuraVillager.LANTERN_MAKER);
		SakuraVillageHelper.spawnVillager(level, bb, getWorldPos(17, 4, 31), SakuraVillager.TEA_MASTER);
		SakuraVillageHelper.spawnVillager(level, bb, getWorldPos(51, 8, 59), SakuraVillager.SHRINE_MAIDEN);
		SakuraVillageHelper.spawnCat(level, bb, getWorldPos(21, 3, 29), ShrineCatRules.Variant.YUKI);
		SakuraVillageHelper.spawnCat(level, bb, getWorldPos(46, 3, 29), ShrineCatRules.Variant.ODIE);
	}

	private void canalAndBridges(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState stone = s(Blocks.STONE_BRICKS);
		BlockState mossy = s(Blocks.MOSSY_STONE_BRICKS);
		BlockState andesite = s(Blocks.POLISHED_ANDESITE);

		// Central sunken canal (x = 26..37, z = 1..66) framed by stone-brick lock weirs at ends & stone quays on sides.
		fill(level, bb, 25, -2, 0, 38, 2, 0, stone);
		fill(level, bb, 25, -2, DEPTH - 1, 38, 2, DEPTH - 1, stone);
		for (int z = 1; z <= DEPTH - 2; z++) {
			for (int x = 26; x <= 37; x++) {
				set(level, bb, stone, x, -2, z);
				set(level, bb, s(Blocks.WATER), x, -1, z);
				set(level, bb, s(Blocks.WATER), x, 0, z);
				set(level, bb, AIR, x, 1, z);
				set(level, bb, AIR, x, 2, z);
				if ((x == 27 || x == 36) && z % 6 == 0) {
					set(level, bb, s(Blocks.SEA_LANTERN), x, -2, z);
				} else if (x == 31 && z % 9 == 0) {
					set(level, bb, s(Blocks.SEA_LANTERN), x, -2, z);
				} else if (Math.abs(z - 18) > 3 && Math.abs(z - 31) > 4 && Math.abs(z - 44) > 3 && noise(x, z) < 10) {
					set(level, bb, s(Blocks.LILY_PAD), x, 1, z);
				}
			}
			for (int y = -1; y <= 2; y++) {
				set(level, bb, noise(25, y, z) < 35 ? mossy : stone, 25, y, z);
				set(level, bb, noise(38, y, z) < 35 ? mossy : stone, 38, y, z);
			}
			for (int x = 22; x <= 25; x++) {
				set(level, bb, (x + z) % 2 == 0 ? andesite : s(Blocks.SMOOTH_STONE), x, 2, z);
			}
			for (int x = 38; x <= 41; x++) {
				set(level, bb, (x + z) % 2 == 0 ? andesite : s(Blocks.SMOOTH_STONE), x, 2, z);
			}
		}
		// Stone steps down to water on South-West and South-East quays.
		for (int z = 8; z <= 11; z++) {
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false), 25, 2, z);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false), 26, 1, z);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false), 38, 2, z);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false), 37, 1, z);
		}
		// Canal promenade lanterns along both quays.
		for (int lz : new int[]{6, 14, 22, 36, 50, 60}) {
			JapaneseArchitecture.ishidoro(p, 22, 3, lz);
			JapaneseArchitecture.ishidoro(p, 41, 3, lz);
		}

		// Twin arched crimson Taiko-bashi bridges (z = 16..20 and z = 42..46) clearing central skiff channel.
		for (int bz : new int[]{16, 42}) {
			for (int z = bz; z <= bz + 4; z++) {
				for (int x = 25; x <= 38; x++) {
					int dist = Math.min(Math.abs(x - 25), Math.abs(38 - x));
					int y = dist <= 1 ? 3 : (dist <= 3 ? 4 : 5);
					Direction climbDir = x < 31 ? Direction.EAST : Direction.WEST;
					BlockState deck = (dist == 0 || dist == 2)
						? stair(Blocks.CRIMSON_STAIRS, climbDir, false)
						: s(Blocks.CRIMSON_PLANKS);
					set(level, bb, deck, x, y, z);
					if (z == bz || z == bz + 4) {
						set(level, bb, s(Blocks.CRIMSON_FENCE), x, y + 1, z);
					}
				}
			}
			for (int[] lp : new int[][]{{25, bz}, {38, bz}, {25, bz + 4}, {38, bz + 4}}) {
				set(level, bb, s(Blocks.STRIPPED_DARK_OAK_LOG), lp[0], 4, lp[1]);
				set(level, bb, s(Blocks.LANTERN), lp[0], 5, lp[1]);
			}
		}

		// Overhead Chochin lantern garland arches strung across the canal.
		BlockState beam = s(Blocks.STRIPPED_DARK_OAK_LOG);
		for (int fz : new int[]{10, 29, 36, 54}) {
			fill(level, bb, 24, 3, fz, 24, 7, fz, beam);
			fill(level, bb, 39, 3, fz, 39, 7, fz, beam);
			fill(level, bb, 24, 7, fz, 39, 7, fz, beam);
			for (int lx : new int[]{27, 30, 33, 36}) {
				set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), lx, 6, fz);
			}
		}
	}

	private void watermillBrewery(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState spruce = s(Blocks.SPRUCE_PLANKS);
		BlockState bamboo = s(Blocks.BAMBOO_PLANKS);
		BlockState darkLog = s(Blocks.STRIPPED_DARK_OAK_LOG);
		BlockState paper = s(Blocks.CALCITE);
		BlockState cherry = s(Blocks.CHERRY_PLANKS);

		// Stone & timber base terrace + interior floor (y = 3).
		fill(level, bb, 6, 3, 23, 21, 3, 40, spruce);
		fill(level, bb, 7, 3, 24, 20, 3, 39, bamboo);
		JapaneseArchitecture.shojiPerimeter(p, 6, 4, 23, 21, 7, 40, darkLog, darkLog, spruce, paper, 4);
		fill(level, bb, 21, 4, 30, 21, 6, 33, AIR);
		set(level, bb, JapaneseArchitecture.openTrapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.EAST), 20, 4, 29);
		set(level, bb, JapaneseArchitecture.openTrapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.EAST), 20, 4, 34);

		// Ground floor Tea Brewery kettle, barrels, counter & dry teacups.
		fill(level, bb, 7, 4, 24, 9, 4, 26, s(Blocks.STONE_BRICKS));
		set(level, bb, s(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, true), 8, 4, 25);
		set(level, bb, s(Blocks.CAULDRON), 8, 6, 25);
		set(level, bb, s(Blocks.IRON_CHAIN), 8, 7, 25);
		for (int z = 28; z <= 35; z += 3) {
			set(level, bb, JapaneseArchitecture.slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM), 16, 4, z);
			set(level, bb, SakuraVillageHelper.dryTeaCups(2), 16, 5, z);
			set(level, bb, s(Blocks.GREEN_CARPET), 15, 4, z);
		}
		set(level, bb, s(Blocks.BARREL), 8, 4, 37);
		set(level, bb, s(Blocks.BARREL), 9, 4, 37);
		set(level, bb, s(Blocks.LANTERN), 21, 5, 28);
		set(level, bb, s(Blocks.LANTERN), 21, 5, 35);
		chest(level, bb, 8, 4, 27, WildsSites.SAKURA_CANAL_HALL, Direction.EAST, 31L);

		// Second story loft + upturned dark-oak eaves & deepslate roof.
		JapaneseArchitecture.upturnedEaveSkirt(p, 5, 22, 22, 41, 8, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, darkLog, true);
		fill(level, bb, 8, 8, 25, 19, 8, 38, spruce);
		JapaneseArchitecture.shojiPerimeter(p, 8, 9, 25, 19, 11, 38, darkLog, darkLog, spruce, paper, 4);
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 14, 11, 31);
		hipRoof(level, bb, 7, 24, 20, 39, 12, Blocks.DEEPSLATE_TILE_STAIRS, cherry, s(Blocks.STRIPPED_CHERRY_LOG));

		// Iconic canal-side Timber Paddle Wheel dipping into water (x = 26, y = 0..6, z = 28..34).
		fill(level, bb, 26, -2, 31, 26, 0, 31, s(Blocks.STONE_BRICKS));
		fill(level, bb, 21, 3, 31, 26, 3, 31, darkLog);
		for (int dz = -3; dz <= 3; dz++) {
			for (int dy = -3; dy <= 3; dy++) {
				int r2 = dz * dz + dy * dy;
				if (r2 >= 6 && r2 <= 11) {
					set(level, bb, s(Blocks.DARK_OAK_PLANKS), 26, 3 + dy, 31 + dz);
					set(level, bb, JapaneseArchitecture.slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM), 27, 3 + dy, 31 + dz);
				} else if (dy == 0 || dz == 0 || Math.abs(dy) == Math.abs(dz)) {
					set(level, bb, s(Blocks.SPRUCE_FENCE), 26, 3 + dy, 31 + dz);
				}
			}
		}
		set(level, bb, darkLog, 26, 3, 31);
	}

	private void lanternWorkshop(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState darkLog = s(Blocks.STRIPPED_DARK_OAK_LOG);
		BlockState spruce = s(Blocks.SPRUCE_PLANKS);
		BlockState paper = s(Blocks.CALCITE);

		fill(level, bb, 6, 3, 48, 20, 3, 61, spruce);
		JapaneseArchitecture.shojiPerimeter(p, 6, 4, 48, 20, 8, 61, darkLog, darkLog, s(Blocks.DARK_OAK_PLANKS), paper, 4);
		fill(level, bb, 20, 4, 51, 20, 6, 58, AIR);

		// Hanging glowing red-and-gold Chochin lantern display rows outside & under porch eaves.
		fill(level, bb, 21, 7, 49, 21, 7, 60, s(Blocks.DARK_OAK_PLANKS));
		for (int z = 50; z <= 59; z += 2) {
			set(level, bb, z % 4 == 0 ? s(Blocks.OCHRE_FROGLIGHT) : s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 21, 6, z);
			set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 21, 5, z);
		}
		set(level, bb, s(Blocks.LOOM), 8, 4, 51);
		set(level, bb, s(Blocks.CRAFTING_TABLE), 8, 4, 53);
		set(level, bb, s(Blocks.RED_WOOL), 8, 4, 55);
		set(level, bb, s(Blocks.YELLOW_WOOL), 8, 4, 56);
		set(level, bb, s(Blocks.BARREL), 8, 4, 58);
		banner(level, bb, Blocks.RED_WALL_BANNER, 19, 6, 50, Direction.EAST);
		banner(level, bb, Blocks.YELLOW_WALL_BANNER, 19, 6, 59, Direction.EAST);

		JapaneseArchitecture.upturnedEaveSkirt(p, 5, 47, 21, 62, 9, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, darkLog, true);
		hipRoof(level, bb, 6, 48, 20, 61, 10, Blocks.DEEPSLATE_TILE_STAIRS, spruce, s(Blocks.GOLD_BLOCK));
	}

	private void bambooDockhouseAndSkiff(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState bambooPlanks = s(Blocks.BAMBOO_PLANKS);
		BlockState bambooBlock = s(Blocks.BAMBOO_BLOCK);
		BlockState bambooFence = s(Blocks.BAMBOO_FENCE);

		// Pier decking jutting onto east canal bank (x = 35..43, y = 2, z = 26..35) with bamboo pilings.
		for (int[] leg : new int[][]{{35, 26}, {35, 35}, {39, 26}, {39, 35}, {43, 26}, {43, 35}}) {
			fill(level, bb, leg[0], -1, leg[1], leg[0], 1, leg[1], bambooBlock);
		}
		fill(level, bb, 35, 2, 26, 43, 2, 35, bambooPlanks);
		for (int z : new int[]{26, 35}) {
			fill(level, bb, 35, 3, z, 37, 3, z, bambooFence);
		}
		// Bamboo Dockhouse canopy pavilion on pier (x = 39..43, y = 3..6, z = 27..34).
		for (int[] post : new int[][]{{39, 27}, {43, 27}, {39, 34}, {43, 34}}) {
			fill(level, bb, post[0], 3, post[1], post[0], 6, post[1], bambooBlock);
		}
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 39, 5, 27);
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 39, 5, 34);
		set(level, bb, s(Blocks.BARREL), 42, 3, 28);
		set(level, bb, s(Blocks.BARREL), 42, 3, 29);
		set(level, bb, s(Blocks.SMOKER), 42, 3, 33);
		gableRoof(level, bb, 39, 27, 43, 34, 7, false, Blocks.BAMBOO_STAIRS, bambooPlanks, Palette.of(bambooBlock, 1));

		// Moored wooden skiff floating gently in canal beside the pier (x = 31..33, y = 0..1, z = 28..33).
		fill(level, bb, 31, 0, 29, 33, 0, 32, s(Blocks.SPRUCE_PLANKS));
		fill(level, bb, 32, 1, 29, 32, 1, 32, AIR);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false), 32, 1, 28);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false), 32, 1, 33);
		set(level, bb, s(Blocks.SPRUCE_FENCE), 32, 2, 28);
		set(level, bb, s(Blocks.LANTERN), 32, 3, 28);
	}

	private void hillsideToriiAndShrine(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState stone = s(Blocks.STONE_BRICKS);
		BlockState mossy = s(Blocks.MOSSY_STONE_BRICKS);
		BlockState red = s(Blocks.RED_TERRACOTTA);
		BlockState cherry = s(Blocks.CHERRY_PLANKS);

		// Ascending stepped grassy & stone hillside on SE corner rising from y = 3 to summit y = 7.
		for (int i = 0; i < 5; i++) {
			int z0 = 44 + i * 3;
			int yTop = 3 + i;
			fill(level, bb, 42, 2, z0, 62, yTop - 1, z0 + 2, s(Blocks.DIRT));
			fill(level, bb, 42, yTop, z0, 62, yTop, z0 + 2, s(Blocks.GRASS_BLOCK));
			for (int x = 49; x <= 53; x++) {
				set(level, bb, noise(x, z0) < 35 ? mossy : stone, x, yTop, z0);
				set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false), x, yTop, z0 + 1);
				set(level, bb, stone, x, yTop, z0 + 2);
			}
			// Row of 5 vermilion Torii gates climbing the hill!
			JapaneseArchitecture.toriiGate(p, 51, yTop + 1, z0 + 1, 2, yTop + 4, i == 4);
		}
		// Summit plateau (z = 59..65, y = 7).
		fill(level, bb, 43, 2, 59, 61, 6, 65, s(Blocks.DIRT));
		fill(level, bb, 43, 7, 59, 61, 7, 65, s(Blocks.GRASS_BLOCK));
		fill(level, bb, 46, 7, 59, 56, 7, 65, stone);

		// Inari Shrine Pavilion at summit.
		JapaneseArchitecture.shojiPerimeter(p, 47, 8, 60, 55, 11, 64, red, cherry, stone, s(Blocks.CALCITE), 4);
		fill(level, bb, 49, 8, 60, 53, 10, 60, AIR);
		set(level, bb, s(Blocks.GOLD_BLOCK), 51, 8, 63);
		set(level, bb, s(Blocks.BELL).setValue(BellBlock.FACING, Direction.SOUTH), 51, 10, 61);
		set(level, bb, s(Blocks.BARREL), 48, 8, 61);
		chest(level, bb, 53, 8, 63, WildsSites.SAKURA_CANAL_VAULT, Direction.SOUTH, 47L);
		JapaneseArchitecture.ishidoro(p, 47, 8, 59);
		JapaneseArchitecture.ishidoro(p, 55, 8, 59);
		hipRoof(level, bb, 46, 59, 56, 65, 12, Blocks.DEEPSLATE_TILE_STAIRS, cherry, s(Blocks.GOLD_BLOCK));
	}

	private void scatterGardenDetails(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		for (int x = 2; x < WIDTH - 2; x++) {
			for (int z = 2; z < DEPTH - 2; z++) {
				if (getBlock(level, x, 2, z, bb).is(Blocks.GRASS_BLOCK) && getBlock(level, x, 3, z, bb).isAir()) {
					if (noise(x, z) < 12) {
						p.set(x, 3, z, s(Blocks.PINK_PETALS));
					} else if (noise(x + 5, z - 3) < 5) {
						p.set(x, 3, z, s(Blocks.MOSS_CARPET));
					}
				}
			}
		}
		for (int[] bm : new int[][]{{21, 14, 4}, {42, 14, 4}, {43, 39, 3}, {20, 46, 4}}) {
			for (int y = 3; y < 3 + bm[2]; y++) {
				set(level, bb, s(Blocks.BAMBOO_BLOCK), bm[0], y, bm[1]);
			}
		}
	}
}
