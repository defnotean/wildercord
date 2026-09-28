package dev.wildercord.cast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The numbers behind the storm and wind fused runes agree with what their descriptions promise. */
class FusedStormNumbersTest {
	@Test
	void aWebDealsThreeAndOneMoreForEveryOtherCaught() {
		assertEquals(0, FusedStormNumbers.weaveDamage(0));
		assertEquals(3, FusedStormNumbers.weaveDamage(1));
		assertEquals(4, FusedStormNumbers.weaveDamage(2));
		assertEquals(6, FusedStormNumbers.weaveDamage(4));
		// A web holds four at most: a fifth changes nothing.
		assertEquals(6, FusedStormNumbers.weaveDamage(5));
	}

	@Test
	void aSlamDealsFourAndOneMoreABlockUpToSix() {
		assertEquals(4, FusedStormNumbers.slamDamage(0));
		assertEquals(4, FusedStormNumbers.slamDamage(0.9));
		assertEquals(7, FusedStormNumbers.slamDamage(3));
		assertEquals(7, FusedStormNumbers.slamDamage(3.7));
		assertEquals(10, FusedStormNumbers.slamDamage(6));
		assertEquals(10, FusedStormNumbers.slamDamage(40));
		assertEquals(4, FusedStormNumbers.slamDamage(-2));
	}

	@Test
	void theHeartSkipsEveryTwoSecondsForSix() {
		assertEquals(3, FusedStormNumbers.heartSkips(120));
		// Extended: twice as long, twice the skips; cut short, fewer.
		assertEquals(6, FusedStormNumbers.heartSkips(240));
		assertEquals(1, FusedStormNumbers.heartSkips(72));
		assertEquals(0, FusedStormNumbers.heartSkips(30));
	}

	@Test
	void aCloudStrikesOnceASecond() {
		assertEquals(4, FusedStormNumbers.cloudStrikes(80));
		assertEquals(8, FusedStormNumbers.cloudStrikes(160));
		assertEquals(0, FusedStormNumbers.cloudStrikes(10));
	}

	@Test
	void theClockStrikesAtTwoAndFourSeconds() {
		assertEquals(40, FusedStormNumbers.CLOCK_STRIKES[0]);
		assertEquals(80, FusedStormNumbers.CLOCK_STRIKES[1]);
		assertEquals(0, FusedStormNumbers.CLOCK_STRIKES[0] % FusedStormNumbers.CLOCK_STEPS, "the hand should land on the hour");
	}

	@Test
	void throwsLandWhereTheDescriptionsSay() {
		for (double blocks : new double[] {FusedStormNumbers.RECOIL_THROW, FusedStormNumbers.GLYPH_THROW}) {
			double speed = FusedStormNumbers.throwSpeed(blocks);
			assertTrue(speed > 0.3 && speed < 2, "a sane throw speed for " + blocks + " blocks, not " + speed);
			assertEquals(blocks, FusedStormNumbers.glide(speed, FusedStormNumbers.HOP), 0.05);
		}
		// Harder throws go further.
		assertTrue(FusedStormNumbers.glide(1.0, FusedStormNumbers.HOP) > FusedStormNumbers.glide(0.5, FusedStormNumbers.HOP));
	}
}
