package dev.wildercord.client.mixin;

import dev.wildercord.client.fx.ScreenEffects;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A field-of-view kick when a charged spell leaves your hands, and a squeeze when a heavy hit lands. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void wildercord$kick(float partial, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(ScreenEffects.fov(cir.getReturnValue(), partial));
	}
}
