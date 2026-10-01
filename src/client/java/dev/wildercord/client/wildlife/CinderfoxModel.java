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
 * A cinderfox: slight and long-legged, a wedge of a head with a narrow snout, a fluff of fur under its chin and tall desert ears,
 * and a long brush of a tail in three parts (root, brush, and the living ember at its tip, drawn again by the glow
 * layer). It trots with its legs in diagonal pairs and its tail streaming and swaying behind, twitches its ears and
 * cocks its head when idle, and sits up on its haunches with its tail curled round beside it. A kit has a bigger head
 * and bigger ears. UVs match {@code cinderfox_skin} in tools/wildlife_art.py.
 */
public final class CinderfoxModel extends EntityModel<WildlifeRenderState> {
	private final ModelPart body, head, leftEar, rightEar, tail, brush, ember, frontLeft, frontRight, hindLeft, hindRight;

	public CinderfoxModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = root.getChild("head");
		leftEar = head.getChild("left_ear");
		rightEar = head.getChild("right_ear");
		tail = root.getChild("tail");
		brush = tail.getChild("brush");
		ember = brush.getChild("ember");
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
			.texOffs(32, 0).addBox(-2.5F, -2.5F, -4, 5, 4, 4)
			.texOffs(32, 8).addBox(-1, -0.5F, -6.5F, 2, 2, 3)
			.texOffs(42, 8).addBox(-2, 1.5F, -3.5F, 4, 1, 3), PartPose.offset(0, 14.5F, -5.5F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(54, 0).addBox(-1.5F, -5, -0.5F, 3, 5, 1),
			PartPose.offsetAndRotation(1.6F, -2.2F, -1.6F, -0.12F, 0, 0.32F));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(54, 0).mirror().addBox(-1.5F, -5, -0.5F, 3, 5, 1),
			PartPose.offsetAndRotation(-1.6F, -2.2F, -1.6F, -0.12F, 0, -0.32F));
		PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(8, 16).addBox(-1.5F, -1.5F, 0, 3, 3, 5),
			PartPose.offsetAndRotation(0, 15, 5.2F, -0.55F, 0, 0));
		PartDefinition brush = tail.addOrReplaceChild("brush", CubeListBuilder.create().texOffs(24, 16).addBox(-2, -2, 0, 4, 4, 4), PartPose.offset(0, 0, 4.5F));
		brush.addOrReplaceChild("ember", CubeListBuilder.create().texOffs(40, 16).addBox(-1.5F, -1.5F, 0, 3, 3, 3), PartPose.offset(0, 0, 3.8F));
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
		leftEar.xRot = -0.12F - twitch * 0.35F + speed * 0.35F;
		rightEar.xRot = -0.12F - Mth.clamp(Mth.sin(t * 0.13F + 2) * 8 - 7, 0, 1) * 0.35F + speed * 0.35F;

		// The brush streams out at a trot and sways when it stands; each part follows the one before a beat behind.
		float sway = Mth.sin(t * 0.09F) * 0.2F * (1 - speed * 0.6F) + Mth.sin(walk) * 0.15F * speed;
		tail.xRot = -0.55F + speed * 0.45F;
		tail.yRot = sway;
		brush.yRot = Mth.sin(t * 0.09F - 0.7F) * 0.18F * (1 - speed * 0.5F);
		brush.xRot = 0.1F - speed * 0.05F;
		ember.yRot = Mth.sin(t * 0.09F - 1.4F) * 0.22F * (1 - speed * 0.5F);
		ember.xRot = 0.12F;

		if (sit > 0) {
			// Up on its haunches: the body tipped up about its rump (which stays on the ground), forelegs straight
			// under its chest, hind legs folded flat, the tail curled round beside it.
			body.xRot = -0.75F * sit;
			body.y = Mth.lerp(sit, body.y, 18.4F);
			body.z = 1.7F * sit;
			head.y = Mth.lerp(sit, 14.5F, 12.6F);
			head.z = Mth.lerp(sit, -5.5F, -2.6F);
			frontLeft.xRot = Mth.lerp(sit, frontLeft.xRot, -0.05F);
			frontRight.xRot = Mth.lerp(sit, frontRight.xRot, -0.05F);
			frontLeft.y = frontRight.y = Mth.lerp(sit, 19, 17.2F);
			frontLeft.yScale = frontRight.yScale = Mth.lerp(sit, 1, 1.36F);
			frontLeft.z = frontRight.z = Mth.lerp(sit, -3.5F, -3.1F);
			hindLeft.xRot = Mth.lerp(sit, hindLeft.xRot, -1.4F);
			hindRight.xRot = Mth.lerp(sit, hindRight.xRot, -1.4F);
			hindLeft.y = hindRight.y = Mth.lerp(sit, 19, 22.0F);
			hindLeft.z = hindRight.z = Mth.lerp(sit, 4, 3.0F);
			hindLeft.x = Mth.lerp(sit, 1.4F, 2.1F);
			hindRight.x = Mth.lerp(sit, -1.4F, -2.1F);
			tail.y = Mth.lerp(sit, 15, 22.4F);
			tail.z = Mth.lerp(sit, 5.2F, 4.4F);
			tail.xRot = Mth.lerp(sit, tail.xRot, 0);
			tail.yRot = Mth.lerp(sit, tail.yRot, 1.0F);
			brush.yRot = Mth.lerp(sit, brush.yRot, 0.75F);
			brush.xRot = Mth.lerp(sit, brush.xRot, 0);
			ember.yRot = Mth.lerp(sit, ember.yRot, 0.7F);
			ember.xRot = Mth.lerp(sit, ember.xRot, 0);
		}

		if (state.isBaby) {
			head.xScale = head.yScale = head.zScale = 1.4F;
			head.z -= 0.5F;
			leftEar.yScale = rightEar.yScale = 1.15F;
		}
	}
}
