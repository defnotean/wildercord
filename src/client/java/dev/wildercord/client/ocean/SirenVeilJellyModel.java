package dev.wildercord.client.ocean;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * 1:1 360-degree translucent bioluminescent rig for the Siren-Veil Jelly (256x256 sheet, 0.5x scale).
 *
 * <p>Parts are inserted in strict painter's order so the translucent outer glass canopy blends
 * over the glowing amethyst diamond crystal nucleus and swirling frilled oral arms inside.</p>
 */
public final class SirenVeilJellyModel extends EntityModel<OceanRenderStates.SirenJellyState> {
	private final ModelPart core;
	private final ModelPart bell;
	private final ModelPart[] oralUpper = new ModelPart[4];
	private final ModelPart[] oralLower = new ModelPart[4];
	private final ModelPart[] tendSeg1 = new ModelPart[8];
	private final ModelPart[] tendSeg2 = new ModelPart[8];
	private final ModelPart[] tendSeg3 = new ModelPart[8];

	public SirenVeilJellyModel(ModelPart root) {
		super(root, RenderTypes::entityTranslucent);
		this.core = root.getChild("core");
		for (int i = 0; i < 4; i++) {
			this.oralUpper[i] = root.getChild("oral_" + i);
			this.oralLower[i] = this.oralUpper[i].getChild("oral_tip_" + i);
		}
		for (int i = 0; i < 8; i++) {
			this.tendSeg1[i] = root.getChild("tendril_" + i);
			this.tendSeg2[i] = this.tendSeg1[i].getChild("tendril_mid_" + i);
			this.tendSeg3[i] = this.tendSeg2[i].getChild("tendril_tip_" + i);
		}
		this.bell = root.getChild("bell");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// 1. Inner faceted crystalline diamond prism core (added FIRST for clean translucency depth)
		root.addOrReplaceChild("core",
			CubeListBuilder.create()
				.texOffs(208, 35).addBox(-2.0F, -2.5F, -2.0F, 4, 5, 4)
				.texOffs(94, 35).addBox(-3.0F, -11.5F, -3.0F, 6, 11, 6)
				.texOffs(152, 35).addBox(-5.0F, -9.5F, -2.0F, 10, 7, 4)
				.texOffs(65, 35).addBox(-2.0F, -9.5F, -5.0F, 4, 7, 10),
			PartPose.offset(0.0F, -3.0F, 0.0F));

		// 2. Four Inner Frilled Oral Arms descending beneath crystal core
		float[][] oralAngles = {{4.0F, 0.0F}, {-4.0F, 0.0F}, {0.0F, 4.0F}, {0.0F, -4.0F}};
		for (int i = 0; i < 4; i++) {
			PartDefinition up = root.addOrReplaceChild("oral_" + i,
				CubeListBuilder.create().texOffs(126, 35).addBox(-2.5F, 0.0F, -1.0F, 5, 11, 2),
				PartPose.offsetAndRotation(oralAngles[i][0], -2.0F, oralAngles[i][1], 0.0F, i * 0.785F, 0.0F));
			up.addOrReplaceChild("oral_tip_" + i,
				CubeListBuilder.create().texOffs(141, 35).addBox(-1.5F, 0.0F, -1.0F, 3, 11, 2),
				PartPose.offset(0.0F, 11.0F, 0.0F));
		}

		// 3. Eight 3-Joint Articulated Glowing Outer Tentacles around bell circumference
		double[] tendrilAngles = new double[8];
		for (int i = 0; i < 8; i++) {
			tendrilAngles[i] = i * (Math.PI / 4.0);
			float r = (i % 2 == 0) ? 10.5F : 11.5F;
			float x = (float) Math.cos(tendrilAngles[i]) * r;
			float z = (float) Math.sin(tendrilAngles[i]) * r;
			PartDefinition s1 = root.addOrReplaceChild("tendril_" + i,
				CubeListBuilder.create().texOffs(181, 35).addBox(-1.0F, 0.0F, -1.0F, 2, 9, 2),
				PartPose.offset(x, -1.0F, z));
			PartDefinition s2 = s1.addOrReplaceChild("tendril_mid_" + i,
				CubeListBuilder.create().texOffs(190, 35).addBox(-1.0F, 0.0F, -1.0F, 2, 9, 2),
				PartPose.offset(0.0F, 9.0F, 0.0F));
			s2.addOrReplaceChild("tendril_tip_" + i,
				CubeListBuilder.create().texOffs(199, 35).addBox(-1.0F, 0.0F, -1.0F, 2, 8, 2),
				PartPose.offset(0.0F, 9.0F, 0.0F));
		}

		// 4. Outer Semi-Translucent Canopy Bell, Radial Emissive Ribs & Scalloped Petal Rim (rendered LAST!)
		PartDefinition bell = root.addOrReplaceChild("bell",
			CubeListBuilder.create()
				.texOffs(0, 35).addBox(-8.0F, -19.0F, -8.0F, 16, 5, 16)
				.texOffs(113, 0).addBox(-12.0F, -14.0F, -12.0F, 24, 8, 24)
				.texOffs(0, 0).addBox(-14.0F, -6.0F, -14.0F, 28, 6, 28),
			PartPose.ZERO);

		// 8 radiating structural veins + scalloped rim petal flaps
		for (int i = 0; i < 8; i++) {
			float angle = i * Mth.PI * 0.25F;
			float rRib = 11.8F;
			bell.addOrReplaceChild("bell_rib_" + i,
				CubeListBuilder.create().texOffs(119, 35).addBox(-1.0F, -13.0F, -0.5F, 2, 13, 1),
				PartPose.offsetAndRotation((float) Math.cos(angle) * rRib, -0.5F, (float) Math.sin(angle) * rRib, -0.22F, -angle + Mth.HALF_PI, 0.0F));
			float rRim = 13.8F;
			bell.addOrReplaceChild("bell_rim_tab_" + i,
				CubeListBuilder.create().texOffs(225, 35).addBox(-3.0F, 0.0F, -1.5F, 6, 3, 3),
				PartPose.offsetAndRotation((float) Math.cos(angle) * rRim, -0.5F, (float) Math.sin(angle) * rRim, 0.18F, -angle + Mth.HALF_PI, 0.0F));
		}

		return LayerDefinition.create(mesh, 256, 256);
	}

	@Override
	public void setupAnim(OceanRenderStates.SirenJellyState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float pulse = (state.pulseTicks > 0 ? 1.6F : 1.0F) * Mth.sin(t * 0.14F);
		float bellScaleXZ = 1.0F + pulse * 0.085F;
		float bellScaleY = 1.0F - pulse * 0.075F;

		bell.xScale = bellScaleXZ;
		bell.zScale = bellScaleXZ;
		bell.yScale = bellScaleY;
		bell.y = pulse * 1.4F;

		core.yRot = t * 0.045F;
		core.y = -3.0F + Mth.sin(t * 0.14F + 0.5F) * 1.1F;
		float glowScale = state.pulseTicks > 0 ? 1.18F : 1.0F + 0.06F * Mth.sin(t * 0.2F);
		core.xScale = glowScale;
		core.yScale = glowScale;
		core.zScale = glowScale;

		for (int i = 0; i < 4; i++) {
			float p = t * 0.10F + i * 1.57F;
			oralUpper[i].xRot = 0.16F * Mth.sin(p);
			oralUpper[i].zRot = 0.16F * Mth.cos(p);
			oralLower[i].xRot = 0.28F * Mth.sin(p - 0.9F);
			oralLower[i].zRot = 0.25F * Mth.cos(p - 0.9F);
		}

		for (int i = 0; i < 8; i++) {
			float p = t * 0.11F + i * 0.785F;
			tendSeg1[i].xRot = 0.14F + 0.16F * Mth.sin(p);
			tendSeg1[i].zRot = 0.14F * Mth.cos(p);
			tendSeg2[i].xRot = 0.24F * Mth.sin(p - 0.8F);
			tendSeg2[i].zRot = 0.22F * Mth.cos(p - 0.8F);
			tendSeg3[i].xRot = 0.32F * Mth.sin(p - 1.7F);
			tendSeg3[i].zRot = 0.28F * Mth.cos(p - 1.7F);
		}
	}
}
