package dev.wildercord.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

import java.util.function.Function;

/**
 * What the wildlife renderers share: one render state for all six, babies drawn small (their models give them bigger
 * heads), and a glow layer: the model again with a texture holding only what glows, drawn like vanilla's glowing eyes
 * ({@code RenderTypes.eyes}, which every shader pack handles) at the strength {@link WildlifeRenderState#glow} gives.
 */
public abstract class WildlifeRenderer<T extends Mob, M extends EntityModel<WildlifeRenderState>> extends MobRenderer<T, WildlifeRenderState, M> {
	private final Identifier texture;

	protected WildlifeRenderer(EntityRendererProvider.Context context, M model, float shadow, Identifier texture) {
		super(context, model, shadow);
		this.texture = texture;
	}

	/** Adds the glow layer: a texture holding only what glows, which may depend on the creature (a glimmerwing's colouring). */
	protected void glow(Function<WildlifeRenderState, Identifier> texture) {
		addLayer(new Glow<>(this, texture));
	}

	@Override
	public Identifier getTextureLocation(WildlifeRenderState state) {
		return texture;
	}

	@Override
	public WildlifeRenderState createRenderState() {
		return new WildlifeRenderState();
	}

	@Override
	public void extractRenderState(T entity, WildlifeRenderState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.seed = (entity.getId() * 0.6180339F) % 1.0F * 100F;
	}

	@Override
	protected void scale(WildlifeRenderState state, PoseStack poseStack) {
		if (state.isBaby) {
			poseStack.scale(state.ageScale, state.ageScale, state.ageScale);
		}
	}

	/** The model again, emissive, with only what glows. */
	private static final class Glow<M extends EntityModel<WildlifeRenderState>> extends RenderLayer<WildlifeRenderState, M> {
		private final Function<WildlifeRenderState, Identifier> texture;

		Glow(RenderLayerParent<WildlifeRenderState, M> parent, Function<WildlifeRenderState, Identifier> texture) {
			super(parent);
			this.texture = texture;
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, WildlifeRenderState state, float yRot, float xRot) {
			int alpha = Math.max(0, Math.min(255, (int) (state.glow * 255)));
			if (state.isInvisible || alpha < 4) {
				return;
			}
			collector.order(1).submitModel(getParentModel(), state, poseStack, RenderTypes.eyes(texture.apply(state)), light, OverlayTexture.NO_OVERLAY,
				(alpha << 24) | 0xFFFFFF, null, state.outlineColor);
		}
	}
}
