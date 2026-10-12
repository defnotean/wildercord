package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.CacklingHyena;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The Cackling Hyena (see {@link CacklingHyenaModel}). Its eyes shine amber in the dark, the way an animal's do in a
 * lantern's light, and brighter as it crouches to dart.
 */
public class CacklingHyenaRenderer extends WildMonsterRenderer<CacklingHyena, CacklingHyenaModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("cackling_hyena"), "main");

	public CacklingHyenaRenderer(EntityRendererProvider.Context context) {
		super(context, new CacklingHyenaModel(context.bakeLayer(LAYER)), 0.55F, Wildercord.id("textures/entity/cackling_hyena.png"));
		glow(Wildercord.id("textures/entity/cackling_hyena_eyes.png"), state -> white(
			(0.5F + 0.45F * Math.max(state.windup, state.acting) + 0.05F * Mth.sin(state.ageInTicks * 0.2F)) * (1 - 0.6F * state.guard) * alive(state)));
	}
}
