package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.KarakuriSentinel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Renders {@link KarakuriSentinel} with a clockwork lacquered-wood body. */
public class KarakuriSentinelRenderer extends WildMonsterRenderer<KarakuriSentinel, KarakuriSentinelModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("karakuri_sentinel"), "main");

	public KarakuriSentinelRenderer(EntityRendererProvider.Context context) {
		super(context, new KarakuriSentinelModel(context.bakeLayer(LAYER)), 0.5F, Wildercord.id("textures/entity/karakuri_sentinel.png"));
	}
}
