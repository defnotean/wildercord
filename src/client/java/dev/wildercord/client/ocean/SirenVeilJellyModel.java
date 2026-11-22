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
 * 1:1 360-degree box rig for Siren-Veil Jelly (64x64 sheet, entityTranslucent):
 * Crown cap [10x3x10], translucent outer bell [14x12x14], magenta inner bell [8x6x8],
 * glowing crystal core [4x4x4], and 8 octagonally arrayed trailing tentacles.
 */
public final class SirenVeilJellyModel extends EntityModel<OceanRenderStates.SirenJellyState> {
	private final ModelPart bell;
	private final ModelPart[] tentacles = new ModelPart[8];

	public SirenVeilJellyModel(ModelPart root) {
		super(root, RenderTypes::entityTranslucent);
		this.bell = root.getChild("bell");
		for (int i = 0; i < 8; i++) {
			this.tentacles[i] = this.bell.getChild("tentacle_" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		PartDefinition bell = root.addOrReplaceChild("bell",
			CubeListBuilder.create()
				.texOffs(0, 0).addBox(-7.0F, -12.0F, -7.0F, 14, 12, 14)
				.texOffs(0, 26).addBox(-5.0F, -15.0F, -5.0F, 10, 3, 10)
				.texOffs(0, 39).addBox(-4.0F, -9.0F, -4.0F, 8, 6, 8)
				.texOffs(32, 39).addBox(-2.0F, -7.0F, -2.0F, 4, 4, 4),
			PartPose.offset(0.0F, 12.0F, 0.0F));

		for (int i = 0; i < 8; i++) {
			double angle = i * (Math.PI * 2.0 / 8.0);
			boolean outer = (i % 2 == 0);
			float r = outer ? 4.8F : 2.6F;
			int len = outer ? 16 : 12;
			int u = outer ? 48 : 52;
			bell.addOrReplaceChild("tentacle_" + i,
				CubeListBuilder.create().texOffs(u, 26).addBox(-0.5F, 0.0F, -0.5F, 1, len, 1),
				PartPose.offset((float) (Math.cos(angle) * r), -1.0F, (float) (Math.sin(angle) * r)));
		}

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(OceanRenderStates.SirenJellyState state) {
		super.setupAnim(state);
		float t = state.ageInTicks * 0.14F;
		float squash = state.pulseTicks > 0 ? 0.16F : 0.06F * Mth.sin(t);
		this.bell.xScale = 1.0F + squash;
		this.bell.zScale = 1.0F + squash;
		this.bell.yScale = 1.0F - squash * 1.15F;
		this.bell.yRot = t * 0.25F;

		for (int i = 0; i < 8; i++) {
			float phase = t * 1.6F + i * 0.78F;
			this.tentacles[i].xRot = 0.22F + Mth.sin(phase) * 0.28F;
			this.tentacles[i].zRot = Mth.cos(phase) * 0.20F;
		}
	}
}
