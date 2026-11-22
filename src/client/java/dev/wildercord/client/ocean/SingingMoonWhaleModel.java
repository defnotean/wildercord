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
 * 1:1 360-degree box rig for Singing Moon-Whale (256x128 sheet):
 * ~6-block peaceful leviathan with rostrum & blowhole pad [22x16x26], golden-baleen pleated jaw
 * [20x8x24], torso [22x18x28] encrusted with cyan star-barnacles, dorsal fin, wide pectoral flippers
 * [22x2x10], and articulated fluke tail [26x2x10].
 */
public final class SingingMoonWhaleModel extends EntityModel<OceanRenderStates.MoonWhaleState> {
	private final ModelPart torso;
	private final ModelPart jaw;
	private final ModelPart leftFlipper;
	private final ModelPart rightFlipper;
	private final ModelPart tailPeduncle;
	private final ModelPart tailFluke;

	public SingingMoonWhaleModel(ModelPart root) {
		super(root);
		this.torso = root.getChild("torso");
		this.jaw = this.torso.getChild("head_rostrum").getChild("jaw");
		this.leftFlipper = this.torso.getChild("pectoral_left");
		this.rightFlipper = this.torso.getChild("pectoral_right");
		this.tailPeduncle = this.torso.getChild("tail_peduncle");
		this.tailFluke = this.tailPeduncle.getChild("tail_fluke");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition torso = root.addOrReplaceChild("torso",
			CubeListBuilder.create()
				.texOffs(0, 74).addBox(-11.0F, -9.0F, -10.0F, 22, 18, 28)
				.texOffs(132, 0).addBox(-1.0F, -14.0F, 6.0F, 2, 5, 7),
			PartPose.offset(0.0F, 12.0F, 0.0F));

		PartDefinition head = torso.addOrReplaceChild("head_rostrum",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-11.0F, -8.0F, -26.0F, 22, 16, 26)
				.texOffs(96, 0).addBox(-4.0F, -10.0F, -16.0F, 8, 2, 8),
			PartPose.offset(0.0F, -1.0F, -10.0F));
		head.addOrReplaceChild("jaw",
			CubeListBuilder.create().texOffs(0, 42).addBox(-10.0F, 0.0F, -24.0F, 20, 8, 24),
			PartPose.offset(0.0F, 3.0F, -1.0F));

		torso.addOrReplaceChild("pectoral_left",
			CubeListBuilder.create().texOffs(132, 16).addBox(0.0F, -1.0F, -5.0F, 22, 2, 10),
			PartPose.offset(10.5F, 5.0F, -4.0F));
		torso.addOrReplaceChild("pectoral_right",
			CubeListBuilder.create().texOffs(132, 16).mirror().addBox(-22.0F, -1.0F, -5.0F, 22, 2, 10),
			PartPose.offset(-10.5F, 5.0F, -4.0F));

		PartDefinition peduncle = torso.addOrReplaceChild("tail_peduncle",
			CubeListBuilder.create().texOffs(100, 32).addBox(-6.0F, -5.0F, 0.0F, 12, 10, 18),
			PartPose.offset(0.0F, -1.0F, 17.5F));
		peduncle.addOrReplaceChild("tail_fluke",
			CubeListBuilder.create().texOffs(100, 64).addBox(-13.0F, -1.0F, 0.0F, 26, 2, 10),
			PartPose.offset(0.0F, 0.0F, 17.0F));

		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(OceanRenderStates.MoonWhaleState state) {
		super.setupAnim(state);
		float t = state.ageInTicks * 0.1F;
		this.torso.xRot = state.xRot * Mth.DEG_TO_RAD * 0.4F;
		this.leftFlipper.zRot = 0.12F + Mth.sin(t) * 0.15F;
		this.rightFlipper.zRot = -0.12F - Mth.sin(t) * 0.15F;
		this.tailPeduncle.xRot = Mth.sin(t * 1.4F) * 0.16F;
		this.tailFluke.xRot = Mth.sin(t * 1.4F - 0.8F) * 0.22F;
		this.jaw.xRot = state.singing ? 0.18F + Mth.sin(t * 2.5F) * 0.05F : 0.03F;
	}
}
