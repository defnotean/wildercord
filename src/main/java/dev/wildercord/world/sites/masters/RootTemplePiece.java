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

/**
 * A great stepped temple of the Verdant or Venom school: four battered tiers under a roof-combed shrine, climbed by twin
 * railed stairs that frame an obelisk court, with roots pouring down its flanks. In the hall inside, one Runebound bogged
 * spars in the pit.
 */
public final class RootTemplePiece extends MasterSitePiece {
	private static final int W = 27, H = 30, D = 33;
	/** Each tier's footprint (x0, z0, x1, z1) and its bottom and top courses. */
	private static final int[][] TIERS = {{1, 11, 25, 31, 1, 4}, {3, 13, 23, 29, 5, 7}, {5, 15, 21, 27, 8, 10}, {7, 17, 19, 25, 11, 13}};

	public RootTemplePiece(int x, int y, int z, Direction facing, boolean alt) { super(MasterSites.ROOT_TEMPLE, x, y, z, W, H, D, facing, alt); }
	public RootTemplePiece(CompoundTag tag) { super(MasterSites.ROOT_TEMPLE, tag); }
	@Override public String site() { return "master_root_temple"; }
	@Override public List<String> schools() { return List.of("verdant", "venom"); }

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		BlockState wall = alt ? s(Blocks.MUD_BRICKS) : s(Blocks.MOSSY_STONE_BRICKS), light = alt ? s(Blocks.OCHRE_FROGLIGHT) : s(Blocks.VERDANT_FROGLIGHT);
		BlockState leaves = (alt ? s(Blocks.DARK_OAK_LEAVES) : s(Blocks.FLOWERING_AZALEA_LEAVES)).setValue(LeavesBlock.PERSISTENT, true);
		BlockState chiseled = alt ? s(Blocks.CHISELED_RESIN_BRICKS) : s(Blocks.CHISELED_STONE_BRICKS);
		BlockState rail = alt ? s(Blocks.MUD_BRICK_WALL) : s(Blocks.MOSSY_STONE_BRICK_WALL);
		BlockState slab = alt ? s(Blocks.MUD_BRICK_SLAB) : s(Blocks.MOSSY_STONE_BRICK_SLAB);
		Palette masonry = alt ? Palette.of(Blocks.MUD_BRICKS, 7, Blocks.PACKED_MUD, 2, Blocks.MOSSY_COBBLESTONE, 1)
			: Palette.of(Blocks.MOSSY_STONE_BRICKS, 6, Blocks.STONE_BRICKS, 3, Blocks.CRACKED_STONE_BRICKS, 2, Blocks.MOSSY_COBBLESTONE, 1);
		Block trim = alt ? Blocks.MUD_BRICK_STAIRS : Blocks.STONE_BRICK_STAIRS;
		Block roofStairs = alt ? Blocks.MUD_BRICK_STAIRS : Blocks.MOSSY_STONE_BRICK_STAIRS;
		var roots = s(Blocks.MANGROVE_ROOTS);
		var plant = alt ? s(Blocks.FERN) : s(Blocks.FLOWERING_AZALEA);
		pad(level, bb, 0, 0, W - 1, D - 1, H - 1, s(Blocks.MOSSY_COBBLESTONE), s(Blocks.DIRT));
		for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) if (noise(x, z) < 35) set(level, bb, s(Blocks.MOSS_BLOCK), x, 0, z);

		// The court between the stairs: a paved way to the door, edged in carved stone, braziers at its mouth.
		fill(level, bb, 11, 0, 0, 15, 0, 10, chiseled); fill(level, bb, 12, 0, 0, 14, 0, 10, s(Blocks.MOSSY_COBBLESTONE));
		for (int z = 1; z <= 9; z += 2) set(level, bb, wall, 13, 0, z);
		for (int x : new int[]{11, 15}) { set(level, bb, rail, x, 1, 0); set(level, bb, light, x, 2, 0); }
		// Four obelisks out in the court's wings, each stepped at the foot and lit at the tip.
		for (int[] o : new int[][]{{3, 2}, {23, 2}, {3, 8}, {23, 8}}) {
			for (Direction d : Direction.Plane.HORIZONTAL) set(level, bb, stair(trim, d.getOpposite(), false), o[0] + d.getStepX(), 1, o[1] + d.getStepZ());
			pillar(level, bb, o[0], o[1], 1, 7, chiseled, wall, chiseled);
			set(level, bb, rail, o[0], 8, o[1]); set(level, bb, light, o[0], 9, o[1]);
		}

		// The four battered tiers.
		for (int k = 0; k < TIERS.length; k++) {
			int[] t = TIERS[k];
			fill(level, bb, t[0], t[4], t[1], t[2], t[5], t[3], masonry);
			// Quoins at the corners, and carved stones spaced along the top course.
			for (int[] c : new int[][]{{t[0], t[1]}, {t[2], t[1]}, {t[0], t[3]}, {t[2], t[3]}}) fill(level, bb, c[0], t[4], c[1], c[0], t[5], c[1], chiseled);
			for (int x = t[0] + 2; x < t[2] - 1; x += 3) { set(level, bb, chiseled, x, t[5], t[1]); set(level, bb, chiseled, x, t[5], t[3]); }
			for (int z = t[1] + 2; z < t[3] - 1; z += 3) { set(level, bb, chiseled, t[0], t[5], z); set(level, bb, chiseled, t[2], t[5], z); }
			// A course of stairs round the tier's foot, climbing toward it, and lanterns on the terrace corners.
			stepRing(level, bb, t[0] - 1, t[1] - 1, t[2] + 1, t[3] + 1, t[4], trim, chiseled);
			if (k > 0) for (int[] c : new int[][]{{t[0] - 1, t[1] - 1}, {t[2] + 1, t[1] - 1}, {t[0] - 1, t[3] + 1}, {t[2] + 1, t[3] + 1}}) {
				set(level, bb, s(Blocks.LANTERN), c[0], t[4] + 1, c[1]);
			}
			// The face of the school carved over the door on each upper tier, lit at the eyes.
			if (k > 0) {
				fill(level, bb, 11, t[4], t[1], 15, t[5], t[1], chiseled);
				set(level, bb, light, 12, t[4] + 1, t[1]); set(level, bb, light, 14, t[4] + 1, t[1]);
			}
			// Roots pour down the flanks and the back from the tier's top course.
			for (int z = t[1] + 1; z < t[3]; z++) {
				int a = noise(t[0] - 1, z + k * 17) % 6, b = noise(t[2] + 1, z + k * 17) % 6;
				if (a > 1) fill(level, bb, t[0] - 1, Math.max(t[4], t[5] + 1 - a), z, t[0] - 1, t[5], z, roots);
				if (b > 1) fill(level, bb, t[2] + 1, Math.max(t[4], t[5] + 1 - b), z, t[2] + 1, t[5], z, roots);
			}
			for (int x = t[0] + 1; x < t[2]; x++) {
				int c = noise(x + k * 31, t[3] + 1) % 6;
				if (c > 1) fill(level, bb, x, Math.max(t[4], t[5] + 1 - c), t[3] + 1, x, t[5], t[3] + 1, roots);
			}
		}
		// Overgrowth and moss on the terraces.
		for (int k = 1; k < TIERS.length; k++) {
			int[] u = TIERS[k - 1];
			scatter(level, bb, u[0], u[5] + 1, u[1], u[2], u[5] + 1, u[3], leaves, 14);
			scatter(level, bb, u[0], u[5] + 1, u[1], u[2], u[5] + 1, u[3], s(Blocks.MOSS_CARPET), 24);
		}

		// The twin stairs, railed, rising from the court to the summit.
		for (int[] run : new int[][]{{7, 9}, {17, 19}}) {
			for (int z = 4; z <= 16; z++) {
				int y = z - 3;
				if (y > 1) fill(level, bb, run[0], 1, z, run[1], y - 1, z, masonry);
				for (int x = run[0]; x <= run[1]; x++) set(level, bb, stair(trim, Direction.NORTH, false), x, y, z);
				fill(level, bb, run[0], y + 1, z, run[1], y + 4, z, AIR);
				for (int x : new int[]{run[0] - 1, run[1] + 1}) { fill(level, bb, x, 1, z, x, y, z, masonry); set(level, bb, rail, x, y + 1, z); }
			}
			// Newel posts at the foot, lit like the braziers.
			for (int x : new int[]{run[0] - 1, run[1] + 1}) { fill(level, bb, x, 1, 3, x, 2, 3, chiseled); set(level, bb, light, x, 3, 3); }
		}

		// The portal: a passage through the lowest tier, carved jambs and a lintel, lit behind.
		fill(level, bb, 12, 1, 10, 14, 4, 13, AIR);
		fill(level, bb, 12, 0, 10, 14, 0, 13, s(Blocks.MOSSY_COBBLESTONE));
		for (int x : new int[]{11, 15}) pillar(level, bb, x, 10, 1, 5, chiseled, wall, chiseled);
		for (int x = 12; x <= 14; x++) set(level, bb, stair(trim, Direction.NORTH, true), x, 5, 10);
		fill(level, bb, 11, 6, 10, 15, 6, 10, slab);
		set(level, bb, light, 13, 5, 11);

		// The hall inside the lower tiers, aisled with carved pillars.
		fill(level, bb, 4, 1, 14, 22, 6, 28, AIR);
		fill(level, bb, 4, 0, 14, 22, 0, 28, s(Blocks.MOSSY_COBBLESTONE));
		for (int z = 15; z <= 27; z += 3) for (int x : new int[]{6, 20}) {
			pillar(level, bb, x, z, 1, 6, chiseled, wall, chiseled);
			set(level, bb, stair(trim, Direction.EAST, true), x - 1, 6, z); set(level, bb, stair(trim, Direction.WEST, true), x + 1, 6, z);
		}
		for (int[] p : new int[][]{{9, 16}, {17, 16}, {9, 26}, {17, 26}, {13, 15}}) set(level, bb, light, p[0], 7, p[1]);
		for (int z = 16; z <= 26; z += 5) { set(level, bb, light, 3, 3, z); set(level, bb, light, 23, 3, z); }
		// The back wall carries the school's face, with its colours round the hall.
		fill(level, bb, 11, 1, 29, 15, 6, 29, chiseled);
		fill(level, bb, 12, 2, 29, 14, 2, 29, wall);
		set(level, bb, light, 12, 4, 29); set(level, bb, light, 14, 4, 29);
		Block banner = Blocks.WALL_BANNER.pick(alt ? DyeColor.LIME : DyeColor.GREEN);
		for (int x : new int[]{9, 17}) banner(level, bb, banner, x, 4, 28, Direction.SOUTH);
		for (int z : new int[]{18, 24}) { banner(level, bb, banner, 4, 4, z, Direction.EAST); banner(level, bb, banner, 22, 4, z, Direction.WEST); }
		hangingLantern(level, bb, 13, 6, 21, 2, false);
		for (int[] p : new int[][]{{4, 14}, {22, 14}, {4, 28}, {22, 28}}) { set(level, bb, s(Blocks.MOSS_BLOCK), p[0], 0, p[1]); set(level, bb, plant, p[0], 1, p[1]); }

		// The sparring pit.
		for (int x = 6; x <= 20; x++) for (int z = 14; z <= 28; z++) {
			double d = dist(x, z, 13, 21);
			if (d <= 5.5) fill(level, bb, x, -3, z, x, -1, z, wall);
			if (d <= 3.5) { fill(level, bb, x, -2, z, x, 0, z, AIR); set(level, bb, s(noise(x, z) < 50 ? Blocks.ROOTED_DIRT : Blocks.MOSS_BLOCK), x, -3, z); }
			else if (d <= 4.5) set(level, bb, s(Blocks.MOSSY_STONE_BRICKS), x, 0, z);
		}
		set(level, bb, light, 9, -2, 21); set(level, bb, light, 17, -2, 21);
		fill(level, bb, 13, -2, 24, 13, 0, 24, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		chest(level, bb, 13, -2, 18, rewardLoot(), Direction.NORTH, 2);
		guard(level, bb, EntityTypes.BOGGED, 13, -2, 21, alt ? List.of(Runes.ARC, Runes.VENOM) : List.of(Runes.BOLT, Runes.ROOTSNARE), false);
		// Practice, the lectern and the supplies.
		var post = s(Blocks.JUNGLE_FENCE);
		dummy(level, bb, 5, 1, 16, post); dummy(level, bb, 21, 1, 16, post);
		target(level, bb, 5, 1, 27, post); target(level, bb, 21, 1, 27, post);
		lectern(level, bb, 10, 1, 15);
		chest(level, bb, 16, 1, 15, MasterSites.supplies(site()), Direction.SOUTH, 1);

		// The summit shrine: carved corners, a lit doorway toward the stairs, a hipped roof and a pierced roof comb.
		walls(level, bb, 9, 14, 18, 17, 18, 24, masonry);
		fill(level, bb, 9, 19, 18, 17, 19, 24, wall);
		for (int[] c : new int[][]{{9, 18}, {17, 18}, {9, 24}, {17, 24}, {11, 18}, {15, 18}}) fill(level, bb, c[0], 14, c[1], c[0], 19, c[1], chiseled);
		fill(level, bb, 10, 14, 19, 16, 18, 23, AIR);
		fill(level, bb, 12, 14, 18, 14, 16, 18, AIR);
		for (int x = 12; x <= 14; x++) set(level, bb, stair(trim, Direction.NORTH, true), x, 17, 18);
		set(level, bb, light, 13, 18, 18);
		for (int x : new int[]{9, 17}) fill(level, bb, x, 16, 20, x, 17, 22, AIR);
		hipRoof(level, bb, 9, 18, 17, 24, 20, roofStairs, wall, slab);
		for (int x = 11; x <= 15; x++) for (int y = 24; y <= 28; y++) {
			boolean edge = x == 11 || x == 15 || y == 24 || y == 28;
			set(level, bb, edge || (x + y) % 2 == 0 ? wall : AIR, x, y, 21);
		}
		set(level, bb, light, 13, 26, 21);
		set(level, bb, rail, 12, 29, 21); set(level, bb, light, 13, 29, 21); set(level, bb, rail, 14, 29, 21);
		// Inside: an altar to the school under a light, plants either side.
		set(level, bb, light, 13, 19, 21);
		set(level, bb, chiseled, 13, 14, 23); set(level, bb, s(alt ? Blocks.POTTED_FERN : Blocks.POTTED_FLOWERING_AZALEA), 13, 15, 23);
		for (int x : new int[]{10, 16}) { set(level, bb, s(Blocks.MOSS_BLOCK), x, 13, 23); set(level, bb, plant, x, 14, 23); }
		// Lanterns on the summit's corners, and the canopy creeping onto it.
		for (int[] c : new int[][]{{7, 17}, {19, 17}, {7, 25}, {19, 25}}) set(level, bb, s(Blocks.LANTERN), c[0], 14, c[1]);
		scatter(level, bb, 7, 14, 23, 8, 14, 25, leaves, 50); scatter(level, bb, 18, 14, 23, 19, 14, 25, leaves, 50);
		// A garden in the court's wings, round the obelisks.
		scatter(level, bb, 0, 1, 0, 5, 1, 9, leaves, 18); scatter(level, bb, 21, 1, 0, 26, 1, 9, leaves, 18);
		scatter(level, bb, 0, 1, 0, 5, 1, 9, plant, 34); scatter(level, bb, 21, 1, 0, 26, 1, 9, plant, 34);
	}

	/** A course of stairs round a tier's foot at {@code y}, each climbing toward the tier; corners solid. */
	private void stepRing(WorldGenLevel level, BoundingBox bb, int x0, int z0, int x1, int z1, int y, Block stairs, BlockState corner) {
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

	@Override public List<BoundingBox> wardedBoxes() { return List.of(); }
}
