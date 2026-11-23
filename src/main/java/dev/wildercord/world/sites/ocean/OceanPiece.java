package dev.wildercord.world.sites.ocean;

import java.util.OptionalInt;
import java.util.function.Consumer;
import dev.wildercord.compat.McWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Shared geometry, seabed anchoring, MC 26.3 block palette, loot, and resident/guardian spawning
 * helpers for all 5 Deep Ocean underwater structures.
 */
public abstract class OceanPiece extends StructurePiece {
	protected static final BlockState AIR = Blocks.AIR.defaultBlockState();
	protected static final BlockState WATER = Blocks.WATER.defaultBlockState();
	protected static final BlockState CYAN_GLASS = Blocks.STAINED_GLASS.cyan().defaultBlockState();
	protected static final BlockState GLASS = Blocks.GLASS.defaultBlockState();
	protected static final BlockState YELLOW_GLASS = Blocks.STAINED_GLASS.yellow().defaultBlockState();
	protected static final BlockState PRISMARINE = Blocks.PRISMARINE.defaultBlockState();
	protected static final BlockState PRISMARINE_BRICKS = Blocks.PRISMARINE_BRICKS.defaultBlockState();
	protected static final BlockState DARK_PRISMARINE = Blocks.DARK_PRISMARINE.defaultBlockState();
	protected static final BlockState SEA_LANTERN = Blocks.SEA_LANTERN.defaultBlockState();
	protected static final BlockState COPPER_OXIDIZED = Blocks.COPPER_BLOCK.waxed().oxidized().defaultBlockState();
	protected static final BlockState CUT_COPPER_OXIDIZED = Blocks.CUT_COPPER.waxed().oxidized().defaultBlockState();
	protected static final BlockState COPPER_WEATHERED = Blocks.CUT_COPPER.waxed().weathered().defaultBlockState();
	protected static final BlockState COPPER_EXPOSED = Blocks.COPPER_BLOCK.waxed().exposed().defaultBlockState();
	protected static final BlockState COPPER_BLOCK = Blocks.COPPER_BLOCK.waxed().unaffected().defaultBlockState();
	protected static final BlockState DEEPSLATE_TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
	protected static final BlockState DEEPSLATE_BRICKS = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
	protected static final BlockState POLISHED_DEEPSLATE = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
	protected static final BlockState CHISELED_DEEPSLATE = Blocks.CHISELED_DEEPSLATE.defaultBlockState();
	protected static final BlockState COBBLED_DEEPSLATE = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
	protected static final BlockState BLACKSTONE_BRICKS = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
	protected static final BlockState BASALT = Blocks.BASALT.defaultBlockState();
	protected static final BlockState MAGMA = Blocks.MAGMA_BLOCK.defaultBlockState();
	protected static final BlockState LAVA = Blocks.LAVA.defaultBlockState();
	protected static final BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();
	protected static final BlockState SPRUCE_PLANKS = Blocks.SPRUCE_PLANKS.defaultBlockState();
	protected static final BlockState DARK_OAK_PLANKS = Blocks.DARK_OAK_PLANKS.defaultBlockState();
	protected static final BlockState STRIPPED_SPRUCE = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
	protected static final BlockState STRIPPED_OAK = Blocks.STRIPPED_OAK_LOG.defaultBlockState();
	protected static final BlockState ACACIA_PLANKS = Blocks.ACACIA_PLANKS.defaultBlockState();
	protected static final BlockState MOSS = Blocks.MOSS_BLOCK.defaultBlockState();
	protected static final BlockState GREEN_WOOL = Blocks.WOOL.green().defaultBlockState();
	protected static final BlockState SAND = Blocks.SAND.defaultBlockState();

	protected OceanPiece(StructurePieceType type, int x, int y, int z, int width, int height, int depth, Direction facing) {
		super(type, 0, StructurePiece.makeBoundingBox(x, y, z, facing, width, height, depth));
		setOrientation(facing);
	}

	protected OceanPiece(StructurePieceType type, CompoundTag tag) {
		super(type, tag);
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {}

	@Override
	protected void placeBlock(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox clip) {
		BlockPos at = getWorldPos(x, y, z);
		if (!clip.isInside(at)) return;
		if (!state.hasBlockEntity() && level.getBlockState(at).hasBlockEntity()) {
			level.getBlockEntity(at);
			level.getChunk(at).removeBlockEntity(at);
		}
		super.placeBlock(level, state, x, y, z, clip);
		if (state.hasBlockEntity()) {
			level.getBlockEntity(at);
		}
	}

	protected final void set(WorldGenLevel level, BoundingBox clip, BlockState state, int x, int y, int z) {
		placeBlock(level, state, x, y, z, clip);
	}

	protected final void fill(WorldGenLevel level, BoundingBox clip, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		generateBox(level, clip, x0, y0, z0, x1, y1, z1, state, state, false);
	}

	protected final void footingRect(WorldGenLevel level, BoundingBox clip, int x0, int z0, int x1, int z1, int topY, BlockState cap, BlockState pillar) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				set(level, clip, cap, x, topY, z);
				fillColumnDown(level, pillar, x, topY - 1, z, clip);
			}
		}
	}

	protected final BlockPos localToWorld(int x, int y, int z) {
		return getWorldPos(x, y, z);
	}

	protected static BlockState stair(Block block, Direction facing, boolean top) {
		return block.defaultBlockState()
			.setValue(StairBlock.FACING, facing)
			.setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
	}

	protected static BlockState slab(Block block, boolean top) {
		return block.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
	}

	protected final void cyanBed(WorldGenLevel level, BoundingBox clip, int fx, int y, int fz, Direction headDir) {
		int hx = fx + headDir.getStepX();
		int hz = fz + headDir.getStepZ();
		Block bed = Blocks.BED.cyan();
		set(level, clip, bed.defaultBlockState().setValue(BedBlock.FACING, headDir).setValue(BedBlock.PART, BedPart.FOOT), fx, y, fz);
		set(level, clip, bed.defaultBlockState().setValue(BedBlock.FACING, headDir).setValue(BedBlock.PART, BedPart.HEAD), hx, y, hz);
	}

	protected final void chest(WorldGenLevel level, BoundingBox clip, int x, int y, int z, Direction facing, ResourceKey<LootTable> loot, long salt) {
		BlockPos p = getWorldPos(x, y, z);
		if (!clip.isInside(p) || level.getBlockEntity(p) instanceof ChestBlockEntity) return;
		set(level, clip, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), x, y, z);
		if (level.getBlockEntity(p) instanceof ChestBlockEntity entity) {
			entity.setLootTable(loot, p.asLong() ^ salt);
		}
	}

	protected final void barrel(WorldGenLevel level, BoundingBox clip, int x, int y, int z, Direction facing, ResourceKey<LootTable> loot, long salt) {
		BlockPos p = getWorldPos(x, y, z);
		if (!clip.isInside(p) || level.getBlockEntity(p) instanceof BarrelBlockEntity) return;
		set(level, clip, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, facing), x, y, z);
		if (level.getBlockEntity(p) instanceof BarrelBlockEntity entity) {
			entity.setLootTable(loot, p.asLong() ^ salt);
		}
	}

	protected final <T extends Mob> void spawnMob(WorldGenLevel level, BoundingBox clip, EntityType<T> type, int x, int y, int z, Consumer<T> configure) {
		BlockPos p = getWorldPos(x, y, z);
		if (!clip.isInside(p)) return;
		T mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (mob == null) return;
		mob.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.0f, 0.0f);
		mob.setPersistenceRequired();
		configure.accept(mob);
		level.addFreshEntityWithPassengers(mob);
	}

	protected static int oceanFloorY(Structure.GenerationContext ctx, int worldX, int worldZ) {
		return ctx.chunkGenerator().getFirstOccupiedHeight(
			worldX, worldZ,
			Heightmap.Types.OCEAN_FLOOR_WG,
			ctx.heightAccessor(),
			ctx.randomState()
		);
	}

	/**
	 * Verifies that every sampled corner and center across a structure's placed XZ footprint lies
	 * in an ocean biome, has open water at least {@code minDepthBelowSea} deep above the floor,
	 * and stays within {@code maxFloorRelief} vertical blocks so structures never clip into islands,
	 * coastal cliffs, or emerge into air. Returns the minimum floor Y across the footprint.
	 */
	protected static OptionalInt locateSubmergedFloor(
			Structure.GenerationContext ctx, BoundingBox rawFootprint, int minDepthBelowSea, int maxFloorRelief) {
		int sea = ctx.chunkGenerator().getSeaLevel();
		int minX = rawFootprint.minX();
		int midX = (rawFootprint.minX() + rawFootprint.maxX()) / 2;
		int maxX = rawFootprint.maxX();
		int minZ = rawFootprint.minZ();
		int midZ = (rawFootprint.minZ() + rawFootprint.maxZ()) / 2;
		int maxZ = rawFootprint.maxZ();

		int[] xs = {minX, midX, maxX};
		int[] zs = {minZ, midZ, maxZ};
		int floorMin = Integer.MAX_VALUE;
		int floorMax = Integer.MIN_VALUE;

		for (int px : xs) {
			for (int pz : zs) {
				int surf = ctx.chunkGenerator().getFirstOccupiedHeight(
					px, pz, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
				int floor = oceanFloorY(ctx, px, pz);
				Holder<Biome> biome = McWorldgen.getNoiseBiome(ctx, px, Math.max(ctx.heightAccessor().getMinY() + 1, Math.min(sea - 2, floor)), pz);
				if (!biome.is(BiomeTags.IS_OCEAN)) {
					return OptionalInt.empty();
				}
				// Overworld seaLevel is 63 while the topmost water block sits at 62 (sea - 1).
				if (surf < sea - 1 || floor > surf - 4 || sea - floor < minDepthBelowSea) {
					return OptionalInt.empty();
				}
				floorMin = Math.min(floorMin, floor);
				floorMax = Math.max(floorMax, floor);
			}
		}
		if (floorMax - floorMin > maxFloorRelief) {
			return OptionalInt.empty();
		}
		return OptionalInt.of(floorMin);
	}

	protected static double dist2(double x1, double z1, double x2, double z2) {
		return Math.hypot(x1 - x2, z1 - z2);
	}
}
