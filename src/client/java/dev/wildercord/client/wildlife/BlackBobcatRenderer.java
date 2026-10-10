package dev.wildercord.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.BlackBobcat;
import dev.wildercord.wildlife.BobcatContent;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/** The black bobcat, drawn at {@link #SIZE} times its model so it stands as big as a polar bear. */
public final class BlackBobcatRenderer extends WildlifeRenderer<BlackBobcat, BlackBobcatModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("black_bobcat"), "main");
	/** How much larger than its model it's drawn. */
	static final float SIZE = 1.25F;

	public BlackBobcatRenderer(EntityRendererProvider.Context context) {
		super(context, new BlackBobcatModel(context.bakeLayer(LAYER)), 0.8F, WildlifeRenderers.texture("black_bobcat"));
	}

	@Override
	public void extractRenderState(BlackBobcat bobcat, WildlifeRenderState state, float partial) {
		super.extractRenderState(bobcat, state, partial);
		state.sit = Mth.lerp(partial, bobcat.sitO, bobcat.sit);
	}

	@Override
	protected void scale(WildlifeRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(SIZE, SIZE, SIZE);
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LAYER, BlackBobcatModel::createLayer);
		EntityRendererRegistry.register(BobcatContent.BLACK_BOBCAT, BlackBobcatRenderer::new);
	}
}
