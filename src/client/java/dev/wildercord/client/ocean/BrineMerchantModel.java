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
 * 1:1 360-degree high-voxel rig for Tidefolk Brine-Merchants (256x256 sheet x 3 professions, 0.5x scale):
 * Pearl Jeweler & Abyssal Cartographer feature diving hoods with octagonal riveted brass viewport
 * bezels, clear/glowing glass, ear-ports, collar gorgets, shoulder compass, and back twin brass O2 tanks;
 * Kelp Alchemist features a layered mossy-green kelp hood and crossed leather straps.
 * All three wear tailored teal coats with glowing waist buckles, hip lanterns, pouches, and 1:1 held props.
 */
public final class BrineMerchantModel extends EntityModel<OceanRenderStates.BrineMerchantState> {
	private final ModelPart head;
	private final ModelPart divingHelm;
	private final ModelPart shoulderCompass;
	private final ModelPart kelpHood;
	private final ModelPart backTanks;
	private final ModelPart alchemistStraps;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart propPearl;
	private final ModelPart propScroll;
	private final ModelPart propPotion;

	public BrineMerchantModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.divingHelm = this.head.getChild("diving_helm");
		this.kelpHood = this.head.getChild("kelp_hood");
		ModelPart torso = root.getChild("torso");
		this.shoulderCompass = torso.getChild("shoulder_compass");
		this.backTanks = torso.getChild("back_tanks");
		this.alchemistStraps = torso.getChild("alchemist_straps");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
		this.propPearl = this.leftArm.getChild("prop_pearl");
		this.propScroll = this.leftArm.getChild("prop_scroll");
		this.propPotion = this.leftArm.getChild("prop_potion");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Feet at Y=24.0 (0.5x scale -> legs 24 tall from Y=0..24, torso Y=-24..0, head up to Y=-46 -> 1.95 blocks)
		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-8.0F, -20.0F, -8.0F, 16, 20, 16)
				.texOffs(66, 0).addBox(-2.0F, -7.0F, -11.5F, 4, 8, 4),
			PartPose.offset(0.0F, -24.0F, 0.0F));

		head.addOrReplaceChild("diving_helm",
			CubeListBuilder.create()
				.texOffs(84, 0).addBox(-10.0F, -21.0F, -10.0F, 20, 20, 20)
				.texOffs(166, 0).addBox(-7.0F, -18.0F, -11.5F, 14, 14, 3)
				.texOffs(202, 0).addBox(-5.0F, -16.0F, -12.2F, 10, 10, 2)
				.texOffs(228, 0).addBox(9.5F, -14.5F, -3.5F, 3, 7, 7)
				.texOffs(228, 0).mirror().addBox(-12.5F, -14.5F, -3.5F, 3, 7, 7),
			PartPose.ZERO);

		head.addOrReplaceChild("kelp_hood",
			CubeListBuilder.create()
				.texOffs(70, 42).addBox(-10.5F, -21.5F, -10.0F, 21, 22, 21)
				.texOffs(156, 42).addBox(-11.0F, -4.0F, -6.0F, 22, 10, 18),
			PartPose.ZERO);

		// Tailored Teal Coat, Brass Clasps, Glowing Emerald Buckle, Hanging Left Hip Lantern & Right Pouch
		PartDefinition torso = root.addOrReplaceChild("torso",
			CubeListBuilder.create()
				.texOffs(0, 86).addBox(-8.0F, -24.0F, -6.0F, 16, 24, 12)
				.texOffs(0, 42).addBox(-10.0F, -25.5F, -7.0F, 20, 4, 14)
				.texOffs(58, 86).addBox(-6.0F, -19.0F, -7.0F, 12, 2, 2)
				.texOffs(58, 86).addBox(-6.0F, -14.0F, -7.0F, 12, 2, 2)
				.texOffs(58, 86).addBox(-6.0F, -9.0F, -7.0F, 12, 2, 2)
				.texOffs(20, 62).addBox(-2.0F, -4.5F, -7.2F, 4, 4, 2)
				.texOffs(88, 86).addBox(-8.5F, 0.0F, -6.5F, 17, 16, 13)
				.texOffs(34, 62).addBox(-10.5F, -3.0F, -3.0F, 4, 7, 6)
				.texOffs(74, 124).addBox(7.0F, -1.0F, -2.0F, 4, 6, 4),
			PartPose.offset(0.0F, 0.0F, 0.0F));

		torso.addOrReplaceChild("shoulder_compass",
			CubeListBuilder.create().texOffs(0, 62).addBox(-2.5F, -2.5F, -2.0F, 5, 5, 4),
			PartPose.offsetAndRotation(9.0F, -20.0F, -4.0F, 0.0F, 0.25F, 0.15F));

		torso.addOrReplaceChild("back_tanks",
			CubeListBuilder.create()
				.texOffs(150, 86).addBox(-6.5F, -21.0F, 6.0F, 5, 15, 5)
				.texOffs(150, 86).mirror().addBox(1.5F, -21.0F, 6.0F, 5, 15, 5)
				.texOffs(172, 86).addBox(-7.0F, -16.0F, 5.5F, 14, 3, 5)
				.texOffs(172, 86).addBox(-7.0F, -10.0F, 5.5F, 14, 3, 5),
			PartPose.ZERO);

		torso.addOrReplaceChild("alchemist_straps",
			CubeListBuilder.create().texOffs(222, 124).addBox(-1.0F, -24.0F, 5.5F, 2, 22, 2),
			PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.35F));

		root.addOrReplaceChild("right_arm",
			CubeListBuilder.create().texOffs(212, 86).addBox(-7.0F, -2.0F, -3.5F, 7, 22, 7),
			PartPose.offset(-8.0F, -22.0F, 0.0F));

		PartDefinition leftArm = root.addOrReplaceChild("left_arm",
			CubeListBuilder.create().texOffs(212, 86).mirror().addBox(0.0F, -2.0F, -3.5F, 7, 22, 7),
			PartPose.offset(8.0F, -22.0F, 0.0F));

		leftArm.addOrReplaceChild("prop_pearl",
			CubeListBuilder.create().texOffs(92, 124).addBox(0.0F, 14.0F, -11.0F, 7, 7, 7),
			PartPose.ZERO);
		PartDefinition scroll = leftArm.addOrReplaceChild("prop_scroll",
			CubeListBuilder.create()
				.texOffs(122, 124).addBox(-4.0F, 12.0F, -12.0F, 18, 12, 2)
				.texOffs(164, 124).addBox(-6.0F, 11.0F, -12.5F, 3, 14, 3)
				.texOffs(164, 124).mirror().addBox(13.0F, 11.0F, -12.5F, 3, 14, 3),
			PartPose.ZERO);
		scroll.setRotation(0.28F, 0.0F, 0.0F);
		leftArm.addOrReplaceChild("prop_potion",
			CubeListBuilder.create()
				.texOffs(178, 124).addBox(0.0F, 13.0F, -11.0F, 7, 7, 7)
				.texOffs(208, 124).addBox(2.0F, 9.5F, -9.0F, 3, 4, 3),
			PartPose.ZERO);

		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create()
				.texOffs(0, 124).addBox(-4.0F, 0.0F, -4.0F, 8, 24, 8)
				.texOffs(34, 124).addBox(-4.5F, 18.0F, -5.0F, 9, 6, 10),
			PartPose.offset(-4.0F, 0.0F, 0.0F));

		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create()
				.texOffs(0, 124).mirror().addBox(-4.0F, 0.0F, -4.0F, 8, 24, 8)
				.texOffs(34, 124).mirror().addBox(-4.5F, 18.0F, -5.0F, 9, 6, 10),
			PartPose.offset(4.0F, 0.0F, 0.0F));

		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(OceanRenderStates.BrineMerchantState state) {
		super.setupAnim(state);
		float swing = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed;
		head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.8F;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.8F;
		rightLeg.xRot = swing;
		leftLeg.xRot = -swing;
		rightArm.xRot = -swing * 0.55F;
		leftArm.xRot = -0.58F + swing * 0.25F;

		boolean alchemist = state.profession == OceanRules.MerchantProfession.KELP_ALCHEMIST;
		boolean cartographer = state.profession == OceanRules.MerchantProfession.ABYSSAL_CARTOGRAPHER;
		boolean jeweler = state.profession == OceanRules.MerchantProfession.PEARL_JEWELER;

		divingHelm.visible = !alchemist;
		kelpHood.visible = alchemist;
		backTanks.visible = !alchemist;
		alchemistStraps.visible = alchemist;
		shoulderCompass.visible = cartographer;
		propPearl.visible = jeweler;
		propScroll.visible = cartographer;
		propPotion.visible = alchemist;
	}
}
