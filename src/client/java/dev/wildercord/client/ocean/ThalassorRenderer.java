package dev.wildercord.client.ocean;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.ocean.ThalassorBoss;
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

public final class ThalassorRenderer extends MobRenderer<ThalassorBoss, OceanRenderStates.ThalassorState, ThalassorModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("thalassor"), "main");
	private static final ResourceLocation SKIN = Wildercord.id("textures/entity/ocean/thalassor.png");
	private static final ResourceLocation GLOW = Wildercord.id("textures/entity/ocean/thalassor_glow.png");

	public ThalassorRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new ThalassorModel(ctx.bakeLayer(LAYER)), 1.6F);
		addLayer(new RuneGlowLayer(this));
	}

	@Override
	public OceanRenderStates.ThalassorState createRenderState() {
		return new OceanRenderStates.ThalassorState();
	}

	@Override
	public ResourceLocation getTextureLocation(OceanRenderStates.ThalassorState state) {
		return SKIN;
	}

	@Override
	public void extractRenderState(ThalassorBoss entity, OceanRenderStates.ThalassorState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.jawOpen = entity.getJawOpen();
		state.attackMode = entity.getAttackMode();
	}

	@Override
	protected void scale(OceanRenderStates.ThalassorState state, PoseStack pose) {
		pose.scale(0.72F, 0.72F, 0.72F);
	}

	@Override
	protected float getFlipDegrees() {
		return 0.0F;
	}

	private static final class RuneGlowLayer extends RenderLayer<OceanRenderStates.ThalassorState, ThalassorModel> {
		private final RenderType glowType = RenderType.eyes(GLOW);

		RuneGlowLayer(RenderLayerParent<OceanRenderStates.ThalassorState, ThalassorModel> parent) {
			super(parent);
		}

		@Override
		public void render(PoseStack pose, MultiBufferSource collector, int light, OceanRenderStates.ThalassorState state, float yaw, float pitch) {
			if (state.isInvisible) return;
			RenderCompat.submitModel(collector, getParentModel(), state, pose, glowType, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, RenderCompat.outlineOf(state));
		}
	}
}
