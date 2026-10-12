package dev.wildercord.client.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.BrookbillDuck;
import dev.wildercord.wildlife.DuckContent;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** The brookbill duck (see {@link BrookbillDuckModel}): floating, up-ending to dabble, flapping, and its yellow ducklings. */
public final class BrookbillDuckRenderer extends WildlifeRenderer<BrookbillDuck, BrookbillDuckModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("brookbill_duck"), "main");
	private static final Identifier ADULT = WildlifeRenderers.texture("brookbill_duck");
	private static final Identifier DUCKLING = WildlifeRenderers.texture("brookbill_duckling");

	public BrookbillDuckRenderer(EntityRendererProvider.Context context) {
		super(context, new BrookbillDuckModel(context.bakeLayer(LAYER)), 0.3F, ADULT);
	}

	@Override
	public Identifier getTextureLocation(WildlifeRenderState state) {
		return state.isBaby ? DUCKLING : ADULT;
	}

	@Override
	public void extractRenderState(BrookbillDuck duck, WildlifeRenderState state, float partial) {
		super.extractRenderState(duck, state, partial);
		state.swim = Mth.lerp(partial, duck.swimO, duck.swim);
		state.dive = Mth.lerp(partial, duck.dabbleO, duck.dabble);
		state.flapStrength = Mth.lerp(partial, duck.flapO, duck.flap);
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LAYER, BrookbillDuckModel::createLayer);
		EntityRendererRegistry.register(DuckContent.BROOKBILL_DUCK, BrookbillDuckRenderer::new);
	}
}
