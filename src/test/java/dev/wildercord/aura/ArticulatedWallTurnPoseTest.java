package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.wildercord.aura.ArticulatedCombatPose.*;
import static org.junit.jupiter.api.Assertions.*;

/** Pure Wall Turn palette contracts across the separate accepted form events. Native silhouettes need client captures. */
class ArticulatedWallTurnPoseTest {
	private static final float EPS = .001F;

	@Test
	void onlyWallTurnPhasesOwnThePaletteAndEveryOtherFormKeepsItsFallback() {
		for (int phase = WallTurnRules.BRACE; phase <= WallTurnRules.ABORT; phase++) assertTrue(wallTurnMove(-2 - phase));
		for (int move : new int[] {-1, -2, -8, -9, -10, -11, -20, -21, -27, 0, 1}) assertFalse(wallTurnMove(move), "move " + move);
		for (int phase : new int[] {WallTurnRules.IDLE, StoneHingeRules.BRACE, StoneHingeRules.CATCH, StoneHingeRules.TURN, StoneHingeRules.SPENT, 42})
			assertSame(NONE, sampleWallTurn(phase, 5, 1, false));
	}

	@Test
	void invalidStaleAndFinishedEventsCannotRetainAPose() {
		for (float age : new float[] {-1, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
			for (int phase = WallTurnRules.BRACE; phase <= WallTurnRules.ABORT; phase++) assertSame(NONE, sampleWallTurn(phase, 8, age, false));
		assertSame(NONE, sampleWallTurn(WallTurnRules.KICK, -1, 1, false));
		assertSame(NONE, sampleWallTurn(WallTurnRules.KICK, WallTurnRules.KICK_TICKS + 1, 1, false));
		assertSame(NONE, sampleWallTurn(WallTurnRules.BRACE, WallTurnRules.REST_TICKS + 1, 1, false));
		assertSame(NONE, sampleWallTurn(WallTurnRules.BRACE, WallTurnRules.BRACE_TICKS, WallTurnRules.BRACE_TICKS + 3, false));
		assertSame(NONE, sampleWallTurn(WallTurnRules.KICK, 4, 6, false));
		assertSame(NONE, sampleWallTurn(WallTurnRules.FALL, 0, WALL_FALL_END, false));
		assertSame(NONE, sampleWallTurn(WallTurnRules.LAND, WallTurnRules.RECOVERY_TICKS, WallTurnRules.RECOVERY_TICKS, false));
		assertSame(NONE, sampleWallTurn(WallTurnRules.ABORT, 5, 5, false));
		assertTrue(WALL_FALL_END < 30, "The carry ends before the client drops the event");
	}

	@Test
	void theWholeTurnIsOneContinuousClipAcrossEventHandoffs() {
		for (boolean left : new boolean[] {false, true}) {
			// Every clip starts from and returns to the neutral rig.
			same(NONE, sampleWallTurn(WallTurnRules.BRACE, WallTurnRules.BRACE_TICKS, .0001F, left), .01F);
			same(NONE, sampleWallTurn(WallTurnRules.FALL, 0, WALL_FALL_END - .0001F, left), .01F);
			same(NONE, sampleWallTurn(WallTurnRules.LAND, WallTurnRules.RECOVERY_TICKS, WallTurnRules.RECOVERY_TICKS - .0001F, left), .01F);
			same(NONE, sampleWallTurn(WallTurnRules.ABORT, 5, 4.9999F, left), .01F);
			// Brace to the first kick step, each later kick step, the final push to the fall, and the carry to the landing.
			same(sampleWallTurn(WallTurnRules.BRACE, WallTurnRules.BRACE_TICKS, WallTurnRules.BRACE_TICKS, left),
				sampleWallTurn(WallTurnRules.KICK, WallTurnRules.KICK_TICKS, 0, left), .003F);
			for (int remaining = WallTurnRules.KICK_TICKS; remaining > 1; remaining--)
				same(sampleWallTurn(WallTurnRules.KICK, remaining, 1, left), sampleWallTurn(WallTurnRules.KICK, remaining - 1, 0, left), .003F);
			same(sampleWallTurn(WallTurnRules.KICK, 1, 1, left), sampleWallTurn(WallTurnRules.FALL, 0, 0, left), .003F);
			same(sampleWallTurn(WallTurnRules.KICK, 0, 0, left), sampleWallTurn(WallTurnRules.FALL, 0, 0, left), .003F);
			for (float fall = 3; fall <= WALL_FALL_HOLD; fall += .5F)
				same(sampleWallTurn(WallTurnRules.FALL, 0, fall, left), sampleWallTurn(WallTurnRules.LAND, WallTurnRules.RECOVERY_TICKS, 0, left), .003F);
			// Within each event the clip has no sub-tick jumps.
			for (int[] event : new int[][] {{WallTurnRules.BRACE, 10}, {WallTurnRules.KICK, 8}, {WallTurnRules.KICK, 1}, {WallTurnRules.FALL, 0},
					{WallTurnRules.LAND, 10}, {WallTurnRules.ABORT, 5}})
				for (float age = 0; age < 30; age += .005F)
					same(sampleWallTurn(event[0], event[1], age, left), sampleWallTurn(event[0], event[1], age + .005F, left), .1F);
		}
	}

	@Test
	void braceKickCarryAndLandingAreDistinctReadableShapes() {
		Pose brace = sampleWallTurn(WallTurnRules.BRACE, 10, 6, false), push = sampleWallTurn(WallTurnRules.KICK, 5, 1, false);
		Pose carry = sampleWallTurn(WallTurnRules.FALL, 0, 8, false), land = sampleWallTurn(WallTurnRules.LAND, 10, 2, false);
		assertEquals(Phase.WINDUP, brace.phase()); assertEquals(Phase.ACTIVE, push.phase());
		assertEquals(Phase.RECOVERY, carry.phase()); assertEquals(Phase.RECOVERY, land.phase());
		for (Pose pose : new Pose[] {brace, push, carry, land}) assertEquals(1, pose.weight(), EPS);
		Vec3 wallFoot = brace.world(Joint.LEFT_FOOT).transform(0, 0, 0);
		assertTrue(wallFoot.y() < 21.4F && wallFoot.z() < -3, "The guard-side sole is raised onto the wall: " + wallFoot);
		assertTrue(push.world(Joint.LEFT_FOOT).transform(0, 0, 0).z() > wallFoot.z() + 3, "The push drives the wall foot back");
		assertTrue(land.local(Joint.PELVIS).y() > carry.local(Joint.PELVIS).y() + .6F, "The landing absorbs into a crouch");
		List<Pose> shapes = List.of(brace, push, carry, land, sampleWallTurn(WallTurnRules.ABORT, 5, 0, false));
		for (int i = 0; i < shapes.size(); i++) for (int j = i + 1; j < shapes.size(); j++)
			assertNotEquals(shapes.get(i).local(Joint.RIGHT_UPPER_ARM), shapes.get(j).local(Joint.RIGHT_UPPER_ARM), i + " vs " + j);
	}

	@Test
	void everyFrameIsFiniteRigidMirroredLevelFootedAndKeepsSocketsOnHands() {
		for (Sampled frame : frames()) {
			Pose right = frame.right(), left = frame.left();
			for (Joint joint : Joint.values()) {
				for (float value : right.world(joint).values()) assertTrue(Float.isFinite(value) && Math.abs(value) < 32);
				for (Vec3 p : new Vec3[] {Vec3.ZERO, new Vec3(1, 2, -3)}) {
					Vec3 a = right.world(joint).transform(p), b = left.world(joint.opposite()).transform(-p.x(), p.y(), p.z());
					point(new Vec3(-a.x(), a.y(), a.z()), b);
				}
				if (joint.parent() != null) assertEquals(joint.bind().translation().length(),
					right.world(joint).transform(0, 0, 0).minus(right.world(joint.parent()).transform(0, 0, 0)).length(), EPS);
			}
			for (Joint foot : new Joint[] {Joint.RIGHT_FOOT, Joint.LEFT_FOOT})
				point(new Vec3(0, 1, 0), right.world(foot).direction(new Vec3(0, 1, 0)));
			for (boolean handed : new boolean[] {false, true}) {
				Pose pose = handed ? left : right;
				Joint hand = handed ? Joint.LEFT_HAND : Joint.RIGHT_HAND;
				point(pose.world(hand).transform(0, 1, 0), pose.socket(handed).transform(0, 0, 0));
				Rotation wrist = pose.local(hand).rotation();
				assertTrue(Math.abs(wrist.x()) <= .1801F && Math.abs(wrist.y()) <= .1501F && Math.abs(wrist.z()) <= .1801F, "Wrist " + wrist);
			}
		}
	}

	@Test
	void firstPersonFollowsTheSameEvents() {
		for (boolean left : new boolean[] {false, true}) {
			ViewPose brace = view(sampleWallTurn(WallTurnRules.BRACE, 10, 6, left), left);
			ViewPose push = view(sampleWallTurn(WallTurnRules.KICK, 4, 1, left), left);
			assertNotNull(brace); assertNotNull(push);
			assertNotEquals(brace.socket(left).transform(0, 0, 0), push.socket(left).transform(0, 0, 0));
		}
	}

	/** Every phase at fine steps, paired right/left handed. */
	record Sampled(String label, Pose right, Pose left) {}

	static List<Sampled> frames() {
		List<Sampled> frames = new ArrayList<>();
		int[][] events = {{WallTurnRules.BRACE, 10}, {WallTurnRules.KICK, 8}, {WallTurnRules.KICK, 4}, {WallTurnRules.KICK, 1}, {WallTurnRules.KICK, 0},
			{WallTurnRules.FALL, 0}, {WallTurnRules.LAND, 10}, {WallTurnRules.ABORT, 5}};
		for (int[] event : events) for (float age = 0; age < 30; age += .25F) {
			Pose right = sampleWallTurn(event[0], event[1], age, false);
			if (right == NONE) continue;
			frames.add(new Sampled("phase=" + event[0] + " remaining=" + event[1] + " age=" + age, right, sampleWallTurn(event[0], event[1], age, true)));
		}
		return frames;
	}

	private static void same(Pose a, Pose b, float tolerance) {
		for (Joint joint : Joint.values()) for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++)
			assertEquals(a.world(joint).get(row, col), b.world(joint).get(row, col), tolerance, joint + " [" + row + "," + col + "]");
	}
	private static void point(Vec3 a, Vec3 b) { assertEquals(a.x(), b.x(), EPS); assertEquals(a.y(), b.y(), EPS); assertEquals(a.z(), b.z(), EPS); }
}
