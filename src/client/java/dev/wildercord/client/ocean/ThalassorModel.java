package dev.wildercord.client.ocean;

import dev.wildercord.ocean.OceanRules;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 1:1 360-degree box rig for Thalassor, Abyssal Sovereign [Boss] (128x128 sheet):
 * Crowned dragon-serpent head [14x10x14] + snout [10x6x10] + hinged 40-deg lower jaw [10x4x12] +
 * twin horns & cheek frills + 14 articulated serpentine segments [12x12x10] + twin pectoral wings + tail flukes.
 */
public final class ThalassorModel extends EntityModel<OceanRenderStates.ThalassorState> {
	private static final int SEGMENTS = OceanRules.THALASSOR_SEGMENT_COUNT;
	private final ModelPart head;
	private final ModelPart jaw;
	private final ModelPart leftWing;
	private final ModelPart rightWing;
	private final ModelPart[] segs = new ModelPart[SEGMENTS];

	public ThalassorModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.jaw = this.head.getChild("lower_jaw");
		this.leftWing = this.head.getChild("pectoral_left");
		this.rightWing = this.head.getChild("pectoral_right");
		ModelPart cursor = this.head;
		for (int i = 0; i < SEGMENTS; i++) {
			cursor = cursor.getChild("segment_" + i);
			this.segs[i] = cursor;
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition head = root.addOrReplaceChild("head",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-7.0F, -5.0F, -14.0F, 14, 10, 14)
				.texOffs(56, 0).addBox(-5.0F, -3.0F, -24.0F, 10, 6, 10)
				.texOffs(44, 24).addBox(-5.0F, -11.0F, -9.0F, 10, 6, 2)
				.texOffs(68, 24).addBox(4.0F, -14.0F, -7.0F, 3, 10, 3)
				.texOffs(68, 24).mirror().addBox(-7.0F, -14.0F, -7.0F, 3, 10, 3)
				.texOffs(80, 24).addBox(7.0F, -5.0F, -9.0F, 1, 10, 8)
				.texOffs(80, 24).mirror().addBox(-8.0F, -5.0F, -9.0F, 1, 10, 8),
			PartPose.offset(0.0F, 12.0F, -16.0F));

		head.addOrReplaceChild("lower_jaw",
			CubeListBuilder.create().texOffs(0, 24).addBox(-5.0F, 0.0F, -12.0F, 10, 4, 12),
			PartPose.offset(0.0F, 3.0F, -11.0F));

		head.addOrReplaceChild("pectoral_left",
			CubeListBuilder.create().texOffs(0, 42).addBox(0.0F, -1.0F, -4.0F, 16, 2, 10),
			PartPose.offset(6.5F, 2.0F, 2.0F));
		head.addOrReplaceChild("pectoral_right",
			CubeListBuilder.create().texOffs(0, 42).mirror().addBox(-16.0F, -1.0F, -4.0F, 16, 2, 10),
			PartPose.offset(-6.5F, 2.0F, 2.0F));

		PartDefinition parent = head;
		for (int i = 0; i < SEGMENTS; i++) {
			CubeListBuilder segCubes = CubeListBuilder.create()
				.texOffs(0, 56).addBox(-6.0F, -6.0F, 0.0F, 12, 12, 10);
			if (i % 2 == 0) {
				segCubes.texOffs(44, 56).addBox(-1.0F, -12.0F, 1.0F, 2, 7, 8);
			}
			PartDefinition seg = parent.addOrReplaceChild("segment_" + i, segCubes,
				PartPose.offset(0.0F, 0.0F, i == 0 ? -0.5F : 9.2F));
			if (i == SEGMENTS - 1) {
				seg.addOrReplaceChild("fluke_left",
					CubeListBuilder.create().texOffs(64, 56).addBox(0.0F, -1.0F, 0.0F, 10, 2, 12),
					PartPose.offsetAndRotation(2.0F, 0.0F, 9.0F, 0.0F, -0.35F, 0.0F));
				seg.addOrReplaceChild("fluke_right",
					CubeListBuilder.create().texOffs(64, 56).mirror().addBox(-10.0F, -1.0F, 0.0F, 10, 2, 12),
					PartPose.offsetAndRotation(-2.0F, 0.0F, 9.0F, 0.0F, 0.35F, 0.0F));
			}
			parent = seg;
		}

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(OceanRenderStates.ThalassorState state) {
		super.setupAnim(state);
		float t = state.ageInTicks * 0.16F;
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.45F;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.4F;
		this.jaw.xRot = Math.max(0.08F, state.jawOpen * 0.70F + Mth.sin(t * 0.7F) * 0.06F);
		this.leftWing.zRot = 0.2F + Mth.sin(t * 0.8F) * 0.25F;
		this.rightWing.zRot = -0.2F - Mth.sin(t * 0.8F) * 0.25F;

		for (int i = 0; i < SEGMENTS; i++) {
			float amp = 0.16F + (i / (float) SEGMENTS) * 0.14F;
			this.segs[i].yRot = Mth.sin(t - i * 0.55F) * amp;
			this.segs[i].xRot = Mth.cos(t * 0.65F - i * 0.42F) * 0.07F;
			float taper = i >= 10 ? 1.0F - (i - 9) * 0.12F : 1.0F;
			this.segs[i].xScale = taper;
			this.segs[i].yScale = taper;
		}
	}
}
