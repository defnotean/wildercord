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
 * The Reefback Turtle: a broad domed shell with a raised ridge down its spine and two knobs of coral on top, a pale
 * belly plate, a stout neck and a blunt-beaked head, long front flippers and short back ones, and a stub of a tail.
 * Ashore it rests on its belly and rows itself along; in water its front flippers beat like wings. Drawn larger by its
 * renderer. UVs match tools/mount_art.py.
 */
public final class ReefbackTurtleModel extends EntityModel<WildlifeRenderState> {
	/** How far the flippers droop to the ground at rest. */
	private static final float DROOP = 0.25F;

	private final ModelPart body, neck, head, frontLeft, frontRight, backLeft, backRight, tail;

	public ReefbackTurtleModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		neck = body.getChild("neck");
		head = neck.getChild("head");
		frontLeft = body.getChild("front_left_flipper");
		frontRight = body.getChild("front_right_flipper");
		backLeft = body.getChild("back_left_flipper");
		backRight = body.getChild("back_right_flipper");
		tail = body.getChild("tail");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-8, -6, -10, 16, 6, 20)
			.texOffs(0, 26).addBox(-5, -8, -7, 10, 2, 14)
			.texOffs(0, 42).addBox(-7, 0, -9, 14, 2, 18)
			.texOffs(96, 30).addBox(1, -11, -3, 3, 3, 3)
			.texOffs(96, 30).addBox(-4, -10, 2, 3, 3, 3), PartPose.offset(0, 22, 0));
		PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(72, 12).addBox(-2, -2, -5, 4, 4, 5),
			PartPose.offset(0, -2, -10));
		neck.addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(72, 0).addBox(-3, -3, -7, 6, 5, 7)
			.texOffs(98, 0).addBox(-2, 0, -9, 4, 2, 2), PartPose.offset(0, 0, -4));
		body.addOrReplaceChild("front_left_flipper", CubeListBuilder.create().texOffs(72, 22).addBox(0, -0.5F, -2.5F, 12, 1, 5),
			PartPose.offsetAndRotation(7, -1, -6, 0, 0, DROOP));
		body.addOrReplaceChild("front_right_flipper", CubeListBuilder.create().texOffs(72, 22).mirror().addBox(-12, -0.5F, -2.5F, 12, 1, 5),
			PartPose.offsetAndRotation(-7, -1, -6, 0, 0, -DROOP));
		body.addOrReplaceChild("back_left_flipper", CubeListBuilder.create().texOffs(72, 30).addBox(0, -0.5F, -2.5F, 7, 1, 5),
			PartPose.offsetAndRotation(6, -1, 7, 0, 0, DROOP));
		body.addOrReplaceChild("back_right_flipper", CubeListBuilder.create().texOffs(72, 30).mirror().addBox(-7, -0.5F, -2.5F, 7, 1, 5),
			PartPose.offsetAndRotation(-6, -1, 7, 0, 0, -DROOP));
		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(110, 0).addBox(-1, -1, 0, 2, 2, 4), PartPose.offset(0, -1, 10));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed);
		float walk = state.walkAnimationPos * 0.5F;
		float swim = state.swim;
		float land = 1 - swim;

		// Ashore it rows: each front flipper sweeps back in turn with the opposite back one, the body rocking over them.
		float row = Mth.cos(walk) * 0.6F * speed * land;
		// In water the front flippers beat together like wings, the back ones steering, faster when it's going somewhere.
		float beat = Mth.sin(t * (0.12F + 0.12F * speed)) * swim;
		frontLeft.yRot = row;
		frontRight.yRot = row;
		frontLeft.zRot = DROOP * land + beat * 0.9F - Mth.abs(row) * 0.3F;
		frontRight.zRot = -frontLeft.zRot;
		frontLeft.xRot = frontRight.xRot = beat * 0.25F;
		backLeft.yRot = -row * 0.7F + Mth.sin(t * 0.1F) * 0.25F * swim;
		backRight.yRot = -backLeft.yRot;
		backLeft.zRot = DROOP * land;
		backRight.zRot = -DROOP * land;
		body.zRot = Mth.sin(walk) * 0.03F * speed * land;
		body.y = 22 - Mth.abs(beat) * 0.6F;

		neck.yRot = Mth.clamp(state.yRot, -40, 40) * Mth.DEG_TO_RAD * 0.5F;
		head.yRot = neck.yRot;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.6F;
		// A slow breath in and out of its shell now and then.
		neck.z = -10 + Mth.clamp(Mth.sin(t * 0.02F) * 4 - 3, 0, 1) * 2 * land;
		tail.yRot = Mth.sin(t * 0.08F) * 0.2F;
	}
}
