package dev.wildercord.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.client.fx.AuraBlade;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Third person: while a hand's item is drawn, the blade's aura (see {@link AuraBlade}) knows whose and which hand it is. */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerAuraMixin {
	// 1.21.10's renderArmWithItem carries no ItemStack: the render state's noted hand stacks stand in.
	@Inject(method = "renderArmWithItem", at = @At("HEAD"))
	private void wildercord$auraBegin(ArmedEntityRenderState state, ItemStackRenderState item, HumanoidArm arm, PoseStack pose,
			MultiBufferSource buffer, int light, CallbackInfo ci) {
		AuraBlade.beginThirdPerson(state, arm, stackFor(state, arm));
	}

	@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"))
	private void wildercord$thrustGrip(ArmedEntityRenderState state, ItemStackRenderState item, HumanoidArm arm, PoseStack pose,
			MultiBufferSource buffer, int light, CallbackInfo ci) {
		ItemStack stack = stackFor(state, arm);
		dev.wildercord.client.GlaivePose.heldItem(state, arm, stack, pose);
		dev.wildercord.client.MastersArtPose.heldSword(state, arm, stack, pose);
		dev.wildercord.client.auraworld.MasterModel.heldSword(state, arm, stack, pose);
	}

	@Inject(method = "renderArmWithItem", at = @At("RETURN"))
	private void wildercord$auraEnd(ArmedEntityRenderState state, ItemStackRenderState item, HumanoidArm arm, PoseStack pose,
			MultiBufferSource buffer, int light, CallbackInfo ci) {
		AuraBlade.end();
	}

	private static ItemStack stackFor(ArmedEntityRenderState state, HumanoidArm arm) {
		return arm == HumanoidArm.LEFT ? RenderCompat.leftHandStack(state) : RenderCompat.rightHandStack(state);
	}
}
