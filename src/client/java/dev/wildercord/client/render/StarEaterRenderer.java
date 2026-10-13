package dev.wildercord.client.render;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.StarEater;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The Star-Eater (see {@link StarEaterModel}): a knot of void on its own skin, and two glowing layers:
 * its eye and the veins across it (brighter as it casts or charges a move, gone wide and white while
 * it's open, guttering out as it swallows the light), and
 * the edges of its shards (burning bright with its shield up, guttering while it's broken).
 */
public class StarEaterRenderer extends DungeonBossRenderer<StarEater, StarEaterModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("star_eater"), "main");

	public StarEaterRenderer(EntityRendererProvider.Context context) {
		super(context, new StarEaterModel(context.bakeLayer(LAYER)), 0.0F, Wildercord.id("textures/entity/star_eater.png"));
		glow(Wildercord.id("textures/entity/star_eater_eye.png"), state -> {
			// Its eye burns up as it charges a move, and goes out as it swallows the light.
			float pulse = 0.1F * Mth.sin(state.ageInTicks * 0.12F);
			float charge = Math.max(Math.max(state.casting, state.exposed), state.extra[1]);
			return white((0.6F + pulse + 0.4F * charge) * (1 - 0.8F * state.extra[4]) * alive(state));
		});
		glow(Wildercord.id("textures/entity/star_eater_shards.png"), state -> {
			float flicker = state.exposed > 0 ? 0.25F * Mth.sin(state.ageInTicks * 0.9F) * state.exposed : 0;
			return white((0.25F + 0.75F * state.guarded + flicker) * alive(state));
		});
	}
}
