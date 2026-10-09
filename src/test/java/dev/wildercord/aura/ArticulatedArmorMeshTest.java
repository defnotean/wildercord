package dev.wildercord.aura;

import dev.wildercord.aura.ArticulatedArmorMesh.*;
import dev.wildercord.aura.ArticulatedCombatPose.Joint;
import dev.wildercord.aura.ArticulatedCombatPose.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.wildercord.aura.ArticulatedCombatPose.*;
import static org.junit.jupiter.api.Assertions.*;

class ArticulatedArmorMeshTest {
	private static final float EPS = .00004F;

	@Test
	void everyOriginalExteriorIsPartitionedWithoutNewCapsOrUvArea() {
		for (Region region : Region.values()) {
			var original = box(region);
			Mesh mesh = ArticulatedArmorMesh.bake(original);
			for (int i = 0; i < original.size(); i++) {
				float actual = 0;
				for (var face : mesh.faces()) if (face.sourceFace() == i) {
					actual += uvArea(face.corners());
					for (var corner : face.corners()) {
						Vec3 position = mesh.controlPoints().get(corner.controlPoint()).bindPosition();
					assertEquals(plane(original.get(i).vertices().get(0).position(), face.bindNormal()), plane(position, face.bindNormal()), EPS);
						assertTrue(corner.u() >= .125F && corner.u() <= .25F);
						assertTrue(corner.v() >= .5F && corner.v() <= 1);
					}
				}
				assertEquals(.0625F, actual, EPS, "Original UV area is conserved, source " + i);
			}
			long caps = mesh.faces().stream().filter(face -> Math.abs(face.bindNormal().y()) > .9F).count();
			assertEquals(2, caps, "Only the two original exterior end caps remain");
		}
	}

	@Test
	void seamVerticesAreWeldedEvenAcrossUvSeamsAndEveryEdgeHasExactlyTwoFaces() {
		for (Region region : Region.values()) {
			Mesh mesh = ArticulatedArmorMesh.bake(box(region));
			Map<String, Integer> edges = new HashMap<>();
			for (var face : mesh.faces()) for (int i = 0; i < 4; i++) {
				int a = face.corners().get(i).controlPoint(), b = face.corners().get((i + 1) % 4).controlPoint();
				edges.merge(Math.min(a, b) + ":" + Math.max(a, b), 1, Integer::sum);
			}
			assertTrue(edges.values().stream().allMatch(count -> count == 2));
			assertEquals(mesh.controlPoints().size(), mesh.controlPoints().stream().map(ControlPoint::bindPosition).distinct().count());
		}
	}

	@Test
	void bindIsExactAndJointInfluencesAreNormalizedAndAttachedToCorrectRegion() {
		for (Region region : Region.values()) {
			Mesh mesh = ArticulatedArmorMesh.bake(box(region));
			var output = mesh.deform((joint, p) -> p);
			for (int i = 0; i < mesh.controlPoints().size(); i++) {
				var control = mesh.controlPoints().get(i);
				assertEquals(control.bindPosition(), output.positions().get(i));
				assertTrue(control.secondWeight() >= 0 && control.secondWeight() <= 1);
				assertNotNull(control.first()); assertNotNull(control.second());
			}
			for (int i = 0; i < mesh.faces().size(); i++) assertEquals(1, mesh.faces().get(i).bindNormal().dot(output.normals().get(i)), EPS);
		}
	}

	@Test
	void denseTwoHandedPalettesAreFiniteAndFrameOrderCannotMutateEarlierOutputs() {
		for (Region region : Region.values()) {
			Mesh mesh = ArticulatedArmorMesh.bake(box(region));
			var bind = mesh.deform((joint, p) -> p);
			for (boolean left : new boolean[] {false, true}) for (float age = 0; age < 16; age += .125F) {
				var pose = sampleSpellcut(0, age, 4, 12, left);
				Deformed first = deform(mesh, pose), second = deform(mesh, sampleSpellcut(0, 4, 4, 12, !left));
				assertEquals(first, deform(mesh, pose));
				for (Vec3 p : first.positions()) assertTrue(Float.isFinite(p.x()) && Float.isFinite(p.y()) && Float.isFinite(p.z()));
				for (Vec3 n : second.normals()) assertEquals(1, n.length(), EPS);
			}
			assertEquals(bind, mesh.deform((joint, p) -> p));
			assertThrows(UnsupportedOperationException.class, () -> mesh.faces().clear());
			assertThrows(UnsupportedOperationException.class, () -> mesh.controlPoints().clear());
			assertThrows(UnsupportedOperationException.class, () -> bind.positions().clear());
		}
	}

	@Test
	void malformedOrNonFiniteSourceAndPaletteFailClosed() {
		assertThrows(IllegalArgumentException.class, () -> new SourceVertex(new Vec3(Float.NaN, 0, 0), 0, 0));
		assertThrows(IllegalArgumentException.class, () -> new SourceFace(Region.BODY, List.of()));
		Mesh mesh = ArticulatedArmorMesh.bake(box(Region.BODY));
		assertThrows(IllegalArgumentException.class, () -> mesh.deform((joint, p) -> new Vec3(0, Float.POSITIVE_INFINITY, 0)));
		assertThrows(IllegalArgumentException.class, () -> mesh.deform((joint, p) -> Vec3.ZERO));
	}

	private static Deformed deform(Mesh mesh, Pose pose) {
		return mesh.deform((joint, p) -> pose.world(joint).multiply(NONE.world(joint).inverseRigid()).transform(p));
	}
	private static float plane(Vec3 p, Vec3 normal) { return p.dot(normal); }
	private static float uvArea(List<Corner> corners) {
		float area = 0;
		for (int i = 0; i < corners.size(); i++) { var a = corners.get(i); var b = corners.get((i + 1) % corners.size()); area += a.u() * b.v() - b.u() * a.v(); }
		return Math.abs(area) / 2;
	}
	private static List<SourceFace> box(Region region) {
		float top = region == Region.HEAD ? -9 : region == Region.RIGHT_LEG || region == Region.LEFT_LEG ? 11 : -1;
		float bottom = region == Region.HEAD ? 1 : top + 14;
		Vec3[] p = {new Vec3(-3, top, -3), new Vec3(3, top, -3), new Vec3(3, bottom, -3), new Vec3(-3, bottom, -3),
			new Vec3(-3, top, 3), new Vec3(3, top, 3), new Vec3(3, bottom, 3), new Vec3(-3, bottom, 3)};
		List<SourceFace> faces = new ArrayList<>();
		for (int[] indices : new int[][] {{1, 0, 3, 2}, {4, 5, 6, 7}, {0, 4, 7, 3}, {5, 1, 2, 6}, {5, 4, 0, 1}, {2, 3, 7, 6}}) {
			faces.add(new SourceFace(region, List.of(new SourceVertex(p[indices[0]], .125F, .5F), new SourceVertex(p[indices[1]], .25F, .5F),
				new SourceVertex(p[indices[2]], .25F, 1), new SourceVertex(p[indices[3]], .125F, 1))));
		}
		return faces;
	}
}
