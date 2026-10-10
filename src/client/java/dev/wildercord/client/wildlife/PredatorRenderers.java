package dev.wildercord.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.BlackBobcat;
import dev.wildercord.wildlife.PredatorContent;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The black bobcat's kin (0.13): the Frost Lynx on the bobcat's own build, a touch smaller, and the Dune Cougar on its
 * long-tailed build, as big as the bobcat.
 */
public final class PredatorRenderers {
	private PredatorRenderers() {}

	public static final ModelLayerLocation LYNX = new ModelLayerLocation(Wildercord.id("frost_lynx"), "main");
	public static final ModelLayerLocation COUGAR = new ModelLayerLocation(Wildercord.id("dune_cougar"), "main");

	/** A big cat on {@link BlackBobcatModel}, drawn at {@code size} times its model. */
	static final class BigCat<T extends BlackBobcat> extends WildlifeRenderer<T, BlackBobcatModel> {
		private final float size;

		BigCat(EntityRendererProvider.Context context, ModelLayerLocation layer, String texture, float size) {
			super(context, new BlackBobcatModel(context.bakeLayer(layer)), 0.65F * size, WildlifeRenderers.texture(texture));
			this.size = size;
		}

		@Override
		public void extractRenderState(T cat, WildlifeRenderState state, float partial) {
			super.extractRenderState(cat, state, partial);
			state.sit = Mth.lerp(partial, cat.sitO, cat.sit);
		}

		@Override
		protected void scale(WildlifeRenderState state, PoseStack poseStack) {
			super.scale(state, poseStack);
			poseStack.scale(size, size, size);
		}
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LYNX, BlackBobcatModel::createLayer);
		ModelLayerRegistry.registerModelLayer(COUGAR, BlackBobcatModel::createLongTailLayer);
		EntityRendererRegistry.register(PredatorContent.FROST_LYNX, context -> new BigCat<>(context, LYNX, "frost_lynx", 1.08F));
		EntityRendererRegistry.register(PredatorContent.DUNE_COUGAR, context -> new BigCat<>(context, COUGAR, "dune_cougar", 1.25F));
	}
}
