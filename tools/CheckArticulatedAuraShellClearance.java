import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.ArticulatedArmorMesh.Region;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.MastersArtRules;
import dev.wildercord.aura.MastersStyleRules;
import dev.wildercord.client.combat.ArticulatedArmorGeometry.Palette;
import dev.wildercord.client.combat.ArticulatedAuraShellGeometry;
import dev.wildercord.client.combat.ArticulatedRig;
import dev.wildercord.client.render.AuraShellLayer;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Actual skin/sleeve clearance by oriented solid angle. CPU geometry, never native pixels. */
public final class CheckArticulatedAuraShellClearance {
	private record Face(Region region, ModelPart.Vertex[] vertices) {}
	private record Triangle(Vector3d a, Vector3d b, Vector3d c) {}
	private static long samples, triangles, outside, reversed;
	private static String sampleLabel;

	public static void main(String[] args) {
		boolean openingStyles = args.length == 1 && args[0].equals("--opening-styles");
		boolean hailSky = args.length == 1 && args[0].equals("--hail-sky");
		boolean groundFields = args.length == 1 && args[0].equals("--ground-fields");
		boolean earnedCounters = args.length == 1 && args[0].equals("--earned-counters");
		boolean styles = openingStyles || hailSky || groundFields || earnedCounters;
		for (boolean slim : new boolean[] {false, true}) for (boolean left : new boolean[] {false, true}) for (int move : earnedCounters ? new int[] {20, 21} : groundFields ? new int[] {17, 18} : hailSky ? new int[] {15, 16} : openingStyles ? new int[] {3, 4} : new int[] {0, 1, 2}) {
			var source = (slim ? AuraShellLayer.createSlimShell() : AuraShellLayer.createShell()).bakeRoot();
			var world = new ArticulatedAuraShellGeometry(source, slim, false);
			var view = new ArticulatedAuraShellGeometry(source, slim, true);
			var rig = new ArticulatedRig(slim, false);
			var style = styles ? MastersStyleRules.animation(move) : null;
			if (openingStyles && (style == null || style.windup() != 6 || style.recovery() != 12))
				throw new AssertionError("Opening-style shell audit requires the accepted 6/12 animation window");
			if (hailSky && (style == null || style.windup() != (move == 15 ? 8 : 6) || style.recovery() != 16))
				throw new AssertionError("Hail/Sky audit requires accepted 8/16 and 6/16 windows");
				if (groundFields && (style == null || style.windup() != 8 || style.recovery() != (move == 17 ? 18 : 16)))
					throw new AssertionError("Ground field audit requires accepted 8/18 and 8/16 windows");
			if (earnedCounters && (style == null || style.windup() != (move == 20 ? 4 : 6) || style.recovery() != (move == 20 ? 14 : 16)))
				throw new AssertionError("Earned-counter audit requires accepted 4/14 and 6/16 windows");
			var rule = styles ? null : MastersArtRules.move(move);
			int windup = styles ? style.windup() : rule.windup();
			int recovery = styles ? style.recovery() : rule.recovery();
			for (int tick = 0; tick <= (windup + recovery) * 8; tick++) {
				var pose = ArticulatedCombatPose.samplePlayer(move, tick / 8F, windup, recovery, left);
				for (boolean firstPerson : new boolean[] {false, true}) {
					sampleLabel = "slim=" + slim + " left=" + left + " move=" + move + " age=" + tick / 8F + " view=" + firstPerson;
					if (firstPerson) rig.apply(ArticulatedCombatPose.view(pose, left)::local); else rig.apply(pose::local);
					var palette = Palette.capture(rig); var model = firstPerson ? view : world;
					model.setupAnim(palette); var faces = snapshot(model);
					checkOrientation(model, palette, faces); checkSkin(rig, faces);
				}
			}
		}
		String scope = earnedCounters ? ",\"clips\":[\"backdraft\",\"rooted_parry\"],\"timeStepTicks\":0.125,\"windows\":[[4,14],[6,16]],\"variants\":8,\"passes\":" + (outside == 0 && reversed == 0) : groundFields ? ",\"clips\":[\"collapse\",\"red_rain\"],\"timeStepTicks\":0.125,\"windows\":[[8,18],[8,16]],\"variants\":8,\"passes\":" + (outside == 0 && reversed == 0) : hailSky ? ",\"clips\":[\"hailfall\",\"skyfall\"],\"timeStepTicks\":0.125,\"windows\":[[8,16],[6,16]],\"variants\":8,\"passes\":" + (outside == 0 && reversed == 0) : openingStyles ? ",\"clips\":[\"" + MastersStyleRules.animation(3).art() + "\",\"" + MastersStyleRules.animation(4).art()
			+ "\"],\"timeStepTicks\":0.125,\"windup\":6,\"recovery\":12,\"variants\":8,\"passes\":" + (outside == 0 && reversed == 0)
			+ ",\"limits\":[\"Finite pure-pose samples, including all skin overlays; not continuous-pose or arbitrary-palette proof.\","
			+ "\"Shell-to-skin containment and outward triangles only; armor, materials, native lighting and cross-client acceptance remain separate.\"]" : "";
		System.out.println("{\"kind\":\"offline original-rig shell/sleeve containment\",\"surfaceSamples\":" + samples
			+ ",\"outsideShell\":" + outside + ",\"triangles\":" + triangles + ",\"reversedTriangles\":" + reversed + scope + ",\"native\":\"not run\"}");
		if (outside != 0 || reversed != 0) throw new AssertionError("Shell clearance/orientation regression");
	}
	private static List<Face> snapshot(ArticulatedAuraShellGeometry model) {
		List<Face> faces = new ArrayList<>();
		model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
			var face = model.mesh().faces().get(faces.size());
			Region region = model.mesh().controlPoints().get(face.corners().getFirst().controlPoint()).region();
			faces.add(new Face(region, cube.polygons[0].vertices().clone()));
		});
		return faces;
	}
	private static void checkOrientation(ArticulatedAuraShellGeometry model, Palette palette, List<Face> faces) {
		for (int f = 0; f < faces.size(); f++) {
			var source = model.mesh().faces().get(f); var vertices = faces.get(f).vertices(); Vector3f expected = new Vector3f();
			for (var corner : source.corners()) {
				var point = model.mesh().controlPoints().get(corner.controlPoint()); var normal = source.bindNormal();
				var a = palette.matrix(point.first()).transformDirection(normal.x(), normal.y(), normal.z(), new Vector3f());
				var b = palette.matrix(point.second()).transformDirection(normal.x(), normal.y(), normal.z(), new Vector3f());
				expected.add(a.lerp(b, point.secondWeight()));
			}
			expected.normalize(); var origin = vec(vertices[0]);
			for (int t = 1; t <= 2; t++) {
				var cross = vec(vertices[t]).sub(origin).cross(vec(vertices[t + 1]).sub(origin));
				double dot = cross.dot(expected.x, expected.y, expected.z()); triangles++;
				if (!Double.isFinite(dot) || dot <= 0) { if (reversed++ == 0) System.out.println("REVERSED " + sampleLabel); }
			}
		}
	}
	private static void checkSkin(ArticulatedRig rig, List<Face> faces) {
		Map<Region, List<Triangle>> shell = new EnumMap<>(Region.class);
		for (Face face : faces) for (int t = 1; t <= 2; t++) shell.computeIfAbsent(face.region(), ignored -> new ArrayList<>())
			.add(new Triangle(vec(face.vertices()[0]), vec(face.vertices()[t]), vec(face.vertices()[t + 1])));
		// All original sleeves/pants/jacket/hat remain present in this worst-case geometry probe.
		rig.root.visit(new PoseStack(), (pose, path, index, cube) -> {
			Region region = region(path); if (!shell.containsKey(region)) return;
			for (var polygon : cube.polygons) for (int a = 0; a <= 2; a++) for (int b = 0; b <= 2; b++) {
				var v = polygon.vertices(); double u = a / 2.0, w = b / 2.0;
				var point = vec(v[0]).mul((1 - u) * (1 - w)).add(vec(v[1]).mul(u * (1 - w))).add(vec(v[2]).mul(u * w)).add(vec(v[3]).mul((1 - u) * w));
				var transformed = pose.pose().transformPosition((float) point.x / 16, (float) point.y / 16, (float) point.z / 16, new Vector3f()).mul(16);
				samples++;
				if (!inside(new Vector3d(transformed), shell.get(region)) && outside++ == 0) System.out.println("OUTSIDE " + sampleLabel + " path=" + path + " point=" + transformed);
			}
		});
	}
	private static Region region(String path) {
		if (path.contains("RIGHT_UPPER_ARM")) return Region.RIGHT_ARM; if (path.contains("LEFT_UPPER_ARM")) return Region.LEFT_ARM;
		if (path.contains("RIGHT_THIGH")) return Region.RIGHT_LEG; if (path.contains("LEFT_THIGH")) return Region.LEFT_LEG;
		return path.contains("HEAD") ? Region.HEAD : Region.BODY;
	}
	private static Vector3d vec(ModelPart.Vertex v) { return new Vector3d(v.x(), v.y(), v.z()); }
	/** Oriented solid angle avoids seam/ray edge double-counting. All dimensions are model pixels. */
	private static boolean inside(Vector3d p, List<Triangle> triangles) {
		double sum = 0;
		for (var triangle : triangles) {
			var a = new Vector3d(triangle.a()).sub(p); var b = new Vector3d(triangle.b()).sub(p); var c = new Vector3d(triangle.c()).sub(p);
			double al = a.length(), bl = b.length(), cl = c.length();
			if (Math.min(al, Math.min(bl, cl)) < .00001) return true;
			double numerator = a.dot(new Vector3d(b).cross(c)), denominator = al * bl * cl + a.dot(b) * cl + b.dot(c) * al + c.dot(a) * bl;
			sum += 2 * Math.atan2(numerator, denominator);
		}
		return Math.abs(sum) > 6;
	}
}
