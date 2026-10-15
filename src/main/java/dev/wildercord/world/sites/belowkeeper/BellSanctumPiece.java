package dev.wildercord.world.sites.belowkeeper;

import dev.wildercord.world.hollowbell.HollowBellContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Belowkeeper Bell Sanctum: a vaulted underground resonance court in the Belowkeeper Hollows where The Hollow Bell waits.
 * Four copper-and-deepslate acoustic causeways meet at a central resonance plinth; the north listening gallery holds the
 * Listening Belfry, a lectern with The Quiet Eye, and a supply chest.
 */
public final class BellSanctumPiece extends BelowkeeperPiece {
	public static final int DECK = 2;

	public BellSanctumPiece(int x, int y, int z, Direction facing) {
		super(BelowkeeperSites.SANCTUM_PIECE, x, y, z, 35, 12, 35, facing);
	}
	public BellSanctumPiece(CompoundTag tag) { super(BelowkeeperSites.SANCTUM_PIECE, tag); }

	public BlockPos plinth() { return world(17, DECK, 17); }
	public BlockPos belfry() { return world(17, DECK + 1, 4); }

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox clip, ChunkPos chunk, BlockPos pivot) {
		// Replaying a clipped piece must not erase the saved belfry identity or refill an opened chest.
		for (int x = 1; x <= 33; x++) for (int y = DECK + 1; y <= 10; y++) for (int z = 1; z <= 33; z++) {
			if (!furniture(x, y, z)) set(level, clip, AIR, x, y, z);
		}
		// Solid outer shell and vaulted ceiling so the underground resonance court stays dry and readable.
		footing(level, clip, 0, 0, 34, 34, DECK - 1);
		for (int x = 0; x <= 34; x++) for (int z = 0; z <= 34; z++) {
			boolean border = x == 0 || x == 34 || z == 0 || z == 34;
			if (border) {
				for (int y = DECK; y <= 10; y++) {
					int n = noise(x, y, z);
					set(level, clip, y == DECK + 4 ? CHISELED : (n < 25 ? CRACKED_BRICK : BRICK), x, y, z);
				}
			}
			BlockState roof = (x % 5 == 0 || z % 5 == 0) ? CHISELED : (Math.hypot(x - 17, z - 17) <= 6.5 ? COPPER_GRATE : TILE);
			set(level, clip, roof, x, 11, z);
		}
		// Resonance court floor: copper-lined acoustic ring and four damped compass causeways.
		for (int x = 1; x <= 33; x++) for (int z = 1; z <= 33; z++) {
			double dist = Math.hypot(x - 17, z - 17);
			boolean causeway = Math.abs(x - 17) <= 2 || Math.abs(z - 17) <= 2;
			boolean ring = dist >= 5.8 && dist <= 7.0;
			boolean outerRing = dist >= 12.5 && dist <= 14.5;
			boolean center = dist < 3.0;
			int n = noise(x, DECK, z);
			BlockState floor;
			if (center) floor = POLISHED;
			else if (ring) floor = (x + z & 1) == 0 ? COPPER : OXIDIZED_COPPER;
			else if (causeway || outerRing) floor = n < 22 ? MOSSY_BRICK : n < 50 ? CRACKED_BRICK : BRICK;
			else floor = n < 28 ? Blocks.MOSS_BLOCK.defaultBlockState() : n < 65 ? Blocks.TUFF.defaultBlockState() : DEEPSLATE;
			set(level, clip, floor, x, DECK, z);
		}
		// Central Resonance Plinth (17, DECK, 17).
		for (int x = 15; x <= 19; x++) for (int z = 15; z <= 19; z++) {
			set(level, clip, (x == 15 || x == 19 || z == 15 || z == 19) ? CHISELED : POLISHED, x, DECK, z);
		}
		set(level, clip, Blocks.LODESTONE.defaultBlockState(), 17, DECK, 17);

		// Eight oxidized-copper and deepslate resonance pillars around the perimeter.
		for (int[] c : new int[][]{{6, 6}, {28, 6}, {6, 28}, {28, 28}, {12, 3}, {22, 3}, {12, 31}, {22, 31}}) {
			for (int y = DECK + 1; y <= DECK + 4; y++) set(level, clip, y == DECK + 2 ? OXIDIZED_COPPER : CHISELED, c[0], y, c[1]);
			set(level, clip, Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState(), c[0], DECK + 5, c[1]);
			set(level, clip, Blocks.LANTERN.defaultBlockState(), c[0], DECK + 6, c[1]);
		}

		// North listening gallery holding the Listening Belfry, instruction lectern, and supply chest.
		for (int x : new int[]{14, 20}) for (int z : new int[]{2, 6}) {
			for (int y = DECK + 1; y <= DECK + 3; y++) set(level, clip, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), x, y, z);
		}
		for (int x = 14; x <= 20; x++) for (int z = 2; z <= 6; z++) {
			set(level, clip, (x == 14 || x == 20 || z == 2 || z == 6) ? Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState() : DARK_OAK, x, DECK + 4, z);
		}
		set(level, clip, CHISELED, 17, DECK, 4);
		if (!(level.getBlockEntity(belfry()) instanceof dev.wildercord.world.hollowbell.ListeningBelfryEntity)) {
			set(level, clip, HollowBellContent.BELFRY.defaultBlockState(), 17, DECK + 1, 4);
		}
		lectern(level, clip, 15, DECK + 1, 4, Direction.SOUTH,
			BelowkeeperSites.book("bell_sanctum", "The Quiet Eye", "Mara, Belowkeeper"));
		chest(level, clip, 19, DECK + 1, 4, Direction.SOUTH, BelowkeeperSites.loot(BelowkeeperSites.BELL_SANCTUM), 53L);
	}

	private static boolean furniture(int x, int y, int z) {
		return y == DECK + 1 && z == 4 && (x == 15 || x == 17 || x == 19);
	}
}
