package dev.wildercord.world.sites.oathhold;

import dev.wildercord.aura.world.AuraWorld;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Four colossal Outer Metropolitan Landmarks at 600-950 blocks from the center:
 * Great Oathbound Tournament Colosseum & Tent Concourse (West), Bustling Imperial Harbor Quay,
 * Lighthouse & 6 Galleons (East), Grand King's Square & Exchange (South), and High Sovereign Abbey (North).
 */
public final class OathholdOuterLandmarks {
	private OathholdOuterLandmarks() {}

	public static void build(OathholdCanvas c) {
		buildColosseum(c);
		buildImperialHarbor(c);
		buildKingsExchangeSquare(c);
		buildSovereignAbbey(c);
	}

	private static void buildColosseum(OathholdCanvas c) {
		int cx = -612, cz = 0, rOut = 60, rIn = 34;
		if (!c.intersects(cx - rOut - 14, cz - rOut - 14, cx + rOut + 14, cz + rOut + 14)) return;

		int minX = Math.max(cx - rOut, c.clipMinLx()), maxX = Math.min(cx + rOut, c.clipMaxLx());
		int minZ = Math.max(cz - rOut, c.clipMinLz()), maxZ = Math.min(cz + rOut, c.clipMaxLz());

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				int dx = x - cx, dz = z - cz;
				double dist = Math.sqrt(dx * dx + dz * dz);
				if (dist > rOut + 0.5) continue;

				if (dist < rIn) {
					c.set(x, 0, z, dist < 5.0 ? OathholdCanvas.POLISHED_ANDESITE : Blocks.SAND.defaultBlockState());
					if (dist < 5.0 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
						c.set(x, 0, z, OathholdCanvas.GOLD);
					}
				} else {
					boolean fighterGate = (Math.abs(dx) <= 4 || Math.abs(dz) <= 4);
					double frac = (dist - rIn) / (double) (rOut - rIn);
					int tierTop = Math.min(26, 3 + (int) Math.round(frac * 23));
					c.fillWeatheredStone(x, 1, z, x, tierTop, z);

					if (dist >= rOut - 2.2) {
						boolean colPillar = Math.floorMod(Math.round(Math.atan2(dz, dx) * rOut), 6) <= 1;
						if (!colPillar && !fighterGate) {
							for (int y = 3; y <= 23; y += 7) {
								c.clear(x, y, z, x, y + 4, z);
							}
						}
						if (Math.floorMod(x + z, 4) < 2) {
							c.set(x, 27, z, OathholdCanvas.STONE_BRICKS);
						}
					} else if (fighterGate) {
						c.clear(x, 1, z, x, 7, z);
					} else {
						c.set(x, tierTop + 1, z, OathholdCanvas.stair(Blocks.STONE_BRICK_STAIRS,
							Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.WEST : Direction.EAST) : (dz > 0 ? Direction.NORTH : Direction.SOUTH), false));
					}
				}
			}
		}

		c.fill(cx + rIn + 1, 14, -6, cx + rIn + 7, 14, 6, OathholdCanvas.GOLD);
		c.wallBannerStrip(cx + rOut, 24, 0, 10, Direction.EAST);
		c.wallBannerStrip(cx - rOut, 24, 0, 10, Direction.WEST);
		c.chest(cx + rIn + 3, 15, 0, Direction.WEST, OathholdSites.LOOT_ARMORY, 501L);
		c.spawnMob(cx - 10, 1, 0, AuraWorld.OATHBOUND_BLADE_CHAMPION, 90.0F, true);
		c.spawnMob(cx + 10, 1, 0, AuraWorld.OATHBOUND_BASTION, -90.0F, true);
		c.spawnMob(cx, 1, -10, AuraWorld.OATHBOUND_HALBERDIER, 0.0F, true);

		// Tournament Pavilion Tents, Statues, Concourse Stalls & Spectators
		BlockState wRed = Blocks.WOOL.red().defaultBlockState();
		BlockState wGold = Blocks.WOOL.yellow().defaultBlockState();
		BlockState wGreen = Blocks.WOOL.green().defaultBlockState();
		BlockState wWhite = Blocks.WOOL.white().defaultBlockState();
		BlockState wBlue = Blocks.WOOL.blue().defaultBlockState();
		BlockState wOrange = Blocks.WOOL.orange().defaultBlockState();
		OathholdStreetscape.tournamentTent(c, -668, 1, -54, wRed, wGold);
		OathholdStreetscape.tournamentTent(c, -668, 1, 54, wBlue, wGold);
		OathholdStreetscape.tournamentTent(c, -556, 1, -54, wGreen, wWhite);
		OathholdStreetscape.tournamentTent(c, -556, 1, 54, wOrange, wWhite);
		OathholdStreetscape.tournamentTent(c, -672, 1, -34, wGreen, wGold);
		OathholdStreetscape.tournamentTent(c, -672, 1, 34, wRed, wWhite);
		OathholdStreetscape.tournamentTent(c, -552, 1, -34, wBlue, wWhite);
		OathholdStreetscape.tournamentTent(c, -552, 1, 34, wOrange, wGold);

		OathholdStreetscape.knightMonument(c, -548, 1, -12);
		OathholdStreetscape.knightMonument(c, -548, 1, 12);
		OathholdStreetscape.marketStall(c, -546, 1, -24, wRed, wWhite, 511);
		OathholdStreetscape.marketStall(c, -546, 1, 24, wOrange, wYellow(), 512);
		for (int sz : new int[]{-64, 64}) {
			OathholdStreetscape.ornateStreetLamp(c, -672, 1, sz, false);
			OathholdStreetscape.ornateStreetLamp(c, -552, 1, sz, false);
			OathholdStreetscape.parkBench(c, -612, 1, sz, sz < 0 ? Direction.SOUTH : Direction.NORTH);
		}
	}

	private static BlockState wYellow() {
		return Blocks.WOOL.yellow().defaultBlockState();
	}

	private static void buildImperialHarbor(OathholdCanvas c) {
		if (!c.intersects(498, -262, 954, 262)) return;

		// Stone Harbor Promenade Quay (x 536..555, top surface y = 1) with sparse flush dock lamps
		c.fillWeatheredStone(536, -1, -220, 555, 1, 220);
		for (int x : new int[]{540, 552}) {
			for (int z = Math.max(-220, c.clipMinLz()); z <= Math.min(220, c.clipMaxLz()); z++) {
				if (Math.floorMod(z, 16) == 8) {
					c.set(x, 1, z, OathholdCanvas.OCHRE_LIGHT);
				}
			}
		}
		for (int z = Math.max(-216, c.clipMinLz()); z <= Math.min(216, c.clipMaxLz()); z++) {
			boolean onPier = Math.abs(z) <= 6 || Math.abs(z - 120) <= 6 || Math.abs(z + 120) <= 6;
			if (!onPier) {
				if (Math.floorMod(z, 8) == 0) {
					boolean hasLamp = Math.floorMod(z, 24) == 0;
					c.set(555, 2, z, OathholdCanvas.TUFF_BRICKS);
					c.set(555, 3, z, hasLamp ? OathholdCanvas.LANTERN : OathholdCanvas.slab(Blocks.STONE_BRICK_SLAB, false));
				} else {
					c.set(555, 2, z, OathholdStreetscape.horizontalChain(true));
				}
			}
			if (Math.floorMod(z, 24) == 0 && !onPier) {
				OathholdStreetscape.ornateStreetLamp(c, 553, 2, z, true);
				OathholdStreetscape.cargoCluster(c, 549, 2, z + 4, z);
				OathholdStreetscape.parkBench(c, 552, 2, z - 4, Direction.EAST);
			}
		}

		// Striped Maritime & Fishmonger Stalls along quay (includes x=542, z=-32 -> canopy & hanging lantern at y=4!)
		BlockState[] harborA = {
			Blocks.WOOL.cyan().defaultBlockState(),
			Blocks.WOOL.blue().defaultBlockState(),
			Blocks.WOOL.orange().defaultBlockState()
		};
		BlockState wWhite = Blocks.WOOL.white().defaultBlockState();
		int[] stallZ = {-180, -148, -92, -62, -32, 32, 62, 92, 148, 180};
		for (int i = 0; i < stallZ.length; i++) {
			int sz = stallZ[i];
			OathholdStreetscape.marketStall(c, 542, 2, sz, harborA[i % harborA.length], wWhite, 620 + i);
		}
		// Dockside timber piles, raw ore stacks & cargo wagons
		c.fill(546, 2, -48, 551, 3, -45, OathholdCanvas.SPRUCE_LOG);
		c.fill(547, 2, 45, 550, 3, 47, Blocks.RAW_IRON_BLOCK.defaultBlockState());
		c.fill(548, 2, 48, 550, 3, 49, OathholdCanvas.GOLD);
		OathholdStreetscape.merchantWagon(c, 547, 2, -78, true, 651);
		OathholdStreetscape.merchantWagon(c, 547, 2, 78, true, 652);

		for (int pz : new int[]{-120, 0, 120}) {
			c.fillWeatheredStone(555, -2, pz - 6, 710, 1, pz + 6);
			c.fill(556, 1, pz - 4, 708, 1, pz + 4, OathholdCanvas.SPRUCE);
			for (int x = 570; x <= 685; x += 24) {
				OathholdStreetscape.ornateStreetLamp(c, x, 2, pz - 5, false);
				OathholdStreetscape.ornateStreetLamp(c, x, 2, pz + 5, false);
				OathholdStreetscape.cargoCluster(c, x + 6, 2, pz - 4, x + pz);
			}
			OathholdStreetscape.merchantWagon(c, 608, 2, pz, false, 670 + pz);
			// Towering Timber Harbor Cargo Crane near tip of pier
			c.fill(694, 2, pz - 1, 695, 15, pz - 1, OathholdCanvas.DARK_OAK_LOG);
			c.fill(694, 14, pz - 1, 704, 14, pz - 1, OathholdCanvas.DARK_OAK);
			c.fill(703, 9, pz - 1, 703, 13, pz - 1, OathholdCanvas.CHAIN);
			c.fill(702, 7, pz - 2, 704, 8, pz, OathholdCanvas.SPRUCE);
		}

		// Admiralty Harbor Guildhall + Dockside Harbor Tavern & patio tables
		OathholdBuildings.build(c, 0, 504, -24, 534, 24, 3,
			Blocks.CUT_COPPER_STAIRS.waxed().weathered(), Blocks.CUT_COPPER_SLAB.waxed().weathered(),
			true, OathholdBuildings.Theme.MARITIME_SE, true, 601);
		OathholdBuildings.build(c, 0, 506, 30, 532, 54, 2,
			Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB,
			false, OathholdBuildings.Theme.MARITIME_SE, false, 603);
		c.chest(518, 2, 0, Direction.EAST, OathholdSites.LOOT_MARKET, 602L);
		for (int ptz : new int[]{-16, -8, 8, 16, 36, 46}) {
			OathholdStreetscape.patioTable(c, 538, 2, ptz, Blocks.CARPET.cyan().defaultBlockState(), wWhite);
		}
		c.spawnMob(551, 2, -12, AuraWorld.OATHBOUND_BASTION, 90.0F, true);
		c.spawnMob(551, 2, 12, AuraWorld.OATHBOUND_HALBERDIER, 90.0F, true);

		// Central Breakwater Imperial Lighthouse between the galleons & NE Headland Beacon
		buildHarborLighthouse(c, 840, -16);
		buildHarborLighthouse(c, 928, -158);

		// Six 3-Mast Sailing Galleons with Emerald-Cross white sails moored across Harbor Basin!
		int[][] galleons = {{626, -58}, {626, 58}, {760, -58}, {760, 58}, {844, -150}, {844, 150}};
		for (int[] g : galleons) {
			buildGalleon(c, g[0], g[1]);
		}
	}

	private static void buildHarborLighthouse(OathholdCanvas c, int lhx, int lhz) {
		if (!c.intersects(lhx - 14, lhz - 14, lhx + 14, lhz + 14)) return;
		c.fillWeatheredStone(lhx - 10, -4, lhz - 10, lhx + 10, 2, lhz + 10);
		for (int x = lhx - 9; x <= lhx + 9; x += 3) {
			for (int z = lhz - 9; z <= lhz + 9; z += 3) {
				c.set(x, 2, z, OathholdCanvas.OCHRE_LIGHT);
			}
		}
		for (int dy = 3; dy <= 44; dy++) {
			int r = Math.max(4, 9 - dy / 10);
			BlockState band = (dy / 5) % 2 == 0 ? OathholdCanvas.CALCITE : OathholdCanvas.STONE_BRICKS;
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d <= r + 0.3 && d >= r - 1.5) {
						c.set(lhx + dx, dy, lhz + dz, band);
					}
				}
			}
		}
		c.fill(lhx - 3, 45, lhz - 3, lhx + 3, 50, lhz + 3, Blocks.STAINED_GLASS.yellow().defaultBlockState());
		c.fill(lhx - 2, 45, lhz - 2, lhx + 2, 50, lhz + 2, OathholdCanvas.SEA_LANTERN);
		c.fill(lhx - 1, 46, lhz - 1, lhx + 1, 49, lhz + 1, OathholdCanvas.OCHRE_LIGHT);
		c.ribbedDome(lhx, 51, lhz, 5, 5, OathholdCanvas.GOLD, OathholdCanvas.RAW_GOLD, OathholdCanvas.GOLD);
		c.flagPennant(lhx, 57, lhz);
	}

	private static void buildGalleon(OathholdCanvas c, int cx, int cz) {
		if (!c.intersects(cx - 28, cz - 14, cx + 30, cz + 14)) return;
		BlockState hull = OathholdCanvas.DARK_OAK;
		BlockState trim = OathholdCanvas.SPRUCE_LOG;
		BlockState deck = OathholdCanvas.SPRUCE;
		BlockState mast = OathholdCanvas.DARK_OAK_LOG;
		BlockState sailWhite = Blocks.WOOL.white().defaultBlockState();
		BlockState sailGreen = Blocks.WOOL.green().defaultBlockState();

		for (int dx = -22; dx <= 22; dx++) {
			int halfBeam = Math.max(1, (int) Math.round(7.5 * Math.sqrt(Math.max(0.0, 1.0 - (dx * dx) / 505.0))));
			for (int dz = -halfBeam; dz <= halfBeam; dz++) {
				for (int dy = -3; dy <= 3; dy++) {
					boolean edge = Math.abs(dz) >= halfBeam - 1 || dy <= -2 || Math.abs(dx) >= 20;
					if (edge) {
						c.set(cx + dx, dy, cz + dz, dy == 3 ? trim : hull);
					} else if (dy == 1) {
						c.set(cx + dx, dy, cz + dz, deck);
					} else if (dy > 1) {
						c.set(cx + dx, dy, cz + dz, OathholdCanvas.AIR);
					}
				}
			}
		}
		// Tiered sterncastle with cabin windows + forecastle + bowsprit
		c.fill(cx - 21, 4, cz - 5, cx - 10, 7, cz + 5, hull);
		c.fill(cx - 20, 8, cz - 4, cx - 11, 8, cz + 4, deck);
		c.set(cx - 21, 6, cz - 2, OathholdCanvas.OCHRE_LIGHT);
		c.set(cx - 21, 6, cz + 2, OathholdCanvas.OCHRE_LIGHT);
		c.fill(cx + 12, 4, cz - 4, cx + 19, 5, cz + 4, hull);
		c.fill(cx + 13, 6, cz - 3, cx + 18, 6, cz + 3, deck);
		c.fill(cx + 21, 5, cz, cx + 29, 5, cz, mast);
		c.chest(cx - 14, 2, cz, Direction.EAST, OathholdSites.LOOT_MARKET, cx * 5L + cz);

		// 3 Masts (Mizzen, Main at cx-2 preserving DARK_OAK_LOG probe at (758, by+18, -58), Foremast) with Emerald Cross Sails
		int[][] masts = {{cx - 14, 22, 4}, {cx - 2, 30, 7}, {cx + 10, 25, 6}};
		for (int[] m : masts) {
			int mx = m[0], topH = m[1], halfW = m[2];
			c.fill(mx, 2, cz, mx, topH, cz, mast);
			// Lower course sail + Upper topsail emblazoned with Emerald Cross
			int[][] tiers = (mx == cx - 14)
				? new int[][]{{11, 19, 4}}
				: new int[][]{{8, 16, halfW}, {18, topH - 3, Math.max(4, halfW - 1)}};
			for (int[] t : tiers) {
				int sy0 = t[0], sy1 = t[1], hw = t[2];
				int midY = (sy0 + sy1) / 2;
				for (int sy = sy0; sy <= sy1; sy++) {
					int bulge = (sy > sy0 + 1 && sy < sy1 - 1) ? 2 : 1;
					for (int sz = cz - hw; sz <= cz + hw; sz++) {
						boolean inCross = (Math.abs(sz - cz) <= 1 && Math.abs(sy - midY) <= 3)
							|| (sy == midY && Math.abs(sz - cz) <= Math.max(2, hw - 2));
						c.set(mx + bulge, sy, sz, inCross ? sailGreen : sailWhite);
					}
				}
			}
			c.flagPennant(mx, topH, cz);
		}
		c.set(cx - 19, 9, cz - 3, OathholdCanvas.LANTERN);
		c.set(cx - 19, 9, cz + 3, OathholdCanvas.LANTERN);
		c.set(cx + 16, 7, cz, OathholdCanvas.LANTERN);
	}

	private static void buildKingsExchangeSquare(OathholdCanvas c) {
		int x0 = -68, x1 = 68, z0 = 536, z1 = 672;
		if (!c.intersects(x0 - 4, z0 - 4, x1 + 4, z1 + 4)) return;

		// Two-story Imperial Exchange Guildhall (lx -56..+56, lz 542..584)
		c.fillWeatheredStone(-56, 1, 542, 56, 12, 584);
		c.clear(-54, 1, 544, 54, 11, 582);
		for (int x = Math.max(-54, c.clipMinLx()); x <= Math.min(54, c.clipMaxLx()); x++) {
			for (int z = Math.max(544, c.clipMinLz()); z <= Math.min(582, c.clipMaxLz()); z++) {
				if (z >= 560 && z <= 566) {
					c.set(x, 0, z, OathholdCanvas.POLISHED_DEEPSLATE);
					c.set(x, 1, z, Blocks.CARPET.red().defaultBlockState());
				} else {
					boolean even = ((x + z) & 1) == 0;
					c.set(x, 0, z, even ? OathholdCanvas.CALCITE : OathholdCanvas.POLISHED_DEEPSLATE);
				}
			}
		}
		// Mezzanine gallery & internal stair + stained-glass windows + 6 merchant exchange counters
		c.fill(-54, 7, 544, 54, 7, 582, OathholdCanvas.DARK_OAK);
		c.clear(-36, 7, 554, 36, 11, 572);
		for (int step = 0; step <= 6; step++) {
			c.fill(-50, 1 + step, 548 + step, -48, 1 + step, 548 + step, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
			c.clear(-50, 2 + step, 548 + step, -48, 11, 548 + step);
		}
		for (int ex = -36; ex <= 36; ex += 24) {
			c.fill(ex - 4, 1, 552, ex + 4, 1, 552, OathholdCanvas.DARK_OAK);
			c.fill(ex - 4, 1, 574, ex + 4, 1, 574, OathholdCanvas.DARK_OAK);
			c.set(ex, 2, 552, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
			c.set(ex, 2, 574, OathholdCanvas.LANTERN);
			c.gothicArchZ(ex, 3, 542, 543, 3, 6, 4, OathholdCanvas.TUFF_BRICKS, Blocks.STAINED_GLASS.yellow().defaultBlockState());
		}
		c.fill(-52, 1, 544, -52, 5, 580, Blocks.BOOKSHELF.defaultBlockState());
		c.fill(52, 1, 544, 52, 5, 580, Blocks.BOOKSHELF.defaultBlockState());
		c.gothicArchZ(0, 1, 583, 585, 6, 6, 4, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		c.pitchedRoofX(-56, 13, 542, 56, 584, Blocks.CUT_COPPER_STAIRS.waxed().weathered(), Blocks.CUT_COPPER_SLAB.waxed().weathered(),
			OathholdCanvas.CALCITE, OathholdCanvas.DARK_OAK);
		c.bastionTower(-54, 0, 584, 5, 22, 12, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(54, 0, 584, 5, 22, 12, OathholdCanvas.DEEPSLATE_TILES, true);
		c.chest(-40, 1, 562, Direction.EAST, OathholdSites.LOOT_MARKET, 701L);
		c.chest(40, 1, 562, Direction.WEST, OathholdSites.LOOT_MARKET, 702L);
		c.chandelier(-24, 12, 4, 562);
		c.chandelier(0, 12, 4, 562);
		c.chandelier(24, 12, 4, 562);

		// King's Plaza Triumphal Obelisk Fountain & Bustling Civic Square (z = 590..668)
		for (int x = Math.max(-14, c.clipMinLx()); x <= Math.min(14, c.clipMaxLx()); x++) {
			for (int z = Math.max(614, c.clipMinLz()); z <= Math.min(642, c.clipMaxLz()); z++) {
				double d = Math.sqrt(x * x + (z - 628) * (z - 628));
				if (d <= 12.0 && d >= 10.5) c.set(x, 1, z, OathholdCanvas.TUFF_BRICKS);
				else if (d <= 3.0) c.fill(x, 1, z, x, 4, z, OathholdCanvas.STONE_BRICKS);
				else if (d < 10.5) c.set(x, 1, z, OathholdCanvas.WATER);
			}
		}
		c.fill(-1, 5, 627, 1, 24, 629, OathholdCanvas.CALCITE);
		c.fill(0, 25, 628, 0, 28, 628, OathholdCanvas.GOLD);

		BlockState wRed = Blocks.WOOL.red().defaultBlockState();
		BlockState wBlue = Blocks.WOOL.blue().defaultBlockState();
		BlockState wGreen = Blocks.WOOL.green().defaultBlockState();
		BlockState wOrange = Blocks.WOOL.orange().defaultBlockState();
		BlockState wWhite = Blocks.WOOL.white().defaultBlockState();
		OathholdStreetscape.knightMonument(c, -10, 1, 592);
		OathholdStreetscape.knightMonument(c, 10, 1, 592);
		for (int sx : new int[]{-46, -26, 26, 46}) {
			OathholdStreetscape.marketStall(c, sx, 1, 610, sx < 0 ? wRed : wBlue, wWhite, 710 + sx);
			OathholdStreetscape.marketStall(c, sx, 1, 646, sx < 0 ? wOrange : wGreen, wWhite, 720 + sx);
			OathholdStreetscape.ornateStreetLamp(c, sx, 1, 596, false);
			OathholdStreetscape.ornateStreetLamp(c, sx, 1, 660, false);
		}
		for (int[] tree : new int[][]{{-18, 602}, {18, 602}, {-18, 654}, {18, 654}}) {
			OathholdStreetscape.treePlanter(c, tree[0], 0, tree[1], tree[0] > 0);
		}
		OathholdStreetscape.parkBench(c, -16, 1, 628, Direction.EAST);
		OathholdStreetscape.parkBench(c, 16, 1, 628, Direction.WEST);
		OathholdStreetscape.merchantWagon(c, -6, 1, 660, true, 731);
		OathholdStreetscape.merchantWagon(c, 6, 1, 598, true, 732);
	}

	private static void buildSovereignAbbey(OathholdCanvas c) {
		int x0 = -68, x1 = 68, z0 = -672, z1 = -536;
		if (!c.intersects(x0 - 4, z0 - 4, x1 + 4, z1 + 4)) return;

		// High Sovereign Abbey Church (lz -666..-622) with Furnished Pews, Windows & High Altar
		c.fillWeatheredStone(-48, 1, -666, 48, 16, -622);
		c.clear(-46, 1, -664, 46, 15, -624);
		for (int x = Math.max(-46, c.clipMinLx()); x <= Math.min(46, c.clipMaxLx()); x++) {
			for (int z = Math.max(-664, c.clipMinLz()); z <= Math.min(-624, c.clipMaxLz()); z++) {
				if (Math.abs(x) <= 2) {
					c.set(x, 0, z, OathholdCanvas.POLISHED_DEEPSLATE);
					c.set(x, 1, z, Blocks.CARPET.cyan().defaultBlockState());
				} else {
					c.set(x, 0, z, ((x + z) & 1) == 0 ? OathholdCanvas.CALCITE : OathholdCanvas.STONE_BRICKS);
				}
			}
		}
		BlockState pewNorth = OathholdCanvas.stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false);
		for (int pz = -656; pz <= -628; pz += 4) {
			c.fill(-16, 1, pz, -5, 1, pz, pewNorth);
			c.fill(5, 1, pz, 16, 1, pz, pewNorth);
		}
		for (int wx : new int[]{-32, -16, 16, 32}) {
			c.gothicArchZ(wx, 3, -666, -665, 3, 7, 4, OathholdCanvas.TUFF_BRICKS, Blocks.STAINED_GLASS.blue().defaultBlockState());
		}
		c.fill(-4, 1, -662, 4, 2, -659, OathholdCanvas.GOLD);
		c.lecternBook(0, 2, -658, Direction.SOUTH,
			"Abbey Codex of Oathhold", "Abbess of the Cloister",
			List.of("Silent stone cloisters, cherry blossoms, and endless vigilance preserve the Sovereign City."));
		c.gothicArchZ(0, 1, -623, -621, 5, 8, 4, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		c.pitchedRoofX(-48, 17, -666, 48, -622, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_SLAB,
			OathholdCanvas.CALCITE, OathholdCanvas.TUFF_BRICKS);
		c.bastionTower(0, 16, -644, 6, 22, 16, OathholdCanvas.COPPER_WEATHERED, true);
		c.chest(0, 1, -658, Direction.SOUTH, OathholdSites.LOOT_ARCANUM, 801L);
		c.chandelier(-22, 16, 4, -644);
		c.chandelier(0, 16, 4, -644);
		c.chandelier(22, 16, 4, -644);

		// Open Arched Cloister Colonnade wrapping Cherry Blossom & Herb Garden Garth (lz -616..-548)
		c.fillWeatheredStone(-28, 1, -616, 28, 6, -614);
		c.fillWeatheredStone(-28, 1, -550, 28, 6, -548);
		c.fillWeatheredStone(-28, 1, -616, -26, 6, -548);
		c.fillWeatheredStone(26, 1, -616, 28, 6, -548);
		for (int gx = -20; gx <= 20; gx += 8) {
			c.gothicArchZ(gx, 1, -616, -614, 2, 3, 2, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
			c.gothicArchZ(gx, 1, -550, -548, 2, 3, 2, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		}
		for (int gz = -608; gz <= -556; gz += 8) {
			c.gothicArchX(-28, -26, 1, gz, 2, 3, 2, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
			c.gothicArchX(26, 28, 1, gz, 2, 3, 2, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		}
		OathholdStreetscape.hedgePlanter(c, -22, 0, -610, 22, -554);
		c.tree(-14, 0, -582, true);
		c.tree(14, 0, -582, true);
		c.tree(-14, 0, -564, true);
		c.tree(14, 0, -564, true);
		c.fill(-3, 1, -585, 3, 2, -579, OathholdCanvas.TUFF_BRICKS);
		c.fill(-2, 1, -584, 2, 1, -580, OathholdCanvas.WATER);
		OathholdStreetscape.parkBench(c, -8, 1, -582, Direction.EAST);
		OathholdStreetscape.parkBench(c, 8, 1, -582, Direction.WEST);
		OathholdStreetscape.ornateStreetLamp(c, -18, 1, -604, false);
		OathholdStreetscape.ornateStreetLamp(c, 18, 1, -560, false);
	}
}
