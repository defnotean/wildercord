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
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
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
	@Unique private AvatarRenderState wildercord$avatar;

	@Override
	public AvatarRenderState wildercord$handsAvatar(AbstractClientPlayer player, float partial) {
		return wildercord$state(player, partial);
	}

	@Override
	public void wildercord$prepareHandsAvatar(AbstractClientPlayer player, float partial) {
		wildercord$avatar = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(player, partial) instanceof AvatarRenderState avatar
			? avatar : null;
	}

	@Override
	public void wildercord$clearHandsAvatar() {
		wildercord$avatar = null;
	}

	@Unique private AvatarRenderState wildercord$state(AbstractClientPlayer player, float partial) {
		if (wildercord$avatar != null) return wildercord$avatar;
		if (Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(player, partial) instanceof AvatarRenderState avatar) {
			return avatar;
		}
		return null;
	}

	@Inject(method = "submitHandsWithItems", at = @At("HEAD"))
	private void wildercord$extractHandsAvatar(float partial, PoseStack pose, SubmitNodeCollector collector, LocalPlayer player, int light, CallbackInfo ci) {
		if (wildercord$avatar == null) wildercord$prepareHandsAvatar(player, partial);
	}

	@Inject(method = "submitHandsWithItems", at = @At("RETURN"))
	private void wildercord$clearHandsAvatar(float partial, PoseStack pose, SubmitNodeCollector collector, LocalPlayer player, int light, CallbackInfo ci) {
		wildercord$avatar = null;
	}

	@Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
	private void wildercord$auraBegin(AbstractClientPlayer player, float partial, float xRot, InteractionHand hand,
			float attack, ItemStack stack, float inverseArmHeight, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		ItemInHandRenderer renderer = (ItemInHandRenderer) (Object) this;
		AvatarRenderState avatar = wildercord$state(player, partial);
		if (avatar != null && dev.wildercord.client.combat.ArticulatedCombat.viewFrame(avatar) != null) {
			AuraBlade.beginFirstPerson(hand == InteractionHand.MAIN_HAND, stack);
			boolean rendered;
			try {
				rendered = dev.wildercord.client.combat.ArticulatedViewModel.submit(avatar, renderer, partial, hand, pose, collector, light);
			} finally {
				AuraBlade.end();
			}
			if (rendered) { ci.cancel(); return; }
		}
		pose.pushPose();
		float ownership = MastersArtPose.firstPersonOwnership(hand, avatar, renderer);
		// The same-item attack ticker lowers the vanilla grip just like an equip. Compensate only
		// its owned fraction in camera space, keeping the original grip conjugation and argument.
		if (ownership > 0) pose.translate(0, MastersViewMotion.heightCompensation(inverseArmHeight, ownership), 0);
		dev.wildercord.client.MastersArtPose.firstPerson(pose, hand, avatar, inverseArmHeight);
		AuraBlade.beginFirstPerson(hand == InteractionHand.MAIN_HAND, stack);
	}

	@WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"))
	private void wildercord$blendAttack(ItemInHandRenderer renderer, float attack, PoseStack pose, int side, HumanoidArm arm,
			Operation<Void> original, @Local(argsOnly = true) AbstractClientPlayer player,
			@Local(argsOnly = true) float partial, @Local(argsOnly = true) InteractionHand hand) {
		AvatarRenderState avatar = wildercord$state(player, partial);
		float ownership = MastersArtPose.firstPersonOwnership(hand, avatar, renderer);
		if (ownership <= 0) {
			if (avatar != null && dev.wildercord.client.GlaivePose.firstPersonSwing(attack, pose, side, arm, avatar, hand)) return;
			original.call(renderer, attack, pose, side, arm);
			return;
		}
		if (ownership >= 1) return;
		// Capture the native delta at its real phase, then fade its amplitude. Scaling the phase
		// would turn a nearly finished neutral swing into a large mid-swing during recovery.
		PoseStack delta = new PoseStack();
		original.call(renderer, attack, delta, side, arm);
		pose.mulPose(MastersViewMotion.fadeSwing(delta.last().pose(), ownership));
	}

	@Inject(method = "submitArmWithItem", at = @At("RETURN"))
	private void wildercord$auraEnd(AbstractClientPlayer player, float partial, float xRot, InteractionHand hand,
			float attack, ItemStack stack, float inverseArmHeight, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		AuraBlade.end();
		pose.popPose();
	}
}
