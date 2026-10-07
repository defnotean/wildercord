package dev.wildercord.gametest;

import dev.wildercord.aura.MastersArtAnimation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Independent audit math for Classic adapter output. Inputs are an observed pre-art native
 * baseline and immutable source key; this never calls the production body/view adapters,
 * pivot/head/view helpers, ownership helper or swing-fading helper.
 */
public final class BraceNullTransformOracle {
	private static final float EPS = .00015F;
	private BraceNullTransformOracle() {}

	/** Six sibling parts, in body/head/right-arm/left-arm/right-leg/left-leg order; xyz then Euler xyz. */
	public static float[] body(MastersArtAnimation.Pose pose, boolean left, float[] baseline, float[] bind) {
		finite(baseline, 36); finite(bind, 36);
		float[] result = baseline.clone(); float w = pose.weight(), side = left ? -1 : 1;
		joint(result, 0, pose.body(), side, w);
		joint(result, left ? 18 : 12, pose.sword(), side, w);
		joint(result, left ? 12 : 18, pose.guard(), side, w);
		joint(result, left ? 24 : 30, pose.frontLeg(), side, w);
		joint(result, left ? 30 : 24, pose.rearLeg(), side, w);
		float hx = baseline[9] + pose.head().x() * w, hy = baseline[10] + pose.head().y() * side * w,
			hz = baseline[11] + pose.head().z() * side * w, radians = (float) (Math.PI / 180);
		float yaw = (hy - result[4]) / radians % 360;
		if (yaw >= 180) yaw -= 360;
		if (yaw < -180) yaw += 360;
		result[9] = mix(hx, result[3] + clamp(hx - result[3], -45 * radians, 60 * radians), w);
		result[10] = mix(hy, result[4] + clamp(yaw * radians, -75 * radians, 75 * radians), w);
		result[11] = mix(hz, result[5] + clamp(hz - result[5], -25 * radians, 25 * radians), w);
		// Matrix composition independently expresses the hip hinge, rather than calling pivot().
		var hinge = new Matrix4f().translation(0, 12 + pose.lower(), pose.forward())
			.rotateZ(pose.body().z() * side).rotateY(pose.body().y() * side).rotateX(pose.body().x()).translate(0, -12, 0);
		for (int offset : new int[] {0, 6, 12, 18}) {
			var target = hinge.transformPosition(new Vector3f(bind[offset], bind[offset + 1], bind[offset + 2]));
			result[offset] = mix(baseline[offset], target.x, w);
			result[offset + 1] = mix(baseline[offset + 1], target.y, w);
			result[offset + 2] = mix(baseline[offset + 2], target.z, w);
		}
		for (int offset : new int[] {24, 30}) {
			result[offset + 1] = mix(baseline[offset + 1], bind[offset + 1] + pose.lower(), w);
			result[offset + 2] = mix(baseline[offset + 2], bind[offset + 2] + pose.forward(), w);
		}
		return result;
	}

	/** Native main-hand item-submit matrix, before the item model's display transform. */
	public static float[] hand(MastersArtAnimation.Pose pose, boolean left, float yawDelta, float pitchDelta,
		float inverseHeight, float attack, boolean defaultMainSwing, float[] entry) {
		finite(entry, 16);
		if (!Float.isFinite(yawDelta) || !Float.isFinite(pitchDelta) || !Float.isFinite(inverseHeight)
			|| inverseHeight < 0 || inverseHeight > 1 || !Float.isFinite(attack) || attack < 0 || attack > 1)
			throw new AssertionError("Invalid native hand inputs");
		float w = pose.weight(), side = left ? -1 : 1;
		float yaw = clamp(yawDelta, -55, 55), pitch = clamp(pitchDelta, -40, 40);
		float clearance = .20F * Math.max(Math.abs(yaw) / 55, Math.abs(pitch) / 40);
		var h = pose.hand(); float gx = .56F * side, gy = -.52F - .6F * inverseHeight, gz = -.72F;
		var result = new Matrix4f().set(entry)
			.translate(0, .6F * inverseHeight * w, 0)
			.translate(h.x() * side * w, h.y() * w, (h.z() - clearance) * w)
			.translate(gx, gy, gz).rotateY(rad((h.yaw() * side - yaw) * w))
			.rotateX(rad((h.pitch() - pitch) * w)).rotateZ(rad(h.roll() * side * w))
			.translate(-gx, -gy, -gz).translate(gx, gy, gz);
		if (defaultMainSwing && w < 1) {
			// Official 26.3 swingArm/applyItemArmAttackTransform bytecode, including its sine table.
			float root = (float) Math.sqrt(attack), wave = nativeSin(root * (float) Math.PI), square = nativeSin(attack * attack * (float) Math.PI);
			var delta = new Matrix4f().translation(side * -.4F * wave, .2F * nativeSin(root * ((float) Math.PI * 2)), -.2F * nativeSin(attack * (float) Math.PI))
				.rotateY(rad(side * (45 + square * -20))).rotateZ(rad(side * wave * -20))
				.rotateX(rad(wave * -80)).rotateY(rad(side * -45));
			var rotation = delta.getNormalizedRotation(new Quaternionf()).normalize();
			var faded = new Quaternionf().slerp(rotation, 1 - w);
			result.translate(delta.m30() * (1 - w), delta.m31() * (1 - w), delta.m32() * (1 - w)).rotate(faded);
		}
		return result.get(new float[16]);
	}
	/** Adult vanilla PlayerModel/ItemInHandLayer hand transform plus the authored hilt hinge. */
	public static float[] worldItem(float[] entry, float[] body, boolean left, boolean slim, float tilt) {
		finite(entry, 16); finite(body, 36); if (!Float.isFinite(tilt)) throw new AssertionError("Invalid hilt angle");
		int a = left ? 18 : 12; float side = left ? -1 : 1;
		return new Matrix4f().set(entry).translate((body[a] + side * (slim ? .5F : 0)) / 16, body[a + 1] / 16, body[a + 2] / 16)
			.rotateZ(body[a + 5]).rotateY(body[a + 4]).rotateX(body[a + 3])
			.rotateX(rad(-90)).rotateY(rad(180)).translate(side / 16, 2F / 16, -10F / 16)
			.translate(0, -1.327F / 16, 1.439F / 16).rotateX(rad(tilt)).translate(0, 1.327F / 16, -1.439F / 16).get(new float[16]);
	}
	/** Official stock handheld display, independently composed; mirrored source fields cancel ry/rz. */
	public static float[] displayed(float[] entry, boolean first, boolean left) {
		finite(entry, 16); float side = left ? -1 : 1;
		return new Matrix4f().set(entry).translate(first ? side * 1.13F / 16 : 0, (first ? 3.2F : 4F) / 16, (first ? 1.13F : .5F) / 16)
			.rotateY(rad(-90)).rotateZ(rad(first ? 25 : 55)).scale(first ? .68F : .85F).translate(-.5F, -.5F, -.5F).get(new float[16]);
	}
	/** Diamond sword hilt center in the generated item model's original unit cube. */
	public static float[] displayedHilt(float[] matrix) {
		finite(matrix, 16); var p = new Matrix4f().set(matrix).transformPosition(new Vector3f(3.5F / 16, 3.5F / 16, 8F / 16));
		return new float[] {p.x, p.y, p.z};
	}
	public static void requireDisplayed(float[] wanted, float[] actual) {
		equal(wanted, actual, 16, "displayed item draw matrix"); equal(displayedHilt(wanted), displayedHilt(actual), 3, "actual displayed hilt point");
	}
	public static float[] grip(float[] matrix) { finite(matrix, 16); var p = new Matrix4f().set(matrix).transformPosition(new Vector3f()); return new float[] {p.x, p.y, p.z}; }
	public static void requireBody(float[] expected, float[] actual) { equal(expected, actual, 36, "renderer-consumed body palette"); }
	public static void requireHand(float[] expected, float[] actual) {
		equal(expected, actual, 16, "native item-submit matrix"); equal(grip(expected), grip(actual), 3, "native grip point");
	}
	private static void equal(float[] expected, float[] actual, int size, String label) {
		finite(expected, size); finite(actual, size);
		for (int i = 0; i < size; i++) if (Math.abs(expected[i] - actual[i]) > EPS)
			throw new AssertionError(label + " differs at component " + i + ": expected=" + expected[i] + " actual=" + actual[i]);
	}
	private static void finite(float[] values, int size) {
		if (values == null || values.length != size) throw new AssertionError("Missing complete native transform evidence");
		for (float value : values) if (!Float.isFinite(value)) throw new AssertionError("Nonfinite native transform evidence");
	}
	private static void joint(float[] parts, int at, MastersArtAnimation.Joint key, float side, float w) {
		parts[at + 3] = mix(parts[at + 3], key.x(), w);
		parts[at + 4] = mix(parts[at + 4], key.y() * side, w);
		parts[at + 5] = mix(parts[at + 5], key.z() * side, w);
	}
	private static float nativeSin(float value) { int index = (int) ((long) (value * 10430.378350470453D) & 65535L); return (float) Math.sin(index * Math.PI * 2 / 65536); }
	private static float rad(float degrees) { return degrees * ((float) Math.PI / 180); }
	private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
	private static float mix(float a, float b, float w) { return a + w * (b - a); }
}
