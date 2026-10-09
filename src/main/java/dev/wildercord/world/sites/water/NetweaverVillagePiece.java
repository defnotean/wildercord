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
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * Three net-weavers' huts on stilts in a mangrove swamp, joined by boardwalks to a platform in the middle where the
 * nets are mended. Two of the weavers are home. In its own frame the water's surface is at y {@link #BASE}, the walks
 * at {@link #DECK}; the way in comes up steps from z 0.
 */
public class NetweaverVillagePiece extends WaterPiece {
	public static final int WIDTH = 27, HEIGHT = 18, DEPTH = 27;
	public static final int BASE = 6;
	/** The walks and hut floors: you stand at {@code DECK + 1}. */
	public static final int DECK = BASE + 3;
	/** The huts' centres. */
	static final int[][] HUTS = {{6, 6}, {20, 8}, {12, 20}};
	static final int CX = 13, CZ = 13;

	private static final BlockState PLANKS = Blocks.MANGROVE_PLANKS.defaultBlockState();
	private static final BlockState LOG = Blocks.MANGROVE_LOG.defaultBlockState();
	private static final BlockState FENCE = Blocks.SPRUCE_FENCE.defaultBlockState();

	public NetweaverVillagePiece(int x, int y, int z, Direction facing) {
		super(WaterSites.NETWEAVER_VILLAGE, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public NetweaverVillagePiece(CompoundTag tag) {
		super(WaterSites.NETWEAVER_VILLAGE, tag);
	}

	/** Shallow swamp or low mud round the middle and under every hut. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		ChunkPos chunk = c.chunkPos();
		NetweaverVillagePiece piece = new NetweaverVillagePiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), Direction.Plane.HORIZONTAL.getRandomDirection(c.random()));
		int sea = c.chunkGenerator().getSeaLevel();
		BlockPos mid = piece.getWorldPos(CX, 0, CZ);
		if (!low(c, mid, sea)) {
			return Optional.empty();
		}
		for (int[] h : HUTS) {
			if (!low(c, piece.getWorldPos(h[0], 0, h[1]), sea)) {
				return Optional.empty();
			}
		}
		piece.move(0, sea - 1 - BASE, 0);
		return stub(piece, new BlockPos(mid.getX(), surface(c, mid) - 1, mid.getZ()));
	}

	/** Water no deeper than 4, or ground at most two over the sea's surface. */
	private static boolean low(Structure.GenerationContext c, BlockPos at, int sea) {
		int top = surface(c, at) - 1;
		return top <= sea + 1 && top >= sea - 1 && depth(c, at) <= 4;
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos reference) {
		walks(level, bb);
		hut(level, bb, HUTS[0][0], HUTS[0][1], 1, 0);
		hut(level, bb, HUTS[1][0], HUTS[1][1], -1, 0);
		hut(level, bb, HUTS[2][0], HUTS[2][1], 0, -1);
		furnish(level, bb);
	}

	/** One stretch of boardwalk on stilts, cleared over. */
	private void walk(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if ((x + z) % 3 == 0) {
					post(level, bb, LOG, x, z, DECK - 1);
				}
				set(level, bb, PLANKS, x, DECK, z);
				clear(level, bb, x, DECK + 1, z, x, DECK + 3, z);
			}
		}
	}

	private void walks(WorldGenLevel level, BoundingBox bb) {
		// The way up from the bank: two steps, then the walk in.
		for (int x = 12; x <= 13; x++) {
			post(level, bb, LOG, x, 0, DECK - 3);
			set(level, bb, stair(Blocks.MANGROVE_STAIRS, Direction.NORTH), x, DECK - 2, 0);
			clear(level, bb, x, DECK - 1, 0, x, DECK + 2, 0);
			post(level, bb, LOG, x, 1, DECK - 2);
			set(level, bb, stair(Blocks.MANGROVE_STAIRS, Direction.NORTH), x, DECK - 1, 1);
			clear(level, bb, x, DECK, 1, x, DECK + 2, 1);
		}
		walk(level, bb, 12, 2, 13, 10);
		walk(level, bb, 10, 6, 11, 7);
		walk(level, bb, 14, 8, 16, 9);
		walk(level, bb, 12, 16, 13, 16);
		// The middle, where the nets are mended.
		walk(level, bb, 11, 11, 15, 15);
		for (int[] p : new int[][] {{11, 11}, {15, 11}, {11, 15}, {15, 15}}) {
			post(level, bb, LOG, p[0], p[1], DECK - 1);
			set(level, bb, FENCE, p[0], DECK + 1, p[1]);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), p[0], DECK + 2, p[1]);
		}
	}

	/** A hut on a 7-wide platform round {@code (cx, cz)}, its door in the wall toward {@code (sx, sz)}. */
	private void hut(WorldGenLevel level, BoundingBox bb, int cx, int cz, int sx, int sz) {
		for (int x = cx - 3; x <= cx + 3; x++) {
			for (int z = cz - 3; z <= cz + 3; z++) {
				boolean corner = Math.abs(x - cx) == 3 && Math.abs(z - cz) == 3;
				if (corner || Math.abs(x - cx) + Math.abs(z - cz) == 3 && (x == cx || z == cz)) {
					post(level, bb, LOG, x, z, DECK - 1);
				}
				set(level, bb, PLANKS, x, DECK, z);
				clear(level, bb, x, DECK + 1, z, x, DECK + 6, z);
			}
		}
		fill(level, bb, cx - 2, DECK + 1, cz - 2, cx + 2, DECK + 3, cz + 2, PLANKS);
		fill(level, bb, cx - 1, DECK + 1, cz - 1, cx + 1, DECK + 3, cz + 1, AIR);
		for (int[] k : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
			fill(level, bb, cx + k[0], DECK + 1, cz + k[1], cx + k[0], DECK + 3, cz + k[1], LOG);
		}
		// Windows on every side, then the door cut through one of them.
		set(level, bb, Blocks.GLASS.defaultBlockState(), cx - 2, DECK + 2, cz);
		set(level, bb, Blocks.GLASS.defaultBlockState(), cx + 2, DECK + 2, cz);
		set(level, bb, Blocks.GLASS.defaultBlockState(), cx, DECK + 2, cz - 2);
		set(level, bb, Blocks.GLASS.defaultBlockState(), cx, DECK + 2, cz + 2);
		int dx = sx * 2, dz = sz * 2;
		fill(level, bb, cx + dx, DECK + 1, cz + dz, cx + dx, DECK + 2, cz + dz, AIR);
		// A stepped roof: a slab skirt, a planked course, a slab cap.
		fill(level, bb, cx - 3, DECK + 4, cz - 3, cx + 3, DECK + 4, cz + 3, Blocks.MANGROVE_SLAB.defaultBlockState());
		fill(level, bb, cx - 2, DECK + 4, cz - 2, cx + 2, DECK + 4, cz + 2, PLANKS);
		fill(level, bb, cx - 1, DECK + 5, cz - 1, cx + 1, DECK + 5, cz + 1, Blocks.MANGROVE_SLAB.defaultBlockState());
		set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), cx, DECK + 3, cz);
	}

	/** What's in each hut, the nets in the middle, and the two weavers who are home. */
	private void furnish(WorldGenLevel level, BoundingBox bb) {
		BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
		// The first hut is the workshop: the loom and the village's chest.
		set(level, bb, Blocks.LOOM.defaultBlockState(), 5, DECK + 1, 5);
		set(level, bb, barrel, 5, DECK + 1, 7);
		chest(level, bb, 6, DECK + 1, 7, WaterSites.NETWEAVER_LOOT, Direction.SOUTH, 307);
		// The second and third are homes.
		bed(level, bb, 20, 7, Direction.EAST);
		set(level, bb, barrel, 21, DECK + 1, 9);
		set(level, bb, Blocks.DRIED_KELP_BLOCK.defaultBlockState(), 20, DECK + 1, 9);
		bed(level, bb, 11, 20, Direction.NORTH);
		set(level, bb, barrel, 13, DECK + 1, 21);
		set(level, bb, Blocks.COMPOSTER.defaultBlockState(), 13, DECK + 1, 20);
		// The middle: nets spread to dry, kelp drying, a cauldron of water.
		for (int x = 12; x <= 14; x++) {
			set(level, bb, Blocks.COBWEB.defaultBlockState(), x, DECK + 1, 15);
		}
		set(level, bb, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3), 14, DECK + 1, 12);
		set(level, bb, Blocks.DRIED_KELP_BLOCK.defaultBlockState(), 12, DECK + 1, 12);
		set(level, bb, barrel, 15, DECK + 1, 13);
		spawn(level, bb, EntityTypes.VILLAGER, 6, DECK + 1, 6, 0);
		spawn(level, bb, EntityTypes.VILLAGER, 20, DECK + 1, 8, 0);
	}

	/** A bed with its foot at {@code (x, z)}, its head one block toward {@code facing} (in the piece's frame NORTH is +z). */
	private void bed(WorldGenLevel level, BoundingBox bb, int x, int z, Direction facing) {
		BlockState bed = Blocks.BED.cyan().defaultBlockState().setValue(BedBlock.FACING, facing);
		set(level, bb, bed.setValue(BedBlock.PART, BedPart.FOOT), x, DECK + 1, z);
		set(level, bb, bed.setValue(BedBlock.PART, BedPart.HEAD), x + facing.getStepX(), DECK + 1, z - facing.getStepZ());
	}
}
