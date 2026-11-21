package dev.wildercord.mixin;

import dev.wildercord.compat.PathTypeCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.21.10 has no fabric land-path registry: residue dangers are folded into the walk evaluator's own answers. */
@Mixin(WalkNodeEvaluator.class)
public abstract class PathTypeCompatMixin {
	@Inject(method = "getPathType(Lnet/minecraft/world/level/pathfinder/PathfindingContext;III)Lnet/minecraft/world/level/pathfinder/PathType;",
		at = @At("RETURN"), cancellable = true)
	private void wildercord$residueDanger(PathfindingContext context, int x, int y, int z, CallbackInfoReturnable<PathType> cir) {
		if (cir.getReturnValue() != PathType.OPEN) {
			return;
		}
		PathType danger = PathTypeCompat.walkDangerFor(context.getBlockState(new BlockPos(x, y, z)));
		if (danger != null) {
			cir.setReturnValue(danger);
		}
	}
}
