package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Overchannel: its stages and their timing, the beat, the surge chance, mana, tracing's trust, and tearing loose safely. */
class OverchannelTest {
	private static final Overchannel.Tuning T = Overchannel.Tuning.DEFAULTS;

	@Test
	void theHeartHoldsMoreStagesAsItGrowsAndCrackedCirclesDontCount() {
		assertEquals(1, Overchannel.stagesFor(0, true), "every caster can push one stage");
		assertEquals(1, Overchannel.stagesFor(1, true));
		assertEquals(2, Overchannel.stagesFor(2, true));
		assertEquals(2, Overchannel.stagesFor(3, true));
		assertEquals(3, Overchannel.stagesFor(4, true));
		assertEquals(3, Overchannel.stagesFor(7, true), "never past three");
		assertEquals(1, Overchannel.stagesFor(-2, true));
		assertEquals(0, Overchannel.stagesFor(7, false), "switched off, a full charge just waits");
	}

	@Test
	void stagesComeEverySecondOrSoAndTheChannelTearsAfterTheLast() {
		assertEquals(24, Overchannel.STAGE_TICKS, "a little over a second");
		assertFalse(Overchannel.due(0, 3, 23));
		assertTrue(Overchannel.due(0, 3, 24));
		assertTrue(Overchannel.due(2, 3, 30));
		assertFalse(Overchannel.due(3, 3, 1000), "nothing past the heart's last stage");
		assertFalse(Overchannel.due(1, 1, 1000));
		assertFalse(Overchannel.tears(2, 3, 1000), "a channel short of its last stage never tears");
		assertFalse(Overchannel.tears(3, 3, Overchannel.GRACE - 1));
		assertTrue(Overchannel.tears(3, 3, Overchannel.GRACE));
		assertTrue(Overchannel.tears(1, 1, Overchannel.GRACE), "a one-stage heart tears after its one stage");
		assertFalse(Overchannel.tears(0, 0, 100_000), "with overchannel off nothing ever tears");
	}

	@Test
	void eachStageAddsPowerMeasurably() {
		assertEquals(1.0, Overchannel.stagePower(0, 0.2), 1e-9);
		assertEquals(1.2, Overchannel.stagePower(1, 0.2), 1e-9);
		assertEquals(1.4, Overchannel.stagePower(2, 0.2), 1e-9);
		assertEquals(1.6, Overchannel.stagePower(3, 0.2), 1e-9);
		assertEquals(1.6, Overchannel.stagePower(9, 0.2), 1e-9, "a bad stage number can't go past three");
		assertEquals(1.0, Overchannel.stagePower(-1, 0.2), 1e-9);
		for (int s = 1; s <= 3; s++) {
			assertTrue(Overchannel.power(s, false, 0, T) > Overchannel.power(s - 1, false, 0, T));
		}
	}

	@Test
	void theBeatIsTheFillingAndEachStageAndOnlyJustAfter() {
		long start = 1000;
		int full = 30;
		// Stage 0: the beat is the moment the charge fills.
		assertEquals(1030, Overchannel.beatTime(start, full, 0, 0));
		assertTrue(Overchannel.onBeat(1030, 1030, 3));
		assertTrue(Overchannel.onBeat(1030 + Overchannel.BEAT_WINDOW, 1030, 3));
		assertFalse(Overchannel.onBeat(1031 + Overchannel.BEAT_WINDOW, 1030, 3));
		assertFalse(Overchannel.onBeat(1029, 1030, 3), "a release before the beat isn't on it");
		assertFalse(Overchannel.onBeat(1030, 1030, 0), "no beats with overchannel off");
		// A stage's beat is when it was reached (late if mana held it back).
		assertEquals(1070, Overchannel.beatTime(start, full, 2, 1070));
		// The HUD's next beat: the filling, then each stage, then the moment it tears.
		assertEquals(1030, Overchannel.nextBeat(start, full, 3, 0, 0, 1010));
		assertEquals(1030 + 24, Overchannel.nextBeat(start, full, 3, 0, 0, 1040));
		assertEquals(1100 + 24, Overchannel.nextBeat(start, full, 3, 2, 1100, 1105));
		assertEquals(1200 + Overchannel.GRACE, Overchannel.nextBeat(start, full, 3, 3, 1200, 1210));
		assertEquals(Long.MIN_VALUE, Overchannel.nextBeat(start, full, 0, 0, 0, 1040));
	}

	@Test
	void theBeatAndATracedGlyphAddSmallBonuses() {
		assertEquals(1.1, Overchannel.power(0, true, 0, T), 1e-9);
		assertEquals(1.6 * 1.1, Overchannel.power(3, true, 0, T), 1e-9);
		assertEquals(0, Overchannel.traceBonus(0, 0.08), 1e-9);
		assertEquals(0, Overchannel.traceBonus(0.3, 0.08), 1e-9, "a scribble earns nothing");
		assertEquals(0.08, Overchannel.traceBonus(1, 0.08), 1e-9);
		assertEquals(0.08, Overchannel.traceBonus(5, 0.08), 1e-9, "never more than the cap");
		assertTrue(Overchannel.traceBonus(0.8, 0.08) > Overchannel.traceBonus(0.6, 0.08));
		Overchannel.Tuning noTracing = new Overchannel.Tuning(true, 0.2, 0.15, 0.07, 0.1, 30, 0.3, false, 0.08);
		assertEquals(1.0, Overchannel.power(0, false, 1.0, noTracing), 1e-9);
		// Nothing performed: exactly the spell as it was.
		assertEquals(1.0, Overchannel.power(0, false, 0, T), 1e-9);
	}

	@Test
	void surgeChanceRisesWithEachStageAndTracingSteadiesIt() {
		assertEquals(0, Overchannel.surgeChance(0, 0.07, 0), 1e-9, "a plain charge never surges");
		assertEquals(0.07, Overchannel.surgeChance(1, 0.07, 0), 1e-9);
		assertEquals(0.14, Overchannel.surgeChance(2, 0.07, 0), 1e-9);
		assertEquals(0.21, Overchannel.surgeChance(3, 0.07, 0), 1e-9);
		assertEquals(0.21 * (1 - Overchannel.STEADYING), Overchannel.surgeChance(3, 0.07, 1), 1e-9);
		assertTrue(Overchannel.surgeChance(3, 0.07, 0.5) < Overchannel.surgeChance(3, 0.07, 0));
		assertEquals(1.0, Overchannel.surgeChance(3, 5.0, 0), 1e-9, "a wild tuning still gives a chance, not more");
		// An overcast that was also overchanneled rolls both as one.
		assertEquals(1 - 0.8 * 0.79, Overchannel.combined(0.2, 0.21), 1e-9);
		assertEquals(0.2, Overchannel.combined(0.2, 0), 1e-9);
	}

	@Test
	void theChannelDrainsSurplusManaAndNeverTheSpellsOwnPrice() {
		assertEquals(30 * 0.15 / 20, Overchannel.drainPerTick(30, 0.15), 1e-9);
		assertEquals(0.05, Overchannel.drainPerTick(0, 0.15), 1e-9, "even a free spell's channel takes a little");
		double drain = Overchannel.drainPerTick(30, 0.15);
		assertTrue(Overchannel.fed(60, 30, drain));
		assertTrue(Overchannel.fed(30 + drain, 30, drain), "exactly enough");
		assertFalse(Overchannel.fed(30, 30, drain), "the spell's own price is never drained");
		assertFalse(Overchannel.fed(10, 30, drain), "a spell that would overcast can't overchannel");
		// A second and a bit at each stage, all three: well under the spell's price again.
		double ticks = 3 * Overchannel.STAGE_TICKS + Overchannel.GRACE;
		assertTrue(ticks * drain < 30, "the whole channel costs less than casting it twice");
	}

	@Test
	void tracingIsOnlyBelievedAsFarAsTheCasterReallySteadied() {
		assertEquals(0, Overchannel.validTrace(1.0, 0, true), 0, "never steadied: nothing");
		assertEquals(0, Overchannel.validTrace(1.0, Overchannel.MIN_STEADY - 1, true), 0);
		assertEquals(0.5, Overchannel.validTrace(1.0, Overchannel.FULL_STEADY / 2, true), 1e-9, "half the time, half the credit");
		assertEquals(1.0, Overchannel.validTrace(1.0, Overchannel.FULL_STEADY, true), 1e-9);
		assertEquals(1.0, Overchannel.validTrace(42.0, 1000, true), 1e-9, "a forged accuracy is clamped");
		assertEquals(0, Overchannel.validTrace(-3.0, 1000, true), 1e-9);
		assertEquals(0, Overchannel.validTrace(Double.NaN, 1000, true), 1e-9);
		assertEquals(0.7, Overchannel.validTrace(0.7, 1000, true), 1e-9);
		assertEquals(0, Overchannel.validTrace(1.0, 1000, false), 1e-9, "a server can switch tracing off");
	}

	@Test
	void tearingLooseNeverHarmsAndKeepsWithinItsBounds() {
		Overchannel.Backfire b = Overchannel.backfire(80, 100, false, T);
		assertEquals(50, b.manaAfter(), 1e-4, "a share of full mana burnt");
		assertEquals(30, b.stunTicks());
		assertEquals(0, b.damage(), 0, "tearing loose deals no damage at all");
		assertEquals(0, Overchannel.backfire(10, 100, false, T).manaAfter(), 0, "never below empty");
		assertEquals(0, Overchannel.backfire(-5, 100, false, T).manaAfter(), 0);
		assertEquals(80, Overchannel.backfire(80, 100, true, T).manaAfter(), 0, "creative players lose nothing");
		Overchannel.Tuning harsh = new Overchannel.Tuning(true, 0.2, 0.15, 0.07, 0.1, 100_000, 50.0, true, 0.08);
		Overchannel.Backfire worst = Overchannel.backfire(80, 100, false, harsh);
		assertEquals(Overchannel.MAX_STUN, worst.stunTicks(), "the daze is capped however a server tunes it");
		assertEquals(0, worst.manaAfter(), 0);
		assertEquals(0, worst.damage(), 0);
		// The surges a torn channel can roll need no spell and hurt nobody.
		Set<WildMagic.Surge> seen = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			WildMagic.Surge s = WildMagic.pickFizzle(i / 1000.0);
			assertTrue(WildMagic.FIZZLES.contains(s));
			seen.add(s);
		}
		assertEquals(WildMagic.FIZZLES, seen, "every one comes up");
		assertFalse(WildMagic.FIZZLES.contains(WildMagic.Surge.BACKFIRE), "no harm to the caster");
		for (WildMagic.Surge s : WildMagic.FIZZLES) {
			assertFalse(s.rewrites, s + " would need a spell to rewrite");
		}
		assertFalse(WildMagic.FIZZLES.contains(WildMagic.Surge.TWICE), "no spell to go off twice");
	}
}
