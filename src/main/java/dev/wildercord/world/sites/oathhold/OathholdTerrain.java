package dev.wildercord.world.sites.oathhold;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Column-wise terrain grading, foundation pouring, canal/moat/harbor excavation, and Acropolis terracing.
 * Operates exclusively within the chunk's {@code clip} bounds in O(256) columns per chunk.
 */
public final class OathholdTerrain {
	private OathholdTerrain() {}

	private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
	private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
	private static final BlockState STONE = Blocks.STONE.defaultBlockState();
	private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();

	public static void prepareChunk(OathholdCanvas c) {
		int minX = Math.max(c.clipMinLx(), -OathholdOrigin.HALF_SIZE + 1);
		int maxX = Math.min(c.clipMaxLx(), OathholdOrigin.HALF_SIZE - 1);
		int minZ = Math.max(c.clipMinLz(), -OathholdOrigin.HALF_SIZE + 1);
		int maxZ = Math.min(c.clipMaxLz(), OathholdOrigin.HALF_SIZE - 1);
		if (minX > maxX || minZ > maxZ) return;

		int minLocalY = Math.max(-24, c.clip().minY() - c.baseY());
		int maxLocalY = c.clip().maxY() - c.baseY();

		for (int lx = minX; lx <= maxX; lx++) {
			for (int lz = minZ; lz <= maxZ; lz++) {
				OathholdOrigin.Zone zone = OathholdOrigin.classify(lx, lz);
				if (zone == OathholdOrigin.Zone.OUTSIDE) continue;

				int wx = c.wx(lx);
				int wz = c.wz(lz);
				int natWorldY = c.level().getHeight(Heightmap.Types.OCEAN_FLOOR_WG, wx, wz);
				int surfWorldY = c.level().getHeight(Heightmap.Types.WORLD_SURFACE_WG, wx, wz);
				int natLocal = natWorldY - c.baseY();
				int surfLocal = Math.max(natLocal, surfWorldY - c.baseY());

				int ax = Math.abs(lx);
				int az = Math.abs(lz);
				int cheb = Math.max(ax, az);

				if (zone == OathholdOrigin.Zone.APPROACH) {
					float t = (cheb - OathholdOrigin.OUTER_MOAT_OUTER) / (float) (OathholdOrigin.HALF_SIZE - OathholdOrigin.OUTER_MOAT_OUTER);
					t = Math.max(0.0F, Math.min(1.0F, t));
					int targetY = Math.round((1.0F - t) * -1.0F + t * natLocal);
					gradeSlopeColumn(c, lx, lz, targetY, surfLocal, minLocalY, maxLocalY, ax <= 11 || az <= 11);
					continue;
				}

				if (zone == OathholdOrigin.Zone.OUTER_MOAT) {
					carveWaterColumn(c, lx, lz, -6, -1, Math.max(surfLocal + 4, 24), minLocalY, maxLocalY);
					continue;
				}
				if (zone == OathholdOrigin.Zone.INNER_MOAT) {
					carveWaterColumn(c, lx, lz, -5, -1, Math.max(surfLocal + 4, 24), minLocalY, maxLocalY);
					continue;
				}
				if (zone == OathholdOrigin.Zone.OUTER_CANAL) {
					// Stone embankments on edges of canal, water in inner channel
					boolean edge = cheb == 622 || cheb == 638;
					if (edge) {
						gradeDeckColumn(c, lx, lz, 1, natLocal, surfLocal, minLocalY, maxLocalY, OathholdCanvas.STONE_BRICKS);
					} else {
						carveWaterColumn(c, lx, lz, -5, -1, Math.max(surfLocal + 4, 24), minLocalY, maxLocalY);
					}
					continue;
				}
				if (zone == OathholdOrigin.Zone.INNER_CANAL && ax <= 9) {
					carveWaterColumn(c, lx, lz, -4, -1, Math.max(surfLocal + 4, 24), minLocalY, maxLocalY);
					continue;
				}
				if (zone == OathholdOrigin.Zone.OUTER_HARBOR && isHarborBasinWater(lx, lz)) {
					carveWaterColumn(c, lx, lz, -8, -1, Math.max(surfLocal + 4, 28), minLocalY, maxLocalY);
					continue;
				}

				boolean isAcropolis = ax <= OathholdOrigin.ACROPOLIS_HALF_X
					&& lz >= OathholdOrigin.ACROPOLIS_MIN_Z - 6
					&& lz <= OathholdOrigin.ACROPOLIS_MAX_Z + 2;
				int deckY = isAcropolis ? OathholdOrigin.ACROPOLIS_Y : 0;

				BlockState top = isAcropolis ? acropolisCap(lx, lz) : lowerCap(lx, lz, zone);
				gradeDeckColumn(c, lx, lz, deckY, natLocal, surfLocal, minLocalY, maxLocalY, top);
			}
		}
	}

	private static boolean isHarborBasinWater(int lx, int lz) {
		// Seawall & promenade along lx 500..548; north & south stone quays at |lz| >= 224; 3 finger piers protruding east
		if (lx < 556 || lx > 948 || Math.abs(lz) > 224) return false;
		if (Math.abs(lz) >= 210) return false;
		// Stone cargo finger piers extending from seawall at z ~ -120, 0, +120
		if (lx <= 710 && (Math.abs(lz) <= 6 || Math.abs(lz - 120) <= 6 || Math.abs(lz + 120) <= 6)) return false;
		return true;
	}

	private static void carveWaterColumn(OathholdCanvas c, int lx, int lz, int bedY, int waterTopY, int surfLocal,
			int minLocalY, int maxLocalY) {
		int startY = Math.max(minLocalY, Math.min(bedY - 2, surfLocal - 2));
		for (int y = startY; y < bedY; y++) c.set(lx, y, lz, STONE);
		int h = OathholdCanvas.hash(lx, bedY, lz);
		c.set(lx, bedY, lz, h < 45 ? OathholdCanvas.MOSSY_STONE_BRICKS : h < 75 ? GRAVEL : STONE);
		for (int y = bedY + 1; y <= waterTopY; y++) c.set(lx, y, lz, OathholdCanvas.WATER);
		int clearTop = Math.min(maxLocalY, Math.max(waterTopY + 18, surfLocal + 14));
		for (int y = waterTopY + 1; y <= clearTop; y++) c.set(lx, y, lz, OathholdCanvas.AIR);
	}

	private static void gradeSlopeColumn(OathholdCanvas c, int lx, int lz, int targetY, int surfLocal,
			int minLocalY, int maxLocalY, boolean road) {
		int fillStart = Math.max(minLocalY, Math.min(targetY - 6, surfLocal - 3));
		for (int y = fillStart; y < targetY; y++) {
			c.set(lx, y, lz, y >= targetY - 2 ? DIRT : STONE);
		}
		if (road) {
			c.set(lx, targetY, lz, OathholdCanvas.hash(lx, targetY, lz) < 55 ? OathholdCanvas.STONE_BRICKS : OathholdCanvas.COBBLESTONE);
		} else {
			c.set(lx, targetY, lz, GRASS);
		}
		int clearTop = Math.min(maxLocalY, Math.max(targetY + 14, surfLocal + 12));
		for (int y = targetY + 1; y <= clearTop; y++) c.set(lx, y, lz, OathholdCanvas.AIR);
	}

	private static void gradeDeckColumn(OathholdCanvas c, int lx, int lz, int deckY, int natLocal, int surfLocal,
			int minLocalY, int maxLocalY, BlockState cap) {
		int fillStart = Math.max(minLocalY, Math.min(deckY - 12, natLocal - 2));
		for (int y = fillStart; y < deckY; y++) {
			c.set(lx, y, lz, y >= deckY - 3 ? OathholdCanvas.COBBLESTONE : STONE);
		}
		c.set(lx, deckY, lz, cap);
		int clearTop = Math.min(maxLocalY, Math.max(deckY + 34, surfLocal + 14));
		for (int y = deckY + 1; y <= clearTop; y++) c.set(lx, y, lz, OathholdCanvas.AIR);
	}

	private static BlockState acropolisCap(int lx, int lz) {
		int h = OathholdCanvas.hash(lx, OathholdOrigin.ACROPOLIS_Y, lz);
		if (Math.floorMod(lx, 8) == 0 || Math.floorMod(lz, 8) == 0) return OathholdCanvas.POLISHED_ANDESITE;
		return h < 60 ? OathholdCanvas.STONE_BRICKS : h < 85 ? OathholdCanvas.TUFF_BRICKS : OathholdCanvas.POLISHED_ANDESITE;
	}

	private static BlockState lowerCap(int lx, int lz, OathholdOrigin.Zone zone) {
		int h = OathholdCanvas.hash(lx, 0, lz);
		if (zone == OathholdOrigin.Zone.OUTER_ABBEY && (Math.abs(lx) <= 22 && Math.abs(lz + 604) <= 22)) {
			return GRASS;
		}
		if (Math.floorMod(lx, 6) == 0 || Math.floorMod(lz, 6) == 0) return OathholdCanvas.POLISHED_ANDESITE;
		return h < 45 ? OathholdCanvas.STONE_BRICKS : h < 72 ? OathholdCanvas.COBBLESTONE : h < 90 ? OathholdCanvas.TUFF_BRICKS : OathholdCanvas.ANDESITE;
	}
}
