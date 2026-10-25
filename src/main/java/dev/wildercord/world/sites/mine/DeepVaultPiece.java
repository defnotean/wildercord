package dev.wildercord.world.sites.mine;

import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.spell.Runes;
import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/**
 * A deepslate vault: a vent tower on the surface over a ladder shaft down to y -4, an antechamber on chiseled pillars held by
 * two Runebound skeletons, and a door of earth and fire seals (a magma block and a mud block flank it as the hint) into a
 * warded vault. The vault stays well above any ancient city and out of the deep dark. Local y 0 is the floor, the tower stands
 * on the ground at {@link #surface()}.
 */
public final class DeepVaultPiece extends MinePiece {
	static final int WIDTH = 17, DEPTH = 29, FLOOR_Y = -5, TOWER = 8, SHAFT_X = 8, SHAFT_Z = 5;

	public DeepVaultPiece(int x, int y, int z, int height, Direction facing) {
		super(MineSites.DEEP_VAULT, x, y, z, WIDTH, height, DEPTH, facing);
	}

	public DeepVaultPiece(CompoundTag tag) {
		super(MineSites.DEEP_VAULT, tag);
	}

	/** The ground's local height: the piece reaches {@link #TOWER} blocks above it. */
	int surface() {
		return getBoundingBox().getYSpan() - TOWER;
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext c) {
		var chunk = c.chunkPos();
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(c.random());
		var probe = new DeepVaultPiece(chunk.getMinBlockX(), FLOOR_Y, chunk.getMinBlockZ(), 1, facing);
		BlockPos top = probe.getWorldPos(SHAFT_X, 0, SHAFT_Z), middle = probe.getWorldPos(8, 0, 18);
		if (wet(c, top) || FLOOR_Y < c.heightAccessor().getMinY() + 8) return Optional.empty();
		int surface = ground(c, top);
		if (surface - FLOOR_Y < 30) return Optional.empty();
		for (int y : new int[] {FLOOR_Y, FLOOR_Y - 16}) if (deepDark(c, top.getX(), y, top.getZ()) || deepDark(c, middle.getX(), y, middle.getZ())) return Optional.empty();
		for (BlockPos at : new BlockPos[] {top, middle, probe.getWorldPos(3, 0, 8), probe.getWorldPos(13, 0, 24)}) {
			if (fluidIn(c, at, FLOOR_Y - 1, FLOOR_Y + 9)) return Optional.empty();
		}
		var piece = new DeepVaultPiece(chunk.getMinBlockX(), FLOOR_Y, chunk.getMinBlockZ(), surface - FLOOR_Y + TOWER, facing);
		return Optional.of(new Structure.GenerationStub(new BlockPos(top.getX(), surface, top.getZ()), b -> b.addPiece(piece)));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox bb, ChunkPos chunk,
			BlockPos reference) {
		DungeonWards.remember(level, this);
		BlockState brick = s(Blocks.DEEPSLATE_BRICKS), tile = s(Blocks.DEEPSLATE_TILES), cobbled = s(Blocks.COBBLED_DEEPSLATE), floor = s(Blocks.POLISHED_DEEPSLATE);
		BlockState chiseled = s(Blocks.CHISELED_DEEPSLATE);
		Palette wall = Palette.of(Blocks.DEEPSLATE_BRICKS, 6, Blocks.CRACKED_DEEPSLATE_BRICKS, 2, Blocks.DEEPSLATE_TILES, 2);
		Palette rough = Palette.of(Blocks.COBBLED_DEEPSLATE, 5, Blocks.DEEPSLATE_BRICKS, 3, Blocks.CRACKED_DEEPSLATE_BRICKS, 2);
		int top = surface();
		// The antechamber: worn brick walls, a tiled aisle to the seal door.
		room(level, bb, 2, 0, 3, 14, 7, 13, brick);
		walls(level, bb, 2, 1, 3, 14, 6, 13, wall);
		fill(level, bb, 2, 0, 3, 14, 0, 13, floor);
		fill(level, bb, 7, 0, 4, 9, 0, 12, tile);
		// Chiseled pillars with corbelled capitals, slab ribs between them under the ceiling.
		for (int x : new int[] {4, 12}) for (int z : new int[] {5, 11}) {
			fill(level, bb, x, 1, z, x, 6, z, chiseled);
			set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.WEST, true), x + 1, 6, z);
			set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.EAST, true), x - 1, 6, z);
			set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.SOUTH, true), x, 6, z + 1);
			set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), x, 6, z - 1);
		}
		for (int x = 6; x <= 10; x++) {
			if (x != SHAFT_X) set(level, bb, topSlab(Blocks.DEEPSLATE_BRICK_SLAB), x, 6, 5);
			set(level, bb, topSlab(Blocks.DEEPSLATE_BRICK_SLAB), x, 6, 11);
		}
		for (int z = 7; z <= 9; z++) {
			set(level, bb, topSlab(Blocks.DEEPSLATE_BRICK_SLAB), 4, 6, z);
			set(level, bb, topSlab(Blocks.DEEPSLATE_BRICK_SLAB), 12, 6, z);
		}
		for (int x : new int[] {5, 11}) for (int z : new int[] {8}) hangingLantern(level, bb, x, 6, z);
		for (int z : new int[] {6, 10}) {
			sconce(level, bb, 3, 3, z, Direction.EAST, true);
			sconce(level, bb, 13, 3, z, Direction.WEST, true);
		}
		chest(level, bb, 13, 1, 8, MineSites.VAULT_ANTECHAMBER, Direction.WEST, 461);
		guard(level, bb, EntityTypes.SKELETON, 5, 1, 9, List.of(Runes.BOLT, Runes.EMBER), false);
		guard(level, bb, EntityTypes.SKELETON, 11, 1, 9, List.of(Runes.BOLT, Runes.MIRE), true);
		// The vault, its walls seamed with ore, behind a door of earth and fire.
		for (int x = 4; x <= 12; x++) for (int y = 0; y <= 6; y++) for (int z = 14; z <= 26; z++) {
			boolean shell = x == 4 || x == 12 || y == 0 || y == 6 || z == 14 || z == 26;
			set(level, bb, !shell ? AIR : y == 0 ? floor : noise(x, y, z) < 6 ? s(Blocks.DEEPSLATE_GOLD_ORE) : rock(x, y, z, tile, true), x, y, z);
		}
		fill(level, bb, 7, 1, 14, 9, 3, 14, AIR);
		sealDoorAcross(level, bb, 13, 7, 9, 1, 3, RuneSealBlock.Element.EARTH, RuneSealBlock.Element.FIRE);
		set(level, bb, s(Blocks.MAGMA_BLOCK), 6, 2, 13);
		set(level, bb, s(Blocks.MUD_BRICKS), 10, 2, 13);
		// The door's frame: polished piers, a hood of stairs and a chiseled lintel, a soul lamp before it.
		for (int x : new int[] {5, 11}) {
			fill(level, bb, x, 1, 12, x, 4, 12, floor);
			set(level, bb, chiseled, x, 5, 12);
		}
		fill(level, bb, 6, 5, 12, 10, 5, 12, brick);
		set(level, bb, chiseled, 8, 5, 12);
		for (int x = 6; x <= 10; x++) set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), x, 4, 12);
		hangingLantern(level, bb, 8, 6, 10, 1, true);
		chest(level, bb, 6, 1, 25, MineSites.VAULT, Direction.SOUTH, 462);
		chest(level, bb, 10, 1, 25, MineSites.VAULT, Direction.SOUTH, 463);
		set(level, bb, s(Blocks.RAW_IRON_BLOCK), 5, 1, 25);
		set(level, bb, s(Blocks.RAW_GOLD_BLOCK), 11, 1, 25);
		set(level, bb, s(Blocks.SOUL_LANTERN), 8, 1, 25);
		// Inside: wall columns with chiseled capitals, a chiseled dais under the hoard and a soul lamp over the hall.
		for (int z : new int[] {17, 22}) {
			fill(level, bb, 5, 1, z, 5, 5, z, s(Blocks.POLISHED_DEEPSLATE_WALL));
			fill(level, bb, 11, 1, z, 11, 5, z, s(Blocks.POLISHED_DEEPSLATE_WALL));
			for (int x = 6; x <= 10; x++) set(level, bb, topSlab(Blocks.DEEPSLATE_TILE_SLAB), x, 5, z);
		}
		fill(level, bb, 5, 0, 24, 11, 0, 25, chiseled);
		hangingLantern(level, bb, 8, 5, 20, 1, true);
		// The shaft: a collar of cobbled deepslate lit every few courses, a ladder up a post from the antechamber floor, a trapdoor at the top.
		fill(level, bb, SHAFT_X - 1, 7, SHAFT_Z - 1, SHAFT_X + 1, top - 1, SHAFT_Z + 1, cobbled);
		for (int y = 12; y < top - 1; y += 6) set(level, bb, s(Blocks.GLOWSTONE), SHAFT_X - 1, y, SHAFT_Z);
		fill(level, bb, SHAFT_X, 1, SHAFT_Z + 1, SHAFT_X, 6, SHAFT_Z + 1, brick);
		fill(level, bb, SHAFT_X, 1, SHAFT_Z, SHAFT_X, top - 1, SHAFT_Z, s(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
		// The vent tower: a polished plinth course, rough walls with chiseled quoins, slit windows on sills under hoods.
		fill(level, bb, 5, top + 1, 2, 11, top + TOWER - 1, 8, AIR);
		room(level, bb, 6, top, 3, 10, top + 5, 7, cobbled);
		walls(level, bb, 6, top + 2, 3, 10, top + 4, 7, rough);
		walls(level, bb, 6, top + 1, 3, 10, top + 1, 7, Palette.of(Blocks.POLISHED_DEEPSLATE, 1));
		for (int x : new int[] {6, 10}) for (int z : new int[] {3, 7}) {
			fill(level, bb, x, top + 2, z, x, top + 4, z, brick);
			set(level, bb, chiseled, x, top + 3, z);
		}
		fill(level, bb, 7, top, 4, 9, top, 6, floor);
		set(level, bb, s(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.HALF, Half.TOP), SHAFT_X, top, SHAFT_Z);
		fill(level, bb, 6, top + 2, 5, 6, top + 3, 5, s(Blocks.IRON_BARS));
		fill(level, bb, 10, top + 2, 5, 10, top + 3, 5, s(Blocks.IRON_BARS));
		fill(level, bb, 8, top + 2, 7, 8, top + 3, 7, s(Blocks.IRON_BARS));
		set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.EAST, true), 5, top + 1, 5);
		set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.WEST, true), 11, top + 1, 5);
		set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.SOUTH, true), 8, top + 1, 8);
		set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.EAST, true), 5, top + 4, 5);
		set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.WEST, true), 11, top + 4, 5);
		set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.SOUTH, true), 8, top + 4, 8);
		// The door: polished jambs, a stair hood over it and a polished step.
		fill(level, bb, 8, top + 1, 3, 8, top + 2, 3, AIR);
		set(level, bb, floor, 7, top + 1, 2);
		set(level, bb, floor, 9, top + 1, 2);
		set(level, bb, s(Blocks.POLISHED_DEEPSLATE_WALL), 7, top + 2, 2);
		set(level, bb, s(Blocks.POLISHED_DEEPSLATE_WALL), 9, top + 2, 2);
		for (int x = 7; x <= 9; x++) set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), x, top + 3, 2);
		set(level, bb, floor, 8, top, 2);
		// The parapet: a corbel table all round, a wall on it, merlons on top, and the vent's grille in the middle.
		for (int x = 6; x <= 10; x++) {
			set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.NORTH, true), x, top + 5, 2);
			set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.SOUTH, true), x, top + 5, 8);
		}
		for (int z = 3; z <= 7; z++) {
			set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.EAST, true), 5, top + 5, z);
			set(level, bb, stair(Blocks.DEEPSLATE_BRICK_STAIRS, Direction.WEST, true), 11, top + 5, z);
		}
		for (int x : new int[] {5, 11}) for (int z : new int[] {2, 8}) set(level, bb, brick, x, top + 5, z);
		walls(level, bb, 5, top + 6, 2, 11, top + 6, 8, rough);
		crenellate(level, bb, 5, 2, 11, 8, top + 7, s(Blocks.COBBLED_DEEPSLATE_WALL));
		fill(level, bb, 6, top + 6, 3, 10, top + 6, 7, AIR);
		fill(level, bb, 6, top + 5, 3, 10, top + 5, 7, floor);
		fill(level, bb, 7, top + 5, 4, 9, top + 5, 6, s(Blocks.IRON_BARS));
		set(level, bb, s(Blocks.SOUL_LANTERN), 7, top + 1, 6);
		hangingLantern(level, bb, 9, top + 4, 4, 0, true);
		footing(level, bb, 5, 2, 11, 8, top, cobbled);
	}

	/** The vault itself, its door wall included, cannot be dug into: the seals are the way in. */
	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of(worldBox(4, 0, 13, 12, 6, 26));
	}
}
