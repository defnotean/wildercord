package dev.wildercord.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The gear part of the public API: the slots it names, and the mistake it refuses. (Stacks need a game, so the game tests cover equipping.) */
class GearApiTest {
	private static final WildercordApi API = WildercordApi.get();

	@Test
	void theApiNamesTheGearSlotsInInventoryOrder() {
		assertEquals(List.of("staff", "focus", "tome"), API.gearSlots());
		assertEquals("1.1", WildercordApi.VERSION);
	}

	@Test
	void anUnknownSlotIsAMistakeNotASilentNothing() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> API.equippedGear(null, "ring"));
		assertEquals(true, e.getMessage().contains("staff"));
		assertThrows(IllegalArgumentException.class, () -> API.equipGear(null, "ring", null));
		assertThrows(IllegalArgumentException.class, () -> API.unequipGear(null, "ring"));
	}
}
