package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.FirstPersonAvatarState;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

/** Passive exact native fallback path. A held sword submits an item, without an arm-model pass. */
@Mixin(ItemInHandRenderer.class)
public abstract class CounterPeerFallbackMixin {
	@Unique private static final ThreadLocal<CounterPeerRenderProbe.FallbackView> wildercord$counter$openingFallback = new ThreadLocal<>();

	@WrapMethod(method = "renderArmWithItem", require = 1, expect = 1, allow = 1)
	private void wildercord$counter$fallback(AbstractClientPlayer player, float partial, float xRot, InteractionHand hand,
			float attack, ItemStack stack, float inverseArmHeight, PoseStack pose, MultiBufferSource buffer,
			int light, Operation<Void> original) {
		var hands = dev.wildercord.client.compat.FirstPersonHandView.of((ItemInHandRenderer) (Object) this, player);
		PlayerRenderState avatar = ((FirstPersonAvatarState) this).wildercord$handsAvatar(player, partial);
		CounterPeerRenderProbe.viewAttempt(avatar, hands, partial, hand);
		var previous = wildercord$counter$openingFallback.get();
		var call = CounterPeerRenderProbe.fallbackViewBegin(hands, avatar, hand, stack, attack, inverseArmHeight, pose, buffer);
		wildercord$counter$openingFallback.set(call);
		boolean completed = false;
		try {
			original.call(player, partial, xRot, hand, attack, stack, inverseArmHeight, pose, buffer, light);
			completed = true;
		} finally {
			CounterPeerRenderProbe.fallbackViewEnd(call, completed);
			wildercord$counter$openingFallback.set(previous);
		}
	}

	// 1.21.8 draws the hand item straight from the ItemStack: there is no item render state
	// to observe here, so the probe sees the state the view began with (and skips when
	// the platform has none).
	@Inject(method = "renderItem", at = @At("TAIL"))
	private void wildercord$counter$item(CallbackInfo ci) {
		var call = wildercord$counter$openingFallback.get();
		if (call == null) {
			return;
		}
		var item = call.item();
		CounterPeerRenderProbe.fallbackViewItem(call, item, new org.joml.Matrix4f());
	}
}
