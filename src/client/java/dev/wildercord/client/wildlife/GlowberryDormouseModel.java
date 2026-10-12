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
 * A glowberry dormouse: a round little body on four tiny legs, a head with two great round ears, a pink nose and two cheek
 * pouches that swell with the berries in them ({@link WildlifeRenderState#held}), and a long tail ending in a fluffy brush.
 * It scurries with quick little steps, its nose twitching and its tail streaming; stood up to pluck a berry
 * ({@link WildlifeRenderState#rise}) it sits back on its haunches with its forepaws up at its mouth. Asleep
 * ({@link WildlifeRenderState#rest}) it curls into a ball, its head tucked down, its ears laid back and its tail wrapped round
 * over its nose, breathing slow. UVs match tools/dormouse_art.py.
 */
public final class GlowberryDormouseModel extends EntityModel<WildlifeRenderState> {
	private static final float BODY_Y = 22.5F, HEAD_Y = -2.5F;

	private final ModelPart body, head, leftEar, rightEar, leftCheek, rightCheek, nose, tail, brush;
	private final ModelPart leftPaw, rightPaw, leftLeg, rightLeg;

	public GlowberryDormouseModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = body.getChild("head");
		leftEar = head.getChild("left_ear");
		rightEar = head.getChild("right_ear");
		leftCheek = head.getChild("left_cheek");
		rightCheek = head.getChild("right_cheek");
		nose = head.getChild("nose");
		tail = body.getChild("tail");
		brush = tail.getChild("brush");
		leftPaw = body.getChild("left_paw");
		rightPaw = body.getChild("right_paw");
		leftLeg = root.getChild("left_leg");
		rightLeg = root.getChild("right_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// The body pivots at its rear, so standing up it sits back on its haunches.
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -4, -5.5F, 5, 4, 6),
			PartPose.offset(0, BODY_Y, 2.5F));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 0).addBox(-2, -2.5F, -3.5F, 4, 4, 4),
			PartPose.offset(0, HEAD_Y, -5.5F));
		for (int side = -1; side <= 1; side += 2) {
			String name = side > 0 ? "left" : "right";
			head.addOrReplaceChild(name + "_ear", CubeListBuilder.create().texOffs(42, 0).mirror(side < 0).addBox(-1, -2, -0.5F, 2, 2, 1),
				PartPose.offsetAndRotation(side * 1.4F, -2.3F, -1.2F, 0, 0, side * 0.35F));
			head.addOrReplaceChild(name + "_cheek", CubeListBuilder.create().texOffs(50, 0).mirror(side < 0).addBox(-1, -1, -1, 2, 2, 2),
				PartPose.offset(side * 1.6F, 0.6F, -2.2F));
			body.addOrReplaceChild(name + "_paw", CubeListBuilder.create().texOffs(38, 12).mirror(side < 0).addBox(-0.5F, 0, -0.5F, 1, 2, 1),
				PartPose.offset(side * 1.3F, -0.5F, -4.5F));
			root.addOrReplaceChild(name + "_leg", CubeListBuilder.create().texOffs(32, 12).mirror(side < 0).addBox(-0.5F, 0, -0.5F, 1, 2, 1),
				PartPose.offset(side * 1.5F, 22, 2));
		}
		head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(42, 4).addBox(-0.5F, -0.5F, -1, 1, 1, 1), PartPose.offset(0, 0.3F, -3.5F));
		PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 12).addBox(-1, -1, 0, 2, 2, 5),
			PartPose.offsetAndRotation(0, -1.5F, 0.3F, -0.5F, 0, 0));
		tail.addOrReplaceChild("brush", CubeListBuilder.create().texOffs(16, 12).addBox(-1.5F, -1.5F, 0, 3, 3, 4),
			PartPose.offsetAndRotation(0, 0, 4.5F, -0.4F, 0, 0));
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed * 2);
		float step = state.walkAnimationPos * 2.2F;
		float rise = state.rise;
		float sleep = state.rest;
		float awake = 1 - sleep;

		// Quick little steps, the body bobbing over them.
		leftLeg.xRot = Mth.sin(step) * 1.1F * speed;
		rightLeg.xRot = Mth.sin(step + Mth.PI) * 1.1F * speed;
		leftPaw.xRot = Mth.sin(step + Mth.PI) * 1.1F * speed;
		rightPaw.xRot = Mth.sin(step) * 1.1F * speed;
		body.y = BODY_Y - Math.abs(Mth.sin(step)) * 0.5F * speed;

		// The head looks about, the nose ever twitching, an ear flicking now and then.
		head.yRot = Mth.clamp(state.yRot, -40, 40) * Mth.DEG_TO_RAD * awake;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * awake;
		head.y = HEAD_Y;
		nose.y = 0.3F + Mth.sin(t * 1.3F) * 0.15F * awake;
		float flick = Mth.clamp(Mth.sin(t * 0.05F) * 6 - 5, 0, 1);
		leftEar.zRot = 0.35F + flick * 0.4F * Mth.sin(t * 1.5F);
		rightEar.zRot = -0.35F;
		leftEar.xRot = rightEar.xRot = 0;

		// The cheeks: flat when empty, swelling with each berry.
		float cheeks = Math.min(3, state.held) / 3.0F;
		float puff = 0.5F + 0.8F * cheeks;
		leftCheek.xScale = rightCheek.xScale = puff;
		leftCheek.yScale = rightCheek.yScale = leftCheek.zScale = rightCheek.zScale = 0.7F + 0.5F * cheeks;
		leftCheek.x = 1.6F + 0.4F * cheeks;
		rightCheek.x = -1.6F - 0.4F * cheeks;

		// The tail streams out behind running, lifts and sways standing still.
		tail.xRot = Mth.lerp(speed, -0.5F + Mth.sin(t * 0.08F) * 0.08F, -0.1F);
		tail.yRot = Mth.sin(t * 0.06F) * 0.25F * (1 - speed);
		tail.zRot = 0;
		brush.xRot = Mth.lerp(speed, -0.4F + Mth.sin(t * 0.08F + 1) * 0.15F, 0);
		brush.yRot = 0;
		body.xRot = 0;
		body.zRot = 0;
		body.xScale = body.yScale = body.zScale = 1;

		if (rise > 0) {
			// Up on its haunches, forepaws at its mouth, nose up to the berry.
			body.xRot = -1.05F * rise;
			head.xRot = Mth.lerp(rise, head.xRot, 0.55F + Mth.sin(t * 0.9F) * 0.05F);
			leftPaw.xRot = Mth.lerp(rise, leftPaw.xRot, -0.5F + Mth.sin(t * 0.8F) * 0.2F);
			rightPaw.xRot = Mth.lerp(rise, rightPaw.xRot, -0.5F + Mth.sin(t * 0.8F + 1) * 0.2F);
			tail.xRot = Mth.lerp(rise, tail.xRot, 0.9F);
			brush.xRot = Mth.lerp(rise, brush.xRot, -0.8F);
		}

		if (sleep > 0) {
			// Curled in a ball: the body hunched round, the head tucked down to its belly, ears laid back, paws drawn in, and
			// the tail swung round the side and over its nose; breathing slow.
			float breath = Mth.sin(t * 0.08F) * 0.05F;
			body.y = Mth.lerp(sleep, body.y, BODY_Y + 1.2F);
			body.xScale = 1 + (0.12F + breath) * sleep;
			body.yScale = 1 + breath * sleep;
			body.zScale = 1 - 0.2F * sleep;
			head.xRot = Mth.lerp(sleep, head.xRot, 1.1F);
			head.yRot = Mth.lerp(sleep, head.yRot, 0.3F);
			head.y = HEAD_Y + 1.2F * sleep;
			leftEar.xRot = rightEar.xRot = 0.7F * sleep;
			leftEar.zRot = Mth.lerp(sleep, leftEar.zRot, 0.9F);
			rightEar.zRot = Mth.lerp(sleep, rightEar.zRot, -0.9F);
			leftPaw.xRot = Mth.lerp(sleep, leftPaw.xRot, -1.2F);
			rightPaw.xRot = Mth.lerp(sleep, rightPaw.xRot, -1.2F);
			leftLeg.xRot = Mth.lerp(sleep, leftLeg.xRot, -1.3F);
			rightLeg.xRot = Mth.lerp(sleep, rightLeg.xRot, -1.3F);
			tail.xRot = Mth.lerp(sleep, tail.xRot, 0.05F);
			tail.yRot = Mth.lerp(sleep, tail.yRot, 1.9F);
			brush.yRot = Mth.lerp(sleep, brush.yRot, 1.5F);
			brush.xRot = Mth.lerp(sleep, brush.xRot, -0.15F + breath);
		}

		if (state.isBaby) {
			head.xScale = head.yScale = head.zScale = 1.35F;
		} else {
			head.xScale = head.yScale = head.zScale = 1;
		}
	}
}
