package dev.wildercord.world.dungeons;

import dev.wildercord.config.Config;
import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.monster.MonsterContent;
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
import net.minecraft.world.level.block.LecternBlock;
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
 * The Lantern Bathhouse: Japanese Onsen Ryokan dungeon modeled after exterior &amp; interior
 * reference sheets. Features a stone terrace with an outdoor steaming footbath &amp; bamboo
 * shishi-odoshi water spout, weeping sakura &amp; bamboo garden with ishidoro lanterns, karahafu
 * entrance porch with noren curtain banners, reception counter &amp; carved wooden entry screen,
 * sweeping irimoya roof &amp; steaming cupola, central cypress bridge &amp; twin washing walkways
 * flanking mineral pools, indoor vermilion Torii Portal sealing the Fire + Wind vault, and
 * a fortified 14-mob garrison.
 */
public final class LanternBathhousePiece extends DungeonPiece {
	public static final int WIDTH = 39, HEIGHT = 32, DEPTH = 48;
	public static final int F = 4;

	private static final BlockState SPRUCE = Blocks.SPRUCE_PLANKS.defaultBlockState();
	private static final BlockState DARK_OAK = Blocks.DARK_OAK_PLANKS.defaultBlockState();
	private static final BlockState CYPRESS = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
	private static final BlockState DEEPSLATE = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
	private static final BlockState POLISHED = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
	private static final BlockState MOSSY = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
	private static final BlockState SHOJI = Blocks.CALCITE.defaultBlockState();

	public LanternBathhousePiece(int x, int y, int z, Direction facing) {
		super(DungeonWorldgen.LANTERN_BATHHOUSE_PIECE, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public LanternBathhousePiece(CompoundTag tag) {
		super(DungeonWorldgen.LANTERN_BATHHOUSE_PIECE, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		var piece = new LanternBathhousePiece(c.chunkPos().getMinBlockX(), 0, c.chunkPos().getMinBlockZ(),
			Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		var gen = c.chunkGenerator();
		int sea = gen.getSeaLevel();
		int high = Integer.MIN_VALUE, low = Integer.MAX_VALUE;
		for (int[] p : new int[][]{{19, 22}, {6, 10}, {32, 10}, {6, 38}, {32, 38}}) {
			var wp = piece.getWorldPos(p[0], 0, p[1]);
			int surface = gen.getFirstOccupiedHeight(wp.getX(), wp.getZ(), Heightmap.Types.WORLD_SURFACE_WG, c.heightAccessor(), c.randomState());
			int floor = gen.getFirstOccupiedHeight(wp.getX(), wp.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, c.heightAccessor(), c.randomState());
			if (surface <= sea + 1 || surface - floor > 2) return Optional.empty();
			high = Math.max(high, floor);
			low = Math.min(low, floor);
		}
		if (high - low > 8) return Optional.empty();
		int ground = (high + low) / 2;
		piece.move(0, ground - 1 - F, 0);
		var entry = piece.getWorldPos(19, F + 1, 2);
		return Optional.of(new Structure.GenerationStub(new BlockPos(entry.getX(), ground, entry.getZ()), b -> b.addPiece(piece)));
	}

	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of(
			worldBox(4, F - 1, 8, 34, F + 8, 34),
			worldBox(12, F, 34, 26, F + 7, 42)
		);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);
		BlockPlacer p = (x, y, z, st) -> set(level, bb, st, x, y, z);

		groundsAndExteriorWalls(level, bb, p);
		karahafuPorch(level, bb);
		onsenInterior(level, bb);
		vaultAndToriiPortal(level, bb);
		roofAndCupola(level, bb, p);

		if (Config.get().monsters().spawnRate() > 0) {
			// 14 Dungeon Defenders (7 PaperLanternSpirits + 7 KarakuriSentinels) guarding approach, bridge & pools.
			sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 16, F + 1, 5);
			sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 22, F + 1, 5);
			sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 19, F + 2, 16);
			sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 19, F + 2, 24);
			sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 19, F + 2, 29);
			sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 6, F + 1, 18);
			sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 32, F + 1, 18);

			sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 12, F + 2, 6);
			sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 26, F + 2, 6);
			sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 10, F + 2, 15);
			sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 28, F + 2, 15);
			sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 10, F + 2, 25);
			sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 28, F + 2, 25);
			sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 19, F + 3, 21);
		}
	}

	private void groundsAndExteriorWalls(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		for (int x = 2; x <= 36; x++) {
			for (int z = 0; z <= 44; z++) {
				fill(level, bb, x, 0, z, x, F - 1, z, noise(x, z) < 20 ? MOSSY : DEEPSLATE);
				set(level, bb, Blocks.GRASS_BLOCK.defaultBlockState(), x, F, z);
				fill(level, bb, x, F + 1, z, x, HEIGHT - 1, z, AIR);
			}
		}
		// Stone walk from courtyard into bathhouse steps & symmetrical ishidoro stone lanterns.
		for (int z = 0; z <= 8; z++) {
			for (int x = 16; x <= 22; x++) {
				set(level, bb, noise(x, z) < 28 ? MOSSY : Blocks.POLISHED_ANDESITE.defaultBlockState(), x, F, z);
			}
		}
		for (int[] il : new int[][]{{14, 1}, {24, 1}, {14, 5}, {24, 5}, {4, 2}, {34, 2}}) {
			JapaneseArchitecture.ishidoro(p, il[0], F + 1, il[1]);
		}
		// Outdoor Steaming Hot-Spring Footbath Pool (west forecourt) & Bamboo Shishi-Odoshi Spout (east forecourt).
		for (int x = 8; x <= 12; x++) {
			for (int z = 2; z <= 5; z++) {
				boolean rim = x == 8 || x == 12 || z == 2 || z == 5;
				if (rim) {
					set(level, bb, MOSSY, x, F + 1, z);
				} else {
					set(level, bb, Blocks.MAGMA_BLOCK.defaultBlockState(), x, F - 1, z);
					set(level, bb, Blocks.WATER.defaultBlockState(), x, F, z);
				}
			}
		}
		set(level, bb, JapaneseArchitecture.slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM), 10, F + 1, 6);
		fill(level, bb, 26, F, 2, 29, F, 5, POLISHED);
		set(level, bb, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL), 28, F + 1, 4);
		set(level, bb, Blocks.BAMBOO_BLOCK.defaultBlockState(), 29, F + 1, 4);
		set(level, bb, Blocks.BAMBOO_BLOCK.defaultBlockState(), 29, F + 2, 4);

		JapaneseArchitecture.weepingCherryTree(p, 5, F + 1, 4, 4, false);
		JapaneseArchitecture.weepingCherryTree(p, 33, F + 1, 4, 4, false);
		for (int[] bm : new int[][]{{6, 6, 5}, {7, 5, 4}, {31, 6, 5}, {32, 5, 4}}) {
			for (int y = F + 1; y <= F + bm[2]; y++) {
				set(level, bb, Blocks.BAMBOO_BLOCK.defaultBlockState(), bm[0], y, bm[1]);
			}
		}

		// Main Warded Onsen Hall box + cypress/shoji/trapdoor lattice facade + perimeter veranda railing.
		room(level, bb, 4, F, 8, 34, F + 8, 34, DEEPSLATE);
		for (int y = F + 1; y <= F + 7; y++) {
			for (int x = 4; x <= 34; x++) {
				for (int z = 8; z <= 34; z++) {
					boolean onX = x == 4 || x == 34;
					boolean onZ = z == 8 || z == 34;
					if (!onX && !onZ) {
						continue;
					}
					boolean pillar = (onX && (z - 8) % 5 == 0) || (onZ && (x - 4) % 5 == 0) || (onX && onZ);
					if (pillar) {
						set(level, bb, CYPRESS, x, y, z);
					} else if (y == F + 1) {
						set(level, bb, DEEPSLATE, x, y, z);
					} else if (y == F + 6 || y == F + 7) {
						set(level, bb, DARK_OAK, x, y, z);
					} else {
						set(level, bb, SHOJI, x, y, z);
					}
				}
			}
		}
		fill(level, bb, 5, F, 9, 33, F, 33, SPRUCE);
		// Exterior wooden deck skirt & railing along the front & sides.
		for (int x = 3; x <= 35; x++) {
			if (x < 15 || x > 23) {
				set(level, bb, Blocks.SPRUCE_FENCE.defaultBlockState(), x, F + 1, 7);
			}
		}
		for (int z = 8; z <= 35; z++) {
			set(level, bb, Blocks.SPRUCE_FENCE.defaultBlockState(), 3, F + 1, z);
			set(level, bb, Blocks.SPRUCE_FENCE.defaultBlockState(), 35, F + 1, z);
		}
	}

	private void karahafuPorch(WorldGenLevel level, BoundingBox bb) {
		fill(level, bb, 17, F + 1, 8, 21, F + 4, 8, AIR);
		for (int x : new int[]{15, 23}) {
			fill(level, bb, x, F + 1, 6, x, F + 5, 6, CYPRESS);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), x, F + 2, 5);
		}
		// Dark-oak & gold karahafu entry gable + alternating blue & red noren cloth banners.
		fill(level, bb, 15, F + 5, 6, 23, F + 5, 8, DARK_OAK);
		fill(level, bb, 17, F + 6, 6, 21, F + 6, 8, Blocks.GOLD_BLOCK.defaultBlockState());
		for (int z = 6; z <= 8; z++) {
			set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, false), 14, F + 5, z);
			set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST, false), 24, F + 5, z);
			set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, false), 16, F + 6, z);
			set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST, false), 22, F + 6, z);
			set(level, bb, JapaneseArchitecture.slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM), 19, F + 7, z);
		}
		for (int x = 16; x <= 22; x++) {
			set(level, bb, DARK_OAK, x, F + 4, 7);
		}
		for (int x = 16; x <= 22; x += 2) {
			banner(level, bb, dev.wildercord.compat.McBlocksAndItems.dyedBlock("wall_banner", x == 18 || x == 20 ? DyeColor.BLUE : DyeColor.RED),
				x, F + 4, 6, Direction.SOUTH);
		}
	}

	private void onsenInterior(WorldGenLevel level, BoundingBox bb) {
		// Genkan Reception Desk (west lobby), Tsuitate Privacy Screen (center) & Cedar Robe Shelves (east lobby).
		fill(level, bb, 9, F + 1, 10, 12, F + 1, 10, DARK_OAK);
		set(level, bb, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.SOUTH), 10, F + 2, 10);
		set(level, bb, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.SOUTH), 12, F + 2, 10);
		fill(level, bb, 26, F + 1, 9, 29, F + 1, 9, Blocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
		set(level, bb, Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), 25, F + 1, 9);
		for (int x = 18; x <= 20; x++) {
			set(level, bb, Blocks.SPRUCE_FENCE.defaultBlockState(), x, F + 1, 10);
			set(level, bb, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.BOTTOM), x, F + 2, 10);
		}

		// Left & right steaming magma/sea-lantern heated mineral pools (dry side walkways at x = 6..7 and x = 31..32).
		for (int z = 12; z <= 30; z++) {
			for (int x = 8; x <= 14; x++) {
				set(level, bb, (x + z) % 3 == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState()
					: (x == 11 && z % 6 == 0 ? Blocks.SEA_LANTERN.defaultBlockState() : POLISHED), x, F - 1, z);
				set(level, bb, Blocks.WATER.defaultBlockState(), x, F, z);
			}
			for (int x = 24; x <= 30; x++) {
				set(level, bb, (x + z) % 3 == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState()
					: (x == 27 && z % 6 == 0 ? Blocks.SEA_LANTERN.defaultBlockState() : POLISHED), x, F - 1, z);
				set(level, bb, Blocks.WATER.defaultBlockState(), x, F, z);
			}
			set(level, bb, z % 4 == 0 ? MOSSY : POLISHED, 15, F, z);
			set(level, bb, z % 4 == 0 ? MOSSY : POLISHED, 23, F, z);
		}
		// Floating cedar onsen bath tubs in pools.
		for (int[] tub : new int[][]{{9, 15}, {12, 20}, {10, 27}, {26, 16}, {29, 21}, {27, 27}}) {
			set(level, bb, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP), tub[0], F, tub[1]);
		}

		// Central arched hinoki-cypress bridge deck with fence railings, banners & corner lantern posts.
		fill(level, bb, 16, F + 1, 12, 22, F + 1, 30, SPRUCE);
		for (int x = 16; x <= 22; x++) {
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false), x, F + 1, 11);
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false), x, F + 1, 31);
		}
		for (int z = 12; z <= 30; z++) {
			boolean post = z == 12 || z == 18 || z == 24 || z == 30;
			for (int bx : new int[]{16, 22}) {
				set(level, bb, post ? CYPRESS : Blocks.SPRUCE_FENCE.defaultBlockState(), bx, F + 2, z);
				if (post) {
					set(level, bb, Blocks.LANTERN.defaultBlockState(), bx, F + 3, z);
				}
			}
		}

		// Side-wall bath washing stations & mossy waterfall boulders (filled water cauldrons, stools, barrels & towel shelves).
		for (int wz : new int[]{14, 21, 28}) {
			for (int sideX : new int[]{5, 33}) {
				set(level, bb, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, LayeredCauldronBlock.MAX_FILL_LEVEL),
					sideX, F + 1, wz);
				set(level, bb, JapaneseArchitecture.slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM), sideX, F + 1, wz + 1);
				set(level, bb, Blocks.BARREL.defaultBlockState(), sideX, F + 1, wz - 1);
				set(level, bb, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP),
					sideX, F + 3, wz);
			}
		}
		for (int[] wr : new int[][]{{5, 17}, {33, 17}, {5, 24}, {33, 24}}) {
			set(level, bb, MOSSY, wr[0], F + 1, wr[1]);
			set(level, bb, Blocks.POTTED_FLOWERING_AZALEA.defaultBlockState(), wr[0], F + 2, wr[1]);
		}
		chest(level, bb, 6, F + 1, 10, DungeonWorldgen.LANTERN_BATHHOUSE_HALL, Direction.EAST, 101);

		// Towering cypress pillars with bracketed overhead ceiling rafters, banners & rows of hanging golden lanterns.
		for (int z : new int[]{14, 21, 28}) {
			for (int x : new int[]{15, 23}) {
				fill(level, bb, x, F + 1, z, x, F + 6, z, CYPRESS);
				set(level, bb, stair(Blocks.DARK_OAK_STAIRS, x < 19 ? Direction.EAST : Direction.WEST, true),
					x + (x < 19 ? 1 : -1), F + 6, z);
				banner(level, bb, dev.wildercord.compat.McBlocksAndItems.dyedBlock("wall_banner", z == 21 ? DyeColor.RED : DyeColor.BLUE),
					x + (x < 19 ? 1 : -1), F + 4, z, x < 19 ? Direction.EAST : Direction.WEST);
			}
			fill(level, bb, 5, F + 7, z, 33, F + 7, z, DARK_OAK);
			for (int lx : new int[]{11, 17, 21, 27}) {
				set(level, bb, Blocks.CHAIN.defaultBlockState(), lx, F + 6, z);
				set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), lx, F + 5, z);
			}
		}
	}

	private void vaultAndToriiPortal(WorldGenLevel level, BoundingBox bb) {
		BlockState red = Blocks.RED_TERRACOTTA.defaultBlockState();
		// Monumental indoor vermilion-and-gold Torii Shrine Portal framing the Fire + Wind RuneSeal doorway.
		for (int px : new int[]{15, 23}) {
			fill(level, bb, px, F + 1, 33, px, F + 6, 33, red);
			set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), px, F + 1, 33);
			set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), px, F + 1, 32);
			set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState(), px, F + 2, 32);
		}
		fill(level, bb, 14, F + 5, 33, 24, F + 5, 33, red);
		fill(level, bb, 14, F + 6, 33, 24, F + 6, 33, red);
		set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), 19, F + 5, 33);
		set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 15, F + 4, 32);
		set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 23, F + 4, 32);

		// Sealed Inner Treasure Vault behind Fire + Wind RuneSeal door at z = 34.
		room(level, bb, 12, F, 34, 26, F + 7, 42, DEEPSLATE);
		fill(level, bb, 13, F, 35, 25, F, 41, POLISHED);
		fill(level, bb, 16, F, 36, 22, F, 40, Blocks.GILDED_BLACKSTONE.defaultBlockState());
		set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), 14, F + 1, 40);
		set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState(), 14, F + 2, 40);
		set(level, bb, Blocks.GOLD_BLOCK.defaultBlockState(), 24, F + 1, 40);
		set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState(), 24, F + 2, 40);
		sealDoorAcross(level, bb, 34, 17, 21, F + 1, F + 4, RuneSealBlock.Element.FIRE, RuneSealBlock.Element.WIND);
		chest(level, bb, 17, F + 1, 40, DungeonWorldgen.LANTERN_BATHHOUSE_VAULT, Direction.NORTH, 202);
		chest(level, bb, 21, F + 1, 40, DungeonWorldgen.LANTERN_BATHHOUSE_VAULT, Direction.NORTH, 303);
	}

	private void roofAndCupola(WorldGenLevel level, BoundingBox bb, BlockPlacer p) {
		JapaneseArchitecture.upturnedEaveSkirt(p, 3, 7, 35, 35, F + 9, Blocks.DARK_OAK_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, DEEPSLATE, true);
		hipRoof(level, bb, 4, 8, 34, 34, F + 9, Blocks.DARK_OAK_STAIRS, DEEPSLATE, POLISHED);

		fill(level, bb, 17, F + 23, 19, 21, F + 23, 23, POLISHED);
		for (int[] cp : new int[][]{{17, 19}, {21, 19}, {17, 23}, {21, 23}}) {
			fill(level, bb, cp[0], F + 24, cp[1], cp[0], F + 25, cp[1], CYPRESS);
		}
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 18, F + 24, 19);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 20, F + 24, 19);
		set(level, bb, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 19, F + 24, 21);
		fill(level, bb, 17, F + 26, 19, 21, F + 26, 23, DARK_OAK);
		fill(level, bb, 18, F + 27, 20, 20, F + 27, 22, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
	}
}
