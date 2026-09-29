package dev.wildercord.cast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The numbers of the signature fusions, against their rune descriptions. */
class SignatureRulesTest {
	@Test
	void frostwireHitsTheFrozenHarder() {
		assertEquals(4.0, SignatureRules.frostwireDamage(false), 1e-9);
		assertEquals(6.0, SignatureRules.frostwireDamage(true), 1e-9);
		assertEquals(6, SignatureRules.FROSTWIRE_CHAIN);
		assertEquals(6.0, SignatureRules.FROSTWIRE_REACH, 1e-9);
	}

	@Test
	void seetheScaldsEveryHalfSecond() {
		assertEquals(4, SignatureRules.seetheScalds(40), "four over 2 seconds");
		assertEquals(8, SignatureRules.seetheScalds(80), "Extended, twice as many");
		assertEquals(1, SignatureRules.seetheScalds(3), "never none");
		assertEquals(4.0, SignatureRules.SEETHE_BURST, 1e-9);
	}

	@Test
	void skyburstGoesOffAtTheTopOfItsFlight() {
		assertFalse(SignatureRules.skyburstApex(2, 0.0), "not straight away, even if it hits a ceiling");
		assertFalse(SignatureRules.skyburstApex(8, 0.6), "still rising");
		assertTrue(SignatureRules.skyburstApex(8, 0.0));
		assertTrue(SignatureRules.skyburstApex(8, -0.2));
		assertTrue(SignatureRules.skyburstApex(20, 1.0), "never later than a second");
		assertEquals(1.0, SignatureRules.blastFalloff(0, 3), 1e-9);
		assertEquals(0.7, SignatureRules.blastFalloff(3, 3), 1e-9);
		assertEquals(0.7, SignatureRules.blastFalloff(9, 3), 1e-9, "no weaker than at its edge");
	}

	@Test
	void stitchtimeHealsBackWhatItCountedUpToItsCap() {
		assertEquals(5.0, SignatureRules.stitchBack(5, 12), 1e-9);
		assertEquals(12.0, SignatureRules.stitchBack(30, 12), 1e-9);
		assertEquals(0.0, SignatureRules.stitchBack(0, 12), 1e-9);
		// Renewed, it runs on, but never past three times its length from when it began.
		assertEquals(150, SignatureRules.stitchUntil(0, 80, 70, 80));
		assertEquals(240, SignatureRules.stitchUntil(0, 200, 190, 80));
		assertEquals(80, SignatureRules.stitchUntil(0, 80, 0, 80), "cast twice at once, it doesn't grow");
	}

	@Test
	void aParasiteTakesWhatItHadLeft() {
		assertEquals(4, SignatureRules.parasiteLeft(6, 2));
		assertEquals(1, SignatureRules.parasiteLeft(6, 6), "a second at least");
	}

	@Test
	void doomclockWindsTwoAtATimeFourTimesASecondAtMost() {
		double wound = 0;
		for (int i = 0; i < 10; i++) {
			wound = SignatureRules.wind(wound);
		}
		assertEquals(12.0, wound, 1e-9, "12 at most");
		assertEquals(2.0, SignatureRules.wind(0), 1e-9);
		assertTrue(SignatureRules.winds(Long.MIN_VALUE, 100));
		assertFalse(SignatureRules.winds(100, 104));
		assertTrue(SignatureRules.winds(100, 105));
		assertEquals(8.0, SignatureRules.doomclockBurst(0), 1e-9);
		assertEquals(20.0, SignatureRules.doomclockBurst(12), 1e-9);
		assertEquals(20.0, SignatureRules.doomclockBurst(99), 1e-9);
	}

	@Test
	void aHaloSmitesEveryTwoSeconds() {
		assertEquals(5, SignatureRules.haloSmites(160), "8 seconds");
		assertEquals(11, SignatureRules.haloSmites(320), "Extended");
		assertEquals(1, SignatureRules.haloSmites(5));
		int smites = 0;
		for (int tick = 0; tick < 160; tick += 10) {
			if (SignatureRules.haloSmitesAt(tick)) {
				smites++;
			}
		}
		assertEquals(SignatureRules.haloSmites(160), smites, "the steps smite as often as the rule says");
		assertFalse(SignatureRules.haloSmitesAt(0));
		assertTrue(SignatureRules.haloSmitesAt(10));
		assertTrue(SignatureRules.haloSmitesAt(40));
	}

	@Test
	void thunderquakeIsHarderTheNearerTheHeart() {
		assertEquals(2.0, SignatureRules.quakeReach(1), 1e-9);
		assertEquals(6.0, SignatureRules.quakeReach(3), 1e-9);
		assertEquals(12.0, SignatureRules.quakeTotal(0.5, 1.0), 1e-9, "12 at the heart");
		assertEquals(8.0, SignatureRules.quakeTotal(3.0, 1.0), 1e-9);
		assertEquals(4.0, SignatureRules.quakeTotal(5.5, 1.0), 1e-9);
		assertEquals(0.0, SignatureRules.quakeTotal(7.0, 1.0), 1e-9);
		assertEquals(12.0, SignatureRules.quakeTotal(2.5, 1.5), 1e-9, "Widened, every wave reaches further");
	}

	@Test
	void theCometIsWeakerAtItsEdge() {
		assertEquals(16.0, SignatureRules.cometDamage(0, 4), 1e-9);
		assertEquals(9.6, SignatureRules.cometDamage(4, 4), 1e-9);
		assertEquals(5, SignatureRules.COMET_SHARDS);
		assertEquals(4.0, SignatureRules.COMET_SHARD_DAMAGE, 1e-9);
	}

	@Test
	void aDustDevilDriftsThreeBlocksASecond() {
		assertEquals(0.75, SignatureRules.devilDrift(10), 1e-9);
		assertEquals(0.2, SignatureRules.devilDrift(0.5), 1e-9, "it stops at its quarry");
		assertEquals(0.0, SignatureRules.devilDrift(0.1), 1e-9);
		assertEquals(3.0, SignatureRules.DEVIL_STEP * 20 / 5, 1e-9);
	}

	@Test
	void malisonPassesOnWhatWasLeft() {
		assertEquals(100, SignatureRules.malisonLeft(200, 100));
		assertEquals(20, SignatureRules.malisonLeft(105, 100), "a second at least");
	}

	@Test
	void avalancheFallsHarderOnABareHead() {
		assertEquals(6.0, SignatureRules.avalancheDamage(false), 1e-9);
		assertEquals(9.0, SignatureRules.avalancheDamage(true), 1e-9);
	}
}
