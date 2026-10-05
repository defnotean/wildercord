package dev.wildercord.aura;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;

/** Minecraft-independent ownership of vanilla first-person swing and equip motion. */
public final class MastersViewMotion {
	private MastersViewMotion() {}

	/** An ineligible or nonfinite pose never takes ownership of vanilla motion. */
	public static float ownership(float poseWeight, boolean eligible) {
		return eligible && Float.isFinite(poseWeight) ? Math.max(0, Math.min(1, poseWeight)) : 0;
	}

	/** Cancels only the owned share of vanilla's downward equip-height translation. */
	public static float heightCompensation(float inverseHeight, float ownership) {
		return Float.isFinite(inverseHeight) ? .6F * inverseHeight * ownership(ownership, true) : 0;
	}

	/**
	 * Fades a rigid swing delta toward identity without resampling its attack phase. Translation
	 * and shortest-path rotation lose amplitude together; the caller's delta is never modified.
	 * Zero ownership preserves the exact matrix, and full ownership returns exact identity.
	 * A nonfinite intermediate delta cannot contribute usable motion and returns identity.
	 */
	public static Matrix4f fadeSwing(Matrix4fc delta, float ownership) {
		float weight = ownership(ownership, true);
		if (weight == 0) return new Matrix4f(delta);
		if (weight == 1 || !delta.isFinite()) return new Matrix4f();
		float remaining = 1 - weight;
		Quaternionf rotation = delta.getNormalizedRotation(new Quaternionf());
		if (!rotation.isFinite() || rotation.lengthSquared() == 0) return new Matrix4f();
		rotation.normalize();
		Quaternionf faded = new Quaternionf().slerp(rotation, remaining);
		return new Matrix4f().translation(delta.m30() * remaining, delta.m31() * remaining,
			delta.m32() * remaining).rotate(faded);
	}

	/**
	 * Remembers real equip motion from its cause through both lowering and raising. A height
	 * dip on its own may be attack cooldown, so it must not start a new equip transition.
	 */
	public static final class EquipTransition {
		private boolean active = true;

		/** Starts protection after item use or a change of owning player. */
		public void begin() {
			active = true;
		}

		/** Both interpolation endpoints must be settled before genuine equip motion is released. */
		public void tick(boolean actualSwap, boolean handsBusy, float previousHeight, float height) {
			if (actualSwap || handsBusy) active = true;
			else if (active && Float.isFinite(previousHeight) && Float.isFinite(height)
					&& previousHeight >= .999F && height >= .999F) active = false;
		}

		public boolean active() {
			return active;
		}
	}
}
