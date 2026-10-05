package dev.wildercord.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.wildercord.aura.MastersViewMotion;
import dev.wildercord.client.MastersArtPose;
import net.minecraft.world.entity.HumanoidArm;
import dev.wildercord.client.fx.AuraBlade;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First person: while the main hand's item is drawn, the blade's aura (see {@link AuraBlade}) goes with it. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonAuraMixin {
	@Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
	private void wildercord$auraBegin(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, float partial, float xRot, InteractionHand hand,
			float attack, ItemStack stack, float inverseArmHeight, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		if (dev.wildercord.client.combat.ArticulatedCombat.viewFrame(player.avatarRenderState) != null) {
			AuraBlade.beginFirstPerson(hand == InteractionHand.MAIN_HAND, stack);
			boolean rendered;
			try {
				rendered = dev.wildercord.client.combat.ArticulatedViewModel.submit(player.avatarRenderState, state, partial, hand, pose, collector, light);
			} finally {
				AuraBlade.end();
			}
			if (rendered) { ci.cancel(); return; }
		}
		pose.pushPose();
		float ownership = MastersArtPose.firstPersonOwnership(hand, player.avatarRenderState, state);
		// The same-item attack ticker lowers the vanilla grip just like an equip. Compensate only
		// its owned fraction in camera space, keeping the original grip conjugation and argument.
		if (ownership > 0) pose.translate(0, MastersViewMotion.heightCompensation(inverseArmHeight, ownership), 0);
		dev.wildercord.client.MastersArtPose.firstPerson(pose, hand, player.avatarRenderState, inverseArmHeight);
		AuraBlade.beginFirstPerson(hand == InteractionHand.MAIN_HAND, stack);
	}

	@WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"))
	private void wildercord$blendAttack(FirstPersonHandsAndItemsRenderer renderer, float attack, PoseStack pose, int side, HumanoidArm arm,
			Operation<Void> original, @Local(argsOnly = true) PlayerRenderState player,
			@Local(argsOnly = true) FirstPersonHandsAndItemsRenderState state, @Local(argsOnly = true) InteractionHand hand) {
		float ownership = MastersArtPose.firstPersonOwnership(hand, player.avatarRenderState, state);
		if (ownership <= 0) { original.call(renderer, attack, pose, side, arm); return; }
		if (ownership >= 1) return;
		// Capture the native delta at its real phase, then fade its amplitude. Scaling the phase
		// would turn a nearly finished neutral swing into a large mid-swing during recovery.
		PoseStack delta = new PoseStack();
		original.call(renderer, attack, delta, side, arm);
		pose.mulPose(MastersViewMotion.fadeSwing(delta.last().pose(), ownership));
	}

	@Inject(method = "submitArmWithItem", at = @At("RETURN"))
	private void wildercord$auraEnd(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, float partial, float xRot, InteractionHand hand,
			float attack, ItemStack stack, float inverseArmHeight, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		AuraBlade.end();
		pose.popPose();
	}
}
