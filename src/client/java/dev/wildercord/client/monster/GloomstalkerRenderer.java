package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.Gloomstalker;
import dev.wildercord.monster.MonsterRules;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * The Gloomstalker (see {@link GloomstalkerModel}). Hidden, it's drawn almost clear, a ripple in the dark, through the
 * same translucent entity pass vanilla uses for a creature that's invisible to everyone but its team: shader packs know it,
 * and nothing about it is blended any other way. Its eyes are drawn apart, through the glowing-eyes pass, so they always
 * hang in the gloom (dim while it stalks, flaring as it crouches to pounce: the tell).
 */
public class GloomstalkerRenderer extends WildMonsterRenderer<Gloomstalker, GloomstalkerModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("gloomstalker"), "main");
	private static final Identifier TEXTURE = Wildercord.id("textures/entity/gloomstalker.png");

	public GloomstalkerRenderer(EntityRendererProvider.Context context) {
		super(context, new GloomstalkerModel(context.bakeLayer(LAYER)), 0.5F, TEXTURE);
		glow(Wildercord.id("textures/entity/gloomstalker_eyes.png"), state -> white((0.55F + 0.45F * Math.max(state.windup, state.acting)) * alive(state)));
		// The violet seams in its fur, faint, and only where it can be seen at all.
		glow(Wildercord.id("textures/entity/gloomstalker_glow.png"), state -> white(0.35F * (1 - state.veil) * alive(state)));
	}

	@Override
	protected RenderType getRenderType(MonsterRenderState state, boolean bodyVisible, boolean translucent, boolean glowing) {
		if (!bodyVisible && !translucent) {
			return super.getRenderType(state, bodyVisible, translucent, glowing);
		}
		return getModel().renderType(TEXTURE);
	}

	@Override
	protected int getModelTint(MonsterRenderState state) {
		return ARGB.color(Math.round(MonsterRules.opacity(state.veil) * 255), 255, 255, 255);
	}

	@Override
	protected float getShadowRadius(MonsterRenderState state) {
		return super.getShadowRadius(state) * MonsterRules.opacity(state.veil);
	}
}
