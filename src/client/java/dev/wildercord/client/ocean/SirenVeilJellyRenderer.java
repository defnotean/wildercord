package dev.wildercord.client.ocean;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.ocean.SirenVeilJelly;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class SirenVeilJellyRenderer extends MobRenderer<SirenVeilJelly, OceanRenderStates.SirenJellyState, SirenVeilJellyModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("siren_veil_jelly"), "main");
	private static final ResourceLocation SKIN = Wildercord.id("textures/entity/ocean/siren_veil_jelly.png");
	private static final ResourceLocation GLOW = Wildercord.id("textures/entity/ocean/siren_veil_jelly_glow.png");

	public SirenVeilJellyRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new SirenVeilJellyModel(ctx.bakeLayer(LAYER)), 0.55F);
		addLayer(new CoreGlowLayer(this));
	}

	@Override
	public OceanRenderStates.SirenJellyState createRenderState() {
		return new OceanRenderStates.SirenJellyState();
	}

	@Override
	public ResourceLocation getTextureLocation(OceanRenderStates.SirenJellyState state) {
		return SKIN;
	}

	@Override
	public void extractRenderState(SirenVeilJelly entity, OceanRenderStates.SirenJellyState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.pulseTicks = entity.getPulseTicks();
	}

	@Override
	protected void scale(OceanRenderStates.SirenJellyState state, PoseStack pose) {
		pose.scale(0.5F, 0.5F, 0.5F);
	}

	private static final class CoreGlowLayer extends RenderLayer<OceanRenderStates.SirenJellyState, SirenVeilJellyModel> {
		private final RenderType glowType = RenderTypes.eyes(GLOW);

		CoreGlowLayer(RenderLayerParent<OceanRenderStates.SirenJellyState, SirenVeilJellyModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, OceanRenderStates.SirenJellyState state, float yaw, float pitch) {
			if (state.isInvisible) return;
			RenderCompat.submitModel(collector.order(1), getParentModel(), state, pose, glowType, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, state.outlineColor);
		}
	}
}
