package dev.wildercord.client.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.CapcrownSnail;
import dev.wildercord.wildlife.SnailContent;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/** The capcrown snail, its shell crowned red or brown, or bare once shorn. */
public final class CapcrownSnailRenderer extends WildlifeRenderer<CapcrownSnail, CapcrownSnailModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("capcrown_snail"), "main");

	public CapcrownSnailRenderer(EntityRendererProvider.Context context) {
		super(context, new CapcrownSnailModel(context.bakeLayer(LAYER)), 0.35F, WildlifeRenderers.texture("capcrown_snail"));
	}

	@Override
	public void extractRenderState(CapcrownSnail snail, WildlifeRenderState state, float partial) {
		super.extractRenderState(snail, state, partial);
		state.hide = Mth.lerp(partial, snail.hideO, snail.hide);
		state.variant = snail.colour() + (snail.capped() ? 0 : 2);
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LAYER, CapcrownSnailModel::createLayer);
		EntityRendererRegistry.register(SnailContent.CAPCROWN_SNAIL, CapcrownSnailRenderer::new);
	}
}
