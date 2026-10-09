package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Passive exact native fallback path. A held sword submits an item, without an arm-model pass. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class CounterPeerFallbackMixin {
	@Unique private static final ThreadLocal<CounterPeerRenderProbe.FallbackView> wildercord$counter$openingFallback = new ThreadLocal<>();

	@WrapMethod(method = "submitArmWithItem", require = 1, expect = 1, allow = 1)
	private void wildercord$counter$fallback(PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands, float partial,
			float xRot, InteractionHand hand, float attack, ItemStack stack, float inverseArmHeight,
			PoseStack pose, SubmitNodeCollector collector, int light, Operation<Void> original) {
		CounterPeerRenderProbe.viewAttempt(player.avatarRenderState, hands, partial, hand);
		var previous = wildercord$counter$openingFallback.get();
		var call = CounterPeerRenderProbe.fallbackViewBegin((FirstPersonHandsAndItemsRenderer) (Object) this,
			player.avatarRenderState, hands, hand, stack, attack, inverseArmHeight, pose, collector);
		wildercord$counter$openingFallback.set(call);
		boolean completed = false;
		try {
			original.call(player, hands, partial, xRot, hand, attack, stack, inverseArmHeight, pose, collector, light);
			completed = true;
		} finally {
			CounterPeerRenderProbe.fallbackViewEnd(call, completed);
			wildercord$counter$openingFallback.set(previous);
		}
	}

	@WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"), require = 2, expect = 2, allow = 2)
	private void wildercord$counter$item(ItemStackRenderState item, PoseStack pose, SubmitNodeCollector collector,
			int light, int overlay, int outline, Operation<Void> original) {
		var submitted = new org.joml.Matrix4f(pose.last().pose());
		original.call(item, pose, collector, light, overlay, outline);
		CounterPeerRenderProbe.fallbackViewItem(wildercord$counter$openingFallback.get(), item, submitted);
	}
}
