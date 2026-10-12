package dev.wildercord.client.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.CapybaraContent;
import dev.wildercord.wildlife.MirebathCapybara;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/** The mirebath capybara (see {@link MirebathCapybaraModel}): settled in its bath, and the apple on its head. */
public final class MirebathCapybaraRenderer extends WildlifeRenderer<MirebathCapybara, MirebathCapybaraModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("mirebath_capybara"), "main");

	public MirebathCapybaraRenderer(EntityRendererProvider.Context context) {
		super(context, new MirebathCapybaraModel(context.bakeLayer(LAYER)), 0.5F, WildlifeRenderers.texture("mirebath_capybara"));
	}

	@Override
	public void extractRenderState(MirebathCapybara capybara, WildlifeRenderState state, float partial) {
		super.extractRenderState(capybara, state, partial);
		state.rest = Mth.lerp(partial, capybara.batheO, capybara.bathe);
		state.hold = Mth.lerp(partial, capybara.appleO, capybara.apple);
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LAYER, MirebathCapybaraModel::createLayer);
		EntityRendererRegistry.register(CapybaraContent.MIREBATH_CAPYBARA, MirebathCapybaraRenderer::new);
	}
}
