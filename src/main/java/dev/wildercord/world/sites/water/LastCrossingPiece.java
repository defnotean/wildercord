package dev.wildercord.world.sites.water;

import dev.wildercord.world.ferryman.FerrymanContent;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.Optional;

/** A shallow ferry basin with dry crossing paths. Ringing its bell is optional; every edge remains open. */
public final class LastCrossingPiece extends WaterPiece {
	public static final int SIZE = 35, HEIGHT = 21, BASE = 12, DECK = BASE + 1;
	public LastCrossingPiece(int x, int y, int z, Direction facing) { super(WaterSites.LAST_CROSSING, x, y, z, SIZE, HEIGHT, SIZE, facing); }
	public LastCrossingPiece(CompoundTag tag) { super(WaterSites.LAST_CROSSING, tag); }
	public BlockPos bellPosition() { return localPosition(17, DECK + 1, 4); }
	public BlockPos mooringPosition() { return localPosition(17, DECK + 1, 17); }
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		var chunk = c.chunkPos();
		var piece = new LastCrossingPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		int sea = c.chunkGenerator().getSeaLevel(); boolean pool = false;
		for (int[] probe : new int[][] {{3,3},{31,3},{3,31},{31,31},{17,17},{17,3},{3,17},{31,17},{17,31}}) {
			var at = piece.localPosition(probe[0], 0, probe[1]); int top = surface(c, at);
			if (top < sea || top > sea + 2 || floor(c, at) < sea - BASE) return Optional.empty();
			pool |= water(c, at, 1, BASE);
		}
		// A wide basin can cross a deeper channel than a small landing. Every actual interior support
		// must still meet the ground within the taller declared foundation; no unbounded downward fills.
		for (int x : new int[] {4,13,21,30}) for (int z : new int[] {4,13,21,30}) {
			var at=piece.localPosition(x,0,z);
			if (floor(c,at)<sea-BASE || surface(c,at)>sea+2) return Optional.empty();
		}
		if (!pool) return Optional.empty();
		piece.move(0, sea - 1 - BASE, 0); return stub(piece, piece.mooringPosition());
	}
	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
		BoundingBox clip, ChunkPos chunk, BlockPos reference) {
		var planks = Blocks.SPRUCE_PLANKS.defaultBlockState(); var log = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
		// Replaying a clipped piece must not erase the saved bell identity or refill an opened chest.
		for (int x=2;x<=32;x++) for (int y=DECK+1;y<HEIGHT;y++) for (int z=2;z<=32;z++)
			if (!furniture(x,y,z)) set(level,clip,AIR,x,y,z);
		fill(level, clip, 2, DECK - 1, 2, 32, DECK - 1, 32, Blocks.MUD_BRICKS.defaultBlockState());
		fill(level, clip, 2, DECK, 2, 32, DECK, 32, planks);
		// Four shallow pools, each surrounded by deck. Both safe wake orientations have a five-block-wide dry path.
		for (int x : new int[] {5,22}) for (int z : new int[] {5,22}) {
			fill(level, clip, x, DECK, z, x + 7, DECK, z + 7, Blocks.WATER.defaultBlockState());
			for (int px : new int[] {x - 1, x + 8}) for (int pz : new int[] {z - 1, z + 8}) post(level, clip, log, px, pz, DECK - 2);
		}
		for (int v = 0; v < SIZE; v++) for (int width = 15; width <= 19; width++) {
			set(level, clip, planks, width, DECK, v); set(level, clip, planks, v, DECK, width);
			if (!furniture(width,DECK+1,v)) set(level, clip, AIR, width, DECK + 1, v);
			set(level, clip, AIR, width, DECK + 2, v);
			set(level, clip, AIR, v, DECK + 1, width); set(level, clip, AIR, v, DECK + 2, width);
		}
		for (int x : new int[] {3,31}) for (int z : new int[] {3,31}) {
			post(level, clip, log, x, z, DECK); fill(level, clip, x, DECK + 1, z, x, DECK + 3, z, log);
			set(level, clip, Blocks.LANTERN.defaultBlockState(), x, DECK + 4, z);
		}
		// Open toll shelter, beside the shore entrance rather than across it.
		for (int x : new int[] {14,21}) fill(level, clip, x, DECK + 1, 3, x, DECK + 4, 3, log);
		fill(level, clip, 14, DECK + 5, 2, 21, DECK + 5, 5, Blocks.DARK_PRISMARINE.defaultBlockState());
		set(level, clip, FerrymanContent.BELL.defaultBlockState(), 17, DECK + 1, 4);
		lectern(level, clip, 20, DECK + 1, 4, Direction.SOUTH, "The Last Crossing", "book.wildercord.last_crossing", 2, "Tideward boatkeepers");
		chest(level, clip, 20, DECK + 1, 6, WaterSites.CROSSING_LOOT, Direction.WEST, 317);
	}
	private static boolean furniture(int x,int y,int z) {return y==DECK+1 && ((z==4 && (x==17 || x==20)) || (x==20 && z==6));}
}
