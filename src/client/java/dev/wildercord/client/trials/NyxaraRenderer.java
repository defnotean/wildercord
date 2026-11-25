package dev.wildercord.client.trials;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.CullableEntityRenderer;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.trials.NyxaraBoss;
import dev.wildercord.trials.NyxaraRules;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renders Nyxara's 40-bone Blockbench rig, emissive astral script, and eclipse telegraphs. */
public final class NyxaraRenderer extends CullableEntityRenderer<NyxaraBoss, NyxaraRenderState> {
	private static final Identifier SKIN = Wildercord.id("textures/entity/trials/nyxara.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/trials/nyxara_glow.png");
	private static final Identifier MARK = Wildercord.id("textures/entity/trials/vaelkhor_mark.png");
	private static final Identifier GEO = Wildercord.id("models/entity/trials/nyxara.geo.json");
	private static final Identifier ANIM = Wildercord.id("animations/entity/trials/nyxara.animation.json");

	private static BedrockGeoRig sharedRig;

	public NyxaraRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 1.3F;
	}

	private static BedrockGeoRig rig() {
		if (sharedRig == null) {
			sharedRig = BedrockGeoRig.load(GEO, ANIM);
		}
		return sharedRig;
	}

	@Override
	public NyxaraRenderState createRenderState() {
		return new NyxaraRenderState();
	}

	@Override
	protected boolean shouldRenderEntity(NyxaraBoss entity, Frustum frustum, double x, double y, double z, float partial) {
		return superShouldRenderEntity(entity, frustum, x, y, z, partial)
			|| frustum.isVisible(entity.getBoundingBox().inflate(18.0, 4.0, 18.0));
	}

	@Override
	public void extractRenderState(NyxaraBoss entity, NyxaraRenderState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.bodyYaw = Mth.lerp(partial, entity.yBodyRotO, entity.yBodyRot);
		state.move = entity.move();
		state.elapsedTicks = entity.elapsed(partial);
		state.aimYaw = entity.aimYaw();
		state.phaseTwo = entity.phaseTwo();
		state.staggered = entity.staggered();
		state.hurtFlash = entity.hurtTime > 0 ? Math.max(0.0F, (entity.hurtTime - partial) / 10.0F) : 0.0F;
		state.prismMask = entity.prismMask();
		state.soakCount = entity.soakCount();
		state.soakFilledMask = entity.soakFilledMask();
		double px = Mth.lerp(partial, entity.xo, entity.getX());
		double pz = Mth.lerp(partial, entity.zo, entity.getZ());
		var center = entity.effectiveCenter();
		state.centerOffsetX = (float) (center.getX() + 0.5 - px);
		state.centerOffsetZ = (float) (center.getZ() + 0.5 - pz);
	}

	@Override
	public void submit(NyxaraRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		if (state.isInvisible) return;
		BedrockGeoRig rig = rig();
		String baseClip = state.move == NyxaraRules.STRIDE ? "animation.nyxara.stride" : "animation.nyxara.idle";
		float baseSec = state.ageInTicks / 20.0F;
		String actionClip = switch (state.move) {
			case NyxaraRules.CRESCENT_SEVER -> "animation.nyxara.crescent_sever";
			case NyxaraRules.PRISM_CONVERGENCE -> "animation.nyxara.prism_convergence";
			case NyxaraRules.TOTAL_ECLIPSE -> "animation.nyxara.total_eclipse";
			default -> null;
		};
		float actionSec = Math.max(0.0F, state.elapsedTicks / 20.0F);
		float actionWeight = actionClip == null ? 0.0F : Math.min(1.0F, state.elapsedTicks / 3.0F);

		int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(state.hurtFlash > 0.0F));
		int tint = state.staggered ? 0xFFB8D4FF : 0xFFFFFFFF;

		poseStack.pushPose();
		RenderCompat.rotateDegrees(poseStack, Axis.YP, 180.0F - state.bodyYaw);
		poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

		collector.order(0).submitCustomGeometry(poseStack, RenderTypes.entityCutout(SKIN),
			(pose, buffer) -> rig.poseAndSubmit(baseClip, baseSec, actionClip, actionSec, actionWeight,
				TrialTelegraphs.poseStackFrom(pose), buffer, state.lightCoords, overlay, tint));
		collector.order(1).submitCustomGeometry(poseStack, RenderTypes.eyes(GLOW),
			(pose, buffer) -> rig.poseAndSubmit(baseClip, baseSec, actionClip, actionSec, actionWeight,
				TrialTelegraphs.poseStackFrom(pose), buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF));

		poseStack.popPose();

		if (state.move == NyxaraRules.CRESCENT_SEVER || state.move == NyxaraRules.TOTAL_ECLIPSE
			|| (state.move == NyxaraRules.PRISM_CONVERGENCE && state.prismMask != 0)) {
			collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(MARK),
				(pose, buffer) -> drawTelegraphs(state, pose, buffer));
		}
	}

	private static void drawTelegraphs(NyxaraRenderState s, PoseStack.Pose p, VertexConsumer b) {
		if (s.move == NyxaraRules.CRESCENT_SEVER && s.elapsedTicks <= NyxaraRules.SEVER_ACTIVE_END_TICKS) {
			int color = TrialTelegraphs.color(s.elapsedTicks >= NyxaraRules.SEVER_TELL_TICKS ? 0xEEFFF1A0 : 0xCCB46CFF);
			TrialTelegraphs.cleaveCone(p, b, s.aimYaw, (float) NyxaraRules.SEVER_HALF_ANGLE_RAD, (float) NyxaraRules.SEVER_REACH, color);
		} else if (s.move == NyxaraRules.TOTAL_ECLIPSE && s.soakCount > 0) {
			for (int i = 0; i < s.soakCount; i++) {
				boolean filled = (s.soakFilledMask & (1 << i)) != 0;
				int color = TrialTelegraphs.color(filled ? 0xDD40E8B0 : 0xDDB46CFF);
				TrialTelegraphs.ring(p, b, s.centerOffsetX + (float) NyxaraRules.ECLIPSE_SOAK_OFFSETS[i][0],
					s.centerOffsetZ + (float) NyxaraRules.ECLIPSE_SOAK_OFFSETS[i][1], (float) NyxaraRules.SOAK_RADIUS, color);
			}
		} else if (s.move == NyxaraRules.PRISM_CONVERGENCE && s.prismMask != 0) {
			int color = TrialTelegraphs.color(0xCCB87EFF);
			for (int i = 0; i < 4; i++) {
				if ((s.prismMask & (1 << i)) != 0) {
					float tx = s.centerOffsetX + NyxaraRules.PRISM_OFFSETS[i][0];
					float tz = s.centerOffsetZ + NyxaraRules.PRISM_OFFSETS[i][1];
					TrialTelegraphs.line(p, b, 0.0F, 0.0F, tx, tz, 0.11F, color);
					TrialTelegraphs.ring(p, b, tx, tz, 1.8F, color);
				}
			}
		}
	}
}
