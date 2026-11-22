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
 * 1:1 360-degree high-voxel rig for Singing Moon-Whale (512x256 sheet, 0.72x scale):
 * Graceful tapering 6-tier indigo leviathan with raised blowhole pad, sculpted baleen jaw housing
 * pale-gold comb plates & pleated throat grooves, 12 glowing star-barnacle clusters, swept 3-step
 * crescent pectoral flippers & horizontal crescent tail fluke rimmed in glowing cyan.
 */
public final class SingingMoonWhaleModel extends EntityModel<OceanRenderStates.MoonWhaleState> {
	private final ModelPart torso;
	private final ModelPart jaw;
	private final ModelPart midBody;
	private final ModelPart rearBody;
	private final ModelPart tailPeduncle;
	private final ModelPart tailFluke;
	private final ModelPart leftFlipper;
	private final ModelPart rightFlipper;

	public SingingMoonWhaleModel(ModelPart root) {
		super(root);
		this.torso = root.getChild("torso");
		this.jaw = this.torso.getChild("head_rostrum").getChild("jaw");
		this.leftFlipper = this.torso.getChild("pectoral_left");
		this.rightFlipper = this.torso.getChild("pectoral_right");
		this.midBody = this.torso.getChild("mid_body");
		this.rearBody = this.midBody.getChild("rear_body");
		this.tailPeduncle = this.rearBody.getChild("tail_peduncle");
		this.tailFluke = this.tailPeduncle.getChild("tail_fluke");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Broad Main Torso + Dorsal Fin + Raised Star-Barnacles
		PartDefinition torso = root.addOrReplaceChild("torso",
			CubeListBuilder.create()
				.texOffs(154, 0).addBox(-17.0F, -14.0F, -14.0F, 34, 28, 44)
				.texOffs(188, 74).addBox(-2.0F, -22.0F, 12.0F, 4, 10, 14)
				// Emissive 3D star-barnacle clusters encrusted along dorsal flanks
				.texOffs(0, 144).addBox(-10.0F, -16.0F, -4.0F, 6, 3, 6)
				.texOffs(0, 144).addBox(5.0F, -16.0F, 8.0F, 6, 3, 6)
				.texOffs(0, 144).addBox(-18.5F, -5.0F, 10.0F, 6, 3, 6)
				.texOffs(0, 144).addBox(12.5F, -4.0F, 4.0F, 6, 3, 6),
			PartPose.offset(0.0F, 8.0F, -2.0F));

		// Rounded Rostrum + Recessed Eyes + Blowhole Cap + Articulated Baleen Jaw
		PartDefinition head = torso.addOrReplaceChild("head_rostrum",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-18.0F, -13.0F, -40.0F, 36, 26, 40)
				.texOffs(130, 74).addBox(-7.0F, -16.5F, -24.0F, 14, 4, 14)
				.texOffs(0, 144).addBox(-12.0F, -15.0F, -32.0F, 6, 3, 6)
				.texOffs(0, 144).addBox(7.0F, -15.0F, -20.0F, 6, 3, 6)
				// Eyes nestled near corner of jaw
				.texOffs(26, 144).addBox(17.2F, 4.0F, -12.0F, 2, 4, 4)
				.texOffs(26, 144).mirror().addBox(-19.2F, 4.0F, -12.0F, 2, 4, 4),
			PartPose.offset(0.0F, -2.0F, -13.0F));

		head.addOrReplaceChild("jaw",
			CubeListBuilder.create()
				// Pale-gold baleen comb & pleated throat grooves
				.texOffs(0, 74).addBox(-15.0F, -4.0F, -34.0F, 30, 8, 34)
				.texOffs(312, 0).addBox(-16.0F, 1.0F, -36.0F, 32, 12, 36),
			PartPose.offset(0.0F, 7.0F, -1.0F));

		// 3-step Swept Crescent Pectoral Flippers (cyan glowing outer rims)
		torso.addOrReplaceChild("pectoral_left",
			CubeListBuilder.create()
				.texOffs(0, 120).addBox(0.0F, -2.0F, -8.0F, 34, 4, 16)
				.texOffs(102, 120).addBox(30.0F, -1.5F, -3.0F, 24, 3, 12)
				.texOffs(176, 120).addBox(50.0F, -1.0F, 1.0F, 16, 2, 8),
			PartPose.offsetAndRotation(16.0F, 8.0F, -6.0F, 0.0F, -0.34F, 0.24F));

		torso.addOrReplaceChild("pectoral_right",
			CubeListBuilder.create()
				.texOffs(0, 120).mirror().addBox(-34.0F, -2.0F, -8.0F, 34, 4, 16)
				.texOffs(102, 120).mirror().addBox(-54.0F, -1.5F, -3.0F, 24, 3, 12)
				.texOffs(176, 120).mirror().addBox(-66.0F, -1.0F, 1.0F, 16, 2, 8),
			PartPose.offsetAndRotation(-16.0F, 8.0F, -6.0F, 0.0F, 0.34F, -0.24F));

		// Tapering Mid -> Rear -> Peduncle -> Giant Crescent Tail Fluke
		PartDefinition mid = torso.addOrReplaceChild("mid_body",
			CubeListBuilder.create()
				.texOffs(312, 50).addBox(-13.0F, -11.0F, 0.0F, 26, 22, 26)
				.texOffs(0, 144).addBox(-5.0F, -13.0F, 8.0F, 6, 3, 6),
			PartPose.offset(0.0F, 0.0F, 29.0F));

		PartDefinition rear = mid.addOrReplaceChild("rear_body",
			CubeListBuilder.create().texOffs(418, 50).addBox(-9.0F, -8.0F, 0.0F, 18, 16, 24),
			PartPose.offset(0.0F, 0.0F, 25.0F));

		PartDefinition ped = rear.addOrReplaceChild("tail_peduncle",
			CubeListBuilder.create().texOffs(226, 74).addBox(-6.0F, -6.0F, 0.0F, 12, 12, 22),
			PartPose.offset(0.0F, 0.0F, 23.0F));

		ped.addOrReplaceChild("tail_fluke",
			CubeListBuilder.create()
				.texOffs(226, 120).addBox(-22.0F, -2.0F, 0.0F, 44, 4, 18)
				.texOffs(352, 120).addBox(-40.0F, -1.5F, 6.0F, 22, 3, 12)
				.texOffs(352, 120).mirror().addBox(18.0F, -1.5F, 6.0F, 22, 3, 12)
				.texOffs(0, 144).addBox(-14.0F, -3.5F, 6.0F, 6, 3, 6),
			PartPose.offset(0.0F, 0.0F, 20.0F));

		return LayerDefinition.create(mesh, 512, 256);
	}

	@Override
	public void setupAnim(OceanRenderStates.MoonWhaleState state) {
		super.setupAnim(state);
		float glide = Mth.sin(state.ageInTicks * 0.075F + state.walkAnimationPos * 0.25F);
		torso.y = 8.0F + glide * 2.0F;
		torso.xRot = state.xRot * Mth.DEG_TO_RAD * 0.25F + glide * 0.04F;
		midBody.xRot = glide * 0.06F;
		rearBody.xRot = glide * 0.09F;
		tailPeduncle.xRot = -glide * 0.14F;
		tailFluke.xRot = -glide * 0.22F;
		leftFlipper.zRot = 0.24F + glide * 0.12F;
		rightFlipper.zRot = -leftFlipper.zRot;
		jaw.xRot = state.singing ? (0.34F + 0.12F * Mth.sin(state.ageInTicks * 0.28F)) : (0.04F + 0.03F * glide);
	}
}
