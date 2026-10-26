package dev.wildercord.world.sites.masters;

import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/**
 * A raised observatory terrace of the Starlit or Dawn school: a glowing star map in its floor, a copper telescope, and a domed
 * reading room sealed with arcane light.
 */
public final class StarTerracePiece extends MasterSitePiece {
	public StarTerracePiece(int x, int y, int z, Direction facing, boolean alt) { super(MasterSites.STAR_TERRACE, x, y, z, 17, 19, 18, facing, alt); }
	public StarTerracePiece(CompoundTag tag) { super(MasterSites.STAR_TERRACE, tag); }
	@Override public String site() { return "master_star_terrace"; }
	@Override public List<String> schools() { return List.of("starlit", "dawn"); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		DungeonWards.remember(level, this);
		BlockState base = alt ? s(Blocks.CALCITE) : s(Blocks.POLISHED_DEEPSLATE), wall = alt ? s(Blocks.QUARTZ_BRICKS) : s(Blocks.DEEPSLATE_TILES);
		BlockState glass = alt ? s(Blocks.STAINED_GLASS.orange()) : s(Blocks.STAINED_GLASS.purple()), glow = alt ? s(Blocks.GLOWSTONE) : s(Blocks.SEA_LANTERN);
		BlockState chiseled = alt ? s(Blocks.CHISELED_QUARTZ_BLOCK) : s(Blocks.CHISELED_DEEPSLATE);
		BlockState rail = alt ? s(Blocks.QUARTZ_SLAB) : s(Blocks.POLISHED_DEEPSLATE_SLAB);
		BlockState column = alt ? s(Blocks.QUARTZ_PILLAR) : s(Blocks.POLISHED_DEEPSLATE);
		Palette masonry = alt ? Palette.of(Blocks.CALCITE, 6, Blocks.POLISHED_DIORITE, 2, Blocks.SMOOTH_QUARTZ, 1)
			: Palette.of(Blocks.POLISHED_DEEPSLATE, 5, Blocks.DEEPSLATE_BRICKS, 3, Blocks.CRACKED_DEEPSLATE_BRICKS, 1);
		Block trim = alt ? Blocks.QUARTZ_STAIRS : Blocks.POLISHED_DEEPSLATE_STAIRS;
		Block roofStairs = alt ? Blocks.QUARTZ_STAIRS : Blocks.DEEPSLATE_TILE_STAIRS;
		BlockState band = alt ? s(Blocks.SMOOTH_RED_SANDSTONE) : s(Blocks.CALCITE);
		pad(level, bb, 0, 0, 16, 17, 14, s(Blocks.GRASS_BLOCK), s(Blocks.DIRT));
		var flower = alt ? s(Blocks.DANDELION) : s(Blocks.CORNFLOWER);
		for (int x = 0; x <= 16; x++) for (int z = 0; z <= 2; z++) if ((x < 6 || x > 10) && noise(x, z) < 40) set(level, bb, flower, x, 1, z);
		// The terrace, its steps and its star map: glass rays lit from beneath.
		fill(level, bb, 1, 0, 3, 15, 3, 16, base);
		walls(level, bb, 1, 0, 3, 15, 3, 16, masonry);
		var stair = s(Blocks.STONE_BRICK_STAIRS).setValue(StairBlock.FACING, Direction.NORTH);
		for (int x = 7; x <= 9; x++) {
			set(level, bb, stair, x, 1, 0);
			set(level, bb, base, x, 1, 1); set(level, bb, stair, x, 2, 1);
			fill(level, bb, x, 1, 2, x, 2, 2, base); set(level, bb, stair, x, 3, 2);
		}
		// Cheek walls beside the steps, lanterns on their heads.
		for (int x : new int[]{6, 10}) {
			set(level, bb, chiseled, x, 1, 0); fill(level, bb, x, 1, 1, x, 2, 1, wall); fill(level, bb, x, 1, 2, x, 3, 2, wall);
			set(level, bb, rail, x, 2, 0); set(level, bb, rail, x, 3, 1); set(level, bb, s(Blocks.LANTERN), x, 4, 2);
		}
		// A plinth course and an overhanging cornice round the terrace, piers between the bays.
		for (int z = 3; z <= 16; z++) {
			boolean pier = z == 3 || z == 7 || z == 12 || z == 16;
			if (pier) { fill(level, bb, 0, 1, z, 0, 3, z, chiseled); fill(level, bb, 16, 1, z, 16, 3, z, chiseled); continue; }
			set(level, bb, stair(trim, Direction.EAST, false), 0, 1, z); set(level, bb, stair(trim, Direction.EAST, true), 0, 3, z);
			set(level, bb, stair(trim, Direction.WEST, false), 16, 1, z); set(level, bb, stair(trim, Direction.WEST, true), 16, 3, z);
		}
		for (int x = 1; x <= 15; x++) {
			set(level, bb, stair(trim, Direction.SOUTH, false), x, 1, 17); set(level, bb, stair(trim, Direction.SOUTH, true), x, 3, 17);
			if (x < 6 || x > 10) { set(level, bb, stair(trim, Direction.NORTH, false), x, 1, 2); set(level, bb, stair(trim, Direction.NORTH, true), x, 3, 2); }
		}
		for (int[] c : new int[][]{{0, 2}, {16, 2}, {0, 17}, {16, 17}}) fill(level, bb, c[0], 1, c[1], c[0], 3, c[1], chiseled);
		// A pale band between plinth and cornice, so the terrace reads light against the grass.
		for (int z = 3; z <= 16; z++) if (z != 3 && z != 7 && z != 12 && z != 16) { set(level, bb, band, 0, 2, z); set(level, bb, band, 16, 2, z); }
		for (int x = 1; x <= 15; x++) { set(level, bb, band, x, 2, 17); if (x < 6 || x > 10) set(level, bb, band, x, 2, 2); }
		// The star map, ringed in carved stone.
		for (int x = 2; x <= 14; x++) for (int z = 4; z <= 11; z++) {
			double d = dist(x, z, 8, 8);
			if (d >= 2 && d <= 6 && onSpoke(x, z, 8, 8, 8, 0.5)) { set(level, bb, glass, x, 3, z); set(level, bb, glow, x, 2, z); }
			else if (d > 6.4 && d <= 7.2) set(level, bb, wall, x, 3, z);
		}
		set(level, bb, chiseled, 8, 3, 8);
		// A low balustrade round the deck: slab rails between carved posts.
		for (int z = 4; z <= 15; z++) {
			BlockState b = z % 3 == 0 ? chiseled : rail;
			set(level, bb, b, 1, 4, z); set(level, bb, b, 15, 4, z);
		}
		for (int x = 2; x <= 14; x++) if (x < 7 || x > 9) set(level, bb, x % 3 == 0 ? chiseled : rail, x, 4, 3);
		for (int x : new int[]{2, 3, 13, 14}) set(level, bb, rail, x, 4, 16);
		// Obelisks at the corners, each carrying a light.
		for (int[] p : new int[][]{{1, 3}, {15, 3}, {1, 16}, {15, 16}}) {
			pillar(level, bb, p[0], p[1], 4, 8, chiseled, column, chiseled);
			set(level, bb, alt ? s(Blocks.LANTERN) : s(Blocks.END_ROD), p[0], 9, p[1]);
		}
		// The telescope, on a stepped mount.
		var copper = s(Blocks.COPPER_BLOCK.waxed().unaffected());
		fill(level, bb, 8, 4, 8, 8, 6, 8, wall);
		set(level, bb, stair(trim, Direction.EAST, false), 7, 4, 8); set(level, bb, stair(trim, Direction.WEST, false), 9, 4, 8);
		set(level, bb, stair(trim, Direction.NORTH, false), 8, 4, 7); set(level, bb, stair(trim, Direction.SOUTH, false), 8, 4, 9);
		set(level, bb, copper, 8, 7, 8); set(level, bb, copper, 8, 8, 9); set(level, bb, copper, 8, 9, 10);
		// The reading room, sealed with arcane light.
		room(level, bb, 4, 3, 12, 12, 8, 16, wall);
		fill(level, bb, 6, 8, 13, 10, 8, 15, glass);
		set(level, bb, glow, 8, 8, 14);
		fill(level, bb, 5, 4, 13, 5, 5, 15, s(Blocks.BOOKSHELF)); fill(level, bb, 11, 4, 13, 11, 5, 15, s(Blocks.BOOKSHELF));
		set(level, bb, s(Blocks.LANTERN), 5, 6, 14); set(level, bb, s(Blocks.LANTERN), 11, 6, 14);
		fill(level, bb, 6, 4, 13, 10, 4, 14, Blocks.CARPET.pick(alt ? DyeColor.ORANGE : DyeColor.PURPLE).defaultBlockState());
		// A corbelled cornice, a stepped roof and a glass cupola over the reading room.
		for (int z = 12; z <= 16; z++) { set(level, bb, stair(trim, Direction.EAST, true), 3, 8, z); set(level, bb, stair(trim, Direction.WEST, true), 13, 8, z); }
		for (int x = 4; x <= 12; x++) { set(level, bb, stair(trim, Direction.NORTH, true), x, 8, 11); set(level, bb, stair(trim, Direction.SOUTH, true), x, 8, 17); }
		for (int[] c : new int[][]{{3, 11}, {13, 11}, {3, 17}, {13, 17}}) set(level, bb, chiseled, c[0], 8, c[1]);
		stairRing(level, bb, 4, 12, 12, 16, 9, roofStairs, chiseled);
		fill(level, bb, 5, 9, 13, 5, 9, 15, wall); fill(level, bb, 11, 9, 13, 11, 9, 15, wall);
		walls(level, bb, 6, 9, 13, 10, 10, 15, Palette.of(glass, 1));
		for (int[] c : new int[][]{{6, 13}, {10, 13}, {6, 15}, {10, 15}}) fill(level, bb, c[0], 9, c[1], c[0], 10, c[1], column);
		fill(level, bb, 7, 9, 14, 9, 10, 14, AIR);
		stairRing(level, bb, 6, 13, 10, 15, 11, roofStairs, chiseled);
		fill(level, bb, 7, 11, 14, 9, 11, 14, wall);
		set(level, bb, glow, 8, 11, 14);
		// A lit belvedere on the cupola: four columns under a hipped cap, an armillary of copper in the middle.
		for (int[] c : new int[][]{{7, 13}, {9, 13}, {7, 15}, {9, 15}}) fill(level, bb, c[0], 12, c[1], c[0], 13, c[1], column);
		set(level, bb, copper, 8, 12, 14);
		set(level, bb, s(Blocks.LIGHTNING_ROD.waxed().unaffected()), 8, 13, 14);
		hipRoof(level, bb, 7, 13, 9, 15, 14, roofStairs, wall, chiseled);
		set(level, bb, alt ? s(Blocks.LANTERN) : s(Blocks.END_ROD), 8, 17, 14);
		// Light through the reading room's flanks and back, so the building reads at dusk from the ground.
		for (int z = 13; z <= 15; z++) { fill(level, bb, 4, 6, z, 4, 7, z, glass); fill(level, bb, 12, 6, z, 12, 7, z, glass); }
		for (int x = 6; x <= 10; x += 2) fill(level, bb, x, 5, 16, x, 7, 16, glass);
		for (int z : new int[]{13, 15}) { set(level, bb, stair(trim, Direction.WEST, true), 3, 5, z); set(level, bb, stair(trim, Direction.EAST, true), 13, 5, z); }
		set(level, bb, rail, 3, 5, 14); set(level, bb, rail, 13, 5, 14);
		// The portico before the sealed door: columns, a lintel, a hung lantern and the school's colours.
		for (int x : new int[]{4, 6, 10, 12}) pillar(level, bb, x, 11, 4, 7, chiseled, column, chiseled);
		for (int x = 7; x <= 9; x++) set(level, bb, stair(trim, Direction.NORTH, true), x, 7, 11);
		hangingLantern(level, bb, 8, 6, 11, 0, false);
		Block banner = Blocks.WALL_BANNER.pick(alt ? DyeColor.ORANGE : DyeColor.BLUE);
		banner(level, bb, banner, 5, 6, 11, Direction.SOUTH); banner(level, bb, banner, 11, 6, 11, Direction.SOUTH);
		var e = element(school());
		sealDoorAcross(level, bb, 12, 7, 9, 4, 6, e, e);
		chest(level, bb, 8, 4, 15, rewardLoot(), Direction.SOUTH, 2);
		// Practice and the lectern.
		var post = alt ? s(Blocks.CHERRY_FENCE) : s(Blocks.DARK_OAK_FENCE);
		dummy(level, bb, 3, 4, 5, post); dummy(level, bb, 13, 4, 5, post);
		target(level, bb, 3, 4, 10, post); target(level, bb, 13, 4, 10, post);
		lectern(level, bb, 6, 4, 5);
		chest(level, bb, 11, 4, 4, MasterSites.supplies(site()), Direction.SOUTH, 1);
	}

	/** A course of stairs round a rectangle at {@code y}, each rising toward its middle, corners solid. */
	private void stairRing(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1, int y, Block stairs, BlockState corner) {
		for (int x = x0 + 1; x < x1; x++) {
			set(level, bb, stair(stairs, Direction.NORTH, false), x, y, z0);
			set(level, bb, stair(stairs, Direction.SOUTH, false), x, y, z1);
		}
		for (int z = z0 + 1; z < z1; z++) {
			set(level, bb, stair(stairs, Direction.EAST, false), x0, y, z);
			set(level, bb, stair(stairs, Direction.WEST, false), x1, y, z);
		}
		for (int[] c : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) set(level, bb, corner, c[0], y, c[1]);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(4, 3, 12, 12, 8, 16)); }
}
