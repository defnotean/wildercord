package dev.wildercord.client.wildlife;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A blossom tanuki: a plump round body on four short legs, a round head with a dark mask, a short pointed snout and two
 * small round ears, and a big bushy ringed tail hanging low behind. It trots with short quick steps, its tail swinging.
 * Startled ({@link WildlifeRenderState#hold}) it freezes with a cherry leaf on its head; turned to a bush
 * ({@link WildlifeRenderState#hide}) it isn't drawn at all, the bush is (see {@link BlossomTanukiRenderer}). Under a full moon
 * ({@link WildlifeRenderState#dance}) it sits up on its haunches and drums its round belly with its forepaws, its head
 * nodding to the beat. UVs match tools/tanuki_art.py.
 */
public final class BlossomTanukiModel extends EntityModel<WildlifeRenderState> {
	private static final float BODY_Y = 21, BODY_Z = 4;

	private final ModelPart body, head, leaf, leftEar, rightEar, tail, leftArm, rightArm, leftLeg, rightLeg;

	public BlossomTanukiModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = body.getChild("head");
		leaf = head.getChild("leaf");
		leftEar = head.getChild("left_ear");
		rightEar = head.getChild("right_ear");
		tail = body.getChild("tail");
		leftArm = body.getChild("left_arm");
		rightArm = body.getChild("right_arm");
		leftLeg = root.getChild("left_leg");
		rightLeg = root.getChild("right_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// The body pivots at its rear, so sitting up it rocks back onto its haunches.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -5, -8, 6, 5, 8),
			PartPose.offset(0, BODY_Y, BODY_Z));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 0).addBox(-2.5F, -3, -3.5F, 5, 4, 4),
			PartPose.offset(0, -3.5F, -8));
		head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(46, 0).addBox(-1.5F, -1, -2, 3, 2, 2), PartPose.offset(0, 0, -3.5F));
		head.addOrReplaceChild("leaf", CubeListBuilder.create().texOffs(46, 5).addBox(-2, 0, -2, 4, 0, 4),
			PartPose.offsetAndRotation(0, -3.05F, -1.5F, 0.2F, 0, 0));
		for (int side = -1; side <= 1; side += 2) {
			String name = side > 0 ? "left" : "right";
			head.addOrReplaceChild(name + "_ear", CubeListBuilder.create().texOffs(56, 0).mirror(side < 0).addBox(-1, -2, -0.5F, 2, 2, 1),
				PartPose.offsetAndRotation(side * 1.6F, -2.8F, -1, 0, 0, side * 0.2F));
			body.addOrReplaceChild(name + "_arm", CubeListBuilder.create().texOffs(20, 14).mirror(side < 0).addBox(-1, 0, -1, 2, 3, 2),
				PartPose.offset(side * 1.8F, 0, -6.5F));
			root.addOrReplaceChild(name + "_leg", CubeListBuilder.create().texOffs(20, 14).mirror(side < 0).addBox(-1, 0, -1, 2, 3, 2),
				PartPose.offset(side * 1.8F, BODY_Y, 2.5F));
		}
		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 14).addBox(-1.5F, -1.5F, 0, 3, 3, 7),
			PartPose.offsetAndRotation(0, -3.5F, 0, 0.45F, 0, 0));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		root().visible = state.hide < 0.5F;
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed * 2);
		float step = state.walkAnimationPos * 2.0F;
		float drum = state.dance;
		float freeze = state.hold;

		// A quick little trot, the body bobbing and the tail swinging.
		leftLeg.xRot = Mth.sin(step) * 1.0F * speed;
		rightLeg.xRot = Mth.sin(step + Mth.PI) * 1.0F * speed;
		leftArm.xRot = Mth.sin(step + Mth.PI) * 1.0F * speed;
		rightArm.xRot = Mth.sin(step) * 1.0F * speed;
		leftArm.yRot = rightArm.yRot = 0;
		body.y = BODY_Y - Math.abs(Mth.sin(step)) * 0.4F * speed;
		body.xRot = 0;
		tail.xRot = 0.45F + Mth.sin(t * 0.07F) * 0.05F - 0.25F * speed;
		tail.yRot = Mth.sin(step * 0.5F) * 0.3F * speed + Mth.sin(t * 0.05F) * 0.12F * (1 - speed);

		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.zRot = 0;
		float flick = Mth.clamp(Mth.sin(t * 0.05F) * 6 - 5, 0, 1);
		leftEar.zRot = 0.2F + flick * 0.3F * Mth.sin(t * 1.4F);
		rightEar.zRot = -0.2F;

		// The leaf: it slaps it on its head and holds dead still, ears flat, tail tucked.
		leaf.visible = freeze > 0.05F;
		leaf.xScale = leaf.zScale = freeze;
		if (freeze > 0) {
			leftEar.zRot = Mth.lerp(freeze, leftEar.zRot, 0.7F);
			rightEar.zRot = Mth.lerp(freeze, rightEar.zRot, -0.7F);
			tail.xRot = Mth.lerp(freeze, tail.xRot, 0.9F);
			body.y = Mth.lerp(freeze, body.y, BODY_Y + 0.8F);
		}

		if (drum > 0) {
			// Sat up on its haunches, drumming its belly with its forepaws, its head nodding to the beat.
			float beat = t * Mth.TWO_PI / 16;
			body.xRot = -1.15F * drum;
			head.xRot = Mth.lerp(drum, head.xRot, 1.15F + Mth.sin(beat * 2) * 0.08F - 0.2F);
			head.zRot = Mth.sin(beat) * 0.12F * drum;
			leftArm.xRot = Mth.lerp(drum, leftArm.xRot, -1.0F - Math.max(0, Mth.sin(beat)) * 0.7F);
			rightArm.xRot = Mth.lerp(drum, rightArm.xRot, -1.0F - Math.max(0, -Mth.sin(beat)) * 0.7F);
			leftArm.yRot = 0.35F * drum;
			rightArm.yRot = -0.35F * drum;
			leftLeg.xRot = Mth.lerp(drum, leftLeg.xRot, -1.3F);
			rightLeg.xRot = Mth.lerp(drum, rightLeg.xRot, -1.3F);
			tail.xRot = Mth.lerp(drum, tail.xRot, 1.45F);
		}

		if (state.isBaby) {
			head.xScale = head.yScale = head.zScale = 1.35F;
		} else {
			head.xScale = head.yScale = head.zScale = 1;
		}
	}
}
