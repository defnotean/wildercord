package dev.wildercord.client.monster;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.monster.BogWitchFrog;
import dev.wildercord.monster.WildMonster;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.function.ToIntFunction;

/**
 * What the monsters of the wilds' renderers share, after the dungeon bosses': the model on its own skin, and layers holding
 * only what glows (eyes, crystals, a throat sac, lightning streaks) drawn over it at full brightness whatever the light,
 * each with its strength worked out per frame. Glows go through vanilla's own glowing-eyes pass ({@code RenderTypes.eyes}),
 * which every shader pack is written for.
 */
public class WildMonsterRenderer<T extends WildMonster, M extends EntityModel<MonsterRenderState>> extends MobRenderer<T, MonsterRenderState, M> {
	private final Identifier texture;

	public WildMonsterRenderer(EntityRendererProvider.Context context, M model, float shadow, Identifier texture) {
		super(context, model, shadow);
		this.texture = texture;
	}

	/** Adds a layer holding only what glows, drawn at full brightness with the ARGB {@code color} gives each frame. */
	protected void glow(Identifier texture, ToIntFunction<MonsterRenderState> color) {
		addLayer(new Glow<>(this, texture, color));
	}

	@Override
	public Identifier getTextureLocation(MonsterRenderState state) {
		return texture;
	}

	@Override
	public MonsterRenderState createRenderState() {
		return new MonsterRenderState();
	}

	@Override
	public void extractRenderState(T monster, MonsterRenderState state, float partial) {
		super.extractRenderState(monster, state, partial);
		state.windup = monster.pose(WildMonster.WINDUP, partial);
		state.acting = monster.pose(WildMonster.ACTING, partial);
		state.guard = monster.pose(WildMonster.GUARD, partial);
		state.stunned = monster.pose(WildMonster.STUNNED, partial);
		state.veil = monster.pose(WildMonster.VEILED, partial);
		state.alt = monster.pose(WildMonster.ALT, partial);
		state.engorged = monster instanceof BogWitchFrog frog && frog.engorged();
		state.variantTint = monster.variant().tint;
	}

	/** A Frost or Ash variant's tint over its skin. */
	@Override
	protected int getModelTint(MonsterRenderState state) {
		return 0xFF000000 | state.variantTint;
	}

	/** An alpha (0 to 1) as the white ARGB a glow layer is tinted with. */
	protected static int white(float alpha) {
		return (Math.max(0, Math.min(255, (int) (alpha * 255))) << 24) | 0xFFFFFF;
	}

	/** How much a glow has faded as it dies: 1 alive, 0 a second into its death. */
	protected static float alive(MonsterRenderState state) {
		return state.deathTime > 0 ? Math.max(0, 1 - state.deathTime / 20.0F) : 1;
	}

	/** An emissive copy of the model with a texture holding only what glows, its strength per frame. */
	private static final class Glow<M extends EntityModel<MonsterRenderState>> extends RenderLayer<MonsterRenderState, M> {
		private final RenderType renderType;
		private final ToIntFunction<MonsterRenderState> color;

		Glow(RenderLayerParent<MonsterRenderState, M> parent, Identifier texture, ToIntFunction<MonsterRenderState> color) {
			super(parent);
			this.renderType = RenderTypes.eyes(texture);
			this.color = color;
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, MonsterRenderState state, float yRot, float xRot) {
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
