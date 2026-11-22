package dev.wildercord.world.sites.ocean;

import dev.wildercord.ocean.OceanBlocks;
import dev.wildercord.ocean.OceanEntities;
import dev.wildercord.ocean.OceanRules;
import dev.wildercord.ocean.SovereignAltarBlockEntity;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Abyssal Trench Citadel (65x28x65):
 * Western stepped dark_prismarine & gold Temple Ziggurat with Grand Gatehouse Arch and
 * vaulted Inner Sovereign Sanctum, opening east onto a sunken deepslate Boss Colosseum
 * ringed by 8 sea_lantern obelisks (8 blocks tall), central Sovereign Altar, and Thalassor.
 */
public final class AbyssalCitadelPiece extends OceanPiece {
	public static final int WIDTH = OceanRules.CITADEL_SIZE_XZ;
	public static final int HEIGHT = OceanRules.CITADEL_HEIGHT;
	public static final int DEPTH = OceanRules.CITADEL_SIZE_XZ;
	public static final int BASE = 3;
	private static final int CZ = 32;
	private static final int ARENA_CX = 47;

	public AbyssalCitadelPiece(int x, int y, int z, Direction facing) {
		super(OceanSites.ABYSSAL_CITADEL, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public AbyssalCitadelPiece(CompoundTag tag) {
		super(OceanSites.ABYSSAL_CITADEL, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext ctx) {
		ChunkPos cp = ctx.chunkPos();
		int x = cp.getMinBlockX();
		int z = cp.getMinBlockZ();
		BoundingBox probe = StructurePiece.makeBoundingBox(x, 0, z, Direction.EAST, WIDTH, 1, DEPTH);
		var floorOpt = locateSubmergedFloor(ctx, probe, 20, 12);
		if (floorOpt.isEmpty()) return Optional.empty();
		int floor = floorOpt.getAsInt();
		AbyssalCitadelPiece piece = new AbyssalCitadelPiece(x, floor - BASE, z, Direction.EAST);
		BlockPos center = probe.getCenter();
		return Optional.of(new Structure.GenerationStub(new BlockPos(center.getX(), floor, center.getZ()), b -> b.addPiece(piece)));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunkPos, BlockPos reference) {
		buildWesternTempleZiggurat(level, bb);
		buildInnerSanctum(level, bb);
		buildEasternColosseum(level, bb);
		buildEightObelisks(level, bb);
		placeSovereignAltarAndBoss(level, bb);
	}

	private void buildWesternTempleZiggurat(WorldGenLevel level, BoundingBox bb) {
		footingRect(level, bb, 2, 10, 30, 54, BASE, DARK_PRISMARINE, DEEPSLATE_BRICKS);
		// 4 stepped pyramid tiers with sloped stair buttresses, fluted colonnades & spired turret pinnacles
		int[][] tiers = {
			{2, 10, 30, 54, BASE + 1, BASE + 4},
			{6, 15, 28, 49, BASE + 5, BASE + 9},
			{10, 20, 26, 44, BASE + 10, BASE + 14},
			{14, 25, 23, 39, BASE + 15, BASE + 19}
		};
		for (int t = 0; t < tiers.length; t++) {
			int x0 = tiers[t][0], z0 = tiers[t][1], x1 = tiers[t][2], z1 = tiers[t][3];
			int y0 = tiers[t][4], y1 = tiers[t][5];
			fill(level, bb, x0, y0, z0, x1, y1, z1, t % 2 == 0 ? DARK_PRISMARINE : PRISMARINE_BRICKS);
			// Sloped cornice stair trim, fluted pilasters & gold filigree belt
			for (int x = x0; x <= x1; x++) {
				boolean pilaster = (x - x0) % 4 == 0;
				set(level, bb, GOLD, x, y1, z0);
				set(level, bb, GOLD, x, y1, z1);
				set(level, bb, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.SOUTH, false).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), x, y1 + 1, z0);
				set(level, bb, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.NORTH, false).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), x, y1 + 1, z1);
				if (pilaster) {
					for (int py = y0; py < y1; py++) {
						set(level, bb, py == y0 + 2 ? SEA_LANTERN : CHISELED_DEEPSLATE, x, py, z0);
						set(level, bb, py == y0 + 2 ? SEA_LANTERN : CHISELED_DEEPSLATE, x, py, z1);
					}
				}
			}
			for (int z = z0; z <= z1; z++) {
				boolean pilaster = (z - z0) % 4 == 0;
				set(level, bb, GOLD, x0, y1, z);
				set(level, bb, GOLD, x1, y1, z);
				set(level, bb, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.EAST, false).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), x0, y1 + 1, z);
				set(level, bb, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.WEST, false).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), x1, y1 + 1, z);
				if (pilaster) {
					for (int py = y0; py < y1; py++) {
						set(level, bb, py == y0 + 2 ? SEA_LANTERN : CHISELED_DEEPSLATE, x0, py, z);
					}
				}
			}
			for (int cx : new int[] {x0, x1}) {
				for (int cz : new int[] {z0, z1}) {
					set(level, bb, COPPER_OXIDIZED, cx, y1 + 1, cz);
					set(level, bb, GOLD, cx, y1 + 2, cz);
					set(level, bb, SEA_LANTERN, cx, y1 + 3, cz);
					set(level, bb, Blocks.LIGHTNING_ROD.waxed().oxidized().defaultBlockState().trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), cx, y1 + 4, cz);
				}
			}
		}
		// Grand Crown Golden Trident Monument atop Ziggurat Peak (fits cleanly inside HEIGHT=28 -> y<=BASE+24)
		for (int dy = 20; dy <= 24; dy++) {
			set(level, bb, dy == 24 ? SEA_LANTERN : GOLD, 18, BASE + dy, CZ);
		}
		for (int dz : new int[] {-2, 2}) {
			set(level, bb, GOLD, 18, BASE + 21, CZ + dz / 2);
			set(level, bb, GOLD, 18, BASE + 22, CZ + dz);
			set(level, bb, GOLD, 18, BASE + 23, CZ + dz);
			set(level, bb, SEA_LANTERN, 18, BASE + 24, CZ + dz);
		}
	}

	private void buildInnerSanctum(WorldGenLevel level, BoundingBox bb) {
		// Vaulted Inner Sovereign Sanctum carved through temple heart + East Gatehouse Arch
		fill(level, bb, 10, BASE + 1, CZ - 10, 29, BASE + 11, CZ + 10, WATER);
		// Vertical sea_lantern ribs + vaulted roof ribs + hanging siren pearl chandeliers
		for (int x = 12; x <= 26; x += 4) {
			for (int y = BASE + 1; y <= BASE + 10; y++) {
				set(level, bb, y % 2 == 0 ? SEA_LANTERN : CHISELED_DEEPSLATE, x, y, CZ - 10);
				set(level, bb, y % 2 == 0 ? SEA_LANTERN : CHISELED_DEEPSLATE, x, y, CZ + 10);
			}
			set(level, bb, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.SOUTH, true).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), x, BASE + 10, CZ - 9);
			set(level, bb, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.NORTH, true).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), x, BASE + 10, CZ + 9);
			set(level, bb, Blocks.IRON_CHAIN.defaultBlockState().trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), x, BASE + 10, CZ);
			set(level, bb, OceanBlocks.SIREN_PEARL_LANTERN.defaultBlockState().trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), x, BASE + 9, CZ);
		}
		// Grand Gatehouse Arch opening east (x=28..30, z=27..37) + Flanking Sentinel Pillars
		fill(level, bb, 28, BASE + 1, CZ - 4, 30, BASE + 9, CZ + 4, WATER);
		for (int y = BASE + 1; y <= BASE + 11; y++) {
			set(level, bb, y % 3 == 0 ? SEA_LANTERN : GOLD, 29, y, CZ - 5);
			set(level, bb, y % 3 == 0 ? SEA_LANTERN : GOLD, 29, y, CZ + 5);
			if (y <= BASE + 6) {
				set(level, bb, COPPER_OXIDIZED, 30, y, CZ - 6);
				set(level, bb, COPPER_OXIDIZED, 30, y, CZ + 6);
			}
		}
		set(level, bb, OceanBlocks.SIREN_PEARL_LANTERN.defaultBlockState().trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 30, BASE + 7, CZ - 6);
		set(level, bb, OceanBlocks.SIREN_PEARL_LANTERN.defaultBlockState().trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 30, BASE + 7, CZ + 6);
		fill(level, bb, 29, BASE + 10, CZ - 5, 29, BASE + 11, CZ + 5, DARK_PRISMARINE);

		// Western gilded trident dais & Citadel vault loot
		fill(level, bb, 11, BASE + 1, CZ - 3, 14, BASE + 2, CZ + 3, POLISHED_DEEPSLATE);
		fill(level, bb, 12, BASE + 3, CZ - 1, 13, BASE + 3, CZ + 1, GOLD);
		chest(level, bb, 13, BASE + 4, CZ, Direction.EAST, OceanSites.CITADEL_LOOT, 51L);
		chest(level, bb, 12, BASE + 3, CZ - 2, Direction.EAST, OceanSites.CITADEL_LOOT, 52L);
	}

	private void buildEasternColosseum(WorldGenLevel level, BoundingBox bb) {
		// Sunken deepslate fighting pit (x=31..63, z=10..54 -> radius ~21 around (47,32))
		for (int x = 31; x <= 63; x++) {
			for (int z = 10; z <= 54; z++) {
				double d = dist2(x, z, ARENA_CX, CZ);
				if (d > 22.0) continue;
				fillColumnDown(level, DEEPSLATE_BRICKS, x, BASE - 2, z, bb);
				if (d <= 15.0) {
					BlockState floor = ((Math.abs(x - ARENA_CX) == Math.abs(z - CZ)) || d < 4.5)
						? CHISELED_DEEPSLATE
						: DEEPSLATE_TILES;
					set(level, bb, floor, x, BASE - 1, z);
					fill(level, bb, x, BASE, z, x, BASE + 14, z, WATER);
				} else if (d <= 21.5) {
					// Tiered colosseum steps & dark_prismarine parapet
					set(level, bb, DEEPSLATE_TILES, x, BASE, z);
					set(level, bb, POLISHED_DEEPSLATE, x, BASE + 1, z);
					if (d >= 19.8) {
						boolean eastGap = x >= 61 && Math.abs(z - CZ) <= 4;
						if (!eastGap) {
							fill(level, bb, x, BASE + 2, z, x, BASE + 4, z, DARK_PRISMARINE);
							if (((x + z) % 3) == 0) {
								set(level, bb, Blocks.PRISMARINE_WALL.defaultBlockState(), x, BASE + 5, z);
							}
						}
					}
				}
			}
		}
		// Ruined Eastern Portal Arches + Ceremonial Lanterns
		for (int y = BASE + 1; y <= BASE + 10; y++) {
			set(level, bb, DARK_PRISMARINE, 62, y, CZ - 5);
			set(level, bb, DARK_PRISMARINE, 62, y, CZ + 5);
		}
		fill(level, bb, 62, BASE + 9, CZ - 5, 62, BASE + 10, CZ + 5, GOLD);
		set(level, bb, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.SOUTH, true).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 62, BASE + 8, CZ - 4);
		set(level, bb, stair(Blocks.DARK_PRISMARINE_STAIRS, Direction.NORTH, true).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 62, BASE + 8, CZ + 4);
		set(level, bb, OceanBlocks.SIREN_PEARL_LANTERN.defaultBlockState().trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 62, BASE + 11, CZ - 5);
		set(level, bb, OceanBlocks.SIREN_PEARL_LANTERN.defaultBlockState().trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 62, BASE + 11, CZ + 5);

		// Citadel Sentinel guards
		spawnMob(level, bb, OceanEntities.DREAD_NAUTILUS_SENTINEL, 36, BASE + 2, CZ - 11, m -> {});
		spawnMob(level, bb, OceanEntities.DREAD_NAUTILUS_SENTINEL, 58, BASE + 2, CZ + 11, m -> {});
	}

	private void buildEightObelisks(WorldGenLevel level, BoundingBox bb) {
		double radius = 16.5;
		for (int i = 0; i < OceanRules.CITADEL_OBELISK_COUNT; i++) {
			double angle = i * (Math.PI * 2.0 / OceanRules.CITADEL_OBELISK_COUNT) + (Math.PI / 8.0);
			int ox = ARENA_CX + (int) Math.round(Math.cos(angle) * radius);
			int oz = CZ + (int) Math.round(Math.sin(angle) * radius);
			// Exact 8-block obelisk: 2 plinth + 5 sea_lantern core + 1 slab cap = 8 tall
			set(level, bb, DARK_PRISMARINE, ox, BASE, oz);
			set(level, bb, DARK_PRISMARINE, ox, BASE + 1, oz);
			for (int y = BASE + 2; y <= BASE + 6; y++) {
				set(level, bb, SEA_LANTERN, ox, y, oz);
			}
			set(level, bb, slab(Blocks.PRISMARINE_BRICK_SLAB, false), ox, BASE + 7, oz);
		}
	}

	private void placeSovereignAltarAndBoss(WorldGenLevel level, BoundingBox bb) {
		// Central 5x5 stepped Sovereign Awakening Altar with corner beacons
		fill(level, bb, ARENA_CX - 2, BASE, CZ - 2, ARENA_CX + 2, BASE, CZ + 2, DARK_PRISMARINE);
		fill(level, bb, ARENA_CX - 1, BASE + 1, CZ - 1, ARENA_CX + 1, BASE + 1, CZ + 1, GOLD);
		for (int sx : new int[] {ARENA_CX - 2, ARENA_CX + 2}) {
			for (int sz : new int[] {CZ - 2, CZ + 2}) {
				set(level, bb, OceanBlocks.SIREN_PEARL_LANTERN.defaultBlockState()
					.trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), sx, BASE + 1, sz);
			}
		}
		set(level, bb, OceanBlocks.SOVEREIGN_ALTAR.defaultBlockState()
			.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), ARENA_CX, BASE + 2, CZ);

		BlockPos altarPos = getWorldPos(ARENA_CX, BASE + 2, CZ);
		if (bb.isInside(altarPos)) {
			BlockPos bossPos = getWorldPos(ARENA_CX, BASE + 4, CZ);
			var boss = OceanEntities.THALASSOR.create(level.getLevel(), net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
			if (boss != null) {
				boss.snapTo(bossPos.getX() + 0.5, bossPos.getY(), bossPos.getZ() + 0.5, 0.0f, 0.0f);
				boss.setArenaCenter(altarPos);
				level.addFreshEntityWithPassengers(boss);
				if (level.getBlockEntity(altarPos) instanceof SovereignAltarBlockEntity altar) {
					altar.bindExistingBoss(boss);
				}
			}
		}
	}
}
