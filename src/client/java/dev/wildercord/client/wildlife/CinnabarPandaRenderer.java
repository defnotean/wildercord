package dev.wildercord.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.CinnabarPanda;
import dev.wildercord.wildlife.RedPandaContent;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** The cinnabar panda, rust-red and black with its white mask and ringed tail. */
public final class CinnabarPandaRenderer extends WildlifeRenderer<CinnabarPanda, CinnabarPandaModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("cinnabar_panda"), "main");
	private static final Identifier TEXTURE = WildlifeRenderers.texture("cinnabar_panda");
	static final float SIZE = 0.95F;

	public CinnabarPandaRenderer(EntityRendererProvider.Context context) {
		super(context, new CinnabarPandaModel(context.bakeLayer(LAYER)), 0.35F, TEXTURE);
	}

	@Override
	public void extractRenderState(CinnabarPanda panda, WildlifeRenderState state, float partial) {
		super.extractRenderState(panda, state, partial);
		state.alert = Mth.lerp(partial, panda.standO, panda.stand);
		state.rest = Mth.lerp(partial, panda.washO, panda.wash);
		state.graze = Mth.lerp(partial, panda.munchO, panda.munch);
		state.hide = Mth.lerp(partial, panda.curlO, panda.curl);
		state.dance = Mth.lerp(partial, panda.tumbleO, panda.tumble);
	}

	@Override
	protected void scale(WildlifeRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(SIZE, SIZE, SIZE);
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LAYER, CinnabarPandaModel::createLayer);
		EntityRendererRegistry.register(RedPandaContent.CINNABAR_PANDA, CinnabarPandaRenderer::new);
	}
}
