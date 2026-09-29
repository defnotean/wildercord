package dev.wildercord.spell;

import dev.wildercord.spell.PlayerAffinity.Source;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** A player's own affinities: levels and thresholds, what each level gives, the cost shares, the daily allowances, leaning. */
class PlayerAffinityTest {
	@Test
	void levelsRiseAtTheirThresholds() {
		assertEquals(0, PlayerAffinity.level(0));
		assertEquals(0, PlayerAffinity.level(99));
		assertEquals(1, PlayerAffinity.level(100));
		assertEquals(1, PlayerAffinity.level(599));
		assertEquals(2, PlayerAffinity.level(600));
		assertEquals(3, PlayerAffinity.level(1800));
		assertEquals(4, PlayerAffinity.level(4500));
		assertEquals(5, PlayerAffinity.level(10000));
		assertEquals(5, PlayerAffinity.level(1_000_000));
		assertEquals(0, PlayerAffinity.level(-5));
		// Each threshold is well past the last: a level takes real play.
		for (int level = 2; level <= PlayerAffinity.MAX_LEVEL; level++) {
			assertTrue(PlayerAffinity.threshold(level) >= PlayerAffinity.threshold(level - 1) * 2, "level " + level);
		}
		assertEquals(0, PlayerAffinity.threshold(0));
		assertEquals(100, PlayerAffinity.next(0));
		assertEquals(600, PlayerAffinity.next(100));
		assertEquals(-1, PlayerAffinity.next(10000));
		assertEquals(0.0, PlayerAffinity.progress(100), 1e-9);
		assertEquals(0.5, PlayerAffinity.progress(350), 1e-9);
		assertEquals(1.0, PlayerAffinity.progress(20000), 1e-9);
	}

	@Test
	void eachLevelGivesALittle() {
		assertEquals(1.0, PlayerAffinity.power(0), 1e-9);
		assertEquals(1.03, PlayerAffinity.power(1), 1e-9);
		assertEquals(1.15, PlayerAffinity.power(5), 1e-9);
		assertEquals(1.15, PlayerAffinity.power(9), 1e-9);
		// Resisting only from III: 10%, 15%, 20%.
		assertEquals(0.0, PlayerAffinity.resistance(1), 1e-9);
		assertEquals(0.0, PlayerAffinity.resistance(2), 1e-9);
		assertEquals(0.10, PlayerAffinity.resistance(3), 1e-9);
		assertEquals(0.15, PlayerAffinity.resistance(4), 1e-9);
		assertEquals(0.20, PlayerAffinity.resistance(5), 1e-9);
		assertEquals(0.8, PlayerAffinity.damageTaken(5), 1e-9);
		assertEquals(1.0, PlayerAffinity.damageTaken(2), 1e-9);
	}

	@Test
	void aMasteredElementMakesItsShareCheaper() {
		Map<String, Double> fire = SpellCompiler.elementShares(SpellCompiler.compile(List.of(Runes.BOLT, Runes.FIRE)).root());
		assertEquals(Map.of("fire", 1.0), fire);
		Map<String, Integer> mastered = Map.of("fire", PlayerAffinity.threshold(5));
		assertEquals(0.9, PlayerAffinity.costFactor(fire, mastered), 1e-9);
		assertTrue(PlayerAffinity.anyMastered(mastered));
		// Level IV isn't enough.
		Map<String, Integer> almost = Map.of("fire", PlayerAffinity.threshold(5) - 1);
		assertEquals(1.0, PlayerAffinity.costFactor(fire, almost), 1e-9);
		assertFalse(PlayerAffinity.anyMastered(almost));
		// Half fire and half frost (both cost the same): 5% off.
		Map<String, Double> mixed = SpellCompiler.elementShares(SpellCompiler.compile(List.of(Runes.BOLT, Runes.FIRE, Runes.FROST)).root());
		assertEquals(0.5, mixed.get("fire"), 1e-9);
		assertEquals(0.5, mixed.get("frost"), 1e-9);
		assertEquals(0.95, PlayerAffinity.costFactor(mixed, mastered), 1e-9);
		// An Amplified fire is the dearer part of the spell, so it's the bigger share.
		Map<String, Double> amplified = SpellCompiler.elementShares(SpellCompiler.compile(List.of(Runes.BOLT, Runes.FIRE, Runes.AMPLIFY, Runes.FROST)).root());
		assertTrue(amplified.get("fire") > amplified.get("frost"), amplified.toString());
		assertEquals(1.0, amplified.get("fire") + amplified.get("frost"), 1e-9);
		// A spell with no effects has nothing to share.
		assertTrue(SpellCompiler.elementShares(SpellCompiler.compile(List.of(Runes.BOLT)).root()).isEmpty());
	}

	@Test
	void allowancesStopFarming() {
		// Under the allowance, in full.
		assertEquals(3.0, PlayerAffinity.grant(Source.FISH, 0, 3), 1e-9);
		// Crossing it, only what fits.
		assertEquals(2.0, PlayerAffinity.grant(Source.FISH, Source.FISH.daily - 2, 3), 1e-9);
		// Past it, nothing at all.
		assertEquals(0.0, PlayerAffinity.grant(Source.FISH, Source.FISH.daily, 3), 1e-9);
		assertEquals(0.0, PlayerAffinity.grant(Source.STONE, 1000, 0.1), 1e-9);
		// Casting slows to a tenth past its allowance rather than stopping: mana already limits it.
		assertEquals(10.0, PlayerAffinity.grant(Source.CAST, 0, 10), 1e-9);
		assertEquals(1.0, PlayerAffinity.grant(Source.CAST, Source.CAST.daily, 10), 1e-9);
		assertEquals(5.0 + 0.5, PlayerAffinity.grant(Source.CAST, Source.CAST.daily - 5, 10), 1e-9);
		assertEquals(0.0, PlayerAffinity.grant(Source.CAST, 0, -1), 1e-9);
		// A 150-block stone farm fills the day's stone, and no more.
		double earned = 0;
		double granted = 0;
		for (int i = 0; i < 400; i++) {
			granted += PlayerAffinity.grant(Source.STONE, earned, Source.STONE.points);
			earned += Source.STONE.points;
		}
		assertEquals(Source.STONE.daily, granted, 1e-6);
		// Days run by game time, so sleeping through a night doesn't hand out a fresh allowance.
		assertEquals(0, PlayerAffinity.day(23999));
		assertEquals(1, PlayerAffinity.day(24000));
		assertEquals(-1, PlayerAffinity.day(-1));
	}

	@Test
	void everySourceIsSensible() {
		Set<String> ids = new HashSet<>();
		for (Source source : Source.values()) {
			assertTrue(ids.add(source.id), "two sources called " + source.id);
			assertTrue(source.general() || PlayerAffinity.isElement(source.element), source.id);
			assertTrue(source.points > 0 && source.daily > 0, source.id);
			assertTrue(source.tail >= 0 && source.tail < 1, source.id);
			// No everyday thing alone reaches level I in a single day.
			assertTrue(source.daily < PlayerAffinity.threshold(1) + 1, source.id);
		}
		for (String element : Affinity.ELEMENTS) {
			long own = PlayerAffinity.sources(element).stream().filter(s -> !s.general()).count();
			assertTrue(own >= 3, element + " should have at least three everyday ways to grow (has " + own + ")");
		}
		assertEquals("cast:fire", Source.CAST.tallyKey("fire"));
		assertEquals("fish", Source.FISH.tallyKey("fire"));
	}

	@Test
	void everyReactionFeedsItsElements() {
		for (String reaction : Feats.REACTIONS) {
			List<String> elements = PlayerAffinity.REACTION_ELEMENTS.get(reaction);
			assertNotNull(elements, reaction);
			assertFalse(elements.isEmpty(), reaction);
			elements.forEach(e -> assertTrue(PlayerAffinity.isElement(e), reaction + ": " + e));
		}
		assertEquals(List.of("fire", "frost"), PlayerAffinity.REACTION_ELEMENTS.get("shatter"));
	}

	@Test
	void aWholeNightMustBeWatched() {
		assertTrue(PlayerAffinity.nightWatched(PlayerAffinity.NIGHT_CHECKS, PlayerAffinity.NIGHT_CHECKS));
		assertTrue(PlayerAffinity.nightWatched(PlayerAffinity.NIGHT_CHECKS * 4 / 5, PlayerAffinity.NIGHT_CHECKS));
		assertFalse(PlayerAffinity.nightWatched(PlayerAffinity.NIGHT_CHECKS / 2, PlayerAffinity.NIGHT_CHECKS));
		assertFalse(PlayerAffinity.nightWatched(0, 0));
	}

	@Test
	void castsFromBeforeGiveAStartButNotMore() {
		assertEquals(0, PlayerAffinity.seed(0));
		assertEquals(80, PlayerAffinity.seed(40));
		assertEquals(PlayerAffinity.threshold(2), PlayerAffinity.seed(100000));
		assertEquals(2, PlayerAffinity.level(PlayerAffinity.seed(Integer.MAX_VALUE)));
	}

	@Test
	void aFirstLevelIsASmallGrimoireEntry() {
		assertEquals(PlayerAffinity.REWARD, Feats.reward(PlayerAffinity.KEY_PREFIX + "frost"));
		assertTrue(PlayerAffinity.REWARD < Feats.reward("feat:" + Feats.OVERCAST));
		// Not part of a full Grimoire: it isn't a discovery everyone can be sure of.
		assertFalse(Feats.everyEntry().contains(PlayerAffinity.KEY_PREFIX + "frost"));
	}

	@Test
	void leaningFollowsTheDeepestAffinity() {
		assertEquals("", Leaning.of(Map.of("frost", 99)));
		assertEquals("frost", Leaning.of(Map.of("frost", 100, "fire", 20)));
		assertEquals("", Leaning.of(Map.of("frost", 2000, "fire", 1900)));
		assertEquals("fire", Leaning.of(Map.of("frost", 2000, "fire", 2600)));
	}
}
