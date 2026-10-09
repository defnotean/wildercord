package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmberKilnRulesTest {
	@Test void innerOuterAndVerticalAnswersAreDifferentFromASweep() {
		for (int degrees = 0; degrees < 360; degrees++) {
			double angle = Math.toRadians(degrees);
			assertTrue(EmberKilnRules.hits(4 * Math.cos(angle), 4 * Math.sin(angle), 0));
			assertFalse(EmberKilnRules.hits(2.5 * Math.cos(angle), 2.5 * Math.sin(angle), 0));
			assertFalse(EmberKilnRules.hits(5.75 * Math.cos(angle), 5.75 * Math.sin(angle), 0));
		}
		assertFalse(EmberKilnRules.hits(4, 0, .76), "A normal timed jump clears the low band");
		assertFalse(EmberKilnRules.hits(4, 0, -.06), "The pulse never follows downhill terrain");
		assertFalse(EmberKilnRules.hits(Double.NaN, 0, 0));
		assertFalse(EmberKilnRules.hits(4, 0, Double.POSITIVE_INFINITY));
		assertTrue(EmberKilnRules.hits(-4, 0, 0), "The ring also threatens behind its fixed owner");
		assertFalse(MastersRules.hits(MastersRules.Move.SWEEP, -4, 0, 0));
	}

	@Test void admissionIsFinitePaidAndCooldownBoundedWithoutPhaseAcceleration() {
		assertTrue(EmberKilnRules.eligible(0, 1, 4, 0, 28, 180, 180));
		assertFalse(EmberKilnRules.eligible(0, 1, 4, 0, 28, 179, 180));
		for (double aura : new double[] {27.99, -1, 101, Double.NaN, Double.POSITIVE_INFINITY})
			assertFalse(EmberKilnRules.eligible(0, 1, 4, 0, aura, 180, 180));
		for (int school : new int[] {-1, 1, 2, 3}) assertFalse(EmberKilnRules.eligible(school, 1, 4, 0, 100, 200, 0));
		for (int sequence : new int[] {0, 2, 3, 4}) assertFalse(EmberKilnRules.eligible(0, sequence, 4, 0, 100, 200, 0));
		for (int phase = 0; phase < 3; phase++) {
			assertEquals(40, MastersRules.Move.KILN_RING.tell);
			assertEquals(48, MastersRules.Move.KILN_RING.recovery);
			assertFalse(EmberWakeRules.next(0, 1, phase, 4), "The new ordinary slot never displaces a due Wake");
		}
		assertTrue(EmberKilnRules.DAMAGE <= MastersRules.Move.SWEEP.damage);
		assertTrue(EmberKilnRules.INNER > 2.5, "Eight participants have a meaningful inner pocket around the body");
		assertTrue(EmberKilnRules.ESCAPE > EmberKilnRules.OUTER + .6);
	}

	@Test void thinAndBoundaryCoverMaskWholeSectorsConservatively() {
		assertTrue(EmberKilnRules.blocksSector(1.99, 2.01, -.01, .01, 0));
		assertTrue(EmberKilnRules.blocksSector(1.99, 2.01, -.01, .01, 31), "The wrap seam cannot leak through thin cover");
		assertFalse(EmberKilnRules.blocksSector(1.99, 2.01, -.01, .01, 16));
		for (int sector = 0; sector < EmberKilnRules.SECTORS; sector++) {
			double angle = EmberKilnRules.angle(sector) + .01;
			double x = Math.cos(angle) * 2, z = Math.sin(angle) * 2;
			assertTrue(EmberKilnRules.blocksSector(x - .001, x + .001, z - .001, z + .001, sector));
			assertTrue(EmberKilnRules.blocksSector(-.1, .1, -.1, .1, sector), "Cover over the origin masks every direction");
			assertFalse(EmberKilnRules.blocksSector(20, 21, 20, 21, sector), "Distant cover cannot veto the arena");
		}
		assertTrue(EmberKilnRules.blocksSector(Double.NaN, 2, 0, 1, 0));
		assertEquals(-1, EmberKilnRules.sector(0, 0));
		assertEquals(-1, EmberKilnRules.sector(Double.NaN, 1));
	}

	@Test void everyPointWithinCoverHasItsSectorMasked() {
		for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
			for (double dx : new double[] {0, .15, .5, .85, 1}) for (double dz : new double[] {0, .15, .5, .85, 1}) {
				double px = x + dx, pz = z + dz;
				int sector = EmberKilnRules.sector(px, pz);
				if (sector >= 0 && px * px + pz * pz <= EmberKilnRules.OUTER * EmberKilnRules.OUTER)
					assertTrue(EmberKilnRules.blocksSector(x, x + 1, z, z + 1, sector));
			}
		}
	}
}
