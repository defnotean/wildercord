package dev.wildercord.aura;

import dev.wildercord.aura.ArticulatedArmorMesh.Region;
import dev.wildercord.aura.ArticulatedArmorMesh.SourceFace;
import dev.wildercord.aura.ArticulatedArmorMesh.SourceVertex;
import dev.wildercord.aura.ArticulatedCombatPose.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.wildercord.aura.ArticulatedCombatPose.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Full-clock collar orientation for stock armor and funded shells; runtime pixels remain a separate gate. */
class ArticulatedHailfallSkyfallArmorTest {
	@Test
	void targetFormsKeepStockLegCollarTrianglesOutward() {
		for (int move : new int[] {HAILFALL, SKYFALL}) for (boolean left : new boolean[] {false, true})
			for (Region region : new Region[] {Region.RIGHT_LEG, Region.LEFT_LEG}) for (float grow : new float[] {.4F, .9F}) {
				var mesh = ArticulatedArmorMesh.bake(stockLeg(region, grow));
				var rule = MastersStyleRules.animation(move);
				for (float age = 0; age <= rule.windup() + rule.recovery(); age += .125F) {
					var pose = samplePlayer(move, age, rule.windup(), rule.recovery(), left);
					var deformed = mesh.deform((joint, p) -> pose.world(joint).multiply(NONE.world(joint).inverseRigid()).transform(p));
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
							float dot = b.minus(origin).cross(c.minus(origin)).dot(expected);
							assertTrue(Float.isFinite(dot) && dot > 0, "Stock leg collar reverses: move=" + move + " age=" + age
								+ " left=" + left + " region=" + region + " grow=" + grow + " orientation=" + dot);
						}
					}
				}
			}
	}

	/** Same stock vanilla 4x12x4 leg extents, pivots and leggings/boots inflation as the runtime audit. */
	private static List<SourceFace> stockLeg(Region region, float grow) {
		float center = region == Region.LEFT_LEG ? 1.9F : -1.9F;
		float lo = center - 2 - grow, hi = center + 2 + grow, top = 12 - grow, bottom = 24 + grow, near = -2 - grow, far = 2 + grow;
		Vec3[] p = {new Vec3(lo, top, near), new Vec3(hi, top, near), new Vec3(hi, bottom, near), new Vec3(lo, bottom, near),
			new Vec3(lo, top, far), new Vec3(hi, top, far), new Vec3(hi, bottom, far), new Vec3(lo, bottom, far)};
		List<SourceFace> faces = new ArrayList<>();
		for (int[] indices : new int[][] {{1, 0, 3, 2}, {4, 5, 6, 7}, {0, 4, 7, 3}, {5, 1, 2, 6}, {5, 4, 0, 1}, {2, 3, 7, 6}})
			faces.add(new SourceFace(region, List.of(new SourceVertex(p[indices[0]], 0, 0), new SourceVertex(p[indices[1]], 1, 0),
				new SourceVertex(p[indices[2]], 1, 1), new SourceVertex(p[indices[3]], 0, 1))));
		return faces;
	}

	@Test
	void targetFormsKeepEveryStockArmorRegionOutwardForBothSkinWidths() {
		for (boolean slim : new boolean[] {false, true}) for (Region region : Region.values()) {
			var mesh = ArticulatedArmorMesh.bake(stockRegion(region, slim, .9F));
			for (int move : new int[] {HAILFALL, SKYFALL}) for (boolean left : new boolean[] {false, true}) {
				var rule = MastersStyleRules.animation(move);
				for (float age = 0; age <= rule.windup() + rule.recovery(); age += .125F)
					assertOutward(mesh, samplePlayer(move, age, rule.windup(), rule.recovery(), left),
						"Stock armor " + region + " move=" + move + " age=" + age + " slim=" + slim + " left=" + left);
			}
		}
	}

	private static void assertOutward(ArticulatedArmorMesh.Mesh mesh, Pose pose, String label) {
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
				assertTrue(Float.isFinite(dot) && dot > 0, label + " orientation=" + dot);
			}
		}
	}

	@Test
	void targetFormsKeepFundedShellTrianglesOutwardAcrossHandsAndWidths() {
		for (boolean slim : new boolean[] {false, true}) for (Region region : Region.values()) {
			var mesh = ArticulatedArmorMesh.bakeAuraShell(shell(region, slim));
			for (int move : new int[] {HAILFALL, SKYFALL}) for (boolean left : new boolean[] {false, true}) {
				var rule = MastersStyleRules.animation(move);
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


	private static List<SourceFace> shell(Region region, boolean slim) {
		return stockRegion(region, slim, .55F);
	}

	private static List<SourceFace> stockRegion(Region region, boolean slim, float grow) {
		boolean arm = region.arm(), left = region == Region.LEFT_ARM || region == Region.LEFT_LEG, head = region == Region.HEAD, body = region == Region.BODY;
		float width = head || body ? 8 : arm && slim ? 3 : 4;
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
