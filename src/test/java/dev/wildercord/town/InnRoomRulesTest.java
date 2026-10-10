package dev.wildercord.town;

import dev.wildercord.town.BountyRules.Tier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InnRoomRulesTest {
	@Test
	void keysCostLessAsTheKeepersKnowYou() {
		assertEquals(6, InnRoomRules.price(Tier.STRANGER));
		assertEquals(3, InnRoomRules.price(Tier.HONOURED));
		for (Tier tier : Tier.values()) {
			if (tier.next() != null) assertTrue(InnRoomRules.price(tier.next()) < InnRoomRules.price(tier));
		}
	}

	@Test
	void aKeyAddsToAStayStillRunning() {
		long rent = InnRoomRules.RENT_DAYS * BountyRules.DAY;
		assertEquals(1000 + rent, InnRoomRules.extend(0, 1000));
		assertEquals(5000 + rent, InnRoomRules.extend(5000, 1000));
		assertEquals(9000 + rent, InnRoomRules.extend(5000, 9000));
	}

	@Test
	void aStayRunsOut() {
		assertTrue(InnRoomRules.rented(100, 99));
		assertFalse(InnRoomRules.rented(100, 100));
		assertEquals(InnRoomRules.RENT_DAYS, InnRoomRules.daysLeft(InnRoomRules.extend(0, 0), 0));
		assertEquals(1, InnRoomRules.daysLeft(10, 9));
		assertEquals(0, InnRoomRules.daysLeft(10, 10));
	}
}
