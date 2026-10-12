package dev.wildercord.client.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.PuffinContent;
import dev.wildercord.wildlife.StonecrestPuffin;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** The stonecrest puffin, black above and white below, its great bill banded to an orange-red tip. */
public final class StonecrestPuffinRenderer extends WildlifeRenderer<StonecrestPuffin, StonecrestPuffinModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("stonecrest_puffin"), "main");
	private static final Identifier TEXTURE = WildlifeRenderers.texture("stonecrest_puffin");

	public StonecrestPuffinRenderer(EntityRendererProvider.Context context) {
		super(context, new StonecrestPuffinModel(context.bakeLayer(LAYER)), 0.25F, TEXTURE);
	}

	@Override
	public void extractRenderState(StonecrestPuffin puffin, WildlifeRenderState state, float partial) {
		super.extractRenderState(puffin, state, partial);
		state.swim = Mth.lerp(partial, puffin.swimO, puffin.swim);
		state.dive = Mth.lerp(partial, puffin.diveO, puffin.dive);
		state.flapStrength = Mth.lerp(partial, puffin.flapO, puffin.flap);
		state.flapPhase = Mth.lerp(partial, puffin.beatO, puffin.beat);
		state.bow = Mth.lerp(partial, puffin.preenO, puffin.preen);
		state.knock = Mth.lerp(partial, puffin.tapO, puffin.tap);
		state.settle = Mth.lerp(partial, puffin.sleepO, puffin.sleep);
		state.graze = Mth.lerp(partial, puffin.munchO, puffin.munch);
		state.held = puffin.carrying() ? 1 : 0;
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LAYER, StonecrestPuffinModel::createLayer);
		EntityRendererRegistry.register(PuffinContent.STONECREST_PUFFIN, StonecrestPuffinRenderer::new);
	}
}
