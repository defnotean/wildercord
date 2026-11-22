package dev.wildercord.world.sites.ocean;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Hydrothermal Vent Forge (27x20x27):
 * Polished blackstone & deepslate foundry tower surrounded by 4 jagged basalt chimneys and glowing
 * magma/lava seabed fissure trenches, piped with exposed copper into a dry glass dome housing Trench Anvil.
 */
public final class HydrothermalForgePiece extends OceanPiece {
	public static final int WIDTH = OceanRules.FORGE_SIZE_XZ;
	public static final int HEIGHT = OceanRules.FORGE_HEIGHT;
	public static final int DEPTH = OceanRules.FORGE_SIZE_XZ;
	public static final int BASE = 3;
	private static final int CX = 13;
	private static final int CZ = 13;

	public HydrothermalForgePiece(int x, int y, int z, Direction facing) {
		super(OceanSites.HYDROTHERMAL_FORGE, x, y, z, WIDTH, HEIGHT, DEPTH, facing);
	}

	public HydrothermalForgePiece(CompoundTag tag) {
		super(OceanSites.HYDROTHERMAL_FORGE, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext ctx) {
		ChunkPos cp = ctx.chunkPos();
		int x = cp.getMinBlockX();
		int z = cp.getMinBlockZ();
		int floor = oceanFloorY(ctx, x + CX, z + CZ);
		int sea = ctx.chunkGenerator().getSeaLevel();
		if (sea - floor < 16) return Optional.empty();
		HydrothermalForgePiece piece = new HydrothermalForgePiece(x, floor - BASE, z, Direction.NORTH);
		return Optional.of(new Structure.GenerationStub(new BlockPos(x + CX, floor, z + CZ), b -> b.addPiece(piece)));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunkPos, BlockPos reference) {
		// 1. Seabed Fissure Base & Central Foundry Core
		footingRect(level, bb, 6, 6, 20, 20, BASE, BLACKSTONE_BRICKS, DEEPSLATE_BRICKS);
		for (int i = -11; i <= 11; i++) {
			// Magma/lava cross fissures
			int px = CX + i;
			int pz = CZ;
			set(level, bb, (Math.abs(i) > 7 && Math.abs(i) % 2 == 1) ? LAVA : MAGMA, px, BASE - 1, pz);
			set(level, bb, (Math.abs(i) > 7 && Math.abs(i) % 2 == 1) ? LAVA : MAGMA, CX, BASE - 1, CZ + i);
		}

		// Central Industrial Tower (x=8..18, z=8..18, y=BASE..BASE+7)
		for (int y = BASE + 1; y <= BASE + 7; y++) {
			for (int x = 8; x <= 18; x++) {
				for (int z = 8; z <= 18; z++) {
					boolean edge = x == 8 || x == 18 || z == 8 || z == 18;
					if (!edge) {
						set(level, bb, y == BASE + 1 ? AIR : (y == BASE + 7 ? BLACKSTONE_BRICKS : AIR), x, y, z);
					} else {
						BlockState s = (y == BASE + 4) ? COPPER_EXPOSED : ((x + z) % 2 == 0 ? DEEPSLATE_BRICKS : BLACKSTONE_BRICKS);
						set(level, bb, s, x, y, z);
					}
				}
			}
		}
		// Lower smelting alcove (south opening + dry blast furnaces)
		fill(level, bb, CX - 1, BASE + 1, 18, CX + 1, BASE + 3, 18, AIR);
		fill(level, bb, 10, BASE + 1, 10, 16, BASE + 5, 16, AIR);
		set(level, bb, Blocks.BLAST_FURNACE.defaultBlockState(), 11, BASE + 1, 10);
		set(level, bb, Blocks.BLAST_FURNACE.defaultBlockState(), 15, BASE + 1, 10);
		set(level, bb, Blocks.CAULDRON.defaultBlockState(), 13, BASE + 1, 10);
		set(level, bb, ACACIA_PLANKS, 10, BASE + 1, 12);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 10, BASE + 2, 12);
		chest(level, bb, 16, BASE + 1, 12, Direction.WEST, OceanSites.FORGE_LOOT, 31L);

		// 2. 4 Jagged Basalt Chimney Stacks + External Exposed Copper Heat Pipes
		int[][] stacks = {{4, 4, 15}, {22, 4, 17}, {4, 22, 16}, {22, 22, 14}};
		for (int[] st : stacks) {
			int sx = st[0], sz = st[1], top = st[2];
			footingRect(level, bb, sx - 1, sz - 1, sx + 1, sz + 1, BASE, DEEPSLATE_BRICKS, BASALT);
			for (int y = BASE + 1; y <= top; y++) {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dz == 0) {
							set(level, bb, y == top ? MAGMA : BLACKSTONE_BRICKS, sx, y, sz);
						} else if (y <= top - (Math.abs(dx) + Math.abs(dz))) {
							set(level, bb, BASALT, sx + dx, y, sz + dz);
						}
					}
				}
			}
			// Exposed copper duct linking stack into central forge body
			int stepX = Integer.compare(CX, sx);
			int stepZ = Integer.compare(CZ, sz);
			for (int k = 1; k <= 4; k++) {
				set(level, bb, COPPER_EXPOSED, sx + stepX * k, BASE + 6, sz + stepZ * k);
			}
		}

		// 3. Top Dry 11x7x11 Glass Dome housing Trench Anvil (y=10..17)
		int domeY = BASE + 7; // y = 10
		fill(level, bb, 8, domeY, 8, 18, domeY, 18, BLACKSTONE_BRICKS);
		for (int x = 8; x <= 18; x++) {
			for (int z = 8; z <= 18; z++) {
				for (int y = domeY + 1; y <= domeY + 6; y++) {
					double r = Math.sqrt(Math.pow(x - CX, 2) + Math.pow((y - domeY) * 0.9, 2) + Math.pow(z - CZ, 2));
					if (r >= 4.4 && r <= 5.6) {
						boolean rib = (x == CX || z == CZ);
						set(level, bb, rib ? COPPER_BLOCK : GLASS, x, y, z);
					}
				}
			}
		}
		// Ensure dry dome interior & Trench Anvil centerpiece
		for (int x = 9; x <= 17; x++) {
			for (int z = 9; z <= 17; z++) {
				for (int y = domeY + 1; y <= domeY + 5; y++) {
					double r = Math.sqrt(Math.pow(x - CX, 2) + Math.pow((y - domeY) * 0.9, 2) + Math.pow(z - CZ, 2));
					if (r < 4.35) {
						set(level, bb, AIR, x, y, z);
					}
				}
			}
		}
		// Doorway stair inside central shaft up to forge dome
		fill(level, bb, CX - 1, BASE + 1, CZ - 1, CX + 1, domeY, CZ + 1, AIR);
		for (int y = BASE + 1; y <= domeY; y++) {
			set(level, bb, Blocks.LADDER.defaultBlockState(), CX - 2, y, CZ);
		}
		set(level, bb, OceanBlocks.TRENCH_ANVIL.defaultBlockState(), CX, domeY + 1, CZ + 2);
		set(level, bb, OceanBlocks.SIREN_PEARL_LANTERN.defaultBlockState(), CX - 2, domeY + 1, CZ + 2);
		set(level, bb, Blocks.BLAST_FURNACE.defaultBlockState(), CX + 2, domeY + 1, CZ + 2);

		spawnMob(level, bb, OceanEntities.BARNACLE_HULK_DREDGER, CX, BASE + 1, 22, m -> {});
	}
}
