package dev.wildercord.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.SnowcapStoat;
import dev.wildercord.wildlife.StoatContent;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** The snowcap stoat, in its white winter coat where it is cold enough to snow and its brown summer coat elsewhere. */
public final class SnowcapStoatRenderer extends WildlifeRenderer<SnowcapStoat, SnowcapStoatModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("snowcap_stoat"), "main");
	private static final Identifier WINTER = WildlifeRenderers.texture("snowcap_stoat"), SUMMER = WildlifeRenderers.texture("snowcap_stoat_summer");
	static final float SIZE = 0.9F;

	public SnowcapStoatRenderer(EntityRendererProvider.Context context) {
		super(context, new SnowcapStoatModel(context.bakeLayer(LAYER)), 0.3F, WINTER);
	}

	@Override
	public void extractRenderState(SnowcapStoat stoat, WildlifeRenderState state, float partial) {
		super.extractRenderState(stoat, state, partial);
		state.alert = Mth.lerp(partial, stoat.standO, stoat.stand);
		state.dance = Mth.lerp(partial, stoat.danceO, stoat.dance);
		state.dive = Mth.lerp(partial, stoat.diveO, stoat.dive);
		state.variant = stoat.winter() ? 0 : 1;
	}

	@Override
	public Identifier getTextureLocation(WildlifeRenderState state) {
		return state.variant == 0 ? WINTER : SUMMER;
	}

	@Override
	protected void scale(WildlifeRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(SIZE, SIZE, SIZE);
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LAYER, SnowcapStoatModel::createLayer);
		EntityRendererRegistry.register(StoatContent.SNOWCAP_STOAT, SnowcapStoatRenderer::new);
	}
}
