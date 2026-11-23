package dev.wildercord.client.ocean;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.ocean.LumiSeahorse;
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

public final class LumiSeahorseRenderer extends MobRenderer<LumiSeahorse, OceanRenderStates.LumiSeahorseState, LumiSeahorseModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("lumi_seahorse"), "main");
	private static final ResourceLocation SKIN = Wildercord.id("textures/entity/ocean/lumi_seahorse.png");
	private static final ResourceLocation GLOW = Wildercord.id("textures/entity/ocean/lumi_seahorse_glow.png");

	public LumiSeahorseRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new LumiSeahorseModel(ctx.bakeLayer(LAYER)), 0.5F);
		addLayer(new BioGlowLayer(this));
	}

	@Override
	public OceanRenderStates.LumiSeahorseState createRenderState() {
		return new OceanRenderStates.LumiSeahorseState();
	}

	@Override
	public ResourceLocation getTextureLocation(OceanRenderStates.LumiSeahorseState state) {
		return SKIN;
	}

	@Override
	public void extractRenderState(LumiSeahorse entity, OceanRenderStates.LumiSeahorseState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.saddled = entity.isSaddled();
		state.dashTicks = entity.getDashTicks();
	}

	@Override
	protected void scale(OceanRenderStates.LumiSeahorseState state, PoseStack pose) {
		pose.scale(0.5F, 0.5F, 0.5F);
	}

	private static final class BioGlowLayer extends RenderLayer<OceanRenderStates.LumiSeahorseState, LumiSeahorseModel> {
		private final RenderType glowType = RenderType.eyes(GLOW);

		BioGlowLayer(RenderLayerParent<OceanRenderStates.LumiSeahorseState, LumiSeahorseModel> parent) {
			super(parent);
		}

		@Override
		public void render(PoseStack pose, MultiBufferSource collector, int light, OceanRenderStates.LumiSeahorseState state, float yaw, float pitch) {
			if (state.isInvisible) return;
			RenderCompat.submitModel(collector, getParentModel(), state, pose, glowType, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, RenderCompat.outlineOf(state));
		}
	}
}
