package dev.wildercord.world.sites.oathhold;

import dev.wildercord.aura.world.AuraWorld;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Remaining three crown jewels of the Upper Acropolis (all standing at {@code ly = ACROPOLIS_Y = 16}):
 * <ol>
 *   <li><b>Hall of the Oathbound</b> — fortified crenellated barracks keep &amp; knight training courtyard;</li>
 *   <li><b>Arcanum Grand Library &amp; Observatory</b> — calcite &amp; stone hall with a weathered-copper dome,
 *       multi-colored stained glass, terraced staircases, blossoming cherry trees, and bookshelves;</li>
 *   <li><b>Sovereign Cathedral &amp; Grand Bell-Spire</b> — cruciform gothic cathedral rising 98 blocks skyward.</li>
 * </ol>
 */
public final class OathholdAcropolisHalls {
	private OathholdAcropolisHalls() {}

	private static final int AY = OathholdOrigin.ACROPOLIS_Y;

	public static void build(OathholdCanvas c) {
		buildOathboundBarracks(c);
		buildArcanumLibrary(c);
		buildCathedralSpire(c);
	}

	private static void buildOathboundBarracks(OathholdCanvas c) {
		if (!c.intersects(-178, -194, -48, -58)) return;

		// Fortified Crenellated Castle Barracks Keep: lx [-168..-96], lz [-180..-106]
		int x0 = -168, x1 = -96, z0 = -180, z1 = -106;
		c.fillWeatheredStone(x0, AY + 1, z0, x1, AY + 18, z1);
		c.clear(x0 + 2, AY + 1, z0 + 2, x1 - 2, AY + 17, z1 - 2);
		c.fill(x0 + 2, AY + 9, z0 + 2, x1 - 2, AY + 9, z1 - 2, OathholdCanvas.SPRUCE);
		c.fill(x0 + 1, AY + 18, z0 + 1, x1 - 1, AY + 18, z1 - 1, OathholdCanvas.POLISHED_ANDESITE);

		// Crenellated battlements & banners around keep roof
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
		// Corner Bastion Turrets + Central Tall Command Tower (matches concept_city_districts_courtyard center keep!)
		c.bastionTower(x0, AY, z0, 5, 24, 0, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(x1, AY, z0, 5, 24, 0, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(x0, AY, z1, 5, 24, 0, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(x1, AY, z1, 6, 26, 0, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(-132, AY, -143, 7, 32, 0, OathholdCanvas.DEEPSLATE_TILES, true);

		// East gate arch leading directly out onto Training Courtyard + interior furnishings
		c.gothicArchX(x1 - 2, x1 + 1, AY + 1, -132, 4, 6, 4, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		for (int bx = -160; bx <= -116; bx += 8) {
			c.bed(bx, AY + 1, -172, net.minecraft.world.item.DyeColor.GREEN, Direction.NORTH);
			c.set(bx + 2, AY + 1, -172, Blocks.BARREL.defaultBlockState());
			c.set(bx - 1, AY + 1, -162, Blocks.ANVIL.defaultBlockState());
			c.set(bx, AY + 7, -140, OathholdCanvas.HANGING_LANTERN);
		}
		c.chest(-160, AY + 1, -120, Direction.EAST, OathholdSites.LOOT_BARRACKS, 201L);
		c.chest(-104, AY + 1, -120, Direction.WEST, OathholdSites.LOOT_ARMORY, 202L);
		c.lecternBook(-132, AY + 1, -124, Direction.SOUTH,
			"Chronicles of the Four Orders", "Oathbound Chronicler",
			List.of(
				"Bastions hold the breach, Blade-Champions turn every steel edge, Halberdiers claim the skies, and Banner-Marshals rally the line.",
				"Let no squire cross these gates without mastering poise, guard, and counter."
			));

		// Open-Air Knight Training & Jousting Courtyard: lx [-94..-54], lz [-146..-66]
		for (int px = Math.max(-94, c.clipMinLx()); px <= Math.min(-54, c.clipMaxLx()); px++) {
			if (Math.abs(px + 74) > 5) {
				c.fill(px, AY + 1, -66, px, AY + 2, -66, OathholdCanvas.TUFF_BRICKS);
			}
			c.set(px, AY + 1, -146, OathholdCanvas.TUFF_BRICKS);
			if (Math.floorMod(px, 8) == 0) {
				c.streetLamp(px, AY + 1, -144);
				c.streetLamp(px, AY + 1, -68);
			}
		}
		// Sparring ring + banners + patrolling Oathbound High Knights
		c.fill(-80, AY, -112, -68, AY, -100, OathholdCanvas.POLISHED_ANDESITE);
		c.fill(-76, AY, -108, -72, AY, -104, OathholdCanvas.DEEPSLATE_BRICKS);
		c.spawnMob(-74, AY + 1, -110, AuraWorld.OATHBOUND_BLADE_CHAMPION, 0.0F, true);
		c.spawnMob(-74, AY + 1, -102, AuraWorld.OATHBOUND_BASTION, 180.0F, true);
		c.spawnMob(-84, AY + 1, -92, AuraWorld.OATHBOUND_HALBERDIER, 45.0F, true);
		c.spawnMob(-66, AY + 1, -86, AuraWorld.OATHBOUND_BANNER_MARSHAL, -45.0F, true);
	}

	private static void buildArcanumLibrary(OathholdCanvas c) {
		if (!c.intersects(40, -180, 120, -56)) return;

		int cx = 80, cz = -124;
		// White Calcite & Stone-Brick Collegiate Hall: lx [52..108], lz [-164..-84]
		c.clear(53, AY + 1, -163, 107, AY + 28, -85);
		c.fill(52, AY + 1, -164, 53, AY + 22, -84, OathholdCanvas.CALCITE);
		c.fill(107, AY + 1, -164, 108, AY + 22, -84, OathholdCanvas.CALCITE);
		c.fill(52, AY + 1, -164, 108, AY + 22, -163, OathholdCanvas.CALCITE);
		c.fill(52, AY + 1, -85, 108, AY + 22, -84, OathholdCanvas.CALCITE);

		// Interior library floor, 2nd gallery & bookshelves
		c.fill(54, AY, -162, 106, AY, -86, OathholdCanvas.POLISHED_ANDESITE);
		for (int x = 55; x <= 105; x += 6) {
			if (Math.abs(x - cx) < 6) continue;
			c.fill(x, AY + 1, -158, x, AY + 8, -142, Blocks.BOOKSHELF.defaultBlockState());
			c.fill(x, AY + 1, -108, x, AY + 8, -92, Blocks.CHISELED_BOOKSHELF.defaultBlockState());
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
		c.spawnMob(cx - 4, AY + 1, cz + 6, EntityTypes.VILLAGER, 0.0F, true);
		c.spawnMob(cx + 4, AY + 1, cz - 6, EntityTypes.VILLAGER, 180.0F, true);

		// Steep Dark-Oak Roof + Monumental Weathered-Copper Ribbed Observatory Dome
		c.pitchedRoofZ(52, AY + 23, -164, 108, -84, Blocks.DARK_OAK_STAIRS, Blocks.DARK_OAK_SLAB, OathholdCanvas.CALCITE, OathholdCanvas.TUFF_BRICKS);
		c.fill(cx - 16, AY + 24, cz - 16, cx + 16, AY + 30, cz + 16, OathholdCanvas.STONE_BRICKS);
		c.clear(cx - 14, AY + 25, cz - 14, cx + 14, AY + 29, cz + 14);
		c.ribbedDome(cx, AY + 31, cz, 16, 15, OathholdCanvas.COPPER_WEATHERED, OathholdCanvas.COPPER_OXIDIZED, OathholdCanvas.GOLD);

		// 4 Flanking Calcite Turrets with Dark-Oak Spires (1:1 to concept_city_districts_courtyard!)
		for (int[] t : new int[][]{{52, -84}, {108, -84}, {52, -164}, {108, -164}}) {
			c.bastionTower(t[0], AY, t[1], 5, 25, 11, OathholdCanvas.DARK_OAK, false);
		}

		// South Facade Rose/Lancet Multi-Colored Stained Glass & Terraced Cherry Blossom Garden Stairs
		c.roseWindowZ(cx, AY + 15, -84, 5, Blocks.STAINED_GLASS.cyan().defaultBlockState(), Blocks.STAINED_GLASS.yellow().defaultBlockState(), OathholdCanvas.STONE_BRICKS);
		c.gothicArchZ(cx, AY + 1, -85, -83, 4, 6, 4, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		for (int step = 0; step <= 6; step++) {
			int sz = -82 + step * 2;
			int sy = AY + 1 - Math.max(0, step - 4);
			c.fill(cx - 8, AY, sz, cx + 8, sy, sz + 1, OathholdCanvas.STONE_BRICKS);
		}
		// Blossoming Pink Cherry Trees & Outdoor Bookshelf Terraces (concept_city_districts_courtyard right!)
		c.tree(cx - 14, AY, -76, true);
		c.tree(cx + 14, AY, -76, true);
		c.fill(cx - 22, AY + 1, -78, cx - 18, AY + 3, -78, Blocks.BOOKSHELF.defaultBlockState());
		c.fill(cx + 18, AY + 1, -78, cx + 22, AY + 3, -78, Blocks.BOOKSHELF.defaultBlockState());
	}

	private static void buildCathedralSpire(OathholdCanvas c) {
		if (!c.intersects(116, -204, 184, -78)) return;

		int cx = 150, cz = -140;
		BlockState glassBlue = Blocks.STAINED_GLASS.blue().defaultBlockState();
		BlockState glassGold = Blocks.STAINED_GLASS.yellow().defaultBlockState();

		// Cruciform Cathedral Nave (lx [132..168], lz [-192..-88]) + Transept (lx [124..176], lz [-154..-126])
		c.clear(133, AY + 1, -191, 167, AY + 30, -89);
		c.fillWeatheredStone(132, AY + 1, -192, 133, AY + 28, -88);
		c.fillWeatheredStone(167, AY + 1, -192, 168, AY + 28, -88);
		c.fillWeatheredStone(132, AY + 1, -192, 168, AY + 30, -191);
		c.fillWeatheredStone(132, AY + 1, -89, 168, AY + 30, -88);
		c.fill(133, AY, -191, 167, AY, -89, OathholdCanvas.POLISHED_DEEPSLATE);

		// Steep Gothic Roof + Twin West Facade Spires
		c.pitchedRoofZ(132, AY + 29, -192, 168, -88, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_SLAB, OathholdCanvas.STONE_BRICKS, OathholdCanvas.TUFF_BRICKS);
		c.gothicArchZ(cx, AY + 1, -90, -87, 5, 9, 5, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		c.roseWindowZ(cx, AY + 20, -88, 6, glassGold, glassBlue, OathholdCanvas.TUFF_BRICKS);

		c.bastionTower(134, AY, -90, 4, 34, 16, OathholdCanvas.STONE_BRICKS, false);
		c.bastionTower(166, AY, -90, 4, 34, 16, OathholdCanvas.STONE_BRICKS, false);

		// Soaring Central Openwork Gothic Bell-Spire rising 98 blocks above Acropolis (ly = AY..114!)
		c.bastionTower(cx, AY + 28, cz, 7, 36, 34, OathholdCanvas.STONE_BRICKS, true);
		c.set(cx, AY + 48, cz, OathholdCanvas.CHAIN);
		c.set(cx, AY + 47, cz, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.SOUTH));
		c.set(cx, AY + 56, cz, OathholdCanvas.CHAIN);
		c.set(cx, AY + 55, cz, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.EAST));

		// Interior candlelit gilded sanctuary altar + treasure chest
		c.fill(cx - 4, AY + 1, -184, cx + 4, AY + 1, -176, OathholdCanvas.TUFF_BRICKS);
		c.fill(cx - 2, AY + 2, -182, cx + 2, AY + 2, -180, OathholdCanvas.GOLD);
		c.set(cx - 2, AY + 3, -181, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
		c.set(cx + 2, AY + 3, -181, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, true));
		c.chest(cx + 4, AY + 2, -181, Direction.WEST, OathholdSites.LOOT_TREASURY, 221L);
		c.chandelier(cx, AY + 26, 7, -110);
		c.chandelier(cx, AY + 26, 7, -140);
		c.chandelier(cx, AY + 26, 7, -170);
	}
}
