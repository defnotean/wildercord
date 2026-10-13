package dev.wildercord.client.cartographer;

import dev.wildercord.Wildercord;
import net.minecraft.client.animation.*;
import net.minecraft.client.animation.definitions.KeyframeAnimations;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;

public final class GlassCartographerModel extends EntityModel<GlassCartographerRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("glass_cartographer"), "main");
	private static final AnimationDefinition IDLE = AnimationDefinition.Builder.withLength(3F).looping()
		.addAnimation("torso", new AnimationChannel(AnimationChannel.Targets.POSITION,
			new Keyframe(0F, KeyframeAnimations.posVec(0, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.5F, KeyframeAnimations.posVec(0, .55F, 0), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(3F, KeyframeAnimations.posVec(0, 0, 0), AnimationChannel.Interpolations.CATMULLROM)))
		.addAnimation("ring", new AnimationChannel(AnimationChannel.Targets.ROTATION,
			new Keyframe(0F, KeyframeAnimations.degreeVec(8, 0, -4), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.5F, KeyframeAnimations.degreeVec(-8, 18, 4), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(3F, KeyframeAnimations.degreeVec(8, 0, -4), AnimationChannel.Interpolations.CATMULLROM)))
		.addAnimation("lens", new AnimationChannel(AnimationChannel.Targets.ROTATION,
			new Keyframe(0F, KeyframeAnimations.degreeVec(0, 0, 5), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.5F, KeyframeAnimations.degreeVec(6, 0, -5), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(3F, KeyframeAnimations.degreeVec(0, 0, 5), AnimationChannel.Interpolations.CATMULLROM))).build();
	private static final AnimationDefinition SWEEP = AnimationDefinition.Builder.withLength(1.6F)
		.addAnimation("right_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
			new Keyframe(0F, KeyframeAnimations.degreeVec(-18, -10, 8), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.15F, KeyframeAnimations.degreeVec(-68, -56, 28), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.6F, KeyframeAnimations.degreeVec(-14, 52, -16), AnimationChannel.Interpolations.CATMULLROM)))
		.addAnimation("transit", new AnimationChannel(AnimationChannel.Targets.ROTATION,
			new Keyframe(0F, KeyframeAnimations.degreeVec(12, 0, -14), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.15F, KeyframeAnimations.degreeVec(52, -40, 26), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.6F, KeyframeAnimations.degreeVec(-18, 48, -22), AnimationChannel.Interpolations.CATMULLROM))).build();
	private static final AnimationDefinition PLUMB = AnimationDefinition.Builder.withLength(2.2F)
		.addAnimation("left_arm", new AnimationChannel(AnimationChannel.Targets.ROTATION,
			new Keyframe(0F, KeyframeAnimations.degreeVec(-12, 10, -8), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.5F, KeyframeAnimations.degreeVec(-105, 18, -20), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.2F, KeyframeAnimations.degreeVec(-22, -10, 12), AnimationChannel.Interpolations.CATMULLROM)))
		.addAnimation("plumb_weight", new AnimationChannel(AnimationChannel.Targets.POSITION,
			new Keyframe(0F, KeyframeAnimations.posVec(0, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(1.5F, KeyframeAnimations.posVec(0, 4, -2), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.2F, KeyframeAnimations.posVec(0, -6, -5), AnimationChannel.Interpolations.CATMULLROM))).build();
	private static final AnimationDefinition GRID = AnimationDefinition.Builder.withLength(2.8F)
		.addAnimation("wing_left", new AnimationChannel(AnimationChannel.Targets.ROTATION,
			new Keyframe(0F, KeyframeAnimations.degreeVec(0, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.1F, KeyframeAnimations.degreeVec(12, 24, -36), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.8F, KeyframeAnimations.degreeVec(0, 0, -8), AnimationChannel.Interpolations.CATMULLROM)))
		.addAnimation("wing_right", new AnimationChannel(AnimationChannel.Targets.ROTATION,
			new Keyframe(0F, KeyframeAnimations.degreeVec(0, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.1F, KeyframeAnimations.degreeVec(12, -24, 36), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.8F, KeyframeAnimations.degreeVec(0, 0, 8), AnimationChannel.Interpolations.CATMULLROM)))
		.addAnimation("lens", new AnimationChannel(AnimationChannel.Targets.POSITION,
			new Keyframe(0F, KeyframeAnimations.posVec(0, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.1F, KeyframeAnimations.posVec(0, 3.5F, -2), AnimationChannel.Interpolations.CATMULLROM),
			new Keyframe(2.8F, KeyframeAnimations.posVec(0, 1, 0), AnimationChannel.Interpolations.CATMULLROM))).build();
	private final KeyframeAnimation idleAnim, sweepAnim, plumbAnim, gridAnim;
	private final ModelPart head;

	public GlassCartographerModel(ModelPart root) {
		super(root);
		var torso = root.getChild("torso");
		head = torso.getChild("head");
		idleAnim = IDLE.bake(root);
		sweepAnim = SWEEP.bake(root);
		plumbAnim = PLUMB.bake(root);
		gridAnim = GRID.bake(root);
	}

	public static LayerDefinition createBodyLayer() {
		var mesh = new MeshDefinition();
		var root = mesh.getRoot();
		root.addOrReplaceChild("pedestal_base", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-16, -4, -16, 32, 4, 32)
			.texOffs(0, 36).addBox(-12, -8, -12, 24, 4, 24), PartPose.offset(0, 24, 0));
		var torso = root.addOrReplaceChild("torso", CubeListBuilder.create()
			.texOffs(0, 64).addBox(-7, -24, -5, 14, 20, 10)
			.texOffs(48, 64).addBox(-8, -4, -6, 16, 10, 12), PartPose.offset(0, 14, 0));
		var head = torso.addOrReplaceChild("head", CubeListBuilder.create()
			.texOffs(0, 94).addBox(-5, -9, -5, 10, 9, 10), PartPose.offset(0, -24, 0));
		head.addOrReplaceChild("ring", CubeListBuilder.create()
			.texOffs(40, 94).addBox(-8, -14, -8, 16, 6, 16), PartPose.offset(0, 0, 0));
		torso.addOrReplaceChild("wing_left", CubeListBuilder.create()
			.texOffs(104, 64).addBox(0, -2, -6, 6, 22, 12), PartPose.offset(7, -22, 0));
		torso.addOrReplaceChild("wing_right", CubeListBuilder.create()
			.texOffs(104, 64).mirror().addBox(-6, -2, -6, 6, 22, 12), PartPose.offset(-7, -22, 0));
		var rightArm = torso.addOrReplaceChild("right_arm", CubeListBuilder.create()
			.texOffs(128, 0).addBox(-4, -2, -3, 5, 18, 6), PartPose.offset(-10, -21, 0));
		rightArm.addOrReplaceChild("transit", CubeListBuilder.create()
			.texOffs(150, 0).addBox(-1, -18, -2, 2, 38, 4)
			.texOffs(162, 0).addBox(-3, 18, -3, 6, 10, 6), PartPose.offset(-1, 10, -2));
		var leftArm = torso.addOrReplaceChild("left_arm", CubeListBuilder.create()
			.texOffs(128, 24).addBox(-1, -2, -3, 5, 16, 6), PartPose.offset(10, -21, 0));
		leftArm.addOrReplaceChild("plumb_weight", CubeListBuilder.create()
			.texOffs(150, 42).addBox(-3, 0, -3, 6, 9, 6), PartPose.offset(1, 13, -3));
		torso.addOrReplaceChild("lens", CubeListBuilder.create()
			.texOffs(128, 48).addBox(-3, -6, -8, 6, 8, 6), PartPose.offset(0, -14, -2));
		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override public void setupAnim(GlassCartographerRenderState s) {
		super.setupAnim(s);
		idleAnim.apply(s.idle, s.ageInTicks);
		sweepAnim.apply(s.sweep, s.ageInTicks);
		plumbAnim.apply(s.plumb, s.ageInTicks);
		gridAnim.apply(s.grid, s.ageInTicks);
		head.yRot += s.yRot * ((float) Math.PI / 180F) * .35F;
		head.xRot += s.xRot * ((float) Math.PI / 180F) * .25F;
	}
}
