package dev.wildercord.client.mixin;

import dev.wildercord.client.fx.HitStop;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Aura's hit-stop ({@link HitStop}): while an entity is held, each fresh look made for it stands and is posed as it was when the
 * hold began. Every entity's look passes through here, the local player's first-person hands among them, so a held swing holds in
 * first person too.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRenderDispatcherHitStopMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("RETURN"))
	private void wildercord$held(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
		HitStop.extracted(entity, state);
	}
}
