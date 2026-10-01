package dev.wildercord.client.auraworld;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.world.AuraFighter;
import dev.wildercord.client.fx.AuraBlade;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Draws a duelist or a fallen knight: its model on its skin, a glow layer of only what glows (a duelist's sash emblem, a
 * knight's visor and the cracks in its plate) in its aura's colour through vanilla's glowing-eyes pass, and the aura along its
 * drawn blade, the same {@link AuraBlade} a player's carries (its colour, stage and guard ride into the render state, and the
 * item layer draws round the weapon). Everything glowing is {@code RenderTypes.eyes}, so shader packs take it as they take
 * glowing eyes.
 */
public class AuraFighterRenderer<T extends AuraFighter, M extends HumanoidModel<AuraFighterRenderState>> extends HumanoidMobRenderer<T, AuraFighterRenderState, M> {
	private final Function<AuraFighterRenderState, Identifier> texture;
	/** How strongly its blade's aura shows (a fallen knight's is dim and smoky). */
	private final float bladeStrength;

	public AuraFighterRenderer(EntityRendererProvider.Context context, M model, Function<AuraFighterRenderState, Identifier> texture, Identifier glow,
			ToIntFunction<AuraFighterRenderState> glowAlpha, float bladeStrength) {
		super(context, model, 0.5F);
		this.texture = texture;
		this.bladeStrength = bladeStrength;
		addLayer(new Glow<>(this, glow, glowAlpha));
	}

	@Override
	public Identifier getTextureLocation(AuraFighterRenderState state) {
		return texture.apply(state);
	}

	@Override
	public AuraFighterRenderState createRenderState() {
		return new AuraFighterRenderState();
	}

	@Override
	public void extractRenderState(T fighter, AuraFighterRenderState state, float partial) {
		super.extractRenderState(fighter, state, partial);
		state.color = fighter.auraColor();
		state.stage = fighter.stage();
		state.method = fighter.method().id();
		state.windup = fighter.pose(AuraFighter.WINDUP, partial);
		state.guard = fighter.pose(AuraFighter.GUARD, partial);
		state.stagger = fighter.pose(AuraFighter.STAGGER, partial);
		state.bow = fighter.pose(AuraFighter.BOW, partial);
		state.yield = fighter.pose(AuraFighter.YIELD, partial);
		state.drawn = fighter.pose(AuraFighter.DRAWN, partial);
		state.sit = fighter.pose(AuraFighter.SIT, partial);
		state.dash = fighter.pose(AuraFighter.DASH, partial);
		// Its drawn blade carries its aura as a player's does; brighter in its tell, braced in guard.
		boolean blade = !fighter.isInvisible() && fighter.getMainHandItem().is(Aura.WEAPONS);
		float strength = bladeStrength * (0.8F + 0.45F * state.windup);
		state.setData(AuraBlade.GLOW, blade ? new AuraBlade.Glow(state.color, state.stage, Math.min(1.2F, strength), state.guard > 0.5F,
			fighter.getMainArm()) : null);
	}

	@Override
	protected void scale(AuraFighterRenderState state, PoseStack poseStack) {
		// Sitting by its fire, or down on one knee: lower to the ground.
		float down = 0.62F * state.sit + 0.36F * state.yield;
		if (down > 0.001F) {
			poseStack.translate(0.0F, down, 0.0F);
		}
	}

	/** An alpha (0 to 1) as the ARGB a glow layer is tinted with, in its aura's colour lifted toward white. */
	static int tint(float alpha, int color) {
		int a = Mth.clamp((int) (alpha * 255), 0, 255);
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		r = r + (255 - r) / 3;
		g = g + (255 - g) / 3;
		b = b + (255 - b) / 3;
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/** How much a glow has faded as it dies. */
	static float alive(AuraFighterRenderState state) {
		return state.deathTime > 0 ? Math.max(0, 1 - state.deathTime / 20.0F) : 1;
	}

	/** An emissive copy of the model with a texture of only what glows, its strength and colour per frame. */
	private static final class Glow<M extends HumanoidModel<AuraFighterRenderState>> extends RenderLayer<AuraFighterRenderState, M> {
		private final Identifier texture;
		private final ToIntFunction<AuraFighterRenderState> color;

		Glow(RenderLayerParent<AuraFighterRenderState, M> parent, Identifier texture, ToIntFunction<AuraFighterRenderState> color) {
			super(parent);
			this.texture = texture;
			this.color = color;
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AuraFighterRenderState state, float yRot, float xRot) {
			if (state.isInvisible) {
				return;
			}
			int argb = color.applyAsInt(state);
			if ((argb >>> 24) < 4) {
				return;
			}
			collector.order(1).submitModel(getParentModel(), state, poseStack, RenderTypes.eyes(texture), light, OverlayTexture.NO_OVERLAY, argb, null,
				state.outlineColor);
		}
	}
}
