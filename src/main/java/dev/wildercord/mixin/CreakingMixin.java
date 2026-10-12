package dev.wildercord.mixin;

import dev.wildercord.wildlife.HushwingOwlet;
import net.minecraft.world.entity.monster.creaking.Creaking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A creaking a hushwing owlet is staring down can't move, just as when a player looks at it. */
@Mixin(Creaking.class)
public abstract class CreakingMixin {
	@Inject(method = "checkCanMove", at = @At("HEAD"), cancellable = true)
	private void wildercord$stared(CallbackInfoReturnable<Boolean> cir) {
		Creaking creaking = (Creaking) (Object) this;
		if (!creaking.level().isClientSide() && HushwingOwlet.pinned(creaking)) {
			cir.setReturnValue(false);
		}
	}
}
