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
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * The Cat Sanctuary: four-story Japanese pagoda dungeon modeled 1:1 from exterior &amp; floor
 * reference sheets. Features a walled courtyard with grand Myojin Torii gate, ishidoro stone
 * lantern avenue, chozuya basin pavilion &amp; weeping cherry trees; multi-tiered curved deepslate-tile
 * roofs with kumimono brackets, balcony railings &amp; golden Sorin finial; and four richly detailed
 * guardian rooms for {@code Yuki}, {@code Odie}, {@code Freya}, and {@code Kuro}.
 */
public final class CatSanctuaryPiece extends DungeonPiece {
	public static final int WIDTH = 49, HEIGHT = 56, DEPTH = 64;
	public static final int F1 = 4, F2 = 11, F3 = 18, F4 = 25;

	private static final BlockState CHERRY_PLANKS = Blocks.CHERRY_PLANKS.defaultBlockState();
	private static final BlockState DARK_OAK = Blocks.DARK_OAK_PLANKS.defaultBlockState();
	private static final BlockState DARK_LOG = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
	private static final BlockState CHERRY_LOG = Blocks.STRIPPED_CHERRY_LOG.defaultBlockState();
	private static final BlockState BAMBOO_PLANKS = Blocks.BAMBOO_PLANKS.defaultBlockState();
	private static final BlockState BAMBOO_BLOCK = Blocks.BAMBOO_BLOCK.defaultBlockState();
	private static final BlockState STONE_BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
	private static final BlockState MOSSY_BRICKS = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
	private static final BlockState DEEPSLATE_TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
	private static final BlockState VERMILION = Blocks.DYED_TERRACOTTA.red().defaultBlockState();
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
		for (int[] p : new int[][]{{24, 36}, {8, 18}, {40, 18}, {8, 54}, {40, 54}}) {
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
		var entry = piece.getWorldPos(24, F1 + 1, 2);
		return Optional.of(new Structure.GenerationStub(new BlockPos(entry.getX(), ground, entry.getZ()), b -> b.addPiece(piece)));
	}

	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of(
			worldBox(6, F1, 16, 42, F2, 56),
			worldBox(8, F2, 18, 40, F3, 54),
			worldBox(10, F3, 20, 38, F4, 52),
			worldBox(12, F4, 22, 36, F4 + 10, 50)
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
		for (int x = 2; x <= 46; x++) {
			for (int z = 0; z <= 58; z++) {
				fill(level, bb, x, 0, z, x, F1 - 1, z, noise(x, z) < 25 ? MOSSY_BRICKS : STONE_BRICKS);
				set(level, bb, Blocks.GRASS_BLOCK.defaultBlockState(), x, F1, z);
				fill(level, bb, x, F1 + 1, z, x, HEIGHT - 1, z, AIR);
			}
		}
		// Stone courtyard perimeter garden wall with front entry opening & pink petal scatter.
		for (int x = 3; x <= 45; x++) {
			if (x < 19 || x > 29) {
				set(level, bb, STONE_BRICKS, x, F1 + 1, 2);
				set(level, bb, JapaneseArchitecture.slab(Blocks.STONE_BRICK_SLAB, SlabType.BOTTOM), x, F1 + 2, 2);
			}
			set(level, bb, STONE_BRICKS, x, F1 + 1, 57);
			set(level, bb, JapaneseArchitecture.slab(Blocks.STONE_BRICK_SLAB, SlabType.BOTTOM), x, F1 + 2, 57);
		}
		for (int z = 3; z < 57; z++) {
			set(level, bb, STONE_BRICKS, 3, F1 + 1, z);
			set(level, bb, Blocks.STONE_BRICK_WALL.defaultBlockState(), 3, F1 + 2, z);
			set(level, bb, STONE_BRICKS, 45, F1 + 1, z);
			set(level, bb, Blocks.STONE_BRICK_WALL.defaultBlockState(), 45, F1 + 2, z);
		}
		// Stone foundation promenade & main approach lane.
		for (int z = 0; z <= 15; z++) {
			for (int x = 21; x <= 27; x++) {
				set(level, bb, noise(x, z) < 35 ? MOSSY_BRICKS : Blocks.POLISHED_ANDESITE.defaultBlockState(), x, F1, z);
			}
		}
		fill(level, bb, 5, F1, 15, 43, F1, 56, STONE_BRICKS);
		for (int x = 20; x <= 28; x++) {
			set(level, bb, Blocks.POLISHED_ANDESITE.defaultBlockState(), x, F1 + 1, 15);
		}

		// Grand vermilion Myojin Torii gate & avenue of stone ishidoro lanterns.
		JapaneseArchitecture.toriiGate(p, 24, F1 + 1, 6, 4, F1 + 6, true);
		for (int[] il : new int[][]{{18, 3}, {30, 3}, {18, 10}, {30, 10}, {18, 14}, {30, 14}, {4, 3}, {44, 3}}) {
			JapaneseArchitecture.ishidoro(p, il[0], F1 + 1, il[1]);
		}
		// Chozuya purification basin pavilion in the left forecourt.
		fill(level, bb, 11, F1 + 1, 9, 15, F1 + 1, 13, Blocks.POLISHED_ANDESITE.defaultBlockState());
		set(level, bb, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL),
			13, F1 + 2, 11);
		for (int[] cp : new int[][]{{11, 9}, {15, 9}, {11, 13}, {15, 13}}) {
			fill(level, bb, cp[0], F1 + 2, cp[1], cp[0], F1 + 4, cp[1], DARK_LOG);
		}
		hipRoof(level, bb, 11, 9, 15, 13, F1 + 5, Blocks.DEEPSLATE_TILE_STAIRS, DARK_OAK, DEEPSLATE_TILES);

		// Ornamental weeping sakura trees & bamboo flanking the forecourt.
		JapaneseArchitecture.weepingCherryTree(p, 8, F1 + 1, 8, 5, true);
		JapaneseArchitecture.weepingCherryTree(p, 38, F1 + 1, 9, 5, true);
		for (int[] bm : new int[][]{{5, 10, 5}, {6, 12, 4}, {42, 11, 5}, {43, 8, 4}}) {
			for (int y = F1 + 1; y <= F1 + bm[2]; y++) {
				set(level, bb, BAMBOO_BLOCK, bm[0], y, bm[1]);
			}
		}
		for (int x = 5; x < 44; x++) {
			for (int z = 3; z < 15; z++) {
				if (Math.abs(x - 24) > 4 && noise(x, z) < 16) {
					set(level, bb, Blocks.PINK_PETALS.defaultBlockState(), x, F1 + 1, z);
				}
			}
		}
	}

	private void floor1(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		room(level, bb, 6, F1, 16, 42, F2, 56, DARK_OAK);
		pagodaWalls(p, 6, F1 + 1, 16, 42, F2 - 1, 56, DARK_LOG, VERMILION, true);
		fill(level, bb, 7, F1, 17, 41, F1, 55, CHERRY_PLANKS);
		// Grand 7x4 main entrance + decorative dark-oak door framing.
		fill(level, bb, 21, F1 + 1, 16, 27, F1 + 4, 16, AIR);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 20, F1 + 2, 15);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 28, F1 + 2, 15);

		// Interior vermilion colonnade, kumimono ceiling crossbeams & warm hanging lanterns.
		interiorColonnade(level, bb, 13, 35, new int[]{22, 30, 38, 46}, F1 + 1, F2 - 1, VERMILION, DARK_LOG);
		ceilingCrossbeams(level, bb, 7, 41, new int[]{22, 30, 38, 46}, F2 - 1, DARK_LOG, 24);

		// Twin sunken mossy koi brooks with glowing sea lanterns, lily pads, bamboo & arched crimson bridges.
		for (int z = 21; z <= 43; z++) {
			for (int x : new int[]{15, 16, 32, 33}) {
				set(level, bb, MOSSY_BRICKS, x, F1 - 1, z);
				set(level, bb, Blocks.WATER.defaultBlockState(), x, F1, z);
				if (z % 7 == 0 && (x == 15 || x == 33)) {
					set(level, bb, Blocks.SEA_LANTERN.defaultBlockState(), x, F1 - 1, z);
				} else if (Math.abs(z - 34) > 2 && noise(x, z) < 22) {
					set(level, bb, Blocks.LILY_PAD.defaultBlockState(), x, F1 + 1, z);
				}
			}
			set(level, bb, Blocks.MOSS_BLOCK.defaultBlockState(), 14, F1, z);
			set(level, bb, Blocks.MOSS_BLOCK.defaultBlockState(), 34, F1, z);
			if (z == 21 || z == 43) {
				for (int y = F1 + 1; y <= F1 + 4; y++) {
					set(level, bb, BAMBOO_BLOCK, 14, y, z);
					set(level, bb, BAMBOO_BLOCK, 34, y, z);
				}
			}
		}
		for (int bx0 : new int[]{14, 31}) {
			for (int bz = 33; bz <= 35; bz++) {
				set(level, bb, Blocks.CRIMSON_SLAB.defaultBlockState(), bx0, F1 + 1, bz);
				set(level, bb, Blocks.CRIMSON_PLANKS.defaultBlockState(), bx0 + 1, F1 + 1, bz);
				set(level, bb, Blocks.CRIMSON_PLANKS.defaultBlockState(), bx0 + 2, F1 + 1, bz);
				set(level, bb, Blocks.CRIMSON_SLAB.defaultBlockState(), bx0 + 3, F1 + 1, bz);
			}
		}

		// Yuki's central Moss & Stone Shrine Dais (center tile stays flush at F1 for clean camera/spawn).
		fill(level, bb, 21, F1, 31, 27, F1, 37, Blocks.MOSS_BLOCK.defaultBlockState());
		for (int[] sl : new int[][]{{22, 32}, {26, 32}, {22, 36}, {26, 36}}) {
			set(level, bb, Blocks.SEA_LANTERN.defaultBlockState(), sl[0], F1, sl[1]);
		}
		for (int[] post : new int[][]{{21, 31}, {27, 31}, {21, 37}, {27, 37}}) {
			set(level, bb, Blocks.MOSSY_STONE_BRICK_WALL.defaultBlockState(), post[0], F1 + 1, post[1]);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), post[0], F1 + 2, post[1]);
		}
		set(level, bb, Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), 22, F1 + 1, 31);
		set(level, bb, Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), 26, F1 + 1, 31);
		set(level, bb, Blocks.MOSS_CARPET.defaultBlockState(), 24, F1 + 1, 34);

		// NE Tea-House Checkpoint Pavilion (open toward hall so mobs & Ward checks walk in).
		JapaneseArchitecture.teaCheckpointPavilion(p, 33, F1, 47, 40, 54, BAMBOO_BLOCK, DARK_OAK,
			Blocks.CARPET.red().defaultBlockState(), Direction.SOUTH);
		chest(level, bb, 39, F1 + 1, 53, DungeonWorldgen.CAT_SANCTUARY_HALL, Direction.WEST, 11);

		// Eave skirt & 2nd-story exterior balcony railing.
		JapaneseArchitecture.upturnedEaveSkirt(p, 5, 15, 43, 57, F2, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, DARK_LOG, true);
		balconyRailing(level, bb, 7, 17, 41, 55, F2 + 1);
	}

	private void floor2(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		room(level, bb, 8, F2, 18, 40, F3, 54, DARK_OAK);
		pagodaWalls(p, 8, F2 + 1, 18, 40, F3 - 1, 54, BAMBOO_BLOCK, CHERRY_LOG, true);
		// Woven bamboo tatami floor grid with dark-oak border strips.
		for (int x = 9; x <= 39; x++) {
			for (int z = 19; z <= 53; z++) {
				boolean border = (x - 9) % 6 == 0 || (z - 19) % 6 == 0;
				set(level, bb, border ? DARK_LOG : BAMBOO_PLANKS, x, F2, z);
			}
		}

		// Cherry colonnade, coffered ceiling beams, warm lanterns & amber banners.
		interiorColonnade(level, bb, 13, 33, new int[]{22, 32, 40, 48}, F2 + 1, F3 - 1, CHERRY_LOG, DARK_LOG);
		ceilingCrossbeams(level, bb, 9, 39, new int[]{22, 32, 40, 48}, F3 - 1, DARK_LOG, 24);
		banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.YELLOW), 13, F2 + 5, 31, Direction.SOUTH);
		banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.ORANGE), 33, F2 + 5, 31, Direction.SOUTH);

		// Free-standing folding byobu shoji screens with potted azaleas along left & right galleries.
		for (int sz : new int[]{29, 38}) {
			for (int x : new int[]{11, 37}) {
				set(level, bb, DARK_LOG, x, F2 + 1, sz);
				set(level, bb, DARK_LOG, x, F2 + 2, sz);
				set(level, bb, JapaneseArchitecture.openTrapdoor(Blocks.SPRUCE_TRAPDOOR, x < 24 ? Direction.EAST : Direction.WEST),
					x, F2 + 1, sz + 1);
				set(level, bb, JapaneseArchitecture.openTrapdoor(Blocks.SPRUCE_TRAPDOOR, x < 24 ? Direction.EAST : Direction.WEST),
					x, F2 + 2, sz + 1);
				set(level, bb, Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), x, F2 + 1, sz - 1);
			}
		}

		// Odie's central Sunbeam Kotatsu & Hearth Dais (center tile at F2 so Odie loaves clearly in frame).
		fill(level, bb, 21, F2, 31, 27, F2, 37, CHERRY_PLANKS);
		fill(level, bb, 22, F2 + 1, 32, 26, F2 + 1, 36, Blocks.CARPET.yellow().defaultBlockState());
		fill(level, bb, 23, F2 + 1, 35, 25, F2 + 1, 36, Blocks.CARPET.orange().defaultBlockState());
		set(level, bb, Blocks.GLOWSTONE.defaultBlockState(), 24, F2, 36);
		for (int[] post : new int[][]{{21, 31}, {27, 31}, {21, 37}, {27, 37}}) {
			set(level, bb, DARK_LOG, post[0], F2 + 1, post[1]);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), post[0], F2 + 2, post[1]);
		}

		// NW Tea-House Checkpoint Pavilion.
		JapaneseArchitecture.teaCheckpointPavilion(p, 10, F2, 20, 16, 26, BAMBOO_BLOCK, CHERRY_PLANKS,
			Blocks.CARPET.yellow().defaultBlockState(), Direction.NORTH);
		chest(level, bb, 11, F2 + 1, 21, DungeonWorldgen.CAT_SANCTUARY_HALL, Direction.EAST, 22);

		JapaneseArchitecture.upturnedEaveSkirt(p, 7, 17, 41, 55, F3, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, CHERRY_PLANKS, true);
		balconyRailing(level, bb, 9, 19, 39, 53, F3 + 1);
	}

	private void floor3(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		room(level, bb, 10, F3, 20, 38, F4, 52, DARK_OAK);
		pagodaWalls(p, 10, F3 + 1, 20, 38, F4 - 1, 52, CHERRY_PLANKS, VERMILION, false);
		fill(level, bb, 11, F3, 21, 37, F3, 51, CHERRY_PLANKS);
		// Bordered ceremonial pink & magenta blossom carpet runner toward Freya's central dais.
		fill(level, bb, 22, F3 + 1, 22, 26, F3 + 1, 38, Blocks.CARPET.magenta().defaultBlockState());
		fill(level, bb, 23, F3 + 1, 22, 25, F3 + 1, 38, Blocks.CARPET.pink().defaultBlockState());

		// Vaulted vermilion ceiling beams & hanging lanterns.
		interiorColonnade(level, bb, 12, 36, new int[]{25, 33, 41, 48}, F3 + 1, F4 - 1, VERMILION, DARK_LOG);
		ceilingCrossbeams(level, bb, 11, 37, new int[]{25, 33, 41, 48}, F4 - 1, VERMILION, 24);

		// Four quadrant Suzu Bell Pavilions with cherry fence corners, hanging bells, pink banners & azalea pots.
		for (int[] bp : new int[][]{{15, 27}, {33, 27}, {15, 37}, {33, 37}}) {
			int x = bp[0], z = bp[1];
			fill(level, bb, x - 1, F3, z - 1, x + 1, F3, z + 1, Blocks.POLISHED_ANDESITE.defaultBlockState());
			for (int[] c : new int[][]{{x - 1, z - 1}, {x + 1, z - 1}, {x - 1, z + 1}, {x + 1, z + 1}}) {
				fill(level, bb, c[0], F3 + 1, c[1], c[0], F3 + 4, c[1], CHERRY_LOG);
			}
			fill(level, bb, x - 1, F3 + 5, z - 1, x + 1, F3 + 5, z + 1, CHERRY_PLANKS);
			set(level, bb, Blocks.IRON_CHAIN.defaultBlockState(), x, F3 + 4, z);
			set(level, bb, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, x < 24 ? Direction.EAST : Direction.WEST),
				x, F3 + 3, z);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), x, F3 + 6, z);
			banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.PINK), x < 24 ? x + 2 : x - 2, F3 + 4, z - 1,
				x < 24 ? Direction.EAST : Direction.WEST);
			set(level, bb, Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), x, F3 + 1, z - 2);
		}

		// Freya's elevated Silver-Bell Shrine Arch right before the grand stairwell to Floor 4.
		for (int px : new int[]{22, 26}) {
			fill(level, bb, px, F3 + 1, 38, px, F3 + 4, 38, VERMILION);
		}
		fill(level, bb, 21, F3 + 4, 38, 27, F3 + 4, 38, VERMILION);
		set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), 24, F3 + 4, 38);
		set(level, bb, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.NORTH), 24, F3 + 3, 38);
		set(level, bb, Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), 21, F3 + 1, 37);
		set(level, bb, Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), 27, F3 + 1, 37);

		// SE Tea-House Checkpoint Pavilion.
		JapaneseArchitecture.teaCheckpointPavilion(p, 30, F3, 44, 36, 50, CHERRY_LOG, CHERRY_PLANKS,
			Blocks.CARPET.pink().defaultBlockState(), Direction.SOUTH);
		chest(level, bb, 35, F3 + 1, 49, DungeonWorldgen.CAT_SANCTUARY_HALL, Direction.WEST, 33);

		JapaneseArchitecture.upturnedEaveSkirt(p, 9, 19, 39, 53, F4, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, VERMILION, true);
		balconyRailing(level, bb, 11, 21, 37, 51, F4 + 1);
	}

	private void floor4(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		room(level, bb, 12, F4, 22, 36, F4 + 9, 50, DEEPSLATE_TILES);
		pagodaWalls(p, 12, F4 + 1, 22, 36, F4 + 8, 50, DEEPSLATE_TILES, VERMILION, false);
		fill(level, bb, 13, F4, 23, 35, F4, 49, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		// Coffered gilded-blackstone & gold border walkways across Floor 4 floor.
		fill(level, bb, 19, F4, 31, 29, F4, 41, Blocks.GILDED_BLACKSTONE.defaultBlockState());
		fill(level, bb, 20, F4, 32, 28, F4, 40, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		set(level, bb, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 24, F4, 36);
		for (int gx = 16; gx <= 32; gx += 4) {
			for (int gz = 26; gz <= 46; gz += 4) {
				if (Math.abs(gx - 24) > 3 || Math.abs(gz - 36) > 3) {
					set(level, bb, Blocks.GILDED_BLACKSTONE.defaultBlockState(), gx, F4, gz);
				}
			}
		}

		// Four monumental Obsidian & Gilded-Blackstone Brazier Towers with soul campfires, chains & red banners.
		for (int[] t : new int[][]{{15, 25}, {33, 25}, {15, 47}, {33, 47}}) {
			int tx = t[0], tz = t[1];
			fill(level, bb, tx - 1, F4 + 1, tz - 1, tx + 1, F4 + 1, tz + 1, Blocks.GILDED_BLACKSTONE.defaultBlockState());
			fill(level, bb, tx, F4 + 2, tz, tx, F4 + 4, tz, Blocks.OBSIDIAN.defaultBlockState());
			set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), tx, F4 + 5, tz);
			set(level, bb, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), tx, F4 + 6, tz);
			set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true),
				tx < 24 ? tx + 1 : tx - 1, F4 + 4, tz);
			banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.RED), tx, F4 + 4, tz < 36 ? tz + 1 : tz - 1,
				tz < 36 ? Direction.NORTH : Direction.SOUTH);
		}

		// Kuro's central Torii Altar Arch directly behind the crying-obsidian throne focal point + North Vault Alcove.
		for (int px : new int[]{21, 27}) {
			fill(level, bb, px, F4 + 1, 39, px, F4 + 5, 39, VERMILION);
			set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), px, F4 + 1, 39);
		}
		fill(level, bb, 20, F4 + 5, 39, 28, F4 + 5, 39, VERMILION);
		set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), 24, F4 + 5, 39);
		fill(level, bb, 21, F4 + 1, 48, 27, F4 + 5, 49, Blocks.GOLD_BLOCK.defaultBlockState());
		fill(level, bb, 22, F4 + 1, 48, 26, F4 + 4, 48, AIR);
		chest(level, bb, 24, F4 + 1, 48, DungeonWorldgen.CAT_SANCTUARY_VAULT, Direction.NORTH, 44);

		// Exposed diagonal dark-oak roof trusses & glowing amethyst/sea-lantern/tinted-glass starlit skylight.
		for (int i = 0; i < 4; i++) {
			fill(level, bb, 13 + i, F4 + 6 + i / 2, 23, 35 - i, F4 + 6 + i / 2, 23 + i, DARK_LOG);
			fill(level, bb, 13 + i, F4 + 6 + i / 2, 49 - i, 35 - i, F4 + 6 + i / 2, 49, DARK_LOG);
		}
		fill(level, bb, 21, F4 + 9, 33, 27, F4 + 9, 39, Blocks.AMETHYST_BLOCK.defaultBlockState());
		fill(level, bb, 22, F4 + 9, 34, 26, F4 + 9, 38, Blocks.TINTED_GLASS.defaultBlockState());
		for (int[] sl : new int[][]{{21, 33}, {27, 33}, {21, 39}, {27, 39}}) {
			set(level, bb, Blocks.SEA_LANTERN.defaultBlockState(), sl[0], F4 + 9, sl[1]);
			set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), sl[0], F4 + 8, sl[1]);
		}

		// Sweeping hipped crown pagoda roof + ornate golden Sorin spire finial.
		JapaneseArchitecture.upturnedEaveSkirt(p, 11, 21, 37, 51, F4 + 10, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, VERMILION, true);
		hipRoof(level, bb, 11, 21, 37, 51, F4 + 10, Blocks.DEEPSLATE_TILE_STAIRS, DEEPSLATE_TILES, Blocks.GOLD_BLOCK.defaultBlockState());
		set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), 24, F4 + 24, 36);
		set(level, bb, VERMILION, 24, F4 + 25, 36);
		set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), 24, F4 + 26, 36);
		set(level, bb, VERMILION, 24, F4 + 27, 36);
		set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), 24, F4 + 28, 36);
		set(level, bb, Blocks.SEA_LANTERN.defaultBlockState(), 24, F4 + 29, 36);
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
				set(level, bb, stair(Blocks.DARK_OAK_STAIRS, x < 24 ? Direction.EAST : Direction.WEST, true),
					x + (x < 24 ? 1 : -1), y1, z);
				set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true),
					x + (x < 24 ? 1 : -1), y1 - 1, z);
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
		stairFlight(level, bb, 10, 12, F1, 45, Direction.NORTH);
		stairFlight(level, bb, 34, 36, F2, 24, Direction.NORTH);
		stairFlight(level, bb, 23, 25, F3, 40, Direction.NORTH);
	}

	private void stairFlight(WorldGenLevel level, BoundingBox bb, int x0, int x1, int baseY, int startZ, Direction dir) {
		for (int step = 0; step < 7; step++) {
			int z = startZ + step;
			int stairY = baseY + 1 + step;
			fill(level, bb, x0, baseY + 1, z, x1, stairY - 1, z, CHERRY_PLANKS);
			fill(level, bb, x0, stairY, z, x1, stairY, z, stair(Blocks.CHERRY_STAIRS, dir, false));
			fill(level, bb, x0, stairY + 1, z, x1, Math.min(HEIGHT - 2, baseY + 11), z, AIR);
		}
	}

	private void inhabitants(WorldGenLevel level, BoundingBox bb) {
		BlockPos heart = getWorldPos(24, F4 + 1, 36).immutable();
		spawnCat(level, bb, 24, F1 + 1, 34, ShrineCatRules.Variant.YUKI, heart);
		spawnCat(level, bb, 24, F2 + 1, 34, ShrineCatRules.Variant.ODIE, heart);
		spawnCat(level, bb, 24, F3 + 1, 36, ShrineCatRules.Variant.FREYA, heart);
		spawnCat(level, bb, 24, F4 + 1, 36, ShrineCatRules.Variant.KURO, heart);

		BlockPos keeperPos = getWorldPos(24, F1 + 1, 11);
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
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 10, F1 + 1, 25);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 38, F1 + 1, 25);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 17, F2 + 1, 30);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 31, F2 + 1, 30);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 19, F3 + 1, 32);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 29, F3 + 1, 32);
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
