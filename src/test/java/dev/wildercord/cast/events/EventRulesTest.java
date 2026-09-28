package dev.wildercord.cast.events;

import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The world events' pure rules: how often they roll, how long they last, their waves and their rewards. */
class EventRulesTest {
	@Test
	void chancesAddUpToTheIntendedFrequency() {
		// Rolled every check, the chance comes out at one event per intended stretch of time.
		double checksPerStormWindow = EventRules.STORM_EVERY_DAYS * EventRules.DAY / EventRules.CHECK_INTERVAL;
		assertEquals(1.0, EventRules.stormChance() * checksPerStormWindow, 1e-9);
		double checksPerStarWindow = EventRules.STAR_EVERY_NIGHTS * EventRules.NIGHT / EventRules.CHECK_INTERVAL;
		assertEquals(1.0, EventRules.starChance() * checksPerStarWindow, 1e-9);
		double checksPerRiftWindow = EventRules.RIFT_EVERY_NIGHTS * EventRules.NIGHT / EventRules.CHECK_INTERVAL;
		assertEquals(1.0, EventRules.riftChance() * checksPerRiftWindow, 1e-9);
		// All rare: well under one in fifty per check.
		assertTrue(EventRules.stormChance() < 0.02);
		assertTrue(EventRules.starChance() < 0.02);
		assertTrue(EventRules.riftChance() < 0.02);
		assertEquals(1.0, EventRules.chancePerCheck(0));
		assertEquals(1.0, EventRules.chancePerCheck(10));
	}

	@Test
	void stormsLastThreeToFiveMinutes() {
		assertEquals(3 * 60 * 20, EventRules.stormTicks(0));
		assertEquals(5 * 60 * 20, EventRules.stormTicks(0.9999999));
		assertEquals(5 * 60 * 20, EventRules.stormTicks(1.0));
		int middle = EventRules.stormTicks(0.5);
		assertTrue(middle > 3 * 60 * 20 && middle < 5 * 60 * 20);
		assertTrue(EventRules.STORM_COST < 1.0);
		assertEquals(1.0F, EventRules.STORM_REGEN_BONUS);
	}

	@Test
	void nightIsTheDarkPartOfTheDay() {
		assertFalse(EventRules.night(1000));
		assertFalse(EventRules.night(12999));
		assertTrue(EventRules.night(13000));
		assertTrue(EventRules.night(18000));
		assertTrue(EventRules.night(23000));
		assertFalse(EventRules.night(23001));
		// The clock keeps counting past one day.
		assertTrue(EventRules.night(24000 * 5 + 18000));
	}

	@Test
	void regionsAreBigSquares() {
		assertEquals(EventRules.region(0, 0), EventRules.region(511, 511));
		assertFalse(EventRules.region(0, 0) == EventRules.region(512, 0));
		assertFalse(EventRules.region(0, 0) == EventRules.region(0, 512));
		assertFalse(EventRules.region(-1, 0) == EventRules.region(0, 0));
		assertFalse(EventRules.region(512, 0) == EventRules.region(0, 512));
	}

	@Test
	void surgesAreRareAndMostlyKind() {
		assertEquals(EventRules.Surge.NONE, EventRules.surge(0.10, 0.0));
		assertEquals(EventRules.Surge.NONE, EventRules.surge(0.99, 0.5));
		assertEquals(EventRules.Surge.BIGGER, EventRules.surge(0.0, 0.0));
		assertEquals(EventRules.Surge.ELEMENT, EventRules.surge(0.05, 0.4));
		assertEquals(EventRules.Surge.ECHO, EventRules.surge(0.05, 0.7));
		assertEquals(EventRules.Surge.BACKFIRE, EventRules.surge(0.05, 0.9));
		// Over a fine sweep of rolls: 10% surge, and backfires are the rarest kind.
		Map<EventRules.Surge, Integer> counts = new EnumMap<>(EventRules.Surge.class);
		int n = 200;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				counts.merge(EventRules.surge((i + 0.5) / n, (j + 0.5) / n), 1, Integer::sum);
			}
		}
		assertEquals(0.9, counts.get(EventRules.Surge.NONE) / (double) (n * n), 0.001);
		int backfire = counts.get(EventRules.Surge.BACKFIRE);
		for (EventRules.Surge surge : List.of(EventRules.Surge.BIGGER, EventRules.Surge.ELEMENT, EventRules.Surge.ECHO)) {
			assertTrue(counts.get(surge) > backfire, surge.name());
		}
		assertEquals(EventRules.SURGE_POWER, EventRules.surgePower(EventRules.Surge.BIGGER));
		assertEquals(1.0, EventRules.surgePower(EventRules.Surge.ECHO));
		assertEquals(1.0, EventRules.surgePower(EventRules.Surge.NONE));
	}

	@Test
	void wavesGrowWithTheWaveAndThePlayers() {
		assertEquals(3, EventRules.waveSize(1, 1));
		assertEquals(4, EventRules.waveSize(2, 1));
		assertEquals(4, EventRules.waveSize(3, 1));
		assertEquals(5, EventRules.waveSize(1, 3));
		assertEquals(8, EventRules.waveSize(3, 3));
		for (int wave = 1; wave <= EventRules.WAVES; wave++) {
			for (int players = 1; players <= 20; players++) {
				int size = EventRules.waveSize(wave, players);
				assertTrue(size >= 1 && size <= EventRules.MAX_WAVE, "wave " + wave + " with " + players);
				assertTrue(size >= EventRules.waveSize(wave, players - 1 < 1 ? 1 : players - 1));
				int adepts = EventRules.adepts(wave, players);
				assertTrue(adepts >= 0 && adepts <= size);
			}
		}
		assertEquals(0, EventRules.adepts(1, 4));
		assertEquals(1, EventRules.adepts(2, 1));
		assertEquals(1, EventRules.adepts(3, 1));
		assertEquals(2, EventRules.adepts(3, 3));
		// Three waves, in order, all out within about two minutes.
		assertTrue(EventRules.waveAt(1) < EventRules.waveAt(2) && EventRules.waveAt(2) < EventRules.waveAt(3));
		assertTrue(EventRules.waveAt(3) <= 2 * 60 * 20);
		assertEquals(EventRules.waveAt(3), EventRules.waveAt(9));
	}

	@Test
	void rewardTables() {
		for (double roll = 0; roll < 1; roll += 0.01) {
			int guards = EventRules.guards(roll);
			assertTrue(guards >= 2 && guards <= 4);
			int blanks = EventRules.riftBlanks(roll);
			assertTrue(blanks >= 2 && blanks <= 4);
			int star = EventRules.starRuneTier(roll);
			assertTrue(star == 3 || star == 4);
			int rift = EventRules.riftRuneTier(roll);
			assertTrue(rift >= 2 && rift <= 4);
		}
		assertEquals(4, EventRules.guards(0.99));
		assertEquals(2, EventRules.guards(0.0));
		assertEquals(4, EventRules.starRuneTier(0.1));
		assertEquals(3, EventRules.starRuneTier(0.5));
		assertEquals(4, EventRules.riftRuneTier(0.01));
		assertEquals(3, EventRules.riftRuneTier(0.2));
		assertEquals(2, EventRules.riftRuneTier(0.8));
	}

	@Test
	void rewardRunesAreOfTheTierAndNeverInnate() {
		for (String source : List.of("starfall", "rift", "mana_storm")) {
			for (int tier = 1; tier <= 4; tier++) {
				List<RuneDef> pool = EventRules.rewardRunes(source, tier);
				assertFalse(pool.isEmpty(), source + " tier " + tier);
				for (RuneDef rune : pool) {
					assertEquals(tier, rune.tier(), rune.id());
					assertFalse(Runes.innate(rune), rune.id());
				}
			}
			// Every roll lands in the pool, the edges included.
			List<RuneDef> fours = EventRules.rewardRunes(source, 4);
			assertTrue(fours.contains(EventRules.rewardRune(source, 4, 0.0)));
			assertTrue(fours.contains(EventRules.rewardRune(source, 4, 0.9999999)));
			assertTrue(fours.contains(EventRules.rewardRune(source, 4, 1.0)));
		}
	}

	@Test
	void riftRewardsGrowWithTheWavesBeaten() {
		// Sealed early, with no wave beaten: a token; every wave beaten, the lot.
		assertEquals(0, EventRules.riftRunes(0));
		assertEquals(1, EventRules.riftRunes(1));
		assertEquals(EventRules.RIFT_RUNES, EventRules.riftRunes(EventRules.WAVES));
		assertEquals(EventRules.RIFT_RUNES, EventRules.riftRunes(EventRules.WAVES + 5));
		assertEquals(1.0, EventRules.riftShare(EventRules.WAVES), 1e-9);
		assertEquals(EventRules.RIFT_XP, EventRules.riftXp(EventRules.WAVES));
		assertFalse(EventRules.riftCrystal(0));
		assertTrue(EventRules.riftCrystal(1));
		for (int cleared = 0; cleared < EventRules.WAVES; cleared++) {
			assertTrue(EventRules.riftShare(cleared) < EventRules.riftShare(cleared + 1));
			assertTrue(EventRules.riftRunes(cleared) <= EventRules.riftRunes(cleared + 1));
			assertTrue(EventRules.riftXp(cleared) < EventRules.riftXp(cleared + 1));
			for (double roll : new double[] {0.0, 0.5, 0.99}) {
				assertTrue(EventRules.riftBlanks(roll, cleared) >= 1);
				assertTrue(EventRules.riftBlanks(roll, cleared) <= EventRules.riftBlanks(roll, cleared + 1));
			}
		}
		assertEquals(EventRules.riftBlanks(0.99), EventRules.riftBlanks(0.99, EventRules.WAVES));
		// It can't be sealed in its first wave.
		assertTrue(EventRules.RIFT_SEAL_FROM_WAVE > 1 && EventRules.RIFT_SEAL_FROM_WAVE <= EventRules.WAVES);
	}
}
