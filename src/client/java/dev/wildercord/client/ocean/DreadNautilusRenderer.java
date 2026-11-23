package dev.wildercord.client.ocean;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.ocean.DreadNautilusSentinel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class DreadNautilusRenderer extends MobRenderer<DreadNautilusSentinel, OceanRenderStates.DreadNautilusState, DreadNautilusModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("dread_nautilus_sentinel"), "main");
	private static final Identifier SKIN = Wildercord.id("textures/entity/ocean/dread_nautilus_sentinel.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/ocean/dread_nautilus_sentinel_glow.png");

	public DreadNautilusRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new DreadNautilusModel(ctx.bakeLayer(LAYER)), 0.7F);
		addLayer(new GlowLayer(this));
	}

	@Override
	public OceanRenderStates.DreadNautilusState createRenderState() {
		return new OceanRenderStates.DreadNautilusState();
	}

	@Override
	public Identifier getTextureLocation(OceanRenderStates.DreadNautilusState state) {
		return SKIN;
	}

	@Override
	public void extractRenderState(DreadNautilusSentinel entity, OceanRenderStates.DreadNautilusState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.laserTicks = entity.getLaserTicks();
	}

	@Override
	protected void scale(OceanRenderStates.DreadNautilusState state, PoseStack pose) {
		pose.scale(0.5F, 0.5F, 0.5F);
	}

	private static final class GlowLayer extends RenderLayer<OceanRenderStates.DreadNautilusState, DreadNautilusModel> {
		private final RenderType glowType = RenderTypes.eyes(GLOW);

		GlowLayer(RenderLayerParent<OceanRenderStates.DreadNautilusState, DreadNautilusModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, OceanRenderStates.DreadNautilusState state, float yaw, float pitch) {
			if (state.isInvisible) return;
			int color = state.laserTicks > 0 ? 0xFFFFFFFF : 0xB8FFFFFF;
			RenderCompat.submitModel(collector.order(1), getParentModel(), state, pose, glowType, light, OverlayTexture.NO_OVERLAY, color, state.outlineColor);
		}
	}
}
