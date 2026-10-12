package dev.wildercord.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.wildlife.BrookOtter;
import dev.wildercord.wildlife.OtterContent;
import dev.wildercord.wildlife.OtterRules;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/** The brook otter, brown with a cream throat. */
public final class BrookOtterRenderer extends WildlifeRenderer<BrookOtter, BrookOtterModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("brook_otter"), "main");
	static final float SIZE = 0.95F;

	public BrookOtterRenderer(EntityRendererProvider.Context context) {
		super(context, new BrookOtterModel(context.bakeLayer(LAYER)), 0.35F, WildlifeRenderers.texture("brook_otter"));
	}

	@Override
	public void extractRenderState(BrookOtter otter, WildlifeRenderState state, float partial) {
		super.extractRenderState(otter, state, partial);
		state.onBack = Mth.lerp(partial, otter.backO, otter.back);
		state.sit = Mth.lerp(partial, otter.sitO, otter.sit);
		state.swim = Mth.lerp(partial, otter.swimO, otter.swim);
		state.crack = Mth.lerp(partial, otter.crackO, otter.crack);
		state.hold = Mth.lerp(partial, otter.holdO, otter.hold);
		state.held = otter.held();
		state.otterPose = otter.pose();
		state.reachRight = otter.reachesRight();
		// The knock: paws rise slowly through most of each beat, then come down hard.
		int left = otter.poseLeft();
		float p = Mth.clamp(((OtterRules.KNOCK_EVERY - left % OtterRules.KNOCK_EVERY) + partial) / OtterRules.KNOCK_EVERY, 0, 1);
		state.knock = left <= 6 ? 0 : p < 0.7F ? 1 - p / 0.7F : (p - 0.7F) / 0.3F;
	}

	@Override
	protected void scale(WildlifeRenderState state, PoseStack poseStack) {
		super.scale(state, poseStack);
		poseStack.scale(SIZE, SIZE, SIZE);
	}

	public static void init() {
		ModelLayerRegistry.registerModelLayer(LAYER, BrookOtterModel::createLayer);
		EntityRendererRegistry.register(OtterContent.BROOK_OTTER, BrookOtterRenderer::new);
	}
}
