package dev.wildercord.world.sites.masters;

import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/** A badlands forge dojo of the Ember or Crimson school: a brick hall, a smoking chimney and a sealed vault. */
public final class ForgeDojoPiece extends MasterSitePiece {
	public ForgeDojoPiece(int x, int y, int z, Direction facing, boolean alt) { super(MasterSites.FORGE_DOJO, x, y, z, 17, 18, 19, facing, alt); }
	public ForgeDojoPiece(CompoundTag tag) { super(MasterSites.FORGE_DOJO, tag); }
	@Override public String site() { return "master_forge_dojo"; }
	@Override public List<String> schools() { return List.of("ember", "crimson"); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);
		BlockState wall = alt ? s(Blocks.RED_NETHER_BRICKS) : s(Blocks.BRICKS), dark = s(Blocks.POLISHED_BLACKSTONE_BRICKS);
		BlockState chiseled = s(Blocks.CHISELED_POLISHED_BLACKSTONE);
		Palette brick = alt ? Palette.of(Blocks.RED_NETHER_BRICKS, 8, Blocks.NETHER_BRICKS, 2, Blocks.CRACKED_NETHER_BRICKS, 1)
			: Palette.of(Blocks.BRICKS, 9, Blocks.DYED_TERRACOTTA.red(), 2, Blocks.GRANITE, 1);
		Palette stone = Palette.of(Blocks.POLISHED_BLACKSTONE_BRICKS, 7, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, 2, Blocks.BLACKSTONE, 1);
		Block trimStairs = Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS;
		Block roofStairs = alt ? Blocks.NETHER_BRICK_STAIRS : Blocks.DEEPSLATE_TILE_STAIRS;
		BlockState roofUnder = alt ? s(Blocks.NETHER_BRICKS) : s(Blocks.DEEPSLATE_TILES);
		BlockState roofCap = alt ? s(Blocks.NETHER_BRICK_SLAB) : s(Blocks.DEEPSLATE_TILE_SLAB);
		pad(level, bb, 0, 0, 16, 18, 17, s(Blocks.POLISHED_BLACKSTONE), s(Blocks.BLACKSTONE));
		// The hall: brick walls over a dark blackstone dado, a patterned floor.
		room(level, bb, 1, 0, 2, 15, 7, 12, wall);
		walls(level, bb, 1, 1, 2, 15, 7, 12, brick);
		walls(level, bb, 1, 1, 2, 15, 1, 12, stone);
		fill(level, bb, 1, 0, 2, 15, 0, 12, s(Blocks.POLISHED_BLACKSTONE));
		for (int x = 2; x <= 14; x++) for (int z = 3; z <= 11; z++) if (x == 2 || x == 14 || z == 3 || z == 11) set(level, bb, dark, x, 0, z);
		fill(level, bb, 7, 1, 2, 9, 3, 2, AIR);
		fill(level, bb, 6, 4, 2, 10, 4, 2, dark);
		for (int z : new int[]{5, 8}) { fill(level, bb, 1, 3, z, 1, 4, z, s(Blocks.IRON_BARS)); fill(level, bb, 15, 3, z, 15, 4, z, s(Blocks.IRON_BARS)); }
		set(level, bb, s(Blocks.SHROOMLIGHT), 5, 7, 7); set(level, bb, s(Blocks.SHROOMLIGHT), 11, 7, 7);
		fill(level, bb, 8, 1, 3, 8, 1, 11, alt ? s(Blocks.CARPET.red()) : s(Blocks.CARPET.orange()));
		// Battered plinth stairs around the walls, sills under the windows and a string course that hoods them.
		for (int z = 3; z <= 11; z++) {
			set(level, bb, stair(trimStairs, Direction.EAST, false), 0, 1, z);
			set(level, bb, stair(trimStairs, Direction.WEST, false), 16, 1, z);
			set(level, bb, stair(trimStairs, Direction.EAST, true), 0, 5, z);
			set(level, bb, stair(trimStairs, Direction.WEST, true), 16, 5, z);
		}
		for (int z : new int[]{5, 8}) {
			set(level, bb, stair(trimStairs, Direction.EAST, true), 0, 2, z);
			set(level, bb, stair(trimStairs, Direction.WEST, true), 16, 2, z);
		}
		for (int x = 2; x <= 14; x++) {
			if (x < 6 || x > 10) set(level, bb, stair(trimStairs, Direction.NORTH, false), x, 1, 1);
			set(level, bb, stair(trimStairs, Direction.NORTH, true), x, 5, 1);
		}
		for (int x : new int[]{2, 3, 13, 14}) set(level, bb, stair(trimStairs, Direction.SOUTH, false), x, 1, 13);
		// A recessed porch: two chiseled piers, a hanging lantern and the school's colours either side.
		for (int x : new int[]{6, 10}) pillar(level, bb, x, 1, 1, 4, chiseled, dark, chiseled);
		hangingLantern(level, bb, 8, 4, 1, 0, false);
		Block banner = alt ? Blocks.WALL_BANNER.red() : Blocks.WALL_BANNER.orange();
		banner(level, bb, banner, 5, 4, 1, Direction.SOUTH); banner(level, bb, banner, 11, 4, 1, Direction.SOUTH);
		fill(level, bb, 7, 0, 0, 9, 0, 1, dark);
		// Interior: smoke-dark beams with lanterns, banners flanking the vault door.
		BlockState beam = s(Blocks.STRIPPED_DARK_OAK_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
		for (int z : new int[]{5, 9}) fill(level, bb, 2, 6, z, 14, 6, z, beam);
		for (int[] l : new int[][]{{4, 5}, {8, 5}, {12, 5}, {4, 9}, {8, 9}}) hangingLantern(level, bb, l[0], 5, l[1], 0, false);
		banner(level, bb, banner, 6, 4, 11, Direction.SOUTH); banner(level, bb, banner, 10, 4, 11, Direction.SOUTH);
		set(level, bb, s(Blocks.BARREL), 2, 1, 4); set(level, bb, s(Blocks.BARREL), 14, 1, 4);
		set(level, bb, s(Blocks.DECORATED_POT), 2, 1, 10);
		// The roof: a corbelled cornice under a deep hipped roof of dark tile, a finial on the ridge.
		for (int z = 2; z <= 12; z++) {
			set(level, bb, stair(trimStairs, Direction.EAST, true), 0, 7, z);
			set(level, bb, stair(trimStairs, Direction.WEST, true), 16, 7, z);
		}
		for (int x = 1; x <= 15; x++) {
			set(level, bb, stair(trimStairs, Direction.NORTH, true), x, 7, 1);
			set(level, bb, stair(trimStairs, Direction.SOUTH, true), x, 7, 13);
		}
		hipRoof(level, bb, 1, 2, 15, 12, 8, roofStairs, roofUnder, roofCap);
		for (int x : new int[]{6, 10}) set(level, bb, s(Blocks.POLISHED_BLACKSTONE_BRICK_WALL), x, 15, 7);
		set(level, bb, s(Blocks.POLISHED_BLACKSTONE_BRICK_WALL), 8, 15, 7);
		set(level, bb, s(Blocks.LANTERN), 8, 16, 7);
		// Corner towers rising through the eaves, each capped with a lantern.
		for (int[] c : new int[][]{{0, 1}, {15, 1}, {0, 12}, {15, 12}}) {
			fill(level, bb, c[0], 1, c[1], c[0] + 1, 9, c[1] + 1, stone);
			fill(level, bb, c[0], 10, c[1], c[0] + 1, 10, c[1] + 1, s(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB));
			int ox = c[0] == 0 ? 0 : 16, oz = c[1] == 1 ? 1 : 13;
			set(level, bb, chiseled, ox, 10, oz);
			set(level, bb, s(Blocks.LANTERN), ox, 11, oz);
		}
		// The forge chimney: a lit hearth behind bars, its smoke rising through a hollow stack above the roof.
		fill(level, bb, 12, 1, 9, 14, 16, 11, stone);
		fill(level, bb, 13, 2, 10, 13, 16, 10, AIR);
		for (int i = 9; i <= 11; i++) {
			set(level, bb, stair(trimStairs, Direction.EAST, true), 11, 16, i);
			set(level, bb, stair(trimStairs, Direction.WEST, true), 15, 16, i);
		}
		for (int i = 12; i <= 14; i++) {
			set(level, bb, stair(trimStairs, Direction.NORTH, true), i, 16, 8);
			set(level, bb, stair(trimStairs, Direction.SOUTH, true), i, 16, 12);
		}
		for (int[] c : new int[][]{{12, 9}, {14, 9}, {12, 11}, {14, 11}}) set(level, bb, s(Blocks.POLISHED_BLACKSTONE_BRICK_WALL), c[0], 17, c[1]);
		set(level, bb, s(Blocks.MAGMA_BLOCK), 13, 0, 10);
		set(level, bb, s(Blocks.CAMPFIRE), 13, 1, 10);
		fill(level, bb, 13, 1, 9, 13, 2, 9, s(Blocks.IRON_BARS));
		set(level, bb, s(Blocks.ANVIL), 11, 1, 10);
		set(level, bb, s(Blocks.SMITHING_TABLE), 2, 1, 3);
		// A red gate before the porch: two posts, a tie beam and an upswept dark lintel.
		BlockState post = s(Blocks.STRIPPED_MANGROVE_LOG);
		for (int x : new int[]{6, 10}) fill(level, bb, x, 1, 0, x, 4, 0, post);
		fill(level, bb, 5, 3, 0, 11, 3, 0, topSlab(Blocks.MANGROVE_SLAB));
		for (int x : new int[]{6, 10}) set(level, bb, post, x, 3, 0);
		fill(level, bb, 5, 5, 0, 11, 5, 0, s(Blocks.STRIPPED_MANGROVE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		fill(level, bb, 5, 6, 0, 11, 6, 0, s(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB));
		set(level, bb, stair(trimStairs, Direction.WEST, false), 4, 6, 0);
		set(level, bb, stair(trimStairs, Direction.EAST, false), 12, 6, 0);
		for (int x : new int[]{2, 14}) { set(level, bb, s(Blocks.POLISHED_BLACKSTONE_WALL), x, 1, 0); set(level, bb, s(Blocks.LANTERN), x, 2, 0); }
		// Practice: four dummies and two targets down the hall.
		var dummyPost = s(Blocks.DARK_OAK_FENCE);
		dummy(level, bb, 4, 1, 5, dummyPost); dummy(level, bb, 4, 1, 8, dummyPost); dummy(level, bb, 12, 1, 5, dummyPost);
		target(level, bb, 3, 1, 11, dummyPost); target(level, bb, 5, 1, 11, dummyPost);
		lectern(level, bb, 6, 1, 4);
		chest(level, bb, 14, 1, 3, MasterSites.supplies(site()), Direction.WEST, 1);
		// The vault behind the back wall, sealed with the school's element, under its own hipped roof.
		room(level, bb, 5, 0, 12, 11, 5, 17, dark);
		fill(level, bb, 5, 1, 13, 5, 4, 17, stone); fill(level, bb, 11, 1, 13, 11, 4, 17, stone); fill(level, bb, 6, 1, 17, 10, 4, 17, stone);
		fill(level, bb, 6, 0, 13, 10, 0, 16, s(Blocks.POLISHED_BLACKSTONE));
		for (int z = 13; z <= 17; z++) {
			set(level, bb, stair(trimStairs, Direction.EAST, false), 4, 1, z);
			set(level, bb, stair(trimStairs, Direction.WEST, false), 12, 1, z);
		}
		for (int x = 5; x <= 11; x++) set(level, bb, stair(trimStairs, Direction.SOUTH, false), x, 1, 18);
		set(level, bb, dark, 4, 1, 18); set(level, bb, dark, 12, 1, 18);
		hipRoof(level, bb, 5, 14, 11, 17, 6, roofStairs, roofUnder, roofCap);
		var e = element(school());
		sealDoorAcross(level, bb, 12, 7, 9, 1, 3, e, e);
		set(level, bb, s(Blocks.LANTERN), 6, 1, 16); set(level, bb, s(Blocks.LANTERN), 10, 1, 16);
		set(level, bb, alt ? s(Blocks.CRYING_OBSIDIAN) : s(Blocks.GILDED_BLACKSTONE), 8, 0, 16);
		chest(level, bb, 8, 1, 15, rewardLoot(), Direction.SOUTH, 2);
		// The yard behind: fuel and quench barrels by the chimney.
		set(level, bb, s(Blocks.BARREL), 14, 1, 15); set(level, bb, s(Blocks.COAL_BLOCK), 15, 1, 15);
		set(level, bb, s(Blocks.WATER_CAULDRON), 14, 1, 16);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(5, 0, 12, 11, 5, 17)); }
}
