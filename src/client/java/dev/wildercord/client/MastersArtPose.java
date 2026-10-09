package dev.wildercord.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.aura.MastersArtAnimation;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;

/** The accepted art's full-body and first-person animation, layered on the existing player rig. */
public final class MastersArtPose {
	private MastersArtPose() {}

	public record Frame(MastersArtAnimation.Pose pose, boolean leftHanded, float yawDelta, float pitchDelta, float bladeTilt, long activation, int move) {
		public boolean sameActivation(Frame other) { return other != null && activation == other.activation && move == other.move && leftHanded == other.leftHanded; }
	}
	public static final RenderStateDataKey<Frame> FRAME = RenderStateDataKey.create(() -> "wildercord:masters_art_pose");

	/** Captures an immutable frame before drawing; models never look back into the live entity. */
	public static void extract(Avatar avatar, AvatarRenderState state, float partial) {
		MastersArtAnimation.Pose pose = MastersArtsClient.pose(avatar, partial);
		var timeline = MastersArtsClient.timeline(avatar);
		state.setData(FRAME, null);
		if (!eligible(state)) return;
		var form = MasterFormsClient.timeline(avatar);
		if (form != null && timeline == null) {
			var event = form.event();
			var movement = dev.wildercord.aura.StoneHingeAnimation.sampleForm(event.phase(), event.ticks(), avatar.level().getGameTime() - form.received() + partial);
			if (movement.weight() > 0) {
				var facing = MastersArtAnimation.facing(state.bodyRot, state.yRot, state.xRot, event.yaw(), 0, movement.weight());
				state.bodyRot = facing.bodyYaw(); state.yRot = facing.headYaw();
				state.setData(FRAME, new Frame(movement, avatar.getMainArm() == HumanoidArm.LEFT, facing.yawDelta(), 0, 0, event.serial(), -2 - event.phase()));
				return;
			}
		}
		var field = FormDashClient.timeline(avatar);
		if (field != null && timeline == null) {
			var event = field.event();
			var movement = dev.wildercord.aura.FormDashAnimation.sample(event.form(), event.phase(), event.ticks(), avatar.level().getGameTime() - field.received() + partial);
			if (movement.weight() > 0) {
				var facing = MastersArtAnimation.facing(state.bodyRot, state.yRot, state.xRot, event.yaw(), 0, movement.weight());
				state.bodyRot = facing.bodyYaw(); state.yRot = facing.headYaw();
				// Below every Wall Turn move id, so the articulated rig keeps its whole Classic fallback here too.
				state.setData(FRAME, new Frame(movement, avatar.getMainArm() == HumanoidArm.LEFT, facing.yawDelta(), 0, 0, event.serial(), -20 - event.phase()));
				return;
			}
		}
		if (pose.weight() <= 0 || timeline == null) return;
		var facing = MastersArtAnimation.facing(state.bodyRot, state.yRot, state.xRot, timeline.yaw(), timeline.pitch(), pose.weight());
		state.bodyRot = facing.bodyYaw();
		state.yRot = facing.headYaw();
		float age = avatar.level().getGameTime() - timeline.startTick() + partial;
		state.setData(FRAME, new Frame(pose, avatar.getMainArm() == HumanoidArm.LEFT, facing.yawDelta(), facing.pitchDelta(),
			MastersArtAnimation.bladeTilt(timeline.move(), age, timeline.windup(), pose.weight()), timeline.startTick(), timeline.move()));
	}

	/** Returns true when this art owns the pose, ahead of an older spell-casting gesture. */
	public static boolean apply(PlayerModel model, AvatarRenderState state) {
		Frame frame = frame(state);
		if (frame == null) return false;
		MastersArtAnimation.Pose pose = frame.pose();
		float weight = pose.weight();
		float side = frame.leftHanded() ? -1 : 1;
		ModelPart sword = frame.leftHanded() ? model.leftArm : model.rightArm;
		ModelPart guard = frame.leftHanded() ? model.rightArm : model.leftArm;
		ModelPart front = frame.leftHanded() ? model.rightLeg : model.leftLeg;
		ModelPart rear = frame.leftHanded() ? model.leftLeg : model.rightLeg;
		joint(model.body, pose.body(), side, weight);
		// Keep the fighter looking at the target while the shoulders wind across it.
		model.head.xRot += pose.head().x() * weight;
		model.head.yRot += pose.head().y() * side * weight;
		model.head.zRot += pose.head().z() * side * weight;
		var head = MastersArtAnimation.boundedHead(new MastersArtAnimation.Joint(model.body.xRot, model.body.yRot, model.body.zRot),
			new MastersArtAnimation.Joint(model.head.xRot, model.head.yRot, model.head.zRot), weight);
		model.head.xRot = head.x();
		model.head.yRot = head.y();
		model.head.zRot = head.z();
		joint(sword, pose.sword(), side, weight);
		joint(guard, pose.guard(), side, weight);
		joint(front, pose.frontLeg(), side, weight);
		joint(rear, pose.rearLeg(), side, weight);
		// These are sibling parts. Hinge the torso at the hips and carry its neck/shoulders with it;
		// a rotation at the default upper-body origin would separate a deep lean from the legs.
		for (ModelPart part : new ModelPart[] {model.body, model.head, model.rightArm, model.leftArm}) {
			var initial = part.getInitialPose();
			var target = MastersArtAnimation.pivot(pose, initial.x(), initial.y(), initial.z(), frame.leftHanded());
			part.x = Mth.lerp(weight, part.x, target.x());
			part.y = Mth.lerp(weight, part.y, target.y());
			part.z = Mth.lerp(weight, part.z, target.z());
		}
		for (ModelPart part : new ModelPart[] {model.rightLeg, model.leftLeg}) {
			part.y = Mth.lerp(weight, part.y, part.getInitialPose().y() + pose.lower());
			part.z = Mth.lerp(weight, part.z, part.getInitialPose().z() + pose.forward());
		}
		// In 26.3 the hat, jacket, sleeves and trouser overlays are children of these limbs.
		// They inherit the changed transforms; copying the parent pose would apply every turn twice.
		return true;
	}

	/** The same extracted palette owns residual attack motion; genuine equip/use retains vanilla. */
	public static float firstPersonOwnership(InteractionHand hand, AvatarRenderState state,
			net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState hands) {
		Frame frame = frame(state);
		boolean owns = hand == InteractionHand.MAIN_HAND && frame != null
			&& hands instanceof MastersHandMotionState motion && !motion.wildercord$mainHandEquipping()
			&& !hands.mainHandItem.isEmpty()
			&& net.minecraft.world.item.ItemStack.isSameItemSameComponents(hands.mainHandItem, state.getMainHandItemStack());
		return dev.wildercord.aura.MastersViewMotion.ownership(frame == null ? 0 : frame.pose().weight(), owns);
	}

	/** Main-hand weapon travel follows the same accepted timeline, mirrored for left-handed play. */
	public static void firstPerson(PoseStack stack, InteractionHand hand, AvatarRenderState state, float inverseArmHeight) {
		Frame frame = frame(state);
		if (hand != InteractionHand.MAIN_HAND || frame == null) return;
		MastersArtAnimation.Pose pose = frame.pose();
		var view = MastersArtAnimation.view(pose, frame.leftHanded(), inverseArmHeight, frame.yawDelta(), frame.pitchDelta());
		var move = view.transform();
		var grip = view.grip();
		stack.translate(move.x(), move.y(), move.z());
		// Vanilla adds the hand translation after this hook. Conjugation turns about that hilt,
		// rather than rotating the entire hand behind the camera during a high guard or uppercut.
		stack.translate(grip.x(), grip.y(), grip.z());
		stack.rotateDegrees(Axis.YP, move.yaw());
		stack.rotateDegrees(Axis.XP, move.pitch());
		stack.rotateDegrees(Axis.ZP, move.roll());
		stack.translate(-grip.x(), -grip.y(), -grip.z());
	}

	/** The rigid arm cannot bend its wrist, so thrusts and low cuts turn the held sword about its hilt. */
	public static void heldSword(net.minecraft.client.renderer.entity.state.ArmedEntityRenderState state, HumanoidArm arm,
			net.minecraft.world.item.ItemStack item, PoseStack stack) {
		if (!(state instanceof AvatarRenderState avatar) || arm != state.mainArm || !item.is(net.minecraft.tags.ItemTags.SWORDS)) return;
		Frame frame = frame(avatar);
		if (dev.wildercord.client.combat.ArticulatedCombat.frame(avatar) != null || frame == null || frame.bladeTilt() == 0) return;
		// Vanilla handheld display's hilt centre, in the pre-item-submit coordinate frame.
		float y = -1.327F / 16, z = 1.439F / 16;
		stack.translate(0, y, z);
		stack.rotateDegrees(Axis.XP, frame.bladeTilt());
		stack.translate(0, -y, -z);
	}

	private static Frame frame(AvatarRenderState state) {
		return eligible(state) ? state.getData(FRAME) : null;
	}

	private static boolean eligible(AvatarRenderState state) {
		return state != null && !state.isSpectator && state.deathTime <= 0 && !state.isAutoSpinAttack && !state.isFallFlying
			&& !state.hasPose(Pose.SWIMMING) && !state.hasPose(Pose.SLEEPING) && !state.isUpsideDown;
	}

	private static void joint(ModelPart part, MastersArtAnimation.Joint joint, float side, float weight) {
		part.xRot = Mth.lerp(weight, part.xRot, joint.x());
		part.yRot = Mth.lerp(weight, part.yRot, joint.y() * side);
		part.zRot = Mth.lerp(weight, part.zRot, joint.z() * side);
	}
}
