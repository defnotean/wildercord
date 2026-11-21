package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.combat.ArticulatedViewModel;
import dev.wildercord.client.compat.FirstPersonHandView;
import dev.wildercord.gametest.CrimsonMoonRenderProbe;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The shell has a separate model/submit method; these hooks observe only the skin model and real item. */
@Mixin(value = ArticulatedViewModel.class, remap = false)
public abstract class CrimsonMoonViewMixin {
	@WrapMethod(method = "submit(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Ldev/wildercord/client/compat/FirstPersonHandView;FLnet/minecraft/world/InteractionHand;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)Z", require = 1, expect = 1, allow = 1)
	private static boolean wildercord$moon$view(PlayerRenderState avatar, FirstPersonHandView hands, float partial,
			InteractionHand hand, PoseStack pose, MultiBufferSource buffer, int light, Operation<Boolean> original) {
		var call = CrimsonMoonRenderProbe.viewEnter(avatar, partial, hand, pose);
		boolean submitted = false;
		try { submitted = original.call(avatar, hands, partial, hand, pose, buffer, light); return submitted; }
		finally { CrimsonMoonRenderProbe.viewLeave(call, submitted); }
	}

	@WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Ldev/wildercord/client/compat/FirstPersonHandView;FLnet/minecraft/world/InteractionHand;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)Z",
		at = @At(value = "INVOKE", target = "Ldev/wildercord/client/compat/RenderCompat;submitModel(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderType;IIII)V"), require = 1, expect = 1, allow = 1)
	private static void wildercord$moon$model(MultiBufferSource buffer, Model model, Object state, PoseStack pose,
			RenderType type, int light, int overlay, int color, int outline, Operation<Void> original) {
		var submitted = new org.joml.Matrix4f(pose.last().pose());
		original.call(buffer, model, state, pose, type, light, overlay, color, outline);
		CrimsonMoonRenderProbe.viewSubmitted(model, state, submitted, type);
	}

	@WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Ldev/wildercord/client/compat/FirstPersonHandView;FLnet/minecraft/world/InteractionHand;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)Z",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"), require = 1, expect = 1, allow = 1)
	private static void wildercord$moon$item(ItemStackRenderState item, PoseStack pose, MultiBufferSource buffer,
			int light, int overlay, int outline, Operation<Void> original) {
		var submitted = new org.joml.Matrix4f(pose.last().pose());
		original.call(item, pose, buffer, light, overlay, outline);
		CrimsonMoonRenderProbe.viewItem(item, submitted);
	}

	@WrapMethod(method = "setupAnim(Ldev/wildercord/client/combat/ArticulatedViewModel$Frame;)V", require = 1, expect = 1, allow = 1)
	private void wildercord$moon$palette(ArticulatedViewModel.Frame frame, Operation<Void> original) {
		original.call(frame);
		CrimsonMoonRenderProbe.viewPalette((ArticulatedViewModel) (Object) this, frame);
	}
}
