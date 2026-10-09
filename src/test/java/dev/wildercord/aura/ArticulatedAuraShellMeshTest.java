package dev.wildercord.aura;

import dev.wildercord.aura.ArticulatedArmorMesh.Region;
import dev.wildercord.aura.ArticulatedArmorMesh.SourceFace;
import dev.wildercord.aura.ArticulatedArmorMesh.SourceVertex;
import dev.wildercord.aura.ArticulatedCombatPose.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.wildercord.aura.ArticulatedCombatPose.*;
import static org.junit.jupiter.api.Assertions.*;

/** Pure topology regression for the original .55px aura shell; actual bake/UV checks are separate. */
class ArticulatedAuraShellMeshTest {
	@Test
	void subdivisionsRemainClosedExteriorFacesWithoutNewInternalCaps() {
		for (boolean slim : new boolean[] {false, true}) for (Region region : Region.values()) {
			var source = shell(region, slim);
			var mesh = ArticulatedArmorMesh.bakeAuraShell(source);
			var bind = mesh.deform((joint, point) -> point);
			Map<String, Integer> edges = new HashMap<>();
			for (var face : mesh.faces()) {
				var original = source.get(face.sourceFace());
				Vec3 normal = face.bindNormal(), origin = original.vertices().getFirst().position();
				for (int i = 0; i < 4; i++) {
					var corner = face.corners().get(i);
					Vec3 p = bind.positions().get(corner.controlPoint());
					assertEquals(0, p.minus(origin).dot(normal), .00001, "Subdivision left its original exterior plane");
					assertTrue(corner.u() >= 0 && corner.u() <= 1 && corner.v() >= 0 && corner.v() <= 1);
					int a = corner.controlPoint(), b = face.corners().get((i + 1) % 4).controlPoint();
					edges.merge(Math.min(a, b) + ":" + Math.max(a, b), 1, Integer::sum);
				}
			}
			assertTrue(edges.values().stream().allMatch(count -> count == 2), "All subdivided UV seams share a closed edge, with no caps or overlap");
		}
	}

	@Test
	void shellTrianglesStayOutwardAcrossAllPlayerFormsHandsAndWidths() {
		for (boolean slim : new boolean[] {false, true}) for (Region region : Region.values()) {
			var mesh = ArticulatedArmorMesh.bakeAuraShell(shell(region, slim));
			for (int move : new int[] {SPELLCUT, RISING_BREAK, DRIVING_CUT}) for (boolean left : new boolean[] {false, true}) {
				var rule = MastersArtRules.move(move);
				for (float age = 0; age <= rule.windup() + rule.recovery(); age += .25F) {
					var pose = samplePlayer(move, age, rule.windup(), rule.recovery(), left);
					var deformed = mesh.deform((joint, p) -> pose.world(joint).multiply(NONE.world(joint).inverseRigid()).transform(p));
					for (var face : mesh.faces()) {
						Vec3 expected = Vec3.ZERO;
						for (var c : face.corners()) {
							var p = mesh.controlPoints().get(c.controlPoint());
							expected = expected.plus(pose.world(p.first()).direction(face.bindNormal())
								.toward(pose.world(p.second()).direction(face.bindNormal()), p.secondWeight()));
						}
						Vec3 a = deformed.positions().get(face.corners().getFirst().controlPoint());
						for (int triangle = 1; triangle <= 2; triangle++) {
							Vec3 b = deformed.positions().get(face.corners().get(triangle).controlPoint());
							Vec3 c = deformed.positions().get(face.corners().get(triangle + 1).controlPoint());
							float dot = b.minus(a).cross(c.minus(a)).dot(expected);
							assertTrue(Float.isFinite(dot) && dot > 0, "Folded shell: " + region + " move=" + move + " age=" + age + " slim=" + slim + " left=" + left);
						}
					}
				}
			}
		}
	}

	@Test
	void shellCapGuardDoesNotChangeArmorOrBindGeometry() {
		var source = shell(Region.RIGHT_ARM, false);
		var armor = ArticulatedArmorMesh.bake(source);
		var shell = ArticulatedArmorMesh.bakeAuraShell(source);
		assertEquals(armor.deform((joint, p) -> p), shell.deform((joint, p) -> p), "Original bind dimensions stay identical");
		var rule = MastersArtRules.move(DRIVING_CUT);
		var pose = samplePlayer(DRIVING_CUT, 3.875F, rule.windup(), rule.recovery(), false);
		ArticulatedArmorMesh.SkinTransform palette = (joint, p) -> pose.world(joint).multiply(NONE.world(joint).inverseRigid()).transform(p);
		var original = armor.deform(palette); var cleared = shell.deform(palette);
		assertNotEquals(original.positions(), cleared.positions(), "Thin-shell sleeve clearance must be enabled explicitly");
		for (int i = 0; i < armor.controlPoints().size(); i++) {
			var p = armor.controlPoints().get(i);
			if (p.first() == p.second()) assertEquals(original.positions().get(i), cleared.positions().get(i), "Rigid sections keep original stand-off");
		}
	}

	private static List<SourceFace> shell(Region region, boolean slim) {
		boolean arm = region.arm(), left = region == Region.LEFT_ARM || region == Region.LEFT_LEG, head = region == Region.HEAD, body = region == Region.BODY;
		float grow = .55F, width = head || body ? 8 : arm && slim ? 3 : 4;
		float x = head || body ? -4 : arm ? (left ? 5 - 1 : -5 + (slim ? -2 : -3)) : (left ? 1.9F : -1.9F) - 2;
		float y = head ? -8 : body || arm ? 0 : 12, z = head ? -4 : -2, height = head ? 8 : 12, depth = head ? 8 : 4;
		float lo = x - grow, hi = x + width + grow, top = y - grow, bottom = y + height + grow, near = z - grow, far = z + depth + grow;
		Vec3[] p = {new Vec3(lo, top, near), new Vec3(hi, top, near), new Vec3(hi, bottom, near), new Vec3(lo, bottom, near),
			new Vec3(lo, top, far), new Vec3(hi, top, far), new Vec3(hi, bottom, far), new Vec3(lo, bottom, far)};
		List<SourceFace> out = new ArrayList<>();
		for (int[] indices : new int[][] {{1, 0, 3, 2}, {4, 5, 6, 7}, {0, 4, 7, 3}, {5, 1, 2, 6}, {5, 4, 0, 1}, {2, 3, 7, 6}})
			out.add(new SourceFace(region, List.of(new SourceVertex(p[indices[0]], 0, 0), new SourceVertex(p[indices[1]], 1, 0),
				new SourceVertex(p[indices[2]], 1, 1), new SourceVertex(p[indices[3]], 0, 1))));
		return out;
	}
}
