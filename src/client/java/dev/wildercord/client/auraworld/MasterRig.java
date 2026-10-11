package dev.wildercord.client.auraworld;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildercord.Wildercord;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The Sword Master's Blockbench rig ({@code art/blockbench/sword_master_v2.bbmodel}), baked to
 * {@code rigs/sword_master.json}: a bone tree with its textured quads in each bone's own space, and every clip sampled at
 * 20 frames a second as each bone's local translation, rotation and scale. Units are model pixels, y up, forward -z, feet
 * at 0, with Blockbench's x (the sword arm at -5), so a renderer mirrors x to put the blade in the right hand.
 */
public final class MasterRig {
	/** Floats per quad: four corners of x, y, z, u, v, then the face normal. */
	static final int QUAD = 23;
	private static final String PATH = "/assets/wildercord/rigs/sword_master.json";
	private static MasterRig loaded;
	private static boolean tried;

	public final Bone[] bones;
	public final Map<String, Clip> clips;
	public final int sword, sheathHilt, head, torso;
	final float fps;

	public record Bone(String name, int parent, float[] pos, float[] rot, float[] quads) {}

	/** Per bone (null where the clip leaves it at rest) a flat run of frames of each channel it moves. */
	public record Track(float @Nullable [] p, float @Nullable [] q, float @Nullable [] s) {}

	/**
	 * A clip; {@code markers} (attacks only) are where its strike starts, where it lands, where the follow-through's hold ends,
	 * and its end, in seconds, so a server's tell, hit and recovery can be stretched onto them.
	 */
	public record Clip(String name, float length, boolean loop, int frames, float @Nullable [] markers, Track[] tracks) {
		public float strike() { return markers == null ? 0 : markers[0]; }
		public float impact() { return markers == null ? 0 : markers[1]; }
		public float hold() { return markers == null ? length : markers[2]; }
	}

	/** One frame of the whole rig: each bone's local translation, rotation (quaternion x, y, z, w) and scale. */
	public static final class Pose {
		public final float[] p, q, s;

		Pose(int bones) {
			p = new float[bones * 3];
			q = new float[bones * 4];
			s = new float[bones * 3];
		}

		public Pose copy() {
			Pose out = new Pose(p.length / 3);
			System.arraycopy(p, 0, out.p, 0, p.length);
			System.arraycopy(q, 0, out.q, 0, q.length);
			System.arraycopy(s, 0, out.s, 0, s.length);
			return out;
		}

		/** Turns a bone by {@code turn} about its parent's axes, after whatever the clip did. */
		public void preRotate(int bone, Quaternionf turn) {
			Quaternionf q0 = new Quaternionf(q[bone * 4], q[bone * 4 + 1], q[bone * 4 + 2], q[bone * 4 + 3]);
			Quaternionf out = new Quaternionf(turn).mul(q0);
			q[bone * 4] = out.x;
			q[bone * 4 + 1] = out.y;
			q[bone * 4 + 2] = out.z;
			q[bone * 4 + 3] = out.w;
		}

		public void scale(int bone, float scale) {
			s[bone * 3] = s[bone * 3 + 1] = s[bone * 3 + 2] = scale;
		}
	}

	private MasterRig(Bone[] bones, Map<String, Clip> clips, float fps) {
		this.bones = bones;
		this.clips = clips;
		this.fps = fps;
		this.sword = index("sword");
		this.sheathHilt = index("sheath_hilt");
		this.head = index("head");
		this.torso = index("torso");
	}

	/** The rig, loaded once; null (and the old model kept) if it's missing or broken. */
	public static @Nullable MasterRig get() {
		if (!tried) {
			tried = true;
			try (InputStream in = MasterRig.class.getResourceAsStream(PATH)) {
				if (in == null) throw new IllegalStateException("missing " + PATH);
				loaded = parse(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
			} catch (Exception e) {
				Wildercord.LOGGER.error("Sword Master rig failed to load; keeping the old model", e);
				loaded = null;
			}
		}
		return loaded;
	}

	static MasterRig parse(JsonObject doc) {
		JsonArray boneArray = doc.getAsJsonArray("bones");
		Bone[] bones = new Bone[boneArray.size()];
		Map<String, Integer> byName = new HashMap<>();
		for (int i = 0; i < bones.length; i++) {
			JsonObject b = boneArray.get(i).getAsJsonObject();
			JsonArray quads = b.getAsJsonArray("quads");
			float[] flat = new float[quads.size() * QUAD];
			for (int k = 0; k < quads.size(); k++) {
				JsonArray quad = quads.get(k).getAsJsonArray();
				if (quad.size() != QUAD) throw new IllegalStateException("quad of " + quad.size() + " floats");
				for (int j = 0; j < QUAD; j++) flat[k * QUAD + j] = quad.get(j).getAsFloat();
			}
			int parent = b.get("parent").getAsInt();
			if (parent >= i) throw new IllegalStateException("bone " + i + " before its parent");
			bones[i] = new Bone(b.get("name").getAsString(), parent, floats(b.getAsJsonArray("pos")), floats(b.getAsJsonArray("rot")), flat);
			byName.put(bones[i].name(), i);
		}
		Map<String, Clip> clips = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : doc.getAsJsonObject("clips").entrySet()) {
			JsonObject c = entry.getValue().getAsJsonObject();
			int frames = c.get("frames").getAsInt();
			Track[] tracks = new Track[bones.length];
			for (Map.Entry<String, JsonElement> t : c.getAsJsonObject("tracks").entrySet()) {
				Integer bone = byName.get(t.getKey());
				if (bone == null) continue;
				JsonObject track = t.getValue().getAsJsonObject();
				tracks[bone] = new Track(channel(track, "p", frames * 3), channel(track, "q", frames * 4), channel(track, "s", frames * 3));
			}
			float[] markers = c.has("markers") && c.get("markers").isJsonArray() ? floats(c.getAsJsonArray("markers")) : null;
			clips.put(entry.getKey(), new Clip(entry.getKey(), c.get("length").getAsFloat(), c.get("loop").getAsBoolean(), frames, markers, tracks));
		}
		return new MasterRig(bones, clips, doc.get("fps").getAsFloat());
	}

	private static float @Nullable [] channel(JsonObject track, String key, int size) {
		if (!track.has(key)) return null;
		float[] values = floats(track.getAsJsonArray(key));
		if (values.length != size) throw new IllegalStateException("channel " + key + " has " + values.length + " of " + size);
		return values;
	}

	private static float[] floats(JsonArray array) {
		float[] out = new float[array.size()];
		for (int i = 0; i < out.length; i++) out[i] = array.get(i).getAsFloat();
		return out;
	}

	public int index(String name) {
		for (int i = 0; i < bones.length; i++) if (bones[i].name().equals(name)) return i;
		throw new IllegalStateException("rig has no " + name);
	}

	public Clip clip(String name) {
		Clip clip = clips.get(name);
		if (clip == null) throw new IllegalStateException("rig has no clip " + name);
		return clip;
	}

	public Pose newPose() {
		return new Pose(bones.length);
	}

	/** {@code clip} at {@code seconds} (wrapped if it loops, held at its ends if not). */
	public Pose sample(Clip clip, float seconds, Pose out) {
		float t = clip.loop() ? ((seconds % clip.length()) + clip.length()) % clip.length() : Math.clamp(seconds, 0, clip.length());
		float f = Math.min(t * fps, clip.frames() - 1);
		int a = (int) Math.floor(f);
		int b = Math.min(a + 1, clip.frames() - 1);
		float w = f - a;
		for (int bone = 0; bone < bones.length; bone++) {
			Track track = clip.tracks()[bone];
			Bone rest = bones[bone];
			lerp(track == null ? null : track.p(), rest.pos(), 3, a, b, w, out.p, bone);
			if (track != null && track.q() != null) nlerp(track.q(), a * 4, b * 4, w, out.q, bone * 4);
			else System.arraycopy(rest.rot(), 0, out.q, bone * 4, 4);
			float[] one = {1, 1, 1};
			lerp(track == null ? null : track.s(), one, 3, a, b, w, out.s, bone);
		}
		return out;
	}

	private static void lerp(float @Nullable [] values, float[] rest, int n, int a, int b, float w, float[] out, int bone) {
		for (int j = 0; j < n; j++) {
			out[bone * n + j] = values == null ? rest[j] : values[a * n + j] + (values[b * n + j] - values[a * n + j]) * w;
		}
	}

	/** Normalised lerp the short way round: frames 1/20 s apart are close enough that it reads as a slerp. */
	private static void nlerp(float[] q, int a, int b, float w, float[] out, int o) {
		float dot = q[a] * q[b] + q[a + 1] * q[b + 1] + q[a + 2] * q[b + 2] + q[a + 3] * q[b + 3];
		float sign = dot < 0 ? -1 : 1;
		float len = 0;
		for (int j = 0; j < 4; j++) {
			out[o + j] = q[a + j] * (1 - w) + sign * q[b + j] * w;
			len += out[o + j] * out[o + j];
		}
		len = len < 1.0E-8F ? 1 : (float) (1 / Math.sqrt(len));
		for (int j = 0; j < 4; j++) out[o + j] *= len;
	}

	/** {@code from} toward {@code to} by {@code w} (0 all from, 1 all to), into {@code out} (which may be either). */
	public static Pose blend(Pose from, Pose to, float w, Pose out) {
		if (w <= 0) return copyInto(from, out);
		if (w >= 1) return copyInto(to, out);
		for (int i = 0; i < out.p.length; i++) out.p[i] = from.p[i] + (to.p[i] - from.p[i]) * w;
		for (int i = 0; i < out.s.length; i++) out.s[i] = from.s[i] + (to.s[i] - from.s[i]) * w;
		float[] pair = new float[8];
		for (int bone = 0; bone < out.q.length / 4; bone++) {
			System.arraycopy(from.q, bone * 4, pair, 0, 4);
			System.arraycopy(to.q, bone * 4, pair, 4, 4);
			nlerp(pair, 0, 4, w, out.q, bone * 4);
		}
		return out;
	}

	private static Pose copyInto(Pose from, Pose out) {
		if (from == out) return out;
		System.arraycopy(from.p, 0, out.p, 0, from.p.length);
		System.arraycopy(from.q, 0, out.q, 0, from.q.length);
		System.arraycopy(from.s, 0, out.s, 0, from.s.length);
		return out;
	}

	/** Each bone's placement in rig space. */
	public Matrix4f[] matrices(Pose pose) {
		Matrix4f[] out = new Matrix4f[bones.length];
		Quaternionf q = new Quaternionf();
		for (int i = 0; i < bones.length; i++) {
			Matrix4f m = bones[i].parent() < 0 ? new Matrix4f() : new Matrix4f(out[bones[i].parent()]);
			q.set(pose.q[i * 4], pose.q[i * 4 + 1], pose.q[i * 4 + 2], pose.q[i * 4 + 3]);
			m.translate(pose.p[i * 3], pose.p[i * 3 + 1], pose.p[i * 3 + 2]).rotate(q).scale(pose.s[i * 3], pose.s[i * 3 + 1], pose.s[i * 3 + 2]);
			out[i] = m;
		}
		return out;
	}

	/** Which bones a pass draws. */
	public interface Filter {
		boolean draws(int bone);
	}

	/**
	 * Emits every drawn bone's quads. {@code mirrored} reverses each quad's winding so a pose with a flipped axis still faces
	 * out under culled render types.
	 */
	public void emit(Matrix4f[] placed, Filter filter, boolean mirrored, PoseStack.Pose pose, VertexConsumer buffer, int argb, int overlay, int light) {
		Vector3f v = new Vector3f();
		Vector3f n = new Vector3f();
		Matrix3f normal = new Matrix3f();
		for (int bone = 0; bone < bones.length; bone++) {
			float[] quads = bones[bone].quads();
			if (quads.length == 0 || !filter.draws(bone)) continue;
			Matrix4f m = placed[bone];
			if (Math.abs(m.determinant3x3()) < 1.0E-6F) continue;
			m.normal(normal);
			for (int q = 0; q < quads.length; q += QUAD) {
				n.set(quads[q + 20], quads[q + 21], quads[q + 22]);
				normal.transform(n);
				if (n.lengthSquared() > 1.0E-8F) n.normalize();
				for (int c = 0; c < 4; c++) {
					int k = q + 5 * (mirrored ? 3 - c : c);
					m.transformPosition(quads[k], quads[k + 1], quads[k + 2], v);
					buffer.addVertex(pose, v.x, v.y, v.z).setColor(argb).setUv(quads[k + 3], quads[k + 4]).setOverlay(overlay).setLight(light)
						.setNormal(pose, n.x, n.y, n.z);
				}
			}
		}
	}
}
