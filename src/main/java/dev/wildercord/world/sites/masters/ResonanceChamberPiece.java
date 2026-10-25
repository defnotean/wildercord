package dev.wildercord.world.sites.masters;

import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * A deep resonance chamber of the Hollow or Echo school, found by the bell frame over its shaft: a long ladder down to a hall
 * of amethyst and sculk, and a vault sealed with the school's element. Local y=0 is the chamber floor; the ground is
 * {@link #surface()} above it.
 */
public final class ResonanceChamberPiece extends MasterSitePiece {
	/** Blocks of the piece above the ground: the bell frame. */
	static final int ABOVE = 6;

	public ResonanceChamberPiece(int x, int y, int z, Direction facing, boolean alt, int height) {
		super(MasterSites.RESONANCE_CHAMBER, x, y, z, 17, height, 17, facing, alt);
	}
	public ResonanceChamberPiece(CompoundTag tag) { super(MasterSites.RESONANCE_CHAMBER, tag); }
	@Override public String site() { return "master_resonance_chamber"; }
	@Override public List<String> schools() { return List.of("hollow", "echo"); }

	/** The ground's height above the chamber floor (where the shaft comes out). */
	public int surface() { return getBoundingBox().getYSpan() - ABOVE; }

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		ChunkPos chunk = c.chunkPos();
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(c.random());
		var probe = new ResonanceChamberPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), facing, altFor(chunk), 40);
		var shaft = probe.localPosition(8, 0, 2);
		int surface = height(c, shaft, Heightmap.Types.WORLD_SURFACE_WG);
		if (surface <= c.chunkGenerator().getSeaLevel() || surface != height(c, shaft, Heightmap.Types.OCEAN_FLOOR_WG)) return Optional.empty();
		for (int[] k : new int[][]{{5, 0}, {11, 0}, {5, 4}, {11, 4}}) {
			if (Math.abs(height(c, probe.localPosition(k[0], 0, k[1]), Heightmap.Types.WORLD_SURFACE_WG) - surface) > 3) return Optional.empty();
		}
		int floor = Math.max(c.heightAccessor().getMinY() + 16, Math.min(surface - 50, -20));
		if (surface + ABOVE > c.heightAccessor().getMaxY()) return Optional.empty();
		var piece = new ResonanceChamberPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), facing, altFor(chunk), surface - floor + ABOVE);
		piece.move(0, floor, 0);
		var centre = piece.localPosition(8, 1, 8);
		return Optional.of(new Structure.GenerationStub(centre, b -> b.addPiece(piece)));
	}

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);
		BlockState wall = alt ? s(Blocks.TUFF_BRICKS) : s(Blocks.DEEPSLATE_TILES), trim = alt ? s(Blocks.POLISHED_TUFF) : s(Blocks.POLISHED_DEEPSLATE);
		BlockState patch = alt ? s(Blocks.AMETHYST_BLOCK) : s(Blocks.SCULK), light = alt ? s(Blocks.PEARLESCENT_FROGLIGHT) : s(Blocks.SEA_LANTERN);
		BlockState chiseled = alt ? s(Blocks.CHISELED_TUFF_BRICKS) : s(Blocks.CHISELED_DEEPSLATE);
		BlockState slab = alt ? s(Blocks.POLISHED_TUFF_SLAB) : s(Blocks.DEEPSLATE_TILE_SLAB);
		Block trimStairs = alt ? Blocks.POLISHED_TUFF_STAIRS : Blocks.POLISHED_DEEPSLATE_STAIRS;
		Block roofStairs = alt ? Blocks.TUFF_BRICK_STAIRS : Blocks.DEEPSLATE_TILE_STAIRS;
		int s = surface();
		// The chamber.
		room(level, bb, 1, 0, 3, 15, 8, 16, wall);
		for (int x = 2; x <= 14; x++) for (int z = 4; z <= 15; z++) if (noise(x, z) < 30) set(level, bb, patch, x, 0, z);
		// A border round the floor and a resonance ring inlaid at its heart.
		for (int x = 2; x <= 14; x++) for (int z = 4; z <= 15; z++) {
			double d = dist(x, z, 8, 8);
			if (x == 2 || x == 14 || z == 4 || z == 15 || (d >= 2.5 && d < 3.3)) set(level, bb, trim, x, 0, z);
		}
		set(level, bb, chiseled, 8, 0, 8);
		// A corbel course under the ceiling, and ribs across it.
		for (int z = 5; z <= 14; z++) {
			set(level, bb, stair(trimStairs, Direction.WEST, true), 2, 7, z);
			set(level, bb, stair(trimStairs, Direction.EAST, true), 14, 7, z);
		}
		for (int x = 3; x <= 13; x++) {
			set(level, bb, stair(trimStairs, Direction.SOUTH, true), x, 7, 4);
			set(level, bb, stair(trimStairs, Direction.NORTH, true), x, 7, 15);
		}
		for (int[] c : new int[][]{{2, 4}, {14, 4}, {2, 15}, {14, 15}}) set(level, bb, trim, c[0], 7, c[1]);
		for (int x : new int[]{4, 8, 12}) fill(level, bb, x, 8, 4, x, 8, 15, trim);
		// Pilasters down both long walls, banded in the school's stone.
		for (int z : new int[]{6, 10, 14}) {
			pillar(level, bb, 2, z, 1, 7, trim, chiseled, trim);
			pillar(level, bb, 14, z, 1, 7, trim, chiseled, trim);
		}
		// The four resonant columns, lit at their crowns.
		for (int[] p : new int[][]{{4, 7}, {12, 7}, {4, 13}, {12, 13}}) {
			fill(level, bb, p[0], 1, p[1], p[0], 7, p[1], s(Blocks.AMETHYST_BLOCK));
			set(level, bb, trim, p[0], 1, p[1]);
			set(level, bb, light, p[0], 8, p[1]);
		}
		set(level, bb, light, 8, 8, 7);
		hangingLantern(level, bb, 8, 7, 10, 1, true);
		hangingLantern(level, bb, 4, 7, 10, 2, true); hangingLantern(level, bb, 12, 7, 10, 2, true);
		// Shelves of recorded tones along the back wall.
		fill(level, bb, 2, 1, 15, 3, 3, 15, s(Blocks.BOOKSHELF)); fill(level, bb, 13, 1, 15, 14, 3, 15, s(Blocks.BOOKSHELF));
		set(level, bb, s(alt ? Blocks.AMETHYST_CLUSTER : Blocks.CANDLE), 3, 4, 15); set(level, bb, s(alt ? Blocks.AMETHYST_CLUSTER : Blocks.CANDLE), 13, 4, 15);
		// The shaft: a ladder up to the ground, coming in through the front wall.
		fill(level, bb, 7, 1, 1, 9, s, 3, trim);
		fill(level, bb, 8, 1, 2, 8, s, 2, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH));
		fill(level, bb, 8, 1, 3, 8, 2, 3, AIR);
		// The shaft door, framed and lintelled on the chamber side.
		set(level, bb, chiseled, 7, 1, 3); set(level, bb, chiseled, 9, 1, 3); set(level, bb, chiseled, 8, 3, 3);
		set(level, bb, stair(trimStairs, Direction.SOUTH, true), 8, 3, 4);
		sconce(level, bb, 7, 2, 4, Direction.NORTH, true); sconce(level, bb, 9, 2, 4, Direction.NORTH, true);
		// The bell pavilion over it: a stepped platform, four posts and a hipped stone roof.
		fill(level, bb, 5, s, 0, 11, s, 4, trim);
		for (int x = 5; x <= 11; x++) for (int z = 0; z <= 4; z++) fillColumnDown(level, wall, x, s - 1, z, bb);
		for (int z = 0; z <= 4; z++) {
			set(level, bb, stair(trimStairs, Direction.EAST, false), 4, s, z);
			set(level, bb, stair(trimStairs, Direction.WEST, false), 12, s, z);
			fillColumnDown(level, wall, 4, s - 1, z, bb); fillColumnDown(level, wall, 12, s - 1, z, bb);
		}
		for (int x = 5; x <= 11; x++) { set(level, bb, stair(trimStairs, Direction.SOUTH, false), x, s, 5); fillColumnDown(level, wall, x, s - 1, 5, bb); }
		for (int x = 6; x <= 10; x++) set(level, bb, chiseled, x, s, 2);
		set(level, bb, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH), 8, s, 2);
		fill(level, bb, 5, s + 1, 0, 11, s + 5, 4, AIR);
		for (int[] c : new int[][]{{5, 0}, {11, 0}, {5, 4}, {11, 4}}) pillar(level, bb, c[0], c[1], s + 1, s + 3, chiseled, wall, trim);
		for (int z = 1; z <= 3; z++) { set(level, bb, slab, 5, s + 1, z); set(level, bb, slab, 11, s + 1, z); }
		for (int x : new int[]{6, 10}) { set(level, bb, slab, x, s + 1, 0); set(level, bb, slab, x, s + 1, 4); }
		fill(level, bb, 6, s + 1, 2, 6, s + 4, 2, wall); fill(level, bb, 10, s + 1, 2, 10, s + 4, 2, wall);
		for (int x = 6; x <= 10; x++) {
			set(level, bb, stair(roofStairs, Direction.NORTH, false), x, s + 4, 0);
			set(level, bb, stair(roofStairs, Direction.SOUTH, false), x, s + 4, 4);
		}
		for (int z = 1; z <= 3; z++) {
			set(level, bb, stair(roofStairs, Direction.EAST, false), 5, s + 4, z);
			set(level, bb, stair(roofStairs, Direction.WEST, false), 11, s + 4, z);
		}
		for (int[] c : new int[][]{{5, 0}, {11, 0}, {5, 4}, {11, 4}}) set(level, bb, chiseled, c[0], s + 4, c[1]);
		for (int x = 7; x <= 9; x++) {
			set(level, bb, stair(roofStairs, Direction.NORTH, false), x, s + 5, 1);
			set(level, bb, stair(roofStairs, Direction.SOUTH, false), x, s + 5, 3);
		}
		for (int[] c : new int[][]{{6, 1}, {10, 1}, {6, 3}, {10, 3}}) set(level, bb, trim, c[0], s + 5, c[1]);
		fill(level, bb, 6, s + 5, 2, 10, s + 5, 2, trim);
		set(level, bb, s(Blocks.BELL).setValue(BellBlock.FACING, Direction.EAST).setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING), 8, s + 4, 2);
		hangingLantern(level, bb, 8, s + 3, 0, 0, alt); hangingLantern(level, bb, 8, s + 3, 4, 0, alt);
		// The vault, sealed with the school's element, behind a columned front.
		room(level, bb, 5, 0, 11, 11, 5, 16, trim);
		for (int x : new int[]{6, 10}) pillar(level, bb, x, 10, 1, 4, trim, chiseled, trim);
		for (int x = 7; x <= 9; x++) set(level, bb, stair(trimStairs, Direction.NORTH, true), x, 4, 10);
		set(level, bb, chiseled, 8, 5, 10);
		var e = element(school());
		sealDoorAcross(level, bb, 11, 7, 9, 1, 3, e, e);
		set(level, bb, light, 8, 5, 13);
		chest(level, bb, 8, 1, 14, rewardLoot(), Direction.SOUTH, 2);
		// Practice and the lectern.
		var post = alt ? s(Blocks.BIRCH_FENCE) : s(Blocks.DARK_OAK_FENCE);
		dummy(level, bb, 6, 1, 7, post); dummy(level, bb, 10, 1, 7, post);
		target(level, bb, 3, 1, 10, post); target(level, bb, 13, 1, 10, post);
		lectern(level, bb, 6, 1, 4);
		chest(level, bb, 13, 1, 4, MasterSites.supplies(site()), Direction.WEST, 1);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(5, 0, 11, 11, 5, 16)); }
}
