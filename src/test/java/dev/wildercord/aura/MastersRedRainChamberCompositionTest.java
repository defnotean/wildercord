package dev.wildercord.aura;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/** Source geometry only; unchanged native captures still decide shaded blade readability. */
class MastersRedRainChamberCompositionTest {
	private static final float NEAR = .05F;
	private static final float NATIVE_WEIGHT = .9505149F, NATIVE_HEIGHT = .85689604F;
	private static final int[][] LAYOUTS = {{854, 480, 2}, {1280, 720, 2}, {1280, 960, 4}, {1920, 810, 3}};
	// Original immutable 1280x720 HUD-on native sidecar, run 37476041308, head 3d392.
	// masters_style_red_rain_left_turn_first_windup: one sword submit, zero cyan pixels.
	private static final float[] FAILED_NATIVE_PROJECTION = {
		.5207933F, .7735608F, -1.30642675E-5F, .53509927F,
		-.5298018F, 1.072084F, -9.554958E-7F, .03913614F,
		.30566087F, .5402284F, 2.06031E-5F, -.8438823F,
		-.50206125F, -.25608504F, .049975697F, 1.0455663F
	};

	@Test
	void recordedNativeFailureHasNoVisibleBroadFaceAndCorrectedChamberRestoresIt() throws Exception {
		var texture = texture();
		var observed = new Matrix4f(FAILED_NATIVE_PROJECTION);
		// Native hand projection uses reversed depth. Separate the camera prefix from the
		// residual swing so both retain their actual side of the authored hand.
		var projection = new Matrix4f().zero().m00((float) (1 / Math.tan(Math.toRadians(35)) / (1280D / 720)))
			.m11((float) (1 / Math.tan(Math.toRadians(35)))).m22(NEAR / (2048 - NEAR))
			.m32(NEAR * 2048 / (2048 - NEAR)).m23(-1);
		var actualCamera = projection.invert(new Matrix4f()).mul(observed);
		var old = new MastersArtAnimation.Hand(.06F, .38F, -.14F, -62, 30, -38);
		var chamber = MastersArtAnimation.sample(18, 4.5F, 8, 16);
		assertEquals(NATIVE_WEIGHT, chamber.weight(), .00001F);
		var originalPose = withHand(chamber, old);
		var originalItem = new Matrix4f(actualCamera).mul(item(true));
		var broken = metrics(texture, originalItem, LAYOUTS[1]);
		assertEquals(0, broken.area, "Regression fixture must expose neither broad face");
		assertTrue(Math.abs(faceDistance(originalItem)) < .68F / 32, "Camera lies within the sprite's thickness slab");
		var residualSwing = MastersViewMotion.fadeSwing(swing(.75F, true), chamber.weight());
		var originalHandAndSwing = hand(originalPose, true, -90, -75, NATIVE_HEIGHT).mul(residualSwing);
		var cameraPrefix = new Matrix4f(actualCamera).mul(originalHandAndSwing.invert());
		assertTrue(cameraPrefix.getTranslation(new Vector3f()).length() < .00001F,
			"Recorded residual decomposes into the native camera rotation before hand and swing");
		var corrected = cameraPrefix.mul(hand(chamber, true, -90, -75, NATIVE_HEIGHT)).mul(residualSwing).mul(item(true));
		var fixed = metrics(texture, corrected, LAYOUTS[1]);
		assertReadable(fixed, LAYOUTS[1], "corrected exact native camera");
		assertFalse(fixed.aimOverlap, "The correction keeps the native aim corridor clear");
	}

	@Test
	void chamberEntryAndFullGuardKeepAVisibleFaceAcrossBothHandsAndTheEntireLookDomain() throws Exception {
		var texture = texture();
		for (float age = 0; age <= 5.25F; age += .25F) {
			// Cover the complete settled-hand entry. From the first native capture age (3), also
			// replay the observed attack-height curve; an actually lowered vanilla hand at weight
			// zero is not a readable-art frame. 5.2 is the chamber keyframe before the cut.
			var pose = MastersArtAnimation.sample(18, Math.min(age, 5.2F), 8, 16);
			for (boolean residual : age < 3 ? new boolean[] {false} : new boolean[] {false, true})
			for (boolean left : new boolean[] {false, true}) for (int[] layout : LAYOUTS)
				for (float yaw = -55; yaw <= 55; yaw += 5) for (float pitch = -40; pitch <= 40; pitch += 5) {
					String label = "age=" + age + " left=" + left + " yaw=" + yaw + " pitch=" + pitch + " width=" + layout[0];
					float previous = Math.min(1, ((int) age + 2) / 12.5F), current = Math.min(1, ((int) age + 3) / 12.5F);
					float height = residual ? 1 - (previous * previous * previous + current * current * current) / 2 : 0;
					var camera = hand(pose, left, yaw, pitch, height)
						.mul(MastersViewMotion.fadeSwing(swing(residual ? Math.min(1, age / 6) : 0, left), pose.weight())).mul(item(left));
					var measured = metrics(texture, camera, layout);
					assertReadable(measured, layout, label);
					assertFalse(measured.aimOverlap, "Opaque chamber blade enters the aim corridor: " + label);
				}
		}
	}

	@Test
	void chamberChangeKeepsTheGripAndTimelineContinuousAndDoesNotAlterReleaseOrRecovery() {
		assertEquals(new MastersArtAnimation.Hand(.22F, .38F, -.14F, -62, 48, -20), MastersArtAnimation.sample(18, 5.2F, 8, 16).hand());
		var oldHand = new MastersArtAnimation.Hand(.06F, .38F, -.14F, -62, 30, -38);
		for (boolean left : new boolean[] {false, true}) for (float age = 0; age <= 8; age += .125F) {
			var pose = MastersArtAnimation.sample(18, age, 8, 16);
			var now = MastersArtAnimation.view(pose, left, .85689604F, -90, -75);
			var before = MastersArtAnimation.view(withHand(pose, oldHand), left, .85689604F, -90, -75);
			assertEquals(before.grip(), now.grip(), "View rotation retains the vanilla hilt pivot");
		}
		for (float at : new float[] {0, 5.2F, 8, 12, 24}) {
			var a = MastersArtAnimation.sample(18, Math.max(0, at - .001F), 8, 16);
			var b = MastersArtAnimation.sample(18, at + .001F, 8, 16);
			for (boolean left : new boolean[] {false, true}) {
				var ma = hand(a, left, -90, -75, 0).mul(item(left));
				var mb = hand(b, left, -90, -75, 0).mul(item(left));
				for (float x : new float[] {0, 1}) for (float y : new float[] {0, 1})
					assertTrue(ma.transformPosition(new Vector3f(x, y, .5F)).distance(mb.transformPosition(new Vector3f(x, y, .5F))) < .002F,
						"No camera-space jump across age " + at);
			}
		}
		assertEquals(new MastersArtAnimation.Hand(-.28F, .28F, -.40F, 42, -32, 46), MastersArtAnimation.sample(18, 8, 8, 16).hand());
		assertEquals(new MastersArtAnimation.Hand(-.34F, .34F, -.28F, 48, -40, 58), MastersArtAnimation.sample(18, 12, 8, 16).hand());
	}

	private static void assertReadable(Metrics m, int[] layout, String label) {
		assertTrue(m.near > NEAR, "Blade crosses the camera near plane: " + label);
		assertTrue(m.area >= 512 * Math.pow(layout[1] / 720D, 2), "Visible cyan face lacks area above HUD: " + label + " area=" + m.area);
		assertTrue(m.span >= 16 * layout[2], "Visible cutting edge lacks span above HUD: " + label + " span=" + m.span);
	}

	private record Metrics(double area, double span, double near, boolean aimOverlap) {}
	private static Metrics metrics(BufferedImage texture, Matrix4f camera, int[] layout) {
		int width = layout[0], height = layout[1], hudTop = height - 39 * layout[2];
		var projection = new Matrix4f().perspective((float) Math.toRadians(70), width / (float) height, NEAR, 2048).mul(camera);
		float faceDistance = faceDistance(camera), z = faceDistance > 0 ? 8.5F / 16 : 7.5F / 16;
		boolean facing = Math.abs(faceDistance) > .68F / 32;
		double area = 0, near = Double.POSITIVE_INFINITY, minX = Double.POSITIVE_INFINITY, minY = minX, maxX = -minX, maxY = maxX;
		boolean overlap = false;
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
			int rgb = texture.getRGB(x, y), r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
			if ((rgb >>> 24) == 0) continue;
			for (float zz : new float[] {7.5F / 16, 8.5F / 16}) for (int dy = 0; dy <= 1; dy++) for (int dx = 0; dx <= 1; dx++)
				near = Math.min(near, -camera.transformPosition(new Vector3f((x + dx) / 16F, 1 - (y + dy) / 16F, zz)).z);
			// Include the extrusion side walls, so a dark edge cannot hide reticle occlusion.
			var front = screen(projection, x, y, 7.5F / 16, width, height);
			var back = screen(projection, x, y, 8.5F / 16, width, height);
			var faces = new ArrayList<List<double[]>>(); faces.add(front); faces.add(back);
			for (int edge = 0; edge < 4; edge++) { int next = (edge + 1) % 4;
				faces.add(List.of(front.get(edge), front.get(next), back.get(next), back.get(edge)));
			}
			for (var outline : faces) {
				var path = new Path2D.Double(); path.moveTo(outline.getFirst()[0], outline.getFirst()[1]);
				for (int n = 1; n < outline.size(); n++) path.lineTo(outline.get(n)[0], outline.get(n)[1]);
				path.closePath();
				overlap |= path.intersects(width / 2D - height * 32D / 720, height / 2D - height * 32D / 720, height * 64D / 720, height * 64D / 720);
			}
			if (!facing || y >= 8 || x < 6 || !(g > 75 && r < g * .7 && b > g * .65 && b < g * 1.3)) continue;
			List<double[]> quad = screen(projection, x, y, z, width, height);
			quad = clip(quad, 0, 0, true); quad = clip(quad, 0, width, false);
			quad = clip(quad, 1, 0, true); quad = clip(quad, 1, hudTop, false);
			if (quad.size() < 3) continue;
			double cross = 0;
			for (int n = 0; n < quad.size(); n++) {
				var a = quad.get(n); var next = quad.get((n + 1) % quad.size());
				cross += a[0] * next[1] - a[1] * next[0];
				minX = Math.min(minX, a[0]); minY = Math.min(minY, a[1]); maxX = Math.max(maxX, a[0]); maxY = Math.max(maxY, a[1]);
			}
			area += Math.abs(cross) / 2;
		}
		return new Metrics(area, area == 0 ? 0 : Math.max(maxX - minX, maxY - minY), near, overlap);
	}

	private static List<double[]> screen(Matrix4f projection, int x, int y, float z, int width, int height) {
		var result = new ArrayList<double[]>();
		for (int[] offset : new int[][] {{0, 0}, {1, 0}, {1, 1}, {0, 1}}) {
			var p = projection.transform(new Vector4f((x + offset[0]) / 16F, 1 - (y + offset[1]) / 16F, z, 1));
			result.add(new double[] {(p.x / p.w + 1) * width / 2, (1 - p.y / p.w) * height / 2});
		}
		return result;
	}
	private static List<double[]> clip(List<double[]> polygon, int axis, double boundary, boolean greater) {
		var result = new ArrayList<double[]>(); if (polygon.isEmpty()) return result;
		var previous = polygon.getLast(); double before = (previous[axis] - boundary) * (greater ? 1 : -1);
		for (var current : polygon) {
			double after = (current[axis] - boundary) * (greater ? 1 : -1);
			if ((before >= 0) != (after >= 0)) { double t = before / (before - after); result.add(new double[] {previous[0] + (current[0] - previous[0]) * t, previous[1] + (current[1] - previous[1]) * t}); }
			if (after >= 0) result.add(current); previous = current; before = after;
		}
		return result;
	}
	private static float faceDistance(Matrix4f camera) {
		var normal = camera.transformDirection(new Vector3f(0, 0, 1)).normalize();
		return normal.dot(camera.transformPosition(new Vector3f(.5F, .5F, .5F)).negate());
	}
	private static BufferedImage texture() throws Exception {
		try (var in = Objects.requireNonNull(MastersRedRainChamberCompositionTest.class.getResourceAsStream("/assets/minecraft/textures/item/diamond_sword.png"))) { return ImageIO.read(in); }
	}
	private static MastersArtAnimation.Pose withHand(MastersArtAnimation.Pose pose, MastersArtAnimation.Hand hand) {
		return new MastersArtAnimation.Pose(pose.weight(), pose.body(), pose.head(), pose.sword(), pose.guard(), pose.frontLeg(), pose.rearLeg(), pose.lower(), pose.forward(), hand);
	}
	private static Matrix4f hand(MastersArtAnimation.Pose pose, boolean left, float yaw, float pitch, float height) {
		var view = MastersArtAnimation.view(pose, left, height, yaw, pitch); var h = view.transform(); var grip = view.grip();
		return new Matrix4f().translate(0, MastersViewMotion.heightCompensation(height, pose.weight()), 0)
			.translate(h.x(), h.y(), h.z()).translate(grip.x(), grip.y(), grip.z())
			.rotateY((float) Math.toRadians(h.yaw())).rotateX((float) Math.toRadians(h.pitch())).rotateZ((float) Math.toRadians(h.roll()));
	}
	private static Matrix4f item(boolean left) {
		return new Matrix4f().translate((left ? -1.13F : 1.13F) / 16, 3.2F / 16, 1.13F / 16)
			.rotateY((float) -Math.PI / 2).rotateZ((float) Math.toRadians(25)).scale(.68F).translate(-.5F, -.5F, -.5F);
	}
	private static Matrix4f swing(float attack, boolean left) {
		float side = left ? -1 : 1, root = (float) Math.sqrt(attack), swing = (float) Math.sin(root * Math.PI), squared = (float) Math.sin(attack * attack * Math.PI);
		return new Matrix4f().translate(side * -.4F * swing, .2F * (float) Math.sin(root * 2 * Math.PI), -.2F * (float) Math.sin(attack * Math.PI))
			.rotateY((float) Math.toRadians(side * (45 - 20 * squared))).rotateZ((float) Math.toRadians(side * -20 * swing))
			.rotateX((float) Math.toRadians(-80 * swing)).rotateY((float) Math.toRadians(side * -45));
	}
}
