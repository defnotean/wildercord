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

/** A climb through three charged floors to a sealed observatory above the peaks. */
public class StormSpirePiece extends DungeonPiece {
	private static final BlockState STONE = Blocks.POLISHED_ANDESITE.defaultBlockState();
	private static final BlockState BRICK = Blocks.STONE_BRICKS.defaultBlockState();
	private static final BlockState LIGHT = Blocks.SEA_LANTERN.defaultBlockState();

	public StormSpirePiece(int x, int y, int z, Direction facing) {
		super(DungeonWorldgen.STORM_SPIRE_PIECE, x, y, z, 23, 29, 23, facing);
	}

	public StormSpirePiece(CompoundTag tag) {
		super(DungeonWorldgen.STORM_SPIRE_PIECE, tag);
	}

	static Optional<Structure.GenerationStub> locate(Structure.GenerationContext context) {
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
		ChunkPos chunk = context.chunkPos();
		StormSpirePiece piece = new StormSpirePiece(chunk.getMinBlockX(), 0, chunk.getMinBlockZ(), facing);
		BlockPos entrance = piece.getWorldPos(11, 0, 0);
		int ground = context.chunkGenerator().getFirstOccupiedHeight(entrance.getX(), entrance.getZ(), Heightmap.Types.WORLD_SURFACE_WG,
			context.heightAccessor(), context.randomState());
		piece.move(0, ground - 1, 0);
		return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(), ground, entrance.getZ()), builder -> builder.addPiece(piece)));
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunkPos, BlockPos reference) {
		DungeonWards.remember(level, this);
		room(level, bb, 1, 0, 1, 21, 27, 21, BRICK);
		fill(level, bb, 9, 1, 1, 13, 4, 1, AIR);
		for (int y : new int[] {7, 14, 21}) {
			fill(level, bb, 2, y, 2, 20, y, 20, STONE);
			fill(level, bb, 10, y, 10, 12, y, 12, AIR);
			for (int x : new int[] {4, 18}) {
				for (int z : new int[] {4, 18}) {
					set(level, bb, LIGHT, x, y, z);
				}
			}
		}
		// The central scaffold shaft can be climbed or descended from every floor.
		for (int y = 1; y <= 23; y++) {
			set(level, bb, Blocks.SCAFFOLDING.defaultBlockState(), 11, y, 11);
		}
		for (int y : new int[] {1, 8, 15, 22}) {
			for (int x : new int[] {3, 19}) {
				for (int z : new int[] {3, 19}) {
					fill(level, bb, x, y, z, x, y + 3, z, STONE);
					set(level, bb, LIGHT, x, y + 3, z);
				}
			}
		}
		// Each floor has a guarded side cache. The upper chamber is warded and opened with magic.
		chest(level, bb, 18, 8, 10, DungeonWorldgen.STORM_HALL, Direction.WEST, 201);
		chest(level, bb, 4, 15, 17, DungeonWorldgen.STORM_HALL, Direction.EAST, 202);
		fill(level, bb, 2, 22, 15, 20, 25, 15, BRICK);
		sealDoorAcross(level, bb, 15, 9, 13, 22, 25, RuneSealBlock.Element.STORM, RuneSealBlock.Element.WIND);
		chest(level, bb, 6, 22, 19, DungeonWorldgen.STORM_VAULT, Direction.EAST, 203);
		chest(level, bb, 17, 22, 19, DungeonWorldgen.STORM_VAULT, Direction.WEST, 204);
		altar(level, bb, dev.wildercord.content.dungeons.DungeonAltarBlock.Kind.STORM, 11, 22, 18);
		for (int x : new int[] {6, 11, 16}) set(level, bb, Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState(), x, 22, 17);
		for (int x : new int[] {5, 11, 17}) {
			set(level, bb, Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState(), x, 28, 11);
		}
		guard(level, bb, EntityTypes.STRAY, 6, 1, 16, List.of(Runes.BOLT, Runes.SHOCK), false);
		guard(level, bb, EntityTypes.SKELETON, 16, 8, 5, List.of(Runes.ARC, Runes.WINDCUT), false);
		guard(level, bb, EntityTypes.STRAY, 6, 15, 6, List.of(Runes.ORB, Runes.THUNDERCLAP), true);
	}

	@Override
	public List<BoundingBox> wardedBoxes() {
		return List.of(worldBox(2, 21, 15, 20, 27, 21));
	}
}
