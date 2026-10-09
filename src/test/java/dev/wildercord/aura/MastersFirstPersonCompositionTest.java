package dev.wildercord.aura;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/** Geometry regressions only. Actual lighting, aura, submitted frames and pixels remain native gates. */
class MastersFirstPersonCompositionTest {
	@Test
	void everyFallbackPhaseKeepsPartOfTheCuttingEdgeAboveTheSurvivalHud() throws Exception {
		try (var source = Objects.requireNonNull(getClass().getResourceAsStream("/assets/minecraft/textures/item/diamond_sword.png"))) {
			BufferedImage sword = ImageIO.read(source);
			assertEquals(16, sword.getWidth()); assertEquals(16, sword.getHeight());
			for (int move = 0; move < 15; move++) {
				var style = MastersStyleRules.animation(move);
				int windup = style == null ? new int[] {4, 8, 6}[move] : style.windup();
				int recovery = style == null ? new int[] {12, 18, 14}[move] : style.recovery();
				for (float age : new float[] {windup / 2 + .5F, windup + .5F, windup + recovery / 2 + .5F}) {
					var pose = MastersArtAnimation.sample(move, age, windup, recovery);
					for (boolean left : new boolean[] {false, true}) for (float pitch : new float[] {0, -12}) {
						String label = "move=" + move + " age=" + age + " left=" + left + " pitch=" + pitch;
						checkEdge(sword, pose, left, 0, pitch, label);
						// Replay the observed default-diamond input curve: raw inverse height is
						// .912704 at age3.5 and .682304 at age6.5, then settles fully. These are
						// synthetic geometry checks, not an assertion about future framebuffer ages.
						float previous = Math.min(1, ((int) age + 2) / 12.5F), current = Math.min(1, ((int) age + 3) / 12.5F);
						float inverseHeight = 1 - (previous * previous * previous + current * current * current) / 2;
						checkEdge(sword, pose, left, 0, pitch, label + " residual-input-motion", Math.min(1, age / 6), inverseHeight);
					}
					if (move == 13 || move == 14) checkEdge(sword, pose, true, -90, -50, "second-form turned left move=" + move + " age=" + age);
				}
			}
		}
	}

	private static void checkEdge(BufferedImage sword, MastersArtAnimation.Pose pose, boolean left, float yaw, float pitch, String label) {
		checkEdge(sword, pose, left, yaw, pitch, label, 0, 0);
	}

	private static void checkEdge(BufferedImage sword, MastersArtAnimation.Pose pose, boolean left, float yaw, float pitch, String label,
			float attack, float inverseHeight) {
		var view = MastersArtAnimation.view(pose, left, inverseHeight, yaw, pitch);
		var h = view.transform(); var grip = view.grip();
		// The recorded 26.3 matrices independently calibrate this 70-degree native hand camera and
		// vanilla handheld display. The source view supplies bounded aim, mirroring and blend weight.
		var matrix = new Matrix4f().perspective((float) Math.toRadians(70), 1280F / 720, .05F, 2048)
			.translate(0, MastersViewMotion.heightCompensation(inverseHeight, pose.weight()), 0)
			.translate(h.x(), h.y(), h.z()).translate(grip.x(), grip.y(), grip.z())
			.rotateY((float) Math.toRadians(h.yaw())).rotateX((float) Math.toRadians(h.pitch())).rotateZ((float) Math.toRadians(h.roll()))
			.mul(MastersViewMotion.fadeSwing(vanillaSwing(attack, left ? -1 : 1), pose.weight()))
			.translate((left ? -1.13F : 1.13F) / 16, 3.2F / 16, 1.13F / 16)
			.rotateY((float) -Math.PI / 2).rotateZ((float) Math.toRadians(25)).scale(.68F).translate(-.5F, -.5F, -.5F);
		int visible = 0;
		float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
		// Only the cutting edge's upper half, excluding the cyan crossguard and pommel. Sample
		// opaque cyan texels on both native front/back planes; transparent quad corners prove nothing.
		for (int y = 0; y < 8; y++) for (int x = 6; x < 16; x++) {
			int argb = sword.getRGB(x, y), r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
			if ((argb >>> 24) == 0 || !(g > 75 && r < g * .7 && b > g * .65 && b < g * 1.3)) continue;
			for (float z : new float[] {7.5F / 16, 8.5F / 16}) for (float dy : new float[] {.2F, .5F, .8F}) for (float dx : new float[] {.2F, .5F, .8F}) {
				var point = matrix.transform(new Vector4f((x + dx) / 16, 1 - (y + dy) / 16, z, 1));
				if (point.w <= .05F) continue;
				float sx = (point.x / point.w + 1) * 640, sy = (1 - point.y / point.w) * 360;
				if (sx < 0 || sx >= 1280 || sy < 0 || sy >= 642) continue;
				visible++; minX = Math.min(minX, sx); maxX = Math.max(maxX, sx); minY = Math.min(minY, sy); maxY = Math.max(maxY, sy);
			}
		}
		assertTrue(visible >= 8 && Math.max(maxX - minX, maxY - minY) >= 32,
			"Readable cutting-edge geometry above the observed scale-2 HUD: " + label + " visibleSamples=" + visible);
	}
	private static Matrix4f vanillaSwing(float attack, float side) {
		float root = (float) Math.sqrt(attack);
		float swing = (float) Math.sin(root * Math.PI), squared = (float) Math.sin(attack * attack * Math.PI);
		return new Matrix4f().translate(side * -.4F * swing, .2F * (float) Math.sin(root * 2 * Math.PI), -.2F * (float) Math.sin(attack * Math.PI))
			.rotateY((float) Math.toRadians(side * (45 - 20 * squared))).rotateZ((float) Math.toRadians(side * -20 * swing))
			.rotateX((float) Math.toRadians(-80 * swing)).rotateY((float) Math.toRadians(side * -45));
	}

}
