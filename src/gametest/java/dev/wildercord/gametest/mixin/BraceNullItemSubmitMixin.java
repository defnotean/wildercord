package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.BraceNullItemDrawProbe;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;

/** A displayed callback counts only while the original bound ItemStackRenderState.submit executes. */
@Mixin(ItemStackRenderState.class)
public abstract class BraceNullItemSubmitMixin {
	@WrapMethod(method = "submit", require = 1, expect = 1, allow = 1)
	private void wildercord$braceNullSubmitting(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outline, Operation<Void> original) {
		var item = (ItemStackRenderState) (Object) this;
		BraceNullItemDrawProbe.itemEnter(item, pose, collector); boolean complete = false;
		try { original.call(pose, collector, light, overlay, outline); complete = true; }
		finally { BraceNullItemDrawProbe.itemLeave(item, complete); }
	}
}
