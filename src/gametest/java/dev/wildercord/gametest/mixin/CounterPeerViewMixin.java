package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.combat.ArticulatedViewModel;
import dev.wildercord.gametest.CounterPeerRenderProbe;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The shell has a separate model/submit method; these hooks observe only the skin model and real item. */
@Mixin(value = ArticulatedViewModel.class, remap = false)
public abstract class CounterPeerViewMixin {
	@WrapMethod(method = "submit(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FLnet/minecraft/world/InteractionHand;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)Z", require = 1, expect = 1, allow = 1)
	private static boolean wildercord$counter$view(AvatarRenderState avatar, FirstPersonHandsAndItemsRenderState hands, float partial,
			InteractionHand hand, PoseStack pose, SubmitNodeCollector collector, int light, Operation<Boolean> original) {
		var call = CounterPeerRenderProbe.viewEnter(avatar, partial, hand, pose, collector);
		boolean submitted = false;
		try { submitted = original.call(avatar, hands, partial, hand, pose, collector, light); return submitted; }
		finally { CounterPeerRenderProbe.viewLeave(call, submitted); }
	}

	@WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FLnet/minecraft/world/InteractionHand;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)Z",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"), require = 1, expect = 1, allow = 1)
	private static void wildercord$counter$model(SubmitNodeCollector collector, Model<?> model, Object state, PoseStack pose,
			RenderType type, int light, int overlay, int color, UvMapping uv, int outline, Operation<Void> original) {
		var submitted = new org.joml.Matrix4f(pose.last().pose());
		original.call(collector, model, state, pose, type, light, overlay, color, uv, outline);
		CounterPeerRenderProbe.viewSubmitted(model, state, submitted, type);
	}

	@WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FLnet/minecraft/world/InteractionHand;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)Z",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"), require = 1, expect = 1, allow = 1)
	private static void wildercord$counter$item(ItemStackRenderState item, PoseStack pose, SubmitNodeCollector collector,
			int light, int overlay, int outline, Operation<Void> original) {
		var submitted = new org.joml.Matrix4f(pose.last().pose());
		original.call(item, pose, collector, light, overlay, outline);
		CounterPeerRenderProbe.viewItem(item, submitted);
	}

	@WrapMethod(method = "setupAnim(Ldev/wildercord/client/combat/ArticulatedViewModel$Frame;)V", require = 1, expect = 1, allow = 1)
	private void wildercord$counter$palette(ArticulatedViewModel.Frame frame, Operation<Void> original) {
		original.call(frame);
		CounterPeerRenderProbe.viewPalette((ArticulatedViewModel) (Object) this, frame);
	}
}
