package dev.wildercord.world.sites.belowkeeper;

import dev.wildercord.world.hollowbell.HollowBellContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Belowkeeper Bell Sanctum: a vaulted underground resonance court in the Belowkeeper Hollows where The Hollow Bell waits.
 * Four copper-and-deepslate acoustic causeways meet at a central resonance plinth; the north listening gallery holds the
 * Listening Belfry, a lectern with The Quiet Eye, and a supply chest. Where the ground is within reach, a lined shaft runs
 * up the south causeway to a ruined wayside belfry above {@link BellSanctumRules}, so the sanctum shows in the world.
 */
public final class BellSanctumPiece extends BelowkeeperPiece {
	public static final int DECK = 2;

	public BellSanctumPiece(int x, int y, int z, Direction facing) {
		this(x, y, z, facing, BellSanctumRules.ROOM);
	}
	public BellSanctumPiece(int x, int y, int z, Direction facing, int height) {
		super(BelowkeeperSites.SANCTUM_PIECE, x, y, z, 35, height, 35, facing);
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
		// Candles burning on the plinth's four corners.
		for (int x : new int[]{15, 19}) for (int z : new int[]{15, 19}) {
			set(level, clip, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true), x, DECK + 1, z);
		}

		// A ledge of tile slabs runs round the walls at the chiseled course, so the walls read in two stages.
		for (int i = 1; i <= 33; i++) {
			set(level, clip, ledge(Blocks.DEEPSLATE_TILE_SLAB), i, DECK + 4, 1);
			set(level, clip, ledge(Blocks.DEEPSLATE_TILE_SLAB), i, DECK + 4, 33);
			set(level, clip, ledge(Blocks.DEEPSLATE_TILE_SLAB), 1, DECK + 4, i);
			set(level, clip, ledge(Blocks.DEEPSLATE_TILE_SLAB), 33, DECK + 4, i);
		}
		// Corbel table under the vault: brick stairs stepping out from all four walls, chiseled at the corners.
		for (int i = 2; i <= 32; i++) {
			set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.SOUTH, true), i, 10, 1);
			set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), i, 10, 33);
			set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.WEST, true), 1, 10, i);
			set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.EAST, true), 33, 10, i);
		}
		for (int x : new int[]{1, 33}) for (int z : new int[]{1, 33}) set(level, clip, CHISELED, x, 10, z);
		// Pilasters in bays along every wall; the ones under a rib carry a corbel for it.
		for (int p : new int[]{6, 11, 17, 23, 28}) {
			boolean rib = p == 6 || p == 17 || p == 28;
			if (p != 17) pilaster(level, clip, p, 1, rib ? Direction.SOUTH : null);
			pilaster(level, clip, p, 33, rib ? Direction.NORTH : null);
			pilaster(level, clip, 1, p, rib ? Direction.WEST : null);
			pilaster(level, clip, 33, p, rib ? Direction.EAST : null);
		}
		// Ribs of polished deepslate cross the vault on three lines each way, chiseled where they meet the walls.
		for (int r : new int[]{6, 17, 28}) {
			fill(level, clip, r, 10, 1, r, 10, 33, POLISHED);
			fill(level, clip, 1, 10, r, 33, 10, r, POLISHED);
			set(level, clip, CHISELED, r, 10, 1);
			set(level, clip, CHISELED, r, 10, 33);
			set(level, clip, CHISELED, 1, 10, r);
			set(level, clip, CHISELED, 33, 10, r);
			for (int q : new int[]{6, 17, 28}) set(level, clip, CHISELED, r, 10, q);
		}

		// Eight oxidized-copper and deepslate resonance pillars around the perimeter.
		for (int[] c : new int[][]{{6, 6}, {28, 6}, {6, 28}, {28, 28}, {12, 3}, {22, 3}, {12, 31}, {22, 31}}) {
			for (int y = DECK + 1; y <= DECK + 4; y++) set(level, clip, y == DECK + 2 ? OXIDIZED_COPPER : CHISELED, c[0], y, c[1]);
			set(level, clip, Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState(), c[0], DECK + 5, c[1]);
			set(level, clip, Blocks.LANTERN.defaultBlockState(), c[0], DECK + 6, c[1]);
		}
		// The four inner pillars rise on to the rib crossings, with a corbelled capital under each.
		for (int x : new int[]{6, 28}) for (int z : new int[]{6, 28}) {
			set(level, clip, CHISELED, x, DECK + 5, z);
			fill(level, clip, x, DECK + 6, z, x, 8, z, BRICK);
			set(level, clip, CHISELED, x, 9, z);
			set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.EAST, true), x - 1, 9, z);
			set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.WEST, true), x + 1, 9, z);
			set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), x, 9, z - 1);
			set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.SOUTH, true), x, 9, z + 1);
			for (Direction d : new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}) {
				set(level, clip, st(Blocks.POLISHED_DEEPSLATE_STAIRS, d, false), x - d.getStepX(), DECK + 1, z - lz(d));
			}
		}
		// Lamps hung from the ribs: soul light over the copper ring, plain lanterns out along the aisles.
		for (int[] l : new int[][]{{17, 10}, {17, 24}, {10, 17}, {24, 17}}) hang(level, clip, l[0], l[1], true);
		for (int[] l : new int[][]{{6, 12}, {6, 22}, {28, 12}, {28, 22}, {12, 6}, {22, 6}, {12, 28}, {22, 28}}) hang(level, clip, l[0], l[1], false);

		// North listening gallery holding the Listening Belfry, instruction lectern, and supply chest.
		for (int x : new int[]{14, 20}) for (int z : new int[]{2, 6}) {
			for (int y = DECK + 1; y <= DECK + 3; y++) set(level, clip, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(), x, y, z);
		}
		for (int x = 14; x <= 20; x++) for (int z = 2; z <= 6; z++) {
			set(level, clip, (x == 14 || x == 20 || z == 2 || z == 6) ? Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState() : DARK_OAK, x, DECK + 4, z);
		}
		// The gallery's front: corbelled shoulders off the posts, soul lamps in the opening, a hipped tile roof above.
		set(level, clip, st(Blocks.DARK_OAK_STAIRS, Direction.WEST, true), 15, DECK + 3, 6);
		set(level, clip, st(Blocks.DARK_OAK_STAIRS, Direction.EAST, true), 19, DECK + 3, 6);
		set(level, clip, st(Blocks.DARK_OAK_STAIRS, Direction.WEST, true), 15, DECK + 3, 2);
		set(level, clip, st(Blocks.DARK_OAK_STAIRS, Direction.EAST, true), 19, DECK + 3, 2);
		for (int x : new int[]{16, 18}) set(level, clip, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), x, DECK + 3, 6);
		hip(level, clip, 14, 2, 20, 6, DECK + 5, Blocks.DEEPSLATE_TILE_STAIRS, DARK_OAK, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
		// Shelves of listening records along the wall behind the belfry.
		for (int x = 14; x <= 20; x++) {
			set(level, clip, x == 17 ? CHISELED : Blocks.BOOKSHELF.defaultBlockState(), x, DECK + 1, 1);
			set(level, clip, x == 17 ? CHISELED : Blocks.BOOKSHELF.defaultBlockState(), x, DECK + 2, 1);
			set(level, clip, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState(), x, DECK + 3, 1);
		}
		set(level, clip, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true), 15, DECK + 3, 1);
		set(level, clip, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true), 19, DECK + 3, 1);
		set(level, clip, CHISELED, 17, DECK, 4);
		if (!(level.getBlockEntity(belfry()) instanceof dev.wildercord.world.hollowbell.ListeningBelfryEntity)) {
			set(level, clip, HollowBellContent.BELFRY.defaultBlockState(), 17, DECK + 1, 4);
		}
		lectern(level, clip, 15, DECK + 1, 4, Direction.SOUTH,
			BelowkeeperSites.book("bell_sanctum", "The Quiet Eye", "Mara, Belowkeeper"));
		chest(level, clip, 19, DECK + 1, 4, Direction.SOUTH, BelowkeeperSites.loot(BelowkeeperSites.BELL_SANCTUM), 53L);

		// Where the locate found the ground within reach, the piece was built tall: dig the shaft and raise the belfry.
		int span = getBoundingBox().getYSpan();
		if (span > BellSanctumRules.ROOM) tower(level, clip, span - BellSanctumRules.ABOVE);
	}

	/**
	 * The ruined belfry over the shaft: the vault roof opens above the south causeway, a lined 3x3 shaft with a ladder
	 * climbs to the ground, and a small broken wayside tower with the old bell on its beam marks the way down.
	 * {@code s} is the ground's height in this piece's local frame.
	 */
	private void tower(WorldGenLevel level, BoundingBox clip, int s) {
		// Broken vault: the roof opens over the south causeway, stair rubble on the lips of the hole.
		for (int x = 16; x <= 18; x++) for (int z = 24; z <= 26; z++) set(level, clip, AIR, x, 11, z);
		for (int z = 24; z <= 26; z++) {
			set(level, clip, st(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST, true), 15, 11, z);
			set(level, clip, st(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST, true), 19, 11, z);
		}
		for (int x = 16; x <= 18; x++) {
			set(level, clip, st(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH, true), x, 11, 23);
			set(level, clip, st(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH, true), x, 11, 27);
		}
		// The shaft's masonry continues down the causeway as a stub wall, so the ladder always has stone behind it.
		fill(level, clip, 17, DECK + 1, 23, 17, 10, 23, BRICK);
		set(level, clip, CHISELED, 17, DECK + 1, 23);
		// The shaft: a lined 3x3 rise from the vault to the surface.
		for (int y = 12; y <= s; y++) for (int x = 15; x <= 19; x++) for (int z = 23; z <= 27; z++) {
			boolean inner = x >= 16 && x <= 18 && z >= 24 && z <= 26;
			if (inner) set(level, clip, AIR, x, y, z);
			else {
				int n = noise(x, y, z);
				set(level, clip, n < 18 ? CRACKED_BRICK : n < 40 ? MOSSY_BRICK : TILE, x, y, z);
			}
		}
		for (int y = DECK + 1; y <= s; y++) set(level, clip, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH), 17, y, 24);
		// The tower floor: a flagged court at the ground, open over the shaft, packed under its outer ring.
		for (int x = 14; x <= 20; x++) for (int z = 22; z <= 28; z++) {
			boolean inner = x >= 16 && x <= 18 && z >= 24 && z <= 26;
			int n = noise(x, s, z);
			set(level, clip, inner ? AIR : n < 25 ? COBBLED : n < 55 ? BRICK : MOSSY_BRICK, x, s, z);
			if (!inner && (x == 14 || x == 20 || z == 22 || z == 28)) fillColumnDown(level, n < 50 ? COBBLED : DEEPSLATE, x, s - 1, z, clip);
			fill(level, clip, x, s + 1, z, x, s + BellSanctumRules.ABOVE - 1, z, AIR); // shrug off hillside and trees
		}
		// The tower: a 5x5 ring of brick, chiseled at the plinth, its top course kept only on the north and west,
		// the south-east corner collapsed, and a door with a stair arch on the south.
		for (int y = s + 1; y <= s + 4; y++) for (int x = 15; x <= 19; x++) for (int z = 23; z <= 27; z++) {
			if (!(x == 15 || x == 19 || z == 23 || z == 27)) continue;
			if (x == 17 && z == 27 && (y == s + 1 || y == s + 2)) continue; // the doorway
			if (y == s + 4 && !(x == 15 || z == 23)) continue; // the cornice survives on two sides
			if (y >= s + 3 && x >= 18 && z >= 26) continue; // the collapsed corner
			int n = noise(x, y, z);
			set(level, clip, y == s + 1 ? CHISELED : n < 15 ? CRACKED_BRICK : n < 35 ? MOSSY_BRICK : BRICK, x, y, z);
		}
		set(level, clip, CHISELED, 17, s + 3, 27); // lintel over the door
		set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.EAST, true), 16, s + 2, 27);
		set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.WEST, true), 18, s + 2, 27);
		// The two north corners stand whole, capped and lit.
		for (int[] c : new int[][]{{15, 23}, {19, 23}}) {
			fill(level, clip, c[0], s + 5, c[1], c[0], s + 6, c[1], BRICK);
			set(level, clip, CHISELED, c[0], s + 6, c[1]);
			set(level, clip, Blocks.LANTERN.defaultBlockState(), c[0], s + 7, c[1]);
		}
		// The bell beam over the shaft and the old bell hung from it.
		fill(level, clip, 15, s + 4, 25, 19, s + 4, 25, POLISHED);
		set(level, clip, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.EAST).setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING), 17, s + 3, 25);
		set(level, clip, Blocks.COBWEB.defaultBlockState(), 16, s + 1, 26);
		set(level, clip, Blocks.COBWEB.defaultBlockState(), 18, s + 1, 26);
		set(level, clip, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), 17, s + 2, 27);
		// A flagged approach south from the door.
		for (int x = 16; x <= 18; x++) for (int z = 28; z <= 31; z++) {
			int n = noise(x, s, z);
			set(level, clip, n < 30 ? MOSSY_BRICK : n < 60 ? COBBLED : BRICK, x, s, z);
			fillColumnDown(level, COBBLED, x, s - 1, z, clip);
			fill(level, clip, x, s + 1, z, x, s + 2, z, AIR);
		}
	}

	private static boolean furniture(int x, int y, int z) {
		return y == DECK + 1 && z == 4 && (x == 15 || x == 17 || x == 19);
	}

	/** A pilaster of polished deepslate against the wall, chiseled at foot and head; {@code corbel} faces the wall when a rib lands on it. */
	private void pilaster(WorldGenLevel level, BoundingBox clip, int x, int z, Direction corbel) {
		fill(level, clip, x, DECK + 1, z, x, 9, z, POLISHED);
		set(level, clip, CHISELED, x, DECK + 1, z);
		set(level, clip, CHISELED, x, 9, z);
		set(level, clip, CHISELED, x, 10, z);
		if (corbel != null) set(level, clip, st(Blocks.DEEPSLATE_BRICK_STAIRS, corbel, true), x - corbel.getStepX(), 9, z - lz(corbel));
	}

	/** A lantern hung from the rib above on one link of chain. */
	private void hang(WorldGenLevel level, BoundingBox clip, int x, int z, boolean soul) {
		set(level, clip, Blocks.CHAIN.defaultBlockState(), x, 9, z);
		set(level, clip, (soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN).defaultBlockState().setValue(LanternBlock.HANGING, true), x, 8, z);
	}

	/** A hipped roof over x0..x1, z0..z1 with one block of eave, each ring a step higher, closed by {@code cap}. */
	private void hip(WorldGenLevel level, BoundingBox clip, int x0, int z0, int x1, int z1, int y, Block stairs, BlockState under, BlockState cap) {
		for (int k = 0; ; k++) {
			int a = x0 - 1 + k, b = x1 + 1 - k, c = z0 - 1 + k, d = z1 + 1 - k, yy = y + k;
			if (a > b || c > d) return;
			if (a == b || c == d) {
				fill(level, clip, a, yy, c, b, yy, d, cap);
				return;
			}
			for (int x = a; x <= b; x++) {
				set(level, clip, st(stairs, Direction.NORTH, false), x, yy, c);
				set(level, clip, st(stairs, Direction.SOUTH, false), x, yy, d);
			}
			for (int z = c + 1; z <= d - 1; z++) {
				set(level, clip, st(stairs, Direction.EAST, false), a, yy, z);
				set(level, clip, st(stairs, Direction.WEST, false), b, yy, z);
			}
			if (k > 0 && a + 1 <= b - 1 && c + 1 <= d - 1) fill(level, clip, a + 1, yy, c + 1, b - 1, yy, d - 1, under);
		}
	}

	private static BlockState st(Block stairs, Direction facing, boolean top) {
		return stairs.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
	}

	/** The local z step of a facing: this piece's frame has NORTH pointing inward, along +z. */
	private static int lz(Direction d) {
		return d == Direction.NORTH ? 1 : d == Direction.SOUTH ? -1 : 0;
	}

	private static BlockState ledge(Block slab) {
		return slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
	}
}
