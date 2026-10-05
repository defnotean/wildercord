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

/** Focused regression for stock-sized leg collars; the official runtime-bake audit remains separate. */
class ArticulatedSharedPlayerArmorTest {
	@Test
	void bothSharedArtsKeepStockLegCollarTrianglesOutward() {
		for (int move : new int[] {RISING_BREAK, DRIVING_CUT}) for (boolean left : new boolean[] {false, true})
			for (Region region : new Region[] {Region.RIGHT_LEG, Region.LEFT_LEG}) for (float grow : new float[] {.4F, .9F}) {
				var mesh = ArticulatedArmorMesh.bake(stockLeg(region, grow));
				var rule = MastersArtRules.move(move);
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
}
