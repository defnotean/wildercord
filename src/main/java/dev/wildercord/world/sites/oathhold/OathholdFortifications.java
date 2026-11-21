package dev.wildercord.world.sites.oathhold;

import dev.wildercord.aura.world.AuraWorld;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

/**
 * All three defensive curtain wall rings, 48 bastion watchtowers, 8 twin-tower gatehouses with portcullises,
 * 8 arched stone moat bridges, the Acropolis retaining fortress wall & grand stairway, and harbor sea-gates.
 */
public final class OathholdFortifications {
	private OathholdFortifications() {}

	public static void build(OathholdCanvas c) {
		buildCurtainRing(c, OathholdOrigin.INNER_WALL_R, 6, 17, false);
		buildInnerGatehouses(c);
		buildInnerBastions(c);
		buildAcropolisRetainingWall(c);

		buildCurtainRing(c, OathholdOrigin.OUTER_WALL_R, 8, 20, true);
		buildOuterBarbicans(c);
		buildOuterBastions(c);
	}

	private static void buildCurtainRing(OathholdCanvas c, int r, int halfThick, int parapetY, boolean isOuter) {
		int outer = r + halfThick / 2;
		int inner = r - halfThick / 2;
		int minX = c.clipMinLx(), maxX = c.clipMaxLx();
		int minZ = c.clipMinLz(), maxZ = c.clipMaxLz();

		// South & North stretches
		for (int zSign : new int[]{1, -1}) {
			int z0 = zSign * (zSign > 0 ? inner : outer);
			int z1 = zSign * (zSign > 0 ? outer : inner);
			int loZ = Math.min(z0, z1), hiZ = Math.max(z0, z1);
			if (!c.intersects(-outer, loZ - 1, outer, hiZ + 1)) continue;
			int xStart = Math.max(-outer, minX), xEnd = Math.min(outer, maxX);
			for (int x = xStart; x <= xEnd; x++) {
				if (Math.abs(x) <= 9) continue; // Gate arch carved separately
				paintWallSliceZ(c, x, loZ, hiZ, zSign > 0 ? Direction.SOUTH : Direction.NORTH, parapetY, isOuter);
			}
		}

		// East & West stretches
		for (int xSign : new int[]{1, -1}) {
			int x0 = xSign * (xSign > 0 ? inner : outer);
			int x1 = xSign * (xSign > 0 ? outer : inner);
			int loX = Math.min(x0, x1), hiX = Math.max(x0, x1);
			if (!c.intersects(loX - 1, -outer, hiX + 1, outer)) continue;
			int zStart = Math.max(-outer, minZ), zEnd = Math.min(outer, maxZ);
			for (int z = zStart; z <= zEnd; z++) {
				if (Math.abs(z) <= 9) continue;
				if (isOuter && xSign > 0 && (Math.abs(z - 120) <= 13 || Math.abs(z + 120) <= 13)) {
					paintHarborSeaGateSlice(c, loX, hiX, z, parapetY);
					continue;
				}
				paintWallSliceX(c, loX, hiX, z, xSign > 0 ? Direction.EAST : Direction.WEST, parapetY, isOuter);
			}
		}
	}

	private static void paintWallSliceZ(OathholdCanvas c, int x, int z0, int z1, Direction outDir, int parapetY, boolean isOuter) {
		int outZ = outDir == Direction.SOUTH ? z1 : z0;
		int inZ = outDir == Direction.SOUTH ? z0 : z1;
		c.fillDeepslateBase(x, -5, z0, x, 4, z1);
		c.fillWeatheredStone(x, 5, z0, x, parapetY - 1, z1);
		for (int z = z0 + 1; z <= z1 - 1; z++) {
			c.set(x, parapetY, z, OathholdCanvas.POLISHED_ANDESITE);
		}
		// Outer machicolated corbels + arrow slits + crenellations + alternating lanterns
		c.set(x, parapetY, outZ, OathholdCanvas.TUFF_BRICKS);
		c.set(x, parapetY - 1, outZ + outDir.getStepZ(), ((x & 1) == 0) ? OathholdCanvas.TUFF_BRICKS : OathholdCanvas.AIR);
		c.set(x, parapetY, outZ + outDir.getStepZ(), OathholdCanvas.STONE_BRICKS);
		boolean merlon = Math.floorMod(x, 4) < 2;
		c.set(x, parapetY + 1, outZ + outDir.getStepZ(), OathholdCanvas.STONE_BRICKS);
		if (merlon) {
			c.set(x, parapetY + 2, outZ + outDir.getStepZ(), OathholdCanvas.STONE_BRICKS);
			c.set(x, parapetY + 3, outZ + outDir.getStepZ(), OathholdCanvas.slab(Blocks.STONE_BRICK_SLAB, false));
		}
		// Inner parapet low wall
		c.set(x, parapetY, inZ, OathholdCanvas.STONE_BRICKS);
		c.set(x, parapetY + 1, inZ, OathholdCanvas.STONE_BRICKS);

		// Wall lanterns & lower cyan soul-lantern sconces matching concept_city_walls_gatehouse_v2
		if (Math.floorMod(x, 8) == 0) {
			c.set(x, parapetY - 4, outZ + outDir.getStepZ(), OathholdCanvas.HANGING_LANTERN);
		}
		if (Math.floorMod(x, 16) == 8) {
			c.set(x, 6, outZ + outDir.getStepZ(), OathholdCanvas.HANGING_SOUL_LANTERN);
		}
		// Exterior timber access stairways inside curtain wall every 128 blocks
		int mod = Math.floorMod(x, 128);
		if (mod >= 48 && mod < 48 + parapetY && Math.abs(x) > 36) {
			int stepY = mod - 48 + 1;
			int stepZ = outDir == Direction.SOUTH ? inZ - 1 : inZ + 1;
			c.fill(x, 1, stepZ, x, stepY - 1, stepZ, OathholdCanvas.STONE_BRICKS);
			c.set(x, stepY, stepZ, OathholdCanvas.stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false));
		}
	}

	private static void paintWallSliceX(OathholdCanvas c, int x0, int x1, int z, Direction outDir, int parapetY, boolean isOuter) {
		int outX = outDir == Direction.EAST ? x1 : x0;
		int inX = outDir == Direction.EAST ? x0 : x1;
		c.fillDeepslateBase(x0, -5, z, x1, 4, z);
		c.fillWeatheredStone(x0, 5, z, x1, parapetY - 1, z);
		for (int x = x0 + 1; x <= x1 - 1; x++) {
			c.set(x, parapetY, z, OathholdCanvas.POLISHED_ANDESITE);
		}
		c.set(outX, parapetY, z, OathholdCanvas.TUFF_BRICKS);
		c.set(outX + outDir.getStepX(), parapetY - 1, z, ((z & 1) == 0) ? OathholdCanvas.TUFF_BRICKS : OathholdCanvas.AIR);
		c.set(outX + outDir.getStepX(), parapetY, z, OathholdCanvas.STONE_BRICKS);
		boolean merlon = Math.floorMod(z, 4) < 2;
		c.set(outX + outDir.getStepX(), parapetY + 1, z, OathholdCanvas.STONE_BRICKS);
		if (merlon) {
			c.set(outX + outDir.getStepX(), parapetY + 2, z, OathholdCanvas.STONE_BRICKS);
			c.set(outX + outDir.getStepX(), parapetY + 3, z, OathholdCanvas.slab(Blocks.STONE_BRICK_SLAB, false));
		}
		c.set(inX, parapetY, z, OathholdCanvas.STONE_BRICKS);
		c.set(inX, parapetY + 1, z, OathholdCanvas.STONE_BRICKS);

		if (Math.floorMod(z, 8) == 0) {
			c.set(outX + outDir.getStepX(), parapetY - 4, z, OathholdCanvas.HANGING_LANTERN);
		}
		if (Math.floorMod(z, 16) == 8) {
			c.set(outX + outDir.getStepX(), 6, z, OathholdCanvas.HANGING_SOUL_LANTERN);
		}
		int mod = Math.floorMod(z, 128);
		if (mod >= 48 && mod < 48 + parapetY && Math.abs(z) > 36 && Math.abs(Math.abs(z) - 120) > 22) {
			int stepY = mod - 48 + 1;
			int stepX = outDir == Direction.EAST ? inX - 1 : inX + 1;
			c.fill(stepX, 1, z, stepX, stepY - 1, z, OathholdCanvas.STONE_BRICKS);
			c.set(stepX, stepY, z, OathholdCanvas.stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, false));
		}
	}

	private static void paintHarborSeaGateSlice(OathholdCanvas c, int x0, int x1, int z, int parapetY) {
		int cz = z >= 0 ? 120 : -120;
		int dz = Math.abs(z - cz);
		int archTop = Math.max(12, parapetY - 2 - (dz * dz) / 28);
		c.clear(x0, 0, z, x1, archTop, z);
		for (int x = x0; x <= x1; x++) {
			c.set(x, -1, z, OathholdCanvas.WATER);
			for (int y = -8; y <= -2; y++) c.set(x, y, z, y == -8 ? OathholdCanvas.MOSSY_STONE_BRICKS : OathholdCanvas.WATER);
		}
		c.fillWeatheredStone(x0, archTop + 1, z, x1, parapetY - 1, z);
		c.fill(x0 + 1, parapetY, z, x1 - 1, parapetY, z, OathholdCanvas.POLISHED_ANDESITE);
		c.set(x1 + 1, parapetY + 1, z, OathholdCanvas.STONE_BRICKS);
		c.set(x0, parapetY + 1, z, OathholdCanvas.STONE_BRICKS);
		int midX = (x0 + x1) / 2;
		for (int y = Math.max(8, archTop - 4); y <= archTop; y++) {
			c.set(midX, y, z, OathholdCanvas.IRON_BARS);
		}
	}

	private static void buildInnerGatehouses(OathholdCanvas c) {
		int r = OathholdOrigin.INNER_WALL_R;
		// 1:1 South Grand Gatehouse + Moat Bridge
		buildCardinalGatehouseZ(c, r, true, true);
		buildCardinalGatehouseZ(c, -r, false, false);
		buildCardinalGatehouseX(c, r, false);
		buildCardinalGatehouseX(c, -r, false);
	}

	private static void buildOuterBarbicans(OathholdCanvas c) {
		int r = OathholdOrigin.OUTER_WALL_R;
		buildCardinalGatehouseZ(c, r, true, true);
		buildCardinalGatehouseZ(c, -r, false, true);
		buildCardinalGatehouseX(c, r, true);
		buildCardinalGatehouseX(c, -r, true);
	}

	private static void buildCardinalGatehouseZ(OathholdCanvas c, int wallZ, boolean guardBridge, boolean outerScale) {
		int sign = wallZ >= 0 ? 1 : -1;
		int minZ = Math.min(wallZ - 14, wallZ + sign * 36);
		int maxZ = Math.max(wallZ - 14, wallZ + sign * 36);
		if (!c.intersects(-30, minZ, 30, maxZ)) return;

		int towR = outerScale ? 7 : 6;
		int shaftH = outerScale ? 32 : 27;
		int spireH = outerScale ? 18 : 15;
		int gateH = outerScale ? 19 : 17;

		// Curtain lintel spanning above arch
		c.fillDeepslateBase(-12, -5, wallZ - 4, 12, 4, wallZ + 4);
		c.fillWeatheredStone(-12, 5, wallZ - 4, 12, gateH + 4, wallZ + 4);
		c.fill(-12, gateH + 5, wallZ - 4, 12, gateH + 5, wallZ + 4, OathholdCanvas.POLISHED_ANDESITE);
		for (int x = -12; x <= 12; x += 2) {
			c.set(x, gateH + 6, wallZ + sign * 4, OathholdCanvas.STONE_BRICKS);
			c.set(x, gateH + 7, wallZ + sign * 4, OathholdCanvas.TUFF_BRICKS);
		}
		// Carve tall vaulted Gothic gatehouse tunnel + raised Iron Portcullis
		c.gothicArchZ(0, 1, wallZ - 4, wallZ + 4, 7, outerScale ? 10 : 8, 5, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		for (int x = -6; x <= 6; x++) {
			for (int y = (outerScale ? 10 : 8); y <= (outerScale ? 14 : 12); y++) {
				c.set(x, y, wallZ, OathholdCanvas.IRON_BARS);
			}
		}
		c.set(-8, 8, wallZ + sign * 5, OathholdCanvas.HANGING_LANTERN);
		c.set(8, 8, wallZ + sign * 5, OathholdCanvas.HANGING_LANTERN);
		c.wallBannerStrip(-9, gateH + 2, wallZ + sign * 5, 6, sign > 0 ? Direction.SOUTH : Direction.NORTH);
		c.wallBannerStrip(9, gateH + 2, wallZ + sign * 5, 6, sign > 0 ? Direction.SOUTH : Direction.NORTH);

		// Twin Octagonal Gatehouse Towers + guardroom chests
		c.bastionTower(-16, 0, wallZ, towR, shaftH, spireH, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(16, 0, wallZ, towR, shaftH, spireH, OathholdCanvas.DEEPSLATE_TILES, true);
		c.fill(-16, 1, wallZ, -16, 1, wallZ, OathholdCanvas.STONE_BRICKS);
		c.chest(-15, 2, wallZ, Direction.EAST, OathholdSites.LOOT_ARMORY, wallZ * 13L);
		c.chest(15, 2, wallZ, Direction.WEST, OathholdSites.LOOT_ARMORY, wallZ * 17L);

		// Arched Stone Moat Bridge + lanterns + Oathbound Gate Sentries
		int bridgeStart = Math.min(wallZ + sign * 5, wallZ + sign * 36);
		int bridgeEnd = Math.max(wallZ + sign * 5, wallZ + sign * 36);
		c.fillWeatheredStone(-9, 0, bridgeStart, 9, 0, bridgeEnd);
		for (int z = bridgeStart; z <= bridgeEnd; z++) {
			c.fillPaving(-7, 1, z, 7, z);
			c.fill(-9, 2, z, -8, 2, z, OathholdCanvas.TUFF_BRICKS);
			c.fill(8, 2, z, 9, 2, z, OathholdCanvas.TUFF_BRICKS);
			c.clear(-7, 2, z, 7, 10, z);
			if (Math.floorMod(z, 6) == 0) {
				c.set(-8, 3, z, OathholdCanvas.STONE_BRICKS);
				c.set(8, 3, z, OathholdCanvas.STONE_BRICKS);
				c.set(-8, 4, z, OathholdCanvas.LANTERN);
				c.set(8, 4, z, OathholdCanvas.LANTERN);
			}
			// Bridge masonry piers in moat
			if (Math.floorMod(z, 8) == 0) {
				c.fillDeepslateBase(-9, -6, z, -7, -1, z);
				c.fillDeepslateBase(7, -6, z, 9, -1, z);
			}
		}

		if (guardBridge) {
			int gz = wallZ + sign * 18;
			float yaw = sign > 0 ? 0.0F : 180.0F;
			c.spawnMob(-5, 2, gz, AuraWorld.OATHBOUND_HALBERDIER, yaw, true);
			c.spawnMob(5, 2, gz, AuraWorld.OATHBOUND_BASTION, yaw, true);
		}
	}

	private static void buildCardinalGatehouseX(OathholdCanvas c, int wallX, boolean outerScale) {
		int sign = wallX >= 0 ? 1 : -1;
		int minX = Math.min(wallX - 14, wallX + sign * 36);
		int maxX = Math.max(wallX - 14, wallX + sign * 36);
		if (!c.intersects(minX, -30, maxX, 30)) return;

		int towR = outerScale ? 7 : 6;
		int shaftH = outerScale ? 32 : 27;
		int spireH = outerScale ? 18 : 15;
		int gateH = outerScale ? 19 : 17;

		c.fillDeepslateBase(wallX - 4, -5, -12, wallX + 4, 4, 12);
		c.fillWeatheredStone(wallX - 4, 5, -12, wallX + 4, gateH + 4, 12);
		c.fill(wallX - 4, gateH + 5, -12, wallX + 4, gateH + 5, 12, OathholdCanvas.POLISHED_ANDESITE);
		c.gothicArchX(wallX - 4, wallX + 4, 1, 0, 7, outerScale ? 10 : 8, 5, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);
		for (int z = -6; z <= 6; z++) {
			for (int y = (outerScale ? 10 : 8); y <= (outerScale ? 14 : 12); y++) {
				c.set(wallX, y, z, OathholdCanvas.IRON_BARS);
			}
		}
		c.wallBannerStrip(wallX + sign * 5, gateH + 2, -9, 6, sign > 0 ? Direction.EAST : Direction.WEST);
		c.wallBannerStrip(wallX + sign * 5, gateH + 2, 9, 6, sign > 0 ? Direction.EAST : Direction.WEST);

		c.bastionTower(wallX, 0, -16, towR, shaftH, spireH, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(wallX, 0, 16, towR, shaftH, spireH, OathholdCanvas.DEEPSLATE_TILES, true);

		int b0 = Math.min(wallX + sign * 5, wallX + sign * 36);
		int b1 = Math.max(wallX + sign * 5, wallX + sign * 36);
		c.fillWeatheredStone(b0, 0, -9, b1, 0, 9);
		for (int x = b0; x <= b1; x++) {
			c.fillPaving(x, 1, -7, x, 7);
			c.fill(x, 2, -9, x, 2, -8, OathholdCanvas.TUFF_BRICKS);
			c.fill(x, 2, 8, x, 2, 9, OathholdCanvas.TUFF_BRICKS);
			c.clear(x, 2, -7, x, 10, 7);
			if (Math.floorMod(x, 6) == 0) {
				c.set(x, 3, -8, OathholdCanvas.STONE_BRICKS);
				c.set(x, 3, 8, OathholdCanvas.STONE_BRICKS);
				c.set(x, 4, -8, OathholdCanvas.LANTERN);
				c.set(x, 4, 8, OathholdCanvas.LANTERN);
			}
			if (Math.floorMod(x, 8) == 0) {
				c.fillDeepslateBase(x, -6, -9, x, -1, -7);
				c.fillDeepslateBase(x, -6, 7, x, -1, 9);
			}
		}
	}

	private static void buildInnerBastions(OathholdCanvas c) {
		int r = OathholdOrigin.INNER_WALL_R;
		int[][] spots = {
			{-r, r}, {r, r}, {-r, -r}, {r, -r},
			{-112, r}, {112, r}, {-112, -r}, {112, -r},
			{-r, -112}, {-r, 112}, {r, -112}, {r, 112}
		};
		for (int[] s : spots) {
			c.bastionTower(s[0], 0, s[1], 6, 24, 0, OathholdCanvas.DEEPSLATE_TILES, true);
		}
	}

	private static void buildOuterBastions(OathholdCanvas c) {
		int r = OathholdOrigin.OUTER_WALL_R;
		int[] pts = {-960, -720, -480, -240, 240, 480, 720, 960};
		for (int p : pts) {
			c.bastionTower(p, 0, r, 7, 29, 0, OathholdCanvas.DEEPSLATE_TILES, true);
			c.bastionTower(p, 0, -r, 7, 29, 0, OathholdCanvas.DEEPSLATE_TILES, true);
			c.bastionTower(-r, 0, p, 7, 29, 0, OathholdCanvas.DEEPSLATE_TILES, true);
			if (Math.abs(p) >= 240) {
				c.bastionTower(r, 0, p, 7, 29, 0, OathholdCanvas.DEEPSLATE_TILES, true);
			}
		}
		// Twin Harbor Sea-Gate Towers framing Eastern Harbor openings
		c.bastionTower(r, 0, -140, 8, 32, 16, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(r, 0, -100, 8, 32, 16, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(r, 0, 100, 8, 32, 16, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(r, 0, 140, 8, 32, 16, OathholdCanvas.DEEPSLATE_TILES, true);
	}

	private static void buildAcropolisRetainingWall(OathholdCanvas c) {
		int wz = OathholdOrigin.ACROPOLIS_MAX_Z;
		int minX = -OathholdOrigin.ACROPOLIS_HALF_X;
		int maxX = OathholdOrigin.ACROPOLIS_HALF_X;
		if (!c.intersects(minX - 8, wz - 22, maxX + 8, wz + 14)) return;

		// Solid masonry retaining wall + arched arcade niches + battlements along lz = -44
		c.fillDeepslateBase(minX, -2, wz - 3, maxX, 4, wz + 3);
		c.fillWeatheredStone(minX, 5, wz - 3, maxX, 20, wz + 3);
		c.fill(minX, 17, wz - 3, maxX, 17, wz + 3, OathholdCanvas.POLISHED_ANDESITE);
		for (int x = Math.max(minX, c.clipMinLx()); x <= Math.min(maxX, c.clipMaxLx()); x++) {
			if (Math.abs(x) <= 8) continue;
			// Arched wall ribs every 8 blocks
			if (Math.floorMod(x, 8) == 0) {
				c.fill(x, 1, wz + 4, x, 17, wz + 4, OathholdCanvas.TUFF_BRICKS);
				c.set(x, 18, wz + 4, OathholdCanvas.HANGING_LANTERN);
			}
			boolean merlon = Math.floorMod(x, 4) < 2;
			if (merlon) {
				c.set(x, 21, wz + 3, OathholdCanvas.STONE_BRICKS);
				c.set(x, 22, wz + 3, OathholdCanvas.slab(Blocks.STONE_BRICK_SLAB, false));
			}
		}

		// Central Twin Acropolis Gatehouse Towers & Grand Ascending Staircase to Upper Acropolis (+16)
		c.bastionTower(-13, 0, wz, 5, 29, 0, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(13, 0, wz, 5, 29, 0, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(-184, 0, wz, 6, 30, 0, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(184, 0, wz, 6, 30, 0, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(-104, 0, wz, 5, 28, 0, OathholdCanvas.DEEPSLATE_TILES, true);
		c.bastionTower(104, 0, wz, 5, 28, 0, OathholdCanvas.DEEPSLATE_TILES, true);

		// Clear tall arch through Acropolis Gatehouse
		c.gothicArchZ(0, 1, wz - 4, wz + 4, 8, 12, 6, OathholdCanvas.TUFF_BRICKS, OathholdCanvas.AIR);

		// Grand 16-block-wide stone staircase climbing from (ly=1, lz=-36) to (ly=16, lz=-52)
		for (int step = 0; step <= 15; step++) {
			int sz = -37 - step;
			int sy = step + 1;
			c.fill(-8, 1, sz, 8, sy - 1, sz, OathholdCanvas.STONE_BRICKS);
			c.fill(-7, sy, sz, 7, sy, sz, OathholdCanvas.stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, false));
			c.clear(-7, sy + 1, sz, 7, 26, sz);
			c.set(-8, sy, sz, OathholdCanvas.TUFF_BRICKS);
			c.set(8, sy, sz, OathholdCanvas.TUFF_BRICKS);
			if (step % 4 == 0) {
				c.set(-8, sy + 1, sz, OathholdCanvas.LANTERN);
				c.set(8, sy + 1, sz, OathholdCanvas.LANTERN);
			}
		}
		// Side East & West Acropolis retainer walls rising to y=20 around perimeter
		c.fillWeatheredStone(-OathholdOrigin.ACROPOLIS_HALF_X - 3, 0, OathholdOrigin.ACROPOLIS_MIN_Z,
			-OathholdOrigin.ACROPOLIS_HALF_X, 20, wz);
		c.fillWeatheredStone(OathholdOrigin.ACROPOLIS_HALF_X, 0, OathholdOrigin.ACROPOLIS_MIN_Z,
			OathholdOrigin.ACROPOLIS_HALF_X + 3, 20, wz);
	}
}
