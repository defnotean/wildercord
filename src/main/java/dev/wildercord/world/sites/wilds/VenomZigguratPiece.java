package dev.wildercord.world.sites.wilds;

import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.spell.Runes;
import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * A stepped jungle ziggurat. The front stair climbs to a shrine where venom-bound Bogged keep watch; a low tunnel in
 * the back runs past two tripwires (poison darts from both walls) to a vault behind a Life and Void seal. It stands in
 * its own cut clearing: a mossy plaza with lily pools either side of a processional avenue lined with smoking obelisks.
 */
public final class VenomZigguratPiece extends WildsPiece {
	/** The plaza around the ziggurat. */
	static final int MARGIN = 6;
	public VenomZigguratPiece(int x, int z, Direction facing) { super(WildsSites.VENOM_ZIGGURAT, x, z, 23, 22, 23, facing, MARGIN); }
	public VenomZigguratPiece(CompoundTag tag) { super(WildsSites.VENOM_ZIGGURAT, tag); }
	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		return surface(c, new VenomZigguratPiece(c.chunkPos().getMinBlockX(), c.chunkPos().getMinBlockZ(), facing(c)), 5);
	}
	@Override protected int margin() { return MARGIN; }
	/** The two tripwires across the tunnel. */
	static final int[] TRAPS = {19, 16};

	private static BlockState stone(int x, int y, int z) {
		int n = noise(x, y, z);
		return s(n < 22 ? Blocks.MOSSY_COBBLESTONE : n < 55 ? Blocks.MOSSY_STONE_BRICKS : n < 63 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS);
	}

	@Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos reference) {
		clearCanopy(level, bb);
		DungeonWards.remember(level, this);
		footing(level, bb, -MARGIN, -MARGIN, 22 + MARGIN, 22 + MARGIN, s(Blocks.COBBLESTONE));
		clear(level, bb, -MARGIN, 1, -MARGIN, 22 + MARGIN, 21, 22 + MARGIN);
		plaza(level, bb);
		// Four tiers, each two in and three up from the last.
		for (int t = 0; t < 4; t++) for (int x = 2 * t; x <= 22 - 2 * t; x++) for (int z = 2 * t; z <= 22 - 2 * t; z++) for (int y = 3 * t; y <= 3 * t + 2; y++)
			set(level, bb, stone(x, y, z), x, y, z);
		// Moss and leaves along each tier's lip.
		for (int t = 0; t < 4; t++) for (int x = 2 * t; x <= 22 - 2 * t; x++) for (int z = 2 * t; z <= 22 - 2 * t; z++) {
			boolean lip = x - 2 * t < 2 || 22 - 2 * t - x < 2 || z - 2 * t < 2 || 22 - 2 * t - z < 2;
			if (lip && t < 3 && noise(x, z) < 22) set(level, bb, s(Blocks.MOSS_BLOCK), x, 3 * t + 2, z);
			if (lip && t < 3 && noise(z, x) < 9) set(level, bb, s(Blocks.JUNGLE_LEAVES).setValue(LeavesBlock.PERSISTENT, true), x, 3 * t + 3, z);
		}
		// Chiselled quoins on every tier's corners, a lantern burning on each lower corner.
		for (int t = 0; t < 4; t++) for (int[] c : new int[][]{{2 * t, 2 * t}, {22 - 2 * t, 2 * t}, {2 * t, 22 - 2 * t}, {22 - 2 * t, 22 - 2 * t}}) {
			fill(level, bb, c[0], 3 * t, c[1], c[0], 3 * t + 2, c[1], s(Blocks.CHISELED_STONE_BRICKS));
			if (t < 3) set(level, bb, s(Blocks.LANTERN), c[0], 3 * t + 3, c[1]);
		}
		// The shrine on top.
		room(level, bb, 7, 11, 7, 15, 15, 15, s(Blocks.MOSSY_STONE_BRICKS));
		fill(level, bb, 8, 11, 8, 14, 11, 14, s(Blocks.CHISELED_STONE_BRICKS));
		for (int x : new int[]{9, 13}) { set(level, bb, AIR, 7, 13, x); set(level, bb, AIR, 15, 13, x); }
		set(level, bb, s(Blocks.VERDANT_FROGLIGHT), 11, 15, 11);
		set(level, bb, s(Blocks.CAULDRON), 11, 12, 13);
		for (int[] p : new int[][]{{8, 8}, {14, 8}, {8, 14}}) set(level, bb, s(Blocks.LANTERN), p[0], 12, p[1]);
		hangingLantern(level, bb, 11, 14, 9, 0, false);
		banner(level, bb, Blocks.WALL_BANNER.pick(DyeColor.GREEN), 11, 14, 14, Direction.SOUTH);
		// Its outside: corner piers and pilasters, window sills, a corbelled cornice, merlons and a stepped crown.
		for (int[] p : new int[][]{{6, 6}, {16, 6}, {6, 16}, {16, 16}, {6, 11}, {16, 11}, {11, 16}}) fill(level, bb, p[0], 12, p[1], p[0], 13, p[1], s(Blocks.CHISELED_STONE_BRICKS));
		for (int z : new int[]{9, 13}) {
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 6, 12, z);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 16, 12, z);
		}
		for (int v = 7; v <= 15; v++) {
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true), v, 14, 6);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.SOUTH, true), v, 14, 16);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true), 6, 14, v);
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true), 16, 14, v);
			for (int[] c : new int[][]{{v, 6}, {v, 16}, {6, v}, {16, v}}) set(level, bb, topSlab(Blocks.STONE_BRICK_SLAB), c[0], 15, c[1]);
		}
		for (int[] c : new int[][]{{6, 6}, {16, 6}, {6, 16}, {16, 16}}) fill(level, bb, c[0], 14, c[1], c[0], 15, c[1], s(Blocks.CHISELED_STONE_BRICKS));
		crenellate(level, bb, 7, 7, 15, 15, 16, s(Blocks.MOSSY_STONE_BRICKS));
		for (int[] c : new int[][]{{7, 7}, {15, 7}, {7, 15}, {15, 15}}) set(level, bb, s(Blocks.LANTERN), c[0], 17, c[1]);
		fill(level, bb, 9, 16, 9, 13, 16, 13, s(Blocks.MOSSY_STONE_BRICKS));
		fill(level, bb, 10, 17, 10, 12, 17, 12, s(Blocks.CHISELED_STONE_BRICKS));
		set(level, bb, s(Blocks.VERDANT_FROGLIGHT), 11, 17, 11);
		// A stepped spire over the crown, its green fire seen above the jungle.
		for (int[] c : new int[][]{{10, 10}, {12, 10}, {10, 12}, {12, 12}}) set(level, bb, s(Blocks.MOSSY_STONE_BRICK_WALL), c[0], 18, c[1]);
		fill(level, bb, 11, 18, 11, 11, 19, 11, s(Blocks.CHISELED_STONE_BRICKS));
		set(level, bb, s(Blocks.VERDANT_FROGLIGHT), 11, 20, 11);
		set(level, bb, s(Blocks.MOSSY_STONE_BRICK_WALL), 11, 21, 11);
		// The front stair, cut through the tiers into the shrine.
		for (int step = 0; step <= 10; step++) {
			clear(level, bb, 10, step + 2, step, 12, step + 4, step);
			fill(level, bb, 10, step + 1, step, 12, step + 1, step, s(Blocks.STONE_BRICK_STAIRS).setValue(StairBlock.FACING, Direction.NORTH));
		}
		chest(level, bb, 14, 12, 13, WildsSites.ZIGGURAT_HALL, Direction.WEST, 1);
		// Lamp posts at the stair's foot and at its head.
		for (int[] p : new int[][]{{9, 3, 0}, {13, 3, 0}, {9, 12, 6}, {13, 12, 6}}) {
			set(level, bb, s(Blocks.STONE_BRICK_WALL), p[0], p[1], p[2]);
			set(level, bb, s(Blocks.LANTERN), p[0], p[1] + 1, p[2]);
		}
		// The back tunnel and its darts.
		clear(level, bb, 10, 1, 14, 12, 2, 22);
		fill(level, bb, 9, 3, 21, 13, 3, 22, s(Blocks.CHISELED_STONE_BRICKS));
		for (int x : new int[]{9, 13}) fill(level, bb, x, 1, 22, x, 2, 22, s(Blocks.CHISELED_STONE_BRICKS));
		for (int z : TRAPS) {
			// A wire across the whole tunnel, hooks in niches either side, a dart dispenser over each hook.
			set(level, bb, s(Blocks.TRIPWIRE_HOOK).setValue(TripWireHookBlock.FACING, Direction.EAST).setValue(TripWireHookBlock.ATTACHED, true), 9, 1, z);
			fill(level, bb, 10, 1, z, 12, 1, z, s(Blocks.TRIPWIRE).setValue(TripWireBlock.ATTACHED, true).setValue(TripWireBlock.EAST, true).setValue(TripWireBlock.WEST, true));
			set(level, bb, s(Blocks.TRIPWIRE_HOOK).setValue(TripWireHookBlock.FACING, Direction.WEST).setValue(TripWireHookBlock.ATTACHED, true), 13, 1, z);
			set(level, bb, s(Blocks.MOSSY_STONE_BRICKS), 8, 1, z);
			set(level, bb, s(Blocks.MOSSY_STONE_BRICKS), 14, 1, z);
			RandomSource darts = RandomSource.create(getBoundingBox().minX() * 31L + getBoundingBox().minZ() * 17L + z);
			createDispenser(level, bb, darts, 9, 2, z, Direction.EAST, WildsSites.ZIGGURAT_DARTS);
			createDispenser(level, bb, darts, 13, 2, z, Direction.WEST, WildsSites.ZIGGURAT_DARTS);
		}
		// The vault in the ziggurat's heart.
		room(level, bb, 6, 0, 6, 16, 5, 13, s(Blocks.MOSSY_STONE_BRICKS));
		fill(level, bb, 7, 0, 7, 15, 0, 12, s(Blocks.MOSS_BLOCK));
		set(level, bb, s(Blocks.VERDANT_FROGLIGHT), 11, 0, 10);
		sealDoorAcross(level, bb, 13, 10, 12, 1, 2, RuneSealBlock.Element.LIFE, RuneSealBlock.Element.VOID);
		chest(level, bb, 11, 1, 8, WildsSites.ZIGGURAT_VAULT, Direction.NORTH, 2);
		for (int x : new int[]{8, 14}) set(level, bb, s(Blocks.LANTERN), x, 1, 7);
		for (int[] p : new int[][]{{7, 7}, {15, 7}, {7, 12}, {15, 12}}) pillar(level, bb, p[0], p[1], 1, 4, s(Blocks.CHISELED_STONE_BRICKS), s(Blocks.MOSSY_STONE_BRICKS), s(Blocks.CHISELED_STONE_BRICKS));
		guard(level, bb, EntityTypes.BOGGED, 9, 12, 12, List.of(Runes.BOLT, Runes.VENOM), false);
		guard(level, bb, EntityTypes.BOGGED, 13, 12, 10, List.of(Runes.BOLT, Runes.VENOM), false);
		guard(level, bb, EntityTypes.WITCH, 9, 1, 10, List.of(Runes.ZONE, Runes.VENOM), true);
	}

	/** The cut clearing: a worn mossy plaza, lily pools beside the avenue, smoking obelisks along it and at the corners. */
	private void plaza(WorldGenLevel level, BoundingBox bb) {
		for (int x = -MARGIN; x <= 22 + MARGIN; x++) for (int z = -MARGIN; z <= 22 + MARGIN; z++) {
			int n = noise(x, 5, z);
			boolean avenue = z < 0 && x >= 8 && x <= 14;
			set(level, bb, avenue ? s(n < 30 ? Blocks.MOSSY_STONE_BRICKS : n < 40 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS)
				: s(n < 25 ? Blocks.MOSS_BLOCK : n < 45 ? Blocks.MOSSY_COBBLESTONE : n < 60 ? Blocks.COARSE_DIRT : n < 85 ? Blocks.GRASS_BLOCK : Blocks.MOSSY_STONE_BRICKS), x, 0, z);
			if (!avenue && n < 25 && noise(z, 9, x) < 40) set(level, bb, s(Blocks.FERN), x, 1, z);
		}
		// The avenue's kerbs and the pools either side of it.
		for (int z = -MARGIN; z < 0; z++) for (int x : new int[]{7, 15}) set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), x, 0, z);
		for (int x0 : new int[]{1, 17}) {
			walls(level, bb, x0, 0, -5, x0 + 4, 0, -1, Palette.of(Blocks.MOSSY_STONE_BRICKS, 1));
			fill(level, bb, x0 + 1, -1, -4, x0 + 3, -1, -2, s(Blocks.MUD));
			fill(level, bb, x0 + 1, 0, -4, x0 + 3, 0, -2, s(Blocks.WATER));
			for (int[] p : new int[][]{{x0 + 1, -4}, {x0 + 3, -2}}) set(level, bb, s(Blocks.LILY_PAD), p[0], 1, p[1]);
			set(level, bb, s(Blocks.STONE_BRICK_SLAB), x0, 1, -5);
			set(level, bb, s(Blocks.STONE_BRICK_SLAB), x0 + 4, 1, -1);
		}
		// Obelisks: a chiselled plinth, a mossy shaft, a burning cap. Two pairs line the avenue, four mark the corners.
		for (int[] o : new int[][]{{7, -5, 4}, {15, -5, 4}, {7, -2, 4}, {15, -2, 4}, {-4, -4, 7}, {26, -4, 7}, {-4, 26, 7}, {26, 26, 7}}) {
			set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), o[0], 1, o[1]);
			fill(level, bb, o[0], 2, o[1], o[0], o[2], o[1], s(Blocks.MOSSY_STONE_BRICKS));
			set(level, bb, s(Blocks.CHISELED_STONE_BRICKS), o[0], o[2] + 1, o[1]);
			set(level, bb, s(Blocks.CAMPFIRE), o[0], o[2] + 2, o[1]);
			if (o[2] > 4) for (Direction d : Direction.Plane.HORIZONTAL) {
				set(level, bb, stair(Blocks.MOSSY_STONE_BRICK_STAIRS, d.getOpposite(), false), o[0] + d.getStepX(), 1, o[1] + dz(d));
			}
		}
	}

	@Override public List<BoundingBox> wardedBoxes() { return List.of(worldBox(6, 0, 6, 16, 5, 13)); }
}
