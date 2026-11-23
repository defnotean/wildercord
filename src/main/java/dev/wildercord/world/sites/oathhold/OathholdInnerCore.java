package dev.wildercord.world.sites.oathhold;

import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.town.BountyBoardBlock;
import dev.wildercord.town.Town;
import dev.wildercord.town.WayfarerKeeper;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Dense 1:1 implementation of the Lower Sovereign City inside the Inner Wall:
 * Landscaped Circular Statue Fountain Plaza with Raised Sidewalks, Central Canal Promenade & Cafes,
 * Vibrant Grand Market Hall & Bazaar, Blacksmiths' Great Forge Yard, Arched Stone Aqueduct,
 * Two-Story Atrium Guildhall, Gilded Lantern Grand Inn + Beer-Garden, and Multi-Story Furnished Shops.
 */
public final class OathholdInnerCore {
	private OathholdInnerCore() {}

	public static void build(OathholdCanvas c) {
		if (!c.intersects(-OathholdOrigin.INNER_WALL_R, -OathholdOrigin.INNER_WALL_R,
			OathholdOrigin.INNER_WALL_R, OathholdOrigin.INNER_WALL_R)) return;

		buildFountainPlaza(c);
		buildCentralCanalAndBridges(c);
		buildWestAqueduct(c);
		buildGrandMarketHall(c);
		buildBazaarSquare(c);
		buildGreatForgeHall(c);
		buildCopperGuildhall(c);
		buildGrandInn(c);
		buildLowerCityTownhouses(c);
	}

	private static void buildFountainPlaza(OathholdCanvas c) {
		int cx = 0, cz = 176;
		if (!c.intersects(cx - 28, cz - 28, cx + 28, cz + 40)) return;

		// Processional carriageway (y=0) flanked by raised sidewalks (y=1) skirting the central Statue Fountain
		for (int x = Math.max(-24, c.clipMinLx()); x <= Math.min(24, c.clipMaxLx()); x++) {
			for (int z = Math.max(149, c.clipMinLz()); z <= Math.min(213, c.clipMaxLz()); z++) {
				double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
				if (d <= 11.2) {
					if (d >= 9.6) {
						c.set(x, 1, z, OathholdCanvas.TUFF_BRICKS);
						c.set(x, 2, z, OathholdCanvas.slab(Blocks.STONE_BRICK_SLAB, false));
					} else if (d <= 2.6) {
						c.set(x, 1, z, OathholdCanvas.STONE_BRICKS);
						c.set(x, 2, z, OathholdCanvas.TUFF_BRICKS);
					} else {
						boolean glow = Math.floorMod(x, 3) == 0 && Math.floorMod(z, 3) == 0;
						c.set(x, 0, z, glow ? OathholdCanvas.SEA_LANTERN : OathholdCanvas.STONE_BRICKS);
						c.set(x, 1, z, OathholdCanvas.WATER);
					}
				} else if (Math.abs(x) >= 12 && z <= 208) {
					c.set(x, 1, z, ((x + z) & 1) == 0 ? OathholdCanvas.STONE_BRICKS : OathholdCanvas.POLISHED_ANDESITE);
				} else if (Math.abs(x) == 11 && z <= 208 && z >= 152) {
					c.set(x, 1, z, OathholdCanvas.stair(Blocks.STONE_BRICK_STAIRS, x < 0 ? Direction.WEST : Direction.EAST, false));
				} else if (d <= 22.0 || Math.abs(x) <= 10) {
					c.set(x, 0, z, Math.floorMod(Math.round(d), 4) == 0 ? OathholdCanvas.TUFF_BRICKS : OathholdCanvas.POLISHED_ANDESITE);
				}
			}
		}

		// Central Sculpted Hero Knight Fountain Monument
		c.fill(cx - 1, 1, cz - 1, cx + 1, 4, cz + 1, OathholdCanvas.STONE_BRICKS);
		OathholdStreetscape.knightMonument(c, cx, 5, cz);

		// Corner boxwood hedges, ornate lamps, benches, stalls & supply wagons
		OathholdStreetscape.hedgePlanter(c, -23, 2, 152, -17, 158);
		OathholdStreetscape.hedgePlanter(c, 17, 2, 152, 23, 158);
		OathholdStreetscape.hedgePlanter(c, -23, 2, 194, -17, 200);
		OathholdStreetscape.hedgePlanter(c, 17, 2, 194, 23, 200);
		for (int[] l : new int[][]{{-13, 162}, {13, 162}, {-13, 190}, {13, 190}, {-14, 176}, {14, 176}, {-13, 206}, {13, 206}}) {
			OathholdStreetscape.ornateStreetLamp(c, l[0], 2, l[1], false);
		}
		OathholdStreetscape.parkBench(c, -13, 2, 170, Direction.EAST);
		OathholdStreetscape.parkBench(c, 13, 2, 170, Direction.WEST);
		OathholdStreetscape.parkBench(c, -13, 2, 182, Direction.EAST);
		OathholdStreetscape.parkBench(c, 13, 2, 182, Direction.WEST);

		OathholdStreetscape.marketStall(c, -20, 2, 176, Blocks.RED_WOOL.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState(), 1);
		OathholdStreetscape.marketStall(c, 20, 2, 176, Blocks.BLUE_WOOL.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState(), 2);
		OathholdStreetscape.merchantWagon(c, -6, 1, 200, true, 3);
		OathholdStreetscape.merchantWagon(c, 6, 1, 156, true, 4);

		c.spawnMob(-7, 1, 160, AuraWorld.OATHBOUND_HALBERDIER, 45.0F, true);
		c.spawnMob(7, 1, 194, AuraWorld.OATHBOUND_BLADE_CHAMPION, -135.0F, true);
	}

	private static void buildCentralCanalAndBridges(OathholdCanvas c) {
		if (!c.intersects(-24, -44, 24, 150)) return;

		for (int z = Math.max(-42, c.clipMinLz()); z <= Math.min(148, c.clipMaxLz()); z++) {
			c.fill(-11, -3, z, -10, 1, z, OathholdCanvas.STONE_BRICKS);
			c.fill(10, -3, z, 11, 1, z, OathholdCanvas.STONE_BRICKS);
			boolean bridgeGap = Math.abs(z - 116) <= 4 || Math.abs(z - 56) <= 4 || Math.abs(z + 4) <= 4;
			if (!bridgeGap) {
				if (Math.floorMod(z, 8) == 0) {
					OathholdStreetscape.chainRailingBollard(c, -10, 2, z, Math.floorMod(z, 16) == 0);
					OathholdStreetscape.chainRailingBollard(c, 10, 2, z, Math.floorMod(z, 16) == 0);
				} else {
					c.set(-10, 2, z, OathholdStreetscape.horizontalChain(true));
					c.set(10, 2, z, OathholdStreetscape.horizontalChain(true));
				}
			}
		}

		// Three Arched Stone Footbridges across the Central Canal
		for (int bz : new int[]{116, 56, -4}) {
			buildCanalBridge(c, bz);
		}

		// Five Masted Wooden Canal Cargo Barges moored in water (leaving z = 80 clear open water)
		for (int[] boat : new int[][]{{0, 134}, {-3, 98}, {3, 72}, {0, 34}, {0, -22}}) {
			OathholdStreetscape.canalBarge(c, boat[0], -1, boat[1], true);
		}

		// Canal-Side Promenade Cafe Patios, Benches, Planters & Villagers
		BlockState redCarpet = Blocks.RED_CARPET.defaultBlockState();
		BlockState cyanCarpet = Blocks.CYAN_CARPET.defaultBlockState();
		BlockState greenCarpet = Blocks.GREEN_CARPET.defaultBlockState();
		BlockState whiteWool = Blocks.WHITE_WOOL.defaultBlockState();
		for (int cz : new int[]{138, 102, 74, 40, 12}) {
			OathholdStreetscape.patioTable(c, -15, 1, cz, cz % 2 == 0 ? redCarpet : greenCarpet, whiteWool);
			OathholdStreetscape.patioTable(c, 15, 1, cz, cz % 2 == 0 ? cyanCarpet : redCarpet, whiteWool);
		}
		for (int tz : new int[]{128, 92, 66, 30, -16}) {
			OathholdStreetscape.treePlanter(c, -19, 0, tz, tz > 60);
			OathholdStreetscape.treePlanter(c, 19, 0, tz, tz <= 60);
			OathholdStreetscape.parkBench(c, -13, 1, tz - 2, Direction.EAST);
			OathholdStreetscape.parkBench(c, 13, 1, tz - 2, Direction.WEST);
		}
	}

	/** Arched stone footbridge over the Central Canal centered at local Z = {@code bz}. */
	public static void buildCanalBridge(OathholdCanvas c, int bz) {
		if (!c.intersects(-16, bz - 5, 16, bz + 5)) return;
		for (int x = -13; x <= 13; x++) {
			int archLift = Math.max(0, 3 - (x * x) / 40);
			c.fill(x, archLift, bz - 3, x, archLift, bz + 3, OathholdCanvas.POLISHED_ANDESITE);
			c.fill(x, archLift - 1, bz - 3, x, archLift - 1, bz + 3, OathholdCanvas.STONE_BRICKS);
			c.set(x, archLift + 1, bz - 3, OathholdCanvas.TUFF_BRICKS);
			c.set(x, archLift + 1, bz + 3, OathholdCanvas.TUFF_BRICKS);
			if (Math.abs(x) == 9) {
				c.set(x, archLift + 2, bz - 3, OathholdCanvas.LANTERN);
				c.set(x, archLift + 2, bz + 3, OathholdCanvas.LANTERN);
			}
		}
		c.fill(-12, -4, bz - 3, -11, 1, bz + 3, OathholdCanvas.STONE_BRICKS);
		c.fill(11, -4, bz - 3, 12, 1, bz + 3, OathholdCanvas.STONE_BRICKS);
	}

	private static void buildWestAqueduct(OathholdCanvas c) {
		if (!c.intersects(-216, -34, -120, 34)) return;
		for (int x = Math.max(-212, c.clipMinLx()); x <= Math.min(-126, c.clipMaxLx()); x++) {
			int mod = Math.floorMod(x, 10);
			boolean pier = mod <= 2;
			if (pier) {
				c.fillWeatheredStone(x, 1, 16, x, 13, 24);
			} else {
				int archBase = 10 - Math.max(0, 4 - Math.abs(mod - 6));
				c.fillWeatheredStone(x, archBase, 16, x, 13, 24);
			}
			c.fill(x, 14, 16, x, 15, 17, OathholdCanvas.STONE_BRICKS);
			c.fill(x, 14, 23, x, 15, 24, OathholdCanvas.STONE_BRICKS);
			c.fill(x, 14, 18, x, 14, 22, OathholdCanvas.MOSSY_STONE_BRICKS);
			c.fill(x, 15, 18, x, 15, 22, OathholdCanvas.WATER);
		}
		for (int z = Math.max(-28, c.clipMinLz()); z <= Math.min(22, c.clipMaxLz()); z++) {
			int mod = Math.floorMod(z, 10);
			boolean pier = mod <= 2;
			if (pier) {
				c.fillWeatheredStone(-202, 1, z, -194, 13, z);
			} else {
				int archBase = 10 - Math.max(0, 4 - Math.abs(mod - 6));
				c.fillWeatheredStone(-202, archBase, z, -194, 13, z);
			}
			c.fill(-202, 14, z, -201, 15, z, OathholdCanvas.STONE_BRICKS);
			c.fill(-195, 14, z, -194, 15, z, OathholdCanvas.STONE_BRICKS);
			c.fill(-200, 14, z, -196, 14, z, OathholdCanvas.MOSSY_STONE_BRICKS);
			c.fill(-200, 15, z, -196, 15, z, OathholdCanvas.WATER);
		}
		// Shaded arcade stalls, benches & greenery under and alongside the Aqueduct arches
		OathholdStreetscape.marketStall(c, -184, 1, 20, Blocks.CYAN_WOOL.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState(), 11);
		OathholdStreetscape.marketStall(c, -154, 1, 20, Blocks.ORANGE_WOOL.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState(), 12);
		OathholdStreetscape.parkBench(c, -168, 1, 14, Direction.NORTH);
		OathholdStreetscape.cargoCluster(c, -142, 1, 13, 13);
		OathholdStreetscape.hedgePlanter(c, -190, 0, 27, -172, 31);
	}

	private static void buildGrandMarketHall(OathholdCanvas c) {
		int x0 = -114, x1 = -56, z0 = 46, z1 = 86;
		if (!c.intersects(x0 - 4, z0 - 4, x1 + 4, z1 + 4)) return;

		c.clear(x0 + 1, 1, z0 + 1, x1 - 1, 18, z1 - 1);
		c.fill(x0, 0, z0, x1, 0, z1, OathholdCanvas.POLISHED_ANDESITE);
		c.fillWeatheredStone(x0, 1, z0, x1, 8, z0 + 1);
		c.fillWeatheredStone(x0, 1, z1 - 1, x1, 8, z1);
		c.fillWeatheredStone(x0, 1, z0, x0 + 1, 8, z1);
		c.fillWeatheredStone(x1 - 1, 1, z0, x1, 8, z1);
		for (int gx = x0 + 8; gx <= x1 - 8; gx += 10) {
			c.gothicArchZ(gx, 1, z0, z0 + 1, 3, 4, 2, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
			c.gothicArchZ(gx, 1, z1 - 1, z1, 3, 4, 2, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		}
		for (int gz = z0 + 8; gz <= z1 - 8; gz += 10) {
			c.gothicArchX(x0, x0 + 1, 1, gz, 3, 4, 2, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
			c.gothicArchX(x1 - 1, x1, 1, gz, 3, 4, 2, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		}

		c.fill(x0, 9, z0, x1, 14, z0, OathholdCanvas.CALCITE);
		c.fill(x0, 9, z1, x1, 14, z1, OathholdCanvas.CALCITE);
		c.fill(x0, 9, z0, x0, 14, z1, OathholdCanvas.CALCITE);
		c.fill(x1, 9, z0, x1, 14, z1, OathholdCanvas.CALCITE);
		for (int x = x0; x <= x1; x += 6) {
			c.fill(x, 9, z0, x, 14, z0, OathholdCanvas.DARK_OAK_LOG);
			c.fill(x, 9, z1, x, 14, z1, OathholdCanvas.DARK_OAK_LOG);
			if (x > x0 && x < x1) {
				c.set(x + 3, 11, z0, Blocks.GLASS_PANE.defaultBlockState());
				c.set(x + 3, 11, z1, Blocks.GLASS_PANE.defaultBlockState());
				c.set(x + 3, 7, z1 + 1, OathholdCanvas.HANGING_LANTERN);
			}
		}
		c.pitchedRoofX(x0, 15, z0, x1, z1, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, OathholdCanvas.CALCITE, OathholdCanvas.DARK_OAK);

		for (int mx = x0 + 6; mx <= x1 - 6; mx += 12) {
			c.fill(mx - 2, 1, 64, mx + 2, 1, 68, OathholdCanvas.SPRUCE);
			c.set(mx, 2, 65, OathholdCanvas.LANTERN);
			c.set(mx - 1, 1, 69, Blocks.BARREL.defaultBlockState());
			c.chest(mx + 1, 2, 66, Direction.SOUTH, OathholdSites.LOOT_MARKET, mx * 7L);
			c.set(mx, 8, 66, OathholdCanvas.HANGING_LANTERN);
		}
	}

	private static void buildBazaarSquare(OathholdCanvas c) {
		if (!c.intersects(-124, 88, -36, 154)) return;
		BlockState[] awnings = {
			Blocks.RED_WOOL.defaultBlockState(),
			Blocks.BLUE_WOOL.defaultBlockState(),
			Blocks.LIME_WOOL.defaultBlockState(),
			Blocks.ORANGE_WOOL.defaultBlockState(),
			Blocks.YELLOW_WOOL.defaultBlockState(),
			Blocks.WHITE_WOOL.defaultBlockState()
		};
		int idx = 0;
		for (int sz = 96; sz <= 140; sz += 14) {
			for (int sx = -110; sx <= -52; sx += 14) {
				BlockState roofA = awnings[idx % awnings.length];
				BlockState roofB = awnings[(idx + 1) % awnings.length];
				idx++;
				c.fill(sx - 3, 1, sz - 2, sx + 3, 1, sz + 2, OathholdCanvas.AIR);
				c.fill(sx - 2, 1, sz - 1, sx + 2, 1, sz - 1, OathholdCanvas.SPRUCE);
				for (int corner : new int[]{-2, 2}) {
					c.fill(sx + corner, 1, sz - 2, sx + corner, 3, sz - 2, Blocks.OAK_FENCE.defaultBlockState());
					c.fill(sx + corner, 1, sz + 2, sx + corner, 3, sz + 2, Blocks.OAK_FENCE.defaultBlockState());
				}
				for (int ax = sx - 2; ax <= sx + 2; ax++) {
					BlockState stripe = ((ax + sz) & 1) == 0 ? roofA : roofB;
					c.fill(ax, 4, sz - 2, ax, 4, sz + 2, stripe);
				}
				c.set(sx + 2, 2, sz - 1, Blocks.BARREL.defaultBlockState());
				c.set(sx - 1, 2, sz - 1, (idx & 1) == 0 ? Blocks.LOOM.defaultBlockState() : Blocks.CRAFTING_TABLE.defaultBlockState());
				c.set(sx - 2, 3, sz + 1, OathholdCanvas.HANGING_LANTERN);
			}
		}
		// Bazaar wagons, crate clusters, outdoor cafe tables & perimeter streetlamps
		OathholdStreetscape.merchantWagon(c, -80, 1, 132, false, 21);
		OathholdStreetscape.merchantWagon(c, -96, 1, 104, false, 22);
		OathholdStreetscape.cargoCluster(c, -44, 1, 96, 23);
		OathholdStreetscape.cargoCluster(c, -118, 1, 118, 24);
		OathholdStreetscape.patioTable(c, -44, 1, 108, Blocks.ORANGE_CARPET.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState());
		OathholdStreetscape.patioTable(c, -44, 1, 134, Blocks.RED_CARPET.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState());
		for (int lx : new int[]{-116, -80, -44}) {
			OathholdStreetscape.ornateStreetLamp(c, lx, 1, 90, false);
			OathholdStreetscape.ornateStreetLamp(c, lx, 1, 148, false);
		}
	}

	private static void buildGreatForgeHall(OathholdCanvas c) {
		int x0 = -186, x1 = -138, z0 = 54, z1 = 92;
		if (!c.intersects(x0 - 6, 32, x1 + 16, z1 + 6)) return;

		c.fillWeatheredStone(x0, 1, z0, x1, 10, z1);
		c.clear(x0 + 2, 1, z0 + 2, x1 - 2, 9, z1 - 2);
		c.gothicArchX(x1 - 2, x1 + 1, 1, 73, 4, 4, 2, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		c.pitchedRoofX(x0, 11, z0, x1, z1, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, OathholdCanvas.BRICKS, OathholdCanvas.DARK_OAK);

		for (int cx : new int[]{-174, -154}) {
			c.fill(cx - 2, 1, 60, cx + 2, 30, 64, OathholdCanvas.BRICKS);
			c.fill(cx - 1, 2, 61, cx + 1, 31, 63, OathholdCanvas.BRICKS);
			c.set(cx, 32, 62, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
			c.clear(cx - 1, 1, 63, cx + 1, 4, 65);
			c.set(cx, 1, 63, Blocks.MAGMA_BLOCK.defaultBlockState());
			c.set(cx, 2, 63, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
			c.set(cx - 3, 1, 68, Blocks.BLAST_FURNACE.defaultBlockState());
			c.set(cx + 3, 1, 68, Blocks.ANVIL.defaultBlockState());
		}
		c.fill(-168, 1, 78, -160, 1, 82, OathholdCanvas.TUFF_BRICKS);
		c.fill(-167, 1, 79, -161, 1, 81, Blocks.LAVA.defaultBlockState());
		c.set(-152, 1, 84, Blocks.GRINDSTONE.defaultBlockState());
		c.chest(-144, 1, 60, Direction.WEST, OathholdSites.LOOT_ARMORY, 311L);

		// Open-Air Blacksmiths' Yard (z = 36..52): quenching basin, anvils, ore piles, wagon & armorer
		c.fill(-176, 1, 38, -170, 1, 42, OathholdCanvas.TUFF_BRICKS);
		c.fill(-175, 1, 39, -171, 1, 41, OathholdCanvas.WATER);
		for (int ax : new int[]{-164, -156, -148}) {
			c.set(ax, 1, 44, Blocks.ANVIL.defaultBlockState());
			c.set(ax + 2, 1, 44, Blocks.SMITHING_TABLE.defaultBlockState());
		}
		c.fill(-182, 1, 38, -179, 2, 41, Blocks.RAW_IRON_BLOCK.defaultBlockState());
		c.fill(-164, 1, 36, -160, 2, 38, Blocks.COAL_BLOCK.defaultBlockState());
		OathholdStreetscape.merchantWagon(c, -132, 1, 46, true, 31);
		OathholdStreetscape.ornateStreetLamp(c, -132, 1, 56, false);
		OathholdStreetscape.ornateStreetLamp(c, -132, 1, 88, false);
	}

	private static void buildCopperGuildhall(OathholdCanvas c) {
		int x0 = 116, x1 = 156, z0 = 44, z1 = 82;
		if (!c.intersects(x0 - 4, z0 - 4, x1 + 4, z1 + 4)) return;

		c.clear(x0 + 1, 1, z0 + 1, x1 - 1, 21, z1 - 1);
		c.fillWeatheredStone(x0, 1, z0, x1, 9, z1);
		c.clear(x0 + 2, 1, z0 + 2, x1 - 2, 8, z1 - 2);
		c.fill(x0 + 1, 10, z0 + 1, x1 - 1, 18, z1 - 1, OathholdCanvas.CALCITE);
		c.clear(x0 + 2, 10, z0 + 2, x1 - 2, 17, z1 - 2);
		c.fill(x0 + 2, 10, z0 + 2, x1 - 2, 10, z1 - 2, OathholdCanvas.DARK_OAK);
		c.gothicArchX(x0 - 1, x0 + 2, 1, 63, 3, 4, 2, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);

		// Checkered ground hall floor & crimson guild runner
		for (int x = Math.max(x0 + 2, c.clipMinLx()); x <= Math.min(x1 - 2, c.clipMaxLx()); x++) {
			for (int z = Math.max(z0 + 2, c.clipMinLz()); z <= Math.min(z1 - 2, c.clipMaxLz()); z++) {
				if (z >= 61 && z <= 65) {
					c.set(x, 0, z, OathholdCanvas.POLISHED_DEEPSLATE);
					c.set(x, 1, z, Blocks.RED_CARPET.defaultBlockState());
				} else {
					boolean even = ((x + z) & 1) == 0;
					c.set(x, 0, z, even ? OathholdCanvas.CALCITE : OathholdCanvas.POLISHED_DEEPSLATE);
				}
			}
		}

		// Central open atrium looking up through 2nd floor gallery (x=128..146, z=56..70)
		c.clear(128, 10, 56, 146, 17, 70);
		c.fill(127, 11, 55, 147, 11, 55, OathholdCanvas.TUFF_BRICKS);
		c.fill(127, 11, 71, 147, 11, 71, OathholdCanvas.TUFF_BRICKS);
		c.fill(127, 11, 55, 127, 11, 71, OathholdCanvas.TUFF_BRICKS);
		c.fill(147, 11, 55, 147, 11, 71, OathholdCanvas.TUFF_BRICKS);

		// Internal Spruce Staircase climbing North to the 2nd floor gallery (x = 120, z = 48..57)
		for (int step = 0; step <= 9; step++) {
			int sz = 57 - step;
			int sy = 1 + step;
			c.fill(119, 1, sz, 121, sy - 1, sz, OathholdCanvas.STONE_BRICKS);
			c.fill(119, sy, sz, 121, sy, sz, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
			c.clear(119, sy + 1, sz, 121, 15, sz);
		}

		// Ground guild counters, bookshelves, mezzanine study desks & 2nd floor windows
		for (int gx = 126; gx <= 146; gx += 10) {
			c.fill(gx - 2, 1, 51, gx + 2, 1, 51, OathholdCanvas.DARK_OAK);
			c.fill(gx - 2, 1, 75, gx + 2, 1, 75, OathholdCanvas.DARK_OAK);
			c.set(gx, 2, 51, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
			c.set(gx, 2, 75, OathholdCanvas.LANTERN);
		}
		c.fill(152, 1, 48, 153, 5, 78, Blocks.BOOKSHELF.defaultBlockState());
		c.lecternBook(150, 1, 63, Direction.WEST,
			"Guild Ledger of Oathhold", "Grand Bursar",
			List.of("Every merchant ship docking at East Harbor registers bullion and cloth with the Exchange."));
		for (int sx = 124; sx <= 148; sx += 12) {
			OathholdStreetscape.windowWithShuttersAndBox(c, sx, 13, z0, Direction.NORTH, sx);
			OathholdStreetscape.windowWithShuttersAndBox(c, sx, 13, z1, Direction.SOUTH, sx + 1);
			c.set(sx, 11, 49, Blocks.OAK_FENCE.defaultBlockState());
			c.set(sx, 12, 49, Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState());
			c.set(sx - 1, 11, 49, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
		}

		c.pitchedRoofZ(x0 + 1, 19, z0 + 1, x1 - 1, z1 - 1,
			Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB,
			OathholdCanvas.CALCITE, OathholdCanvas.COPPER_OXIDIZED);
		c.bastionTower(x0 + 4, 0, z0 + 4, 4, 24, 12, OathholdCanvas.COPPER_WEATHERED, false);
		c.bastionTower(x1 - 4, 0, z0 + 4, 4, 24, 12, OathholdCanvas.COPPER_WEATHERED, false);
		c.chest(148, 1, 76, Direction.NORTH, OathholdSites.LOOT_TREASURY, 321L);
		c.chandelier(136, 17, 4, 63);
	}

	private static void buildGrandInn(OathholdCanvas c) {
		int x0 = 36, x1 = 74, z0 = 96, z1 = 130;
		if (!c.intersects(x0 - 4, z0 - 4, x1 + 4, 152)) return;

		OathholdBuildings.build(c, 0, x0, z0, x1, z1, 3, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB,
			false, OathholdBuildings.Theme.CITADEL, true, 401);

		// Ground Tavern hearth, bar counter, timber pillars, chandeliers, common tables, Bounty Board & Wayfarer Keeper
		c.fill(44, 1, 104, 48, 3, 104, OathholdCanvas.BRICKS);
		c.set(46, 1, 104, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		c.fill(40, 1, 121, 48, 1, 121, OathholdCanvas.DARK_OAK);
		c.set(41, 2, 121, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
		c.set(46, 2, 121, OathholdCanvas.LANTERN);
		for (int px : new int[]{48, 62}) {
			c.fill(px, 1, 108, px, 4, 108, OathholdCanvas.DARK_OAK_LOG);
			c.fill(px, 1, 118, px, 4, 118, OathholdCanvas.DARK_OAK_LOG);
		}
		c.chandelier(55, 5, 1, 114);
		c.chandelier(64, 5, 1, 114);
		c.fill(52, 1, 108, 60, 1, 112, OathholdCanvas.DARK_OAK);
		c.set(54, 2, 110, OathholdCanvas.LANTERN);
		c.set(58, 2, 110, OathholdCanvas.LANTERN);
		c.fill(52, 1, 118, 62, 1, 121, OathholdCanvas.SPRUCE);
		c.set(54, 2, 119, OathholdCanvas.LANTERN);
		c.set(60, 2, 120, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true));

		// Furnished 2nd-Story Inn Lounge & Guest Suites (Floor deck at y = 5, furnishings at y = 6)
		for (int gx = 42; gx <= 66; gx += 12) {
			c.fill(gx, 6, 104, gx + 4, 6, 107, OathholdCanvas.SPRUCE);
			c.set(gx + 2, 7, 106, OathholdCanvas.LANTERN);
			c.set(gx - 1, 6, 106, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
			c.set(gx + 5, 6, 106, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
			c.bed(gx, 6, 120, DyeColor.RED, Direction.EAST);
		}
		// Ensure exact Floor 2 Inn lounge furnishing at (ox+44, by+6, oz+106)
		c.fill(43, 6, 105, 45, 6, 107, OathholdCanvas.DARK_OAK);
		c.set(44, 7, 106, OathholdCanvas.LANTERN);

		// Enclosed Tavern Beer-Garden Patio south of Grand Inn (z = 132..148)
		OathholdStreetscape.hedgePlanter(c, 36, 0, 132, 42, 148);
		OathholdStreetscape.hedgePlanter(c, 68, 0, 132, 74, 148);
		for (int px : new int[]{48, 56, 63}) {
			OathholdStreetscape.patioTable(c, px, 1, 138, Blocks.RED_CARPET.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState());
			OathholdStreetscape.patioTable(c, px, 1, 145, Blocks.GREEN_CARPET.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState());
			c.set(px, 5, 141, OathholdStreetscape.horizontalChain(false));
		}
		c.set(54, 1, 132, Town.BOUNTY_BOARD.defaultBlockState().setValue(BountyBoardBlock.FACING, Direction.SOUTH));
		WayfarerKeeper cook = c.spawnMob(50, 1, 118, Town.KEEPER, 0.0F, true);
		if (cook != null) cook.setRole(WayfarerKeeper.Role.COOK);
	}

	private static void buildLowerCityTownhouses(OathholdCanvas c) {
		for (int gx = -192; gx <= 160; gx += 32) {
			for (int gz = -32; gz <= 160; gz += 32) {
				int x0 = gx + 3, x1 = gx + 29, z0 = gz + 3, z1 = gz + 29;
				if (!c.intersects(x0 - 3, z0 - 3, x1 + 3, z1 + 3)) continue;

				if (gx == 32 && gz == 64) {
					// Showcase Canal-Side Curio & Armor Shop-House with double-height mezzanine loft
					OathholdBuildings.build(c, 0, 35, 67, 55, 89, 2, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB,
						false, OathholdBuildings.Theme.CITADEL, false, 777);
					OathholdStreetscape.hedgePlanter(c, 57, 0, 68, 61, 88);
					OathholdStreetscape.patioTable(c, 58, 1, 78, Blocks.RED_CARPET.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState());
					if (c.inClip(52, 1, 82)) {
						ArmorStand stand = EntityType.ARMOR_STAND.create(c.level().getLevel(), EntitySpawnReason.STRUCTURE);
						if (stand != null) {
							stand.snapTo(c.wx(52) + 0.5, c.wy(1), c.wz(82) + 0.5, 125.0F, 0.0F);
							stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
							stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
							stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
							stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
							c.level().addFreshEntityWithPassengers(stand);
						}
					}
					continue;
				}
				if (overlapsInnerLandmark(x0, z0, x1, z1)) continue;

				int h = OathholdCanvas.hash(gx, 11, gz);
				OathholdStreetscape.ornateStreetLamp(c, gx + 16, 1, gz + 1, false);
				OathholdStreetscape.parkBench(c, gx + 12, 1, gz + 1, Direction.SOUTH);
				OathholdStreetscape.cargoCluster(c, gx + 20, 1, gz + 1, h);

				if (h < 20) {
					OathholdStreetscape.treePlanter(c, gx + 16, 0, gz + 16, h < 10);
					c.fill(gx + 14, 1, gz + 8, gx + 18, 1, gz + 10, OathholdCanvas.STONE_BRICKS);
					c.set(gx + 16, 1, gz + 9, OathholdCanvas.WATER);
					OathholdStreetscape.patioTable(c, gx + 9, 1, gz + 16,
						Blocks.YELLOW_CARPET.defaultBlockState(), Blocks.WHITE_WOOL.defaultBlockState());
					OathholdStreetscape.parkBench(c, gx + 22, 1, gz + 16, Direction.WEST);
				} else {
					buildMedievalBuilding(c, x0, z0, gx + 14, gz + 14, 2 + (h % 2),
						h < 50 ? Blocks.SPRUCE_STAIRS : Blocks.DEEPSLATE_TILE_STAIRS,
						h < 50 ? Blocks.SPRUCE_SLAB : Blocks.DEEPSLATE_TILE_SLAB,
						h % 7 == 0, gx * 31 + gz);
					buildMedievalBuilding(c, gx + 17, z0, x1, gz + 14, 2 + ((h + 1) % 2),
						h < 65 ? Blocks.DEEPSLATE_TILE_STAIRS : Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS,
						h < 65 ? Blocks.DEEPSLATE_TILE_SLAB : Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB,
						false, gx * 37 + gz);
					buildMedievalBuilding(c, x0, gz + 17, x1, z1, 2 + ((h + 2) % 2),
						Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, h % 5 == 0, gx * 41 + gz);
				}
			}
		}
	}

	public static boolean overlapsInnerLandmark(int x0, int z0, int x1, int z1) {
		return OathholdOrigin.overlapsInnerLandmark(x0, z0, x1, z1);
	}

	/** Reusable detailed half-timbered stone-and-calcite medieval shop-house with furnished interior. */
	public static void buildMedievalBuilding(OathholdCanvas c, int x0, int z0, int x1, int z1, int stories,
			Block roofStair, Block roofSlab, boolean copperAccent, int seed) {
		OathholdBuildings.build(c, 0, x0, z0, x1, z1, stories, roofStair, roofSlab, copperAccent,
			OathholdBuildings.Theme.CITADEL, (seed & 1) == 0, seed);
	}
}
