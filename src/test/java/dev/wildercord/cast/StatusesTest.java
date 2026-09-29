package dev.wildercord.cast;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The shared seams: how far wind carries a body by its size, and what an airborne creature takes. */
class StatusesTest {
	@Test
	void aPersonMovesAboutOneWhenBlownByWind() {
		assertEquals(1.0, Statuses.windMass(0.6, 1.8), 0.2);
		assertEquals(1.0, Statuses.windMass(0.6, 1.95), 0.2, "a zombie");
	}

	@Test
	void lightThingsFlyFarAndHeavyOnesBarelyMoveBoundedByAHalfAndOneAndAHalf() {
		double bat = Statuses.windMass(0.5, 0.9);
		double chicken = Statuses.windMass(0.4, 0.7);
		double golem = Statuses.windMass(1.4, 2.7);
		double ravager = Statuses.windMass(1.95, 2.2);
		assertTrue(chicken >= bat && bat > 1.0, "the lighter, the farther");
		assertTrue(Statuses.windMass(0.6, 0.85) > Statuses.windMass(1.4, 0.9), "a wolf flies farther than a spider");
		assertTrue(golem < 0.8 && golem >= 0.5, "an iron golem barely budges");
		assertEquals(0.5, ravager, 1.0E-9, "the heaviest is held at half");
		assertEquals(1.5, Statuses.windMass(0.1, 0.1), 1.0E-9, "and the lightest at one and a half");
	}

	@Test
	void theAirborneBonusIsAFifth() {
		assertEquals(1.2, Statuses.AIRBORNE_BONUS, 1.0E-9);
		assertTrue(Statuses.INTERRUPT_GAP >= 100, "an interrupt can't lock a creature out");
	}
}
