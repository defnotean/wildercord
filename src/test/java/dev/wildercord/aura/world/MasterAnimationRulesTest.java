package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MasterAnimationRulesTest {
	@Test
	void onlyLiveBoundedServerTimelinesAnimate() {
		for (int id : new int[] {-1, 0, 8, Integer.MAX_VALUE}) {
			assertSame(MasterAnimationRules.NONE, MasterAnimationRules.sample(id, 2, 18, 1, 19));
		}
		for (float age : new float[] {-1, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, 38, 200}) {
			assertSame(MasterAnimationRules.NONE, MasterAnimationRules.sample(1, age, 18, 1, 19));
		}
		for (int[] timing : new int[][] {{0, 1, 19}, {81, 1, 19}, {18, 0, 19}, {18, 11, 19}, {18, 1, 0}, {18, 1, 121}}) {
			assertSame(MasterAnimationRules.NONE, MasterAnimationRules.sample(1, 2, timing[0], timing[1], timing[2]));
		}
	}

	@Test
	void authoritativeHitIsAlwaysAtFullImpactAndRecoveryIsVisible() {
		for (var move : MastersRules.Move.values()) {
			int id = move.ordinal() + 1, tell = move.tell, active = 1, recovery = move.recovery - active;
			assertEquals(0, sample(move, 0).weight());
			assertEquals(1, sample(move, tell).weight());
			assertEquals(sample(move, tell), MasterAnimationRules.sample(id, 30, 30, 2, 40),
				"Balancing tell/recovery must never retime the visual impact relative to damage");
			assertTrue(sample(move, tell + active + 2).weight() > .99F, "The body follows through after damage");
			assertTrue(sample(move, tell + active + recovery - .01F).weight() < .0001F);
			assertSame(MasterAnimationRules.NONE, sample(move, tell + active + recovery));
		}
	}

	@Test
	void allFourAttacksHaveDistinctWeaponAndTorsoChoreography() {
		var sweep = sample(MastersRules.Move.SWEEP, MastersRules.Move.SWEEP.tell);
		var thrust = sample(MastersRules.Move.THRUST, MastersRules.Move.THRUST.tell);
		var crescent = sample(MastersRules.Move.CRESCENT, MastersRules.Move.CRESCENT.tell);
		var breaker = sample(MastersRules.Move.BREAK_CAST, MastersRules.Move.BREAK_CAST.tell);
		assertTrue(sweep.sword().y() < -.8F && sweep.body().y() < -.3F, "Sweep turns across its target");
		assertEquals(-80, thrust.bladeTilt(), .001F, "Thrust rotates the blade around its hilt to point forward");
		assertTrue(thrust.body().x() > .3F && thrust.stance() >= .3F, "The thrust commits a planted split stance");
		assertTrue(crescent.sword().x() < -2 && crescent.body().x() < 0, "Crescent follows a rising diagonal");
		assertTrue(breaker.bladeTilt() < -90 && breaker.sword().x() > thrust.sword().x(), "Breaker is a distinct downward point-first interruption");
		assertEquals(4, java.util.Set.of(sweep, thrust, crescent, breaker).size());
	}

	@Test
	void weightedTransformsRemainContinuousAtEveryBoundary() {
		for (var move : MastersRules.Move.values()) {
			float follow = move.tell + 1 + Math.min(3, (move.recovery - 1) * .20F);
			for (float at : new float[] {0, move.tell * .65F, move.tell, move.tell + 1, follow, move.tell + move.recovery}) {
				var before = values(sample(move, at - .0001F));
				var after = values(sample(move, at + .0001F));
				for (int n = 0; n < before.length; n++) assertEquals(before[n], after[n], .003F, move + " at " + at + " channel " + n);
			}
		}
	}

	@Test
	void stanceIsPlantedDuringTheStrikeAndRigidSolesDoNotEnterTheFloor() {
		for (var move : MastersRules.Move.values()) {
			float stance = sample(move, move.tell).stance();
			for (float age = move.tell * .65F; age <= move.tell + 3; age += .05F) {
				var pose = sample(move, age);
				assertEquals(stance, pose.stance(), .00001F, "Feet do not slide during release");
				assertEquals(1, pose.weight(), .00001F);
			}
			for (float weight = 0; weight <= 1; weight += .01F) {
				float angle = stance * weight, lower = MasterAnimationRules.lower(angle);
				for (int sign : new int[] {-1, 1}) {
					float sole = 12 + lower + 12 * (float) Math.cos(sign * angle) + 2 * Math.abs((float) Math.sin(sign * angle));
					assertEquals(24, sole, .00001F, "The lowest rigid sole edge stays on the floor");
				}
			}
		}
	}

	@Test
	void hipHingeKeepsTorsoAndShoulderAttachmentsConnectedAndMirrorsExactly() {
		for (var move : MastersRules.Move.values()) {
			for (float age = 0; age < move.tell + move.recovery; age += .1F) {
				var pose = sample(move, age);
				float lower = MasterAnimationRules.lower(pose.stance());
				var body = pose.body();
				var hips = MasterAnimationRules.pivot(body, lower, 0, 12, 0);
				assertEquals(0, hips.x(), .00001F);
				assertEquals(12 + lower, hips.y(), .00001F);
				assertEquals(0, hips.z(), .00001F);
				var neck = MasterAnimationRules.pivot(body, lower, 0, 0, 0);
				var right = MasterAnimationRules.pivot(body, lower, -5, 2, 0);
				var left = MasterAnimationRules.pivot(body, lower, 5, 2, 0);
				assertEquals(10, distance(right, left), .00001F, "Shoulder span is never stretched");
				assertEquals(Math.sqrt(29), distance(neck, right), .00001F);
				var mirrored = MasterAnimationRules.mirrored(body, true);
				var reflected = MasterAnimationRules.pivot(mirrored, lower, 5, 2, 0);
				assertEquals(-right.x(), reflected.x(), .00001F);
				assertEquals(right.y(), reflected.y(), .00001F);
				assertEquals(right.z(), reflected.z(), .00001F);
				assertEquals(body, MasterAnimationRules.mirrored(mirrored, true));
			}
		}
	}

	@Test
	void guardDodgeAndStaggerAreBoundedAndDoNotResurrectAnAttack() {
		assertSame(MasterAnimationRules.NONE, MasterAnimationRules.defence(0, 0, 0));
		assertSame(MasterAnimationRules.NONE, MasterAnimationRules.defence(Float.NaN, Float.POSITIVE_INFINITY, -3));
		assertEquals(1, MasterAnimationRules.defence(9, 0, 0).weight());
		assertEquals(.25F, MasterAnimationRules.defence(1, 1, .25F).weight());
		assertNotEquals(MasterAnimationRules.defence(1, 0, 0).body(), MasterAnimationRules.defence(0, 1, 0).body());
		assertEquals(0, MasterAnimationRules.defence(0, 1, 0).bladeTilt());
	}

	@Test
	void crosswindHasAReadableLateralGatherPlantAndDistinctReply() {
		var move = MastersRules.Move.CROSSWIND_REPRISE;
		assertEquals(7, move.ordinal() + 1);
		assertEquals(MasterAnimationRules.CROSSWIND_REPRISE, move.ordinal() + 1);
		var step = sample(move, GaleRepriseRules.GATHER);
		var plant = sample(move, GaleRepriseRules.GATHER + GaleRepriseRules.STEP_TICKS);
		var reply = sample(move, move.tell);
		assertTrue(step.body().z() > .25F && step.sword().x() < -1.5F, "Visible lean carries a high blade through lateral movement");
		assertTrue(plant.body().y() > .4F && plant.stance() > step.stance(), "The planted chamber precedes the separately warned reply");
		assertTrue(reply.bladeTilt() < -80 && reply.body().y() < 0, "The reply opens a compact point-first stroke");
		for (var other : MastersRules.Move.values()) if (other != move) assertNotEquals(reply, sample(other, other.tell));
		for (float at : new float[] {GaleRepriseRules.GATHER, GaleRepriseRules.GATHER + GaleRepriseRules.STEP_TICKS}) {
			var before = values(sample(move, at - .0001F));
			var after = values(sample(move, at + .0001F));
			for (int n = 0; n < before.length; n++) assertEquals(before[n], after[n], .003F, "Crosswind handoff channel " + n);
		}
	}

	@Test
	void acceptedCrescentElevationTiltsReleaseWithoutChangingTimingOrFootPlant() {
		var pose = sample(MastersRules.Move.CRESCENT, MastersRules.Move.CRESCENT.tell);
		var up = MasterAnimationRules.aimed(pose, MasterAnimationRules.CRESCENT, -65);
		var down = MasterAnimationRules.aimed(pose, MasterAnimationRules.CRESCENT, 65);
		assertTrue(up.sword().x() < pose.sword().x());
		assertTrue(down.sword().x() > pose.sword().x());
		assertTrue(up.body().x() < pose.body().x());
		assertEquals(pose.weight(), up.weight());
		assertEquals(pose.stance(), up.stance());
		assertEquals(MasterAnimationRules.aimed(pose, 3, 70), MasterAnimationRules.aimed(pose, 3, 10000));
		assertSame(pose, MasterAnimationRules.aimed(pose, 2, -60));
		assertSame(pose, MasterAnimationRules.aimed(pose, 3, Float.NaN));
		assertSame(MasterAnimationRules.NONE, MasterAnimationRules.aimed(MasterAnimationRules.NONE, 3, -60));
	}

	private static MasterAnimationRules.Pose sample(MastersRules.Move move, float age) {
		return MasterAnimationRules.sample(move.ordinal() + 1, age, move.tell, 1, move.recovery - 1);
	}

	private static float[] values(MasterAnimationRules.Pose pose) {
		float w = pose.weight();
		return new float[] {w, pose.body().x() * w, pose.body().y() * w, pose.body().z() * w,
			pose.sword().x() * w, pose.sword().y() * w, pose.sword().z() * w, pose.offhand().x() * w,
			pose.offhand().y() * w, pose.offhand().z() * w, pose.stance() * w, pose.bladeTilt() * w};
	}

	private static double distance(MasterAnimationRules.Pivot a, MasterAnimationRules.Pivot b) {
		return Math.sqrt(Math.pow(a.x() - b.x(), 2) + Math.pow(a.y() - b.y(), 2) + Math.pow(a.z() - b.z(), 2));
	}
}
