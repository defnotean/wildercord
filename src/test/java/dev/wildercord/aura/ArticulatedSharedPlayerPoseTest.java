package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import static dev.wildercord.aura.ArticulatedCombatPose.*;
import static org.junit.jupiter.api.Assertions.*;

/** Geometry/clock contracts only. Native body, armor and HUD pixels require their own captures. */
class ArticulatedSharedPlayerPoseTest {
	private static final float EPS = .0008F;
	private static final int[] MOVES = {RISING_BREAK, DRIVING_CUT};

	@Test
	void playerAndNpcOrdinalsStaySeparateAndSpellcutRemainsBitIdentical() {
		for (int move : new int[] {-1, 3, 7, 8, Integer.MAX_VALUE}) {
			assertFalse(supportsPlayer(move));
			assertSame(NONE, samplePlayer(move, 5, 8, 18, false));
		}
		assertTrue(supportsMaster(MASTER_SWEEP));
		assertFalse(supportsMaster(DRIVING_CUT));
		assertSame(NONE, sampleSpellcut(RISING_BREAK, 5, 8, 18, false));
		assertNotEquals(samplePlayer(RISING_BREAK, 8, 8, 18, false).local(Joint.RIGHT_UPPER_ARM),
			sampleMaster(MASTER_SWEEP, 8, 8, 1, 18, false).local(Joint.RIGHT_UPPER_ARM));
		for (boolean left : new boolean[] {false, true}) for (float age = -1; age <= 17; age += .125F) {
			Pose expected = sampleSpellcut(SPELLCUT, age, 4, 12, left), actual = samplePlayer(SPELLCUT, age, 4, 12, left);
			for (Joint joint : Joint.values()) assertArrayEquals(expected.world(joint).values(), actual.world(joint).values());
		}
	}

	@Test
	void acceptedWindowsReleaseExactlyOnceAndNeverExtendExpiry() {
		for (int move : MOVES) {
			var rule = MastersArtRules.move(move);
			assertEquals(move == RISING_BREAK ? 8 : 6, rule.windup());
			assertEquals(move == RISING_BREAK ? 18 : 14, rule.recovery());
			assertEquals(Phase.WINDUP, sample(move, rule.windup() - .001F, false).phase());
			assertEquals(Phase.ACTIVE, sample(move, rule.windup(), false).phase());
			assertEquals(Phase.ACTIVE, sample(move, rule.windup() + .999F, false).phase());
			assertEquals(Phase.RECOVERY, sample(move, rule.windup() + 1, false).phase());
			assertSame(NONE, sample(move, rule.windup() + rule.recovery(), false));
			for (float bad : new float[] {-1, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY})
				assertSame(NONE, sample(move, bad, false));
			for (int[] bad : new int[][] {{0, 18}, {61, 18}, {8, 0}, {8, 121}})
				assertSame(NONE, samplePlayer(move, 1, bad[0], bad[1], false));
			for (boolean left : new boolean[] {false, true}) for (Joint joint : Joint.values())
				assertEquals(joint.bind(), sample(move, 0, left).local(joint));
		}
	}

	@Test
	void risingUncoilsUpwardWhileDrivingExtendsForwardThenRecoils() {
		Pose low = sample(RISING_BREAK, 5.2F, false), high = sample(RISING_BREAK, 8, false);
		assertTrue(low.local(Joint.PELVIS).y() > high.local(Joint.PELVIS).y() + .8F);
		assertTrue(low.socket(false).transform(0, 0, 0).y() > high.socket(false).transform(0, 0, 0).y() + 12);
		assertTrue(low.local(Joint.RIGHT_SHIN).rotation().x() > high.local(Joint.RIGHT_SHIN).rotation().x());
		assertTrue(high.local(Joint.RIGHT_UPPER_ARM).rotation().x() < -2);
		Pose chamber = sample(DRIVING_CUT, 3.9F, false), point = sample(DRIVING_CUT, 6, false), recoil = sample(DRIVING_CUT, 9.5F, false);
		assertTrue(chamber.socket(false).transform(0, 0, 0).z() > point.socket(false).transform(0, 0, 0).z() + 8);
		assertTrue(recoil.socket(false).transform(0, 0, 0).z() > point.socket(false).transform(0, 0, 0).z() + 2);
		assertTrue(chamber.local(Joint.RIGHT_FOREARM).rotation().x() < point.local(Joint.RIGHT_FOREARM).rotation().x() - .7F);
		assertTrue(recoil.local(Joint.RIGHT_FOREARM).rotation().x() < point.local(Joint.RIGHT_FOREARM).rotation().x() - .4F);
	}

	@Test
	void everySampleKeepsFlatFeetRigidLinksContinuousWristsAndImmutableMatrices() {
		for (int move : MOVES) for (boolean left : new boolean[] {false, true}) {
			var rule = MastersArtRules.move(move);
			for (float age = 0; age <= rule.windup() + rule.recovery(); age += .0625F) {
				Pose pose = sample(move, age, left);
				for (Joint foot : new Joint[] {Joint.RIGHT_FOOT, Joint.LEFT_FOOT})
					for (float x : new float[] {-2, 2}) for (float z : new float[] {-2, 2})
						assertEquals(24, pose.world(foot).transform(x, 2, z).y(), EPS, move + " plant at " + age);
				for (Joint joint : Joint.values()) {
					Matrix world = pose.world(joint);
					for (float n : world.values()) assertTrue(Float.isFinite(n));
					matrix(Matrix.identity(), world.multiply(world.inverseRigid()), EPS);
					float original = world.get(0, 0); world.values()[0] = Float.NaN; assertEquals(original, world.get(0, 0));
					if (joint.parent() != null) assertEquals(joint.bind().translation().length(),
						world.transform(0, 0, 0).minus(pose.world(joint.parent()).transform(0, 0, 0)).length(), EPS, joint.name());
				}
				for (Joint hand : new Joint[] {Joint.RIGHT_HAND, Joint.LEFT_HAND}) {
					var wrist = pose.local(hand).rotation();
					assertTrue(Math.abs(wrist.x()) <= .181 && Math.abs(wrist.y()) <= .151 && Math.abs(wrist.z()) <= .181);
				}
				point(pose.world(Joint.RIGHT_FOREARM).transform(0, 4, 0), pose.world(Joint.RIGHT_HAND).transform(0, 0, 0), EPS);
				point(pose.world(Joint.LEFT_FOREARM).transform(0, 4, 0), pose.world(Joint.LEFT_HAND).transform(0, 0, 0), EPS);
			}
		}
	}

	@Test
	void plantsStayFixedThroughoutFullWeightCommitment() {
		for (int move : MOVES) {
			var rule = MastersArtRules.move(move);
			Pose impact = sample(move, rule.windup(), false);
			for (float age = rule.windup() * .65F; age <= rule.windup() + Math.min(4, rule.recovery() * .25F); age += .0625F) {
				Pose pose = sample(move, age, false);
				assertEquals(1, pose.weight());
				for (Joint foot : new Joint[] {Joint.RIGHT_FOOT, Joint.LEFT_FOOT})
					point(impact.world(foot).transform(0, 0, 0), pose.world(foot).transform(0, 0, 0), EPS);
			}
		}
	}

	@Test
	void bothPalettesMirrorExactlyAtEveryPhaseAndSocket() {
		for (int move : MOVES) for (float age = 0; age <= 26; age += .125F) {
			Pose right = sample(move, age, false), left = sample(move, age, true);
			ViewPose rv = view(right, false), lv = view(left, true);
			for (Joint joint : Joint.values()) for (Vec3 p : new Vec3[] {Vec3.ZERO, new Vec3(1, 2, -3)}) {
				Vec3 a = right.world(joint).transform(p), b = left.world(joint.opposite()).transform(-p.x(), p.y(), p.z());
				point(new Vec3(-a.x(), a.y(), a.z()), b, EPS);
				Vec3 av = rv.world(joint).transform(p), bv = lv.world(joint.opposite()).transform(-p.x(), p.y(), p.z());
				point(new Vec3(-av.x(), av.y(), av.z()), bv, EPS);
			}
		}
	}

	@Test
	void bodyAndViewJoinContinuouslyAtPhaseAndIdleEdgesEvenWithShortAcceptedWindows() {
		for (int move : MOVES) for (boolean left : new boolean[] {false, true})
			for (int tell : new int[] {1, MastersArtRules.move(move).windup(), 60}) for (int recovery : new int[] {1, 18, 120}) {
				float follow = tell + Math.min(4, recovery * .25F);
				for (float edge : new float[] {0, tell * .65F, tell, tell + 1, follow, tell + recovery}) {
					Pose before = samplePlayer(move, edge - .0001F, tell, recovery, left), after = samplePlayer(move, edge + .0001F, tell, recovery, left);
					ViewPose bv = view(before, left), av = view(after, left);
					for (Joint joint : Joint.values()) {
						matrix(before.world(joint), after.world(joint), .025F);
						matrix(bv.world(joint), av.world(joint), .025F);
					}
				}
			}
	}

	@Test
	void firstPersonGripAndGuardStayAboveHudBelowAimAndBeyondNearPlane() {
		for (int move : MOVES) for (boolean left : new boolean[] {false, true}) for (float age = 0; age <= 26; age += .0625F) {
			ViewPose camera = view(sample(move, age, left), left);
			assertEquals(new Vec3(0, -.7F, -.9F), camera.origin());
			for (Joint joint : new Joint[] {Joint.RIGHT_SHOULDER, Joint.RIGHT_FOREARM, Joint.RIGHT_HAND, Joint.RIGHT_SOCKET,
				Joint.LEFT_SHOULDER, Joint.LEFT_FOREARM, Joint.LEFT_HAND, Joint.LEFT_SOCKET})
				assertTrue(camera.cameraPoint(joint, 0, 0, 0).z() < -.45F, "Near-plane joint margin");
			for (Joint joint : new Joint[] {left ? Joint.LEFT_SOCKET : Joint.RIGHT_SOCKET, left ? Joint.RIGHT_HAND : Joint.LEFT_HAND}) {
				Vec3 p = camera.cameraPoint(joint, 0, 0, 0);
				double y = .5 - p.y() / -p.z() / (2 * Math.tan(Math.toRadians(35)));
				assertTrue(y > .6 && y < .81, move + " " + joint + " at " + age + ": " + y);
				for (int[] viewport : new int[][] {{854, 480, 2}, {1280, 720, 3}, {1280, 960, 4}, {1920, 810, 3}})
					assertTrue(y * viewport[1] < viewport[1] - 42 * viewport[2], "Conservative HUD band at supported scale");
			}
		}
	}

	private static Pose sample(int move, float age, boolean left) {
		var rule = MastersArtRules.move(move);
		return samplePlayer(move, age, rule.windup(), rule.recovery(), left);
	}
	private static void point(Vec3 a, Vec3 b, float tolerance) {
		assertEquals(a.x(), b.x(), tolerance); assertEquals(a.y(), b.y(), tolerance); assertEquals(a.z(), b.z(), tolerance);
	}
	private static void matrix(Matrix a, Matrix b, float tolerance) {
		for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++) assertEquals(a.get(row, col), b.get(row, col), tolerance);
	}
}
