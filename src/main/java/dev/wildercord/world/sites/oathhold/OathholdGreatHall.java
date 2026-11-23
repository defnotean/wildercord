package dev.wildercord.world.sites.oathhold;

import dev.wildercord.aura.world.AuraWorld;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The Sovereign Great Hall on the Upper Acropolis: 1:1 exterior match to the central gilded-dome palace
 * and 1:1 interior match to the gothic vaulted nave, rose window, viewing galleries, roaring hearths,
 * war-council tables, hanging chandeliers, crimson carpet aisle, and elevated gilded Throne Dais.
 */
public final class OathholdGreatHall {
	private OathholdGreatHall() {}

	private static final int AY = OathholdOrigin.ACROPOLIS_Y;
	private static final int MIN_X = -34, MAX_X = 34;
	private static final int MIN_Z = -204, MAX_Z = -92;

	public static void build(OathholdCanvas c) {
		if (!c.intersects(MIN_X - 4, MIN_Z - 4, MAX_X + 4, MAX_Z + 14)) return;

		BlockState glassWhite = Blocks.WHITE_STAINED_GLASS.defaultBlockState();
		BlockState glassGold = Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
		BlockState glassOrange = Blocks.ORANGE_STAINED_GLASS.defaultBlockState();
		BlockState carpetRed = Blocks.RED_CARPET.defaultBlockState();
		BlockState carpetTeal = Blocks.CYAN_CARPET.defaultBlockState();
		BlockState woolRed = Blocks.RED_WOOL.defaultBlockState();
		BlockState carpetGreen = Blocks.GREEN_CARPET.defaultBlockState();

		// 1. Clear vaulted interior cavity & build checkered deepslate/calcite/gold nave floor
		c.clear(-25, AY + 1, MIN_Z + 1, 25, AY + 40, MAX_Z - 1);
		for (int x = Math.max(-25, c.clipMinLx()); x <= Math.min(25, c.clipMaxLx()); x++) {
			for (int z = Math.max(MIN_Z + 1, c.clipMinLz()); z <= Math.min(MAX_Z - 1, c.clipMaxLz()); z++) {
				int ax = Math.abs(x);
				if (ax <= 2 && z >= -168) {
					c.set(x, AY, z, (x == 0 && Math.floorMod(z, 4) == 0 && z <= -98)
						? OathholdCanvas.OCHRE_LIGHT : OathholdCanvas.POLISHED_DEEPSLATE);
					c.set(x, AY + 1, z, carpetRed);
				} else if (ax == 3 && z >= -168) {
					c.set(x, AY, z, OathholdCanvas.STONE_BRICKS);
					c.set(x, AY + 1, z, carpetTeal);
				} else if (ax == 4 && z >= -168) {
					c.set(x, AY, z, OathholdCanvas.GOLD);
				} else {
					boolean even = (Math.floorDiv(x, 2) + Math.floorDiv(z, 2)) % 2 == 0;
					boolean goldTile = Math.floorMod(x, 6) == 0 && Math.floorMod(z, 6) == 0;
					c.set(x, AY, z, goldTile ? OathholdCanvas.GOLD : even ? OathholdCanvas.CALCITE : OathholdCanvas.POLISHED_DEEPSLATE);
				}
			}
		}

		// 2. Outer nave walls (full gable height to ceilingY) with warm brick arcading & towering clerestory lancets
		c.fillWeatheredStone(-26, AY + 1, MIN_Z, -25, AY + 34, MAX_Z);
		c.fillWeatheredStone(25, AY + 1, MIN_Z, 26, AY + 34, MAX_Z);
		for (int x = -26; x <= 26; x++) {
			int wallTop = AY + 42 - Math.abs(x) * 14 / 25;
			c.fillWeatheredStone(x, AY + 1, MIN_Z, x, wallTop, MIN_Z + 1);
			c.fillWeatheredStone(x, AY + 1, MAX_Z - 1, x, wallTop, MAX_Z);
		}

		// Brick arcade backing lining inside outer walls (matches concept_city_great_hall_interior brick + stone look)
		c.fill(-25, AY + 1, MIN_Z + 2, -25, AY + 14, MAX_Z - 2, OathholdCanvas.BRICKS);
		c.fill(25, AY + 1, MIN_Z + 2, 25, AY + 14, MAX_Z - 2, OathholdCanvas.BRICKS);
		c.fill(-25, AY + 16, MIN_Z + 2, -25, AY + 28, MAX_Z - 2, OathholdCanvas.BRICKS);
		c.fill(25, AY + 16, MIN_Z + 2, 25, AY + 28, MAX_Z - 2, OathholdCanvas.BRICKS);

		// External flying buttresses along East & West walls
		for (int bz = -196; bz <= -100; bz += 12) {
			c.fillWeatheredStone(-32, AY, bz - 1, -27, AY + 18, bz + 1);
			c.fill(-29, AY + 19, bz - 1, -27, AY + 22, bz + 1, OathholdCanvas.TUFF_BRICKS);
			c.fillWeatheredStone(27, AY, bz - 1, 32, AY + 18, bz + 1);
			c.fill(27, AY + 19, bz - 1, 29, AY + 22, bz + 1, OathholdCanvas.TUFF_BRICKS);
		}

		// 3. Clustered nave columns, ground pointed arches, second-story viewing galleries & gothic vault ribs
		for (int bz = -196; bz <= -100; bz += 12) {
			for (int px : new int[]{-13, 13}) {
				c.fill(px - 1, AY + 1, bz - 1, px + 1, AY + 24, bz + 1, OathholdCanvas.TUFF_BRICKS);
				c.fill(px - 1, AY + 1, bz - 1, px + 1, AY + 2, bz + 1, OathholdCanvas.CHISELED_TUFF_BRICKS);
				c.fill(px - 1, AY + 14, bz - 1, px + 1, AY + 15, bz + 1, OathholdCanvas.GOLD);
				c.set(px + (px < 0 ? 1 : -1), AY + 12, bz, OathholdCanvas.HANGING_LANTERN);
				c.set(px, AY + 17, bz, OathholdCanvas.LANTERN);
			}
			// Hanging Emerald-Gold Heraldic Banners on both nave columns & gallery outer walls
			c.wallBannerStrip(-11, AY + 10, bz, 6, Direction.EAST);
			c.wallBannerStrip(11, AY + 10, bz, 6, Direction.WEST);
			c.wallBannerStrip(-24, AY + 12, bz, 6, Direction.EAST);
			c.wallBannerStrip(24, AY + 12, bz, 6, Direction.WEST);

			// Transverse gothic vault arch curving overhead from lx = +-13 (y=AY+24) up to lx = 0 (y=AY+38)
			for (int dx = 0; dx <= 13; dx++) {
				int archY = AY + 38 - (int) Math.round(14.0 * Math.pow(dx / 13.0, 1.55));
				c.set(-dx, archY, bz, dx == 0 ? OathholdCanvas.GOLD : OathholdCanvas.TUFF_BRICKS);
				c.set(dx, archY, bz, dx == 0 ? OathholdCanvas.GOLD : OathholdCanvas.TUFF_BRICKS);
				c.set(-dx, archY - 1, bz, OathholdCanvas.STONE_BRICKS);
				c.set(dx, archY - 1, bz, OathholdCanvas.STONE_BRICKS);
			}
		}

		// Longitudinal upper galleries (ly = AY + 14) on both sides with stone balustrades + ground arch openings + clerestory lancets
		for (int bz = -190; bz <= -106; bz += 12) {
			c.gothicArchX(-13, -12, AY + 1, bz, 4, 7, 4, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
			c.gothicArchX(12, 13, AY + 1, bz, 4, 7, 4, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
			// Sunlit Clerestory Gothic Windows
			c.gothicArchX(-26, -24, AY + 18, bz, 3, 7, 4, OathholdCanvas.STONE_BRICKS, glassWhite);
			c.gothicArchX(24, 26, AY + 18, bz, 3, 7, 4, OathholdCanvas.STONE_BRICKS, glassWhite);
		}
		for (int bz : new int[]{-122, -146, -170}) {
			c.gothicArchX(-26, -24, AY + 3, bz, 3, 5, 3, OathholdCanvas.TUFF_BRICKS, glassWhite);
			c.gothicArchX(24, 26, AY + 3, bz, 3, 5, 3, OathholdCanvas.TUFF_BRICKS, glassWhite);
		}
		for (int bz : new int[]{-114, -126, -138, -154, -166, -178}) {
			c.set(-24, AY + 6, bz, OathholdCanvas.OCHRE_LIGHT);
			c.set(24, AY + 6, bz, OathholdCanvas.OCHRE_LIGHT);
			c.set(-14, AY + 15, bz, OathholdCanvas.OCHRE_LIGHT);
			c.set(14, AY + 15, bz, OathholdCanvas.OCHRE_LIGHT);
		}
		for (int gx : new int[]{-14, 14}) {
			int lo = gx < 0 ? -24 : 13, hi = gx < 0 ? -13 : 24;
			c.fill(lo, AY + 14, MIN_Z + 2, hi, AY + 14, MAX_Z - 2, OathholdCanvas.STONE_BRICKS);
			int railX = gx < 0 ? -13 : 13;
			c.fill(railX, AY + 15, MIN_Z + 2, railX, AY + 16, MAX_Z - 2, OathholdCanvas.TUFF_BRICKS);
		}

		// Steep timber-and-stone gothic gable vault ceiling covering entire hall
		for (int x = -25; x <= 25; x++) {
			int ceilingY = AY + 42 - Math.abs(x) * 14 / 25;
			c.fill(x, ceilingY, MIN_Z, x, ceilingY, MAX_Z, OathholdCanvas.DARK_OAK);
			c.fill(x, ceilingY + 1, MIN_Z, x, ceilingY + 2, MAX_Z, Math.abs(x) <= 2 ? OathholdCanvas.GOLD : OathholdCanvas.DEEPSLATE_TILES);
		}

		// 4. Exterior Tall Octagonal Drum + Gilded Central Golden Dome & 4 Corner Golden Turrets
		for (int dy = 40; dy <= 47; dy++) {
			for (int dx = -15; dx <= 15; dx++) {
				for (int dz = -15; dz <= 15; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d <= 15.4 && d >= 13.6) {
						boolean ring = dy == 40 || dy == 47;
						boolean win = !ring && (Math.abs(dx) <= 1 || Math.abs(dz) <= 1);
						c.set(dx, AY + dy, -132 + dz, ring ? OathholdCanvas.GOLD : (win ? glassGold : OathholdCanvas.CALCITE));
					}
				}
			}
		}
		c.ribbedDome(0, AY + 48, -132, 16, 18, OathholdCanvas.GOLD, OathholdCanvas.RAW_GOLD, OathholdCanvas.GOLD);
		c.flagPennant(0, AY + 72, -132);
		for (int[] tx : new int[][]{{-26, -94}, {26, -94}, {-26, -202}, {26, -202}}) {
			c.bastionTower(tx[0], AY, tx[1], 5, 34, 0, OathholdCanvas.DEEPSLATE_TILES, false);
			c.ribbedDome(tx[0], AY + 35, tx[1], 5, 8, OathholdCanvas.GOLD, OathholdCanvas.RAW_GOLD, OathholdCanvas.GOLD);
		}

		// 5. Open-Daylight North Solar Rose Window + South Grand Portico, Gilded Pediment & Windows
		c.roseWindowZOpen(0, AY + 30, MIN_Z + 1, MIN_Z, 8, glassGold, glassOrange, OathholdCanvas.TUFF_BRICKS);
		c.roseWindowZOpen(0, AY + 32, MAX_Z, MAX_Z - 1, 5, glassGold, glassOrange, OathholdCanvas.TUFF_BRICKS);
		c.gothicArchZ(-18, AY + 8, MAX_Z - 1, MAX_Z, 2, 7, 4, OathholdCanvas.TUFF_BRICKS, glassGold);
		c.gothicArchZ(18, AY + 8, MAX_Z - 1, MAX_Z, 2, 7, 4, OathholdCanvas.TUFF_BRICKS, glassGold);
		c.gothicArchZ(0, AY + 1, MAX_Z - 4, MAX_Z + 2, 6, 10, 5, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		c.wallBannerStrip(-10, AY + 16, MAX_Z + 1, 8, Direction.SOUTH);
		c.wallBannerStrip(10, AY + 16, MAX_Z + 1, 8, Direction.SOUTH);

		// Monumental South Portico & Gilded Pediment (z = -91..-80)
		c.fillWeatheredStone(-14, AY, -85, 14, AY, -80);
		c.fill(-12, AY + 1, -85, 12, AY + 1, -84, OathholdCanvas.POLISHED_ANDESITE);
		for (int px : new int[]{-12, -6, 6, 12}) {
			c.fill(px, AY + 1, -87, px, AY + 3, -86, OathholdCanvas.TUFF_BRICKS);
			c.fill(px, AY + 4, -87, px, AY + 18, -86, OathholdCanvas.CALCITE);
			c.fill(px, AY + 19, -87, px, AY + 20, -86, OathholdCanvas.GOLD);
		}
		c.fill(-14, AY + 21, -91, 14, AY + 21, -89, OathholdCanvas.CALCITE);
		c.fill(-15, AY + 21, -88, 15, AY + 21, -86, OathholdCanvas.GOLD);
		for (int dy = 1; dy <= 8; dy++) {
			int half = 14 - dy * 14 / 8;
			if (half > 0) {
				c.fill(-half + 1, AY + 21 + dy, -88, half - 1, AY + 21 + dy, -86, OathholdCanvas.CALCITE);
			}
			c.fill(-half, AY + 21 + dy, -88, -half, AY + 21 + dy, -86, OathholdCanvas.GOLD);
			c.fill(half, AY + 21 + dy, -88, half, AY + 21 + dy, -86, OathholdCanvas.GOLD);
		}
		c.set(0, AY + 24, -86, OathholdCanvas.OCHRE_LIGHT);
		c.set(-1, AY + 24, -86, OathholdCanvas.GOLD);
		c.set(1, AY + 24, -86, OathholdCanvas.GOLD);
		c.set(0, AY + 23, -86, OathholdCanvas.GOLD);
		c.set(0, AY + 25, -86, OathholdCanvas.GOLD);

		// 6. Six Suspended Iron-Chain & Dark-Oak Lantern Chandeliers
		for (int cz : new int[]{-116, -142, -166}) {
			c.chandelier(-7, AY + 36, 7, cz);
			c.chandelier(7, AY + 36, 7, cz);
			c.set(0, AY + 38, cz, OathholdCanvas.OCHRE_LIGHT);
		}

		// 7. Four Roaring Stone Hearth Fireplaces & Four Long Dark-Oak War-Council Tables
		for (int fz : new int[]{-110, -158}) {
			for (int side : new int[]{-1, 1}) {
				int fx = side * 23;
				c.fill(fx - side, AY + 1, fz - 2, fx, AY + 12, fz + 2, OathholdCanvas.TUFF_BRICKS);
				c.clear(fx - side, AY + 1, fz - 1, fx - side, AY + 3, fz + 1);
				c.set(fx - side, AY + 1, fz, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
				c.set(fx - side * 2, AY + 2, fz, OathholdCanvas.OCHRE_LIGHT);
				c.set(fx - side * 2, AY + 4, fz, OathholdCanvas.LANTERN);
			}
		}
		for (int tz : new int[]{-132, -172}) {
			for (int side : new int[]{-1, 1}) {
				int tx = side * 18;
				c.fill(tx - 1, AY + 1, tz - 4, tx + 1, AY + 1, tz + 4, OathholdCanvas.DARK_OAK);
				c.set(tx, AY + 2, tz - 1, carpetGreen);
				c.set(tx, AY + 2, tz + 1, carpetGreen);
				c.set(tx, AY + 2, tz, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
				for (int sz = tz - 3; sz <= tz + 3; sz += 2) {
					c.set(tx - 2, AY + 1, sz, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
					c.set(tx + 2, AY + 1, sz, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
				}
			}
		}

		// 8. Grand 8-Step Throne Staircase + Gilded Sovereign Throne Dais + Glowing Solar Sunburst Emblem
		for (int step = 0; step <= 7; step++) {
			int sz = -168 - step;
			int sy = AY + step + 1;
			c.fill(-6, AY + 1, sz, 6, sy - 1, sz, OathholdCanvas.STONE_BRICKS);
			c.fill(-5, sy, sz, 5, sy, sz, OathholdCanvas.stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
			c.set(-6, sy, sz, OathholdCanvas.GOLD);
			c.set(6, sy, sz, OathholdCanvas.GOLD);
		}
		int daisY = AY + 8;
		c.fill(-12, AY + 1, MIN_Z + 2, 12, daisY, -176, OathholdCanvas.TUFF_BRICKS);
		c.fill(-10, daisY, MIN_Z + 3, 10, daisY, -177, OathholdCanvas.POLISHED_DEEPSLATE);
		c.fill(-3, daisY + 1, -189, 3, daisY + 1, -178, carpetRed);

		// Sovereign Throne & Carved Gilded Arch + Blazing Solar Sunburst
		c.fill(-2, daisY + 1, -193, 2, daisY + 1, -190, OathholdCanvas.GOLD);
		c.fill(-1, daisY + 2, -193, 1, daisY + 4, -193, OathholdCanvas.GOLD);
		c.set(0, daisY + 2, -191, woolRed);
		c.set(-1, daisY + 2, -191, OathholdCanvas.GOLD);
		c.set(1, daisY + 2, -191, OathholdCanvas.GOLD);
		// Blazing Solar Sunburst on apse behind throne & carved gilded pointed arch
		int sunY = daisY + 9;
		for (int dx = -4; dx <= 4; dx++) {
			for (int dy = -4; dy <= 4; dy++) {
				double dist = Math.sqrt(dx * dx + dy * dy);
				if (dist <= 1.8) c.set(dx, sunY + dy, -196, OathholdCanvas.OCHRE_LIGHT);
				else if (dist <= 3.1 && (dx == 0 || dy == 0 || Math.abs(dx) == Math.abs(dy))) c.set(dx, sunY + dy, -196, OathholdCanvas.GLOWSTONE);
				else if (dist <= 4.3 && (dx == 0 || dy == 0 || Math.abs(dx) == Math.abs(dy))) c.set(dx, sunY + dy, -196, OathholdCanvas.GOLD);
			}
		}
		c.gothicArchZ(0, daisY + 1, -195, -195, 5, 8, 6, OathholdCanvas.GOLD, OathholdCanvas.AIR);
		c.fill(-6, daisY + 1, -195, -6, daisY + 10, -195, OathholdCanvas.CHISELED_TUFF_BRICKS);
		c.fill(6, daisY + 1, -195, 6, daisY + 10, -195, OathholdCanvas.CHISELED_TUFF_BRICKS);

		c.chest(-8, daisY + 1, -191, Direction.EAST, OathholdSites.LOOT_TREASURY, 111L);
		c.chest(8, daisY + 1, -191, Direction.WEST, OathholdSites.LOOT_TREASURY, 113L);
		c.lecternBook(0, AY + 1, -110, Direction.SOUTH,
			"Sovereign Charter of Oathhold", "First Banner-Marshal",
			List.of(
				"Here stands Oathhold, Bulwark of the Realms, raised across two thousand paces of stone and oath.",
				"May every traveler pass the South Gate under the Sovereign Sunburst and find honor in the Hall."
			));

		// 9. Ceremonial Oathbound High Knights lining the Sovereign Great Hall aisle!
		c.spawnMob(-5, AY + 1, -118, AuraWorld.OATHBOUND_BASTION, -90.0F, true);
		c.spawnMob(5, AY + 1, -118, AuraWorld.OATHBOUND_BASTION, 90.0F, true);
		c.spawnMob(-5, AY + 1, -132, AuraWorld.OATHBOUND_HALBERDIER, -90.0F, true);
		c.spawnMob(5, AY + 1, -132, AuraWorld.OATHBOUND_HALBERDIER, 90.0F, true);
		c.spawnMob(-5, AY + 1, -146, AuraWorld.OATHBOUND_BLADE_CHAMPION, -90.0F, true);
		c.spawnMob(5, AY + 1, -146, AuraWorld.OATHBOUND_BLADE_CHAMPION, 90.0F, true);
		c.spawnMob(-5, AY + 1, -160, AuraWorld.OATHBOUND_BASTION, -90.0F, true);
		c.spawnMob(5, AY + 1, -160, AuraWorld.OATHBOUND_BASTION, 90.0F, true);
		c.spawnMob(0, daisY + 1, -183, AuraWorld.OATHBOUND_BANNER_MARSHAL, 0.0F, true);
	}
}
