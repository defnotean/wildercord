package dev.wildercord.aura;

import dev.wildercord.aura.world.MasterAnimationRules;
import dev.wildercord.aura.world.StoneMarchRules;
import org.junit.jupiter.api.Test;

import static dev.wildercord.aura.ArticulatedCombatPose.*;
import static org.junit.jupiter.api.Assertions.*;

/** Pure choreography and attachment contracts. Native silhouettes and runtime layers need client captures. */
class ArticulatedStoneFaultMarchPoseTest {
	private static final float EPS = .001F;
	private static final Vec3 BLADE = new Vec3(0, .17364818F, -.98480775F);

	@Test
	void nextNpcIdUsesTheThreeSharedPulseTicksAndNeverEntersThePlayerNamespace() {
		assertEquals(10, MASTER_STONE_FAULT_MARCH);
		assertEquals(MasterAnimationRules.STONE_FAULT_MARCH, MASTER_STONE_FAULT_MARCH);
		assertTrue(supportsMaster(10));
		assertFalse(supportsPlayer(10));
		assertSame(NONE, samplePlayer(10, 32, 32, 64, false));
		assertEquals(Phase.WINDUP, sample(StoneMarchRules.TELL - .001F, false).phase());
		for (int pulse : new int[] {StoneMarchRules.TELL, StoneMarchRules.SECOND, StoneMarchRules.THIRD}) {
			assertEquals(Phase.ACTIVE, sample(pulse, false).phase());
			assertEquals(Phase.ACTIVE, sample(pulse + .999F, false).phase());
			assertEquals(Phase.RECOVERY, sample(pulse + 1, false).phase());
			assertEquals(1, sample(pulse, false).weight());
		}
		assertEquals(1, sample(StoneMarchRules.THIRD + StoneMarchRules.RECOVERY * .5F, false).weight());
		assertTrue(sample(90, false).weight() > 0);
		assertSame(NONE, sample(StoneMarchRules.END, false));
	}

	@Test
	void compatibilityWindowsCannotRetimeTheCommittedExecutorClock() {
		for (int tell : new int[] {1, 32, 80}) for (int active : new int[] {1, 10}) for (int recovery : new int[] {1, 63, 120})
			for (float age : new float[] {0, 12, 24, 31.99F, 32, 40, 48, 72, 95.99F, 96}) {
				Pose a = sample(age, false), b = sampleMaster(10, age, tell, active, recovery, false);
				assertEquals(a.phase(), b.phase()); assertEquals(a.weight(), b.weight());
				for (Joint joint : Joint.values()) matrix(a.world(joint), b.world(joint), 0);
			}
	}

	@Test
	void invalidAndCancelledClipsCannotRetainAHandOrPose() {
		for (float age : new float[] {-1, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 96, 200})
			assertSame(NONE, sample(age, false));
		for (int[] timing : new int[][] {{0, 1, 63}, {81, 1, 63}, {32, 0, 63}, {32, 11, 63}, {32, 1, 0}, {32, 1, 121}})
			assertSame(NONE, sampleMaster(10, 20, timing[0], timing[1], timing[2], false));
		assertSame(NONE, sampleMaster(0, 40, 32, 1, 63, false));
		for (Joint joint : Joint.values()) {
			matrix(NONE.world(joint), sample(0, false).world(joint), EPS);
			matrix(NONE.world(joint), sample(95.999F, false).world(joint), EPS);
		}
	}

	@Test
	void overheadStrikeAndBracedShocksAreDistinctFromTheEarlierStoneReply() {
		Pose overhead = sample(24, false), strike = sample(32, false), brace = sample(36, false), recoil = sample(40, false), extract = sample(72, false);
		Vec3 high = overhead.socket(false).transform(0, 0, 0), low = strike.socket(false).transform(0, 0, 0);
		assertTrue(high.y() < 0, "The warning raises the hilt above the head");
		assertTrue(low.y() > high.y() + 12, "The first pulse coincides with one committed downward stroke");
		assertTrue(overhead.socket(false).direction(BLADE).y() < -.97F, "The overhead blade points up");
		for (float age = 32; age <= 56; age += .125F) {
			Pose posed = sample(age, false);
			assertTrue(posed.socket(false).direction(BLADE).y() > .97F, "Later pulses keep the blade sunk instead of repeating the attack: " + age);
			assertTrue(posed.socket(false).transform(0, 0, 0).y() > 10, "The blade stays low through both transmitted shocks");
		}
		assertTrue(recoil.local(Joint.PELVIS).y() > brace.local(Joint.PELVIS).y() + .2F, "The pulse compresses the brace");
		assertTrue(extract.socket(false).transform(0, 0, 0).y() < brace.socket(false).transform(0, 0, 0).y() - 1,
			"Recovery slowly lifts the embedded blade");
		assertNotEquals(strike.local(Joint.LEFT_UPPER_ARM), brace.local(Joint.LEFT_UPPER_ARM));
		for (int other : new int[] {1, 7, 8, 9}) {
			Pose old = sampleMaster(other, 32, 32, 1, 63, false);
			assertTrue(low.minus(old.socket(false).transform(0, 0, 0)).length() > 1);
			assertNotEquals(strike.local(Joint.CHEST), old.local(Joint.CHEST));
		}
	}

	@Test
	void fullClockKeepsBothSolesPlantedHeadingFixedBonesRigidAndSocketsOnTheirHands() {
		for (float age = 0; age < StoneMarchRules.END; age += .125F) {
			Pose right = sample(age, false), left = sample(age, true);
			assertFalse(masterFootwork(10, age, 32));
			assertEquals(0, right.local(Joint.PELVIS).x());
			assertEquals(0, right.world(Joint.PELVIS).get(0, 2), EPS, "No model-root yaw replaces the accepted heading");
			for (Joint joint : Joint.values()) {
				for (float value : right.world(joint).values()) assertTrue(Float.isFinite(value) && Math.abs(value) < 32);
				for (Vec3 p : new Vec3[] {Vec3.ZERO, new Vec3(1, 2, -3)}) {
					Vec3 a = right.world(joint).transform(p), b = left.world(joint.opposite()).transform(-p.x(), p.y(), p.z());
					point(new Vec3(-a.x(), a.y(), a.z()), b);
				}
				if (joint.parent() != null) assertEquals(joint.bind().translation().length(),
					right.world(joint).transform(0, 0, 0).minus(right.world(joint.parent()).transform(0, 0, 0)).length(), EPS);
			}
			for (Joint foot : new Joint[] {Joint.RIGHT_FOOT, Joint.LEFT_FOOT}) {
				Matrix world = right.world(foot);
				assertEquals(22, world.transform(0, 0, 0).y(), EPS);
				point(new Vec3(0, 1, 0), world.direction(new Vec3(0, 1, 0)));
				for (float x : new float[] {-2, 2}) for (float z : new float[] {-2, 2})
					assertEquals(24, world.transform(x, 2, z).y(), EPS);
			}
			if (age >= 12 && age <= 72) {
				point(new Vec3(-2.65F, 22, 1.7F), right.world(Joint.RIGHT_FOOT).transform(0, 0, 0));
				point(new Vec3(2.65F, 22, -1.7F), right.world(Joint.LEFT_FOOT).transform(0, 0, 0));
			}
			for (boolean handed : new boolean[] {false, true}) {
				Pose pose = handed ? left : right;
				Joint hand = handed ? Joint.LEFT_HAND : Joint.RIGHT_HAND;
				point(pose.world(hand).transform(0, 1, 0), pose.socket(handed).transform(0, 0, 0));
				Rotation wrist = pose.local(hand).rotation();
				assertTrue(Math.abs(wrist.x()) <= .1801F && Math.abs(wrist.y()) <= .1501F && Math.abs(wrist.z()) <= .1801F, "Wrist at " + age + ": " + wrist);
			}
		}
	}

	@Test
	void eachGatherImpactRecoilExtractionAndResetBoundaryIsContinuous() {
		for (float edge : new float[] {0, 12, 24, 32, 33, 36, 40, 41, 44, 48, 49, 56, 72, 96})
			for (boolean left : new boolean[] {false, true}) {
				Pose before = sample(edge - .0001F, left), after = sample(edge + .0001F, left);
				for (Joint joint : Joint.values()) matrix(before.world(joint), after.world(joint), .003F);
			}
	}

	private static Pose sample(float age, boolean left) { return sampleMaster(10, age, 32, 1, 63, left); }
	private static void point(Vec3 a, Vec3 b) { assertEquals(a.x(), b.x(), EPS); assertEquals(a.y(), b.y(), EPS); assertEquals(a.z(), b.z(), EPS); }
	private static void matrix(Matrix a, Matrix b, float tolerance) {
		for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++) assertEquals(a.get(row, col), b.get(row, col), tolerance);
	}
}
