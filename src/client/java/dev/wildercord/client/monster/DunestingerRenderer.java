package dev.wildercord.client.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.monster.Dunestinger;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The Dunestinger (see {@link DunestingerModel}). Its little eyes catch the light like beads of glass, faintly even buried,
 * and the barb of its sting glows hot as the sting is drawn back.
 */
public class DunestingerRenderer extends WildMonsterRenderer<Dunestinger, DunestingerModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("dunestinger"), "main");

	public DunestingerRenderer(EntityRendererProvider.Context context) {
		super(context, new DunestingerModel(context.bakeLayer(LAYER)), 0.6F, Wildercord.id("textures/entity/dunestinger.png"));
		glow(Wildercord.id("textures/entity/dunestinger_eyes.png"), state -> white(
			(0.3F + 0.6F * state.windup + 0.06F * Mth.sin(state.ageInTicks * 0.25F)) * alive(state)));
	}
}
