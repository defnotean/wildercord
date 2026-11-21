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

		int roofY = baseY + stories * 5 + 1;
		int mx = (x0 + x1) / 2;
		int mz = (z0 + z1) / 2;
		int hx = Math.min(x1 - 4, x0 + 4);
		c.clear(x0 + 1, baseY + 1, z0 + 1, x1 - 1, roofY + 2, z1 - 1);

		// Ground plank floor with dark-oak border & woven center rug
		c.fill(x0 + 1, baseY, z0 + 1, x1 - 1, baseY, z1 - 1, OathholdCanvas.DARK_OAK);
		c.fill(x0 + 2, baseY, z0 + 2, x1 - 2, baseY, z1 - 2, OathholdCanvas.SPRUCE);
		BlockState rugA = RUG_COLORS[Math.floorMod(seed, RUG_COLORS.length)];
		BlockState rugB = RUG_COLORS[Math.floorMod(seed + 1, RUG_COLORS.length)];
		for (int rx = mx - 1; rx <= mx + 1; rx++) {
			for (int rz = mz - 1; rz <= mz + 1; rz++) {
				if (rx > x0 + 1 && rx < x1 - 1 && rz > z0 + 1 && rz < z1 - 1) {
					c.set(rx, baseY + 1, rz, ((rx + rz) & 1) == 0 ? rugA : rugB);
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
				// Timber horizontal sill beams + upper floor planks
				c.fill(x0 + 1, fy - 1, z0 + 1, x1 - 1, fy - 1, z1 - 1, OathholdCanvas.SPRUCE);
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
				c.set(mx, baseY + 2, z0, Blocks.GLASS_PANE.defaultBlockState());
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

		// Zero-tick red-brick fireplace hearth & chimney (Ochre Froglight flame)
		c.fill(hx - 1, baseY + 1, z0 + 1, hx + 1, baseY + 4, z0 + 1, OathholdCanvas.BRICKS);
		c.fill(hx, baseY + 1, z0 + 1, hx, roofY + 3, z0 + 1, OathholdCanvas.BRICKS);
		c.set(hx, roofY + 4, z0 + 1, Blocks.COBBLESTONE_WALL.defaultBlockState());
		c.set(hx, roofY + 5, z0 + 1, Blocks.FLOWER_POT.defaultBlockState());
		c.set(hx, baseY + 1, z0 + 1, OathholdCanvas.OCHRE_LIGHT);
		c.set(hx, baseY, z0 + 1, Blocks.MAGMA_BLOCK.defaultBlockState());
		c.set(hx, baseY + 1, z0 + 2, OathholdCanvas.IRON_BARS);
		c.set(hx, baseY + 3, z0 + 2, OathholdCanvas.slab(Blocks.BRICK_SLAB, true));
		c.set(hx, baseY + 4, z0 + 2, OathholdCanvas.LANTERN);

		// Furnish Ground Floor: dining table by hearth, shop counter, bookshelves & workstation
		int tableX = Math.min(x1 - 4, hx + 2);
		int tableZ = Math.min(z1 - 4, z0 + 4);
		if (tableX > x0 + 2 && tableZ > z0 + 2) {
			c.set(tableX, baseY + 1, tableZ, Blocks.OAK_FENCE.defaultBlockState());
			c.set(tableX, baseY + 2, tableZ, Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState());
			c.set(tableX, baseY + 3, tableZ, OathholdStreetscape.pottedPlant(seed));
			c.set(tableX - 1, baseY + 1, tableZ, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
			c.set(tableX + 1, baseY + 1, tableZ, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
		}

		int cx0 = Math.max(mx + 1, x1 - 4);
		int czShop = z1 - 3;
		if (cx0 <= x1 - 2 && czShop > z0 + 2) {
			c.fill(cx0, baseY + 1, czShop, x1 - 2, baseY + 1, czShop, OathholdCanvas.SPRUCE);
			c.set(cx0, baseY + 2, czShop, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true));
			c.set(x1 - 2, baseY + 2, czShop, OathholdStreetscape.pottedPlant(seed + 4));
			c.set(x1 - 2, baseY + 1, czShop + 1, districtWorkstation(theme));
		}

		// Bookshelf & storage wall on NE inside corner
		if (x1 - 4 > hx + 1 && z0 + 1 < z1 - 4) {
			c.fill(x1 - 3, baseY + 1, z0 + 1, x1 - 2, baseY + 3, z0 + 1, Blocks.BOOKSHELF.defaultBlockState());
			c.set(x1 - 4, baseY + 1, z0 + 1, Blocks.BARREL.defaultBlockState());
			if ((seed & 3) == 0) {
				c.chest(x1 - 4, baseY + 2, z0 + 1, Direction.SOUTH,
					theme == Theme.SCHOLARS_NE ? OathholdSites.LOOT_ARCANUM : OathholdSites.LOOT_MARKET, seed * 17L);
			}
		}
		c.set(mx, baseY + 4, mz, OathholdCanvas.HANGING_LANTERN);

		// Walkable Internal Spruce Staircase & Open Stairwell to every upper floor
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

		// Upper-story furnished bedrooms & studies
		for (int s = 1; s < stories; s++) {
			int fy = baseY + s * 5 + 1;
			DyeColor bedCol = BED_COLORS[Math.floorMod(seed + s, BED_COLORS.length)];
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
