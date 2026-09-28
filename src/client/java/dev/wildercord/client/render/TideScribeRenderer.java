package dev.wildercord.client.render;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.TideScribe;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The Tide Scribe (see {@link TideScribeModel}): its drowned body and robe on its own skin, and the
 * glow of the deep drawn over them at full brightness: its eyes and the lights along its tendrils,
 * slowly pulsing, and the script on its scroll, dim at rest and blazing while it writes a spell.
 */
public class TideScribeRenderer extends DungeonBossRenderer<TideScribe, TideScribeModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("tide_scribe"), "main");

	public TideScribeRenderer(EntityRendererProvider.Context context) {
		super(context, new TideScribeModel(context.bakeLayer(LAYER)), 0.6F, Wildercord.id("textures/entity/tide_scribe.png"));
		glow(Wildercord.id("textures/entity/tide_scribe_glow.png"), state -> {
			float pulse = 0.15F * Mth.sin(state.ageInTicks * 0.09F);
			return white((0.7F + pulse) * alive(state));
		});
		glow(Wildercord.id("textures/entity/tide_scribe_script.png"), state -> {
			float glow = Math.max(state.casting, state.shifting);
			return white((0.3F + 0.7F * glow + 0.05F * Mth.sin(state.ageInTicks * 0.2F)) * alive(state));
		});
	}
}
