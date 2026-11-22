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
 * 1:1 360-degree high-voxel rig for the Barnacle-Hulk Dredger (256x256 sheet, 0.5x render scale):
 * Oxidized verdigris bronze diving armor with bolted dome helmet, top chimney valve,
 * barred furnace-orange glass viewport, neck gorget, twin back steam tanks with manifold,
 * forked staghorn coral and volcano barnacles across shoulder/head, chained iron Anchor-Fist
 * right weapon, barnacle bracer shield on left arm, bolted knee guards, and weighted lead diving boots.
 */
public final class BarnacleHulkModel extends EntityModel<OceanRenderStates.BarnacleHulkState> {
	private final ModelPart head;
	private final ModelPart torso;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart anchorWeapon;

	public BarnacleHulkModel(ModelPart root) {
		super(root);
		this.torso = root.getChild("torso");
		this.head = this.torso.getChild("head");
		this.rightArm = this.torso.getChild("right_arm");
		this.leftArm = this.torso.getChild("left_arm");
		this.anchorWeapon = this.rightArm.getChild("anchor_weapon");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Legs anchored at hip Y=-3.0 down to Y=+24.0 (27 units * 0.5 = ground Y=0 at Y=+24)
		PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
			CubeListBuilder.create()
				.texOffs(154, 35).addBox(-4.5F, 0.0F, -4.5F, 9, 12, 9)
				.texOffs(187, 57).addBox(-5.0F, 9.5F, -5.0F, 10, 5, 10)
				.texOffs(100, 57).addBox(-4.5F, 14.0F, -4.5F, 9, 10, 9)
				.texOffs(49, 57).addBox(-5.5F, 22.0F, -8.5F, 11, 5, 14),
			PartPose.offset(-6.5F, -3.0F, 0.0F));
		rightLeg.addOrReplaceChild("leg_barnacle_r",
			CubeListBuilder.create().texOffs(170, 94).addBox(-6.0F, 15.0F, -1.0F, 3, 3, 3),
			PartPose.ZERO);

		PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
			CubeListBuilder.create()
				.texOffs(154, 35).mirror().addBox(-4.5F, 0.0F, -4.5F, 9, 12, 9)
				.texOffs(187, 57).mirror().addBox(-5.0F, 9.5F, -5.0F, 10, 5, 10)
				.texOffs(100, 57).mirror().addBox(-4.5F, 14.0F, -4.5F, 9, 10, 9)
				.texOffs(49, 57).mirror().addBox(-5.5F, 22.0F, -8.5F, 11, 5, 14),
			PartPose.offset(6.5F, -3.0F, 0.0F));
		leftLeg.addOrReplaceChild("leg_barnacle_l",
			CubeListBuilder.create().texOffs(77, 94).addBox(2.5F, 12.0F, -3.0F, 5, 4, 5),
			PartPose.ZERO);

		// Heavy diving barrel torso, furnace core hatch, riveted belt & twin back pressure tanks
		PartDefinition torso = root.addOrReplaceChild("torso",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-11.0F, -23.0F, -7.0F, 22, 20, 14)
				.texOffs(31, 78).addBox(-6.0F, -18.0F, -8.5F, 12, 12, 3)
				.texOffs(208, 78).addBox(-4.0F, -16.0F, -8.8F, 8, 8, 2)
				.texOffs(0, 35).addBox(-12.0F, -5.0F, -8.0F, 24, 5, 16)
				.texOffs(133, 94).addBox(-3.0F, -5.5F, -9.0F, 6, 6, 2)
				.texOffs(191, 35).addBox(-7.5F, -20.5F, 7.0F, 5, 16, 5)
				.texOffs(191, 35).mirror().addBox(2.5F, -20.5F, 7.0F, 5, 16, 5)
				.texOffs(98, 94).addBox(-6.5F, -14.5F, 7.5F, 13, 4, 4),
			PartPose.offset(0.0F, -1.0F, 0.0F));

		// Bell helmet with bolted neck gorget, blazing grille viewport, crown stack & coral cluster
		PartDefinition head = torso.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(81, 35).addBox(-10.0F, -4.0F, -8.0F, 20, 5, 16)
				.texOffs(73, 0).addBox(-8.0F, -19.0F, -7.0F, 16, 16, 14)
				.texOffs(62, 78).addBox(-5.0F, -22.5F, -5.0F, 10, 4, 10)
				.texOffs(229, 78).addBox(-2.0F, -28.0F, -2.0F, 4, 6, 4)
				.texOffs(0, 78).addBox(-6.0F, -17.0F, -8.5F, 12, 12, 3)
				.texOffs(187, 78).addBox(-4.0F, -15.0F, -9.0F, 8, 8, 2)
				.texOffs(180, 78).addBox(-1.0F, -16.0F, -9.6F, 2, 10, 1)
				.texOffs(183, 94).addBox(-5.0F, -12.0F, -9.6F, 10, 2, 1)
				.texOffs(103, 78).addBox(7.5F, -14.5F, -3.5F, 3, 7, 7)
				.texOffs(103, 78).mirror().addBox(-10.5F, -14.5F, -3.5F, 3, 7, 7)
				.texOffs(77, 94).addBox(-9.5F, -18.0F, -1.0F, 5, 4, 5),
			PartPose.offset(0.0F, -23.0F, 0.0F));

		head.addOrReplaceChild("head_coral_stem",
			CubeListBuilder.create().texOffs(0, 94).addBox(-1.5F, -7.0F, -1.5F, 3, 7, 3),
			PartPose.offsetAndRotation(-5.5F, -18.5F, 1.5F, -0.15F, 0.0F, -0.35F));
		head.addOrReplaceChild("head_coral_tine",
			CubeListBuilder.create().texOffs(161, 94).addBox(-1.0F, -5.0F, -1.0F, 2, 5, 2),
			PartPose.offsetAndRotation(-7.5F, -22.0F, 1.0F, 0.2F, 0.0F, -0.75F));

		// Right Arm + Chained Anchor-Fist weapon
		PartDefinition rightArm = torso.addOrReplaceChild("right_arm",
			CubeListBuilder.create()
				.texOffs(0, 57).addBox(-10.0F, -4.0F, -6.0F, 12, 8, 12)
				.texOffs(137, 57).addBox(-8.0F, 4.0F, -4.0F, 8, 11, 8)
				.texOffs(124, 78).addBox(-8.5F, 14.0F, -4.5F, 9, 4, 9)
				.texOffs(134, 0).addBox(-9.0F, 18.0F, -5.0F, 10, 12, 10),
			PartPose.offset(-12.0F, -19.0F, 0.0F));

		PartDefinition anchorWeapon = rightArm.addOrReplaceChild("anchor_weapon",
			CubeListBuilder.create()
				.texOffs(56, 94).addBox(-2.5F, -2.0F, -2.5F, 5, 4, 5)
				.texOffs(150, 94).addBox(-1.5F, 2.0F, -1.0F, 3, 5, 2)
				.texOffs(170, 57).addBox(-2.0F, 6.0F, -2.0F, 4, 14, 4)
				.texOffs(13, 94).addBox(-8.0F, 16.0F, -2.5F, 16, 4, 5)
				.texOffs(161, 78).addBox(-9.5F, 10.5F, -2.5F, 4, 7, 5)
				.texOffs(161, 78).mirror().addBox(5.5F, 10.5F, -2.5F, 4, 7, 5),
			PartPose.offsetAndRotation(-4.0F, 29.0F, -2.0F, -0.42F, 0.0F, 0.0F));
		anchorWeapon.addOrReplaceChild("anchor_tip_l",
			CubeListBuilder.create().texOffs(170, 94).addBox(-9.0F, 8.5F, -1.5F, 3, 3, 3),
			PartPose.ZERO);

		// Left Arm + Barnacle-Bracer Shield + Forked Staghorn Coral Crown
		PartDefinition leftArm = torso.addOrReplaceChild("left_arm",
			CubeListBuilder.create()
				.texOffs(0, 57).mirror().addBox(-2.0F, -4.0F, -6.0F, 12, 8, 12)
				.texOffs(137, 57).mirror().addBox(0.0F, 4.0F, -4.0F, 8, 11, 8)
				.texOffs(124, 78).mirror().addBox(-0.5F, 14.0F, -4.5F, 9, 4, 9)
				.texOffs(134, 0).mirror().addBox(-1.0F, 18.0F, -5.0F, 10, 12, 10)
				.texOffs(175, 0).addBox(8.0F, 17.5F, -5.0F, 3, 12, 10)
				.texOffs(77, 94).addBox(3.5F, -7.0F, -2.5F, 5, 4, 5),
			PartPose.offset(12.0F, -19.0F, 0.0F));
		leftArm.addOrReplaceChild("arm_coral_main",
			CubeListBuilder.create().texOffs(0, 94).addBox(-1.5F, -7.0F, -1.5F, 3, 7, 3),
			PartPose.offsetAndRotation(6.5F, -5.0F, 0.5F, 0.1F, 0.0F, 0.35F));
		leftArm.addOrReplaceChild("arm_coral_tine",
			CubeListBuilder.create().texOffs(161, 94).addBox(-1.0F, -5.0F, -1.0F, 2, 5, 2),
			PartPose.offsetAndRotation(9.0F, -8.5F, 1.0F, -0.15F, 0.0F, 0.85F));

		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(OceanRenderStates.BarnacleHulkState state) {
		super.setupAnim(state);
		float walkPos = state.walkAnimationPos;
		float walkSpd = state.walkAnimationSpeed;
		float swing = Mth.cos(walkPos * 0.55F) * 0.9F * walkSpd;
		float sway = Mth.sin(walkPos * 0.55F) * 0.06F * walkSpd;

		head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.7F;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F + Mth.sin(state.ageInTicks * 0.06F) * 0.03F;

		torso.zRot = sway;
		rightLeg.xRot = swing;
		leftLeg.xRot = -swing;

		if (state.slamTicks > 0) {
			float phase = (24 - Math.min(24, state.slamTicks)) / 24.0F;
			float raise = Mth.sin(phase * Mth.PI);
			torso.xRot = 0.28F * raise;
			rightArm.xRot = -2.1F * raise + 0.35F;
			leftArm.xRot = -1.65F * raise;
			anchorWeapon.xRot = -0.1F + 0.55F * raise;
		} else {
			torso.xRot = 0.04F;
			rightArm.xRot = -swing * 0.55F - 0.18F;
			leftArm.xRot = swing * 0.55F;
			anchorWeapon.xRot = -0.38F + Mth.sin(state.ageInTicks * 0.08F) * 0.10F;
		}
	}
}
