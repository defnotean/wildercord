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
 * A cinderfox: slight and long-legged, with a narrow snout, a ruff at the cheeks, tall desert ears and a long brush of
 * a tail in two parts, its tip a living ember (drawn again by the glow layer). It trots with its legs in diagonal
 * pairs and its tail streaming and swaying behind, twitches its ears and cocks its head when idle, and sits upright
 * with its tail curled round its feet. A kit has a bigger head and bigger ears. UVs match {@code cinderfox_skin} in
 * tools/wildlife_art.py.
 */
public final class CinderfoxModel extends EntityModel<WildlifeRenderState> {
	private final ModelPart body, head, leftEar, rightEar, tail, tailTip, frontLeft, frontRight, hindLeft, hindRight;

	public CinderfoxModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = root.getChild("head");
		leftEar = head.getChild("left_ear");
		rightEar = head.getChild("right_ear");
		tail = root.getChild("tail");
		tailTip = tail.getChild("tail_tip");
		frontLeft = root.getChild("front_left_leg");
		frontRight = root.getChild("front_right_leg");
		hindLeft = root.getChild("hind_left_leg");
		hindRight = root.getChild("hind_right_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.5F, -5.5F, 5, 5, 11), PartPose.offset(0, 16.5F, 0));
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(32, 0).addBox(-3, -3, -4.5F, 6, 5, 5)
			.texOffs(32, 10).addBox(-1.5F, -0.5F, -7.5F, 3, 2, 3)
			.texOffs(44, 10).addBox(-3.5F, -0.5F, -2.5F, 7, 3, 2), PartPose.offset(0, 15, -5.5F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(54, 0).addBox(-1.5F, -4, -0.5F, 3, 4, 1),
			PartPose.offsetAndRotation(2, -2.5F, -2, -0.1F, 0, 0.28F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(54, 0).mirror().addBox(-1.5F, -4, -0.5F, 3, 4, 1),
			PartPose.offsetAndRotation(-2, -2.5F, -2, -0.1F, 0, -0.28F));
		PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(8, 16).addBox(-1.5F, -1.5F, 0, 3, 3, 6),
			PartPose.offsetAndRotation(0, 15, 5.2F, -0.55F, 0, 0));
		tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(26, 16).addBox(-2, -2, 0, 4, 4, 5), PartPose.offset(0, 0, 5.5F));
		leg(root, "front_left", 1.4F, -3.5F, false);
		leg(root, "front_right", -1.4F, -3.5F, true);
		leg(root, "hind_left", 1.4F, 4, false);
		leg(root, "hind_right", -1.4F, 4, true);
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static void leg(PartDefinition root, String name, float x, float z, boolean mirror) {
		CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 16);
		if (mirror) {
			cube.mirror();
		}
		root.addOrReplaceChild(name + "_leg", cube.addBox(-1, 0, -1, 2, 5, 2), PartPose.offset(x, 19, z));
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed);
		float walk = state.walkAnimationPos * 0.6662F;
		float sit = state.sit;
		float stand = 1 - sit;

		float swing = Mth.cos(walk) * 1.15F * speed * stand;
		frontLeft.xRot = swing;
		hindRight.xRot = swing;
		frontRight.xRot = -swing;
		hindLeft.xRot = -swing;
		body.y = 16.5F - Mth.abs(Mth.cos(walk)) * 0.35F * speed;

		head.yRot = Mth.clamp(state.yRot, -50, 50) * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		// Idle: a cock of the head now and then, and ears that never quite stop listening.
		float tilt = Mth.clamp(Mth.sin(t * 0.031F) * 5 - 4, 0, 1) * (1 - speed);
		head.zRot = tilt * 0.28F;
		float twitch = Mth.clamp(Mth.sin(t * 0.11F) * 8 - 7, 0, 1);
		leftEar.xRot = -0.1F - twitch * 0.35F + speed * 0.3F;
		rightEar.xRot = -0.1F - Mth.clamp(Mth.sin(t * 0.13F + 2) * 8 - 7, 0, 1) * 0.35F + speed * 0.3F;

		// The brush streams out at a trot and sways when it stands; the ember tip follows a beat behind.
		float sway = Mth.sin(t * 0.09F) * 0.22F * (1 - speed * 0.6F) + Mth.sin(walk) * 0.15F * speed;
		tail.xRot = -0.55F + speed * 0.4F;
		tail.yRot = sway;
		tailTip.yRot = Mth.sin(t * 0.09F - 0.9F) * 0.3F * (1 - speed * 0.5F);
		tailTip.xRot = -0.15F + speed * 0.1F;

		if (sit > 0) {
			// Sitting up: haunches down, forelegs straight, tail curled round its feet.
			body.xRot = -0.62F * sit;
			body.y += 1.6F * sit;
			body.z = 1.2F * sit;
			head.y = 15 - 3.2F * sit;
			head.z = -5.5F + 2.2F * sit;
			frontLeft.xRot = Mth.lerp(sit, frontLeft.xRot, -0.12F);
			frontRight.xRot = Mth.lerp(sit, frontRight.xRot, -0.12F);
			frontLeft.z = frontRight.z = -3.5F + 0.6F * sit;
			hindLeft.xRot = Mth.lerp(sit, hindLeft.xRot, -1.45F);
			hindRight.xRot = Mth.lerp(sit, hindRight.xRot, -1.45F);
			hindLeft.y = hindRight.y = 19 + 2.6F * sit;
			hindLeft.z = hindRight.z = 4 - 0.6F * sit;
			tail.y = 15 + 6.5F * sit;
			tail.z = 5.2F - 1.4F * sit;
			tail.xRot = Mth.lerp(sit, tail.xRot, -0.05F);
			tail.yRot = Mth.lerp(sit, tail.yRot, 1.05F);
			tailTip.yRot = Mth.lerp(sit, tailTip.yRot, 1.0F);
		}

		if (state.isBaby) {
			head.xScale = head.yScale = head.zScale = 1.4F;
			head.z -= 0.5F;
			leftEar.yScale = rightEar.yScale = 1.15F;
		}
	}
}
