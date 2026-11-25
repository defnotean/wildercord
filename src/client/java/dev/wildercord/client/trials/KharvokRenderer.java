package dev.wildercord.client.trials;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.CullableEntityRenderer;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.trials.KharvokBoss;
import dev.wildercord.trials.KharvokRules;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renders Kharvok's 40-bone Blockbench rig, emissive furnace glow, and foundry combat telegraphs. */
public final class KharvokRenderer extends CullableEntityRenderer<KharvokBoss, KharvokRenderState> {
	private static final Identifier SKIN = Wildercord.id("textures/entity/trials/kharvok.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/trials/kharvok_glow.png");
	private static final Identifier MARK = Wildercord.id("textures/entity/trials/vaelkhor_mark.png");
	private static final Identifier GEO = Wildercord.id("models/entity/trials/kharvok.geo.json");
	private static final Identifier ANIM = Wildercord.id("animations/entity/trials/kharvok.animation.json");

	private static BedrockGeoRig sharedRig;

	public KharvokRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 1.45F;
	}

	private static BedrockGeoRig rig() {
		if (sharedRig == null) {
			sharedRig = BedrockGeoRig.load(GEO, ANIM);
		}
		return sharedRig;
	}

	@Override
	public KharvokRenderState createRenderState() {
		return new KharvokRenderState();
	}

	@Override
	protected boolean shouldRenderEntity(KharvokBoss entity, Frustum frustum, double x, double y, double z, float partial) {
		return superShouldRenderEntity(entity, frustum, x, y, z, partial)
			|| frustum.isVisible(entity.getBoundingBox().inflate(18.0, 4.0, 18.0));
	}

	@Override
	public void extractRenderState(KharvokBoss entity, KharvokRenderState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.bodyYaw = Mth.lerp(partial, entity.yBodyRotO, entity.yBodyRot);
		state.move = entity.move();
		state.elapsedTicks = entity.elapsed(partial);
		state.aimYaw = entity.aimYaw();
		state.phaseTwo = entity.phaseTwo();
		state.staggered = entity.staggered();
		state.hurtFlash = entity.hurtTime > 0 ? Math.max(0.0F, (entity.hurtTime - partial) / 10.0F) : 0.0F;
		state.ventMask = entity.ventMask();
		state.soakCount = entity.soakCount();
		state.soakFilledMask = entity.soakFilledMask();
		double px = Mth.lerp(partial, entity.xo, entity.getX());
		double pz = Mth.lerp(partial, entity.zo, entity.getZ());
		var center = entity.effectiveCenter();
		state.centerOffsetX = (float) (center.getX() + 0.5 - px);
		state.centerOffsetZ = (float) (center.getZ() + 0.5 - pz);
	}

	@Override
	public void submit(KharvokRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		if (state.isInvisible) return;
		BedrockGeoRig rig = rig();
		String baseClip = state.move == KharvokRules.STRIDE ? "animation.kharvok.stride" : "animation.kharvok.idle";
		float baseSec = state.ageInTicks / 20.0F;
		String actionClip = switch (state.move) {
			case KharvokRules.ANVIL_SHATTER -> "animation.kharvok.anvil_shatter";
			case KharvokRules.CHAIN_SHACKLE -> "animation.kharvok.chain_shackle";
			case KharvokRules.CRUCIBLE_MELTDOWN -> "animation.kharvok.crucible_meltdown";
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

		if (state.move == KharvokRules.ANVIL_SHATTER || state.move == KharvokRules.CRUCIBLE_MELTDOWN
			|| (state.move == KharvokRules.CHAIN_SHACKLE && state.ventMask != 0)) {
			collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(MARK),
				(pose, buffer) -> drawTelegraphs(state, pose, buffer));
		}
	}

	private static void drawTelegraphs(KharvokRenderState s, PoseStack.Pose p, VertexConsumer b) {
		if (s.move == KharvokRules.ANVIL_SHATTER && s.elapsedTicks <= KharvokRules.SHATTER_ACTIVE_END_TICKS) {
			int color = TrialTelegraphs.color(s.elapsedTicks >= KharvokRules.SHATTER_TELL_TICKS ? 0xEEFFF1A0 : 0xCCF86A28);
			TrialTelegraphs.cleaveCone(p, b, s.aimYaw, (float) KharvokRules.SHATTER_HALF_ANGLE_RAD, (float) KharvokRules.SHATTER_REACH, color);
		} else if (s.move == KharvokRules.CRUCIBLE_MELTDOWN && s.soakCount > 0) {
			for (int i = 0; i < s.soakCount; i++) {
				boolean filled = (s.soakFilledMask & (1 << i)) != 0;
				int color = TrialTelegraphs.color(filled ? 0xDD40E8B0 : 0xDDF86820);
				TrialTelegraphs.ring(p, b, s.centerOffsetX + (float) KharvokRules.MELTDOWN_SOAK_OFFSETS[i][0],
					s.centerOffsetZ + (float) KharvokRules.MELTDOWN_SOAK_OFFSETS[i][1], (float) KharvokRules.SOAK_RADIUS, color);
			}
		} else if (s.move == KharvokRules.CHAIN_SHACKLE && s.ventMask != 0) {
			int color = TrialTelegraphs.color(0xCCF59B2A);
			for (int i = 0; i < 4; i++) {
				if ((s.ventMask & (1 << i)) != 0) {
					float tx = s.centerOffsetX + KharvokRules.VENT_OFFSETS[i][0];
					float tz = s.centerOffsetZ + KharvokRules.VENT_OFFSETS[i][1];
					TrialTelegraphs.line(p, b, 0.0F, 0.0F, tx, tz, 0.11F, color);
					TrialTelegraphs.ring(p, b, tx, tz, 1.8F, color);
				}
			}
		}
	}
}
