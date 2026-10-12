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
 * A mirebath capybara: a big barrel of a body on four short legs, a blunt square head held level, tiny round ears and no
 * tail. It ambles with slow, heavy steps. In its bath ({@link WildlifeRenderState#rest}) it sinks low with its legs tucked,
 * its chin up and its eyes shut; with an apple on its head ({@link WildlifeRenderState#hold}) it holds very still indeed.
 * Now and then it blinks, and its little ears flick. UVs match tools/capybara_art.py.
 */
public final class MirebathCapybaraModel extends EntityModel<WildlifeRenderState> {
	private static final float BODY_Y = 20.5F, LEG_Y = 20;

	private final ModelPart body, head, apple, leftLid, rightLid, leftEar, rightEar, leftFore, rightFore, leftHind, rightHind;

	public MirebathCapybaraModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = body.getChild("head");
		apple = head.getChild("apple");
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
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -6, -5.5F, 7, 6, 11),
			PartPose.offset(0, BODY_Y, 0));
		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(36, 0).addBox(-2.5F, -3, -6, 5, 5, 7),
			PartPose.offset(0, -4, -5.5F));
		head.addOrReplaceChild("apple", CubeListBuilder.create().texOffs(8, 17).addBox(-1.5F, -3, -1.5F, 3, 3, 3),
			PartPose.offset(0, -3, -2.5F));
		for (int side = -1; side <= 1; side += 2) {
			String name = side > 0 ? "left" : "right";
			head.addOrReplaceChild(name + "_ear", CubeListBuilder.create().texOffs(56, 12).mirror(side < 0).addBox(-1, -1, -0.5F, 2, 1, 1),
				PartPose.offset(side * 2.0F, -3, 0));
			head.addOrReplaceChild(name + "_lid", CubeListBuilder.create().texOffs(20, 17).mirror(side < 0).addBox(0, -2, -2, 0, 1, 1),
				PartPose.offset(side * 2.52F, 0, 0));
			root.addOrReplaceChild(name + "_foreleg", CubeListBuilder.create().texOffs(0, 17).mirror(side < 0).addBox(-1, 0, -1, 2, 4, 2),
				PartPose.offset(side * 2.0F, LEG_Y, -3.5F));
			root.addOrReplaceChild(name + "_hind_leg", CubeListBuilder.create().texOffs(0, 17).mirror(side < 0).addBox(-1, 0, -1, 2, 4, 2),
				PartPose.offset(side * 2.0F, LEG_Y, 3.5F));
		}
		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed * 2);
		float step = state.walkAnimationPos * 1.3F;
		float bathe = state.rest;
		float held = state.hold;

		// A slow, heavy amble: diagonal pairs together, the body rolling a little side to side.
		leftFore.xRot = rightHind.xRot = Mth.sin(step) * 0.9F * speed;
		rightFore.xRot = leftHind.xRot = Mth.sin(step + Mth.PI) * 0.9F * speed;
		body.y = BODY_Y - Math.abs(Mth.cos(step)) * 0.3F * speed;
		body.zRot = Mth.sin(step) * 0.04F * speed;
		body.xRot = 0;

		head.xRot = state.xRot * Mth.DEG_TO_RAD * (1 - held * 0.8F);
		head.yRot = state.yRot * Mth.DEG_TO_RAD * (1 - held * 0.8F);
		head.y = -4 + Mth.sin(t * 0.05F) * 0.1F;
		float flick = Mth.clamp(Mth.sin(t * 0.045F) * 6 - 5, 0, 1);
		leftEar.zRot = flick * 0.4F * Mth.sin(t * 1.6F);
		rightEar.zRot = -Math.max(0, Mth.clamp(Mth.sin(t * 0.045F + 2.3F) * 6 - 5, 0, 1) * 0.4F * Mth.sin(t * 1.6F));

		// Its bath: sunk low, legs tucked, chin up and eyes shut, breathing slow.
		if (bathe > 0) {
			body.y = Mth.lerp(bathe, body.y, BODY_Y + 2.5F + Mth.sin(t * 0.06F) * 0.15F);
			body.zRot *= 1 - bathe;
			leftFore.xRot = Mth.lerp(bathe, leftFore.xRot, -1.35F);
			rightFore.xRot = Mth.lerp(bathe, rightFore.xRot, -1.35F);
			leftHind.xRot = Mth.lerp(bathe, leftHind.xRot, 1.35F);
			rightHind.xRot = Mth.lerp(bathe, rightHind.xRot, 1.35F);
			head.xRot -= 0.12F * bathe;
		}
		for (ModelPart leg : new ModelPart[] {leftFore, rightFore, leftHind, rightHind}) {
			leg.y = LEG_Y + bathe * 2.0F;
		}
		boolean blink = Mth.sin(t * 0.031F) > 0.985F;
		leftLid.visible = rightLid.visible = bathe > 0.6F || blink;

		apple.visible = held > 0.05F;
		apple.xScale = apple.yScale = apple.zScale = held;
		apple.y = -3 - (1 - held) * 2;

		float scale = state.isBaby ? 1.35F : 1;
		head.xScale = head.yScale = head.zScale = scale;
	}
}
