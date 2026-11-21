package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.gametest.BraceNullItemDrawProbe;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemInHandLayer.class)
public abstract class BraceNullWorldItemMixin {
	@Unique private static final ThreadLocal<BraceNullItemDrawProbe.Call> wildercord$braceNullItem = new ThreadLocal<>();
	@WrapMethod(method = "renderArmWithItem", require = 1, expect = 1, allow = 1)
	private void wildercord$braceNullWorld(ArmedEntityRenderState state, ItemStackRenderState item, HumanoidArm arm,
		PoseStack pose, MultiBufferSource buffer, int light, Operation<Void> original) {
		ItemStack sword = arm == HumanoidArm.LEFT ? dev.wildercord.client.compat.RenderCompat.leftHandStack(state) : dev.wildercord.client.compat.RenderCompat.rightHandStack(state);
		var call = state instanceof PlayerRenderState avatar ? BraceNullItemDrawProbe.worldBegin(((ItemInHandLayer<?, ?>) (Object) this).getParentModel(), avatar, item, sword, arm, pose, buffer) : null;
		var prior = wildercord$braceNullItem.get(); wildercord$braceNullItem.set(call); boolean complete = false;
		try { original.call(state, item, arm, pose, buffer, light); complete = true; }
		finally { try { BraceNullItemDrawProbe.end(call, complete); } finally { if (prior == null) wildercord$braceNullItem.remove(); else wildercord$braceNullItem.set(prior); } }
	}
	@WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"), require = 1, expect = 1, allow = 1)
	private void wildercord$braceNullWorldItem(ItemStackRenderState item, PoseStack pose, MultiBufferSource buffer, int light, int overlay, int outline, Operation<Void> original) {
		BraceNullItemDrawProbe.worldPre(wildercord$braceNullItem.get(), ((ItemInHandLayer<?, ?>) (Object) this).getParentModel(), item, pose, buffer);
		original.call(item, pose, buffer, light, overlay, outline);
	}
}
