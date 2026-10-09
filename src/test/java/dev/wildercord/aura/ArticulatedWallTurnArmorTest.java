package dev.wildercord.aura;

import dev.wildercord.aura.ArticulatedArmorMesh.Region;
import dev.wildercord.aura.ArticulatedArmorMesh.SourceFace;
import dev.wildercord.aura.ArticulatedArmorMesh.SourceVertex;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.wildercord.aura.ArticulatedCombatPose.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Full Wall Turn stock armor and funded-shell winding contracts; runtime bake and pixels remain separate gates. */
class ArticulatedWallTurnArmorTest {
	@Test
	void stockArmorStaysOutwardAcrossEveryWallTurnFrame() {
		for (boolean slim : new boolean[] {false, true}) for (Region region : Region.values())
			for (float grow : new float[] {.4F, .9F})
				assertOutward(ArticulatedArmorMesh.bake(stockRegion(region, slim, grow)), "Stock armor " + region + " slim=" + slim + " grow=" + grow);
	}

	@Test
	void fundedShellStaysOutwardAcrossEveryWallTurnFrame() {
		for (boolean slim : new boolean[] {false, true}) for (Region region : Region.values())
			assertOutward(ArticulatedArmorMesh.bakeAuraShell(stockRegion(region, slim, .55F)), "Aura shell " + region + " slim=" + slim);
	}

	private static void assertOutward(ArticulatedArmorMesh.Mesh mesh, String label) {
		for (var frame : ArticulatedWallTurnPoseTest.frames()) for (Pose pose : new Pose[] {frame.right(), frame.left()}) {
			var deformed = mesh.deform((joint, point) -> pose.world(joint).multiply(NONE.world(joint).inverseRigid()).transform(point));
			for (Vec3 point : deformed.positions())
				assertTrue(Float.isFinite(point.x()) && Float.isFinite(point.y()) && Float.isFinite(point.z()), label + " non-finite vertex");
			for (var face : mesh.faces()) {
				Vec3 expected = Vec3.ZERO;
				for (var corner : face.corners()) {
					var point = mesh.controlPoints().get(corner.controlPoint());
					expected = expected.plus(pose.world(point.first()).direction(face.bindNormal())
						.toward(pose.world(point.second()).direction(face.bindNormal()), point.secondWeight()));
				}
				Vec3 origin = deformed.positions().get(face.corners().getFirst().controlPoint());
				for (int triangle = 1; triangle <= 2; triangle++) {
					Vec3 b = deformed.positions().get(face.corners().get(triangle).controlPoint());
					Vec3 c = deformed.positions().get(face.corners().get(triangle + 1).controlPoint());
					float orientation = b.minus(origin).cross(c.minus(origin)).dot(expected);
					assertTrue(Float.isFinite(orientation) && orientation > 0,
						label + " " + frame.label() + (pose == frame.left() ? " left" : "") + " face=" + face.sourceFace() + " triangle=" + triangle + " orientation=" + orientation);
				}
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
