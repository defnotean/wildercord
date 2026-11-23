package dev.wildercord.client.ocean;

import dev.wildercord.ocean.OceanRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * 1:1 360-degree high-voxel translucent rig for Tidefolk Brine-Merchants (256x256 sheet x 3 professions, 0.5x scale):
 * <ul>
 *   <li>Pearl Jeweler &amp; Abyssal Cartographer: sculpted villager face &amp; eyes visible cleanly THROUGH
 *       an unobstructed translucent glass faceplate set into the octagonal copper diving helmet, twin back
 *       O2 tanks, shoulder Astro-Compass (Cartographer), and held 3D Pearl Chalice / Archipelago Sea-Chart Scroll.</li>
 *   <li>Kelp Alchemist: no metal helmet — wears a layered mossy-green leaf/kelp cowl hood &amp; shoulder shawl,
 *       crossed leather bandolier straps with corked glow-vials, and a luminescent Alchemist Flask.</li>
 * </ul>
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
		super(root, RenderTypes::entityTranslucent);
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

		// Legs & Weighted Diving Boots at Y=0..24 (0.5x -> Y=24 ground)
		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create()
				.texOffs(57, 43).addBox(-4.0F, 0.0F, -4.0F, 8, 20, 8)
				.texOffs(138, 76).addBox(-4.5F, 18.0F, -6.0F, 9, 6, 10),
			PartPose.offset(-4.0F, 0.0F, 0.0F));

		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create()
				.texOffs(57, 43).mirror().addBox(-4.0F, 0.0F, -4.0F, 8, 20, 8)
				.texOffs(138, 76).mirror().addBox(-4.5F, 18.0F, -6.0F, 9, 6, 10),
			PartPose.offset(4.0F, 0.0F, 0.0F));

		// Tailored Teal Coat + Collar + Split Front/Back Coat Tails + Emerald Belt Buckle + Hip Lantern & Pouch
		PartDefinition torso = root.addOrReplaceChild("torso",
			CubeListBuilder.create()
				.texOffs(166, 0).addBox(-8.0F, -24.0F, -6.0F, 16, 24, 12)
				.texOffs(21, 76).addBox(-10.0F, -25.5F, -7.0F, 20, 4, 14)
				.texOffs(88, 114).addBox(-2.5F, -4.5F, -7.2F, 5, 4, 2)
				.texOffs(0, 97).addBox(-8.2F, 0.0F, -6.2F, 7, 14, 2)
				.texOffs(0, 97).mirror().addBox(1.2F, 0.0F, -6.2F, 7, 14, 2)
				.texOffs(177, 76).addBox(-8.0F, 0.0F, 4.4F, 16, 14, 2)
				.texOffs(210, 97).addBox(-10.5F, -3.0F, -2.5F, 4, 7, 4)
				.texOffs(191, 97).addBox(7.2F, -1.5F, -2.5F, 4, 6, 5),
			PartPose.ZERO);

		torso.addOrReplaceChild("shoulder_compass",
			CubeListBuilder.create().texOffs(0, 114).addBox(-2.5F, -2.5F, -2.0F, 5, 5, 4),
			PartPose.offsetAndRotation(9.0F, -20.5F, -4.0F, 0.0F, 0.25F, 0.15F));

		torso.addOrReplaceChild("back_tanks",
			CubeListBuilder.create()
				.texOffs(0, 76).addBox(-6.5F, -21.0F, 6.0F, 5, 15, 5)
				.texOffs(0, 76).mirror().addBox(1.5F, -21.0F, 6.0F, 5, 15, 5)
				.texOffs(36, 114).addBox(-7.0F, -16.5F, 5.5F, 14, 3, 5)
				.texOffs(36, 114).addBox(-7.0F, -10.5F, 5.5F, 14, 3, 5),
			PartPose.ZERO);

		// Crossed leather bandolier straps + 3 corked glow vials for Kelp Alchemist
		PartDefinition alchemistStraps = torso.addOrReplaceChild("alchemist_straps",
			CubeListBuilder.create(), PartPose.ZERO);
		alchemistStraps.addOrReplaceChild("strap_a",
			CubeListBuilder.create().texOffs(167, 43).addBox(-1.0F, -11.0F, -1.0F, 2, 22, 2),
			PartPose.offsetAndRotation(0.0F, -12.0F, 6.2F, 0.0F, 0.0F, 0.45F));
		alchemistStraps.addOrReplaceChild("strap_b",
			CubeListBuilder.create().texOffs(167, 43).addBox(-1.0F, -11.0F, -1.0F, 2, 22, 2),
			PartPose.offsetAndRotation(0.0F, -12.0F, 6.2F, 0.0F, 0.0F, -0.45F));
		alchemistStraps.addOrReplaceChild("vial_1",
			CubeListBuilder.create().texOffs(103, 114).addBox(-1.0F, -2.0F, -1.0F, 2, 4, 2),
			PartPose.offset(-3.5F, -15.0F, -6.5F));
		alchemistStraps.addOrReplaceChild("vial_2",
			CubeListBuilder.create().texOffs(103, 114).addBox(-1.0F, -2.0F, -1.0F, 2, 4, 2),
			PartPose.offset(0.0F, -13.0F, -6.5F));
		alchemistStraps.addOrReplaceChild("vial_3",
			CubeListBuilder.create().texOffs(103, 114).addBox(-1.0F, -2.0F, -1.0F, 2, 4, 2),
			PartPose.offset(3.5F, -11.0F, -6.5F));

		// Arms with tailored brass/leather flared cuffs + profession props
		root.addOrReplaceChild("right_arm",
			CubeListBuilder.create()
				.texOffs(176, 43).addBox(-7.0F, -2.0F, -3.5F, 7, 14, 7)
				.texOffs(214, 76).addBox(-7.5F, 12.0F, -4.0F, 8, 8, 8),
			PartPose.offset(-8.0F, -22.0F, 0.0F));

		PartDefinition leftArm = root.addOrReplaceChild("left_arm",
			CubeListBuilder.create()
				.texOffs(176, 43).mirror().addBox(0.0F, -2.0F, -3.5F, 7, 14, 7)
				.texOffs(214, 76).mirror().addBox(-0.5F, 12.0F, -4.0F, 8, 8, 8),
			PartPose.offset(8.0F, -22.0F, 0.0F));

		leftArm.addOrReplaceChild("prop_pearl",
			CubeListBuilder.create()
				.texOffs(19, 114).addBox(1.5F, 15.0F, -10.0F, 4, 5, 4)
				.texOffs(110, 97).addBox(0.5F, 9.5F, -11.0F, 6, 6, 6),
			PartPose.ZERO);
		leftArm.addOrReplaceChild("prop_scroll",
			CubeListBuilder.create()
				.texOffs(19, 97).addBox(-4.5F, 10.5F, -12.0F, 18, 12, 2)
				.texOffs(125, 76).addBox(-6.5F, 9.5F, -12.5F, 3, 14, 3)
				.texOffs(125, 76).mirror().addBox(12.5F, 9.5F, -12.5F, 3, 14, 3),
			PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.30F, 0.0F, 0.0F));
		leftArm.addOrReplaceChild("prop_potion",
			CubeListBuilder.create()
				.texOffs(60, 97).addBox(0.0F, 13.0F, -11.5F, 7, 7, 7)
				.texOffs(75, 114).addBox(2.0F, 9.5F, -9.5F, 3, 4, 3),
			PartPose.ZERO);

		// Head inserted LAST vs torso, and inside head:
		// 1. Villager face & nose inserted FIRST -> rendered FIRST
		// 2. Kelp Hood (Kelp Alchemist)
		// 3. Diving Helm shell (alpha=0 viewport window in front) -> followed LAST by translucent glass viewport!
		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 43).addBox(-7.0F, -18.5F, -7.0F, 14, 18, 14)
				.texOffs(227, 97).addBox(-1.5F, -7.0F, -10.5F, 3, 7, 4),
			PartPose.offset(0.0F, -24.0F, 0.0F));

		head.addOrReplaceChild("kelp_hood",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-10.5F, -21.0F, -10.0F, 21, 21, 21)
				.texOffs(205, 43).addBox(-6.0F, -27.5F, -3.5F, 12, 8, 12)
				.texOffs(90, 43).addBox(-11.0F, -3.0F, -7.0F, 22, 8, 16),
			PartPose.ZERO);

		PartDefinition divingHelm = head.addOrReplaceChild("diving_helm",
			CubeListBuilder.create()
				.texOffs(85, 0).addBox(-10.0F, -20.5F, -10.0F, 20, 20, 20)
				.texOffs(135, 97).addBox(-4.0F, -23.0F, -4.0F, 8, 3, 8)
				.texOffs(90, 76).addBox(-7.0F, -17.5F, -11.5F, 14, 14, 3)
				.texOffs(89, 97).addBox(9.5F, -14.0F, -3.5F, 3, 7, 7)
				.texOffs(89, 97).mirror().addBox(-12.5F, -14.0F, -3.5F, 3, 7, 7),
			PartPose.ZERO);
		// Translucent glass pane rendered AFTER shell + inner villager face!
		divingHelm.addOrReplaceChild("helm_glass",
			CubeListBuilder.create().texOffs(168, 97).addBox(-5.0F, -15.5F, -11.8F, 10, 10, 1),
			PartPose.ZERO);

		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(OceanRenderStates.BrineMerchantState state) {
		super.setupAnim(state);
		float swing = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.3F * state.walkAnimationSpeed;
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
