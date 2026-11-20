package dev.wildercord.gametest.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.gametest.BraceNullCaptureProbe;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** Outer native-method entry precedes the production art/height HEAD transforms. */
@Mixin(ItemInHandRenderer.class)
public abstract class BraceNullHandEntryMixin {
	@WrapMethod(method = "renderArmWithItem", require = 1, expect = 1, allow = 1)
	private void wildercord$braceNullEntry(AbstractClientPlayer player, float partial, float xRot,
		InteractionHand hand, float attack, ItemStack item, float inverseHeight, PoseStack pose, SubmitNodeCollector collector, int light, Operation<Void> original) {
		AvatarRenderState avatar = ((dev.wildercord.client.FirstPersonAvatarState) this).wildercord$handsAvatar(player, partial);
		dev.wildercord.gametest.BraceNullItemDrawProbe.Call call = null;
		if (hand == InteractionHand.MAIN_HAND && avatar != null)
			call = BraceNullCaptureProbe.handBefore(avatar, dev.wildercord.client.compat.FirstPersonHandView.of((ItemInHandRenderer) (Object) this, player), item, attack, inverseHeight, pose, collector);
		var previous = BraceNullCaptureProbe.pushHand(avatar, hand, item, attack, inverseHeight);
		boolean complete = false;
		try { original.call(player, partial, xRot, hand, attack, item, inverseHeight, pose, collector, light); complete = true; }
		finally { BraceNullCaptureProbe.popHand(previous); dev.wildercord.gametest.BraceNullItemDrawProbe.end(call, complete); }
	}
}
