package dev.wildercord.aura;

import dev.wildercord.aura.ArticulatedArmorMesh.Region;
import dev.wildercord.aura.ArticulatedArmorMesh.SourceFace;
import dev.wildercord.aura.ArticulatedArmorMesh.SourceVertex;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.wildercord.aura.ArticulatedCombatPose.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Full-clock stock armor and funded-shell winding contracts for Batch 4a Elemental Pinnacles. */
class ArticulatedBatch4aPinnacleArmorTest {
	private static final int[] MOVES = {SUNFALL, WINTERS_HUSH, HEAVENS_SPEAR, HUNDRED_WINDS, MOUNTAIN_SPLITTER};

	@Test
	void stockArmorIncludingLegCollarsStaysOutwardAcrossHandsSkinWidthsAndInflations() {
		for (boolean slim : new boolean[] {false, true}) for (Region region : Region.values())
			for (float grow : new float[] {.4F, .9F}) {
				var mesh = ArticulatedArmorMesh.bake(stockRegion(region, slim, grow));
				assertFullClockOutward(mesh, .25F, "Stock armor " + region + " slim=" + slim + " grow=" + grow);
			}
	}

	@Test
	void fundedShellTrianglesStayOutwardAcrossHandsAndSkinWidths() {
		for (boolean slim : new boolean[] {false, true}) for (Region region : Region.values()) {
			var mesh = ArticulatedArmorMesh.bakeAuraShell(stockRegion(region, slim, .55F));
			assertFullClockOutward(mesh, .25F, "Aura shell " + region + " slim=" + slim);
		}
	}

	private static void assertFullClockOutward(ArticulatedArmorMesh.Mesh mesh, float step, String label) {
		for (int move : MOVES) for (boolean left : new boolean[] {false, true}) {
			var rule = MastersStyleRules.animation(move);
			for (float age = 0; age <= rule.windup() + rule.recovery(); age += step) {
				Pose pose = samplePlayer(move, age, rule.windup(), rule.recovery(), left);
				String at = label + " move=" + move + " age=" + age + " left=" + left;
				assertOutward(mesh, pose::world, at + " world");
				if (mesh.controlPoints().stream().allMatch(point -> point.region().arm()))
					assertOutward(mesh, view(pose, left)::world, at + " first person");
			}
		}
	}

	private static void assertOutward(ArticulatedArmorMesh.Mesh mesh, java.util.function.Function<Joint, Matrix> world, String label) {
		var deformed = mesh.deform((joint, point) -> world.apply(joint).multiply(NONE.world(joint).inverseRigid()).transform(point));
		for (Vec3 point : deformed.positions())
			assertTrue(Float.isFinite(point.x()) && Float.isFinite(point.y()) && Float.isFinite(point.z()), label + " non-finite vertex");
		for (var face : mesh.faces()) {
			Vec3 expected = Vec3.ZERO;
			for (var corner : face.corners()) {
				var point = mesh.controlPoints().get(corner.controlPoint());
				expected = expected.plus(world.apply(point.first()).direction(face.bindNormal())
					.toward(world.apply(point.second()).direction(face.bindNormal()), point.secondWeight()));
			}
			Vec3 origin = deformed.positions().get(face.corners().getFirst().controlPoint());
			for (int triangle = 1; triangle <= 2; triangle++) {
				Vec3 b = deformed.positions().get(face.corners().get(triangle).controlPoint());
				Vec3 c = deformed.positions().get(face.corners().get(triangle + 1).controlPoint());
				float orientation = b.minus(origin).cross(c.minus(origin)).dot(expected);
				assertTrue(Float.isFinite(orientation) && orientation > 0,
					label + " face=" + face.sourceFace() + " triangle=" + triangle + " orientation=" + orientation);
			}
		}
	}

	/** Stock vanilla pivots/extents, including 4x12x4 legs and the two real arm widths. */
	private static List<SourceFace> stockRegion(Region region, boolean slim, float grow) {
		boolean arm = region.arm(), left = region == Region.LEFT_ARM || region == Region.LEFT_LEG;
		boolean head = region == Region.HEAD, body = region == Region.BODY;
		float width = head || body ? 8 : arm && slim ? 3 : 4;
		float x = head || body ? -4 : arm ? (left ? 5 - 1 : -5 + (slim ? -2 : -3)) : (left ? 1.9F : -1.9F) - 2;
		float y = head ? -8 : body || arm ? 0 : 12, z = head ? -4 : -2, height = head ? 8 : 12, depth = head ? 8 : 4;
		float lo = x - grow, hi = x + width + grow, top = y - grow, bottom = y + height + grow, near = z - grow, far = z + depth + grow;
		Vec3[] p = {new Vec3(lo, top, near), new Vec3(hi, top, near), new Vec3(hi, bottom, near), new Vec3(lo, bottom, near),
			new Vec3(lo, top, far), new Vec3(hi, top, far), new Vec3(hi, bottom, far), new Vec3(lo, bottom, far)};
		List<SourceFace> faces = new ArrayList<>();
		for (int[] indices : new int[][] {{1, 0, 3, 2}, {4, 5, 6, 7}, {0, 4, 7, 3}, {5, 1, 2, 6}, {5, 4, 0, 1}, {2, 3, 7, 6}})
			faces.add(new SourceFace(region, List.of(new SourceVertex(p[indices[0]], 0, 0), new SourceVertex(p[indices[1]], 1, 0),
				new SourceVertex(p[indices[2]], 1, 1), new SourceVertex(p[indices[3]], 0, 1))));
		return faces;
	}
}
