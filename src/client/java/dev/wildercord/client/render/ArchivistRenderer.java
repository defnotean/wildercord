package dev.wildercord.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.cast.Archivist;
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

import java.util.function.ToIntFunction;

/**
 * The Archivist (see {@link ArchivistModel}): its robe and tome on its own skin, and two glowing
 * layers drawn over them at full brightness, whatever the light: its eyes and the gold sigil on its
 * chest, always lit, and the writing on its pages, dim at rest and blazing while it casts.
 */
public class ArchivistRenderer extends MobRenderer<Archivist, ArchivistRenderState, ArchivistModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("archivist"), "main");
	private static final Identifier TEXTURE = Wildercord.id("textures/entity/archivist.png");
	private static final Identifier EYES = Wildercord.id("textures/entity/archivist_eyes.png");
	private static final Identifier RUNES = Wildercord.id("textures/entity/archivist_runes.png");

	public ArchivistRenderer(EntityRendererProvider.Context context) {
		super(context, new ArchivistModel(context.bakeLayer(LAYER)), 0.6F);
		addLayer(new Glow(this, EYES, state -> {
			// The eyes gutter out as it dies.
			float alpha = state.deathTime > 0 ? Math.max(0, 1 - state.deathTime / 14F) : 1;
			return ((int) (alpha * 255) << 24) | 0xFFFFFF;
		}));
		addLayer(new Glow(this, RUNES, state -> {
			float glow = Math.max(state.casting, state.rewriting);
			float alpha = 0.35F + 0.1F * Mth.sin(state.ageInTicks * 0.1F) + 0.6F * glow;
			if (state.deathTime > 0) {
				alpha *= Math.max(0, 1 - state.deathTime / 20F);
			}
			return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | 0xFFFFFF;
		}));
	}

	@Override
	public Identifier getTextureLocation(ArchivistRenderState state) {
		return TEXTURE;
	}

	@Override
	public ArchivistRenderState createRenderState() {
		return new ArchivistRenderState();
	}

	@Override
	public void extractRenderState(Archivist archivist, ArchivistRenderState state, float partial) {
		super.extractRenderState(archivist, state, partial);
		state.casting = archivist.castPose(partial);
		state.rewriting = archivist.rewritePose(partial);
	}

	/** It doesn't topple when it dies: it sinks into its robe and its pages scatter (see the model). */
	@Override
	protected float getFlipDegrees() {
		return 0;
	}

	/** An emissive copy of the model with a texture holding only what glows, its strength per frame. */
	private static final class Glow extends RenderLayer<ArchivistRenderState, ArchivistModel> {
		private final RenderType renderType;
		private final ToIntFunction<ArchivistRenderState> color;

		Glow(RenderLayerParent<ArchivistRenderState, ArchivistModel> parent, Identifier texture, ToIntFunction<ArchivistRenderState> color) {
			super(parent);
			this.renderType = RenderTypes.eyes(texture);
			this.color = color;
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, ArchivistRenderState state, float yRot, float xRot) {
			if (state.isInvisible) {
				return;
			}
			int argb = color.applyAsInt(state);
			if ((argb >>> 24) < 4) {
				return;
			}
			collector.order(1).submitModel(getParentModel(), state, poseStack, renderType, light, OverlayTexture.NO_OVERLAY, argb, null,
				state.outlineColor);
		}
	}
}
