package dev.wildercord.world.sites.oathhold;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A 128x128-block (8x8 chunk) architectural sector tile of Oathhold, anchored at {@code (sectorCx, sectorCz)}
 * and referencing the owning metropolis {@code (originX, baseY, originZ)}.
 */
public final class OathholdSectorPiece extends StructurePiece {
	private final int originX;
	private final int baseY;
	private final int originZ;

	public OathholdSectorPiece(int sectorCx, int sectorCz, int originX, int baseY, int originZ) {
		super(OathholdSites.SECTOR_PIECE, 0, new BoundingBox(
			(sectorCx - 4) * 16, baseY - 24, (sectorCz - 4) * 16,
			(sectorCx + 4) * 16 - 1, baseY + 132, (sectorCz + 4) * 16 - 1
		));
		this.originX = originX;
		this.baseY = baseY;
		this.originZ = originZ;
	}

	public OathholdSectorPiece(CompoundTag tag) {
		super(OathholdSites.SECTOR_PIECE, tag);
		this.originX = tag.contains("OX") ? tag.getInt("OX") : 0;
		this.baseY = tag.contains("OY") ? tag.getInt("OY") : 64;
		this.originZ = tag.contains("OZ") ? tag.getInt("OZ") : 0;
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
		tag.putInt("OX", originX);
		tag.putInt("OY", baseY);
		tag.putInt("OZ", originZ);
	}

	public int originX() {
		return originX;
	}

	public int originZ() {
		return originZ;
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
			BoundingBox bb, ChunkPos chunk, BlockPos pivot) {
		OathholdCanvas canvas = new OathholdCanvas(level, bb, originX, baseY, originZ, random);
		OathholdLaw.rememberCity(level, originX, baseY, originZ);
		OathholdTerrain.prepareChunk(canvas);
		OathholdFortifications.build(canvas);
		OathholdInnerCore.build(canvas);
		OathholdGreatHall.build(canvas);
		OathholdAcropolisHalls.build(canvas);
		OathholdOuterLandmarks.build(canvas);
		OathholdOuterWards.build(canvas);
		OathholdPopulation.build(canvas);
	}
}
