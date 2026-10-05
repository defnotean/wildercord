import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.ArticulatedArmorMesh;
import dev.wildercord.aura.ArticulatedArmorMesh.Region;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.MastersArtRules;
import dev.wildercord.aura.ArticulatedCombatPose.Joint;
import dev.wildercord.client.combat.ArticulatedArmorGeometry;
import dev.wildercord.client.combat.ArticulatedRig;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reproducible CPU geometry gate against pristine official Minecraft runtime bakes. Run through
 * tools/check_articulated_armor.py. This does not launch Minecraft, apply mixins, test a GPU,
 * simulate texture transparency, or assert native visual/material acceptance.
 */
public final class CheckArticulatedArmorGeometry {
	private record Face(Region region, ModelPart.Vertex[] vertices) {}
	private record Triangle(Vector3d a, Vector3d b, Vector3d c) {}
	private static final class Metrics {
		long skinSamples, outsideSkin, overlapSamples, outsideOuterShell, triangles, reversed;
		double minimumOrientation = 1, maximumExpansion, minimumSlotSeparation = Double.POSITIVE_INFINITY;
		int move; float age; boolean firstPerson;
		String worstOrientationAt = "none";
		final Map<String, Long> reversedByRegion = new java.util.TreeMap<>();
		void add(Metrics other) {
			skinSamples += other.skinSamples; outsideSkin += other.outsideSkin;
			overlapSamples += other.overlapSamples; outsideOuterShell += other.outsideOuterShell;
			triangles += other.triangles; reversed += other.reversed;
			if (other.minimumOrientation < minimumOrientation) worstOrientationAt = other.worstOrientationAt;
			minimumOrientation = Math.min(minimumOrientation, other.minimumOrientation);
			other.reversedByRegion.forEach((key, value) -> reversedByRegion.merge(key, value, Long::sum));
			maximumExpansion = Math.max(maximumExpansion, other.maximumExpansion);
			minimumSlotSeparation = Math.min(minimumSlotSeparation, other.minimumSlotSeparation);
		}
		String json() {
			StringBuilder regions = new StringBuilder("{");
			for (var entry : reversedByRegion.entrySet()) {
				if (regions.length() > 1) regions.append(',');
				regions.append('"').append(entry.getKey()).append("\":").append(entry.getValue());
			}
			regions.append('}');
			return "{\"skinSamples\":" + skinSamples + ",\"outsideSkin\":" + outsideSkin
				+ ",\"overlapSamples\":" + overlapSamples + ",\"outsideOuterShell\":" + outsideOuterShell
				+ ",\"triangles\":" + triangles + ",\"reversedTriangles\":" + reversed
				+ ",\"reversedByRegion\":" + regions + ",\"worstOrientationAt\":\"" + worstOrientationAt + "\""
				+ ",\"minimumOrientationCosine\":" + minimumOrientation + ",\"maximumExpansionPixels\":" + maximumExpansion
				+ ",\"minimumSampledSlotSeparationPixels\":" + (Double.isFinite(minimumSlotSeparation) ? minimumSlotSeparation : "null") + "}";
		}
	}

	public static void main(String[] args) {
		boolean shared = args.length == 1 && args[0].equals("--shared-player");
		var roots = EntityModelSet.vanilla();
		Metrics total = new Metrics();
		StringBuilder variants = new StringBuilder();
		int bindFaces = 0;
		for (boolean slim : new boolean[] {false, true}) {
			var layers = slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR;
			Map<EquipmentSlot, ArticulatedArmorGeometry> models = new EnumMap<>(EquipmentSlot.class);
			for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				ModelPart source = roots.bakeLayer(layers.get(slot));
				var model = new ArticulatedArmorGeometry(source, slot);
				bindFaces += verifyBind(source, model); models.put(slot, model);
			}
			var viewModel = new ArticulatedArmorGeometry(roots.bakeLayer(layers.chest()), EquipmentSlot.CHEST, true);
			if (viewModel.mesh().controlPoints().stream().anyMatch(point -> !point.region().arm())) throw new AssertionError("Non-arm first-person armor");
			for (boolean left : new boolean[] {false, true}) for (int move : shared ? new int[] {1, 2} : new int[] {0}) {
				Metrics metrics = new Metrics();
				var rig = new ArticulatedRig(slim, false);
				var rule = MastersArtRules.move(move);
				for (int tick = 0; tick <= (rule.windup() + rule.recovery()) * 8; tick++) {
					metrics.move = move; metrics.age = tick / 8F; metrics.firstPerson = false;
					var pose = ArticulatedCombatPose.samplePlayer(move, tick / 8F, rule.windup(), rule.recovery(), left);
					var palette = ArticulatedArmorGeometry.Palette.from(joint -> new Matrix4f().set(pose.world(joint).values()));
					rig.apply(pose::local);
					Map<EquipmentSlot, List<Face>> snapshots = new EnumMap<>(EquipmentSlot.class);
					for (var entry : models.entrySet()) {
						var model = entry.getValue(); model.setupAnim(palette);
						List<Face> faces = snapshot(model); snapshots.put(entry.getKey(), faces);
						verifyPose(model, palette, faces, metrics);
						verifySkin(rig, faces, metrics);
					}
					verifySlots(snapshots.get(EquipmentSlot.LEGS), snapshots.get(EquipmentSlot.CHEST), Region.BODY, metrics);
					verifySlots(snapshots.get(EquipmentSlot.LEGS), snapshots.get(EquipmentSlot.FEET), Region.RIGHT_LEG, metrics);
					verifySlots(snapshots.get(EquipmentSlot.LEGS), snapshots.get(EquipmentSlot.FEET), Region.LEFT_LEG, metrics);
					var view = ArticulatedCombatPose.view(pose, left);
					var viewPalette = ArticulatedArmorGeometry.Palette.from(joint -> new Matrix4f().set(view.world(joint).values()));
					viewModel.setupAnim(viewPalette); rig.apply(view::local); metrics.firstPerson = true;
					List<Face> arms = snapshot(viewModel);
					verifyPose(viewModel, viewPalette, arms, metrics); verifySkin(rig, arms, metrics);
				}
				if (!variants.isEmpty()) variants.append(',');
				variants.append("{");
				if (shared) variants.append("\"move\":").append(move).append(",\"clip\":\"").append(rule.id()).append("\",");
				variants.append("\"slim\":").append(slim).append(",\"left\":").append(left).append(",\"metrics\":").append(metrics.json()).append('}');
				total.add(metrics);
			}
		}
		boolean pass = total.outsideSkin == 0 && total.outsideOuterShell == 0 && total.reversed == 0 && total.minimumSlotSeparation > .001;
		System.out.println("{\"kind\":\"offline production CPU geometry, not native acceptance\",\"timeStepTicks\":0.125,\"surfaceSampleGrid\":3,"
			+ "\"bindFacesVerified\":" + bindFaces + ",\"variants\":[" + variants + "],\"totals\":" + total.json() + ",\"passes\":" + pass
			+ ",\"limits\":[\"Finite time and surface sampling is not a proof for all continuous poses or arbitrary modded palettes.\","
			+ "\"All underlying skin and overlay samples are tested, including transparent armor regions.\","
			+ "\"Orientation checks do not prove absence of every nonadjacent triangle or opposite-arm intersection.\","
			+ "\"Expansion is measured against the same palette's ordinary linear skinning, not an extra draw shell.\","
			+ "\"Native silhouette, material, trim, glint, lighting and cross-client acceptance remain pending.\"]}");
		if (!pass) throw new AssertionError("Articulated armor CPU geometry gate failed");
	}

	private static int verifyBind(ModelPart source, ArticulatedArmorGeometry model) {
		List<ModelPart.Polygon> originals = new ArrayList<>();
		source.visit(new PoseStack(), (pose, path, index, cube) -> {
			for (var polygon : cube.polygons) {
				ModelPart.Vertex[] vertices = new ModelPart.Vertex[4];
				for (int i = 0; i < 4; i++) {
					var v = polygon.vertices()[i];
					Vector3f p = pose.pose().transformPosition(v.worldX(), v.worldY(), v.worldZ(), new Vector3f()).mul(16);
					vertices[i] = new ModelPart.Vertex(p.x, p.y, p.z, v.u(), v.v());
				}
				originals.add(new ModelPart.Polygon(vertices, new Vector3f(polygon.normal())));
			}
		});
		Map<String, Integer> edges = new HashMap<>();
		double[] area = new double[originals.size()];
		List<Face> bind = snapshot(model);
		for (int f = 0; f < model.mesh().faces().size(); f++) {
			var face = model.mesh().faces().get(f); var original = originals.get(face.sourceFace());
			Vector3f normal = new Vector3f(face.bindNormal().x(), face.bindNormal().y(), face.bindNormal().z());
			if (normal.dot(original.normal()) < .99999) throw new AssertionError("Vanilla bind normal mismatch");
			for (int i = 0; i < 4; i++) {
				var corner = face.corners().get(i); var next = face.corners().get((i + 1) % 4);
				int a = corner.controlPoint(), b = next.controlPoint(); edges.merge(Math.min(a, b) + ":" + Math.max(a, b), 1, Integer::sum);
				var p = model.mesh().controlPoints().get(a).bindPosition(); var v = bind.get(f).vertices()[i];
				if (new Vector3f(p.x(), p.y(), p.z()).distance(v.x(), v.y(), v.z()) > .00001) throw new AssertionError("Bind position changed");
				var ref = original.vertices()[0];
				if (Math.abs((p.x() - ref.x()) * normal.x + (p.y() - ref.y()) * normal.y + (p.z() - ref.z()) * normal.z) > .00001)
					throw new AssertionError("Added internal/off-source face");
				if (!uvOnOriginal(v, original.vertices())) throw new AssertionError("UV was not interpolated from its original exterior edge");
				area[face.sourceFace()] += ((double) corner.u() * next.v() - (double) next.u() * corner.v()) / 2;
			}
		}
		if (edges.values().stream().anyMatch(count -> count != 2)) throw new AssertionError("Unwelded/nonmanifold armor edge");
		for (int f = 0; f < originals.size(); f++) {
			double expected = 0; var vertices = originals.get(f).vertices();
			for (int i = 0; i < 4; i++) expected += ((double) vertices[i].u() * vertices[(i + 1) % 4].v() - (double) vertices[(i + 1) % 4].u() * vertices[i].v()) / 2;
			if (Math.abs(expected - area[f]) > .000001) throw new AssertionError("Original UV area changed");
		}
		return model.mesh().faces().size();
	}
	private static boolean uvOnOriginal(ModelPart.Vertex v, ModelPart.Vertex[] original) {
		for (var a : original) for (var b : original) {
			if (a.x() != v.x() || a.z() != v.z() || b.x() != v.x() || b.z() != v.z()) continue;
			if (a.y() == b.y()) { if (Math.abs(v.y() - a.y()) < .00001 && Math.abs(v.u() - a.u()) < .000001 && Math.abs(v.v() - a.v()) < .000001) return true; }
			else {
				float t = (v.y() - a.y()) / (b.y() - a.y());
				if (t >= 0 && t <= 1 && Math.abs(v.u() - (a.u() + (b.u() - a.u()) * t)) < .000001
					&& Math.abs(v.v() - (a.v() + (b.v() - a.v()) * t)) < .000001) return true;
			}
		}
		return false;
	}
	private static List<Face> snapshot(ArticulatedArmorGeometry model) {
		List<Face> faces = new ArrayList<>();
		model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
			var sourceFace = model.mesh().faces().get(faces.size());
			Region region = model.mesh().controlPoints().get(sourceFace.corners().getFirst().controlPoint()).region();
			faces.add(new Face(region, cube.polygons[0].vertices().clone()));
		});
		return faces;
	}
	private static void verifyPose(ArticulatedArmorGeometry model, ArticulatedArmorGeometry.Palette palette, List<Face> faces, Metrics metrics) {
		for (int f = 0; f < faces.size(); f++) {
			var source = model.mesh().faces().get(f); var vertices = faces.get(f).vertices(); Vector3f expected = new Vector3f();
			for (int i = 0; i < 4; i++) {
				var point = model.mesh().controlPoints().get(source.corners().get(i).controlPoint()); var n = source.bindNormal();
				Vector3f a = palette.matrix(point.first()).transformDirection(n.x(), n.y(), n.z(), new Vector3f());
				Vector3f b = palette.matrix(point.second()).transformDirection(n.x(), n.y(), n.z(), new Vector3f());
				expected.add(a.lerp(b, point.secondWeight()));
				Vector3f linear = skin(palette, point.first(), point.bindPosition()).lerp(skin(palette, point.second(), point.bindPosition()), point.secondWeight());
				metrics.maximumExpansion = Math.max(metrics.maximumExpansion, linear.distance(vertices[i].x(), vertices[i].y(), vertices[i].z()));
			}
			expected.normalize(); Vector3d origin = vec(vertices[0]);
			for (int t = 1; t <= 2; t++) {
				Vector3d cross = vec(vertices[t]).sub(origin).cross(vec(vertices[t + 1]).sub(origin));
				double cosine = cross.lengthSquared() < 1.0e-12 ? -1 : cross.normalize().dot(expected.x, expected.y, expected.z);
				metrics.triangles++;
				if (cosine < metrics.minimumOrientation) metrics.worstOrientationAt = "move=" + metrics.move + ";age=" + metrics.age
					+ ";view=" + metrics.firstPerson + ";region=" + faces.get(f).region() + ";face=" + f + ";triangle=" + t;
				metrics.minimumOrientation = Math.min(metrics.minimumOrientation, cosine);
				if (!Double.isFinite(cosine) || cosine <= 0) {
					metrics.reversed++;
					metrics.reversedByRegion.merge((metrics.firstPerson ? "view_" : "body_") + faces.get(f).region(), 1L, Long::sum);
				}
			}
		}
	}
	private static Vector3f skin(ArticulatedArmorGeometry.Palette palette, Joint joint, ArticulatedCombatPose.Vec3 point) {
		return palette.matrix(joint).mul(new Matrix4f().set(ArticulatedCombatPose.NONE.world(joint).values()).invert())
			.transformPosition(point.x(), point.y(), point.z(), new Vector3f());
	}
	private static void verifySkin(ArticulatedRig rig, List<Face> armor, Metrics metrics) {
		var triangles = triangles(armor);
		rig.root.visit(new PoseStack(), (pose, path, index, cube) -> {
			Region region = region(path); if (!triangles.containsKey(region)) return;
			for (var polygon : cube.polygons) for (Vector3d point : samples(polygon.vertices())) {
				Vector3f transformed = pose.pose().transformPosition((float) point.x / 16, (float) point.y / 16, (float) point.z / 16, new Vector3f()).mul(16);
				metrics.skinSamples++; if (!inside(new Vector3d(transformed), triangles.get(region))) metrics.outsideSkin++;
			}
		});
	}
	private static void verifySlots(List<Face> inner, List<Face> outer, Region region, Metrics metrics) {
		List<Triangle> shell = triangles(outer).get(region);
		for (Face face : inner) if (face.region() == region) for (Vector3d p : samples(face.vertices())) {
			metrics.overlapSamples++; if (!inside(p, shell)) metrics.outsideOuterShell++;
			for (Triangle triangle : shell) metrics.minimumSlotSeparation = Math.min(metrics.minimumSlotSeparation, distance(p, triangle));
		}
	}
	private static Map<Region, List<Triangle>> triangles(List<Face> faces) {
		Map<Region, List<Triangle>> result = new EnumMap<>(Region.class);
		for (Face face : faces) for (int i = 1; i <= 2; i++) result.computeIfAbsent(face.region(), key -> new ArrayList<>())
			.add(new Triangle(vec(face.vertices()[0]), vec(face.vertices()[i]), vec(face.vertices()[i + 1])));
		return result;
	}
	private static List<Vector3d> samples(ModelPart.Vertex[] v) {
		List<Vector3d> points = new ArrayList<>(9);
		for (int a = 0; a <= 2; a++) for (int b = 0; b <= 2; b++) {
			double u = a / 2.0, w = b / 2.0;
			points.add(vec(v[0]).mul((1 - u) * (1 - w)).add(vec(v[1]).mul(u * (1 - w))).add(vec(v[2]).mul(u * w)).add(vec(v[3]).mul((1 - u) * w)));
		}
		return points;
	}
	private static Region region(String path) {
		if (path.contains("RIGHT_UPPER_ARM")) return Region.RIGHT_ARM; if (path.contains("LEFT_UPPER_ARM")) return Region.LEFT_ARM;
		if (path.contains("RIGHT_THIGH")) return Region.RIGHT_LEG; if (path.contains("LEFT_THIGH")) return Region.LEFT_LEG;
		return path.contains("HEAD") ? Region.HEAD : Region.BODY;
	}
	private static Vector3d vec(ModelPart.Vertex v) { return new Vector3d(v.x(), v.y(), v.z()); }
	/** Oriented solid angle avoids ray/edge double-counting on welded polygon seams. */
	private static boolean inside(Vector3d p, List<Triangle> triangles) {
		double sum = 0;
		for (Triangle triangle : triangles) {
			Vector3d a = new Vector3d(triangle.a()).sub(p), b = new Vector3d(triangle.b()).sub(p), c = new Vector3d(triangle.c()).sub(p);
			double al = a.length(), bl = b.length(), cl = c.length();
			if (Math.min(al, Math.min(bl, cl)) < .00001) return true;
			double numerator = a.dot(new Vector3d(b).cross(c)), denominator = al * bl * cl + a.dot(b) * cl + b.dot(c) * al + c.dot(a) * bl;
			sum += 2 * Math.atan2(numerator, denominator);
		}
		return Math.abs(sum) > 6;
	}
	private static double distance(Vector3d p, Triangle t) {
		Vector3d ab = new Vector3d(t.b()).sub(t.a()), ac = new Vector3d(t.c()).sub(t.a()), n = new Vector3d(ab).cross(ac).normalize();
		double plane = new Vector3d(p).sub(t.a()).dot(n);
		Vector3d q = new Vector3d(p).sub(new Vector3d(n).mul(plane));
		Vector3d aq = new Vector3d(q).sub(t.a());
		double d00 = ab.dot(ab), d01 = ab.dot(ac), d11 = ac.dot(ac), d20 = aq.dot(ab), d21 = aq.dot(ac), determinant = d00 * d11 - d01 * d01;
		if (determinant > 1.0e-12) {
			double v = (d11 * d20 - d01 * d21) / determinant, w = (d00 * d21 - d01 * d20) / determinant;
			if (v >= 0 && w >= 0 && v + w <= 1) return Math.abs(plane);
		}
		return Math.min(segmentDistance(p, t.a(), t.b()), Math.min(segmentDistance(p, t.b(), t.c()), segmentDistance(p, t.c(), t.a())));
	}
	private static double segmentDistance(Vector3d p, Vector3d a, Vector3d b) {
		Vector3d edge = new Vector3d(b).sub(a);
		double amount = Math.max(0, Math.min(1, new Vector3d(p).sub(a).dot(edge) / edge.lengthSquared()));
		return p.distance(new Vector3d(a).add(edge.mul(amount)));
	}
}
