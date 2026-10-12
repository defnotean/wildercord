package dev.wildercord.client.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.PikaContent;
import dev.wildercord.wildlife.TussockPika;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/** The tussock pika (see {@link TussockPikaModel}): basking on its rock, and what it carries in its mouth. */
public final class TussockPikaRenderer extends WildlifeRenderer<TussockPika, TussockPikaModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("tussock_pika"), "main");

	public TussockPikaRenderer(EntityRendererProvider.Context context) {
		super(context, new TussockPikaModel(context.bakeLayer(LAYER)), 0.3F, WildlifeRenderers.texture("tussock_pika"));
	}

	@Override
	public void extractRenderState(TussockPika pika, WildlifeRenderState state, float partial) {
		super.extractRenderState(pika, state, partial);
		state.rest = Mth.lerp(partial, pika.baskO, pika.bask);
		state.held = pika.carrying();
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LAYER, TussockPikaModel::createLayer);
		EntityRendererRegistry.register(PikaContent.TUSSOCK_PIKA, TussockPikaRenderer::new);
	}
}
