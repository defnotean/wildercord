package dev.wildercord.aura.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildercord.aura.world.MethodsBSignatureRules.Signature;
import org.junit.jupiter.api.Test;

class MethodsBSignatureRulesTest {
	@Test
	void theTollCatchesItsLaneAndOnlyItsLane() {
		assertTrue(MethodsBSignatureRules.tollHits(4, 0, 0));
		assertTrue(MethodsBSignatureRules.tollHits(MethodsBSignatureRules.TOLL_REACH, MethodsBSignatureRules.TOLL_HALF_WIDTH, 0));
		// Answer: leave the lane.
		assertFalse(MethodsBSignatureRules.tollHits(4, MethodsBSignatureRules.TOLL_HALF_WIDTH + 0.3, 0));
		assertFalse(MethodsBSignatureRules.tollHits(4, -2, 0));
		assertFalse(MethodsBSignatureRules.tollHits(MethodsBSignatureRules.TOLL_REACH + 0.5, 0, 0));
		assertFalse(MethodsBSignatureRules.tollHits(-2, 0, 0), "behind the Master is out of the lane");
		// A jump does not clear it; a high ledge does.
		assertTrue(MethodsBSignatureRules.tollHits(4, 0, 1.25));
		assertFalse(MethodsBSignatureRules.tollHits(4, 0, 3));
	}

	@Test
	void theGlareCatchesOnlyThoseLookingAtTheMaster() {
		assertTrue(MethodsBSignatureRules.glareHits(5, 1));
		assertTrue(MethodsBSignatureRules.glareHits(MethodsBSignatureRules.GLARE_RANGE, 0.9));
		// Answer: turn away. Side-on or back turned is safe, wherever they stand.
		assertFalse(MethodsBSignatureRules.glareHits(2, 0));
		assertFalse(MethodsBSignatureRules.glareHits(2, -1));
		assertFalse(MethodsBSignatureRules.glareHits(2, MethodsBSignatureRules.GLARE_FACING));
		assertFalse(MethodsBSignatureRules.glareHits(MethodsBSignatureRules.GLARE_RANGE + 1, 1));
		assertFalse(MethodsBSignatureRules.glareHits(Double.NaN, 1));
		assertFalse(MethodsBSignatureRules.glareHits(3, Double.NaN));
		// Through the dispatcher the glare ignores where in front the player stands.
		assertTrue(MethodsBSignatureRules.hits(Signature.GLARE, -3, 4, 0, 1, 90));
		assertFalse(MethodsBSignatureRules.hits(Signature.GLARE, 2, 0, 0, -0.2, 90));
	}

	@Test
	void theCoilLeavesOneGapAtItsSide() {
		for (long began : new long[] {0, 1, 2, 7}) {
			double gap = MethodsBSignatureRules.gapDegrees(began);
			assertEquals(90, Math.abs(gap));
			double r = 3;
			// The gap's middle is safe; the far side and the front are not.
			assertFalse(MethodsBSignatureRules.coilHits(Math.cos(Math.toRadians(gap)) * r, Math.sin(Math.toRadians(gap)) * r, 0, gap));
			assertTrue(MethodsBSignatureRules.coilHits(-Math.cos(Math.toRadians(gap)) * r, -Math.sin(Math.toRadians(gap)) * r, 0, gap));
			assertTrue(MethodsBSignatureRules.coilHits(r, 0, 0, gap));
			assertTrue(MethodsBSignatureRules.coilHits(-r, 0, 0, gap));
			// Just inside the gap's edge is safe, just outside it is not.
			double half = MethodsBSignatureRules.COIL_GAP_DEGREES / 2;
			double in = Math.toRadians(gap + half - 3), out = Math.toRadians(gap + half + 3);
			assertFalse(MethodsBSignatureRules.coilHits(Math.cos(in) * r, Math.sin(in) * r, 0, gap));
			assertTrue(MethodsBSignatureRules.coilHits(Math.cos(out) * r, Math.sin(out) * r, 0, gap));
			// Outside the ring, or high above it, is safe; a jump is not.
			assertFalse(MethodsBSignatureRules.coilHits(MethodsBSignatureRules.COIL_RADIUS + 0.5, 0, 0, gap));
			assertTrue(MethodsBSignatureRules.coilHits(r, 0, 1.25, gap));
			assertFalse(MethodsBSignatureRules.coilHits(r, 0, 3, gap));
		}
		// It alternates sides cast to cast, so standing still in the last gap is not an answer.
		assertEquals(-MethodsBSignatureRules.gapDegrees(4), MethodsBSignatureRules.gapDegrees(5));
		assertEquals(MethodsBSignatureRules.gapDegrees(-2), MethodsBSignatureRules.gapDegrees(2));
		assertFalse(MethodsBSignatureRules.coilHits(Double.NaN, 0, 0, 90));
	}

	@Test
	void wrapBringsAnglesIntoTheHalfTurn() {
		assertEquals(0, MethodsBSignatureRules.wrap(360), 1e-9);
		assertEquals(-90, MethodsBSignatureRules.wrap(270), 1e-9);
		assertEquals(90, MethodsBSignatureRules.wrap(-270), 1e-9);
		assertEquals(170, MethodsBSignatureRules.wrap(170), 1e-9);
		assertEquals(-170, MethodsBSignatureRules.wrap(190), 1e-9);
		for (double d = -1000; d <= 1000; d += 7.3) {
			double w = MethodsBSignatureRules.wrap(d);
			assertTrue(w >= -180 && w <= 180, String.valueOf(d));
		}
	}
}
