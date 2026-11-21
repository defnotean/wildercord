package dev.wildercord.world.sites.oathhold;

import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.town.BountyBoardBlock;
import dev.wildercord.town.Town;
import dev.wildercord.town.WayfarerKeeper;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dense 1:1 implementation of the Lower Sovereign City inside the Inner Wall:
 * Circular Statue Fountain Plaza, Central Grand Canal with 3 Arched Stone Bridges & Canal Skiffs,
 * Grand Market Hall & Vibrant Cloth Bazaar Square, Smoking Great Forge Hall, Arched Stone Aqueduct,
 * Copper-Roofed Twin-Spire Guildhall, Gilded Lantern Grand Inn, and Multi-Story Medieval Townhouses.
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
		if (!c.intersects(cx - 26, cz - 26, cx + 26, cz + 38)) return;

		// Processional avenue links South Gatehouse (z=214) to Fountain Plaza to South Canal Quay (z=149)
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
						c.set(x, 0, z, OathholdCanvas.STONE_BRICKS);
						c.set(x, 1, z, OathholdCanvas.WATER);
					}
				} else if (d <= 22.0 || Math.abs(x) <= 9) {
					c.set(x, 0, z, Math.floorMod(Math.round(d), 4) == 0 ? OathholdCanvas.TUFF_BRICKS : OathholdCanvas.POLISHED_ANDESITE);
				}
			}
		}

		// Central Gilded Knight Statue Pedestal & Jetting Water
		c.fill(cx - 1, 1, cz - 1, cx + 1, 5, cz + 1, OathholdCanvas.STONE_BRICKS);
		c.fill(cx - 1, 6, cz - 1, cx + 1, 9, cz + 1, OathholdCanvas.TUFF_BRICKS);
		c.set(cx, 10, cz, OathholdCanvas.CHISELED_STONE_BRICKS);
		c.set(cx, 11, cz, OathholdCanvas.GOLD);
		c.set(cx, 12, cz, OathholdCanvas.LANTERN);
		for (int[] l : new int[][]{{-12, 164}, {12, 164}, {-12, 188}, {12, 188}, {-10, 152}, {10, 152}, {-10, 206}, {10, 206}}) {
			c.streetLamp(l[0], 1, l[1]);
		}
	}

	private static void buildCentralCanalAndBridges(OathholdCanvas c) {
		if (!c.intersects(-14, -44, 14, 150)) return;

		for (int z = Math.max(-42, c.clipMinLz()); z <= Math.min(148, c.clipMaxLz()); z++) {
			c.fill(-11, -3, z, -10, 1, z, OathholdCanvas.STONE_BRICKS);
			c.fill(10, -3, z, 11, 1, z, OathholdCanvas.STONE_BRICKS);
			if (Math.floorMod(z, 14) == 0) {
				c.set(-10, 2, z, OathholdCanvas.TUFF_BRICKS);
				c.set(10, 2, z, OathholdCanvas.TUFF_BRICKS);
				c.set(-10, 3, z, OathholdCanvas.LANTERN);
				c.set(10, 3, z, OathholdCanvas.LANTERN);
			}
		}
		// Three Arched Stone Footbridges across the Central Canal (matches aerial_overview & districts_courtyard!)
		for (int bz : new int[]{116, 56, -4}) {
			buildCanalBridge(c, bz);
		}
		// Two Wooden Canal Cargo Skiffs moored in water
		for (int[] boat : new int[][]{{0, 86}, {0, 26}}) {
			int bx = boat[0], bz = boat[1];
			c.fill(bx - 1, -1, bz - 4, bx + 1, -1, bz + 4, OathholdCanvas.SPRUCE);
			c.set(bx - 1, 0, bz - 3, OathholdCanvas.SPRUCE);
			c.set(bx + 1, 0, bz - 3, OathholdCanvas.SPRUCE);
			c.set(bx - 1, 0, bz + 3, OathholdCanvas.SPRUCE);
			c.set(bx + 1, 0, bz + 3, OathholdCanvas.SPRUCE);
			c.set(bx, 0, bz + 2, Blocks.BARREL.defaultBlockState());
			c.set(bx, 1, bz - 4, OathholdCanvas.LANTERN);
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
		if (!c.intersects(-216, -34, -120, 30)) return;
		// Long arched East-West stone viaduct (lx [-212..-126], lz [16..24]) + turn North toward Inner Wall
		for (int x = Math.max(-212, c.clipMinLx()); x <= Math.min(-126, c.clipMaxLx()); x++) {
			int mod = Math.floorMod(x, 10);
			boolean pier = mod <= 2;
			if (pier) {
				c.fillWeatheredStone(x, 1, 16, x, 13, 24);
			} else {
				int archBase = 10 - Math.max(0, 4 - Math.abs(mod - 6));
				c.fillWeatheredStone(x, archBase, 16, x, 13, 24);
			}
			// Elevated Aqueduct Water Trough at ly = 14..15
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
	}

	private static void buildGrandMarketHall(OathholdCanvas c) {
		int x0 = -114, x1 = -56, z0 = 46, z1 = 86;
		if (!c.intersects(x0 - 4, z0 - 4, x1 + 4, z1 + 4)) return;

		c.clear(x0 + 1, 1, z0 + 1, x1 - 1, 18, z1 - 1);
		c.fill(x0, 0, z0, x1, 0, z1, OathholdCanvas.POLISHED_ANDESITE);
		// Open Stone-Arched Ground Colonnade (ly = 1..7)
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

		// Upper Half-Timbered Clerestory + Vaulted Timber Roof (concept_city_aerial_overview & districts_courtyard)
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

		// Interior market counters, barrels, chests, hanging lanterns & Villagers
		for (int mx = x0 + 6; mx <= x1 - 6; mx += 12) {
			c.fill(mx - 2, 1, 64, mx + 2, 1, 68, OathholdCanvas.SPRUCE);
			c.set(mx, 2, 65, OathholdCanvas.LANTERN);
			c.set(mx - 1, 1, 69, Blocks.BARREL.defaultBlockState());
			c.chest(mx + 1, 2, 66, Direction.SOUTH, OathholdSites.LOOT_MARKET, mx * 7L);
			c.set(mx, 8, 66, OathholdCanvas.HANGING_LANTERN);
			c.spawnMob(mx, 1, 60, EntityTypes.VILLAGER, 0.0F, true);
		}
	}

	private static void buildBazaarSquare(OathholdCanvas c) {
		if (!c.intersects(-120, 88, -40, 150)) return;
		BlockState[] awnings = {
			Blocks.WOOL.red().defaultBlockState(),
			Blocks.WOOL.blue().defaultBlockState(),
			Blocks.WOOL.lime().defaultBlockState(),
			Blocks.WOOL.orange().defaultBlockState(),
			Blocks.WOOL.yellow().defaultBlockState(),
			Blocks.WOOL.white().defaultBlockState()
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
				c.set(sx - 2, 3, sz + 1, OathholdCanvas.HANGING_LANTERN);
				if ((idx & 1) == 0) {
					c.spawnMob(sx, 1, sz + 1, EntityTypes.VILLAGER, 180.0F, true);
				}
			}
		}
	}

	private static void buildGreatForgeHall(OathholdCanvas c) {
		int x0 = -186, x1 = -138, z0 = 54, z1 = 92;
		if (!c.intersects(x0 - 4, z0 - 4, x1 + 4, z1 + 4)) return;

		c.fillWeatheredStone(x0, 1, z0, x1, 10, z1);
		c.clear(x0 + 2, 1, z0 + 2, x1 - 2, 9, z1 - 2);
		c.gothicArchX(x1 - 2, x1 + 1, 1, 73, 4, 4, 2, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		c.pitchedRoofX(x0, 11, z0, x1, z1, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, OathholdCanvas.BRICKS, OathholdCanvas.DARK_OAK);

		// Twin 32-block tall smoking red-brick blast chimneys (matches aerial_overview left!)
		for (int cx : new int[]{-174, -154}) {
			c.fill(cx - 2, 1, 60, cx + 2, 30, 64, OathholdCanvas.BRICKS);
			c.fill(cx - 1, 2, 61, cx + 1, 31, 63, OathholdCanvas.BRICKS);
			c.set(cx, 32, 62, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
			// Glowing ground forge hearth + magma + lava pit inside
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
		c.spawnMob(-160, 1, 72, EntityTypes.VILLAGER, -90.0F, true);
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

		// Steep oxidized/weathered copper roof & twin green-copper spires (matches aerial_overview right!)
		c.pitchedRoofZ(x0 + 1, 19, z0 + 1, x1 - 1, z1 - 1,
			Blocks.CUT_COPPER_STAIRS.waxed().weathered(), Blocks.CUT_COPPER_SLAB.waxed().weathered(),
			OathholdCanvas.CALCITE, OathholdCanvas.COPPER_OXIDIZED);
		c.bastionTower(x0 + 4, 0, z0 + 4, 4, 24, 12, OathholdCanvas.COPPER_WEATHERED, false);
		c.bastionTower(x1 - 4, 0, z0 + 4, 4, 24, 12, OathholdCanvas.COPPER_WEATHERED, false);
		c.chest(148, 1, 76, Direction.NORTH, OathholdSites.LOOT_TREASURY, 321L);
		c.chandelier(136, 17, 4, 63);
	}

	private static void buildGrandInn(OathholdCanvas c) {
		int x0 = 36, x1 = 74, z0 = 96, z1 = 130;
		if (!c.intersects(x0 - 4, z0 - 4, x1 + 4, z1 + 4)) return;

		buildMedievalBuilding(c, x0, z0, x1, z1, 3, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, false, 401);
		// Inn Tavern hearth, common tables, Bounty Board & Wayfarer Keeper
		c.fill(44, 1, 104, 48, 2, 104, OathholdCanvas.BRICKS);
		c.set(46, 1, 104, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
		c.fill(52, 1, 108, 60, 1, 112, OathholdCanvas.DARK_OAK);
		c.set(54, 2, 110, OathholdCanvas.LANTERN);
		c.set(58, 2, 110, OathholdCanvas.LANTERN);
		c.set(54, 1, 132, Town.BOUNTY_BOARD.defaultBlockState().setValue(BountyBoardBlock.FACING, Direction.SOUTH));
		WayfarerKeeper cook = c.spawnMob(50, 1, 118, Town.KEEPER, 0.0F, true);
		if (cook != null) cook.setRole(WayfarerKeeper.Role.COOK);
	}

	private static void buildLowerCityTownhouses(OathholdCanvas c) {
		for (int gx = -192; gx <= 160; gx += 32) {
			for (int gz = -32; gz <= 160; gz += 32) {
				int x0 = gx + 3, x1 = gx + 29, z0 = gz + 3, z1 = gz + 29;
				if (!c.intersects(x0 - 2, z0 - 2, x1 + 2, z1 + 2)) continue;
				if (overlapsInnerLandmark(x0, z0, x1, z1)) continue;

				int h = OathholdCanvas.hash(gx, 11, gz);
				// Subdivide each 26x26 urban block into two crisp townhouses separated by a cobblestone alley
				c.streetLamp(gx + 16, 1, gz + 1);
				if (h < 20) {
					// Lush inner city courtyard square with cherry/oak tree & fountain well
					c.tree(gx + 16, 0, gz + 16, h < 10);
					c.fill(gx + 14, 1, gz + 8, gx + 18, 1, gz + 10, OathholdCanvas.STONE_BRICKS);
					c.set(gx + 16, 1, gz + 9, OathholdCanvas.WATER);
				} else {
					buildMedievalBuilding(c, x0, z0, gx + 14, gz + 14, 2 + (h % 2),
						h < 50 ? Blocks.SPRUCE_STAIRS : Blocks.DEEPSLATE_TILE_STAIRS,
						h < 50 ? Blocks.SPRUCE_SLAB : Blocks.DEEPSLATE_TILE_SLAB,
						h % 7 == 0, gx * 31 + gz);
					buildMedievalBuilding(c, gx + 17, z0, x1, gz + 14, 2 + ((h + 1) % 2),
						h < 65 ? Blocks.DEEPSLATE_TILE_STAIRS : Blocks.CUT_COPPER_STAIRS.waxed().weathered(),
						h < 65 ? Blocks.DEEPSLATE_TILE_SLAB : Blocks.CUT_COPPER_SLAB.waxed().weathered(),
						false, gx * 37 + gz);
					buildMedievalBuilding(c, x0, gz + 17, x1, z1, 2 + ((h + 2) % 2),
						Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB, h % 5 == 0, gx * 41 + gz);
				}
			}
		}
	}

	private static boolean overlapsInnerLandmark(int x0, int z0, int x1, int z1) {
		if (x0 <= 26 && x1 >= -26 && z1 >= -44) return true; // Central canal & Fountain Plaza & South Avenue
		if (z1 <= -36) return true; // Acropolis wall buffer
		if (x1 >= -124 && x0 <= -36 && z1 >= 42 && z0 <= 152) return true; // Grand Market Hall & Bazaar
		if (x1 >= -190 && x0 <= -134 && z1 >= 50 && z0 <= 96) return true; // Great Forge Hall
		if (x1 >= -216 && x0 <= -120 && z1 >= 10 && z0 <= 32) return true; // West Aqueduct
		if (x1 >= 112 && x0 <= 160 && z1 >= 40 && z0 <= 86) return true; // Copper Guildhall
		if (x1 >= 32 && x0 <= 78 && z1 >= 92 && z0 <= 134) return true; // Grand Inn
		return false;
	}

	/** Reusable detailed half-timbered stone-and-calcite medieval building with pitched roof & furnished interior. */
	public static void buildMedievalBuilding(OathholdCanvas c, int x0, int z0, int x1, int z1, int stories,
			net.minecraft.world.level.block.Block roofStair, net.minecraft.world.level.block.Block roofSlab,
			boolean copperAccent, int seed) {
		if (!c.intersects(x0 - 2, z0 - 2, x1 + 2, z1 + 2)) return;
		int roofY = stories * 5 + 1;
		c.clear(x0 + 1, 1, z0 + 1, x1 - 1, roofY - 1, z1 - 1);

		for (int s = 0; s < stories; s++) {
			int fy = s * 5 + 1;
			boolean ground = s == 0;
			BlockState wall = ground ? OathholdCanvas.STONE_BRICKS : OathholdCanvas.CALCITE;
			c.fill(x0, fy, z0, x1, fy + 4, z0, wall);
			c.fill(x0, fy, z1, x1, fy + 4, z1, wall);
			c.fill(x0, fy, z0, x0, fy + 4, z1, wall);
			c.fill(x1, fy, z0, x1, fy + 4, z1, wall);
			if (!ground) {
				c.fill(x0 + 1, fy - 1, z0 + 1, x1 - 1, fy - 1, z1 - 1, OathholdCanvas.SPRUCE);
			}
			// Dark-oak half-timber corner posts & horizontal beams
			c.fill(x0, fy, z0, x0, fy + 4, z0, OathholdCanvas.DARK_OAK_LOG);
			c.fill(x1, fy, z0, x1, fy + 4, z0, OathholdCanvas.DARK_OAK_LOG);
			c.fill(x0, fy, z1, x0, fy + 4, z1, OathholdCanvas.DARK_OAK_LOG);
			c.fill(x1, fy, z1, x1, fy + 4, z1, OathholdCanvas.DARK_OAK_LOG);
			// Warm glass windows
			int mx = (x0 + x1) / 2;
			int mz = (z0 + z1) / 2;
			c.set(mx, fy + 2, z0, Blocks.GLASS_PANE.defaultBlockState());
			c.set(mx, fy + 2, z1, Blocks.GLASS_PANE.defaultBlockState());
			c.set(x0, fy + 2, mz, Blocks.GLASS_PANE.defaultBlockState());
			c.set(x1, fy + 2, mz, Blocks.GLASS_PANE.defaultBlockState());
		}

		// Front door + hanging lantern + interior bed, table & chimney
		int mx = (x0 + x1) / 2;
		c.door(mx, 1, z1, Blocks.SPRUCE_DOOR, Direction.NORTH);
		c.set(mx + 1, 3, z1 + 1, OathholdCanvas.HANGING_LANTERN);
		c.fill(x0 + 2, 1, z0 + 1, x0 + 2, roofY + 3, z0 + 1, OathholdCanvas.COBBLESTONE);
		if (x1 - x0 >= 8 && z1 - z0 >= 8) {
			c.bed(x1 - 3, 1, z0 + 2, DyeColor.RED, Direction.EAST);
			c.set(x1 - 2, 1, z0 + 4, Blocks.BARREL.defaultBlockState());
			c.set(mx, 4, (z0 + z1) / 2, OathholdCanvas.HANGING_LANTERN);
		}
		if (copperAccent) {
			c.pitchedRoofZ(x0, roofY, z0, x1, z1,
				Blocks.CUT_COPPER_STAIRS.waxed().weathered(), Blocks.CUT_COPPER_SLAB.waxed().weathered(),
				OathholdCanvas.CALCITE, OathholdCanvas.DARK_OAK);
		} else if ((seed & 1) == 0) {
			c.pitchedRoofZ(x0, roofY, z0, x1, z1, roofStair, roofSlab, OathholdCanvas.CALCITE, OathholdCanvas.DARK_OAK);
		} else {
			c.pitchedRoofX(x0, roofY, z0, x1, z1, roofStair, roofSlab, OathholdCanvas.CALCITE, OathholdCanvas.DARK_OAK);
		}
	}
}
