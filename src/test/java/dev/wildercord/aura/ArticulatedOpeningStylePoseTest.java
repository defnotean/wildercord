package dev.wildercord.aura;

import org.junit.jupiter.api.Test;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.nio.ByteBuffer;

import static dev.wildercord.aura.ArticulatedCombatPose.*;
import static org.junit.jupiter.api.Assertions.*;

/** Geometry/clock contracts only. Native body, armor and HUD pixels require their own captures. */
class ArticulatedOpeningStylePoseTest {
	private static final float EPS = .0008F;
	private static final int[] MOVES = {KINDLING_DRAW, FROSTBITE};

	@Test
	void onlyTwoExistingFirstFormsJoinThePlayerBackend() {
		for (int move : new int[] {KINDLING_DRAW, FROSTBITE}) {
			assertTrue(supportsPlayer(move));
			var rule = MastersStyleRules.animation(move);
			assertEquals(move == KINDLING_DRAW ? "kindling_draw" : "frostbite", rule.art());
			assertEquals(MastersStyleRules.TargetPolicy.ACTIVE_CONE, rule.targets());
			assertNull(MastersArtRules.move(move), "Style input remains the existing sword string, never a new shared key");
		}
		for (int move : new int[] {-1, Integer.MAX_VALUE}) {
			assertFalse(supportsPlayer(move));
			assertSame(NONE, samplePlayer(move, 5, 6, 12, false));
		}
		assertFalse(supportsMaster(KINDLING_DRAW));
		assertFalse(supportsMaster(FROSTBITE));
	}

	@Test
	void acceptedWindowsReleaseExactlyOnceAndNeverExtendExpiry() {
		for (int move : MOVES) {
			var rule = MastersStyleRules.animation(move);
			assertEquals(6, rule.windup());
			assertEquals(12, rule.recovery());
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
	void lowDrawOpensAcrossTheBodyWhileFrostbiteClosesItsCompactGuard() {
		Pose emberChamber = sample(KINDLING_DRAW, 3.9F, false), emberCut = sample(KINDLING_DRAW, 6, false);
		Pose frostChamber = sample(FROSTBITE, 3.9F, false), frostCut = sample(FROSTBITE, 6, false), frostGuard = sample(FROSTBITE, 9, false);
		assertTrue(emberChamber.socket(false).transform(0, 0, 0).x() < -6);
		assertTrue(emberCut.socket(false).transform(0, 0, 0).x() > 3);
		assertTrue(emberChamber.socket(false).transform(0, 0, 0).z() > 1, "Low hilt chambers beside the rear hip");
		assertTrue(emberCut.socket(false).transform(0, 0, 0).y() > frostCut.socket(false).transform(0, 0, 0).y() + 3,
			"Ember draws lower than the compact Rime cut");
		assertTrue(Math.abs(emberCut.local(Joint.CHEST).rotation().y()) > Math.abs(frostCut.local(Joint.CHEST).rotation().y()) + .09F);
		assertTrue(frostChamber.socket(false).transform(0, 0, 0).x() < frostCut.socket(false).transform(0, 0, 0).x() - 10);
		assertTrue(frostGuard.local(Joint.RIGHT_FOREARM).rotation().x() < frostCut.local(Joint.RIGHT_FOREARM).rotation().x() - .5F,
			"Rime folds the elbow to return to closed guard after its single cut");
		assertNotEquals(view(emberCut, false).local(Joint.RIGHT_SOCKET), view(frostCut, false).local(Joint.RIGHT_SOCKET));
	}

	@Test
	void everySampleKeepsFlatFeetRigidLinksContinuousWristsAndImmutableMatrices() {
		for (int move : MOVES) for (boolean left : new boolean[] {false, true}) {
			var rule = MastersStyleRules.animation(move);
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
			var rule = MastersStyleRules.animation(move);
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
			for (int tell : new int[] {1, MastersStyleRules.animation(move).windup(), 60}) for (int recovery : new int[] {1, 18, 120}) {
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

	@Test
	void existingSharedBodyAndViewPalettesKeepTheirImmutableSourceFingerprints() throws Exception {
		// Captured independently from ae9d57d8f3f631aeaf67ad11b2dc643da764d379 on Java 25.
		// Includes every local/world transform, both sockets, phases, weight, origin and both
		// hands at 0.125-tick intervals, including the complete windup and recovery edges.
		String[] expected = {
			"f8ccb270e9ff749e83e303bed335715ac7447188b750748f549b5b024b05487b",
			"6a529359ce9e6608241c4e1fad9577ed85c84e4976b05f4f4096403f16d6f95e",
			"219738e5e42a8d4bd2870e240ada26e44b48d23c468d03662d8ff4fe3b2ba5f2"
		};
		for (int move = SPELLCUT; move <= DRIVING_CUT; move++) assertEquals(expected[move], fingerprint(move));
	}

	private static String fingerprint(int move) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		var rule = MastersArtRules.move(move);
		for (boolean left : new boolean[] {false, true}) for (int step = 0; step <= (rule.windup() + rule.recovery()) * 8; step++) {
			var pose = samplePlayer(move, step / 8F, rule.windup(), rule.recovery(), left);
			var view = view(pose, left);
			add(digest, pose.weight()); add(digest, pose.phase().ordinal());
			add(digest, view.origin().x()); add(digest, view.origin().y()); add(digest, view.origin().z());
			for (var joint : Joint.values()) {
				var a = pose.local(joint); var b = view.local(joint);
				for (float v : new float[] {a.x(), a.y(), a.z(), a.rotation().x(), a.rotation().y(), a.rotation().z(),
					b.x(), b.y(), b.z(), b.rotation().x(), b.rotation().y(), b.rotation().z()}) add(digest, v);
				for (float v : pose.world(joint).values()) add(digest, v);
				for (float v : view.world(joint).values()) add(digest, v);
			}
		}
		return HexFormat.of().formatHex(digest.digest());
	}
	private static void add(MessageDigest digest, float value) {
		digest.update(ByteBuffer.allocate(4).putFloat(value).array());
	}

	private static Pose sample(int move, float age, boolean left) {
		var rule = MastersStyleRules.animation(move);
		return samplePlayer(move, age, rule.windup(), rule.recovery(), left);
	}
	private static void point(Vec3 a, Vec3 b, float tolerance) {
		assertEquals(a.x(), b.x(), tolerance); assertEquals(a.y(), b.y(), tolerance); assertEquals(a.z(), b.z(), tolerance);
	}
	private static void matrix(Matrix a, Matrix b, float tolerance) {
		for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++) assertEquals(a.get(row, col), b.get(row, col), tolerance);
	}
}
