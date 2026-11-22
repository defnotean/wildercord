package dev.wildercord.client.ocean;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 1:1 360-degree box rig for Barnacle-Hulk Dredger (128x128 sheet):
 * Massive torso [12x14x8], bronze collar [12x3x10], caged helm head [10x10x10],
 * asymmetrical coral-overgrown shoulders, twin back O2 tanks [4x10x4], and Anchor-Flail on chain.
 */
public final class BarnacleHulkModel extends EntityModel<OceanRenderStates.BarnacleHulkState> {
	private final ModelPart head;
	private final ModelPart torso;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart anchorFlail;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public BarnacleHulkModel(ModelPart root) {
		super(root);
		this.torso = root.getChild("torso");
		this.head = this.torso.getChild("head");
		this.rightArm = this.torso.getChild("right_arm");
		this.leftArm = this.torso.getChild("left_arm");
		this.anchorFlail = this.leftArm.getChild("anchor_flail");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition torso = root.addOrReplaceChild("torso",
			CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -14.0F, -4.0F, 12, 14, 8),
			PartPose.offset(0.0F, 12.0F, 0.0F));
		torso.addOrReplaceChild("collar",
			CubeListBuilder.create().texOffs(40, 0).addBox(-6.0F, -15.5F, -5.0F, 12, 3, 10),
			PartPose.ZERO);
		torso.addOrReplaceChild("back_tanks",
			CubeListBuilder.create()
				.texOffs(64, 42).addBox(-5.0F, -12.0F, 4.0F, 4, 10, 4)
				.texOffs(64, 42).mirror().addBox(1.0F, -12.0F, 4.0F, 4, 10, 4)
				.texOffs(80, 42).addBox(-3.0F, -13.0F, 4.5F, 6, 2, 3),
			PartPose.ZERO);

		torso.addOrReplaceChild("head",
			CubeListBuilder.create().texOffs(0, 22).addBox(-5.0F, -10.0F, -5.0F, 10, 10, 10),
			PartPose.offset(0.0F, -15.0F, 0.0F));

		// Right arm: massive branching pink coral & barnacles on right pauldron & bracer
		PartDefinition rightArm = torso.addOrReplaceChild("right_arm",
			CubeListBuilder.create()
				.texOffs(0, 42).addBox(-5.0F, -2.0F, -2.5F, 5, 14, 5)
				.texOffs(40, 22).addBox(-6.0F, -3.5F, -3.5F, 7, 6, 7)
				.texOffs(40, 42).addBox(-5.5F, 6.0F, -3.0F, 6, 5, 6)
				.texOffs(84, 0).addBox(-6.5F, -8.0F, -1.0F, 3, 5, 2),
			PartPose.offset(-6.5F, -11.5F, 0.0F));

		// Left arm: pale blue coral sprig on left shoulder & swinging Anchor-Flail on 8-link chain
		PartDefinition leftArm = torso.addOrReplaceChild("left_arm",
			CubeListBuilder.create()
				.texOffs(20, 42).addBox(0.0F, -2.0F, -2.5F, 5, 14, 5)
				.texOffs(68, 22).addBox(-1.0F, -3.5F, -3.5F, 7, 6, 7)
				.texOffs(40, 42).mirror().addBox(-0.5F, 6.0F, -3.0F, 6, 5, 6)
				.texOffs(94, 0).addBox(3.5F, -7.0F, -1.0F, 2, 4, 2),
			PartPose.offset(6.5F, -11.5F, 0.0F));

		PartDefinition flail = leftArm.addOrReplaceChild("anchor_flail",
			CubeListBuilder.create().texOffs(40, 62).addBox(-0.5F, 0.0F, -0.5F, 1, 12, 1),
			PartPose.offset(2.5F, 11.0F, 0.0F));
		flail.addOrReplaceChild("anchor_head",
			CubeListBuilder.create().texOffs(48, 62).addBox(-6.0F, 10.0F, -1.5F, 12, 12, 3),
			PartPose.ZERO);

		PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
			CubeListBuilder.create()
				.texOffs(0, 62).addBox(-2.5F, 0.0F, -2.5F, 5, 12, 5)
				.texOffs(20, 62).addBox(-3.0F, 4.0F, -3.2F, 6, 4, 2),
			PartPose.offset(-3.2F, 12.0F, 0.0F));
		PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
			CubeListBuilder.create()
				.texOffs(0, 62).mirror().addBox(-2.5F, 0.0F, -2.5F, 5, 12, 5)
				.texOffs(20, 62).addBox(-3.0F, 4.0F, -3.2F, 6, 4, 2),
			PartPose.offset(3.2F, 12.0F, 0.0F));

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(OceanRenderStates.BarnacleHulkState state) {
		super.setupAnim(state);
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

		float walk = state.walkAnimationPos * 0.6F;
		float spd = state.walkAnimationSpeed;
		this.rightLeg.xRot = Mth.cos(walk) * 0.85F * spd;
		this.leftLeg.xRot = -Mth.cos(walk) * 0.85F * spd;
		this.rightArm.xRot = -Mth.cos(walk) * 0.7F * spd;

		if (state.slamTicks > 0) {
			float windup = Mth.clamp(state.slamTicks / 16.0F, 0.0F, 1.0F);
			this.leftArm.xRot = -2.4F * windup;
			this.anchorFlail.xRot = 0.85F * windup;
		} else {
			this.leftArm.xRot = Mth.cos(walk) * 0.6F * spd - 0.15F;
			this.anchorFlail.xRot = Mth.sin(state.ageInTicks * 0.12F) * 0.18F;
		}
	}
}
