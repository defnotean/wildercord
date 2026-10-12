package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.DripfangLurker;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The Dripfang Lurker (see {@link DripfangLurkerModel}). Hanging from the roof it casts no shadow on the floor below. */
public class DripfangLurkerRenderer extends WildMonsterRenderer<DripfangLurker, DripfangLurkerModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("dripfang_lurker"), "main");

	public DripfangLurkerRenderer(EntityRendererProvider.Context context) {
		super(context, new DripfangLurkerModel(context.bakeLayer(LAYER)), 0.5F, Wildercord.id("textures/entity/dripfang_lurker.png"));
	}

	@Override
	protected float getShadowRadius(MonsterRenderState state) {
		return super.getShadowRadius(state) * (1 - Math.max(state.guard, state.alt));
	}
}
