package dev.wildercord.world.sites.water;

import dev.wildercord.spell.Runes;
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
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * A sea cave on a stony shore, domed over and lit from a hole in its top, its floor cut with tidepools: two shallow
 * ones bright with pickles and coral, and a deep one where something drowned still keeps watch. A hermit lived here:
 * their notes are on the lectern, their chest up on the ledge. In its own frame the floor is at y {@link #BASE}, the
 * dome centred on x 9, z 9, the way in at z 0.
 */
public class TidepoolGrottoPiece extends WaterPiece {
	public static final int WIDTH = 19, HEIGHT = 16, DEPTH = 19;
	public static final int BASE = 5;
	static final int CX = 9, CZ = 9;
	/** The deep pool's centre. */
	static final int POOL_X = 9, POOL_Z = 12;

	private static final BlockState STONE = Blocks.STONE.defaultBlockState();
	private static final BlockState WATER = Blocks.WATER.defaultBlockState();

	public TidepoolGrottoPiece(int x, int y, int z, Direction facing) {
		super(WaterSites.TIDEPOOL_GROTTO, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public TidepoolGrottoPiece(CompoundTag tag) {
		super(WaterSites.TIDEPOOL_GROTTO, tag);
	}

	/** Dry rock a little over the sea, the way in no higher than the floor: tried each way round. */
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		ChunkPos chunk = c.chunkPos();
		int sea = c.chunkGenerator().getSeaLevel();
		for (Direction facing : facings(c)) {
			TidepoolGrottoPiece piece = new TidepoolGrottoPiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), facing);
			BlockPos mid = piece.getWorldPos(CX, 0, CZ);
			if (!land(c, mid, sea, sea + 5)) {
				continue;
			}
			int ground = surface(c, mid) - 1;
			if (surface(c, piece.getWorldPos(CX, 0, 1)) - 1 > ground + 1) {
				continue;
			}
			piece.move(0, ground - BASE, 0);
			return stub(piece, new BlockPos(mid.getX(), ground, mid.getZ()));
		}
		return Optional.empty();
	}

	/** The cave's rock: stone, with andesite, tuff and mossy cobblestone through it. */
	private static BlockState rock(int n) {
		if (n < 20) {
			return Blocks.ANDESITE.defaultBlockState();
		}
		if (n < 32) {
			return Blocks.TUFF.defaultBlockState();
		}
		return n < 42 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : STONE;
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos reference) {
		int ox = boundingBox.minX(), oz = boundingBox.minZ();
		for (int x = 0; x < WIDTH; x++) {
			for (int z = 0; z < DEPTH; z++) {
				double d = dist(x, z, CX, CZ);
				if (d > 8.5) {
					continue;
				}
				post(level, bb, rock(noise(x + ox, z + oz)), x, z, BASE);
				for (int y = BASE + 1; y < HEIGHT; y++) {
					int dy = y - BASE;
					double r = Math.sqrt(d * d + dy * dy);
					if (r <= 7) {
						set(level, bb, AIR, x, y, z);
					} else if (r <= 8.2) {
						set(level, bb, rock(noise(x + ox, y, z + oz)), x, y, z);
					}
				}
				// The hole in the top that lights it.
				if (d <= 1.2) {
					fill(level, bb, x, BASE + 6, z, x, HEIGHT - 1, z, AIR);
				}
			}
		}
		// The way in from the shore.
		clear(level, bb, CX - 1, BASE + 1, 0, CX + 1, BASE + 3, 3);
		clear(level, bb, CX, BASE + 4, 0, CX, BASE + 4, 2);
		for (int x = CX - 1; x <= CX + 1; x++) {
			for (int z = 0; z <= 2; z++) {
				post(level, bb, Blocks.GRAVEL.defaultBlockState(), x, z, BASE);
			}
		}
		pools(level, bb);
		hermit(level, bb);
		guard(level, bb, EntityTypes.DROWNED, POOL_X, BASE - 2, POOL_Z, List.of(Runes.BOLT, Runes.TIDEHOOK), false);
		sentry(level, bb, EntityTypes.DROWNED, 6, BASE + 1, 10);
	}

	private void pools(WorldGenLevel level, BoundingBox bb) {
		BlockState[] growth = {Blocks.BRAIN_CORAL_FAN.defaultBlockState(), Blocks.FIRE_CORAL.defaultBlockState(), Blocks.BUBBLE_CORAL_FAN.defaultBlockState()};
		for (int[] p : new int[][] {{5, 7, 18}, {13, 7, 16}}) {
			for (int x = p[0] - 2; x <= p[0] + 2; x++) {
				for (int z = p[1] - 2; z <= p[1] + 2; z++) {
					if (dist(x, z, p[0], p[1]) * 10 > p[2]) {
						continue;
					}
					set(level, bb, STONE, x, BASE - 1, z);
					int n = noise(x + boundingBox.minX(), z + boundingBox.minZ());
					if (n < 14) {
						soaked(level, bb, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, 1 + n % 4), x, BASE, z);
					} else if (n < 30) {
						soaked(level, bb, growth[n % growth.length], x, BASE, z);
					} else {
						set(level, bb, WATER, x, BASE, z);
					}
				}
			}
		}
		// The deep pool, in a basin of its own so it holds whatever lies under it.
		for (int x = POOL_X - 4; x <= POOL_X + 4; x++) {
			for (int z = POOL_Z - 4; z <= POOL_Z + 4; z++) {
				double d = dist(x, z, POOL_X, POOL_Z);
				if (d > 3.3) {
					continue;
				}
				fill(level, bb, x, BASE - 4, z, x, BASE, z, STONE);
				if (d <= 2.2) {
					fill(level, bb, x, BASE - 3, z, x, BASE, z, WATER);
					if (noise(x + boundingBox.minX(), z + boundingBox.minZ()) < 25 && (x != POOL_X || z != POOL_Z)) {
						set(level, bb, Blocks.KELP_PLANT.defaultBlockState(), x, BASE - 3, z);
						set(level, bb, Blocks.KELP.defaultBlockState(), x, BASE - 2, z);
					}
				}
			}
		}
		set(level, bb, Blocks.SEA_LANTERN.defaultBlockState(), POOL_X, BASE - 4, POOL_Z);
	}

	/** The hermit's corner: a lectern with their notes, a barrel, a cold campfire; their chest up on the ledge. */
	private void hermit(WorldGenLevel level, BoundingBox bb) {
		lectern(level, bb, 5, BASE + 1, 13, Direction.EAST, "Hermit's Notes", "book.wildercord.water_tidepool_grotto", 2);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 4, BASE + 1, 12);
		set(level, bb, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false), 6, BASE + 1, 15);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 5, BASE + 1, 14);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 12, BASE + 1, 5);
		fill(level, bb, 13, BASE + 1, 11, 14, BASE + 2, 12, STONE);
		set(level, bb, STONE, 12, BASE + 1, 11);
		chest(level, bb, 14, BASE + 3, 12, WaterSites.GROTTO_LOOT, Direction.WEST, 308);
	}
}
