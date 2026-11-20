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
 * Sakura Feudal Castle-Town &amp; Dojo Market Village: white-plaster {@code Shirakabe} castle
 * walls with deepslate coping, three corner Yagura watchtowers, a fortified gatehouse &amp; front
 * moat bridge, a towering three-tier Village Bell-Pagoda centerpiece with a golden Sorin spire, a
 * two-story Kendo Dojo &amp; roaring Katana Swordsmith Forge, a Covered Lantern Market Arcade, and
 * a North Herbalist &amp; Apothecary Cottage with a bamboo herb garden.
 */
public final class SakuraCastleTownPiece extends WildsPiece {
	private static final int WIDTH = 64, HEIGHT = 34, DEPTH = 68;

	public SakuraCastleTownPiece(int x, int z, Direction facing) {
		super(WildsSites.SAKURA_CASTLE_TOWN, x, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public SakuraCastleTownPiece(CompoundTag tag) {
		super(WildsSites.SAKURA_CASTLE_TOWN, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		return surface(c, new SakuraCastleTownPiece(c.chunkPos().getMinBlockX(), c.chunkPos().getMinBlockZ(), facing(c)), 10);
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

		shirakabePerimeterAndMoat(level, bb, p);
		bellPagodaCenterpiece(level, bb, p);
		kendoDojoAndForge(level, bb, p);
		coveredMarketArcade(level, bb);
		herbalistCottageAndBamboo(level, bb, p);

		for (int[] t : new int[][]{{15, 7, 4}, {48, 7, 4}, {24, 18, 4}, {39, 18, 4}, {39, 41, 4}}) {
			JapaneseArchitecture.weepingCherryTree(p, t[0], 1, t[1], t[2], true);
		}
		for (int x = 2; x < WIDTH - 2; x++) {
			for (int z = 2; z < DEPTH - 2; z++) {
				if (getBlock(level, x, 0, z, bb).is(Blocks.GRASS_BLOCK) && getBlock(level, x, 1, z, bb).isAir() && noise(x, z) < 14) {
					set(level, bb, s(Blocks.PINK_PETALS), x, 1, z);
				}
			}
		}

		SakuraVillageHelper.spawnVillager(level, bb, getWorldPos(16, 2, 46), SakuraVillager.SWORDSMITH);
		SakuraVillageHelper.spawnVillager(level, bb, getWorldPos(49, 1, 28), SakuraVillager.LANTERN_MAKER);
		SakuraVillageHelper.spawnVillager(level, bb, getWorldPos(32, 3, 25), SakuraVillager.SHRINE_MAIDEN);
		SakuraVillageHelper.spawnVillager(level, bb, getWorldPos(28, 1, 46), SakuraVillager.TEA_MASTER);
		SakuraVillageHelper.spawnCat(level, bb, getWorldPos(32, 3, 32), ShrineCatRules.Variant.FREYA);
		SakuraVillageHelper.spawnCat(level, bb, getWorldPos(22, 2, 30), ShrineCatRules.Variant.KURO);
	}

	private void shirakabePerimeterAndMoat(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState stone = s(Blocks.STONE_BRICKS);
		BlockState mossy = s(Blocks.MOSSY_STONE_BRICKS);
		BlockState plaster = s(Blocks.CALCITE);
		BlockState red = s(Blocks.RED_TERRACOTTA);
		BlockState cap = JapaneseArchitecture.slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM);

		for (int x = 1; x <= WIDTH - 2; x++) {
			if (x < 28 || x > 36) {
				set(level, bb, noise(x, 1) < 35 ? mossy : stone, x, 1, 1);
				set(level, bb, stone, x, 2, 1);
				set(level, bb, plaster, x, 3, 1);
				set(level, bb, plaster, x, 4, 1);
				set(level, bb, cap, x, 5, 1);
			}
			set(level, bb, noise(x, 66) < 35 ? mossy : stone, x, 1, DEPTH - 2);
			set(level, bb, stone, x, 2, DEPTH - 2);
			set(level, bb, plaster, x, 3, DEPTH - 2);
			set(level, bb, plaster, x, 4, DEPTH - 2);
			set(level, bb, cap, x, 5, DEPTH - 2);
		}
		for (int z = 2; z < DEPTH - 2; z++) {
			set(level, bb, noise(1, z) < 35 ? mossy : stone, 1, 1, z);
			set(level, bb, stone, 1, 2, z);
			set(level, bb, plaster, 1, 3, z);
			set(level, bb, plaster, 1, 4, z);
			set(level, bb, cap, 1, 5, z);
			set(level, bb, noise(62, z) < 35 ? mossy : stone, WIDTH - 2, 1, z);
			set(level, bb, stone, WIDTH - 2, 2, z);
			set(level, bb, plaster, WIDTH - 2, 3, z);
			set(level, bb, plaster, WIDTH - 2, 4, z);
			set(level, bb, cap, WIDTH - 2, 5, z);
		}
		// Three Corner Yagura Watchtowers (SW, SE, NE).
		castleYagura(level, bb, p, 1, 1);
		castleYagura(level, bb, p, 56, 1);
		castleYagura(level, bb, p, 56, 60);

		// Ote-mon South Gatehouse + Courtyard Flagstones & Moat Channels.
		JapaneseArchitecture.toriiGate(p, 32, 1, 2, 4, 7, true);
		for (int x = 28; x <= 36; x++) {
			for (int z = 0; z <= 47; z++) {
				set(level, bb, (x + z) % 2 == 0 ? s(Blocks.SMOOTH_STONE) : s(Blocks.POLISHED_ANDESITE), x, 0, z);
			}
		}
		for (int x = 23; x <= 40; x++) {
			for (int z = 20; z <= 42; z++) {
				set(level, bb, (x + z) % 2 == 0 ? s(Blocks.SMOOTH_STONE) : s(Blocks.POLISHED_ANDESITE), x, 0, z);
			}
		}
		// Twin walled castle moat channels (z = 10..13) bridged by central crimson/stone span.
		for (int[] span : new int[][]{{9, 27}, {37, 55}}) {
			fill(level, bb, span[0] - 1, 0, 9, span[1] + 1, 0, 14, stone);
			for (int x = span[0]; x <= span[1]; x++) {
				for (int z = 10; z <= 13; z++) {
					set(level, bb, s(Blocks.SAND), x, -1, z);
					set(level, bb, s(Blocks.WATER), x, 0, z);
					if (x % 6 == 0 && z == 11) {
						set(level, bb, s(Blocks.SEA_LANTERN), x, -1, z);
					}
				}
			}
		}
		for (int z = 10; z <= 13; z++) {
			set(level, bb, red, 28, 1, z);
			set(level, bb, red, 36, 1, z);
		}
		JapaneseArchitecture.ishidoro(p, 26, 1, 6);
		JapaneseArchitecture.ishidoro(p, 38, 1, 6);
		JapaneseArchitecture.ishidoro(p, 24, 1, 22);
		JapaneseArchitecture.ishidoro(p, 40, 1, 22);
	}

	private void castleYagura(WorldGenLevel level, BoundingBox bb, BlockPlacer p, int x0, int z0) {
		int x1 = x0 + 6, z1 = z0 + 6;
		BlockState red = s(Blocks.RED_TERRACOTTA);
		fill(level, bb, x0, 1, z0, x1, 4, z1, s(Blocks.STONE_BRICKS));
		for (int y = 5; y <= 8; y++) {
			for (int x = x0 + 1; x <= x1 - 1; x++) {
				for (int z = z0 + 1; z <= z1 - 1; z++) {
					if (x == x0 + 1 || x == x1 - 1 || z == z0 + 1 || z == z1 - 1) {
						boolean corner = (x == x0 + 1 || x == x1 - 1) && (z == z0 + 1 || z == z1 - 1);
						set(level, bb, corner ? red : s(Blocks.CALCITE), x, y, z);
					}
				}
			}
		}
		JapaneseArchitecture.upturnedEaveSkirt(p, x0, z0, x1, z1, 9, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, s(Blocks.DARK_OAK_PLANKS), true);
		hipRoof(level, bb, x0 + 1, z0 + 1, x1 - 1, z1 - 1, 10, Blocks.DEEPSLATE_TILE_STAIRS, s(Blocks.CHERRY_PLANKS), s(Blocks.GOLD_BLOCK));
		set(level, bb, s(Blocks.GOLD_BLOCK), (x0 + x1) / 2, 13, (z0 + z1) / 2);
	}

	private void bellPagodaCenterpiece(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState stone = s(Blocks.POLISHED_ANDESITE);
		BlockState red = s(Blocks.RED_TERRACOTTA);
		BlockState plaster = s(Blocks.CALCITE);
		BlockState cherry = s(Blocks.CHERRY_PLANKS);

		// Raised plinth (y = 1..2).
		fill(level, bb, 26, 1, 26, 38, 1, 38, s(Blocks.STONE_BRICKS));
		fill(level, bb, 27, 2, 27, 37, 2, 37, stone);
		for (int i = 30; i <= 34; i++) {
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false), i, 1, 25);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false), i, 2, 26);
		}
		// Tier 1 Open Belfry Pavilion (y = 3..8) housing hanging bronze Bell & Altar Light.
		for (int[] col : new int[][]{{28, 28}, {36, 28}, {28, 36}, {36, 36}}) {
			fill(level, bb, col[0], 3, col[1], col[0], 7, col[1], red);
		}
		fill(level, bb, 29, 7, 29, 35, 7, 35, cherry);
		set(level, bb, s(Blocks.OCHRE_FROGLIGHT), 32, 3, 32);
		set(level, bb, s(Blocks.IRON_CHAIN), 32, 6, 32);
		set(level, bb, s(Blocks.BELL).setValue(BellBlock.FACING, Direction.SOUTH), 32, 5, 32);
		banner(level, bb, Blocks.RED_WALL_BANNER, 28, 6, 27, Direction.SOUTH);
		banner(level, bb, Blocks.RED_WALL_BANNER, 36, 6, 27, Direction.SOUTH);
		JapaneseArchitecture.upturnedEaveSkirt(p, 26, 26, 38, 38, 8, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, cherry, true);

		// Tier 2 Shrine Story (y = 9..14).
		fill(level, bb, 29, 9, 29, 35, 9, 35, s(Blocks.SPRUCE_PLANKS));
		JapaneseArchitecture.shojiPerimeter(p, 29, 10, 29, 35, 13, 35, red, red, cherry, plaster, 3);
		set(level, bb, s(Blocks.OCHRE_FROGLIGHT), 32, 11, 32);
		JapaneseArchitecture.upturnedEaveSkirt(p, 27, 27, 37, 37, 14, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, cherry, true);

		// Tier 3 Spire Belfry (y = 15..20) + Golden Sorin Finial reaching y = 31.
		fill(level, bb, 30, 15, 30, 34, 15, 34, s(Blocks.SPRUCE_PLANKS));
		JapaneseArchitecture.shojiPerimeter(p, 30, 16, 30, 34, 19, 34, red, red, cherry, plaster, 2);
		JapaneseArchitecture.upturnedEaveSkirt(p, 28, 28, 36, 36, 20, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, cherry, true);
		hipRoof(level, bb, 29, 29, 35, 35, 21, Blocks.DEEPSLATE_TILE_STAIRS, cherry, s(Blocks.GOLD_BLOCK));
		fill(level, bb, 32, 24, 32, 32, 27, 32, s(Blocks.GOLD_BLOCK));
		fill(level, bb, 32, 28, 32, 32, 30, 32, s(Blocks.IRON_BARS));
		set(level, bb, s(Blocks.GOLD_BLOCK), 32, 31, 32);
	}

	private void kendoDojoAndForge(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState red = s(Blocks.RED_TERRACOTTA);
		BlockState spruce = s(Blocks.SPRUCE_PLANKS);
		BlockState bamboo = s(Blocks.BAMBOO_PLANKS);
		BlockState plaster = s(Blocks.CALCITE);
		BlockState cherry = s(Blocks.CHERRY_PLANKS);

		// Grand 2-Story Kendo Dojo (x = 5..22, z = 18..42, engawa floor y = 1).
		fill(level, bb, 4, 1, 17, 23, 1, 43, spruce);
		for (int z = 27; z <= 33; z++) {
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false), 23, 1, z);
		}
		fill(level, bb, 6, 1, 19, 21, 1, 41, bamboo);
		JapaneseArchitecture.shojiPerimeter(p, 6, 2, 18, 21, 6, 42, red, red, spruce, plaster, 4);
		fill(level, bb, 21, 2, 26, 21, 5, 34, AIR);
		banner(level, bb, Blocks.RED_WALL_BANNER, 22, 5, 25, Direction.EAST);
		banner(level, bb, Blocks.YELLOW_WALL_BANNER, 22, 5, 35, Direction.EAST);

		// Weapon & armor display racks along front veranda & inside Dojo hall.
		for (int rz : new int[]{21, 23, 37, 39}) {
			set(level, bb, s(Blocks.SPRUCE_FENCE), 22, 2, rz);
			set(level, bb, s(Blocks.IRON_BARS), 22, 3, rz);
		}
		set(level, bb, s(Blocks.CHISELED_BOOKSHELF).setValue(ChiseledBookShelfBlock.FACING, Direction.EAST), 7, 2, 30);
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 14, 6, 30);
		chest(level, bb, 8, 2, 24, WildsSites.SAKURA_CASTLE_VAULT, Direction.EAST, 71L);

		JapaneseArchitecture.upturnedEaveSkirt(p, 4, 17, 22, 43, 7, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, red, true);
		fill(level, bb, 7, 7, 20, 20, 7, 40, spruce);
		JapaneseArchitecture.shojiPerimeter(p, 7, 8, 20, 20, 11, 40, red, red, spruce, plaster, 4);
		JapaneseArchitecture.upturnedEaveSkirt(p, 5, 18, 22, 42, 12, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, red, true);
		hipRoof(level, bb, 6, 19, 21, 41, 13, Blocks.DEEPSLATE_TILE_STAIRS, cherry, s(Blocks.GOLD_BLOCK));

		// Roaring Katana Swordsmith Forge & Tapered Stone Chimney (x = 5..15, z = 44..54).
		fill(level, bb, 5, 1, 44, 15, 1, 54, s(Blocks.POLISHED_ANDESITE));
		fill(level, bb, 7, 1, 46, 11, 6, 51, s(Blocks.STONE_BRICKS));
		fill(level, bb, 8, 7, 47, 10, 12, 50, s(Blocks.DEEPSLATE_TILES));
		fill(level, bb, 8, 13, 47, 10, 13, 50, s(Blocks.STONE_BRICKS));
		set(level, bb, s(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, true), 9, 14, 48);
		fill(level, bb, 8, 2, 47, 10, 3, 49, AIR);
		set(level, bb, s(Blocks.MAGMA_BLOCK), 9, 1, 48);
		set(level, bb, s(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, true), 9, 2, 48);
		set(level, bb, s(Blocks.IRON_BARS), 11, 2, 48);
		set(level, bb, s(Blocks.BLAST_FURNACE), 12, 2, 46);
		set(level, bb, s(Blocks.ANVIL), 13, 2, 49);
		set(level, bb, s(Blocks.GRINDSTONE), 13, 2, 51);
		set(level, bb, s(Blocks.SMITHING_TABLE), 12, 2, 52);
		set(level, bb, s(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL), 14, 2, 47);
	}

	private void coveredMarketArcade(WorldGenLevel level, BoundingBox bb) {
		BlockState redWool = s(Blocks.RED_WOOL);
		BlockState yellowWool = s(Blocks.YELLOW_WOOL);
		BlockState pinkWool = s(Blocks.PINK_WOOL);
		BlockState darkLog = s(Blocks.STRIPPED_DARK_OAK_LOG);
		BlockState spruce = s(Blocks.SPRUCE_PLANKS);

		// Flagstone Covered Market Lane (x = 41..58, z = 16..40).
		for (int x = 41; x <= 58; x++) {
			for (int z = 16; z <= 40; z++) {
				set(level, bb, (x + z) % 2 == 0 ? s(Blocks.SMOOTH_STONE) : s(Blocks.POLISHED_ANDESITE), x, 0, z);
			}
		}
		for (int z = 16; z <= 40; z += 6) {
			fill(level, bb, 42, 1, z, 42, 5, z, darkLog);
			fill(level, bb, 57, 1, z, 57, 5, z, darkLog);
			set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 49, 5, z);
		}
		// Striped Crimson / Saffron / Blossom arcade canopy + market booth counters.
		for (int z = 16; z <= 40; z++) {
			int stripe = Math.floorMod(z - 16, 6);
			BlockState fabric = stripe < 2 ? redWool : (stripe < 4 ? yellowWool : pinkWool);
			for (int x = 42; x <= 57; x++) {
				set(level, bb, fabric, x, 6, z);
			}
		}
		for (int z = 17; z <= 39; z++) {
			if (z % 6 != 0) {
				set(level, bb, spruce, 43, 1, z);
				set(level, bb, spruce, 56, 1, z);
			}
		}
		set(level, bb, s(Blocks.BARREL), 43, 1, 20);
		set(level, bb, s(Blocks.BARREL), 56, 1, 26);
		set(level, bb, s(Blocks.LECTERN), 43, 2, 29);
		set(level, bb, SakuraVillageHelper.dryTeaCups(3), 56, 2, 32);
		set(level, bb, s(Blocks.POTTED_FLOWERING_AZALEA), 43, 2, 35);
		banner(level, bb, Blocks.PINK_WALL_BANNER, 49, 5, 16, Direction.SOUTH);
		banner(level, bb, Blocks.YELLOW_WALL_BANNER, 49, 5, 40, Direction.NORTH);
		chest(level, bb, 56, 1, 20, WildsSites.SAKURA_CASTLE_HALL, Direction.WEST, 83L);
	}

	private void herbalistCottageAndBamboo(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		BlockState darkLog = s(Blocks.STRIPPED_DARK_OAK_LOG);
		BlockState spruce = s(Blocks.SPRUCE_PLANKS);
		BlockState plaster = s(Blocks.CALCITE);

		// North Herbalist & Apothecary Cottage (x = 21..39, z = 49..61).
		fill(level, bb, 21, 0, 49, 39, 0, 61, spruce);
		JapaneseArchitecture.shojiPerimeter(p, 21, 1, 49, 39, 5, 61, darkLog, darkLog, s(Blocks.STONE_BRICKS), plaster, 4);
		fill(level, bb, 29, 1, 49, 31, 3, 49, AIR);
		set(level, bb, s(Blocks.POTTED_RED_TULIP), 25, 2, 48);
		set(level, bb, s(Blocks.POTTED_ALLIUM), 35, 2, 48);
		set(level, bb, s(Blocks.BREWING_STAND), 24, 1, 58);
		set(level, bb, s(Blocks.COMPOSTER), 26, 1, 58);
		set(level, bb, s(Blocks.CAULDRON), 28, 1, 58);
		set(level, bb, s(Blocks.BARREL), 36, 1, 58);
		set(level, bb, s(Blocks.CHISELED_BOOKSHELF).setValue(ChiseledBookShelfBlock.FACING, Direction.NORTH), 34, 1, 58);
		set(level, bb, s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true), 30, 5, 55);
		gableRoof(level, bb, 20, 48, 40, 62, 6, true, Blocks.DEEPSLATE_TILE_STAIRS, s(Blocks.STRIPPED_CHERRY_LOG), Palette.of(plaster, 1));
		// Cottage chimney.
		fill(level, bb, 36, 1, 57, 37, 10, 58, s(Blocks.COBBLESTONE));
		set(level, bb, s(Blocks.CAMPFIRE).setValue(CampfireBlock.LIT, true), 36, 11, 57);

		// East Bamboo Herb Garden beside Herbalist Cottage (x = 43..53, z = 46..61).
		for (int x = 44; x <= 52; x += 2) {
			for (int z = 47; z <= 60; z += 3) {
				int h = 4 + noise(x, z) / 25;
				for (int y = 1; y <= h; y++) {
					set(level, bb, s(Blocks.BAMBOO_BLOCK), x, y, z);
				}
			}
		}
		JapaneseArchitecture.ishidoro(p, 42, 1, 52);
	}
}
