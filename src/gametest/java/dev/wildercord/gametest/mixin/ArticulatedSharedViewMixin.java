package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.client.combat.ArticulatedViewModel;
import dev.wildercord.client.compat.FirstPersonHandView;
import dev.wildercord.gametest.ArticulatedSharedRenderProbe;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import dev.wildercord.client.compat.vanilla.ItemStackRenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The shell has a separate model/submit method; these hooks observe only the skin model and real item. */
@Mixin(value = ArticulatedViewModel.class, remap = false)
public abstract class ArticulatedSharedViewMixin {
	@WrapMethod(method = "submit(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Ldev/wildercord/client/compat/FirstPersonHandView;FLnet/minecraft/world/InteractionHand;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)Z", require = 1, expect = 1, allow = 1)
	private static boolean wildercord$view(PlayerRenderState avatar, FirstPersonHandView hands, float partial,
			InteractionHand hand, PoseStack pose, MultiBufferSource buffer, int light, Operation<Boolean> original) {
		var call = ArticulatedSharedRenderProbe.viewEnter(avatar, partial, hand);
		boolean submitted = false;
		try { submitted = original.call(avatar, hands, partial, hand, pose, buffer, light); return submitted; }
		finally { ArticulatedSharedRenderProbe.viewLeave(call, submitted); }
	}

	@WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Ldev/wildercord/client/compat/FirstPersonHandView;FLnet/minecraft/world/InteractionHand;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)Z",
		at = @At(value = "INVOKE", target = "Ldev/wildercord/client/compat/RenderCompat;submitModel(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/RenderType;IIII)V"), require = 1, expect = 1, allow = 1)
	private static void wildercord$model(MultiBufferSource buffer, Model model, Object state, PoseStack pose,
			RenderType type, int light, int overlay, int color, int outline, Operation<Void> original) {
		original.call(buffer, model, state, pose, type, light, overlay, color, outline);
		ArticulatedSharedRenderProbe.viewSubmitted(model, state);
	}

	@WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Ldev/wildercord/client/compat/FirstPersonHandView;FLnet/minecraft/world/InteractionHand;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)Z",
		at = @At(value = "INVOKE", target = "Ldev/wildercord/client/compat/RenderCompat;submitItemState(Ldev/wildercord/client/compat/vanilla/ItemStackRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;III)V"), require = 1, expect = 1, allow = 1)
	private static void wildercord$item(ItemStackRenderState item, PoseStack pose, MultiBufferSource buffer,
			int light, int overlay, int outline, Operation<Void> original) {
		original.call(item, pose, buffer, light, overlay, outline);
		// 1.21.8 carries no hand item state, so there is nothing to observe when none is handed over.
		if (item != null) {
			ArticulatedSharedRenderProbe.viewItem(item);
		}
	}

	@WrapMethod(method = "setupAnim(Ldev/wildercord/client/combat/ArticulatedViewModel$Frame;)V", require = 1, expect = 1, allow = 1)
	private void wildercord$palette(ArticulatedViewModel.Frame frame, Operation<Void> original) {
		original.call(frame);
		ArticulatedSharedRenderProbe.viewPalette((ArticulatedViewModel) (Object) this, frame);
	}
}
