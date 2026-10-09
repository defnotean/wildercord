package dev.wildercord.world.sites.mine;

import dev.wildercord.world.dungeons.DungeonPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;

/** What the mining sites share: posts down to firm ground, barrels, lamps and the checks their locators make. */
abstract class MinePiece extends DungeonPiece {
	MinePiece(StructurePieceType type, int x, int y, int z, int width, int height, int depth, Direction facing) {
		super(type, x, y, z, width, height, depth, facing);
	}

	MinePiece(StructurePieceType type, CompoundTag tag) {
		super(type, tag);
	}

	static BlockState s(Block block) {
		return block.defaultBlockState();
	}

	/** A peaceful site guards nothing. */
	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of();
	}

	/** Fills each column under {@code y} with {@code state} down to firm ground (at most 24 blocks), so a floor on a slope stands. */
	protected void footing(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1, int y, BlockState state) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int d = y - 1; d > y - 25; d--) {
					BlockPos at = getWorldPos(x, d, z);
					if (!bb.isInside(at) || at.getY() <= level.getMinY()) break;
					BlockState here = level.getBlockState(at);
					if (!(here.isAir() || here.canBeReplaced() || !here.getFluidState().isEmpty() || here.is(BlockTags.LEAVES))) break;
					level.setBlock(at, state, 2);
				}
			}
		}
	}

	/** A barrel opening upward (so it needs no turning), filled from {@code loot} when given. */
	protected void barrel(WorldGenLevel level, BoundingBox bb, int x, int y, int z, ResourceKey<LootTable> loot, long salt) {
		BlockPos at = getWorldPos(x, y, z);
		if (!bb.isInside(at)) return;
		level.setBlock(at, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 2);
		if (loot != null) RandomizableContainer.setBlockEntityLootTable(level, RandomSource.create(boundingBox.minX() * 31L + boundingBox.minZ() * 17L + salt), at, loot);
	}

	protected void hangingLantern(WorldGenLevel level, BoundingBox bb, int x, int y, int z) {
		set(level, bb, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), x, y, z);
	}

	/** A few ore blocks in a wall, by the spot's noise: coal and copper often, iron now and then. */
	protected static BlockState rock(int x, int y, int z, BlockState stone, boolean deep) {
		int n = noise(x, y, z);
		if (n < 4) return s(deep ? Blocks.DEEPSLATE_COAL_ORE : Blocks.COAL_ORE);
		if (n < 7) return s(deep ? Blocks.DEEPSLATE_COPPER_ORE : Blocks.COPPER_ORE);
		if (n < 9) return s(deep ? Blocks.DEEPSLATE_IRON_ORE : Blocks.IRON_ORE);
		return stone;
	}

	// ------------------------------------------------------------------ locating

	/** The top solid block of a column, as the world will first generate it. */
	static int ground(Structure.GenerationContext c, BlockPos at) {
		return c.chunkGenerator().getFirstOccupiedHeight(at.getX(), at.getZ(), Heightmap.Types.WORLD_SURFACE_WG, c.heightAccessor(), c.randomState());
	}

	/** Whether water stands over a column (rivers, lakes, the sea). */
	static boolean wet(Structure.GenerationContext c, BlockPos at) {
		return c.chunkGenerator().getFirstOccupiedHeight(at.getX(), at.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, c.heightAccessor(), c.randomState()) < ground(c, at);
	}

	/** Whether the deep dark (and so, perhaps, an ancient city) is at this spot: underground sites stay out of it. */
	static boolean deepDark(Structure.GenerationContext c, int x, int y, int z) {
		return c.biomeResolver().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)).is(Biomes.DEEP_DARK);
	}

	/** Whether the ground's own water or lava (an aquifer) fills any of a column from {@code y0} to {@code y1}. */
	static boolean fluidIn(Structure.GenerationContext c, BlockPos at, int y0, int y1) {
		var column = c.chunkGenerator().getBaseColumn(at.getX(), at.getZ(), c.heightAccessor(), c.randomState());
		for (int y = y0; y <= y1; y++) if (!column.getBlock(y).getFluidState().isEmpty()) return true;
		return false;
	}
}
