package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.BraceNullItemDrawProbe;
import net.minecraft.client.renderer.MultiBufferSource;
import dev.wildercord.client.compat.vanilla.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;

/** A displayed callback counts only while the original bound ItemStackRenderState.render executes. */
@Mixin(ItemStackRenderState.class)
public abstract class BraceNullItemSubmitMixin {
	@WrapMethod(method = "render", require = 1, expect = 1, allow = 1)
	private void wildercord$braceNullSubmitting(PoseStack pose, MultiBufferSource buffer, int light, int overlay, Operation<Void> original) {
		var item = (ItemStackRenderState) (Object) this;
		BraceNullItemDrawProbe.itemEnter(item, pose, buffer); boolean complete = false;
		try { original.call(pose, buffer, light, overlay); complete = true; }
		finally { BraceNullItemDrawProbe.itemLeave(item, complete); }
	}
}
