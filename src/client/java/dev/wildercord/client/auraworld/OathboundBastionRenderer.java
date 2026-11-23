package dev.wildercord.client.auraworld;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.OathboundBastion;
import dev.wildercord.aura.world.OathboundBastionRules;
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

/** Renders the towering {@link OathboundBastion} with 4-tick PoseBlend and emissive visor/mace/hex-barrier glow. */
public final class OathboundBastionRenderer extends MobRenderer<OathboundBastion, OathboundBastionRenderState, OathboundBastionModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("oathbound_bastion"), "main");
	private static final ResourceLocation SKIN = Wildercord.id("textures/entity/oathbound_bastion/skin.png");
	private static final ResourceLocation GLOW = Wildercord.id("textures/entity/oathbound_bastion/glow.png");
	private static final float BLEND_TICKS = 4.0F;

	public OathboundBastionRenderer(EntityRendererProvider.Context context) {
		super(context, new OathboundBastionModel(context.bakeLayer(LAYER)), 0.78F);
		addLayer(new EmissiveLayer(this));
	}

	@Override
	public OathboundBastionRenderState createRenderState() {
		return new OathboundBastionRenderState();
	}

	@Override
	public ResourceLocation getTextureLocation(OathboundBastionRenderState state) {
		return SKIN;
	}

	@Override
	public void extractRenderState(OathboundBastion entity, OathboundBastionRenderState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.move = entity.move();
		state.tell = entity.tell();
		state.elapsed = entity.elapsed(partial);
		state.barrier = entity.barrier();
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
	protected void scale(OathboundBastionRenderState state, PoseStack poseStack) {
		poseStack.scale(OathboundBastionRules.SCALE, OathboundBastionRules.SCALE, OathboundBastionRules.SCALE);
	}

	private static final class EmissiveLayer extends RenderLayer<OathboundBastionRenderState, OathboundBastionModel> {
		private final RenderType type = RenderType.eyes(GLOW);

		EmissiveLayer(RenderLayerParent<OathboundBastionRenderState, OathboundBastionModel> parent) {
			super(parent);
		}

		@Override
		public void render(PoseStack poseStack, MultiBufferSource collector, int light, OathboundBastionRenderState s, float yRot, float xRot) {
			if (s.isInvisible) {
				return;
			}
			float base = s.empowered ? 0.92F : 0.78F;
			if (s.move == OathboundBastionRules.PHALANX_BRACE || s.move == OathboundBastionRules.ANVIL_SLAM) {
				base = 1.0F;
			} else if (s.move == OathboundBastionRules.STAGGERED) {
				base = 0.42F + 0.15F * Mth.sin(s.ageInTicks * 0.4F);
			}
			if (s.deathTime > 0) {
				base *= Math.max(0.0F, 1.0F - s.deathTime / 18.0F);
			}
			int alpha = Mth.clamp((int) (base * 255.0F), 0, 255);
			if (alpha < 4) {
				return;
			}
			RenderCompat.submitModel(collector, getParentModel(), s, poseStack, type, light,
				OverlayTexture.NO_OVERLAY, (alpha << 24) | 0xFFFFFF, s.outlineColor);
		}
	}
}
