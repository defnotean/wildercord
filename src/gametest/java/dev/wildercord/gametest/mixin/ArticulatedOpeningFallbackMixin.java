package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.FirstPersonAvatarState;
import dev.wildercord.gametest.ArticulatedSharedRenderProbe;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Passive exact native fallback path. A held sword submits an item, without an arm-model pass. */
@Mixin(ItemInHandRenderer.class)
public abstract class ArticulatedOpeningFallbackMixin {
	@Unique private static final ThreadLocal<ArticulatedSharedRenderProbe.FallbackView> wildercord$openingFallback = new ThreadLocal<>();

	@WrapMethod(method = "submitArmWithItem", require = 1, expect = 1, allow = 1)
	private void wildercord$fallback(AbstractClientPlayer player, float partial, float xRot, InteractionHand hand,
			float attack, ItemStack stack, float inverseArmHeight, PoseStack pose, SubmitNodeCollector collector,
			int light, Operation<Void> original) {
		var hands = dev.wildercord.client.compat.FirstPersonHandView.of((ItemInHandRenderer) (Object) this, player);
		AvatarRenderState avatar = ((FirstPersonAvatarState) this).wildercord$handsAvatar(player, partial);
		ArticulatedSharedRenderProbe.viewAttempt(avatar, hands, partial, hand);
		var previous = wildercord$openingFallback.get();
		var call = ArticulatedSharedRenderProbe.fallbackViewBegin(hands, avatar, hand, stack);
		wildercord$openingFallback.set(call);
		boolean completed = false;
		try {
			original.call(player, partial, xRot, hand, attack, stack, inverseArmHeight, pose, collector, light);
			completed = true;
		} finally {
			ArticulatedSharedRenderProbe.fallbackViewEnd(call, completed);
			wildercord$openingFallback.set(previous);
		}
	}

	@WrapOperation(method = "renderItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"), require = 1, expect = 1, allow = 1)
	private void wildercord$item(ItemStackRenderState item, PoseStack pose, SubmitNodeCollector collector,
			int light, int overlay, int outline, Operation<Void> original) {
		var call = wildercord$openingFallback.get();
		if (call != null) call.bindItem(item);
		original.call(item, pose, collector, light, overlay, outline);
		ArticulatedSharedRenderProbe.fallbackViewItem(call, item);
	}
}
