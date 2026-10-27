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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * A sea cave in a craggy headland on a stony shore, its rock grown over with moss and a cairn or two on top, sea stacks
 * standing off it. Inside it is a high vault lit from a hole in its crown and a sea window to one side, dripstone hanging
 * from its roof, its floor cut with tidepools: two shallow ones bright with pickles and coral, and a deep one where
 * something drowned still keeps watch. A hermit lived here: their notes are on the lectern, their chest up on the ledge.
 * In its own frame the floor is at y {@link #BASE}, the vault centred on x 13, z 13, the way in at z 0.
 */
public class TidepoolGrottoPiece extends WaterPiece {
	public static final int WIDTH = 27, HEIGHT = 20, DEPTH = 27;
	public static final int BASE = 5;
	static final int CX = 13, CZ = 13;
	/** The deep pool's centre. */
	static final int POOL_X = 13, POOL_Z = 16;
	/** The vault's radius at the floor; it rises a little taller than it is wide. */
	static final double VAULT = 8.3, STRETCH = 0.85;

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
			if (surface(c, piece.getWorldPos(CX, 0, 2)) - 1 > ground + 1) {
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

	/** The highest open block of the vault over a column {@code d} from its centre, or below the floor where it's rock. */
	static int ceiling(double d) {
		return d >= VAULT ? BASE : BASE + (int) (Math.sqrt(VAULT * VAULT - d * d) / STRETCH);
	}

	/** How far out the headland reaches over a column, ragged by its noise. */
	private static double reach(int n) {
		return 11 + (n - 50) / 30.0;
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb,
			ChunkPos chunkPos, BlockPos reference) {
		int ox = boundingBox.minX(), oz = boundingBox.minZ();
		int[][] tops = new int[WIDTH][DEPTH];
		for (int x = 0; x < WIDTH; x++) {
			for (int z = 0; z < DEPTH; z++) {
				double d = dist(x, z, CX, CZ);
				int n = noise(x + ox, z + oz);
				double r = reach(n);
				if (d > r) {
					continue;
				}
				// Never thinner than two blocks over the vault.
				int top = Math.min(HEIGHT - 2, Math.max(ceiling(d) + 2, BASE + (int) (Math.sqrt(r * r - d * d) * 1.05) + n % 3 - 1));
				tops[x][z] = top;
				post(level, bb, rock(n), x, z, BASE);
				for (int y = BASE + 1; y <= top; y++) {
					set(level, bb, rock(noise(x + ox, y, z + oz)), x, y, z);
				}
				// Moss and turf over the headland's crown, a little grass on it.
				if (top > BASE + 1) {
					set(level, bb, n < 35 ? Blocks.MOSS_BLOCK.defaultBlockState() : n < 50 ? Blocks.GRASS_BLOCK.defaultBlockState() : rock(n), x, top, z);
					if (n < 50 && noise(x * 3 + ox, z * 5 + oz) < 30) {
						set(level, bb, Blocks.SHORT_GRASS.defaultBlockState(), x, top + 1, z);
					}
				}
				// The vault inside.
				for (int y = BASE + 1; y < HEIGHT; y++) {
					int dy = y - BASE;
					if (d * d + dy * STRETCH * dy * STRETCH <= VAULT * VAULT) {
						set(level, bb, AIR, x, y, z);
					}
				}
				// The hole in the crown that lights it.
				if (d <= 1.5) {
					fill(level, bb, x, BASE + 6, z, x, HEIGHT - 1, z, AIR);
				}
			}
		}
		// The way in from the shore, and a gravel track to it.
		clear(level, bb, CX - 1, BASE + 1, 0, CX + 1, BASE + 3, 6);
		clear(level, bb, CX, BASE + 4, 0, CX, BASE + 4, 5);
		for (int x = CX - 1; x <= CX + 1; x++) {
			for (int z = 0; z <= 5; z++) {
				post(level, bb, Blocks.GRAVEL.defaultBlockState(), x, z, BASE);
			}
		}
		stacks(level, bb);
		seaWindow(level, bb);
		portal(level, bb);
		oculus(level, bb, tops);
		dripstone(level, bb);
		pools(level, bb);
		hermit(level, bb);
		cairns(level, bb, tops);
		guard(level, bb, EntityTypes.DROWNED, POOL_X, BASE - 2, POOL_Z, List.of(Runes.BOLT, Runes.TIDEHOOK), false);
		sentry(level, bb, EntityTypes.DROWNED, 10, BASE + 1, 14);
	}

	/** Sea stacks standing off the headland, tapering to points, weed and pickles round their feet. */
	private void stacks(WorldGenLevel level, BoundingBox bb) {
		int ox = boundingBox.minX(), oz = boundingBox.minZ();
		for (int[] s : new int[][] {{3, 4, 8}, {24, 3, 10}, {24, 23, 7}, {2, 21, 6}}) {
			for (int x = s[0] - 2; x <= s[0] + 2; x++) {
				for (int z = s[1] - 2; z <= s[1] + 2; z++) {
					if (x < 0 || z < 0 || x >= WIDTH || z >= DEPTH) {
						continue;
					}
					double d = dist(x, z, s[0], s[1]);
					for (int y = BASE - 1; y <= BASE + s[2]; y++) {
						double t = (y - BASE + 1) / (double) (s[2] + 1);
						if (d <= 2.2 - 1.9 * t) {
							if (y == BASE - 1) {
								post(level, bb, rock(noise(x + ox, z + oz)), x, z, y);
							}
							set(level, bb, rock(noise(x + ox, y, z + oz)), x, y, z);
						}
					}
					if (d > 1.5 && d <= 2.5 && noise(x + ox + 7, z + oz) < 25 && getBlock(level, x, BASE + 1, z, bb).isAir()
							&& getBlock(level, x, BASE, z, bb).isSolid()) {
						set(level, bb, Blocks.MOSS_CARPET.defaultBlockState(), x, BASE + 1, z);
					}
				}
			}
		}
	}

	/** A window the sea wore through the east wall, its sill kerbed, a lantern hung in it. */
	private void seaWindow(WorldGenLevel level, BoundingBox bb) {
		for (int x = CX + 6; x < WIDTH; x++) {
			clear(level, bb, x, BASE + 3, CZ - 1, x, BASE + 5, CZ + 1);
			clear(level, bb, x, BASE + 6, CZ, x, BASE + 6, CZ);
			set(level, bb, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), x, BASE + 2, CZ - 1);
			set(level, bb, Blocks.STONE_BRICK_SLAB.defaultBlockState(), x, BASE + 2, CZ);
			set(level, bb, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), x, BASE + 2, CZ + 1);
		}
		hangingLantern(level, bb, CX + 7, BASE + 6, CZ, 1, false);
	}

	/** A dressed sea arch over the way in, buttressed and corniced, on a paved forecourt with lamp posts. */
	private void portal(WorldGenLevel level, BoundingBox bb) {
		Palette dressed = Palette.of(Blocks.STONE_BRICKS, 5, Blocks.MOSSY_STONE_BRICKS, 3, Blocks.CRACKED_STONE_BRICKS, 2);
		for (int x = CX - 4; x <= CX + 4; x++) {
			for (int z = 0; z <= 1; z++) {
				post(level, bb, Math.abs(x - CX) <= 1 ? Blocks.GRAVEL.defaultBlockState() : noise(x, z) < 40 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
					: Blocks.STONE_BRICKS.defaultBlockState(), x, z, BASE);
				clear(level, bb, x, BASE + 1, z, x, BASE + 2, z);
			}
		}
		for (int x : new int[] {CX - 2, CX + 2}) {
			fill(level, bb, x, BASE + 1, 2, x, BASE + 6, 2, dressed);
			fill(level, bb, x, BASE + 1, 3, x, BASE + 5, 3, dressed);
			post(level, bb, Blocks.COBBLESTONE.defaultBlockState(), x, 2, BASE);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false), x, BASE + 1, 1);
		}
		for (int x : new int[] {CX - 3, CX + 3}) {
			fill(level, bb, x, BASE + 1, 2, x, BASE + 4, 2, dressed);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, x < CX ? Direction.EAST : Direction.WEST, false), x, BASE + 5, 2);
		}
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), CX - 1, BASE + 4, 2);
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), CX + 1, BASE + 4, 2);
		set(level, bb, Blocks.STONE_BRICKS.defaultBlockState(), CX - 1, BASE + 5, 2);
		set(level, bb, Blocks.STONE_BRICKS.defaultBlockState(), CX + 1, BASE + 5, 2);
		set(level, bb, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), CX, BASE + 5, 2);
		fill(level, bb, CX - 2, BASE + 7, 2, CX + 2, BASE + 7, 2, Blocks.STONE_BRICK_SLAB.defaultBlockState());
		fill(level, bb, CX - 1, BASE + 6, 2, CX + 1, BASE + 6, 2, Blocks.STONE_BRICKS.defaultBlockState());
		set(level, bb, Blocks.STONE_BRICK_WALL.defaultBlockState(), CX, BASE + 8, 2);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), CX, BASE + 9, 2);
		hangingLantern(level, bb, CX, BASE + 4, 3, 0, false);
		for (int x : new int[] {CX - 4, CX + 4}) {
			post(level, bb, Blocks.COBBLESTONE.defaultBlockState(), x, 0, BASE);
			set(level, bb, Blocks.STONE_BRICKS.defaultBlockState(), x, BASE + 1, 0);
			set(level, bb, Blocks.STONE_BRICK_WALL.defaultBlockState(), x, BASE + 2, 0);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), x, BASE + 3, 0);
		}
	}

	/** A ring of dressed stone round the hole in the crown, and lanterns hung from the vault. */
	private void oculus(WorldGenLevel level, BoundingBox bb, int[][] tops) {
		for (int x = CX - 3; x <= CX + 3; x++) {
			for (int z = CZ - 3; z <= CZ + 3; z++) {
				double d = dist(x, z, CX, CZ);
				if (d > 1.5 && d <= 2.7 && tops[x][z] > BASE) {
					set(level, bb, Blocks.STONE_BRICKS.defaultBlockState(), x, tops[x][z], z);
					set(level, bb, Blocks.STONE_BRICK_SLAB.defaultBlockState(), x, tops[x][z] + 1, z);
				}
			}
		}
		for (int[] l : new int[][] {{CX - 4, CZ}, {CX + 4, CZ - 2}, {CX, CZ - 4}, {CX - 3, CZ + 4}}) {
			hangingLantern(level, bb, l[0], ceiling(dist(l[0], l[1], CX, CZ)), l[1], 2, false);
		}
		hangingLantern(level, bb, POOL_X, ceiling(dist(POOL_X, POOL_Z, CX, CZ)), POOL_Z, 3, false);
	}

	/** Dripstone hanging from the vault, clear of the crown's hole and the lamps. */
	private void dripstone(WorldGenLevel level, BoundingBox bb) {
		int ox = boundingBox.minX(), oz = boundingBox.minZ();
		BlockState down = Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(SpeleothemBlock.TIP_DIRECTION, Direction.DOWN);
		for (int x = CX - 7; x <= CX + 7; x++) {
			for (int z = CZ - 7; z <= CZ + 7; z++) {
				double d = dist(x, z, CX, CZ);
				int n = noise(x * 3 + ox, z * 5 + oz);
				if (d < 2.5 || d > 7 || n >= 14) {
					continue;
				}
				int y = ceiling(d);
				if (!getBlock(level, x, y + 1, z, bb).isSolid() || !getBlock(level, x, y, z, bb).isAir()) {
					continue;
				}
				if (n < 6 && getBlock(level, x, y - 1, z, bb).isAir()) {
					set(level, bb, down.setValue(SpeleothemBlock.THICKNESS, SpeleothemThickness.FRUSTUM), x, y, z);
					set(level, bb, down.setValue(SpeleothemBlock.THICKNESS, SpeleothemThickness.TIP), x, y - 1, z);
				} else {
					set(level, bb, down.setValue(SpeleothemBlock.THICKNESS, SpeleothemThickness.TIP), x, y, z);
				}
			}
		}
	}

	private void pools(WorldGenLevel level, BoundingBox bb) {
		BlockState[] growth = {Blocks.BRAIN_CORAL_FAN.defaultBlockState(), Blocks.FIRE_CORAL.defaultBlockState(), Blocks.BUBBLE_CORAL_FAN.defaultBlockState()};
		for (int[] p : new int[][] {{9, 11, 18}, {17, 11, 16}}) {
			for (int x = p[0] - 2; x <= p[0] + 2; x++) {
				for (int z = p[1] - 2; z <= p[1] + 2; z++) {
					double r = dist(x, z, p[0], p[1]) * 10;
					if (r > p[2]) {
						// A worn kerb of dressed stone round each shallow pool.
						if (r <= p[2] + 10) {
							set(level, bb, noise(x, z) < 50 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState(), x, BASE, z);
						}
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
				set(level, bb, noise(x, z) < 60 ? Blocks.POLISHED_ANDESITE.defaultBlockState() : Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), x, BASE, z);
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
		lectern(level, bb, 9, BASE + 1, 17, Direction.EAST, "Hermit's Notes", "book.wildercord.water_tidepool_grotto", 2);
		set(level, bb, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 8, BASE + 1, 16);
		set(level, bb, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false), 10, BASE + 1, 19);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 9, BASE + 1, 18);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 16, BASE + 1, 9);
		// The hermit's rug, a shelf of books and a pot on the barrel.
		fill(level, bb, 10, BASE + 1, 17, 11, BASE + 1, 18, Blocks.CARPET.brown().defaultBlockState());
		set(level, bb, Blocks.BOOKSHELF.defaultBlockState(), 8, BASE + 1, 17);
		set(level, bb, Blocks.FLOWER_POT.defaultBlockState(), 8, BASE + 2, 16);
		// The ledge, faced in dressed stone, a stair up to it and a lantern by the chest.
		fill(level, bb, 17, BASE + 1, 15, 18, BASE + 2, 16, Palette.of(Blocks.MOSSY_STONE_BRICKS, 3, Blocks.STONE_BRICKS, 2, Blocks.CRACKED_STONE_BRICKS, 1));
		set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false), 16, BASE + 1, 15);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 17, BASE + 3, 15);
		chest(level, bb, 18, BASE + 3, 16, WaterSites.GROTTO_LOOT, Direction.WEST, 308);
	}

	/** Cairns of piled stone on the headland's crown, a lamp on the tallest for boats coming in. */
	private void cairns(WorldGenLevel level, BoundingBox bb, int[][] tops) {
		for (int[] c : new int[][] {{CX + 5, CZ - 3, 1}, {CX - 4, CZ + 5, 0}}) {
			int y = tops[c[0]][c[1]];
			if (y <= BASE + 2) {
				continue;
			}
			set(level, bb, Blocks.COBBLESTONE.defaultBlockState(), c[0], y + 1, c[1]);
			set(level, bb, Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState(), c[0], y + 2, c[1]);
			if (c[2] == 1) {
				set(level, bb, Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState(), c[0], y + 3, c[1]);
				set(level, bb, Blocks.LANTERN.defaultBlockState(), c[0], y + 4, c[1]);
			} else {
				set(level, bb, Blocks.COBBLESTONE_SLAB.defaultBlockState(), c[0], y + 3, c[1]);
			}
		}
	}
}
