package dev.wildercord.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.wildercord.aura.MastersViewMotion;
import dev.wildercord.client.MastersArtPose;
import dev.wildercord.client.fx.AuraBlade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First person: while the main hand's item is drawn, the blade's aura (see {@link AuraBlade}) goes with it. */
@Mixin(ItemInHandRenderer.class)
public abstract class FirstPersonAuraMixin implements dev.wildercord.client.FirstPersonAvatarState {
	@Unique private PlayerRenderState wildercord$avatar;

	@Override
	public PlayerRenderState wildercord$handsAvatar(AbstractClientPlayer player, float partial) {
		return wildercord$state(player, partial);
	}

	@Override
	public void wildercord$prepareHandsAvatar(AbstractClientPlayer player, float partial) {
		wildercord$avatar = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player).createRenderState(player, partial) instanceof PlayerRenderState avatar
			? avatar : null;
	}

	@Override
	public void wildercord$clearHandsAvatar() {
		wildercord$avatar = null;
	}

	@Unique private PlayerRenderState wildercord$state(AbstractClientPlayer player, float partial) {
		if (wildercord$avatar != null) return wildercord$avatar;
		if (Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player).createRenderState(player, partial) instanceof PlayerRenderState avatar) {
			return avatar;
		}
		return null;
	}

	@Inject(method = "renderHandsWithItems", at = @At("HEAD"))
	private void wildercord$extractHandsAvatar(float partial, PoseStack pose, MultiBufferSource buffer, LocalPlayer player, int light, CallbackInfo ci) {
		if (wildercord$avatar == null) wildercord$prepareHandsAvatar(player, partial);
	}

	@Inject(method = "renderHandsWithItems", at = @At("RETURN"))
	private void wildercord$clearHandsAvatar(float partial, PoseStack pose, MultiBufferSource buffer, LocalPlayer player, int light, CallbackInfo ci) {
		wildercord$avatar = null;
	}

	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void wildercord$auraBegin(AbstractClientPlayer player, float partial, float xRot, InteractionHand hand,
			float attack, ItemStack stack, float inverseArmHeight, PoseStack pose, MultiBufferSource buffer, int light, CallbackInfo ci) {
		ItemInHandRenderer renderer = (ItemInHandRenderer) (Object) this;
		dev.wildercord.client.compat.FirstPersonHandView hands = dev.wildercord.client.compat.FirstPersonHandView.of(renderer, player);
		PlayerRenderState avatar = wildercord$state(player, partial);
		if (avatar != null && dev.wildercord.client.combat.ArticulatedCombat.viewFrame(avatar) != null) {
			AuraBlade.beginFirstPerson(hand == InteractionHand.MAIN_HAND, stack);
			boolean rendered;
			try {
				rendered = dev.wildercord.client.combat.ArticulatedViewModel.submit(avatar, hands, partial, hand, pose, buffer, light);
			} finally {
				AuraBlade.end();
			}
			if (rendered) { ci.cancel(); return; }
		}
		pose.pushPose();
		float ownership = MastersArtPose.firstPersonOwnership(hand, avatar, hands);
		// The same-item attack ticker lowers the vanilla grip just like an equip. Compensate only
		// its owned fraction in camera space, keeping the original grip conjugation and argument.
		if (ownership > 0) pose.translate(0, MastersViewMotion.heightCompensation(inverseArmHeight, ownership), 0);
		dev.wildercord.client.MastersArtPose.firstPerson(pose, hand, avatar, inverseArmHeight);
		AuraBlade.beginFirstPerson(hand == InteractionHand.MAIN_HAND, stack);
	}

	@WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;swingArm(FFLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"))
	private void wildercord$blendAttack(ItemInHandRenderer renderer, float swing, float partialTick, PoseStack pose, int side, HumanoidArm arm,
			Operation<Void> original, @Local(argsOnly = true) AbstractClientPlayer player,
			@Local(argsOnly = true, ordinal = 0) float partial, @Local(argsOnly = true) InteractionHand hand) {
		PlayerRenderState avatar = wildercord$state(player, partial);
		float ownership = MastersArtPose.firstPersonOwnership(hand, avatar, dev.wildercord.client.compat.FirstPersonHandView.of(renderer, player));
		if (ownership <= 0) {
			if (avatar != null && dev.wildercord.client.GlaivePose.firstPersonSwing(swing, pose, side, arm, avatar, hand)) return;
			original.call(renderer, swing, partialTick, pose, side, arm);
			return;
		}
		if (ownership >= 1) return;
		// Capture the native delta at its real phase, then fade its amplitude. Scaling the phase
		// would turn a nearly finished neutral swing into a large mid-swing during recovery.
		PoseStack delta = new PoseStack();
		original.call(renderer, swing, partialTick, delta, side, arm);
		pose.mulPose(MastersViewMotion.fadeSwing(delta.last().pose(), ownership));
	}

	@Inject(method = "renderArmWithItem", at = @At("RETURN"))
	private void wildercord$auraEnd(AbstractClientPlayer player, float partial, float xRot, InteractionHand hand,
			float attack, ItemStack stack, float inverseArmHeight, PoseStack pose, MultiBufferSource buffer, int light, CallbackInfo ci) {
		AuraBlade.end();
		pose.popPose();
	}
}
