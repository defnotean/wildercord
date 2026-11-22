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
 * 1:1 360-degree high-voxel rig for Dread-Nautilus Sentinel (256x128 sheet, 0.5x sub-voxel scale):
 * True 12-segment coiled radial bronze-and-verdigris spiral shell (300 deg sweep), stepped spiral
 * side hub bosses, beveled 4-piece aperture hood framing a pulsing sea-lantern cyan focal eye,
 * and 4 articulated multi-joint outer pincers + inner feeler appendages.
 */
public final class DreadNautilusModel extends EntityModel<OceanRenderStates.DreadNautilusState> {
	private final ModelPart shell;
	private final ModelPart outerLeft;
	private final ModelPart outerRight;
	private final ModelPart innerLeft;
	private final ModelPart innerRight;

	public DreadNautilusModel(ModelPart root) {
		super(root);
		this.shell = root.getChild("shell");
		this.outerLeft = this.shell.getChild("outer_pincer_left");
		this.outerRight = this.shell.getChild("outer_pincer_right");
		this.innerLeft = this.shell.getChild("inner_feeler_left");
		this.innerRight = this.shell.getChild("inner_feeler_right");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition shell = root.addOrReplaceChild("shell",
			CubeListBuilder.create()
				// Stepped symmetrical bronze spiral hub bosses on left & right flanks
				.texOffs(128, 0).addBox(-11.0F, -6.0F, -6.0F, 22, 12, 12)
				.texOffs(128, 50).addBox(-13.0F, -3.5F, -3.5F, 26, 7, 7)
				// Dark recessed throat & 4-piece beveled bronze aperture frame around focal eye
				.texOffs(128, 26).addBox(-7.5F, -11.0F, -22.0F, 15, 7, 9)
				.texOffs(176, 26).addBox(-6.0F, -5.0F, -19.5F, 12, 11, 7)
				.texOffs(214, 26).addBox(-3.5F, -2.5F, -21.0F, 7, 7, 4),
			PartPose.offset(0.0F, 4.0F, 2.0F));

		// True 12-segment logarithmic coiled radial spiral shell sweeping from aperture up & around 300 degrees
		for (int i = 0; i < 12; i++) {
			float frac = i / 11.0F;
			float angle = -0.35F + frac * 5.05F; // -20 deg to +270 deg around Z/X coil in YZ plane
			float radius = 13.5F - frac * 5.2F;
			float cy = Mth.sin(angle) * radius;
			float cz = -Mth.cos(angle) * radius;
			// Alternate wide corroded copper chamber plates and narrower recessed verdigris suture ribs
			int u = (i % 2 == 0) ? 0 : 60;
			shell.addOrReplaceChild("coil_chamber_" + i,
				CubeListBuilder.create().texOffs(u, 0).addBox(-9.0F, -5.5F, -5.0F, 18, 11, 10),
				PartPose.offsetAndRotation(0.0F, cy, cz, angle + Mth.HALF_PI, 0.0F, 0.0F));
		}

		// 4 Articulated Mechanical Pincers & Inner Feelers (matching Turret Beam Pose 1:1)
		PartDefinition ol = shell.addOrReplaceChild("outer_pincer_left",
			CubeListBuilder.create().texOffs(0, 48).addBox(-2.0F, -2.0F, -13.0F, 4, 4, 13),
			PartPose.offsetAndRotation(6.5F, 3.0F, -17.0F, 0.12F, -0.25F, 0.0F));
		ol.addOrReplaceChild("claw_tip_l",
			CubeListBuilder.create().texOffs(34, 48).addBox(-2.0F, -2.0F, -11.0F, 3, 4, 11),
			PartPose.offsetAndRotation(-0.5F, 0.0F, -12.0F, 0.0F, 0.65F, 0.0F));

		PartDefinition or = shell.addOrReplaceChild("outer_pincer_right",
			CubeListBuilder.create().texOffs(0, 48).mirror().addBox(-2.0F, -2.0F, -13.0F, 4, 4, 13),
			PartPose.offsetAndRotation(-6.5F, 3.0F, -17.0F, 0.12F, 0.25F, 0.0F));
		or.addOrReplaceChild("claw_tip_r",
			CubeListBuilder.create().texOffs(34, 48).mirror().addBox(-1.0F, -2.0F, -11.0F, 3, 4, 11),
			PartPose.offsetAndRotation(0.5F, 0.0F, -12.0F, 0.0F, -0.65F, 0.0F));

		shell.addOrReplaceChild("inner_feeler_left",
			CubeListBuilder.create().texOffs(64, 48).addBox(-1.0F, -1.0F, -10.0F, 2, 2, 10),
			PartPose.offsetAndRotation(2.5F, 4.0F, -17.5F, 0.16F, -0.08F, 0.0F));

		shell.addOrReplaceChild("inner_feeler_right",
			CubeListBuilder.create().texOffs(64, 48).mirror().addBox(-1.0F, -1.0F, -10.0F, 2, 2, 10),
			PartPose.offsetAndRotation(-2.5F, 4.0F, -17.5F, 0.16F, 0.08F, 0.0F));

		return LayerDefinition.create(mesh, 256, 128);
	}

	@Override
	public void setupAnim(OceanRenderStates.DreadNautilusState state) {
		super.setupAnim(state);
		float bob = Mth.sin(state.ageInTicks * 0.1F);
		float flare = state.laserTicks > 0 ? 0.34F : 0.08F * bob;
		shell.y = 4.0F + bob * 1.4F;
		shell.xRot = state.xRot * Mth.DEG_TO_RAD * 0.35F;
		outerLeft.yRot = -0.25F - flare;
		outerRight.yRot = 0.25F + flare;
		innerLeft.xRot = 0.16F + 0.12F * Mth.sin(state.ageInTicks * 0.22F);
		innerRight.xRot = 0.16F - 0.12F * Mth.sin(state.ageInTicks * 0.22F);
	}
}
