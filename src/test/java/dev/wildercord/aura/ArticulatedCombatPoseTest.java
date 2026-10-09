package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import static dev.wildercord.aura.ArticulatedCombatPose.*;
import static org.junit.jupiter.api.Assertions.*;

class ArticulatedCombatPoseTest {
	private static final float EPS = .0003F;

	@Test
	void twentyJointBindHierarchyKeepsEveryAttachmentAtItsParentEndpoint() {
		assertEquals(20, Joint.values().length);
		for (Joint joint : Joint.values()) {
			assertEquals(joint, joint.opposite().opposite());
			assertEquals(joint.bind(), NONE.local(joint));
			if (joint.parent() != null) {
				assertTrue(joint.parent().ordinal() < joint.ordinal());
				point(NONE.world(joint.parent()).transform(joint.bind().translation()), NONE.world(joint).transform(0, 0, 0), EPS);
			}
		}
		point(new Vec3(0, 0, 0), NONE.world(Joint.HEAD).transform(0, 0, 0), EPS);
		point(new Vec3(-5, 2, 0), NONE.world(Joint.RIGHT_UPPER_ARM).transform(0, 0, 0), EPS);
		point(new Vec3(-5, 11, 0), NONE.socket(false).transform(0, 0, 0), EPS);
		point(new Vec3(-1.9F, 24, 0), NONE.world(Joint.RIGHT_FOOT).transform(0, 2, 0), EPS);
	}

	@Test
	void invalidUnsupportedCancelledAndExpiredTimelinesReturnExactBind() {
		for (float age : new float[] {-1, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 16, 500})
			assertSame(NONE, sampleSpellcut(0, age, 4, 12, false));
		for (int move : new int[] {-1, 1, 2, 14, Integer.MAX_VALUE}) assertSame(NONE, sampleSpellcut(move, 2, 4, 12, false));
		for (int[] timing : new int[][] {{0, 12}, {61, 12}, {4, 0}, {4, 121}, {Integer.MAX_VALUE, 1}})
			assertSame(NONE, sampleSpellcut(0, 2, timing[0], timing[1], false));
		for (int attack : new int[] {-1, 0, 2, 5, Integer.MAX_VALUE}) assertSame(NONE, sampleMaster(attack, 5, 18, 1, 19, false));
		for (int[] timing : new int[][] {{0, 1, 20}, {81, 1, 20}, {18, 0, 20}, {18, 11, 20}, {18, 1, 121}})
			assertSame(NONE, sampleMaster(1, 2, timing[0], timing[1], timing[2], false));
		assertSame(NONE, sampleMaster(1, 38, 18, 1, 19, false));
		// A cleared server attack cancels immediately; sampling is stateless and cannot keep a stale clip.
		assertTrue(sampleMaster(1, 18, 18, 1, 19, false).weight() > 0);
		assertSame(NONE, sampleMaster(0, 18, 18, 1, 19, false));
		assertSame(NONE, sampleSpellcut(-1, 4, 4, 12, true));
		assertEquals(0, sampleSpellcut(0, 0, 4, 12, false).weight());
		for (Joint joint : Joint.values()) assertEquals(joint.bind(), sampleSpellcut(0, 0, 4, 12, false).local(joint));
	}

	@Test
	void acceptedImpactTicksAndGameplayPhaseWindowsAreUnchanged() {
		assertEquals(Phase.WINDUP, sampleSpellcut(0, 3.99F, 4, 12, false).phase());
		assertEquals(Phase.ACTIVE, sampleSpellcut(0, 4, 4, 12, false).phase());
		assertEquals(Phase.RECOVERY, sampleSpellcut(0, 5, 4, 12, false).phase());
		assertEquals(Phase.WINDUP, sampleMaster(1, 17.99F, 18, 1, 19, false).phase());
		assertEquals(Phase.ACTIVE, sampleMaster(1, 18, 18, 1, 19, false).phase());
		assertEquals(Phase.ACTIVE, sampleMaster(1, 18.99F, 18, 1, 19, false).phase());
		assertEquals(Phase.RECOVERY, sampleMaster(1, 19, 18, 1, 19, false).phase());
		Pose cut = sampleSpellcut(0, 4, 4, 12, false), slowCut = sampleSpellcut(0, 31, 31, 58, false);
		Pose master = sampleMaster(1, 18, 18, 1, 19, false), slowMaster = sampleMaster(1, 57, 57, 7, 93, false);
		assertEquals(1, cut.weight()); assertEquals(1, master.weight());
		for (Joint joint : Joint.values()) {
			matrix(cut.world(joint), slowCut.world(joint), EPS);
			matrix(master.world(joint), slowMaster.world(joint), EPS);
		}
	}

	@Test
	void elbowsKneesHipsAndWristsActuallyArticulate() {
		for (Pose pose : new Pose[] {sampleSpellcut(0, 2.6F, 4, 12, false), sampleSpellcut(0, 4, 4, 12, false), sampleMaster(1, 18, 18, 1, 19, false)}) {
			assertTrue(pose.local(Joint.RIGHT_FOREARM).rotation().x() < -.35F);
			assertTrue(pose.local(Joint.LEFT_FOREARM).rotation().x() < -.7F);
			assertTrue(pose.local(Joint.RIGHT_SHIN).rotation().x() > .6F);
			assertTrue(pose.local(Joint.LEFT_SHIN).rotation().x() > .6F);
			assertTrue(Math.abs(pose.local(Joint.PELVIS).rotation().y()) > .15F);
			assertTrue(Math.abs(pose.local(Joint.CHEST).rotation().y()) > .15F);
			assertTrue(Math.abs(pose.local(Joint.RIGHT_HAND).rotation().z()) > .04F);
			assertTrue(Math.abs(pose.local(Joint.RIGHT_HAND).rotation().z()) < .2F);
			assertTrue(Math.abs(pose.local(Joint.RIGHT_SOCKET).rotation().z()) > .1F);
			point(pose.world(Joint.RIGHT_UPPER_ARM).transform(0, 4, 0), pose.world(Joint.RIGHT_FOREARM).transform(0, 0, 0), EPS);
			point(pose.world(Joint.RIGHT_FOREARM).transform(0, 4, 0), pose.world(Joint.RIGHT_HAND).transform(0, 0, 0), EPS);
			point(pose.world(Joint.RIGHT_HAND).transform(0, 1, 0), pose.socket(false).transform(0, 0, 0), EPS);
		}
		Pose chamber = sampleSpellcut(0, 2.6F, 4, 12, false), impact = sampleSpellcut(0, 4, 4, 12, false), follow = sampleSpellcut(0, 7, 4, 12, false);
		assertTrue(chamber.local(Joint.RIGHT_FOREARM).rotation().x() < impact.local(Joint.RIGHT_FOREARM).rotation().x());
		assertTrue(follow.local(Joint.RIGHT_FOREARM).rotation().x() < impact.local(Joint.RIGHT_FOREARM).rotation().x());
		assertTrue(chamber.local(Joint.PELVIS).rotation().y() > 0 && impact.local(Joint.PELVIS).rotation().y() < 0);
	}

	@Test
	void everyFrameKeepsBothSolesFlatAndLimbLengthsRigid() {
		for (boolean left : new boolean[] {false, true}) for (int clip = 0; clip < 2; clip++) {
			float end = clip == 0 ? 16 : 38;
			for (float age = 0; age < end; age += .03125F) {
				Pose pose = clip == 0 ? sampleSpellcut(0, age, 4, 12, left) : sampleMaster(1, age, 18, 1, 19, left);
				for (Joint foot : new Joint[] {Joint.RIGHT_FOOT, Joint.LEFT_FOOT}) {
					Matrix world = pose.world(foot);
					for (float x : new float[] {-2, 2}) for (float z : new float[] {-2, 2})
						assertEquals(24, world.transform(x, 2, z).y(), .0008F, "Sole plant at age " + age);
					assertEquals(22, world.transform(0, 0, 0).y(), .0008F);
					point(new Vec3(0, 1, 0), world.direction(new Vec3(0, 1, 0)), .0005F);
				}
				for (Joint joint : Joint.values()) {
					if (joint.parent() == null) continue;
					float expected = joint.bind().translation().length();
					float actual = pose.world(joint).transform(0, 0, 0).minus(pose.world(joint.parent()).transform(0, 0, 0)).length();
					assertEquals(expected, actual, .0006F, joint.name());
				}
			}
		}
	}

	@Test
	void plantTargetsDoNotSlideDuringTheCommittedCutAndFollowThrough() {
		for (float age = 2.6F; age <= 7; age += .05F) {
			Pose pose = sampleSpellcut(0, age, 4, 12, false);
			point(new Vec3(-2.4F, 22, 1.5F), pose.world(Joint.RIGHT_FOOT).transform(0, 0, 0), EPS);
			point(new Vec3(2.4F, 22, -1.7F), pose.world(Joint.LEFT_FOOT).transform(0, 0, 0), EPS);
		}
		for (float age = 11.7F; age <= 22; age += .05F) {
			Pose pose = sampleMaster(1, age, 18, 1, 19, false);
			point(new Vec3(-2.5F, 22, 1.8F), pose.world(Joint.RIGHT_FOOT).transform(0, 0, 0), EPS);
			point(new Vec3(2.5F, 22, -2), pose.world(Joint.LEFT_FOOT).transform(0, 0, 0), EPS);
		}
	}

	@Test
	void leftHandedMatricesAreExactReflectionsIncludingSocketsAndPlants() {
		for (float age = 0; age < 16; age += .17F) {
			Pose right = sampleSpellcut(0, age, 4, 12, false), left = sampleSpellcut(0, age, 4, 12, true);
			ViewPose rightView = view(right, false), leftView = view(left, true);
			for (Joint joint : Joint.values()) for (Vec3 p : new Vec3[] {Vec3.ZERO, new Vec3(1, 2, -3)}) {
				Vec3 a = right.world(joint).transform(p), b = left.world(joint.opposite()).transform(-p.x(), p.y(), p.z());
				point(new Vec3(-a.x(), a.y(), a.z()), b, EPS);
				Vec3 av = rightView.world(joint).transform(p), bv = leftView.world(joint.opposite()).transform(-p.x(), p.y(), p.z());
				point(new Vec3(-av.x(), av.y(), av.z()), bv, EPS);
			}
		}
	}

	@Test
	void cameraCompositionUsesIndependentArticulatedArmsAndNeverChangesCameraOrigin() {
		for (boolean left : new boolean[] {false, true}) for (float age = 0; age < 16; age += .025F) {
			Pose pose = sampleSpellcut(0, age, 4, 12, left);
			ViewPose camera = view(pose, left);
			assertEquals(new Vec3(0, -.7F, -.9F), camera.origin());
			assertEquals(Transform.IDENTITY, camera.local(Joint.PELVIS));
			assertEquals(Transform.IDENTITY, camera.local(Joint.SPINE));
			assertEquals(Transform.IDENTITY, camera.local(Joint.CHEST));
			for (Joint joint : new Joint[] {Joint.RIGHT_SHOULDER, Joint.RIGHT_FOREARM, Joint.RIGHT_HAND, Joint.RIGHT_SOCKET,
				Joint.LEFT_SHOULDER, Joint.LEFT_FOREARM, Joint.LEFT_HAND, Joint.LEFT_SOCKET}) {
				Vec3 p = camera.cameraPoint(joint, 0, 0, 0);
				assertTrue(p.z() < -.45F, "Articulated arms stay beyond the near plane: " + joint + " " + p);
			}
			Joint socket = left ? Joint.LEFT_SOCKET : Joint.RIGHT_SOCKET;
			Vec3 hilt = camera.cameraPoint(socket, 0, 0, 0);
			assertTrue(Math.abs(hilt.x()) < .65F, "Hilt remains in the peripheral view: " + hilt);
			assertTrue(hilt.y() > -.8F && hilt.y() < -.08F, "Hilt remains below the reticle: " + hilt);
		}
		ViewPose camera = view(sampleSpellcut(0, 4, 4, 12, false), false);
		Pose body = sampleSpellcut(0, 4, 4, 12, false);
		assertNotEquals(camera.local(Joint.RIGHT_UPPER_ARM), body.local(Joint.RIGHT_UPPER_ARM));
		assertNotEquals(camera.local(Joint.RIGHT_FOREARM), body.local(Joint.RIGHT_FOREARM));
		assertNotEquals(camera.local(Joint.RIGHT_HAND), body.local(Joint.RIGHT_HAND));
	}

	@Test
	void firstPersonGripAndGuardStayReadableAboveTheSurvivalHud() {
		// Canonical aim, native 70-degree hand projection, conservative 42-GUI-pixel HUD band.
		// The rendered mesh/armor and free-look grid are checked separately with actual polygons.
		for (boolean left : new boolean[] {false, true}) for (int step = 0; step <= 640; step++) {
			ViewPose camera = view(sampleSpellcut(0, step / 40F, 4, 12, left), left);
			for (Joint joint : new Joint[] {left ? Joint.LEFT_SOCKET : Joint.RIGHT_SOCKET,
				left ? Joint.RIGHT_HAND : Joint.LEFT_HAND}) {
				Vec3 p = camera.cameraPoint(joint, 0, 0, 0);
				double fractionY = .5 - p.y() / -p.z() / (2 * Math.tan(Math.toRadians(35)));
				assertTrue(fractionY > .6 && fractionY < .81, "Grip/guard remain below aim and above HUD: " + joint + " " + fractionY);
				for (int[] viewport : new int[][] {{854, 480, 2}, {1280, 720, 3}, {1280, 960, 4}, {1920, 810, 3}})
					assertTrue(fractionY * viewport[1] < viewport[1] - 42 * viewport[2], "HUD-safe wrist anchor");
			}
		}
	}

	@Test
	void firstPersonPaletteIsContinuousAtEveryClipAndIdleBoundary() {
		for (boolean left : new boolean[] {false, true})
			for (float boundary : new float[] {0, 2.6F, 4, 7, 16}) {
				ViewPose before = view(sampleSpellcut(0, boundary - .001F, 4, 12, left), left);
				ViewPose after = view(sampleSpellcut(0, boundary + .001F, 4, 12, left), left);
				assertEquals(before.origin(), after.origin());
				for (Joint joint : Joint.values()) matrix(before.world(joint), after.world(joint), .025F);
			}
	}

	@Test
	void phaseBoundariesAndBindEntryExitAreContinuousAndFinite() {
		for (float boundary : new float[] {0, 2.6F, 4, 7, 16}) {
			Pose before = sampleSpellcut(0, boundary - .001F, 4, 12, false);
			Pose after = sampleSpellcut(0, boundary + .001F, 4, 12, false);
			assertEquals(before.weight(), after.weight(), .003F);
			for (Joint joint : Joint.values()) matrix(before.world(joint), after.world(joint), .018F);
		}
		for (int tell : new int[] {1, 4, 60}) for (int recovery : new int[] {1, 12, 120}) {
			for (float age = 0; age < tell + recovery; age += .23F) {
				Pose pose = sampleSpellcut(0, age, tell, recovery, false);
				assertTrue(Float.isFinite(pose.weight()) && pose.weight() >= 0 && pose.weight() <= 1);
				for (Joint joint : Joint.values()) for (float value : pose.world(joint).values()) assertTrue(Float.isFinite(value));
			}
		}
	}

	@Test
	void masterBoundariesAndMirroredBindRemainContinuousAtExtremeValidTimings() {
		for (boolean left : new boolean[] {false, true}) {
			for (Joint joint : Joint.values()) assertEquals(joint.bind(), sampleMaster(1, 0, 18, 1, 19, left).local(joint));
			for (int tell : new int[] {1, 18, 80}) for (int active : new int[] {1, 2, 10}) for (int recovery : new int[] {1, 20, 120}) {
				float follow = tell + active + Math.min(3, recovery * .20F), end = tell + active + recovery;
				for (float boundary : new float[] {0, tell * .65F, tell, tell + active, follow, end}) {
					Pose before = sampleMaster(1, boundary - .0001F, tell, active, recovery, left);
					Pose after = sampleMaster(1, boundary + .0001F, tell, active, recovery, left);
					for (Joint joint : Joint.values()) matrix(before.world(joint), after.world(joint), .022F);
				}
			}
		}
	}

	@Test
	void quaternionInterpolationTakesTheShortArcAndMatricesStayRigidAndImmutable() {
		Rotation a = new Rotation((float) Math.PI - .1F, .02F, 0), b = new Rotation(-(float) Math.PI + .1F, .02F, 0);
		Matrix midpoint = new Transform(0, 0, 0, a.toward(b, .5F)).matrix();
		assertTrue(midpoint.transform(0, 1, 0).y() < -.99F, "Interpolate across pi, not through zero");
		for (float age = 0; age < 16; age += .19F) {
			Pose pose = sampleSpellcut(0, age, 4, 12, false);
			for (Joint joint : Joint.values()) {
				Matrix world = pose.world(joint);
				matrix(Matrix.identity(), world.multiply(world.inverseRigid()), .0005F);
				float original = world.get(0, 0); world.values()[0] = Float.NaN;
				assertEquals(original, world.get(0, 0));
			}
		}
	}

	@Test
	void freeLookClearanceIsCanonicalZeroBoundedMirroredAndSafeForNonFiniteInput() {
		assertEquals(0, viewClearance(0, 0));
		assertEquals(.125F, viewClearance(12.5F, 0), EPS);
		assertEquals(.125F, viewClearance(0, -10), EPS);
		assertEquals(.25F, viewClearance(25, -20), EPS);
		for (float yaw : new float[] {-180, -25, -17, 0, 17, 25, 180}) for (float pitch : new float[] {-90, -20, -7, 0, 7, 20, 90}) {
			assertEquals(viewClearance(yaw, pitch), viewClearance(-yaw, -pitch));
			assertTrue(viewClearance(yaw, pitch) >= 0 && viewClearance(yaw, pitch) <= .25F);
		}
		for (float value : new float[] {Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY}) {
			assertEquals(.25F, viewClearance(value, 0)); assertEquals(.25F, viewClearance(0, value));
		}
	}

	@Test
	void socketResidualPreservesAuthoredGripWhileTheHandAvoidsExcessiveWristFolding() {
		Pose body = sampleSpellcut(0, 4, 4, 12, false);
		Matrix hand = new Transform(0, 0, 0, body.local(Joint.RIGHT_HAND).rotation()).matrix();
		Matrix socket = new Transform(0, 0, 0, body.local(Joint.RIGHT_SOCKET).rotation()).matrix();
		matrix(new Transform(0, 0, 0, new Rotation(.12F, -.20F, .46F)).matrix(), hand.multiply(socket), EPS);
		ViewPose camera = view(body, false);
		hand = new Transform(0, 0, 0, camera.local(Joint.RIGHT_HAND).rotation()).matrix();
		socket = new Transform(0, 0, 0, camera.local(Joint.RIGHT_SOCKET).rotation()).matrix();
		matrix(new Transform(0, 0, 0, new Rotation(.12F, -.06F, .48F)).matrix(), hand.multiply(socket), EPS);
		assertEquals(new Vec3(0, 1, 0), body.local(Joint.RIGHT_SOCKET).translation());
		assertEquals(new Vec3(0, 1, 0), camera.local(Joint.RIGHT_SOCKET).translation());
	}

	private static void point(Vec3 expected, Vec3 actual, float tolerance) {
		assertEquals(expected.x(), actual.x(), tolerance, "x"); assertEquals(expected.y(), actual.y(), tolerance, "y"); assertEquals(expected.z(), actual.z(), tolerance, "z");
	}
	private static void matrix(Matrix expected, Matrix actual, float tolerance) {
		for (int row = 0; row < 4; row++) for (int column = 0; column < 4; column++)
			assertEquals(expected.get(row, column), actual.get(row, column), tolerance, "matrix[" + row + "," + column + "]");
	}
}
