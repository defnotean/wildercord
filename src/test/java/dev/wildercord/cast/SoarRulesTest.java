package dev.wildercord.cast;

import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.Passives;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.spell.Trait;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Soar: the rune itself, and the pure rules of its flight (length, warning, speed, who it lifts, logging in). */
class SoarRulesTest {
	@Test
	void theRuneIsATierThreeHelpfulWindEffect() {
		RuneDef soar = Runes.SOAR;
		assertEquals(3, soar.tier());
		assertEquals(18, soar.cost(), 1e-9);
		assertEquals("wind", soar.element());
		assertEquals(EffectKind.HELPFUL, soar.kind());
		assertEquals("movement", soar.category());
		assertTrue(soar.traits().contains(Trait.DURATION), "Extend lengthens it");
		assertTrue(soar.traits().contains(Trait.FRUGAL));
		assertTrue(soar.traits().contains(Trait.SHARE), "Kindred can share it with an ally");
		assertFalse(soar.traits().contains(Trait.POWER), "nothing about it is stronger or weaker: Amplify has nothing to change");
		// Endless flight is no passive.
		assertFalse(Passives.allowed(soar));
	}

	@Test
	void extendAttachesAndDoublesTheFlightAmplifyDoesNot() {
		SpellPlan.EffectNode plain = node(Runes.SELF, Runes.SOAR);
		assertEquals(400, SoarRules.flightTicks(SpellNumbers.duration(plain)));
		SpellPlan.EffectNode extended = node(Runes.SELF, Runes.SOAR, Runes.EXTEND);
		assertEquals(1, extended.count(Runes.EXTEND));
		assertEquals(800, SoarRules.flightTicks(SpellNumbers.duration(extended)));
		SpellPlan.EffectNode frugal = node(Runes.SELF, Runes.SOAR, Runes.FRUGAL_MOD);
		assertEquals(240, SoarRules.flightTicks(SpellNumbers.duration(frugal)));
		SpellPlan.EffectNode amplified = node(Runes.SELF, Runes.SOAR, Runes.AMPLIFY);
		assertEquals(0, amplified.count(Runes.AMPLIFY), "Amplify finds nothing on Soar to attach to");
	}

	@Test
	void aFlightIsABurstOfTwentySecondsFortyAtMost() {
		assertEquals(400, SoarRules.flightTicks(1.0));
		assertEquals(800, SoarRules.flightTicks(2.0), "one Extend: 40 seconds");
		// However many Extends and circles: 40 seconds at most.
		assertEquals(800, SoarRules.MAX_TICKS);
		assertEquals(SoarRules.MAX_TICKS, SoarRules.flightTicks(4.0));
		assertEquals(SoarRules.MAX_TICKS, SoarRules.flightTicks(32.0));
		assertEquals(420, SoarRules.flightTicks(1.05), "a little over 20 with the caster's circles");
		// However shortened: a second at least.
		assertEquals(SoarRules.MIN_TICKS, SoarRules.flightTicks(0.01));
		assertEquals(SoarRules.MIN_TICKS, SoarRules.flightTicks(-1));
	}

	@Test
	void theWingsRestThirtySecondsAfterAFlight() {
		assertEquals(600, SoarRules.REST_TICKS);
		long ended = 10_000;
		long until = SoarRules.restUntil(ended, 0);
		assertEquals(ended + 600, until, "no rest yet: 30 seconds from the end");
		assertTrue(SoarRules.resting(ended, until));
		assertTrue(SoarRules.resting(until - 1, until));
		assertFalse(SoarRules.resting(until, until), "rested");
		assertFalse(SoarRules.resting(ended, 0), "never flew");
		assertEquals(30, SoarRules.restSecondsLeft(ended, until));
		assertEquals(12, SoarRules.restSecondsLeft(until - 240, until));
		assertEquals(1, SoarRules.restSecondsLeft(until - 1, until));
		assertEquals(0, SoarRules.restSecondsLeft(until, until));
		// A rest already running that's longer is kept; a shorter one gives way to the full rest.
		assertEquals(ended + 650, SoarRules.restUntil(ended, ended + 650));
		assertEquals(ended + 600, SoarRules.restUntil(ended, ended + 100));
		// A rest from another world's clock isn't one, and doesn't stretch the next.
		assertFalse(SoarRules.resting(ended, ended + 1_000_000));
		assertEquals(ended + 600, SoarRules.restUntil(ended, ended + 1_000_000));
	}

	@Test
	void itNeverRenewsAFlightOrLiftsCreativeOrSomeoneWhoAlreadyFlies() {
		assertEquals(SoarRules.Lift.LIFT, SoarRules.lift(false, false, false, false, false));
		assertEquals(SoarRules.Lift.CREATIVE, SoarRules.lift(true, true, false, false, false));
		assertEquals(SoarRules.Lift.CREATIVE, SoarRules.lift(true, false, true, false, false), "even one who was soaring before turning creative");
		assertEquals(SoarRules.Lift.ALREADY_SOARING, SoarRules.lift(false, true, true, false, false), "a flight is never renewed mid-air");
		assertEquals(SoarRules.Lift.ALREADY_SOARING, SoarRules.lift(false, true, true, true, true));
		assertEquals(SoarRules.Lift.ALREADY_FLIES, SoarRules.lift(false, true, false, false, false), "a flight from elsewhere isn't Soar's");
		assertEquals(SoarRules.Lift.WARDED, SoarRules.lift(false, false, false, true, false));
		assertEquals(SoarRules.Lift.RESTING, SoarRules.lift(false, false, false, false, true), "resting wings");
	}

	@Test
	void theWarningComesOnceThreeSecondsBeforeTheEnd() {
		long until = 1000;
		assertFalse(SoarRules.warnNow(until - 61, until, false));
		assertTrue(SoarRules.warnNow(until - 60, until, false));
		assertTrue(SoarRules.warnNow(until - 1, until, false), "a flight given back at login in its last seconds still warns");
		assertFalse(SoarRules.warnNow(until - 30, until, true), "once");
		assertFalse(SoarRules.warnNow(until, until, false), "over");
		assertEquals(60, SoarRules.WARNING_TICKS);
	}

	@Test
	void aFlightFromAnotherClockIsntTrusted() {
		assertFalse(SoarRules.stale(0, SoarRules.MAX_TICKS));
		assertFalse(SoarRules.stale(0, SoarRules.DESCENT_TICKS));
		assertTrue(SoarRules.stale(0, 100_000));
		assertFalse(SoarRules.stale(100_000, 0), "long past is just over, not stale");
	}

	@Test
	void loggingInResumesWhatsLeftOrLetsThemDown() {
		assertEquals(SoarRules.Login.RESUME, SoarRules.onLogin(1000, 1200, false));
		assertEquals(SoarRules.Login.DESCEND, SoarRules.onLogin(1000, 1000, false), "ran out as they left");
		assertEquals(SoarRules.Login.DESCEND, SoarRules.onLogin(5000, 1200, false), "ran out while they were away (a crash saved it)");
		assertEquals(SoarRules.Login.DESCEND, SoarRules.onLogin(1000, 1200, true), "coming down when they left");
		assertEquals(SoarRules.Login.DESCEND, SoarRules.onLogin(1000, 900_000, false), "another world's clock");
	}

	@Test
	void flyingSpeedIsGentlerAndGoesBack() {
		assertTrue(SoarRules.FLY_SPEED < SoarRules.DEFAULT_FLY_SPEED, "below creative's");
		assertEquals(0.05F, SoarRules.priorSpeed(0.05F), 1e-6);
		assertEquals(0.08F, SoarRules.priorSpeed(0.08F), 1e-6, "another mod's speed is kept");
		assertEquals(SoarRules.DEFAULT_FLY_SPEED, SoarRules.priorSpeed(SoarRules.FLY_SPEED), 1e-6, "Soar's own speed left over is healed");
		assertEquals(0.05F, SoarRules.restoredSpeed(SoarRules.FLY_SPEED, 0.05F), 1e-6);
		assertEquals(0.07F, SoarRules.restoredSpeed(0.07F, 0.05F), 1e-6, "changed by something else since: left alone");
	}

	@Test
	void aRefusedSoarGivesBackItsShare() {
		// Soar alone on Self: all of it.
		SpellPlan.Segment self = SpellCompiler.compile(List.of(Runes.SELF, Runes.SOAR)).root();
		assertEquals(1.0, SpellCompiler.effectShare(self, self.groups.getFirst().effects.getFirst()), 1e-9);
		// Beside a Heal (12 mana against Soar's 18): three fifths.
		SpellPlan.Segment both = SpellCompiler.compile(List.of(Runes.SELF, Runes.SOAR, Runes.HEAL)).root();
		SpellPlan.EffectNode soar = both.groups.getFirst().effects.getFirst();
		assertEquals(Runes.SOAR, soar.effect);
		assertEquals(0.6, SpellCompiler.effectShare(both, soar), 1e-9);
		// Found by its rune and modifiers in a plan of its own (a chorus recasts a spell).
		SpellPlan.Segment again = SpellCompiler.compile(List.of(Runes.SELF, Runes.SOAR, Runes.HEAL)).root();
		assertEquals(0.6, SpellCompiler.effectShare(again, soar), 1e-9);
		// Not in the spell at all: nothing.
		assertEquals(0.0, SpellCompiler.effectShare(SpellCompiler.compile(List.of(Runes.SELF, Runes.HEAL)).root(), soar), 1e-9);
	}

	@Test
	void gustsAreQuietWhileHovering() {
		assertTrue(SoarRules.gustVolume(0) < SoarRules.gustVolume(0.6));
		assertEquals(SoarRules.gustVolume(0.6), SoarRules.gustVolume(3.0), 1e-6);
		assertTrue(SoarRules.gustVolume(3.0) <= 0.5F);
	}

	private static SpellPlan.EffectNode node(RuneDef... runes) {
		return SpellCompiler.compile(List.of(runes)).root().groups.getFirst().effects.getFirst();
	}
}
