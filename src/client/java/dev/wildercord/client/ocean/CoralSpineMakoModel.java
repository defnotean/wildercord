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
 * 1:1 360-degree high-voxel rig for the Coral-Spine Mako (256x256 sheet, 0.5x scale):
 * Sleek deep-teal predator shark with pale countershaded belly keel, 2-tier pointed snout
 * with serrated upper &amp; lower teeth, gill plates, armor-encrusted crimson brain/staghorn coral
 * crest &amp; branching dorsal spires, swept 2-piece pectoral wings, and asymmetric crescent shark tail.
 */
public final class CoralSpineMakoModel extends EntityModel<OceanRenderStates.CoralMakoState> {
	private final ModelPart rootBody;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart rightFin;
	private final ModelPart leftFin;
	private final ModelPart tail1;
	private final ModelPart tail2;
	private final ModelPart tailFin;

	public CoralSpineMakoModel(ModelPart root) {
		super(root);
		this.rootBody = root.getChild("torso");
		this.head = this.rootBody.getChild("head");
		this.jaw = this.head.getChild("jaw");
		this.rightFin = this.rootBody.getChild("right_fin");
		this.leftFin = this.rootBody.getChild("left_fin");
		this.tail1 = this.rootBody.getChild("tail_1");
		this.tail2 = this.tail1.getChild("tail_2");
		this.tailFin = this.tail2.getChild("tail_fin");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Torso centered at Y=12.0 (at 0.5x scale, shark swims naturally above floor)
		PartDefinition torso = root.addOrReplaceChild("torso",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-6.0F, -6.0F, -11.0F, 12, 12, 22)
				.texOffs(0, 35).addBox(-6.5F, -5.5F, -12.0F, 13, 11, 6)
				.texOffs(114, 0).addBox(-4.0F, 4.5F, -9.0F, 8, 3, 18)
				.texOffs(222, 35).addBox(-2.0F, -8.5F, -6.0F, 4, 3, 7)
				.texOffs(222, 35).addBox(-2.0F, -8.0F, 3.0F, 4, 3, 7),
			PartPose.offset(0.0F, 12.0F, 0.0F));

		// Tall Armored Shark Dorsal Fin + Branching Crimson Coral Spires
		torso.addOrReplaceChild("dorsal_fin",
			CubeListBuilder.create().texOffs(167, 0).addBox(-1.0F, -11.5F, -4.5F, 2, 12, 9),
			PartPose.offsetAndRotation(0.0F, -6.0F, -1.0F, -0.32F, 0.0F, 0.0F));
		torso.addOrReplaceChild("coral_spire_front",
			CubeListBuilder.create().texOffs(184, 35).addBox(-1.0F, -9.0F, -1.0F, 2, 9, 2),
			PartPose.offsetAndRotation(-0.5F, -7.5F, -4.5F, -0.25F, 0.0F, -0.20F));
		torso.addOrReplaceChild("coral_fork_l",
			CubeListBuilder.create().texOffs(86, 53).addBox(-1.0F, -6.0F, -1.0F, 2, 6, 2),
			PartPose.offsetAndRotation(0.8F, -11.5F, -3.5F, -0.35F, 0.0F, 0.65F));
		torso.addOrReplaceChild("coral_spire_back",
			CubeListBuilder.create().texOffs(184, 35).addBox(-1.0F, -9.0F, -1.0F, 2, 9, 2),
			PartPose.offsetAndRotation(0.5F, -7.0F, 5.5F, -0.45F, 0.0F, 0.18F));
		torso.addOrReplaceChild("pelvic_fin_r",
			CubeListBuilder.create().texOffs(118, 53).addBox(-4.5F, -1.0F, -2.0F, 5, 2, 4),
			PartPose.offsetAndRotation(-4.5F, 5.0F, 6.0F, 0.0F, 0.25F, -0.45F));
		torso.addOrReplaceChild("pelvic_fin_l",
			CubeListBuilder.create().texOffs(118, 53).mirror().addBox(-0.5F, -1.0F, -2.0F, 5, 2, 4),
			PartPose.offsetAndRotation(4.5F, 5.0F, 6.0F, 0.0F, -0.25F, 0.45F));

		// Streamlined Bullet Skull + 2-Tier Pointed Snout + Coral Brow Crests + Serrated Teeth
		PartDefinition head = torso.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(69, 0).addBox(-5.0F, -5.0F, -12.0F, 10, 10, 12)
				.texOffs(120, 35).addBox(-4.0F, -4.5F, -18.5F, 8, 7, 7)
				.texOffs(0, 53).addBox(-2.5F, -3.0F, -23.0F, 5, 4, 5)
				.texOffs(151, 35).addBox(-3.5F, 2.2F, -18.0F, 7, 2, 9)
				.texOffs(21, 53).addBox(-5.8F, -5.5F, -10.5F, 3, 3, 6)
				.texOffs(21, 53).mirror().addBox(2.8F, -5.5F, -10.5F, 3, 3, 6),
			PartPose.offset(0.0F, -0.5F, -11.0F));

		head.addOrReplaceChild("jaw",
			CubeListBuilder.create()
				.texOffs(83, 35).addBox(-4.0F, 0.0F, -10.0F, 8, 4, 10)
				.texOffs(193, 35).addBox(-3.0F, -1.4F, -9.2F, 6, 2, 8),
			PartPose.offset(0.0F, 2.5F, -6.5F));

		// Swept 2-Piece Winged Pectoral Fins
		PartDefinition rightFin = torso.addOrReplaceChild("right_fin",
			CubeListBuilder.create().texOffs(53, 53).addBox(-10.0F, -1.0F, -3.0F, 10, 2, 6),
			PartPose.offsetAndRotation(-6.0F, 3.5F, -5.0F, 0.05F, 0.32F, -0.24F));
		rightFin.addOrReplaceChild("right_fin_tip",
			CubeListBuilder.create().texOffs(95, 53).addBox(-6.5F, -1.0F, -2.0F, 7, 2, 4),
			PartPose.offsetAndRotation(-9.5F, 0.0F, 0.5F, 0.0F, 0.25F, -0.10F));

		PartDefinition leftFin = torso.addOrReplaceChild("left_fin",
			CubeListBuilder.create().texOffs(53, 53).mirror().addBox(0.0F, -1.0F, -3.0F, 10, 2, 6),
			PartPose.offsetAndRotation(6.0F, 3.5F, -5.0F, 0.05F, -0.32F, 0.24F));
		leftFin.addOrReplaceChild("left_fin_tip",
			CubeListBuilder.create().texOffs(95, 53).mirror().addBox(-0.5F, -1.0F, -2.0F, 7, 2, 4),
			PartPose.offsetAndRotation(9.5F, 0.0F, 0.5F, 0.0F, -0.25F, 0.10F));

		// 3-Segment Caudal Tail + Asymmetric Heterocercal Shark Tail Fin
		PartDefinition tail1 = torso.addOrReplaceChild("tail_1",
			CubeListBuilder.create()
				.texOffs(207, 0).addBox(-4.0F, -4.5F, 0.0F, 8, 9, 10)
				.texOffs(40, 53).addBox(-1.0F, -8.5F, 4.0F, 2, 5, 4),
			PartPose.offset(0.0F, 0.0F, 11.0F));
		PartDefinition tail2 = tail1.addOrReplaceChild("tail_2",
			CubeListBuilder.create().texOffs(39, 35).addBox(-2.5F, -3.0F, 0.0F, 5, 6, 9),
			PartPose.offset(0.0F, 0.0F, 9.5F));
		PartDefinition tailFin = tail2.addOrReplaceChild("tail_fin",
			CubeListBuilder.create(),
			PartPose.offset(0.0F, 0.0F, 8.5F));
		tailFin.addOrReplaceChild("tail_fin_upper",
			CubeListBuilder.create().texOffs(190, 0).addBox(-1.0F, -14.0F, -2.0F, 2, 15, 6),
			PartPose.offsetAndRotation(0.0F, -0.5F, 0.5F, -0.46F, 0.0F, 0.0F));
		tailFin.addOrReplaceChild("tail_fin_lower",
			CubeListBuilder.create().texOffs(68, 35).addBox(-1.0F, -1.0F, -2.0F, 2, 10, 5),
			PartPose.offsetAndRotation(0.0F, 1.0F, 0.5F, 0.42F, 0.0F, 0.0F));

		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(OceanRenderStates.CoralMakoState state) {
		super.setupAnim(state);
		float swim = Mth.sin(state.ageInTicks * 0.24F) * (0.14F + 0.18F * state.walkAnimationSpeed);
		rootBody.yRot = Mth.sin(state.ageInTicks * 0.24F) * 0.05F;
		head.yRot = -swim * 0.35F + state.yRot * Mth.DEG_TO_RAD * 0.4F;
		tail1.yRot = swim * 0.9F;
		tail2.yRot = swim * 1.25F;
		tailFin.yRot = swim * 1.45F;

		float lungeRatio = state.lungeTicks > 0 ? Math.min(1.0F, state.lungeTicks / 10.0F) : 0.0F;
		jaw.xRot = 0.08F + lungeRatio * 0.62F + Mth.sin(state.ageInTicks * 0.15F) * 0.04F;
		rightFin.zRot = -0.24F + Mth.sin(state.ageInTicks * 0.18F) * 0.06F;
		leftFin.zRot = 0.24F - Mth.sin(state.ageInTicks * 0.18F) * 0.06F;
	}
}
