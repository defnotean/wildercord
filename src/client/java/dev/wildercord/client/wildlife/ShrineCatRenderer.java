package dev.wildercord.client.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.ShrineCat;
import dev.wildercord.wildlife.ShrineCatRules;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** Renders {@link ShrineCat} with each real cat's photo-matched skin and model features. */
public class ShrineCatRenderer extends MobRenderer<ShrineCat, ShrineCatRenderState, ShrineCatModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("shrine_cat"), "main");

	private static final Identifier[] TEXTURES = {
		Wildercord.id("textures/entity/shrine_cat_yuki.png"),
		Wildercord.id("textures/entity/shrine_cat_odie.png"),
		Wildercord.id("textures/entity/shrine_cat_freya.png"),
		Wildercord.id("textures/entity/shrine_cat_kuro.png")
	};

	public ShrineCatRenderer(EntityRendererProvider.Context context) {
		super(context, new ShrineCatModel(context.bakeLayer(LAYER)), 0.34F);
	}

	@Override
	public Identifier getTextureLocation(ShrineCatRenderState state) {
		ShrineCatRules.Variant v = ShrineCatRules.Variant.byId(state.variant);
		return TEXTURES[v.id];
	}

	@Override
	public ShrineCatRenderState createRenderState() {
		return new ShrineCatRenderState();
	}

	@Override
	public void extractRenderState(ShrineCat cat, ShrineCatRenderState state, float partial) {
		super.extractRenderState(cat, state, partial);
		state.variant = cat.variant().id;
		state.role = cat.getRole();
		state.sitting = cat.isOrderedToSit();
		state.loafing = cat.isLoafing();
		state.tamed = cat.isTame();
		state.actionTick = cat.actionTick();
		state.earFlick = cat.earFlickTicks();
	}
}
