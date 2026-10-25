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
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The Astral Observatory, on an outer End island, laid out in its own coordinates (x across, y up, z
 * inward from the entrance). Its floor is at y 12 (you stand at 13); below that, foundations of end
 * stone reach down to the island wherever the ground falls away.
 *
 * <pre>
 *   z 0-9    the approach: a terrace of end stone and purpur, pillars crowned with end rods
 *   z 10     a Rune Seal door of Void and Arcane
 *   z 10-34  the Gallery of Orreries: star charts, hanging orreries, Runebound watchers, a chest
 *   z 34     a second door, of Time and Wind
 *   z 34-47  the windowed corridor, looking out on the void
 *   z 47-74  the Dome: glass over a star map in the floor, a telescope aimed through a slit, the altar
 *   x 33-40  the Vault, off the dome behind a door of Void and Life
 * </pre>
 */
public class AstralObservatoryPiece extends DungeonPiece {
	public static final int WIDTH = 42;
	public static final int HEIGHT = 32;
	public static final int DEPTH = 76;
	/** The floor's local y. */
	public static final int F = 12;
	private static final int CX = 20;
	private static final int DOME_Z = 60;
	private static final int DOME_R = 13;

	private static final BlockState PURPUR = Blocks.PURPUR_BLOCK.defaultBlockState();
	private static final BlockState PILLAR = Blocks.PURPUR_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
	private static final BlockState BRICKS = Blocks.END_STONE_BRICKS.defaultBlockState();
	private static final BlockState END_STONE = Blocks.END_STONE.defaultBlockState();
	private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();
	private static final BlockState CRYING = Blocks.CRYING_OBSIDIAN.defaultBlockState();
	private static final BlockState GLASS = Blocks.STAINED_GLASS.purple().defaultBlockState();
	private static final BlockState DARK_GLASS = Blocks.STAINED_GLASS.black().defaultBlockState();
	private static final BlockState STAR = Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
	private static final BlockState ROD_UP = Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP);
	private static final BlockState ROD_DOWN = Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.DOWN);
	private static final BlockState TUBE = Blocks.CUT_COPPER.waxed().oxidized().defaultBlockState();
	private static final Palette MASONRY = Palette.of(Blocks.END_STONE_BRICKS, 8, Blocks.END_STONE, 1, Blocks.PURPUR_BLOCK, 1);
	private static final Palette CLERESTORY = Palette.of(Blocks.PURPUR_BLOCK, 7, Blocks.END_STONE_BRICKS, 2);

	/** The star map: three constellations, star by star (offsets from the dome's centre). */
	private static final int[][][] CONSTELLATIONS = {
		{{-9, -3}, {-6, -6}, {-3, -4}, {-4, 0}, {-7, 2}},
		{{3, -9}, {6, -7}, {8, -4}, {5, -2}, {9, 0}},
		{{-2, 5}, {1, 8}, {4, 6}, {7, 8}, {6, 4}, {2, 3}}};
	private static final Set<Long> STARS = new HashSet<>();
	private static final Set<Long> LINES = new HashSet<>();

	static {
		for (int[][] constellation : CONSTELLATIONS) {
			for (int i = 0; i < constellation.length; i++) {
				STARS.add(key(constellation[i][0], constellation[i][1]));
				if (i > 0) {
					line(constellation[i - 1], constellation[i]);
				}
			}
		}
	}

	private static long key(int dx, int dz) {
		return ((long) dx << 32) ^ (dz & 0xFFFFFFFFL);
	}

	/** The cells between two stars, as a straight line of stone. */
	private static void line(int[] a, int[] b) {
		int steps = Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1]));
		for (int s = 0; s <= steps; s++) {
			int x = Math.round(a[0] + (b[0] - a[0]) * s / (float) steps);
			int z = Math.round(a[1] + (b[1] - a[1]) * s / (float) steps);
			LINES.add(key(x, z));
		}
	}

	public AstralObservatoryPiece(int x, int y, int z, Direction facing) {
		super(DungeonWorldgen.ASTRAL_OBSERVATORY_PIECE, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public AstralObservatoryPiece(CompoundTag tag) {
		super(DungeonWorldgen.ASTRAL_OBSERVATORY_PIECE, tag);
	}

	/** Finds its footing: the island's surface under the dome. Over the void, there's nothing to build on. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext context) {
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
		ChunkPos chunk = context.chunkPos();
		AstralObservatoryPiece piece = new AstralObservatoryPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), facing);
		BlockPos dome = piece.getWorldPos(CX, 0, DOME_Z);
		BlockPos entrance = piece.getWorldPos(CX, 0, 4);
		int ground = context.chunkGenerator().getFirstOccupiedHeight(dome.getX(), dome.getZ(), Heightmap.Types.WORLD_SURFACE_WG,
			context.heightAccessor(), context.randomState());
		if (ground <= context.heightAccessor().getMinY() + 8) {
			return Optional.empty();
		}
		piece.move(0, ground - 1 - F, 0);
		return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(), ground, entrance.getZ()), builder -> builder.addPiece(piece)));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos reference) {
		dev.wildercord.world.dungeons.DungeonWards.remember(level, this);
		approach(level, bb);
		gallery(level, bb);
		corridor(level, bb);
		dome(level, bb);
		vault(level, bb);
		gate(level, bb, 10, RuneSealBlock.Element.VOID, RuneSealBlock.Element.ARCANE);
		gate(level, bb, 34, RuneSealBlock.Element.TIME, RuneSealBlock.Element.WIND);
		guards(level, bb);
	}

	/** End stone under a stretch of floor, down to the island (at most as deep as the piece reaches). */
	private void foundation(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = F - 1; y >= 0; y--) {
					BlockState there = getBlock(level, x, y, z, bb);
					if (!there.isAir() && there.getFluidState().isEmpty()) {
						break;
					}
					set(level, bb, y == F - 1 ? BRICKS : END_STONE, x, y, z);
				}
			}
		}
	}

	// ------------------------------------------------------------------ the approach

	private void approach(WorldGenLevel level, BoundingBox bb) {
		foundation(level, bb, 14, 0, 26, 9);
		fill(level, bb, 14, F, 0, 26, F, 9, BRICKS);
		fill(level, bb, 19, F, 0, 21, F, 9, PURPUR);
		fill(level, bb, 14, F, 0, 14, F, 9, OBSIDIAN);
		fill(level, bb, 26, F, 0, 26, F, 9, OBSIDIAN);
		fill(level, bb, 14, F + 1, 0, 26, F + 8, 9, AIR);
		for (int z : new int[] {2, 5, 8}) {
			for (int x : new int[] {15, 25}) {
				fill(level, bb, x, F + 1, z, x, F + 3, z, PILLAR);
				set(level, bb, ROD_UP, x, F + 4, z);
			}
		}
		facade(level, bb);
	}

	/** The facade: two pylons of end stone flanking a tall corbelled portal under a stepped gable. */
	private void facade(WorldGenLevel level, BoundingBox bb) {
		for (int x0 : new int[] {8, 27}) {
			foundation(level, bb, x0, 6, x0 + 5, 9);
			fill(level, bb, x0, F, 6, x0 + 5, F, 6, BRICKS);
			fill(level, bb, x0, F, 7, x0 + 5, F + 14, 9, MASONRY);
			// A battered plinth, a string course and a cornice across the face, purpur shafts at the corners.
			for (int x = x0; x <= x0 + 5; x++) {
				set(level, bb, stair(Blocks.END_STONE_BRICK_STAIRS, Direction.NORTH, false), x, F + 1, 6);
				set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.NORTH, true), x, F + 8, 6);
				set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.NORTH, true), x, F + 14, 6);
			}
			for (int x : new int[] {x0, x0 + 5}) {
				fill(level, bb, x, F + 2, 6, x, F + 13, 6, PILLAR);
			}
			// A purpur panel low on the face and a weeping eye of crying obsidian high on it.
			fill(level, bb, x0 + 2, F + 3, 7, x0 + 3, F + 6, 7, PURPUR);
			fill(level, bb, x0 + 2, F + 10, 7, x0 + 3, F + 11, 7, CRYING);
			// Merlons on top, end rods on the corners and a purpur pinnacle in the middle.
			crenellate(level, bb, x0, 7, x0 + 5, 9, F + 15, PURPUR);
			for (int x : new int[] {x0, x0 + 5}) {
				set(level, bb, ROD_UP, x, F + 16, 7);
				set(level, bb, ROD_UP, x, F + 16, 9);
			}
			fill(level, bb, x0 + 2, F + 15, 8, x0 + 3, F + 16, 8, PILLAR);
			set(level, bb, ROD_UP, x0 + 2, F + 17, 8);
			set(level, bb, ROD_UP, x0 + 3, F + 17, 8);
		}
		// The portal: purpur jambs and a corbelled arch, the wall made up between it and the pylons.
		fill(level, bb, 14, F + 1, 9, 15, F + 10, 9, MASONRY);
		fill(level, bb, 25, F + 1, 9, 26, F + 10, 9, MASONRY);
		fill(level, bb, 16, F + 1, 9, 16, F + 8, 9, PILLAR);
		fill(level, bb, 24, F + 1, 9, 24, F + 8, 9, PILLAR);
		fill(level, bb, 16, F + 9, 9, 24, F + 10, 9, BRICKS);
		set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.WEST, true), 17, F + 8, 9);
		set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.EAST, true), 23, F + 8, 9);
		fill(level, bb, 18, F + 8, 9, 22, F + 8, 9, topSlab(Blocks.PURPUR_SLAB));
		set(level, bb, CRYING, CX, F + 9, 9);
		set(level, bb, ROD_DOWN, CX, F + 7, 9);
		// A stepped gable over the portal, edged in purpur stairs, an end rod at its peak.
		for (int k = 0; k <= 4; k++) {
			int y = F + 11 + k;
			fill(level, bb, 16 + k, y, 9, 24 - k, y, 9, MASONRY);
			set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.EAST, false), 15 + k, y, 9);
			set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.WEST, false), 25 - k, y, 9);
		}
		set(level, bb, CRYING, CX, F + 13, 9);
		set(level, bb, ROD_UP, CX, F + 16, 9);
	}

	/** A seal door in the wall at {@code z}, framed in purpur pillars and crowned with crying obsidian. */
	private void gate(WorldGenLevel level, BoundingBox bb, int z, RuneSealBlock.Element a, RuneSealBlock.Element b) {
		sealDoorAcross(level, bb, z, 18, 22, F + 1, F + 4, a, b);
		for (int y = F + 1; y <= F + 5; y++) {
			set(level, bb, PILLAR, 17, y, z);
			set(level, bb, PILLAR, 23, y, z);
		}
		fill(level, bb, 17, F + 5, z, 23, F + 5, z, BRICKS);
		set(level, bb, CRYING, 20, F + 6, z);
	}

	// ------------------------------------------------------------------ the Gallery of Orreries

	private void gallery(WorldGenLevel level, BoundingBox bb) {
		foundation(level, bb, 8, 10, 32, 34);
		room(level, bb, 8, F, 10, 32, F + 11, 34, PURPUR);
		for (int z = 11; z <= 33; z++) {
			for (int x = 9; x <= 31; x++) {
				boolean grid = (x - CX) % 4 == 0 || z % 4 == 0;
				BlockState floor = grid ? ((x - CX) % 4 == 0 && z % 4 == 0 && noise(x, z) < 50 ? CRYING : OBSIDIAN) : BRICKS;
				set(level, bb, floor, x, F, z);
			}
			if (z % 4 == 2) {
				fill(level, bb, 8, F + 1, z, 8, F + 10, z, PILLAR);
				fill(level, bb, 32, F + 1, z, 32, F + 10, z, PILLAR);
			} else {
				// Star charts on the shelves.
				fill(level, bb, 9, F + 1, z, 9, F + 4, z, Blocks.BOOKSHELF.defaultBlockState());
				fill(level, bb, 31, F + 1, z, 31, F + 4, z, Blocks.BOOKSHELF.defaultBlockState());
			}
		}
		// The nave: a clerestory over the middle, its pointed vault ribbed above every pair of columns.
		fill(level, bb, 12, F + 11, 10, 28, F + 16, 34, CLERESTORY);
		for (int z = 11; z <= 33; z++) {
			boolean ribbed = z % 6 == 2;
			for (int x = 13; x <= 27; x++) {
				int top = vaultTop(x);
				fill(level, bb, x, F + 11, z, x, ribbed ? top - 1 : top, z, AIR);
				if (ribbed) {
					set(level, bb, x == CX ? CRYING : BRICKS, x, top, z);
					if (x == 13 || x == 27) {
						set(level, bb, BRICKS, x, F + 11, z);
					}
				}
			}
			if (ribbed) {
				set(level, bb, ROD_DOWN, CX, vaultTop(CX) - 1, z);
			}
		}
		// Lunette windows in the clerestory, one over each row of orreries.
		for (int z : new int[] {17, 23, 29}) {
			for (int y = F + 13; y <= F + 14; y++) {
				set(level, bb, GLASS, 12, y, z);
				set(level, bb, GLASS, 28, y, z);
				fill(level, bb, 13, y, z, 14, y, z, AIR);
				fill(level, bb, 26, y, z, 27, y, z, AIR);
			}
		}
		// Columns on obsidian plinths, each with a capital of purpur corbels.
		for (int z : new int[] {14, 20, 26, 32}) {
			for (int x : new int[] {13, 27}) {
				pillar(level, bb, x, z, F + 1, F + 10, OBSIDIAN, PILLAR, PURPUR);
				set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.EAST, true), x - 1, F + 10, z);
				set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.WEST, true), x + 1, F + 10, z);
				set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.NORTH, true), x, F + 10, z - 1);
				set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.SOUTH, true), x, F + 10, z + 1);
			}
		}
		// Purple colours high on the aisle walls.
		for (int z : new int[] {16, 24}) {
			banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.PURPLE), 9, F + 7, z, Direction.EAST);
			banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.PURPLE), 31, F + 7, z, Direction.WEST);
		}
		// Outside: buttresses down both long walls, pilasters on the clerestory, parapets on both roofs.
		for (int z : new int[] {14, 20, 26, 32}) {
			foundation(level, bb, 5, z, 7, z);
			foundation(level, bb, 33, z, 35, z);
			buttress(level, bb, 7, z, F, 10, Direction.WEST, BRICKS, Blocks.END_STONE_BRICK_STAIRS);
			buttress(level, bb, 33, z, F, 10, Direction.EAST, BRICKS, Blocks.END_STONE_BRICK_STAIRS);
			fill(level, bb, 12, F + 11, z, 12, F + 16, z, PILLAR);
			fill(level, bb, 28, F + 11, z, 28, F + 16, z, PILLAR);
		}
		crenellate(level, bb, 8, 10, 32, 34, F + 12, BRICKS);
		crenellate(level, bb, 12, 10, 28, 34, F + 17, BRICKS);
		// Orreries: amethyst worlds hanging on chains from the vault, an end rod shining down from each.
		for (int z : new int[] {17, 23, 29}) {
			for (int x : new int[] {17, 23}) {
				fill(level, bb, x, F + 8, z, x, vaultTop(x), z, Blocks.IRON_CHAIN.defaultBlockState());
				set(level, bb, Blocks.AMETHYST_BLOCK.defaultBlockState(), x, F + 7, z);
				set(level, bb, ROD_DOWN, x, F + 6, z);
			}
		}
		chest(level, bb, 30, F + 1, 32, DungeonWorldgen.ASTRAL_HALL, Direction.WEST, 11);
	}

	/** The top of the nave's pointed vault over column x: one block over the aisle ceiling at the sides, four at the crown. */
	private static int vaultTop(int x) {
		double t = (x - CX) / 7.5;
		return F + 11 + (int) Math.round(4 * (1 - t * t));
	}

	// ------------------------------------------------------------------ the corridor

	private void corridor(WorldGenLevel level, BoundingBox bb) {
		foundation(level, bb, 14, 34, 26, 47);
		room(level, bb, 14, F, 34, 26, F + 8, 47, PURPUR);
		fill(level, bb, 15, F, 35, 25, F, 46, PURPUR);
		fill(level, bb, 19, F, 35, 21, F, 46, BRICKS);
		for (int z = 36; z <= 45; z++) {
			if (z % 2 == 1) {
				fill(level, bb, 14, F + 3, z, 14, F + 5, z, GLASS);
				fill(level, bb, 26, F + 3, z, 26, F + 5, z, GLASS);
			} else {
				set(level, bb, ROD_UP, 15, F + 1, z);
				set(level, bb, ROD_UP, 25, F + 1, z);
			}
		}
		// A purpur cornice under the ceiling, end rods hanging down the middle.
		for (int z = 35; z <= 46; z++) {
			set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.WEST, true), 15, F + 7, z);
			set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.EAST, true), 25, F + 7, z);
			if (z % 4 == 0) {
				set(level, bb, ROD_DOWN, CX, F + 7, z);
			}
		}
	}

	// ------------------------------------------------------------------ the Dome

	/** The dome, from its foundations to its glass, and the vault off it (with the door between). */
	@Override
	public java.util.List<BoundingBox> wardedBoxes() {
		return java.util.List.of(
			worldBox(CX - DOME_R - 3, 0, DOME_Z - DOME_R - 3, CX + DOME_R + 3, HEIGHT - 1, Math.min(DEPTH - 1, DOME_Z + DOME_R + 3)),
			worldBox(30, 0, 53, WIDTH - 1, F + 10, 67));
	}

	private void dome(WorldGenLevel level, BoundingBox bb) {
		double rise = DOME_R * 0.9;
		for (int x = CX - DOME_R - 2; x <= CX + DOME_R + 2; x++) {
			for (int z = DOME_Z - DOME_R - 2; z <= DOME_Z + DOME_R + 2; z++) {
				double d = dist(x, z, CX, DOME_Z);
				if (d > DOME_R + 1.5) {
					continue;
				}
				foundation(level, bb, x, z, x, z);
				boolean wall = d > DOME_R;
				set(level, bb, wall ? PURPUR : floor(x, z, d), x, F, z);
				for (int y = F + 1; y <= F + 6; y++) {
					set(level, bb, wall ? (y == F + 6 ? BRICKS : y == F + 1 ? OBSIDIAN : PURPUR) : AIR, x, y, z);
				}
				// The glass shell: an upturned bowl over the room, with a slit on the far side for the telescope.
				boolean slit = Math.abs(x - CX) <= 1 && z > DOME_Z;
				for (int y = F + 7; y <= F + 7 + (int) Math.ceil(rise) + 1; y++) {
					double h = y - (F + 6);
					double e = (d / (DOME_R + 0.5)) * (d / (DOME_R + 0.5)) + (h / rise) * (h / rise);
					if (e <= 0.86) {
						set(level, bb, AIR, x, y, z);
					} else if (e <= 1.08) {
						// Purpur ribs over the pillars, a froglight boss at the crown, glass between.
						BlockState pane = d < 1.5 ? STAR : d > 2 && rib(x, z) ? PURPUR : noise(x, y, z) < 22 ? DARK_GLASS : GLASS;
						set(level, bb, slit && h >= 2 ? AIR : pane, x, y, z);
					}
				}
			}
		}
		// Eight pillars round the edge, each crowned with an end rod.
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8 + Math.PI / 8;
			int px = CX + (int) Math.round(Math.cos(a) * 11.5);
			int pz = DOME_Z + (int) Math.round(Math.sin(a) * 11.5);
			pillar(level, bb, px, pz, F + 1, F + 5, OBSIDIAN, PILLAR, PURPUR);
			set(level, bb, ROD_UP, px, F + 6, pz);
		}
		// A star hangs over the altar from the dome's crown, its rays of end rods reaching out.
		fill(level, bb, CX, F + 13, DOME_Z, CX, F + 16, DOME_Z, Blocks.IRON_CHAIN.defaultBlockState());
		set(level, bb, STAR, CX, F + 12, DOME_Z);
		set(level, bb, ROD_DOWN, CX, F + 11, DOME_Z);
		for (Direction dir : Direction.Plane.HORIZONTAL) {
			set(level, bb, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, dir), CX + dir.getStepX(), F + 12, DOME_Z + dz(dir));
		}
		// The telescope: a mount, and a tube of old copper climbing toward the slit, a lens at its end.
		fill(level, bb, CX, F + 1, 67, CX, F + 3, 67, PILLAR);
		for (int k = 0; k <= 6; k++) {
			set(level, bb, k == 6 ? Blocks.TINTED_GLASS.defaultBlockState() : TUBE, CX, F + 4 + k, 67 + k);
		}
		// The way in, and the altar at the heart of the star map.
		fill(level, bb, 18, F, 45, 22, F, 49, PURPUR);
		fill(level, bb, 18, F + 1, 45, 22, F + 5, 49, AIR);
		// Its portal through the dome wall: purpur jambs, corbels and a lintel keyed with crying obsidian.
		for (int x : new int[] {17, 23}) {
			fill(level, bb, x, F + 1, 47, x, F + 6, 47, PILLAR);
		}
		fill(level, bb, 18, F + 6, 47, 22, F + 6, 47, BRICKS);
		set(level, bb, CRYING, CX, F + 6, 47);
		set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.WEST, true), 18, F + 5, 47);
		set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.EAST, true), 22, F + 5, 47);
		altar(level, bb, DungeonAltarBlock.Kind.ASTRAL, CX, F + 1, DOME_Z);
	}

	/** The star map: obsidian night, two rings, three constellations of stone with shining stars. */
	private BlockState floor(int x, int z, double d) {
		long cell = key(x - CX, z - DOME_Z);
		if (STARS.contains(cell)) {
			return STAR;
		}
		if (LINES.contains(cell) || Math.abs(d - 12) < 0.55) {
			return BRICKS;
		}
		if (Math.abs(d - 7.5) < 0.5) {
			return PURPUR;
		}
		if (d < 1.6) {
			return CRYING;
		}
		return noise(x, z) < 3 ? STAR : OBSIDIAN;
	}

	/** Whether a column lies under one of the dome's eight ribs, which spring from the pillars. */
	private static boolean rib(int x, int z) {
		double a = Math.atan2(z - DOME_Z, x - CX) - Math.PI / 8;
		double off = Math.abs(Math.IEEEremainder(a, Math.PI / 4));
		return dist(x, z, CX, DOME_Z) * Math.sin(off) < 0.6;
	}

	// ------------------------------------------------------------------ the Vault

	private void vault(WorldGenLevel level, BoundingBox bb) {
		foundation(level, bb, 33, 54, 40, 66);
		room(level, bb, 33, F, 54, 40, F + 8, 66, PURPUR);
		fill(level, bb, 34, F, 55, 39, F, 65, BRICKS);
		fill(level, bb, 35, F, 58, 38, F, 62, OBSIDIAN);
		fill(level, bb, 31, F, 58, 33, F, 62, PURPUR);
		fill(level, bb, 31, F + 1, 58, 33, F + 4, 62, AIR);
		sealDoorAlong(level, bb, 33, 58, 62, F + 1, F + 4, RuneSealBlock.Element.VOID, RuneSealBlock.Element.LIFE);
		chest(level, bb, 38, F + 1, 56, DungeonWorldgen.ASTRAL_VAULT, Direction.WEST, 12);
		chest(level, bb, 38, F + 1, 64, DungeonWorldgen.ASTRAL_VAULT, Direction.WEST, 13);
		set(level, bb, CRYING, 38, F + 1, 60);
		set(level, bb, ROD_UP, 38, F + 2, 60);
		set(level, bb, ROD_DOWN, 36, F + 7, 57);
		set(level, bb, ROD_DOWN, 36, F + 7, 63);
		// A cornice round the treasury and purpur pilasters against its back wall.
		for (int z = 55; z <= 65; z++) {
			set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.WEST, true), 34, F + 7, z);
			set(level, bb, stair(Blocks.PURPUR_STAIRS, Direction.EAST, true), 39, F + 7, z);
		}
		for (int z : new int[] {58, 62}) {
			pillar(level, bb, 39, z, F + 1, F + 6, OBSIDIAN, PILLAR, PURPUR);
		}
	}

	// ------------------------------------------------------------------ guards

	private void guards(WorldGenLevel level, BoundingBox bb) {
		guard(level, bb, EntityTypes.STRAY, 14, F + 1, 18, List.of(Runes.SPARK, Runes.HARM), false);
		guard(level, bb, EntityTypes.SKELETON, 26, F + 1, 22, List.of(Runes.BOLT, Runes.BLIND), false);
		guard(level, bb, EntityTypes.STRAY, 20, F + 1, 28, List.of(Runes.ORB, Runes.PULL), true);
		guard(level, bb, EntityTypes.WITCH, 24, F + 1, 31, List.of(Runes.BOLT, Runes.HEX), false);
	}
}
