package dev.wildercord.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.fx.AuraBlade;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.BakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * One layer of an item's model has just been submitted, its pose (the hand's, the display transform's, the model's own) still
 * on the stack: the blade's aura, when one is being drawn, goes round it in the model's own space (see {@link AuraBlade}).
 * 1.21.4's layer state keeps no quads or extents, so the layer's baked model answers for both.
 */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public abstract class ItemLayerAuraMixin {
	@Shadow
	BakedModel model;

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
	private void wildercord$aura(PoseStack pose, MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
		AuraBlade.layer(pose, buffer, model == null ? null : model.getQuads(null, null, net.minecraft.util.RandomSource.create()), () -> null);
	}
}
