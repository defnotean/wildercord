package dev.wildercord.world.sites.water;

import dev.wildercord.world.ferryman.FerrymanContent;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.Optional;

/**
 * A shallow ferry basin with dry crossing paths on a stone quay, a roofed toll shelter by the shore entrance and lamp
 * standards at every path mouth. Ringing its bell is optional; every edge remains open.
 */
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
		// The paths read as boardwalks: stripped log kerbs down each edge, a prismarine compass rose under the mooring.
		var alongZ = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z); var alongX = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
		for (int v = 0; v < SIZE; v++) for (int edge : new int[] {15,19}) {
			if (v < 15 || v > 19) { set(level, clip, alongZ, edge, DECK, v); set(level, clip, alongX, v, DECK, edge); }
		}
		for (int x = 15; x <= 19; x++) for (int z = 15; z <= 19; z++) {
			int d = Math.abs(x - 17) + Math.abs(z - 17);
			set(level, clip, d == 0 ? Blocks.DARK_PRISMARINE.defaultBlockState() : d == 1 || x == 17 || z == 17 ? Blocks.PRISMARINE_BRICKS.defaultBlockState() : planks, x, DECK, z);
		}
		// Each pool glows from below and has a little weed in its corners.
		for (int x : new int[] {5,22}) for (int z : new int[] {5,22}) {
			fill(level, clip, x + 3, DECK - 1, z + 3, x + 4, DECK - 1, z + 4, Blocks.SEA_LANTERN.defaultBlockState());
			for (int px = x; px <= x + 7; px++) for (int pz = z; pz <= z + 7; pz++)
				if ((px == x || px == x + 7 || pz == z || pz == z + 7) && noise(px, pz) < 22) set(level, clip, Blocks.SEAGRASS.defaultBlockState(), px, DECK, pz);
		}
		// A dressed stone quay just outside the arena: a deepslate coping on prismarine corbels, piles every few blocks.
		Palette coping = Palette.of(Blocks.DEEPSLATE_BRICKS, 6, Blocks.POLISHED_DEEPSLATE, 3, Blocks.CRACKED_DEEPSLATE_BRICKS, 1);
		for (int v = 1; v <= 33; v++) {
			for (int[] c : new int[][] {{1, v}, {33, v}, {v, 1}, {v, 33}}) set(level, clip, coping.at(c[0], DECK, c[1]), c[0], DECK, c[1]);
			if (v == 1 || v == 33) continue;
			wet(level, clip, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.EAST, true), 1, DECK - 1, v);
			wet(level, clip, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.WEST, true), 33, DECK - 1, v);
			wet(level, clip, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.NORTH, true), v, DECK - 1, 1);
			wet(level, clip, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.SOUTH, true), v, DECK - 1, 33);
			if ((v - 1) % 4 == 0 && (v < 14 || v > 20)) {
				post(level, clip, log, 1, v, DECK - 2); post(level, clip, log, 33, v, DECK - 2);
				post(level, clip, log, v, 1, DECK - 2); post(level, clip, log, v, 33, DECK - 2);
			}
		}
		for (int x : new int[] {1,33}) for (int z : new int[] {1,33}) post(level, clip, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), x, z, DECK - 1);
		// Corner lamp posts on deepslate bases, kept to the arena's very corners.
		for (int x : new int[] {3,31}) for (int z : new int[] {3,31}) {
			post(level, clip, log, x, z, DECK); set(level, clip, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), x, DECK + 1, z);
			fill(level, clip, x, DECK + 2, z, x, DECK + 3, z, log);
			set(level, clip, Blocks.LANTERN.defaultBlockState(), x, DECK + 4, z);
		}
		// Taller lamp standards flank every path where it meets the quay, outside the arena.
		for (int mouth : new int[] {1,33}) for (int side : new int[] {14,20}) {
			for (int[] c : new int[][] {{side, mouth}, {mouth, side}}) {
				post(level, clip, log, c[0], c[1], DECK - 1);
				set(level, clip, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), c[0], DECK, c[1]);
				set(level, clip, Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState(), c[0], DECK + 1, c[1]);
				fill(level, clip, c[0], DECK + 2, c[1], c[0], DECK + 3, c[1], Blocks.DARK_OAK_FENCE.defaultBlockState());
				set(level, clip, Blocks.CHISELED_DEEPSLATE.defaultBlockState(), c[0], DECK + 4, c[1]);
				set(level, clip, Blocks.LANTERN.defaultBlockState(), c[0], DECK + 5, c[1]);
			}
		}
		// The toll shelter, beside the shore entrance rather than across it: four posts, a boarded ceiling and a
		// hipped prismarine roof whose eaves reach over the chest. The path through it stays clear.
		for (int x : new int[] {14,21}) for (int z : new int[] {3,5}) fill(level, clip, x, DECK + 1, z, x, DECK + 5, z, log);
		fill(level, clip, 14, DECK + 5, 2, 21, DECK + 5, 5, Blocks.DARK_OAK_PLANKS.defaultBlockState());
		for (int z : new int[] {3,5}) fill(level, clip, 15, DECK + 5, z, 20, DECK + 5, z, alongX);
		hipRoof(level, clip, 14, 2, 21, 5, DECK + 5, Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE.defaultBlockState(), Blocks.PRISMARINE_BRICK_SLAB.defaultBlockState());
		for (int x : new int[] {14,21}) set(level, clip, stair(Blocks.SPRUCE_STAIRS, x == 14 ? Direction.WEST : Direction.EAST, true), x, DECK + 4, 4);
		for (int x : new int[] {15,19}) hangingLantern(level, clip, x, DECK + 4, 4, 0, false);
		for (int x : new int[] {14,21}) banner(level, clip, Blocks.WALL_BANNER.pick(DyeColor.BLUE), x, DECK + 3, 2, Direction.SOUTH);
		// The boatkeepers' clutter under the eaves, off the path.
		set(level, clip, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 13, DECK + 1, 3);
		set(level, clip, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.EAST), 13, DECK + 1, 4);
		set(level, clip, Blocks.FLOWER_POT.defaultBlockState(), 13, DECK + 2, 3);
		set(level, clip, Blocks.HAY_BLOCK.defaultBlockState(), 22, DECK + 1, 3);
		set(level, clip, Blocks.LANTERN.defaultBlockState(), 22, DECK + 2, 3);
		set(level, clip, FerrymanContent.BELL.defaultBlockState(), 17, DECK + 1, 4);
		lectern(level, clip, 20, DECK + 1, 4, Direction.SOUTH, "The Last Crossing", "book.wildercord.last_crossing", 2, "Tideward boatkeepers");
		chest(level, clip, 20, DECK + 1, 6, WaterSites.CROSSING_LOOT, Direction.WEST, 317);
	}
	private static boolean furniture(int x,int y,int z) {return y==DECK+1 && ((z==4 && (x==17 || x==20)) || (x==20 && z==6));}
}
