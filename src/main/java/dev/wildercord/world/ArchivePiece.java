package dev.wildercord.world;

import dev.wildercord.cast.Runebound;
import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.content.WildercordBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The whole Archive as one piece, laid out in its own coordinates (x across, y up, z inward from
 * the entrance) and turned to face a random way. Every block that depends on the terrain (the
 * pillars around the entrance) is worked out per column, so each chunk builds its part the same
 * way whatever order chunks generate in.
 *
 * <pre>
 *   z 0-12   the entrance: broken pillars around the top of the stairway
 *   z 6-29   the stairway down (24 steps)
 *   z 30-48  the Hall of Shelves (Runebound guards, a chest)         door: Frost and Storm
 *   z 48-60  the Hall of Braziers (campfires a fire spell lights)     door: Fire and Wind
 *   z 60-64  a corridor
 *   z 64-90  the Arena, a domed circle where the Archivist waits       side door: Arcane and Life
 *   x 33-39  the Vault (two chests of Tier IV runes)
 * </pre>
 */
public class ArchivePiece extends ScatteredFeaturePiece implements dev.wildercord.world.dungeons.WardedPiece {
	public static final int WIDTH = 41;
	public static final int HEIGHT = 34;
	public static final int DEPTH = 92;
	/** Local y of the ground at the entrance (where you stand). */
	public static final int SURFACE = 26;
	private static final int STAIR_START = 6;
	private static final int ARENA_Z = 77;
	private static final int ARENA_R = 12;

	private static final BlockState TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
	private static final BlockState BRICKS = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
	private static final BlockState CRACKED = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
	private static final BlockState POLISHED = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
	private static final BlockState CHISELED = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	public ArchivePiece(int x, int y, int z, Direction facing) {
		super(WildercordWorldgen.ARCHIVE_PIECE, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public ArchivePiece(CompoundTag tag) {
		super(WildercordWorldgen.ARCHIVE_PIECE, tag);
	}

	/** The world position of the top of the stairway (before the piece is lowered into place). */
	public BlockPos entrance() {
		return getWorldPos(20, 0, STAIR_START);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		super.addAdditionalSaveData(context, tag);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos reference) {
		dev.wildercord.world.dungeons.DungeonWards.remember(level, this);
		stairway(level, bb);
		shrine(level, bb);
		library(level, bb);
		braziers(level, bb);
		corridor(level, bb);
		// The doors go in after the rooms: each room's box writes its end walls, which would wall them up.
		sealDoor(level, bb, 48, RuneSealBlock.Element.FROST, RuneSealBlock.Element.STORM);
		sealDoor(level, bb, 60, RuneSealBlock.Element.FIRE, RuneSealBlock.Element.WIND);
		arena(level, bb);
		vault(level, bb);
		guards(level, bb);
	}

	// ------------------------------------------------------------------ helpers

	private void fill(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		generateBox(level, bb, x0, y0, z0, x1, y1, z1, state, state, false);
	}

	private void room(WorldGenLevel level, BoundingBox bb, int x0, int y0, int z0, int x1, int y1, int z1, BlockState wall) {
		generateBox(level, bb, x0, y0, z0, x1, y1, z1, wall, AIR, false);
	}

	private void set(WorldGenLevel level, BoundingBox bb, BlockState state, int x, int y, int z) {
		placeBlock(level, state, x, y, z, bb);
	}

	/** A deterministic 0-99 for a spot, so the same column always gets the same variation. */
	private static int noise(int x, int z) {
		int h = x * 73856093 ^ z * 19349663;
		h ^= h >>> 13;
		h *= 0x5bd1e995;
		h ^= h >>> 15;
		return Math.floorMod(h, 100);
	}

	private BlockState wallBrick(int x, int y, int z) {
		return noise(x * 3 + y, z) < 18 ? CRACKED : BRICKS;
	}

	// ------------------------------------------------------------------ the entrance and the stairway

	private void stairway(WorldGenLevel level, BoundingBox bb) {
		// In a piece's own frame NORTH points down the stairway (+z), so steps climbing back up face SOUTH.
		BlockState stair = Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
		for (int z = STAIR_START; z <= STAIR_START + 23; z++) {
			int step = SURFACE - 1 - (z - STAIR_START);
			// Walls, the step, headroom and (once underground) a roof.
			for (int y = step - 1; y <= step + 5; y++) {
				set(level, bb, wallBrick(18, y, z), 18, y, z);
				set(level, bb, wallBrick(22, y, z), 22, y, z);
			}
			fill(level, bb, 19, step - 1, z, 21, step - 1, z, BRICKS);
			fill(level, bb, 19, step, z, 21, step, z, stair);
			fill(level, bb, 19, step + 1, z, 21, step + 4, z, AIR);
			if (z >= STAIR_START + 4) {
				fill(level, bb, 18, step + 5, z, 22, step + 5, z, TILES);
				if ((z - STAIR_START) % 5 == 2) {
					set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 20, step + 4, z);
				}
			} else {
				// The mouth of the stairway is open to the sky.
				fill(level, bb, 19, step + 5, z, 21, step + 7, z, AIR);
			}
		}
	}

	/** Broken pillars in a ring around the stairway's mouth, each standing on its own patch of ground. */
	private void shrine(WorldGenLevel level, BoundingBox bb) {
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8 + Math.PI / 8;
			int px = 20 + (int) Math.round(Math.cos(a) * 6.5);
			int pz = STAIR_START + 1 + (int) Math.round(Math.sin(a) * 6.5);
			if (px >= 18 && px <= 22 && pz >= STAIR_START) {
				continue;
			}
			BlockPos world = getWorldPos(px, 0, pz);
			if (!bb.isInside(world.getX(), bb.minY(), world.getZ())) {
				continue;
			}
			int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, world.getX(), world.getZ());
			int localGround = ground - boundingBox.minY();
			int height = 2 + (i * 7 + 3) % 4;
			for (int y = localGround - 2; y < localGround + height; y++) {
				set(level, bb, y == localGround + height - 1 ? CHISELED : wallBrick(px, y, pz), px, y, pz);
			}
			if (i % 3 == 0) {
				set(level, bb, Blocks.AMETHYST_CLUSTER.defaultBlockState(), px, localGround + height, pz);
			}
		}
		// The arch over the way down.
		for (int y = SURFACE; y <= SURFACE + 4; y++) {
			set(level, bb, POLISHED, 17, y, STAIR_START);
			set(level, bb, POLISHED, 23, y, STAIR_START);
		}
		fill(level, bb, 17, SURFACE + 5, STAIR_START, 23, SURFACE + 5, STAIR_START, CHISELED);
		set(level, bb, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 20, SURFACE + 5, STAIR_START);
		set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 20, SURFACE + 4, STAIR_START);
	}

	// ------------------------------------------------------------------ the Hall of Shelves

	private void library(WorldGenLevel level, BoundingBox bb) {
		room(level, bb, 8, 1, 30, 32, 12, 48, TILES);
		fill(level, bb, 9, 2, 31, 31, 2, 47, Blocks.DARK_OAK_PLANKS.defaultBlockState());
		fill(level, bb, 9, 2, 31, 31, 2, 31, POLISHED);
		fill(level, bb, 9, 2, 47, 31, 2, 47, POLISHED);
		fill(level, bb, 19, 2, 31, 21, 2, 47, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
		fill(level, bb, 19, 3, 32, 21, 3, 46, Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.PURPLE).defaultBlockState());
		fill(level, bb, 19, 3, 30, 21, 6, 30, AIR);
		for (int z = 32; z <= 46; z++) {
			boolean pillar = z % 4 == 2;
			for (int y = 3; y <= 11; y++) {
				BlockState left;
				BlockState right;
				if (pillar) {
					left = right = POLISHED;
				} else if (y <= 7) {
					left = z % 3 == 0 ? Blocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(net.minecraft.world.level.block.ChiseledBookShelfBlock.FACING, Direction.EAST)
						: Blocks.BOOKSHELF.defaultBlockState();
					right = z % 3 == 1 ? Blocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(net.minecraft.world.level.block.ChiseledBookShelfBlock.FACING, Direction.WEST)
						: Blocks.BOOKSHELF.defaultBlockState();
				} else {
					left = right = TILES;
				}
				set(level, bb, left, 9, y, z);
				set(level, bb, right, 31, y, z);
			}
			if (pillar) {
				set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState(), 10, 3, z);
				set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState(), 30, 3, z);
			}
		}
		// Reading tables with candles.
		for (int[] t : new int[][] {{13, 35}, {13, 41}, {27, 35}, {27, 41}}) {
			fill(level, bb, t[0], 3, t[1], t[0] + 1, 3, t[1] + 1, Blocks.DARK_OAK_PLANKS.defaultBlockState());
			set(level, bb, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true), t[0], 4, t[1]);
			set(level, bb, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true), t[0] + 1, 4, t[1] + 1);
		}
		set(level, bb, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.NORTH), 20, 3, 44);
		// Chandeliers.
		for (int[] c : new int[][] {{14, 35}, {26, 35}, {14, 43}, {26, 43}, {20, 39}}) {
			set(level, bb, Blocks.IRON_CHAIN.defaultBlockState(), c[0], 11, c[1]);
			set(level, bb, Blocks.IRON_CHAIN.defaultBlockState(), c[0], 10, c[1]);
			set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), c[0], 9, c[1]);
		}
		createChest(level, bb, RandomSource.create(boundingBox.minX() * 31L + boundingBox.minZ()), 29, 3, 46, WildercordWorldgen.LIBRARY_LOOT);
	}

	/** A 5-by-4 door of Rune Seals in a wall, checkered with two elements, in a carved frame. */
	private void sealDoor(WorldGenLevel level, BoundingBox bb, int z, RuneSealBlock.Element a, RuneSealBlock.Element b) {
		for (int x = 18; x <= 22; x++) {
			for (int y = 3; y <= 6; y++) {
				RuneSealBlock.Element element = (x + y) % 2 == 0 ? a : b;
				set(level, bb, WildercordBlocks.RUNE_SEAL.defaultBlockState().setValue(RuneSealBlock.ELEMENT, element), x, y, z);
			}
		}
		for (int y = 3; y <= 7; y++) {
			set(level, bb, POLISHED, 17, y, z);
			set(level, bb, POLISHED, 23, y, z);
		}
		fill(level, bb, 17, 7, z, 23, 7, z, CHISELED);
		set(level, bb, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 20, 8, z);
	}

	// ------------------------------------------------------------------ the Hall of Braziers

	private void braziers(WorldGenLevel level, BoundingBox bb) {
		room(level, bb, 12, 1, 48, 28, 10, 60, TILES);
		fill(level, bb, 13, 2, 49, 27, 2, 59, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		fill(level, bb, 18, 2, 49, 22, 2, 59, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
		for (int[] p : new int[][] {{14, 50}, {26, 50}, {14, 58}, {26, 58}}) {
			set(level, bb, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), p[0], 3, p[1]);
			set(level, bb, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false), p[0], 4, p[1]);
			set(level, bb, Blocks.IRON_CHAIN.defaultBlockState(), p[0], 10, p[1]);
			set(level, bb, Blocks.IRON_CHAIN.defaultBlockState(), p[0], 9, p[1]);
		}
		for (int z = 50; z <= 58; z += 4) {
			set(level, bb, Blocks.AMETHYST_CLUSTER.defaultBlockState(), 13, 3, z);
			set(level, bb, Blocks.AMETHYST_CLUSTER.defaultBlockState(), 27, 3, z);
		}
		set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 20, 9, 54);
		set(level, bb, Blocks.IRON_CHAIN.defaultBlockState(), 20, 10, 54);
	}

	private void corridor(WorldGenLevel level, BoundingBox bb) {
		room(level, bb, 17, 1, 60, 23, 8, 64, BRICKS);
		fill(level, bb, 18, 2, 61, 22, 2, 64, POLISHED);
		fill(level, bb, 18, 3, 61, 22, 6, 64, AIR);
		set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 20, 7, 62);
	}

	// ------------------------------------------------------------------ the Arena

	private int ceiling(double d) {
		return 13 + (int) Math.floor(Math.sqrt(Math.max(0, ARENA_R * ARENA_R - d * d)) * 0.55);
	}

	/** The Archivist's arena, from under its floor to over its dome, and the vault off it (with the door between). */
	@Override
	public java.util.List<BoundingBox> wardedBoxes() {
		return java.util.List.of(
			worldBox(20 - ARENA_R - 3, 0, ARENA_Z - ARENA_R - 3, 20 + ARENA_R + 3, SURFACE - 2, Math.min(DEPTH - 1, ARENA_Z + ARENA_R + 3)),
			worldBox(30, 0, 71, WIDTH - 1, 11, 83));
	}

	/** A box given in the piece's own coordinates, turned and placed as the piece is: in the world's. */
	private BoundingBox worldBox(int x0, int y0, int z0, int x1, int y1, int z1) {
		return BoundingBox.fromCorners(new net.minecraft.core.Vec3i(getWorldX(x0, z0), getWorldY(y0), getWorldZ(x0, z0)),
			new net.minecraft.core.Vec3i(getWorldX(x1, z1), getWorldY(y1), getWorldZ(x1, z1)));
	}

	private void arena(WorldGenLevel level, BoundingBox bb) {
		BlockState gilded = Blocks.GILDED_BLACKSTONE.defaultBlockState();
		for (int x = 20 - ARENA_R - 2; x <= 20 + ARENA_R + 2; x++) {
			for (int z = ARENA_Z - ARENA_R - 2; z <= ARENA_Z + ARENA_R + 2; z++) {
				double d = Math.sqrt((x - 20) * (x - 20) + (z - ARENA_Z) * (z - ARENA_Z));
				if (d > ARENA_R + 1.5) {
					continue;
				}
				set(level, bb, TILES, x, 1, z);
				if (d > ARENA_R) {
					// The wall.
					for (int y = 2; y <= ceiling(ARENA_R) + 1; y++) {
						set(level, bb, y % 5 == 0 ? CHISELED : wallBrick(x, y, z), x, y, z);
					}
					continue;
				}
				set(level, bb, floor(x, z, d, gilded), x, 2, z);
				int top = ceiling(d);
				for (int y = 3; y <= top; y++) {
					set(level, bb, AIR, x, y, z);
				}
				set(level, bb, noise(x, z) < 6 ? Blocks.SHROOMLIGHT.defaultBlockState() : TILES, x, top + 1, z);
				set(level, bb, TILES, x, top + 2, z);
			}
		}
		// Eight pillars, each with a light at its heart.
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8 + Math.PI / 8;
			int px = 20 + (int) Math.round(Math.cos(a) * 10);
			int pz = ARENA_Z + (int) Math.round(Math.sin(a) * 10);
			for (int y = 3; y <= ceiling(10); y++) {
				set(level, bb, y == 8 ? Blocks.SHROOMLIGHT.defaultBlockState() : POLISHED, px, y, pz);
			}
			set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState(), px + (px > 20 ? -1 : 1), 3, pz);
		}
		// The way in, and the Archivist's lectern at the centre.
		fill(level, bb, 18, 3, 64, 22, 6, 66, AIR);
		set(level, bb, WildercordBlocks.ARCHIVE_LECTERN.defaultBlockState(), 20, 3, ARENA_Z);
	}

	/** The arena floor: a magic circle in stone, with a ring, a hexagram and a gilded heart. */
	private BlockState floor(int x, int z, double d, BlockState gilded) {
		if (d < 1.6) {
			return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
		}
		if (Math.abs(d - 9) < 0.55) {
			return Blocks.AMETHYST_BLOCK.defaultBlockState();
		}
		if (Math.abs(d - 5) < 0.5) {
			return gilded;
		}
		if (d < 9.2 && onHexagram(x - 20, z - ARENA_Z)) {
			return Blocks.CRYING_OBSIDIAN.defaultBlockState();
		}
		return noise(x, z) < 10 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : POLISHED;
	}

	private static boolean onHexagram(double x, double z) {
		for (int t = 0; t < 2; t++) {
			for (int k = 0; k < 3; k++) {
				double a1 = Math.PI / 2 + t * Math.PI + k * Math.PI * 2 / 3;
				double a2 = a1 + Math.PI * 2 / 3;
				double x1 = Math.cos(a1) * 9, z1 = Math.sin(a1) * 9;
				double x2 = Math.cos(a2) * 9, z2 = Math.sin(a2) * 9;
				double dx = x2 - x1, dz = z2 - z1;
				double s = Math.max(0, Math.min(1, ((x - x1) * dx + (z - z1) * dz) / (dx * dx + dz * dz)));
				double px = x1 + s * dx - x, pz = z1 + s * dz - z;
				if (px * px + pz * pz < 0.36) {
					return true;
				}
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ the Vault

	private void vault(WorldGenLevel level, BoundingBox bb) {
		room(level, bb, 32, 1, 72, 40, 9, 82, TILES);
		fill(level, bb, 33, 2, 73, 39, 2, 81, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		fill(level, bb, 35, 2, 75, 37, 2, 79, Blocks.GILDED_BLACKSTONE.defaultBlockState());
		// The door out of the arena: Arcane and Life.
		fill(level, bb, 32, 3, 75, 33, 6, 79, AIR);
		for (int z = 75; z <= 79; z++) {
			for (int y = 3; y <= 6; y++) {
				RuneSealBlock.Element element = (z + y) % 2 == 0 ? RuneSealBlock.Element.ARCANE : RuneSealBlock.Element.LIFE;
				set(level, bb, WildercordBlocks.RUNE_SEAL.defaultBlockState().setValue(RuneSealBlock.ELEMENT, element), 33, y, z);
			}
		}
		RandomSource random = RandomSource.create(boundingBox.minX() * 17L + boundingBox.minZ() * 5L);
		createChest(level, bb, random, 38, 3, 74, WildercordWorldgen.VAULT_LOOT, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
		createChest(level, bb, random, 38, 3, 80, WildercordWorldgen.VAULT_LOOT, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
		set(level, bb, WildercordBlocks.WELLSTONE.defaultBlockState(), 38, 3, 77);
		for (int z = 74; z <= 80; z += 3) {
			set(level, bb, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 36, 8, z);
		}
	}

	private boolean createChest(WorldGenLevel level, BoundingBox bb, RandomSource random, int x, int y, int z,
			net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot, BlockState state) {
		// Turned with the piece, as every other block is (a chest given a state isn't, otherwise).
		return createChest(level, bb, random, getWorldPos(x, y, z), loot, state.mirror(getMirror()).rotate(getRotation()));
	}

	// ------------------------------------------------------------------ guards

	private void guards(WorldGenLevel level, BoundingBox bb) {
		guard(level, bb, EntityTypes.SKELETON, 13, 3, 38, false);
		guard(level, bb, EntityTypes.WITCH, 27, 3, 44, false);
		guard(level, bb, EntityTypes.PILLAGER, 20, 3, 36, false);
		guard(level, bb, EntityTypes.VINDICATOR, 20, 3, 54, true);
		guard(level, bb, EntityTypes.SKELETON, 15, 3, 56, false);
	}

	/** A Runebound guard, placed once: by the chunk its feet are in. */
	private void guard(WorldGenLevel level, BoundingBox bb, EntityType<? extends Mob> type, int x, int y, int z, boolean adept) {
		BlockPos pos = getWorldPos(x, y, z);
		if (!bb.isInside(pos)) {
			return;
		}
		Mob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (mob == null) {
			return;
		}
		mob.setPersistenceRequired();
		mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		Runebound.bindAtGeneration(mob, adept);
		level.addFreshEntityWithPassengers(mob);
	}
}
