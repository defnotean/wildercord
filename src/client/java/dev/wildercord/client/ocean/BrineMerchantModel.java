package dev.wildercord.client.ocean;

import dev.wildercord.ocean.OceanRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 1:1 360-degree box rig for Tidefolk Brine-Merchants (64x64 sheet per profession):
 * Supports all 3 professions with profession-specific headgear (bubble helm + twin back tanks for
 * Pearl Jeweler & Abyssal Cartographer; woven kelp cowl for Kelp Alchemist) and held props.
 */
public final class BrineMerchantModel extends EntityModel<OceanRenderStates.BrineMerchantState> {
	private final ModelPart head;
	private final ModelPart bubbleHelm;
	private final ModelPart kelpCowl;
	private final ModelPart backCylinders;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart seaChart;
	private final ModelPart pearlGem;
	private final ModelPart alchemyFlask;

	public BrineMerchantModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.bubbleHelm = this.head.getChild("bubble_helm");
		this.kelpCowl = this.head.getChild("kelp_cowl");
		this.backCylinders = root.getChild("body").getChild("back_cylinders");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.seaChart = this.leftArm.getChild("sea_chart");
		this.pearlGem = this.leftArm.getChild("pearl_gem");
		this.alchemyFlask = this.leftArm.getChild("alchemy_flask");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8),
			PartPose.offset(0.0F, 0.0F, 0.0F));
		head.addOrReplaceChild("bubble_helm",
			CubeListBuilder.create().texOffs(0, 18).addBox(-5.0F, -10.5F, -5.0F, 10, 10, 10),
			PartPose.ZERO);
		head.addOrReplaceChild("kelp_cowl",
			CubeListBuilder.create().texOffs(0, 18).addBox(-5.0F, -11.0F, -5.0F, 10, 11, 10),
			PartPose.ZERO);

		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create()
				.texOffs(32, 0).addBox(-4.0F, 0.0F, -3.0F, 8, 12, 6)
				.texOffs(0, 39).addBox(-4.0F, 10.0F, -3.0F, 8, 10, 6)
				.texOffs(0, 55).addBox(4.0F, 9.0F, -1.5F, 3, 4, 3)
				.texOffs(12, 55).addBox(-5.5F, 9.0F, -2.0F, 2, 4, 4),
			PartPose.offset(0.0F, 0.0F, 0.0F));
		body.addOrReplaceChild("back_cylinders",
			CubeListBuilder.create()
				.texOffs(40, 18).addBox(-3.5F, 1.5F, 3.0F, 3, 8, 3)
				.texOffs(40, 18).mirror().addBox(0.5F, 1.5F, 3.0F, 3, 8, 3),
			PartPose.ZERO);

		root.addOrReplaceChild("right_arm",
			CubeListBuilder.create().texOffs(28, 39).addBox(-4.0F, -2.0F, -2.0F, 4, 12, 4),
			PartPose.offset(-4.0F, 2.0F, 0.0F));

		PartDefinition leftArm = root.addOrReplaceChild("left_arm",
			CubeListBuilder.create().texOffs(28, 39).mirror().addBox(0.0F, -2.0F, -2.0F, 4, 12, 4),
			PartPose.offset(4.0F, 2.0F, 0.0F));
		leftArm.addOrReplaceChild("sea_chart",
			CubeListBuilder.create().texOffs(24, 55).addBox(-5.0F, 7.0F, -4.0F, 10, 7, 1),
			PartPose.ZERO);
		leftArm.addOrReplaceChild("pearl_gem",
			CubeListBuilder.create().texOffs(46, 55).addBox(0.0F, 8.0F, -4.0F, 4, 4, 4),
			PartPose.ZERO);
		leftArm.addOrReplaceChild("alchemy_flask",
			CubeListBuilder.create().texOffs(46, 55).addBox(0.0F, 7.0F, -4.0F, 4, 5, 4),
			PartPose.ZERO);

		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create().texOffs(44, 39).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4),
			PartPose.offset(-2.0F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create().texOffs(44, 39).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4),
			PartPose.offset(2.0F, 12.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(OceanRenderStates.BrineMerchantState state) {
		super.setupAnim(state);
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

		float walk = state.walkAnimationPos * 0.6662F;
		float spd = state.walkAnimationSpeed;
		this.rightLeg.xRot = Mth.cos(walk) * 1.1F * spd;
		this.leftLeg.xRot = -Mth.cos(walk) * 1.1F * spd;
		this.rightArm.xRot = -Mth.cos(walk) * 0.8F * spd;
		this.leftArm.xRot = -0.45F + Mth.cos(walk) * 0.3F * spd;

		boolean isAlchemist = state.profession == OceanRules.MerchantProfession.KELP_ALCHEMIST;
		this.bubbleHelm.visible = !isAlchemist;
		this.backCylinders.visible = !isAlchemist;
		this.kelpCowl.visible = isAlchemist;

		this.pearlGem.visible = state.profession == OceanRules.MerchantProfession.PEARL_JEWELER;
		this.seaChart.visible = state.profession == OceanRules.MerchantProfession.ABYSSAL_CARTOGRAPHER;
		this.alchemyFlask.visible = isAlchemist;
	}
}
