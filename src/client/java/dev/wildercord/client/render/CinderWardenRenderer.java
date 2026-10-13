package dev.wildercord.client.render;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.CinderWarden;
import dev.wildercord.cast.CinderWardenRules;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The Cinder Warden (see {@link CinderWardenModel}): its iron plates on its own skin, and the magma
 * between them drawn over at full brightness: a slow ember-glow at rest that flickers, flaring white-hot
 * while it casts, winds up a blow or stands cracked open by a reaction, and going out as it dies.
 *
 * <p>Its moves show in its heat too: the glow climbs as its fists rise for the hammerfall, burns
 * full while it breathes fire, beats like a heart that quickens as it gathers its heat to flare and
 * burns white as it bursts; and once it has flared it sinks almost dark, spent, until it is struck
 * open again.</p>
 */
public class CinderWardenRenderer extends DungeonBossRenderer<CinderWarden, CinderWardenModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("cinder_warden"), "main");

	public CinderWardenRenderer(EntityRendererProvider.Context context) {
		super(context, new CinderWardenModel(context.bakeLayer(LAYER)), 1.1F, Wildercord.id("textures/entity/cinder_warden.png"));
		glow(Wildercord.id("textures/entity/cinder_warden_glow.png"), state -> {
			float flare = Math.max(Math.max(state.casting, state.slamming), Math.max(state.exposed, state.shifting));
			float flicker = 0.08F * Mth.sin(state.ageInTicks * 0.37F) + 0.05F * Mth.sin(state.ageInTicks * 1.13F);
			float level = Math.max(0.5F + flicker + 0.5F * flare, heat(state));
			float cold = state.moves[CinderWardenRules.SPENT] * (1 - state.exposed);
			return white(Mth.lerp(cold, level, 0.12F + 0.5F * flicker) * alive(state));
		});
		glow(Wildercord.id("textures/entity/cinder_warden_eyes.png"), state -> white(alive(state)));
	}

	/** How hot its current move burns it, 0 to 1. */
	private static float heat(DungeonBossRenderState state) {
		float s = state.since;
		int tell = Math.max(1, state.tell);
		return switch (state.move) {
			case CinderWardenRules.HAMMERFALL -> s < tell ? 0.5F + 0.5F * s / tell : Math.max(0, 1 - (s - tell) / 10);
			case CinderWardenRules.BELLOWS -> s >= tell && s < tell + CinderWardenRules.BREATH_TICKS ? 0.95F : 0;
			case CinderWardenRules.FLARE -> {
				float gather = CinderWardenRules.FLARE_GATHER;
				if (s >= tell + gather) {
					yield 1;
				}
				// A heartbeat, quickening and brightening as the heat gathers in it.
				float g = Mth.clamp(s / (tell + gather), 0, 1);
				float beat = 0.5F + 0.5F * Mth.sin(s * (0.3F + 0.9F * g));
				yield 0.4F + 0.3F * g + 0.3F * g * beat;
			}
			default -> 0;
		};
	}
}
