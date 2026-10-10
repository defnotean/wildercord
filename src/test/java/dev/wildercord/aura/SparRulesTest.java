package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sparring, the pure part ({@link SparRules}): when a spar may begin and why not, the ring and its edge, the count-in, what a spar teaches
 * (more to the winner, more from a stronger partner, never much), the daily allowance per pair, what makes a spar real, and each
 * swordsman's record.
 */
class SparRulesTest {
	private static final UUID A = UUID.nameUUIDFromBytes("a".getBytes());
	private static final UUID B = UUID.nameUUIDFromBytes("b".getBytes());

	private static boolean[] both(boolean value) {
		return new boolean[] {value, value};
	}

	// ------------------------------------------------------------------ when

	@Test
	void theRefusalsComeInOrder() {
		boolean[] yes = both(true);
		boolean[] no = both(false);
		double[] whole = {1.0, 1.0};
		assertNull(SparRules.refusal(true, yes, yes, yes, 2.0, true, no, no, whole));
		assertEquals(SparRules.Refusal.OFF, SparRules.refusal(false, no, no, no, 99, false, yes, yes, new double[] {0, 0}));
		assertEquals(SparRules.Refusal.NO_METHOD, SparRules.refusal(true, new boolean[] {true, false}, no, no, 99, false, yes, yes, whole));
		assertEquals(SparRules.Refusal.NO_WEAPON, SparRules.refusal(true, yes, new boolean[] {false, true}, no, 99, false, yes, yes, whole));
		assertEquals(SparRules.Refusal.UNHURTABLE, SparRules.refusal(true, yes, yes, new boolean[] {true, false}, 99, false, yes, yes, whole));
		assertEquals(SparRules.Refusal.TOO_FAR, SparRules.refusal(true, yes, yes, yes, SparRules.REACH + 0.1, true, yes, yes, whole));
		assertEquals(SparRules.Refusal.TOO_FAR, SparRules.refusal(true, yes, yes, yes, 1.0, false, yes, yes, whole), "not in the same world");
		assertEquals(SparRules.Refusal.BUSY, SparRules.refusal(true, yes, yes, yes, 2.0, true, new boolean[] {false, true}, yes, whole));
		assertEquals(SparRules.Refusal.AWAKENED, SparRules.refusal(true, yes, yes, yes, 2.0, true, no, new boolean[] {true, false}, whole));
		assertEquals(SparRules.Refusal.HURT, SparRules.refusal(true, yes, yes, yes, 2.0, true, no, no, new double[] {1.0, 0.45}));
		assertNull(SparRules.refusal(true, yes, yes, yes, 2.0, true, no, no, new double[] {0.5, 1.0}), "half their health is enough");
		assertEquals("message.wildercord.aura.spar.refused.too_far", SparRules.Refusal.TOO_FAR.key());
	}

	@Test
	void aSaluteIsDeliberateAndWaitsALittleWhile() {
		assertTrue(SparRules.REACH >= 3 && SparRules.REACH <= 6, "up close: the reach of using something on someone, and a little over");
		assertTrue(SparRules.OFFER_TICKS >= 200 && SparRules.OFFER_TICKS <= 1200, "time to turn and answer, not long enough to forget");
		assertTrue(SparRules.RESALUTE < SparRules.OFFER_TICKS);
		assertTrue(SparRules.READY_SHARE > 0 && SparRules.READY_SHARE < 1);
	}

	// ------------------------------------------------------------------ the ring

	@Test
	void theRingIsAFlatCircleAndLeavingItIsFree() {
		assertFalse(SparRules.outside(0, 0, 3, 4, 5), "on the edge is inside");
		assertTrue(SparRules.outside(0, 0, 3, 4.1, 5));
		assertFalse(SparRules.outside(10, -10, 10, -4, 7));
		assertTrue(SparRules.outside(10, -10, 10, -2.9, 7));
		assertEquals(2, SparRules.inside(0, 0, 3, 4, 7), 1e-9);
		assertTrue(SparRules.inside(0, 0, 9, 0, 7) < 0, "negative outside");
		assertTrue(SparRules.RING_RADIUS >= SparRules.MIN_RADIUS && SparRules.RING_RADIUS <= SparRules.MAX_RADIUS);
		assertTrue(SparRules.MIN_RADIUS > SparRules.REACH / 2 + 1, "the smallest ring holds two who saluted from as far as a salute reaches, with room");
		assertTrue(SparRules.EDGE_WARNING < SparRules.MIN_RADIUS / 2);
	}

	@Test
	void theCountInIsThreeSecondsAndEndsAtOneHeart() {
		assertEquals(3, SparRules.count(0));
		assertEquals(3, SparRules.count(19));
		assertEquals(2, SparRules.count(21));
		assertEquals(1, SparRules.count(59));
		assertEquals(0, SparRules.count(60));
		assertEquals(0, SparRules.count(400));
		assertEquals(2.0F, SparRules.KNOCKOUT, "one heart");
		assertTrue(SparRules.MAX_FIGHT_TICKS >= 1200, "long enough for a real fight");
		assertTrue(SparRules.RING_LIFE > SparRules.RING_EVERY, "each drawing of the ring outlasts the gap to the next");
	}

	// ------------------------------------------------------------------ what it teaches

	@Test
	void theWinnerLearnsMoreAndTheLoserStillLearns() {
		double road = SparRules.road(AuraRules.EDGE);
		assertEquals(AuraRules.threshold(AuraRules.FORM) - AuraRules.threshold(AuraRules.EDGE), road, 1e-9);
		double won = SparRules.xp(AuraRules.EDGE, road, AuraRules.EDGE, SparRules.Result.WON, 1.0);
		double lost = SparRules.xp(AuraRules.EDGE, road, AuraRules.EDGE, SparRules.Result.LOST, 1.0);
		double even = SparRules.xp(AuraRules.EDGE, road, AuraRules.EDGE, SparRules.Result.EVEN, 1.0);
		assertTrue(won > lost && lost > 0, "both learn, the winner more");
		assertEquals(lost, even, 1e-9, "an even spar teaches what a loss does");
		assertEquals(road * SparRules.XP_WIN, won, 1e-9);
		assertEquals(2 * won, SparRules.xp(AuraRules.EDGE, road, AuraRules.EDGE, SparRules.Result.WON, 2.0), 1e-9, "times the server's spar_xp");
		assertEquals(0, SparRules.xp(AuraRules.EDGE, road, AuraRules.EDGE, SparRules.Result.WON, 0), 1e-9);
		assertEquals(0, SparRules.xp(AuraRules.NONE, 0, AuraRules.EDGE, SparRules.Result.WON, 1.0), 1e-9, "nothing before a method");
		// Never less than the floor, however short the road.
		assertEquals(SparRules.XP_FLOOR, SparRules.xp(AuraRules.GLOW, 1, AuraRules.GLOW, SparRules.Result.LOST, 1.0), 1e-9);
		assertTrue(SparRules.xp(AuraRules.GLOW, SparRules.road(AuraRules.GLOW), AuraRules.GLOW, SparRules.Result.LOST, 1.0) >= SparRules.XP_FLOOR);
		assertEquals(0, SparRules.road(AuraRules.SOVEREIGN), 1e-9, "no road past the last stage");
	}

	@Test
	void aStrongerPartnerTeachesMoreAndAMuchWeakerOneLittle() {
		assertEquals(1.0, SparRules.partner(AuraRules.EDGE, AuraRules.EDGE), 1e-9);
		assertEquals(1.25, SparRules.partner(AuraRules.EDGE, AuraRules.FORM), 1e-9);
		assertEquals(1.5, SparRules.partner(AuraRules.GLOW, AuraRules.SOVEREIGN), 1e-9, "half again at most");
		assertEquals(0.6, SparRules.partner(AuraRules.EDGE, AuraRules.FLOW), 1e-9);
		assertEquals(0.25, SparRules.partner(AuraRules.SOVEREIGN, AuraRules.GLOW), 1e-9, "a master beating a beginner learns next to nothing");
		double road = SparRules.road(AuraRules.FORM);
		assertTrue(SparRules.xp(AuraRules.FORM, road, AuraRules.FLOW, SparRules.Result.WON, 1.0)
			< SparRules.xp(AuraRules.FORM, road, AuraRules.FORM, SparRules.Result.LOST, 1.0), "beating someone far below teaches less than losing to an equal");
	}

	@Test
	void aDaysSparringIsWorthLessThanRealFighting() {
		// Meaningful fighting earns about 300 experience an hour (AuraRules); a pair's whole day of counted spars, all won, teaches a few
		// hundredths of the road, so sparring is a way to practise, never a way round the road.
		for (int s = AuraRules.GLOW; s < AuraRules.SOVEREIGN; s++) {
			double road = SparRules.road(s);
			double day = SparRules.DAILY * SparRules.xp(s, road, s, SparRules.Result.WON, 1.0);
			assertTrue(day <= Math.max(road * 0.05, SparRules.DAILY * SparRules.XP_FLOOR) + 1e-9, "a day's spars at stage " + s + ": " + day);
			// The roads are three times longer since 0.12; a day's spars stay under three hours of real fighting.
			assertTrue(day < 900, "under three hours' real fighting");
		}
	}

	@Test
	void aSparIsRealOnlyAfterAFightWithBlowsBothWays() {
		assertTrue(SparRules.real(SparRules.MIN_FIGHT_TICKS, true));
		assertFalse(SparRules.real(SparRules.MIN_FIGHT_TICKS - 1, true), "stepping straight out of the ring teaches nothing");
		assertFalse(SparRules.real(SparRules.MAX_FIGHT_TICKS, false), "nor does standing still to be beaten");
		assertTrue(SparRules.MIN_FIGHT_TICKS >= 100 && SparRules.MIN_FIGHT_TICKS < SparRules.MAX_FIGHT_TICKS);
	}

	// ------------------------------------------------------------------ the allowance and the record

	@Test
	void onlySoManySparsADayCountForAnyOnePair() {
		long day = SparRules.day(5 * SparRules.DAY + 10);
		assertEquals(5, day);
		assertEquals(-1, SparRules.day(-1));
		SparRules.Log log = SparRules.Log.NONE;
		for (int i = 0; i < SparRules.DAILY; i++) {
			assertTrue(SparRules.counts(log.counted(B, day), SparRules.DAILY), "spar " + (i + 1) + " counts");
			log = log.count(B, day);
		}
		assertFalse(SparRules.counts(log.counted(B, day), SparRules.DAILY), "the next is for its own sake");
		assertEquals(0, log.counted(A, day), "another partner counts afresh");
		assertTrue(SparRules.counts(log.counted(A, day), SparRules.DAILY));
		assertEquals(0, log.counted(B, day + 1), "and so does a new day");
		SparRules.Log tomorrow = log.count(B, day + 1);
		assertEquals(1, tomorrow.counted(B, day + 1));
		assertEquals(0, tomorrow.counted(B, day), "yesterday's counts are forgotten");
		assertFalse(SparRules.counts(0, 0), "a server that counts none");
	}

	@Test
	void theRecordKeepsWinsLossesAndEvens() {
		SparRules.Log log = SparRules.Log.NONE.record(SparRules.Result.WON).record(SparRules.Result.WON).record(SparRules.Result.LOST)
			.record(SparRules.Result.EVEN);
		assertEquals(2, log.wins());
		assertEquals(1, log.losses());
		assertEquals(1, log.evens());
		assertEquals(4, log.total());
		SparRules.Log counted = log.count(A, 3);
		assertEquals(2, counted.wins(), "counting a spar keeps the record");
		assertEquals(1, counted.counted(A, 3));
		assertEquals(0, new SparRules.Log(0, 0, 0, 0, null).today().size(), "a missing map is an empty one");
	}
}
