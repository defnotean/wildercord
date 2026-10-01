package dev.wildercord.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.fx.AuraBlade;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * One layer of an item's model has just been submitted, its pose (the hand's, the display transform's, the model's own) still
 * on the stack: the blade's aura, when one is being drawn, goes round it in the model's own space (see {@link AuraBlade}).
 */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public abstract class ItemLayerAuraMixin {
	@Shadow
	private ItemQuads quads;

	@Shadow
	private Supplier<Vector3fc[]> extents;

	@Inject(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"))
	private void wildercord$aura(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outline, CallbackInfo ci) {
		AuraBlade.layer(pose, collector, quads, extents);
	}
}
