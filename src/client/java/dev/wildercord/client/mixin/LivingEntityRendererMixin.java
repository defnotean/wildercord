package dev.wildercord.client.mixin;

import dev.wildercord.client.render.RuneMarksLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Carries a Runebound's rune marks into its render state, where {@link RuneMarksLayer} draws them.
 * A render layer only ever sees the state, never the entity, and there is no event for this step.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
		at = @At("TAIL"))
	private void wildercord$runeMarks(LivingEntity entity, LivingEntityRenderState state, float partial, CallbackInfo ci) {
		RuneMarksLayer.extract(entity, state, partial);
	}
}
