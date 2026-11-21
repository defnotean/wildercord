package dev.wildercord.gametest.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.MastersCaptureProbe;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class MastersHandRenderProbeMixin {
	@Inject(method = "renderItem", at = @At("TAIL"))
	private void wildercord$submittedSword(CallbackInfo ci,
			@com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.world.item.ItemStack stack,
			@com.llamalad7.mixinextras.sugar.Local(argsOnly = true) PoseStack pose,
			@com.llamalad7.mixinextras.sugar.Local(argsOnly = true) MultiBufferSource buffer) {
		// 1.21.8 has no item render state for the first-person hand: nothing to observe.
		var itemState = dev.wildercord.client.compat.FirstPersonHandView
			.of((ItemInHandRenderer) (Object) this).initialMainHandRenderState();
		if (itemState == null) {
			return;
		}
		var entry = dev.wildercord.gametest.BraceNullCaptureProbe.currentHand();
		if (entry != null && entry.hand() == net.minecraft.world.InteractionHand.MAIN_HAND && stack.is(Items.DIAMOND_SWORD) && !itemState.isEmpty())
			MastersCaptureProbe.hand(entry.avatar().id, itemState, pose, entry.avatar(), dev.wildercord.client.compat.FirstPersonHandView.of((ItemInHandRenderer) (Object) this), entry.attack(), entry.inverseHeight(), buffer);
	}
}
