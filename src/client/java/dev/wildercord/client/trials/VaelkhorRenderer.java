package dev.wildercord.client.trials;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.CullableEntityRenderer;
import dev.wildercord.client.compat.RenderCompat;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.presentation.SafeColourRules;
import dev.wildercord.trials.VaelkhorBoss;
import dev.wildercord.trials.VaelkhorRules;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renders Vaelkhor's 1:1 40-bone Blockbench rig, emissive leyline veins, and safe-palette telegraphs. */
public final class VaelkhorRenderer extends CullableEntityRenderer<VaelkhorBoss, VaelkhorRenderState> {
	private static final Identifier SKIN = Wildercord.id("textures/entity/trials/vaelkhor.png");
	private static final Identifier GLOW = Wildercord.id("textures/entity/trials/vaelkhor_glow.png");
	private static final Identifier MARK = Wildercord.id("textures/entity/trials/vaelkhor_mark.png");
	private static final Identifier GEO = Wildercord.id("models/entity/trials/vaelkhor.geo.json");
	private static final Identifier ANIM = Wildercord.id("animations/entity/trials/vaelkhor.animation.json");

	private static BedrockGeoRig sharedRig;

	public VaelkhorRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 1.4F;
	}

	private static BedrockGeoRig rig() {
		if (sharedRig == null) {
			sharedRig = BedrockGeoRig.load(GEO, ANIM);
		}
		return sharedRig;
	}

	@Override
	public VaelkhorRenderState createRenderState() {
		return new VaelkhorRenderState();
	}

	@Override
	protected boolean shouldRenderEntity(VaelkhorBoss entity, Frustum frustum, double x, double y, double z, float partial) {
		return superShouldRenderEntity(entity, frustum, x, y, z, partial)
			|| frustum.isVisible(entity.getBoundingBox().inflate(18.0, 4.0, 18.0));
	}

	@Override
	public void extractRenderState(VaelkhorBoss entity, VaelkhorRenderState state, float partial) {
		super.extractRenderState(entity, state, partial);
		state.bodyYaw = Mth.lerp(partial, entity.yBodyRotO, entity.yBodyRot);
		state.move = entity.move();
		state.elapsedTicks = entity.elapsed(partial);
		state.aimYaw = entity.aimYaw();
		state.phaseTwo = entity.phaseTwo();
		state.staggered = entity.staggered();
		state.hurtFlash = entity.hurtTime > 0 ? Math.max(0.0F, (entity.hurtTime - partial) / 10.0F) : 0.0F;
		state.conduitMask = entity.conduitMask();
		state.soakCount = entity.soakCount();
		state.soakFilledMask = entity.soakFilledMask();
		double px = Mth.lerp(partial, entity.xo, entity.getX());
		double pz = Mth.lerp(partial, entity.zo, entity.getZ());
		var center = entity.effectiveCenter();
		state.centerOffsetX = (float) (center.getX() + 0.5 - px);
		state.centerOffsetZ = (float) (center.getZ() + 0.5 - pz);
		for (int i = 0; i < 3; i++) {
			state.soakX[i] = entity.soakOffsetX(i);
			state.soakZ[i] = entity.soakOffsetZ(i);
		}
	}

	@Override
	public void submit(VaelkhorRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		super.submit(state, poseStack, collector, camera);
		if (state.isInvisible) {
			return;
		}
		BedrockGeoRig rig = rig();
		String baseClip = state.move == VaelkhorRules.STRIDE ? "animation.vaelkhor.stride" : "animation.vaelkhor.idle";
		float baseSec = state.ageInTicks / 20.0F;
		String actionClip = switch (state.move) {
			case VaelkhorRules.HALBERD_CLEAVE -> "animation.vaelkhor.halberd_cleave";
			case VaelkhorRules.BELL_TOLL_CONDUIT -> "animation.vaelkhor.bell_toll_conduit";
			case VaelkhorRules.LEYLINE_CATACLYSM -> "animation.vaelkhor.leyline_cataclysm";
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
			(pose, buffer) -> rig.poseAndSubmit(baseClip, baseSec, actionClip, actionSec, actionWeight, poseStackFrom(pose), buffer, state.lightCoords, overlay, tint));
		collector.order(1).submitCustomGeometry(poseStack, RenderTypes.eyes(GLOW),
			(pose, buffer) -> rig.poseAndSubmit(baseClip, baseSec, actionClip, actionSec, actionWeight, poseStackFrom(pose), buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF));

		poseStack.popPose();

		if (state.move == VaelkhorRules.HALBERD_CLEAVE || state.move == VaelkhorRules.LEYLINE_CATACLYSM
			|| (state.move == VaelkhorRules.BELL_TOLL_CONDUIT && state.conduitMask != 0)) {
			collector.order(2).submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(MARK),
				(pose, buffer) -> drawTelegraphs(state, pose, buffer));
		}
	}

	private static PoseStack poseStackFrom(PoseStack.Pose basePose) {
		PoseStack local = new PoseStack();
		local.last().pose().set(basePose.pose());
		local.last().normal().set(basePose.normal());
		return local;
	}

	private static int telegraphColor(int argb) {
		return MagicQuality.safeTelegraphs ? SafeColourRules.safe(argb) : argb;
	}

	private static void drawTelegraphs(VaelkhorRenderState s, PoseStack.Pose p, VertexConsumer b) {
		if (s.move == VaelkhorRules.HALBERD_CLEAVE && s.elapsedTicks <= VaelkhorRules.CLEAVE_ACTIVE_END_TICKS) {
			int color = telegraphColor(s.elapsedTicks >= VaelkhorRules.CLEAVE_TELL_TICKS ? 0xEEFFF1A0 : 0xCCF04050);
			float aimRad = s.aimYaw * Mth.DEG_TO_RAD;
			float half = (float) VaelkhorRules.CLEAVE_HALF_ANGLE_RAD;
			float r = (float) VaelkhorRules.CLEAVE_REACH;
			int steps = 24;
			for (int i = 0; i < steps; i++) {
				float a0 = aimRad - half + 2.0F * half * i / steps;
				float a1 = aimRad - half + 2.0F * half * (i + 1) / steps;
				float x0 = -Mth.sin(a0), z0 = Mth.cos(a0);
				float x1 = -Mth.sin(a1), z1 = Mth.cos(a1);
				quad(p, b, x0 * (r - 0.18F), z0 * (r - 0.18F), x0 * r, z0 * r, x1 * r, z1 * r, x1 * (r - 0.18F), z1 * (r - 0.18F), color);
			}
			for (float edge : new float[]{aimRad - half, aimRad + half}) {
				line(p, b, 0.0F, 0.0F, -Mth.sin(edge) * r, Mth.cos(edge) * r, 0.09F, color);
			}
		} else if (s.move == VaelkhorRules.LEYLINE_CATACLYSM && s.soakCount > 0) {
			for (int i = 0; i < s.soakCount; i++) {
				boolean filled = (s.soakFilledMask & (1 << i)) != 0;
				int color = telegraphColor(filled ? 0xDD40E8B0 : 0xDDF05830);
				ring(p, b, s.centerOffsetX + s.soakX[i], s.centerOffsetZ + s.soakZ[i], (float) VaelkhorRules.SOAK_RADIUS, color);
			}
		} else if (s.move == VaelkhorRules.BELL_TOLL_CONDUIT && s.conduitMask != 0) {
			int color = telegraphColor(0xCC3CE8E0);
			for (int i = 0; i < 4; i++) {
				if ((s.conduitMask & (1 << i)) != 0) {
					float tx = s.centerOffsetX + VaelkhorRules.CONDUIT_OFFSETS[i][0];
					float tz = s.centerOffsetZ + VaelkhorRules.CONDUIT_OFFSETS[i][1];
					line(p, b, 0.0F, 0.0F, tx, tz, 0.11F, color);
					ring(p, b, tx, tz, 1.8F, color);
				}
			}
		}
	}

	private static void ring(PoseStack.Pose p, VertexConsumer b, float cx, float cz, float radius, int color) {
		int segs = 32;
		for (int i = 0; i < segs; i++) {
			float a = i * Mth.TWO_PI / segs;
			float c = (i + 1) * Mth.TWO_PI / segs;
			line(p, b, cx + Mth.sin(a) * radius, cz + Mth.cos(a) * radius,
				cx + Mth.sin(c) * radius, cz + Mth.cos(c) * radius, 0.10F, color);
		}
	}

	private static void line(PoseStack.Pose p, VertexConsumer b, float x0, float z0, float x1, float z1, float width, int color) {
		float len = Mth.sqrt((x1 - x0) * (x1 - x0) + (z1 - z0) * (z1 - z0));
		if (len < 1.0e-4F) return;
		float nx = -(z1 - z0) / len * width;
		float nz = (x1 - x0) / len * width;
		quad(p, b, x0 - nx, z0 - nz, x1 - nx, z1 - nz, x1 + nx, z1 + nz, x0 + nx, z0 + nz, color);
	}

	private static void quad(PoseStack.Pose p, VertexConsumer b,
		float x0, float z0, float x1, float z1, float x2, float z2, float x3, float z3, int color) {
		vtx(p, b, x0, z0, 0.0F, 0.0F, color);
		vtx(p, b, x1, z1, 1.0F, 0.0F, color);
		vtx(p, b, x2, z2, 1.0F, 1.0F, color);
		vtx(p, b, x3, z3, 0.0F, 1.0F, color);
	}

	private static void vtx(PoseStack.Pose p, VertexConsumer b, float x, float z, float u, float v, int color) {
		b.addVertex(p, x, 0.06F, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0, 1, 0);
	}
}
