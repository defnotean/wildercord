package dev.wildercord.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.MeerkatContent;
import dev.wildercord.wildlife.SunwatchMeerkat;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** The sunwatch meerkat, sandy and banded, dark round the eyes. */
public final class SunwatchMeerkatRenderer extends WildlifeRenderer<SunwatchMeerkat, SunwatchMeerkatModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("sunwatch_meerkat"), "main");
	private static final Identifier TEXTURE = WildlifeRenderers.texture("sunwatch_meerkat");
	static final float SIZE = 0.9F;

	public SunwatchMeerkatRenderer(EntityRendererProvider.Context context) {
		super(context, new SunwatchMeerkatModel(context.bakeLayer(LAYER)), 0.25F, TEXTURE);
	}

	@Override
	public void extractRenderState(SunwatchMeerkat meerkat, WildlifeRenderState state, float partial) {
		super.extractRenderState(meerkat, state, partial);
		state.watch = Mth.lerp(partial, meerkat.standO, meerkat.stand);
		state.hide = Mth.lerp(partial, meerkat.downO, meerkat.down);
		state.rest = Mth.lerp(partial, meerkat.sunO, meerkat.sun);
		state.dig = Mth.lerp(partial, meerkat.digO, meerkat.dig);
		state.settle = Mth.lerp(partial, meerkat.sleepO, meerkat.sleep);
		state.graze = Mth.lerp(partial, meerkat.munchO, meerkat.munch);
	}

	@Override
	protected void scale(WildlifeRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(SIZE, SIZE, SIZE);
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LAYER, SunwatchMeerkatModel::createLayer);
		EntityRendererRegistry.register(MeerkatContent.SUNWATCH_MEERKAT, SunwatchMeerkatRenderer::new);
	}
}
