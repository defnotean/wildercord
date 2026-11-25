package dev.wildercord.client.trials;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildercord.Wildercord;
import dev.wildercord.client.compat.RenderCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads Bedrock .geo.json geometry (bones, pivots, rotated/inflated cubes, 6-face UVs)
 * and .animation.json keyframe clips, sampling smooth ZYX bone transforms with zero per-frame allocations.
 */
public final class BedrockGeoRig {
	private static final String[] FACE_ORDER = {"north", "south", "east", "west", "up", "down"};

	private static final class BakedQuad {
		final float x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3;
		final float u0, v0, u1, v1;
		final float nx, ny, nz;

		BakedQuad(Vector3f[] v, float u0, float v0, float u1, float v1, Vector3f n) {
			this.x0 = v[0].x; this.y0 = v[0].y; this.z0 = v[0].z;
			this.x1 = v[1].x; this.y1 = v[1].y; this.z1 = v[1].z;
			this.x2 = v[2].x; this.y2 = v[2].y; this.z2 = v[2].z;
			this.x3 = v[3].x; this.y3 = v[3].y; this.z3 = v[3].z;
			this.u0 = u0; this.v0 = v0; this.u1 = u1; this.v1 = v1;
			this.nx = n.x; this.ny = n.y; this.nz = n.z;
		}
	}

	private static final class BoneNode {
		final int index;
		final String name;
		final float relPivotX, relPivotY, relPivotZ;
		final BakedQuad[] quads;
		final List<BoneNode> children = new ArrayList<>();

		BoneNode(int index, String name, float relPivotX, float relPivotY, float relPivotZ, BakedQuad[] quads) {
			this.index = index;
			this.name = name;
			this.relPivotX = relPivotX;
			this.relPivotY = relPivotY;
			this.relPivotZ = relPivotZ;
			this.quads = quads;
		}
	}

	private record Track(float[] times, float[] values) {
		void sample(float t, float[] out, int outOffset) {
			if (times.length == 0) {
				out[outOffset] = 0.0F;
				out[outOffset + 1] = 0.0F;
				out[outOffset + 2] = 0.0F;
				return;
			}
			if (times.length == 1 || t <= times[0]) {
				out[outOffset] = values[0];
				out[outOffset + 1] = values[1];
				out[outOffset + 2] = values[2];
				return;
			}
			int last = times.length - 1;
			if (t >= times[last]) {
				int base = last * 3;
				out[outOffset] = values[base];
				out[outOffset + 1] = values[base + 1];
				out[outOffset + 2] = values[base + 2];
				return;
			}
			int lo = 0;
			int hi = last;
			while (lo + 1 < hi) {
				int mid = (lo + hi) >>> 1;
				if (times[mid] <= t) lo = mid;
				else hi = mid;
			}
			float span = times[hi] - times[lo];
			float alpha = span > 1.0e-6F ? (t - times[lo]) / span : 0.0F;
			int i0 = lo * 3;
			int i1 = hi * 3;
			out[outOffset] = Mth.lerp(alpha, values[i0], values[i1]);
			out[outOffset + 1] = Mth.lerp(alpha, values[i0 + 1], values[i1 + 1]);
			out[outOffset + 2] = Mth.lerp(alpha, values[i0 + 2], values[i1 + 2]);
		}
	}

	private record Clip(float length, boolean loop, Track[] rotTracks, Track[] posTracks) {
		void sample(float timeSec, float[] rotOut, float[] posOut, float weight) {
			float t = loop && length > 0.0F ? (timeSec % length + length) % length : Mth.clamp(timeSec, 0.0F, length);
			float[] tmp = new float[3];
			for (int i = 0; i < rotTracks.length; i++) {
				int o = i * 3;
				if (rotTracks[i] != null) {
					rotTracks[i].sample(t, tmp, 0);
					rotOut[o] += tmp[0] * weight;
					rotOut[o + 1] += tmp[1] * weight;
					rotOut[o + 2] += tmp[2] * weight;
				}
				if (posTracks[i] != null) {
					posTracks[i].sample(t, tmp, 0);
					posOut[o] += tmp[0] * weight;
					posOut[o + 1] += tmp[1] * weight;
					posOut[o + 2] += tmp[2] * weight;
				}
			}
		}
	}

	private final List<BoneNode> roots;
	private final int boneCount;
	private final Map<String, Clip> clips;
	private final float[] rotBuffer;
	private final float[] posBuffer;
	private final Quaternionf tmpQuat = new Quaternionf();

	private BedrockGeoRig(List<BoneNode> roots, int boneCount, Map<String, Clip> clips) {
		this.roots = roots;
		this.boneCount = boneCount;
		this.clips = clips;
		this.rotBuffer = new float[boneCount * 3];
		this.posBuffer = new float[boneCount * 3];
	}

	public static BedrockGeoRig load(Identifier geoId, Identifier animId) {
		JsonObject geoRoot = readJson(geoId);
		JsonObject geom = geoRoot.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
		JsonObject desc = geom.getAsJsonObject("description");
		float texW = desc.get("texture_width").getAsFloat();
		float texH = desc.get("texture_height").getAsFloat();

		Map<String, float[]> rawPivots = new LinkedHashMap<>();
		Map<String, String> parents = new LinkedHashMap<>();
		Map<String, BakedQuad[]> boneQuads = new LinkedHashMap<>();
		List<String> order = new ArrayList<>();

		for (JsonElement el : geom.getAsJsonArray("bones")) {
			JsonObject b = el.getAsJsonObject();
			String name = b.get("name").getAsString();
			order.add(name);
			if (b.has("parent") && !b.get("parent").isJsonNull()) {
				parents.put(name, b.get("parent").getAsString());
			}
			float[] piv = vec3(b.get("pivot"));
			rawPivots.put(name, piv);
			List<BakedQuad> quads = new ArrayList<>();
			if (b.has("cubes")) {
				for (JsonElement ce : b.getAsJsonArray("cubes")) {
					bakeCube(ce.getAsJsonObject(), piv, texW, texH, quads);
				}
			}
			boneQuads.put(name, quads.toArray(BakedQuad[]::new));
		}

		Map<String, Integer> indices = new HashMap<>();
		Map<String, BoneNode> nodes = new LinkedHashMap<>();
		List<BoneNode> roots = new ArrayList<>();
		for (int i = 0; i < order.size(); i++) {
			String name = order.get(i);
			indices.put(name, i);
			float[] piv = rawPivots.get(name);
			String parent = parents.get(name);
			float[] pPiv = parent != null ? rawPivots.getOrDefault(parent, new float[3]) : new float[3];
			BoneNode node = new BoneNode(i, name, piv[0] - pPiv[0], piv[1] - pPiv[1], piv[2] - pPiv[2], boneQuads.get(name));
			nodes.put(name, node);
		}
		for (String name : order) {
			BoneNode node = nodes.get(name);
			String parent = parents.get(name);
			if (parent != null && nodes.containsKey(parent)) {
				nodes.get(parent).children.add(node);
			} else {
				roots.add(node);
			}
		}

		JsonObject animRoot = readJson(animId).getAsJsonObject("animations");
		Map<String, Clip> clips = new HashMap<>();
		for (var entry : animRoot.entrySet()) {
			JsonObject aObj = entry.getValue().getAsJsonObject();
			float length = aObj.has("animation_length") ? aObj.get("animation_length").getAsFloat() : 1.0F;
			boolean loop = aObj.has("loop") && aObj.get("loop").isJsonPrimitive() && aObj.get("loop").getAsJsonPrimitive().isBoolean()
				&& aObj.get("loop").getAsBoolean();
			Track[] rotTracks = new Track[order.size()];
			Track[] posTracks = new Track[order.size()];
			if (aObj.has("bones")) {
				for (var be : aObj.getAsJsonObject("bones").entrySet()) {
					Integer idx = indices.get(be.getKey());
					if (idx == null) continue;
					JsonObject bObj = be.getValue().getAsJsonObject();
					if (bObj.has("rotation")) rotTracks[idx] = parseTrack(bObj.getAsJsonObject("rotation"));
					if (bObj.has("position")) posTracks[idx] = parseTrack(bObj.getAsJsonObject("position"));
				}
			}
			clips.put(entry.getKey(), new Clip(length, loop, rotTracks, posTracks));
		}
		return new BedrockGeoRig(roots, order.size(), clips);
	}

	private static Track parseTrack(JsonObject obj) {
		List<Map.Entry<Float, float[]>> keys = new ArrayList<>();
		for (var e : obj.entrySet()) {
			float t = Float.parseFloat(e.getKey());
			float[] v = vec3(e.getValue());
			keys.add(Map.entry(t, v));
		}
		keys.sort(Map.Entry.comparingByKey());
		float[] times = new float[keys.size()];
		float[] values = new float[keys.size() * 3];
		for (int i = 0; i < keys.size(); i++) {
			times[i] = keys.get(i).getKey();
			float[] v = keys.get(i).getValue();
			values[i * 3] = v[0];
			values[i * 3 + 1] = v[1];
			values[i * 3 + 2] = v[2];
		}
		return new Track(times, values);
	}

	private static void bakeCube(JsonObject c, float[] bonePivot, float texW, float texH, List<BakedQuad> out) {
		float[] origin = vec3(c.get("origin"));
		float[] size = vec3(c.get("size"));
		float inf = c.has("inflate") ? c.get("inflate").getAsFloat() : 0.0F;
		float x0 = origin[0] - inf;
		float y0 = origin[1] - inf;
		float z0 = origin[2] - inf;
		float x1 = origin[0] + size[0] + inf;
		float y1 = origin[1] + size[1] + inf;
		float z1 = origin[2] + size[2] + inf;

		Matrix3f cubeRot = null;
		float[] cPivot = null;
		if (c.has("rotation")) {
			float[] rot = vec3(c.get("rotation"));
			cPivot = c.has("pivot") ? vec3(c.get("pivot")) : new float[]{(x0 + x1) * 0.5F, (y0 + y1) * 0.5F, (z0 + z1) * 0.5F};
			Quaternionf q = new Quaternionf().rotationZYX(
				rot[2] * Mth.DEG_TO_RAD, rot[1] * Mth.DEG_TO_RAD, rot[0] * Mth.DEG_TO_RAD);
			cubeRot = new Matrix3f().rotation(q);
		}

		JsonObject uvObj = c.has("uv") && c.get("uv").isJsonObject() ? c.getAsJsonObject("uv") : null;
		if (uvObj == null) return;

		for (String face : FACE_ORDER) {
			if (!uvObj.has(face)) continue;
			JsonObject fObj = uvObj.getAsJsonObject(face);
			float[] uv = vec2(fObj.get("uv"));
			float[] uvSize = vec2(fObj.get("uv_size"));
			float u0 = uv[0] / texW;
			float v0 = uv[1] / texH;
			float u1 = (uv[0] + uvSize[0]) / texW;
			float v1 = (uv[1] + uvSize[1]) / texH;

			Vector3f[] verts;
			Vector3f normal;
			switch (face) {
				case "north" -> {
					verts = new Vector3f[]{v(x1, y1, z0), v(x1, y0, z0), v(x0, y0, z0), v(x0, y1, z0)};
					normal = new Vector3f(0, 0, -1);
				}
				case "south" -> {
					verts = new Vector3f[]{v(x0, y1, z1), v(x0, y0, z1), v(x1, y0, z1), v(x1, y1, z1)};
					normal = new Vector3f(0, 0, 1);
				}
				case "east" -> {
					verts = new Vector3f[]{v(x1, y1, z1), v(x1, y0, z1), v(x1, y0, z0), v(x1, y1, z0)};
					normal = new Vector3f(1, 0, 0);
				}
				case "west" -> {
					verts = new Vector3f[]{v(x0, y1, z0), v(x0, y0, z0), v(x0, y0, z1), v(x0, y1, z1)};
					normal = new Vector3f(-1, 0, 0);
				}
				case "up" -> {
					verts = new Vector3f[]{v(x0, y1, z0), v(x0, y1, z1), v(x1, y1, z1), v(x1, y1, z0)};
					normal = new Vector3f(0, 1, 0);
				}
				default -> { // down
					verts = new Vector3f[]{v(x0, y0, z1), v(x0, y0, z0), v(x1, y0, z0), v(x1, y0, z1)};
					normal = new Vector3f(0, -1, 0);
				}
			}

			if (cubeRot != null) {
				for (Vector3f vert : verts) {
					vert.sub(cPivot[0], cPivot[1], cPivot[2]);
					cubeRot.transform(vert);
					vert.add(cPivot[0], cPivot[1], cPivot[2]);
				}
				cubeRot.transform(normal);
			}
			for (Vector3f vert : verts) {
				vert.sub(bonePivot[0], bonePivot[1], bonePivot[2]);
			}
			out.add(new BakedQuad(verts, u0, v0, u1, v1, normal));
		}
	}

	private static Vector3f v(float x, float y, float z) {
		return new Vector3f(x, y, z);
	}

	private static float[] vec2(JsonElement el) {
		var arr = el.getAsJsonArray();
		return new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat()};
	}

	private static float[] vec3(JsonElement el) {
		var arr = el.getAsJsonArray();
		return new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat()};
	}

	private static JsonObject readJson(Identifier id) {
		try {
			var resOpt = Minecraft.getInstance().getResourceManager().getResource(id);
			if (resOpt.isPresent()) {
				try (InputStream in = resOpt.get().open();
					 var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
					return JsonParser.parseReader(reader).getAsJsonObject();
				}
			}
			String cp = "/assets/" + id.getNamespace() + "/" + id.getPath();
			try (InputStream in = BedrockGeoRig.class.getResourceAsStream(cp)) {
				if (in == null) {
					throw new IllegalStateException("Missing Bedrock asset: " + id);
				}
				try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
					return JsonParser.parseReader(reader).getAsJsonObject();
				}
			}
		} catch (Exception e) {
			Wildercord.LOGGER.error("Failed loading Bedrock asset {}", id, e);
			throw new IllegalStateException("Failed loading Bedrock asset " + id, e);
		}
	}

	public synchronized void poseClip(String baseClip, float baseTimeSec, String actionClip, float actionTimeSec, float actionWeight) {
		java.util.Arrays.fill(rotBuffer, 0.0F);
		java.util.Arrays.fill(posBuffer, 0.0F);
		Clip base = clips.get(baseClip);
		Clip action = actionClip != null ? clips.get(actionClip) : null;
		float wAct = action != null ? Mth.clamp(actionWeight, 0.0F, 1.0F) : 0.0F;
		float wBase = 1.0F - wAct;
		if (base != null && wBase > 0.001F) {
			base.sample(baseTimeSec, rotBuffer, posBuffer, wBase);
		}
		if (action != null && wAct > 0.001F) {
			action.sample(actionTimeSec, rotBuffer, posBuffer, wAct);
		}
	}

	public synchronized void poseAndSubmit(
		String baseClip, float baseTimeSec, String actionClip, float actionTimeSec, float actionWeight,
		PoseStack stack, VertexConsumer consumer, int light, int overlay, int color
	) {
		poseClip(baseClip, baseTimeSec, actionClip, actionTimeSec, actionWeight);
		submit(stack, consumer, light, overlay, color);
	}

	public synchronized void submit(PoseStack stack, VertexConsumer consumer, int light, int overlay, int color) {
		for (BoneNode root : roots) {
			drawBone(root, stack, consumer, light, overlay, color);
		}
	}

	private void drawBone(BoneNode node, PoseStack stack, VertexConsumer consumer, int light, int overlay, int color) {
		int off = node.index * 3;
		stack.pushPose();
		stack.translate(node.relPivotX + posBuffer[off], node.relPivotY + posBuffer[off + 1], node.relPivotZ + posBuffer[off + 2]);
		float rx = rotBuffer[off];
		float ry = rotBuffer[off + 1];
		float rz = rotBuffer[off + 2];
		if (rx != 0.0F || ry != 0.0F || rz != 0.0F) {
			RenderCompat.rotate(stack, tmpQuat.rotationZYX(rz * Mth.DEG_TO_RAD, ry * Mth.DEG_TO_RAD, rx * Mth.DEG_TO_RAD));
		}
		PoseStack.Pose pose = stack.last();
		for (BakedQuad q : node.quads) {
			consumer.addVertex(pose, q.x0, q.y0, q.z0).setColor(color).setUv(q.u0, q.v0).setOverlay(overlay).setLight(light).setNormal(pose, q.nx, q.ny, q.nz);
			consumer.addVertex(pose, q.x1, q.y1, q.z1).setColor(color).setUv(q.u0, q.v1).setOverlay(overlay).setLight(light).setNormal(pose, q.nx, q.ny, q.nz);
			consumer.addVertex(pose, q.x2, q.y2, q.z2).setColor(color).setUv(q.u1, q.v1).setOverlay(overlay).setLight(light).setNormal(pose, q.nx, q.ny, q.nz);
			consumer.addVertex(pose, q.x3, q.y3, q.z3).setColor(color).setUv(q.u1, q.v0).setOverlay(overlay).setLight(light).setNormal(pose, q.nx, q.ny, q.nz);
		}
		for (BoneNode child : node.children) {
			drawBone(child, stack, consumer, light, overlay, color);
		}
		stack.popPose();
	}
}
