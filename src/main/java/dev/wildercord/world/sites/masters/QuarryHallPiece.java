package dev.wildercord.world.sites.masters;

import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/**
 * A gabled mountain hall of the Stone or Iron school around a sunken sparring ring, one Runebound sparring guard waiting in it,
 * with its working quarry beside it: a stepped pit, two timber derricks and stacks of cut blocks.
 */
public final class QuarryHallPiece extends MasterSitePiece {
	public QuarryHallPiece(int x, int y, int z, Direction facing, boolean alt) { super(MasterSites.QUARRY_HALL, x, y, z, 29, 19, 19, facing, alt); }
	public QuarryHallPiece(CompoundTag tag) { super(MasterSites.QUARRY_HALL, tag); }
	@Override public String site() { return "master_quarry_hall"; }
	@Override public List<String> schools() { return List.of("stone", "iron"); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		BlockState wall = alt ? s(Blocks.DEEPSLATE_BRICKS) : s(Blocks.STONE_BRICKS);
		BlockState rim = alt ? s(Blocks.POLISHED_DEEPSLATE) : s(Blocks.POLISHED_ANDESITE), light = alt ? s(Blocks.SEA_LANTERN) : s(Blocks.SHROOMLIGHT);
		BlockState chiseled = alt ? s(Blocks.CHISELED_DEEPSLATE) : s(Blocks.CHISELED_STONE_BRICKS);
		Palette masonry = alt ? Palette.of(Blocks.DEEPSLATE_BRICKS, 7, Blocks.CRACKED_DEEPSLATE_BRICKS, 2, Blocks.DEEPSLATE_TILES, 1)
			: Palette.of(Blocks.STONE_BRICKS, 8, Blocks.CRACKED_STONE_BRICKS, 2, Blocks.MOSSY_STONE_BRICKS, 1);
		Block trim = alt ? Blocks.POLISHED_DEEPSLATE_STAIRS : Blocks.STONE_BRICK_STAIRS;
		Block roofStairs = alt ? Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS : Blocks.DEEPSLATE_TILE_STAIRS;
		BlockState timber = alt ? s(Blocks.STRIPPED_DARK_OAK_LOG) : s(Blocks.STRIPPED_SPRUCE_LOG);
		BlockState wallTop = alt ? s(Blocks.POLISHED_DEEPSLATE_WALL) : s(Blocks.STONE_BRICK_WALL);
		pad(level, bb, 0, 0, 28, 18, 18, s(Blocks.SMOOTH_STONE), s(Blocks.STONE));
		// The hall walls, open to the rafters.
		room(level, bb, 1, 0, 1, 17, 7, 17, wall);
		walls(level, bb, 1, 1, 1, 17, 7, 17, masonry);
		fill(level, bb, 2, 7, 2, 16, 7, 16, AIR);
		fill(level, bb, 1, 0, 1, 17, 0, 17, s(Blocks.SMOOTH_STONE));
		fill(level, bb, 8, 0, 1, 10, 0, 5, rim);
		fill(level, bb, 1, 8, 1, 1, 8, 17, masonry); fill(level, bb, 17, 8, 1, 17, 8, 17, masonry);
		fill(level, bb, 8, 1, 1, 10, 4, 1, AIR);
		for (int z : new int[]{5, 9, 13}) { fill(level, bb, 1, 3, z, 1, 4, z, s(Blocks.IRON_BARS)); fill(level, bb, 17, 3, z, 17, 4, z, s(Blocks.IRON_BARS)); }
		for (int x : new int[]{5, 13}) fill(level, bb, x, 3, 1, x, 4, 1, s(Blocks.IRON_BARS));
		// A steep gabled roof with deep eaves, its ridge running back from the entrance gable.
		gableRoof(level, bb, 1, 1, 17, 17, 8, false, roofStairs, chiseled, masonry);
		for (int z : new int[]{0, 6, 12, 18}) set(level, bb, wallTop, 9, 18, z);
		// The front gable: a carved roundel over the door.
		for (int x = 8; x <= 10; x++) for (int y = 11; y <= 13; y++) {
			boolean corner = x != 9 && y != 12;
			set(level, bb, x == 9 && y == 12 ? light : corner ? chiseled : rim, x, y, 1);
		}
		// Pilasters down the long walls, plinth stairs between them, sills and hoods at the windows.
		for (int z = 0; z <= 18; z++) {
			boolean pier = z == 0 || z == 1 || z == 3 || z == 7 || z == 11 || z == 15 || z == 17 || z == 18;
			if (pier) {
				pillar(level, bb, 0, z, 1, 6, rim, wall, chiseled);
				pillar(level, bb, 18, z, 1, 6, rim, wall, chiseled);
			} else {
				set(level, bb, stair(trim, Direction.EAST, false), 0, 1, z);
				set(level, bb, stair(trim, Direction.WEST, false), 18, 1, z);
			}
		}
		for (int z : new int[]{5, 9, 13}) {
			set(level, bb, stair(trim, Direction.EAST, true), 0, 2, z); set(level, bb, stair(trim, Direction.EAST, true), 0, 5, z);
			set(level, bb, stair(trim, Direction.WEST, true), 18, 2, z); set(level, bb, stair(trim, Direction.WEST, true), 18, 5, z);
		}
		for (int x = 1; x <= 17; x++) {
			boolean pier = x == 1 || x == 4 || x == 14 || x == 17;
			if (pier) { pillar(level, bb, x, 0, 1, 6, rim, wall, chiseled); pillar(level, bb, x, 18, 1, 6, rim, wall, chiseled); continue; }
			set(level, bb, stair(trim, Direction.SOUTH, false), x, 1, 18);
			if (x < 7 || x > 11) set(level, bb, stair(trim, Direction.NORTH, false), x, 1, 0);
		}
		for (int x : new int[]{5, 13}) { set(level, bb, stair(trim, Direction.NORTH, true), x, 2, 0); set(level, bb, stair(trim, Direction.NORTH, true), x, 5, 0); }
		// The porch: two columns under a small stepped pediment, a lantern hung in the opening.
		for (int x : new int[]{7, 11}) pillar(level, bb, x, 0, 1, 5, rim, wall, chiseled);
		set(level, bb, stair(trim, Direction.EAST, false), 7, 6, 0); set(level, bb, stair(trim, Direction.WEST, false), 11, 6, 0);
		fill(level, bb, 8, 6, 0, 10, 6, 0, rim);
		set(level, bb, stair(trim, Direction.EAST, false), 8, 7, 0); set(level, bb, stair(trim, Direction.WEST, false), 10, 7, 0);
		set(level, bb, chiseled, 9, 7, 0);
		set(level, bb, alt ? s(Blocks.POLISHED_DEEPSLATE_SLAB) : s(Blocks.STONE_BRICK_SLAB), 9, 8, 0);
		hangingLantern(level, bb, 9, 5, 0, 0, false);
		// Timber trusses: posts on the walls, tie beams with lights set in them, king posts to the ridge.
		BlockState tie = timber.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
		for (int z : new int[]{3, 7, 11, 15}) {
			fill(level, bb, 2, 1, z, 2, 6, z, timber); fill(level, bb, 16, 1, z, 16, 6, z, timber);
			fill(level, bb, 2, 7, z, 16, 7, z, tie);
			fill(level, bb, 9, 8, z, 9, 16, z, timber);
			hangingLantern(level, bb, 9, 6, z, 1, false);
		}
		for (int[] p : new int[][]{{5, 7}, {13, 7}, {5, 15}, {13, 15}}) set(level, bb, light, p[0], 7, p[1]);
		for (int z : new int[]{5, 13}) { hangingLantern(level, bb, 5, 16 - 4, z, 1, false); hangingLantern(level, bb, 13, 16 - 4, z, 1, false); }
		Block banner = Blocks.WALL_BANNER.pick(alt ? DyeColor.LIGHT_GRAY : DyeColor.BROWN);
		banner(level, bb, banner, 5, 5, 16, Direction.SOUTH); banner(level, bb, banner, 13, 5, 16, Direction.SOUTH);
		banner(level, bb, banner, 3, 5, 0, Direction.NORTH); banner(level, bb, banner, 15, 5, 0, Direction.NORTH);
		// The sparring ring: a pit two blocks deep, lit, with a ladder out on the far side.
		for (int x = 3; x <= 15; x++) for (int z = 4; z <= 16; z++) {
			double d = dist(x, z, 9, 10);
			if (d <= 5.5) fill(level, bb, x, -3, z, x, -1, z, wall);
			if (d <= 3.5) { fill(level, bb, x, -2, z, x, 0, z, AIR); set(level, bb, s(noise(x, z) < 50 ? Blocks.GRAVEL : Blocks.COARSE_DIRT), x, -3, z); }
			else if (d <= 4.5) set(level, bb, rim, x, 0, z);
		}
		set(level, bb, light, 5, -2, 10); set(level, bb, light, 13, -2, 10);
		fill(level, bb, 9, -2, 13, 9, 0, 13, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		// Four lantern posts at the ring's corners.
		for (int[] p : new int[][]{{5, 6}, {13, 6}, {5, 14}, {13, 14}}) { set(level, bb, wallTop, p[0], 1, p[1]); set(level, bb, s(Blocks.LANTERN), p[0], 2, p[1]); }
		chest(level, bb, 9, -2, 7, rewardLoot(), Direction.NORTH, 2);
		if (alt) guard(level, bb, EntityTypes.VINDICATOR, 9, -2, 10, List.of(Runes.ARC, Runes.TREMOR), false);
		else guard(level, bb, EntityTypes.HUSK, 9, -2, 10, List.of(Runes.NOVA, Runes.TREMOR), false);
		// Practice around the ring.
		var post = alt ? s(Blocks.IRON_BARS) : s(Blocks.OAK_FENCE);
		dummy(level, bb, 3, 1, 4, post); dummy(level, bb, 15, 1, 4, post); dummy(level, bb, 3, 1, 15, post);
		target(level, bb, 15, 1, 15, post);
		set(level, bb, alt ? s(Blocks.ANVIL) : s(Blocks.STONECUTTER), 15, 1, 9);
		set(level, bb, alt ? s(Blocks.RAW_IRON_BLOCK) : s(Blocks.CHISELED_STONE_BRICKS), 3, 1, 9);
		set(level, bb, s(Blocks.BARREL), 14, 1, 2); set(level, bb, s(Blocks.BARREL), 4, 1, 16);
		lectern(level, bb, 6, 1, 3);
		chest(level, bb, 12, 1, 2, MasterSites.supplies(site()), Direction.SOUTH, 1);
		quarry(level, bb, rim, timber, wallTop);
	}

	/** The working quarry east of the hall, reached through a side door: a stepped pit, two derricks and stockpiles of cut stone. */
	private void quarry(WorldGenLevel level, BoundingBox bb, BlockState rim, BlockState timber, BlockState wallTop) {
		BlockState raw = alt ? s(Blocks.DEEPSLATE) : s(Blocks.STONE), cut = alt ? s(Blocks.POLISHED_DEEPSLATE) : s(Blocks.SMOOTH_STONE);
		BlockState cobble = alt ? s(Blocks.COBBLED_DEEPSLATE) : s(Blocks.COBBLESTONE);
		fill(level, bb, 17, 1, 9, 18, 2, 9, AIR);
		set(level, bb, rim, 18, 0, 9);
		// A trodden yard of gravel, rubble and bare stone.
		for (int x = 19; x <= 28; x++) for (int z = 0; z <= 18; z++) {
			int n = noise(x, z);
			set(level, bb, n < 30 ? s(Blocks.GRAVEL) : n < 55 ? cobble : n < 75 ? s(Blocks.COARSE_DIRT) : raw, x, 0, z);
		}
		// The pit, cut down in four benches, rainwater pooled at the bottom.
		for (int x = 20; x <= 27; x++) for (int z = 3; z <= 15; z++) {
			int d = 0;
			for (int k = 1; k <= 4; k++) if (x >= 19 + k && x <= 28 - k && z >= 2 + k && z <= 16 - k) d = k;
			if (d == 0) continue;
			fill(level, bb, x, -d - 1, z, x, -1, z, raw);
			fill(level, bb, x, -d, z, x, 0, z, AIR);
			int n = noise(x, -d, z);
			set(level, bb, d == 4 ? s(Blocks.WATER) : n < 25 ? s(Blocks.ANDESITE) : n < 40 ? s(Blocks.GRAVEL) : raw, x, -d, z);
		}
		// The great derrick: a mast at the back, its jib reaching out over the pit with a cut block on the chain.
		BlockState mast = timber.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y), jib = timber.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
		set(level, bb, cut, 26, 0, 17);
		fill(level, bb, 26, 1, 17, 26, 13, 17, mast);
		fill(level, bb, 26, 12, 8, 26, 12, 18, jib);
		set(level, bb, stair(alt ? Blocks.DARK_OAK_STAIRS : Blocks.SPRUCE_STAIRS, Direction.SOUTH, true), 26, 11, 16);
		set(level, bb, cobble, 26, 11, 18);
		fill(level, bb, 26, 3, 9, 26, 11, 9, s(Blocks.IRON_CHAIN));
		set(level, bb, cut, 26, 2, 9);
		hangingLantern(level, bb, 26, 11, 13, 1, false);
		// A lesser derrick by the gate, lifting from the first bench.
		set(level, bb, cut, 20, 0, 2);
		fill(level, bb, 20, 1, 2, 20, 9, 2, mast);
		fill(level, bb, 20, 9, 0, 20, 9, 7, jib);
		set(level, bb, stair(alt ? Blocks.DARK_OAK_STAIRS : Blocks.SPRUCE_STAIRS, Direction.NORTH, true), 20, 8, 3);
		fill(level, bb, 20, 3, 6, 20, 8, 6, s(Blocks.IRON_CHAIN));
		set(level, bb, raw, 20, 2, 6);
		// Stacks of dressed blocks waiting to be hauled, a cook fire and lantern posts at the pit's ends.
		fill(level, bb, 27, 1, 0, 28, 2, 1, cut);
		set(level, bb, AIR, 28, 2, 1);
		fill(level, bb, 21, 1, 17, 22, 1, 18, rim);
		set(level, bb, cut, 21, 2, 18);
		set(level, bb, s(Blocks.CAMPFIRE), 23, 1, 1);
		for (int x : new int[]{19, 28}) { fill(level, bb, x, 1, 9, x, 2, 9, wallTop); set(level, bb, s(Blocks.LANTERN), x, 3, 9); }
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(); }
}
