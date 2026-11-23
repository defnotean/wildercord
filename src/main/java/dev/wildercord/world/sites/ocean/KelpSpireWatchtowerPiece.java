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
		BoundingBox probe = makeBoundingBox(x, 0, z, Direction.NORTH, WIDTH, 1, DEPTH);
		var floorOpt = locateSubmergedFloor(ctx, probe, 16, 12);
		if (floorOpt.isEmpty()) return Optional.empty();
		int floor = floorOpt.getAsInt();
		int sea = ctx.chunkGenerator().getSeaLevel();
		int depthBelowSea = sea - floor;
		if (depthBelowSea > 50) return Optional.empty();
		int h = Math.clamp(depthBelowSea + BASE + 10, OceanRules.WATCHTOWER_MIN_HEIGHT, 64);
		KelpSpireWatchtowerPiece piece = new KelpSpireWatchtowerPiece(x, floor - BASE, z, Direction.NORTH, h);
		BlockPos center = probe.getCenter();
		return Optional.of(new Structure.GenerationStub(new BlockPos(center.getX(), floor, center.getZ()), b -> b.addPiece(piece)));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunkPos, BlockPos reference) {
		int cx = 7, cz = 7;
		int bellTop = BASE + 12;
		int dockY = Math.max(bellTop + 4, totalHeight - 9);

		// 1. Seabed 15x14x15 Chamfered Brass-Ribbed Diving-Bell Chamber
		footingRect(level, bb, 2, 2, 12, 12, BASE, DARK_PRISMARINE, PRISMARINE_BRICKS);
		fill(level, bb, 3, BASE, 3, 11, BASE, 11, SPRUCE_PLANKS);

		for (int y = BASE + 1; y <= bellTop; y++) {
			boolean collar = y == BASE + 5 || y == bellTop;
			int inset = (y >= bellTop - 1) ? 1 : 0;
			for (int x = 2 + inset; x <= 12 - inset; x++) {
				for (int z = 2 + inset; z <= 12 - inset; z++) {
					boolean edge = x == 2 + inset || x == 12 - inset || z == 2 + inset || z == 12 - inset;
					if (!edge) {
						set(level, bb, AIR, x, y, z);
					} else if ((x == 2 + inset || x == 12 - inset) && (z == 2 + inset || z == 12 - inset)) {
						set(level, bb, y % 3 == 0 ? COPPER_BLOCK : DARK_PRISMARINE, x, y, z);
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
		// Exterior brass rib buttresses, diagonal stair braces & kelp spire overgrowth around diving bell corners
		set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.SOUTH, false).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), cx, BASE + 1, 1);
		set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.NORTH, false).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), cx, BASE + 1, 13);
		set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.EAST, false).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 1, BASE + 1, cz);
		set(level, bb, stair(Blocks.PRISMARINE_BRICK_STAIRS, Direction.WEST, false).trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), 13, BASE + 1, cz);
		for (int bx : new int[] {1, 13}) {
			for (int bz : new int[] {1, 13}) {
				set(level, bb, COPPER_OXIDIZED, bx, BASE, bz);
				set(level, bb, dev.wildercord.ocean.OceanBlocks.SIREN_PEARL_LANTERN.defaultBlockState()
					.trySetValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true), bx, BASE + 1, bz);
				for (int y = BASE + 2; y < dockY - 1; y++) {
					set(level, bb, Blocks.KELP_PLANT.defaultBlockState(), bx, y, bz);
				}
				set(level, bb, Blocks.KELP.defaultBlockState(), bx, dockY - 1, bz);
			}
		}
		// Cap bell ceiling & richly furnish dry bell
		fill(level, bb, 3, bellTop, 3, 11, bellTop, 11, DARK_PRISMARINE);
		fill(level, bb, 4, bellTop + 1, 4, 10, bellTop + 1, 10, COPPER_OXIDIZED);
		chest(level, bb, 4, BASE + 1, 4, Direction.SOUTH, OceanSites.WATCHTOWER_LOOT, 21L);
		chest(level, bb, 10, BASE + 1, 4, Direction.SOUTH, OceanSites.WATCHTOWER_LOOT, 22L);
		barrel(level, bb, 4, BASE + 1, 6, Direction.EAST, OceanSites.WATCHTOWER_LOOT, 23L);
		set(level, bb, Blocks.CRAFTING_TABLE.defaultBlockState(), 4, BASE + 1, 10);
		set(level, bb, Blocks.LECTERN.defaultBlockState(), 10, BASE + 1, 6);
		set(level, bb, Blocks.BLAST_FURNACE.defaultBlockState(), 10, BASE + 1, 10);
		set(level, bb, dev.wildercord.ocean.OceanBlocks.SIREN_PEARL_LANTERN.defaultBlockState(), 7, BASE + 1, 10);
		set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 4, bellTop - 1, 4);

		// 2. Twin Bubble-Elevator Shafts (Soul Sand UP at x=6, Magma DOWN at x=8) + Brass Pressure Rings
		for (int y = BASE + 1; y <= dockY; y++) {
			boolean ring = (y - BASE) % 5 == 0;
			for (int x = 5; x <= 9; x++) {
				for (int z = 5; z <= 9; z++) {
					boolean shell = x == 5 || x == 9 || z == 5 || z == 9;
					if (shell) {
						boolean pillar = (x == 5 || x == 9) && (z == 5 || z == 9);
						set(level, bb, ring ? (pillar ? COPPER_BLOCK : SEA_LANTERN) : (pillar ? DARK_PRISMARINE : CYAN_GLASS), x, y, z);
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

		// 3. Surface Spruce Boat Dock at Sea Level with Pilings, Mooring Bollards & Crates
		fill(level, bb, 0, dockY, 0, 14, dockY, 4, SPRUCE_PLANKS);
		fill(level, bb, 3, dockY, 5, 11, dockY, 11, SPRUCE_PLANKS);
		barrel(level, bb, 2, dockY + 1, 2, Direction.UP, OceanSites.WATCHTOWER_LOOT, 24L);
		chest(level, bb, 12, dockY + 1, 2, Direction.SOUTH, OceanSites.WATCHTOWER_LOOT, 25L);
		for (int x = 1; x <= 13; x++) {
			if (x < 5 || x > 9) {
				set(level, bb, Blocks.SPRUCE_FENCE.defaultBlockState(), x, dockY + 1, 4);
			}
		}
		for (int x : new int[] {0, 4, 10, 14}) {
			for (int y = dockY - 3; y <= dockY + 1; y++) {
				set(level, bb, STRIPPED_SPRUCE, x, y, 0);
			}
			set(level, bb, dev.wildercord.ocean.OceanBlocks.SIREN_PEARL_LANTERN.defaultBlockState(), x, dockY + 2, 0);
			set(level, bb, Blocks.SPRUCE_FENCE.defaultBlockState(), x, dockY + 1, 4);
		}

		// 4. Tiered Beacon Crown (sea_lantern core + copper ribs + oxidized cupola & spire)
		int crownBase = dockY + 1;
		for (int y = crownBase; y <= Math.min(totalHeight - 3, crownBase + 5); y++) {
			for (int x = 5; x <= 9; x++) {
				for (int z = 5; z <= 9; z++) {
					boolean edge = x == 5 || x == 9 || z == 5 || z == 9;
					if (!edge) {
						set(level, bb, SEA_LANTERN, x, y, z);
					} else if ((x == 5 || x == 9) && (z == 5 || z == 9)) {
						set(level, bb, y == crownBase + 2 ? COPPER_BLOCK : COPPER_OXIDIZED, x, y, z);
					} else if (y == crownBase || y == crownBase + 4) {
						set(level, bb, DARK_PRISMARINE, x, y, z);
					} else {
						set(level, bb, CYAN_GLASS, x, y, z);
					}
				}
			}
		}
		int tipY = Math.min(totalHeight - 1, crownBase + 6);
		fill(level, bb, 5, tipY - 2, 5, 9, tipY - 2, 9, DARK_PRISMARINE);
		fill(level, bb, 6, tipY - 1, 6, 8, tipY - 1, 8, COPPER_OXIDIZED);
		set(level, bb, GOLD, 7, tipY - 1, 7);
		set(level, bb, Blocks.LIGHTNING_ROD.defaultBlockState(), 7, tipY, 7);
		set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 7, dockY + 3, 2);
	}
}
