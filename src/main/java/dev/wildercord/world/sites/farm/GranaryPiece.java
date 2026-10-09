package dev.wildercord.world.sites.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A tall gabled barn full of hay and stores, two cows in their stall, and in the yard a harvest shrine: a cake on a
 * bale, a bell, carved pumpkins and candles, and an offering chest for whoever comes to share the feast.
 */
public final class GranaryPiece extends FarmPiece {
	public GranaryPiece(int x, int z, Direction facing) { super(FarmSites.GRANARY, x, 0, z, 21, 16, 25, facing); }
	public GranaryPiece(CompoundTag tag) { super(FarmSites.GRANARY, tag); }

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		site(level, bb, s(Blocks.GRASS_BLOCK), 1);
		fill(level, bb, 9, 0, 0, 11, 0, 5, s(Blocks.DIRT_PATH));
		// The barn: spruce walls on a dark frame, a dark oak gable roof with its ridge down the middle.
		var planks = s(Blocks.SPRUCE_PLANKS);
		var frame = s(Blocks.STRIPPED_DARK_OAK_LOG);
		fill(level, bb, 3, 0, 6, 17, 0, 22, planks);
		walls(level, bb, 3, 1, 6, 17, 6, 22, planks);
		for (int x : new int[]{3, 17}) for (int z : new int[]{6, 14, 22}) fill(level, bb, x, 1, z, x, 6, z, frame);
		fill(level, bb, 3, 6, 6, 17, 6, 6, frame);
		fill(level, bb, 3, 6, 22, 17, 6, 22, frame);
		for (int i = 0; i < 8; i++) {
			int y = 7 + i;
			fill(level, bb, 2 + i, y, 5, 2 + i, y, 23, stairs(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
			fill(level, bb, 18 - i, y, 5, 18 - i, y, 23, stairs(Blocks.DARK_OAK_STAIRS, Direction.WEST, false));
			fill(level, bb, 3 + i, y, 6, 17 - i, y, 6, planks);
			fill(level, bb, 3 + i, y, 22, 17 - i, y, 22, planks);
		}
		fill(level, bb, 10, 14, 5, 10, 14, 23, s(Blocks.DARK_OAK_PLANKS));
		fill(level, bb, 10, 15, 5, 10, 15, 23, s(Blocks.DARK_OAK_SLAB));
		fill(level, bb, 9, 9, 6, 11, 11, 6, s(Blocks.HAY_BLOCK));
		for (int z : new int[]{10, 18}) for (int x : new int[]{3, 17}) fill(level, bb, x, 3, z, x, 4, z, s(Blocks.GLASS));
		// The big door, wide open.
		fill(level, bb, 8, 1, 6, 12, 4, 6, AIR);
		fill(level, bb, 8, 5, 6, 12, 5, 6, frame);
		// Stores: hay, barrels, composters, the barn chest.
		for (int x = 4; x <= 7; x++) for (int z = 18; z <= 21; z++) {
			int top = 1 + noise(x, z) % 3;
			fill(level, bb, x, 1, z, x, top, z, s(Blocks.HAY_BLOCK));
		}
		var barrel = s(Blocks.BARREL).setValue(BlockStateProperties.FACING, Direction.UP);
		fill(level, bb, 16, 1, 7, 16, 2, 9, barrel);
		set(level, bb, s(Blocks.COMPOSTER), 4, 1, 7);
		set(level, bb, s(Blocks.COMPOSTER), 5, 1, 7);
		chest(level, bb, 16, 1, 13, FarmSites.GRANARY_BARN, Direction.WEST, 1);
		set(level, bb, s(Blocks.CRAFTING_TABLE), 16, 1, 12);
		set(level, bb, s(Blocks.LANTERN), 16, 3, 9);
		// The cow stall.
		fill(level, bb, 12, 1, 17, 12, 1, 21, s(Blocks.OAK_FENCE));
		fill(level, bb, 12, 1, 17, 16, 1, 17, s(Blocks.OAK_FENCE));
		set(level, bb, facing(Blocks.OAK_FENCE_GATE, Direction.WEST), 12, 1, 19);
		set(level, bb, s(Blocks.HAY_BLOCK), 16, 1, 21);
		set(level, bb, s(Blocks.WATER_CAULDRON).setValue(BlockStateProperties.LEVEL_CAULDRON, 3), 16, 1, 18);
		sentry(level, bb, EntityTypes.COW, 14, 1, 19);
		sentry(level, bb, EntityTypes.COW, 14, 1, 20);
		// The harvest shrine in the yard.
		set(level, bb, s(Blocks.STONE_BRICKS), 14, 1, 3);
		set(level, bb, facing(Blocks.BELL, Direction.SOUTH).setValue(BlockStateProperties.BELL_ATTACHMENT, BellAttachType.FLOOR), 14, 2, 3);
		set(level, bb, s(Blocks.HAY_BLOCK), 16, 1, 3);
		set(level, bb, s(Blocks.CAKE), 16, 2, 3);
		set(level, bb, s(Blocks.HAY_BLOCK), 18, 1, 3);
		set(level, bb, s(Blocks.CANDLE).setValue(BlockStateProperties.CANDLES, 3).setValue(BlockStateProperties.LIT, true), 18, 2, 3);
		set(level, bb, facing(Blocks.CARVED_PUMPKIN, Direction.SOUTH), 15, 1, 2);
		set(level, bb, facing(Blocks.JACK_O_LANTERN, Direction.SOUTH), 17, 1, 2);
		set(level, bb, facing(Blocks.CARVED_PUMPKIN, Direction.SOUTH), 19, 1, 4);
		chest(level, bb, 16, 1, 1, FarmSites.GRANARY_SHRINE, Direction.SOUTH, 2);
		sign(level, bb, standingSign(), 18, 1, 1, "farm_granary", 3);
		for (int x = 1; x <= 6; x++) for (int z = 1; z <= 4; z++) {
			set(level, bb, farmland(false), x, 0, z);
			set(level, bb, crop(noise(x, z) < 50 ? Blocks.WHEAT : Blocks.CARROTS, 7), x, 1, z);
		}
	}
}
