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
 * Lightweight center anchor piece for {@code wildercord:oathhold} so {@code /locate} and structure
 * galleries resolve a valid bounded centerpiece even when inspected in isolation.
 */
public final class OathholdAnchorPiece extends StructurePiece {
	private final int originX;
	private final int baseY;
	private final int originZ;

	public OathholdAnchorPiece(int originX, int baseY, int originZ) {
		super(OathholdSites.ANCHOR_PIECE, 0,
			new BoundingBox(originX - 16, baseY - 24, originZ - 16, originX + 15, baseY + 48, originZ + 15));
		this.originX = originX;
		this.baseY = baseY;
		this.originZ = originZ;
	}

	public OathholdAnchorPiece(CompoundTag tag) {
		super(OathholdSites.ANCHOR_PIECE, tag);
		this.originX = tag.getIntOr("OX", 0);
		this.baseY = tag.getIntOr("OY", 64);
		this.originZ = tag.getIntOr("OZ", 0);
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
		OathholdInnerCore.build(canvas);
	}
}
