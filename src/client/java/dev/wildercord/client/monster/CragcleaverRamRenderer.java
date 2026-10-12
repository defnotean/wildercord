package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.CragcleaverRam;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The Cragcleaver Ram (see {@link CragcleaverRamModel}). */
public class CragcleaverRamRenderer extends WildMonsterRenderer<CragcleaverRam, CragcleaverRamModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("cragcleaver_ram"), "main");

	public CragcleaverRamRenderer(EntityRendererProvider.Context context) {
		super(context, new CragcleaverRamModel(context.bakeLayer(LAYER)), 0.8F, Wildercord.id("textures/entity/cragcleaver_ram.png"));
	}
}
