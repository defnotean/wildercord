package dev.wildercord.world.sites.oathhold;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Chunk-clipped streetscape, promenade, garden, and furnishing primitives for Oathhold.
 * Matches the Master Map and 6 voxel reference renders 1:1 while avoiding unnecessary ticking BlockEntities.
 */
public final class OathholdStreetscape {
	private OathholdStreetscape() {}

	private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
	private static final BlockState COBBLE_WALL = Blocks.COBBLESTONE_WALL.defaultBlockState();
	private static final BlockState OAK_FENCE = Blocks.OAK_FENCE.defaultBlockState();
	private static final BlockState DARK_OAK_FENCE = Blocks.DARK_OAK_FENCE.defaultBlockState();
	private static final BlockState HEDGE_LEAVES = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
	private static final BlockState SPRUCE_PLATE = Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState();
	private static final BlockState CRAFTING = Blocks.CRAFTING_TABLE.defaultBlockState();
	private static final BlockState LOOM = Blocks.LOOM.defaultBlockState();
	private static final BlockState NOTE = Blocks.NOTE_BLOCK.defaultBlockState();
	private static final BlockState HAY = Blocks.HAY_BLOCK.defaultBlockState();
	private static final BlockState BARREL = Blocks.BARREL.defaultBlockState();
	private static final BlockState RAIL_CHAIN_X = Blocks.IRON_CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
	private static final BlockState RAIL_CHAIN_Z = Blocks.IRON_CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);

	private static final BlockState[] FLOWERS = {
		Blocks.POPPY.defaultBlockState(),
		Blocks.BLUE_ORCHID.defaultBlockState(),
		Blocks.ALLIUM.defaultBlockState(),
		Blocks.DANDELION.defaultBlockState(),
		Blocks.AZURE_BLUET.defaultBlockState()
	};

	private static final BlockState[] POTTED = {
		Blocks.POTTED_POPPY.defaultBlockState(),
		Blocks.POTTED_FERN.defaultBlockState(),
		Blocks.POTTED_AZALEA.defaultBlockState(),
		Blocks.POTTED_BLUE_ORCHID.defaultBlockState(),
		Blocks.POTTED_CHERRY_SAPLING.defaultBlockState()
	};

	public static BlockState trapdoor(Block block, Direction facing, boolean open) {
		return block.defaultBlockState()
			.setValue(TrapDoorBlock.FACING, facing)
			.setValue(TrapDoorBlock.OPEN, open)
			.setValue(TrapDoorBlock.HALF, Half.BOTTOM);
	}

	public static BlockState trapdoorShelf(Block block, Direction facing) {
		return block.defaultBlockState()
			.setValue(TrapDoorBlock.FACING, facing)
			.setValue(TrapDoorBlock.OPEN, false)
			.setValue(TrapDoorBlock.HALF, Half.TOP);
	}

	public static BlockState pottedPlant(int hashSeed) {
		return POTTED[Math.floorMod(hashSeed, POTTED.length)];
	}

	/** Horizontal iron chain railing along X or Z. */
	public static BlockState chainAxis(Direction.Axis axis) {
		return Blocks.IRON_CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
	}

	/** Ornate double-arm hanging-lantern streetlamp matching boulevard & esplanade concept renders. */
	public static void ornateStreetLamp(OathholdCanvas c, int x, int y, int z, boolean alongZ) {
		if (!c.intersects(x - 1, z - 1, x + 1, z + 1)) return;
		c.set(x, y, z, OathholdCanvas.CHISELED_TUFF_BRICKS);
		c.set(x, y + 1, z, COBBLE_WALL);
		c.set(x, y + 2, z, COBBLE_WALL);
		c.set(x, y + 3, z, OathholdCanvas.IRON_BARS);
		c.set(x, y + 4, z, OathholdCanvas.TUFF_BRICKS);
		if (alongZ) {
			c.set(x, y + 4, z - 1, DARK_OAK_FENCE);
			c.set(x, y + 4, z + 1, DARK_OAK_FENCE);
			c.set(x, y + 3, z - 1, OathholdCanvas.HANGING_LANTERN);
			c.set(x, y + 3, z + 1, OathholdCanvas.HANGING_LANTERN);
		} else {
			c.set(x - 1, y + 4, z, DARK_OAK_FENCE);
			c.set(x + 1, y + 4, z, DARK_OAK_FENCE);
			c.set(x - 1, y + 3, z, OathholdCanvas.HANGING_LANTERN);
			c.set(x + 1, y + 3, z, OathholdCanvas.HANGING_LANTERN);
		}
	}

	/** 3-wide spruce-stair park bench with trapdoor armrests. */
	public static void parkBench(OathholdCanvas c, int x, int y, int z, Direction facing) {
		if (!c.intersects(x - 2, z - 2, x + 2, z + 2)) return;
		BlockState seat = OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, facing, false);
		if (facing == Direction.NORTH || facing == Direction.SOUTH) {
			c.set(x - 1, y, z, seat);
			c.set(x, y, z, seat);
			c.set(x + 1, y, z, seat);
			c.set(x - 2, y, z, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.WEST, true));
			c.set(x + 2, y, z, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.EAST, true));
		} else {
			c.set(x, y, z - 1, seat);
			c.set(x, y, z, seat);
			c.set(x, y, z + 1, seat);
			c.set(x, y, z - 2, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.NORTH, true));
			c.set(x, y, z + 2, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.SOUTH, true));
		}
	}

	/** 3x3 stone-brick curb tree planter holding an oak or cherry tree above baseY. */
	public static void treePlanter(OathholdCanvas c, int x, int baseY, int z, boolean cherry) {
		if (!c.intersects(x - 3, z - 3, x + 3, z + 3)) return;
		BlockState curb = OathholdCanvas.slab(Blocks.STONE_BRICK_SLAB, false);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) {
					c.set(x, baseY, z, GRASS);
				} else {
					c.set(x + dx, baseY, z + dz, OathholdCanvas.TUFF_BRICKS);
					c.set(x + dx, baseY + 1, z + dz, curb);
				}
			}
		}
		c.tree(x, baseY, z, cherry);
	}

	/** Raised flowerbed framed in spruce trapdoors matching boulevard sidewalk planters. */
	public static void flowerBox(OathholdCanvas c, int x, int y, int z, Direction outward) {
		if (!c.intersects(x - 2, z - 2, x + 2, z + 2)) return;
		c.set(x, y, z, GRASS);
		c.set(x, y + 1, z, FLOWERS[OathholdCanvas.hash(x, y, z) % FLOWERS.length]);
		int fx = x + outward.getStepX(), fz = z + outward.getStepZ();
		c.set(fx, y, fz, trapdoor(Blocks.SPRUCE_TRAPDOOR, outward, true));
	}

	/** Shuttered multi-pane window with lower spruce-trapdoor shelf and potted flower. */
	public static void windowWithShuttersAndBox(OathholdCanvas c, int x, int y, int z, Direction outward, int seed) {
		int sx = outward.getStepX(), sz = outward.getStepZ();
		int px = sz != 0 ? 1 : 0, pz = sx != 0 ? 1 : 0;
		if (!c.intersects(x - 2, z - 2, x + 2, z + 2)) return;
		c.set(x, y, z, Blocks.GLASS_PANE.defaultBlockState());
		c.set(x, y + 1, z, Blocks.GLASS_PANE.defaultBlockState());
		c.set(x - px + sx, y, z - pz + sz, trapdoor(Blocks.SPRUCE_TRAPDOOR, outward, true));
		c.set(x + px + sx, y, z + pz + sz, trapdoor(Blocks.SPRUCE_TRAPDOOR, outward, true));
		c.set(x + sx, y - 1, z + sz, trapdoorShelf(Blocks.SPRUCE_TRAPDOOR, outward));
		c.set(x + sx, y, z + sz, pottedPlant(seed));
	}

	/** Striped sloped cloth shop awning projecting along Z from wallZ + zSign. */
	public static void shopAwningZ(OathholdCanvas c, int x0, int x1, int y, int wallZ, int zSign,
			BlockState woolA, BlockState woolB) {
		int minZ = Math.min(wallZ + zSign, wallZ + 2 * zSign);
		int maxZ = Math.max(wallZ + zSign, wallZ + 2 * zSign);
		if (!c.intersects(x0, minZ, x1, maxZ)) return;
		for (int x = x0; x <= x1; x++) {
			BlockState col = ((x - x0) & 1) == 0 ? woolA : woolB;
			c.set(x, y + 1, wallZ + zSign, col);
			c.set(x, y, wallZ + 2 * zSign, col);
		}
		c.set(x0, y - 1, wallZ + 2 * zSign, OAK_FENCE);
		c.set(x1, y - 1, wallZ + 2 * zSign, OAK_FENCE);
		c.set((x0 + x1) / 2, y, wallZ + zSign, OathholdCanvas.HANGING_LANTERN);
	}

	/** Striped sloped cloth shop awning projecting along X from wallX + xSign. */
	public static void shopAwningX(OathholdCanvas c, int z0, int z1, int y, int wallX, int xSign,
			BlockState woolA, BlockState woolB) {
		int minX = Math.min(wallX + xSign, wallX + 2 * xSign);
		int maxX = Math.max(wallX + xSign, wallX + 2 * xSign);
		if (!c.intersects(minX, z0, maxX, z1)) return;
		for (int z = z0; z <= z1; z++) {
			BlockState col = ((z - z0) & 1) == 0 ? woolA : woolB;
			c.set(wallX + xSign, y + 1, z, col);
			c.set(wallX + 2 * xSign, y, z, col);
		}
		c.set(wallX + 2 * xSign, y - 1, z0, OAK_FENCE);
		c.set(wallX + 2 * xSign, y - 1, z1, OAK_FENCE);
		c.set(wallX + xSign, y, (z0 + z1) / 2, OathholdCanvas.HANGING_LANTERN);
	}

	/** Horse-wagon style timber merchant cart with dark-oak wheels, hay/barrel cargo & lantern. */
	public static void merchantWagon(OathholdCanvas c, int x, int y, int z, boolean alongZ, int seed) {
		if (!c.intersects(x - 3, z - 3, x + 3, z + 3)) return;
		BlockState cargoA = (seed & 1) == 0 ? HAY : CRAFTING;
		if (alongZ) {
			c.fill(x - 1, y, z - 2, x + 1, y, z + 2, OathholdCanvas.SPRUCE);
			c.set(x - 2, y, z - 1, trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.WEST, true));
			c.set(x + 2, y, z - 1, trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.EAST, true));
			c.set(x - 2, y, z + 1, trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.WEST, true));
			c.set(x + 2, y, z + 1, trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.EAST, true));
			c.set(x - 1, y + 1, z - 1, cargoA);
			c.set(x + 1, y + 1, z - 1, HAY);
			c.set(x, y + 1, z + 1, BARREL);
			c.set(x - 1, y, z + 3, OAK_FENCE);
			c.set(x + 1, y, z + 3, OAK_FENCE);
			c.set(x, y + 2, z - 1, OathholdCanvas.LANTERN);
		} else {
			c.fill(x - 2, y, z - 1, x + 2, y, z + 1, OathholdCanvas.SPRUCE);
			c.set(x - 1, y, z - 2, trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.NORTH, true));
			c.set(x + 1, y, z - 2, trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.NORTH, true));
			c.set(x - 1, y, z + 2, trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.SOUTH, true));
			c.set(x + 1, y, z + 2, trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.SOUTH, true));
			c.set(x - 1, y + 1, z - 1, cargoA);
			c.set(x - 1, y + 1, z + 1, HAY);
			c.set(x + 1, y + 1, z, BARREL);
			c.set(x + 3, y, z - 1, OAK_FENCE);
			c.set(x + 3, y, z + 1, OAK_FENCE);
			c.set(x - 1, y + 2, z, OathholdCanvas.LANTERN);
		}
	}

	/** Compact 5x3 vendor market stall with striped cloth awning and zero-tick counter props. */
	public static void marketStall(OathholdCanvas c, int cx, int y, int cz,
			BlockState woolA, BlockState woolB, int seed) {
		if (!c.intersects(cx - 3, cz - 3, cx + 3, cz + 3)) return;
		c.fill(cx - 2, y, cz - 1, cx + 2, y, cz - 1, OathholdCanvas.SPRUCE);
		for (int sx : new int[]{-2, 2}) {
			c.fill(cx + sx, y, cz - 2, cx + sx, y + 2, cz - 2, OAK_FENCE);
			c.fill(cx + sx, y, cz + 2, cx + sx, y + 2, cz + 2, OAK_FENCE);
		}
		for (int x = cx - 2; x <= cx + 2; x++) {
			BlockState stripe = ((x + cz) & 1) == 0 ? woolA : woolB;
			c.fill(x, y + 3, cz - 2, x, y + 3, cz + 2, stripe);
		}
		c.set(cx - 1, y + 1, cz - 1, (seed & 1) == 0 ? LOOM : CRAFTING);
		c.set(cx + 2, y + 1, cz - 1, BARREL);
		c.set(cx, y + 2, cz, OathholdCanvas.HANGING_LANTERN);
	}

	/** Stacked crate and barrel cargo pile with at most one real Barrel block entity. */
	public static void cargoCluster(OathholdCanvas c, int x, int y, int z, int seed) {
		if (!c.intersects(x - 1, z - 1, x + 2, z + 2)) return;
		BlockState main = (seed & 1) == 0 ? CRAFTING : NOTE;
		c.set(x, y, z, main);
		c.set(x + 1, y, z, HAY);
		c.set(x, y, z + 1, BARREL);
		c.set(x + 1, y, z + 1, LOOM);
		c.set(x, y + 1, z, NOTE);
		c.set(x, y + 2, z, OathholdCanvas.LANTERN);
	}

	/** Outdoor waterside/patio umbrella table with sleek carpet canopy & two spruce-stair chairs. */
	public static void patioTable(OathholdCanvas c, int x, int y, int z, BlockState umbrellaCarpet, BlockState accentWool) {
		if (!c.intersects(x - 2, z - 2, x + 2, z + 2)) return;
		BlockState carpetWhite = Blocks.CARPET.white().defaultBlockState();
		BlockState trapTop = trapdoorShelf(Blocks.SPRUCE_TRAPDOOR, Direction.NORTH);
		c.set(x, y, z, OAK_FENCE);
		c.set(x, y + 1, z, OAK_FENCE);
		c.set(x, y + 2, z, OAK_FENCE);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx != 0 || dz != 0) {
					c.set(x + dx, y + 2, z + dz, trapTop);
				}
				c.set(x + dx, y + 3, z + dz, ((dx + dz) & 1) == 0 ? umbrellaCarpet : carpetWhite);
			}
		}
		c.set(x - 1, y, z, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
		c.set(x + 1, y, z, OathholdCanvas.stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
	}

	/** Moored 13-block masted wooden canal cargo barge sitting proudly in canal water. */
	public static void canalBarge(OathholdCanvas c, int cx, int waterTopY, int cz, boolean alongZ) {
		if (!c.intersects(cx - 7, cz - 7, cx + 7, cz + 7)) return;
		if (alongZ) {
			c.fill(cx - 2, waterTopY, cz - 5, cx + 2, waterTopY, cz + 5, OathholdCanvas.SPRUCE);
			c.fill(cx - 1, waterTopY, cz - 6, cx + 1, waterTopY, cz - 6, OathholdCanvas.DARK_OAK);
			c.fill(cx - 1, waterTopY, cz + 6, cx + 1, waterTopY, cz + 6, OathholdCanvas.DARK_OAK);
			c.fill(cx - 2, waterTopY + 1, cz - 4, cx - 2, waterTopY + 1, cz + 4, OathholdCanvas.DARK_OAK);
			c.fill(cx + 2, waterTopY + 1, cz - 4, cx + 2, waterTopY + 1, cz + 4, OathholdCanvas.DARK_OAK);
			c.fill(cx - 1, waterTopY + 1, cz - 5, cx + 1, waterTopY + 1, cz - 5, OathholdCanvas.DARK_OAK);
			c.fill(cx - 1, waterTopY + 1, cz + 5, cx + 1, waterTopY + 1, cz + 5, OathholdCanvas.DARK_OAK);
			// Tall central cargo mast, yardarm sail bar & pennant
			c.fill(cx, waterTopY + 1, cz, cx, waterTopY + 7, cz, OathholdCanvas.DARK_OAK_LOG);
			c.fill(cx - 2, waterTopY + 5, cz, cx + 2, waterTopY + 5, cz, Blocks.WOOL.white().defaultBlockState());
			c.fill(cx - 1, waterTopY + 6, cz, cx + 1, waterTopY + 6, cz, OathholdCanvas.DARK_OAK);
			c.flagPennant(cx, waterTopY + 7, cz);
			// Cargo crates, hay bales, barrels & bow/stern lanterns
			c.set(cx - 1, waterTopY + 1, cz - 2, CRAFTING);
			c.set(cx + 1, waterTopY + 1, cz - 2, HAY);
			c.set(cx, waterTopY + 1, cz + 2, BARREL);
			c.set(cx + 1, waterTopY + 1, cz + 3, HAY);
			c.set(cx - 1, waterTopY + 1, cz + 3, LOOM);
			c.set(cx, waterTopY + 2, cz - 2, NOTE);
			c.set(cx - 2, waterTopY + 2, cz - 4, OathholdCanvas.LANTERN);
			c.set(cx + 2, waterTopY + 2, cz + 4, OathholdCanvas.LANTERN);
		} else {
			c.fill(cx - 5, waterTopY, cz - 2, cx + 5, waterTopY, cz + 2, OathholdCanvas.SPRUCE);
			c.fill(cx - 6, waterTopY, cz - 1, cx - 6, waterTopY, cz + 1, OathholdCanvas.DARK_OAK);
			c.fill(cx + 6, waterTopY, cz - 1, cx + 6, waterTopY, cz + 1, OathholdCanvas.DARK_OAK);
			c.fill(cx - 4, waterTopY + 1, cz - 2, cx + 4, waterTopY + 1, cz - 2, OathholdCanvas.DARK_OAK);
			c.fill(cx - 4, waterTopY + 1, cz + 2, cx + 4, waterTopY + 1, cz + 2, OathholdCanvas.DARK_OAK);
			c.fill(cx - 5, waterTopY + 1, cz - 1, cx - 5, waterTopY + 1, cz + 1, OathholdCanvas.DARK_OAK);
			c.fill(cx + 5, waterTopY + 1, cz - 1, cx + 5, waterTopY + 1, cz + 1, OathholdCanvas.DARK_OAK);
			c.fill(cx, waterTopY + 1, cz, cx, waterTopY + 7, cz, OathholdCanvas.DARK_OAK_LOG);
			c.fill(cx, waterTopY + 5, cz - 2, cx, waterTopY + 5, cz + 2, Blocks.WOOL.white().defaultBlockState());
			c.fill(cx, waterTopY + 6, cz - 1, cx, waterTopY + 6, cz + 1, OathholdCanvas.DARK_OAK);
			c.flagPennant(cx, waterTopY + 7, cz);
			c.set(cx - 2, waterTopY + 1, cz - 1, CRAFTING);
			c.set(cx - 2, waterTopY + 1, cz + 1, HAY);
			c.set(cx + 2, waterTopY + 1, cz, BARREL);
			c.set(cx + 3, waterTopY + 1, cz + 1, HAY);
			c.set(cx - 4, waterTopY + 2, cz - 2, OathholdCanvas.LANTERN);
			c.set(cx + 4, waterTopY + 2, cz + 2, OathholdCanvas.LANTERN);
		}
	}

	/** Sculpted multi-block Oathbound Hero Knight Statue with stone armor, shield, gold crest & halberd. */
	public static void knightMonument(OathholdCanvas c, int x, int y, int z) {
		if (!c.intersects(x - 2, z - 2, x + 2, z + 2)) return;
		c.fill(x - 1, y, z - 1, x + 1, y, z + 1, OathholdCanvas.TUFF_BRICKS);
		c.fill(x - 1, y + 1, z - 1, x + 1, y + 1, z + 1, OathholdCanvas.STONE_BRICKS);
		c.set(x, y + 1, z + 1, OathholdCanvas.GOLD);
		c.set(x, y + 1, z - 1, OathholdCanvas.GOLD);
		c.set(x - 1, y + 1, z, OathholdCanvas.GOLD);
		c.set(x + 1, y + 1, z, OathholdCanvas.GOLD);
		// Knight greaves, waist belt, armored cuirass with pauldrons & shield, visored helm & emerald plume
		c.set(x, y + 2, z, OathholdCanvas.POLISHED_DEEPSLATE);
		c.set(x, y + 3, z, OathholdCanvas.EMERALD_GREEN);
		c.set(x - 1, y + 3, z, OathholdCanvas.IRON_BARS);
		c.set(x, y + 4, z, OathholdCanvas.CHISELED_STONE_BRICKS);
		c.set(x - 1, y + 4, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
		c.set(x + 1, y + 4, z, Blocks.STONE_BRICK_WALL.defaultBlockState());
		c.set(x, y + 5, z, OathholdCanvas.STONE_BRICKS);
		c.set(x, y + 6, z, OathholdCanvas.GOLD);
		c.set(x, y + 7, z, OathholdCanvas.EMERALD_GREEN);
		// Upright Halberd shaft + axe head
		c.fill(x + 1, y + 2, z, x + 1, y + 7, z, OathholdCanvas.IRON_BARS);
		c.set(x + 1, y + 6, z, OathholdCanvas.POLISHED_ANDESITE);
		c.set(x + 1, y + 7, z, OathholdCanvas.GOLD);
		// Corner lanterns on pedestal
		c.set(x - 1, y + 2, z - 1, OathholdCanvas.LANTERN);
		c.set(x + 1, y + 2, z + 1, OathholdCanvas.LANTERN);
	}

	/** Trimmed stone-bordered green boxwood hedge parterre planter. */
	public static void hedgePlanter(OathholdCanvas c, int x0, int y, int z0, int x1, int z1) {
		int minX = Math.min(x0, x1), maxX = Math.max(x0, x1);
		int minZ = Math.min(z0, z1), maxZ = Math.max(z0, z1);
		if (!c.intersects(minX, minZ, maxX, maxZ)) return;
		BlockState curb = OathholdCanvas.slab(Blocks.STONE_BRICK_SLAB, false);
		boolean compactKnot = (maxX - minX) >= 6 && (maxX - minX) <= 12 && (maxZ - minZ) >= 6 && (maxZ - minZ) <= 12;
		int midX = (minX + maxX) / 2;
		int midZ = (minZ + maxZ) / 2;
		for (int x = Math.max(minX, c.clipMinLx()); x <= Math.min(maxX, c.clipMaxLx()); x++) {
			for (int z = Math.max(minZ, c.clipMinLz()); z <= Math.min(maxZ, c.clipMaxLz()); z++) {
				boolean border = x == minX || x == maxX || z == minZ || z == maxZ;
				if (border) {
					c.set(x, y, z, OathholdCanvas.TUFF_BRICKS);
					boolean corner = (x == minX || x == maxX) && (z == minZ || z == maxZ);
					c.set(x, y + 1, z, corner ? OathholdCanvas.LANTERN : curb);
				} else if (x == minX + 1 || x == maxX - 1 || z == minZ + 1 || z == maxZ - 1) {
					c.set(x, y, z, GRASS);
					c.set(x, y + 1, z, HEDGE_LEAVES);
					c.set(x, y + 2, z, HEDGE_LEAVES);
				} else {
					c.set(x, y, z, GRASS);
					if (compactKnot && x == midX && z == midZ) {
						c.set(x, y + 1, z, HEDGE_LEAVES);
						c.set(x, y + 2, z, HEDGE_LEAVES);
						c.set(x, y + 3, z, OathholdCanvas.LANTERN);
					} else if (compactKnot && (x == midX || z == midZ)) {
						c.set(x, y + 1, z, HEDGE_LEAVES);
					} else if (OathholdCanvas.hash(x, y, z) < 35) {
						c.set(x, y + 1, z, FLOWERS[OathholdCanvas.hash(z, y, x) % FLOWERS.length]);
					}
				}
			}
		}
	}

	/** 7x7 Striped Knight Tournament Pavilion Tent with forge/bench inside. */
	public static void tournamentTent(OathholdCanvas c, int cx, int y, int cz,
			BlockState colorA, BlockState colorB) {
		if (!c.intersects(cx - 4, cz - 4, cx + 4, cz + 4)) return;
		for (int px : new int[]{-3, 3}) {
			for (int pz : new int[]{-3, 3}) {
				c.fill(cx + px, y, cz + pz, cx + px, y + 2, cz + pz, OAK_FENCE);
			}
		}
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				int r = Math.max(Math.abs(dx), Math.abs(dz));
				int roofY = y + 3 + (3 - r);
				c.set(cx + dx, roofY, cz + dz, ((dx + dz) & 1) == 0 ? colorA : colorB);
			}
		}
		c.set(cx, y + 7, cz, OathholdCanvas.GOLD);
		c.set(cx, y + 5, cz, OathholdCanvas.HANGING_LANTERN);
		c.set(cx - 1, y, cz, Blocks.SMITHING_TABLE.defaultBlockState());
		c.set(cx + 1, y, cz, Blocks.ANVIL.defaultBlockState());
		c.set(cx, y, cz + 1, SPRUCE_PLATE);
	}

	/** Links stone bollards along a canal or harbor edge with horizontal iron chains. */
	public static void chainRailingBollard(OathholdCanvas c, int x, int y, int z, boolean hasLantern) {
		if (!c.intersects(x - 1, z - 1, x + 1, z + 1)) return;
		c.set(x, y, z, OathholdCanvas.TUFF_BRICKS);
		c.set(x, y + 1, z, hasLantern ? OathholdCanvas.LANTERN : OathholdCanvas.slab(Blocks.STONE_BRICK_SLAB, false));
	}

	/** Exposes the standard X/Z horizontal chain states cleanly. */
	public static BlockState horizontalChain(boolean alongZ) {
		return alongZ ? RAIL_CHAIN_Z : RAIL_CHAIN_X;
	}
}
