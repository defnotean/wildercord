package dev.wildercord.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
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
		dev.wildercord.client.MastersArtPose.firstPerson(pose, hand, player.avatarRenderState, inverseArmHeight);
		AuraBlade.beginFirstPerson(hand == InteractionHand.MAIN_HAND, stack);
	}

	@Inject(method = "submitArmWithItem", at = @At("RETURN"))
	private void wildercord$auraEnd(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, float partial, float xRot, InteractionHand hand,
			float attack, ItemStack stack, float inverseArmHeight, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		AuraBlade.end();
		pose.popPose();
	}
}
