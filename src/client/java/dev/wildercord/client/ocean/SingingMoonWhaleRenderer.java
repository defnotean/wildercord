package dev.wildercord.client.ocean;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.ocean.SingingMoonWhale;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class SingingMoonWhaleRenderer extends MobRenderer<SingingMoonWhale, OceanRenderStates.MoonWhaleState, SingingMoonWhaleModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("singing_moon_whale"), "main");
	private static final ResourceLocation SKIN = Wildercord.id("textures/entity/ocean/singing_moon_whale.png");
	private static final ResourceLocation GLOW = Wildercord.id("textures/entity/ocean/singing_moon_whale_glow.png");

	public SingingMoonWhaleRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new SingingMoonWhaleModel(ctx.bakeLayer(LAYER)), 1.7F);
		addLayer(new StarBarnacleGlowLayer(this));
	}

	@Override
	public OceanRenderStates.MoonWhaleState createRenderState() {
		return new OceanRenderStates.MoonWhaleState();
	}

	@Override
	public ResourceLocation getTextureLocation(OceanRenderStates.MoonWhaleState state) {
		return SKIN;
	}

	@Override
	public void extractRenderState(SingingMoonWhale entity, OceanRenderStates.MoonWhaleState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.singing = entity.isSinging();
	}

	@Override
	protected void scale(OceanRenderStates.MoonWhaleState state, PoseStack pose) {
		pose.scale(0.72F, 0.72F, 0.72F);
	}

	private static final class StarBarnacleGlowLayer extends RenderLayer<OceanRenderStates.MoonWhaleState, SingingMoonWhaleModel> {
		private final RenderType glowType = RenderType.eyes(GLOW);

		StarBarnacleGlowLayer(RenderLayerParent<OceanRenderStates.MoonWhaleState, SingingMoonWhaleModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, OceanRenderStates.MoonWhaleState state, float yaw, float pitch) {
			if (state.isInvisible) return;
			float pulse = state.singing ? (0.75F + 0.25F * Mth.sin(state.ageInTicks * 0.35F)) : 0.65F;
			int a = Mth.clamp(Math.round(pulse * 255), 0, 255);
			RenderCompat.submitModel(collector.order(1), getParentModel(), state, pose, glowType, light, OverlayTexture.NO_OVERLAY, (a << 24) | 0xFFFFFF, state.outlineColor);
		}
	}
}
