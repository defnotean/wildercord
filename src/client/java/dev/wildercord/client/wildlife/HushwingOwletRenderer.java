package dev.wildercord.client.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.HushwingOwlet;
import dev.wildercord.wildlife.OwletContent;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/** The hushwing owlet, asleep, staring something down or fluttering down from a height. */
public final class HushwingOwletRenderer extends WildlifeRenderer<HushwingOwlet, HushwingOwletModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("hushwing_owlet"), "main");

	public HushwingOwletRenderer(EntityRendererProvider.Context context) {
		super(context, new HushwingOwletModel(context.bakeLayer(LAYER)), 0.25F, WildlifeRenderers.texture("hushwing_owlet"));
	}

	@Override
	public void extractRenderState(HushwingOwlet owlet, WildlifeRenderState state, float partial) {
		super.extractRenderState(owlet, state, partial);
		state.rest = Mth.lerp(partial, owlet.sleepO, owlet.sleep);
		state.alert = Mth.lerp(partial, owlet.staringO, owlet.staring);
		state.flapStrength = Mth.lerp(partial, owlet.flutterO, owlet.flutter);
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LAYER, HushwingOwletModel::createLayer);
		EntityRendererRegistry.register(OwletContent.HUSHWING_OWLET, HushwingOwletRenderer::new);
	}
}
