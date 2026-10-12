package dev.wildercord.client.wildlife;

import dev.wildercord.wildlife.PikaRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A tussock pika: a little round ball of a body on four tiny feet, a blunt round head, two big round ears and no tail to
 * speak of. It scurries in quick little bursts, its nose always going. Hayed, it carries a tuft of grass crosswise in its
 * mouth ({@link WildlifeRenderState#held}), or for a friend a flower; basking ({@link WildlifeRenderState#rest}) it
 * flattens out on the warm rock with its feet tucked under and its eyes shut. UVs match tools/pika_art.py.
 */
public final class TussockPikaModel extends EntityModel<WildlifeRenderState> {
	private static final float BODY_Y = 22.5F, LEG_Y = 22;

	private final ModelPart body, head, grass, stem, bloom, leftLid, rightLid, leftEar, rightEar, leftFore, rightFore, leftHind, rightHind;

	public TussockPikaModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = body.getChild("head");
		grass = head.getChild("grass");
		stem = head.getChild("stem");
		bloom = head.getChild("bloom");
		leftLid = head.getChild("left_lid");
		rightLid = head.getChild("right_lid");
		leftEar = head.getChild("left_ear");
		rightEar = head.getChild("right_ear");
		leftFore = root.getChild("left_foreleg");
		rightFore = root.getChild("right_foreleg");
		leftHind = root.getChild("left_hind_leg");
		rightHind = root.getChild("right_hind_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -5, -3.5F, 6, 5, 7),
			PartPose.offset(0, BODY_Y, 0));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(28, 0).addBox(-2.5F, -4, -4, 5, 4, 4),
			PartPose.offset(0, -1.5F, -2.5F));
		head.addOrReplaceChild("grass", CubeListBuilder.create().texOffs(10, 14).addBox(-3.5F, -0.5F, -0.5F, 7, 1, 1),
			PartPose.offset(0, -0.6F, -4.2F));
		head.addOrReplaceChild("stem", CubeListBuilder.create().texOffs(28, 10).addBox(-2.5F, -0.5F, -0.5F, 5, 1, 1),
			PartPose.offset(0, -0.6F, -4.2F));
		head.addOrReplaceChild("bloom", CubeListBuilder.create().texOffs(42, 10).addBox(-1, -1, -1, 2, 2, 2),
			PartPose.offset(3.2F, -0.9F, -4.2F));
		for (int side = -1; side <= 1; side += 2) {
			String name = side > 0 ? "left" : "right";
			head.addOrReplaceChild(name + "_ear", CubeListBuilder.create().texOffs(48, 0).mirror(side < 0).addBox(-1.5F, -3, -0.5F, 3, 3, 1),
				PartPose.offsetAndRotation(side * 1.8F, -3.5F, -1.5F, 0, 0, side * 0.35F));
			head.addOrReplaceChild(name + "_lid", CubeListBuilder.create().texOffs(52, 10).mirror(side < 0).addBox(0, -3, -3, 0, 1, 1),
				PartPose.offset(side * 2.52F, 0, 0));
			root.addOrReplaceChild(name + "_foreleg", CubeListBuilder.create().texOffs(0, 14).mirror(side < 0).addBox(-1, 0, -1, 2, 2, 2),
				PartPose.offset(side * 1.8F, LEG_Y, -2.0F));
			root.addOrReplaceChild(name + "_hind_leg", CubeListBuilder.create().texOffs(0, 14).mirror(side < 0).addBox(-1, 0, -1, 2, 2, 2),
				PartPose.offset(side * 1.8F, LEG_Y, 2.2F));
		}
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed * 2);
		float step = state.walkAnimationPos * 2.2F;
		float bask = state.rest;

		// A quick scurry: fore and hind feet in pairs, the round body bobbing as it goes.
		leftFore.xRot = rightFore.xRot = Mth.sin(step) * 1.1F * speed;
		leftHind.xRot = rightHind.xRot = Mth.sin(step + Mth.PI) * 1.1F * speed;
		body.y = BODY_Y - Math.abs(Mth.sin(step)) * 0.8F * speed;
		body.xRot = Mth.sin(step) * 0.06F * speed;
		body.xScale = body.zScale = 1;
		body.yScale = 1;

		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		// Its nose is always going: a little twitch of the whole head.
		head.y = -1.5F + Math.max(0, Mth.sin(t * 1.9F)) * 0.12F;
		float flick = Mth.clamp(Mth.sin(t * 0.06F) * 6 - 5, 0, 1);
		leftEar.zRot = 0.35F + flick * 0.3F * Mth.sin(t * 1.4F);
		rightEar.zRot = -0.35F - Math.max(0, Mth.clamp(Mth.sin(t * 0.06F + 2.1F) * 6 - 5, 0, 1)) * 0.3F * Mth.sin(t * 1.4F);
		leftEar.xRot = rightEar.xRot = 0;

		// Basking: flattened out on the rock, feet tucked under, chin down and ears laid back, breathing slow.
		if (bask > 0) {
			float breathe = Mth.sin(t * 0.07F) * 0.03F;
			body.y = Mth.lerp(bask, body.y, BODY_Y + 1.6F);
			body.xRot *= 1 - bask;
			body.yScale = 1 - bask * (0.22F + breathe);
			body.xScale = body.zScale = 1 + bask * 0.12F;
			head.xRot = Mth.lerp(bask, head.xRot, 0.15F);
			head.yRot *= 1 - bask;
			head.y = Mth.lerp(bask, head.y, -0.6F);
			leftEar.xRot = rightEar.xRot = 0.5F * bask;
			leftEar.zRot = Mth.lerp(bask, leftEar.zRot, 0.7F);
			rightEar.zRot = Mth.lerp(bask, rightEar.zRot, -0.7F);
		}
		for (ModelPart leg : new ModelPart[] {leftFore, rightFore, leftHind, rightHind}) {
			leg.y = LEG_Y + bask * 1.6F;
			leg.xRot *= 1 - bask;
		}
		boolean blink = Mth.sin(t * 0.037F) > 0.985F;
		leftLid.visible = rightLid.visible = bask > 0.5F || blink;

		grass.visible = state.held == PikaRules.GRASS;
		stem.visible = bloom.visible = state.held == PikaRules.FLOWER;

		float scale = state.isBaby ? 1.3F : 1;
		head.xScale = head.yScale = head.zScale = scale;
	}
}
