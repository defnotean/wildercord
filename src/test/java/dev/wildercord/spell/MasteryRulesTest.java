package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Spell mastery's numbers: ranks and their thresholds, what each kind of cast is worth, the moment, repetition, practice, the trait caps and keys. */
class MasteryRulesTest {
	@Test
	void ranksRiseAtTheirThresholds() {
		assertEquals(1, MasteryRules.rank(0));
		assertEquals(1, MasteryRules.rank(99.9));
		assertEquals(2, MasteryRules.rank(100));
		assertEquals(3, MasteryRules.rank(350));
		assertEquals(4, MasteryRules.rank(1000));
		assertEquals(5, MasteryRules.rank(3000));
		assertEquals(5, MasteryRules.rank(1_000_000));
		assertEquals(1, MasteryRules.rank(-5));
		assertEquals(0, MasteryRules.threshold(1));
		assertEquals(1000, MasteryRules.threshold(MasteryRules.MASTER));
		// Each rank takes well over twice the last.
		for (int rank = 3; rank <= MasteryRules.MAX_RANK; rank++) {
			assertTrue(MasteryRules.threshold(rank) >= 2.5 * MasteryRules.threshold(rank - 1), "rank " + rank);
		}
		assertEquals(0.0, MasteryRules.progress(100), 1e-9);
		assertEquals(0.5, MasteryRules.progress(225), 1e-9);
		assertEquals(1.0, MasteryRules.progress(5000), 1e-9);
		assertEquals(List.of("Kindled", "Practised", "Adept", "Master", "Mythic"),
			List.of(MasteryRules.name(1), MasteryRules.name(2), MasteryRules.name(3), MasteryRules.name(4), MasteryRules.name(5)));
	}

	@Test
	void ranksTwoToFiveEachOpenOneTraitSlot() {
		assertEquals(-1, MasteryRules.slotOf(1));
		for (int rank = 2; rank <= MasteryRules.MAX_RANK; rank++) {
			int slot = MasteryRules.slotOf(rank);
			assertEquals(rank, MasteryRules.rankOf(slot));
		}
		assertEquals(MasteryRules.SLOTS, MasteryRules.MAX_RANK - 1);
	}

	@Test
	void realFoesAreWorthTheirStrikeTheirDamageAndTheKill() {
		assertEquals(1.0, MasteryRules.strike(1, 0, false), 1e-9);
		assertEquals(4.0, MasteryRules.strike(1, 1, true), 1e-9);
		// More than its whole health counts as its whole health.
		assertEquals(3.0, MasteryRules.strike(1, 5, false), 1e-9);
		assertEquals(0.0, MasteryRules.strike(0, 1, true), 1e-9);
		assertTrue(MasteryRules.strike(MasteryRules.BOSS, 0.05, false) > MasteryRules.strike(1, 0.05, false));
		assertTrue(MasteryRules.strike(MasteryRules.PLAYER, 0.5, false) < MasteryRules.strike(1, 0.5, false));
		// Heals: by what they truly restore, capped.
		assertEquals(1.0, MasteryRules.heal(4), 1e-9);
		assertEquals(MasteryRules.HEAL_MAX, MasteryRules.heal(400), 1e-9);
		assertEquals(0.0, MasteryRules.heal(-3), 1e-9);
		// A cast can never earn more than its cap, however many it strikes.
		assertTrue(MasteryRules.MAX_PER_CAST < 64 * MasteryRules.strike(1, 0, false));
	}

	@Test
	void dangerAndBossesAreWorthMoreButNotWithoutLimit() {
		assertEquals(1.0, MasteryRules.situation(1.0, 0, false, false), 1e-9);
		assertEquals(1.5, MasteryRules.situation(0.2, 0, false, false), 1e-9);
		assertEquals(1.3, MasteryRules.situation(1.0, MasteryRules.CROWD, false, false), 1e-9);
		assertEquals(1.5, MasteryRules.situation(1.0, MasteryRules.HORDE, false, false), 1e-9);
		assertEquals(1.25, MasteryRules.situation(1.0, 0, false, true), 1e-9);
		assertEquals(MasteryRules.MAX_SITUATION, MasteryRules.situation(0.1, 20, true, true), 1e-9);
		assertTrue(MasteryRules.danger(0.2, 0));
		assertTrue(MasteryRules.danger(1.0, MasteryRules.CROWD));
		assertFalse(MasteryRules.danger(1.0, 1));
	}

	@Test
	void theSameThingInTheSamePlaceIsWorthLessAndLess() {
		assertEquals(1.0, MasteryRules.repetition(0), 1e-9);
		assertTrue(MasteryRules.repetition(10) < MasteryRules.repetition(5));
		assertEquals(MasteryRules.REPEAT_FLOOR, MasteryRules.repetition(10_000), 1e-9);
		// A place is half forgotten after the half-life, and fully remembered right away.
		assertEquals(8.0, MasteryRules.forget(8, 0), 1e-9);
		assertEquals(4.0, MasteryRules.forget(8, MasteryRules.REPEAT_HALF_LIFE), 1e-9);
		assertEquals(2.0, MasteryRules.forget(8, 2 * MasteryRules.REPEAT_HALF_LIFE), 1e-9);
		// A mob farm: 400 casts in ten minutes is worth almost nothing per cast by the end.
		double recent = 0;
		double last = 1;
		for (int i = 0; i < 400; i++) {
			last = MasteryRules.repetition(recent);
			recent = MasteryRules.forget(recent, 30) + 1;
		}
		assertTrue(last < 0.15, "a farm's casts end up worth " + last);
	}

	@Test
	void dummiesAndThePracticeArenaTeachOnlySoMuch() {
		assertEquals(5.0, MasteryRules.practice(0, 10), 1e-9);
		assertEquals(MasteryRules.PRACTICE_CAP, MasteryRules.practice(0, 100_000), 1e-9);
		assertEquals(0.0, MasteryRules.practice(MasteryRules.PRACTICE_CAP, 10), 1e-9);
		assertTrue(MasteryRules.PRACTICE_CAP < MasteryRules.threshold(2), "practice alone must not reach Practised");
	}

	/**
	 * The tuning, worked through: a spell used as a main attack in ordinary play. Two meaningful casts a minute (between
	 * travelling, mining and building), each striking one or two monsters for about a third of their health and slaying
	 * one in three; now and then a crowd or a dungeon; repetition taking about a fifth off overall.
	 */
	@Test
	void masterTakesAFewHoursOfRealPlay() {
		double perCast = 1.5 * MasteryRules.strike(1, 0.33, false) + 0.33 * MasteryRules.KILL;
		double moment = 0.85 * MasteryRules.situation(1.0, 1, false, false) + 0.15 * MasteryRules.situation(1.0, MasteryRules.CROWD, false, false);
		double perHour = 2 * 60 * perCast * moment * 0.8;
		double master = MasteryRules.threshold(MasteryRules.MASTER) / perHour;
		assertTrue(master >= 2.5 && master <= 4.5, "Master after " + master + " hours");
		assertTrue(MasteryRules.threshold(2) / perHour <= 0.5, "Practised after " + MasteryRules.threshold(2) / perHour + " hours");
		assertTrue(MasteryRules.threshold(MasteryRules.MYTHIC) / perHour >= 2 * master, "Mythic should take well past Master");
	}

	@Test
	void traitsNeverStackIntoAOneShot() {
		assertEquals(MasteryRules.MAX_TRAIT_POWER, MasteryRules.traitPower(1.15 * 1.15 * 1.12, false), 1e-9);
		assertEquals(1.05, MasteryRules.traitPower(1.1, true), 1e-9);
		assertEquals(1.0, MasteryRules.traitPower(0.8, false), 1e-9);
		assertTrue(MasteryRules.traitPower(10, true) <= 1.1 + 1e-9);
		assertTrue(MasteryRules.COST_FLOOR >= 0.8 && MasteryRules.COOLDOWN_FLOOR >= 0.8);
	}

	@Test
	void aSpellIsItsExactSequence() {
		assertEquals("wildercord:bolt wildercord:fire", MasteryRules.key(List.of("wildercord:bolt", "wildercord:fire")));
		assertNotEquals(MasteryRules.key(List.of("wildercord:fire", "wildercord:bolt")), MasteryRules.key(List.of("wildercord:bolt", "wildercord:fire")));
		String longId = "wildercord:knot/" + "a".repeat(900);
		String key = MasteryRules.key(List.of(longId, "wildercord:fire"));
		assertTrue(key.startsWith("#") && key.length() < 64, key);
		assertEquals(key, MasteryRules.key(List.of(longId, "wildercord:fire")));
		// The hash is the same everywhere (FNV-1a).
		assertEquals(0xcbf29ce484222325L, MasteryRules.hash(""));
		assertEquals(0xaf63dc4c8601ec8cL, MasteryRules.hash("a"));
	}
}
