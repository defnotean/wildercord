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
		for (int z : new int[] {14, 20, 26, 32}) {
			fill(level, bb, 13, F + 1, z, 13, F + 10, z, PILLAR);
			fill(level, bb, 27, F + 1, z, 27, F + 10, z, PILLAR);
		}
		// Orreries: amethyst worlds hanging on chains, an end rod shining down from each.
		for (int z : new int[] {17, 23, 29}) {
			for (int x : new int[] {17, 23}) {
				fill(level, bb, x, F + 8, z, x, F + 10, z, Blocks.IRON_CHAIN.defaultBlockState());
				set(level, bb, Blocks.AMETHYST_BLOCK.defaultBlockState(), x, F + 7, z);
				set(level, bb, ROD_DOWN, x, F + 6, z);
			}
		}
		chest(level, bb, 30, F + 1, 32, DungeonWorldgen.ASTRAL_HALL, Direction.WEST, 11);
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
					set(level, bb, wall ? (y == F + 6 ? BRICKS : PURPUR) : AIR, x, y, z);
				}
				// The glass shell: an upturned bowl over the room, with a slit on the far side for the telescope.
				boolean slit = Math.abs(x - CX) <= 1 && z > DOME_Z;
				for (int y = F + 7; y <= F + 7 + (int) Math.ceil(rise) + 1; y++) {
					double h = y - (F + 6);
					double e = (d / (DOME_R + 0.5)) * (d / (DOME_R + 0.5)) + (h / rise) * (h / rise);
					if (e <= 0.86) {
						set(level, bb, AIR, x, y, z);
					} else if (e <= 1.08) {
						set(level, bb, slit && h >= 2 ? AIR : noise(x, y, z) < 22 ? DARK_GLASS : GLASS, x, y, z);
					}
				}
			}
		}
		// Eight pillars round the edge, each crowned with an end rod.
		for (int i = 0; i < 8; i++) {
			double a = Math.PI * 2 * i / 8 + Math.PI / 8;
			int px = CX + (int) Math.round(Math.cos(a) * 11.5);
			int pz = DOME_Z + (int) Math.round(Math.sin(a) * 11.5);
			fill(level, bb, px, F + 1, pz, px, F + 5, pz, PILLAR);
			set(level, bb, ROD_UP, px, F + 6, pz);
		}
		// The telescope: a mount, and a tube of old copper climbing toward the slit, a lens at its end.
		fill(level, bb, CX, F + 1, 67, CX, F + 3, 67, PILLAR);
		for (int k = 0; k <= 6; k++) {
			set(level, bb, k == 6 ? Blocks.TINTED_GLASS.defaultBlockState() : TUBE, CX, F + 4 + k, 67 + k);
		}
		// The way in, and the altar at the heart of the star map.
		fill(level, bb, 18, F, 45, 22, F, 49, PURPUR);
		fill(level, bb, 18, F + 1, 45, 22, F + 5, 49, AIR);
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
	}

	// ------------------------------------------------------------------ guards

	private void guards(WorldGenLevel level, BoundingBox bb) {
		guard(level, bb, EntityTypes.STRAY, 14, F + 1, 18, List.of(Runes.SPARK, Runes.HARM), false);
		guard(level, bb, EntityTypes.SKELETON, 26, F + 1, 22, List.of(Runes.BOLT, Runes.BLIND), false);
		guard(level, bb, EntityTypes.STRAY, 20, F + 1, 28, List.of(Runes.ORB, Runes.PULL), true);
		guard(level, bb, EntityTypes.WITCH, 24, F + 1, 31, List.of(Runes.BOLT, Runes.HEX), false);
	}
}
