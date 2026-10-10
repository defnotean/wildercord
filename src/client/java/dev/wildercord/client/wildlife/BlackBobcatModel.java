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
 * A black bobcat: a deep-chested cat on thick legs, a broad head framed by a ruff of cheek fur, a short muzzle, upright ears
 * with long black tufts, and a bobbed stub of a tail. Built at a big cat's proportions and drawn larger by its renderer (to a
 * polar bear's size). It pads with its legs in diagonal pairs and its head low, flicks its tail and swivels its ears when idle,
 * and sits up on its haunches. A kitten has a bigger head. UVs match tools/bobcat_art.py.
 */
public final class BlackBobcatModel extends EntityModel<WildlifeRenderState> {
	private final ModelPart body, head, leftEar, rightEar, tail, frontLeft, frontRight, hindLeft, hindRight;

	public BlackBobcatModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = root.getChild("head");
		leftEar = head.getChild("left_ear");
		rightEar = head.getChild("right_ear");
		tail = root.getChild("tail");
		frontLeft = root.getChild("front_left_leg");
		frontRight = root.getChild("front_right_leg");
		hindLeft = root.getChild("hind_left_leg");
		hindRight = root.getChild("hind_right_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -4, -7, 10, 8, 14), PartPose.offset(0, 13, 0));
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(0, 22).addBox(-3.5F, -3.5F, -6, 7, 6, 6)
			.texOffs(26, 22).addBox(-1.5F, 0.5F, -8, 3, 2, 3)
			.texOffs(0, 34).addBox(-5, 0, -3, 10, 3, 2), PartPose.offset(0, 10, -7));
		ear(head, "left_ear", 2.3F, 0.18F, false);
		ear(head, "right_ear", -2.3F, -0.18F, true);
		root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(18, 40).addBox(-1.5F, -1.5F, 0, 3, 3, 5),
			PartPose.offsetAndRotation(0, 10.5F, 6.5F, 0.55F, 0, 0));
		leg(root, "front_left", 3, -4.5F, false);
		leg(root, "front_right", -3, -4.5F, true);
		leg(root, "hind_left", 3, 4.5F, false);
		leg(root, "hind_right", -3, 4.5F, true);
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static void ear(PartDefinition head, String name, float x, float lean, boolean mirror) {
		CubeListBuilder ear = CubeListBuilder.create().texOffs(40, 22);
		CubeListBuilder tuft = CubeListBuilder.create().texOffs(48, 22);
		if (mirror) {
			ear.mirror();
			tuft.mirror();
		}
		PartDefinition part = head.addOrReplaceChild(name, ear.addBox(-1, -3, -0.5F, 2, 3, 1), PartPose.offsetAndRotation(x, -3.5F, -2.5F, 0, 0, lean));
		part.addOrReplaceChild("tuft", tuft.addBox(-0.5F, -2, -0.5F, 1, 2, 1), PartPose.offset(0, -3, 0));
	}

	private static void leg(PartDefinition root, String name, float x, float z, boolean mirror) {
		CubeListBuilder cube = CubeListBuilder.create().texOffs(0, 40);
		if (mirror) {
			cube.mirror();
		}
		root.addOrReplaceChild(name + "_leg", cube.addBox(-2, 0, -2, 4, 8, 4), PartPose.offset(x, 16, z));
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed);
		float walk = state.walkAnimationPos * 0.6662F;
		float sit = state.sit;
		float stand = 1 - sit;

		float swing = Mth.cos(walk) * 0.9F * speed * stand;
		frontLeft.xRot = swing;
		hindRight.xRot = swing;
		frontRight.xRot = -swing;
		hindLeft.xRot = -swing;
		// A heavy, rolling pad: the shoulders rise and fall over each stride.
		body.y = 13 - Mth.abs(Mth.cos(walk)) * 0.4F * speed;
		body.zRot = Mth.sin(walk) * 0.03F * speed;

		head.yRot = Mth.clamp(state.yRot, -45, 45) * Mth.DEG_TO_RAD;
		// It carries its head a touch low when it moves, as a stalking cat does.
		head.xRot = state.xRot * Mth.DEG_TO_RAD + speed * 0.12F;
		head.y = 10 + speed * 0.5F;
		float swivel = Mth.clamp(Mth.sin(t * 0.07F) * 6 - 5, 0, 1);
		leftEar.yRot = -swivel * 0.5F;
		rightEar.yRot = Mth.clamp(Mth.sin(t * 0.083F + 1.7F) * 6 - 5, 0, 1) * 0.5F;
		leftEar.xRot = rightEar.xRot = -speed * 0.2F;

		// The bob flicks now and then, and lifts at a trot.
		float flick = Mth.clamp(Mth.sin(t * 0.05F) * 4 - 3, 0, 1);
		tail.xRot = 0.55F + speed * 0.25F + flick * Mth.sin(t * 0.9F) * 0.25F;
		tail.yRot = Mth.sin(t * 0.06F) * 0.15F;

		if (sit > 0) {
			// Up on its haunches: the body tipped up about its rump, forelegs straight under its chest, hind legs folded
			// flat, the bob resting on the ground behind.
			body.xRot = -0.55F * sit;
			body.y = Mth.lerp(sit, body.y, 15.6F);
			body.z = 1.6F * sit;
			head.y = Mth.lerp(sit, head.y, 7.4F);
			head.z = Mth.lerp(sit, -7, -4.6F);
			frontLeft.xRot = Mth.lerp(sit, frontLeft.xRot, -0.05F);
			frontRight.xRot = Mth.lerp(sit, frontRight.xRot, -0.05F);
			frontLeft.y = frontRight.y = Mth.lerp(sit, 16, 14.4F);
			frontLeft.yScale = frontRight.yScale = Mth.lerp(sit, 1, 1.2F);
			frontLeft.z = frontRight.z = Mth.lerp(sit, -4.5F, -3.6F);
			hindLeft.xRot = Mth.lerp(sit, hindLeft.xRot, -1.45F);
			hindRight.xRot = Mth.lerp(sit, hindRight.xRot, -1.45F);
			hindLeft.y = hindRight.y = Mth.lerp(sit, 16, 21.6F);
			hindLeft.z = hindRight.z = Mth.lerp(sit, 4.5F, 3.4F);
			hindLeft.x = Mth.lerp(sit, 3, 3.8F);
			hindRight.x = Mth.lerp(sit, -3, -3.8F);
			tail.y = Mth.lerp(sit, 10.5F, 21.5F);
			tail.z = Mth.lerp(sit, 6.5F, 7.5F);
			tail.xRot = Mth.lerp(sit, tail.xRot, -0.1F);
		}

		if (state.isBaby) {
			head.xScale = head.yScale = head.zScale = 1.35F;
			head.z -= 0.6F;
		}
	}
}
