package dev.wildercord.world.sites.ocean;

import dev.wildercord.ocean.BrineMerchant;
import dev.wildercord.ocean.OceanBlocks;
import dev.wildercord.ocean.OceanEntities;
import dev.wildercord.ocean.OceanRules;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Coral-Dome Tidefolk Sanctuary (49x18x49):
 * Central 11x11 open-water Conduit Plaza linked by 4 dry cyan-glass airlock tunnels
 * to 4 furnished 15x11x15 geodesic domes (Pearl Jeweler, Abyssal Cartographer, Kelp Alchemist, Wayfarer Rest).
 */
public final class CoralDomeSanctuaryPiece extends OceanPiece {
	public static final int WIDTH = OceanRules.SANCTUARY_SIZE_XZ;
	public static final int HEIGHT = OceanRules.SANCTUARY_HEIGHT;
	public static final int DEPTH = OceanRules.SANCTUARY_SIZE_XZ;
	public static final int BASE = 3;
	private static final int CX = 24;
	private static final int CZ = 24;

	private static final BlockState[] CORAL = {
		Blocks.TUBE_CORAL_BLOCK.defaultBlockState(),
		Blocks.BRAIN_CORAL_BLOCK.defaultBlockState(),
		Blocks.FIRE_CORAL_BLOCK.defaultBlockState(),
		Blocks.HORN_CORAL_BLOCK.defaultBlockState()
	};

	public CoralDomeSanctuaryPiece(int x, int y, int z, Direction facing) {
		super(OceanSites.CORAL_DOME_SANCTUARY, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public CoralDomeSanctuaryPiece(CompoundTag tag) {
		super(OceanSites.CORAL_DOME_SANCTUARY, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext ctx) {
		ChunkPos cp = ctx.chunkPos();
		int x = cp.getMinBlockX();
		int z = cp.getMinBlockZ();
		int floor = oceanFloorY(ctx, x + CX, z + CZ);
		int sea = ctx.chunkGenerator().getSeaLevel();
		if (sea - floor < 14) return Optional.empty();
		CoralDomeSanctuaryPiece piece = new CoralDomeSanctuaryPiece(x, floor - BASE, z, Direction.NORTH);
		return Optional.of(new Structure.GenerationStub(new BlockPos(x + CX, floor, z + CZ), b -> b.addPiece(piece)));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunkPos, BlockPos reference) {
		buildConduitPlaza(level, bb);

		int[][] domeCenters = {{CX, 9}, {39, CZ}, {CX, 39}, {9, CZ}};
		for (int[] dc : domeCenters) {
			buildGeodesicDome(level, bb, dc[0], dc[1]);
		}

		// Four 5x5 arched cyan-glass dry tunnels with oxidized copper ribs
		buildTunnelZ(level, bb, CX, 15, 19);
		buildTunnelZ(level, bb, CX, 29, 33);
		buildTunnelX(level, bb, CZ, 15, 19);
		buildTunnelX(level, bb, CZ, 29, 33);

		// Re-carve dome interiors dry AFTER tunnels
		for (int[] dc : domeCenters) {
			carveDomeInteriorAir(level, bb, dc[0], dc[1]);
		}

		furnishJewelerDome(level, bb, CX, 9);
		furnishCartographerDome(level, bb, 39, CZ);
		furnishAlchemistDome(level, bb, CX, 39);
		furnishRestDome(level, bb, 9, CZ);
	}

	private void buildConduitPlaza(WorldGenLevel level, BoundingBox bb) {
		footingRect(level, bb, CX - 5, CZ - 5, CX + 5, CZ + 5, BASE, PRISMARINE_BRICKS, DARK_PRISMARINE);
		for (int x = CX - 5; x <= CX + 5; x++) {
			for (int z = CZ - 5; z <= CZ + 5; z++) {
				boolean border = x == CX - 5 || x == CX + 5 || z == CZ - 5 || z == CZ + 5;
				boolean cross = Math.abs(x - CX) <= 1 || Math.abs(z - CZ) <= 1;
				if (border) {
					set(level, bb, DARK_PRISMARINE, x, BASE, z);
				} else if (cross) {
					set(level, bb, (Math.abs(x - CX) == 0 && Math.abs(z - CZ) == 0) ? SEA_LANTERN : PRISMARINE_BRICKS, x, BASE, z);
				} else if ((Math.abs(x - CX) + Math.abs(z - CZ)) % 4 == 0) {
					set(level, bb, SEA_LANTERN, x, BASE, z);
				}
				fill(level, bb, x, BASE + 1, z, x, BASE + 7, z, WATER);
			}
		}
		// Central Conduit Shrine cage
		for (int dy = 1; dy <= 5; dy++) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					boolean onFrame = (Math.abs(dx) == 2 || Math.abs(dz) == 2 || dy == 1 || dy == 5)
						&& (Math.abs(dx) + Math.abs(dz) == 2 || dy == 1 || dy == 5);
					if (onFrame && !(dy > 1 && dy < 5 && (dx == 0 || dz == 0))) {
						set(level, bb, PRISMARINE_BRICKS, CX + dx, BASE + dy, CZ + dz);
					}
				}
			}
		}
		set(level, bb, Blocks.CONDUIT.defaultBlockState(), CX, BASE + 3, CZ);
		// Coral reef corners on plaza
		int[][] corners = {{-4, -4}, {4, -4}, {-4, 4}, {4, 4}};
		for (int i = 0; i < corners.length; i++) {
			int px = CX + corners[i][0];
			int pz = CZ + corners[i][1];
			set(level, bb, SAND, px, BASE, pz);
			set(level, bb, CORAL[i], px, BASE + 1, pz);
		}
	}

	private void buildGeodesicDome(WorldGenLevel level, BoundingBox bb, int dxCenter, int dzCenter) {
		for (int x = dxCenter - 7; x <= dxCenter + 7; x++) {
			for (int z = dzCenter - 7; z <= dzCenter + 7; z++) {
				double d2 = dist2(x, z, dxCenter, dzCenter);
				if (d2 > 7.4) continue;
				set(level, bb, d2 > 6.1 ? DARK_PRISMARINE : PRISMARINE_BRICKS, x, BASE, z);
				fillColumnDown(level, DARK_PRISMARINE, x, BASE - 1, z, bb);
			}
		}
		for (int x = dxCenter - 7; x <= dxCenter + 7; x++) {
			for (int z = dzCenter - 7; z <= dzCenter + 7; z++) {
				for (int y = BASE + 1; y <= BASE + 10; y++) {
					double r = Math.sqrt(Math.pow(x - dxCenter, 2) + Math.pow((y - BASE) * 0.9, 2) + Math.pow(z - dzCenter, 2));
					if (r >= 6.2 && r <= 7.45) {
						boolean rib = (x == dxCenter || z == dzCenter) && y > BASE + 1;
						set(level, bb, (rib || y == BASE + 1) ? COPPER_OXIDIZED : CYAN_GLASS, x, y, z);
					}
				}
			}
		}
	}

	private void carveDomeInteriorAir(WorldGenLevel level, BoundingBox bb, int dxCenter, int dzCenter) {
		for (int x = dxCenter - 6; x <= dxCenter + 6; x++) {
			for (int z = dzCenter - 6; z <= dzCenter + 6; z++) {
				for (int y = BASE + 1; y <= BASE + 9; y++) {
					double r = Math.sqrt(Math.pow(x - dxCenter, 2) + Math.pow((y - BASE) * 0.9, 2) + Math.pow(z - dzCenter, 2));
					if (r < 6.15) {
						set(level, bb, AIR, x, y, z);
					}
				}
			}
		}
	}

	private void buildTunnelZ(WorldGenLevel level, BoundingBox bb, int cx, int z0, int z1) {
		for (int z = z0; z <= z1; z++) {
			boolean rib = (z % 2 == 0);
			fill(level, bb, cx - 2, BASE, z, cx + 2, BASE, z, PRISMARINE_BRICKS);
			fill(level, bb, cx - 1, BASE + 1, z, cx + 1, BASE + 3, z, AIR);
			for (int y = BASE + 1; y <= BASE + 4; y++) {
				set(level, bb, rib ? COPPER_OXIDIZED : CYAN_GLASS, cx - 2, y, z);
				set(level, bb, rib ? COPPER_OXIDIZED : CYAN_GLASS, cx + 2, y, z);
			}
			fill(level, bb, cx - 1, BASE + 4, z, cx + 1, BASE + 4, z, rib ? COPPER_OXIDIZED : CYAN_GLASS);
			if (z == z0 || z == z1) {
				for (int y = BASE + 1; y <= BASE + 3; y++) {
					set(level, bb, AIR, cx, y, z);
				}
			}
		}
	}

	private void buildTunnelX(WorldGenLevel level, BoundingBox bb, int cz, int x0, int x1) {
		for (int x = x0; x <= x1; x++) {
			boolean rib = (x % 2 == 0);
			fill(level, bb, x, BASE, cz - 2, x, BASE, cz + 2, PRISMARINE_BRICKS);
			fill(level, bb, x, BASE + 1, cz - 1, x, BASE + 3, cz + 1, AIR);
			for (int y = BASE + 1; y <= BASE + 4; y++) {
				set(level, bb, rib ? COPPER_OXIDIZED : CYAN_GLASS, x, y, cz - 2);
				set(level, bb, rib ? COPPER_OXIDIZED : CYAN_GLASS, x, y, cz + 2);
			}
			fill(level, bb, x, BASE + 4, cz - 1, x, BASE + 4, cz + 1, rib ? COPPER_OXIDIZED : CYAN_GLASS);
			if (x == x0 || x == x1) {
				for (int y = BASE + 1; y <= BASE + 3; y++) {
					set(level, bb, AIR, x, y, cz);
				}
			}
		}
	}

	private void furnishJewelerDome(WorldGenLevel level, BoundingBox bb, int cx, int cz) {
		fill(level, bb, cx - 2, BASE, cz - 2, cx + 2, BASE, cz + 2, SPRUCE_PLANKS);
		set(level, bb, Blocks.STONECUTTER.defaultBlockState(), cx - 3, BASE + 1, cz - 2);
		set(level, bb, CORAL[0], cx + 3, BASE + 1, cz - 2);
		set(level, bb, OceanBlocks.SIREN_PEARL_LANTERN.defaultBlockState(), cx, BASE + 2, cz - 3);
		set(level, bb, Blocks.PRISMARINE_BRICK_SLAB.defaultBlockState(), cx, BASE + 1, cz - 3);
		chest(level, bb, cx + 2, BASE + 1, cz - 3, Direction.SOUTH, OceanSites.SANCTUARY_LOOT, 101L);
		spawnMob(level, bb, OceanEntities.BRINE_MERCHANT, cx, BASE + 1, cz,
			m -> m.setMerchantProfession(OceanRules.MerchantProfession.PEARL_JEWELER));
	}

	private void furnishCartographerDome(WorldGenLevel level, BoundingBox bb, int cx, int cz) {
		fill(level, bb, cx - 2, BASE, cz - 2, cx + 2, BASE, cz + 2, SPRUCE_PLANKS);
		set(level, bb, Blocks.CARTOGRAPHY_TABLE.defaultBlockState(), cx + 2, BASE + 1, cz - 2);
		set(level, bb, Blocks.LECTERN.defaultBlockState(), cx + 2, BASE + 1, cz + 2);
		set(level, bb, Blocks.BOOKSHELF.defaultBlockState(), cx + 3, BASE + 1, cz);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), cx + 3, BASE + 2, cz);
		chest(level, bb, cx + 1, BASE + 1, cz - 3, Direction.SOUTH, OceanSites.SANCTUARY_LOOT, 202L);
		spawnMob(level, bb, OceanEntities.BRINE_MERCHANT, cx, BASE + 1, cz,
			m -> m.setMerchantProfession(OceanRules.MerchantProfession.ABYSSAL_CARTOGRAPHER));
	}

	private void furnishAlchemistDome(WorldGenLevel level, BoundingBox bb, int cx, int cz) {
		fill(level, bb, cx - 2, BASE, cz - 2, cx + 2, BASE, cz + 2, SPRUCE_PLANKS);
		set(level, bb, Blocks.BREWING_STAND.defaultBlockState(), cx - 2, BASE + 1, cz + 2);
		set(level, bb, Blocks.CAULDRON.defaultBlockState(), cx + 2, BASE + 1, cz + 2);
		set(level, bb, Blocks.DRIED_KELP_BLOCK.defaultBlockState(), cx - 3, BASE + 1, cz);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), cx, BASE + 2, cz + 3);
		barrel(level, bb, cx + 2, BASE + 1, cz + 3, Direction.NORTH, OceanSites.SANCTUARY_LOOT, 303L);
		spawnMob(level, bb, OceanEntities.BRINE_MERCHANT, cx, BASE + 1, cz,
			m -> m.setMerchantProfession(OceanRules.MerchantProfession.KELP_ALCHEMIST));
	}

	private void furnishRestDome(WorldGenLevel level, BoundingBox bb, int cx, int cz) {
		fill(level, bb, cx - 2, BASE, cz - 2, cx + 2, BASE, cz + 2, SPRUCE_PLANKS);
		cyanBed(level, bb, cx - 3, BASE + 1, cz - 2, Direction.WEST);
		cyanBed(level, bb, cx - 3, BASE + 1, cz + 2, Direction.WEST);
		set(level, bb, Blocks.GRINDSTONE.defaultBlockState(), cx - 2, BASE + 1, cz);
		set(level, bb, Blocks.CRAFTING_TABLE.defaultBlockState(), cx + 1, BASE + 1, cz + 3);
		set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), cx, BASE + 6, cz);
		chest(level, bb, cx + 2, BASE + 1, cz - 3, Direction.SOUTH, OceanSites.SANCTUARY_LOOT, 404L);
		spawnMob(level, bb, OceanEntities.PEARL_PUP, cx, BASE + 1, cz, pup -> {});
	}
}
