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
 * A hushwing owlet: a round fluffy body on two short legs, a great square head with two ear tufts, a pale face disc, two big
 * amber eyes and a little hooked beak, two stubby wings and a short fan of a tail. It waddles, rocking side to side; its head
 * turns right round over its back to follow you ({@link WildlifeRenderState#yRot}, up to 160 degrees either way). Falling
 * ({@link WildlifeRenderState#flapStrength}) it flutters its stubby wings hard. Staring down a creaking
 * ({@link WildlifeRenderState#alert}) it hunches forward with its tufts up and its head bobbing side to side. Asleep
 * ({@link WildlifeRenderState#rest}) it fluffs up round, sinks down over its feet and shuts its eyes. UVs match tools/owlet_art.py.
 */
public final class HushwingOwletModel extends EntityModel<WildlifeRenderState> {
	private static final float BODY_Y = 22, HEAD_Y = -5;

	private final ModelPart body, head, leftTuft, rightTuft, lids, leftWing, rightWing, tail, leftLeg, rightLeg;

	public HushwingOwletModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = body.getChild("head");
		leftTuft = head.getChild("left_tuft");
		rightTuft = head.getChild("right_tuft");
		lids = head.getChild("lids");
		leftWing = body.getChild("left_wing");
		rightWing = body.getChild("right_wing");
		tail = body.getChild("tail");
		leftLeg = root.getChild("left_leg");
		rightLeg = root.getChild("right_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -5, -2.5F, 6, 5, 5),
			PartPose.offset(0, BODY_Y, 0));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 0).addBox(-3, -5, -2.5F, 6, 5, 5),
			PartPose.offset(0, HEAD_Y, 0));
		head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(48, 4).addBox(-0.5F, 0, -1, 1, 1, 1), PartPose.offset(0, -2, -2.5F));
		head.addOrReplaceChild("lids", CubeListBuilder.create().texOffs(48, 8).addBox(-3, -1, -0.5F, 6, 2, 1), PartPose.offset(0, -3, -2.3F));
		for (int side = -1; side <= 1; side += 2) {
			String name = side > 0 ? "left" : "right";
			head.addOrReplaceChild(name + "_tuft", CubeListBuilder.create().texOffs(48, 0).mirror(side < 0).addBox(-0.5F, -2, -0.5F, 1, 2, 1),
				PartPose.offsetAndRotation(side * 2.3F, -5, -0.5F, 0, 0, side * 0.3F));
			body.addOrReplaceChild(name + "_wing", CubeListBuilder.create().texOffs(0, 12).mirror(side < 0)
					.addBox(side > 0 ? 0 : -1, 0, -2, 1, 4, 4),
				PartPose.offset(side * 3, -4.5F, 0.3F));
			root.addOrReplaceChild(name + "_leg", CubeListBuilder.create().texOffs(12, 12).mirror(side < 0).addBox(-0.5F, 0, -1, 1, 2, 1),
				PartPose.offset(side * 1.3F, 22, 0));
		}
		body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 12).addBox(-1.5F, 0, 0, 3, 1, 2),
			PartPose.offsetAndRotation(0, -1.5F, 2.3F, 0.5F, 0, 0));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed * 2);
		float step = state.walkAnimationPos * 1.8F;
		float sleep = state.rest;
		float awake = 1 - sleep;
		float stare = state.alert * awake;
		float flutter = state.flapStrength * awake;

		// A waddle: short steps, the body rocking over them.
		leftLeg.xRot = Mth.sin(step) * 0.9F * speed;
		rightLeg.xRot = Mth.sin(step + Mth.PI) * 0.9F * speed;
		body.zRot = Mth.sin(step) * 0.12F * speed;
		body.y = BODY_Y;
		body.xRot = 0.35F * stare;
		body.xScale = body.zScale = 1;
		body.yScale = 1;

		// The head turns right round to follow you, and bobs from side to side when it stares something down.
		head.yRot = state.yRot * Mth.DEG_TO_RAD * awake;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * awake - body.xRot;
		head.zRot = Mth.sin(t * 0.25F) * 0.25F * stare - body.zRot;
		head.x = Mth.sin(t * 0.25F) * 0.6F * stare;
		head.y = HEAD_Y;
		head.xScale = head.yScale = head.zScale = state.isBaby ? 1.3F : 1;

		// Tufts flick now and then, and stand up straight when it stares.
		float flick = Mth.clamp(Mth.sin(t * 0.04F) * 6 - 5, 0, 1);
		leftTuft.zRot = Mth.lerp(stare, 0.3F + flick * 0.3F * Mth.sin(t * 1.4F), 0.05F);
		rightTuft.zRot = Mth.lerp(stare, -0.3F, -0.05F);
		leftTuft.xRot = rightTuft.xRot = -0.2F * stare;

		// The wings lie folded, lifting a little when it stares and beating hard when it falls.
		float beat = Mth.sin(t * 1.6F) * 0.5F + 0.5F;
		leftWing.zRot = -(0.05F + 0.15F * stare + flutter * (0.3F + 1.0F * beat));
		rightWing.zRot = -leftWing.zRot;
		leftWing.xRot = rightWing.xRot = 0;
		tail.xRot = 0.5F + Mth.sin(t * 0.07F) * 0.05F - 0.3F * flutter;

		lids.visible = sleep > 0.5F || Mth.sin(t * 0.03F) > 0.985F;

		if (sleep > 0) {
			// Fluffed round and sunk down over its feet, head drawn in, breathing slow.
			float breath = Mth.sin(t * 0.07F) * 0.04F;
			body.y = Mth.lerp(sleep, body.y, BODY_Y + 1.5F);
			body.xScale = body.zScale = 1 + (0.15F + breath) * sleep;
			body.yScale = 1 + breath * sleep;
			head.y = HEAD_Y + 0.8F * sleep;
			head.xRot = Mth.lerp(sleep, head.xRot, 0.15F);
			head.yRot = Mth.lerp(sleep, head.yRot, 0.2F);
			leftTuft.zRot = Mth.lerp(sleep, leftTuft.zRot, 0.7F);
			rightTuft.zRot = Mth.lerp(sleep, rightTuft.zRot, -0.7F);
			leftLeg.xRot = Mth.lerp(sleep, leftLeg.xRot, 0);
			rightLeg.xRot = Mth.lerp(sleep, rightLeg.xRot, 0);
		}
	}
}
