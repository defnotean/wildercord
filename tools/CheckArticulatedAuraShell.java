import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.MastersArtRules;
import dev.wildercord.aura.MastersStyleRules;
import dev.wildercord.client.combat.ArticulatedArmorGeometry.Palette;
import dev.wildercord.client.combat.ArticulatedAuraShellGeometry;
import dev.wildercord.client.combat.ArticulatedRig;
import dev.wildercord.client.render.AuraShellLayer;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Offline original-runtime bake, UV, topology and deferred-palette checks. Never a native pass. */
public final class CheckArticulatedAuraShell {
	private static final Set<String> PARTS = Set.of("/head", "/body", "/right_arm", "/left_arm", "/right_leg", "/left_leg");
	public static void main(String[] args) {
		boolean openingStyles = args.length == 1 && args[0].equals("--opening-styles");
		boolean hailSky = args.length == 1 && args[0].equals("--hail-sky");
		boolean styles = openingStyles || hailSky;
		int snapshots = 0, negatives = 0;
		for (boolean slim : new boolean[] {false, true}) {
			var source = source(slim);
			var world = new ArticulatedAuraShellGeometry(source, slim, false);
			var view = new ArticulatedAuraShellGeometry(source, slim, true);
			check(world.mesh().faces().stream().map(f -> f.sourceFace()).distinct().count() == 36, "Exactly the six original shell cuboids supply surfaces");
			check(view.mesh().faces().stream().map(f -> f.sourceFace()).distinct().count() == 12, "View uses only the two original arm surfaces");
			check(view.mesh().controlPoints().stream().allMatch(p -> p.region().arm()), "View has no head/body/leg geometry");
			check(world.mesh().faces().size() > 36, "World shell actually has joint subdivisions");
			validateSource(source, world);
			var rig = new ArticulatedRig(slim, false);
			for (boolean left : new boolean[] {false, true}) for (int move : hailSky ? new int[] {15, 16} : openingStyles ? new int[] {3, 4} : new int[] {0, 1, 2}) {
				var style = styles ? MastersStyleRules.animation(move) : null;
				if (openingStyles && (style == null || style.windup() != 6 || style.recovery() != 12))
					throw new AssertionError("Opening-style shell audit requires the accepted 6/12 animation window");
				if (hailSky && (style == null || style.windup() != (move == 15 ? 8 : 6) || style.recovery() != 16))
					throw new AssertionError("Hail/Sky audit requires accepted 8/16 and 6/16 windows");
				var rule = styles ? null : MastersArtRules.move(move);
				int windup = styles ? style.windup() : rule.windup();
				int recovery = styles ? style.recovery() : rule.recovery();
				for (float age = 0; age <= windup + recovery; age += .125F) {
					var pose = ArticulatedCombatPose.samplePlayer(move, age, windup, recovery, left);
					rig.apply(pose::local);
					// Final free-look and baseline corrections must survive snapshot and later rig edits.
					rig.part(ArticulatedCombatPose.Joint.HEAD).yRot += .31F;
					rig.part(ArticulatedCombatPose.Joint.LEFT_SHOULDER).x += .07F;
					var captured = Palette.capture(rig);
					Matrix4f[] external = new Matrix4f[ArticulatedCombatPose.Joint.values().length];
					for (var joint : ArticulatedCombatPose.Joint.values()) external[joint.ordinal()] = captured.matrix(joint);
					var copied = Palette.from(j -> external[j.ordinal()]);
					for (var matrix : external) matrix.zero();
					world.setupAnim(captured); int[] a = snapshot(world);
					rig.apply(ArticulatedCombatPose.samplePlayer(move, age < windup ? windup : 0, windup, recovery, !left)::local);
					var b = Palette.capture(rig);
					world.setupAnim(b); world.setupAnim(captured);
					check(Arrays.equals(a, snapshot(world)), "World A/B/A positions, UVs and normals changed");
					world.setupAnim(copied);
					check(Arrays.equals(a, snapshot(world)), "Shell palette retained caller-owned matrices");
					captured.matrix(ArticulatedCombatPose.Joint.HEAD).zero();
					world.setupAnim(b); world.setupAnim(captured);
					check(Arrays.equals(a, snapshot(world)), "Shell palette exposed mutable matrices");
					var camera = ArticulatedCombatPose.view(pose, left);
					rig.apply(camera::local); var viewA = Palette.capture(rig);
					view.setupAnim(viewA); int[] va = snapshot(view);
					view.setupAnim(b); view.setupAnim(viewA);
					check(Arrays.equals(va, snapshot(view)), "View A/B/A replay changed");
					snapshots += 2;
				}
			}
			for (boolean armsOnly : new boolean[] {false, true}) {
				reject(() -> new ArticulatedAuraShellGeometry(source(!slim), slim, armsOnly), "Wrong skin width"); negatives++;
				reject(() -> new ArticulatedAuraShellGeometry(new ModelPart(List.of(), Map.of()), slim, armsOnly), "Empty geometry"); negatives++;
				var shifted = source(slim); shifted.getChild("body").x += 1;
				reject(() -> new ArticulatedAuraShellGeometry(shifted, slim, armsOnly), "Posed source"); negatives++;
				for (String part : List.of("head", "body", "right_arm", "left_arm", "right_leg", "left_leg")) {
					var missing = source(slim); Map<String, ModelPart> children = new LinkedHashMap<>();
					for (String name : List.of("head", "body", "right_arm", "left_arm", "right_leg", "left_leg")) if (!name.equals(part)) children.put(name, missing.getChild(name));
					reject(() -> new ArticulatedAuraShellGeometry(new ModelPart(List.of(), children), slim, armsOnly), "Missing " + part); negatives++;
				}
				var uv = source(slim);
				uv.visit(new PoseStack(), (pose, path, index, cube) -> {
					if (!path.equals("/right_arm")) return;
					var polygon = cube.polygons[0]; var vertices = polygon.vertices().clone(); var old = vertices[0];
					vertices[0] = new ModelPart.Vertex(old.x(), old.y(), old.z(), old.u() + .01F, old.v());
					cube.polygons[0] = new ModelPart.Polygon(vertices, polygon.normal());
				});
				reject(() -> new ArticulatedAuraShellGeometry(uv, slim, armsOnly), "Altered UV"); negatives++;
			}
		}
		String scope = hailSky ? ",\"clips\":[\"hailfall\",\"skyfall\"],\"timeStepTicks\":0.125,\"windows\":[[8,16],[6,16]],\"variants\":8" : openingStyles ? ",\"clips\":[\"" + MastersStyleRules.animation(3).art() + "\",\"" + MastersStyleRules.animation(4).art()
			+ "\"],\"timeStepTicks\":0.125,\"windup\":6,\"recovery\":12,\"variants\":8" : "";
		System.out.println("{\"kind\":\"offline original-runtime shell geometry and deferred palettes\",\"snapshots\":" + snapshots + ",\"negativeCases\":" + negatives + scope + ",\"passes\":true,\"native\":\"not run\"}");
	}
	private static ModelPart source(boolean slim) { return (slim ? AuraShellLayer.createSlimShell() : AuraShellLayer.createShell()).bakeRoot(); }
	private static void validateSource(ModelPart source, ArticulatedAuraShellGeometry model) {
		List<List<ModelPart.Vertex>> faces = new ArrayList<>();
		source.visit(new PoseStack(), (pose, path, index, cube) -> {
			if (!PARTS.contains(path)) return;
			for (var polygon : cube.polygons) {
				List<ModelPart.Vertex> vertices = new ArrayList<>();
				for (var v : polygon.vertices()) {
					var p = pose.pose().transformPosition(v.worldX(), v.worldY(), v.worldZ(), new Vector3f()).mul(16);
					vertices.add(new ModelPart.Vertex(p.x, p.y, p.z, v.u(), v.v()));
				}
				faces.add(vertices);
			}
		});
		for (var face : model.mesh().faces()) {
			var original = faces.get(face.sourceFace());
			for (var corner : face.corners()) {
				var p = model.mesh().controlPoints().get(corner.controlPoint()).bindPosition();
				boolean match = false;
				for (var a : original) for (var b : original) {
					if (a.x() != p.x() || a.z() != p.z() || b.x() != p.x() || b.z() != p.z()) continue;
					float t = a.y() == b.y() ? 0 : (p.y() - a.y()) / (b.y() - a.y());
					if (t < 0 || t > 1 || a.y() == b.y() && p.y() != a.y()) continue;
					if (Math.abs(corner.u() - (a.u() + (b.u() - a.u()) * t)) < .000001F
						&& Math.abs(corner.v() - (a.v() + (b.v() - a.v()) * t)) < .000001F) match = true;
				}
				check(match, "Shell position/UV must be an affine sample of its exact original quad");
			}
		}
	}
	private static int[] snapshot(ArticulatedAuraShellGeometry model) {
		List<Integer> bits = new ArrayList<>();
		model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
			check(cube.polygons.length == 1, "Storage cuboid emitted additional shell faces");
			for (var polygon : cube.polygons) {
				for (var v : polygon.vertices()) add(bits, v.x(), v.y(), v.z(), v.u(), v.v());
				add(bits, polygon.normal().x(), polygon.normal().y(), polygon.normal().z());
			}
		});
		return bits.stream().mapToInt(Integer::intValue).toArray();
	}
	private static void add(List<Integer> bits, float... values) { for (float v : values) { check(Float.isFinite(v), "Nonfinite shell vertex"); bits.add(Float.floatToIntBits(v)); } }
	private static void reject(Runnable action, String label) { try { action.run(); } catch (IllegalArgumentException expected) { return; } throw new AssertionError("Accepted " + label); }
	private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
