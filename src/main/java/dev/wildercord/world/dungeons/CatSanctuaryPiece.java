package dev.wildercord.world.dungeons;

import dev.wildercord.config.Config;
import dev.wildercord.monster.MonsterContent;
import dev.wildercord.wildlife.SakuraVillager;
import dev.wildercord.wildlife.ShrineCat;
import dev.wildercord.wildlife.ShrineCatContent;
import dev.wildercord.wildlife.ShrineCatRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * The Cat Sanctuary: a massive four-tier Japanese pagoda dungeon rising above an entry torii garden.
 * Each floor holds its own layout, trick/enemies, tea-house checkpoint buff, and one of the four
 * real guardian cats in order:
 * <ol>
 *   <li>Floor 1 (Moss &amp; Bamboo Courtyard): {@code Yuki} + hovering {@code PaperLanternSpirit}s</li>
 *   <li>Floor 2 (Sunlit Tatami &amp; Shoji Hall): {@code Odie} + clockwork {@code KarakuriSentinel}s</li>
 *   <li>Floor 3 (Suzu Bell Gallery): {@code Freya} + paired {@code KarakuriSentinel}s &amp; {@code PaperLanternSpirit}s</li>
 *   <li>Floor 4 (Starlit Roof Sanctum): {@code Kuro} — completing the sanctuary unlocks taming for all four cats</li>
 * </ol>
 */
public final class CatSanctuaryPiece extends DungeonPiece {
	public static final int WIDTH = 49, HEIGHT = 52, DEPTH = 64;
	public static final int F1 = 4, F2 = 11, F3 = 18, F4 = 25;

	private static final BlockState CHERRY_PLANKS = Blocks.CHERRY_PLANKS.defaultBlockState();
	private static final BlockState DARK_OAK = Blocks.DARK_OAK_PLANKS.defaultBlockState();
	private static final BlockState BAMBOO_PLANKS = Blocks.BAMBOO_PLANKS.defaultBlockState();
	private static final BlockState STONE_BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
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
		if (high - low > 6) return Optional.empty();
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
		courtyard(level, bb);
		floor1(level, bb);
		floor2(level, bb);
		floor3(level, bb);
		floor4(level, bb);
		stairwells(level, bb);
		inhabitants(level, bb);
	}

	private void courtyard(WorldGenLevel level, BoundingBox bb) {
		for (int x = 2; x <= 46; x++) {
			for (int z = 0; z <= 58; z++) {
				fill(level, bb, x, 0, z, x, F1 - 1, z, STONE_BRICKS);
				set(level, bb, Blocks.GRASS_BLOCK.defaultBlockState(), x, F1, z);
				fill(level, bb, x, F1 + 1, z, x, HEIGHT - 1, z, AIR);
			}
		}
		// Stone path from entry to pagoda steps.
		for (int z = 0; z <= 15; z++) {
			for (int x = 21; x <= 27; x++) {
				set(level, bb, noise(x, z) < 35 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState(), x, F1, z);
			}
		}
		// Vermilion Torii gate at z = 6.
		for (int x : new int[]{19, 29}) {
			fill(level, bb, x, F1 + 1, 6, x, F1 + 6, 6, VERMILION);
			set(level, bb, DEEPSLATE_TILES, x, F1, 6);
		}
		fill(level, bb, 18, F1 + 5, 6, 30, F1 + 5, 6, VERMILION);
		fill(level, bb, 17, F1 + 7, 6, 31, F1 + 7, 6, DEEPSLATE_TILES);
		// Entry lanterns & ornamental cherry trees.
		for (int x : new int[]{16, 32}) {
			set(level, bb, Blocks.POLISHED_ANDESITE.defaultBlockState(), x, F1 + 1, 10);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), x, F1 + 2, 10);
			for (int y = F1 + 1; y <= F1 + 4; y++) {
				set(level, bb, Blocks.CHERRY_LOG.defaultBlockState(), x < 24 ? 10 : 38, y, 8);
			}
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					if (dx * dx + dz * dz <= 5) {
						set(level, bb, Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true),
							(x < 24 ? 10 : 38) + dx, F1 + 5, 8 + dz);
					}
				}
			}
		}
	}

	private void floor1(WorldGenLevel level, BoundingBox bb) {
		Palette walls = Palette.of(PAPER_WALL, 6, CHERRY_PLANKS, 2, Blocks.STRIPPED_DARK_OAK_LOG, 2);
		room(level, bb, 6, F1, 16, 42, F2, 56, DARK_OAK);
		walls(level, bb, 6, F1 + 1, 16, 42, F2 - 1, 56, walls);
		fill(level, bb, 7, F1, 17, 41, F1, 55, CHERRY_PLANKS);
		// Main entrance doorway (7x4 open air so walking/delving starts cleanly).
		fill(level, bb, 21, F1 + 1, 16, 27, F1 + 4, 16, AIR);
		// Moss & bamboo koi channels inside Floor 1.
		for (int z = 22; z <= 42; z++) {
			for (int x : new int[]{16, 17, 31, 32}) {
				set(level, bb, STONE_BRICKS, x, F1 - 1, z);
				set(level, bb, Blocks.WATER.defaultBlockState(), x, F1, z);
			}
			set(level, bb, Blocks.MOSS_BLOCK.defaultBlockState(), 15, F1, z);
			set(level, bb, Blocks.MOSS_BLOCK.defaultBlockState(), 33, F1, z);
		}
		// Tea-House Checkpoint 1 (NE alcove).
		teaCheckpoint(level, bb, 34, F1 + 1, 48, 39, 53);
		chest(level, bb, 40, F1 + 1, 52, DungeonWorldgen.CAT_SANCTUARY_HALL, Direction.WEST, 11);
		eaveSkirt(level, bb, 5, 15, 43, 57, F2);
	}

	private void floor2(WorldGenLevel level, BoundingBox bb) {
		Palette shojiWalls = Palette.of(PAPER_WALL, 6, BAMBOO_PLANKS, 3, Blocks.WOOL.white(), 1);
		room(level, bb, 8, F2, 18, 40, F3, 54, DARK_OAK);
		walls(level, bb, 8, F2 + 1, 18, 40, F3 - 1, 54, shojiWalls);
		fill(level, bb, 9, F2, 19, 39, F2, 53, BAMBOO_PLANKS);
		// Sunlit carpet centerpiece where Odie loafs.
		fill(level, bb, 21, F2 + 1, 31, 27, F2 + 1, 37, Blocks.CARPET.yellow().defaultBlockState());
		// Decorative open shoji arches on sides (always >= 3 blocks wide).
		for (int x : new int[]{16, 32}) {
			fill(level, bb, x, F2 + 1, 24, x, F2 + 5, 26, BAMBOO_PLANKS);
			fill(level, bb, x, F2 + 1, 42, x, F2 + 5, 44, BAMBOO_PLANKS);
		}
		teaCheckpoint(level, bb, 10, F2 + 1, 20, 15, 25);
		chest(level, bb, 10, F2 + 1, 20, DungeonWorldgen.CAT_SANCTUARY_HALL, Direction.EAST, 22);
		eaveSkirt(level, bb, 7, 17, 41, 55, F3);
	}

	private void floor3(WorldGenLevel level, BoundingBox bb) {
		Palette bellWalls = Palette.of(CHERRY_PLANKS, 5, PAPER_WALL, 4, Blocks.STRIPPED_CHERRY_LOG, 1);
		room(level, bb, 10, F3, 20, 38, F4, 52, DARK_OAK);
		walls(level, bb, 10, F3 + 1, 20, 38, F4 - 1, 52, bellWalls);
		fill(level, bb, 11, F3, 21, 37, F3, 51, CHERRY_PLANKS);
		fill(level, bb, 22, F3 + 1, 24, 26, F3 + 1, 46, Blocks.CARPET.pink().defaultBlockState());
		// Four Suzu bell chimes hanging along the gallery.
		for (int[] b : new int[][]{{16, 28}, {32, 28}, {16, 40}, {32, 40}}) {
			fill(level, bb, b[0], F3 + 1, b[1], b[0], F3 + 4, b[1], Blocks.STRIPPED_CHERRY_LOG.defaultBlockState());
			set(level, bb, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, b[0] < 24 ? Direction.EAST : Direction.WEST),
				b[0] + (b[0] < 24 ? 1 : -1), F3 + 3, b[1]);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), b[0], F3 + 5, b[1]);
		}
		teaCheckpoint(level, bb, 31, F3 + 1, 44, 36, 49);
		chest(level, bb, 36, F3 + 1, 49, DungeonWorldgen.CAT_SANCTUARY_HALL, Direction.WEST, 33);
		eaveSkirt(level, bb, 9, 19, 39, 53, F4);
	}

	private void floor4(WorldGenLevel level, BoundingBox bb) {
		room(level, bb, 12, F4, 22, 36, F4 + 9, 50, DEEPSLATE_TILES);
		fill(level, bb, 13, F4, 23, 35, F4, 49, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		fill(level, bb, 20, F4, 32, 28, F4, 40, Blocks.GILDED_BLACKSTONE.defaultBlockState());
		set(level, bb, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 24, F4, 36);
		for (int[] l : new int[][]{{15, 25}, {33, 25}, {15, 47}, {33, 47}}) {
			set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), l[0], F4 + 8, l[1]);
		}
		// Skylight amethyst ring and pagoda hipped crown roof.
		fill(level, bb, 22, F4 + 9, 34, 26, F4 + 9, 38, Blocks.TINTED_GLASS.defaultBlockState());
		hipRoof(level, bb, 11, 21, 37, 51, F4 + 10, Blocks.DEEPSLATE_TILE_STAIRS, DEEPSLATE_TILES, Blocks.GOLD_BLOCK.defaultBlockState());
		chest(level, bb, 24, F4 + 1, 48, DungeonWorldgen.CAT_SANCTUARY_VAULT, Direction.NORTH, 44);
	}

	/** Connects all four pagoda floors cleanly with solid under-stair fill and open headroom so no closed cavity forms. */
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
			// Cut stairwell shaft straight through the floor above (+3 headroom).
			fill(level, bb, x0, stairY + 1, z, x1, Math.min(HEIGHT - 2, baseY + 11), z, AIR);
		}
	}

	private void teaCheckpoint(WorldGenLevel level, BoundingBox bb, int x0, int y, int z0, int x1, int z1) {
		fill(level, bb, x0, y, z0, x1, y, z1, Blocks.CARPET.red().defaultBlockState());
		int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
		set(level, bb, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), cx, y, cz);
		set(level, bb, Blocks.CAULDRON.defaultBlockState(), cx + 1, y, cz);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), x0, y, z0);
	}

	private void eaveSkirt(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1, int y) {
		for (int x = x0; x <= x1; x++) {
			set(level, bb, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH, false), x, y, z0);
			set(level, bb, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH, false), x, y, z1);
		}
		for (int z = z0 + 1; z < z1; z++) {
			set(level, bb, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST, false), x0, y, z);
			set(level, bb, stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST, false), x1, y, z);
		}
	}

	private void inhabitants(WorldGenLevel level, BoundingBox bb) {
		BlockPos heart = getWorldPos(24, F4 + 1, 36).immutable();
		// Floor 1 -> Yuki, Floor 2 -> Odie, Floor 3 -> Freya, Floor 4 -> Kuro.
		spawnCat(level, bb, 24, F1 + 1, 34, ShrineCatRules.Variant.YUKI, heart);
		spawnCat(level, bb, 24, F2 + 1, 34, ShrineCatRules.Variant.ODIE, heart);
		spawnCat(level, bb, 24, F3 + 1, 36, ShrineCatRules.Variant.FREYA, heart);
		spawnCat(level, bb, 24, F4 + 1, 36, ShrineCatRules.Variant.KURO, heart);

		// Welcoming Tea Master at entry courtyard.
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
		// Distinct floor sentries matching each tier's rhythm.
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 12, F1 + 1, 24);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 36, F1 + 1, 24);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 15, F2 + 1, 30);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 33, F2 + 1, 30);
		sentry(level, bb, MonsterContent.KARAKURI_SENTINEL, 16, F3 + 1, 35);
		sentry(level, bb, MonsterContent.PAPER_LANTERN_SPIRIT, 32, F3 + 1, 35);
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
