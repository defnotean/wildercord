package dev.wildercord.client.oathhold;

import dev.wildercord.world.sites.oathhold.OathholdCitizenRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Articulated humanoid 64x64 model for all 16 {@link OathholdCitizenRules.Archetype} professions,
 * complete with role headwear/aprons/skirts, interactive greeting wave animations, and held profession props.
 */
public class OathholdCitizenModel extends EntityModel<OathholdCitizenRenderState> {
	private final ModelPart head;
	private final ModelPart hat;
	private final ModelPart body;
	private final ModelPart tunicSkirt;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart propRod;
	private final ModelPart propHead;
	private final ModelPart ledger;

	public OathholdCitizenModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.hat = this.head.getChild("hat");
		this.body = root.getChild("body");
		this.tunicSkirt = this.body.getChild("tunic_skirt");
		this.rightArm = root.getChild("right_arm");
		this.propRod = this.rightArm.getChild("prop_rod");
		this.propHead = this.rightArm.getChild("prop_head");
		this.leftArm = root.getChild("left_arm");
		this.ledger = this.leftArm.getChild("ledger");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8),
			PartPose.offset(0.0F, 0.0F, 0.0F));
		head.addOrReplaceChild("hat",
			CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, new CubeDeformation(0.45F)),
			PartPose.ZERO);

		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4),
			PartPose.offset(0.0F, 0.0F, 0.0F));
		body.addOrReplaceChild("tunic_skirt",
			CubeListBuilder.create().texOffs(40, 16).addBox(-4.0F, 0.0F, -2.0F, 8, 6, 4, new CubeDeformation(0.28F)),
			PartPose.offset(0.0F, 8.0F, 0.0F));

		PartDefinition rightArm = root.addOrReplaceChild("right_arm",
			CubeListBuilder.create().texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4),
			PartPose.offset(-5.0F, 2.0F, 0.0F));
		rightArm.addOrReplaceChild("prop_rod",
			CubeListBuilder.create().texOffs(0, 32).addBox(-1.5F, 8.0F, -8.0F, 1, 1, 10),
			PartPose.ZERO);
		rightArm.addOrReplaceChild("prop_head",
			CubeListBuilder.create().texOffs(8, 32).addBox(-2.5F, 6.5F, -10.5F, 3, 4, 2),
			PartPose.ZERO);

		PartDefinition leftArm = root.addOrReplaceChild("left_arm",
			CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4),
			PartPose.offset(5.0F, 2.0F, 0.0F));
		leftArm.addOrReplaceChild("ledger",
			CubeListBuilder.create().texOffs(20, 32).addBox(-1.5F, 6.0F, -5.5F, 5, 6, 1),
			PartPose.ZERO);

		root.addOrReplaceChild("right_leg",
			CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4),
			PartPose.offset(-1.9F, 12.0F, 0.0F));
		root.addOrReplaceChild("left_leg",
			CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4),
			PartPose.offset(1.9F, 12.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(OathholdCitizenRenderState state) {
		super.setupAnim(state);
		OathholdCitizenRules.Archetype role = OathholdCitizenRules.Archetype.byIndex(state.archetype);

		hat.visible = role != OathholdCitizenRules.Archetype.TOWNSFOLK;
		tunicSkirt.visible = role == OathholdCitizenRules.Archetype.GUILD_MASTER
			|| role == OathholdCitizenRules.Archetype.GUILD_RECEPTIONIST
			|| role == OathholdCitizenRules.Archetype.SCHOLAR_SCRIBE
			|| role == OathholdCitizenRules.Archetype.PRIEST_CLERIC
			|| role == OathholdCitizenRules.Archetype.NOBLE_PATRON
			|| role == OathholdCitizenRules.Archetype.BLACKSMITH;

		boolean hasTool = role == OathholdCitizenRules.Archetype.BLACKSMITH
			|| role == OathholdCitizenRules.Archetype.STREET_SWEEPER
			|| role == OathholdCitizenRules.Archetype.CITY_WATCHMAN
			|| role == OathholdCitizenRules.Archetype.CARPENTER_MASON
			|| role == OathholdCitizenRules.Archetype.ARENA_PROMOTER;
		propRod.visible = hasTool;
		propHead.visible = hasTool;

		ledger.visible = role == OathholdCitizenRules.Archetype.GUILD_RECEPTIONIST
			|| role == OathholdCitizenRules.Archetype.SCHOLAR_SCRIBE
			|| role == OathholdCitizenRules.Archetype.JEWELER_BANKER;

		head.yRot = state.yRot * Mth.DEG_TO_RAD;
		head.xRot = state.xRot * Mth.DEG_TO_RAD;

		float swing = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.15F * state.walkAnimationSpeed;
		rightLeg.xRot = swing;
		leftLeg.xRot = -swing;
		rightArm.xRot = -swing * 0.75F;
		leftArm.xRot = swing * 0.75F;
		rightArm.zRot = 0.05F;
		leftArm.zRot = -0.05F;

		if (ledger.visible) {
			leftArm.xRot = -0.45F + swing * 0.2F;
		}
		if (hasTool && state.walkAnimationSpeed < 0.05F) {
			rightArm.xRot = -0.35F + Mth.sin(state.ageInTicks * 0.08F) * 0.06F;
		}

		if (state.waveTick > 0) {
			float p = Math.min(1.0F, state.waveTick / 24.0F);
			rightArm.xRot = -2.2F * Mth.sin(p * Mth.PI);
			rightArm.zRot = 0.35F * Mth.sin(state.ageInTicks * 0.45F);
		}
	}
}
