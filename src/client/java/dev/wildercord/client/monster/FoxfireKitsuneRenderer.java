package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.FoxfireKitsune;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The Foxfire Kitsune (see {@link FoxfireKitsuneModel}). The fire in its tail tips and its eyes glows blue whatever the
 * light, flaring as it fans its tails to throw. Its copies are drawn just the same, but cast no shadow: a careful eye on the
 * ground beneath them tells the real one.
 */
public class FoxfireKitsuneRenderer extends WildMonsterRenderer<FoxfireKitsune, FoxfireKitsuneModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("foxfire_kitsune"), "main");

	public FoxfireKitsuneRenderer(EntityRendererProvider.Context context) {
		super(context, new FoxfireKitsuneModel(context.bakeLayer(LAYER)), 0.45F, Wildercord.id("textures/entity/foxfire_kitsune.png"));
		glow(Wildercord.id("textures/entity/foxfire_kitsune_glow.png"), state -> white(
			(0.65F + 0.35F * Math.max(state.windup, state.alt) + 0.08F * Mth.sin(state.ageInTicks * 0.45F)) * alive(state)));
	}

	@Override
	public MonsterRenderState createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(FoxfireKitsune kitsune, MonsterRenderState state, float partial) {
		super.extractRenderState(kitsune, state, partial);
		((State) state).decoy = kitsune.decoy();
	}

	@Override
	protected float getShadowRadius(MonsterRenderState state) {
		return state instanceof State kitsune && kitsune.decoy ? 0 : super.getShadowRadius(state);
	}

	public static final class State extends MonsterRenderState {
		/** Only a copy: no shadow. */
		public boolean decoy;
	}
}
