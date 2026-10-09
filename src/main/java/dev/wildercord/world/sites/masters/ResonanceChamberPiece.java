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
		int s = surface();
		// The chamber.
		room(level, bb, 1, 0, 3, 15, 8, 16, wall);
		for (int x = 2; x <= 14; x++) for (int z = 4; z <= 15; z++) if (noise(x, z) < 30) set(level, bb, patch, x, 0, z);
		for (int[] p : new int[][]{{4, 7}, {12, 7}, {4, 13}, {12, 13}}) { fill(level, bb, p[0], 1, p[1], p[0], 7, p[1], s(Blocks.AMETHYST_BLOCK)); set(level, bb, light, p[0], 8, p[1]); }
		set(level, bb, light, 8, 8, 7);
		// The shaft: a ladder up to the ground, coming in through the front wall.
		fill(level, bb, 7, 1, 1, 9, s, 3, trim);
		fill(level, bb, 8, 1, 2, 8, s, 2, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH));
		fill(level, bb, 8, 1, 3, 8, 2, 3, AIR);
		// The bell frame over it.
		fill(level, bb, 5, s, 0, 11, s, 4, trim);
		set(level, bb, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH), 8, s, 2);
		fill(level, bb, 5, s + 1, 0, 11, s + 5, 4, AIR);
		fill(level, bb, 6, s + 1, 2, 6, s + 4, 2, wall); fill(level, bb, 10, s + 1, 2, 10, s + 4, 2, wall);
		fill(level, bb, 6, s + 5, 2, 10, s + 5, 2, trim);
		set(level, bb, s(Blocks.BELL).setValue(BellBlock.FACING, Direction.EAST).setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING), 8, s + 4, 2);
		set(level, bb, s(Blocks.LANTERN), 5, s + 1, 0); set(level, bb, s(Blocks.LANTERN), 11, s + 1, 4);
		// The vault, sealed with the school's element.
		room(level, bb, 5, 0, 11, 11, 5, 16, trim);
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
