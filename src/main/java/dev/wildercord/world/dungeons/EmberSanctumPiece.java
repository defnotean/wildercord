package dev.wildercord.world.dungeons;

import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.content.dungeons.DungeonAltarBlock;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
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
		dev.wildercord.world.dungeons.DungeonWards.remember(level, this);
		cavern(level, bb);
		approach(level, bb);
		hall(level, bb);
		forge(level, bb);
		arena(level, bb);
		vault(level, bb);
		exterior(level, bb);
		// The doors go in after the rooms: each room's box writes its end walls, which would wall them up.
		gate(level, bb, 10, RuneSealBlock.Element.FIRE, RuneSealBlock.Element.EARTH);
		gate(level, bb, 36, RuneSealBlock.Element.VOID, RuneSealBlock.Element.STORM);
		guards(level, bb);
	}

	private BlockState brick(int x, int y, int z) {
		return noise(x, y, z) < 20 ? CRACKED : BRICKS;
	}

	// ------------------------------------------------------------------ the cavern

	/**
	 * A cavern hollowed round the whole temple, so its buttresses, roofs and dome stand free in firelight instead of
	 * vanishing into the rock: a vault of rough stone that slopes down to the walls, a floor of blackstone and basalt
	 * propped down to whatever lies below, shroomlight bosses in the ceiling and basalt columns along the edges.
	 * Built first; the temple is laid over it.
	 */
	private void cavern(WorldGenLevel level, BoundingBox bb) {
		for (int x = 1; x <= WIDTH - 2; x++) {
			for (int z = 0; z <= DEPTH - 3; z++) {
				int edge = Math.min(Math.min(x - 1, WIDTH - 2 - x), DEPTH - 3 - z);
				int n = noise(x, z);
				int top = HEIGHT - 2 - n % 3 - 2 * Math.max(0, 4 - edge);
				fill(level, bb, x, 2, z, x, top, z, AIR);
				fillColumnDown(level, BLACKSTONE, x, 0, z, bb);
				set(level, bb, n < 5 ? MAGMA : n < 40 ? Blocks.BASALT.defaultBlockState() : BLACKSTONE, x, 1, z);
				// Light hangs from the vault, and the rock round the edge stands in columns.
				if (n > 95 && edge > 3) {
					set(level, bb, Blocks.SHROOMLIGHT.defaultBlockState(), x, top + 1, z);
					set(level, bb, Blocks.SHROOMLIGHT.defaultBlockState(), x, top, z);
				} else if (n > 90 && edge <= 2) {
					fill(level, bb, x, 2, z, x, top, z, BASALT);
				} else if (n > 84 && edge <= 3) {
					fill(level, bb, x, top - 1 - n % 3, z, x, top, z, Blocks.BASALT.defaultBlockState());
				}
			}
		}
	}

	// ------------------------------------------------------------------ the approach

	private void approach(WorldGenLevel level, BoundingBox bb) {
		// A forecourt cut tall enough that the whole front shows, pylon crowns and gable included.
		fill(level, bb, 8, 0, 0, 32, 1, 9, BLACKSTONE);
		fill(level, bb, 8, 2, 0, 32, 20, 5, AIR);
		fill(level, bb, 12, 2, 0, 28, 10, 9, AIR);
		fill(level, bb, 16, 11, 6, 24, 20, 8, AIR);
		// Shroomlight lamps set into the cut walls, so the black front stands against light.
		for (int z = 1; z <= 5; z += 2) {
			set(level, bb, Blocks.SHROOMLIGHT.defaultBlockState(), 8, 6, z);
			set(level, bb, Blocks.SHROOMLIGHT.defaultBlockState(), 32, 6, z);
			set(level, bb, Blocks.SHROOMLIGHT.defaultBlockState(), 8, 13, z);
			set(level, bb, Blocks.SHROOMLIGHT.defaultBlockState(), 32, 13, z);
		}
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
		facade(level, bb);
	}

	/**
	 * The temple front: two battered pylons with fire on their crowns flank the gate, a corbelled lintel spans
	 * between them, and a stepped gable of black brick climbs over the door.
	 */
	private void facade(WorldGenLevel level, BoundingBox bb) {
		Palette face = Palette.of(Blocks.POLISHED_BLACKSTONE_BRICKS, 8, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, 3, Blocks.BLACKSTONE, 1);
		for (int x0 : new int[] {11, 25}) {
			int mid = x0 + 2;
			fill(level, bb, x0, 1, 7, x0 + 4, 16, 9, face);
			// A sloped plinth, a gilded string course at the hall's floor line, and another under the crown.
			for (int x = x0; x <= x0 + 4; x++) {
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH, false), x, 2, 6);
				set(level, bb, x == mid ? GILDED : CHISELED, x, 8, 7);
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH, true), x, 15, 6);
			}
			// Corner shafts of basalt and a magma eye glaring down the causeway.
			for (int x : new int[] {x0, x0 + 4}) fill(level, bb, x, 3, 6, x, 14, 6, BASALT);
			fill(level, bb, mid, 11, 7, mid, 12, 7, MAGMA);
			banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.RED), mid, 6, 6, Direction.SOUTH);
			// The crown: battlements round a fire.
			fill(level, bb, x0, 16, 6, x0 + 4, 16, 9, POLISHED);
			crenellate(level, bb, x0, 6, x0 + 4, 9, 17, BRICKS);
			set(level, bb, GILDED, mid, 16, 8);
			set(level, bb, FIRE, mid, 17, 8);
		}
		// A lintel on corbels between the pylons, lanterns hanging from it over the door.
		for (int x = 16; x <= 24; x++) {
			set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH, true), x, 9, 9);
			set(level, bb, x % 4 == 0 ? GILDED : CHISELED, x, 10, 9);
		}
		for (int x : new int[] {18, 22}) hangingLantern(level, bb, x, 8, 9, 1, false);
		// The stepped gable over the gate, its peak a gilded block between two magma eyes.
		for (int k = 0; k <= 4; k++) fill(level, bb, 15 + k, 11 + k, 9, 25 - k, 11 + k, 10, face);
		set(level, bb, GILDED, CX, 16, 9);
		set(level, bb, MAGMA, CX - 2, 13, 9);
		set(level, bb, MAGMA, CX + 2, 13, 9);
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
			// Chiseled bases and capitals, corbels either side of each capital.
			for (int x : new int[] {10, 30}) {
				set(level, bb, CHISELED, x, 2, z);
				set(level, bb, CHISELED, x, 10, z);
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH, true), x, 10, z - 1);
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.SOUTH, true), x, 10, z + 1);
			}
			// A pointed transverse arch across the nave from column to column, a lantern from its keystone.
			for (int x = 11; x <= 29; x++) {
				double t = Math.abs(x - CX) / 10.0;
				int foot = 10 + (int) Math.round(2 * (1 - t * t));
				fill(level, bb, x, foot, z, x, 12, z, BRICKS);
				if (foot < 12) {
					set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, x < CX ? Direction.WEST : Direction.EAST, true), x, foot, z);
				}
			}
			set(level, bb, GILDED, CX, 12, z);
			hangingLantern(level, bb, CX, 11, z, 1, false);
			// Lower arches over the aisles, from the wall to the column.
			for (int[] a : new int[][] {{7, 9}, {31, 33}}) {
				fill(level, bb, a[0], 11, z, a[1], 12, z, BRICKS);
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, a[0] == 7 ? Direction.WEST : Direction.EAST, true), a[0] == 7 ? 7 : 33, 10, z);
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, a[0] == 7 ? Direction.EAST : Direction.WEST, true), a[0] == 7 ? 9 : 31, 10, z);
			}
		}
		// Shroomlight bosses in the vault between the arches, and red banners down both walls.
		for (int z : new int[] {17, 23, 29}) {
			set(level, bb, Blocks.SHROOMLIGHT.defaultBlockState(), CX, 13, z);
			banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.RED), 7, 7, z, Direction.EAST);
			banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.RED), 33, 7, z, Direction.WEST);
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
		// A smoke hood over the furnaces on a course of corbels, magma glowing in its throat.
		fill(level, bb, 14, 6, 37, 15, 8, 45, BRICKS);
		for (int z = 37; z <= 45; z++) {
			set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true), 16, 6, z);
			if (z % 2 == 0) set(level, bb, MAGMA, 15, 6, z);
		}
		// Pilasters down the anvil wall and racked chains between them.
		for (int z : new int[] {37, 45}) fill(level, bb, 26, 2, z, 26, 8, z, BASALT);
		for (int z : new int[] {38, 44}) fill(level, bb, 26, 5, z, 26, 8, z, CHAIN);
		// The way on into the arena, under a gilded arch with corbels in its corners.
		fill(level, bb, 18, 2, 46, 22, 6, 50, AIR);
	}

	/** A carved arch over the five-wide passage at {@code z}: basalt jambs, corner corbels and a gilded lintel. */
	private void portal(WorldGenLevel level, BoundingBox bb, int z) {
		for (int x : new int[] {17, 23}) fill(level, bb, x, 2, z, x, 7, z, BASALT);
		set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true), 18, 6, z);
		set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, true), 22, 6, z);
		fill(level, bb, 17, 7, z, 23, 7, z, CHISELED);
		set(level, bb, GILDED, CX, 7, z);
		set(level, bb, MAGMA, 18, 8, z);
		set(level, bb, MAGMA, 22, 8, z);
	}

	// ------------------------------------------------------------------ the Arena

	/** The arena, from its outer wall to its dome, and the vault off it (with the door between). */
	@Override
	public java.util.List<BoundingBox> wardedBoxes() {
		return java.util.List.of(
			worldBox(CX - ARENA_R - 3, 0, ARENA_Z - ARENA_R - 3, CX + ARENA_R + 3, HEIGHT - 1, Math.min(DEPTH - 1, ARENA_Z + ARENA_R + 3)),
			worldBox(31, 0, 55, WIDTH - 1, 11, 69));
	}

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
					// Battlements round the drum's rim.
					if (d > ARENA_R + 0.5 && (x + z) % 2 == 0) set(level, bb, BRICKS, x, ceiling(ARENA_R) + 2, z);
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
				// Eight ribs spring from the pillars and meet in a gilded ring round the crown.
				if (d > 2.5 && d < ARENA_R && rib(x, z)) {
					set(level, bb, d > 11.5 ? GILDED : BRICKS, x, top, z);
					// The same ribs stand proud on the outside of the dome.
					set(level, bb, POLISHED, x, top + 3, z);
				} else if (Math.abs(d - 3) < 0.5) {
					set(level, bb, GILDED, x, top, z);
				}
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
				set(level, bb, y == 8 ? Blocks.SHROOMLIGHT.defaultBlockState() : y == 2 || y == ceiling(11) ? CHISELED : BASALT, px, y, pz);
			}
			// A gilded collar under the capital.
			set(level, bb, GILDED, px, ceiling(11) - 1, pz);
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
		// A crown of lanterns over the altar, hung from the top of the dome.
		fill(level, bb, CX, 15, ARENA_Z, CX, ceiling(0), ARENA_Z, CHAIN);
		set(level, bb, Blocks.SHROOMLIGHT.defaultBlockState(), CX, 14, ARENA_Z);
		for (Direction d : Direction.Plane.HORIZONTAL) {
			hangingLantern(level, bb, CX + d.getStepX() * 3, ceiling(3), ARENA_Z + dz(d) * 3, 5, false);
		}
		// The way in, and the Warden's altar at the centre.
		fill(level, bb, 18, 1, 46, 22, 1, 50, BRICKS);
		fill(level, bb, 18, 2, 46, 22, 6, 50, AIR);
		portal(level, bb, 46);
		portal(level, bb, 50);
		altar(level, bb, DungeonAltarBlock.Kind.CINDER, CX, 2, ARENA_Z);
	}

	/** Whether a ceiling column lies on one of the eight ribs, which run out to the pillars (turned an eighth of a turn off the spokes). */
	private static boolean rib(int x, int z) {
		double a = Math.atan2(z - ARENA_Z, x - CX) - Math.PI / 8;
		double off = Math.abs(Math.IEEEremainder(a, Math.PI / 4));
		return dist(x, z, CX, ARENA_Z) * Math.sin(off) < 0.6;
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
		// Basalt pilasters in the corners, a gilded cornice, and the treasury's banner over its brazier.
		for (int[] c : new int[][] {{35, 57}, {40, 57}, {35, 67}, {40, 67}}) {
			pillar(level, bb, c[0], c[1], 2, 8, CHISELED, BASALT, GILDED);
		}
		for (int z = 58; z <= 66; z++) set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true), 40, 8, z);
		banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.RED), 40, 5, 62, Direction.WEST);
	}

	// ------------------------------------------------------------------ the outside

	/**
	 * What shows where the cave opens onto the temple: buttresses down the hall and forge, red lancets between them on
	 * a basalt plinth, a corbelled gilded band, a battlemented roof with a raised
	 * clerestory and burning corner turrets, smoking chimneys over the forge and a gilded finial on the arena dome.
	 */
	private void exterior(WorldGenLevel level, BoundingBox bb) {
		for (int z : new int[] {13, 19, 25, 31}) {
			buttress(level, bb, 5, z, 1, 12, Direction.WEST, BRICKS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
			buttress(level, bb, 35, z, 1, 12, Direction.EAST, BRICKS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
		}
		fill(level, bb, 6, 14, 10, 34, 14, 36, POLISHED);
		crenellate(level, bb, 6, 10, 34, 36, 15, BRICKS);
		fill(level, bb, 16, 15, 11, 24, 16, 35, BRICKS);
		fill(level, bb, 16, 17, 11, 24, 17, 35, POLISHED);
		crenellate(level, bb, 16, 11, 24, 35, 18, BRICKS);
		for (int z = 13; z <= 33; z += 4) {
			set(level, bb, MAGMA, 16, 16, z);
			set(level, bb, MAGMA, 24, 16, z);
		}
		// A gilded ridge down the clerestory roof, skylights glowing along it, and lamps on the aisle roofs.
		fill(level, bb, CX, 17, 11, CX, 17, 35, GILDED);
		for (int z = 13; z <= 33; z += 4) set(level, bb, Blocks.SHROOMLIGHT.defaultBlockState(), CX, 17, z);
		for (int z = 13; z <= 33; z += 6) {
			set(level, bb, Blocks.SHROOMLIGHT.defaultBlockState(), 11, 14, z);
			set(level, bb, Blocks.SHROOMLIGHT.defaultBlockState(), 29, 14, z);
		}
		// A gilded band round the hall's flanks under the battlements, on a corbel table, over a basalt plinth.
		for (int z = 10; z <= 36; z++) {
			set(level, bb, GILDED, 5, 13, z);
			set(level, bb, GILDED, 35, 13, z);
			fill(level, bb, 5, 1, z, 5, 2, z, BASALT);
			fill(level, bb, 35, 1, z, 35, 2, z, BASALT);
			if (z % 2 == 0) {
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.EAST, true), 4, 13, z);
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.WEST, true), 36, 13, z);
			}
		}
		// Between the buttresses, tall red lancets lit from inside, framed in chiseled jambs under gilded hoods.
		for (int z : new int[] {16, 22, 28}) {
			for (int[] w : new int[][] {{6, 5}, {34, 35}}) {
				fill(level, bb, w[0], 5, z, w[0], 9, z, Blocks.STAINED_GLASS.red().defaultBlockState());
				fill(level, bb, w[1], 4, z - 1, w[1], 10, z - 1, CHISELED);
				fill(level, bb, w[1], 4, z + 1, w[1], 10, z + 1, CHISELED);
				set(level, bb, MAGMA, w[1], 3, z);
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, w[1] < CX ? Direction.EAST : Direction.WEST, false), w[1], 4, z);
				set(level, bb, GILDED, w[1], 11, z);
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.NORTH, false), w[1], 11, z - 1);
				set(level, bb, stair(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Direction.SOUTH, false), w[1], 11, z + 1);
			}
		}
		// Buttresses down the forge's flanks too.
		for (int z : new int[] {38, 44}) {
			buttress(level, bb, 12, z, 1, 9, Direction.WEST, BRICKS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
			buttress(level, bb, 28, z, 1, 9, Direction.EAST, BRICKS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
		}
		for (int[] c : new int[][] {{6, 10}, {34, 10}, {6, 36}, {34, 36}}) {
			pillar(level, bb, c[0], c[1], 14, 19, CHISELED, BASALT, GILDED);
			set(level, bb, FIRE, c[0], 20, c[1]);
		}
		// Two chimneys over the forge's smoke hood; their campfires send smoke up through the cave.
		for (int z : new int[] {39, 43}) {
			fill(level, bb, 14, 10, z, 15, 17, z + 1, BRICKS);
			set(level, bb, CHISELED, 14, 13, z);
			set(level, bb, CHISELED, 15, 13, z + 1);
			fill(level, bb, 14, 18, z, 15, 18, z + 1, FIRE);
		}
		fill(level, bb, 13, 10, 36, 27, 10, 46, POLISHED);
		crenellate(level, bb, 13, 36, 27, 46, 11, BRICKS);
		// The dome's crown: a gilded drum and a magma finial.
		int crown = ceiling(0) + 3;
		set(level, bb, GILDED, CX, crown, ARENA_Z);
		set(level, bb, MAGMA, CX, crown + 1, ARENA_Z);
		// Basalt pilasters round the drum in line with the ribs, each with a magma eye and fire on its capital
		// (the two that would stand in the vault are left out).
		int rim = ceiling(ARENA_R) + 2;
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8 + Math.PI / 8;
			int px = CX + (int) Math.round(Math.cos(a) * (ARENA_R + 2));
			int pz = ARENA_Z + (int) Math.round(Math.sin(a) * (ARENA_R + 2));
			if (px >= 33 && pz >= 55 && pz <= 69) continue;
			pillar(level, bb, px, pz, 1, rim + 1, CHISELED, BASALT, GILDED);
			set(level, bb, MAGMA, px, 8, pz);
			set(level, bb, FIRE, px, rim + 2, pz);
		}
		vaultFaces(level, bb);
	}

	/** The treasury's outer walls: basalt pilasters, red lancets under gilded hoods, a gilded band and a burning parapet. */
	private void vaultFaces(WorldGenLevel level, BoundingBox bb) {
		for (int z : new int[] {56, 60, 64, 68}) pillar(level, bb, 41, z, 1, 9, CHISELED, BASALT, GILDED);
		for (int x : new int[] {37, 38}) {
			pillar(level, bb, x, 56, 1, 9, CHISELED, BASALT, GILDED);
			pillar(level, bb, x, 68, 1, 9, CHISELED, BASALT, GILDED);
		}
		for (int z : new int[] {58, 62, 66}) {
			fill(level, bb, 41, 3, z, 41, 6, z, Blocks.STAINED_GLASS.red().defaultBlockState());
			set(level, bb, MAGMA, 41, 2, z);
			set(level, bb, GILDED, 41, 7, z);
		}
		for (int z = 57; z <= 67; z++) if (z % 4 != 0 && z % 4 != 2) set(level, bb, CHISELED, 41, 8, z);
		fill(level, bb, 35, 10, 57, 40, 10, 67, POLISHED);
		crenellate(level, bb, 34, 56, 41, 68, 10, BRICKS);
		for (int[] c : new int[][] {{41, 56}, {41, 68}}) {
			set(level, bb, CHISELED, c[0], 11, c[1]);
			set(level, bb, FIRE, c[0], 12, c[1]);
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
