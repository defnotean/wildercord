package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.GulperPitcher;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The Gulper Pitcher (see {@link GulperPitcherModel}). The nectar beaded round its rim and pooled in its mouth glows
 * faintly, a lure in the dark, brighter once it's open.
 */
public class GulperPitcherRenderer extends WildMonsterRenderer<GulperPitcher, GulperPitcherModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("gulper_pitcher"), "main");

	public GulperPitcherRenderer(EntityRendererProvider.Context context) {
		super(context, new GulperPitcherModel(context.bakeLayer(LAYER)), 0.6F, Wildercord.id("textures/entity/gulper_pitcher.png"));
		glow(Wildercord.id("textures/entity/gulper_pitcher_nectar.png"), state -> white(
			(0.35F + 0.45F * state.alt + 0.08F * Mth.sin(state.ageInTicks * 0.1F)) * alive(state)));
	}
}
