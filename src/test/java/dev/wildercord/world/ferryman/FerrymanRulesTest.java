package dev.wildercord.world.ferryman;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static dev.wildercord.world.ferryman.FerrymanRules.*;

class FerrymanRulesTest {
	@Test void everyPhaseGivesEveryMoveATellAndRecovery() {
		for (boolean phase : new boolean[] {false, true}) {
			var seen = new java.util.HashSet<Integer>();
			for (int turn = 0; turn < 3; turn++) {
				int move = nextMove(turn, phase);
				assertTrue(seen.add(move));
				assertTrue(tell(move) >= 32 && recovery(move) >= 24);
				assertEquals(move, nextMove(turn + 3, phase));
			}
		}
		assertFalse(second(90.1F, 180)); assertTrue(second(90, 180));
		assertEquals(SWEEP, nextMove(0, false)); assertEquals(WAKE, nextMove(0, true));
	}
	@Test void sweepLocksItsDirectionAndLeavesSidesBackAndOuterRimClear() {
		assertTrue(sweep(0, 6.5, 0)); assertFalse(sweep(0, 6.501, 0));
		assertTrue(sweep(3, 3, 0)); assertFalse(sweep(4, 0, 0)); assertFalse(sweep(0, -2, 0));
		assertTrue(sweep(6, 0, Math.PI / 2)); assertFalse(sweep(0, 6, Math.PI / 2));
		assertTrue(sweep(0, 0, 0));
	}
	@Test void anchorUsesTheMarkedPlaceRatherThanFollowingThePlayer() {
		assertTrue(anchor(8, -4, 8, -4)); assertTrue(anchor(10.25, -4, 8, -4));
		assertFalse(anchor(10.251, -4, 8, -4)); assertFalse(anchor(0, 0, 8, -4));
	}
	@Test void bothWakeOrientationsLeaveAContinuousDryCrossing() {
		for (double along = -ARENA; along <= ARENA; along += .25) {
			for (double side = -SAFE_HALF_WIDTH; side <= SAFE_HALF_WIDTH; side += .25) {
				assertFalse(wake(side, along, false)); assertFalse(wake(along, side, true));
			}
			assertTrue(wake(SAFE_HALF_WIDTH + .01, along, false));
			assertTrue(wake(along, -SAFE_HALF_WIDTH - .01, true));
		}
		assertFalse(wake(15, 8, false));
	}
	@Test void noAttackHitsOutsideTheArenaOrAboveItsReadableFloorBand() {
		for (int move : new int[] {SWEEP, ANCHOR, WAKE}) {
			assertFalse(hits(move, 15, 0, 0, Math.PI / 2, 15, 0, false));
			assertFalse(hits(move, 4, 4.01, 4, 0, 4, 4, false));
			assertFalse(hits(move, 4, -1.01, 4, 0, 4, 4, false));
			assertFalse(hits(move, Double.NaN, 0, 0, 0, 0, 0, false));
		}
		assertFalse(hits(IDLE, 0, 0, 0, 0, 0, 0, false));
	}
}
