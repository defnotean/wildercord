package dev.wildercord.world.sites.water;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * A pier from a beach out over the sea, ending in a boathouse with a boat in its slip, the slip open to the sea at
 * the back. In its own frame the pier leaves the beach up two steps at z 0; the water's surface is at y {@link #BASE}, the deck
 * two over it; the boathouse covers z 20-27.
 */
public class PierBoathousePiece extends WaterPiece {
	public static final int WIDTH = 11, HEIGHT = 23, DEPTH = 28;
	public static final int BASE = 10;
	/** The deck: you stand at {@code DECK + 1}. */
	public static final int DECK = BASE + 2;

	private static final BlockState PLANKS = Blocks.SPRUCE_PLANKS.defaultBlockState();
	private static final BlockState LOG = Blocks.SPRUCE_LOG.defaultBlockState();

	public PierBoathousePiece(int x, int y, int z, Direction facing) {
		super(WaterSites.PIER_BOATHOUSE, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public PierBoathousePiece(CompoundTag tag) {
		super(WaterSites.PIER_BOATHOUSE, tag);
	}

	/**
	 * A low beach or shore with open water running out ahead of it, shallow enough for the posts to reach the floor:
	 * tried each way round from several spots in the chunk.
	 */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		ChunkPos chunk = c.chunkPos();
		int sea = c.chunkGenerator().getSeaLevel();
		java.util.List<Direction> facings = facings(c);
		for (int[] off : OFFSETS) {
			for (Direction facing : facings) {
				PierBoathousePiece piece = new PierBoathousePiece(chunk.getMinBlockX() + off[0], 0, chunk.getMinBlockZ() + off[1], facing);
				BlockPos beach = piece.getWorldPos(5, 0, 1);
				if (land(c, beach, sea - 1, sea + 2) && water(c, piece.getWorldPos(5, 0, 12), 1, BASE - 1)
						&& water(c, piece.getWorldPos(5, 0, 24), 1, BASE - 1)) {
					piece.move(0, sea - 1 - BASE, 0);
					return stub(piece, new BlockPos(beach.getX(), surface(c, beach) - 1, beach.getZ()));
				}
			}
		}
		return Optional.empty();
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos reference) {
		clear(level, bb, 0, DECK + 1, 0, 10, HEIGHT - 1, 27);
		pier(level, bb);
		boathouse(level, bb);
	}

	private void pier(WorldGenLevel level, BoundingBox bb) {
		for (int z = 3; z <= 18; z += 3) {
			post(level, bb, LOG, 3, z, DECK - 1);
			post(level, bb, LOG, 7, z, DECK - 1);
		}
		fill(level, bb, 3, DECK, 2, 7, DECK, 19, PLANKS);
		// Two steps up off the beach.
		for (int x = 3; x <= 7; x++) {
			post(level, bb, Blocks.SPRUCE_PLANKS.defaultBlockState(), x, 0, DECK - 2);
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH), x, DECK - 1, 0);
			clear(level, bb, x, DECK, 0, x, DECK + 2, 0);
			post(level, bb, Blocks.SPRUCE_PLANKS.defaultBlockState(), x, 1, DECK - 1);
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH), x, DECK, 1);
		}
		for (int z = 2; z <= 19; z++) {
			set(level, bb, Blocks.SPRUCE_FENCE.defaultBlockState(), 3, DECK + 1, z);
			set(level, bb, Blocks.SPRUCE_FENCE.defaultBlockState(), 7, DECK + 1, z);
		}
		for (int z = 6; z <= 18; z += 6) {
			set(level, bb, Blocks.LANTERN.defaultBlockState(), 3, DECK + 2, z);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), 7, DECK + 2, z);
		}
	}

	private void boathouse(WorldGenLevel level, BoundingBox bb) {
		// Side walls from the water up, on posts.
		for (int z = 20; z <= 27; z++) {
			for (int x : new int[] {0, 10}) {
				post(level, bb, LOG, x, z, BASE - 1);
				fill(level, bb, x, BASE, z, x, DECK + 4, z, z == 20 || z == 27 ? LOG : PLANKS);
			}
		}
		// The landing across the front and the walkways either side of the slip.
		for (int x : new int[] {1, 9}) {
			for (int z = 20; z <= 26; z += 2) {
				post(level, bb, LOG, x, z, DECK - 1);
			}
		}
		fill(level, bb, 1, DECK, 20, 9, DECK, 21, PLANKS);
		fill(level, bb, 1, DECK, 22, 2, DECK, 26, PLANKS);
		fill(level, bb, 8, DECK, 22, 9, DECK, 26, PLANKS);
		// Front wall with its door, back wall with the slip's mouth.
		fill(level, bb, 1, DECK + 1, 20, 9, DECK + 4, 20, PLANKS);
		fill(level, bb, 4, DECK + 1, 20, 6, DECK + 2, 20, AIR);
		for (int x = 1; x <= 9; x++) {
			post(level, bb, LOG, x, 27, BASE - 1);
			fill(level, bb, x, BASE, 27, x, DECK + 4, 27, PLANKS);
		}
		clear(level, bb, 3, BASE + 1, 22, 7, DECK, 26);
		for (int x = 3; x <= 7; x++) {
			set(level, bb, Blocks.WATER.defaultBlockState(), x, BASE, 27);
			fill(level, bb, x, BASE + 1, 27, x, DECK + 1, 27, AIR);
			// The slip: open water under open air.
			for (int z = 22; z <= 26; z++) {
				if (!wetAt(level, bb, x, BASE, z)) {
					set(level, bb, Blocks.WATER.defaultBlockState(), x, BASE, z);
				}
			}
		}
		gable(level, bb, 0, 10, 20, 27, DECK + 5, Blocks.SPRUCE_STAIRS, PLANKS, PLANKS, 20, 27);
		fill(level, bb, 5, DECK + 5, 24, 5, DECK + 9, 24, Blocks.IRON_CHAIN.defaultBlockState());
		set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 5, DECK + 4, 24);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 1, DECK + 1, 23);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 9, DECK + 1, 22);
		chest(level, bb, 9, DECK + 1, 26, WaterSites.BOATHOUSE_LOOT, Direction.WEST, 305);
		set(level, bb, Blocks.CRAFTING_TABLE.defaultBlockState(), 1, DECK + 1, 26);
		spawn(level, bb, EntityTypes.SPRUCE_BOAT, 5, BASE, 24, 0.6);
	}
}
