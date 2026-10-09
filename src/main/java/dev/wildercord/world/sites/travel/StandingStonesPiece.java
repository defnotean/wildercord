package dev.wildercord.world.sites.travel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Nine standing stones around a reading dais. The book on the dais tells how the first roads were laid between
 * stones like these, and where the road-makers left their gift: under the kneeling stone before the tallest one.
 */
public final class StandingStonesPiece extends TravelPiece {
	static final int C = 10, R = 8, STONES = 9;
	/** The buried gift, in local coordinates: under the coarse dirt before the tallest stone. */
	public static final int GIFT_X = C, GIFT_Y = -1, GIFT_Z = C + R - 2;

	public StandingStonesPiece(int x, int y, int z, Direction facing) { super(TravelSites.STONES_PIECE, x, y, z, 21, 10, 21, facing); }
	public StandingStonesPiece(CompoundTag tag) { super(TravelSites.STONES_PIECE, tag); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		// A trodden green inside the ring.
		for (int x = 0; x <= 20; x++) for (int z = 0; z <= 20; z++) {
			if (dist(x, z, C, C) > R + 1.5) continue;
			int n = noise(x, z);
			footing(level, bb, x, z, x, z, n < 12 ? b(Blocks.COARSE_DIRT) : n < 20 ? b(Blocks.MOSS_BLOCK) : GRASS, DIRT, 8);
			if (n > 88) set(level, bb, b(Blocks.SHORT_GRASS), x, 1, z);
		}
		// The stones: the tallest stands inward (local north), the rest lean shorter around the ring.
		for (int i = 0; i < STONES; i++) {
			double a = Math.PI / 2 + i * Math.PI * 2 / STONES;
			int x = C + (int) Math.round(R * Math.cos(a)), z = C + (int) Math.round(R * Math.sin(a));
			int top = i == 0 ? 6 : 3 + noise(i, 7) % 3;
			for (int y = -2; y <= top; y++) {
				int n = noise(x, y, z);
				BlockState s = n < 25 ? b(Blocks.MOSSY_COBBLESTONE) : n < 55 ? b(Blocks.ANDESITE) : b(Blocks.STONE);
				set(level, bb, s, x, y, z);
			}
			fillColumnDown(level, b(Blocks.STONE), x, -3, z, bb);
			if (i == 0) set(level, bb, b(Blocks.CHISELED_STONE_BRICKS), x, top + 1, z);
		}
		// The dais and its book.
		fill(level, bb, C - 1, 0, C - 1, C + 1, 0, C + 1, b(Blocks.POLISHED_ANDESITE));
		lectern(level, bb, C, 1, C, Direction.SOUTH, TravelSites.book("travel_first_roads", "The First Roads", "The Wayfarers"));
		// The kneeling stone before the tallest, and the gift buried beneath the dirt beside it.
		set(level, bb, b(Blocks.SMOOTH_STONE_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM), C, 1, GIFT_Z + 1);
		fill(level, bb, C, 0, GIFT_Z + 1, C, 0, GIFT_Z + 1, b(Blocks.STONE));
		set(level, bb, b(Blocks.COARSE_DIRT), GIFT_X, 0, GIFT_Z);
		chest(level, bb, GIFT_X, GIFT_Y, GIFT_Z, TravelSites.loot(TravelSites.STONES), Direction.SOUTH, 5);
		fill(level, bb, GIFT_X, GIFT_Y - 1, GIFT_Z, GIFT_X, GIFT_Y - 1, GIFT_Z, b(Blocks.STONE));
	}
}
