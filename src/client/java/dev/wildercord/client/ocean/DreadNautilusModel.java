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
 * 1:1 360-degree high-voxel rig for the Dread-Nautilus Sentinel (256x256 sheet, 0.5x scale):
 * Chambered nautilus spiral constructed with strictly stepped widths (15/17/19/21) to eliminate
 * any side Z-fighting across all 14 verdigris-bronze shell ribs and raised umbilicus hubs,
 * hooded cephalopod mantle, fiery golden-amber recessed oculi, siphon jet tube, beak, and
 * 10 articulated 2-joint curling crimson-spotted tentacles.
 */
public final class DreadNautilusModel extends EntityModel<OceanRenderStates.DreadNautilusState> {
	private final ModelPart shell;
	private final ModelPart head;
	private final ModelPart[] tentBase = new ModelPart[10];
	private final ModelPart[] tentTip = new ModelPart[10];

	public DreadNautilusModel(ModelPart root) {
		super(root);
		this.shell = root.getChild("shell");
		this.head = this.shell.getChild("head");
		for (int i = 0; i < 10; i++) {
			this.tentBase[i] = this.head.getChild("tentacle_" + i);
			this.tentTip[i] = this.tentBase[i].getChild("tentacle_tip_" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Stepped non-coplanar widths: body=17, crest=15, aperture=20, ribs=19, umbilicus=21
		PartDefinition shell = root.addOrReplaceChild("shell",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-8.5F, -22.0F, -8.0F, 17, 22, 20)
				.texOffs(75, 0).addBox(-7.5F, -25.0F, -5.0F, 15, 12, 14)
				.texOffs(134, 0).addBox(-10.0F, -12.0F, -10.5F, 20, 16, 4)
				.texOffs(0, 43).addBox(-10.5F, -14.0F, -1.0F, 21, 6, 6),
			PartPose.offset(0.0F, 16.0F, 0.0F));

		// 14 radiating chambered ribs strictly wider (19) than body (17) so flanks never Z-fight
		for (int i = 0; i < 14; i++) {
			float angle = -0.35F + i * (Mth.TWO_PI * 0.72F / 14.0F);
			float r = 10.2F - i * 0.14F;
			float ry = -11.5F - Mth.sin(angle) * r;
			float rz = 2.0F + Mth.cos(angle) * r;
			shell.addOrReplaceChild("rib_" + i,
				CubeListBuilder.create().texOffs(99, 43).addBox(-9.5F, -2.0F, -3.0F, 19, 4, 6),
				PartPose.offsetAndRotation(0.0F, ry, rz, -angle + 0.15F, 0.0F, 0.0F));
		}

		// Hooded Mantle, Siphon Tube, Sculpted Eye Sockets, Central Beak & 10x 2-Segment Tentacles
		PartDefinition head = shell.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(183, 0).addBox(-7.0F, -6.5F, -9.5F, 14, 7, 10)
				.texOffs(78, 43).addBox(-8.5F, -3.5F, -8.0F, 5, 6, 5)
				.texOffs(78, 43).mirror().addBox(3.5F, -3.5F, -8.0F, 5, 6, 5)
				.texOffs(150, 43).addBox(-1.5F, 3.0F, -10.5F, 3, 3, 6)
				.texOffs(188, 43).addBox(-2.0F, -1.0F, -10.0F, 4, 4, 3),
			PartPose.offset(0.0F, -4.0F, -8.0F));

		// 10 grasping tentacles arranged cleanly in an oval ring around the beak
		for (int i = 0; i < 10; i++) {
			float theta = i * (Mth.TWO_PI / 10.0F);
			float tx = Mth.cos(theta) * 5.2F;
			float ty = Mth.sin(theta) * 3.8F + 1.0F;
			float basePitch = 0.25F - Mth.sin(theta) * 0.22F;
			float baseYaw = Mth.cos(theta) * 0.25F;
			PartDefinition t1 = head.addOrReplaceChild("tentacle_" + i,
				CubeListBuilder.create().texOffs(55, 43).addBox(-1.5F, -1.5F, -8.0F, 3, 3, 8),
				PartPose.offsetAndRotation(tx, ty, -8.5F, basePitch, baseYaw, 0.0F));
			t1.addOrReplaceChild("tentacle_tip_" + i,
				CubeListBuilder.create().texOffs(169, 43).addBox(-1.0F, -1.0F, -7.0F, 2, 2, 7),
				PartPose.offsetAndRotation(0.0F, 0.0F, -7.8F, 0.25F, 0.0F, 0.0F));
		}

		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(OceanRenderStates.DreadNautilusState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		shell.y = 16.0F + Mth.sin(t * 0.12F) * 1.4F;
		shell.xRot = Mth.sin(t * 0.10F) * 0.07F;
		shell.yRot = state.yRot * Mth.DEG_TO_RAD * 0.55F;

		float flare = state.laserTicks > 0 ? 0.38F : 0.0F;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.45F - flare * 0.12F;

		for (int i = 0; i < 10; i++) {
			float phase = t * 0.16F + i * 0.628F;
			float theta = i * (Mth.TWO_PI / 10.0F);
			tentBase[i].xRot = (0.22F - Mth.sin(theta) * 0.20F) + Mth.sin(phase) * 0.18F - Mth.sin(theta) * flare;
			tentBase[i].yRot = Mth.cos(theta) * (0.24F + flare) + Mth.cos(phase) * 0.16F;
			tentTip[i].xRot = 0.26F + Mth.sin(phase - 0.9F) * 0.24F;
		}
	}
}
