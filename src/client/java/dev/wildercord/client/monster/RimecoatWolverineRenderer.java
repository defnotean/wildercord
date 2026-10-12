package dev.wildercord.client.monster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import dev.wildercord.monster.RimecoatWolverine;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * The Rimecoat Wolverine (see {@link RimecoatWolverineModel}), and whatever food it has stolen, held crosswise in its
 * jaws as a fox holds things.
 */
public class RimecoatWolverineRenderer extends WildMonsterRenderer<RimecoatWolverine, RimecoatWolverineModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("rimecoat_wolverine"), "main");

	private final ItemModelResolver items;

	public RimecoatWolverineRenderer(EntityRendererProvider.Context context) {
		super(context, new RimecoatWolverineModel(context.bakeLayer(LAYER)), 0.6F, Wildercord.id("textures/entity/rimecoat_wolverine.png"));
		this.items = context.getItemModelResolver();
		addLayer(new MouthLayer(this));
	}

	@Override
	public MonsterRenderState createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(RimecoatWolverine wolverine, MonsterRenderState state, float partial) {
		super.extractRenderState(wolverine, state, partial);
		items.updateForLiving(((State) state).mouth, wolverine.getMainHandItem(), ItemDisplayContext.GROUND, wolverine);
	}

	/** Its pose, and what it carries. */
	static final class State extends MonsterRenderState {
		final ItemStackRenderState mouth = new ItemStackRenderState();
	}

	/** What it carries, between its jaws. */
	private static final class MouthLayer extends RenderLayer<MonsterRenderState, RimecoatWolverineModel> {
		MouthLayer(RenderLayerParent<MonsterRenderState, RimecoatWolverineModel> parent) {
			super(parent);
		}

		@Override
		public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, MonsterRenderState state, float yRot, float xRot) {
			ItemStackRenderState mouth = ((State) state).mouth;
			if (state.isInvisible || mouth.isEmpty()) {
				return;
			}
			poseStack.pushPose();
			getParentModel().toMouth(poseStack);
			poseStack.rotateDegrees(Axis.XP, 90);
			poseStack.rotateDegrees(Axis.ZP, 90);
			poseStack.scale(0.8F, 0.8F, 0.8F);
			mouth.submit(poseStack, collector, light, LivingEntityRenderer.getOverlayCoords(state, 0), state.outlineColor);
			poseStack.popPose();
		}
	}
}
