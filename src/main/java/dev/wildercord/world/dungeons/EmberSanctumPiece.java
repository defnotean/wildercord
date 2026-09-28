package dev.wildercord.world.dungeons;

import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.content.dungeons.DungeonAltarBlock;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * The Ember Sanctum, a forge-temple in the Nether's caves, laid out in its own coordinates (x across,
 * y up, z inward from the entrance). The floor is at y 1 throughout; you stand at y 2.
 *
 * <pre>
 *   z 0-9    the approach: a causeway between two lava trenches, lit braziers on the parapets
 *   z 10     the gate: a Rune Seal door of Fire and Earth
 *   z 10-36  the Hall of Chains: a lava channel down the middle, bridges, Runebound keepers, a chest
 *   z 36     a second door, of Void and Storm
 *   z 36-46  the forge: blast furnaces, anvils, cauldrons of lava
 *   z 47-77  the Arena, a domed circle with a forge-circle in its floor, and the Cinder Warden's altar
 *   x 34-41  the Vault, off the arena behind a door of Frost and Fire (freeze, then burn: Shatter)
 * </pre>
 */
public class EmberSanctumPiece extends DungeonPiece {
	public static final int WIDTH = 42;
	public static final int HEIGHT = 24;
	public static final int DEPTH = 80;
	private static final int CX = 20;
	private static final int ARENA_Z = 62;
	private static final int ARENA_R = 13;

	private static final BlockState BRICKS = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
	private static final BlockState CRACKED = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
	private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.defaultBlockState();
	private static final BlockState POLISHED = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
	private static final BlockState CHISELED = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
	private static final BlockState GILDED = Blocks.GILDED_BLACKSTONE.defaultBlockState();
	private static final BlockState MAGMA = Blocks.MAGMA_BLOCK.defaultBlockState();
	private static final BlockState BASALT = Blocks.POLISHED_BASALT.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
	private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();
	private static final BlockState CHAIN = Blocks.IRON_CHAIN.defaultBlockState();
	private static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
	private static final BlockState FIRE = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);

	public EmberSanctumPiece(int x, int y, int z, Direction facing) {
		super(DungeonWorldgen.EMBER_SANCTUM_PIECE, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public EmberSanctumPiece(CompoundTag tag) {
		super(DungeonWorldgen.EMBER_SANCTUM_PIECE, tag);
	}

	/**
	 * Finds its footing: the first cave floor (solid, with room to stand over it) going down the
	 * column at the entrance, between y 100 and 32 (above the lava sea). With none, it's carved into
	 * the rock at y 48.
	 */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext context) {
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
		ChunkPos chunk = context.chunkPos();
		EmberSanctumPiece piece = new EmberSanctumPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), facing);
		BlockPos entrance = piece.getWorldPos(CX, 0, 4);
		NoiseColumn column = context.chunkGenerator().getBaseColumn(entrance.getX(), entrance.getZ(), context.heightAccessor(), context.randomState());
		int floor = 48;
		for (int y = 100; y >= 32; y--) {
			BlockState ground = column.getBlock(y);
			if (!ground.isAir() && ground.getFluidState().isEmpty() && column.getBlock(y + 1).isAir() && column.getBlock(y + 2).isAir()) {
				floor = y;
				break;
			}
		}
		piece.move(0, floor - 1, 0);
		int standX = entrance.getX();
		int standZ = entrance.getZ();
		return Optional.of(new Structure.GenerationStub(new BlockPos(standX, floor + 1, standZ), builder -> builder.addPiece(piece)));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos reference) {
		approach(level, bb);
		hall(level, bb);
		forge(level, bb);
		arena(level, bb);
		vault(level, bb);
		// The doors go in after the rooms: each room's box writes its end walls, which would wall them up.
		gate(level, bb, 10, RuneSealBlock.Element.FIRE, RuneSealBlock.Element.EARTH);
		gate(level, bb, 36, RuneSealBlock.Element.VOID, RuneSealBlock.Element.STORM);
		guards(level, bb);
	}

	private BlockState brick(int x, int y, int z) {
		return noise(x, y, z) < 20 ? CRACKED : BRICKS;
	}

	// ------------------------------------------------------------------ the approach

	private void approach(WorldGenLevel level, BoundingBox bb) {
		fill(level, bb, 12, 0, 0, 28, 1, 9, BLACKSTONE);
		fill(level, bb, 12, 2, 0, 28, 10, 9, AIR);
		fill(level, bb, 17, 1, 0, 23, 1, 9, BRICKS);
		fill(level, bb, 19, 1, 0, 21, 1, 9, POLISHED);
		// Lava trenches either side of the causeway, walled in at both ends.
		fill(level, bb, 15, 1, 1, 16, 1, 8, LAVA);
		fill(level, bb, 24, 1, 1, 25, 1, 8, LAVA);
		for (int z = 0; z <= 9; z++) {
			for (int x : new int[] {13, 14, 26, 27}) {
				set(level, bb, brick(x, 1, z), x, 1, z);
			}
			set(level, bb, BRICKS, 14, 2, z);
			set(level, bb, BRICKS, 26, 2, z);
			// Braziers on the parapets.
			if (z % 3 == 2) {
				set(level, bb, POLISHED, 14, 3, z);
				set(level, bb, FIRE, 14, 4, z);
				set(level, bb, POLISHED, 26, 3, z);
				set(level, bb, FIRE, 26, 4, z);
			}
		}
		// Two great braziers either side of the gate.
		for (int x : new int[] {16, 24}) {
			set(level, bb, GILDED, x, 2, 9);
			set(level, bb, POLISHED, x, 3, 9);
			set(level, bb, FIRE, x, 4, 9);
		}
	}

	/** A seal door in the wall at {@code z}, in a carved frame with a gilded keystone and magma eyes. */
	private void gate(WorldGenLevel level, BoundingBox bb, int z, RuneSealBlock.Element a, RuneSealBlock.Element b) {
		sealDoorAcross(level, bb, z, 18, 22, 2, 5, a, b);
		for (int y = 2; y <= 6; y++) {
			set(level, bb, POLISHED, 17, y, z);
			set(level, bb, POLISHED, 23, y, z);
		}
		fill(level, bb, 17, 6, z, 23, 6, z, CHISELED);
		set(level, bb, GILDED, 20, 7, z);
		set(level, bb, MAGMA, 18, 7, z);
		set(level, bb, MAGMA, 22, 7, z);
	}

	// ------------------------------------------------------------------ the Hall of Chains

	private void hall(WorldGenLevel level, BoundingBox bb) {
		fill(level, bb, 6, 0, 10, 34, 0, 36, BLACKSTONE);
		room(level, bb, 6, 1, 10, 34, 13, 36, BRICKS);
		for (int z = 11; z <= 35; z++) {
			for (int x = 7; x <= 33; x++) {
				int n = noise(x, z);
				set(level, bb, n < 7 && Math.abs(x - CX) > 3 ? MAGMA : n < 30 ? BRICKS : POLISHED, x, 1, z);
			}
			for (int y = 2; y <= 12; y++) {
				set(level, bb, brick(6, y, z), 6, y, z);
				set(level, bb, brick(34, y, z), 34, y, z);
			}
			if (z % 6 == 0) {
				set(level, bb, GILDED, 6, 8, z);
				set(level, bb, GILDED, 34, 8, z);
			}
		}
		// The lava channel down the middle, with three bridges.
		fill(level, bb, 19, 1, 13, 21, 1, 33, LAVA);
		for (int z : new int[] {17, 23, 29}) {
			fill(level, bb, 19, 1, z, 21, 1, z, BRICKS);
			set(level, bb, CHAIN, 18, 2, z);
			set(level, bb, CHAIN, 22, 2, z);
		}
		for (int z : new int[] {14, 20, 26, 32}) {
			fill(level, bb, 10, 2, z, 10, 12, z, BASALT);
			fill(level, bb, 30, 2, z, 30, 12, z, BASALT);
		}
		for (int z : new int[] {17, 23, 29}) {
			set(level, bb, POLISHED, 8, 2, z);
			set(level, bb, FIRE, 8, 3, z);
			set(level, bb, POLISHED, 32, 2, z);
			set(level, bb, FIRE, 32, 3, z);
		}
		for (int[] c : new int[][] {{15, 15}, {25, 15}, {15, 21}, {25, 21}, {15, 27}, {25, 27}, {15, 33}, {25, 33}}) {
			fill(level, bb, c[0], 10, c[1], c[0], 12, c[1], CHAIN);
			set(level, bb, LANTERN, c[0], 9, c[1]);
		}
		chest(level, bb, 32, 2, 34, DungeonWorldgen.EMBER_HALL, Direction.WEST, 1);
	}

	// ------------------------------------------------------------------ the forge

	private void forge(WorldGenLevel level, BoundingBox bb) {
		fill(level, bb, 13, 0, 36, 27, 0, 46, BLACKSTONE);
		room(level, bb, 13, 1, 36, 27, 9, 46, BRICKS);
		fill(level, bb, 14, 1, 37, 26, 1, 45, POLISHED);
		fill(level, bb, 18, 1, 37, 22, 1, 45, BRICKS);
		for (int z = 38; z <= 44; z += 2) {
			set(level, bb, Blocks.BLAST_FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.EAST), 14, 2, z);
		}
		set(level, bb, Blocks.LAVA_CAULDRON.defaultBlockState(), 26, 2, 39);
		set(level, bb, Blocks.LAVA_CAULDRON.defaultBlockState(), 26, 2, 43);
		set(level, bb, Blocks.SMITHING_TABLE.defaultBlockState(), 26, 2, 41);
		set(level, bb, Blocks.ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.NORTH), 24, 2, 41);
		set(level, bb, Blocks.CHIPPED_ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.EAST), 16, 2, 44);
		for (int z : new int[] {39, 43}) {
			fill(level, bb, 20, 7, z, 20, 8, z, CHAIN);
			set(level, bb, LANTERN, 20, 6, z);
		}
		// The way on into the arena.
		fill(level, bb, 18, 2, 46, 22, 6, 50, AIR);
	}

	// ------------------------------------------------------------------ the Arena

	private int ceiling(double d) {
		return 13 + (int) Math.floor(Math.sqrt(Math.max(0, ARENA_R * ARENA_R - d * d)) * 0.5);
	}

	private void arena(WorldGenLevel level, BoundingBox bb) {
		for (int x = CX - ARENA_R - 2; x <= CX + ARENA_R + 2; x++) {
			for (int z = ARENA_Z - ARENA_R - 2; z <= ARENA_Z + ARENA_R + 2; z++) {
				double d = dist(x, z, CX, ARENA_Z);
				if (d > ARENA_R + 1.5) {
					continue;
				}
				set(level, bb, BLACKSTONE, x, 0, z);
				if (d > ARENA_R) {
					for (int y = 1; y <= ceiling(ARENA_R) + 1; y++) {
						set(level, bb, y % 6 == 0 ? GILDED : brick(x, y, z), x, y, z);
					}
					continue;
				}
				set(level, bb, floor(x, z, d), x, 1, z);
				int top = ceiling(d);
				for (int y = 2; y <= top; y++) {
					set(level, bb, AIR, x, y, z);
				}
				int n = noise(x, z);
				set(level, bb, n < 5 ? Blocks.SHROOMLIGHT.defaultBlockState() : n < 14 ? MAGMA : BLACKSTONE, x, top + 1, z);
				set(level, bb, BLACKSTONE, x, top + 2, z);
			}
		}
		// Four pools of lava between the pillars, walled by the floor around them.
		for (int k = 0; k < 4; k++) {
			double a = Math.PI / 4 + k * Math.PI / 2;
			int px = CX + (int) Math.round(Math.cos(a) * 10);
			int pz = ARENA_Z + (int) Math.round(Math.sin(a) * 10);
			// Two by two, reaching in toward the centre from the spot.
			int qx = px + (px > CX ? -1 : 1);
			int qz = pz + (pz > ARENA_Z ? -1 : 1);
			fill(level, bb, Math.min(px, qx), 1, Math.min(pz, qz), Math.max(px, qx), 1, Math.max(pz, qz), LAVA);
		}
		// Eight basalt pillars, each with a light at its heart and a lantern.
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8 + Math.PI / 8;
			int px = CX + (int) Math.round(Math.cos(a) * 11);
			int pz = ARENA_Z + (int) Math.round(Math.sin(a) * 11);
			for (int y = 2; y <= ceiling(11); y++) {
				set(level, bb, y == 8 ? Blocks.SHROOMLIGHT.defaultBlockState() : BASALT, px, y, pz);
			}
			set(level, bb, Blocks.LANTERN.defaultBlockState(), px + (px > CX ? -1 : 1), 2, pz);
		}
		// Chains hanging from the dome.
		for (int k = 0; k < 4; k++) {
			int px = CX + (k == 0 ? 6 : k == 2 ? -6 : 0);
			int pz = ARENA_Z + (k == 1 ? 6 : k == 3 ? -6 : 0);
			int top = ceiling(6);
			fill(level, bb, px, top - 3, pz, px, top, pz, CHAIN);
			set(level, bb, LANTERN, px, top - 4, pz);
		}
		// The way in, and the Warden's altar at the centre.
		fill(level, bb, 18, 1, 46, 22, 1, 50, BRICKS);
		fill(level, bb, 18, 2, 46, 22, 6, 50, AIR);
		altar(level, bb, DungeonAltarBlock.Kind.CINDER, CX, 2, ARENA_Z);
	}

	/** The arena floor: a forge-circle in stone, with a ring of magma, a gilded ring and eight spokes. */
	private BlockState floor(int x, int z, double d) {
		if (d < 2) {
			return CHISELED;
		}
		if (Math.abs(d - 5) < 0.5) {
			return MAGMA;
		}
		if (Math.abs(d - 9) < 0.55) {
			return GILDED;
		}
		if (d < 12 && onSpoke(x, z, CX, ARENA_Z, 8, 0.5)) {
			return BRICKS;
		}
		return noise(x, z) < 12 ? BLACKSTONE : POLISHED;
	}

	// ------------------------------------------------------------------ the Vault

	private void vault(WorldGenLevel level, BoundingBox bb) {
		fill(level, bb, 34, 0, 56, 41, 0, 68, BLACKSTONE);
		room(level, bb, 34, 1, 56, 41, 9, 68, BRICKS);
		fill(level, bb, 35, 1, 57, 40, 1, 67, POLISHED);
		fill(level, bb, 36, 1, 60, 39, 1, 64, GILDED);
		// The door out of the arena: Frost and Fire (freeze, then burn).
		fill(level, bb, 32, 1, 60, 34, 1, 64, BRICKS);
		fill(level, bb, 32, 2, 60, 34, 5, 64, AIR);
		sealDoorAlong(level, bb, 34, 60, 64, 2, 5, RuneSealBlock.Element.FROST, RuneSealBlock.Element.FIRE);
		chest(level, bb, 39, 2, 58, DungeonWorldgen.EMBER_VAULT, Direction.WEST, 2);
		chest(level, bb, 39, 2, 66, DungeonWorldgen.EMBER_VAULT, Direction.WEST, 3);
		set(level, bb, GILDED, 39, 2, 62);
		set(level, bb, FIRE, 39, 3, 62);
		for (int z : new int[] {59, 65}) {
			set(level, bb, CHAIN, 37, 8, z);
			set(level, bb, LANTERN, 37, 7, z);
		}
	}

	// ------------------------------------------------------------------ guards

	private void guards(WorldGenLevel level, BoundingBox bb) {
		guard(level, bb, EntityTypes.WITHER_SKELETON, 12, 2, 18, List.of(Runes.BOLT, Runes.FIRE), false);
		guard(level, bb, EntityTypes.WITHER_SKELETON, 28, 2, 24, List.of(Runes.ARC, Runes.FIRE), false);
		guard(level, bb, EntityTypes.SKELETON, 26, 2, 31, List.of(Runes.BOLT, Runes.FROST), false);
		guard(level, bb, EntityTypes.PIGLIN_BRUTE, 14, 2, 30, List.of(Runes.CONE, Runes.FLASHFIRE), true);
	}
}
