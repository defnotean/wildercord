package dev.wildercord.client.mixin;

import dev.wildercord.client.fx.ScreenEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A field-of-view kick when a charged spell leaves your hands, and a squeeze when a heavy hit lands. */
@Mixin(GameRenderer.class)
public abstract class CameraMixin {
	// 1.21.11 keeps the fov maths on GameRenderer, not Camera.
	@Inject(method = "getFov(Lnet/minecraft/client/Camera;FZ)F", at = @At("RETURN"), cancellable = true)
	private void wildercord$kick(Camera camera, float partial, boolean zoomed, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(ScreenEffects.fov(cir.getReturnValue(), partial));
	}
}
