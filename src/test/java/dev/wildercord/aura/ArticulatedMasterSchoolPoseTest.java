package dev.wildercord.aura;

import dev.wildercord.aura.world.GaleRepriseRules;
import dev.wildercord.aura.world.MasterAnimationRules;
import dev.wildercord.aura.world.StoneFractureRules;
import org.junit.jupiter.api.Test;

import static dev.wildercord.aura.ArticulatedCombatPose.*;
import static org.junit.jupiter.api.Assertions.*;

/** Pure timing/rig contracts. These cannot establish native silhouette or gameplay acceptance. */
class ArticulatedMasterSchoolPoseTest {
	private static final float EPS = .0008F;

	@Test
	void supportedNpcIdsAndExactServerPhasesStayBounded() {
		assertEquals(MasterAnimationRules.CROSSWIND_REPRISE, MASTER_CROSSWIND_REPRISE);
		assertEquals(MasterAnimationRules.STONE_FRACTURE, MASTER_STONE_FRACTURE);
		for (int move = -1; move <= 10; move++)
			assertEquals(move == 1 || move == 7 || move == 8 || move == 9 || move == 10, supportsMaster(move));
		for (int move : new int[] {7, 8}) {
			int tell = tell(move), recovery = recovery(move);
			assertEquals(Phase.WINDUP, sample(move, tell - .001F, false).phase());
			assertEquals(Phase.ACTIVE, sample(move, tell, false).phase());
			assertEquals(Phase.RECOVERY, sample(move, tell + 1, false).phase());
			assertSame(NONE, sample(move, tell + 1 + recovery, false));
			assertEquals(0, sample(move, 0, false).weight());
			for (float age : new float[] {-1, Float.NaN, Float.POSITIVE_INFINITY}) assertSame(NONE, sample(move, age, false));
			assertSame(NONE, sampleMaster(move, 1, 0, 1, recovery, false));
			assertSame(NONE, sampleMaster(move, 1, tell, 11, recovery, false));
		}
	}

	@Test
	void locomotionExceptionCoversOnlyTheFourAcceptedGaleStepTicks() {
		float start = GaleRepriseRules.GATHER, end = start + GaleRepriseRules.STEP_TICKS;
		assertFalse(masterFootwork(7, start - .001F, GaleRepriseRules.TELL));
		assertTrue(masterFootwork(7, start, GaleRepriseRules.TELL));
		assertTrue(masterFootwork(7, end - .001F, GaleRepriseRules.TELL));
		assertFalse(masterFootwork(7, end, GaleRepriseRules.TELL));
		assertFalse(masterFootwork(7, end + .001F, GaleRepriseRules.TELL));
		for (int move : new int[] {0, 1, 2, 6, 8}) assertFalse(masterFootwork(move, start + 1, GaleRepriseRules.TELL));
		for (float age : new float[] {-1, Float.NaN, Float.POSITIVE_INFINITY}) assertFalse(masterFootwork(7, age, GaleRepriseRules.TELL));
		assertFalse(masterFootwork(7, 1, 0)); assertFalse(masterFootwork(7, 1, 81));
	}

	@Test
	void galeShowsOneAlternatingFootClearanceAndPlantsBeforeTheReply() {
		Pose lead = sample(7, 9, false), trail = sample(7, 11, false);
		assertTrue(lead.world(Joint.RIGHT_FOOT).transform(0, 0, 0).y() < 21);
		assertEquals(22, lead.world(Joint.LEFT_FOOT).transform(0, 0, 0).y(), EPS);
		assertTrue(trail.world(Joint.LEFT_FOOT).transform(0, 0, 0).y() < 21);
		assertEquals(22, trail.world(Joint.RIGHT_FOOT).transform(0, 0, 0).y(), EPS);
		for (float age = GaleRepriseRules.GATHER + GaleRepriseRules.STEP_TICKS; age <= GaleRepriseRules.TELL + 4; age += .125F) {
			Pose pose = sample(7, age, false);
			point(new Vec3(-2.55F, 22, 1.7F), pose.world(Joint.RIGHT_FOOT).transform(0, 0, 0));
			point(new Vec3(2.55F, 22, -1.8F), pose.world(Joint.LEFT_FOOT).transform(0, 0, 0));
		}
	}

	@Test
	void stoneHoldsItsBraceThenLiftsAnIndependentOverheadWarning() {
		Pose brace = sample(8, StoneFractureRules.PLANT, false);
		Pose held = sample(8, StoneFractureRules.PLANT + StoneFractureRules.BRACE - .01F, false);
		for (Joint joint : Joint.values()) matrix(brace.world(joint), held.world(joint), EPS);
		Pose overhead = sample(8, 24, false), release = sample(8, StoneFractureRules.TELL, false);
		assertTrue(overhead.socket(false).transform(0, 0, 0).y() < 0, "Overhead grip clears the head top");
		assertTrue(release.socket(false).transform(0, 0, 0).y() > 10, "The fracture finishes low");
		assertTrue(overhead.local(Joint.RIGHT_FOREARM).rotation().x() < -.5F);
		assertTrue(release.local(Joint.RIGHT_FOREARM).rotation().x() > -.5F);
		for (float age = StoneFractureRules.PLANT; age <= StoneFractureRules.TELL + 4; age += .125F) {
			Pose pose = sample(8, age, false);
			point(new Vec3(-2.75F, 22, 1.8F), pose.world(Joint.RIGHT_FOOT).transform(0, 0, 0));
			point(new Vec3(2.75F, 22, -1.8F), pose.world(Joint.LEFT_FOOT).transform(0, 0, 0));
		}
	}

	@Test
	void everySchoolFrameKeepsRigidBonesLevelFeetMirroredHandsAndImmutablePalettes() {
		for (int move : new int[] {7, 8}) for (float age = 0; age < tell(move) + recovery(move) + 1; age += .125F) {
			Pose right = sample(move, age, false), left = sample(move, age, true);
			for (Joint joint : Joint.values()) {
				for (float value : right.world(joint).values()) assertTrue(Float.isFinite(value));
				for (Vec3 p : new Vec3[] {Vec3.ZERO, new Vec3(1, 2, -3)}) {
					Vec3 a = right.world(joint).transform(p), b = left.world(joint.opposite()).transform(-p.x(), p.y(), p.z());
					point(new Vec3(-a.x(), a.y(), a.z()), b);
				}
				if (joint.parent() != null) {
					float length = right.world(joint).transform(0, 0, 0).minus(right.world(joint.parent()).transform(0, 0, 0)).length();
					assertEquals(joint.bind().translation().length(), length, EPS);
				}
				float original = right.world(joint).get(0, 0);
				right.world(joint).values()[0] = Float.NaN;
				assertEquals(original, right.world(joint).get(0, 0));
			}
			for (Joint wrist : new Joint[] {Joint.RIGHT_HAND, Joint.LEFT_HAND}) {
				Rotation rotation = right.local(wrist).rotation();
				assertTrue(Math.abs(rotation.x()) <= .1801F && Math.abs(rotation.y()) <= .1501F && Math.abs(rotation.z()) <= .1801F,
					"Blade attitude stays on the child socket without excessive wrist folding");
			}
			for (Joint foot : new Joint[] {Joint.RIGHT_FOOT, Joint.LEFT_FOOT}) {
				Matrix m = right.world(foot);
				point(new Vec3(0, 1, 0), m.direction(new Vec3(0, 1, 0)));
				for (float x : new float[] {-2, 2}) for (float z : new float[] {-2, 2})
					assertTrue(m.transform(x, 2, z).y() <= 24 + EPS, "No foot penetrates the floor");
				if (!masterFootwork(move, age, tell(move))) assertEquals(24, m.transform(0, 2, 0).y(), EPS, "Sole settles outside the step");
			}
		}
	}

	@Test
	void allSharedBeatBoundariesAndExtremeValidTimingsStayContinuous() {
		for (int move : new int[] {7, 8}) for (boolean left : new boolean[] {false, true})
			for (int tell : new int[] {1, tell(move), 80}) for (int active : new int[] {1, 10}) for (int recovery : new int[] {1, 40, 120}) {
				float step = tell * GaleRepriseRules.GATHER / (float) GaleRepriseRules.TELL;
				float plant = tell * (GaleRepriseRules.GATHER + GaleRepriseRules.STEP_TICKS) / (float) GaleRepriseRules.TELL;
				float stonePlant = tell * StoneFractureRules.PLANT / (float) StoneFractureRules.TELL;
				float warning = tell * (StoneFractureRules.PLANT + StoneFractureRules.BRACE) / (float) StoneFractureRules.TELL;
				for (float edge : new float[] {0, step, (step + plant) / 2, plant, plant + (tell - plant) * .25F, stonePlant, warning,
					warning + (tell - warning) / 3, tell, tell + active, tell + active + Math.min(3, recovery * .2F), tell + active + recovery}) {
					Pose before = sampleMaster(move, edge - .0001F, tell, active, recovery, left);
					Pose after = sampleMaster(move, edge + .0001F, tell, active, recovery, left);
					for (Joint joint : Joint.values()) matrix(before.world(joint), after.world(joint), .03F);
				}
			}
	}

	private static int tell(int move) { return move == 7 ? GaleRepriseRules.TELL : StoneFractureRules.TELL; }
	private static int recovery(int move) { return (move == 7 ? GaleRepriseRules.RECOVERY : StoneFractureRules.RECOVERY) - 1; }
	private static Pose sample(int move, float age, boolean left) { return sampleMaster(move, age, tell(move), 1, recovery(move), left); }
	private static void point(Vec3 a, Vec3 b) { assertEquals(a.x(), b.x(), EPS); assertEquals(a.y(), b.y(), EPS); assertEquals(a.z(), b.z(), EPS); }
	private static void matrix(Matrix a, Matrix b, float tolerance) {
		for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++) assertEquals(a.get(row, col), b.get(row, col), tolerance);
	}
}
