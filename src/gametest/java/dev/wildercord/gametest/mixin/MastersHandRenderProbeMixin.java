package dev.wildercord.gametest.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.MastersCaptureProbe;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class MastersHandRenderProbeMixin {
	@Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
	private void wildercord$submittedSword(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, float partial, float xRot,
			InteractionHand hand, float attack, ItemStack stack, float inverseArmHeight, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		if (hand == InteractionHand.MAIN_HAND && stack.is(Items.DIAMOND_SWORD) && !state.mainHandRenderState.isEmpty())
			MastersCaptureProbe.hand(player.avatarRenderState.id, state.mainHandRenderState, pose, player.avatarRenderState, state, attack, inverseArmHeight);
	}
}
