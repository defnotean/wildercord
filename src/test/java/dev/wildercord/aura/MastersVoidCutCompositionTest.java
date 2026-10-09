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

/** Source geometry only; unchanged native framebuffer assertions remain the acceptance gate. */
class MastersVoidCutCompositionTest {
    private static final float NEAR = .05F, NATIVE_HEIGHT = .6823041F;
    private static final int[][] LAYOUTS = {{854, 480, 2}, {1280, 720, 2}, {1280, 960, 4}, {1920, 810, 3}};
    // Original, hash/CRC-verified HUD-on ACTIVE sidecar: run 37496328238, head dff8c58f,
    // masters_style_void_cut_first_active, nominal age 6.5 (exact rendered age unmeasured). One sword submit, 137 cyan pixels, zero readable span.
    private static final float[] FAILED_NATIVE_PROJECTION = {
        .74148035F, .3134868F, 7.7159375E-6F, -.3160371F,
        -.10591961F, 1.3596326F, -6.742344E-6F, .2761596F,
        -.29039997F, .30451992F, 2.216034E-5F, -.9076653F,
        .31262714F, -.8673487F, .049975608F, 1.0490724F
    };

    @Test
    void recordedActiveFailureIsEdgeOnAndCorrectedWristRestoresTheVisibleFace() throws Exception {
        var texture = texture();
        var projection = new Matrix4f().zero().m00((float) (1 / Math.tan(Math.toRadians(35)) / (1280D / 720)))
            .m11((float) (1 / Math.tan(Math.toRadians(35)))).m22(NEAR / (2048 - NEAR))
            .m32(NEAR * 2048 / (2048 - NEAR)).m23(-1);
        var actualCamera = projection.invert(new Matrix4f()).mul(new Matrix4f(FAILED_NATIVE_PROJECTION));
        var originalItem = new Matrix4f(actualCamera).mul(item(false));
        var broken = metrics(texture, originalItem, LAYOUTS[1]);
        assertEquals(-.0350332F, faceDistance(originalItem), .00001F);
        assertTrue(broken.area < 512, "Original broad face is too edge-on to provide a blade-area margin");
        assertTrue(broken.near > .78, "Original failure is not near-plane clipping");
        assertTrue(broken.span > 200, "Original upper blade extends above HUD despite its thin face");
        assertFalse(broken.aimOverlap);

        // The capture log labels this ACTIVE request age 6.5, but its rendered hand contains
        // a later fractional follow blend. Replay that observed hand, not the requested age.
        var observed = new MastersArtAnimation.Hand(-.17254335F, -.09220296F, -.31186575F, -15.559408F, -21.644186F, 13.084778F);
        var originalPose = withHand(MastersArtAnimation.sample(9, 6, 6, 12), observed);
        float followBlend = (observed.y() - (-.08F)) / (-.16F - (-.08F));
        assertEquals(.152537F, followBlend, .000001F);
        var impact = MastersArtAnimation.sample(9, 6, 6, 12).hand();
        var follow = MastersArtAnimation.sample(9, 9, 6, 12).hand();
        var correctedPose = withHand(originalPose, new MastersArtAnimation.Hand(observed.x(), observed.y(), observed.z(), observed.pitch(),
            impact.yaw() + (follow.yaw() - impact.yaw()) * followBlend,
            impact.roll() + (follow.roll() - impact.roll()) * followBlend));
        // Full ownership suppresses vanilla swing. The remaining native camera rotation
        // precedes the authored hand: preserve that prefix on the left of the replacement.
        var prefix = new Matrix4f(actualCamera).mul(hand(originalPose, false, 0, -3, NATIVE_HEIGHT).invert());
        assertTrue(prefix.getTranslation(new Vector3f()).length() < .00001F, "Native residual is a camera rotation, not a moved grip");
        var corrected = prefix.mul(hand(correctedPose, false, 0, -3, NATIVE_HEIGHT)).mul(item(false));
        var fixed = metrics(texture, corrected, LAYOUTS[1]);
        assertReadable(fixed, LAYOUTS[1], "corrected exact native camera");
        assertTrue(fixed.area > 3000, "The corrected native face has a substantial margin");
        assertFalse(fixed.aimOverlap, "Native aim corridor remains clear");
    }

    @Test
    void cutRemainsReadableAcrossBothHandsNativeHeightAndHudLayouts() throws Exception {
        var texture = texture();
        for (float age = 3.5F; age <= 12.5F; age += .25F) for (boolean left : new boolean[] {false, true})
            for (float pitch : new float[] {0, -3, -12}) for (int[] layout : LAYOUTS)
                for (boolean residual : new boolean[] {false, true}) {
                    var pose = MastersArtAnimation.sample(9, age, 6, 12);
                    float height = residual ? nativeHeight(age) : 0;
                    var camera = hand(pose, left, 0, pitch, height)
                        .mul(MastersViewMotion.fadeSwing(swing(residual ? Math.min(1, age / 6) : 0, left), pose.weight())).mul(item(left));
                    var m = metrics(texture, camera, layout);
                    String label = "age=" + age + " left=" + left + " pitch=" + pitch + " residual=" + residual + " width=" + layout[0];
                    assertReadable(m, layout, label);
                    assertFalse(m.aimOverlap, "Accepted-facing blade clears the aim corridor: " + label);
                }
    }

    @Test
    void changedCutClearsTheNearPlaneAndAimCorridorOverTheEntireBoundedLookDomain() throws Exception {
        var texture = texture();
        // Moving blades can become edge-on during free look. This sweep guarantees camera/aim
        // clearance, not an all-look broad-face claim. Unchanged chamber/follow endpoints are checked too.
        for (float age = 3.75F; age <= 9; age += .25F) for (boolean left : new boolean[] {false, true})
            for (boolean residual : new boolean[] {false, true}) for (float yaw = -55; yaw <= 55; yaw += 5)
                for (float pitch = -40; pitch <= 40; pitch += 5) {
                    var pose = MastersArtAnimation.sample(9, age, 6, 12);
                    float height = residual ? nativeHeight(age) : 0;
                    var camera = hand(pose, left, yaw, pitch, height)
                        .mul(MastersViewMotion.fadeSwing(swing(residual ? Math.min(1, age / 6) : 0, left), pose.weight())).mul(item(left));
                    var m = metrics(texture, camera, LAYOUTS[1]);
                    String label = "age=" + age + " left=" + left + " yaw=" + yaw + " pitch=" + pitch + " residual=" + residual;
                    assertTrue(m.near > NEAR, "Opaque blade crosses camera: " + label);
                    assertFalse(m.aimOverlap, "Opaque face or side wall enters aim corridor: " + label);
                }
    }

    @Test
    void onlyReviewedWristComponentsChangeAndTheGripAndTimelineStayContinuous() {
        assertEquals(new MastersArtAnimation.Hand(.08F, -.05F, -.20F, -5, 18, -10), MastersArtAnimation.sample(9, 3.9F, 6, 12).hand());
        assertEquals(new MastersArtAnimation.Hand(-.20F, -.08F, -.35F, -18, -5, -10), MastersArtAnimation.sample(9, 6, 6, 12).hand());
        assertEquals(new MastersArtAnimation.Hand(-.02F, -.16F, -.10F, -2, -3, 8), MastersArtAnimation.sample(9, 9, 6, 12).hand());
        for (float age = 0; age < 18; age += .125F) {
            var pose = MastersArtAnimation.sample(9, age, 6, 12); var old = originalHand(age); var now = pose.hand();
            assertEquals(old.x(), now.x()); assertEquals(old.y(), now.y()); assertEquals(old.z(), now.z()); assertEquals(old.pitch(), now.pitch());
            float cut = 6 * .65F;
            float t = age < 6 ? MastersArtAnimation.smooth((age - cut) / (6 - cut)) : MastersArtAnimation.smooth((age - 6) / 3);
            assertEquals(age < cut ? 18 : age < 6 ? 18 + (-5 - 18) * t : age < 9 ? -5 + (-3 - (-5)) * t : -3, now.yaw());
            assertEquals(age < 6 ? -10 : age < 9 ? -10 + (8 - (-10)) * t : 8, now.roll());
            for (boolean left : new boolean[] {false, true})
                assertEquals(MastersArtAnimation.view(withHand(pose, old), left, nativeHeight(age), -90, -75).grip(),
                    MastersArtAnimation.view(pose, left, nativeHeight(age), -90, -75).grip());
        }
        for (float at : new float[] {0, 3.9F, 6, 9, 18}) for (boolean left : new boolean[] {false, true}) {
            var a = hand(MastersArtAnimation.sample(9, Math.max(0, at - .001F), 6, 12), left, -90, -75, 0).mul(item(left));
            var b = hand(MastersArtAnimation.sample(9, at + .001F, 6, 12), left, -90, -75, 0).mul(item(left));
            for (float x : new float[] {0, 1}) for (float y : new float[] {0, 1})
                assertTrue(a.transformPosition(new Vector3f(x, y, .5F)).distance(b.transformPosition(new Vector3f(x, y, .5F))) < .002F,
                    "No camera-space jump across age " + at);
        }
    }

    private static float nativeHeight(float age) {
        float previous = Math.min(1, ((int) age + 2) / 12.5F), current = Math.min(1, ((int) age + 3) / 12.5F);
        return 1 - (previous * previous * previous + current * current * current) / 2;
    }
    private static MastersArtAnimation.Hand originalHand(float age) {
        var chamber = new MastersArtAnimation.Hand(.08F, -.05F, -.20F, -5, 18, -10);
        var impact = new MastersArtAnimation.Hand(-.20F, -.08F, -.35F, -18, -25, 14);
        var follow = new MastersArtAnimation.Hand(-.02F, -.16F, -.10F, -2, -3, 8);
        float cut = 6 * .65F;
        var a = age < 6 ? chamber : impact; var b = age < 6 ? impact : follow;
        float t = age < 6 ? MastersArtAnimation.smooth((age - cut) / (6 - cut)) : MastersArtAnimation.smooth((age - 6) / 3);
        if (age < cut) return chamber; if (age >= 9) return follow;
        return new MastersArtAnimation.Hand(a.x() + (b.x() - a.x()) * t, a.y() + (b.y() - a.y()) * t,
            a.z() + (b.z() - a.z()) * t, a.pitch() + (b.pitch() - a.pitch()) * t,
            a.yaw() + (b.yaw() - a.yaw()) * t, a.roll() + (b.roll() - a.roll()) * t);
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
		try (var in = Objects.requireNonNull(MastersVoidCutCompositionTest.class.getResourceAsStream("/assets/minecraft/textures/item/diamond_sword.png"))) { return ImageIO.read(in); }
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
