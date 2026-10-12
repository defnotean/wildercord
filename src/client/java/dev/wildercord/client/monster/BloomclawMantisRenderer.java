package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.BloomclawMantis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The Bloomclaw Mantis (see {@link BloomclawMantisModel}). */
public class BloomclawMantisRenderer extends WildMonsterRenderer<BloomclawMantis, BloomclawMantisModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("bloomclaw_mantis"), "main");

	public BloomclawMantisRenderer(EntityRendererProvider.Context context) {
		super(context, new BloomclawMantisModel(context.bakeLayer(LAYER)), 0.45F, Wildercord.id("textures/entity/bloomclaw_mantis.png"));
	}
}
