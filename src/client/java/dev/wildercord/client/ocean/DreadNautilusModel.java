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
 * 1:1 360-degree box rig for Dread-Nautilus Sentinel (128x64 sheet):
 * Chamfered coiled shell [10x20x20], twin symmetrical bronze spiral hub caps [2x6x6],
 * dark-prismarine hooded aperture [6x6x4], glowing central focal eye [4x4x2],
 * articulated outer brass pincers & inner feeler tendrils.
 */
public final class DreadNautilusModel extends EntityModel<OceanRenderStates.DreadNautilusState> {
	private final ModelPart body;
	private final ModelPart leftPincer;
	private final ModelPart rightPincer;
	private final ModelPart leftFeeler;
	private final ModelPart rightFeeler;

	public DreadNautilusModel(ModelPart root) {
		super(root);
		this.body = root.getChild("shell");
		this.leftPincer = this.body.getChild("outer_pincer_left");
		this.rightPincer = this.body.getChild("outer_pincer_right");
		this.leftFeeler = this.body.getChild("inner_feeler_left");
		this.rightFeeler = this.body.getChild("inner_feeler_right");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition shell = root.addOrReplaceChild("shell",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-5.0F, -10.0F, -10.0F, 10, 20, 20)
				.texOffs(60, 0).addBox(-7.0F, -3.0F, -3.0F, 2, 6, 6)
				.texOffs(60, 0).mirror().addBox(5.0F, -3.0F, -3.0F, 2, 6, 6)
				.texOffs(60, 12).addBox(-4.0F, -8.0F, -13.0F, 8, 5, 5)
				.texOffs(60, 22).addBox(-3.0F, -3.0F, -12.0F, 6, 6, 4)
				.texOffs(80, 22).addBox(-2.0F, -2.0F, -13.0F, 4, 4, 2),
			PartPose.offset(0.0F, 13.0F, 1.0F));

		PartDefinition lp = shell.addOrReplaceChild("outer_pincer_left",
			CubeListBuilder.create().texOffs(60, 32).addBox(-1.0F, -1.0F, -8.0F, 2, 2, 8),
			PartPose.offset(4.0F, 1.0F, -10.5F));
		lp.addOrReplaceChild("claw",
			CubeListBuilder.create().texOffs(80, 32).addBox(-1.5F, -1.5F, -6.0F, 3, 3, 6),
			PartPose.offset(0.0F, 0.0F, -7.5F));

		PartDefinition rp = shell.addOrReplaceChild("outer_pincer_right",
			CubeListBuilder.create().texOffs(60, 32).mirror().addBox(-1.0F, -1.0F, -8.0F, 2, 2, 8),
			PartPose.offset(-4.0F, 1.0F, -10.5F));
		rp.addOrReplaceChild("claw",
			CubeListBuilder.create().texOffs(80, 32).mirror().addBox(-1.5F, -1.5F, -6.0F, 3, 3, 6),
			PartPose.offset(0.0F, 0.0F, -7.5F));

		shell.addOrReplaceChild("inner_feeler_left",
			CubeListBuilder.create().texOffs(98, 32).addBox(-1.0F, -1.0F, -5.0F, 2, 2, 5),
			PartPose.offset(2.0F, 3.0F, -10.5F));
		shell.addOrReplaceChild("inner_feeler_right",
			CubeListBuilder.create().texOffs(98, 32).mirror().addBox(-1.0F, -1.0F, -5.0F, 2, 2, 5),
			PartPose.offset(-2.0F, 3.0F, -10.5F));

		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(OceanRenderStates.DreadNautilusState state) {
		super.setupAnim(state);
		this.body.yRot = state.yRot * Mth.DEG_TO_RAD * 0.4F;
		this.body.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F + Mth.sin(state.ageInTicks * 0.1F) * 0.06F;

		float flare = state.laserTicks > 0 ? 0.45F : 0.15F;
		float wave = Mth.sin(state.ageInTicks * 0.18F) * 0.18F;
		this.leftPincer.yRot = -flare + wave;
		this.rightPincer.yRot = flare - wave;
		this.leftFeeler.xRot = 0.25F + wave * 1.4F;
		this.rightFeeler.xRot = 0.25F - wave * 1.4F;
	}
}
