package dev.wildercord.client.pet;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Cinnamon's own short, round shape: dark saddle, white belly from the chest back, floppy ears, fluffy legs and a black nub of a tail. */
public final class CinnamonModel extends EntityModel<CinnamonRenderState> {
	private final ModelPart body, head, tail, frontLeft, frontRight, backLeft, backRight, bell, tongue, bow;

	public CinnamonModel(ModelPart root) {
		super(root);
		body = root.getChild("body");
		head = root.getChild("head");
		tail = root.getChild("tail");
		frontLeft = root.getChild("front_left");
		frontRight = root.getChild("front_right");
		backLeft = root.getChild("back_left");
		backRight = root.getChild("back_right");
		bell = body.getChild("bell");
		tongue = head.getChild("tongue");
		bow = head.getChild("bow");
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
			.texOffs(78, 65).addBox(-3.5F, 16, -4.5F, 7, 4, 2)
			// Her red collar, just showing under her chin.
			.texOffs(0, 100).addBox(-4, 14.5F, -4.5F, 8, 2, 2, new CubeDeformation(0.25F)), PartPose.ZERO);
		PartDefinition body = root.getChild("body");
		// The little gold bell on her collar, hung from its top so it can swing.
		body.addOrReplaceChild("bell", CubeListBuilder.create().texOffs(24, 100).addBox(-1, 0, -1, 2, 2, 2), PartPose.offset(0, 15.3F, -5));
		// A round head (a wide core with a taller, narrower block through it to take its corners off), a little tuft on top,
		// and a small snout under her eyes with a black nose.
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(52, 0).addBox(-4.5F, -3, -7, 9, 6, 7)
			.texOffs(0, 82).addBox(-3.5F, -4, -6.5F, 7, 8, 6)
			.texOffs(52, 43).addBox(-2.5F, 0.5F, -8.5F, 5, 3, 2)
			.texOffs(80, 24).addBox(-1, 0.5F, -9, 2, 1, 1)
			.texOffs(104, 46).addBox(-1.5F, -4.5F, -7, 3, 1, 2),
			PartPose.offset(0, 12, -3));
		// The tip of her tongue, peeking out under her snout (shown now and then).
		head.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(34, 100).addBox(-0.5F, 0, -0.5F, 1, 1, 1), PartPose.offset(0, 3, -8.25F));
		// Her bow: a knot between two loops, tipped to one side on top of her head (shown while she wears it).
		head.addOrReplaceChild("bow", CubeListBuilder.create()
			.texOffs(40, 100).addBox(-0.5F, -0.5F, -0.75F, 1, 1, 1)
			.texOffs(46, 100).addBox(-2.5F, -1, -0.5F, 2, 2, 1)
			.texOffs(46, 100).mirror().addBox(0.5F, -1, -0.5F, 2, 2, 1),
			PartPose.offsetAndRotation(2.5F, -5, -5.5F, 0, 0, -0.35F));
		head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(0, 46).addBox(0, 0, -2, 3, 5, 3), PartPose.offset(4, -3, -3));
		head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(0, 46).mirror().addBox(-3, 0, -2, 3, 5, 3), PartPose.offset(-4, -3, -3));
		// Short legs, fluffy all the way down to the ground.
		root.addOrReplaceChild("front_left", CubeListBuilder.create().texOffs(20, 46).addBox(-2, 0, -2, 4, 6, 4), PartPose.offset(3.4F, 18, -4.2F));
		root.addOrReplaceChild("front_right", CubeListBuilder.create().texOffs(20, 46).mirror().addBox(-2, 0, -2, 4, 6, 4), PartPose.offset(-3.4F, 18, -4.2F));
		root.addOrReplaceChild("back_left", CubeListBuilder.create().texOffs(20, 46).addBox(-2, 0, -2, 4, 6, 4), PartPose.offset(3.4F, 18, 7.4F));
		root.addOrReplaceChild("back_right", CubeListBuilder.create().texOffs(20, 46).mirror().addBox(-2, 0, -2, 4, 6, 4), PartPose.offset(-3.4F, 18, 7.4F));
		// A little black nub, tipped up, that pokes just past her rump.
		root.addOrReplaceChild("tail", CubeListBuilder.create()
			.texOffs(40, 46).addBox(-1.5F, -1.5F, 0, 3, 3, 3), PartPose.offsetAndRotation(0, 14, 9, 0.45F, 0, 0));
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
		tongue.visible = state.tongue;
		bow.visible = state.wearingBow;
		// The bell swings as she trots, and shivers when it rings.
		bell.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 0.5F * state.walkAnimationSpeed;
		if (state.ringing) {
			bell.zRot = Mth.sin(state.ageInTicks * 1.7F) * 0.5F;
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
			tail.yRot = -.8F; tail.y = 18;
		}
		if (state.exhausted) {
			// Tired but still walking after her owner, never the curled-up sleeping pose.
			head.xRot = 0.28F + Mth.sin(state.ageInTicks * 0.06F) * 0.025F;
			head.zRot = 0;
			tail.yRot = Mth.sin(state.ageInTicks * 0.08F) * 0.06F;
			tail.xRot = -0.25F;
			tongue.visible = true;
		}
	}
}
