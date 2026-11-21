package dev.wildercord.gametest.mixin;

import dev.wildercord.gametest.CrimsonMoonRenderProbe;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The return follows the production dispatcher HitStop hook; this is the actual entity argument. */
@Mixin(EntityRenderer.class)
public abstract class CrimsonMoonExtractionMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("RETURN"), require = 1)
	private void wildercord$moon$extracted(Entity entity, EntityRenderState state, float partial, CallbackInfo ci) {
		CrimsonMoonRenderProbe.extracted(entity, partial, state);
	}
}
