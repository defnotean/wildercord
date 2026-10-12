package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.Strawman;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The Strawman (see {@link StrawmanModel}). Whether it's held still is taken as it is, not eased, so that when someone
 * looks back at it it's already standing however it stands. Its stitched eyes burn faintly, like embers in a sack, and
 * brighter while it hunts.
 */
public class StrawmanRenderer extends WildMonsterRenderer<Strawman, StrawmanModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("strawman"), "main");

	public StrawmanRenderer(EntityRendererProvider.Context context) {
		super(context, new StrawmanModel(context.bakeLayer(LAYER)), 0.45F, Wildercord.id("textures/entity/strawman.png"));
		glow(Wildercord.id("textures/entity/strawman_eyes.png"), state -> white(
			(0.15F + 0.65F * state.windup + 0.05F * Mth.sin(state.ageInTicks * 0.3F)) * alive(state)));
	}

	@Override
	public void extractRenderState(Strawman strawman, MonsterRenderState state, float partial) {
		super.extractRenderState(strawman, state, partial);
		state.veil = strawman.frozen() ? 1 : 0;
		state.stance = strawman.stance();
	}
}
