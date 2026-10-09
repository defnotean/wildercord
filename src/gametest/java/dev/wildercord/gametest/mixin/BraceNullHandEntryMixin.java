package dev.wildercord.gametest.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.gametest.BraceNullCaptureProbe;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/** Outer native-method entry precedes the production art/height HEAD transforms. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class BraceNullHandEntryMixin {
	@WrapMethod(method = "submitArmWithItem", require = 1, expect = 1, allow = 1)
	private void wildercord$braceNullEntry(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, float partial, float xRot,
		InteractionHand hand, float attack, ItemStack item, float inverseHeight, PoseStack pose, SubmitNodeCollector collector, int light, Operation<Void> original) {
		dev.wildercord.gametest.BraceNullItemDrawProbe.Call call = null;
		if (hand == InteractionHand.MAIN_HAND && player.avatarRenderState != null)
			call = BraceNullCaptureProbe.handBefore(player.avatarRenderState, state, item, attack, inverseHeight, pose, collector);
		boolean complete = false;
		try { original.call(player, state, partial, xRot, hand, attack, item, inverseHeight, pose, collector, light); complete = true; }
		finally { dev.wildercord.gametest.BraceNullItemDrawProbe.end(call, complete); }
	}
}
