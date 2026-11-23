package dev.wildercord.client.wildlife;

import dev.wildercord.wildlife.WhiteRabbit;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Custom 3D voxel model for {@link WhiteRabbit}: crouched snow-white body, perked tall ears with
 * rosy linings, strong hind haunches, fluffy tail, crimson neck collar ribbon, and a suspended brass
 * pocket watch at its chest that it glances down at while waiting for its follower.
 * UVs match {@code tools/white_rabbit_art.py} (64x32 texture sheet).
 */
public final class WhiteRabbitModel extends EntityModel<WhiteRabbitRenderState> {
	private final ModelPart body;
	private final ModelPart collar;
	private final ModelPart watch;
	private final ModelPart head;
	private final ModelPart muzzle;
	private final ModelPart leftEar;
	private final ModelPart rightEar;
	private final ModelPart leftHaunch;
	private final ModelPart rightHaunch;
	private final ModelPart leftFoot;
	private final ModelPart rightFoot;
	private final ModelPart frontLeft;
	private final ModelPart frontRight;
	private final ModelPart tail;

	private static final float BODY_REST = -0.22F;

	public WhiteRabbitModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.collar = this.body.getChild("collar");
		this.watch = this.collar.getChild("pocket_watch");
		this.head = root.getChild("head");
		this.muzzle = this.head.getChild("muzzle");
		this.leftEar = this.head.getChild("left_ear");
		this.rightEar = this.head.getChild("right_ear");
		this.leftHaunch = root.getChild("left_haunch");
		this.rightHaunch = root.getChild("right_haunch");
		this.leftFoot = this.leftHaunch.getChild("left_foot");
		this.rightFoot = this.rightHaunch.getChild("right_foot");
		this.frontLeft = root.getChild("front_left_leg");
		this.frontRight = root.getChild("front_right_leg");
		this.tail = root.getChild("tail");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Body (5x5x8): texOffs(0, 0) -> width (5+8)*2=26, height 8+5=13 -> x:[0..26), y:[0..13)
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.5F, -4.0F, 5, 5, 8),
			PartPose.offsetAndRotation(0.0F, 18.5F, 1.0F, BODY_REST, 0.0F, 0.0F));

		// Crimson ribbon collar around neck (5x2x2): texOffs(26, 20) -> width (5+2)*2=14, height 2+2=4 -> x:[26..40), y:[20..24)
		PartDefinition collar = body.addOrReplaceChild("collar",
			CubeListBuilder.create().texOffs(26, 20).addBox(-2.5F, -2.8F, -4.6F, 5, 2, 2),
			PartPose.ZERO);

		// Brass pocket watch pendant (3x3x1): texOffs(42, 20) -> width (3+1)*2=8, height 1+3=4 -> x:[42..50), y:[20..24)
		collar.addOrReplaceChild("pocket_watch",
			CubeListBuilder.create().texOffs(42, 20).addBox(-1.5F, 0.0F, -0.5F, 3, 3, 1),
			PartPose.offset(0.0F, -1.2F, -5.2F));

		// Head (4x4x4): texOffs(26, 0) -> width 16, height 8 -> x:[26..42), y:[0..8)
		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create().texOffs(26, 0).addBox(-2.0F, -3.5F, -3.5F, 4, 4, 4),
			PartPose.offset(0.0F, 15.5F, -3.0F));
		// Muzzle (3x2x2): texOffs(42, 0) -> width 10, height 4 -> x:[42..52), y:[0..4)
		head.addOrReplaceChild("muzzle",
			CubeListBuilder.create().texOffs(42, 0).addBox(-1.5F, -1.5F, -4.5F, 3, 2, 2),
			PartPose.ZERO);
		// Ears (2x6x1): texOffs(52, 0) -> width 6, height 7 -> x:[52..58), y:[0..7)
		head.addOrReplaceChild("left_ear",
			CubeListBuilder.create().texOffs(52, 0).addBox(-1.0F, -6.0F, -0.5F, 2, 6, 1),
			PartPose.offsetAndRotation(1.1F, -3.3F, -1.0F, 0.25F, 0.0F, 0.16F));
		head.addOrReplaceChild("right_ear",
			CubeListBuilder.create().texOffs(52, 0).mirror().addBox(-1.0F, -6.0F, -0.5F, 2, 6, 1),
			PartPose.offsetAndRotation(-1.1F, -3.3F, -1.0F, 0.25F, 0.0F, -0.16F));

		haunch(root, "left", 2.1F, false);
		haunch(root, "right", -2.1F, true);
		frontLeg(root, "front_left", 1.2F, false);
		frontLeg(root, "front_right", -1.2F, true);

		// Tail (3x3x2): texOffs(36, 13) -> width 10, height 5 -> x:[36..46), y:[13..18)
		root.addOrReplaceChild("tail",
			CubeListBuilder.create().texOffs(36, 13).addBox(-1.5F, -1.5F, 0.0F, 3, 3, 2),
			PartPose.offset(0.0F, 17.5F, 4.6F));

		return LayerDefinition.create(mesh, 64, 32);
	}

	private static void haunch(PartDefinition root, String side, float x, boolean mirror) {
		// Thigh (2x4x5): texOffs(0, 13) -> x:[0..14), y:[13..22)
		CubeListBuilder thigh = CubeListBuilder.create().texOffs(0, 13);
		// Foot (2x1x5): texOffs(14, 13) -> x:[14..28), y:[13..19)
		CubeListBuilder foot = CubeListBuilder.create().texOffs(14, 13);
		if (mirror) {
			thigh.mirror();
			foot.mirror();
		}
		PartDefinition haunch = root.addOrReplaceChild(side + "_haunch",
			thigh.addBox(-1.0F, -2.0F, -2.5F, 2, 4, 5),
			PartPose.offset(x, 19.5F, 3.0F));
		haunch.addOrReplaceChild(side + "_foot",
			foot.addBox(-1.0F, 0.0F, -3.5F, 2, 1, 5),
			PartPose.offset(0.0F, 3.5F, 0.0F));
	}

	private static void frontLeg(PartDefinition root, String name, float x, boolean mirror) {
		// Foreleg (2x5x2): texOffs(28, 13) -> x:[28..36), y:[13..20)
		CubeListBuilder cube = CubeListBuilder.create().texOffs(28, 13);
		if (mirror) {
			cube.mirror();
		}
		root.addOrReplaceChild(name + "_leg",
			cube.addBox(-1.0F, 0.0F, -1.0F, 2, 5, 2),
			PartPose.offset(x, 19.0F, -2.2F));
	}

	@Override
	public void setupAnim(WhiteRabbitRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float air = state.air;
		boolean checkingWatch = state.checkingWatch;
		float alert = (checkingWatch || state.rabbitState == WhiteRabbit.STATE_WAITING_LOOKBACK) ? (1.0F - air) : 0.0F;
		float speed = Math.min(1.0F, state.walkAnimationSpeed);

		head.yRot = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.7F;

		// Nose twitch & expressive ear swivel
		muzzle.y = Mth.sin(t * 1.9F) * 0.14F;
		leftEar.zRot = 0.16F + Mth.sin(t * 0.08F) * 0.08F;
		rightEar.zRot = -0.16F + Mth.sin(t * 0.09F + 1.1F) * 0.08F;
		leftEar.xRot = rightEar.xRot = 0.25F - alert * 0.25F + air * 0.65F + speed * 0.25F;

		// Sit upright & hold paw/eye toward pocket watch when waiting for player
		body.xRot = BODY_REST - alert * 0.45F;
		body.y = 18.5F - alert * 1.0F;
		head.y = 15.5F - alert * 1.8F;
		head.z = -3.0F + alert * 0.6F;
		if (checkingWatch) {
			head.xRot += 0.22F;
		}
		watch.zRot = Mth.sin(t * 0.18F) * 0.12F + (checkingWatch ? 0.15F * Mth.sin(t * 0.35F) : 0.0F);
		watch.xRot = checkingWatch ? -0.35F : 0.12F;

		frontLeft.xRot = frontRight.xRot = -alert * 0.5F;
		frontLeft.y = frontRight.y = 19.0F - alert * 1.2F;
		frontLeft.z = frontRight.z = -2.2F + alert * 0.5F;

		if (air > 0.0F) {
			body.xRot = Mth.lerp(air, body.xRot, -0.05F);
			leftHaunch.xRot = rightHaunch.xRot = air * 0.85F;
			leftFoot.xRot = rightFoot.xRot = air * 0.7F;
			frontLeft.xRot = frontRight.xRot = -air * 0.65F;
			tail.xRot = air * 0.4F;
		} else {
			leftHaunch.xRot = rightHaunch.xRot = 0.0F;
			leftFoot.xRot = rightFoot.xRot = 0.0F;
			frontLeft.xRot += Mth.sin(state.walkAnimationPos * 1.6F) * 0.45F * speed;
			frontRight.xRot -= Mth.sin(state.walkAnimationPos * 1.6F) * 0.45F * speed;
			tail.xRot = Mth.sin(t * 0.28F) * 0.08F;
		}
	}
}
