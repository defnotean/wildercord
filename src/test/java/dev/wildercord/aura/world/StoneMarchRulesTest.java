package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StoneMarchRulesTest {
	@Test void adjoiningBandsAreExclusiveAndKeepAllThreeCounterRoutes() {
		assertEquals(-1, StoneMarchRules.band(1.49999, 0, 0));
		assertEquals(0, StoneMarchRules.band(1.5, 0, 0));
		assertEquals(0, StoneMarchRules.band(3.49999, 0, 0));
		assertEquals(1, StoneMarchRules.band(3.5, 0, 0));
		assertEquals(1, StoneMarchRules.band(5.49999, 0, 0));
		assertEquals(2, StoneMarchRules.band(5.5, 0, 0));
		assertEquals(2, StoneMarchRules.band(7.5, 0, 0));
		assertEquals(-1, StoneMarchRules.band(7.50001, 0, 0));
		for (int band = 0; band < 3; band++) {
			double forward = StoneMarchRules.start(band) + 1;
			assertTrue(StoneMarchRules.hits(band, forward, 1.75, .65));
			assertTrue(StoneMarchRules.hits(band, forward, -1.75, -.05));
			assertFalse(StoneMarchRules.hits(band, forward, 1.75001, 0), "A side step leaves the entire lane");
			assertFalse(StoneMarchRules.hits(band, forward, 0, .65001), "Feet above the low band clear its one pulse");
			assertFalse(StoneMarchRules.hits(band, forward, 0, -.05001), "The wave cannot propagate down a drop");
			assertFalse(StoneMarchRules.hits(band, forward - 2, 0, 0), "Moving inward stays behind the front");
		}
		for (double bad : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
			assertEquals(-1, StoneMarchRules.band(bad, 0, 0));
			assertEquals(-1, StoneMarchRules.band(2.5, bad, 0));
			assertEquals(-1, StoneMarchRules.band(2.5, 0, bad));
		}
	}

	@Test void pulseClocksHaveNoCatchupAndPreserveFullExposure() {
		for (int age = -10; age < 250; age++) {
			int expected = age == 32 ? 0 : age == 40 ? 1 : age == 48 ? 2 : -1;
			assertEquals(expected, StoneMarchRules.pulse(age));
		}
		assertEquals(96, StoneMarchRules.END);
		assertEquals(48, StoneMarchRules.RECOVERY);
		assertEquals(StoneMarchRules.END, MastersRules.Move.STONE_FAULT_MARCH.tell + MastersRules.Move.STONE_FAULT_MARCH.recovery);
		assertEquals(30, LegacyMasterMoves.auraCost(MastersRules.Move.STONE_FAULT_MARCH, MastersRules.STONE));
		assertEquals(26.4, MastersRules.damage(8, MastersRules.STONE, MastersRules.Move.STONE_FAULT_MARCH), 1e-9);
	}

	@Test void ordinaryStoneSlotRetainsFiniteBudgetRestAndExactSchool() {
		assertTrue(StoneMarchRules.eligible(2, 3, 2.5, 0, 30, 220, 220));
		assertTrue(StoneMarchRules.eligible(2, 7, 6, .15, 100, 440, 220));
		assertFalse(StoneMarchRules.eligible(2, 3, 2.5, 0, 100, 219, 220));
		for (int school : new int[] {-1, 0, 1, 3}) assertFalse(StoneMarchRules.eligible(school, 3, 2.5, 0, 100, 220, 0));
		for (int sequence : new int[] {-1, 0, 1, 2, 4, 5, 6}) assertFalse(StoneMarchRules.eligible(2, sequence, 2.5, 0, 100, 220, 0));
		for (double aura : new double[] {-1, 28, 29.999, 100.01, Double.NaN, Double.POSITIVE_INFINITY})
			assertFalse(StoneMarchRules.eligible(2, 3, 2.5, 0, aura, 220, 0));
	}

	@Test void residentChecksIncludeTheActualNativeCollisionHaloAtBothChunkSeams() {
		assertEquals(14, StoneMarchRules.collisionMin(16));
		assertEquals(17, StoneMarchRules.collisionMax(16));
		assertEquals(-18, StoneMarchRules.collisionMin(-16));
		assertEquals(-15, StoneMarchRules.collisionMax(-16));
		assertEquals(14, StoneMarchRules.collisionMin(15.75));
		assertEquals(17, StoneMarchRules.collisionMax(16.25));
	}

	@Test void entireThinCollisionBoxesAndRotatedLaneBoundariesAreConservative() {
		for (int degrees = 0; degrees < 360; degrees++) {
			double ax = Math.cos(Math.toRadians(degrees)), az = Math.sin(Math.toRadians(degrees));
			for (double forward : new double[] {1.5, 2.5, 3.5}) for (double side : new double[] {-1.75, 0, 1.75}) {
				double x = ax * forward - az * side, z = az * forward + ax * side;
				assertTrue(StoneMarchRules.overlaps(x - .00001, x + .00001, z - .00001, z + .00001, ax, az, 1.5, 3.5, 1.75));
			}
			assertFalse(StoneMarchRules.overlaps(100, 101, 100, 101, ax, az, 1.5, 3.5, 1.75));
		}
		assertFalse(StoneMarchRules.overlaps(2, 3, 2, 3, 1, 0, 1.5, 3.5, 1.75));
		assertTrue(StoneMarchRules.overlaps(2, 3, 1.7499, 1.7501, 1, 0, 1.5, 3.5, 1.75));
		assertTrue(StoneMarchRules.overlaps(Double.NaN, 3, 0, 1, 1, 0, 1.5, 3.5, 1.75));
	}

	@Test void externalAdmissionAddsExactlyOneSignatureAndEndsStonePhrase() {
		var planner = new MasterOrdinaryPlanner(MastersRules.STONE, 123);
		planner.admittedExternal(MastersRules.Move.THRUST);
		long before = planner.state().successfulDecisions();
		planner.admittedExternal(MastersRules.Move.STONE_FAULT_MARCH);
		assertEquals(before + 1, planner.state().successfulDecisions());
		assertEquals(0, planner.state().depth());
		assertEquals("wildercord:master/stone_fault_march", planner.state().history().getLast());
		assertThrows(IllegalArgumentException.class, () -> new MasterOrdinaryPlanner(MastersRules.GALE, 123).admittedExternal(MastersRules.Move.STONE_FAULT_MARCH));
	}
}
