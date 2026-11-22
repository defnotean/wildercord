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
 * 1:1 360-degree Colossal Celestial Rig for the Singing Moon-Whale (256x256 sheet, 0.72x render scale):
 * Cosmic indigo-midnight hull dappled with multi-pixel constellation stars &amp; gold barnacle clusters,
 * 3-tier tapered whale rostrum, exposed pale-gold baleen plates along the upper jaw, 3-tier pleated
 * ventral throat pouch that opens serenely while singing, small curved dorsal fin, 2-segment sweeping
 * humpback pectoral wings with scalloped tubercles, and colossal horizontal notched whale tail flukes.
 */
public final class SingingMoonWhaleModel extends EntityModel<OceanRenderStates.MoonWhaleState> {
	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart rightWing1;
	private final ModelPart rightWing2;
	private final ModelPart leftWing1;
	private final ModelPart leftWing2;
	private final ModelPart midBody;
	private final ModelPart tailBase;
	private final ModelPart tailPeduncle;
	private final ModelPart flukes;

	public SingingMoonWhaleModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = this.body.getChild("head");
		this.jaw = this.head.getChild("jaw");
		this.rightWing1 = this.body.getChild("right_wing_1");
		this.rightWing2 = this.rightWing1.getChild("right_wing_2");
		this.leftWing1 = this.body.getChild("left_wing_1");
		this.leftWing2 = this.leftWing1.getChild("left_wing_2");
		this.midBody = this.body.getChild("mid_body");
		this.tailBase = this.midBody.getChild("tail_base");
		this.tailPeduncle = this.tailBase.getChild("tail_peduncle");
		this.flukes = this.tailPeduncle.getChild("flukes");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Main Colossal Trunk + Curved Dorsal Fin
		PartDefinition body = root.addOrReplaceChild("body",
			CubeListBuilder.create().texOffs(0, 0).addBox(-12.0F, -10.0F, -13.0F, 24, 20, 26),
			PartPose.offset(0.0F, 6.0F, 0.0F));
		body.addOrReplaceChild("dorsal_fin",
			CubeListBuilder.create().texOffs(57, 110).addBox(-1.5F, -7.0F, -3.5F, 3, 7, 9),
			PartPose.offsetAndRotation(0.0F, -9.5F, 6.0F, -0.35F, 0.0F, 0.0F));

		// 3-Tier Tapered Whale Cranium & Rostrum + Exposed Pale-Gold Baleen Plates
		PartDefinition head = body.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 47).addBox(-11.0F, -8.5F, -20.0F, 22, 15, 20)
				.texOffs(77, 83).addBox(-9.0F, -5.5F, -31.5F, 18, 11, 12)
				.texOffs(143, 110).addBox(-6.5F, -3.5F, -38.0F, 13, 8, 7)
				.texOffs(183, 83).addBox(-9.2F, 4.0F, -29.0F, 2, 5, 16)
				.texOffs(183, 83).mirror().addBox(7.2F, 4.0F, -29.0F, 2, 5, 16),
			PartPose.offset(0.0F, -1.5F, -12.5F));

		// 3-Tier Stepped Pleated Throat Pouch / Lower Mandible
		head.addOrReplaceChild("jaw",
			CubeListBuilder.create()
				.texOffs(85, 47).addBox(-10.0F, 0.0F, -28.0F, 20, 5, 28)
				.texOffs(0, 83).addBox(-8.0F, 4.5F, -25.0F, 16, 4, 22)
				.texOffs(0, 110).addBox(-6.0F, 8.0F, -20.0F, 12, 3, 16),
			PartPose.offset(0.0F, 6.0F, -7.0F));

		// 2-Segment Sweeping Humpback Pectoral Wings
		PartDefinition rw1 = body.addOrReplaceChild("right_wing_1",
			CubeListBuilder.create().texOffs(184, 110).addBox(-22.0F, -1.5F, -5.0F, 22, 3, 10),
			PartPose.offsetAndRotation(-11.5F, 4.5F, -4.0F, 0.05F, 0.25F, -0.18F));
		rw1.addOrReplaceChild("right_wing_2",
			CubeListBuilder.create().texOffs(0, 130).addBox(-14.0F, -1.5F, -3.5F, 14, 3, 7),
			PartPose.offsetAndRotation(-21.0F, 0.0F, 0.5F, 0.0F, 0.28F, -0.12F));

		PartDefinition lw1 = body.addOrReplaceChild("left_wing_1",
			CubeListBuilder.create().texOffs(184, 110).mirror().addBox(0.0F, -1.5F, -5.0F, 22, 3, 10),
			PartPose.offsetAndRotation(11.5F, 4.5F, -4.0F, 0.05F, -0.25F, 0.18F));
		lw1.addOrReplaceChild("left_wing_2",
			CubeListBuilder.create().texOffs(0, 130).mirror().addBox(0.0F, -1.5F, -3.5F, 14, 3, 7),
			PartPose.offsetAndRotation(21.0F, 0.0F, 0.5F, 0.0F, -0.28F, 0.12F));

		// Streamlined Tapering Tail & Monumental Horizontal Flukes
		PartDefinition mid = body.addOrReplaceChild("mid_body",
			CubeListBuilder.create().texOffs(101, 0).addBox(-9.5F, -8.0F, 0.0F, 19, 16, 20),
			PartPose.offset(0.0F, 0.5F, 12.5F));
		PartDefinition tb = mid.addOrReplaceChild("tail_base",
			CubeListBuilder.create().texOffs(182, 47).addBox(-6.5F, -6.0F, 0.0F, 13, 12, 16),
			PartPose.offset(0.0F, 0.5F, 19.0F));
		PartDefinition tp = tb.addOrReplaceChild("tail_peduncle",
			CubeListBuilder.create().texOffs(138, 83).addBox(-4.0F, -4.0F, 0.0F, 8, 8, 14),
			PartPose.offset(0.0F, 0.5F, 15.0F));
		PartDefinition flukes = tp.addOrReplaceChild("flukes",
			CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 13.0F));
		flukes.addOrReplaceChild("fluke_r",
			CubeListBuilder.create().texOffs(82, 110).addBox(-17.5F, -1.5F, -2.0F, 18, 3, 12),
			PartPose.offsetAndRotation(-0.5F, 0.0F, 0.0F, 0.0F, 0.24F, 0.0F));
		flukes.addOrReplaceChild("fluke_l",
			CubeListBuilder.create().texOffs(82, 110).mirror().addBox(-0.5F, -1.5F, -2.0F, 18, 3, 12),
			PartPose.offsetAndRotation(0.5F, 0.0F, 0.0F, 0.0F, -0.24F, 0.0F));

		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(OceanRenderStates.MoonWhaleState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float surge = Mth.sin(t * 0.085F);
		body.y = 6.0F + surge * 2.2F;
		body.xRot = Mth.cos(t * 0.085F) * 0.06F;

		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.35F + surge * 0.04F;
		head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.35F;
		jaw.xRot = state.singing ? 0.26F + 0.10F * Mth.sin(t * 0.14F) : 0.04F;

		float wingSweep = Mth.sin(t * 0.085F - 0.6F) * 0.18F;
		rightWing1.zRot = -0.18F + wingSweep;
		leftWing1.zRot = 0.18F - wingSweep;
		rightWing2.zRot = -0.12F + wingSweep * 0.7F;
		leftWing2.zRot = 0.12F - wingSweep * 0.7F;

		midBody.xRot = Mth.sin(t * 0.085F - 0.7F) * 0.10F;
		tailBase.xRot = Mth.sin(t * 0.085F - 1.4F) * 0.16F;
		tailPeduncle.xRot = Mth.sin(t * 0.085F - 2.1F) * 0.22F;
		flukes.xRot = Mth.sin(t * 0.085F - 2.8F) * 0.28F;
	}
}
