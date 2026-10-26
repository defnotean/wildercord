package dev.wildercord.world.dungeons;

import dev.wildercord.content.RuneSealBlock;
import dev.wildercord.spell.Runes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;

/** A swamp ruin whose branching passages lead to a sealed garden vault. */
public class RootboundMazePiece extends DungeonPiece {
	private static final BlockState BRICK = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
	private static final BlockState STONE = Blocks.STONE_BRICKS.defaultBlockState();
	private static final BlockState MOSS = Blocks.MOSS_BLOCK.defaultBlockState();
	private static final BlockState LAMP = Blocks.SHROOMLIGHT.defaultBlockState();

	public RootboundMazePiece(int x, int y, int z, Direction facing) {
		super(DungeonWorldgen.ROOTBOUND_MAZE_PIECE, x, y, z, 31, 18, 43, facing);
	}

	public RootboundMazePiece(CompoundTag tag) {
		super(DungeonWorldgen.ROOTBOUND_MAZE_PIECE, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext context) {
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
		ChunkPos chunk = context.chunkPos();
		RootboundMazePiece piece = new RootboundMazePiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), facing);
		BlockPos entrance = piece.getWorldPos(15, 0, 1);
		int ground = context.chunkGenerator().getFirstOccupiedHeight(entrance.getX(), entrance.getZ(), Heightmap.Types.WORLD_SURFACE_WG,
			context.heightAccessor(), context.randomState());
		piece.move(0, ground - 1, 0);
		return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(), ground, entrance.getZ()), builder -> builder.addPiece(piece)));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunkPos, BlockPos reference) {
		DungeonWards.remember(level, this);
		// Two forked galleries, a crossing and a final garden. The open arches make the route readable.
		room(level, bb, 8, 0, 0, 22, 7, 11, BRICK);
		room(level, bb, 2, 0, 10, 13, 7, 25, STONE);
		room(level, bb, 17, 0, 10, 28, 7, 25, STONE);
		room(level, bb, 8, 0, 23, 22, 8, 32, BRICK);
		room(level, bb, 7, 0, 31, 23, 10, 42, BRICK);
		fill(level, bb, 12, 1, 10, 18, 4, 11, AIR);
		fill(level, bb, 10, 1, 10, 13, 4, 12, AIR);
		fill(level, bb, 17, 1, 10, 20, 4, 12, AIR);
		fill(level, bb, 10, 1, 23, 13, 4, 25, AIR);
		fill(level, bb, 17, 1, 23, 20, 4, 25, AIR);
		fill(level, bb, 12, 1, 24, 18, 4, 31, AIR);
		fill(level, bb, 12, 1, 0, 18, 4, 0, AIR);
		shell(level, bb);
		for (int x = 3; x <= 27; x++) {
			for (int z = 1; z <= 41; z++) {
				if (noise(x, z) < 12 && ((x >= 8 && x <= 22 && z <= 11) || (x <= 13 && z >= 11 && z <= 25)
					|| (x >= 17 && z >= 11 && z <= 25) || (x >= 8 && x <= 22 && z >= 24))) {
					set(level, bb, MOSS, x, 0, z);
				}
			}
		}
		for (int z : new int[] {5, 17, 29, 37}) {
			for (int x : new int[] {9, 21}) {
				set(level, bb, LAMP, x, 5, z);
			}
		}
		interior(level, bb);
		// Two side prizes tempt the player into both branches before the sealed vault.
		chest(level, bb, 4, 1, 20, DungeonWorldgen.ROOT_HALL, Direction.EAST, 101);
		chest(level, bb, 26, 1, 20, DungeonWorldgen.ROOT_HALL, Direction.WEST, 102);
		fill(level, bb, 8, 1, 32, 22, 4, 32, BRICK);
		sealDoorAcross(level, bb, 32, 13, 17, 1, 4, RuneSealBlock.Element.LIFE, RuneSealBlock.Element.EARTH);
		chest(level, bb, 12, 1, 39, DungeonWorldgen.ROOT_VAULT, Direction.NORTH, 103);
		chest(level, bb, 18, 1, 39, DungeonWorldgen.ROOT_VAULT, Direction.NORTH, 104);
		altar(level, bb, dev.wildercord.content.dungeons.DungeonAltarBlock.Kind.ROOT, 15, 1, 37);
		guard(level, bb, EntityTypes.ZOMBIE, 8, 1, 18, List.of(Runes.TOUCH, Runes.ROOTSNARE), false);
		guard(level, bb, EntityTypes.WITCH, 22, 1, 18, List.of(Runes.BOLT, Runes.VENOM), false);
		guard(level, bb, EntityTypes.BOGGED, 15, 1, 28, List.of(Runes.ARC, Runes.MIRE), true);
	}

	/** The ruin's outside: a gabled entrance hall with a framed portal, buttressed galleries and a steep roof over the vault. */
	private void shell(WorldGenLevel level, BoundingBox bb) {
		Palette worn = Palette.of(BRICK, 5, STONE, 3, Blocks.CRACKED_STONE_BRICKS, 2);
		BlockState chiseled = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
		// The entrance hall's gable faces the path; its ridge runs inward.
		gableRoof(level, bb, 8, 1, 22, 11, 8, false, Blocks.STONE_BRICK_STAIRS, BRICK, worn);
		// The portal: carved jambs, a lintel with a keystone, a hood of upturned stairs and a lantern under it.
		fill(level, bb, 11, 1, 0, 11, 5, 0, chiseled);
		fill(level, bb, 19, 1, 0, 19, 5, 0, chiseled);
		fill(level, bb, 12, 5, 0, 18, 5, 0, worn);
		set(level, bb, chiseled, 15, 6, 0);
		for (int x = 12; x <= 18; x++) if (x != 15) set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, true), x, 6, 0);
		hangingLantern(level, bb, 15, 4, 0, 0, false);
		// A rose window over the portal, its bars long rusted.
		for (int x = 14; x <= 16; x++) for (int y = 9; y <= 11; y++) set(level, bb, x == 15 && y == 10 ? LAMP : Blocks.IRON_BARS.defaultBlockState(), x, y, 1);
		// The galleries: parapets broken into merlons and stepped buttresses on their long outer walls.
		crenellate(level, bb, 2, 10, 13, 25, 8, BRICK);
		crenellate(level, bb, 17, 10, 28, 25, 8, BRICK);
		for (int z : new int[] {13, 18, 23}) {
			buttress(level, bb, 2, z, 0, 7, Direction.WEST, STONE, Blocks.STONE_BRICK_STAIRS);
			buttress(level, bb, 28, z, 0, 7, Direction.EAST, STONE, Blocks.STONE_BRICK_STAIRS);
		}
		// String courses of slabs under the gallery parapets.
		for (int z = 10; z <= 25; z++) {
			set(level, bb, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 1, 7, z);
			set(level, bb, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 29, 7, z);
		}
		// The crossing gets a low parapet; the vault a steep gable along x, ridged in moss brick.
		crenellate(level, bb, 8, 23, 22, 30, 9, BRICK);
		gableRoof(level, bb, 7, 31, 23, 41, 11, true, Blocks.STONE_BRICK_STAIRS, BRICK, worn);
		// A lantern tower over the crossing, barred windows on every face, a shroomlight burning between its merlons.
		fill(level, bb, 12, 9, 24, 18, 13, 30, worn);
		fill(level, bb, 13, 9, 25, 17, 12, 29, AIR);
		for (int x : new int[] {12, 18}) for (int z : new int[] {24, 30}) pillar(level, bb, x, z, 9, 14, chiseled, STONE, chiseled);
		fill(level, bb, 15, 10, 24, 15, 12, 24, Blocks.IRON_BARS.defaultBlockState());
		fill(level, bb, 15, 10, 30, 15, 12, 30, Blocks.IRON_BARS.defaultBlockState());
		fill(level, bb, 12, 10, 27, 12, 12, 27, Blocks.IRON_BARS.defaultBlockState());
		fill(level, bb, 18, 10, 27, 18, 12, 27, Blocks.IRON_BARS.defaultBlockState());
		crenellate(level, bb, 12, 24, 18, 30, 14, BRICK);
		set(level, bb, LAMP, 15, 9, 27);
		set(level, bb, LAMP, 15, 14, 27);
		set(level, bb, Blocks.MOSS_CARPET.defaultBlockState(), 15, 15, 27);
		for (int x : new int[] {7, 23}) {
			fill(level, bb, x, 13, 35, x, 15, 37, Blocks.IRON_BARS.defaultBlockState());
		}
		// Corner piers on the vault, standing proud of its walls.
		for (int x : new int[] {7, 23}) for (int z : new int[] {31, 42}) pillar(level, bb, x, z, 0, 11, chiseled, STONE, chiseled);
		// The swamp has had the run of it.
		weather(level, bb, 0, 0, 0, 30, 17, 42, STONE, Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 22);
		weather(level, bb, 0, 0, 0, 30, 17, 42, BRICK, Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 8);
	}

	/** Lamps hung on chains, roots through the ceilings, columns and a moss dais in the vault. */
	private void interior(WorldGenLevel level, BoundingBox bb) {
		BlockState chain = Blocks.IRON_CHAIN.defaultBlockState();
		for (int x : new int[] {9, 21}) {
			set(level, bb, chain, x, 6, 5);
			set(level, bb, chain, x, 6, 17);
			fill(level, bb, x, 6, 29, x, 7, 29, chain);
			fill(level, bb, x, 6, 37, x, 9, 37, chain);
		}
		// Columns down both sides of the vault, carrying corbels under the ceiling.
		for (int x : new int[] {9, 21}) for (int z : new int[] {34, 40}) {
			pillar(level, bb, x, z, 1, 9, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), STONE, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
			set(level, bb, stair(Blocks.STONE_BRICK_STAIRS, x == 9 ? Direction.WEST : Direction.EAST, true), x == 9 ? 10 : 20, 9, z);
		}
		hangingLantern(level, bb, 15, 9, 37, 3, false);
		// A moss dais round the altar, kept low so it never blocks the way.
		for (int x = 12; x <= 18; x++) for (int z = 35; z <= 40; z++) {
			if (dist(x, z, 15, 37) <= 3.2) set(level, bb, MOSS, x, 0, z);
		}
		// Roots hanging through every ceiling.
		int[][] rooms = {{9, 1, 21, 10, 6}, {3, 11, 12, 24, 6}, {18, 11, 27, 24, 6}, {9, 24, 21, 30, 7}, {8, 33, 22, 41, 9}};
		for (int[] r : rooms) for (int x = r[0]; x <= r[2]; x++) for (int z = r[1]; z <= r[3]; z++) {
			if (noise(x + 7, z) < 9 && getBlock(level, x, r[4], z, bb).isAir()) set(level, bb, Blocks.HANGING_ROOTS.defaultBlockState(), x, r[4], z);
		}
		scatter(level, bb, 3, 1, 1, 27, 1, 41, Blocks.MOSS_CARPET.defaultBlockState(), 10);
	}

	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of(worldBox(7, 0, 31, 23, 10, 42));
	}
}
