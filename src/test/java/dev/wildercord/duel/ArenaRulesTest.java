package dev.wildercord.duel;

import dev.wildercord.duel.ArenaRules.Rank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaRulesTest {
	@Test
	void evenRatingsMoveByHalfK() {
		assertEquals(0.5, ArenaRules.expected(1000, 1000), 1e-9);
		assertEquals(ArenaRules.K / 2, ArenaRules.change(1000, 1000, 1.0));
		assertEquals(-ArenaRules.K / 2, ArenaRules.change(1000, 1000, 0.0));
		assertEquals(0, ArenaRules.change(1000, 1000, 0.5));
	}

	@Test
	void beatingAStrongerCasterIsWorthMore() {
		assertTrue(ArenaRules.change(1000, 1400, 1.0) > ArenaRules.change(1400, 1000, 1.0));
		assertTrue(ArenaRules.change(1000, 1400, 1.0) <= ArenaRules.K);
		assertTrue(ArenaRules.change(1400, 1000, 0.5) < 0, "a draw against a weaker caster costs rating");
	}

	@Test
	void ranksClimbWithRating() {
		assertEquals(Rank.APPRENTICE, ArenaRules.rank(ArenaRules.START));
		assertEquals(Rank.ADEPT, ArenaRules.rank(1100));
		assertEquals(Rank.DUELLIST, ArenaRules.rank(1250));
		assertEquals(Rank.ARCHMAGE, ArenaRules.rank(1499));
		assertEquals(Rank.GRANDMASTER, ArenaRules.rank(2000));
	}

	@Test
	void seasonsCarryRatingsHalfwayHome() {
		assertEquals(0, ArenaRules.season(0));
		assertEquals(1, ArenaRules.season(24000L * ArenaRules.SEASON_DAYS));
		assertEquals(1200, ArenaRules.carried(1400));
		assertEquals(900, ArenaRules.carried(800));
		assertEquals(1100, ArenaRules.carried(1400, 2));
		assertEquals(1400, ArenaRules.carried(1400, 0));
	}

	@Test
	void anOldStandingIsBroughtUpToTheSeason() {
		ArenaLadder.Standing old = new ArenaLadder.Standing("a", 1400, 5, 2, 0);
		ArenaLadder.Standing now = old.in(1);
		assertEquals(1200, now.rating());
		assertEquals(0, now.wins() + now.losses());
		assertEquals(old, old.in(0));
	}
}
