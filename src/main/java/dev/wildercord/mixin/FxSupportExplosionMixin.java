package dev.wildercord.mixin;

import dev.wildercord.cast.packs.WardState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** No explosion breaks a block inside a Blastward or a Citadel, nor one under Keepsafe. */
@Mixin(ServerExplosion.class)
public abstract class FxSupportExplosionMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
	private void wildercord$fxSupportSpare(CallbackInfoReturnable<List<BlockPos>> cir) {
		List<BlockPos> blown = cir.getReturnValue();
		if (blown.isEmpty() || !WardState.anyBlockWards()) {
			return;
		}
		List<BlockPos> kept = new ArrayList<>(blown.size());
		for (BlockPos pos : blown) {
			if (!WardState.sparesBlock(level, pos)) {
				kept.add(pos);
			}
		}
		if (kept.size() != blown.size()) {
			cir.setReturnValue(kept);
		}
	}
}
