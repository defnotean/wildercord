package dev.wildercord.client.wildlife;

import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.WhiteRabbit;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Renders {@link WhiteRabbit} with its crimson neck ribbon and swinging brass pocket watch. */
public final class WhiteRabbitRenderer extends MobRenderer<WhiteRabbit, WhiteRabbitRenderState, WhiteRabbitModel> {
	public static final ModelLayerLocation LAYER =
		new ModelLayerLocation(Wildercord.id("white_rabbit"), "main");
	private static final ResourceLocation TEXTURE =
		Wildercord.id("textures/entity/wildlife/white_rabbit.png");

	public WhiteRabbitRenderer(EntityRendererProvider.Context context) {
		super(context, new WhiteRabbitModel(context.bakeLayer(LAYER)), 0.35F);
	}

	@Override
	public WhiteRabbitRenderState createRenderState() {
		return new WhiteRabbitRenderState();
	}

	@Override
	public ResourceLocation getTextureLocation(WhiteRabbitRenderState state) {
		return TEXTURE;
	}

	@Override
	public void extractRenderState(WhiteRabbit entity, WhiteRabbitRenderState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.rabbitState = entity.rabbitState();
		state.checkingWatch = entity.isCheckingWatch();
		state.air = Mth.lerp(partial, entity.airO, entity.air);
	}
}
