package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import static dev.wildercord.aura.ArticulatedCombatPose.*;
import static org.junit.jupiter.api.Assertions.*;

/** Pure body/view geometry and accepted-clock contracts; native silhouette and HUD pixels need captures. */
class ArticulatedGroundFieldPoseTest {
	private static final float EPS = .0008F;
	private static final int[] MOVES = {COLLAPSE, RED_RAIN};
	private static final Joint[] FEET = {Joint.RIGHT_FOOT, Joint.LEFT_FOOT};
	private static final Joint[] HANDS = {Joint.RIGHT_HAND, Joint.LEFT_HAND};

	@Test
	void groundFieldsUsePresentationIdsWithoutCreatingSharedInputsOrNpcAttacks() {
		assertEquals(17, COLLAPSE);
		assertEquals(18, RED_RAIN);
		for (int move : MOVES) {
			assertTrue(supportsPlayer(move));
			assertTrue(MastersArtAnimation.supports(move));
			assertNull(MastersArtRules.move(move), "Style forms do not add shared activation ordinals");
			assertFalse(supportsMaster(move));
			assertEquals(move == COLLAPSE ? "collapse" : "red_rain", MastersStyleRules.animation(move).art());
		}
		for (int move : new int[] {-1, 5, 14, MastersStyleRules.STYLES.stream().mapToInt(MastersStyleRules.Style::animation).max().orElseThrow() + 1, Integer.MAX_VALUE}) {
			assertFalse(supportsPlayer(move));
			assertSame(NONE, samplePlayer(move, 5, 8, 18, false));
		}
	}

	@Test
	void acceptedWindowsHaveOneReleaseTickAndEndAtTheExistingExpiry() {
		for (int move : MOVES) {
			var rule = MastersStyleRules.animation(move);
			assertEquals(8, rule.windup());
			assertEquals(move == COLLAPSE ? 18 : 16, rule.recovery());
			for (boolean left : new boolean[] {false, true}) {
				for (float age = 0; age < rule.windup() + rule.recovery(); age += .125F) {
					Pose pose = sample(move, age, left);
					Phase expected = age < rule.windup() ? Phase.WINDUP : age < rule.windup() + 1 ? Phase.ACTIVE : Phase.RECOVERY;
					assertEquals(expected, pose.phase(), "One physical release only, including the field's lingering ticks");
					assertEquals(expected, view(pose, left).phase());
					assertTrue(pose.weight() >= 0 && pose.weight() <= 1);
				}
				assertEquals(Phase.WINDUP, sample(move, rule.windup() - .001F, left).phase());
				assertEquals(Phase.ACTIVE, sample(move, rule.windup(), left).phase());
				assertEquals(Phase.ACTIVE, sample(move, rule.windup() + .999F, left).phase());
				assertEquals(Phase.RECOVERY, sample(move, rule.windup() + 1, left).phase());
				assertSame(NONE, sample(move, rule.windup() + rule.recovery(), left));
				assertSame(NONE, sample(move, rule.windup() + rule.recovery() + 1, left));
				for (Joint joint : Joint.values()) assertEquals(joint.bind(), sample(move, 0, left).local(joint));
			}
			for (float bad : new float[] {-1, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY})
				assertSame(NONE, sample(move, bad, false));
			for (int[] bad : new int[][] {{0, 18}, {61, 18}, {8, 0}, {8, 121}})
				assertSame(NONE, samplePlayer(move, 1, bad[0], bad[1], false));
		}
	}

	@Test
	void collapseCommitsGroundwardWhileRedRainDescendsAcrossTheBody() {
		Pose collapseChamber = sample(COLLAPSE, 5.2F, false), collapseRelease = sample(COLLAPSE, 8, false);
		Pose rainChamber = sample(RED_RAIN, 5.2F, false), rainRelease = sample(RED_RAIN, 8, false);
		Vec3 collapseStart = collapseChamber.socket(false).transform(0, 0, 0);
		Vec3 collapseEnd = collapseRelease.socket(false).transform(0, 0, 0);
		Vec3 rainStart = rainChamber.socket(false).transform(0, 0, 0);
		Vec3 rainEnd = rainRelease.socket(false).transform(0, 0, 0);
		assertTrue(collapseEnd.y() > collapseStart.y() + 4, "Collapse lowers its grip into a groundward commitment");
		assertTrue(collapseEnd.y() - collapseStart.y() > Math.abs(collapseEnd.x() - collapseStart.x()),
			"Collapse reads primarily downward rather than as a lateral sweep");
		assertTrue(rainEnd.y() > rainStart.y() + 4, "Red Rain descends from its high chamber");
		assertTrue(rainEnd.x() > rainStart.x() + 4, "Red Rain carries the descending cut across the body");
		assertTrue(rainStart.x() < 0 && rainEnd.x() > 0, "Red Rain crosses the body's centre line");
		assertNotEquals(collapseRelease.local(Joint.RIGHT_UPPER_ARM), rainRelease.local(Joint.RIGHT_UPPER_ARM));
		assertNotEquals(collapseRelease.local(Joint.RIGHT_SOCKET), rainRelease.local(Joint.RIGHT_SOCKET));
		assertNotEquals(view(collapseRelease, false).local(Joint.RIGHT_SOCKET), view(rainRelease, false).local(Joint.RIGHT_SOCKET));
		for (int move : MOVES) {
			Pose release = sample(move, 8, false);
			assertNotEquals(release.local(Joint.RIGHT_UPPER_ARM), view(release, false).local(Joint.RIGHT_UPPER_ARM),
				"First-person framing is authored independently of the world body");
		}
	}

	@Test
	void everyFrameKeepsLevelSolesRigidBonesImmutableMatricesAndContinuousHandSockets() {
		for (int move : MOVES) for (boolean left : new boolean[] {false, true}) {
			var rule = MastersStyleRules.animation(move);
			for (float age = 0; age <= rule.windup() + rule.recovery(); age += .0625F) {
				Pose pose = sample(move, age, left);
				ViewPose camera = view(pose, left);
				for (Joint foot : FEET) for (float x : new float[] {-2, 2}) for (float z : new float[] {-2, 2})
					assertEquals(24, pose.world(foot).transform(x, 2, z).y(), EPS, move + " sole at " + age);
				for (Joint joint : Joint.values()) {
					assertRigidAndImmutable(pose.world(joint));
					assertRigidAndImmutable(camera.world(joint));
					if (joint.parent() == null) continue;
					matrix(pose.world(joint.parent()).multiply(pose.local(joint).matrix()), pose.world(joint), EPS);
					matrix(camera.world(joint.parent()).multiply(camera.local(joint).matrix()), camera.world(joint), EPS);
					assertEquals(joint.bind().translation().length(),
						pose.world(joint).transform(0, 0, 0).minus(pose.world(joint.parent()).transform(0, 0, 0)).length(), EPS, joint.name());
				}
				for (Joint hand : HANDS) {
					Joint socket = hand == Joint.RIGHT_HAND ? Joint.RIGHT_SOCKET : Joint.LEFT_SOCKET;
					assertSame(hand, socket.parent());
					assertWrist(pose.local(hand).rotation());
					assertWrist(camera.local(hand).rotation());
					point(pose.world(hand.parent()).transform(0, 4, 0), pose.world(hand).transform(0, 0, 0), EPS);
					point(camera.world(hand.parent()).transform(0, 4, 0), camera.world(hand).transform(0, 0, 0), EPS);
					point(pose.world(hand).transform(0, 1, 0), pose.world(socket).transform(0, 0, 0), EPS);
					point(camera.world(hand).transform(0, 1, 0), camera.world(socket).transform(0, 0, 0), EPS);
				}
			}
		}
	}

	@Test
	void plantsStayFixedThroughChamberReleaseAndFullWeightFollowThroughForBothHands() {
		for (int move : MOVES) for (boolean left : new boolean[] {false, true}) {
			var rule = MastersStyleRules.animation(move);
			Pose release = sample(move, rule.windup(), left);
			for (float age = rule.windup() * .65F; age <= rule.windup() + Math.min(4, rule.recovery() * .25F); age += .0625F) {
				Pose pose = sample(move, age, left);
				assertEquals(1, pose.weight());
				for (Joint foot : FEET) matrix(release.world(foot), pose.world(foot), EPS);
			}
		}
	}

	@Test
	void bodyAndViewMirrorEveryJointAndSocketThroughoutTheAcceptedClock() {
		for (int move : MOVES) for (float age = 0; age <= 27; age += .125F) {
			Pose right = sample(move, age, false), left = sample(move, age, true);
			ViewPose rv = view(right, false), lv = view(left, true);
			assertEquals(right.phase(), left.phase());
			assertEquals(right.weight(), left.weight());
			for (Joint joint : Joint.values()) for (Vec3 p : new Vec3[] {Vec3.ZERO, new Vec3(1, 2, -3)}) {
				Vec3 a = right.world(joint).transform(p), b = left.world(joint.opposite()).transform(-p.x(), p.y(), p.z());
				point(new Vec3(-a.x(), a.y(), a.z()), b, EPS);
				Vec3 av = rv.world(joint).transform(p), bv = lv.world(joint.opposite()).transform(-p.x(), p.y(), p.z());
				point(new Vec3(-av.x(), av.y(), av.z()), bv, EPS);
			}
		}
	}

	@Test
	void phaseBoundariesAndIdleEdgesStayContinuousForShortAndExtremeAcceptedWindows() {
		for (int move : MOVES) for (boolean left : new boolean[] {false, true})
			for (int tell : new int[] {1, 8, 60}) for (int recovery : new int[] {1, 16, 18, 120}) {
				float follow = tell + Math.min(4, recovery * .25F);
				for (float edge : new float[] {0, tell * .65F, tell, tell + 1, follow, tell + recovery}) {
					Pose before = samplePlayer(move, edge - .0001F, tell, recovery, left);
					Pose after = samplePlayer(move, edge + .0001F, tell, recovery, left);
					ViewPose bv = view(before, left), av = view(after, left);
					for (Joint joint : Joint.values()) {
						matrix(before.world(joint), after.world(joint), .025F);
						matrix(bv.world(joint), av.world(joint), .025F);
					}
				}
			}
	}

	@Test
	void firstPersonGripAndGuardRemainBeyondNearPlaneBelowAimAndAboveTheHud() {
		for (int move : MOVES) for (boolean left : new boolean[] {false, true}) for (float age = 0; age <= 27; age += .0625F) {
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
		var rule = MastersStyleRules.animation(move);
		return samplePlayer(move, age, rule.windup(), rule.recovery(), left);
	}
	private static void assertRigidAndImmutable(Matrix world) {
		for (float value : world.values()) assertTrue(Float.isFinite(value));
		matrix(Matrix.identity(), world.multiply(world.inverseRigid()), EPS);
		float original = world.get(0, 0);
		world.values()[0] = Float.NaN;
		assertEquals(original, world.get(0, 0));
	}
	private static void assertWrist(Rotation wrist) {
		assertTrue(Math.abs(wrist.x()) <= .181F && Math.abs(wrist.y()) <= .151F && Math.abs(wrist.z()) <= .181F,
			"Blade attitude belongs to the child socket without folding the wrist");
	}
	private static void point(Vec3 a, Vec3 b, float tolerance) {
		assertEquals(a.x(), b.x(), tolerance); assertEquals(a.y(), b.y(), tolerance); assertEquals(a.z(), b.z(), tolerance);
	}
	private static void matrix(Matrix a, Matrix b, float tolerance) {
		for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++) assertEquals(a.get(row, col), b.get(row, col), tolerance);
	}
}
