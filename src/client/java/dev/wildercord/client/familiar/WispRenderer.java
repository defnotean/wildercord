package dev.wildercord.client.familiar;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.familiar.Wisp;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

/**
 * A wisp (see {@link WispModel}): its pale skin tinted by its element, always at full brightness,
 * and drawn again as light (additive, like the Archivist's eyes) so it glows in the dark. It flares
 * brighter for a moment when it casts, chimes or bonds, and a familiar grows a little with each level.
 */
public class WispRenderer extends MobRenderer<Wisp, WispRenderState, WispModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("wisp"), "main");
	private static final Identifier TEXTURE = Wildercord.id("textures/entity/wisp.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/wisp_glow.png");

	public WispRenderer(EntityRendererProvider.Context context) {
		super(context, new WispModel(context.bakeLayer(LAYER)), 0.0F);
		addLayer(new Glow(this));
	}

	@Override
	public Identifier getTextureLocation(WispRenderState state) {
		return TEXTURE;
	}

	@Override
	public WispRenderState createRenderState() {
		return new WispRenderState();
	}

	@Override
	public void extractRenderState(Wisp wisp, WispRenderState state, float partial) {
		super.extractRenderState(wisp, state, partial);
		state.color = wisp.color();
		state.level = wisp.familiarLevel();
		state.flare = Mth.clamp(1 - wisp.sinceFlare(partial) / 10F, 0, 1);
		state.climb = (float) wisp.getDeltaMovement().y;
		// It never turns red or topples: it's light.
		state.hasRedOverlay = false;
		state.deathTime = 0;
	}

	@Override
	protected int getModelTint(WispRenderState state) {
		return 0xFF000000 | state.color;
	}

	@Override
	protected void scale(WispRenderState state, PoseStack poseStack) {
		float s = 1.0F + 0.12F * (state.level - 1);
		poseStack.scale(s, s, s);
	}

	@Override
	protected int getBlockLightLevel(Wisp wisp, BlockPos pos) {
		return 15;
	}

	/** The wisp again, as light: its core and tail glow in its colour, brighter as it flares. */
	private static final class Glow extends RenderLayer<WispRenderState, WispModel> {
		private final RenderType renderType = RenderTypes.eyes(GLOW);

		Glow(RenderLayerParent<WispRenderState, WispModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, WispRenderState state, float yRot, float xRot) {
			if (state.isInvisible) {
				return;
			}
			float pulse = 0.6F + 0.1F * Mth.sin(state.ageInTicks * 0.2F) + 0.4F * state.flare;
			int alpha = Mth.clamp((int) (pulse * 255), 0, 255);
			collector.order(1).submitModel(getParentModel(), state, poseStack, renderType, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
				(alpha << 24) | state.color, null, state.outlineColor);
		}
	}
}
