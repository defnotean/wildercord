package dev.wildercord.spell;

import dev.wildercord.spell.ClimateRules.Condition;
import dev.wildercord.spell.ClimateRules.Surroundings;
import dev.wildercord.spell.ClimateRules.Tuning;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Places and times of power: the moon, the hour and the weather, and ley crossings, on the climate table. */
class PowerTableTest {
	private static final double EPS = 1e-9;

	/** A clear Overworld sky over open ground, at {@code time} on day {@code day}. */
	private static Surroundings sky(long day, long time, boolean raining, boolean crossing) {
		return new Surroundings("minecraft:overworld", true, time, true, raining, false, raining, false, 0.8F, false, 70, crossing, crossing,
			ClimateRules.moonPhase(day * 24000 + time), true);
	}

	@Test
	void theSkysTable() {
		assertEquals(1.15, ClimateRules.factor(Set.of(Condition.FULL_MOON), "arcane"), EPS);
		assertEquals(1.15, ClimateRules.factor(Set.of(Condition.FULL_MOON), "void"), EPS);
		assertEquals(1.15, ClimateRules.factor(Set.of(Condition.NEW_MOON), "blood"), EPS);
		assertEquals(1.10, ClimateRules.factor(Set.of(Condition.NEW_MOON), "void"), EPS);
		assertEquals(1.15, ClimateRules.factor(Set.of(Condition.NOON), "fire"), EPS);
		assertEquals(1.15, ClimateRules.factor(Set.of(Condition.DAWN), "time"), EPS);
		assertEquals(1.15, ClimateRules.factor(Set.of(Condition.DUSK), "time"), EPS);
		assertEquals(1.10, ClimateRules.factor(Set.of(Condition.RAIN), "frost"), EPS);
		assertEquals(0.90, ClimateRules.factor(Set.of(Condition.RAIN), "fire"), EPS, "rain still dulls fire");
		assertEquals(1.25, ClimateRules.factor(Set.of(Condition.THUNDER), "storm"), EPS, "a thunderstorm still feeds storm");
		assertEquals(1.0, ClimateRules.factor(Set.of(Condition.FULL_MOON), "fire"), EPS);
		// Every one of the sky's bonuses is modest: 10 to 20%.
		for (ClimateRules.Shift shift : ClimateRules.SHIFTS) {
			if (shift.celestial()) {
				assertTrue(shift.factor() >= 1.10 - EPS && shift.factor() <= 1.20 + EPS, shift.toString());
			}
		}
	}

	@Test
	void aLeyCrossingStrengthensEveryElementAndCheapensEverySpell() {
		for (String element : Affinity.ELEMENTS) {
			assertEquals(1.10, ClimateRules.factor(Set.of(Condition.LEY_CROSSING), element), EPS, element);
		}
		assertEquals(1.0, ClimateRules.factor(Set.of(Condition.LEY_CROSSING), ""), EPS, "a spell with no element isn't an element's");
		// A crossing is on a ley line too: arcane gets both.
		assertEquals(1.15 * 1.10, ClimateRules.factor(EnumSet.of(Condition.LEY, Condition.LEY_CROSSING), "arcane"), EPS);
		assertEquals(0.90, Tuning.DEFAULT.crossingCost(), EPS);
		assertEquals(0.75, new Tuning(0.25, 1).crossingCost(), EPS);
		assertEquals(0.5, new Tuning(0.9, 1).crossingCost(), EPS, "never under half price");
	}

	@Test
	void theServerTunesThem() {
		Tuning half = new Tuning(0.05, 0.5);
		assertEquals(1.075, ClimateRules.factor(Set.of(Condition.FULL_MOON), "arcane", half), EPS);
		assertEquals(1.05, ClimateRules.factor(Set.of(Condition.RAIN), "frost", half), EPS);
		assertEquals(0.90, ClimateRules.factor(Set.of(Condition.RAIN), "fire", half), EPS, "the old climate isn't the sky's to scale");
		assertEquals(1.05, ClimateRules.factor(Set.of(Condition.LEY_CROSSING), "earth", half), EPS);
		assertEquals(1.0, ClimateRules.factor(Set.of(Condition.FULL_MOON, Condition.LEY_CROSSING), "arcane", Tuning.NONE), EPS);
		assertTrue(ClimateRules.factors(Set.of(Condition.NOON), Tuning.NONE).isEmpty());
	}

	@Test
	void theHudAndGrimoireSayWhy() {
		assertEquals("arcane +15%, void +15%", ClimateRules.describe(Condition.FULL_MOON, e -> e));
		assertEquals("time +15%", ClimateRules.describe(Condition.DAWN, e -> e));
		assertEquals("fire -10%, frost +10%", ClimateRules.describe(Condition.RAIN, e -> e));
		assertEquals("all +10%", ClimateRules.describe(Condition.LEY_CROSSING, e -> e));
		assertEquals("fire -10%", ClimateRules.describe(Condition.RAIN, e -> e, Tuning.NONE), "the sky's part left out when it's off");
		assertEquals("", ClimateRules.describe(Condition.NOON, e -> e, Tuning.NONE));
		assertTrue(ClimateRules.celestial(Condition.FULL_MOON));
		assertFalse(ClimateRules.celestial(Condition.THUNDER));
	}

	@Test
	void theMoonTurnsThroughEightPhases() {
		assertEquals(0, ClimateRules.moonPhase(0));
		assertEquals(0, ClimateRules.moonPhase(18000), "the first night is a full moon");
		assertEquals(1, ClimateRules.moonPhase(24000));
		assertEquals(4, ClimateRules.moonPhase(4 * 24000 + 18000), "the fifth night is a new moon");
		assertEquals(0, ClimateRules.moonPhase(8 * 24000 + 100));
		assertEquals(7, ClimateRules.moonPhase(-1), "a clock set back still has a phase");
	}

	@Test
	void whenTheSkysConditionsHold() {
		assertTrue(ClimateRules.conditions(sky(0, 18000, false, false)).containsAll(EnumSet.of(Condition.NIGHT, Condition.FULL_MOON)));
		assertTrue(ClimateRules.conditions(sky(4, 18000, false, false)).contains(Condition.NEW_MOON));
		Set<Condition> waning = ClimateRules.conditions(sky(2, 18000, false, false));
		assertFalse(waning.contains(Condition.FULL_MOON) || waning.contains(Condition.NEW_MOON), "a quarter moon is neither");
		assertFalse(ClimateRules.conditions(sky(0, 18000, true, false)).contains(Condition.FULL_MOON), "clouds hide the moon");
		assertTrue(ClimateRules.conditions(sky(0, 6000, false, false)).containsAll(EnumSet.of(Condition.SUN, Condition.NOON)));
		assertFalse(ClimateRules.conditions(sky(0, 9000, false, false)).contains(Condition.NOON));
		assertFalse(ClimateRules.conditions(sky(0, 6000, true, false)).contains(Condition.NOON), "no noon sun through rain");
		assertTrue(ClimateRules.conditions(sky(0, 23500, false, false)).contains(Condition.DAWN));
		assertTrue(ClimateRules.conditions(sky(0, 500, false, false)).contains(Condition.DAWN));
		assertTrue(ClimateRules.conditions(sky(0, 12500, false, false)).contains(Condition.DUSK));
		assertTrue(ClimateRules.conditions(sky(0, 6000, false, true)).contains(Condition.LEY_CROSSING));
		// Under a roof, the sky's hours and the moon don't reach you; a crossing is in the ground and does.
		Surroundings roofed = new Surroundings("minecraft:overworld", true, 18000, false, false, false, false, false, 0.8F, false, 70, true, true, 0, true);
		assertEquals(EnumSet.of(Condition.LEY, Condition.LEY_CROSSING), ClimateRules.conditions(roofed));
		// The server's switch, and the Nether, which has no sky of its own.
		Surroundings off = new Surroundings("minecraft:overworld", true, 18000, true, false, false, false, false, 0.8F, false, 70, false, false, 0, false);
		assertEquals(EnumSet.of(Condition.NIGHT), ClimateRules.conditions(off));
		Surroundings nether = new Surroundings("minecraft:the_nether", false, 18000, false, false, false, false, false, 2.0F, true, 40, false, false, 0, true);
		assertEquals(EnumSet.of(Condition.NETHER), ClimateRules.conditions(nether));
		// The 0.7.1 surroundings (the old constructor) know nothing of the sky's hours.
		Surroundings old = new Surroundings("minecraft:overworld", true, 6000, true, false, false, false, false, 0.8F, false, 70, false);
		assertEquals(EnumSet.of(Condition.SUN), ClimateRules.conditions(old));
	}

	@Test
	void theyStayInsideTheClimatesBounds() {
		Map<String, Double> everything = ClimateRules.factors(EnumSet.allOf(Condition.class), new Tuning(0.5, 2));
		for (double factor : everything.values()) {
			assertTrue(factor >= ClimateRules.MIN && factor <= ClimateRules.MAX, everything.toString());
		}
		for (Condition c : Condition.values()) {
			assertEquals(c, Condition.byId(c.id).orElseThrow());
		}
	}
}
