package dev.wildercord.client.mixin;

import dev.wildercord.client.SigilTrace;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Steadied hands: while a charging caster holds sneak to trace their spell's glyph, the mouse moves the
 * tracing point instead of turning the view, so the camera holds still (see {@link SigilTrace}).
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Shadow
	private double accumulatedDX;
	@Shadow
	private double accumulatedDY;

	@Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
	private void wildercord$trace(double mousea, CallbackInfo ci) {
		if (SigilTrace.capturing()) {
			SigilTrace.mouse(accumulatedDX, accumulatedDY);
			ci.cancel();
		}
	}
}
