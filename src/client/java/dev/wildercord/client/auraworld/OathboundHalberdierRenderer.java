package dev.wildercord.client.auraworld;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.OathboundHalberdier;
import dev.wildercord.aura.world.OathboundHalberdierRules;
import dev.wildercord.client.compat.RenderCompat;
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
import net.minecraft.util.Mth;

/** Renders {@link OathboundHalberdier} with PoseBlend transitions and emissive cyan/violet visor, circuit & halberd glow. */
public final class OathboundHalberdierRenderer
		extends MobRenderer<OathboundHalberdier, OathboundHalberdierRenderState, OathboundHalberdierModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("oathbound_halberdier"), "main");
	private static final Identifier SKIN = Wildercord.id("textures/entity/oathbound_halberdier/skin.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/oathbound_halberdier/glow.png");
	private static final float BLEND_TICKS = 4.0F;

	public OathboundHalberdierRenderer(EntityRendererProvider.Context context) {
		super(context, new OathboundHalberdierModel(context.bakeLayer(LAYER)), 0.70F);
		addLayer(new EmissiveLayer(this));
	}

	@Override
	public OathboundHalberdierRenderState createRenderState() {
		return new OathboundHalberdierRenderState();
	}

	@Override
	public Identifier getTextureLocation(OathboundHalberdierRenderState state) {
		return SKIN;
	}

	@Override
	public void extractRenderState(OathboundHalberdier entity, OathboundHalberdierRenderState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.move = entity.move();
		state.tell = entity.tell();
		state.elapsed = entity.elapsed(partial);
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
	protected void scale(OathboundHalberdierRenderState state, PoseStack poseStack) {
		poseStack.scale(OathboundHalberdierRules.SCALE, OathboundHalberdierRules.SCALE, OathboundHalberdierRules.SCALE);
	}

	private static final class EmissiveLayer extends RenderLayer<OathboundHalberdierRenderState, OathboundHalberdierModel> {
		private final RenderType type = RenderTypes.eyes(GLOW);

		EmissiveLayer(RenderLayerParent<OathboundHalberdierRenderState, OathboundHalberdierModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, OathboundHalberdierRenderState s, float yRot, float xRot) {
			if (s.isInvisible) {
				return;
			}
			float base = s.empowered ? 0.94F : 0.80F;
			if (s.move == OathboundHalberdierRules.TEMPEST_WHIRL
					|| s.move == OathboundHalberdierRules.SHADOWSTEP_THRUST
					|| s.move == OathboundHalberdierRules.SKY_VAULT_CRASH) {
				base = 1.0F;
			} else if (s.move == OathboundHalberdierRules.STAGGERED) {
				base = 0.42F + 0.15F * Mth.sin(s.ageInTicks * 0.4F);
			}
			if (s.deathTime > 0) {
				base *= Math.max(0.0F, 1.0F - s.deathTime / 18.0F);
			}
			int alpha = Mth.clamp((int) (base * 255.0F), 0, 255);
			if (alpha <= 4) {
				return;
			}
			RenderCompat.submitModel(collector.order(1), getParentModel(), s, poseStack, type, light,
				OverlayTexture.NO_OVERLAY, (alpha << 24) | 0xFFFFFF, s.outlineColor);
		}
	}
}
