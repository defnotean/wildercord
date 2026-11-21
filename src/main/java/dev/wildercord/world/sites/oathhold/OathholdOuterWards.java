package dev.wildercord.world.sites.oathhold;

import dev.wildercord.aura.world.AuraWorld;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Populates the entire 1,920x1,920 Outer Metropolitan Wards with 1:1 boulevards, raised sidewalks,
 * ring-canal promenades & moored cargo barges, patrol-ring supply depots, and 4 color-distinct
 * regional urban quadrants matching the Master Map and street concept renders.
 */
public final class OathholdOuterWards {
	private OathholdOuterWards() {}

	private static final BlockState ANDESITE = OathholdCanvas.POLISHED_ANDESITE;
	private static final BlockState STONE = OathholdCanvas.STONE_BRICKS;
	private static final BlockState COBBLESTONE = OathholdCanvas.COBBLESTONE;

	public static void build(OathholdCanvas c) {
		buildCausewaysAndArches(c);
		buildRingCanalPromenade(c);
		buildRingCanalBridges(c);
		buildOuterPatrolRing(c);
		buildUrbanGrid(c);
	}

	private static void buildCausewaysAndArches(OathholdCanvas c) {
		int outer = OathholdOrigin.OUTER_WALL_R - 8;
		int inner = OathholdOrigin.INNER_MOAT_OUTER + 1;

		// North & South Imperial Causeways with Raised Sidewalks
		if (c.intersects(-15, -outer, 15, outer)) {
			for (int z = Math.max(-outer, c.clipMinLz()); z <= Math.min(outer, c.clipMaxLz()); z++) {
				if (Math.abs(z) < inner) continue;
				if (Math.abs(z) >= 534 && Math.abs(z) <= 674) continue;
				paintRoadStripZ(c, z);
				if (Math.abs(z) > 280 && Math.floorMod(Math.abs(z), 144) == 108) {
					buildTriumphalArchZ(c, z);
				}
			}
		}

		// West & East Imperial Causeways with Raised Sidewalks
		if (c.intersects(-outer, -15, outer, 15)) {
			for (int x = Math.max(-outer, c.clipMinLx()); x <= Math.min(outer, c.clipMaxLx()); x++) {
				if (Math.abs(x) < inner) continue;
				if (x >= 500 || (x <= -542 && x >= -682)) continue;
				paintRoadStripX(c, x);
				if (Math.abs(x) > 280 && Math.floorMod(Math.abs(x), 144) == 108) {
					buildTriumphalArchX(c, x);
				}
			}
		}
	}

	private static void paintRoadStripZ(OathholdCanvas c, int z) {
		int az = Math.abs(z);
		boolean crosswalk = az <= 262 || az >= 942 || Math.floorMod(az, 48) <= 2;
		boolean nearArch = az > 270 && (Math.floorMod(az, 144) >= 100 && Math.floorMod(az, 144) <= 116);

		// Sunken cobblestone & deepslate carriageway with central andesite runner
		c.fill(-5, 0, z, 5, 0, z, COBBLESTONE);
		c.set(-5, 0, z, OathholdCanvas.DEEPSLATE_BRICKS);
		c.set(5, 0, z, OathholdCanvas.DEEPSLATE_BRICKS);
		c.fill(-1, 0, z, 1, 0, z, ANDESITE);

		if (crosswalk) {
			c.fillPaving(-11, 0, z, -6, z);
			c.fillPaving(6, 0, z, 11, z);
		} else {
			// Raised 1-block sidewalks with inward stone-brick stair curbs
			c.fill(-11, 1, z, -7, 1, z, ((z & 3) == 0) ? STONE : ANDESITE);
			c.fill(7, 1, z, 11, 1, z, ((z & 3) == 0) ? STONE : ANDESITE);
			c.set(-6, 1, z, OathholdCanvas.stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false));
			c.set(6, 1, z, OathholdCanvas.stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false));

			if (Math.floorMod(z, 16) == 8 && !nearArch) {
				OathholdStreetscape.ornateStreetLamp(c, -8, 2, z, false);
				OathholdStreetscape.ornateStreetLamp(c, 8, 2, z, false);
			}
			if (Math.floorMod(z, 32) == 24 && !nearArch && az < 928) {
				boolean cherry = Math.floorMod(z, 64) == 24;
				OathholdStreetscape.treePlanter(c, -9, 1, z, cherry);
				OathholdStreetscape.treePlanter(c, 9, 1, z, !cherry);
			}
			if (Math.floorMod(z, 32) == 14 && !nearArch) {
				OathholdStreetscape.parkBench(c, -9, 2, z, Direction.EAST);
				OathholdStreetscape.parkBench(c, 9, 2, z, Direction.WEST);
				OathholdStreetscape.flowerBox(c, -11, 2, z + 3, Direction.EAST);
				OathholdStreetscape.flowerBox(c, 11, 2, z + 3, Direction.WEST);
			}
			if (Math.floorMod(az, 64) == 48 && !nearArch) {
				int wx = (Math.floorMod(az, 128) < 64) ? -3 : 3;
				OathholdStreetscape.merchantWagon(c, wx, 1, z, true, z);
			}
		}
	}

	private static void paintRoadStripX(OathholdCanvas c, int x) {
		int ax = Math.abs(x);
		boolean crosswalk = ax <= 262 || ax >= 942 || Math.floorMod(ax, 48) <= 2;
		boolean nearArch = ax > 270 && (Math.floorMod(ax, 144) >= 100 && Math.floorMod(ax, 144) <= 116);

		c.fill(x, 0, -5, x, 0, 5, COBBLESTONE);
		c.set(x, 0, -5, OathholdCanvas.DEEPSLATE_BRICKS);
		c.set(x, 0, 5, OathholdCanvas.DEEPSLATE_BRICKS);
		c.fill(x, 0, -1, x, 0, 1, ANDESITE);

		if (crosswalk) {
			c.fillPaving(x, 0, -11, x, -6);
			c.fillPaving(x, 0, 6, x, 11);
		} else {
			c.fill(x, 1, -11, x, 1, -7, ((x & 3) == 0) ? STONE : ANDESITE);
			c.fill(x, 1, 7, x, 1, 11, ((x & 3) == 0) ? STONE : ANDESITE);
			c.set(x, 1, -6, OathholdCanvas.stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
			c.set(x, 1, 6, OathholdCanvas.stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, false));

			if (Math.floorMod(x, 16) == 8 && !nearArch) {
				OathholdStreetscape.ornateStreetLamp(c, x, 2, -8, true);
				OathholdStreetscape.ornateStreetLamp(c, x, 2, 8, true);
			}
			if (Math.floorMod(x, 32) == 24 && !nearArch && ax < 928) {
				boolean cherry = Math.floorMod(x, 64) == 24;
				OathholdStreetscape.treePlanter(c, x, 1, -9, cherry);
				OathholdStreetscape.treePlanter(c, x, 1, 9, !cherry);
			}
			if (Math.floorMod(x, 32) == 14 && !nearArch) {
				OathholdStreetscape.parkBench(c, x, 2, -9, Direction.SOUTH);
				OathholdStreetscape.parkBench(c, x, 2, 9, Direction.NORTH);
				OathholdStreetscape.flowerBox(c, x + 3, 2, -11, Direction.SOUTH);
				OathholdStreetscape.flowerBox(c, x + 3, 2, 11, Direction.NORTH);
			}
			if (Math.floorMod(ax, 64) == 48 && !nearArch) {
				int wz = (Math.floorMod(ax, 128) < 64) ? -3 : 3;
				OathholdStreetscape.merchantWagon(c, x, 1, wz, false, x);
			}
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
		c.spawnMob(-4, 1, cz, AuraWorld.OATHBOUND_HALBERDIER, cz > 0 ? 0.0F : 180.0F, true);
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
		c.spawnMob(cx, 1, 4, AuraWorld.OATHBOUND_BASTION, cx > 0 ? 90.0F : -90.0F, true);
	}

	private static void buildRingCanalPromenade(OathholdCanvas c) {
		if (!c.intersects(-645, -645, 645, 645)) return;
		// North & South Ring Canal banks at z = +-621 and +-639
		for (int z : new int[]{-639, -621, 621, 639}) {
			if (c.clipMinLz() <= z && c.clipMaxLz() >= z) {
				for (int x = Math.max(-638, c.clipMinLx()); x <= Math.min(638, c.clipMaxLx()); x++) {
					if (Math.abs(x) <= 72 || x >= 500 || (x <= -540 && Math.abs(z) <= 72)) continue;
					if (Math.floorMod(Math.abs(x), 8) == 0) {
						boolean lamp = Math.floorMod(Math.abs(x), 24) == 0;
						c.set(x, 1, z, OathholdCanvas.TUFF_BRICKS);
						if (lamp) {
							OathholdStreetscape.ornateStreetLamp(c, x, 1, z, false);
						}
					} else {
						c.set(x, 1, z, OathholdStreetscape.horizontalChain(false));
					}
					if (Math.floorMod(Math.abs(x), 96) == 48 && z > 0) {
						OathholdStreetscape.canalBarge(c, x, -1, 630, false);
						OathholdStreetscape.canalBarge(c, x, -1, -630, false);
					}
				}
			}
		}
		// West & East Ring Canal banks at x = +-621 and +-639
		for (int x : new int[]{-639, -621, 621, 639}) {
			if (c.clipMinLx() <= x && c.clipMaxLx() >= x) {
				for (int z = Math.max(-638, c.clipMinLz()); z <= Math.min(638, c.clipMaxLz()); z++) {
					if (Math.abs(z) <= 72 || (x > 0 && Math.abs(z) <= 264)) continue;
					if (Math.floorMod(Math.abs(z), 8) == 0) {
						boolean lamp = Math.floorMod(Math.abs(z), 24) == 0;
						c.set(x, 1, z, OathholdCanvas.TUFF_BRICKS);
						if (lamp) {
							OathholdStreetscape.ornateStreetLamp(c, x, 1, z, true);
						}
					} else {
						c.set(x, 1, z, OathholdStreetscape.horizontalChain(true));
					}
					if (Math.floorMod(Math.abs(z), 96) == 48 && x > 0) {
						OathholdStreetscape.canalBarge(c, -630, -1, z, true);
						if (Math.abs(z) >= 280) {
							OathholdStreetscape.canalBarge(c, 630, -1, z, true);
						}
					}
				}
			}
		}
	}

	private static void buildRingCanalBridges(OathholdCanvas c) {
		for (int pos : new int[]{-360, -144, 144, 360}) {
			for (int cz : new int[]{630, -630}) {
				if (c.intersects(pos - 6, cz - 12, pos + 6, cz + 12)) {
					bridgeOverZCanal(c, pos, cz);
				}
			}
			if (c.intersects(-642, pos - 6, -618, pos + 6)) {
				bridgeOverXCanal(c, -630, pos);
			}
			if (Math.abs(pos) >= 280 && c.intersects(618, pos - 6, 642, pos + 6)) {
				bridgeOverXCanal(c, 630, pos);
			}
		}
	}

	private static void bridgeOverZCanal(OathholdCanvas c, int cx, int cz) {
		for (int z = cz - 10; z <= cz + 10; z++) {
			int dz = Math.abs(z - cz);
			int y = Math.max(0, 3 - (dz * dz) / 36);
			c.fill(cx - 4, y, z, cx + 4, y, z, ANDESITE);
			c.fill(cx - 4, y - 1, z, cx + 4, y - 1, z, STONE);
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
			c.fill(x, y, cz - 4, x, y, cz + 4, ANDESITE);
			c.fill(x, y - 1, cz - 4, x, y - 1, cz + 4, STONE);
			c.set(x, y + 1, cz - 4, OathholdCanvas.TUFF_BRICKS);
			c.set(x, y + 1, cz + 4, OathholdCanvas.TUFF_BRICKS);
			c.clear(x, y + 1, cz - 3, x, y + 6, cz + 3);
			if (dx == 0 || dx == 8) {
				c.set(x, y + 2, cz - 4, OathholdCanvas.LANTERN);
				c.set(x, y + 2, cz + 4, OathholdCanvas.LANTERN);
			}
		}
	}

	private static void buildOuterPatrolRing(OathholdCanvas c) {
		if (!c.intersects(-942, -942, 942, 942)) return;
		for (int p = -912; p <= 912; p += 32) {
			if (Math.abs(p) <= 24) continue;
			for (int s : new int[]{-934, 934}) {
				OathholdStreetscape.ornateStreetLamp(c, p, 1, s, false);
				OathholdStreetscape.ornateStreetLamp(c, s, 1, p, true);
				if (Math.floorMod(p, 96) == 0) {
					OathholdStreetscape.cargoCluster(c, p + 4, 1, s, p);
					if (!(s > 0 && Math.abs(p) <= 240)) {
						OathholdStreetscape.merchantWagon(c, s - (s > 0 ? 4 : -4), 1, p + 6, true, p + s);
					}
				}
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
				if (!c.intersects(x0 - 3, z0 - 3, x1 + 3, z1 + 3)) continue;
				if (overlapsMajorFeature(x0, z0, x1, z1)) continue;

				buildUrbanBlock(c, x0, z0, x1, z1);
			}
		}
	}

	public static boolean overlapsMajorFeature(int x0, int z0, int x1, int z1) {
		return OathholdOrigin.overlapsMajorFeature(x0, z0, x1, z1);
	}

	private static void buildUrbanBlock(OathholdCanvas c, int x0, int z0, int x1, int z1) {
		int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
		int h = OathholdCanvas.hash(x0, 77, z0);

		// Raise the entire 44x44 urban island pad to y = 1 with stair-curbed perimeter sidewalks
		c.fill(x0 - 2, 1, z0 - 2, x1 + 2, 1, z1 + 2, STONE);
		c.fill(x0 - 1, 1, z0 - 1, x1 + 1, 1, z1 + 1, ANDESITE);
		c.fill(x0, 1, z0, x1, 1, z1, STONE);
		for (int x = Math.max(x0 - 1, c.clipMinLx()); x <= Math.min(x1 + 1, c.clipMaxLx()); x++) {
			c.set(x, 1, z0 - 2, OathholdCanvas.stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, false));
			c.set(x, 1, z1 + 2, OathholdCanvas.stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
		}
		for (int z = Math.max(z0 - 1, c.clipMinLz()); z <= Math.min(z1 + 1, c.clipMaxLz()); z++) {
			c.set(x0 - 2, 1, z, OathholdCanvas.stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false));
			c.set(x1 + 2, 1, z, OathholdCanvas.stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false));
		}

		// Sidewalk streetlamps & corner props
		OathholdStreetscape.ornateStreetLamp(c, x0 - 1, 2, z0 - 1, false);
		OathholdStreetscape.ornateStreetLamp(c, x1 + 1, 2, z1 + 1, false);

		boolean east = cx >= 0;
		boolean north = cz < 0;
		OathholdBuildings.Theme theme = east
			? (north ? OathholdBuildings.Theme.SCHOLARS_NE : OathholdBuildings.Theme.MARITIME_SE)
			: (north ? OathholdBuildings.Theme.IRONWORKS_NW : OathholdBuildings.Theme.MERCHANTS_SW);

		if (h < 18) {
			buildQuadrantSignaturePlaza(c, x0, z0, x1, z1, cx, cz, theme, h);
			return;
		}

		Block stair = switch (theme) {
			case IRONWORKS_NW -> (h % 3 == 0) ? Blocks.BRICK_STAIRS : Blocks.DEEPSLATE_TILE_STAIRS;
			case SCHOLARS_NE -> (h % 3 == 0) ? Blocks.CUT_COPPER_STAIRS.waxed().weathered() : Blocks.DEEPSLATE_TILE_STAIRS;
			case MARITIME_SE -> (h % 2 == 0) ? Blocks.CUT_COPPER_STAIRS.waxed().weathered() : Blocks.SPRUCE_STAIRS;
			default -> (h % 4 == 0) ? Blocks.CUT_COPPER_STAIRS.waxed().weathered() : Blocks.SPRUCE_STAIRS;
		};
		Block slab = stair == Blocks.CUT_COPPER_STAIRS.waxed().weathered() ? Blocks.CUT_COPPER_SLAB.waxed().weathered()
			: stair == Blocks.BRICK_STAIRS ? Blocks.BRICK_SLAB
			: stair == Blocks.DEEPSLATE_TILE_STAIRS ? Blocks.DEEPSLATE_TILE_SLAB
			: Blocks.SPRUCE_SLAB;

		int[][] lots = {
			{x0, z0, x0 + 16, z0 + 16},
			{x1 - 16, z0, x1, z0 + 16},
			{x0, z1 - 16, x0 + 16, z1},
			{x1 - 16, z1 - 16, x1, z1}
		};
		for (int i = 0; i < lots.length; i++) {
			int[] l = lots[i];
			int sh = OathholdCanvas.hash(l[0], i + 3, l[1]);
			int stories = 2 + (sh % 3);
			boolean spawnVillager = (i == 0 && (h & 1) == 0);
			OathholdBuildings.build(c, 1, l[0], l[1], l[2], l[3], stories, stair, slab,
				theme == OathholdBuildings.Theme.MARITIME_SE && sh < 25, theme, spawnVillager, sh);
		}

		// Central block courtyard furnishing + sidewalk seating/crates
		OathholdStreetscape.treePlanter(c, cx, 1, cz, north);
		OathholdStreetscape.parkBench(c, cx, 2, z1 + 1, Direction.SOUTH);
		OathholdStreetscape.cargoCluster(c, cx - 2, 2, z0 - 1, h);
		if (theme == OathholdBuildings.Theme.MERCHANTS_SW || theme == OathholdBuildings.Theme.MARITIME_SE) {
			OathholdStreetscape.patioTable(c, x1 + 1, 2, cz,
				Blocks.CARPET.red().defaultBlockState(), Blocks.WOOL.white().defaultBlockState());
		}
	}

	private static void buildQuadrantSignaturePlaza(OathholdCanvas c, int x0, int z0, int x1, int z1,
			int cx, int cz, OathholdBuildings.Theme theme, int h) {
		switch (theme) {
			case IRONWORKS_NW -> {
				c.fill(cx - 4, 2, cz - 4, cx + 4, 6, cz + 4, OathholdCanvas.BRICKS);
				c.set(cx, 2, cz, OathholdCanvas.OCHRE_LIGHT);
				c.set(cx, 2, cz + 5, Blocks.ANVIL.defaultBlockState());
				c.set(cx + 2, 2, cz + 5, Blocks.SMITHING_TABLE.defaultBlockState());
				c.fill(x0 + 4, 2, z0 + 4, x0 + 6, 3, z0 + 5, Blocks.RAW_IRON_BLOCK.defaultBlockState());
				OathholdStreetscape.merchantWagon(c, x1 - 5, 2, cz, true, h);
			}
			case SCHOLARS_NE -> {
				OathholdStreetscape.hedgePlanter(c, x0 + 2, 1, z0 + 2, x1 - 2, z1 - 2);
				OathholdStreetscape.treePlanter(c, x0 + 9, 1, z0 + 9, true);
				OathholdStreetscape.treePlanter(c, x1 - 9, 1, z1 - 9, true);
				OathholdStreetscape.treePlanter(c, x0 + 9, 1, z1 - 9, false);
				OathholdStreetscape.treePlanter(c, x1 - 9, 1, z0 + 9, false);
				c.fill(cx - 2, 2, cz - 2, cx + 2, 3, cz + 2, OathholdCanvas.TUFF_BRICKS);
				c.fill(cx - 1, 2, cz - 1, cx + 1, 2, cz + 1, OathholdCanvas.WATER);
				OathholdStreetscape.parkBench(c, cx, 2, cz + 5, Direction.NORTH);
			}
			case MERCHANTS_SW -> {
				c.fill(cx - 2, 2, cz - 2, cx + 2, 3, cz + 2, OathholdCanvas.TUFF_BRICKS);
				c.fill(cx - 1, 2, cz - 1, cx + 1, 2, cz + 1, OathholdCanvas.WATER);
				OathholdStreetscape.marketStall(c, x0 + 8, 2, z0 + 8, Blocks.WOOL.red().defaultBlockState(), Blocks.WOOL.white().defaultBlockState(), h);
				OathholdStreetscape.marketStall(c, x1 - 8, 2, z0 + 8, Blocks.WOOL.blue().defaultBlockState(), Blocks.WOOL.white().defaultBlockState(), h + 1);
				OathholdStreetscape.marketStall(c, x0 + 8, 2, z1 - 8, Blocks.WOOL.orange().defaultBlockState(), Blocks.WOOL.yellow().defaultBlockState(), h + 2);
				OathholdStreetscape.marketStall(c, x1 - 8, 2, z1 - 8, Blocks.WOOL.lime().defaultBlockState(), Blocks.WOOL.white().defaultBlockState(), h + 3);
			}
			case MARITIME_SE -> {
				c.fill(x0 + 4, 2, z0 + 4, x0 + 9, 3, z0 + 6, OathholdCanvas.SPRUCE_LOG);
				OathholdStreetscape.cargoCluster(c, x1 - 6, 2, z0 + 6, h);
				OathholdStreetscape.patioTable(c, cx - 4, 2, cz + 5, Blocks.CARPET.cyan().defaultBlockState(), Blocks.WOOL.white().defaultBlockState());
				OathholdStreetscape.patioTable(c, cx + 4, 2, cz + 5, Blocks.CARPET.green().defaultBlockState(), Blocks.WOOL.white().defaultBlockState());
			}
			default -> {}
		}
	}
}
