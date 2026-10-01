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
 * A rimehare: a crouched body carried nose-up, a round head with a blunt muzzle and two very long ears, strong
 * haunches over long hind feet, slim forelegs and a puff of a tail. On the ground it twitches its nose and swivels its
 * ears; on alert it sits up tall; in a bound it stretches out (hind feet thrown back, forelegs reaching, ears laid
 * flat) and gathers itself again to land. A leveret has a bigger head. UVs match {@code rimehare_skin} in
 * tools/wildlife_art.py.
 */
public final class RimehareModel extends EntityModel<WildlifeRenderState> {
	private final ModelPart body, head, muzzle, leftEar, rightEar, leftHaunch, rightHaunch, leftFoot, rightFoot, frontLeft, frontRight, tail;

	private static final float BODY_REST = -0.22F;

	public RimehareModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = root.getChild("head");
		muzzle = head.getChild("muzzle");
		leftEar = head.getChild("left_ear");
		rightEar = head.getChild("right_ear");
		leftHaunch = root.getChild("left_haunch");
		rightHaunch = root.getChild("right_haunch");
		leftFoot = leftHaunch.getChild("left_foot");
		rightFoot = rightHaunch.getChild("right_foot");
		frontLeft = root.getChild("front_left_leg");
		frontRight = root.getChild("front_right_leg");
		tail = root.getChild("tail");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.5F, -4, 5, 5, 8),
			PartPose.offsetAndRotation(0, 18.5F, 1, BODY_REST, 0, 0));
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(26, 0).addBox(-2, -3.5F, -3.5F, 4, 4, 4),
			PartPose.offset(0, 15.5F, -3));
		head.addOrReplaceChild("muzzle", CubeListBuilder.create().texOffs(42, 0).addBox(-1.5F, -1.5F, -4.5F, 3, 2, 2), PartPose.ZERO);
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(52, 0).addBox(-1, -6, -0.5F, 2, 6, 1),
			PartPose.offsetAndRotation(1.1F, -3.3F, -1.0F, 0.35F, 0, 0.18F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(52, 0).mirror().addBox(-1, -6, -0.5F, 2, 6, 1),
			PartPose.offsetAndRotation(-1.1F, -3.3F, -1.0F, 0.35F, 0, -0.18F));
		haunch(root, "left", 2.1F, false);
		haunch(root, "right", -2.1F, true);
		frontLeg(root, "front_left", 1.2F, false);
		frontLeg(root, "front_right", -1.2F, true);
		root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(36, 13).addBox(-1.5F, -1.5F, 0, 3, 3, 2), PartPose.offset(0, 17.5F, 4.6F));
		return LayerDefinition.create(mesh, 64, 32);
	}

	private static void haunch(PartDefinition root, String side, float x, boolean mirror) {
		CubeListBuilder thigh = CubeListBuilder.create().texOffs(0, 13);
		CubeListBuilder foot = CubeListBuilder.create().texOffs(14, 13);
		if (mirror) {
			thigh.mirror();
			foot.mirror();
		}
		PartDefinition haunch = root.addOrReplaceChild(side + "_haunch", thigh.addBox(-1, -2, -2.5F, 2, 4, 5), PartPose.offset(x, 19.5F, 3));
		haunch.addOrReplaceChild(side + "_foot", foot.addBox(-1, 0, -3.5F, 2, 1, 5), PartPose.offset(0, 3.5F, 0));
	}

	private static void frontLeg(PartDefinition root, String name, float x, boolean mirror) {
		CubeListBuilder cube = CubeListBuilder.create().texOffs(28, 13);
		if (mirror) {
			cube.mirror();
		}
		root.addOrReplaceChild(name + "_leg", cube.addBox(-1, 0, -1, 2, 5, 2), PartPose.offset(x, 19, -2.2F));
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float air = state.air;
		float alert = state.alert * (1 - air);
		float speed = Math.min(1, state.walkAnimationSpeed);

		head.yRot = Mth.clamp(state.yRot, -50, 50) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.7F;

		// Its nose never stops; its ears turn to every sound.
		float sniff = Mth.clamp(Mth.sin(t * 0.17F) * 3 - 1.5F, 0, 1);
		muzzle.y = Mth.sin(t * 2.1F) * 0.18F * sniff;
		leftEar.zRot = 0.18F + Mth.sin(t * 0.05F) * 0.08F;
		rightEar.zRot = -0.18F + Mth.sin(t * 0.06F + 1.3F) * 0.08F;
		leftEar.xRot = rightEar.xRot = 0.35F - alert * 0.35F + air * 0.8F + speed * 0.3F;

		// On alert it sits up tall, forepaws lifted.
		body.xRot = BODY_REST - alert * 0.6F;
		body.y = 18.5F - alert * 1.2F;
		head.y -= alert * 2.4F;
		head.z = -3 + alert * 0.8F;
		frontLeft.xRot = frontRight.xRot = -alert * 0.5F;
		frontLeft.y = frontRight.y = 19 - alert * 1.5F;
		frontLeft.z = frontRight.z = -2.2F + alert * 0.6F;

		// In the air: stretched long, hind feet thrown back, forelegs reaching, nose up as it rises and down as it comes in.
		if (air > 0) {
			float stretch = air;
			body.xRot = Mth.lerp(stretch, body.xRot, -0.05F + state.rise * 0.18F);
			leftHaunch.xRot = rightHaunch.xRot = stretch * (0.85F + state.rise * 0.2F);
			leftFoot.xRot = rightFoot.xRot = stretch * 0.7F;
			frontLeft.xRot = frontRight.xRot = -stretch * (0.7F - state.rise * 0.35F);
			tail.xRot = stretch * 0.4F;
			head.xRot += state.rise * 0.15F * stretch;
		} else {
			// Gathered between bounds: forepaws tucked back under it.
			frontLeft.xRot += speed * 0.35F;
			frontRight.xRot += speed * 0.35F;
			tail.xRot = Mth.sin(t * 0.3F) * 0.08F;
		}

		if (state.isBaby) {
			head.xScale = head.yScale = head.zScale = 1.35F;
			head.z -= 0.4F;
		}
	}
}
