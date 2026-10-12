package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.GlowlureAngler;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The Glowlure Angler (see {@link GlowlureAnglerModel}). Its lure glows whatever the light: a slow steady pulse as it waits,
 * a flicker as its jaw drops, dimmed almost out while it lies winded.
 */
public class GlowlureAnglerRenderer extends WildMonsterRenderer<GlowlureAngler, GlowlureAnglerModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("glowlure_angler"), "main");

	public GlowlureAnglerRenderer(EntityRendererProvider.Context context) {
		super(context, new GlowlureAnglerModel(context.bakeLayer(LAYER)), 0.55F, Wildercord.id("textures/entity/glowlure_angler.png"));
		glow(Wildercord.id("textures/entity/glowlure_angler_glow.png"), state -> white(
			(0.75F + 0.2F * Mth.sin(state.ageInTicks * 0.08F) * state.guard + 0.25F * Mth.sin(state.ageInTicks * 1.7F) * state.windup
				- 0.6F * state.stunned) * alive(state)));
	}
}
