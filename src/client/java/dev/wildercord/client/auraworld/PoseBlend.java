package dev.wildercord.client.auraworld;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Blends a boss model's posed parts out of the move before into the one now showing, so one blow flows into the next. It
 * keeps one buffer per model instead of a new pose for every part every frame.
 */
final class PoseBlend {
	private static final int FIELDS = 9;
	private final ModelPart[] parts;
	private float[] saved = new float[0];

	PoseBlend(ModelPart[] parts) {
		this.parts = parts;
	}

	/** Remembers where every part is now: the pose the blend ends at. */
	void store() {
		if (saved.length != parts.length * FIELDS) saved = new float[parts.length * FIELDS];
		for (int i = 0, j = 0; i < parts.length; i++, j += FIELDS) {
			ModelPart p = parts[i];
			saved[j] = p.x; saved[j + 1] = p.y; saved[j + 2] = p.z;
			saved[j + 3] = p.xRot; saved[j + 4] = p.yRot; saved[j + 5] = p.zRot;
			saved[j + 6] = p.xScale; saved[j + 7] = p.yScale; saved[j + 8] = p.zScale;
		}
	}

	/** Moves every part from where it is now (the pose before) toward the stored one, {@code k} of the way. */
	void mix(float k) {
		for (int i = 0, j = 0; i < parts.length; i++, j += FIELDS) {
			ModelPart p = parts[i];
			p.x = Mth.lerp(k, p.x, saved[j]); p.y = Mth.lerp(k, p.y, saved[j + 1]); p.z = Mth.lerp(k, p.z, saved[j + 2]);
			p.xRot = Mth.lerp(k, p.xRot, saved[j + 3]); p.yRot = Mth.lerp(k, p.yRot, saved[j + 4]); p.zRot = Mth.lerp(k, p.zRot, saved[j + 5]);
			p.xScale = Mth.lerp(k, p.xScale, saved[j + 6]); p.yScale = Mth.lerp(k, p.yScale, saved[j + 7]); p.zScale = Mth.lerp(k, p.zScale, saved[j + 8]);
		}
	}
}
