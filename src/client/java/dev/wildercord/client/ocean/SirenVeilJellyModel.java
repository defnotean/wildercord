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
 * 1:1 360-degree high-voxel rig for Siren-Veil Jelly (128x128 sheet, 0.5x sub-voxel scale):
 * Multi-stepped translucent indigo outer medusa bell, nested magenta inner bell, blazing incandescent
 * cyan crystal heart, 8 radial underside ribs, 8 zig-zag multi-segment glowing tentacles, and an
 * expanding 12-segment Shock-Ring pulse halo.
 */
public final class SirenVeilJellyModel extends EntityModel<OceanRenderStates.SirenJellyState> {
	private final ModelPart rootBell;
	private final ModelPart shockRing;
	private final ModelPart[] tentacles = new ModelPart[8];

	public SirenVeilJellyModel(ModelPart root) {
		super(root);
		this.rootBell = root.getChild("bell");
		this.shockRing = this.rootBell.getChild("shock_ring");
		for (int i = 0; i < 8; i++) {
			this.tentacles[i] = this.rootBell.getChild("tentacle_" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// 0.5x scale around Y=24: bell floats prominently around Y=4..20
		PartDefinition bell = root.addOrReplaceChild("bell",
			CubeListBuilder.create()
				// Outer translucent layered glass-indigo bell + shoulder + crown + lower rim
				.texOffs(0, 0).addBox(-11.0F, -10.0F, -11.0F, 22, 16, 22)
				.texOffs(0, 38).addBox(-9.0F, -13.0F, -9.0F, 18, 3, 18)
				.texOffs(0, 59).addBox(-6.0F, -15.0F, -6.0F, 12, 2, 12)
				.texOffs(0, 73).addBox(-12.0F, 5.0F, -12.0F, 24, 3, 24)
				// Nested glowing magenta inner dome & collar
				.texOffs(72, 38).addBox(-7.0F, -8.0F, -7.0F, 14, 11, 14)
				.texOffs(46, 100).addBox(-5.0F, 2.5F, -5.0F, 10, 3, 10)
				// Blazing cyan crystal core + bright inner heart
				.texOffs(96, 0).addBox(-4.0F, -5.0F, -4.0F, 8, 8, 8)
				.texOffs(96, 16).addBox(-2.0F, -3.0F, -2.0F, 4, 4, 4),
			PartPose.offset(0.0F, 4.0F, 0.0F));

		// 8 radial underside cavity ribs + 8 articulated 4-segment zig-zag tentacles
		for (int i = 0; i < 8; i++) {
			double angle = i * (Math.PI / 4.0);
			float ribRotY = (float) angle;
			bell.addOrReplaceChild("cavity_rib_" + i,
				CubeListBuilder.create().texOffs(48, 59).addBox(-1.0F, -1.0F, 3.0F, 2, 2, 9),
				PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.0F, ribRotY, 0.0F));

			float tx = (float) Math.cos(angle) * 6.5F;
			float tz = (float) Math.sin(angle) * 6.5F;
			boolean major = (i & 1) == 0;
			int u = major ? 0 : 10;
			PartDefinition t = bell.addOrReplaceChild("tentacle_" + i,
				CubeListBuilder.create()
					.texOffs(u, 100).addBox(-1.0F, 0.0F, -1.0F, 2, 10, 2)
					.texOffs(u, 100).addBox(-0.2F, 9.0F, -0.5F, 2, 10, 2)
					.texOffs(u, 100).addBox(-1.2F, 18.0F, -1.2F, 2, 10, 2),
				PartPose.offset(tx, 7.0F, tz));
			if (major) {
				t.addOrReplaceChild("tip_" + i,
					CubeListBuilder.create().texOffs(20, 100).addBox(-0.5F, 0.0F, -0.5F, 1, 9, 1),
					PartPose.offset(0.0F, 27.0F, 0.0F));
			}
		}

		// 12-segment octagon-ring Shock-Ring halo (hidden cleanly inside core when idle, expands on pulseTicks)
		PartDefinition ring = bell.addOrReplaceChild("shock_ring", CubeListBuilder.create(), PartPose.offset(0.0F, 2.0F, 0.0F));
		for (int i = 0; i < 12; i++) {
			float rad = (float) (i * (Math.PI * 2.0 / 12.0));
			float px = Mth.cos(rad) * 14.0F;
			float pz = Mth.sin(rad) * 14.0F;
			ring.addOrReplaceChild("seg_" + i,
				CubeListBuilder.create().texOffs(26, 100).addBox(-3.0F, -1.0F, -1.5F, 6, 2, 3),
				PartPose.offsetAndRotation(px, 0.0F, pz, 0.0F, -rad + Mth.HALF_PI, 0.0F));
		}

		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(OceanRenderStates.SirenJellyState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float pulse = state.pulseTicks > 0 ? Mth.sin((state.pulseTicks / 24.0F) * Mth.PI) : 0.0F;
		rootBell.y = 4.0F + Mth.sin(t * 0.14F) * 2.2F;
		float sq = 1.0F + 0.09F * Mth.sin(t * 0.16F) - 0.18F * pulse;
		rootBell.xScale = 1.0F + 0.2F * pulse;
		rootBell.zScale = 1.0F + 0.2F * pulse;
		rootBell.yScale = sq;

		float ringScale = state.pulseTicks > 0 ? (1.0F + (1.0F - state.pulseTicks / 24.0F) * 0.9F) : 0.001F;
		shockRing.xScale = ringScale;
		shockRing.yScale = ringScale;
		shockRing.zScale = ringScale;
		shockRing.yRot = t * 0.18F;

		for (int i = 0; i < tentacles.length; i++) {
			float phase = t * 0.16F + i * 0.785F;
			tentacles[i].xRot = 0.2F * Mth.sin(phase) + pulse * 0.35F;
			tentacles[i].zRot = 0.2F * Mth.cos(phase) + pulse * 0.35F;
		}
	}
}
