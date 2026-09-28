package dev.wildercord.client.render;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.CinderWarden;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The Cinder Warden (see {@link CinderWardenModel}): its iron plates on its own skin, and the magma
 * between them drawn over at full brightness: a slow ember-glow at rest that flickers, flaring white-hot
 * while it casts, winds up a blow or stands cracked open by a reaction, and going out as it dies.
 */
public class CinderWardenRenderer extends DungeonBossRenderer<CinderWarden, CinderWardenModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("cinder_warden"), "main");

	public CinderWardenRenderer(EntityRendererProvider.Context context) {
		super(context, new CinderWardenModel(context.bakeLayer(LAYER)), 1.1F, Wildercord.id("textures/entity/cinder_warden.png"));
		glow(Wildercord.id("textures/entity/cinder_warden_glow.png"), state -> {
			float flare = Math.max(Math.max(state.casting, state.slamming), Math.max(state.exposed, state.shifting));
			float flicker = 0.08F * Mth.sin(state.ageInTicks * 0.37F) + 0.05F * Mth.sin(state.ageInTicks * 1.13F);
			return white((0.5F + flicker + 0.5F * flare) * alive(state));
		});
		glow(Wildercord.id("textures/entity/cinder_warden_eyes.png"), state -> white(alive(state)));
	}
}
