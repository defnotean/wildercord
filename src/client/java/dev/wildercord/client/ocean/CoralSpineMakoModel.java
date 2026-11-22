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
 * 1:1 360-degree box rig for Coral-Spine Mako (128x64 sheet):
 * Streamlined predator with hinged jaw, pectoral/pelvic wings, tall crescent caudal fin,
 * and 5 glowing reef-coral spine ridges along the back.
 */
public final class CoralSpineMakoModel extends EntityModel<OceanRenderStates.CoralMakoState> {
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart bodyFront;
	private final ModelPart bodyMid;
	private final ModelPart peduncle;
	private final ModelPart tailFin;
	private final ModelPart leftPectoral;
	private final ModelPart rightPectoral;

	public CoralSpineMakoModel(ModelPart root) {
		super(root);
		this.bodyFront = root.getChild("body_front");
		this.head = this.bodyFront.getChild("head");
		this.jaw = this.head.getChild("jaw");
		this.leftPectoral = this.bodyFront.getChild("pectoral_left");
		this.rightPectoral = this.bodyFront.getChild("pectoral_right");
		this.bodyMid = this.bodyFront.getChild("body_mid");
		this.peduncle = this.bodyMid.getChild("peduncle");
		this.tailFin = this.peduncle.getChild("tail_fin");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition bodyFront = root.addOrReplaceChild("body_front",
			CubeListBuilder.create()
				.texOffs(0, 19).addBox(-5.0F, -4.5F, -7.0F, 10, 9, 14)
				.texOffs(30, 42).addBox(-1.0F, -12.0F, -2.0F, 2, 8, 6)
				.texOffs(78, 42).addBox(-1.0F, -7.5F, -6.0F, 2, 4, 6)
				.texOffs(78, 42).addBox(-1.0F, -8.5F, 1.0F, 2, 4, 6),
			PartPose.offset(0.0F, 17.5F, 0.0F));

		PartDefinition head = bodyFront.addOrReplaceChild("head",
			CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -4.0F, -12.0F, 10, 7, 12),
			PartPose.offset(0.0F, 0.0F, -6.5F));
		head.addOrReplaceChild("jaw",
			CubeListBuilder.create().texOffs(44, 0).addBox(-4.0F, 0.0F, -10.0F, 8, 3, 10),
			PartPose.offset(0.0F, 1.5F, 0.0F));

		bodyFront.addOrReplaceChild("pectoral_left",
			CubeListBuilder.create().texOffs(46, 42).addBox(0.0F, -1.0F, -3.0F, 10, 2, 6),
			PartPose.offsetAndRotation(4.5F, 2.5F, -2.0F, 0.0F, -0.25F, 0.25F));
		bodyFront.addOrReplaceChild("pectoral_right",
			CubeListBuilder.create().texOffs(46, 42).mirror().addBox(-10.0F, -1.0F, -3.0F, 10, 2, 6),
			PartPose.offsetAndRotation(-4.5F, 2.5F, -2.0F, 0.0F, 0.25F, -0.25F));

		PartDefinition bodyMid = bodyFront.addOrReplaceChild("body_mid",
			CubeListBuilder.create()
				.texOffs(48, 19).addBox(-4.0F, -3.5F, 0.0F, 8, 7, 10)
				.texOffs(78, 42).addBox(-1.0F, -7.0F, 1.0F, 2, 4, 6)
				.texOffs(46, 50).addBox(2.5F, 2.5F, 3.0F, 5, 1, 4)
				.texOffs(46, 50).mirror().addBox(-7.5F, 2.5F, 3.0F, 5, 1, 4),
			PartPose.offset(0.0F, 0.0F, 6.5F));

		PartDefinition peduncle = bodyMid.addOrReplaceChild("peduncle",
			CubeListBuilder.create()
				.texOffs(84, 19).addBox(-2.5F, -2.5F, 0.0F, 5, 5, 8)
				.texOffs(78, 42).addBox(-1.0F, -5.5F, 1.0F, 2, 4, 6),
			PartPose.offset(0.0F, 0.0F, 9.5F));

		peduncle.addOrReplaceChild("tail_fin",
			CubeListBuilder.create()
				.texOffs(0, 42).addBox(-1.0F, -10.0F, 0.0F, 2, 12, 6)
				.texOffs(16, 42).addBox(-1.0F, 0.0F, 0.0F, 2, 8, 5),
			PartPose.offset(0.0F, 0.0F, 7.5F));

		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(OceanRenderStates.CoralMakoState state) {
		super.setupAnim(state);
		float swim = state.ageInTicks * 0.28F;
		this.bodyFront.yRot = Mth.sin(swim) * 0.08F;
		this.bodyMid.yRot = Mth.sin(swim - 0.8F) * 0.22F;
		this.peduncle.yRot = Mth.sin(swim - 1.6F) * 0.30F;
		this.tailFin.yRot = Mth.sin(swim - 2.3F) * 0.42F;

		float snap = state.lungeTicks > 0 ? 0.52F : Math.max(0.0F, Mth.sin(swim * 0.5F)) * 0.14F;
		this.jaw.xRot = snap;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.4F;
		this.leftPectoral.zRot = 0.22F + Mth.sin(swim * 0.5F) * 0.08F;
		this.rightPectoral.zRot = -0.22F - Mth.sin(swim * 0.5F) * 0.08F;
	}
}
