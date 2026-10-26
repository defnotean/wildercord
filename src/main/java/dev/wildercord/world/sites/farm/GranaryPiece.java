package dev.wildercord.world.sites.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * A walled farmstead round a tall gabled barn full of hay and stores, two cows in their stall: a round stone granary
 * tower under a conical cap beside it, a cart shed against its flank, a threshing floor and haystacks behind it,
 * watered fields and a sheep paddock before it, and in the yard a harvest shrine: a cake on a bale, a bell, carved
 * pumpkins and candles, and an offering chest for whoever comes to share the feast.
 */
public final class GranaryPiece extends FarmPiece {
	/** The farmyard round the barn, out to its dry-stone wall. */
	private static final int MARGIN = 8;

	public GranaryPiece(int x, int z, Direction facing) { super(FarmSites.GRANARY, x, 0, z, 21, 27, 25, facing, MARGIN); }
	public GranaryPiece(CompoundTag tag) { super(FarmSites.GRANARY, tag); }

	@Override
	protected int margin() { return MARGIN; }

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		clearCanopy(level, bb);
		site(level, bb, s(Blocks.GRASS_BLOCK), 1);
		fill(level, bb, 9, 0, -MARGIN, 11, 0, 5, s(Blocks.DIRT_PATH));
		yard(level, bb, MARGIN);
		granaryTower(level, bb, 23, 12);
		cartShed(level, bb);
		threshingFloor(level, bb);
		// Lantern posts at the head of the path.
		for (int x : new int[]{8, 12}) { fill(level, bb, x, 1, 1, x, 2, 1, s(Blocks.SPRUCE_FENCE)); set(level, bb, s(Blocks.LANTERN), x, 3, 1); }
		// The barn: spruce walls on a cobbled footing and a dark frame, a dark oak gable roof with its ridge down the middle.
		var planks = s(Blocks.SPRUCE_PLANKS);
		var frame = s(Blocks.STRIPPED_DARK_OAK_LOG);
		var beam = s(Blocks.DARK_OAK_PLANKS);
		Palette footing = Palette.of(Blocks.COBBLESTONE, 5, Blocks.MOSSY_COBBLESTONE, 3, Blocks.STONE, 1);
		fill(level, bb, 3, 0, 6, 17, 0, 22, planks);
		walls(level, bb, 3, 1, 6, 17, 6, 22, planks);
		walls(level, bb, 3, 1, 6, 17, 1, 22, footing);
		for (int x : new int[]{3, 17}) for (int z : new int[]{6, 14, 22}) fill(level, bb, x, 2, z, x, 6, z, frame);
		for (int x : new int[]{7, 13}) fill(level, bb, x, 2, 6, x, 5, 6, frame);
		fill(level, bb, 3, 6, 6, 17, 6, 6, frame);
		fill(level, bb, 3, 6, 22, 17, 6, 22, frame);
		// A sloping stone plinth along the long sides and the back.
		for (int z = 7; z <= 21; z++) {
			set(level, bb, stair(Blocks.COBBLESTONE_STAIRS, Direction.EAST, false), 2, 1, z);
			set(level, bb, stair(Blocks.COBBLESTONE_STAIRS, Direction.WEST, false), 18, 1, z);
		}
		for (int x = 4; x <= 16; x++) set(level, bb, stair(Blocks.COBBLESTONE_STAIRS, Direction.SOUTH, false), x, 1, 23);
		for (int i = 0; i < 8; i++) {
			int y = 7 + i;
			fill(level, bb, 2 + i, y, 5, 2 + i, y, 23, stairs(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
			fill(level, bb, 18 - i, y, 5, 18 - i, y, 23, stairs(Blocks.DARK_OAK_STAIRS, Direction.WEST, false));
			fill(level, bb, 3 + i, y, 6, 17 - i, y, 6, planks);
			fill(level, bb, 3 + i, y, 22, 17 - i, y, 22, planks);
		}
		// Corbels under the long eaves.
		for (int z = 5; z <= 23; z++) {
			set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, true), 2, 6, z);
			set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST, true), 18, 6, z);
		}
		fill(level, bb, 10, 14, 5, 10, 14, 23, s(Blocks.DARK_OAK_PLANKS));
		fill(level, bb, 10, 15, 5, 10, 15, 23, s(Blocks.DARK_OAK_SLAB));
		// A framed hayloft door in the front gable, a hoist beam over it, and a rose window in the back gable.
		fill(level, bb, 8, 8, 6, 12, 12, 6, beam);
		fill(level, bb, 9, 9, 6, 11, 11, 6, s(Blocks.HAY_BLOCK));
		set(level, bb, topSlab(Blocks.DARK_OAK_SLAB), 10, 13, 5); set(level, bb, topSlab(Blocks.DARK_OAK_SLAB), 10, 13, 4);
		set(level, bb, Blocks.IRON_CHAIN.defaultBlockState(), 10, 12, 4);
		set(level, bb, s(Blocks.HAY_BLOCK), 10, 11, 4);
		fill(level, bb, 10, 9, 22, 10, 11, 22, s(Blocks.GLASS));
		set(level, bb, s(Blocks.GLASS), 9, 10, 22); set(level, bb, s(Blocks.GLASS), 11, 10, 22);
		// A louvred cupola on the ridge, a lantern in it and a rod on its cap.
		fill(level, bb, 9, 15, 13, 11, 15, 15, beam);
		for (int x : new int[]{9, 11}) for (int z : new int[]{13, 15}) fill(level, bb, x, 16, z, x, 17, z, frame);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, true), 10, 17, 13); set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, true), 10, 17, 15);
		set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, true), 9, 17, 14); set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 11, 17, 14);
		hipRoof(level, bb, 9, 13, 11, 15, 18, Blocks.DARK_OAK_STAIRS, beam, s(Blocks.DARK_OAK_SLAB));
		set(level, bb, s(Blocks.LIGHTNING_ROD.waxed().unaffected()), 10, 21, 14);
		hangingLantern(level, bb, 10, 16, 14, 0, false);
		// Windows with sills and hoods.
		for (int z : new int[]{10, 18}) {
			for (int x : new int[]{3, 17}) fill(level, bb, x, 3, z, x, 4, z, s(Blocks.GLASS));
			set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, true), 2, 2, z); set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, true), 18, 2, z);
			set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, false), 2, 5, z); set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST, false), 18, 5, z);
		}
		// The big door, wide open, under a little pent hood with a lantern either side.
		fill(level, bb, 8, 1, 6, 12, 4, 6, AIR);
		fill(level, bb, 8, 5, 6, 12, 5, 6, frame);
		for (int x = 7; x <= 13; x++) set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false), x, 6, 5);
		set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, true), 7, 5, 5); set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, true), 13, 5, 5);
		hangingLantern(level, bb, 8, 5, 5, 0, false); hangingLantern(level, bb, 12, 5, 5, 0, false);
		// Tie beams across the barn, lanterns hung from them.
		for (int z : new int[]{10, 14, 18}) {
			fill(level, bb, 4, 6, z, 16, 6, z, beam);
			hangingLantern(level, bb, 7, 5, z, 0, false); hangingLantern(level, bb, 13, 5, z, 0, false);
		}
		// A little hipped canopy over the harvest shrine on four posts.
		for (int x : new int[]{13, 19}) for (int z : new int[]{1, 5}) fill(level, bb, x, 1, z, x, 3, z, frame);
		hipRoof(level, bb, 14, 2, 18, 4, 4, Blocks.SPRUCE_STAIRS, planks, s(Blocks.SPRUCE_SLAB));
		for (int x : new int[]{14, 18}) set(level, bb, s(Blocks.POTTED_RED_TULIP), x, 1, 5);
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
		// Fields before the barn, watered by a channel down their middle.
		for (int x = -4; x <= 6; x++) for (int z = -4; z <= 4; z++) {
			if (z == 0) { set(level, bb, s(Blocks.WATER), x, 0, z); continue; }
			set(level, bb, farmland(true), x, 0, z);
			Block crop = x < 1 ? (z < 0 ? Blocks.POTATOES : Blocks.BEETROOTS) : noise(x, z) < 50 ? Blocks.WHEAT : Blocks.CARROTS;
			set(level, bb, crop == Blocks.BEETROOTS ? s(crop).setValue(BlockStateProperties.AGE_3, 3) : crop(crop, 4 + noise(x, z) % 4), x, 1, z);
		}
		set(level, bb, s(Blocks.COMPOSTER), -5, 1, 0);
	}

	/** A dry-stone wall round the farmyard, piers with lanterns at its corners, a stone gateway where the path comes in. */
	private void yard(WorldGenLevel level, BoundingBox bb, int m) {
		int x0 = -m, z0 = -m, x1 = 20 + m, z1 = 24 + m;
		Palette stone = Palette.of(Blocks.COBBLESTONE, 4, Blocks.MOSSY_COBBLESTONE, 3, Blocks.STONE, 1);
		walls(level, bb, x0, 1, z0, x1, 1, z1, stone);
		for (int x = x0; x <= x1; x++) for (int z : new int[]{z0, z1}) set(level, bb, s(noise(x, z) < 40 ? Blocks.MOSSY_COBBLESTONE_WALL : Blocks.COBBLESTONE_WALL), x, 2, z);
		for (int z = z0; z <= z1; z++) for (int x : new int[]{x0, x1}) set(level, bb, s(noise(x, z) < 40 ? Blocks.MOSSY_COBBLESTONE_WALL : Blocks.COBBLESTONE_WALL), x, 2, z);
		for (int x : new int[]{x0, x1}) for (int z : new int[]{z0, z1}) {
			fill(level, bb, x, 1, z, x, 3, z, s(Blocks.STONE_BRICKS));
			set(level, bb, s(Blocks.LANTERN), x, 4, z);
		}
		// The gateway: tall piers, a timber lintel, a lantern hung over the way in.
		fill(level, bb, 9, 1, z0, 11, 2, z0, AIR);
		for (int x : new int[]{8, 12}) {
			fill(level, bb, x, 1, z0, x, 4, z0, s(Blocks.STONE_BRICKS));
			set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), x, 3, z0);
			set(level, bb, s(Blocks.STONE_BRICK_SLAB), x, 6, z0);
		}
		fill(level, bb, 8, 5, z0, 12, 5, z0, s(Blocks.DARK_OAK_PLANKS));
		fill(level, bb, 9, 6, z0, 11, 6, z0, s(Blocks.DARK_OAK_SLAB));
		set(level, bb, s(Blocks.STRIPPED_DARK_OAK_LOG), 10, 6, z0);
		hangingLantern(level, bb, 10, 4, z0, 0, false);
		// The sheep paddock between the gate and the granary tower, a trough along its side.
		walls(level, bb, 20, 1, -4, 25, 1, 6, s(Blocks.SPRUCE_FENCE));
		set(level, bb, facing(Blocks.SPRUCE_FENCE_GATE, Direction.WEST), 20, 1, 0);
		fill(level, bb, 24, 0, 2, 24, 0, 4, s(Blocks.WATER));
		set(level, bb, s(Blocks.HAY_BLOCK), 24, 1, -3);
		sentry(level, bb, EntityTypes.SHEEP, 22, 1, -1);
		sentry(level, bb, EntityTypes.SHEEP, 22, 1, 3);
	}

	/**
	 * A round stone granary tower beside the barn, banded and slit-windowed, corbelled out under a conical dark oak cap
	 * with a rod on top. Its door faces the barn.
	 */
	private void granaryTower(WorldGenLevel level, BoundingBox bb, int cx, int cz) {
		Palette stone = Palette.of(Blocks.STONE_BRICKS, 6, Blocks.MOSSY_STONE_BRICKS, 2, Blocks.CRACKED_STONE_BRICKS, 1);
		var band = s(Blocks.POLISHED_ANDESITE);
		for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
			int d2 = dx * dx + dz * dz, x = cx + dx, z = cz + dz;
			if (d2 <= 12) {
				set(level, bb, s(Blocks.COBBLESTONE), x, 0, z);
				boolean wall = d2 > 6;
				for (int y = 1; y <= 13; y++) {
					if (!wall) set(level, bb, y <= 4 ? s(Blocks.HAY_BLOCK) : AIR, x, y, z);
					else if (y == 5 || y == 10) set(level, bb, band, x, y, z);
					else fill(level, bb, x, y, z, x, y, z, stone);
				}
			} else if (d2 <= 20) {
				// A sloping footing round the base, corbels under the cap.
				set(level, bb, stair(Blocks.COBBLESTONE_STAIRS, inward(dx, dz), false), x, 1, z);
				set(level, bb, stair(Blocks.DARK_OAK_STAIRS, inward(dx, dz), true), x, 13, z);
			}
		}
		// The cap: rings of stairs climbing inward to a point.
		for (int k = 0; k <= 4; k++) {
			int r = 4 - k, inner = r - 1;
			for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 > r * r + r) continue;
				boolean ring = r > 0 && (inner < 0 || d2 > inner * inner + inner);
				set(level, bb, ring ? stair(Blocks.DARK_OAK_STAIRS, inward(dx, dz), false) : s(Blocks.DARK_OAK_PLANKS), cx + dx, 14 + k, cz + dz);
			}
		}
		set(level, bb, s(Blocks.DARK_OAK_FENCE), cx, 19, cz);
		set(level, bb, s(Blocks.LIGHTNING_ROD.waxed().unaffected()), cx, 20, cz);
		// Slit windows round the shaft.
		for (int y : new int[]{7, 11}) {
			set(level, bb, s(Blocks.GLASS_PANE), cx, y, cz - 3);
			set(level, bb, s(Blocks.GLASS_PANE), cx, y, cz + 3);
			set(level, bb, s(Blocks.GLASS_PANE), cx + 3, y, cz);
		}
		// The door toward the barn under a little hood, and a loading hatch above it.
		fill(level, bb, cx - 3, 1, cz, cx - 3, 2, cz, AIR);
		fill(level, bb, cx - 2, 1, cz, cx - 2, 2, cz, AIR);
		set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), cx - 3, 3, cz);
		set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, false), cx - 4, 3, cz);
		fill(level, bb, cx - 3, 8, cz, cx - 3, 9, cz, s(Blocks.SPRUCE_PLANKS));
		set(level, bb, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, false), cx - 4, 10, cz);
		hangingLantern(level, bb, cx, 12, cz, 0, false);
	}

	/** The stair facing that climbs toward a ring's centre from offset (dx, dz). */
	private static Direction inward(int dx, int dz) {
		if (Math.abs(dx) >= Math.abs(dz)) return dx < 0 ? Direction.EAST : Direction.WEST;
		return dz < 0 ? Direction.SOUTH : Direction.NORTH;
	}

	/** An open-fronted cart shed along the barn's west flank: a lean-to roof on posts, a hay cart, a woodpile and tools. */
	private void cartShed(WorldGenLevel level, BoundingBox bb) {
		var frame = s(Blocks.STRIPPED_DARK_OAK_LOG);
		for (int z : new int[]{7, 11, 15, 19, 21}) fill(level, bb, -3, 1, z, -3, 3, z, frame);
		for (int x = -4; x <= 1; x++) {
			int y = 4 + (x + 4) / 2;
			for (int z = 6; z <= 22; z++) set(level, bb, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false), x, y, z);
			// Boarded ends under the slope.
			if (x > -3 && y > 4) for (int z : new int[]{7, 21}) fill(level, bb, x, 4, z, x, y - 1, z, s(Blocks.SPRUCE_PLANKS));
		}
		fill(level, bb, -3, 4, 7, -3, 4, 21, s(Blocks.DARK_OAK_PLANKS));
		for (int z = 8; z <= 20; z++) set(level, bb, s(z % 3 == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH), -2, 0, z);
		// The hay cart: a bed of slabs on wheels, a load of hay on it.
		fill(level, bb, -2, 1, 9, 0, 1, 11, s(Blocks.SPRUCE_SLAB));
		fill(level, bb, -1, 2, 9, 0, 2, 11, s(Blocks.HAY_BLOCK));
		set(level, bb, s(Blocks.HAY_BLOCK), 0, 3, 10);
		set(level, bb, s(Blocks.DARK_OAK_FENCE), -2, 1, 12);
		// A woodpile, barrels and a grindstone.
		var log = s(Blocks.SPRUCE_LOG).setValue(BlockStateProperties.AXIS, Direction.Axis.Z);
		fill(level, bb, 0, 1, 14, 1, 2, 17, log);
		fill(level, bb, 1, 3, 15, 1, 3, 16, log);
		var barrel = s(Blocks.BARREL).setValue(BlockStateProperties.FACING, Direction.UP);
		set(level, bb, barrel, 1, 1, 19); set(level, bb, barrel, 0, 1, 20); set(level, bb, barrel, 1, 2, 19);
		set(level, bb, floorAttached(Blocks.GRINDSTONE, Direction.EAST), -1, 1, 18);
		hangingLantern(level, bb, -1, 4, 13, 0, false);
	}

	/** Behind the barn, a cobbled threshing floor round a millstone, haystacks along the back wall. */
	private void threshingFloor(WorldGenLevel level, BoundingBox bb) {
		int cx = 10, cz = 27;
		for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
			int d2 = dx * dx + dz * dz;
			if (d2 > 11) continue;
			set(level, bb, s(d2 > 6 ? Blocks.POLISHED_ANDESITE : d2 > 2 ? Blocks.COBBLESTONE : Blocks.STONE_BRICKS), cx + dx, 0, cz + dz);
		}
		set(level, bb, s(Blocks.SMOOTH_STONE), cx, 1, cz);
		set(level, bb, s(Blocks.STRIPPED_SPRUCE_LOG), cx, 2, cz);
		set(level, bb, s(Blocks.SPRUCE_FENCE), cx, 3, cz);
		set(level, bb, s(Blocks.HAY_BLOCK), cx + 2, 1, cz + 1);
		// Haystacks: hay heaped in a dome, a pole through the crown.
		for (int[] h : new int[][]{{3, 27}, {17, 27}, {-2, 27}, {22, 27}}) {
			for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
				if (dx * dz == 0 || noise(h[0] + dx, h[1] + dz) < 60) set(level, bb, s(Blocks.HAY_BLOCK), h[0] + dx, 1, h[1] + dz);
			fill(level, bb, h[0], 2, h[1] - 1, h[0], 2, h[1] + 1, s(Blocks.HAY_BLOCK));
			fill(level, bb, h[0] - 1, 2, h[1], h[0] + 1, 2, h[1], s(Blocks.HAY_BLOCK));
			set(level, bb, s(Blocks.HAY_BLOCK), h[0], 3, h[1]);
			set(level, bb, s(Blocks.SPRUCE_FENCE), h[0], 4, h[1]);
		}
	}
}
