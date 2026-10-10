package dev.wildercord.town;

import dev.wildercord.town.BountyRules.Bounty;
import dev.wildercord.town.BountyRules.Tier;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BountyRulesTest {
	private static final UUID PLAYER = UUID.fromString("7c0a1b2e-1111-4222-8333-944455556666");

	@Test
	void theSameBoardShowsTheSameBountyAllDay() {
		assertEquals(BountyRules.offer(PLAYER, 12, 99L), BountyRules.offer(PLAYER, 12, 99L));
		Set<Bounty> days = new HashSet<>();
		for (long day = 0; day < 40; day++) days.add(BountyRules.offer(PLAYER, day, 99L));
		assertTrue(days.size() > 10, "tomorrow brings a different hunt");
	}

	@Test
	void everyBountyIsFairAndPaysWithinItsBounds() {
		Set<String> seen = new HashSet<>();
		for (long day = 0; day < 2000; day++) {
			Bounty bounty = BountyRules.offer(UUID.randomUUID(), day, day * 7);
			seen.add(bounty.target());
			assertTrue(bounty.needed() >= 3 && bounty.needed() <= 8, bounty.toString());
			assertTrue(bounty.emeralds() >= 3 && bounty.emeralds() <= BountyRules.EMERALDS_MAX, bounty.toString());
			assertTrue(bounty.reputation() >= BountyRules.REPUTATION_MIN && bounty.reputation() <= BountyRules.REPUTATION_MAX, bounty.toString());
		}
		assertEquals(BountyRules.TARGETS.size(), seen.size(), "every target comes up");
	}

	@Test
	void reputationClimbsThroughTheTiers() {
		assertEquals(Tier.STRANGER, Tier.of(0));
		assertEquals(Tier.STRANGER, Tier.of(14));
		assertEquals(Tier.KNOWN, Tier.of(15));
		assertEquals(Tier.FRIEND, Tier.of(40));
		assertEquals(Tier.HONOURED, Tier.of(500));
		assertEquals(Tier.KNOWN, Tier.STRANGER.next());
		assertNull(Tier.HONOURED.next());
		// Honoured takes about a dozen bounties: one a day, so a fortnight of the road.
		int bounties = (int) Math.ceil(Tier.HONOURED.needs / (double) ((BountyRules.REPUTATION_MIN + BountyRules.REPUTATION_MAX) / 2));
		assertTrue(bounties >= 8 && bounties <= 16, "bounties to Honoured: " + bounties);
	}

	@Test
	void killsCountOnlyNearTheBoardAndOneBountyADay() {
		assertTrue(BountyRules.near(100, 100));
		assertTrue(BountyRules.near(0, 256));
		assertFalse(BountyRules.near(200, 200));
		assertTrue(BountyRules.ready(-1, 0));
		assertFalse(BountyRules.ready(5, 5));
		assertTrue(BountyRules.ready(5, 6));
	}
}
