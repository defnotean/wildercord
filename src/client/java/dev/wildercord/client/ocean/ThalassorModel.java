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
 * 1:1 360-degree high-voxel rig for Thalassor, Abyssal Sovereign (512x256 sheet, 0.72x scale):
 * Obsidian dragon skull with 5-pronged glowing cyan crown, twin 4-segment curved horns, 5-ray
 * webbed turquoise gill frills, 16 protruding 3D fangs, blazing aquamarine inner maw core,
 * 14 articulated rune-etched serpentine segments with dorsal fins, giant 6-ray pectoral sea-fins,
 * and a swept crescent tail fluke.
 */
public final class ThalassorModel extends EntityModel<OceanRenderStates.ThalassorState> {
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart[] segments = new ModelPart[14];
	private final ModelPart leftWing;
	private final ModelPart rightWing;
	private final ModelPart tailFluke;

	public ThalassorModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.jaw = this.head.getChild("jaw");
		ModelPart parent = root;
		for (int i = 0; i < 14; i++) {
			ModelPart seg = parent.getChild("segment_" + i);
			this.segments[i] = seg;
			parent = seg;
		}
		this.leftWing = this.segments[1].getChild("wing_left");
		this.rightWing = this.segments[1].getChild("wing_right");
		this.tailFluke = this.segments[13].getChild("tail_fluke");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Sculpted Sea-Dragon Skull, Snout, 8 Upper Fangs, Glowing Eyes, 5-Pronged Crown & Inner Maw Core
		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-12.0F, -10.0F, -18.0F, 24, 16, 22)
				.texOffs(94, 0).addBox(-9.0F, -7.0F, -37.0F, 18, 9, 20)
				.texOffs(242, 0).addBox(-6.0F, -5.0F, -24.0F, 12, 8, 14)
				// Glowing slit eyes on flanks
				.texOffs(76, 68).addBox(11.5F, -6.0F, -15.0F, 2, 4, 6)
				.texOffs(76, 68).mirror().addBox(-13.5F, -6.0F, -15.0F, 2, 4, 6)
				// 5-Pronged Sovereign Cyan Crown
				.texOffs(0, 40).addBox(-10.0F, -16.0F, -12.0F, 20, 8, 2)
				.texOffs(46, 40).addBox(-1.5F, -24.0F, -12.5F, 3, 9, 3)
				.texOffs(46, 40).addBox(-7.0F, -22.0F, -12.5F, 3, 9, 3)
				.texOffs(46, 40).mirror().addBox(4.0F, -22.0F, -12.5F, 3, 9, 3)
				.texOffs(46, 40).addBox(-11.0F, -20.0F, -12.0F, 3, 9, 3)
				.texOffs(46, 40).mirror().addBox(8.0F, -20.0F, -12.0F, 3, 9, 3)
				// 8 Upper Fangs around snout perimeter
				.texOffs(0, 68).addBox(-8.0F, 1.5F, -36.0F, 2, 3, 2)
				.texOffs(0, 68).addBox(-4.0F, 1.5F, -36.5F, 2, 3, 2)
				.texOffs(0, 68).addBox(2.0F, 1.5F, -36.5F, 2, 3, 2)
				.texOffs(0, 68).addBox(6.0F, 1.5F, -36.0F, 2, 3, 2)
				.texOffs(0, 68).addBox(-8.5F, 1.5F, -30.0F, 2, 3, 2)
				.texOffs(0, 68).addBox(6.5F, 1.5F, -30.0F, 2, 3, 2)
				.texOffs(0, 68).addBox(-8.5F, 1.5F, -24.0F, 2, 3, 2)
				.texOffs(0, 68).addBox(6.5F, 1.5F, -24.0F, 2, 3, 2),
			PartPose.offset(0.0F, 4.0F, -12.0F));

		// Hinged Lower Jaw + 8 Lower Fangs
		head.addOrReplaceChild("jaw",
			CubeListBuilder.create()
				.texOffs(172, 0).addBox(-8.0F, 0.0F, -18.0F, 16, 6, 18)
				.texOffs(0, 68).addBox(-7.0F, -2.5F, -17.5F, 2, 3, 2)
				.texOffs(0, 68).addBox(-3.0F, -2.5F, -17.8F, 2, 3, 2)
				.texOffs(0, 68).addBox(1.0F, -2.5F, -17.8F, 2, 3, 2)
				.texOffs(0, 68).addBox(5.0F, -2.5F, -17.5F, 2, 3, 2)
				.texOffs(0, 68).addBox(-7.5F, -2.5F, -11.5F, 2, 3, 2)
				.texOffs(0, 68).addBox(5.5F, -2.5F, -11.5F, 2, 3, 2)
				.texOffs(0, 68).addBox(-7.5F, -2.5F, -6.0F, 2, 3, 2)
				.texOffs(0, 68).addBox(5.5F, -2.5F, -6.0F, 2, 3, 2),
			PartPose.offset(0.0F, 4.5F, -17.5F));

		// Twin multi-segment swept Dragon Horns
		for (int side : new int[]{-1, 1}) {
			String sName = side < 0 ? "right" : "left";
			PartDefinition hBase = head.addOrReplaceChild("horn_base_" + sName,
				CubeListBuilder.create().texOffs(60, 40).addBox(-2.0F, -12.0F, -2.0F, 4, 12, 4),
				PartPose.offsetAndRotation(side * 8.5F, -9.0F, -4.0F, -0.58F, side * 0.22F, side * 0.34F));
			hBase.addOrReplaceChild("horn_tip_" + sName,
				CubeListBuilder.create().texOffs(78, 40).addBox(-1.5F, -10.0F, -1.5F, 3, 10, 3),
				PartPose.offsetAndRotation(0.0F, -11.0F, 0.0F, 0.42F, 0.0F, side * 0.14F));

			// Fan-like 5-ray webbed Glowing Gill Frills
			PartDefinition frill = head.addOrReplaceChild("gill_frill_" + sName,
				CubeListBuilder.create().texOffs(92, 40).addBox(side < 0 ? -18.0F : 0.0F, -1.0F, -4.0F, 18, 2, 16),
				PartPose.offsetAndRotation(side * 11.5F, -3.0F, -3.0F, 0.0F, -side * 0.42F, side * 0.15F));
			for (int r = 0; r < 5; r++) {
				float fanAng = (r - 2) * 0.22F;
				frill.addOrReplaceChild("ray_" + sName + "_" + r,
					CubeListBuilder.create().texOffs(162, 40).addBox(side < 0 ? -18.0F : 0.0F, -1.0F, -1.0F, 2, 2, 18),
					PartPose.offsetAndRotation(side * 1.0F, 0.0F, 0.0F, fanAng, side * (Mth.HALF_PI - 0.15F), 0.0F));
			}
		}

		// 14 Articulated Tapering Rune-Etched Serpentine Body Segments + Winged Pectorals + Dorsal Fins + Forked Tail
		PartDefinition chainParent = root;
		for (int i = 0; i < 14; i++) {
			int w = (i < 4 ? 22 : (i < 10 ? 18 : 14));
			int h = (i < 4 ? 20 : (i < 10 ? 17 : 13));
			int d = (i < 4 ? 16 : (i < 10 ? 15 : 14));
			int segU = (i < 4) ? 296 : (i < 10 ? 374 : 442);
			int offZ = (i == 0) ? -2 : 13;
			CubeListBuilder segCube = CubeListBuilder.create()
				.texOffs(segU, 0).addBox(-w * 0.5F, -h * 0.5F, 0.0F, w, h, d)
				// Pale sea-green ribbed belly armor plate beneath each segment (tapers with tail)
				.texOffs(i < 10 ? 10 : 152, 68).addBox(i < 10 ? -9.0F : -6.0F, h * 0.5F - 2.0F, 0.5F, i < 10 ? 18 : 12, i < 10 ? 4 : 3, i < 10 ? 14 : 12)
				// Swept dorsal crest fin along every segment
				.texOffs(348, 40).addBox(-1.5F, -h * 0.5F - 9.0F, 1.0F, 3, 11, 14);

			PartPose pose = (i == 0) ? PartPose.offset(0.0F, 4.0F, offZ) : PartPose.offset(0.0F, 0.2F, offZ);
			PartDefinition seg = chainParent.addOrReplaceChild("segment_" + i, segCube, pose);

			if (i == 1) {
				for (int side : new int[]{-1, 1}) {
					String sName = side < 0 ? "right" : "left";
					PartDefinition wing = seg.addOrReplaceChild("wing_" + sName,
						CubeListBuilder.create()
							.texOffs(204, 40).addBox(side < 0 ? -28.0F : 0.0F, -1.5F, -4.0F, 28, 3, 18),
						PartPose.offsetAndRotation(side * 10.0F, 1.0F, 4.0F, 0.0F, -side * 0.22F, side * 0.16F));
					for (int r = 0; r < 5; r++) {
						wing.addOrReplaceChild("wray_" + r,
							CubeListBuilder.create().texOffs(298, 40).addBox(side < 0 ? -22.0F : 0.0F, -1.5F, -1.5F, 2, 3, 22),
							PartPose.offsetAndRotation(side * 2.0F, 0.0F, 0.0F, 0.0F, side * (Mth.HALF_PI - (r - 2) * 0.14F), 0.0F));
					}
				}
			}

			if (i == 13) {
				seg.addOrReplaceChild("tail_fluke",
					CubeListBuilder.create()
						.texOffs(384, 40).addBox(-15.0F, -2.0F, 0.0F, 30, 4, 16)
						.texOffs(94, 68).addBox(-20.0F, -1.5F, 8.0F, 16, 3, 12)
						.texOffs(94, 68).mirror().addBox(4.0F, -1.5F, 8.0F, 16, 3, 12),
					PartPose.offset(0.0F, 0.0F, 13.0F));
			}
			chainParent = seg;
		}

		return LayerDefinition.create(mesh, 512, 256);
	}

	@Override
	public void setupAnim(OceanRenderStates.ThalassorState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.45F;
		head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.45F;
		jaw.xRot = Mth.clamp(state.jawOpen, 0.0F, 1.0F) * 0.72F + 0.08F * Mth.sin(t * 0.18F);

		float amp = state.attackMode == 2 ? 0.28F : 0.16F;
		float freq = state.attackMode == 2 ? 0.34F : 0.2F;
		for (int i = 0; i < segments.length; i++) {
			float phase = t * freq - i * 0.45F;
			segments[i].yRot = Mth.sin(phase) * amp;
			segments[i].xRot = Mth.cos(phase * 0.7F) * (amp * 0.3F);
		}
		float wingFlap = Mth.sin(t * 0.16F) * 0.22F;
		leftWing.zRot = 0.16F + wingFlap;
		rightWing.zRot = -0.16F - wingFlap;
		tailFluke.yRot = Mth.sin(t * freq - 6.0F) * 0.28F;
	}
}
