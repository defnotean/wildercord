package dev.wildercord.client.fx;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * How a flat piece of light turns to face the camera, for the particles drawn from many small quads
 * (ribbons of ley light, beams, comets, glows). They lay down thousands of pieces a frame, and each
 * piece used to build its own vectors, matrix and quaternion to work its turn out; here the working
 * is done in a few objects kept for it, with exactly the same arithmetic. Render thread only.
 */
final class Facing {
	private Facing() {}

	private static final Vector3f X = new Vector3f();
	private static final Vector3f Y = new Vector3f();
	private static final Vector3f Z = new Vector3f();
	private static final Vector3f T = new Vector3f();
	private static final Matrix3f M = new Matrix3f();

	/**
	 * Into {@code out}: the turn of a piece at ({@code mx}, {@code my}, {@code mz}) (camera-relative)
	 * lying along the unit direction {@code d}, facing the camera as squarely as that direction allows.
	 */
	static Quaternionf along(Vector3fc d, float mx, float my, float mz, Quaternionf out) {
		Vector3f z = Z.set(-mx, -my, -mz);
		z.sub(T.set(d).mul(z.dot(d)));
		if (z.lengthSquared() < 1.0E-6F) {
			z.set(0, 1, 0);
		}
		z.normalize();
		Vector3f y = Y.set(z).cross(d).normalize();
		return out.setFromNormalized(M.set(d, y, z));
	}

	/**
	 * Turns {@code q} half round its own y axis, for the back of a piece drawn from both sides: to the
	 * last bit what {@code q.rotateY(Mth.PI)} gives (a sine of exactly 1 and a cosine of 0), without
	 * working out the sine and cosine for every piece of every circle.
	 */
	static Quaternionf flip(Quaternionf q) {
		return q.set(-q.z, q.w, q.x, -q.y);
	}

	/**
	 * Into {@code out}: the turn of a quad at ({@code px}, {@code py}, {@code pz}) (camera-relative, and
	 * not right at the camera) facing the camera squarely.
	 */
	static Quaternionf toward(float px, float py, float pz, Quaternionf out) {
		Vector3f z = Z.set(-px, -py, -pz).normalize();
		Vector3f x = (Math.abs(z.y) > 0.95F ? X.set(1, 0, 0) : X.set(0, 1, 0)).cross(z).normalize();
		Vector3f y = Y.set(z).cross(x).normalize();
		return out.setFromNormalized(M.set(x, y, z));
	}
}
