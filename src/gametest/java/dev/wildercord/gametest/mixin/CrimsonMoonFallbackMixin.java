package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.FirstPersonAvatarState;
import dev.wildercord.gametest.CrimsonMoonRenderProbe;
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
public abstract class CrimsonMoonFallbackMixin {
	@Unique private static final ThreadLocal<CrimsonMoonRenderProbe.FallbackView> wildercord$moon$openingFallback = new ThreadLocal<>();

	@WrapMethod(method = "submitArmWithItem", require = 1, expect = 1, allow = 1)
	private void wildercord$moon$fallback(AbstractClientPlayer player, float partial, float xRot, InteractionHand hand,
			float attack, ItemStack stack, float inverseArmHeight, PoseStack pose, SubmitNodeCollector collector,
			int light, Operation<Void> original) {
		ItemInHandRenderer renderer = (ItemInHandRenderer) (Object) this;
		AvatarRenderState avatar = ((FirstPersonAvatarState) this).wildercord$handsAvatar(player, partial);
		CrimsonMoonRenderProbe.viewAttempt(avatar, renderer, player, partial, hand);
		var previous = wildercord$moon$openingFallback.get();
		var call = CrimsonMoonRenderProbe.fallbackViewBegin(renderer, avatar, player, hand, stack, attack, inverseArmHeight, pose);
		wildercord$moon$openingFallback.set(call);
		boolean completed = false;
		try {
			original.call(player, partial, xRot, hand, attack, stack, inverseArmHeight, pose, collector, light);
			completed = true;
		} finally {
			CrimsonMoonRenderProbe.fallbackViewEnd(call, completed);
			wildercord$moon$openingFallback.set(previous);
		}
	}

	@WrapOperation(method = "renderItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"), require = 1, expect = 1, allow = 1)
	private void wildercord$moon$item(ItemStackRenderState item, PoseStack pose, SubmitNodeCollector collector,
			int light, int overlay, int outline, Operation<Void> original) {
		var call = wildercord$moon$openingFallback.get();
		if (call != null) call.bindItem(item);
		var submitted = new org.joml.Matrix4f(pose.last().pose());
		original.call(item, pose, collector, light, overlay, outline);
		CrimsonMoonRenderProbe.fallbackViewItem(call, item, submitted);
	}
}
