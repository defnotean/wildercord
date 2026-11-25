package dev.wildercord.client.ocean;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.CullableEntityRenderer;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.client.trials.BedrockGeoRig;
import dev.wildercord.client.trials.TrialTelegraphs;
import dev.wildercord.ocean.ThalassorBoss;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renders Thalassor on its articulated Bedrock leviathan rig with emissive abyssal runes. */
public final class ThalassorRenderer extends CullableEntityRenderer<ThalassorBoss, OceanRenderStates.ThalassorState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Wildercord.id("thalassor"), "main");
	private static final Identifier SKIN = Wildercord.id("textures/entity/overhaul/thalassor.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/overhaul/thalassor_glow.png");
	private static final Identifier GEO = Wildercord.id("models/entity/overhaul/thalassor.geo.json");
	private static final Identifier ANIM = Wildercord.id("animations/entity/overhaul/thalassor.animation.json");

	private static BedrockGeoRig sharedRig;

	public ThalassorRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.shadowRadius = 1.6F;
	}

	private static BedrockGeoRig rig() {
		if (sharedRig == null) {
			sharedRig = BedrockGeoRig.load(GEO, ANIM);
		}
		return sharedRig;
	}

	@Override
	public OceanRenderStates.ThalassorState createRenderState() {
		return new OceanRenderStates.ThalassorState();
	}

	@Override
	public void extractRenderState(ThalassorBoss entity, OceanRenderStates.ThalassorState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.bodyYaw = Mth.lerp(partial, entity.yBodyRotO, entity.yBodyRot);
		state.hurtFlash = entity.hurtTime > 0 ? Math.max(0.0F, (entity.hurtTime - partial) / 10.0F) : 0.0F;
		state.jawOpen = entity.getJawOpen();
		state.attackMode = entity.getAttackMode();
	}

	@Override
	public void submit(OceanRenderStates.ThalassorState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		if (state.isInvisible) return;
		BedrockGeoRig rig = rig();
		String baseClip = "animation.thalassor.swim_idle";
		float baseSec = state.ageInTicks / 20.0F;
		String actionClip = switch (state.attackMode) {
			case 1 -> "animation.thalassor.bite_lunge";
			case 2 -> "animation.thalassor.maelstrom_spin";
			case 3 -> "animation.thalassor.hydro_beam";
			default -> state.jawOpen > 0.25F ? "animation.thalassor.hydro_beam" : null;
		};
		float actionWeight = actionClip == null ? 0.0F : Math.max(0.75F, Math.min(1.0F, state.jawOpen));
		int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(state.hurtFlash > 0.0F));

		poseStack.pushPose();
		RenderCompat.rotateDegrees(poseStack, Axis.YP, 180.0F - state.bodyYaw);
		float s = 0.78F / 16.0F;
		poseStack.scale(s, s, s);

		collector.order(0).submitCustomGeometry(poseStack, RenderTypes.entityCutout(SKIN),
			(pose, buffer) -> rig.poseAndSubmit(baseClip, baseSec, actionClip, baseSec, actionWeight,
				TrialTelegraphs.poseStackFrom(pose), buffer, state.lightCoords, overlay, 0xFFFFFFFF));
		collector.order(1).submitCustomGeometry(poseStack, RenderTypes.eyes(GLOW),
			(pose, buffer) -> rig.poseAndSubmit(baseClip, baseSec, actionClip, baseSec, actionWeight,
				TrialTelegraphs.poseStackFrom(pose), buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF));

		poseStack.popPose();
	}
}
