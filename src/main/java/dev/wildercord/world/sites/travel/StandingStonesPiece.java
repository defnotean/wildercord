package dev.wildercord.world.sites.travel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Nine standing stones around a reading dais, the tallest framed by a trilithon. The book on the dais tells how the first roads were laid between
 * stones like these, and where the road-makers left their gift: under the kneeling stone before the tallest one. The ring stands on a
 * levelled green held up by a dressed kerb, the stones are pale broad monoliths, and braziers burn at the avenue mouth so the circle reads
 * from across a valley even on a hillside.
 */
public final class StandingStonesPiece extends TravelPiece {
	static final int C = 10, R = 8, STONES = 9;
	/** The buried gift, in local coordinates: under the coarse dirt before the tallest stone. */
	public static final int GIFT_X = C, GIFT_Y = -1, GIFT_Z = C + R - 2;

	public StandingStonesPiece(int x, int y, int z, Direction facing) { super(TravelSites.STONES_PIECE, x, y, z, 21, 14, 21, facing); }
	public StandingStonesPiece(CompoundTag tag) { super(TravelSites.STONES_PIECE, tag); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		// A trodden green inside the ring.
		for (int x = 0; x <= 20; x++) for (int z = 0; z <= 20; z++) {
			if (dist(x, z, C, C) > R + 1.5) continue;
			int n = noise(x, z);
			footing(level, bb, x, z, x, z, n < 12 ? b(Blocks.COARSE_DIRT) : n < 20 ? b(Blocks.MOSS_BLOCK) : GRASS, DIRT, 12);
			if (n > 88) set(level, bb, b(Blocks.SHORT_GRASS), x, 1, z);
		}
		// The kerb holding up the green: a dressed stone-brick ring with a slab coping, open where the avenue comes in.
		for (int x = 0; x <= 20; x++) for (int z = 0; z <= 20; z++) {
			double d = dist(x, z, C, C);
			if (d <= R + 1.5 || d > R + 2.5) continue;
			int n = noise(x, 9, z);
			footing(level, bb, x, z, x, z, n < 30 ? b(Blocks.MOSSY_STONE_BRICKS) : n < 45 ? b(Blocks.CRACKED_STONE_BRICKS) : b(Blocks.STONE_BRICKS),
				b(Blocks.COBBLESTONE), 12);
			if (z < C && Math.abs(x - C) <= 1) continue;
			set(level, bb, b(n < 30 ? Blocks.MOSSY_STONE_BRICK_SLAB : Blocks.STONE_BRICK_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM), x, 1, z);
		}
		// A processional ring trodden into the green, and an avenue from the road to the dais between the two southern stones.
		for (int x = 0; x <= 20; x++) for (int z = 0; z <= 20; z++) {
			double d = dist(x, z, C, C);
			if (d > 4.5 && d < 5.5 && noise(x, 3, z) < 75) set(level, bb, b(Blocks.DIRT_PATH), x, 0, z);
		}
		for (int z = 0; z <= 7; z++) {
			set(level, bb, b(Blocks.DIRT_PATH), C, 0, z);
			for (int x : new int[]{C - 1, C + 1}) set(level, bb, noise(x, z) < 40 ? b(Blocks.MOSSY_COBBLESTONE) : b(Blocks.COBBLESTONE), x, 0, z);
		}
		// Braziers flank the avenue mouth, and lanterns on short posts light the way in.
		for (int x : new int[]{C - 2, C + 2}) {
			set(level, bb, b(Blocks.CHISELED_STONE_BRICKS), x, 1, 0);
			set(level, bb, b(Blocks.CAMPFIRE), x, 2, 0);
		}
		for (int x : new int[]{C - 2, C + 2}) {
			set(level, bb, b(Blocks.STONE_BRICK_WALL), x, 1, 5);
			set(level, bb, b(Blocks.LANTERN), x, 2, 5);
		}
		// The stones: the tallest stands inward (local north), the rest lean shorter around the ring. Each sits in a packed
		// collar of cobble, and the lesser ones carry a wayfarer's candle on top.
		for (int i = 0; i < STONES; i++) {
			double a = Math.PI / 2 + i * Math.PI * 2 / STONES;
			int x = C + (int) Math.round(R * Math.cos(a)), z = C + (int) Math.round(R * Math.sin(a));
			int top = i == 0 ? 9 : 5 + noise(i, 7) % 3;
			// Each monolith is two columns broad along the ring, its shoulder a little lower than its crown.
			int tx = -(int) Math.round(Math.sin(a)), tz = (int) Math.round(Math.cos(a));
			for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				int cx = x + d[0], cz = z + d[1];
				if (i == 0 && cz < z) continue;
				set(level, bb, noise(cx, cz) < 50 ? b(Blocks.MOSSY_COBBLESTONE) : b(Blocks.COBBLESTONE), cx, 0, cz);
				if (noise(cx, 5, cz) < 35) set(level, bb, b(Blocks.FERN), cx, 1, cz);
			}
			for (int y = -2; y <= top; y++) {
				for (int w = 0; w <= 1; w++) {
					int sx = x + tx * w, sz = z + tz * w;
					if (w == 1 && y > top - 1 - noise(i, 2) % 2) continue;
					int n = noise(sx, y, sz);
					BlockState s = y <= 1 && n < 45 ? b(Blocks.MOSSY_COBBLESTONE) : n < 45 ? b(Blocks.CALCITE) : n < 80 ? b(Blocks.DIORITE) : b(Blocks.TUFF);
					set(level, bb, s, sx, y, sz);
				}
			}
			fillColumnDown(level, b(Blocks.STONE), x, -3, z, bb);
			fillColumnDown(level, b(Blocks.STONE), x + tx, -3, z + tz, bb);
			if (i == 0) set(level, bb, b(Blocks.CHISELED_STONE_BRICKS), x, top + 1, z);
			else set(level, bb, b(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 1 + noise(i, 3) % 3).setValue(CandleBlock.LIT, true), x, top + 1, z);
		}
		// The tallest stone is framed by a trilithon: two weathered uprights and a lintel laid across them.
		for (int x : new int[]{C - 2, C + 2}) {
			for (int y = -2; y <= 10; y++) {
				int n = noise(x, y, C + R);
				set(level, bb, n < 30 ? b(Blocks.MOSSY_STONE_BRICKS) : n < 50 ? b(Blocks.CRACKED_STONE_BRICKS) : b(Blocks.STONE_BRICKS), x, y, C + R);
			}
			fillColumnDown(level, b(Blocks.STONE), x, -3, C + R, bb);
		}
		fill(level, bb, C - 2, 11, C + R, C + 2, 11, C + R, Palette.of(Blocks.STONE_BRICKS, 5, Blocks.MOSSY_STONE_BRICKS, 3, Blocks.CRACKED_STONE_BRICKS, 2));
		for (int x : new int[]{C - 1, C + 1}) set(level, bb, stair(Blocks.MOSSY_STONE_BRICK_STAIRS, x < C ? Direction.WEST : Direction.EAST, true), x, 10, C + R);
		set(level, bb, b(Blocks.VINE).setValue(VineBlock.NORTH, true), C - 2, 9, C + R - 1);
		set(level, bb, b(Blocks.VINE).setValue(VineBlock.NORTH, true), C - 2, 8, C + R - 1);
		// The dais and its book: polished stone on a stone-brick kerb, a candle at each corner.
		for (int x = C - 3; x <= C + 3; x++) for (int z = C - 3; z <= C + 3; z++) {
			if (dist(x, z, C, C) > 2.5) continue;
			set(level, bb, noise(x, 1, z) < 30 ? b(Blocks.MOSSY_STONE_BRICKS) : b(Blocks.STONE_BRICKS), x, 0, z);
		}
		fill(level, bb, C - 1, 0, C - 1, C + 1, 0, C + 1, b(Blocks.POLISHED_ANDESITE));
		for (int[] c : new int[][]{{C - 1, C - 1}, {C + 1, C - 1}, {C - 1, C + 1}, {C + 1, C + 1}})
			set(level, bb, b(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true), c[0], 1, c[1]);
		lectern(level, bb, C, 1, C, Direction.SOUTH, TravelSites.book("travel_first_roads", "The First Roads", "The Wayfarers"));
		// The kneeling stone before the tallest, and the gift buried beneath the dirt beside it.
		set(level, bb, b(Blocks.SMOOTH_STONE_SLAB).setValue(SlabBlock.TYPE, SlabType.BOTTOM), C, 1, GIFT_Z + 1);
		fill(level, bb, C, 0, GIFT_Z + 1, C, 0, GIFT_Z + 1, b(Blocks.STONE));
		set(level, bb, b(Blocks.COARSE_DIRT), GIFT_X, 0, GIFT_Z);
		chest(level, bb, GIFT_X, GIFT_Y, GIFT_Z, TravelSites.loot(TravelSites.STONES), Direction.SOUTH, 5);
		fill(level, bb, GIFT_X, GIFT_Y - 1, GIFT_Z, GIFT_X, GIFT_Y - 1, GIFT_Z, b(Blocks.STONE));
	}
}
