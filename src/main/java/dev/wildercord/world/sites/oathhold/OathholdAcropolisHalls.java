package dev.wildercord.world.sites.oathhold;

import dev.wildercord.aura.world.AuraWorld;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityTypes;
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
			c.set(-74, AY + 2, jz, (jz & 1) == 0 ? Blocks.WOOL.green().defaultBlockState() : Blocks.WOOL.yellow().defaultBlockState());
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
		c.clear(53, AY + 1, -163, 107, AY + 28, -85);
		c.fill(52, AY + 1, -164, 53, AY + 22, -84, OathholdCanvas.CALCITE);
		c.fill(107, AY + 1, -164, 108, AY + 22, -84, OathholdCanvas.CALCITE);
		c.fill(52, AY + 1, -164, 108, AY + 22, -163, OathholdCanvas.CALCITE);
		c.fill(52, AY + 1, -85, 108, AY + 22, -84, OathholdCanvas.CALCITE);

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
		c.chandelier(cx, AY + 24, 5, cz);
		c.spawnMob(cx - 4, AY + 1, cz + 6, EntityTypes.VILLAGER, 0.0F, true);
		c.spawnMob(cx + 4, AY + 1, cz - 6, EntityTypes.VILLAGER, 180.0F, true);

		c.pitchedRoofZ(52, AY + 23, -164, 108, -84, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, OathholdCanvas.CALCITE, OathholdCanvas.TUFF_BRICKS);
		c.fill(cx - 16, AY + 24, cz - 16, cx + 16, AY + 30, cz + 16, OathholdCanvas.STONE_BRICKS);
		c.clear(cx - 14, AY + 25, cz - 14, cx + 14, AY + 29, cz + 14);
		c.ribbedDome(cx, AY + 31, cz, 16, 15, OathholdCanvas.COPPER_WEATHERED, OathholdCanvas.COPPER_OXIDIZED, OathholdCanvas.GOLD);

		for (int[] t : new int[][]{{52, -84}, {108, -84}, {52, -164}, {108, -164}}) {
			c.bastionTower(t[0], AY, t[1], 5, 25, 11, OathholdCanvas.DARK_OAK, false);
		}

		c.roseWindowZOpen(cx, AY + 15, -84, -85, 5, Blocks.STAINED_GLASS.cyan().defaultBlockState(), Blocks.STAINED_GLASS.yellow().defaultBlockState(), OathholdCanvas.STONE_BRICKS);
		c.gothicArchZ(cx, AY + 1, -85, -83, 4, 6, 4, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);

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
		BlockState glassBlue = Blocks.STAINED_GLASS.blue().defaultBlockState();
		BlockState glassGold = Blocks.STAINED_GLASS.yellow().defaultBlockState();

		c.clear(133, AY + 1, -191, 167, AY + 30, -89);
		c.fillWeatheredStone(132, AY + 1, -192, 133, AY + 28, -88);
		c.fillWeatheredStone(167, AY + 1, -192, 168, AY + 28, -88);
		c.fillWeatheredStone(132, AY + 1, -192, 168, AY + 30, -191);
		c.fillWeatheredStone(132, AY + 1, -89, 168, AY + 30, -88);

		// Checkered sanctuary floor with crimson-and-gold central aisle runner
		for (int x = Math.max(133, c.clipMinLx()); x <= Math.min(167, c.clipMaxLx()); x++) {
			for (int z = Math.max(-191, c.clipMinLz()); z <= Math.min(-89, c.clipMaxLz()); z++) {
				int dx = Math.abs(x - cx);
				if (dx <= 1 && z >= -175 && z <= -91) {
					c.set(x, AY, z, (x == cx && Math.floorMod(z, 4) == 0) ? OathholdCanvas.OCHRE_LIGHT : OathholdCanvas.POLISHED_DEEPSLATE);
					c.set(x, AY + 1, z, Blocks.CARPET.red().defaultBlockState());
				} else if (dx == 2 && z >= -175 && z <= -91) {
					c.set(x, AY, z, OathholdCanvas.GOLD);
				} else {
					boolean even = (Math.floorDiv(x, 2) + Math.floorDiv(z, 2)) % 2 == 0;
					c.set(x, AY, z, even ? OathholdCanvas.CALCITE : OathholdCanvas.POLISHED_DEEPSLATE);
				}
			}
		}

		// Tall Cobalt-Blue & Golden-Amber Gothic Stained-Glass Windows along both side walls
		for (int wz = -180; wz <= -100; wz += 12) {
			BlockState winGlass = Math.floorMod(wz, 24) == 0 ? glassBlue : glassGold;
			c.gothicArchX(132, 133, AY + 4, wz, 2, 10, 4, OathholdCanvas.TUFF_BRICKS, winGlass);
			c.gothicArchX(167, 168, AY + 4, wz, 2, 10, 4, OathholdCanvas.TUFF_BRICKS, winGlass);
			c.wallBannerStrip(134, AY + 14, wz - 6, 7, Direction.EAST);
			c.wallBannerStrip(166, AY + 14, wz - 6, 7, Direction.WEST);
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

		c.pitchedRoofZ(132, AY + 29, -192, 168, -88, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_SLAB, OathholdCanvas.STONE_BRICKS, OathholdCanvas.TUFF_BRICKS);
		c.gothicArchZ(cx, AY + 1, -90, -87, 5, 9, 5, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		c.roseWindowZOpen(cx, AY + 20, -88, -89, 6, glassGold, glassBlue, OathholdCanvas.TUFF_BRICKS);

		c.bastionTower(134, AY, -90, 4, 34, 16, OathholdCanvas.STONE_BRICKS, false);
		c.bastionTower(166, AY, -90, 4, 34, 16, OathholdCanvas.STONE_BRICKS, false);

		// Soaring Central Openwork Gothic Bell-Spire rising 98 blocks above Acropolis (ly = AY..114+)
		c.bastionTower(cx, AY + 28, cz, 7, 36, 34, OathholdCanvas.STONE_BRICKS, true);
		c.set(cx, AY + 48, cz, OathholdCanvas.CHAIN);
		c.set(cx, AY + 47, cz, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.SOUTH));
		c.set(cx, AY + 56, cz, OathholdCanvas.CHAIN);
		c.set(cx, AY + 55, cz, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.EAST));

		// Pipe Organ Loft over South entrance
		c.fill(142, AY + 14, -91, 158, AY + 16, -90, OathholdCanvas.DARK_OAK);
		for (int ox = 143; ox <= 157; ox += 2) {
			int ph = 17 + (8 - Math.abs(ox - cx));
			c.fill(ox, AY + 17, -91, ox, AY + ph, -91, Math.abs(ox - cx) <= 2 ? OathholdCanvas.GOLD : OathholdCanvas.COPPER_WEATHERED);
		}

		// Candlelit gilded sanctuary altar, lectern pulpit, treasury chest & Cleric Villagers
		c.fill(cx - 5, AY + 1, -186, cx + 5, AY + 1, -176, OathholdCanvas.TUFF_BRICKS);
		c.fill(cx - 2, AY + 2, -183, cx + 2, AY + 2, -180, OathholdCanvas.GOLD);
		c.set(cx - 2, AY + 3, -181, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
		c.set(cx + 2, AY + 3, -181, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
		c.lecternBook(cx - 5, AY + 2, -178, Direction.SOUTH,
			"Liturgy of the High Covenant", "Arch-Prelate",
			List.of("Beneath vaulted stone and sapphire glass, Oathhold keeps the ancient vows unbroken."));
		c.chest(cx + 4, AY + 2, -181, Direction.WEST, OathholdSites.LOOT_TREASURY, 221L);
		c.chandelier(cx, AY + 26, 7, -110);
		c.chandelier(cx, AY + 26, 7, -140);
		c.chandelier(cx, AY + 26, 7, -170);
		c.spawnMob(cx - 4, AY + 1, -175, EntityTypes.VILLAGER, 0.0F, true);
		c.spawnMob(cx + 4, AY + 1, -112, EntityTypes.VILLAGER, 180.0F, true);
	}
}
