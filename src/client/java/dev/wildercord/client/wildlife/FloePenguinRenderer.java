package dev.wildercord.client.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.FloePenguin;
import dev.wildercord.wildlife.PenguinContent;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** The floe penguin (see {@link FloePenguinModel}): tobogganing, swimming, huddling, its catch, and its fluffy grey chicks. */
public final class FloePenguinRenderer extends WildlifeRenderer<FloePenguin, FloePenguinModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("floe_penguin"), "main");
	private static final Identifier ADULT = WildlifeRenderers.texture("floe_penguin");
	private static final Identifier CHICK = WildlifeRenderers.texture("floe_penguin_chick");

	public FloePenguinRenderer(EntityRendererProvider.Context context) {
		super(context, new FloePenguinModel(context.bakeLayer(LAYER)), 0.35F, ADULT);
	}

	@Override
	public Identifier getTextureLocation(WildlifeRenderState state) {
		return state.isBaby ? CHICK : ADULT;
	}

	@Override
	public void extractRenderState(FloePenguin penguin, WildlifeRenderState state, float partial) {
		super.extractRenderState(penguin, state, partial);
		state.dive = Mth.lerp(partial, penguin.slideO, penguin.slide);
		state.swim = Mth.lerp(partial, penguin.swimO, penguin.swim);
		state.rest = Mth.lerp(partial, penguin.huddleO, penguin.huddle);
		state.held = penguin.carrying();
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LAYER, FloePenguinModel::createLayer);
		EntityRendererRegistry.register(PenguinContent.FLOE_PENGUIN, FloePenguinRenderer::new);
	}
}
