package dev.wildercord.client.pet;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Cinnamon's own short, round shape: dark saddle, warm face, long floppy ears and shaggy little paws. */
public final class CinnamonModel extends EntityModel<CinnamonRenderState> {
	private final ModelPart body, head, tail, frontLeft, frontRight, backLeft, backRight;

	public CinnamonModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = root.getChild("head");
		tail = root.getChild("tail");
		frontLeft = root.getChild("front_left");
		frontRight = root.getChild("front_right");
		backLeft = root.getChild("back_left");
		backRight = root.getChild("back_right");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-5.5F, 12, -3, 11, 8, 13)
			.texOffs(0, 25).addBox(-5, 17, -2.5F, 10, 5, 12)
			.texOffs(50, 26).addBox(-4, 15, -4, 8, 6, 4)
			.texOffs(50, 64).addBox(-6, 14, 0, 2, 6, 9)
			.texOffs(50, 64).mirror().addBox(4, 14, 0, 2, 6, 9)
			.texOffs(78, 65).addBox(-3.5F, 16, -4.5F, 7, 4, 2), PartPose.ZERO);
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(52, 0).addBox(-4.5F, -4, -7, 9, 8, 7)
			.texOffs(0, 64).addBox(-4.5F, -4.5F, -7.5F, 9, 2, 7)
			.texOffs(52, 43).addBox(-3.5F, -1, -9, 7, 4, 3)
			.texOffs(80, 24).addBox(-0.5F, 0.5F, -10, 1, 1, 1)
			.texOffs(88, 25).addBox(-4.5F, -2, -8, 2, 4, 2)
			.texOffs(88, 25).mirror().addBox(2.5F, -2, -8, 2, 4, 2)
			.texOffs(104, 46).addBox(-1.5F, -4.5F, -8, 3, 3, 2),
			PartPose.offset(0, 12, -3));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(0, 46).addBox(0, 0, -2, 3, 7, 3), PartPose.offset(4, -3, -3));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(0, 46).mirror().addBox(-3, 0, -2, 3, 7, 3), PartPose.offset(-4, -3, -3));
		root.addOrReplaceChild("front_left", CubeListBuilder.create().texOffs(20, 46).addBox(-1.5F, 0, -1.5F, 3, 6, 3)
			.texOffs(40, 46).addBox(-2, 4, -2.5F, 4, 3, 4), PartPose.offset(3.4F, 18, -4.2F));
		root.addOrReplaceChild("front_right", CubeListBuilder.create().texOffs(20, 46).mirror().addBox(-1.5F, 0, -1.5F, 3, 6, 3)
			.texOffs(40, 46).mirror().addBox(-2, 4, -2.5F, 4, 3, 4), PartPose.offset(-3.4F, 18, -4.2F));
		root.addOrReplaceChild("back_left", CubeListBuilder.create().texOffs(20, 46).addBox(-1.5F, 0, -1.5F, 3, 6, 3)
			.texOffs(40, 46).addBox(-2, 4, -2.5F, 4, 3, 4), PartPose.offset(3.4F, 18, 7.4F));
		root.addOrReplaceChild("back_right", CubeListBuilder.create().texOffs(20, 46).mirror().addBox(-1.5F, 0, -1.5F, 3, 6, 3)
			.texOffs(40, 46).mirror().addBox(-2, 4, -2.5F, 4, 3, 4), PartPose.offset(-3.4F, 18, 7.4F));
		root.addOrReplaceChild("tail", CubeListBuilder.create()
			.texOffs(66, 47).addBox(-1.5F, -2, 0, 3, 4, 5)
			.texOffs(88, 45).addBox(-2, -4, 3, 4, 5, 4), PartPose.offset(0, 15, 9));
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override public void setupAnim(CinnamonRenderState state) {
		super.setupAnim(state);
		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;
		float stride = Mth.cos(state.walkAnimationPos * 0.6662F) * 0.65F * state.walkAnimationSpeed;
		frontLeft.xRot = backRight.xRot = stride;
		frontRight.xRot = backLeft.xRot = -stride;
		if (state.sitting) {
			frontLeft.xRot = frontRight.xRot = -0.25F;
			backLeft.xRot = backRight.xRot = 1.1F;
		}
		tail.yRot = Mth.sin(state.ageInTicks * 0.18F) * 0.3F;
		if (state.greeting || state.playing) {
			tail.yRot = Mth.sin(state.ageInTicks * 0.48F) * 0.65F;
			head.zRot = Mth.sin(state.ageInTicks * 0.14F) * 0.16F;
			head.xRot -= 0.12F;
		} else if (!state.sleeping && state.walkAnimationSpeed < .05F && state.ageInTicks % 211 > 185) {
			head.zRot = Mth.sin((state.ageInTicks % 211 - 185) * Mth.PI / 26) * .22F;
		}
		if (state.sleeping) {
			body.y = 2; body.yRot = .12F;
			head.y = 18; head.yRot = -.48F; head.xRot = .22F; head.zRot = -.10F;
			frontLeft.xRot = frontRight.xRot = -1.25F;
			backLeft.xRot = backRight.xRot = 1.3F;
			tail.yRot = -.8F; tail.y = 19;
		}
	}
}
