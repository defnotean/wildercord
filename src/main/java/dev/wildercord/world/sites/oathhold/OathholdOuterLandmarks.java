package dev.wildercord.world.sites.oathhold;

import dev.wildercord.aura.world.AuraWorld;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Four colossal Outer Metropolitan Landmarks at 600-950 blocks from the center:
 * Great Oathbound Tournament Colosseum (West), Grand Imperial Harbor with Lighthouse &amp; 6 Galleons (East),
 * Grand King's Square &amp; Imperial Exchange Hall (South), and High Sovereign Abbey &amp; Cloister Gardens (North).
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
		if (!c.intersects(cx - rOut - 4, cz - rOut - 4, cx + rOut + 4, cz + rOut + 4)) return;

		int minX = Math.max(cx - rOut, c.clipMinLx()), maxX = Math.min(cx + rOut, c.clipMaxLx());
		int minZ = Math.max(cz - rOut, c.clipMinLz()), maxZ = Math.min(cz + rOut, c.clipMaxLz());

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				int dx = x - cx, dz = z - cz;
				double dist = Math.sqrt(dx * dx + dz * dz);
				if (dist > rOut + 0.5) continue;

				if (dist < rIn) {
					// Golden tournament arena sand floor & central duel dais
					c.set(x, 0, z, dist < 5.0 ? OathholdCanvas.POLISHED_ANDESITE : Blocks.SAND.defaultBlockState());
					if (dist < 5.0 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
						c.set(x, 0, z, OathholdCanvas.GOLD);
					}
				} else {
					// Cardinal arena fighter tunnel gates (E, W, N, S)
					boolean fighterGate = (Math.abs(dx) <= 4 || Math.abs(dz) <= 4);
					double frac = (dist - rIn) / (double) (rOut - rIn);
					int tierTop = Math.min(26, 3 + (int) Math.round(frac * 23));
					c.fillWeatheredStone(x, 1, z, x, tierTop, z);

					// Open radial stepped spectator seating + 3-tier exterior colonnade arches
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

		// Royal Viewing Box on East rim (GOLD + crimson carpet) & 4 Champions patrolling the sand!
		c.fill(cx + rIn + 1, 14, -6, cx + rIn + 7, 14, 6, OathholdCanvas.GOLD);
		c.wallBannerStrip(cx + rOut, 24, 0, 10, Direction.EAST);
		c.wallBannerStrip(cx - rOut, 24, 0, 10, Direction.WEST);
		c.chest(cx + rIn + 3, 15, 0, Direction.WEST, OathholdSites.LOOT_ARMORY, 501L);
		c.spawnMob(cx - 10, 1, 0, AuraWorld.OATHBOUND_BLADE_CHAMPION, 90.0F, true);
		c.spawnMob(cx + 10, 1, 0, AuraWorld.OATHBOUND_BASTION, -90.0F, true);
		c.spawnMob(cx, 1, -10, AuraWorld.OATHBOUND_HALBERDIER, 0.0F, true);
	}

	private static void buildImperialHarbor(OathholdCanvas c) {
		if (!c.intersects(498, -262, 954, 262)) return;

		// Stone Harbor Promenade Quay (lx 540..555) & Finger Cargo Piers (lz = -120, 0, +120)
		c.fillWeatheredStone(540, -1, -220, 555, 1, 220);
		for (int z = Math.max(-216, c.clipMinLz()); z <= Math.min(216, c.clipMaxLz()); z += 16) {
			c.streetLamp(554, 2, z);
		}
		for (int pz : new int[]{-120, 0, 120}) {
			c.fillWeatheredStone(555, -2, pz - 6, 710, 1, pz + 6);
			c.fill(556, 1, pz - 4, 708, 1, pz + 4, OathholdCanvas.SPRUCE);
			for (int x = 570; x <= 700; x += 20) {
				c.streetLamp(x, 2, pz - 5);
				c.streetLamp(x, 2, pz + 5);
				c.set(x + 2, 2, pz - 3, Blocks.BARREL.defaultBlockState());
				c.set(x + 3, 2, pz - 3, Blocks.BARREL.defaultBlockState());
			}
			// Towering Timber Harbor Cargo Crane near tip of pier
			c.fill(694, 2, pz - 1, 695, 15, pz - 1, OathholdCanvas.DARK_OAK_LOG);
			c.fill(694, 14, pz - 1, 704, 14, pz - 1, OathholdCanvas.DARK_OAK);
			c.fill(703, 9, pz - 1, 703, 13, pz - 1, OathholdCanvas.CHAIN);
			c.fill(702, 7, pz - 2, 704, 8, pz, OathholdCanvas.SPRUCE);
		}

		// Admiralty Harbor Guildhall facing Central Pier (lx 502..536, lz -26..+26)
		OathholdInnerCore.buildMedievalBuilding(c, 504, -24, 534, 24, 3,
			Blocks.CUT_COPPER_STAIRS.waxed().weathered(), Blocks.CUT_COPPER_SLAB.waxed().weathered(), true, 601);
		c.chest(518, 2, 0, Direction.EAST, OathholdSites.LOOT_MARKET, 602L);
		c.spawnMob(538, 2, 0, EntityTypes.VILLAGER, -90.0F, true);

		// Harbor Lighthouse Bastion at South-East / North-East Quay (cx = 928, cz = -158, height = 56)
		int lhx = 928, lhz = -158;
		if (c.intersects(lhx - 14, lhz - 14, lhx + 14, lhz + 14)) {
			c.fillWeatheredStone(lhx - 10, -4, lhz - 10, lhx + 10, 2, lhz + 10);
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
			// Lighthouse Lantern Beacon Room & Cupola
			c.fill(lhx - 3, 45, lhz - 3, lhx + 3, 50, lhz + 3, Blocks.STAINED_GLASS.yellow().defaultBlockState());
			c.fill(lhx - 2, 45, lhz - 2, lhx + 2, 50, lhz + 2, OathholdCanvas.SEA_LANTERN);
			c.fill(lhx - 1, 46, lhz - 1, lhx + 1, 49, lhz + 1, OathholdCanvas.OCHRE_LIGHT);
			c.ribbedDome(lhx, 51, lhz, 5, 5, OathholdCanvas.GOLD, OathholdCanvas.RAW_GOLD, OathholdCanvas.GOLD);
		}

		// Six Full-Scale Sailing Galleons moored across Harbor Basin!
		int[][] galleons = {{626, -58}, {626, 58}, {760, -58}, {760, 58}, {844, -150}, {844, 150}};
		for (int[] g : galleons) {
			buildGalleon(c, g[0], g[1]);
		}
	}

	private static void buildGalleon(OathholdCanvas c, int cx, int cz) {
		if (!c.intersects(cx - 26, cz - 12, cx + 26, cz + 12)) return;
		BlockState hull = OathholdCanvas.DARK_OAK;
		BlockState deck = OathholdCanvas.SPRUCE;
		BlockState mast = OathholdCanvas.DARK_OAK_LOG;
		BlockState sailWhite = Blocks.WOOL.white().defaultBlockState();
		BlockState sailGreen = Blocks.WOOL.green().defaultBlockState();

		for (int dx = -22; dx <= 22; dx++) {
			int halfBeam = Math.max(1, (int) Math.round(7.0 * Math.sqrt(Math.max(0.0, 1.0 - (dx * dx) / 500.0))));
			for (int dz = -halfBeam; dz <= halfBeam; dz++) {
				for (int dy = -3; dy <= 3; dy++) {
					boolean edge = Math.abs(dz) >= halfBeam - 1 || dy <= -2 || Math.abs(dx) >= 20;
					if (edge) {
						c.set(cx + dx, dy, cz + dz, hull);
					} else if (dy == 1) {
						c.set(cx + dx, dy, cz + dz, deck);
					} else if (dy > 1) {
						c.set(cx + dx, dy, cz + dz, OathholdCanvas.AIR);
					}
				}
			}
		}
		// Stern Forecastle Deck + Bowsprit
		c.fill(cx - 20, 4, cz - 4, cx - 10, 6, cz + 4, hull);
		c.fill(cx - 19, 7, cz - 3, cx - 11, 7, cz + 3, deck);
		c.fill(cx + 22, 4, cz, cx + 29, 4, cz, mast);
		c.chest(cx - 14, 2, cz, Direction.EAST, OathholdSites.LOOT_MARKET, cx * 5L + cz);

		// Mainmast & Foremast with billowing emerald-crossed white sails
		for (int mx : new int[]{cx - 2, cx + 10}) {
			int topH = mx < cx ? 27 : 22;
			c.fill(mx, 2, cz, mx, topH, cz, mast);
			int yardBase = topH - 12;
			for (int sy = yardBase; sy <= topH - 3; sy++) {
				int halfW = Math.min(7, halfBeamAtSail(mx, cz, sy));
				int curveX = (sy > yardBase + 2 && sy < topH - 5) ? 2 : 1;
				for (int sz = cz - halfW; sz <= cz + halfW; sz++) {
					boolean stripe = Math.abs(sz - cz) <= 1 || sy == yardBase + 4;
					c.set(mx + curveX, sy, sz, stripe ? sailGreen : sailWhite);
				}
			}
			c.flagPennant(mx, topH, cz);
		}
		c.set(cx - 18, 8, cz, OathholdCanvas.LANTERN);
		c.set(cx + 16, 4, cz, OathholdCanvas.LANTERN);
	}

	private static int halfBeamAtSail(int mx, int cz, int sy) {
		return 6;
	}

	private static void buildKingsExchangeSquare(OathholdCanvas c) {
		int x0 = -68, x1 = 68, z0 = 536, z1 = 672;
		if (!c.intersects(x0 - 4, z0 - 4, x1 + 4, z1 + 4)) return;

		// Imperial Exchange Guildhall (lx -56..+56, lz 542..584)
		c.fillWeatheredStone(-56, 1, 542, 56, 12, 584);
		c.clear(-54, 1, 544, 54, 11, 582);
		c.gothicArchZ(0, 1, 583, 585, 6, 6, 4, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		c.pitchedRoofX(-56, 13, 542, 56, 584, Blocks.CUT_COPPER_STAIRS.waxed().weathered(), Blocks.CUT_COPPER_SLAB.waxed().weathered(),
			OathholdCanvas.CALCITE, OathholdCanvas.DARK_OAK);
		c.bastionTower(-54, 0, 584, 5, 22, 12, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(54, 0, 584, 5, 22, 12, OathholdCanvas.DEEPSLATE_TILES, true);
		c.chest(-40, 1, 562, Direction.EAST, OathholdSites.LOOT_MARKET, 701L);
		c.chest(40, 1, 562, Direction.WEST, OathholdSites.LOOT_MARKET, 702L);
		c.chandelier(0, 12, 4, 562);

		// King's Plaza Triumphal Obelisk Fountain (cx=0, cz=628)
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
	}

	private static void buildSovereignAbbey(OathholdCanvas c) {
		int x0 = -68, x1 = 68, z0 = -672, z1 = -536;
		if (!c.intersects(x0 - 4, z0 - 4, x1 + 4, z1 + 4)) return;

		// High Sovereign Abbey Church (lz -666..-622)
		c.fillWeatheredStone(-48, 1, -666, 48, 16, -622);
		c.clear(-46, 1, -664, 46, 15, -624);
		c.gothicArchZ(0, 1, -623, -621, 5, 8, 4, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		c.pitchedRoofX(-48, 17, -666, 48, -622, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_SLAB,
			OathholdCanvas.CALCITE, OathholdCanvas.TUFF_BRICKS);
		c.bastionTower(0, 16, -644, 6, 22, 16, OathholdCanvas.COPPER_WEATHERED, true);
		c.chest(0, 1, -658, Direction.SOUTH, OathholdSites.LOOT_ARCANUM, 801L);

		// Square Cloister Colonnade wrapping Cherry Blossom Garden Courtyard (lz -616..-548)
		c.fillWeatheredStone(-28, 1, -616, 28, 6, -614);
		c.fillWeatheredStone(-28, 1, -550, 28, 6, -548);
		c.fillWeatheredStone(-28, 1, -616, -26, 6, -548);
		c.fillWeatheredStone(26, 1, -616, 28, 6, -548);
		c.tree(-14, 0, -582, true);
		c.tree(14, 0, -582, true);
		c.tree(-14, 0, -564, true);
		c.tree(14, 0, -564, true);
		c.fill(-3, 1, -585, 3, 2, -579, OathholdCanvas.TUFF_BRICKS);
		c.fill(-2, 1, -584, 2, 1, -580, OathholdCanvas.WATER);
	}
}
