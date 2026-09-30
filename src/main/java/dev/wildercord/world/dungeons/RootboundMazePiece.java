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
		super(DungeonWorldgen.ROOTBOUND_MAZE_PIECE, x, y, z, 31, 12, 43, facing);
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
		// Two side prizes tempt the player into both branches before the sealed vault.
		chest(level, bb, 4, 1, 20, DungeonWorldgen.ROOT_HALL, Direction.EAST, 101);
		chest(level, bb, 26, 1, 20, DungeonWorldgen.ROOT_HALL, Direction.WEST, 102);
		fill(level, bb, 8, 1, 32, 22, 4, 32, BRICK);
		sealDoorAcross(level, bb, 32, 13, 17, 1, 4, RuneSealBlock.Element.LIFE, RuneSealBlock.Element.EARTH);
		chest(level, bb, 12, 1, 39, DungeonWorldgen.ROOT_VAULT, Direction.NORTH, 103);
		chest(level, bb, 18, 1, 39, DungeonWorldgen.ROOT_VAULT, Direction.NORTH, 104);
		guard(level, bb, EntityTypes.ZOMBIE, 8, 1, 18, List.of(Runes.TOUCH, Runes.ROOTSNARE), false);
		guard(level, bb, EntityTypes.WITCH, 22, 1, 18, List.of(Runes.BOLT, Runes.VENOM), false);
		guard(level, bb, EntityTypes.BOGGED, 15, 1, 28, List.of(Runes.ARC, Runes.MIRE), true);
	}

	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of(worldBox(7, 0, 31, 23, 10, 42));
	}
}
