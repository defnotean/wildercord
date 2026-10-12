package dev.wildercord.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SandmawRulesTest {
	@Test
	void itFeelsFootstepsButBarelyASneak() {
		assertTrue(SandmawRules.feels(12, false));
		assertFalse(SandmawRules.feels(12, true));
		assertTrue(SandmawRules.feels(4, true));
	}

	@Test
	void itRisesUnderItsPreyOrWhenTiredOfWaiting() {
		assertTrue(SandmawRules.rises(1, SandmawRules.MIN_BURROW));
		assertFalse(SandmawRules.rises(1, SandmawRules.MIN_BURROW - 1), "only just went down");
		assertFalse(SandmawRules.rises(4, SandmawRules.MIN_BURROW), "not under them yet");
		assertTrue(SandmawRules.rises(20, SandmawRules.MAX_BURROW), "tired of waiting");
	}

	@Test
	void upTopItSlamsTheNearSpitsAtTheFarAndDivesAfterTheFled() {
		assertEquals(SandmawRules.Move.SLAM, SandmawRules.choose(3, 100, 100));
		assertEquals(SandmawRules.Move.SPIT, SandmawRules.choose(9, 100, 100));
		assertEquals(SandmawRules.Move.DIVE, SandmawRules.choose(SandmawRules.SPIT_RANGE + 2, 100, 100));
		assertEquals(SandmawRules.Move.NONE, SandmawRules.choose(3, 99, 100), "still recovering");
	}

	@Test
	void theSlamStrikesAlongItsLineOnly() {
		assertTrue(SandmawRules.inLine(4, 0.5));
		assertFalse(SandmawRules.inLine(4, 2), "stepped aside");
		assertFalse(SandmawRules.inLine(SandmawRules.SLAM_LENGTH + 1, 0), "beyond its reach");
		assertFalse(SandmawRules.inLine(-2, 0), "behind it");
	}

	@Test
	void theSpitStrikesDownItsCone() {
		assertTrue(SandmawRules.inSpit(8, 0.95));
		assertFalse(SandmawRules.inSpit(8, 0.3), "off to the side");
		assertFalse(SandmawRules.inSpit(SandmawRules.SPIT_RANGE + 1, 1), "out of range");
	}

	@Test
	void itDivesWhenUpLongOrHurtEnough() {
		assertTrue(SandmawRules.dives(SandmawRules.MAX_SURFACED, 0, 90));
		assertTrue(SandmawRules.dives(10, 25, 90));
		assertFalse(SandmawRules.dives(10, 10, 90));
	}

	@Test
	void itCannotBeHurtUnderTheSandAndIsOpenWithItsHeadStuck() {
		assertEquals(0, SandmawRules.hurt(10, true, false));
		assertEquals(15, SandmawRules.hurt(10, false, true));
		assertEquals(10, SandmawRules.hurt(10, false, false));
	}

	@Test
	void itIsTheSoftestElite() {
		assertEquals(EliteTier.BRUTE, SandmawRules.TIER);
		assertTrue(EliteTier.BRUTE.health < EliteTier.CHAMPION.health && EliteTier.CHAMPION.health < EliteTier.WARLORD.health);
		assertTrue(EliteTier.WARLORD.health < dev.wildercord.aura.world.BossTier.APPRENTICE.health, "below every guardian");
	}
}
