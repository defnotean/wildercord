package dev.wildercord.aura;

import dev.wildercord.aura.world.EmberKilnRules;
import dev.wildercord.aura.world.MasterAnimationRules;
import org.junit.jupiter.api.Test;

import static dev.wildercord.aura.ArticulatedCombatPose.*;
import static org.junit.jupiter.api.Assertions.*;

/** Pure authored rig and clock checks; native silhouette and held-layer proof need client captures. */
class ArticulatedEmberKilnPoseTest {
	private static final float EPS = .001F;
	private static final int TELL = 40, ACTIVE = 1, RECOVERY = 47;
	// Direction of the vanilla handheld sword from hilt to tip after the third-person display
	// and the renderer's Rx(-90), Ry(180). Socket tests use that blade, not the forearm axis.
	private static final Vec3 BLADE = new Vec3(0, .17364818F, -.98480775F);

	@Test
	void onlyTheNinthNpcIdIsAddedAndItsSinglePulseMatchesTheServerClock() {
		assertEquals(TELL, EmberKilnRules.TELL);
		assertEquals(RECOVERY + ACTIVE, EmberKilnRules.RECOVERY);
		assertEquals(MasterAnimationRules.KILN_RING, MASTER_EMBER_KILN_RING);
		assertTrue(supportsMaster(9));
		// Player ID 9 is Void Cut; the namespaces stay separate, so the NPC clip never leaks into it.
		assertTrue(supportsPlayer(9));
		assertNotEquals(sampleMaster(9, TELL, TELL, ACTIVE, RECOVERY, false).local(Joint.RIGHT_UPPER_ARM),
			samplePlayer(9, 6, 6, 12, false).local(Joint.RIGHT_UPPER_ARM));
		for (int move : new int[] {-1, 0, 2, 3, 4, 5, 6, 11, Integer.MAX_VALUE}) {
			assertFalse(supportsMaster(move));
			assertSame(NONE, sampleMaster(move, 20, TELL, ACTIVE, RECOVERY, false));
		}
		assertEquals(Phase.WINDUP, sample(TELL - .001F, false).phase());
		assertEquals(Phase.ACTIVE, sample(TELL, false).phase());
		assertEquals(Phase.ACTIVE, sample(TELL + .999F, false).phase());
		assertEquals(Phase.RECOVERY, sample(TELL + 1, false).phase());
		assertEquals(1, sample(TELL, false).weight());
		assertTrue(sample(65, false).weight() > .4F);
		assertSame(NONE, sample(TELL + ACTIVE + RECOVERY, false));
	}

	@Test
	void cancellationIdleAndInvalidInputsCannotRetainTheTurnOrBlade() {
		for (float age : new float[] {-1, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 88, 300})
			assertSame(NONE, sample(age, false));
		for (int[] timing : new int[][] {{0, 1, 47}, {81, 1, 47}, {40, 0, 47}, {40, 11, 47}, {40, 1, 0}, {40, 1, 121}})
			assertSame(NONE, sampleMaster(9, 10, timing[0], timing[1], timing[2], false));
		assertTrue(sample(20, false).weight() > 0);
		assertSame(NONE, sampleMaster(0, 20, TELL, ACTIVE, RECOVERY, false));
		assertEquals(0, sample(0, false).weight());
		for (Joint joint : Joint.values()) {
			matrix(NONE.world(joint), sample(0, false).world(joint), EPS);
			matrix(NONE.world(joint), sample(87.999F, false).world(joint), EPS);
		}
	}

	@Test
	void wholePelvisCompletesOneTurnBeforeReleaseAndDoesNotUnwindInRecovery() {
		for (boolean left : new boolean[] {false, true}) {
			float previous = heading(sample(8, left)), total = 0;
			for (float age = 8.125F; age <= 32; age += .125F) {
				Pose pose = sample(age, left);
				float heading = heading(pose), delta = wrap(heading - previous);
				assertTrue(left ? delta >= -.0001F : delta <= .0001F, "The planted coil has a single turn direction");
				total += delta; previous = heading;
				assertEquals(0, pose.local(Joint.PELVIS).x(), EPS);
				assertEquals(0, pose.local(Joint.PELVIS).z(), EPS);
			}
			assertEquals((left ? 1 : -1) * 2 * Math.PI, total, .001F, "Full-body turn cannot collapse to a shortest arc");
		}
		for (float age = 32; age < 88; age += .125F)
			assertEquals(-2 * Math.PI, MasterAnimationRules.kilnTurn(age, TELL), EPS, "Recovery keeps completed root yaw neutral");
	}

	@Test
	void oneFootSupportsEveryPivotAndBothSettleBeforeTheRadialIgnition() {
		boolean rightLift = false, leftLift = false;
		for (float age = 0; age < 88; age += .125F) {
			Pose pose = sample(age, false);
			Vec3 right = pose.world(Joint.RIGHT_FOOT).transform(0, 0, 0), left = pose.world(Joint.LEFT_FOOT).transform(0, 0, 0);
			assertTrue(Math.abs(right.y() - 22) < EPS || Math.abs(left.y() - 22) < EPS, "A support foot remains on the ground at " + age);
			rightLift |= right.y() < 21.3F; leftLift |= left.y() < 21.3F;
			for (Joint foot : new Joint[] {Joint.RIGHT_FOOT, Joint.LEFT_FOOT}) {
				Matrix world = pose.world(foot);
				point(new Vec3(0, 1, 0), world.direction(new Vec3(0, 1, 0)));
				for (float x : new float[] {-2, 2}) for (float z : new float[] {-2, 2})
					assertTrue(world.transform(x, 2, z).y() <= 24 + EPS, "A sole cannot sink below ground");
			}
			assertFalse(masterFootwork(9, age, TELL), "Visual pivots never authorize entity locomotion");
		}
		assertTrue(rightLift && leftLift, "Both feet take alternating small clearance steps");
		for (float age = 32; age <= 44; age += .125F) {
			Pose pose = sample(age, false);
			point(new Vec3(-2.6F, 22, 1.1F), pose.world(Joint.RIGHT_FOOT).transform(0, 0, 0));
			point(new Vec3(2.6F, 22, -1.1F), pose.world(Joint.LEFT_FOOT).transform(0, 0, 0));
		}
	}

	@Test
	void circularSwordStaysLowAndItsGripFollowsTheHandThroughIndependentElbowRelease() {
		Pose coil = sample(32, false), pulse = sample(40, false), follow = sample(44, false);
		assertTrue(coil.socket(false).transform(0, 0, 0).x() < -6, "Low warning is held outside the rear hip");
		assertTrue(pulse.socket(false).transform(0, 0, 0).x() > 1, "Release carries the blade across the body");
		assertTrue(coil.local(Joint.RIGHT_FOREARM).rotation().x() < -.39F);
		assertTrue(pulse.local(Joint.RIGHT_FOREARM).rotation().x() > -.30F);
		for (Pose pose : new Pose[] {coil, pulse, follow}) {
			assertTrue(pose.socket(false).transform(0, 0, 0).y() > 11, "Kiln has a low grip, never an overhead strike");
			assertEquals(0, pose.socket(false).direction(BLADE).y(), EPS, "Authored blade lies in the ground-parallel release plane");
		}
		assertTrue(pulse.socket(false).direction(BLADE).x() > .9F);
		for (int other : new int[] {1, 7, 8}) {
			Pose old = sampleMaster(other, TELL, TELL, ACTIVE, RECOVERY, false);
			assertTrue(pulse.socket(false).transform(0, 0, 0).minus(old.socket(false).transform(0, 0, 0)).length() > 2);
			assertNotEquals(pulse.local(Joint.CHEST), old.local(Joint.CHEST));
		}
	}

	@Test
	void allFramesRemainFiniteMirrorExactlyKeepBoneLengthsAndAvoidFoldedWrists() {
		for (float age = 0; age < 88; age += .125F) {
			Pose right = sample(age, false), left = sample(age, true);
			for (Joint joint : Joint.values()) {
				for (float value : right.world(joint).values()) assertTrue(Float.isFinite(value));
				for (Vec3 p : new Vec3[] {Vec3.ZERO, new Vec3(1, 2, -3)}) {
					Vec3 a = right.world(joint).transform(p), b = left.world(joint.opposite()).transform(-p.x(), p.y(), p.z());
					point(new Vec3(-a.x(), a.y(), a.z()), b);
				}
				if (joint.parent() != null) assertEquals(joint.bind().translation().length(),
					right.world(joint).transform(0, 0, 0).minus(right.world(joint.parent()).transform(0, 0, 0)).length(), EPS);
			}
			for (boolean handed : new boolean[] {false, true}) {
				Pose pose = handed ? left : right;
				Joint hand = handed ? Joint.LEFT_HAND : Joint.RIGHT_HAND;
				point(pose.world(hand).transform(0, 1, 0), pose.socket(handed).transform(0, 0, 0));
				Rotation wrist = pose.local(hand).rotation();
				assertTrue(Math.abs(wrist.x()) <= .1801F && Math.abs(wrist.y()) <= .1501F && Math.abs(wrist.z()) <= .1801F);
			}
		}
	}

	@Test
	void everyPivotAndBeatBoundaryIsContinuousIncludingValidExtremeWindows() {
		for (int tell : new int[] {1, TELL, 80}) for (int active : new int[] {1, 10}) for (int recovery : new int[] {1, RECOVERY, 120}) {
			float gather = tell * .2F, release = tell * .8F;
			for (int step = 0; step <= 8; step++) checkEdge(gather + (release - gather) * step / 8, tell, active, recovery);
			for (float edge : new float[] {0, tell, tell + active, tell + active + Math.min(3, recovery * .2F), tell + active + recovery})
				checkEdge(edge, tell, active, recovery);
		}
	}

	private static void checkEdge(float edge, int tell, int active, int recovery) {
		for (boolean left : new boolean[] {false, true}) {
			Pose before = sampleMaster(9, edge - .00001F, tell, active, recovery, left);
			Pose after = sampleMaster(9, edge + .00001F, tell, active, recovery, left);
			for (Joint joint : Joint.values()) matrix(before.world(joint), after.world(joint), .035F);
		}
	}
	private static Pose sample(float age, boolean left) { return sampleMaster(9, age, TELL, ACTIVE, RECOVERY, left); }
	private static float heading(Pose pose) { return (float) Math.atan2(pose.world(Joint.PELVIS).get(0, 2), pose.world(Joint.PELVIS).get(2, 2)); }
	private static float wrap(float angle) { return (float) Math.atan2(Math.sin(angle), Math.cos(angle)); }
	private static void point(Vec3 a, Vec3 b) { assertEquals(a.x(), b.x(), EPS); assertEquals(a.y(), b.y(), EPS); assertEquals(a.z(), b.z(), EPS); }
	private static void matrix(Matrix a, Matrix b, float tolerance) {
		for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++) assertEquals(a.get(row, col), b.get(row, col), tolerance);
	}
}
