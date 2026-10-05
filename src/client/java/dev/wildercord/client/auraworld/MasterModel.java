package dev.wildercord.client.auraworld;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.aura.world.MasterAnimationRules;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/** The authored travelling rig, with original, server-timed master combat rather than a held windup. */
public final class MasterModel extends HumanoidModel<AuraFighterRenderState> {
	public record Frame(MasterAnimationRules.Pose pose, boolean leftHanded) {}
	public static final RenderStateDataKey<Frame> FRAME = RenderStateDataKey.create(() -> "wildercord:master_motion");
	private final ModelPart root, cloak, hilt;

	public MasterModel(ModelPart root) {
		super(root);
		this.root = root;
		cloak = body.getChild("cloak");
		hilt = body.getChild("scabbard").getChild("hilt");
	}

	@Override
	public void setupAnim(AuraFighterRenderState state) {
		// Models are reused between entities and render passes. Reset accessories as well as limbs,
		// and explicitly restore hilt visibility, so cancellation/reuse cannot retain a combat pose.
		root.getAllParts().forEach(ModelPart::resetPose);
		hilt.visible = state.drawn < .5F;
		super.setupAnim(state);
		cloak.xRot = .06F + Math.min(1, state.walkAnimationSpeed) * .45F + Mth.sin(state.ageInTicks * .07F) * .03F;
		if (state.deathTime > 0 || state.isUpsideDown) return;
		Frame frame = state.getData(FRAME);
		boolean left = frame != null ? frame.leftHanded() : state.mainArm == HumanoidArm.LEFT;
		ModelPart sword = left ? leftArm : rightArm;
		ModelPart offhand = left ? rightArm : leftArm;
		float side = left ? -1 : 1;
		float ready = state.drawn;
		sword.xRot = Mth.lerp(ready, sword.xRot, -.58F + sword.xRot * .15F);
		sword.yRot = Mth.lerp(ready, sword.yRot, -.12F * side);
		offhand.xRot = Mth.lerp(ready * .4F, offhand.xRot, -.22F);
		if (frame == null || frame.pose().weight() <= 0) return;
		var pose = frame.pose();
		float weight = pose.weight();
		joint(body, pose.body(), left, weight);
		joint(sword, pose.sword(), left, weight);
		joint(offhand, pose.offhand(), left, weight);
		head.xRot += pose.head().x() * weight;
		head.yRot += pose.head().y() * side * weight;
		head.zRot += pose.head().z() * side * weight;
		ModelPart front = left ? rightLeg : leftLeg, rear = left ? leftLeg : rightLeg;
		front.xRot = Mth.lerp(weight, front.xRot, -pose.stance());
		rear.xRot = Mth.lerp(weight, rear.xRot, pose.stance());
		front.yRot *= 1 - weight;
		rear.yRot *= 1 - weight;
		front.zRot *= 1 - weight;
		rear.zRot *= 1 - weight;
		float lower = MasterAnimationRules.lower(pose.stance() * weight);
		// Body and limbs are siblings. Hinge at the hips, carry each shoulder/neck along, and leave
		// the legs below that hinge. Head and arms retain their independently authored global angles.
		var rotation = new MasterAnimationRules.Joint(body.xRot, body.yRot, body.zRot);
		anchor(body, rotation, lower);
		anchor(head, rotation, lower);
		anchor(rightArm, rotation, lower);
		anchor(leftArm, rotation, lower);
		rightLeg.y = rightLeg.getInitialPose().y() + lower;
		leftLeg.y = leftLeg.getInitialPose().y() + lower;
		cloak.xRot += (.18F + Math.abs(body.yRot) * .12F) * weight;
		// Hat, mantle, cloak and scabbard are children; applying the torso twice would detach them.
	}

	/** Uses vanilla's held-item attachment and only rotates the blade around its existing hilt. */
	public static void heldSword(ArmedEntityRenderState state, HumanoidArm arm, ItemStack item, PoseStack stack) {
		if (!(state instanceof AuraFighterRenderState fighter) || fighter.deathTime > 0 || fighter.isUpsideDown
			|| arm != state.mainArm || !item.is(ItemTags.SWORDS)) return;
		Frame frame = state.getData(FRAME);
		if (frame == null) return;
		float tilt = frame.pose().bladeTilt() * frame.pose().weight();
		if (tilt == 0) return;
		// The vanilla handheld display's hilt centre before ItemStackRenderState.submit.
		float y = -1.327F / 16, z = 1.439F / 16;
		stack.translate(0, y, z);
		stack.rotateDegrees(Axis.XP, tilt);
		stack.translate(0, -y, -z);
	}

	private static void anchor(ModelPart part, MasterAnimationRules.Joint body, float lower) {
		var initial = part.getInitialPose();
		var pivot = MasterAnimationRules.pivot(body, lower, initial.x(), initial.y(), initial.z());
		part.x = pivot.x();
		part.y = pivot.y();
		part.z = pivot.z();
	}

	private static void joint(ModelPart part, MasterAnimationRules.Joint joint, boolean left, float weight) {
		var target = MasterAnimationRules.mirrored(joint, left);
		part.xRot = Mth.lerp(weight, part.xRot, target.x());
		part.yRot = Mth.lerp(weight, part.yRot, target.y());
		part.zRot = Mth.lerp(weight, part.zRot, target.z());
	}
}
