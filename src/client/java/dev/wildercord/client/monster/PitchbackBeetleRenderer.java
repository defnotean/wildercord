package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.PitchbackBeetle;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The Pitchback Beetle (see {@link PitchbackBeetleModel}). Its sac of resin glows faintly amber in the dark, brighter as it
 * swells to throw, and goes dark once burst; its little eyes glint.
 */
public class PitchbackBeetleRenderer extends WildMonsterRenderer<PitchbackBeetle, PitchbackBeetleModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("pitchback_beetle"), "main");

	public PitchbackBeetleRenderer(EntityRendererProvider.Context context) {
		super(context, new PitchbackBeetleModel(context.bakeLayer(LAYER)), 0.7F, Wildercord.id("textures/entity/pitchback_beetle.png"));
		glow(Wildercord.id("textures/entity/pitchback_beetle_glow.png"), state -> white(
			(0.35F + 0.55F * state.windup + 0.08F * Mth.sin(state.ageInTicks * 0.08F)) * (1 - 0.85F * state.alt) * alive(state)));
	}
}
