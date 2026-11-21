package dev.wildercord.world.sites.oathhold;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Populates the entire 1,920x1,920 Outer Metropolitan Wards:
 * 22-block Imperial Causeways with Triumphal Arches, arched Ring-Canal bridges,
 * and a deterministic 48x48 urban-block generator placing multi-story medieval districts across all four quadrants.
 */
public final class OathholdOuterWards {
	private OathholdOuterWards() {}

	public static void build(OathholdCanvas c) {
		buildCausewaysAndArches(c);
		buildRingCanalBridges(c);
		buildUrbanGrid(c);
	}

	private static void buildCausewaysAndArches(OathholdCanvas c) {
		int outer = OathholdOrigin.OUTER_WALL_R - 8;
		int inner = OathholdOrigin.INNER_MOAT_OUTER + 1;

		// North & South Imperial Causeways
		if (c.intersects(-14, -outer, 14, outer)) {
			for (int z = Math.max(-outer, c.clipMinLz()); z <= Math.min(outer, c.clipMaxLz()); z++) {
				if (Math.abs(z) < inner) continue;
				if (Math.abs(z) >= 534 && Math.abs(z) <= 674) continue; // King's Square & Abbey Plaza occupy axis
				paintRoadStripZ(c, z);
				if (Math.abs(z) > 280 && Math.floorMod(Math.abs(z), 144) == 108) {
					buildTriumphalArchZ(c, z);
				}
			}
		}

		// West & East Imperial Causeways
		if (c.intersects(-outer, -14, outer, 14)) {
			for (int x = Math.max(-outer, c.clipMinLx()); x <= Math.min(outer, c.clipMaxLx()); x++) {
				if (Math.abs(x) < inner) continue;
				if (x >= 500 || (x <= -542 && x >= -682)) continue; // Harbor & Colosseum occupy axis
				paintRoadStripX(c, x);
				if (Math.abs(x) > 280 && Math.floorMod(Math.abs(x), 144) == 108) {
					buildTriumphalArchX(c, x);
				}
			}
		}
	}

	private static void paintRoadStripZ(OathholdCanvas c, int z) {
		c.fillPaving(-9, 0, z, 9, z);
		c.fill(-3, 0, z, 3, 0, z, OathholdCanvas.POLISHED_ANDESITE);
		c.set(-10, 1, z, OathholdCanvas.TUFF_BRICKS);
		c.set(10, 1, z, OathholdCanvas.TUFF_BRICKS);
		if (Math.floorMod(z, 24) == 0) {
			c.streetLamp(-10, 1, z);
			c.streetLamp(10, 1, z);
		}
	}

	private static void paintRoadStripX(OathholdCanvas c, int x) {
		c.fillPaving(x, 0, -9, x, 9);
		c.fill(x, 0, -3, x, 0, 3, OathholdCanvas.POLISHED_ANDESITE);
		c.set(x, 1, -10, OathholdCanvas.TUFF_BRICKS);
		c.set(x, 1, 10, OathholdCanvas.TUFF_BRICKS);
		if (Math.floorMod(x, 24) == 0) {
			c.streetLamp(x, 1, -10);
			c.streetLamp(x, 1, 10);
		}
	}

	private static void buildTriumphalArchZ(OathholdCanvas c, int cz) {
		if (!c.intersects(-15, cz - 5, 15, cz + 5)) return;
		c.fillWeatheredStone(-13, 1, cz - 3, 13, 18, cz + 3);
		c.gothicArchZ(0, 1, cz - 3, cz + 3, 8, 9, 5, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		c.fill(-13, 19, cz - 3, 13, 19, cz + 3, OathholdCanvas.GOLD);
		c.wallBannerStrip(-11, 16, cz + 4, 7, Direction.SOUTH);
		c.wallBannerStrip(11, 16, cz + 4, 7, Direction.SOUTH);
		c.wallBannerStrip(-11, 16, cz - 4, 7, Direction.NORTH);
		c.wallBannerStrip(11, 16, cz - 4, 7, Direction.NORTH);
	}

	private static void buildTriumphalArchX(OathholdCanvas c, int cx) {
		if (!c.intersects(cx - 5, -15, cx + 5, 15)) return;
		c.fillWeatheredStone(cx - 3, 1, -13, cx + 3, 18, 13);
		c.gothicArchX(cx - 3, cx + 3, 1, 0, 8, 9, 5, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		c.fill(cx - 3, 19, -13, cx + 3, 19, 13, OathholdCanvas.GOLD);
		c.wallBannerStrip(cx + 4, 16, -11, 7, Direction.EAST);
		c.wallBannerStrip(cx + 4, 16, 11, 7, Direction.EAST);
		c.wallBannerStrip(cx - 4, 16, -11, 7, Direction.WEST);
		c.wallBannerStrip(cx - 4, 16, 11, 7, Direction.WEST);
	}

	private static void buildRingCanalBridges(OathholdCanvas c) {
		// Diagonal & mid-block bridges crossing Outer Ring Canal (cheb in [622..638])
		for (int pos : new int[]{-360, -144, 144, 360}) {
			// South & North Ring Canal bridges at x = pos, z = +-630
			for (int cz : new int[]{630, -630}) {
				if (c.intersects(pos - 6, cz - 12, pos + 6, cz + 12)) {
					bridgeOverZCanal(c, pos, cz);
				}
			}
			// West Ring Canal bridges at x = -630, z = pos
			if (c.intersects(-642, pos - 6, -618, pos + 6)) {
				bridgeOverXCanal(c, -630, pos);
			}
			// East Ring Canal bridges (avoid harbor water z in -220..220)
			if (Math.abs(pos) >= 280 && c.intersects(618, pos - 6, 642, pos + 6)) {
				bridgeOverXCanal(c, 630, pos);
			}
		}
	}

	private static void bridgeOverZCanal(OathholdCanvas c, int cx, int cz) {
		for (int z = cz - 10; z <= cz + 10; z++) {
			int dz = Math.abs(z - cz);
			int y = Math.max(0, 3 - (dz * dz) / 36);
			c.fill(cx - 4, y, z, cx + 4, y, z, OathholdCanvas.POLISHED_ANDESITE);
			c.fill(cx - 4, y - 1, z, cx + 4, y - 1, z, OathholdCanvas.STONE_BRICKS);
			c.set(cx - 4, y + 1, z, OathholdCanvas.TUFF_BRICKS);
			c.set(cx + 4, y + 1, z, OathholdCanvas.TUFF_BRICKS);
			c.clear(cx - 3, y + 1, z, cx + 3, y + 6, z);
			if (dz == 0 || dz == 8) {
				c.set(cx - 4, y + 2, z, OathholdCanvas.LANTERN);
				c.set(cx + 4, y + 2, z, OathholdCanvas.LANTERN);
			}
		}
	}

	private static void bridgeOverXCanal(OathholdCanvas c, int cx, int cz) {
		for (int x = cx - 10; x <= cx + 10; x++) {
			int dx = Math.abs(x - cx);
			int y = Math.max(0, 3 - (dx * dx) / 36);
			c.fill(x, y, cz - 4, x, y, cz + 4, OathholdCanvas.POLISHED_ANDESITE);
			c.fill(x, y - 1, cz - 4, x, y - 1, cz + 4, OathholdCanvas.STONE_BRICKS);
			c.set(x, y + 1, cz - 4, OathholdCanvas.TUFF_BRICKS);
			c.set(x, y + 1, cz + 4, OathholdCanvas.TUFF_BRICKS);
			c.clear(x, y + 1, cz - 3, x, y + 6, cz + 3);
			if (dx == 0 || dx == 8) {
				c.set(x, y + 2, cz - 4, OathholdCanvas.LANTERN);
				c.set(x, y + 2, cz + 4, OathholdCanvas.LANTERN);
			}
		}
	}

	private static void buildUrbanGrid(OathholdCanvas c) {
		int startGridX = Math.max(-912, Math.floorDiv(c.clipMinLx() - 52, 48) * 48);
		int endGridX = Math.min(864, Math.floorDiv(c.clipMaxLx() + 4, 48) * 48);
		int startGridZ = Math.max(-912, Math.floorDiv(c.clipMinLz() - 52, 48) * 48);
		int endGridZ = Math.min(864, Math.floorDiv(c.clipMaxLz() + 4, 48) * 48);

		for (int gx = startGridX; gx <= endGridX; gx += 48) {
			for (int gz = startGridZ; gz <= endGridZ; gz += 48) {
				int x0 = gx + 4, x1 = gx + 44, z0 = gz + 4, z1 = gz + 44;
				if (!c.intersects(x0 - 2, z0 - 2, x1 + 2, z1 + 2)) continue;
				if (overlapsMajorFeature(x0, z0, x1, z1)) continue;

				buildUrbanBlock(c, x0, z0, x1, z1);
			}
		}
	}

	private static boolean overlapsMajorFeature(int x0, int z0, int x1, int z1) {
		int maxC = Math.max(Math.max(Math.abs(x0), Math.abs(x1)), Math.max(Math.abs(z0), Math.abs(z1)));
		int minXAbs = (x0 <= 0 && x1 >= 0) ? 0 : Math.min(Math.abs(x0), Math.abs(x1));
		int minZAbs = (z0 <= 0 && z1 >= 0) ? 0 : Math.min(Math.abs(z0), Math.abs(z1));

		if (maxC >= 916) return true; // Inside Outer Patrol Ring Road / Wall buffer
		if (minXAbs <= 254 && minZAbs <= 254) return true; // Inside Inner Citadel Moat
		if (minXAbs <= 18 || minZAbs <= 18) return true; // Along 4 Imperial Causeways
		if ((Math.abs(x0) <= 644 && Math.abs(x1) >= 616) || (Math.abs(z0) <= 644 && Math.abs(z1) >= 616)) {
			// Skip ring canal & ring boulevard footprints cleanly
			if (Math.max(minXAbs, minZAbs) >= 566) return true;
		}
		// Outer Landmarks
		if (x1 >= 496 && z1 >= -264 && z0 <= 264) return true; // Harbor
		if (x0 <= -540 && x1 >= -684 && z1 >= -72 && z0 <= 72) return true; // Colosseum
		if (x1 >= -72 && x0 <= 72 && z1 >= 532 && z0 <= 676) return true; // King's Square
		if (x1 >= -72 && x0 <= 72 && z1 >= -676 && z0 <= -532) return true; // Abbey
		return false;
	}

	private static void buildUrbanBlock(OathholdCanvas c, int x0, int z0, int x1, int z1) {
		int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
		int h = OathholdCanvas.hash(x0, 77, z0);

		c.streetLamp(x0 - 1, 1, z0 - 1);
		c.streetLamp(x1 + 1, 1, z1 + 1);

		if (h < 14) {
			// District Park Square with Cherry/Oak Trees & Public Well
			c.fill(x0 + 4, 0, z0 + 4, x1 - 4, 0, z1 - 4, Blocks.GRASS_BLOCK.defaultBlockState());
			c.fill(cx - 2, 1, cz - 2, cx + 2, 2, cz + 2, OathholdCanvas.TUFF_BRICKS);
			c.fill(cx - 1, 1, cz - 1, cx + 1, 1, cz + 1, OathholdCanvas.WATER);
			c.tree(x0 + 10, 0, z0 + 10, cx > 0 || h < 7);
			c.tree(x1 - 10, 0, z1 - 10, cz < 0 || h < 7);
			return;
		}

		// Quadrant roof theme (SW spruce/brick, NW dark slate/copper, SE maritime copper, NE scholar copper/spruce)
		boolean east = cx >= 0;
		boolean north = cz < 0;
		Block stair = (east && !north) || (!east && north && h % 3 == 0)
			? Blocks.CUT_COPPER_STAIRS.waxed().weathered()
			: (north ? Blocks.DEEPSLATE_TILE_STAIRS : Blocks.SPRUCE_STAIRS);
		Block slab = stair == Blocks.CUT_COPPER_STAIRS.waxed().weathered()
			? Blocks.CUT_COPPER_SLAB.waxed().weathered()
			: (north ? Blocks.DEEPSLATE_TILE_SLAB : Blocks.SPRUCE_SLAB);

		// Subdivide 40x40 usable pad into 4 corner townhouses (16x16 each) framing a central 8x8 courtyard
		int[][] lots = {
			{x0, z0, x0 + 16, z0 + 16},
			{x1 - 16, z0, x1, z0 + 16},
			{x0, z1 - 16, x0 + 16, z1},
			{x1 - 16, z1 - 16, x1, z1}
		};
		for (int i = 0; i < lots.length; i++) {
			int[] l = lots[i];
			int sh = OathholdCanvas.hash(l[0], i + 3, l[1]);
			int stories = 2 + (sh % 3); // 2, 3, or 4 stories
			OathholdInnerCore.buildMedievalBuilding(c, l[0], l[1], l[2], l[3], stories, stair, slab, sh < 12, sh);
		}
		// Central alley tree & lamp inside block courtyard
		c.tree(cx, 0, cz, north);
		c.streetLamp(cx + 3, 1, cz);
	}
}
