package dev.wildercord.client.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.DormouseContent;
import dev.wildercord.wildlife.GlowberryDormouse;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** The glowberry dormouse, its cheeks glowing with the berries stuffed in them. */
public final class GlowberryDormouseRenderer extends WildlifeRenderer<GlowberryDormouse, GlowberryDormouseModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("glowberry_dormouse"), "main");
	private static final Identifier GLOW = WildlifeRenderers.texture("glowberry_dormouse_glow");

	public GlowberryDormouseRenderer(EntityRendererProvider.Context context) {
		super(context, new GlowberryDormouseModel(context.bakeLayer(LAYER)), 0.2F, WildlifeRenderers.texture("glowberry_dormouse"));
		glow(state -> GLOW);
	}

	@Override
	public void extractRenderState(GlowberryDormouse mouse, WildlifeRenderState state, float partial) {
		super.extractRenderState(mouse, state, partial);
		state.rest = Mth.lerp(partial, mouse.sleepO, mouse.sleep);
		state.rise = Mth.lerp(partial, mouse.riseO, mouse.rise);
		state.held = mouse.pouch();
		state.glow = Mth.lerp(partial, mouse.glowO, mouse.glow) * (0.9F + 0.1F * Mth.sin(state.ageInTicks * 0.15F));
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LAYER, GlowberryDormouseModel::createLayer);
		EntityRendererRegistry.register(DormouseContent.GLOWBERRY_DORMOUSE, GlowberryDormouseRenderer::new);
	}
}
