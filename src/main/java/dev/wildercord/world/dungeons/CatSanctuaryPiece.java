package dev.wildercord.world.dungeons;

import dev.wildercord.config.Config;
import dev.wildercord.monster.MonsterContent;
import dev.wildercord.wildlife.SakuraVillager;
import dev.wildercord.wildlife.ShrineCat;
import dev.wildercord.wildlife.ShrineCatContent;
import dev.wildercord.wildlife.ShrineCatRules;
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
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * The Cat Sanctuary: monumental four-story Japanese pagoda dungeon modeled after castle-pagoda
 * reference sheets. Features a white-plaster walled forecourt with twin Yagura corner watchtowers,
 * Myojin Torii gate, sunken koi canal &amp; arched crimson bridge, Chozuya and Ema votive pavilions;
 * multi-tiered curved deepslate-tile pagoda roofs with kumimono brackets &amp; golden Sorin finial;
 * richly furnished guardian chambers for {@code Yuki}, {@code Odie}, {@code Freya}, and {@code Kuro};
 * and a heavy garrison of 36 {@link dev.wildercord.monster.KarakuriSentinel} and
 * {@link dev.wildercord.monster.PaperLanternSpirit} defenders.
 */
public final class CatSanctuaryPiece extends DungeonPiece {
	public static final int WIDTH = 57, HEIGHT = 68, DEPTH = 74;
	public static final int F1 = 4, F2 = 13, F3 = 22, F4 = 31;

	private static final BlockState CHERRY_PLANKS = Blocks.CHERRY_PLANKS.defaultBlockState();
	private static final BlockState DARK_OAK = Blocks.DARK_OAK_PLANKS.defaultBlockState();
	private static final BlockState DARK_LOG = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
	private static final BlockState CHERRY_LOG = Blocks.STRIPPED_CHERRY_LOG.defaultBlockState();
	private static final BlockState BAMBOO_PLANKS = Blocks.BAMBOO_PLANKS.defaultBlockState();
	private static final BlockState BAMBOO_BLOCK = Blocks.BAMBOO_BLOCK.defaultBlockState();
	private static final BlockState STONE_BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
	private static final BlockState MOSSY_BRICKS = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
	private static final BlockState DEEPSLATE_TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
	private static final BlockState POLISHED_ANDESITE = Blocks.POLISHED_ANDESITE.defaultBlockState();
	private static final BlockState VERMILION = Blocks.RED_TERRACOTTA.defaultBlockState();
	private static final BlockState PAPER_WALL = Blocks.CALCITE.defaultBlockState();

	public CatSanctuaryPiece(int x, int y, int z, Direction facing) {
		super(DungeonWorldgen.CAT_SANCTUARY_PIECE, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public CatSanctuaryPiece(CompoundTag tag) {
		super(DungeonWorldgen.CAT_SANCTUARY_PIECE, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		var piece = new CatSanctuaryPiece(c.chunkPos().getMinBlockX(), 0, c.chunkPos().getMinBlockZ(),
			Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		var gen = c.chunkGenerator();
		int sea = gen.getSeaLevel();
		int high = Integer.MIN_VALUE, low = Integer.MAX_VALUE;
		for (int[] p : new int[][]{{28, 42}, {8, 20}, {48, 20}, {8, 64}, {48, 64}}) {
			var wp = piece.getWorldPos(p[0], 0, p[1]);
			int surface = gen.getFirstOccupiedHeight(wp.getX(), wp.getZ(), Heightmap.Types.WORLD_SURFACE_WG, c.heightAccessor(), c.randomState());
			int floor = gen.getFirstOccupiedHeight(wp.getX(), wp.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, c.heightAccessor(), c.randomState());
			if (surface <= sea + 1 || surface - floor > 2) return Optional.empty();
			high = Math.max(high, floor);
			low = Math.min(low, floor);
		}
		if (high - low > 10) return Optional.empty();
		int ground = (high + low) / 2;
		piece.move(0, ground - 1 - F1, 0);
		var entry = piece.getWorldPos(28, F1 + 1, 2);
		return Optional.of(new Structure.GenerationStub(new BlockPos(entry.getX(), ground, entry.getZ()), b -> b.addPiece(piece)));
	}

	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of(
			worldBox(6, F1, 18, 50, F2, 68),
			worldBox(8, F2, 20, 48, F3, 66),
			worldBox(10, F3, 22, 46, F4, 64),
			worldBox(12, F4, 24, 44, F4 + 11, 62)
		);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);
		BlockPlacer p = (x, y, z, st) -> set(level, bb, st, x, y, z);

		courtyard(level, bb, p);
		floor1(level, bb, p);
		floor2(level, bb, p);
		floor3(level, bb, p);
		floor4(level, bb, p);
		stairwells(level, bb);
		inhabitants(level, bb);
	}

	private void courtyard(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		for (int x = 2; x <= 54; x++) {
			for (int z = 0; z <= 70; z++) {
				fill(level, bb, x, 0, z, x, F1 - 1, z, noise(x, z) < 25 ? MOSSY_BRICKS : STONE_BRICKS);
				set(level, bb, Blocks.GRASS_BLOCK.defaultBlockState(), x, F1, z);
				fill(level, bb, x, F1 + 1, z, x, HEIGHT - 1, z, AIR);
			}
		}

		// White-plaster castle perimeter walls on stone plinths with dark-tile coping.
		for (int x = 3; x <= 53; x++) {
			if (x < 23 || x > 33) {
				set(level, bb, noise(x, 2) < 30 ? MOSSY_BRICKS : STONE_BRICKS, x, F1 + 1, 2);
				set(level, bb, PAPER_WALL, x, F1 + 2, 2);
				set(level, bb, JapaneseArchitecture.slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM), x, F1 + 3, 2);
			}
			set(level, bb, STONE_BRICKS, x, F1 + 1, 69);
			set(level, bb, PAPER_WALL, x, F1 + 2, 69);
			set(level, bb, JapaneseArchitecture.slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM), x, F1 + 3, 69);
		}
		for (int z = 3; z < 69; z++) {
			set(level, bb, noise(3, z) < 30 ? MOSSY_BRICKS : STONE_BRICKS, 3, F1 + 1, z);
			set(level, bb, PAPER_WALL, 3, F1 + 2, z);
			set(level, bb, JapaneseArchitecture.slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM), 3, F1 + 3, z);
			set(level, bb, noise(53, z) < 30 ? MOSSY_BRICKS : STONE_BRICKS, 53, F1 + 1, z);
			set(level, bb, PAPER_WALL, 53, F1 + 2, z);
			set(level, bb, JapaneseArchitecture.slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM), 53, F1 + 3, z);
		}

		// Twin 2-story Corner Yagura Watchtowers guarding the south perimeter.
		yaguraTower(level, bb, p, 3, 1);
		yaguraTower(level, bb, p, 49, 1);

		// Stone promenade beneath the pagoda & forecourt approach lane.
		for (int z = 0; z <= 17; z++) {
			for (int x = 25; x <= 31; x++) {
				set(level, bb, noise(x, z) < 35 ? MOSSY_BRICKS : POLISHED_ANDESITE, x, F1, z);
			}
		}
		fill(level, bb, 5, F1, 17, 51, F1, 68, STONE_BRICKS);
		for (int x = 24; x <= 32; x++) {
			set(level, bb, POLISHED_ANDESITE, x, F1 + 1, 17);
		}

		// Grand Forecourt Sunken Koi Canal (z = 11..13) crossed by an arched crimson bridge.
		for (int x = 6; x <= 50; x++) {
			for (int z = 11; z <= 13; z++) {
				set(level, bb, MOSSY_BRICKS, x, F1 - 1, z);
				set(level, bb, Blocks.WATER.defaultBlockState(), x, F1, z);
				if (x % 9 == 0 && z == 12) {
					set(level, bb, Blocks.SEA_LANTERN.defaultBlockState(), x, F1 - 1, z);
				} else if (Math.abs(x - 28) > 4 && noise(x, z) < 18) {
					set(level, bb, Blocks.LILY_PAD.defaultBlockState(), x, F1 + 1, z);
				}
			}
		}
		for (int x = 25; x <= 31; x++) {
			set(level, bb, stair(Blocks.CRIMSON_STAIRS, Direction.NORTH, false), x, F1 + 1, 11);
			set(level, bb, Blocks.CRIMSON_PLANKS.defaultBlockState(), x, F1 + 1, 12);
			set(level, bb, Blocks.CRIMSON_SLAB.defaultBlockState(), x, F1 + 2, 12);
			set(level, bb, stair(Blocks.CRIMSON_STAIRS, Direction.SOUTH, false), x, F1 + 1, 13);
		}
		for (int z = 11; z <= 13; z++) {
			set(level, bb, Blocks.CRIMSON_FENCE.defaultBlockState(), 25, F1 + 2, z);
			set(level, bb, Blocks.CRIMSON_FENCE.defaultBlockState(), 31, F1 + 2, z);
		}

		// Grand vermilion Myojin Torii gate & 10 stone ishidoro lanterns lining the avenue.
		JapaneseArchitecture.toriiGate(p, 28, F1 + 1, 5, 4, F1 + 6, true);
		for (int[] il : new int[][]{{21, 3}, {35, 3}, {22, 9}, {34, 9}, {22, 15}, {34, 15}, {10, 8}, {46, 8}, {5, 16}, {51, 16}}) {
			JapaneseArchitecture.ishidoro(p, il[0], F1 + 1, il[1]);
		}

		// West Chozuya Purification Basin Pavilion.
		fill(level, bb, 12, F1 + 1, 5, 16, F1 + 1, 9, POLISHED_ANDESITE);
		set(level, bb, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL),
			14, F1 + 2, 7);
		for (int[] cp : new int[][]{{12, 5}, {16, 5}, {12, 9}, {16, 9}}) {
			fill(level, bb, cp[0], F1 + 2, cp[1], cp[0], F1 + 4, cp[1], DARK_LOG);
		}
		hipRoof(level, bb, 12, 5, 16, 9, F1 + 5, Blocks.DEEPSLATE_TILE_STAIRS, DARK_OAK, DEEPSLATE_TILES);

		// East Ema Votive Tablet & Bronze Bell Pavilion.
		fill(level, bb, 40, F1 + 1, 5, 44, F1 + 1, 9, POLISHED_ANDESITE);
		for (int[] cp : new int[][]{{40, 5}, {44, 5}, {40, 9}, {44, 9}}) {
			fill(level, bb, cp[0], F1 + 2, cp[1], cp[0], F1 + 4, cp[1], VERMILION);
		}
		set(level, bb, Blocks.IRON_CHAIN.defaultBlockState(), 42, F1 + 4, 7);
		set(level, bb, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.SOUTH), 42, F1 + 3, 7);
		set(level, bb, Blocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(ChiseledBookShelfBlock.FACING, Direction.SOUTH), 42, F1 + 2, 5);
		hipRoof(level, bb, 40, 5, 44, 9, F1 + 5, Blocks.DEEPSLATE_TILE_STAIRS, VERMILION, DEEPSLATE_TILES);

		// Four Weeping Sakura Trees & Bamboo Clusters framing the forecourt.
		JapaneseArchitecture.weepingCherryTree(p, 9, F1 + 1, 15, 5, true);
		JapaneseArchitecture.weepingCherryTree(p, 47, F1 + 1, 15, 5, true);
		JapaneseArchitecture.weepingCherryTree(p, 7, F1 + 1, 56, 5, true);
		JapaneseArchitecture.weepingCherryTree(p, 49, F1 + 1, 56, 5, true);
		for (int[] bm : new int[][]{{18, 6, 5}, {19, 8, 4}, {37, 8, 5}, {38, 6, 4}}) {
			for (int y = F1 + 1; y <= F1 + bm[2]; y++) {
				set(level, bb, BAMBOO_BLOCK, bm[0], y, bm[1]);
			}
		}
		for (int x = 5; x < 52; x++) {
			for (int z = 3; z < 17; z++) {
				if (Math.abs(x - 28) > 4 && Math.abs(z - 12) > 1 && noise(x, z) < 16) {
					set(level, bb, Blocks.PINK_PETALS.defaultBlockState(), x, F1 + 1, z);
				}
			}
		}
	}

	private void yaguraTower(WorldGenLevel level, BoundingBox bb, BlockPlacer p, int x0, int z0) {
		int x1 = x0 + 4, z1 = z0 + 4;
		fill(level, bb, x0, F1 + 1, z0, x1, F1 + 3, z1, STONE_BRICKS);
		for (int y = F1 + 4; y <= F1 + 7; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
					boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
					if (edge) {
						set(level, bb, corner ? DARK_LOG : PAPER_WALL, x, y, z);
					}
				}
			}
		}
		// Open south & roomward archwindows so interior is lit.
		set(level, bb, Blocks.LANTERN.defaultBlockState(), x0 + 2, F1 + 4, z0 + 2);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), x0 + 2, F1 + 5, z0);
		hipRoof(level, bb, x0 - 1, z0 - 1, x1 + 1, z1 + 1, F1 + 8, Blocks.DEEPSLATE_TILE_STAIRS, DARK_OAK, Blocks.GOLD_BLOCK.defaultBlockState());
	}

	private void floor1(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		room(level, bb, 6, F1, 18, 50, F2, 68, DARK_OAK);
		pagodaWalls(p, 6, F1 + 1, 18, 50, F2 - 1, 68, DARK_LOG, VERMILION, true);
		fill(level, bb, 7, F1, 19, 49, F1, 67, CHERRY_PLANKS);

		// Grand 7x5 main entrance vestibule & flanking Samurai Armory Alcoves (wide open side walkways).
		fill(level, bb, 25, F1 + 1, 18, 31, F1 + 5, 18, AIR);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 24, F1 + 2, 17);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 32, F1 + 2, 17);
		for (int x : new int[]{10, 14, 42, 46}) {
			set(level, bb, Blocks.IRON_BARS.defaultBlockState(), x, F1 + 1, 19);
			set(level, bb, Blocks.IRON_BARS.defaultBlockState(), x, F1 + 2, 19);
			set(level, bb, Blocks.BARREL.defaultBlockState(), x + 1, F1 + 1, 19);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), x + 1, F1 + 2, 19);
		}
		for (int px : new int[]{24, 32}) {
			fill(level, bb, px, F1 + 1, 25, px, F1 + 5, 25, VERMILION);
		}
		fill(level, bb, 23, F1 + 5, 25, 33, F1 + 5, 25, VERMILION);

		// Interior vermilion colonnade, kumimono ceiling crossbeams & warm hanging lanterns.
		interiorColonnade(level, bb, 15, 41, new int[]{27, 36, 46, 52}, F1 + 1, F2 - 1, VERMILION, DARK_LOG);
		ceilingCrossbeams(level, bb, 7, 49, new int[]{27, 36, 46, 52}, F2 - 1, DARK_LOG, 28);

		// Twin sunken mossy koi brooks (3 blocks wide) with sea lanterns, lily pads & dual arched crimson bridges.
		for (int z = 28; z <= 52; z++) {
			for (int x : new int[]{16, 17, 18, 38, 39, 40}) {
				set(level, bb, MOSSY_BRICKS, x, F1 - 1, z);
				set(level, bb, Blocks.WATER.defaultBlockState(), x, F1, z);
				if (z % 6 == 0 && (x == 17 || x == 39)) {
					set(level, bb, Blocks.SEA_LANTERN.defaultBlockState(), x, F1 - 1, z);
				} else if (Math.abs(z - 42) > 2 && noise(x, z) < 20) {
					set(level, bb, Blocks.LILY_PAD.defaultBlockState(), x, F1 + 1, z);
				}
			}
			set(level, bb, Blocks.MOSS_BLOCK.defaultBlockState(), 15, F1, z);
			set(level, bb, Blocks.MOSS_BLOCK.defaultBlockState(), 41, F1, z);
			if (z == 28 || z == 52) {
				for (int y = F1 + 1; y <= F1 + 4; y++) {
					set(level, bb, BAMBOO_BLOCK, 15, y, z);
					set(level, bb, BAMBOO_BLOCK, 41, y, z);
				}
			}
		}
		for (int bx0 : new int[]{15, 37}) {
			for (int bz : new int[]{33, 34, 41, 42, 43}) {
				set(level, bb, Blocks.CRIMSON_SLAB.defaultBlockState(), bx0, F1 + 1, bz);
				fill(level, bb, bx0 + 1, F1 + 1, bz, bx0 + 3, F1 + 1, bz, Blocks.CRIMSON_PLANKS.defaultBlockState());
				set(level, bb, Blocks.CRIMSON_SLAB.defaultBlockState(), bx0 + 4, F1 + 1, bz);
			}
		}

		// Yuki's central Moss & Stone Shrine Dais (center tile stays flush at F1 + 1 for clear spawning).
		fill(level, bb, 24, F1, 38, 32, F1, 46, Blocks.MOSS_BLOCK.defaultBlockState());
		fill(level, bb, 26, F1, 40, 30, F1, 44, POLISHED_ANDESITE);
		for (int[] sl : new int[][]{{25, 39}, {31, 39}, {25, 45}, {31, 45}}) {
			set(level, bb, Blocks.SEA_LANTERN.defaultBlockState(), sl[0], F1, sl[1]);
		}
		for (int[] post : new int[][]{{24, 38}, {32, 38}, {24, 46}, {32, 46}}) {
			set(level, bb, Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState(), post[0], F1 + 1, post[1]);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), post[0], F1 + 2, post[1]);
		}
		set(level, bb, Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), 25, F1 + 1, 38);
		set(level, bb, Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), 31, F1 + 1, 38);
		set(level, bb, Blocks.MOSS_CARPET.defaultBlockState(), 28, F1 + 1, 42);

		// NE Tea-House Checkpoint Pavilion.
		JapaneseArchitecture.teaCheckpointPavilion(p, 40, F1, 57, 48, 65, BAMBOO_BLOCK, DARK_OAK,
			Blocks.RED_CARPET.defaultBlockState(), Direction.SOUTH);
		chest(level, bb, 47, F1 + 1, 64, DungeonWorldgen.CAT_SANCTUARY_HALL, Direction.WEST, 11);

		// Eave skirt & 2nd-story exterior balcony railing.
		JapaneseArchitecture.upturnedEaveSkirt(p, 5, 17, 51, 69, F2, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, DARK_LOG, true);
		balconyRailing(level, bb, 7, 19, 49, 67, F2 + 1);
	}

	private void floor2(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		room(level, bb, 8, F2, 20, 48, F3, 66, DARK_OAK);
		pagodaWalls(p, 8, F2 + 1, 20, 48, F3 - 1, 66, BAMBOO_BLOCK, CHERRY_LOG, true);
		for (int x = 9; x <= 47; x++) {
			for (int z = 21; z <= 65; z++) {
				boolean border = (x - 9) % 6 == 0 || (z - 21) % 6 == 0;
				set(level, bb, border ? DARK_LOG : BAMBOO_PLANKS, x, F2, z);
			}
		}

		// Cherry colonnade, coffered ceiling beams, warm lanterns & amber/gold banners.
		interiorColonnade(level, bb, 14, 40, new int[]{28, 37, 46, 56}, F2 + 1, F3 - 1, CHERRY_LOG, DARK_LOG);
		ceilingCrossbeams(level, bb, 9, 47, new int[]{28, 37, 46, 56}, F3 - 1, DARK_LOG, 28);
		banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.YELLOW), 14, F2 + 5, 36, Direction.SOUTH);
		banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.ORANGE), 40, F2 + 5, 36, Direction.SOUTH);

		// Zig-zag folding Byobu shoji screens with azalea bonsai pots creating multi-lane dojo skirmish lanes.
		for (int sz : new int[]{33, 49}) {
			for (int x : new int[]{11, 45}) {
				set(level, bb, DARK_LOG, x, F2 + 1, sz);
				set(level, bb, DARK_LOG, x, F2 + 2, sz);
				set(level, bb, JapaneseArchitecture.openTrapdoor(Blocks.SPRUCE_TRAPDOOR, x < 28 ? Direction.EAST : Direction.WEST),
					x, F2 + 1, sz + 1);
				set(level, bb, JapaneseArchitecture.openTrapdoor(Blocks.SPRUCE_TRAPDOOR, x < 28 ? Direction.EAST : Direction.WEST),
					x, F2 + 2, sz + 1);
				set(level, bb, Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), x, F2 + 1, sz - 1);
			}
		}

		// Tokonoma South Scroll Alcove + Four flanking Chabudai Tea Tables around Odie's central Kotatsu.
		for (int[] td : new int[][]{{21, 35}, {35, 35}, {21, 49}, {35, 49}}) {
			set(level, bb, JapaneseArchitecture.slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM), td[0], F2 + 1, td[1]);
			set(level, bb, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, 2).setValue(SeaPickleBlock.WATERLOGGED, false), td[0], F2 + 2, td[1]);
			set(level, bb, Blocks.YELLOW_CARPET.defaultBlockState(), td[0] - 1, F2 + 1, td[1]);
			set(level, bb, Blocks.ORANGE_CARPET.defaultBlockState(), td[0] + 1, F2 + 1, td[1]);
		}

		// Odie's central Sunbeam Kotatsu & Hearth Pavilion (suspended kettle above center carpet at (28, F2, 42)).
		fill(level, bb, 24, F2, 38, 32, F2, 46, CHERRY_PLANKS);
		fill(level, bb, 25, F2 + 1, 39, 31, F2 + 1, 45, Blocks.YELLOW_CARPET.defaultBlockState());
		fill(level, bb, 27, F2 + 1, 41, 29, F2 + 1, 43, Blocks.ORANGE_CARPET.defaultBlockState());
		set(level, bb, Blocks.GLOWSTONE.defaultBlockState(), 28, F2, 42);
		set(level, bb, Blocks.CAULDRON.defaultBlockState(), 28, F2 + 6, 42);
		set(level, bb, Blocks.IRON_CHAIN.defaultBlockState(), 28, F2 + 7, 42);
		for (int[] post : new int[][]{{24, 38}, {32, 38}, {24, 46}, {32, 46}}) {
			set(level, bb, DARK_LOG, post[0], F2 + 1, post[1]);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), post[0], F2 + 2, post[1]);
		}

		// NW Tea-House Checkpoint Pavilion.
		JapaneseArchitecture.teaCheckpointPavilion(p, 10, F2, 22, 17, 29, BAMBOO_BLOCK, CHERRY_PLANKS,
			Blocks.YELLOW_CARPET.defaultBlockState(), Direction.NORTH);
		chest(level, bb, 11, F2 + 1, 23, DungeonWorldgen.CAT_SANCTUARY_HALL, Direction.EAST, 22);

		JapaneseArchitecture.upturnedEaveSkirt(p, 7, 19, 49, 67, F3, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, CHERRY_PLANKS, true);
		balconyRailing(level, bb, 9, 21, 47, 65, F3 + 1);
	}

	private void floor3(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		room(level, bb, 10, F3, 22, 46, F4, 64, DARK_OAK);
		pagodaWalls(p, 10, F3 + 1, 22, 46, F4 - 1, 64, CHERRY_PLANKS, VERMILION, false);
		fill(level, bb, 11, F3, 23, 45, F3, 63, CHERRY_PLANKS);

		// Ceremonial pink & magenta silk carpet runner through Freya's bell court.
		fill(level, bb, 26, F3 + 1, 25, 30, F3 + 1, 46, Blocks.MAGENTA_CARPET.defaultBlockState());
		fill(level, bb, 27, F3 + 1, 25, 29, F3 + 1, 46, Blocks.PINK_CARPET.defaultBlockState());

		// Vaulted vermilion colonnade, coffered beams & hanging lanterns (kept clear of F2->F3 & F3->F4 stairwells).
		interiorColonnade(level, bb, 12, 39, new int[]{26, 38, 46, 58}, F3 + 1, F4 - 1, VERMILION, DARK_LOG);
		ceilingCrossbeams(level, bb, 11, 45, new int[]{26, 38, 58}, F4 - 1, VERMILION, 28);

		// Four Quadrant Suzu Bell Pavilions (clear of east F2->F3 stairwell & central F3->F4 stairwell).
		for (int[] bp : new int[][]{{17, 34}, {37, 34}, {17, 48}, {37, 48}}) {
			int x = bp[0], z = bp[1];
			fill(level, bb, x - 1, F3, z - 1, x + 1, F3, z + 1, POLISHED_ANDESITE);
			for (int[] c : new int[][]{{x - 1, z - 1}, {x + 1, z - 1}, {x - 1, z + 1}, {x + 1, z + 1}}) {
				fill(level, bb, c[0], F3 + 1, c[1], c[0], F3 + 4, c[1], CHERRY_LOG);
			}
			fill(level, bb, x - 1, F3 + 5, z - 1, x + 1, F3 + 5, z + 1, CHERRY_PLANKS);
			set(level, bb, Blocks.IRON_CHAIN.defaultBlockState(), x, F3 + 4, z);
			set(level, bb, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, x < 28 ? Direction.EAST : Direction.WEST),
				x, F3 + 3, z);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), x, F3 + 6, z);
			banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.PINK), x < 28 ? x + 2 : x - 2, F3 + 4, z - 1,
				x < 28 ? Direction.EAST : Direction.WEST);
			set(level, bb, Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), x, F3 + 1, z - 2);
		}

		// Freya's central Silver-Bell Shrine Arch right behind her spawn tile (28, F3 + 1, 42).
		for (int px : new int[]{25, 31}) {
			fill(level, bb, px, F3 + 1, 44, px, F3 + 5, 44, VERMILION);
			set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), px, F3 + 1, 44);
		}
		fill(level, bb, 24, F3 + 5, 44, 32, F3 + 5, 44, VERMILION);
		set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), 28, F3 + 5, 44);
		set(level, bb, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.NORTH), 28, F3 + 4, 44);
		set(level, bb, Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), 24, F3 + 1, 43);
		set(level, bb, Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), 32, F3 + 1, 43);

		// SE Tea-House Checkpoint Pavilion.
		JapaneseArchitecture.teaCheckpointPavilion(p, 36, F3, 54, 44, 62, CHERRY_LOG, CHERRY_PLANKS,
			Blocks.PINK_CARPET.defaultBlockState(), Direction.SOUTH);
		chest(level, bb, 43, F3 + 1, 61, DungeonWorldgen.CAT_SANCTUARY_HALL, Direction.WEST, 33);

		JapaneseArchitecture.upturnedEaveSkirt(p, 9, 21, 47, 65, F4, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, VERMILION, true);
		balconyRailing(level, bb, 11, 23, 45, 63, F4 + 1);
	}

	private void floor4(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		room(level, bb, 12, F4, 24, 44, F4 + 11, 62, DEEPSLATE_TILES);
		pagodaWalls(p, 12, F4 + 1, 24, 44, F4 + 10, 62, DEEPSLATE_TILES, VERMILION, false);
		fill(level, bb, 13, F4, 25, 43, F4, 61, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());

		// Coffered gilded-blackstone walkways & crying-obsidian Ley-line throne focal point at (28, F4, 40).
		fill(level, bb, 22, F4, 34, 34, F4, 46, Blocks.GILDED_BLACKSTONE.defaultBlockState());
		fill(level, bb, 23, F4, 35, 33, F4, 45, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		set(level, bb, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 28, F4, 40);
		for (int gx = 16; gx <= 40; gx += 4) {
			for (int gz = 28; gz <= 56; gz += 4) {
				if (Math.abs(gx - 28) > 4 || Math.abs(gz - 40) > 4) {
					set(level, bb, Blocks.GILDED_BLACKSTONE.defaultBlockState(), gx, F4, gz);
				}
			}
		}

		// Six Monumental Obsidian & Gilded-Blackstone Soul-Brazier Towers with soul campfires & red banners.
		for (int[] t : new int[][]{{16, 29}, {40, 29}, {16, 41}, {40, 41}, {16, 53}, {40, 53}}) {
			int tx = t[0], tz = t[1];
			fill(level, bb, tx - 1, F4 + 1, tz - 1, tx + 1, F4 + 1, tz + 1, Blocks.GILDED_BLACKSTONE.defaultBlockState());
			fill(level, bb, tx, F4 + 2, tz, tx, F4 + 4, tz, Blocks.OBSIDIAN.defaultBlockState());
			set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), tx, F4 + 5, tz);
			set(level, bb, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), tx, F4 + 6, tz);
			set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true),
				tx < 28 ? tx + 1 : tx - 1, F4 + 4, tz);
			banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.RED), tx, F4 + 4, tz < 41 ? tz + 1 : tz - 1,
				tz < 41 ? Direction.NORTH : Direction.SOUTH);
		}

		// Kuro's central Obsidian & Vermilion Torii Altar Arch + North Golden Vault Alcove.
		for (int px : new int[]{25, 31}) {
			fill(level, bb, px, F4 + 1, 43, px, F4 + 6, 43, VERMILION);
			set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), px, F4 + 1, 43);
		}
		fill(level, bb, 24, F4 + 6, 43, 32, F4 + 6, 43, VERMILION);
		set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), 28, F4 + 6, 43);

		fill(level, bb, 25, F4 + 1, 58, 31, F4 + 5, 61, Blocks.GOLD_BLOCK.defaultBlockState());
		fill(level, bb, 26, F4 + 1, 58, 30, F4 + 4, 60, AIR);
		chest(level, bb, 28, F4 + 1, 60, DungeonWorldgen.CAT_SANCTUARY_VAULT, Direction.NORTH, 44);

		// Open-span timber ceiling crossbeams & Amethyst + Tinted-Glass Starlit Celestial Dome flush in F4 + 11 ceiling.
		ceilingCrossbeams(level, bb, 13, 43, new int[]{30, 38, 46, 54}, F4 + 10, DARK_LOG, 28);
		fill(level, bb, 24, F4 + 11, 36, 32, F4 + 11, 44, Blocks.AMETHYST_BLOCK.defaultBlockState());
		fill(level, bb, 25, F4 + 11, 37, 31, F4 + 11, 43, Blocks.TINTED_GLASS.defaultBlockState());
		for (int[] sl : new int[][]{{24, 36}, {32, 36}, {24, 44}, {32, 44}}) {
			set(level, bb, Blocks.SEA_LANTERN.defaultBlockState(), sl[0], F4 + 11, sl[1]);
			set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), sl[0], F4 + 10, sl[1]);
		}

		// Sweeping hipped crown pagoda roof + ornate golden Sorin spire finial.
		JapaneseArchitecture.upturnedEaveSkirt(p, 11, 23, 45, 63, F4 + 12, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, VERMILION, true);
		hipRoof(level, bb, 11, 23, 45, 63, F4 + 12, Blocks.DEEPSLATE_TILE_STAIRS, DEEPSLATE_TILES, Blocks.GOLD_BLOCK.defaultBlockState());
		set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), 28, F4 + 30, 43);
		set(level, bb, VERMILION, 28, F4 + 31, 43);
		set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), 28, F4 + 32, 43);
		set(level, bb, VERMILION, 28, F4 + 33, 43);
		set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), 28, F4 + 34, 43);
		set(level, bb, Blocks.SEA_LANTERN.defaultBlockState(), 28, F4 + 35, 43);
	}

	/** Timber-framed shoji pagoda walls with stone plinth row, kumimono top corbels & spruce trapdoor window lattice. */
	private void pagodaWalls(BlockPlacer p, int x0, int y0, int z0, int x1, int y1, int z1,
			BlockState frame, BlockState pillar, boolean shojiLattice) {
		for (int y = y0; y <= y1; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					boolean onX = x == x0 || x == x1;
					boolean onZ = z == z0 || z == z1;
					if (!onX && !onZ) {
						continue;
					}
					boolean corner = onX && onZ;
					boolean bayPillar = (onZ && (x - x0) % 4 == 0) || (onX && (z - z0) % 4 == 0);
					BlockState chosen = corner || bayPillar
						? pillar
						: (y == y0 ? DEEPSLATE_TILES : (y == y1 || y == y0 + 3) ? frame : PAPER_WALL);
					p.set(x, y, z, chosen);
					if (shojiLattice && !corner && !bayPillar && y == y0 + 2) {
						if (onZ && (x - x0) % 4 == 2) {
							p.set(x, y, z == z0 ? z + 1 : z - 1, JapaneseArchitecture.openTrapdoor(Blocks.SPRUCE_TRAPDOOR,
								z == z0 ? Direction.SOUTH : Direction.NORTH));
						} else if (onX && (z - z0) % 4 == 2) {
							p.set(x == x0 ? x + 1 : x - 1, y, z, JapaneseArchitecture.openTrapdoor(Blocks.SPRUCE_TRAPDOOR,
								x == x0 ? Direction.EAST : Direction.WEST));
						}
					}
				}
			}
		}
	}

	private void interiorColonnade(WorldGenLevel level, BoundingBox bb, int lx, int rx, int[] zs, int y0, int y1,
			BlockState col, BlockState bracket) {
		for (int z : zs) {
			for (int x : new int[]{lx, rx}) {
				fill(level, bb, x, y0, z, x, y1, z, col);
				set(level, bb, stair(Blocks.DARK_OAK_STAIRS, x < 28 ? Direction.EAST : Direction.WEST, true),
					x + (x < 28 ? 1 : -1), y1, z);
				set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true),
					x + (x < 28 ? 1 : -1), y1 - 1, z);
			}
		}
	}

	private void ceilingCrossbeams(WorldGenLevel level, BoundingBox bb, int x0, int x1, int[] zs, int beamY,
			BlockState beam, int centerX) {
		for (int z : zs) {
			fill(level, bb, x0, beamY, z, x1, beamY, z, beam);
			set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), centerX, beamY - 1, z);
		}
	}

	private void balconyRailing(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1, int y) {
		BlockState fence = Blocks.DARK_OAK_FENCE.defaultBlockState();
		for (int x = x0; x <= x1; x++) {
			set(level, bb, fence, x, y, z0);
			set(level, bb, fence, x, y, z1);
		}
		for (int z = z0 + 1; z < z1; z++) {
			set(level, bb, fence, x0, y, z);
			set(level, bb, fence, x1, y, z);
		}
	}

	private void stairwells(WorldGenLevel level, BoundingBox bb) {
		stairFlight(level, bb, 10, 13, F1, 54, Direction.NORTH);
		for (int z = 54; z <= 62; z++) {
			set(level, bb, Blocks.DARK_OAK_FENCE.defaultBlockState(), 14, F2 + 1, z);
		}
		for (int x = 10; x <= 13; x++) {
			set(level, bb, Blocks.DARK_OAK_FENCE.defaultBlockState(), x, F2 + 1, 53);
		}
		stairFlight(level, bb, 41, 44, F2, 27, Direction.NORTH);
		for (int z = 27; z <= 35; z++) {
			set(level, bb, Blocks.DARK_OAK_FENCE.defaultBlockState(), 40, F3 + 1, z);
		}
		for (int x = 41; x <= 44; x++) {
			set(level, bb, Blocks.DARK_OAK_FENCE.defaultBlockState(), x, F3 + 1, 26);
		}
		stairFlight(level, bb, 27, 29, F3, 47, Direction.NORTH);
		for (int z = 47; z <= 55; z++) {
			set(level, bb, Blocks.DARK_OAK_FENCE.defaultBlockState(), 26, F4 + 1, z);
			set(level, bb, Blocks.DARK_OAK_FENCE.defaultBlockState(), 30, F4 + 1, z);
		}
		for (int x = 27; x <= 29; x++) {
			set(level, bb, Blocks.DARK_OAK_FENCE.defaultBlockState(), x, F4 + 1, 46);
		}
	}

	private void stairFlight(WorldGenLevel level, BoundingBox bb, int x0, int x1, int baseY, int startZ, Direction dir) {
		for (int step = 0; step < 9; step++) {
			int z = startZ + step;
			int stairY = baseY + 1 + step;
			fill(level, bb, x0, baseY + 1, z, x1, stairY - 1, z, CHERRY_PLANKS);
			fill(level, bb, x0, stairY, z, x1, stairY, z, stair(Blocks.CHERRY_STAIRS, dir, false));
			fill(level, bb, x0, stairY + 1, z, x1, Math.min(HEIGHT - 2, baseY + 14), z, AIR);
		}
	}

	private void inhabitants(WorldGenLevel level, BoundingBox bb) {
		BlockPos heart = getWorldPos(28, F4 + 1, 40).immutable();
		spawnCat(level, bb, 28, F1 + 1, 42, ShrineCatRules.Variant.YUKI, heart);
		spawnCat(level, bb, 28, F2 + 1, 42, ShrineCatRules.Variant.ODIE, heart);
		spawnCat(level, bb, 28, F3 + 1, 42, ShrineCatRules.Variant.FREYA, heart);
		spawnCat(level, bb, 28, F4 + 1, 40, ShrineCatRules.Variant.KURO, heart);

		BlockPos keeperPos = getWorldPos(28, F1 + 1, 20);
		if (bb.isInside(keeperPos)) {
			SakuraVillager keeper = ShrineCatContent.SAKURA_VILLAGER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (keeper != null) {
				keeper.snapTo(keeperPos.getX() + 0.5, keeperPos.getY(), keeperPos.getZ() + 0.5, 180.0F, 0.0F);
				keeper.configure(SakuraVillager.TEA_MASTER);
				level.addFreshEntityWithPassengers(keeper);
			}
		}

		if (Config.get().monsters().spawnRate() <= 0) {
			return;
		}
		// 36 Dungeon Defenders (18 KarakuriSentinels + 18 PaperLanternSpirits) across Forecourt + Floors 1..4:
		// Forecourt (4)
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 22, F1 + 1, 7);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 34, F1 + 1, 7);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 20, F1 + 2, 10);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 36, F1 + 2, 10);

		// Floor 1 (8)
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 14, F1 + 1, 24);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 42, F1 + 1, 24);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 21, F1 + 1, 47);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 35, F1 + 1, 47);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 11, F1 + 2, 36);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 45, F1 + 2, 36);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 19, F1 + 2, 58);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 37, F1 + 2, 58);

		// Floor 2 (8)
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 15, F2 + 1, 34);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 39, F2 + 1, 34);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 20, F2 + 1, 52);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 36, F2 + 1, 52);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 13, F2 + 2, 46);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 43, F2 + 2, 46);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 22, F2 + 2, 26);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 34, F2 + 2, 26);

		// Floor 3 (8)
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 21, F3 + 1, 30);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 33, F3 + 1, 30);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 21, F3 + 1, 48);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 33, F3 + 1, 48);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 14, F3 + 2, 40);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 42, F3 + 2, 40);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 16, F3 + 2, 56);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 32, F3 + 2, 56);

		// Floor 4 (8)
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 20, F4 + 1, 33);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 36, F4 + 1, 33);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 20, F4 + 1, 48);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 36, F4 + 1, 48);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 15, F4 + 2, 35);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 41, F4 + 2, 35);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 22, F4 + 2, 54);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 34, F4 + 2, 54);
	}

	private void spawnCat(WorldGenLevel level, BoundingBox bb, int x, int y, int z, ShrineCatRules.Variant variant, BlockPos heart) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!bb.isInside(pos)) {
			return;
		}
		ShrineCat cat = ShrineCatContent.SHRINE_CAT.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (cat == null) {
			return;
		}
		cat.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180.0F, 0.0F);
		cat.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		cat.configureGuardian(variant, pos, heart);
		level.addFreshEntityWithPassengers(cat);
	}
}
