package dev.wildercord.client.monster;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import dev.wildercord.monster.BogBubble;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * A Bog Witch-Frog's bubble: a wobbling green bubble (vanilla's translucent pass) round a glowing bead of poison (the
 * glowing-eyes pass), turning as it flies. A fat one, from a frog that has just eaten, is half again as big.
 *
 * <p>Laid out on a 32x32 skin: the bubble (0,0) 6x6x6, the bead (0,12) 3x3x3.</p>
 */
public class BogBubbleRenderer extends EntityRenderer<BogBubble, BogBubbleRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("bog_bubble"), "main");
	private static final Identifier TEXTURE = Wildercord.id("textures/entity/bog_bubble.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/bog_bubble_glow.png");

	public static class State extends EntityRenderState {
		public boolean engorged;
	}

	private final Model model;

	public BogBubbleRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new Model(context.bakeLayer(LAYER));
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		poseStack.pushPose();
		float t = state.ageInTicks;
		float size = state.engorged ? 1.5F : 1.0F;
		// A bubble's skin never holds still.
		float wobble = Mth.sin(t * 0.9F) * 0.08F;
		poseStack.translate(0.0F, 0.18F * size, 0.0F);
		poseStack.scale(size * (1 + wobble), size * (1 - wobble), size * (1 + wobble));
		poseStack.rotateDegrees(Axis.YP, t * 9.0F);
		poseStack.rotateDegrees(Axis.XP, t * 5.0F);
		poseStack.translate(0.0F, -1.5F, 0.0F);
		collector.submitModel(model, state, poseStack, RenderTypes.entityTranslucent(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		collector.order(1).submitModel(model, state, poseStack, RenderTypes.eyes(GLOW), state.lightCoords, OverlayTexture.NO_OVERLAY, 0xE0FFFFFF, null,
			state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(BogBubble bubble, State state, float partial) {
		super.extractRenderState(bubble, state, partial);
		state.engorged = bubble.engorged();
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		mesh.getRoot().addOrReplaceChild("bubble", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F)
				.texOffs(0, 12).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));
		return LayerDefinition.create(mesh, 32, 32);
	}

	private static final class Model extends EntityModel<State> {
		Model(ModelPart root) {
			super(root, RenderTypes::entityTranslucent);
		}
	}
}
