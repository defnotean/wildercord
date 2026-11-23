package dev.wildercord.client.auraworld;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.OathboundBannerMarshal;
import dev.wildercord.aura.world.OathboundBannerMarshalRules;
import dev.wildercord.client.compat.RenderCompat;
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
import net.minecraft.util.Mth;

/** Renders {@link OathboundBannerMarshal} with PoseBlend and solar visor, chest core, banner, and shield glow. */
public final class OathboundBannerMarshalRenderer
		extends MobRenderer<OathboundBannerMarshal, OathboundBannerMarshalRenderState, OathboundBannerMarshalModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("oathbound_banner_marshal"), "main");
	private static final ResourceLocation SKIN = Wildercord.id("textures/entity/oathbound_banner_marshal/skin.png");
	private static final ResourceLocation GLOW = Wildercord.id("textures/entity/oathbound_banner_marshal/glow.png");
	private static final float BLEND_TICKS = 4.0F;

	public OathboundBannerMarshalRenderer(EntityRendererProvider.Context context) {
		super(context, new OathboundBannerMarshalModel(context.bakeLayer(LAYER)), 0.75F);
		addLayer(new EmissiveLayer(this));
	}

	@Override
	public OathboundBannerMarshalRenderState createRenderState() {
		return new OathboundBannerMarshalRenderState();
	}

	@Override
	public ResourceLocation getTextureLocation(OathboundBannerMarshalRenderState state) {
		return SKIN;
	}

	@Override
	public void extractRenderState(OathboundBannerMarshal entity, OathboundBannerMarshalRenderState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.move = entity.move();
		state.tell = entity.tell();
		state.elapsed = entity.elapsed(partial);
		state.guardCountered = entity.guardCountered();
		state.guardCounterElapsed = entity.guardCounterElapsed(partial);
		state.empowered = entity.empowered();
		state.hurt = entity.hurtTime > 0 ? Math.max(0.0F, (entity.hurtTime - partial) / 10.0F) : 0.0F;

		float now = entity.tickCount + partial;
		if (entity.shownMove != state.move || state.elapsed < entity.shownElapsed - 1.0F) {
			if (entity.shownMove >= 0) {
				entity.prevMove = entity.shownMove;
				entity.prevTell = entity.shownTell;
				entity.prevElapsed = entity.shownElapsed;
				entity.switchedAt = now;
			}
			entity.shownMove = state.move;
		}
		entity.shownTell = state.tell;
		entity.shownElapsed = state.elapsed;
		state.prevMove = entity.prevMove;
		state.prevTell = entity.prevTell;
		state.prevElapsed = entity.prevElapsed;
		state.blend = state.prevMove < 0 ? 1.0F : Mth.clamp((now - entity.switchedAt) / BLEND_TICKS, 0.0F, 1.0F);
	}

	@Override
	protected float getFlipDegrees() {
		return 0.0F;
	}

	@Override
	protected void scale(OathboundBannerMarshalRenderState state, PoseStack poseStack) {
		poseStack.scale(OathboundBannerMarshalRules.SCALE, OathboundBannerMarshalRules.SCALE, OathboundBannerMarshalRules.SCALE);
	}

	private static final class EmissiveLayer extends RenderLayer<OathboundBannerMarshalRenderState, OathboundBannerMarshalModel> {
		private final RenderType type = RenderType.eyes(GLOW);

		EmissiveLayer(RenderLayerParent<OathboundBannerMarshalRenderState, OathboundBannerMarshalModel> parent) {
			super(parent);
		}

		@Override
		public void render(PoseStack poseStack, MultiBufferSource collector, int light, OathboundBannerMarshalRenderState s, float yRot, float xRot) {
			if (s.isInvisible) {
				return;
			}
			float base = s.empowered ? 0.94F : 0.80F;
			if (s.move == OathboundBannerMarshalRules.DECREE_OF_THE_STANDARD
					|| s.move == OathboundBannerMarshalRules.SOLAR_AEGIS_GUARD
					|| s.move == OathboundBannerMarshalRules.SOVEREIGN_JUDGMENT) {
				base = 1.0F;
			} else if (s.move == OathboundBannerMarshalRules.STAGGERED) {
				base = 0.42F + 0.15F * Mth.sin(s.ageInTicks * 0.4F);
			}
			if (s.deathTime > 0) {
				base *= Math.max(0.0F, 1.0F - s.deathTime / 18.0F);
			}
			int alpha = Mth.clamp((int) (base * 255.0F), 0, 255);
			if (alpha <= 4) {
				return;
			}
			RenderCompat.submitModel(collector, getParentModel(), s, poseStack, type, light,
				OverlayTexture.NO_OVERLAY, (alpha << 24) | 0xFFFFFF, s.outlineColor);
		}
	}
}
