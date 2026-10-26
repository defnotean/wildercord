package dev.wildercord.wildlife;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RootmoltRulesTest {
	@Test
	void onlyTwoShareAPatch() {
		assertTrue(RootmoltRules.room(0));
		assertTrue(RootmoltRules.room(RootmoltRules.LOCAL_CAP - 1));
		assertFalse(RootmoltRules.room(RootmoltRules.LOCAL_CAP));
	}

	@Test
	void itsAttackIsWarnedShortAndRested() {
		assertTrue(RootmoltRules.WINDUP >= 20, "a warning a player can see and step away from");
		assertTrue(RootmoltRules.HELD > 0 && RootmoltRules.HELD <= 40, "a hold never lasts long");
		assertTrue(RootmoltRules.ATTACK_REST > RootmoltRules.WINDUP + RootmoltRules.RAKE + RootmoltRules.HELD + RootmoltRules.RECOVERY,
			"it rests longer than a whole strike, so it can't chain holds");
		assertTrue(RootmoltRules.MEAL_REST > RootmoltRules.ATTACK_REST);
	}

	@Test
	void aVisitFitsItsJourney() {
		assertTrue(RootmoltRules.VISIT_SPEED > 0);
		assertTrue(RootmoltRules.JOURNEY > RootmoltRules.BROWSE);
		assertTrue(RootmoltRules.SEARCH_RADIUS > 0 && RootmoltRules.SEARCH_COLUMNS > 0 && RootmoltRules.PATH_BUDGET > 0);
	}
}
