package dev.wildercord.client.ocean;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.ocean.PearlPup;
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

public final class PearlPupRenderer extends MobRenderer<PearlPup, OceanRenderStates.PearlPupState, PearlPupModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("pearl_pup"), "main");
	private static final Identifier SKIN = Wildercord.id("textures/entity/ocean/pearl_pup.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/ocean/pearl_pup_glow.png");

	public PearlPupRenderer(EntityRendererProvider.Context ctx) {
		super(ctx, new PearlPupModel(ctx.bakeLayer(LAYER)), 0.42F);
		addLayer(new LureGlowLayer(this));
	}

	@Override
	public OceanRenderStates.PearlPupState createRenderState() {
		return new OceanRenderStates.PearlPupState();
	}

	@Override
	public Identifier getTextureLocation(OceanRenderStates.PearlPupState state) {
		return SKIN;
	}

	@Override
	public void extractRenderState(PearlPup entity, OceanRenderStates.PearlPupState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.sniffing = entity.isSniffing();
	}

	@Override
	protected void scale(OceanRenderStates.PearlPupState state, PoseStack pose) {
		pose.scale(0.5F, 0.5F, 0.5F);
	}

	private static final class LureGlowLayer extends RenderLayer<OceanRenderStates.PearlPupState, PearlPupModel> {
		private final RenderType glowType = RenderTypes.eyes(GLOW);

		LureGlowLayer(RenderLayerParent<OceanRenderStates.PearlPupState, PearlPupModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, OceanRenderStates.PearlPupState state, float yaw, float pitch) {
			if (state.isInvisible) return;
			RenderCompat.submitModel(collector.order(1), getParentModel(), state, pose, glowType, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF, state.outlineColor);
		}
	}
}
