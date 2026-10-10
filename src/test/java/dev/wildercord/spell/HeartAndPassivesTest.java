package dev.wildercord.spell;

import dev.wildercord.player.Spellbook;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.*;

class HeartAndPassivesTest {
	private static List<RuneDef> runes(RuneDef... runes) {
		return List.of(runes);
	}

	@Test
	void twoPassiveSlotsOpenAtTheFirstAndFifthCircle() {
		assertEquals(2, Passives.MAX);
		assertEquals(0, Passives.slots(0));
		assertEquals(1, Passives.slots(1));
		assertEquals(1, Passives.slots(4));
		assertEquals(2, Passives.slots(5));
		assertEquals(2, Passives.slots(8));
		assertEquals(List.of(1, 5), List.of(Passives.circleFor(0), Passives.circleFor(1)));
	}

	@Test
	void onlySustainableRunesCanBePassives() {
		for (RuneDef ok : runes(SELF, ORBIT, SWIFT, STONESKIN, REFLECT, SHOCK, DISMANTLE, AMPLIFY, FRUGAL_MOD, QUICKEN)) {
			assertTrue(Passives.allowed(ok), ok.name());
		}
		for (RuneDef no : runes(BOLT, BEAM, DOMAIN, HEAL, REVERSAL, FORESIGHT, STASIS, LIGHTNING, HOLLOW, SUMMON, SHIELD, VEIL, INFINITY, ACCELERATE, SPLIT_MOD,
				VOLLEY_MOD, VOW_MOD, BLOOD_PRICE_MOD, DELAY, ON_HIT, COMBO)) {
			assertFalse(Passives.allowed(no), no.name());
		}
	}

	@Test
	void damageNeedsAnOrbitAndOneShapeAtMost() {
		assertNull(Passives.problem(runes(SWIFT)));
		assertNull(Passives.problem(runes(STONESKIN, AMPLIFY)));
		assertNull(Passives.problem(runes(ORBIT, SHOCK)));
		assertNull(Passives.problem(runes(ORBIT, DISMANTLE)));
		assertTrue(Passives.problem(runes(SELF, SHOCK)).contains("needs an Orbit"));
		assertTrue(Passives.problem(runes(ORBIT, SWIFT, SELF)).contains("one shape"));
		assertTrue(Passives.problem(runes(BOLT, HARM)).contains("can't be sustained"));
		// A passive that starts with an effect is on Self already: an Orbit after it is a second shape (it would
		// renew every two seconds, as Self does, and stack its orbits).
		assertTrue(Passives.problem(runes(SWIFT, ORBIT, SHOCK)).contains("one shape"));
		assertTrue(Passives.problem(runes(SWIFT, EXTEND, ORBIT, SHOCK)).contains("one shape"));
	}

	@Test
	void aPassiveHoldsTwoRunesAtMost() {
		assertEquals(2, Passives.SOCKETS);
		assertNull(Passives.problem(runes(SWIFT, AMPLIFY)));
		assertNull(Passives.problem(runes(ORBIT, SHOCK)));
		assertTrue(Passives.problem(runes(SWIFT, AMPLIFY, STONESKIN)).contains("2 runes"));
		assertTrue(Passives.problem(runes(ORBIT, SHOCK, SWIFT)).contains("2 runes"));
		// A passive saved when they held five is trimmed to its first two on load (the runes themselves are never lost).
		Spellbook old = new Spellbook(List.of(), List.of(), 0, false,
			List.of(List.of("wildercord:swift", "wildercord:haste", "wildercord:stoneskin", "wildercord:amplify", "wildercord:extend"), List.of("wildercord:orbit")),
			0, List.of());
		assertEquals(List.of("wildercord:swift", "wildercord:haste"), old.passives().get(0));
		assertEquals(List.of("wildercord:orbit"), old.passives().get(1));
	}

	@Test
	void upkeepAndRenewal() {
		assertEquals(1.2, Passives.upkeep(10), 1e-9);
		assertEquals(Passives.SELF_INTERVAL, Passives.interval(SpellCompiler.compile(runes(SWIFT)).root()));
		assertEquals(160, Passives.interval(SpellCompiler.compile(runes(ORBIT, SHOCK)).root()));
		assertEquals(320, Passives.interval(SpellCompiler.compile(runes(ORBIT, EXTEND, SHOCK)).root()));
	}

	@Test
	void aPassivesBuffsEndSoonAfterItIsSwitchedOff() {
		// Night Eye's minute of Night Vision is cut to a little past the next renewal...
		assertEquals(Passives.EFFECT_TICKS, Passives.effectTicks(60 * 20));
		assertEquals(Passives.EFFECT_TICKS, Passives.effectTicks(-1));
		assertEquals(100, Passives.effectTicks(100));
		// ...which still outlasts a renewal by Night Vision's 10 seconds of flicker, so a running passive never flickers.
		assertTrue(Passives.EFFECT_TICKS >= Passives.SELF_INTERVAL + 200);
		assertTrue(Passives.EFFECT_TICKS <= 20 * 20);
	}

	@Test
	void circlesNeedMoreAndMoreMana() {
		int last = 0;
		for (int n = 1; n <= Circles.MAX; n++) {
			assertTrue(Circles.condenseNeeded(n) > last, "circle " + n);
			last = Circles.condenseNeeded(n);
		}
		assertEquals(1000, Circles.condenseNeeded(1));
		assertEquals(230000, Circles.condenseNeeded(8));
		assertTrue(Circles.requirements(1).isEmpty());
		for (int n = 2; n <= Circles.MAX; n++) {
			assertFalse(Circles.requirements(n).isEmpty(), "circle " + n + " needs a breakthrough");
		}
		assertTrue(Circles.requirements(7).contains(new Circles.Requirement(Circles.Need.BOSS, 1)));
		assertTrue(Circles.requirements(8).contains(new Circles.Requirement(Circles.Need.CORD, 3)));
		assertEquals(List.of("1st", "2nd", "3rd", "4th", "8th", "11th", "21st"),
			List.of(Circles.ordinal(1), Circles.ordinal(2), Circles.ordinal(3), Circles.ordinal(4), Circles.ordinal(8), Circles.ordinal(11), Circles.ordinal(21)));
	}

	@Test
	void circlesAndEnchantmentsScaleSpells() {
		assertEquals(1.0, Circles.power(0, 0, false), 1e-9);
		assertEquals(1.24 * 1.12, Circles.power(3, 8, false), 1e-9);
		assertEquals(1.24 * 1.12 * 1.15, Circles.power(3, 8, true), 1e-9);
		assertEquals(1.03, Circles.power(0, 2, true), 1e-9); // Overflow needs the 7th Circle
		assertEquals((1 - 0.21) * 0.85, Circles.cost(3, 8), 1e-9);
		assertEquals(0.85, Circles.cooldown(0, 5), 1e-9);
		assertEquals(1 - 0.24, Circles.cooldown(3, 4), 1e-9);
		assertEquals(1.4, Circles.duration(2), 1e-9);
	}
}
