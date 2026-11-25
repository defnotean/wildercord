package dev.wildercord.client.trials;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.presentation.SafeColourRules;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Shared safe-palette ground ring, cone, and tether telegraph geometry for Co-Op Trial boss renderers. */
public final class TrialTelegraphs {
	private TrialTelegraphs() {}

	public static PoseStack poseStackFrom(PoseStack.Pose basePose) {
		PoseStack local = new PoseStack();
		local.last().pose().set(basePose.pose());
		local.last().normal().set(basePose.normal());
		return local;
	}

	static int color(int argb) {
		return MagicQuality.safeTelegraphs ? SafeColourRules.safe(argb) : argb;
	}

	static void cleaveCone(PoseStack.Pose p, VertexConsumer b, float aimYawDeg, float halfRad, float radius, int color) {
		float aimRad = aimYawDeg * Mth.DEG_TO_RAD;
		int steps = 24;
		for (int i = 0; i < steps; i++) {
			float a0 = aimRad - halfRad + 2.0F * halfRad * i / steps;
			float a1 = aimRad - halfRad + 2.0F * halfRad * (i + 1) / steps;
			float x0 = -Mth.sin(a0), z0 = Mth.cos(a0);
			float x1 = -Mth.sin(a1), z1 = Mth.cos(a1);
			quad(p, b, x0 * (radius - 0.18F), z0 * (radius - 0.18F), x0 * radius, z0 * radius,
				x1 * radius, z1 * radius, x1 * (radius - 0.18F), z1 * (radius - 0.18F), color);
		}
		for (float edge : new float[]{aimRad - halfRad, aimRad + halfRad}) {
			line(p, b, 0.0F, 0.0F, -Mth.sin(edge) * radius, Mth.cos(edge) * radius, 0.09F, color);
		}
	}

	static void ring(PoseStack.Pose p, VertexConsumer b, float cx, float cz, float radius, int color) {
		int segs = 32;
		for (int i = 0; i < segs; i++) {
			float a = i * Mth.TWO_PI / segs;
			float c = (i + 1) * Mth.TWO_PI / segs;
			line(p, b, cx + Mth.sin(a) * radius, cz + Mth.cos(a) * radius,
				cx + Mth.sin(c) * radius, cz + Mth.cos(c) * radius, 0.10F, color);
		}
	}

	static void line(PoseStack.Pose p, VertexConsumer b, float x0, float z0, float x1, float z1, float width, int color) {
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
