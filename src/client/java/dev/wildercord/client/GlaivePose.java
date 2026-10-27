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

	private static final SwingAnimation GLAIVE_SWING = new SwingAnimation(SwingAnimationType.WHACK, 10);

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
			state.currentSwing = new LivingEntity.SwingDescription(state.currentSwing.hand(), GLAIVE_SWING, 10);
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

		// Ready stance: main hand grips the rear shaft wrap at the right hip/waist; support hand reaches
		// across the chest to grip the forward shaft wrap with visible exposed pole shaft between both hands.
		float idleMainX = -0.34F + pitch * 0.35F + walkBob + breathe;
		float idleMainY = 0.14F * side + turn * 0.35F;
		float idleMainZ = 0.16F * side;

		float idleOffX = -1.06F + pitch * 0.35F - walkBob * 0.5F + breathe;
		float idleOffY = 0.44F * side + turn * 0.35F;
		float idleOffZ = -0.08F * side;

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

		// Active attack: 3-phase sweeping polearm cleave (windup -> wide horizontal-diagonal slash -> recovery).
		float bodyYaw;
		float bodyPitch;
		float mainX, mainY, mainZ;
		float offX, offY, offZ;

		// Windup: chamber the blade higher on the right before uncoiling.
		float chMainX = -0.30F + pitch * 0.25F;
		float chMainY = 0.06F * side;
		float chMainZ = 0.06F * side;
		float chOffX = -1.18F + pitch * 0.25F;
		float chOffY = 0.54F * side;
		float chOffZ = -0.06F * side;

		// Peak cleave: rear hand levers up to chest height while lead hand drives the forward shaft
		// down and across in a wide horizontal-diagonal polearm slash, keeping both hands on the shaft.
		float cutMainX = -0.84F + pitch * 0.25F;
		float cutMainY = -0.52F * side;
		float cutMainZ = -0.06F * side;
		float cutOffX = -0.68F + pitch * 0.25F;
		float cutOffY = -0.34F * side;
		float cutOffZ = -0.06F * side;

		if (s < 0.12F) {
			float t = smooth(s / 0.12F);
			bodyYaw = Mth.lerp(t, 0.0F, -0.16F * side);
			bodyPitch = Mth.lerp(t, 0.0F, 0.05F);
			mainX = Mth.lerp(t, idleMainX, chMainX);
			mainY = Mth.lerp(t, idleMainY, chMainY);
			mainZ = Mth.lerp(t, idleMainZ, chMainZ);
			offX = Mth.lerp(t, idleOffX, chOffX);
			offY = Mth.lerp(t, idleOffY, chOffY);
			offZ = Mth.lerp(t, idleOffZ, chOffZ);
		} else if (s < 0.72F) {
			float t = smooth(Mth.clamp((s - 0.12F) / 0.22F, 0.0F, 1.0F));
			bodyYaw = Mth.lerp(t, -0.16F * side, 0.34F * side);
			bodyPitch = Mth.lerp(t, 0.05F, 0.18F);
			mainX = Mth.lerp(t, chMainX, cutMainX);
			mainY = Mth.lerp(t, chMainY, cutMainY);
			mainZ = Mth.lerp(t, chMainZ, cutMainZ);
			offX = Mth.lerp(t, chOffX, cutOffX);
			offY = Mth.lerp(t, chOffY, cutOffY);
			offZ = Mth.lerp(t, chOffZ, cutOffZ);
		} else {
			float t = smooth((s - 0.72F) / 0.28F);
			bodyYaw = Mth.lerp(t, 0.34F * side, 0.0F);
			bodyPitch = Mth.lerp(t, 0.18F, 0.0F);
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
		// Positive pitch in submit-space lifts the polearm upward away from the camera axis (-Z_entity);
		// positive cross tilts the shaft inward across the torso (-X_submit = +X_entity) through the support hand.
		float idlePitch = 56.0F;
		float idleCross = 46.0F * side;
		float idleTwist = 18.0F * side;

		float pitch = idlePitch;
		float cross = idleCross;
		float twist = idleTwist;

		float s = state.swingAnimation;
		if (s > 0.0F) {
			float chPitch = 47.0F;
			float chCross = 35.0F * side;
			float chTwist = 6.0F * side;

			float cutPitch = -74.0F;
			float cutCross = 59.0F * side;
			float cutTwist = -70.0F * side;

			if (s < 0.12F) {
				float t = smooth(s / 0.12F);
				pitch = Mth.lerp(t, idlePitch, chPitch);
				cross = Mth.lerp(t, idleCross, chCross);
				twist = Mth.lerp(t, idleTwist, chTwist);
			} else if (s < 0.72F) {
				float t = smooth(Mth.clamp((s - 0.12F) / 0.22F, 0.0F, 1.0F));
				pitch = Mth.lerp(t, chPitch, cutPitch);
				cross = Mth.lerp(t, chCross, cutCross);
				twist = Mth.lerp(t, chTwist, cutTwist);
			} else {
				float t = smooth((s - 0.72F) / 0.28F);
				pitch = Mth.lerp(t, cutPitch, idlePitch);
				cross = Mth.lerp(t, cutCross, idleCross);
				twist = Mth.lerp(t, cutTwist, idleTwist);
			}
		}

		float y = -1.327F / 16.0F;
		float z = 1.439F / 16.0F;
		stack.translate(0.0F, y, z);
		stack.rotateDegrees(Axis.XP, pitch);
		stack.rotateDegrees(Axis.ZP, cross);
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

		if (attack < 0.12F) {
			float t = smooth(attack / 0.12F);
			dx = Mth.lerp(t, 0.0F, 0.10F * s);
			dy = Mth.lerp(t, 0.0F, 0.06F);
			dz = Mth.lerp(t, 0.0F, 0.04F);
			pitch = Mth.lerp(t, 0.0F, 8.0F);
			yaw = Mth.lerp(t, 0.0F, -12.0F * s);
			roll = Mth.lerp(t, 0.0F, -10.0F * s);
		} else if (attack < 0.72F) {
			float t = smooth(Mth.clamp((attack - 0.12F) / 0.22F, 0.0F, 1.0F));
			float arc = Mth.sin(t * Mth.PI);
			dx = Mth.lerp(t, 0.10F * s, -0.32F * s);
			dy = Mth.lerp(t, 0.06F, 0.16F) + 0.04F * arc;
			dz = Mth.lerp(t, 0.04F, -0.10F) - 0.06F * arc;
			pitch = Mth.lerp(t, 8.0F, -14.0F) - 4.0F * arc;
			yaw = Mth.lerp(t, -12.0F * s, 22.0F * s);
			roll = Mth.lerp(t, -10.0F * s, 58.0F * s);
		} else {
			float t = smooth((attack - 0.72F) / 0.28F);
			dx = Mth.lerp(t, -0.32F * s, 0.0F);
			dy = Mth.lerp(t, 0.16F, 0.0F);
			dz = Mth.lerp(t, -0.10F, 0.0F);
			pitch = Mth.lerp(t, -14.0F, 0.0F);
			yaw = Mth.lerp(t, 22.0F * s, 0.0F);
			roll = Mth.lerp(t, 58.0F * s, 0.0F);
		}

		pose.translate(dx, dy, dz);
		pose.rotateDegrees(Axis.ZP, roll);
		pose.rotateDegrees(Axis.YP, yaw);
		pose.rotateDegrees(Axis.XP, pitch);
		return true;
	}

	private static float smooth(float value) {
		float t = Mth.clamp(value, 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}
}
