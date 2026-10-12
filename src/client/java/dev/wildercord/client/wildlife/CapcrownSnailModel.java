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
 * A capcrown snail: a long soft foot, a neck rising at the front with two eye stalks and two little feelers, and a round
 * spiral shell on its back crowned with a mushroom cap: red and domed, or brown and broad (by {@link WildlifeRenderState#variant},
 * colour plus two once shorn). Shorn, and when young, the shell carries only two little nubs. It glides with ripples running
 * down its foot, its stalks swaying, one of them now and then drawn in and out again; stopped, it lowers its head to nibble.
 * Hidden ({@link WildlifeRenderState#hide}), the neck slides back under the shell and the foot draws in; peeking (partway
 * hidden), the eye stalks come out first. UVs match tools/snail_art.py.
 */
public final class CapcrownSnailModel extends EntityModel<WildlifeRenderState> {
	private static final float NECK_Y = -2, NECK_Z = -5;

	private final ModelPart body, neck, leftStalk, rightStalk, leftFeeler, rightFeeler, shell, redCap, brownCap, nubs;

	public CapcrownSnailModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		neck = body.getChild("neck");
		leftStalk = neck.getChild("left_stalk");
		rightStalk = neck.getChild("right_stalk");
		leftFeeler = neck.getChild("left_feeler");
		rightFeeler = neck.getChild("right_feeler");
		shell = body.getChild("shell");
		redCap = shell.getChild("red_cap");
		brownCap = shell.getChild("brown_cap");
		nubs = shell.getChild("nubs");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2, -3, -6, 4, 3, 12), PartPose.offset(0, 24, 0));
		PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(32, 0).addBox(-1.5F, -4, -1.5F, 3, 4, 3),
			PartPose.offset(0, NECK_Y, NECK_Z));
		for (int side = -1; side <= 1; side += 2) {
			String name = side > 0 ? "left" : "right";
			PartDefinition stalk = neck.addOrReplaceChild(name + "_stalk", CubeListBuilder.create().texOffs(44, 0).mirror(side < 0)
				.addBox(-0.5F, -3, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(side * 0.9F, -4, -0.6F, -0.15F, 0, side * 0.25F));
			stalk.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(48, 0).mirror(side < 0).addBox(-1, -2, -1, 2, 2, 2),
				PartPose.offset(0, -3, 0));
			neck.addOrReplaceChild(name + "_feeler", CubeListBuilder.create().texOffs(56, 0).mirror(side < 0).addBox(-0.5F, -0.5F, -2, 1, 1, 2),
				PartPose.offsetAndRotation(side * 0.8F, -0.8F, -1.5F, 0.35F, side * 0.3F, 0));
		}
		PartDefinition shell = body.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 16).addBox(-3, -6, -3, 6, 6, 6)
			.texOffs(24, 16).addBox(3, -4.5F, -1.5F, 1, 3, 3)
			.texOffs(24, 16).mirror().addBox(-4, -4.5F, -1.5F, 1, 3, 3), PartPose.offset(0, -2.5F, 1.5F));
		shell.addOrReplaceChild("red_cap", CubeListBuilder.create().texOffs(0, 28).addBox(-4, -3, -4, 8, 3, 8)
			.texOffs(32, 28).addBox(-3, -4, -3, 6, 1, 6), PartPose.offset(0, -5.5F, 0));
		shell.addOrReplaceChild("brown_cap", CubeListBuilder.create().texOffs(0, 40).addBox(-4, -2, -4, 8, 2, 8)
			.texOffs(32, 40).addBox(-3, -3, -3, 6, 1, 6), PartPose.offset(0, -5.5F, 0));
		shell.addOrReplaceChild("nubs", CubeListBuilder.create().texOffs(32, 16).addBox(-2, -1, -1.5F, 2, 1, 2)
			.texOffs(32, 16).mirror().addBox(0.5F, -1, 0, 2, 1, 2), PartPose.offset(0, -6, 0));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(WildlifeRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks + state.seed;
		float speed = Math.min(1, state.walkAnimationSpeed * 2.5F);
		float glide = state.walkAnimationPos * 1.6F;

		boolean young = state.isBaby;
		boolean shorn = state.variant >= 2;
		int colour = state.variant % 2;
		redCap.visible = !young && !shorn && colour == 0;
		brownCap.visible = !young && !shorn && colour == 1;
		nubs.visible = young || shorn;

		// The glide: ripples running back along the foot, the shell riding a little over them.
		body.zScale = 1 + Mth.sin(glide) * 0.05F * speed;
		body.yScale = 1 - Mth.sin(glide) * 0.04F * speed;
		shell.y = -2.5F + Mth.sin(glide + 1) * 0.15F * speed;
		shell.zRot = Mth.sin(t * 0.05F) * 0.02F;

		// The neck turns to look; stopped, now and then it lowers its head to nibble at the ground.
		float nibble = Mth.clamp(Mth.sin(t * 0.03F) * 4 - 2.5F, 0, 1) * (1 - speed);
		neck.yRot = Mth.clamp(state.yRot, -35, 35) * Mth.DEG_TO_RAD;
		neck.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F * (1 - nibble) + nibble * (0.9F + Mth.sin(t * 0.7F) * 0.08F) - speed * 0.12F;

		// The eye stalks sway, out of step with each other; now and then one draws in and comes out again.
		leftStalk.xRot = -0.15F + Mth.sin(t * 0.09F) * 0.15F;
		rightStalk.xRot = -0.15F + Mth.sin(t * 0.09F + 1.7F) * 0.15F;
		leftStalk.zRot = 0.25F + Mth.sin(t * 0.07F) * 0.12F;
		rightStalk.zRot = -0.25F + Mth.sin(t * 0.07F + 2.1F) * 0.12F;
		float blink = Mth.clamp(Mth.sin(t * 0.021F) * 6 - 5, 0, 1);
		leftStalk.yScale = 1 - 0.75F * blink;
		float blink2 = Mth.clamp(Mth.sin(t * 0.017F + 3) * 6 - 5, 0, 1);
		rightStalk.yScale = 1 - 0.75F * blink2;
		leftFeeler.xRot = 0.35F + Mth.sin(t * 0.15F) * 0.15F;
		rightFeeler.xRot = 0.35F + Mth.sin(t * 0.15F + 1) * 0.15F;

		float h = state.hide;
		if (h > 0) {
			// The neck slides back and down under the front of the shell, the foot drawing in after it; the eye stalks follow
			// last and are the first out again when it peeks.
			float neckIn = Mth.clamp(h / 0.6F, 0, 1);
			float eyesIn = Mth.clamp((h - 0.6F) / 0.4F, 0, 1);
			neck.z = NECK_Z + 3 * neckIn;
			neck.y = NECK_Y + 1.5F * neckIn;
			neck.yScale = 1 - 0.6F * neckIn;
			neck.xRot = Mth.lerp(neckIn, neck.xRot, -0.2F);
			neck.yRot *= 1 - neckIn;
			leftFeeler.zScale = rightFeeler.zScale = 1 - 0.8F * neckIn;
			float stalks = 1 - 0.85F * eyesIn;
			leftStalk.yScale = Math.min(leftStalk.yScale, stalks);
			rightStalk.yScale = Math.min(rightStalk.yScale, stalks);
			// Peeking, the stalks stand up stiff and turn to look about.
			float peek = neckIn * (1 - eyesIn);
			leftStalk.xRot = Mth.lerp(peek, leftStalk.xRot, -0.35F + Mth.sin(t * 0.3F) * 0.1F);
			rightStalk.xRot = Mth.lerp(peek, rightStalk.xRot, -0.35F + Mth.sin(t * 0.3F + 1) * 0.1F);
			body.zScale = Mth.lerp(neckIn, body.zScale, 0.65F);
			body.xScale = 1 + 0.1F * neckIn;
			shell.y = Mth.lerp(neckIn, shell.y, -1.8F);
		}

		if (young) {
			neck.xScale = neck.yScale = neck.zScale = 1.25F * neck.yScale;
			shell.xScale = shell.yScale = shell.zScale = 0.9F;
		}
	}
}
