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
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/** A stepped, root-wrapped jungle temple of the Verdant or Venom school; one Runebound bogged spars in the pit at its heart. */
public final class RootTemplePiece extends MasterSitePiece {
	public RootTemplePiece(int x, int y, int z, Direction facing, boolean alt) { super(MasterSites.ROOT_TEMPLE, x, y, z, 17, 19, 19, facing, alt); }
	public RootTemplePiece(CompoundTag tag) { super(MasterSites.ROOT_TEMPLE, tag); }
	@Override public String site() { return "master_root_temple"; }
	@Override public List<String> schools() { return List.of("verdant", "venom"); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		BlockState wall = alt ? s(Blocks.MUD_BRICKS) : s(Blocks.MOSSY_STONE_BRICKS), light = alt ? s(Blocks.OCHRE_FROGLIGHT) : s(Blocks.VERDANT_FROGLIGHT);
		BlockState leaves = (alt ? s(Blocks.DARK_OAK_LEAVES) : s(Blocks.FLOWERING_AZALEA_LEAVES)).setValue(LeavesBlock.PERSISTENT, true);
		BlockState chiseled = alt ? s(Blocks.CHISELED_RESIN_BRICKS) : s(Blocks.CHISELED_STONE_BRICKS);
		BlockState rail = alt ? s(Blocks.MUD_BRICK_WALL) : s(Blocks.MOSSY_STONE_BRICK_WALL);
		Palette masonry = alt ? Palette.of(Blocks.MUD_BRICKS, 7, Blocks.PACKED_MUD, 2, Blocks.MOSSY_COBBLESTONE, 1)
			: Palette.of(Blocks.MOSSY_STONE_BRICKS, 6, Blocks.STONE_BRICKS, 3, Blocks.CRACKED_STONE_BRICKS, 2, Blocks.MOSSY_COBBLESTONE, 1);
		Block trim = alt ? Blocks.MUD_BRICK_STAIRS : Blocks.STONE_BRICK_STAIRS;
		Block roofStairs = alt ? Blocks.MUD_BRICK_STAIRS : Blocks.MOSSY_STONE_BRICK_STAIRS;
		pad(level, bb, 0, 0, 16, 18, 18, s(Blocks.MOSSY_COBBLESTONE), s(Blocks.DIRT));
		for (int x = 0; x <= 16; x++) for (int z = 0; z <= 18; z++) if (noise(x, z) < 35) set(level, bb, s(Blocks.MOSS_BLOCK), x, 0, z);
		// A paved way up to the door.
		fill(level, bb, 7, 0, 0, 9, 0, 1, chiseled); fill(level, bb, 8, 0, 0, 8, 0, 1, s(Blocks.MOSSY_COBBLESTONE));
		// The hall.
		room(level, bb, 1, 0, 2, 15, 6, 16, wall);
		walls(level, bb, 1, 1, 2, 15, 6, 16, masonry);
		fill(level, bb, 1, 0, 2, 15, 0, 16, s(Blocks.MOSSY_COBBLESTONE));
		// The stepped pyramid over it: three tiers climbing to a roofed shrine.
		fill(level, bb, 2, 7, 3, 14, 7, 15, masonry);
		fill(level, bb, 4, 8, 5, 12, 9, 13, masonry);
		fill(level, bb, 5, 10, 6, 11, 11, 12, masonry);
		// Battered steps round the foot of each tier, corners solid.
		stepRing(level, bb, 1, 2, 15, 16, 7, trim, chiseled);
		stepRing(level, bb, 3, 4, 13, 14, 8, trim, chiseled);
		stepRing(level, bb, 4, 5, 12, 13, 10, trim, chiseled);
		for (int x = 7; x <= 9; x++) set(level, bb, stair(trim, Direction.NORTH, false), x, 7, 2);
		// The ceremonial stair up the front tiers, with railings.
		for (int i = 0; i < 4; i++) {
			int y = 8 + i, z = 4 + i;
			for (int x = 7; x <= 9; x++) set(level, bb, stair(trim, Direction.NORTH, false), x, y, z);
			set(level, bb, rail, 6, y + 1, z); set(level, bb, rail, 10, y + 1, z);
		}
		// The shrine: carved walls, a doorway facing the stair, a hipped roof crowned with light.
		walls(level, bb, 6, 12, 9, 10, 14, 11, masonry);
		for (int[] c : new int[][]{{6, 9}, {10, 9}, {6, 11}, {10, 11}}) fill(level, bb, c[0], 12, c[1], c[0], 14, c[1], chiseled);
		fill(level, bb, 7, 12, 10, 9, 14, 10, AIR);
		fill(level, bb, 8, 12, 9, 8, 13, 9, AIR);
		set(level, bb, stair(trim, Direction.NORTH, true), 8, 14, 9);
		fill(level, bb, 6, 15, 9, 10, 15, 11, wall);
		hipRoof(level, bb, 6, 9, 10, 11, 15, roofStairs, wall, alt ? s(Blocks.MUD_BRICK_SLAB) : s(Blocks.MOSSY_STONE_BRICK_SLAB));
		set(level, bb, light, 8, 15, 10); set(level, bb, light, 8, 18, 10);
		set(level, bb, s(alt ? Blocks.POTTED_FERN : Blocks.POTTED_FLOWERING_AZALEA), 8, 12, 10);
		set(level, bb, s(Blocks.MOSSY_COBBLESTONE), 7, 12, 10); set(level, bb, s(Blocks.MOSSY_COBBLESTONE), 9, 12, 10);
		// Overgrowth on the terraces, and lanterns at the tier corners.
		scatter(level, bb, 2, 8, 3, 5, 8, 15, leaves, 22); scatter(level, bb, 11, 8, 3, 14, 8, 15, leaves, 22);
		scatter(level, bb, 6, 8, 14, 10, 8, 15, leaves, 22);
		scatter(level, bb, 4, 10, 5, 5, 10, 13, leaves, 18); scatter(level, bb, 11, 10, 5, 12, 10, 13, leaves, 18);
		for (int[] c : new int[][]{{2, 3}, {14, 3}, {2, 15}, {14, 15}}) set(level, bb, s(Blocks.LANTERN), c[0], 8, c[1]);
		for (int[] c : new int[][]{{5, 6}, {11, 6}, {5, 12}, {11, 12}}) set(level, bb, s(Blocks.LANTERN), c[0], 12, c[1]);
		// Corner piers, a corbelled cornice and a plinth course outside the hall.
		for (int[] c : new int[][]{{0, 1}, {16, 1}, {0, 17}, {16, 17}}) pillar(level, bb, c[0], c[1], 1, 6, chiseled, wall, chiseled);
		for (int z = 2; z <= 16; z++) {
			set(level, bb, stair(trim, Direction.EAST, true), 0, 6, z);
			set(level, bb, stair(trim, Direction.WEST, true), 16, 6, z);
		}
		for (int x = 1; x <= 15; x++) {
			set(level, bb, stair(trim, Direction.NORTH, true), x, 6, 1);
			set(level, bb, stair(trim, Direction.SOUTH, true), x, 6, 17);
			if (x < 6 || x > 10) set(level, bb, stair(trim, Direction.NORTH, false), x, 1, 1);
		}
		// The portal: carved jambs, a lintel and a lantern over the door.
		fill(level, bb, 7, 1, 2, 9, 3, 2, AIR);
		for (int x : new int[]{6, 10}) pillar(level, bb, x, 1, 1, 5, chiseled, wall, chiseled);
		for (int x = 7; x <= 9; x++) set(level, bb, stair(trim, Direction.NORTH, true), x, 4, 1);
		fill(level, bb, 7, 5, 1, 9, 5, 1, chiseled);
		set(level, bb, light, 8, 5, 1);
		set(level, bb, chiseled, 8, 4, 2);
		// Roots drape the outer walls.
		var roots = s(Blocks.MANGROVE_ROOTS);
		for (int z = 3; z <= 15; z++) { int a = noise(0, z) % 5, b = noise(16, z) % 5; if (a > 0) fill(level, bb, 0, 1, z, 0, a, z, roots); if (b > 0) fill(level, bb, 16, 1, z, 16, b, z, roots); }
		for (int x = 2; x <= 14; x++) { int c = noise(x, 17) % 4; if (c > 0) fill(level, bb, x, 1, 17, x, c, 17, roots); }
		// Inside: pilasters, lights in the ceiling, lanterns and the school's colours.
		for (int z : new int[]{8, 11}) {
			pillar(level, bb, 2, z, 1, 5, chiseled, wall, chiseled);
			pillar(level, bb, 14, z, 1, 5, chiseled, wall, chiseled);
		}
		for (int[] p : new int[][]{{4, 5}, {12, 5}, {4, 14}, {12, 14}}) set(level, bb, light, p[0], 6, p[1]);
		hangingLantern(level, bb, 8, 5, 4, 1, false);
		Block banner = Blocks.WALL_BANNER.pick(alt ? DyeColor.LIME : DyeColor.GREEN);
		banner(level, bb, banner, 5, 4, 15, Direction.SOUTH); banner(level, bb, banner, 11, 4, 15, Direction.SOUTH);
		// The sparring pit.
		for (int x = 2; x <= 14; x++) for (int z = 4; z <= 16; z++) {
			double d = dist(x, z, 8, 10);
			if (d <= 5.5) fill(level, bb, x, -3, z, x, -1, z, wall);
			if (d <= 3.5) { fill(level, bb, x, -2, z, x, 0, z, AIR); set(level, bb, s(noise(x, z) < 50 ? Blocks.ROOTED_DIRT : Blocks.MOSS_BLOCK), x, -3, z); }
			else if (d <= 4.5) set(level, bb, s(Blocks.MOSSY_STONE_BRICKS), x, 0, z);
		}
		set(level, bb, light, 4, -2, 10); set(level, bb, light, 12, -2, 10);
		fill(level, bb, 8, -2, 13, 8, 0, 13, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		chest(level, bb, 8, -2, 7, rewardLoot(), Direction.NORTH, 2);
		guard(level, bb, EntityTypes.BOGGED, 8, -2, 10, alt ? List.of(Runes.ARC, Runes.VENOM) : List.of(Runes.BOLT, Runes.ROOTSNARE), false);
		// Practice, the lectern and the garden corners.
		var post = s(Blocks.JUNGLE_FENCE);
		dummy(level, bb, 3, 1, 5, post); dummy(level, bb, 13, 1, 5, post);
		target(level, bb, 3, 1, 14, post); target(level, bb, 13, 1, 14, post);
		var plant = alt ? s(Blocks.FERN) : s(Blocks.FLOWERING_AZALEA);
		for (int[] p : new int[][]{{2, 3}, {14, 15}, {2, 15}}) { set(level, bb, s(Blocks.MOSS_BLOCK), p[0], 0, p[1]); set(level, bb, plant, p[0], 1, p[1]); }
		lectern(level, bb, 5, 1, 4);
		chest(level, bb, 11, 1, 3, MasterSites.supplies(site()), Direction.SOUTH, 1);
	}

	/** A course of stairs round a tier's foot at {@code y}, each climbing toward the tier; corners solid, the front stair's run left clear. */
	private void stepRing(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1, int y, Block stairs, BlockState corner) {
		for (int x = x0 + 1; x < x1; x++) {
			if (x < 7 || x > 9) set(level, bb, stair(stairs, Direction.NORTH, false), x, y, z0);
			set(level, bb, stair(stairs, Direction.SOUTH, false), x, y, z1);
		}
		for (int z = z0 + 1; z < z1; z++) {
			set(level, bb, stair(stairs, Direction.EAST, false), x0, y, z);
			set(level, bb, stair(stairs, Direction.WEST, false), x1, y, z);
		}
		for (int[] c : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) set(level, bb, corner, c[0], y, c[1]);
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(); }
}
