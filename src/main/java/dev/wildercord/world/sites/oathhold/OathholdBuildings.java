package dev.wildercord.world.sites.oathhold;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generates regional half-timbered medieval townhouses & merchant shop-houses with awning shopfronts,
 * shuttered flowerbox windows, walkable internal staircases, glowing brick hearths, shop counters,
 * and fully furnished upper-floor bedrooms matching the 1:1 interior & street concept renders.
 */
public final class OathholdBuildings {
	private OathholdBuildings() {}

	public enum Theme {
		CITADEL,
		IRONWORKS_NW,
		SCHOLARS_NE,
		MERCHANTS_SW,
		MARITIME_SE
	}

	private static final BlockState[][] AWNING_PALETTE = {
		{Blocks.WOOL.red().defaultBlockState(), Blocks.WOOL.white().defaultBlockState()},
		{Blocks.WOOL.blue().defaultBlockState(), Blocks.WOOL.white().defaultBlockState()},
		{Blocks.WOOL.orange().defaultBlockState(), Blocks.WOOL.yellow().defaultBlockState()},
		{Blocks.WOOL.cyan().defaultBlockState(), Blocks.WOOL.white().defaultBlockState()},
		{Blocks.WOOL.green().defaultBlockState(), Blocks.WOOL.white().defaultBlockState()},
		{Blocks.WOOL.red().defaultBlockState(), Blocks.WOOL.yellow().defaultBlockState()}
	};

	private static final BlockState[] RUG_COLORS = {
		Blocks.CARPET.red().defaultBlockState(),
		Blocks.CARPET.cyan().defaultBlockState(),
		Blocks.CARPET.orange().defaultBlockState(),
		Blocks.CARPET.green().defaultBlockState()
	};

	private static final DyeColor[] BED_COLORS = {
		DyeColor.RED, DyeColor.CYAN, DyeColor.GREEN, DyeColor.ORANGE, DyeColor.BLUE
	};

	public static void build(OathholdCanvas c, int x0, int z0, int x1, int z1, int stories,
			Block roofStair, Block roofSlab, boolean copperAccent, Theme theme, boolean spawnResident, int seed) {
		build(c, 0, x0, z0, x1, z1, stories, roofStair, roofSlab, copperAccent, theme, spawnResident, seed);
	}

	public static void build(OathholdCanvas c, int baseY, int x0, int z0, int x1, int z1, int stories,
			Block roofStair, Block roofSlab, boolean copperAccent, Theme theme, boolean spawnResident, int seed) {
		if (!c.intersects(x0 - 3, z0 - 3, x1 + 3, z1 + 3)) return;
		if (theme != Theme.CITADEL && (x1 - x0 + 1) >= 21 && (z1 - z0 + 1) >= 21 && Math.floorMod(seed, 3) == 0
				&& OathholdSchematic.placeHouse1(c, baseY, x0, z0, x1, z1, spawnResident, seed)) {
			return;
		}

		int roofY = baseY + stories * 5 + 1;
		int mx = (x0 + x1) / 2;
		int mz = (z0 + z1) / 2;
		int hx = Math.min(x1 - 4, x0 + 4);
		boolean cozyLoft = stories >= 2 && (x1 - x0) >= 14 && (x1 - x0) < 30 && (z1 - z0) >= 14;
		c.clear(x0 + 1, baseY + 1, z0 + 1, x1 - 1, roofY + 2, z1 - 1);

		// Ground plank floor with dark-oak border & colorful patchwork rug
		c.fill(x0 + 1, baseY, z0 + 1, x1 - 1, baseY, z1 - 1, OathholdCanvas.DARK_OAK);
		c.fill(x0 + 2, baseY, z0 + 2, x1 - 2, baseY, z1 - 2, OathholdCanvas.SPRUCE);
		BlockState rugA = RUG_COLORS[Math.floorMod(seed, RUG_COLORS.length)];
		BlockState rugB = RUG_COLORS[Math.floorMod(seed + 1, RUG_COLORS.length)];
		BlockState rugC = RUG_COLORS[Math.floorMod(seed + 2, RUG_COLORS.length)];
		BlockState rugD = RUG_COLORS[Math.floorMod(seed + 3, RUG_COLORS.length)];
		int rugCz = cozyLoft ? Math.min(z1 - 4, mz + 3) : mz;
		int rugSpan = cozyLoft ? 2 : 1;
		for (int rx = mx - rugSpan; rx <= mx + rugSpan; rx++) {
			for (int rz = rugCz - rugSpan; rz <= rugCz + rugSpan; rz++) {
				if (rx > x0 + 1 && rx < x1 - 1 && rz > z0 + 1 && rz < z1 - 1) {
					BlockState tile = switch (Math.floorMod(rx * 3 + rz * 5, 4)) {
						case 0 -> rugA;
						case 1 -> rugB;
						case 2 -> rugC;
						default -> rugD;
					};
					c.set(rx, baseY + 1, rz, tile);
				}
			}
		}

		BlockState groundWall = switch (theme) {
			case IRONWORKS_NW -> (seed & 1) == 0 ? OathholdCanvas.TUFF_BRICKS : OathholdCanvas.STONE_BRICKS;
			case MARITIME_SE -> OathholdCanvas.TUFF_BRICKS;
			default -> OathholdCanvas.STONE_BRICKS;
		};
		BlockState upperPlaster = (theme == Theme.MERCHANTS_SW && (seed % 3 == 0))
			? Blocks.SMOOTH_SANDSTONE.defaultBlockState()
			: OathholdCanvas.CALCITE;

		for (int s = 0; s < stories; s++) {
			int fy = baseY + s * 5 + 1;
			boolean ground = s == 0;
			BlockState wall = ground ? groundWall : upperPlaster;

			c.fill(x0, fy, z0, x1, fy + 4, z0, wall);
			c.fill(x0, fy, z1, x1, fy + 4, z1, wall);
			c.fill(x0, fy, z0, x0, fy + 4, z1, wall);
			c.fill(x1, fy, z0, x1, fy + 4, z1, wall);

			if (!ground) {
				// Exposed timber crossbeams + full upper floor OR 3/4 mezzanine loft overlooking ground shop
				int deckMaxZ = (cozyLoft && s == 1) ? mz : z1 - 1;
				c.fill(x0 + 1, fy - 1, z0 + 1, x1 - 1, fy - 1, deckMaxZ, OathholdCanvas.SPRUCE);
				if (cozyLoft && s == 1 && mz + 4 < z1) {
					c.fill(x0 + 1, fy - 1, mz + 4, x1 - 1, fy - 1, mz + 4, OathholdCanvas.DARK_OAK_LOG);
				}
				c.fill(x0, fy, z0, x1, fy, z0, OathholdCanvas.DARK_OAK_LOG);
				c.fill(x0, fy, z1, x1, fy, z1, OathholdCanvas.DARK_OAK_LOG);
				c.fill(x0, fy, z0, x0, fy, z1, OathholdCanvas.DARK_OAK_LOG);
				c.fill(x1, fy, z0, x1, fy, z1, OathholdCanvas.DARK_OAK_LOG);
			}

			// Vertical timber-frame corner posts & intermediate studs
			c.fill(x0, fy, z0, x0, fy + 4, z0, OathholdCanvas.DARK_OAK_LOG);
			c.fill(x1, fy, z0, x1, fy + 4, z0, OathholdCanvas.DARK_OAK_LOG);
			c.fill(x0, fy, z1, x0, fy + 4, z1, OathholdCanvas.DARK_OAK_LOG);
			c.fill(x1, fy, z1, x1, fy + 4, z1, OathholdCanvas.DARK_OAK_LOG);
			if (x1 - x0 >= 14) {
				c.fill(mx - 3, fy, z0, mx - 3, fy + 4, z0, OathholdCanvas.DARK_OAK_LOG);
				c.fill(mx + 3, fy, z0, mx + 3, fy + 4, z0, OathholdCanvas.DARK_OAK_LOG);
				c.fill(mx - 3, fy, z1, mx - 3, fy + 4, z1, OathholdCanvas.DARK_OAK_LOG);
				c.fill(mx + 3, fy, z1, mx + 3, fy + 4, z1, OathholdCanvas.DARK_OAK_LOG);
			}

			// Windows: wide shopfront windows on ground, shuttered flowerbox windows upstairs
			if (ground) {
				c.fill(mx - 2, baseY + 2, z1, mx - 2, baseY + 3, z1, Blocks.GLASS_PANE.defaultBlockState());
				c.fill(mx + 2, baseY + 2, z1, mx + 2, baseY + 3, z1, Blocks.GLASS_PANE.defaultBlockState());
				c.fill(mx, baseY + 2, z0, mx, baseY + 3, z0, Blocks.GLASS_PANE.defaultBlockState());
				if (cozyLoft && x1 - 4 > mx + 1) {
					c.fill(x1 - 4, baseY + 2, z0, x1 - 4, baseY + 3, z0, Blocks.GLASS_PANE.defaultBlockState());
					c.fill(x1 - 2, baseY + 2, z0, x1 - 2, baseY + 3, z0, Blocks.GLASS_PANE.defaultBlockState());
				}
				c.set(x0, baseY + 2, mz, Blocks.GLASS_PANE.defaultBlockState());
				c.set(x1, baseY + 2, mz, Blocks.GLASS_PANE.defaultBlockState());
				OathholdStreetscape.flowerBox(c, x0 - 1, baseY + 1, mz + 2, Direction.WEST);
				OathholdStreetscape.flowerBox(c, x1 + 1, baseY + 1, mz + 2, Direction.EAST);
			} else {
				OathholdStreetscape.windowWithShuttersAndBox(c, mx, fy + 2, z1, Direction.SOUTH, seed + s);
				OathholdStreetscape.windowWithShuttersAndBox(c, mx, fy + 2, z0, Direction.NORTH, seed + s + 1);
				OathholdStreetscape.windowWithShuttersAndBox(c, x0, fy + 2, mz, Direction.WEST, seed + s + 2);
				OathholdStreetscape.windowWithShuttersAndBox(c, x1, fy + 2, mz, Direction.EAST, seed + s + 3);
			}
		}

		// Front door + striped shop awning + hanging lantern shop sign
		c.door(mx, baseY + 1, z1, Blocks.SPRUCE_DOOR, Direction.NORTH);
		BlockState[] awning = pickAwning(theme, seed);
		OathholdStreetscape.shopAwningZ(c, mx - 2, mx + 2, baseY + 3, z1, 1, awning[0], awning[1]);
		if (mx - 3 >= x0 + 1) {
			c.set(mx - 3, baseY + 4, z1 + 1, Blocks.DARK_OAK_FENCE.defaultBlockState());
			c.set(mx - 3, baseY + 3, z1 + 1, OathholdCanvas.HANGING_LANTERN);
		}
		if (mx + 3 <= x1 - 1) {
			c.set(mx + 3, baseY + 1, z1 + 1, Blocks.BARREL.defaultBlockState());
		}

		// Tapered red-brick fireplace hearth & chimney (Ochre Froglight warm glow + iron grate)
		c.fill(hx - 1, baseY + 1, z0 + 1, hx + 1, baseY + 3, z0 + 1, OathholdCanvas.BRICKS);
		c.fill(hx - 1, baseY + 4, z0 + 1, hx + 1, baseY + 4, z0 + 1, OathholdCanvas.BRICKS);
		c.fill(hx, baseY + 1, z0 + 1, hx, roofY + 3, z0 + 1, OathholdCanvas.BRICKS);
		c.set(hx, roofY + 4, z0 + 1, Blocks.COBBLESTONE_WALL.defaultBlockState());
		c.set(hx, roofY + 5, z0 + 1, Blocks.FLOWER_POT.defaultBlockState());
		c.set(hx, baseY + 1, z0 + 1, OathholdCanvas.OCHRE_LIGHT);
		c.set(hx, baseY, z0 + 1, Blocks.MAGMA_BLOCK.defaultBlockState());
		c.set(hx, baseY + 1, z0 + 2, OathholdCanvas.IRON_BARS);
		c.set(hx, baseY + 3, z0 + 2, OathholdCanvas.slab(Blocks.BRICK_SLAB, true));
		c.set(hx, baseY + 4, z0 + 2, OathholdCanvas.LANTERN);

		// Bookshelves & barrels flanking hearth + left foreground bookshelf
		if (hx - 2 >= x0 + 1) {
			c.fill(x0 + 1, baseY + 1, z0 + 1, hx - 2, baseY + 4, z0 + 1, Blocks.BOOKSHELF.defaultBlockState());
		}
		if (cozyLoft) {
			c.fill(x0 + 1, baseY + 1, mz - 2, x0 + 1, baseY + 4, mz + 1, Blocks.BOOKSHELF.defaultBlockState());
			c.set(x0 + 2, baseY + 4, mz, OathholdCanvas.LANTERN);
		}
		if (hx + 2 <= mx - 2) {
			c.fill(hx + 2, baseY + 1, z0 + 1, hx + 3, baseY + 3, z0 + 1, Blocks.BOOKSHELF.defaultBlockState());
			c.set(hx + 4, baseY + 1, z0 + 1, Blocks.BARREL.defaultBlockState());
			c.set(hx + 4, baseY + 2, z0 + 1, Blocks.BARREL.defaultBlockState());
			if ((seed & 3) == 0 || cozyLoft) {
				c.chest(hx + 3, baseY + 4, z0 + 1, Direction.SOUTH,
					theme == Theme.SCHOLARS_NE ? OathholdSites.LOOT_ARCANUM : OathholdSites.LOOT_MARKET, seed * 17L);
			}
		}

		// Furnish Ground Floor: dining table by hearth, L-shaped merchant shop counter & warm overhead lanterns
		int tableX = Math.min(x1 - 4, hx + 1);
		int tableZ = Math.min(z1 - 4, z0 + 5);
		if (tableX > x0 + 2 && tableZ > z0 + 2) {
			c.set(tableX, baseY + 1, tableZ, Blocks.OAK_FENCE.defaultBlockState());
			c.set(tableX, baseY + 2, tableZ, Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState());
			c.set(tableX, baseY + 3, tableZ, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true));
			c.set(tableX - 1, baseY + 1, tableZ, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
			c.set(tableX + 1, baseY + 1, tableZ, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
			c.set(tableX + 1, baseY + 2, tableZ - 1, OathholdStreetscape.pottedPlant(seed));
		}

		int cx0 = Math.max(mx + 2, x1 - 5);
		int czShop = Math.max(z0 + 4, mz + 1);
		int czFront = z1 - 3;
		if (cx0 <= x1 - 2 && czShop <= czFront) {
			c.fill(cx0, baseY + 1, czShop, x1 - 2, baseY + 1, czShop, OathholdCanvas.SPRUCE);
			c.fill(cx0, baseY + 1, czShop, cx0, baseY + 1, czFront, OathholdCanvas.SPRUCE);
			c.set(cx0, baseY + 2, czFront - 1, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true));
			c.set(cx0 + 1, baseY + 2, czShop, Blocks.LOOM.defaultBlockState());
			c.set(x1 - 2, baseY + 2, czShop, OathholdStreetscape.pottedPlant(seed + 4));
			if (czShop + 2 <= czFront) {
				c.set(cx0, baseY + 2, czShop + 2, OathholdStreetscape.pottedPlant(seed + 2));
			}
			c.set(x1 - 2, baseY + 1, czFront + 1, districtWorkstation(theme));
			c.set(x1 - 3, baseY + 4, czFront, OathholdCanvas.HANGING_LANTERN);
		} else {
			int sx = Math.max(mx + 1, x1 - 4);
			if (sx <= x1 - 2 && czFront > z0 + 2) {
				c.fill(sx, baseY + 1, czFront, x1 - 2, baseY + 1, czFront, OathholdCanvas.SPRUCE);
				c.set(sx, baseY + 2, czFront, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true));
				c.set(x1 - 2, baseY + 2, czFront, OathholdStreetscape.pottedPlant(seed + 4));
				c.set(x1 - 2, baseY + 1, czFront + 1, districtWorkstation(theme));
			}
		}

		c.set(mx, baseY + 4, mz + 3, OathholdCanvas.HANGING_LANTERN);
		c.set(x0 + 2, baseY + 4, z0 + 3, OathholdCanvas.HANGING_LANTERN);

		// Walkable Internal Spruce Staircase & Open Mezzanine Balcony
		if (cozyLoft) {
			// Central-left staircase climbing up toward mezzanine bedroom loft at z = mz
			for (int step = 0; step <= 4; step++) {
				int sz = mz + 4 - step;
				int sy = baseY + 1 + step;
				c.fill(mx - 1, baseY + 1, sz, mx - 1, Math.max(baseY + 1, sy - 1), sz, OathholdCanvas.SPRUCE);
				c.set(mx - 1, sy, sz, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
				c.clear(mx - 1, sy + 1, sz, mx - 1, baseY + 9, sz);
			}
			c.fill(x0 + 1, baseY + 6, mz, x1 - 2, baseY + 6, mz, Blocks.OAK_FENCE.defaultBlockState());
			c.clear(mx - 2, baseY + 6, mz, mx, baseY + 8, mz);
			c.clear(mx - 2, baseY + 5, mz + 1, mx, baseY + 9, z1 - 2);
		} else {
			for (int s = 0; s < stories - 1; s++) {
				int fy = baseY + s * 5 + 1;
				for (int step = 0; step <= 4; step++) {
					int sz = z0 + 2 + step;
					int sy = fy + step;
					if (sz >= z1 - 1) break;
					c.set(x0 + 1, sy, sz, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
					c.clear(x0 + 1, sy + 1, sz, x0 + 1, fy + 8, sz);
					if (step > 0 && step < 4) {
						c.set(x0 + 2, fy + 5, sz, Blocks.OAK_FENCE.defaultBlockState());
					}
				}
			}
		}

		// Upper-story furnished bedrooms & studies (clearly visible from ground shop in cozyLoft buildings!)
		for (int s = 1; s < stories; s++) {
			int fy = baseY + s * 5 + 1;
			DyeColor bedCol = BED_COLORS[Math.floorMod(seed + s, BED_COLORS.length)];
			if (cozyLoft && s == 1) {
				int bedX = Math.max(mx + 1, x1 - 6);
				int bedZ = Math.max(z0 + 2, mz - 3);
				c.bed(bedX, fy, bedZ, bedCol, Direction.EAST);
				c.set(bedX + 2, fy, bedZ, Blocks.CRAFTING_TABLE.defaultBlockState());
				c.set(bedX + 1, fy + 3, bedZ, OathholdCanvas.HANGING_LANTERN);
				c.fill(x0 + 2, fy, z0 + 1, x0 + 3, fy + 2, z0 + 1, Blocks.BOOKSHELF.defaultBlockState());
				c.set(mx, fy + 3, z0 + 3, OathholdCanvas.HANGING_LANTERN);
			} else {
				if (x1 - 3 > x0 + 2 && z0 + 2 < z1 - 2) {
					c.bed(x1 - 3, fy, z0 + 2, bedCol, Direction.EAST);
					c.set(x1 - 4, fy, z0 + 2, Blocks.CRAFTING_TABLE.defaultBlockState());
					c.set(x1 - 4, fy + 1, z0 + 2, OathholdCanvas.LANTERN);
				}
				c.fill(x1 - 2, fy, z1 - 1, x1 - 1, fy + 2, z1 - 1, Blocks.BOOKSHELF.defaultBlockState());
				if (x1 - 4 > x0 + 2 && z1 - 2 > z0 + 3) {
					c.set(x1 - 3, fy, z1 - 2, Blocks.OAK_FENCE.defaultBlockState());
					c.set(x1 - 3, fy + 1, z1 - 2, Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState());
					c.set(x1 - 3, fy + 2, z1 - 2, OathholdStreetscape.pottedPlant(seed + s + 1));
					c.set(x1 - 4, fy, z1 - 2, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
				}
				c.set(mx, fy, mz, rugA);
				c.set(mx, fy + 3, mz, OathholdCanvas.HANGING_LANTERN);
			}
		}

		// Roof matching quadrant palette + gable window accent
		if (copperAccent) {
			c.pitchedRoofZ(x0, roofY, z0, x1, z1,
				Blocks.CUT_COPPER_STAIRS.waxed().weathered(), Blocks.CUT_COPPER_SLAB.waxed().weathered(),
				upperPlaster, OathholdCanvas.DARK_OAK);
			c.set(mx, roofY + 2, z1, Blocks.GLASS_PANE.defaultBlockState());
		} else if ((seed & 1) == 0) {
			c.pitchedRoofZ(x0, roofY, z0, x1, z1, roofStair, roofSlab, upperPlaster, OathholdCanvas.DARK_OAK);
			c.set(mx, roofY + 2, z1, Blocks.GLASS_PANE.defaultBlockState());
		} else {
			c.pitchedRoofX(x0, roofY, z0, x1, z1, roofStair, roofSlab, upperPlaster, OathholdCanvas.DARK_OAK);
			c.set(x0, roofY + 2, mz, Blocks.GLASS_PANE.defaultBlockState());
			c.set(x1, roofY + 2, mz, Blocks.GLASS_PANE.defaultBlockState());
		}

		// Structure residents are placed after terrain & interiors by OathholdPopulation.
	}

	private static BlockState[] pickAwning(Theme theme, int seed) {
		return switch (theme) {
			case IRONWORKS_NW -> AWNING_PALETTE[(seed & 1) == 0 ? 5 : 2];
			case SCHOLARS_NE -> AWNING_PALETTE[(seed & 1) == 0 ? 1 : 3];
			case MERCHANTS_SW -> AWNING_PALETTE[Math.floorMod(seed, AWNING_PALETTE.length)];
			case MARITIME_SE -> AWNING_PALETTE[(seed & 1) == 0 ? 3 : 4];
			case CITADEL -> AWNING_PALETTE[Math.floorMod(seed, AWNING_PALETTE.length)];
		};
	}

	private static BlockState districtWorkstation(Theme theme) {
		return switch (theme) {
			case IRONWORKS_NW -> Blocks.SMITHING_TABLE.defaultBlockState();
			case SCHOLARS_NE -> Blocks.CARTOGRAPHY_TABLE.defaultBlockState();
			case MERCHANTS_SW -> Blocks.LOOM.defaultBlockState();
			case MARITIME_SE -> Blocks.FLETCHING_TABLE.defaultBlockState();
			case CITADEL -> Blocks.CRAFTING_TABLE.defaultBlockState();
		};
	}
}
