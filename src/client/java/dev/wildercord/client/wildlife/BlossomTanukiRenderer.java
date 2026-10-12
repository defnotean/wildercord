package dev.wildercord.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.BlossomTanuki;
import dev.wildercord.wildlife.TanukiContent;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

/**
 * The blossom tanuki, and the bush it turns into: a little clump of real cherry leaves (their own block model, drawn small)
 * that swells up out of a puff of petals where it stood, and shrinks away again when it pops back out.
 */
public final class BlossomTanukiRenderer extends WildlifeRenderer<BlossomTanuki, BlossomTanukiModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("blossom_tanuki"), "main");
	private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();
	/** The bush, full grown, in blocks across. */
	private static final float BUSH_SIZE = 0.7F;

	private final BlockModelResolver blocks;

	public BlossomTanukiRenderer(EntityRendererProvider.Context context) {
		super(context, new BlossomTanukiModel(context.bakeLayer(LAYER)), 0.35F, WildlifeRenderers.texture("blossom_tanuki"));
		this.blocks = context.getBlockModelResolver();
		addLayer(new BushLayer(this));
	}

	@Override
	public void extractRenderState(BlossomTanuki tanuki, WildlifeRenderState state, float partial) {
		super.extractRenderState(tanuki, state, partial);
		state.hide = Mth.lerp(partial, tanuki.bushO, tanuki.bush);
		state.dance = Mth.lerp(partial, tanuki.drumO, tanuki.drum);
		state.hold = Mth.lerp(partial, tanuki.leafO, tanuki.leaf);
		blocks.update(state.garden[0], Blocks.CHERRY_LEAVES.defaultBlockState(), DISPLAY);
	}

	/** The bush: cherry leaves set on the ground where it stood, swelling as it turns, with a little shiver now and then. */
	private static final class BushLayer extends RenderLayer<WildlifeRenderState, BlossomTanukiModel> {
		BushLayer(RenderLayerParent<WildlifeRenderState, BlossomTanukiModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, WildlifeRenderState state, float yRot, float xRot) {
			if (state.hide < 0.05F || state.isInvisible) {
				return;
			}
			float grow = state.hide < 0.5F ? state.hide * 2 : 1;
			// A little overshoot as it pops into a bush, then settled.
			float size = BUSH_SIZE * grow * (1 + 0.15F * Mth.sin(Mth.clamp(state.hide, 0, 1) * Mth.PI));
			if (state.isBaby) {
				size *= 0.6F;
			}
			float t = state.ageInTicks + state.seed;
			float shiver = Mth.clamp(Mth.sin(t * 0.04F) * 5 - 4, 0, 1) * Mth.sin(t * 1.7F) * 2.5F;
			poseStack.pushPose();
			// Down to the ground (model space is upside down, its feet at 24 pixels), then upright again and rooted at its base.
			poseStack.translate(0, 1.501F, 0);
			poseStack.scale(-size, -size, size);
			poseStack.rotateDegrees(Axis.ZP, shiver);
			poseStack.translate(-0.5F, 0, -0.5F);
			state.garden[0].submit(poseStack, collector, light, LivingEntityRenderer.getOverlayCoords(state, 0), state.outlineColor);
			poseStack.popPose();
		}
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LAYER, BlossomTanukiModel::createLayer);
		EntityRendererRegistry.register(TanukiContent.BLOSSOM_TANUKI, BlossomTanukiRenderer::new);
	}
}
