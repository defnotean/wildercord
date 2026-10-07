package dev.wildercord.aura;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/** Geometry regressions only. Actual lighting, aura, submitted frames and pixels remain native gates. */
class MastersBraceNullCompositionTest {
	@Test
	void classicBraceThrustAndReceivingCloseRetainTheirOwnSilhouettes() {
		var stone = MastersStyleRules.animation(24);
		var hollow = MastersStyleRules.animation(25);
		var brace = MastersArtAnimation.sample(24, stone.windup() * .65F, stone.windup(), stone.recovery());
		var thrust = MastersArtAnimation.sample(24, stone.windup(), stone.windup(), stone.recovery());
		assertTrue(thrust.sword().x() < brace.sword().x() - .5F, "Unmoved extends a level point from its loaded base");
		assertTrue(Math.abs(thrust.lower() - brace.lower()) < .05F);
		var receive = MastersArtAnimation.sample(25, hollow.windup() * .65F, hollow.windup(), hollow.recovery());
		var sweep = MastersArtAnimation.sample(25, hollow.windup(), hollow.windup(), hollow.recovery());
		assertTrue(sweep.sword().y() < receive.sword().y() - 1, "Null sweeps inward from its oblique receiving plane");
		assertTrue(Math.abs(sweep.sword().x() - receive.sword().x()) < .2F);
		assertNotEquals(thrust, sweep);
		for (int move : new int[] {24, 25}) {
			var style = MastersStyleRules.animation(move);
			for (float age = 0; age < style.windup() + style.recovery(); age += .125F) {
				var pose = MastersArtAnimation.sample(move, age, style.windup(), style.recovery());
				assertTrue(Float.isFinite(pose.weight()) && pose.weight() >= 0 && pose.weight() <= 1);
				assertEquals(move == 24 ? -80 * pose.weight() : 0, MastersArtAnimation.bladeTilt(move, age, style.windup(), pose.weight()), .00001F);
			}
			assertSame(MastersArtAnimation.NONE, MastersArtAnimation.sample(move, style.windup() + style.recovery(), style.windup(), style.recovery()));
		}
	}

	@Test
	void bothFallbackFormsKeepTheOpaqueBladeReadableAcrossPhasesHandsAndLayouts() throws Exception {
		try (var source = Objects.requireNonNull(getClass().getResourceAsStream("/assets/minecraft/textures/item/diamond_sword.png"))) {
			BufferedImage sword = ImageIO.read(source);
			for (int move : new int[] {24, 25}) {
				var style = MastersStyleRules.animation(move);
				for (float age = 0; age <= style.windup() + style.recovery(); age += .25F) {
					var pose = MastersArtAnimation.sample(move, age, style.windup(), style.recovery());
					for (boolean left : new boolean[] {false, true}) for (int[] viewport : VIEWPORTS)
						checkEdge(sword, pose, left, 0, 0, 0, 0, viewport, "move=" + move + " age=" + age);
				}
				for (float age : new float[] {style.windup() * .65F, style.windup(), style.windup() + 3,
					style.windup() + 4, style.windup() + 8, style.windup() + 12}) {
					var pose = MastersArtAnimation.sample(move, age, style.windup(), style.recovery());
					for (boolean left : new boolean[] {false, true}) for (float yaw : new float[] {-180, 0, 180})
						for (float pitch : new float[] {-90, 0, 90}) for (int[] viewport : VIEWPORTS)
							checkEdge(sword, pose, left, yaw, pitch, .7F, .5F, viewport,
								"move=" + move + " age=" + age + " yaw=" + yaw + " pitch=" + pitch);
				}
			}
		}
	}

	private static final int[][] VIEWPORTS = {{854, 480, 2}, {1280, 720, 3}, {1280, 960, 4}, {1920, 810, 3}};

	private static void checkEdge(BufferedImage sword, MastersArtAnimation.Pose pose, boolean left, float yaw, float pitch,
			float attack, float inverseHeight, int[] viewport, String label) {
		var view = MastersArtAnimation.view(pose, left, inverseHeight, yaw, pitch);
		var h = view.transform(); var grip = view.grip();
		int width = viewport[0], height = viewport[1], hudTop = height - 42 * viewport[2];
		var camera = new Matrix4f()
			.translate(0, MastersViewMotion.heightCompensation(inverseHeight, pose.weight()), 0)
			.translate(h.x(), h.y(), h.z()).translate(grip.x(), grip.y(), grip.z())
			.rotateY((float) Math.toRadians(h.yaw())).rotateX((float) Math.toRadians(h.pitch())).rotateZ((float) Math.toRadians(h.roll()))
			.mul(MastersViewMotion.fadeSwing(vanillaSwing(attack, left ? -1 : 1), pose.weight()))
			.translate((left ? -1.13F : 1.13F) / 16, 3.2F / 16, 1.13F / 16)
			.rotateY((float) -Math.PI / 2).rotateZ((float) Math.toRadians(25)).scale(.68F).translate(-.5F, -.5F, -.5F);
		var matrix = new Matrix4f().perspective((float) Math.toRadians(70), width / (float) height, .05F, 2048).mul(camera);
		int visible = 0;
		float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
		// Sample actual opaque cutting-edge texels, not the transparent sprite rectangle.
		for (int y = 0; y < 8; y++) for (int x = 6; x < 16; x++) {
			int argb = sword.getRGB(x, y), r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
			if ((argb >>> 24) == 0 || !(g > 75 && r < g * .7 && b > g * .65 && b < g * 1.3)) continue;
			for (float z : new float[] {7.5F / 16, 8.5F / 16}) for (float dy : new float[] {.2F, .5F, .8F}) for (float dx : new float[] {.2F, .5F, .8F}) {
				var vertex = new Vector4f((x + dx) / 16, 1 - (y + dy) / 16, z, 1);
				var eye = camera.transform(new Vector4f(vertex));
				assertTrue(eye.z < -.05F, "Opaque blade crosses camera near plane: " + label + " left=" + left);
				var point = matrix.transform(vertex);
				float sx = (point.x / point.w + 1) * width / 2, sy = (1 - point.y / point.w) * height / 2;
				if (sx < 0 || sx >= width || sy < 0 || sy >= hudTop) continue;
				visible++; minX = Math.min(minX, sx); maxX = Math.max(maxX, sx); minY = Math.min(minY, sy); maxY = Math.max(maxY, sy);
			}
		}
		assertTrue(visible >= 8 && Math.max(maxX - minX, maxY - minY) >= height * .04F,
			"Readable opaque edge above HUD: " + label + " left=" + left + " viewport=" + width + "x" + height + " visible=" + visible);
	}
	private static Matrix4f vanillaSwing(float attack, float side) {
		float root = (float) Math.sqrt(attack);
		float swing = (float) Math.sin(root * Math.PI), squared = (float) Math.sin(attack * attack * Math.PI);
		return new Matrix4f().translate(side * -.4F * swing, .2F * (float) Math.sin(root * 2 * Math.PI), -.2F * (float) Math.sin(attack * Math.PI))
			.rotateY((float) Math.toRadians(side * (45 - 20 * squared))).rotateZ((float) Math.toRadians(side * -20 * swing))
			.rotateX((float) Math.toRadians(-80 * swing)).rotateY((float) Math.toRadians(side * -45));
	}

}
