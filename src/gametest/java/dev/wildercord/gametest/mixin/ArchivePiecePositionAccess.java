package dev.wildercord.gametest.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reads the actual piece transform; does not reconstruct it or move the piece. */
@Mixin(StructurePiece.class)
public interface ArchivePiecePositionAccess {
	@Invoker("getWorldPos")
	BlockPos.MutableBlockPos wildercord$worldPosition(int x, int y, int z);
}
