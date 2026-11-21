package dev.wildercord.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.fx.AuraBlade;
import dev.wildercord.client.fx.BondGlow;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import dev.wildercord.client.compat.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.wildercord.client.render.RenderStateData;

/**
 * A bonded blade lying on the ground keeps its glow ({@link BondGlow}): its look is carried into the item's render state, the glow goes on
 * the blade as it's drawn (through {@link AuraBlade}'s layer hook), and a pool of its light lies under it.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererBondMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",
		at = @At("TAIL"))
	private void wildercord$bondLook(ItemEntity entity, ItemEntityRenderState state, float partial, CallbackInfo ci) {
		BondGlow.Look look = BondGlow.ground(entity);
		RenderStateData.set(state, BondGlow.GROUND, look);
		RenderStateData.set(state, BondGlow.GROUND_DAY, look != null && BondGlow.day(entity));
	}

	@Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At("HEAD"))
	private void wildercord$bondBegin(ItemEntityRenderState state, PoseStack pose, MultiBufferSource buffer, int light, CallbackInfo ci) {
		BondGlow.Look look = RenderStateData.get(state, BondGlow.GROUND);
		if (look != null) {
			BondGlow.groundExtras(pose, buffer, look, state.ageInTicks, dev.wildercord.client.compat.RenderCompat.cameraState().orientation, Boolean.TRUE.equals(RenderStateData.get(state, BondGlow.GROUND_DAY)));
		}
		AuraBlade.beginGround(look);
	}

	@Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At("RETURN"))
	private void wildercord$bondEnd(ItemEntityRenderState state, PoseStack pose, MultiBufferSource buffer, int light, CallbackInfo ci) {
		AuraBlade.end();
	}
}
