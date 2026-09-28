package dev.wildercord.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * The Archive: a buried library of magic, found by a ring of broken pillars around a stairway
 * going down. Inside: a hall of shelves guarded by Runebound, doors sealed by elements, a hall of
 * braziers, a domed arena where the Archivist waits, and a sealed vault of Tier IV runes.
 */
public class ArchiveStructure extends Structure {
	public static final MapCodec<ArchiveStructure> CODEC = simpleCodec(ArchiveStructure::new);

	public ArchiveStructure(Structure.StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
		if (!context.couldValidBiomeExistOnTopOfChunkCenter()) {
			return Optional.empty();
		}
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
		int x = context.chunkPos().getMinBlockX();
		int z = context.chunkPos().getMinBlockZ();
		ArchivePiece piece = new ArchivePiece(x, 0, z, facing);
		// Sink it so the top of the stairway meets the ground at the entrance.
		BlockPos entrance = piece.entrance();
		int ground = context.chunkGenerator().getFirstOccupiedHeight(entrance.getX(), entrance.getZ(), Heightmap.Types.WORLD_SURFACE_WG,
			context.heightAccessor(), context.randomState());
		// In very low worlds (superflat) it can't be buried that deep: it rises out of the ground instead.
		int floor = Math.max(ground - ArchivePiece.SURFACE, context.heightAccessor().getMinY() + 4);
		piece.move(0, floor, 0);
		return Optional.of(new Structure.GenerationStub(new BlockPos(entrance.getX(), ground, entrance.getZ()), builder -> builder.addPiece(piece)));
	}

	@Override
	public StructureType<?> type() {
		return WildercordWorldgen.ARCHIVE;
	}
}
