package dev.wildercord.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.aura.world.AuraWorldRules;
import dev.wildercord.aura.world.ForgedGear;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;

/**
 * Polearm stance and sweeping cleave animation for the Skyrend Glaive.
 *
 * <p>Because Skyrend Glaive is forged onto a spear base item, vanilla would otherwise hold it
 * one-handed vertically beside the ear ({@link HumanoidModel.ArmPose#SPEAR}) and poke straight
 * ahead ({@link SwingAnimationType#STAB}). A glaive is a long-shafted two-handed polearm: at
 * rest both hands grip the shaft diagonally across the body with the curved blade poised forward
 * and clear of the head, and attacks uncoil the torso in a wide horizontal-diagonal sweep.</p>
 */
public final class GlaivePose {
	private GlaivePose() {}

	private static final SwingAnimation GLAIVE_SWING = new SwingAnimation(SwingAnimationType.WHACK, 8);

	public static boolean isGlaive(ItemStack stack) {
		return stack != null && !stack.isEmpty() && ForgedGear.of(stack) == AuraWorldRules.Forged.SKYREND_GLAIVE;
	}

	private static boolean holdsGlaive(ArmedEntityRenderState state) {
		return state != null && isGlaive(state.getMainHandItemStack());
	}

	private static boolean eligible(AvatarRenderState state) {
		return state != null && !state.isSpectator && state.deathTime <= 0 && !state.isAutoSpinAttack
			&& !state.isFallFlying && !state.hasPose(Pose.SWIMMING) && !state.hasPose(Pose.SLEEPING)
			&& !state.isUpsideDown && !state.isPassenger;
	}

	/**
	 * Strips vanilla's one-handed javelin arm pose and stab swing description when the main hand
	 * carries Skyrend Glaive so third- and first-person renderers use polearm poses instead.
	 */
	public static void extract(Avatar avatar, AvatarRenderState state) {
		if (!holdsGlaive(state)) {
			return;
		}
		if (!state.isUsingItem) {
			if (state.mainArm == HumanoidArm.RIGHT && state.rightArmPose == HumanoidModel.ArmPose.SPEAR) {
				state.rightArmPose = HumanoidModel.ArmPose.ITEM;
			} else if (state.mainArm == HumanoidArm.LEFT && state.leftArmPose == HumanoidModel.ArmPose.SPEAR) {
				state.leftArmPose = HumanoidModel.ArmPose.ITEM;
			}
		}
		if (state.currentSwing != null && state.currentSwing.animation().type() == SwingAnimationType.STAB) {
			state.currentSwing = new LivingEntity.SwingDescription(state.currentSwing.hand(), GLAIVE_SWING);
		}
	}

	/**
	 * Applies the two-handed cross-body polearm guard at rest and a wide two-handed sweeping
	 * polearm cleave during an attack swing.
	 */
	public static boolean applyPlayer(PlayerModel model, AvatarRenderState state) {
		if (!holdsGlaive(state) || !eligible(state) || state.isUsingItem) {
			return false;
		}
		boolean leftHanded = state.mainArm == HumanoidArm.LEFT;
		float side = leftHanded ? -1.0F : 1.0F;
		ModelPart main = leftHanded ? model.leftArm : model.rightArm;
		ModelPart off = leftHanded ? model.rightArm : model.leftArm;
		ItemStack offStack = leftHanded ? state.rightHandItemStack : state.leftHandItemStack;
		boolean twoHanded = offStack == null || offStack.isEmpty();

		float pitch = Mth.clamp(model.head.xRot, -0.55F, 0.55F);
		float turn = Mth.clamp(model.head.yRot, -0.65F, 0.65F);
		float walkBob = Mth.cos(state.walkAnimationPos * 0.6662F) * 0.12F * state.walkAnimationSpeed;
		float breathe = Mth.sin(state.ageInTicks * 0.09F) * 0.025F;

		// Ready stance: main hand grips the lower-mid shaft at the hip/ribs; support hand reaches
		// across the chest to grip the upper-mid shaft so the polearm projects forward and upward.
		float idleMainX = -0.82F + pitch * 0.35F + walkBob + breathe;
		float idleMainY = -0.22F * side + turn * 0.35F;
		float idleMainZ = 0.22F * side;

		float idleOffX = -1.12F + pitch * 0.35F - walkBob * 0.5F + breathe;
		float idleOffY = 0.62F * side + turn * 0.35F;
		float idleOffZ = -0.14F * side;

		float s = state.swingAnimation;
		if (s <= 0.0F) {
			main.xRot = idleMainX;
			main.yRot = idleMainY;
			main.zRot = idleMainZ;
			if (twoHanded) {
				off.xRot = idleOffX;
				off.yRot = idleOffY;
				off.zRot = idleOffZ;
			}
			return true;
		}

		// Active attack: 3-phase sweeping polearm cleave (windup -> wide diagonal-horizontal slash -> recovery).
		float bodyYaw;
		float bodyPitch;
		float mainX, mainY, mainZ;
		float offX, offY, offZ;

		float chMainX = -1.32F + pitch * 0.25F;
		float chMainY = 0.54F * side;
		float chMainZ = 0.44F * side;
		float chOffX = -1.18F + pitch * 0.25F;
		float chOffY = 0.90F * side;
		float chOffZ = -0.18F * side;

		float cutMainX = -1.06F + pitch * 0.25F;
		float cutMainY = -0.82F * side;
		float cutMainZ = 0.28F * side;
		float cutOffX = -1.00F + pitch * 0.25F;
		float cutOffY = -0.24F * side;
		float cutOffZ = -0.34F * side;

		if (s < 0.22F) {
			float t = smooth(s / 0.22F);
			bodyYaw = Mth.lerp(t, 0.0F, 0.44F * side);
			bodyPitch = Mth.lerp(t, 0.0F, 0.08F);
			mainX = Mth.lerp(t, idleMainX, chMainX);
			mainY = Mth.lerp(t, idleMainY, chMainY);
			mainZ = Mth.lerp(t, idleMainZ, chMainZ);
			offX = Mth.lerp(t, idleOffX, chOffX);
			offY = Mth.lerp(t, idleOffY, chOffY);
			offZ = Mth.lerp(t, idleOffZ, chOffZ);
		} else if (s < 0.68F) {
			float t = smooth((s - 0.22F) / 0.46F);
			bodyYaw = Mth.lerp(t, 0.44F * side, -0.46F * side);
			bodyPitch = Mth.lerp(t, 0.08F, 0.16F);
			mainX = Mth.lerp(t, chMainX, cutMainX);
			mainY = Mth.lerp(t, chMainY, cutMainY);
			mainZ = Mth.lerp(t, chMainZ, cutMainZ);
			offX = Mth.lerp(t, chOffX, cutOffX);
			offY = Mth.lerp(t, chOffY, cutOffY);
			offZ = Mth.lerp(t, chOffZ, cutOffZ);
		} else {
			float t = smooth((s - 0.68F) / 0.32F);
			bodyYaw = Mth.lerp(t, -0.46F * side, 0.0F);
			bodyPitch = Mth.lerp(t, 0.16F, 0.0F);
			mainX = Mth.lerp(t, cutMainX, idleMainX);
			mainY = Mth.lerp(t, cutMainY, idleMainY);
			mainZ = Mth.lerp(t, cutMainZ, idleMainZ);
			offX = Mth.lerp(t, cutOffX, idleOffX);
			offY = Mth.lerp(t, cutOffY, idleOffY);
			offZ = Mth.lerp(t, cutOffZ, idleOffZ);
		}

		model.body.yRot = bodyYaw;
		model.body.xRot += bodyPitch;
		// Keep shoulder pivots attached to the rotating torso.
		float sinY = Mth.sin(bodyYaw);
		float cosY = Mth.cos(bodyYaw);
		model.rightArm.z = sinY * 5.0F;
		model.rightArm.x = -cosY * 5.0F;
		model.leftArm.z = -sinY * 5.0F;
		model.leftArm.x = cosY * 5.0F;

		main.xRot = mainX;
		main.yRot = mainY + bodyYaw;
		main.zRot = mainZ;
		if (twoHanded) {
			off.xRot = offX;
			off.yRot = offY + bodyYaw;
			off.zRot = offZ;
		}
		return true;
	}

	/**
	 * Orients the glaive in wrist-local space (around the handheld grip pivot) so that at rest the
	 * shaft passes diagonally across the chest through the support hand, and during a swing the
	 * long polearm extends forward and sweeps its curved cutting edge horizontally through the arc.
	 */
	public static void heldItem(ArmedEntityRenderState state, HumanoidArm arm, ItemStack item, PoseStack stack) {
		if (!(state instanceof AvatarRenderState avatar) || arm != state.mainArm || !isGlaive(item) || !eligible(avatar)) {
			return;
		}
		if (dev.wildercord.client.combat.ArticulatedCombat.frame(avatar) != null
			|| avatar.getData(MastersArtPose.FRAME) != null) {
			return;
		}
		float side = state.mainArm == HumanoidArm.LEFT ? -1.0F : 1.0F;
		// Idle polearm orientation: tilted forward away from the head and angled across toward the support hand.
		float idlePitch = -26.0F;
		float idleCross = -24.0F * side;
		float idleTwist = 12.0F * side;

		float pitch = idlePitch;
		float cross = idleCross;
		float twist = idleTwist;

		float s = state.swingAnimation;
		if (s > 0.0F) {
			float chPitch = -34.0F;
			float chCross = -10.0F * side;
			float chTwist = -22.0F * side;

			float cutPitch = -58.0F;
			float cutCross = 16.0F * side;
			float cutTwist = 56.0F * side;

			if (s < 0.22F) {
				float t = smooth(s / 0.22F);
				pitch = Mth.lerp(t, idlePitch, chPitch);
				cross = Mth.lerp(t, idleCross, chCross);
				twist = Mth.lerp(t, idleTwist, chTwist);
			} else if (s < 0.68F) {
				float t = smooth((s - 0.22F) / 0.46F);
				pitch = Mth.lerp(t, chPitch, cutPitch);
				cross = Mth.lerp(t, chCross, cutCross);
				twist = Mth.lerp(t, chTwist, cutTwist);
			} else {
				float t = smooth((s - 0.68F) / 0.32F);
				pitch = Mth.lerp(t, cutPitch, idlePitch);
				cross = Mth.lerp(t, cutCross, idleCross);
				twist = Mth.lerp(t, cutTwist, idleTwist);
			}
		}

		float y = -1.327F / 16.0F;
		float z = 1.439F / 16.0F;
		stack.translate(0.0F, y, z);
		stack.rotateDegrees(Axis.ZP, cross);
		stack.rotateDegrees(Axis.XP, pitch);
		stack.rotateDegrees(Axis.YP, twist);
		stack.translate(0.0F, -y, -z);
	}

	/**
	 * Replaces vanilla's first-person chop/poke with a wide diagonal-horizontal polearm cleave
	 * sweeping across the screen when swinging Skyrend Glaive.
	 */
	public static boolean firstPersonSwing(float attack, PoseStack pose, int side, HumanoidArm arm,
			AvatarRenderState state, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND || !holdsGlaive(state)) {
			return false;
		}
		if (attack <= 0.0F) {
			return true;
		}
		float s = side;
		float dx, dy, dz;
		float pitch, yaw, roll;

		if (attack < 0.20F) {
			float t = smooth(attack / 0.20F);
			dx = Mth.lerp(t, 0.0F, 0.14F * s);
			dy = Mth.lerp(t, 0.0F, 0.08F);
			dz = Mth.lerp(t, 0.0F, 0.06F);
			pitch = Mth.lerp(t, 0.0F, -14.0F);
			yaw = Mth.lerp(t, 0.0F, -20.0F * s);
			roll = Mth.lerp(t, 0.0F, -18.0F * s);
		} else if (attack < 0.68F) {
			float t = smooth((attack - 0.20F) / 0.48F);
			float arc = Mth.sin(t * Mth.PI);
			dx = Mth.lerp(t, 0.14F * s, -0.54F * s);
			dy = Mth.lerp(t, 0.08F, -0.12F) + 0.06F * arc;
			dz = Mth.lerp(t, 0.06F, -0.10F) - 0.24F * arc;
			pitch = Mth.lerp(t, -14.0F, -38.0F) - 14.0F * arc;
			yaw = Mth.lerp(t, -20.0F * s, 52.0F * s);
			roll = Mth.lerp(t, -18.0F * s, 54.0F * s);
		} else {
			float t = smooth((attack - 0.68F) / 0.32F);
			dx = Mth.lerp(t, -0.54F * s, 0.0F);
			dy = Mth.lerp(t, -0.12F, 0.0F);
			dz = Mth.lerp(t, -0.10F, 0.0F);
			pitch = Mth.lerp(t, -38.0F, 0.0F);
			yaw = Mth.lerp(t, 52.0F * s, 0.0F);
			roll = Mth.lerp(t, 54.0F * s, 0.0F);
		}

		pose.translate(dx, dy, dz);
		pose.rotateDegrees(Axis.YP, yaw);
		pose.rotateDegrees(Axis.XP, pitch);
		pose.rotateDegrees(Axis.ZP, roll);
		return true;
	}

	private static float smooth(float value) {
		float t = Mth.clamp(value, 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}
}
