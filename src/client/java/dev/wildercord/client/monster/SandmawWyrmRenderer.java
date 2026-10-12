package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.SandmawWyrm;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The Sandmaw Wyrm (see {@link SandmawWyrmModel}). Under the sand it casts no shadow. */
public class SandmawWyrmRenderer extends WildMonsterRenderer<SandmawWyrm, SandmawWyrmModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("sandmaw_wyrm"), "main");

	public SandmawWyrmRenderer(EntityRendererProvider.Context context) {
		super(context, new SandmawWyrmModel(context.bakeLayer(LAYER)), 1.0F, Wildercord.id("textures/entity/sandmaw_wyrm.png"));
	}

	@Override
	protected float getShadowRadius(MonsterRenderState state) {
		return super.getShadowRadius(state) * (1 - state.veil);
	}
}
