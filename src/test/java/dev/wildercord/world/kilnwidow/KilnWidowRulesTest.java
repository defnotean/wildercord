package dev.wildercord.world.kilnwidow;

import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.wildercord.world.kilnwidow.KilnWidowRules.*;
import static org.junit.jupiter.api.Assertions.*;

class KilnWidowRulesTest {
	@Test
	void moveCycleRotatesCleanlyWhenSecondPhaseBegins() {
		assertEquals(List.of(DRAFT, SLAG, SWEEP, DRAFT),
			List.of(nextMove(0, false), nextMove(1, false), nextMove(2, false), nextMove(3, false)));
		assertEquals(List.of(SWEEP, DRAFT, SLAG, SWEEP),
			List.of(nextMove(0, true), nextMove(1, true), nextMove(2, true), nextMove(3, true)));
		assertFalse(second(91, 180));
		assertTrue(second(90, 180));
		assertTrue(second(1, 180));
		assertFalse(second(0, 0));
	}

	@Test
	void everyMoveHasReadableTellRecoveryDamageAndKey() {
		for (int move : List.of(DRAFT, SLAG, SWEEP)) {
			assertTrue(tell(move) >= 30, "Tell for " + move);
			assertTrue(recovery(move) >= 20, "Recovery for " + move);
			assertTrue(damage(move) >= 5, "Damage for " + move);
			assertNotEquals("idle", key(move));
		}
		assertEquals(0, tell(IDLE));
		assertEquals(RETIRE, recovery(IDLE));
		assertEquals(0, damage(IDLE));
		assertEquals("idle", key(IDLE));
	}

	@Test
	void annularDraftLeavesInnerHearthAndOuterFirebreakSafe() {
		assertFalse(hits(DRAFT, 0, 0, 2.2, 0, 0, false), "Inner cooled hearth is safe");
		assertTrue(hits(DRAFT, 0, 0, 3.0, 0, 0, false), "Inner draft boundary hits");
		assertTrue(hits(DRAFT, 0, 0, 4.8, 0, 0, false), "Mid draft ring hits");
		assertTrue(hits(DRAFT, 0, 0, 6.5, 0, 0, false), "Outer draft boundary hits");
		assertFalse(hits(DRAFT, 0, 0, 7.2, 0, 0, false), "Outer firebreak is safe");
		assertFalse(hits(DRAFT, 0, 6, 4.8, 0, 0, false), "Vertical bounds respected");
	}

	@Test
	void slagDropAndFlueSweepRespectLockedMarksAndSafeLanes() {
		assertTrue(hits(SLAG, 4.0, 0, -3.0, 4.0, -3.0, false));
		assertTrue(hits(SLAG, 4.0 + SLAG_RADIUS, 0, -3.0, 4.0, -3.0, false));
		assertFalse(hits(SLAG, 4.0 + SLAG_RADIUS + 0.2, 0, -3.0, 4.0, -3.0, false));

		assertFalse(hits(SWEEP, 0, 0, 5.0, 0, 0, false), "North-south firebreak lane is safe when acrossX is false");
		assertTrue(hits(SWEEP, 4.5, 0, 5.0, 0, 0, false), "Side flue outside north-south lane hits");

		assertFalse(hits(SWEEP, 5.0, 0, 0, 0, 0, true), "East-west firebreak lane is safe when acrossX is true");
		assertTrue(hits(SWEEP, 5.0, 0, 4.5, 0, 0, true), "Side flue outside east-west lane hits");
		assertFalse(hits(IDLE, 4.5, 0, 4.5, 0, 0, false));
	}
}
