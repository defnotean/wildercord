package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Each pack B signature has one plain answer, and the wrong answer is still punished. */
class MastersPackBSignatureRulesTest {
	@Test
	void constellationStarsBurstInDrawnOrderAndLeavingTheColumnIsTheAnswer() {
		int previous = 0;
		for (int i = 0; i < StarlitConstellationRules.STARS; i++) {
			int burst = StarlitConstellationRules.burst(i);
			int gap = burst - previous;
			assertTrue(gap >= (i == 0 ? 12 : StarlitConstellationRules.GAP), "star " + i + " warns for " + gap);
			assertTrue(gap <= 14, "star " + i);
			previous = burst;
		}
		assertEquals(StarlitConstellationRules.TELL, previous);
		assertThrows(IllegalArgumentException.class, () -> StarlitConstellationRules.burst(4));
		assertThrows(IllegalArgumentException.class, () -> StarlitConstellationRules.offset(-1));
		// Standing still is hit by the first star; each other star sits far enough away that one step clears it.
		assertTrue(StarlitConstellationRules.hits(0, 0, 0));
		for (int i = 1; i < StarlitConstellationRules.STARS; i++) {
			double[] star = StarlitConstellationRules.offset(i);
			assertFalse(StarlitConstellationRules.hits(star[0], star[1], 0), "Standing at the start is outside star " + i);
			assertTrue(StarlitConstellationRules.hits(0, 0, 0), "Standing on star " + i + " when it bursts is hit");
			assertTrue(Math.hypot(star[0], star[1]) > 2 * StarlitConstellationRules.RADIUS - .8);
		}
		assertFalse(StarlitConstellationRules.hits(StarlitConstellationRules.RADIUS + .1, 0, 0));
		assertFalse(StarlitConstellationRules.hits(0, 0, StarlitConstellationRules.HIGH + .1), "The column has a top");
		assertFalse(StarlitConstellationRules.hits(Double.NaN, 0, 0));
		assertTrue(StarlitConstellationRules.eligible(MastersPackB.STARLIT, 1, 4, 0, 100, 0, 0));
		assertFalse(StarlitConstellationRules.eligible(MastersPackB.STARLIT, 2, 4, 0, 100, 0, 0));
		assertFalse(StarlitConstellationRules.eligible(MastersPackB.STARLIT, 1, 1, 0, 100, 0, 0), "Too close to read the stars");
		assertFalse(StarlitConstellationRules.eligible(MastersPackB.STARLIT, 1, 4, 0, 30, 0, 0), "Keeps a guard's worth of Aura");
		assertFalse(StarlitConstellationRules.eligible(MastersPackB.STARLIT, 1, 4, 0, 100, 5, 10), "Cooldown");
		assertFalse(StarlitConstellationRules.eligible(MastersPackB.HOURGLASS, 1, 4, 0, 100, 0, 0));
	}

	@Test
	void rewindReplaysTheSameLaneSoASidestepClearsBothAndSteppingBackInDoesNot() {
		assertTrue(HourglassRewindRules.STRIKE >= 12 && HourglassRewindRules.STRIKE <= 14, "The first cut tells 12 to 14 ticks");
		assertTrue(HourglassRewindRules.replayTell() >= 12 && HourglassRewindRules.replayTell() <= 14, "So does the replay");
		assertEquals(HourglassRewindRules.REPLAY, HourglassRewindRules.TELL);
		assertTrue(HourglassRewindRules.hits(3, 0, 0), "Standing in the lane");
		assertTrue(HourglassRewindRules.hits(HourglassRewindRules.REACH, HourglassRewindRules.HALF_WIDTH, 0));
		assertFalse(HourglassRewindRules.hits(3, HourglassRewindRules.HALF_WIDTH + .3, 0), "A sidestep");
		assertFalse(HourglassRewindRules.hits(HourglassRewindRules.REACH + .5, 0, 0), "Backing out of reach");
		assertFalse(HourglassRewindRules.hits(-1, 0, 0), "Behind the master");
		assertFalse(HourglassRewindRules.hits(3, 0, 3), "Far above");
		assertFalse(HourglassRewindRules.hits(Double.NaN, 0, 0));
		assertTrue(HourglassRewindRules.eligible(MastersPackB.HOURGLASS, 2, 3, 0, 100, 0, 0));
		assertFalse(HourglassRewindRules.eligible(MastersPackB.HOURGLASS, 2, HourglassRewindRules.REACH, 0, 100, 0, 0));
		assertFalse(HourglassRewindRules.eligible(MastersPackB.HOURGLASS, 1, 3, 0, 100, 0, 0));
	}

	@Test
	void frenzyBeatsEachHaveTheirOwnAnswerAndAWrongAnswerIsHit() {
		var beats = CrimsonFrenzyRules.BEATS;
		int previous = 0;
		for (int beat : beats) {
			assertTrue(beat - previous >= 12 && beat - previous <= 14);
			previous = beat;
		}
		assertEquals(CrimsonFrenzyRules.TELL, previous);
		// Low cut: jump clears it, crouching does not.
		assertTrue(CrimsonFrenzyRules.hits(0, 2, 0, 0, false));
		assertTrue(CrimsonFrenzyRules.hits(0, 2, 0, 0, true));
		assertFalse(CrimsonFrenzyRules.hits(0, 2, 0, 1, false));
		// High cut: crouching clears it, jumping does not.
		assertTrue(CrimsonFrenzyRules.hits(1, 2, 0, 0, false));
		assertTrue(CrimsonFrenzyRules.hits(1, 2, 0, 1, false));
		assertFalse(CrimsonFrenzyRules.hits(1, 2, 0, 0, true));
		// Lane: a sidestep clears it, crouching and jumping do not.
		assertTrue(CrimsonFrenzyRules.hits(2, 3, 0, 1, true));
		assertFalse(CrimsonFrenzyRules.hits(2, 3, CrimsonFrenzyRules.LANE_WIDTH + .3, 0, false));
		// Ring: only leaving the radius clears it, even behind the master.
		assertTrue(CrimsonFrenzyRules.hits(3, -2, 0, 0, true));
		assertTrue(CrimsonFrenzyRules.hits(3, 0, 3, 0, false));
		assertFalse(CrimsonFrenzyRules.hits(3, CrimsonFrenzyRules.REACH[3] + .2, 0, 0, false));
		assertFalse(CrimsonFrenzyRules.hits(4, 0, 0, 0, false));
		assertFalse(CrimsonFrenzyRules.hits(-1, 0, 0, 0, false));
		assertFalse(CrimsonFrenzyRules.hits(0, Double.NaN, 0, 0, false));
		assertEquals(4, CrimsonFrenzyRules.SHAPES.length);
		assertEquals(4, java.util.Set.of(CrimsonFrenzyRules.SHAPES).stream().map(MastersPackB::answer).distinct().count(),
			"Four beats, four different answers");
	}

	@Test
	void frenzyCostsBloodAndOnlyLandedBeatsHeal() {
		assertEquals(6, CrimsonFrenzyRules.price(200), 1e-9);
		assertEquals(10, CrimsonFrenzyRules.heal(200), 1e-9);
		assertEquals(0, CrimsonFrenzyRules.price(Double.NaN));
		assertEquals(0, CrimsonFrenzyRules.heal(-1));
		assertTrue(CrimsonFrenzyRules.recovery(0) > CrimsonFrenzyRules.recovery(1), "A clean read leaves the master open longer");
		assertEquals(CrimsonFrenzyRules.RECOVERY, CrimsonFrenzyRules.recovery(4));
		assertTrue(CrimsonFrenzyRules.recovery(0) >= 14 && CrimsonFrenzyRules.recovery(1) <= 16 + CrimsonFrenzyRules.MISS_RECOVERY);
		assertTrue(CrimsonFrenzyRules.eligible(MastersPackB.CRIMSON, 3, 3, 0, 100, 200, 200, 0, 0));
		assertFalse(CrimsonFrenzyRules.eligible(MastersPackB.CRIMSON, 3, 3, 0, 100, 12, 200, 0, 0), "Never bleeds itself out");
		assertFalse(CrimsonFrenzyRules.eligible(MastersPackB.CRIMSON, 3, 6, 0, 100, 200, 200, 0, 0));
		assertFalse(CrimsonFrenzyRules.eligible(MastersPackB.CRIMSON, 2, 3, 0, 100, 200, 200, 0, 0));
		assertFalse(CrimsonFrenzyRules.eligible(MastersPackB.CRIMSON, 3, 3, 0, 100, 200, 0, 0, 0));
	}

	@Test
	void signaturesRotateIntoDifferentSlotsAndKeepThePauseBetweenExchanges() {
		for (MastersRules.Move move : MastersRules.Move.values()) if (MastersPackB.signature(move)) {
			assertTrue(move.recovery >= 14 && move.recovery <= 16, move.name());
			assertTrue(MastersPackB.cooldown(move) - move.tell - move.recovery >= 40, move.name());
		}
	}
}
