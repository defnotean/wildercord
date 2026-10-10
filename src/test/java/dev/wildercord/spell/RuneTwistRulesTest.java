package dev.wildercord.spell;

import dev.wildercord.spell.RuneTwistRules.Twist;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuneTwistRulesTest {
	@Test
	void flawedIsWeakerAndCorruptedStronger() {
		assertTrue(Twist.FLAWED.power < 1.0);
		assertFalse(Twist.FLAWED.corrupted);
		for (Twist twist : Twist.values()) {
			if (twist != Twist.FLAWED) assertTrue(twist.corrupted && twist.power > 1.0, twist.id());
		}
		assertEquals(1.0, RuneTwistRules.power(null));
	}

	@Test
	void flawedRunesMakeASpellCheaperDownToAFloor() {
		assertEquals(1.0, RuneTwistRules.costFactor(0));
		assertEquals(0.85, RuneTwistRules.costFactor(1), 1e-9);
		assertEquals(0.7225, RuneTwistRules.costFactor(2), 1e-9);
		assertEquals(RuneTwistRules.FLAWED_FLOOR, RuneTwistRules.costFactor(20));
	}

	@Test
	void rollIsFlawedHalfTheTimeAndReachesEveryCorruption() {
		assertEquals(Twist.FLAWED, RuneTwistRules.roll(0.0));
		assertEquals(Twist.FLAWED, RuneTwistRules.roll(0.49));
		Set<Twist> seen = EnumSet.noneOf(Twist.class);
		for (double r = 0.5; r < 1.0; r += 0.01) seen.add(RuneTwistRules.roll(r));
		assertEquals(EnumSet.complementOf(EnumSet.of(Twist.FLAWED)), seen);
		assertTrue(RuneTwistRules.roll(0.9999).corrupted);
	}

	@Test
	void idsRoundTrip() {
		for (Twist twist : Twist.values()) assertEquals(twist, Twist.byId(twist.id()));
		assertNull(Twist.byId("nope"));
		assertNull(Twist.byId(null));
	}
}
