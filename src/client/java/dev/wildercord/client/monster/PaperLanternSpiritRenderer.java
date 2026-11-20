package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.PaperLanternSpirit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** Renders {@link PaperLanternSpirit} as a hovering chochin paper-lantern spirit. */
public class PaperLanternSpiritRenderer extends WildMonsterRenderer<PaperLanternSpirit, PaperLanternSpiritModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("paper_lantern_spirit"), "main");

	public PaperLanternSpiritRenderer(EntityRendererProvider.Context context) {
		super(context, new PaperLanternSpiritModel(context.bakeLayer(LAYER)), 0.35F, Wildercord.id("textures/entity/paper_lantern_spirit.png"));
	}
}
