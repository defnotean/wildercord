package dev.wildercord.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.HedgehogContent;
import dev.wildercord.wildlife.ThistleHedgehog;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/** The thistle hedgehog, drawn a little under its model's size; which trinket rides in its quills is fixed by its UUID. */
public final class ThistleHedgehogRenderer extends WildlifeRenderer<ThistleHedgehog, ThistleHedgehogModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("thistle_hedgehog"), "main");
	static final float SIZE = 0.85F;

	public ThistleHedgehogRenderer(EntityRendererProvider.Context context) {
		super(context, new ThistleHedgehogModel(context.bakeLayer(LAYER)), 0.35F, WildlifeRenderers.texture("thistle_hedgehog"));
	}

	@Override
	public void extractRenderState(ThistleHedgehog hedgehog, WildlifeRenderState state, float partial) {
		super.extractRenderState(hedgehog, state, partial);
		state.hide = Mth.lerp(partial, hedgehog.curlO, hedgehog.curl);
		state.sneeze = Mth.lerp(partial, hedgehog.sneezeO, hedgehog.sneeze);
		// A thistle, a leaf, a berry, or (one in four) nothing.
		state.variant = Math.floorMod(hedgehog.getUUID().hashCode(), 4);
	}

	@Override
	protected void scale(WildlifeRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(SIZE, SIZE, SIZE);
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LAYER, ThistleHedgehogModel::createLayer);
		EntityRendererRegistry.register(HedgehogContent.THISTLE_HEDGEHOG, ThistleHedgehogRenderer::new);
	}
}
