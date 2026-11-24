package dev.wildercord.world.sites.oathhold;

import dev.wildercord.compat.McWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministic coordinate contract and zoning map for Oathhold, the 2,048x2,048 Sovereign Megacity.
 *
 * <p>Both the macro anchor structure ({@code wildercord:oathhold}) and every tiling sector
 * ({@code wildercord:oathhold_sector}) derive the exact same {@link Origin} from the macro
 * {@link RandomSpreadStructurePlacement} grid, guaranteeing seamless alignment across all 256 sectors.</p>
 */
public final class OathholdOrigin {
	private OathholdOrigin() {}

	public static final int HALF_SIZE = 1024;
	public static final int CITY_SIZE = HALF_SIZE * 2;
	public static final int OUTER_WALL_R = 960;
	public static final int OUTER_MOAT_OUTER = 988;
	public static final int INNER_WALL_R = 224;
	public static final int INNER_MOAT_OUTER = 248;

	public static final int ACROPOLIS_Y = 16;
	public static final int ACROPOLIS_MIN_Z = -212;
	public static final int ACROPOLIS_MAX_Z = -44;
	public static final int ACROPOLIS_HALF_X = 184;

	public static final int MACRO_SPACING = 256;
	public static final int MACRO_SEPARATION = 160;
	public static final int CENTER_SALT = 20261130;

	public static final int SECTOR_SPACING = 8;
	public static final int SECTOR_SEPARATION = 7;
	public static final int SECTOR_SALT = 20261131;

	private static final class PlacementHolder {
		static final RandomSpreadStructurePlacement CENTER =
			new RandomSpreadStructurePlacement(MACRO_SPACING, MACRO_SEPARATION, RandomSpreadType.LINEAR, CENTER_SALT);
	}

	public static final Set<ResourceKey<Biome>> CENTER_BIOMES = Set.of(
		Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW,
		Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST,
		Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DARK_FOREST,
		Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.CHERRY_GROVE
	);

	/** World-space anchor at local {@code (0, 0, 0)}. */
	public record Origin(int originX, int baseY, int originZ) {}

	/** Canonical architectural district classification in local {@code (lx, lz)} coordinates. */
	public enum Zone {
		OUTSIDE,
		APPROACH,
		OUTER_MOAT,
		OUTER_WALL,
		OUTER_HARBOR,
		OUTER_COLOSSEUM,
		OUTER_EXCHANGE,
		OUTER_ABBEY,
		OUTER_CAUSEWAY,
		OUTER_RING_ROAD,
		OUTER_CANAL,
		OUTER_URBAN,
		INNER_MOAT,
		INNER_WALL,
		ACROPOLIS_WALL,
		ACROPOLIS_GREAT_HALL,
		ACROPOLIS_BARRACKS,
		ACROPOLIS_LIBRARY,
		ACROPOLIS_CATHEDRAL,
		ACROPOLIS_PLAZA,
		INNER_CANAL,
		FOUNTAIN_PLAZA,
		MARKET_HALL,
		FORGE_HALL,
		GUILD_HALL,
		GRAND_INN,
		LOWER_URBAN
	}

	/** Returns the deterministic macro city-center chunk inside the region containing {@code (sampleChunkX, sampleChunkZ)}. */
	public static ChunkPos macroCenterChunk(long seed, int sampleChunkX, int sampleChunkZ) {
		return PlacementHolder.CENTER.getPotentialStructureChunk(seed, sampleChunkX, sampleChunkZ);
	}

	/** Resolves whether a candidate macro center chunk hosts Oathhold and computes its locked ground deck Y. */
	public static Optional<Origin> resolve(Structure.GenerationContext ctx, int centerCx, int centerCz) {
		int originX = centerCx * 16 + 8;
		int originZ = centerCz * 16 + 8;
		int sea = ctx.chunkGenerator().getSeaLevel();
		int sampleY = Math.max(sea, 64);

		Holder<Biome> centerBiome = McWorldgen.getNoiseBiome(ctx, originX, sampleY, originZ);
		if (centerBiome.value().getBaseTemperature() < 0.35F) {
			return Optional.empty();
		}
		boolean centerValid = centerBiome.unwrapKey().map(CENTER_BIOMES::contains).orElse(false);
		int validBiomes = 0;
		for (int[] off : new int[][]{{-192, 0}, {192, 0}, {0, -192}, {0, 192}}) {
			Holder<Biome> b = McWorldgen.getNoiseBiome(ctx, originX + off[0], sampleY, originZ + off[1]);
			if (b.value().getBaseTemperature() < 0.35F) {
				return Optional.empty();
			}
			if (b.unwrapKey().map(CENTER_BIOMES::contains).orElse(false)) {
				validBiomes++;
			}
		}
		if (!centerValid && validBiomes < 2) {
			return Optional.empty();
		}

		int[] samples = new int[5];
		int dry = 0;
		int[][] pts = {{0, 0}, {-96, 0}, {96, 0}, {0, -96}, {0, 96}};
		for (int i = 0; i < pts.length; i++) {
			int px = originX + pts[i][0];
			int pz = originZ + pts[i][1];
			int surf = ctx.chunkGenerator().getFirstOccupiedHeight(px, pz, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
			int floor = ctx.chunkGenerator().getFirstOccupiedHeight(px, pz, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
			if (surf == floor && (sea <= 0 || floor >= sea)) {
				dry++;
			}
			samples[i] = Math.max(floor, sea + 2);
		}
		if (dry < 2) {
			return Optional.empty();
		}

		Arrays.sort(samples);
		int baseY = sea > 0
			? Mth.clamp(samples[2], sea + 5, sea + 22)
			: Math.max(samples[2], ctx.heightAccessor().getMinBuildHeight() + 24);
		return Optional.of(new Origin(originX, baseY, originZ));
	}

	/** Finds the unique Oathhold city (if any) whose 2,048x2,048 footprint overlaps the 128x128 tile centered at {@code (sectorAnchorCx, sectorAnchorCz)}. */
	public static Optional<Origin> findOwningCity(Structure.GenerationContext ctx, int sectorAnchorCx, int sectorAnchorCz) {
		int minX = (sectorAnchorCx - 4) * 16;
		int maxX = (sectorAnchorCx + 4) * 16 - 1;
		int minZ = (sectorAnchorCz - 4) * 16;
		int maxZ = (sectorAnchorCz + 4) * 16 - 1;

		int baseRx = Math.floorDiv(sectorAnchorCx, MACRO_SPACING);
		int baseRz = Math.floorDiv(sectorAnchorCz, MACRO_SPACING);
		Set<ChunkPos> checked = new HashSet<>();

		for (int drx = -1; drx <= 1; drx++) {
			for (int drz = -1; drz <= 1; drz++) {
				int sampleCx = (baseRx + drx) * MACRO_SPACING;
				int sampleCz = (baseRz + drz) * MACRO_SPACING;
				ChunkPos center = macroCenterChunk(ctx.seed(), sampleCx, sampleCz);
				if (!checked.add(center)) continue;
				int ox = center.x * 16 + 8;
				int oz = center.z * 16 + 8;
				if (ox + HALF_SIZE - 1 < minX || ox - HALF_SIZE > maxX) continue;
				if (oz + HALF_SIZE - 1 < minZ || oz - HALF_SIZE > maxZ) continue;
				Optional<Origin> origin = resolve(ctx, center.x, center.z);
				if (origin.isPresent()) {
					return origin;
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * Returns {@code true} if block {@code (blockX, blockZ)} lies within Oathhold's
	 * {@code 2,048x2,048} city footprint expanded by {@code paddingBlocks} on each side.
	 */
	public static boolean isNearAnyCity(Structure.GenerationContext ctx, int blockX, int blockZ, int paddingBlocks) {
		int half = HALF_SIZE + Math.max(0, paddingBlocks);
		int cx = Math.floorDiv(blockX, 16);
		int cz = Math.floorDiv(blockZ, 16);
		int baseRx = Math.floorDiv(cx, MACRO_SPACING);
		int baseRz = Math.floorDiv(cz, MACRO_SPACING);
		Set<ChunkPos> checked = new HashSet<>();
		for (int drx = -1; drx <= 1; drx++) {
			for (int drz = -1; drz <= 1; drz++) {
				int sampleCx = (baseRx + drx) * MACRO_SPACING;
				int sampleCz = (baseRz + drz) * MACRO_SPACING;
				ChunkPos center = macroCenterChunk(ctx.seed(), sampleCx, sampleCz);
				if (!checked.add(center)) continue;
				int ox = center.x * 16 + 8;
				int oz = center.z * 16 + 8;
				if (Math.abs(blockX - ox) <= half && Math.abs(blockZ - oz) <= half
						&& resolve(ctx, center.x, center.z).isPresent()) {
					return true;
				}
			}
		}
		return false;
	}

	/** Computes city-local X relative to {@link Origin}. */
	public static int localX(Origin origin, int worldX) {
		return worldX - origin.originX();
	}

	/** Computes city-local Z relative to {@link Origin}. */
	public static int localZ(Origin origin, int worldZ) {
		return worldZ - origin.originZ();
	}

	/** Classifies any city-local {@code (lx, lz)} column into its authoritative architectural zone. */
	public static Zone classify(int lx, int lz) {
		int ax = Math.abs(lx);
		int az = Math.abs(lz);
		int cheb = Math.max(ax, az);

		if (cheb >= HALF_SIZE) return Zone.OUTSIDE;
		if (cheb > OUTER_MOAT_OUTER) return Zone.APPROACH;
		if (cheb > OUTER_WALL_R) return ax <= 10 || az <= 10 ? Zone.OUTER_CAUSEWAY : Zone.OUTER_MOAT;
		if (cheb >= OUTER_WALL_R - 8) return Zone.OUTER_WALL;

		// Inner Citadel Moat and Inner Sovereign Curtain Wall
		if (cheb > INNER_WALL_R && cheb <= INNER_MOAT_OUTER) {
			return ax <= 9 || az <= 9 ? Zone.OUTER_CAUSEWAY : Zone.INNER_MOAT;
		}
		if (cheb >= INNER_WALL_R - 6 && cheb <= INNER_WALL_R) {
			return Zone.INNER_WALL;
		}

		// Inside Inner Citadel (r < INNER_WALL_R - 6)
		if (cheb < INNER_WALL_R - 6) {
			if (ax <= ACROPOLIS_HALF_X && lz >= ACROPOLIS_MIN_Z - 6 && lz <= ACROPOLIS_MAX_Z + 4) {
				if (lz >= ACROPOLIS_MAX_Z - 4 && lz <= ACROPOLIS_MAX_Z + 4) return Zone.ACROPOLIS_WALL;
				if (ax <= 34 && lz >= -204 && lz <= -92) return Zone.ACROPOLIS_GREAT_HALL;
				if (lx >= -174 && lx <= -52 && lz >= -188 && lz <= -62) return Zone.ACROPOLIS_BARRACKS;
				if (lx >= 46 && lx <= 114 && lz >= -174 && lz <= -68) return Zone.ACROPOLIS_LIBRARY;
				if (lx >= 122 && lx <= 178 && lz >= -198 && lz <= -84) return Zone.ACROPOLIS_CATHEDRAL;
				return Zone.ACROPOLIS_PLAZA;
			}
			if (ax <= 24 && lz >= 150 && lz <= 212) return Zone.FOUNTAIN_PLAZA;
			if (ax <= 11 && lz >= -40 && lz <= 149) return Zone.INNER_CANAL;
			if (lx >= -120 && lx <= -40 && lz >= 46 && lz <= 148) return Zone.MARKET_HALL;
			if (lx >= -186 && lx <= -138 && lz >= 54 && lz <= 92) return Zone.FORGE_HALL;
			if (lx >= 116 && lx <= 156 && lz >= 44 && lz <= 82) return Zone.GUILD_HALL;
			if (lx >= 36 && lx <= 74 && lz >= 96 && lz <= 130) return Zone.GRAND_INN;
			return Zone.LOWER_URBAN;
		}

		// Outer Metropolitan Wards (248 < cheb < 952)
		if (lx >= 500 && lx <= 951 && lz >= -260 && lz <= 260) return Zone.OUTER_HARBOR;
		if (lx >= -680 && lx <= -544 && az <= 68) return Zone.OUTER_COLOSSEUM;
		if (ax <= 68 && lz >= 536 && lz <= 672) return Zone.OUTER_EXCHANGE;
		if (ax <= 68 && lz >= -672 && lz <= -536) return Zone.OUTER_ABBEY;
		if (ax <= 11 || az <= 11) return Zone.OUTER_CAUSEWAY;
		if (cheb >= 622 && cheb <= 638) return Zone.OUTER_CANAL;
		if ((cheb >= 570 && cheb <= 588) || (cheb >= 924 && cheb <= 944)) return Zone.OUTER_RING_ROAD;
		return Zone.OUTER_URBAN;
	}

	/** Helper for checking whether a local X/Z lies inside a landmark's axis-aligned rectangle. */
	public static boolean inRect(int lx, int lz, int x0, int z0, int x1, int z1) {
		return lx >= x0 && lx <= x1 && lz >= z0 && lz <= z1;
	}

	/** Center {@link BlockPos} of the anchor piece at {@code Origin}. */
	public static BlockPos anchorPos(Origin origin) {
		return new BlockPos(origin.originX(), origin.baseY(), origin.originZ());
	}

	/** Pure deterministic coordinate hash [0..99] without initializing Minecraft registries. */
	public static int hash(int x, int y, int z) {
		int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
		h ^= h >>> 13;
		h *= 0x5bd1e995;
		h ^= h >>> 15;
		return Math.floorMod(h, 100);
	}

	/** Pure bounding check for Lower Inner City landmarks (kept registry-free for dry-run tests). */
	public static boolean overlapsInnerLandmark(int x0, int z0, int x1, int z1) {
		if (x0 <= 26 && x1 >= -26 && z1 >= -44) return true;
		if (z1 <= -36) return true;
		if (x1 >= -124 && x0 <= -36 && z1 >= 42 && z0 <= 152) return true;
		if (x1 >= -190 && x0 <= -134 && z1 >= 50 && z0 <= 96) return true;
		if (x1 >= -216 && x0 <= -120 && z1 >= 10 && z0 <= 32) return true;
		if (x1 >= 112 && x0 <= 160 && z1 >= 40 && z0 <= 86) return true;
		if (x1 >= 32 && x0 <= 78 && z1 >= 92 && z0 <= 152) return true;
		return false;
	}

	/** Pure bounding check for Outer Ward major landmarks &amp; roads (kept registry-free for dry-run tests). */
	public static boolean overlapsMajorFeature(int x0, int z0, int x1, int z1) {
		int maxC = Math.max(Math.max(Math.abs(x0), Math.abs(x1)), Math.max(Math.abs(z0), Math.abs(z1)));
		int minXAbs = (x0 <= 0 && x1 >= 0) ? 0 : Math.min(Math.abs(x0), Math.abs(x1));
		int minZAbs = (z0 <= 0 && z1 >= 0) ? 0 : Math.min(Math.abs(z0), Math.abs(z1));

		if (maxC >= 916) return true;
		if (minXAbs <= 254 && minZAbs <= 254) return true;
		if (minXAbs <= 18 || minZAbs <= 18) return true;
		if ((Math.abs(x0) <= 644 && Math.abs(x1) >= 616) || (Math.abs(z0) <= 644 && Math.abs(z1) >= 616)) {
			if (Math.max(minXAbs, minZAbs) >= 566) return true;
		}
		if (x1 >= 496 && z1 >= -264 && z0 <= 264) return true;
		if (x0 <= -540 && x1 >= -684 && z1 >= -72 && z0 <= 72) return true;
		if (x1 >= -72 && x0 <= 72 && z1 >= 532 && z0 <= 676) return true;
		if (x1 >= -72 && x0 <= 72 && z1 >= -676 && z0 <= -532) return true;
		return false;
	}
}
