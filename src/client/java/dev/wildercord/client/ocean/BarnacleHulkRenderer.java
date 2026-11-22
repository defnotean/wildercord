package dev.wildercord.client.ocean;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.ocean.BarnacleHulkDredger;
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

public final class BarnacleHulkRenderer extends MobRenderer<BarnacleHulkDredger, OceanRenderStates.BarnacleHulkState, BarnacleHulkModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("barnacle_hulk_dredger"), "main");
	private static final Identifier SKIN = Wildercord.id("textures/entity/ocean/barnacle_hulk_dredger.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/ocean/barnacle_hulk_dredger_glow.png");

	public BarnacleHulkRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new BarnacleHulkModel(ctx.bakeLayer(LAYER)), 0.75F);
		addLayer(new GlowLayer(this));
	}

	@Override
	public OceanRenderStates.BarnacleHulkState createRenderState() {
		return new OceanRenderStates.BarnacleHulkState();
	}

	@Override
	public Identifier getTextureLocation(OceanRenderStates.BarnacleHulkState state) {
		return SKIN;
	}

	@Override
	public void extractRenderState(BarnacleHulkDredger entity, OceanRenderStates.BarnacleHulkState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.slamTicks = entity.getSlamTicks();
	}

	@Override
	protected void scale(OceanRenderStates.BarnacleHulkState state, PoseStack pose) {
		pose.scale(0.5F, 0.5F, 0.5F);
	}

	private static final class GlowLayer extends RenderLayer<OceanRenderStates.BarnacleHulkState, BarnacleHulkModel> {
		private final RenderType glowType = RenderTypes.eyes(GLOW);

		GlowLayer(RenderLayerParent<OceanRenderStates.BarnacleHulkState, BarnacleHulkModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, OceanRenderStates.BarnacleHulkState state, float yaw, float pitch) {
			if (state.isInvisible) return;
			RenderCompat.submitModel(collector.order(1), getParentModel(), state, pose, glowType, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, state.outlineColor);
		}
	}
}
