package dev.wildercord.mixin;

import dev.wildercord.world.dungeons.DungeonWards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * No explosion blasts a dungeon's warded arena or vault open (TNT, creepers, fireballs, a spell's blast): the blocks
 * it would break there stay. Anything a player put there goes as usual.
 */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@Shadow
	@Final
	private Vec3 center;

	@Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
	private void wildercord$spareWards(CallbackInfoReturnable<List<BlockPos>> cir) {
		List<BlockPos> blown = cir.getReturnValue();
		if (blown.isEmpty()) {
			return;
		}
		// The wards near the blast, asked for once (a blast reaches a chunk or so at most).
		List<BoundingBox> wards = DungeonWards.boxesNear(level, BlockPos.containing(center), 1);
		if (wards.isEmpty()) {
			return;
		}
		List<BlockPos> kept = new ArrayList<>(blown.size());
		for (BlockPos pos : blown) {
			if (!inside(wards, pos) || level.getBlockState(pos).canBeReplaced() || DungeonWards.placedHere(level, pos)) {
				kept.add(pos);
			}
		}
		if (kept.size() != blown.size()) {
			cir.setReturnValue(kept);
		}
	}

	private static boolean inside(List<BoundingBox> wards, BlockPos pos) {
		for (BoundingBox box : wards) {
			if (box.isInside(pos)) {
				return true;
			}
		}
		return false;
	}
}
