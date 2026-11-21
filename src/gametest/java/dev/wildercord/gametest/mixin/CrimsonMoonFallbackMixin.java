package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.FirstPersonAvatarState;
import dev.wildercord.gametest.CrimsonMoonRenderProbe;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Passive exact native fallback path. A held sword submits an item, without an arm-model pass. */
@Mixin(ItemInHandRenderer.class)
public abstract class CrimsonMoonFallbackMixin {
	@Unique private static final ThreadLocal<CrimsonMoonRenderProbe.FallbackView> wildercord$moon$openingFallback = new ThreadLocal<>();

	@WrapMethod(method = "renderArmWithItem", require = 1, expect = 1, allow = 1)
	private void wildercord$moon$fallback(AbstractClientPlayer player, float partial, float xRot, InteractionHand hand,
			float attack, ItemStack stack, float inverseArmHeight, PoseStack pose, MultiBufferSource buffer,
			int light, Operation<Void> original) {
		var hands = dev.wildercord.client.compat.FirstPersonHandView.of((ItemInHandRenderer) (Object) this, player);
		PlayerRenderState avatar = ((FirstPersonAvatarState) this).wildercord$handsAvatar(player, partial);
		CrimsonMoonRenderProbe.viewAttempt(avatar, hands, partial, hand);
		var previous = wildercord$moon$openingFallback.get();
		var call = CrimsonMoonRenderProbe.fallbackViewBegin(hands, avatar, hand, stack, attack, inverseArmHeight, pose);
		wildercord$moon$openingFallback.set(call);
		boolean completed = false;
		try {
			original.call(player, partial, xRot, hand, attack, stack, inverseArmHeight, pose, buffer, light);
			completed = true;
		} finally {
			CrimsonMoonRenderProbe.fallbackViewEnd(call, completed);
			wildercord$moon$openingFallback.set(previous);
		}
	}

	@WrapOperation(method = "renderItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"), require = 1, expect = 1, allow = 1)
	private void wildercord$moon$item(ItemStackRenderState item, PoseStack pose, MultiBufferSource buffer,
			int light, int overlay, int outline, Operation<Void> original) {
		var call = wildercord$moon$openingFallback.get();
		if (call != null) call.bindItem(item);
		var submitted = new org.joml.Matrix4f(pose.last().pose());
		original.call(item, pose, buffer, light, overlay, outline);
		CrimsonMoonRenderProbe.fallbackViewItem(call, item, submitted);
	}
}
