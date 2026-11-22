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
 * 1:1 360-degree high-voxel rig for Barnacle-Hulk Dredger (256x256 sheet, 0.5x sub-voxel scale):
 * Stepped copper-verdigris diving helmet with 4-piece riveted viewport bezel, glowing cyan glass
 * behind 3 vertical grille bars, circular ear-ports, curved collar gorget, waist belt with 6 pouches,
 * twin back brass O2 tanks with manifold valves & hose, branching 3D pink & pale-blue staghorn coral,
 * volcano barnacle clusters, 6 alternating iron chain links, and hooked Anchor-Flail.
 */
public final class BarnacleHulkModel extends EntityModel<OceanRenderStates.BarnacleHulkState> {
	private final ModelPart torso;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart chainUpper;
	private final ModelPart anchorFlail;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;

	public BarnacleHulkModel(ModelPart root) {
		super(root);
		this.torso = root.getChild("torso");
		this.head = this.torso.getChild("head");
		this.rightArm = this.torso.getChild("right_arm");
		this.leftArm = this.torso.getChild("left_arm");
		this.chainUpper = this.leftArm.getChild("chain_upper");
		this.anchorFlail = this.chainUpper.getChild("anchor_flail");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Legs meet ground at Y = 24.0 (0.5F scale keeps Y=24 grounded; upper body spans up to Y=-48 = 2.25 blocks).
		PartDefinition torso = root.addOrReplaceChild("torso",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-11.0F, -26.0F, -7.0F, 22, 26, 14)
				.texOffs(74, 0).addBox(-9.0F, -22.0F, -8.5F, 18, 12, 2)
				.texOffs(120, 84).addBox(-12.0F, -4.5F, -8.0F, 24, 5, 16)
				// 6 raised waist utility pouches (front, sides, back) + brass center buckle
				.texOffs(218, 84).addBox(-2.5F, -4.5F, -9.2F, 5, 5, 2)
				.texOffs(202, 84).addBox(-9.5F, -5.0F, -9.0F, 4, 6, 3)
				.texOffs(202, 84).mirror().addBox(5.5F, -5.0F, -9.0F, 4, 6, 3)
				.texOffs(202, 84).addBox(-13.2F, -5.0F, -1.5F, 4, 6, 3)
				.texOffs(202, 84).mirror().addBox(9.2F, -5.0F, -1.5F, 4, 6, 3)
				.texOffs(202, 84).addBox(-8.0F, -5.0F, 6.2F, 4, 6, 3)
				.texOffs(202, 84).mirror().addBox(4.0F, -5.0F, 6.2F, 4, 6, 3)
				// Multi-piece bronze gorget collar
				.texOffs(116, 0).addBox(-12.0F, -29.0F, -9.0F, 24, 4, 18)
				.texOffs(84, 130).addBox(-10.0F, -30.5F, -8.0F, 20, 2, 16),
			PartPose.offset(0.0F, 0.0F, 0.0F));

		// Twin back brass oxygen cylinders + clamping bands + upper T-valve manifold + back air hose
		torso.addOrReplaceChild("back_tanks",
			CubeListBuilder.create()
				.texOffs(202, 0).addBox(-8.5F, -23.0F, 7.0F, 6, 18, 6)
				.texOffs(202, 0).mirror().addBox(2.5F, -23.0F, 7.0F, 6, 18, 6)
				.texOffs(228, 0).addBox(-7.5F, -25.0F, 8.0F, 4, 2, 4)
				.texOffs(228, 0).mirror().addBox(3.5F, -25.0F, 8.0F, 4, 2, 4)
				.texOffs(228, 0).addBox(-7.5F, -5.0F, 8.0F, 4, 2, 4)
				.texOffs(228, 0).mirror().addBox(3.5F, -5.0F, 8.0F, 4, 2, 4)
				.texOffs(202, 24).addBox(-9.5F, -19.0F, 6.5F, 8, 2, 8)
				.texOffs(202, 24).mirror().addBox(1.5F, -19.0F, 6.5F, 8, 2, 8)
				.texOffs(202, 24).addBox(-9.5F, -10.0F, 6.5F, 8, 2, 8)
				.texOffs(202, 24).mirror().addBox(1.5F, -10.0F, 6.5F, 8, 2, 8)
				.texOffs(192, 60).addBox(-6.0F, -27.5F, 8.0F, 12, 3, 4)
				.texOffs(226, 60).addBox(-1.5F, -33.0F, 7.5F, 3, 8, 3),
			PartPose.ZERO);

		// Stepped copper helmet, recessed cyan visor, 3 bronze grille bars, circular ear ports & barnacle crust
		PartDefinition head = torso.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 42).addBox(-10.0F, -20.0F, -10.0F, 20, 20, 20)
				.texOffs(82, 42).addBox(-7.0F, -22.5F, -7.0F, 14, 3, 14)
				.texOffs(140, 42).addBox(-3.0F, -24.0F, -3.0F, 6, 2, 6)
				// Protruding front viewport bezel & recessed glowing cyan glass
				.texOffs(82, 60).addBox(-7.0F, -16.0F, -11.5F, 14, 12, 3)
				.texOffs(118, 60).addBox(-5.0F, -14.0F, -12.0F, 10, 8, 1)
				// 3 distinct vertical bronze grille bars across visor
				.texOffs(142, 60).addBox(-3.0F, -14.5F, -13.2F, 1, 9, 2)
				.texOffs(142, 60).addBox(-0.5F, -14.5F, -13.2F, 1, 9, 2)
				.texOffs(142, 60).addBox(2.0F, -14.5F, -13.2F, 1, 9, 2)
				// Left & right circular stepped ear-port domes
				.texOffs(150, 60).addBox(9.5F, -14.0F, -4.0F, 3, 8, 8)
				.texOffs(150, 60).mirror().addBox(-12.5F, -14.0F, -4.0F, 3, 8, 8)
				.texOffs(174, 60).addBox(11.5F, -13.0F, -3.0F, 2, 6, 6)
				.texOffs(174, 60).mirror().addBox(-13.5F, -13.0F, -3.0F, 2, 6, 6)
				// Barnacles encrusting top crown
				.texOffs(136, 150).addBox(2.0F, -23.5F, -4.0F, 6, 3, 6)
				.texOffs(162, 150).addBox(3.0F, -26.0F, -2.0F, 3, 3, 3),
			PartPose.offset(0.0F, -29.0F, 0.0F));
		// Suppress unused head warning while exposing it to setupAnim
		head.addOrReplaceChild("head_coral",
			CubeListBuilder.create()
				.texOffs(102, 150).addBox(-9.5F, -22.0F, 1.0F, 2, 7, 2)
				.texOffs(126, 150).addBox(8.0F, -21.0F, 2.0F, 2, 6, 2),
			PartPose.ZERO);

		// Right Arm + massive coral-encrusted bronze pauldron + branching 3D pink staghorn coral
		PartDefinition rightArm = torso.addOrReplaceChild("right_arm",
			CubeListBuilder.create()
				.texOffs(0, 112).addBox(-9.0F, -2.0F, -4.5F, 9, 14, 9)
				.texOffs(38, 112).addBox(-10.0F, 11.0F, -5.5F, 11, 14, 11)
				.texOffs(84, 112).addBox(-10.5F, 10.0F, -6.0F, 12, 4, 12)
				.texOffs(134, 112).addBox(-8.5F, 24.0F, -4.0F, 8, 6, 8)
				// Stepped bronze pauldron + calcareous barnacle plate + volcano cones
				.texOffs(0, 84).addBox(-13.0F, -6.0F, -7.0F, 14, 11, 14)
				.texOffs(58, 84).addBox(-13.5F, -7.5F, -7.5F, 15, 3, 15)
				.texOffs(136, 150).addBox(-12.5F, -9.5F, -3.0F, 6, 3, 6)
				.texOffs(162, 150).addBox(-11.0F, -12.0F, -2.0F, 3, 3, 3)
				.texOffs(162, 150).addBox(-7.5F, -10.5F, 1.5F, 3, 3, 3),
			PartPose.offset(-12.0F, -24.0F, 0.0F));
		rightArm.addOrReplaceChild("pink_coral_shoulder_1",
			CubeListBuilder.create().texOffs(88, 150).addBox(-1.5F, -12.0F, -1.5F, 3, 12, 3),
			PartPose.offsetAndRotation(-8.5F, -6.0F, 2.0F, -0.18F, 0.0F, -0.34F));
		rightArm.addOrReplaceChild("pink_coral_shoulder_2",
			CubeListBuilder.create().texOffs(102, 150).addBox(-1.0F, -7.0F, -1.0F, 2, 7, 2),
			PartPose.offsetAndRotation(-11.5F, -11.0F, 2.5F, 0.14F, 0.0F, -0.68F));
		rightArm.addOrReplaceChild("pink_coral_shoulder_3",
			CubeListBuilder.create().texOffs(102, 150).addBox(-1.0F, -7.0F, -1.0F, 2, 7, 2),
			PartPose.offsetAndRotation(-6.5F, -13.0F, 0.5F, -0.2F, 0.0F, 0.28F));
		rightArm.addOrReplaceChild("pink_coral_forearm_main",
			CubeListBuilder.create().texOffs(88, 150).addBox(-1.5F, -12.0F, -1.5F, 3, 12, 3),
			PartPose.offsetAndRotation(-10.0F, 17.0F, 0.0F, 0.0F, 0.0F, -0.62F));
		rightArm.addOrReplaceChild("pink_coral_forearm_twig",
			CubeListBuilder.create().texOffs(102, 150).addBox(-1.0F, -7.0F, -1.0F, 2, 7, 2),
			PartPose.offsetAndRotation(-14.5F, 11.0F, 1.0F, 0.25F, 0.0F, -0.22F));

		// Left Arm + pale-blue coral sprig + barnacles + 6-link iron chain & hooked Anchor-Flail
		PartDefinition leftArm = torso.addOrReplaceChild("left_arm",
			CubeListBuilder.create()
				.texOffs(0, 112).mirror().addBox(0.0F, -2.0F, -4.5F, 9, 14, 9)
				.texOffs(38, 112).mirror().addBox(-1.0F, 11.0F, -5.5F, 11, 14, 11)
				.texOffs(84, 112).mirror().addBox(-1.5F, 10.0F, -6.0F, 12, 4, 12)
				.texOffs(134, 112).mirror().addBox(0.5F, 24.0F, -4.0F, 8, 6, 8)
				.texOffs(0, 84).mirror().addBox(-1.0F, -6.0F, -7.0F, 14, 11, 14)
				.texOffs(58, 84).mirror().addBox(-1.5F, -7.5F, -7.5F, 15, 3, 15)
				.texOffs(136, 150).addBox(5.5F, -9.5F, -2.5F, 6, 3, 6)
				.texOffs(162, 150).addBox(7.0F, -12.0F, -1.0F, 3, 3, 3)
				.texOffs(136, 150).addBox(3.5F, 13.0F, 3.0F, 6, 3, 6),
			PartPose.offset(12.0F, -24.0F, 0.0F));
		leftArm.addOrReplaceChild("blue_coral_1",
			CubeListBuilder.create().texOffs(112, 150).addBox(-1.5F, -10.0F, -1.5F, 3, 10, 3),
			PartPose.offsetAndRotation(9.0F, -6.0F, 1.5F, -0.14F, 0.0F, 0.36F));
		leftArm.addOrReplaceChild("blue_coral_2",
			CubeListBuilder.create().texOffs(126, 150).addBox(-1.0F, -6.0F, -1.0F, 2, 6, 2),
			PartPose.offsetAndRotation(11.5F, -11.0F, 2.0F, 0.15F, 0.0F, 0.72F));

		// Chain links + Heavy Hooked Iron Anchor-Flail
		PartDefinition chain = leftArm.addOrReplaceChild("chain_upper",
			CubeListBuilder.create()
				.texOffs(176, 150).addBox(-1.5F, 0.0F, -0.5F, 3, 5, 1)
				.texOffs(186, 150).addBox(-0.5F, 4.0F, -1.5F, 1, 5, 3)
				.texOffs(176, 150).addBox(-1.5F, 8.0F, -0.5F, 3, 5, 1)
				.texOffs(186, 150).addBox(-0.5F, 12.0F, -1.5F, 1, 5, 3),
			PartPose.offset(4.5F, 27.0F, -1.0F));

		PartDefinition flail = chain.addOrReplaceChild("anchor_flail",
			CubeListBuilder.create()
				.texOffs(176, 150).addBox(-1.5F, 0.0F, -0.5F, 3, 5, 1)
				// Central forged iron shank & crossbar stock
				.texOffs(196, 150).addBox(-1.5F, 4.0F, -1.5F, 3, 16, 3)
				.texOffs(210, 150).addBox(-7.0F, 7.0F, -1.5F, 14, 3, 3)
				// Heavy curved crown bar + angled barbed flukes
				.texOffs(0, 172).addBox(-9.0F, 19.0F, -2.0F, 18, 4, 4),
			PartPose.offset(0.0F, 16.0F, 0.0F));
		flail.addOrReplaceChild("fluke_right",
			CubeListBuilder.create()
				.texOffs(46, 172).addBox(-2.0F, -11.0F, -2.0F, 4, 11, 4)
				.texOffs(64, 172).addBox(-3.0F, -13.0F, -1.0F, 6, 4, 2),
			PartPose.offsetAndRotation(-8.5F, 21.0F, 0.0F, 0.0F, 0.0F, -0.28F));
		flail.addOrReplaceChild("fluke_left",
			CubeListBuilder.create()
				.texOffs(46, 172).mirror().addBox(-2.0F, -11.0F, -2.0F, 4, 11, 4)
				.texOffs(64, 172).mirror().addBox(-3.0F, -13.0F, -1.0F, 6, 4, 2),
			PartPose.offsetAndRotation(8.5F, 21.0F, 0.0F, 0.0F, 0.0F, 0.28F));

		// Heavy Thighs, Bronze Kneepads, Armored Shins & Lead-Weighted Boots ending flush at Y = 24.0
		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create()
				.texOffs(168, 112).addBox(-5.0F, 0.0F, -5.0F, 10, 13, 10)
				.texOffs(210, 112).addBox(-4.0F, 7.0F, -7.2F, 8, 7, 3)
				.texOffs(0, 150).addBox(-4.5F, 13.0F, -4.5F, 9, 11, 9)
				.texOffs(38, 150).addBox(-5.5F, 19.0F, -7.0F, 11, 5, 13)
				.texOffs(136, 150).addBox(-6.0F, 3.0F, -5.5F, 6, 3, 6),
			PartPose.offset(-5.5F, 0.0F, 0.0F));

		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create()
				.texOffs(168, 112).mirror().addBox(-5.0F, 0.0F, -5.0F, 10, 13, 10)
				.texOffs(210, 112).mirror().addBox(-4.0F, 7.0F, -7.2F, 8, 7, 3)
				.texOffs(0, 150).mirror().addBox(-4.5F, 13.0F, -4.5F, 9, 11, 9)
				.texOffs(38, 150).mirror().addBox(-5.5F, 19.0F, -7.0F, 11, 5, 13)
				.texOffs(136, 150).mirror().addBox(0.5F, 14.0F, -5.0F, 6, 3, 6),
			PartPose.offset(5.5F, 0.0F, 0.0F));

		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(OceanRenderStates.BarnacleHulkState state) {
		super.setupAnim(state);
		float walk = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		float swing = Mth.cos(walk * 0.55F) * speed;

		head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.7F;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.6F;

		rightLeg.xRot = swing * 0.72F;
		leftLeg.xRot = -swing * 0.72F;

		if (state.slamTicks > 0) {
			float p = 1.0F - (state.slamTicks / 16.0F);
			float slamAngle = p < 0.45F ? -2.35F * (p / 0.45F) : Mth.lerp((p - 0.45F) / 0.55F, -2.35F, 0.7F);
			leftArm.xRot = slamAngle;
			rightArm.xRot = slamAngle * 0.65F;
			chainUpper.xRot = 0.45F * Mth.sin(p * Mth.PI);
			anchorFlail.xRot = 0.55F * Mth.sin(p * Mth.PI);
			torso.xRot = p > 0.5F ? 0.24F : -0.14F;
		} else {
			torso.xRot = 0.05F;
			rightArm.xRot = -swing * 0.65F - 0.1F;
			leftArm.xRot = swing * 0.52F - 0.22F;
			chainUpper.xRot = -leftArm.xRot * 0.45F + 0.06F * Mth.sin(state.ageInTicks * 0.12F);
			anchorFlail.xRot = -leftArm.xRot * 0.35F + 0.09F * Mth.sin(state.ageInTicks * 0.15F);
		}
	}
}
