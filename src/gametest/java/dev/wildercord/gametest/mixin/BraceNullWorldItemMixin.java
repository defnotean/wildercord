package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.BraceNullItemDrawProbe;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemInHandLayer.class)
public abstract class BraceNullWorldItemMixin {
	@Unique private static final ThreadLocal<BraceNullItemDrawProbe.Call> wildercord$braceNullItem = new ThreadLocal<>();
	@WrapMethod(method = "submitArmWithItem", require = 1, expect = 1, allow = 1)
	private void wildercord$braceNullWorld(ArmedEntityRenderState state, ItemStackRenderState item, ItemStack sword, HumanoidArm arm,
		PoseStack pose, SubmitNodeCollector collector, int light, Operation<Void> original) {
		var call = state instanceof AvatarRenderState avatar ? BraceNullItemDrawProbe.worldBegin(((ItemInHandLayer<?, ?>) (Object) this).getParentModel(), avatar, item, sword, arm, pose, collector) : null;
		var prior = wildercord$braceNullItem.get(); wildercord$braceNullItem.set(call); boolean complete = false;
		try { original.call(state, item, sword, arm, pose, collector, light); complete = true; }
		finally { try { BraceNullItemDrawProbe.end(call, complete); } finally { if (prior == null) wildercord$braceNullItem.remove(); else wildercord$braceNullItem.set(prior); } }
	}
	@WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"), require = 1, expect = 1, allow = 1)
	private void wildercord$braceNullWorldItem(ItemStackRenderState item, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outline, Operation<Void> original) {
		BraceNullItemDrawProbe.worldPre(wildercord$braceNullItem.get(), ((ItemInHandLayer<?, ?>) (Object) this).getParentModel(), item, pose, collector);
		original.call(item, pose, collector, light, overlay, outline);
	}
}
