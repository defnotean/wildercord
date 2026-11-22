package dev.wildercord.world.sites.ocean;

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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Kelp-Spire Watchtower (15 x H x 15, scaling dynamically to surface sea level):
 * Dry seabed diving-bell chamber connected via twin glass bubble-elevator shafts
 * (soul_sand UP and magma_block DOWN) to a surface spruce boat dock and beacon crown.
 */
public final class KelpSpireWatchtowerPiece extends OceanPiece {
	public static final int WIDTH = OceanRules.WATCHTOWER_SIZE_XZ;
	public static final int DEPTH = OceanRules.WATCHTOWER_SIZE_XZ;
	public static final int BASE = 3;
	private final int totalHeight;

	public KelpSpireWatchtowerPiece(int x, int y, int z, Direction facing, int totalHeight) {
		super(OceanSites.KELP_SPIRE_WATCHTOWER, x, y, z, WIDTH, Math.clamp(totalHeight, OceanRules.WATCHTOWER_MIN_HEIGHT, 64), DEPTH, facing);
		this.totalHeight = Math.clamp(totalHeight, OceanRules.WATCHTOWER_MIN_HEIGHT, 64);
	}

	public KelpSpireWatchtowerPiece(CompoundTag tag) {
		super(OceanSites.KELP_SPIRE_WATCHTOWER, tag);
		this.totalHeight = Math.clamp(tag.getIntOr("H", 48), OceanRules.WATCHTOWER_MIN_HEIGHT, 64);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
		super.addAdditionalSaveData(ctx, tag);
		tag.putInt("H", totalHeight);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext ctx) {
		ChunkPos cp = ctx.chunkPos();
		int x = cp.getMinBlockX();
		int z = cp.getMinBlockZ();
		int cx = x + 7;
		int cz = z + 7;
		int floor = oceanFloorY(ctx, cx, cz);
		int sea = ctx.chunkGenerator().getSeaLevel();
		int depthBelowSea = sea - floor;
		if (depthBelowSea < 16 || depthBelowSea > 50) return Optional.empty();
		int h = Math.clamp(depthBelowSea + BASE + 10, OceanRules.WATCHTOWER_MIN_HEIGHT, 64);
		KelpSpireWatchtowerPiece piece = new KelpSpireWatchtowerPiece(x, floor - BASE, z, Direction.NORTH, h);
		return Optional.of(new Structure.GenerationStub(new BlockPos(cx, floor, cz), b -> b.addPiece(piece)));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunkPos, BlockPos reference) {
		int cx = 7, cz = 7;
		int bellTop = BASE + 12;
		int dockY = Math.max(bellTop + 4, totalHeight - 9);

		// 1. Seabed 15x14x15 Dry Diving-Bell Chamber
		footingRect(level, bb, 2, 2, 12, 12, BASE, DARK_PRISMARINE, PRISMARINE_BRICKS);
		fill(level, bb, 3, BASE, 3, 11, BASE, 11, SPRUCE_PLANKS);

		for (int y = BASE + 1; y <= bellTop; y++) {
			boolean collar = y == BASE + 5 || y == bellTop;
			for (int x = 2; x <= 12; x++) {
				for (int z = 2; z <= 12; z++) {
					boolean edge = x == 2 || x == 12 || z == 2 || z == 12;
					if (!edge) {
						set(level, bb, AIR, x, y, z);
					} else if ((x == 2 || x == 12) && (z == 2 || z == 12)) {
						set(level, bb, DARK_PRISMARINE, x, y, z);
					} else if (collar) {
						set(level, bb, COPPER_BLOCK, x, y, z);
					} else if (y >= BASE + 2 && y <= BASE + 4 && Math.abs(x - cx) <= 2 && (z == 2 || z == 12)) {
						set(level, bb, CYAN_GLASS, x, y, z);
					} else if (y >= BASE + 2 && y <= BASE + 4 && Math.abs(z - cz) <= 2 && (x == 2 || x == 12)) {
						set(level, bb, CYAN_GLASS, x, y, z);
					} else {
						set(level, bb, PRISMARINE_BRICKS, x, y, z);
					}
				}
			}
		}
		// Cap bell ceiling & furnish dry bell
		fill(level, bb, 2, bellTop, 2, 12, bellTop, 12, DARK_PRISMARINE);
		chest(level, bb, 4, BASE + 1, 4, Direction.SOUTH, OceanSites.WATCHTOWER_LOOT, 21L);
		chest(level, bb, 10, BASE + 1, 4, Direction.SOUTH, OceanSites.WATCHTOWER_LOOT, 22L);
		set(level, bb, Blocks.CRAFTING_TABLE.defaultBlockState(), 4, BASE + 1, 10);
		set(level, bb, Blocks.LANTERN.defaultBlockState(), 10, BASE + 1, 10);

		// 2. Twin Bubble-Elevator Shafts (Soul Sand UP at x=6, Magma DOWN at x=8)
		for (int y = BASE + 1; y <= dockY; y++) {
			for (int x = 5; x <= 9; x++) {
				for (int z = 5; z <= 9; z++) {
					boolean shell = x == 5 || x == 9 || z == 5 || z == 9;
					if (shell) {
						boolean pillar = (x == 5 || x == 9) && (z == 5 || z == 9);
						set(level, bb, pillar ? DARK_PRISMARINE : CYAN_GLASS, x, y, z);
					} else if ((x == 6 || x == 8) && z == 7) {
						set(level, bb, WATER, x, y, z);
					} else if (x == 7 && z == 7) {
						set(level, bb, PRISMARINE_BRICKS, x, y, z);
					} else {
						set(level, bb, AIR, x, y, z);
					}
				}
			}
			// Walkways into bell at bottom & dock at top
			if (y == BASE + 1 || y == BASE + 2 || y == dockY + 1 || y == dockY + 2) {
				set(level, bb, AIR, 6, y, 5);
				set(level, bb, AIR, 8, y, 5);
			}
		}
		set(level, bb, Blocks.SOUL_SAND.defaultBlockState(), 6, BASE, 7);
		set(level, bb, MAGMA, 8, BASE, 7);

		// 3. Surface Spruce Boat Dock at Sea Level
		fill(level, bb, 0, dockY, 0, 14, dockY, 4, SPRUCE_PLANKS);
		fill(level, bb, 3, dockY, 5, 11, dockY, 11, SPRUCE_PLANKS);
		for (int x : new int[] {0, 4, 10, 14}) {
			set(level, bb, STRIPPED_SPRUCE, x, dockY + 1, 0);
			set(level, bb, Blocks.LANTERN.defaultBlockState(), x, dockY + 2, 0);
			if (x == 0 || x == 14) {
				set(level, bb, Blocks.SPRUCE_FENCE.defaultBlockState(), x, dockY + 1, 4);
			}
		}

		// 4. Beacon Crown (sea_lantern core + oxidized copper spire)
		int crownBase = dockY + 1;
		for (int y = crownBase; y <= Math.min(totalHeight - 3, crownBase + 5); y++) {
			for (int x = 5; x <= 9; x++) {
				for (int z = 5; z <= 9; z++) {
					boolean edge = x == 5 || x == 9 || z == 5 || z == 9;
					if (!edge) {
						set(level, bb, SEA_LANTERN, x, y, z);
					} else if ((x == 5 || x == 9) && (z == 5 || z == 9)) {
						set(level, bb, COPPER_OXIDIZED, x, y, z);
					} else if (y == crownBase || y == crownBase + 4) {
						set(level, bb, DARK_PRISMARINE, x, y, z);
					} else {
						set(level, bb, CYAN_GLASS, x, y, z);
					}
				}
			}
		}
		int tipY = Math.min(totalHeight - 1, crownBase + 6);
		fill(level, bb, 6, tipY - 1, 6, 8, tipY - 1, 8, COPPER_OXIDIZED);
		set(level, bb, Blocks.LIGHTNING_ROD.waxed().oxidized().defaultBlockState(), 7, tipY, 7);
		set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 7, dockY + 3, 2);
	}
}
