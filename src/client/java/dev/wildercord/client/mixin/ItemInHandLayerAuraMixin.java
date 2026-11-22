package dev.wildercord.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.fx.AuraBlade;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Third person: while a hand's item is drawn, the blade's aura (see {@link AuraBlade}) knows whose and which hand it is. */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerAuraMixin {
	// 1.21.3 draws the hand item straight from the state's stack and baked model: there is no
	// ItemStackRenderState on the way, so the seam takes the arguments the layer really passes.
	@Inject(method = "renderArmWithItem", at = @At("HEAD"))
	private void wildercord$auraBegin(LivingEntityRenderState state, BakedModel model, ItemStack stack, ItemDisplayContext context,
			HumanoidArm arm, PoseStack pose, MultiBufferSource buffer, int light, CallbackInfo ci) {
		AuraBlade.beginThirdPerson(state, arm, stack);
	}

	@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"))
	private void wildercord$thrustGrip(LivingEntityRenderState state, BakedModel model, ItemStack stack, ItemDisplayContext context,
			HumanoidArm arm, PoseStack pose, MultiBufferSource buffer, int light, CallbackInfo ci) {
		dev.wildercord.client.GlaivePose.heldItem(state, arm, stack, pose);
		dev.wildercord.client.MastersArtPose.heldSword(state, arm, stack, pose);
		dev.wildercord.client.auraworld.MasterModel.heldSword(state, arm, stack, pose);
	}

	@Inject(method = "renderArmWithItem", at = @At("RETURN"))
	private void wildercord$auraEnd(LivingEntityRenderState state, BakedModel model, ItemStack stack, ItemDisplayContext context,
			HumanoidArm arm, PoseStack pose, MultiBufferSource buffer, int light, CallbackInfo ci) {
		AuraBlade.end();
	}
}
