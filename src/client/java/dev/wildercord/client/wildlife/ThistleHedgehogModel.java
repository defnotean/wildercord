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
 * A thistle hedgehog: a round cream belly under a mantle of quills with a ridge along the top, a small head with a pointed
 * snout and a black button nose, little round ears peeking over the quills and four stubby feet. Caught in its quills is one
 * of a thistle bloom, a leaf or a berry, or nothing (by {@link WildlifeRenderState#variant}). It waddles, snuffles at the
 * ground with its nose twitching when it stops, jerks its head through a sneeze, and curls ({@link WildlifeRenderState#hide})
 * with its head tucked under, its feet drawn in and its quills puffed out, peeking out partway before it uncurls. A hoglet
 * has a bigger head. UVs match tools/hedgehog_art.py.
 */
public final class ThistleHedgehogModel extends EntityModel<WildlifeRenderState> {
	private static final float BODY_Y = 22, LEG_Y = 22, LEG_X = 2, FRONT_Z = -2.5F, HIND_Z = 3, HEAD_Y = -3, HEAD_Z = -3;

	private final ModelPart body, quills, head, snout, leftEar, rightEar, frontLeft, frontRight, hindLeft, hindRight;
	private final ModelPart[] legs;
	private final ModelPart[] trinkets;

	public ThistleHedgehogModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		quills = body.getChild("quills");
		head = body.getChild("head");
		snout = head.getChild("snout");
		leftEar = head.getChild("left_ear");
		rightEar = head.getChild("right_ear");
		trinkets = new ModelPart[] {quills.getChild("thistle"), quills.getChild("leaf"), quills.getChild("berry")};
		frontLeft = root.getChild("front_left_leg");
		frontRight = root.getChild("front_right_leg");
		hindLeft = root.getChild("hind_left_leg");
		hindRight = root.getChild("hind_right_leg");
		legs = new ModelPart[] {frontLeft, frontRight, hindLeft, hindRight};
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -4, -4, 6, 4, 8), PartPose.offset(0, BODY_Y, 0));
		PartDefinition quills = body.addOrReplaceChild("quills", CubeListBuilder.create()
			.texOffs(0, 12).addBox(-4, -8, -3, 8, 6, 9)
			.texOffs(0, 27).addBox(-3, -9, -2, 6, 1, 7), PartPose.ZERO);
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(34, 0).addBox(-3, -3, -3, 6, 5, 3),
			PartPose.offset(0, HEAD_Y, HEAD_Z));
		PartDefinition snout = head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(34, 8).addBox(-1.5F, -1.5F, -3, 3, 3, 3),
			PartPose.offset(0, 1, -3));
		snout.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(46, 8).addBox(-0.5F, -1.5F, -4, 1, 1, 1), PartPose.ZERO);
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(52, 0).addBox(-1, -2, -0.5F, 2, 2, 1), PartPose.offset(2, -3, -1));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(52, 0).mirror().addBox(-1, -2, -0.5F, 2, 2, 1), PartPose.offset(-2, -3, -1));
		PartDefinition thistle = quills.addOrReplaceChild("thistle", CubeListBuilder.create().texOffs(44, 16).addBox(-0.5F, -2, -0.5F, 1, 2, 1),
			PartPose.offsetAndRotation(1.5F, -9, 1.5F, 0, 0, 0.35F));
		thistle.addOrReplaceChild("bloom", CubeListBuilder.create().texOffs(48, 16).addBox(-1, -2, -1, 2, 2, 2), PartPose.offset(0, -2, 0));
		quills.addOrReplaceChild("leaf", CubeListBuilder.create().texOffs(34, 22).addBox(-2, -1, -1.5F, 4, 1, 3),
			PartPose.offsetAndRotation(-1.5F, -8.6F, 2, 0, 0.6F, -0.2F));
		quills.addOrReplaceChild("berry", CubeListBuilder.create().texOffs(48, 22).addBox(-1, -2, -1, 2, 2, 2), PartPose.offset(-1, -8.5F, 0.5F));
		leg(root, "front_left", LEG_X, FRONT_Z, false);
		leg(root, "front_right", -LEG_X, FRONT_Z, true);
		leg(root, "hind_left", LEG_X, HIND_Z, false);
		leg(root, "hind_right", -LEG_X, HIND_Z, true);
		return LayerDefinition.create(mesh, 64, 64);
	}

	private static void leg(PartDefinition root, String name, float x, float z, boolean mirror) {
		CubeListBuilder cube = CubeListBuilder.create().texOffs(34, 16);
		root.addOrReplaceChild(name + "_leg", (mirror ? cube.mirror() : cube).addBox(-1, 0, -1, 2, 2, 2), PartPose.offset(x, LEG_Y, z));
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed * 1.6F);
		float walk = state.walkAnimationPos * 1.3F;
		float curl = state.hide;
		float open = 1 - curl;

		for (int i = 0; i < trinkets.length; i++) {
			trinkets[i].visible = !state.isBaby && state.variant == i;
		}

		// A quick little waddle: short legs in diagonal pairs, the body rocking side to side over them.
		float swing = Mth.cos(walk) * 1.1F * speed * open;
		frontLeft.xRot = swing;
		hindRight.xRot = swing;
		frontRight.xRot = -swing;
		hindLeft.xRot = -swing;
		body.y = BODY_Y - Mth.abs(Mth.sin(walk)) * 0.4F * speed;
		body.zRot = Mth.sin(walk) * 0.09F * speed;

		// Stopped, it snuffles at the ground in bursts: head down, bobbing, its nose twitching.
		float sniff = Mth.clamp(Mth.sin(t * 0.045F) * 4 - 1.5F, 0, 1) * (1 - speed);
		head.yRot = Mth.clamp(state.yRot, -40, 40) * Mth.DEG_TO_RAD * (1 - sniff * 0.6F);
		head.xRot = state.xRot * Mth.DEG_TO_RAD * (1 - sniff) + sniff * (0.45F + Mth.sin(t * 0.8F) * 0.08F) + speed * 0.1F;
		snout.xRot = Mth.sin(t * 2.4F) * 0.07F * Math.max(sniff, 0.25F);
		snout.yRot = Mth.sin(t * 1.7F) * 0.08F * sniff;
		float flick = Mth.clamp(Mth.sin(t * 0.06F) * 5 - 4, 0, 1);
		leftEar.xRot = -flick * 0.3F;
		rightEar.xRot = -flick * 0.3F;

		// A sneeze: the head tips back as it draws in, then jerks down and forward.
		float s = state.sneeze;
		if (s > 0) {
			head.xRot += s > 0.7F ? -0.45F * (1 - s) / 0.3F : 0.5F * s / 0.7F;
			snout.xRot += s > 0.7F ? -0.2F : 0.15F;
		}

		if (curl > 0) {
			// Curled: the head tucked down under the quills and the feet drawn in, the quills puffed out round it, a little
			// shiver through the ball. Peeking out (partway curled), the nose comes out first, twitching fast.
			body.y = Mth.lerp(curl, body.y, 23.5F);
			body.xRot = curl * 0.35F;
			body.zRot = Mth.lerp(curl, body.zRot, Mth.sin(t * 0.9F) * 0.025F);
			quills.xScale = 1 + 0.12F * curl;
			quills.yScale = 1 + 0.2F * curl;
			quills.zScale = 1 + 0.15F * curl;
			head.xRot = Mth.lerp(curl, head.xRot, 1.9F);
			head.yRot *= open;
			head.z = HEAD_Z + 2.5F * curl;
			head.y = HEAD_Y + 1.0F * curl;
			snout.xRot += Mth.sin(t * 3.1F) * 0.12F * Mth.clamp(1 - Mth.abs(curl - 0.55F) * 4, 0, 1);
			leftEar.xRot = Mth.lerp(curl, leftEar.xRot, 0.6F);
			rightEar.xRot = Mth.lerp(curl, rightEar.xRot, 0.6F);
			for (ModelPart leg : legs) {
				leg.xRot *= open;
				leg.y = Mth.lerp(curl, LEG_Y, 21);
				leg.yScale = Mth.lerp(curl, 1, 0.3F);
			}
		}

		if (state.isBaby) {
			head.xScale = head.yScale = head.zScale = 1.4F;
		}
	}
}
