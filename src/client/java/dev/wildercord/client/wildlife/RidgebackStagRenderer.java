package dev.wildercord.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.wildlife.MountContent;
import dev.wildercord.wildlife.RidgebackStag;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;

/** The Ridgeback Stag: the Lumen Stag's frame in its own chestnut coat, with a saddle painted on once it wears one. */
public final class RidgebackStagRenderer extends WildlifeRenderer<RidgebackStag, LumenStagModel> {
	private static final Identifier BARE = WildlifeRenderers.texture("ridgeback_stag");
	private static final Identifier SADDLED = WildlifeRenderers.texture("ridgeback_stag_saddled");

	public RidgebackStagRenderer(EntityRendererProvider.Context context) {
		super(context, new LumenStagModel(context.bakeLayer(WildlifeRenderers.LUMEN_STAG)), 0.75F, BARE);
	}

	@Override
	public Identifier getTextureLocation(WildlifeRenderState state) {
		return state.variant == 1 ? SADDLED : BARE;
	}

	@Override
	public void extractRenderState(RidgebackStag stag, WildlifeRenderState state, float partial) {
		super.extractRenderState(stag, state, partial);
		state.variant = stag.getItemBySlot(EquipmentSlot.SADDLE).isEmpty() ? 0 : 1;
		state.graze = stag.getEatAnim(partial);
		state.bow = 0;
		state.watch = 0;
		state.glow = 0;
	}

	/** A touch bigger than its wild cousin, so the saddle sits where a rider's seat is. */
	@Override
	protected void scale(WildlifeRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(1.1F, 1.1F, 1.1F);
	}

	public static void init() {
		EntityRendererRegistry.register(MountContent.RIDGEBACK_STAG, RidgebackStagRenderer::new);
	}
}
