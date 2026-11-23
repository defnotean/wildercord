package dev.wildercord.client.ocean;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.ocean.CoralSpineMako;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class CoralSpineMakoRenderer extends MobRenderer<CoralSpineMako, OceanRenderStates.CoralMakoState, CoralSpineMakoModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("coral_spine_mako"), "main");
	private static final ResourceLocation SKIN = Wildercord.id("textures/entity/ocean/coral_spine_mako.png");
	private static final ResourceLocation GLOW = Wildercord.id("textures/entity/ocean/coral_spine_mako_glow.png");

	public CoralSpineMakoRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new CoralSpineMakoModel(ctx.bakeLayer(LAYER)), 0.65F);
		addLayer(new GlowLayer(this));
	}

	@Override
	public OceanRenderStates.CoralMakoState createRenderState() {
		return new OceanRenderStates.CoralMakoState();
	}

	@Override
	public ResourceLocation getTextureLocation(OceanRenderStates.CoralMakoState state) {
		return SKIN;
	}

	@Override
	public void extractRenderState(CoralSpineMako entity, OceanRenderStates.CoralMakoState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.lungeTicks = entity.getLungeTicks();
	}

	@Override
	protected void scale(OceanRenderStates.CoralMakoState state, PoseStack pose) {
		pose.scale(0.5F, 0.5F, 0.5F);
	}

	private static final class GlowLayer extends RenderLayer<OceanRenderStates.CoralMakoState, CoralSpineMakoModel> {
		private final RenderType glowType = RenderType.eyes(GLOW);

		GlowLayer(RenderLayerParent<OceanRenderStates.CoralMakoState, CoralSpineMakoModel> parent) {
			super(parent);
		}

		@Override
		public void render(PoseStack pose, MultiBufferSource collector, int light, OceanRenderStates.CoralMakoState state, float yaw, float pitch) {
			if (state.isInvisible) return;
			RenderCompat.submitModel(collector, getParentModel(), state, pose, glowType, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, RenderCompat.outlineOf(state));
		}
	}
}
