package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.CofferMimic;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The Coffer Mimic (see {@link CofferMimicModel}). How shut it is comes from the mimic's own easing rather than the shared
 * veil, so one already shut when it comes into view is drawn shut from the first frame. Its eyes glow yellow in the dark
 * of its mouth, and never while it's shut.
 */
public class CofferMimicRenderer extends WildMonsterRenderer<CofferMimic, CofferMimicModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("coffer_mimic"), "main");

	public CofferMimicRenderer(EntityRendererProvider.Context context) {
		super(context, new CofferMimicModel(context.bakeLayer(LAYER)), 0.55F, Wildercord.id("textures/entity/coffer_mimic.png"));
		glow(Wildercord.id("textures/entity/coffer_mimic_eyes.png"), state -> white(
			(0.8F + 0.2F * Math.max(state.windup, state.acting) + 0.05F * Mth.sin(state.ageInTicks * 0.2F)) * (1 - state.veil) * (1 - 0.8F * state.guard)
				* alive(state)));
	}

	@Override
	public void extractRenderState(CofferMimic mimic, MonsterRenderState state, float partial) {
		super.extractRenderState(mimic, state, partial);
		state.veil = mimic.shut(partial);
	}
}
