package dev.wildercord.world.sites.oathhold;

import dev.wildercord.aura.world.AuraWorld;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Crown jewels of the Upper Acropolis (at {@code ly = ACROPOLIS_Y = 16}):
 * <ol>
 *   <li><b>Ceremonial Acropolis Esplanade</b> — gold-trimmed royal avenue, boxwood parterres, cherry gardens, hero statues &amp; braziers;</li>
 *   <li><b>Hall of the Oathbound</b> — two-story furnished barracks keep, wooden jousting list &amp; archery range;</li>
 *   <li><b>Arcanum Grand Library &amp; Observatory</b> — calcite &amp; copper-domed library with cherry-blossom terraces;</li>
 *   <li><b>Sovereign Cathedral &amp; Grand Bell-Spire</b> — 16 rows of dark-oak pews, stained-glass lancets, pipe organ &amp; gilded altar.</li>
 * </ol>
 */
public final class OathholdAcropolisHalls {
	private OathholdAcropolisHalls() {}

	private static final int AY = OathholdOrigin.ACROPOLIS_Y;

	public static void build(OathholdCanvas c) {
		buildOathboundBarracks(c);
		buildArcanumLibrary(c);
		buildCathedralSpire(c);
		buildAcropolisEsplanade(c);
	}

	private static void buildAcropolisEsplanade(OathholdCanvas c) {
		if (!c.intersects(-180, -98, 180, -46)) return;

		// Gold-bordered ceremonial avenue straight up to the Sovereign Great Hall Portico
		for (int z = Math.max(-79, c.clipMinLz()); z <= Math.min(-53, c.clipMaxLz()); z++) {
			c.fill(-6, AY, z, 6, AY, z, OathholdCanvas.POLISHED_ANDESITE);
			c.set(-7, AY, z, OathholdCanvas.GOLD);
			c.set(7, AY, z, OathholdCanvas.GOLD);
		}
		for (int z : new int[]{-76, -62}) {
			for (int sx : new int[]{-10, 10}) {
				c.set(sx, AY + 1, z, OathholdCanvas.CHISELED_TUFF_BRICKS);
				c.set(sx, AY + 2, z, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
			}
		}
		for (int z : new int[]{-72, -56}) {
			OathholdStreetscape.ornateStreetLamp(c, -11, AY + 1, z, false);
			OathholdStreetscape.ornateStreetLamp(c, 11, AY + 1, z, false);
		}
		OathholdStreetscape.knightMonument(c, -15, AY + 1, -68);
		OathholdStreetscape.knightMonument(c, 15, AY + 1, -68);
		OathholdStreetscape.knightMonument(c, -15, AY + 1, -58);
		OathholdStreetscape.knightMonument(c, 15, AY + 1, -58);

		// Symmetric West & East Royal Parterre Gardens (manicured boxwood hedges, cherry trees & benches)
		OathholdStreetscape.hedgePlanter(c, -50, AY, -84, -20, -52);
		OathholdStreetscape.hedgePlanter(c, -36, AY, -72, -28, -64);
		OathholdStreetscape.treePlanter(c, -43, AY, -76, true);
		OathholdStreetscape.treePlanter(c, -26, AY, -60, true);
		OathholdStreetscape.parkBench(c, -35, AY + 1, -54, Direction.NORTH);

		OathholdStreetscape.hedgePlanter(c, 20, AY, -84, 50, -52);
		OathholdStreetscape.hedgePlanter(c, 28, AY, -72, 36, -64);
		OathholdStreetscape.treePlanter(c, 43, AY, -76, true);
		OathholdStreetscape.treePlanter(c, 26, AY, -60, false);
		OathholdStreetscape.parkBench(c, 35, AY + 1, -54, Direction.NORTH);

		// South-West Barracks Parkland & Cathedral Forecourt Parterres
		OathholdStreetscape.hedgePlanter(c, -162, AY, -92, -104, -54);
		OathholdStreetscape.treePlanter(c, -150, AY, -80, true);
		OathholdStreetscape.treePlanter(c, -116, AY, -66, true);
		OathholdStreetscape.knightMonument(c, -134, AY + 1, -74);
		OathholdStreetscape.parkBench(c, -134, AY + 1, -56, Direction.NORTH);
		OathholdStreetscape.ornateStreetLamp(c, -106, AY + 1, -74, true);

		OathholdStreetscape.hedgePlanter(c, 124, AY, -82, 142, -54);
		OathholdStreetscape.hedgePlanter(c, 158, AY, -82, 176, -54);
		OathholdStreetscape.treePlanter(c, 133, AY, -68, true);
		OathholdStreetscape.treePlanter(c, 167, AY, -68, true);
		OathholdStreetscape.ornateStreetLamp(c, 145, AY + 1, -68, false);
		OathholdStreetscape.ornateStreetLamp(c, 155, AY + 1, -68, false);
	}

	private static void buildOathboundBarracks(OathholdCanvas c) {
		if (!c.intersects(-178, -194, -48, -58)) return;

		int x0 = -168, x1 = -96, z0 = -180, z1 = -106;
		c.fillWeatheredStone(x0, AY + 1, z0, x1, AY + 18, z1);
		c.clear(x0 + 2, AY + 1, z0 + 2, x1 - 2, AY + 17, z1 - 2);
		c.fill(x0 + 2, AY + 9, z0 + 2, x1 - 2, AY + 9, z1 - 2, OathholdCanvas.SPRUCE);
		c.fill(x0 + 1, AY + 18, z0 + 1, x1 - 1, AY + 18, z1 - 1, OathholdCanvas.POLISHED_ANDESITE);

		for (int x = Math.max(x0, c.clipMinLx()); x <= Math.min(x1, c.clipMaxLx()); x++) {
			if (Math.floorMod(x, 4) < 2) {
				c.set(x, AY + 19, z0, OathholdCanvas.STONE_BRICKS);
				c.set(x, AY + 19, z1, OathholdCanvas.STONE_BRICKS);
			}
			if (Math.floorMod(x, 12) == 0) {
				c.wallBannerStrip(x, AY + 16, z1 + 1, 7, Direction.SOUTH);
			}
		}
		for (int z = Math.max(z0, c.clipMinLz()); z <= Math.min(z1, c.clipMaxLz()); z++) {
			if (Math.floorMod(z, 4) < 2) {
				c.set(x0, AY + 19, z, OathholdCanvas.STONE_BRICKS);
				c.set(x1, AY + 19, z, OathholdCanvas.STONE_BRICKS);
			}
		}
		c.bastionTower(x0, AY, z0, 5, 24, 0, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(x1, AY, z0, 5, 24, 0, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(x0, AY, z1, 5, 24, 0, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(x1, AY, z1, 6, 26, 0, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(-132, AY, -143, 7, 32, 0, OathholdCanvas.DEEPSLATE_TILES, true);

		// Internal staircase & furnished Two-Story Barracks Keep
		c.gothicArchX(x1 - 2, x1 + 1, AY + 1, -132, 4, 6, 4, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		for (int step = 0; step <= 8; step++) {
			int sz = -170 + step;
			int sy = AY + 1 + step;
			c.fill(-101, AY + 1, sz, -99, sy - 1, sz, OathholdCanvas.STONE_BRICKS);
			c.fill(-101, sy, sz, -99, sy, sz, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
			c.clear(-101, sy + 1, sz, -99, AY + 16, sz);
		}
		c.fill(-165, AY + 1, -145, -163, AY + 5, -141, OathholdCanvas.BRICKS);
		c.set(-163, AY + 1, -143, OathholdCanvas.OCHRE_LIGHT);
		c.fill(-150, AY + 1, -148, -134, AY + 1, -146, OathholdCanvas.DARK_OAK);
		c.set(-142, AY + 2, -147, OathholdCanvas.LANTERN);

		for (int bx = -160; bx <= -116; bx += 8) {
			c.bed(bx, AY + 1, -172, DyeColor.GREEN, Direction.NORTH);
			c.bed(bx, AY + 10, -172, DyeColor.GREEN, Direction.NORTH);
			c.set(bx + 2, AY + 1, -172, Blocks.BARREL.defaultBlockState());
			c.set(bx - 1, AY + 1, -162, Blocks.ANVIL.defaultBlockState());
			c.set(bx, AY + 7, -140, OathholdCanvas.HANGING_LANTERN);
			c.set(bx, AY + 16, -140, OathholdCanvas.HANGING_LANTERN);
		}
		c.fill(-158, AY + 10, -124, -148, AY + 10, -118, OathholdCanvas.DARK_OAK);
		c.set(-153, AY + 11, -121, OathholdCanvas.LANTERN);
		c.chest(-160, AY + 1, -120, Direction.EAST, OathholdSites.LOOT_BARRACKS, 201L);
		c.chest(-104, AY + 1, -120, Direction.WEST, OathholdSites.LOOT_ARMORY, 202L);
		c.lecternBook(-132, AY + 1, -124, Direction.SOUTH,
			"Chronicles of the Four Orders", "Oathbound Chronicler",
			List.of(
				"Bastions hold the breach, Blade-Champions turn every steel edge, Halberdiers claim the skies, and Banner-Marshals rally the line.",
				"Let no squire cross these gates without mastering poise, guard, and counter."
			));

		// Knight Training Yard, Wooden Jousting Tilt List & Archery Targets (x = -94..-54, z = -146..-66)
		for (int px = Math.max(-94, c.clipMinLx()); px <= Math.min(-54, c.clipMaxLx()); px++) {
			if (Math.abs(px + 74) > 5) {
				c.fill(px, AY + 1, -66, px, AY + 2, -66, OathholdCanvas.TUFF_BRICKS);
			}
			c.set(px, AY + 1, -146, OathholdCanvas.TUFF_BRICKS);
			if (Math.floorMod(px, 8) == 0) {
				OathholdStreetscape.ornateStreetLamp(c, px, AY + 1, -144, false);
				OathholdStreetscape.ornateStreetLamp(c, px, AY + 1, -68, false);
			}
		}
		for (int jz = -136; jz <= -96; jz++) {
			c.set(-74, AY + 1, jz, Blocks.OAK_FENCE.defaultBlockState());
			c.set(-74, AY + 2, jz, (jz & 1) == 0 ? Blocks.GREEN_WOOL.defaultBlockState() : Blocks.YELLOW_WOOL.defaultBlockState());
		}
		for (int tx : new int[]{-90, -86, -82}) {
			c.set(tx, AY + 1, -142, Blocks.HAY_BLOCK.defaultBlockState());
			c.set(tx, AY + 2, -142, Blocks.TARGET.defaultBlockState());
		}
		OathholdStreetscape.parkBench(c, -58, AY + 1, -118, Direction.WEST);
		OathholdStreetscape.cargoCluster(c, -90, AY + 1, -80, 205);
		c.fill(-80, AY, -112, -68, AY, -100, OathholdCanvas.POLISHED_ANDESITE);
		c.spawnMob(-78, AY + 1, -110, AuraWorld.OATHBOUND_BLADE_CHAMPION, 0.0F, true);
		c.spawnMob(-70, AY + 1, -102, AuraWorld.OATHBOUND_BASTION, 180.0F, true);
		c.spawnMob(-84, AY + 1, -92, AuraWorld.OATHBOUND_HALBERDIER, 45.0F, true);
		c.spawnMob(-66, AY + 1, -86, AuraWorld.OATHBOUND_BANNER_MARSHAL, -45.0F, true);
	}

	private static void buildArcanumLibrary(OathholdCanvas c) {
		if (!c.intersects(40, -180, 120, -44)) return;

		int cx = 80, cz = -124;
		BlockState glassCyan = Blocks.CYAN_STAINED_GLASS.defaultBlockState();
		BlockState glassGold = Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
		c.clear(53, AY + 1, -163, 107, AY + 45, -85);

		// Classical Palladian stone-and-calcite colonnade walls (flat balustraded roof at AY + 18)
		for (int y = AY + 1; y <= AY + 18; y++) {
			for (int x = 52; x <= 108; x++) {
				boolean cornerCol = x <= 55 || x >= 105;
				boolean col = (x % 4) == 0;
				boolean win = !cornerCol && !col && y >= AY + 4 && y <= AY + 14;
				BlockState blk = y <= AY + 2 || y >= AY + 17 ? OathholdCanvas.STONE_BRICKS
					: (win ? glassCyan : (col ? OathholdCanvas.STONE_BRICKS : OathholdCanvas.CALCITE));
				c.set(x, y, -164, blk);
				c.set(x, y, -84, blk);
			}
			for (int z = -163; z <= -85; z++) {
				boolean col = (z % 4) == 0;
				boolean win = !col && y >= AY + 4 && y <= AY + 14;
				BlockState blk = y <= AY + 2 || y >= AY + 17 ? OathholdCanvas.STONE_BRICKS
					: (win ? glassCyan : (col ? OathholdCanvas.STONE_BRICKS : OathholdCanvas.CALCITE));
				c.set(52, y, z, blk);
				c.set(108, y, z, blk);
			}
		}
		// Flat classical roof slab & stone balustrade (AY + 18..19) so the Teal Copper Dome stands fully exposed!
		c.fill(53, AY + 18, -163, 107, AY + 18, -85, OathholdCanvas.STONE_BRICKS);
		c.clear(cx - 14, AY + 18, cz - 14, cx + 14, AY + 18, cz + 14);
		for (int x = 53; x <= 107; x++) {
			if ((x & 1) == 0) {
				c.set(x, AY + 19, -164, OathholdCanvas.TUFF_BRICKS);
				c.set(x, AY + 19, -84, OathholdCanvas.TUFF_BRICKS);
			}
		}
		for (int z = -163; z <= -85; z++) {
			if ((z & 1) == 0) {
				c.set(52, AY + 19, z, OathholdCanvas.TUFF_BRICKS);
				c.set(108, AY + 19, z, OathholdCanvas.TUFF_BRICKS);
			}
		}

		// Library interior floor, towering bookshelves & astral lectern
		c.fill(54, AY, -162, 106, AY, -86, OathholdCanvas.POLISHED_ANDESITE);
		for (int x = 55; x <= 105; x += 6) {
			if (Math.abs(x - cx) < 6) continue;
			c.fill(x, AY + 1, -158, x, AY + 8, -142, Blocks.BOOKSHELF.defaultBlockState());
			c.fill(x, AY + 1, -108, x, AY + 8, -92, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
		}
		for (int sx : new int[]{68, 92}) {
			c.set(sx, AY + 1, cz, Blocks.OAK_FENCE.defaultBlockState());
			c.set(sx, AY + 2, cz, Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState());
			c.set(sx, AY + 3, cz, OathholdCanvas.LANTERN);
		}
		c.set(cx, AY + 1, cz, Blocks.ENCHANTING_TABLE.defaultBlockState());
		c.chest(cx - 8, AY + 1, cz, Direction.EAST, OathholdSites.LOOT_ARCANUM, 211L);
		c.chest(cx + 8, AY + 1, cz, Direction.WEST, OathholdSites.LOOT_ARCANUM, 212L);
		c.lecternBook(cx, AY + 1, cz + 12, Direction.SOUTH,
			"Astral Cartography of Oathhold", "Arch-Copyist of the Dome",
			List.of(
				"Beneath the copper dome, runic scholars trace leylines across the Overworld while Knights guard the roads below.",
				"Look up at dusk to read the Starlit constellations painted across the weathered vault ribs."
			));
		c.chandelier(cx, AY + 34, 8, cz);

		// Windowed Octagonal Clerestory Drum (AY+19..26) & Towering Teal/Oxidized Copper Ribbed Dome (AY+27..45)
		for (int dy = 19; dy <= 26; dy++) {
			for (int dx = -17; dx <= 17; dx++) {
				for (int dz = -17; dz <= 17; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d <= 17.4 && d >= 15.6) {
						boolean cornice = dy == 19 || dy == 26;
						boolean drumWin = !cornice && (Math.abs(dx) <= 1 || Math.abs(dz) <= 1 || Math.abs(Math.abs(dx) - Math.abs(dz)) <= 1);
						c.set(cx + dx, AY + dy, cz + dz,
							cornice ? OathholdCanvas.TUFF_BRICKS : (drumWin ? glassGold : OathholdCanvas.CALCITE));
					}
				}
			}
		}
		c.ribbedDome(cx, AY + 27, cz, 17, 17, OathholdCanvas.COPPER_OXIDIZED, OathholdCanvas.COPPER_WEATHERED, OathholdCanvas.GOLD);
		c.flagPennant(cx, AY + 51, cz);

		for (int[] t : new int[][]{{52, -84}, {108, -84}, {52, -164}, {108, -164}}) {
			c.bastionTower(t[0], AY, t[1], 4, 21, 9, OathholdCanvas.COPPER_WEATHERED, false);
		}

		c.roseWindowZOpen(cx, AY + 13, -84, -85, 4, glassCyan, glassGold, OathholdCanvas.STONE_BRICKS);
		c.gothicArchZ(cx, AY + 1, -85, -83, 4, 6, 3, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);

		// Cherry Blossom Garden Terrace & Outdoor Bookshelf Terraces (z = -83..-48, x = 56..104)
		c.fill(74, AY, -83, 86, AY, -52, OathholdCanvas.POLISHED_ANDESITE);
		c.fill(56, AY, -79, 72, AY, -51, Blocks.GRASS_BLOCK.defaultBlockState());
		c.fill(88, AY, -79, 104, AY, -51, Blocks.GRASS_BLOCK.defaultBlockState());
		for (int[] ct : new int[][]{{64, -74}, {96, -74}, {64, -56}, {96, -56}, {56, -65}, {104, -65}}) {
			c.tree(ct[0], AY, ct[1], true);
		}
		c.fill(66, AY + 1, -78, 71, AY + 3, -78, Blocks.BOOKSHELF.defaultBlockState());
		c.fill(89, AY + 1, -78, 94, AY + 3, -78, Blocks.BOOKSHELF.defaultBlockState());
		OathholdStreetscape.parkBench(c, 71, AY + 1, -64, Direction.EAST);
		OathholdStreetscape.parkBench(c, 89, AY + 1, -64, Direction.WEST);
		for (int[] lamp : new int[][]{{73, -72}, {87, -72}, {73, -54}, {87, -54}}) {
			OathholdStreetscape.ornateStreetLamp(c, lamp[0], AY + 1, lamp[1], false);
		}
	}

	private static void buildCathedralSpire(OathholdCanvas c) {
		if (!c.intersects(116, -204, 184, -78)) return;

		int cx = 150, cz = -140;
		BlockState glassBlue = Blocks.BLUE_STAINED_GLASS.defaultBlockState();
		BlockState glassLightBlue = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
		BlockState glassPurple = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
		BlockState glassGold = Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
		BlockState glassOrange = Blocks.ORANGE_STAINED_GLASS.defaultBlockState();
		BlockState glassRed = Blocks.RED_STAINED_GLASS.defaultBlockState();

		c.clear(131, AY + 1, -191, 169, AY + 38, -89);
		c.fillWeatheredStone(130, AY + 1, -192, 131, AY + 30, -88);
		c.fillWeatheredStone(169, AY + 1, -192, 170, AY + 30, -88);
		c.fillWeatheredStone(130, AY + 1, -192, 170, AY + 30, -191);
		c.fillWeatheredStone(130, AY + 1, -89, 170, AY + 30, -88);
		// Keep exterior stained-glass perimeter open to natural sky daylight
		c.clear(128, AY + 3, -190, 129, AY + 28, -90);
		c.clear(171, AY + 3, -190, 172, AY + 28, -90);

		// Checkered sanctuary floor with gold-bordered crimson central aisle runner
		for (int x = Math.max(132, c.clipMinLx()); x <= Math.min(168, c.clipMaxLx()); x++) {
			for (int z = Math.max(-191, c.clipMinLz()); z <= Math.min(-89, c.clipMaxLz()); z++) {
				int dx = Math.abs(x - cx);
				if (dx <= 1 && z >= -176 && z <= -90) {
					c.set(x, AY, z, (x == cx && Math.floorMod(z, 4) == 0) ? OathholdCanvas.OCHRE_LIGHT : OathholdCanvas.POLISHED_DEEPSLATE);
					c.set(x, AY + 1, z, Blocks.RED_CARPET.defaultBlockState());
				} else if (dx == 2 && z >= -176 && z <= -90) {
					c.set(x, AY, z, OathholdCanvas.GOLD);
					c.set(x, AY + 1, z, Blocks.YELLOW_CARPET.defaultBlockState());
				} else {
					boolean even = (Math.floorDiv(x, 2) + Math.floorDiv(z, 2)) % 2 == 0;
					c.set(x, AY, z, even ? OathholdCanvas.CALCITE : OathholdCanvas.POLISHED_DEEPSLATE);
				}
			}
		}

		// Twin 3-Aisle Clustered Gothic Arcade Piers, Pointed Side Arches, Emerald-Gold Banners & Vaulted Ceiling Ribs
		int[] pierZs = {-180, -168, -156, -144, -132, -120, -108, -96};
		for (int i = 0; i < pierZs.length; i++) {
			int bz = pierZs[i];
			for (int px : new int[]{135, 165}) {
				int x0 = px == 135 ? 135 : 164;
				int x1 = px == 135 ? 136 : 165;
				c.fillWeatheredStone(x0, AY + 1, bz - 1, x1, AY + 22, bz + 1);
				c.fill(x0, AY + 1, bz - 1, x1, AY + 2, bz + 1, OathholdCanvas.CHISELED_TUFF_BRICKS);
				c.set(px == 135 ? 137 : 163, AY + 4, bz, OathholdCanvas.LANTERN);
				c.set(px, AY + 23, bz, OathholdCanvas.OCHRE_LIGHT);
				c.wallBannerStrip(px == 135 ? 137 : 163, AY + 12, bz, 6, px == 135 ? Direction.EAST : Direction.WEST);
			}
			// Transverse Gothic Stone Vault Ribs curving from arcade piers to central nave apex
			for (int dx = 0; dx <= 15; dx++) {
				int ry = AY + 35 - (int) Math.round(13.0 * Math.pow(dx / 15.0, 1.55));
				c.set(cx - dx, ry, bz, dx == 0 ? OathholdCanvas.GOLD : OathholdCanvas.TUFF_BRICKS);
				c.set(cx + dx, ry, bz, dx == 0 ? OathholdCanvas.GOLD : OathholdCanvas.TUFF_BRICKS);
				c.set(cx - dx, ry - 1, bz, OathholdCanvas.STONE_BRICKS);
				c.set(cx + dx, ry - 1, bz, OathholdCanvas.STONE_BRICKS);
			}
			if (i < pierZs.length - 1) {
				int midZ = (bz + pierZs[i + 1]) / 2;
				c.gothicArchX(135, 136, AY + 12, midZ, 3, 2, 4, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
				c.gothicArchX(164, 165, AY + 12, midZ, 3, 2, 4, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
			}
		}

		// Jewel-Toned Side-Aisle Stained Glass (Cobalt Left / Sunset Gold-Crimson Right) + Clerestory Lancets
		for (int wz = -182; wz <= -98; wz += 12) {
			BlockState westGlass = Math.floorMod(wz, 24) == 0 ? glassBlue : (Math.floorMod(wz, 36) == 0 ? glassPurple : glassLightBlue);
			BlockState eastGlass = Math.floorMod(wz, 24) == 0 ? glassGold : (Math.floorMod(wz, 36) == 0 ? glassRed : glassOrange);
			c.gothicArchX(130, 131, AY + 4, wz, 2, 8, 4, OathholdCanvas.TUFF_BRICKS, westGlass);
			c.gothicArchX(169, 170, AY + 4, wz, 2, 8, 4, OathholdCanvas.TUFF_BRICKS, eastGlass);
			c.gothicArchX(130, 131, AY + 20, wz, 2, 5, 3, OathholdCanvas.STONE_BRICKS, westGlass);
			c.gothicArchX(169, 170, AY + 20, wz, 2, 5, 3, OathholdCanvas.STONE_BRICKS, eastGlass);
			c.set(132, AY + 3, wz - 6, OathholdCanvas.LANTERN);
			c.set(168, AY + 3, wz - 6, OathholdCanvas.LANTERN);
			c.wallBannerStrip(132, AY + 12, wz - 6, 7, Direction.EAST);
			c.wallBannerStrip(168, AY + 12, wz - 6, 7, Direction.WEST);
		}
		// Side-aisle dark-oak confessionals/shrines
		for (int sz : new int[]{-174, -150, -126, -102}) {
			for (int sx : new int[]{132, 167}) {
				c.fill(sx, AY + 1, sz - 1, sx + 1, AY + 3, sz + 1, OathholdCanvas.DARK_OAK);
				c.set(sx == 132 ? 133 : 167, AY + 1, sz, OathholdCanvas.AIR);
				c.set(sx == 132 ? 133 : 167, AY + 2, sz, OathholdCanvas.AIR);
				c.set(sx == 132 ? 133 : 167, AY + 4, sz, OathholdCanvas.LANTERN);
			}
		}

		// 16 Rows of Dark-Oak Cathedral Pews with trapdoor ends flanking both sides (includes x=141, z=-142!)
		BlockState pewStair = OathholdCanvas.stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false);
		BlockState pewEndW = OathholdStreetscape.trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.WEST, true);
		BlockState pewEndE = OathholdStreetscape.trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.EAST, true);
		for (int pz = -172; pz <= -97; pz += 5) {
			c.fill(138, AY + 1, pz, 146, AY + 1, pz, pewStair);
			c.set(137, AY + 1, pz, pewEndW);
			c.set(147, AY + 1, pz, pewEndE);
			c.fill(154, AY + 1, pz, 162, AY + 1, pz, pewStair);
			c.set(153, AY + 1, pz, pewEndW);
			c.set(163, AY + 1, pz, pewEndE);
		}

		// Stone nave vault ceiling & exterior pitched slate roof (cleanly ABOVE AY + 36!)
		for (int x = 132; x <= 168; x++) {
			int dx = Math.abs(x - cx);
			int vaultY = AY + 36 - Math.min(12, dx * 12 / 18);
			c.fill(x, vaultY, -191, x, vaultY, -89, OathholdCanvas.STONE_BRICKS);
			c.fill(x, vaultY + 1, -191, x, AY + 38, -89, OathholdCanvas.TUFF_BRICKS);
		}
		c.pitchedRoofZ(130, AY + 31, -192, 170, -88, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_SLAB, OathholdCanvas.STONE_BRICKS, OathholdCanvas.TUFF_BRICKS);
		c.gothicArchZ(cx, AY + 1, -90, -87, 5, 9, 5, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		c.roseWindowZOpen(cx, AY + 22, -88, -89, 6, glassGold, glassBlue, OathholdCanvas.TUFF_BRICKS);

		c.bastionTower(132, AY, -90, 4, 34, 16, OathholdCanvas.STONE_BRICKS, false);
		c.bastionTower(168, AY, -90, 4, 34, 16, OathholdCanvas.STONE_BRICKS, false);

		// Roof-Mounted Central Openwork Gothic Bell-Spire rising strictly above the AY+36 vault (y = AY+45..104; ly = 61..120)
		int spireBase = AY + 45;
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -6; dz <= 6; dz++) {
				int manhattan = Math.abs(dx) + Math.abs(dz);
				int cheb = Math.max(Math.abs(dx), Math.abs(dz));
				if (cheb > 6 || manhattan > 9) continue;
				boolean isWall = cheb == 6 || manhattan == 9;
				for (int y = spireBase; y <= AY + 78; y++) {
					if (isWall) {
						boolean louvre = (y >= spireBase + 6 && y <= AY + 74 && (y % 7 < 3) && (dx == 0 || dz == 0));
						c.set(cx + dx, y, cz + dz, louvre ? OathholdCanvas.IRON_BARS : OathholdCanvas.STONE_BRICKS);
					}
				}
			}
		}
		for (int dy = 1; dy <= 28; dy++) {
			int sr = Math.max(0, 6 - dy * 6 / 28);
			for (int dx = -sr; dx <= sr; dx++) {
				for (int dz = -sr; dz <= sr; dz++) {
					if (Math.abs(dx) + Math.abs(dz) <= sr + 1) {
						if (dx == 0 && dz == 0 || Math.max(Math.abs(dx), Math.abs(dz)) == sr) {
							c.set(cx + dx, AY + 78 + dy, cz + dz, OathholdCanvas.STONE_BRICKS);
						}
					}
				}
			}
		}
		c.set(cx, AY + 107, cz, OathholdCanvas.GOLD);
		c.flagPennant(cx, AY + 107, cz);
		c.set(cx, AY + 56, cz, OathholdCanvas.CHAIN);
		c.set(cx, AY + 55, cz, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.SOUTH));
		c.set(cx, AY + 68, cz, OathholdCanvas.CHAIN);
		c.set(cx, AY + 67, cz, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.EAST));

		// Pipe Organ Loft over South entrance
		c.fill(142, AY + 14, -91, 158, AY + 16, -90, OathholdCanvas.DARK_OAK);
		for (int ox = 143; ox <= 157; ox += 2) {
			int ph = 17 + (8 - Math.abs(ox - cx));
			c.fill(ox, AY + 17, -91, ox, AY + ph, -91, Math.abs(ox - cx) <= 2 ? OathholdCanvas.GOLD : OathholdCanvas.COPPER_WEATHERED);
		}

		// Stepped Golden High Altar Sanctuary, Reredos Spire, Apse Stained Glass & Four Chandeliers
		c.fill(cx - 5, AY + 1, -187, cx + 5, AY + 1, -177, OathholdCanvas.TUFF_BRICKS);
		c.fill(cx - 4, AY + 2, -186, cx + 4, AY + 2, -179, OathholdCanvas.STONE_BRICKS);
		c.fill(cx - 2, AY + 3, -184, cx + 2, AY + 3, -181, OathholdCanvas.GOLD);
		c.fill(cx - 1, AY + 4, -186, cx + 1, AY + 10, -186, OathholdCanvas.GOLD);
		c.set(cx, AY + 5, -185, OathholdCanvas.OCHRE_LIGHT);
		c.set(cx - 2, AY + 4, -182, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
		c.set(cx + 2, AY + 4, -182, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
		c.set(cx - 3, AY + 3, -182, OathholdCanvas.GOLD);
		c.set(cx - 3, AY + 4, -182, OathholdCanvas.LANTERN);
		c.set(cx + 3, AY + 3, -182, OathholdCanvas.GOLD);
		c.set(cx + 3, AY + 4, -182, OathholdCanvas.LANTERN);
		c.roseWindowZOpen(cx, AY + 20, -191, -192, 5, glassGold, glassBlue, OathholdCanvas.TUFF_BRICKS);
		c.lecternBook(cx - 5, AY + 2, -178, Direction.SOUTH,
			"Liturgy of the High Covenant", "Arch-Prelate",
			List.of("Beneath vaulted stone and sapphire glass, Oathhold keeps the ancient vows unbroken."));
		c.chest(cx + 4, AY + 3, -181, Direction.WEST, OathholdSites.LOOT_TREASURY, 221L);
		for (int chZ : new int[]{-114, -134, -154, -172}) {
			c.chandelier(cx, AY + 34, 11, chZ);
		}
	}
}
