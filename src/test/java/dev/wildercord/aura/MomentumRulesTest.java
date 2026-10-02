package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Momentum, the pure part ({@link MomentumRules}): its tiers and what each gives, what builds it and what knocks it down, the ebb
 * both sides work out, what keeps it honest (practice, budgets, reach), each method's temper, and how a fight plays out over it.
 */
class MomentumRulesTest {
	private static final List<String> METHODS = List.of("ember", "rime", "thunder", "gale", "stone", "verdant", "hollow", "starlit", "hourglass",
		"crimson");

	// ------------------------------------------------------------------ the meter and its tiers

	@Test
	void theTiersClimbAtAQuarterEachAndThePeakIsNearTheTop() {
		assertEquals(0, MomentumRules.tier(0));
		assertEquals(0, MomentumRules.tier(24.9));
		assertEquals(1, MomentumRules.tier(25));
		assertEquals(2, MomentumRules.tier(50));
		assertEquals(3, MomentumRules.tier(75));
		assertEquals(3, MomentumRules.tier(94.9));
		assertEquals(MomentumRules.PEAK_TIER, MomentumRules.tier(95));
		assertEquals(MomentumRules.PEAK_TIER, MomentumRules.tier(100));
		assertFalse(MomentumRules.peak(94.9));
		assertTrue(MomentumRules.peak(MomentumRules.PEAK));
		assertTrue(MomentumRules.PEAK < MomentumRules.MAX && MomentumRules.PEAK > MomentumRules.TIERS[2]);
		assertEquals(0, MomentumRules.clamp(-5));
		assertEquals(MomentumRules.MAX, MomentumRules.clamp(250));
	}

	@Test
	void eachTierMakesArtsCheaperStrongerAndHarderOnStance() {
		for (int t = 1; t <= MomentumRules.PEAK_TIER; t++) {
			assertTrue(MomentumRules.priceFactor(t) < MomentumRules.priceFactor(t - 1), "tier " + t + " is cheaper");
			assertTrue(MomentumRules.strength(t) > MomentumRules.strength(t - 1), "tier " + t + " strikes harder");
			assertTrue(MomentumRules.stanceFactor(t) > MomentumRules.stanceFactor(t - 1), "tier " + t + " wears stance faster");
		}
		assertEquals(1.0, MomentumRules.priceFactor(0), 1e-9);
		assertEquals(1.0, MomentumRules.strength(0), 1e-9);
		// The peak: a quarter off, a fifth harder; never so much it outgrows the balance between methods.
		assertEquals(0.75, MomentumRules.priceFactor(MomentumRules.PEAK_TIER), 1e-9);
		assertEquals(1.2, MomentumRules.strength(MomentumRules.PEAK_TIER), 1e-9);
		assertEquals(30.0, MomentumRules.price(40, MomentumRules.PEAK_TIER), 1e-9, "the Final Art at the peak");
		assertEquals(0, MomentumRules.price(-3, 2), 1e-9);
		// Out-of-range tiers read as the nearest.
		assertEquals(MomentumRules.priceFactor(0), MomentumRules.priceFactor(-1), 1e-9);
		assertEquals(MomentumRules.priceFactor(MomentumRules.PEAK_TIER), MomentumRules.priceFactor(9), 1e-9);
	}

	@Test
	void theFinalArtsReleaseFallsBackFromThePeak() {
		assertFalse(MomentumRules.peak(MomentumRules.MAX - MomentumRules.FINAL_SPEND), "after the release it must be built again");
		assertTrue(MomentumRules.tier(MomentumRules.MAX - MomentumRules.FINAL_SPEND) >= 2, "but a good fight keeps some of it");
		assertEquals(0, MomentumRules.art(4), 1e-9, "the Final Art itself builds nothing");
	}

	// ------------------------------------------------------------------ what builds it

	@Test
	void techniqueBuildsMoreThanPlainBlows() {
		assertTrue(MomentumRules.cleanHit(true) > MomentumRules.cleanHit(false), "a falling blow more than a plain one");
		for (int slot = 1; slot < 4; slot++) {
			assertTrue(MomentumRules.art(slot) >= MomentumRules.art(slot - 1), "a later art builds at least as much");
		}
		assertTrue(MomentumRules.art(0) > MomentumRules.CLEAN_HIT, "an art lands more than a blow");
		assertTrue(MomentumRules.PERFECT_GUARD > MomentumRules.art(3), "a perfect guard, the hardest timing, the most of anything but a finisher");
		assertTrue(MomentumRules.PERFECT_GUARD > MomentumRules.PERFECT_DEFLECT, "a blow turned aside more than an arrow");
		assertTrue(MomentumRules.STEP_THROUGH > MomentumRules.art(0));
		assertTrue(MomentumRules.FINISHER > MomentumRules.PERFECT_GUARD, "a finisher most of all");
		assertTrue(MomentumRules.BREAK > MomentumRules.CRITICAL_HIT);
	}

	@Test
	void anArtsFirstFoeCountsMostThenAFewMoreALittle() {
		MomentumRules.Temper plain = MomentumRules.Temper.PLAIN;
		assertEquals(MomentumRules.art(1), MomentumRules.artFoe(1, 1, plain), 1e-9);
		assertEquals(MomentumRules.ART_EXTRA, MomentumRules.artFoe(1, 2, plain), 1e-9);
		assertEquals(MomentumRules.ART_EXTRA, MomentumRules.artFoe(1, 1 + MomentumRules.ART_EXTRA_FOES, plain), 1e-9);
		assertEquals(0, MomentumRules.artFoe(1, 2 + MomentumRules.ART_EXTRA_FOES, plain), 1e-9, "a crowd's tail adds nothing");
		assertEquals(0, MomentumRules.artFoe(1, 0, plain), 1e-9);
		assertEquals(0, MomentumRules.artFoe(4, 2, plain), 1e-9, "nor does anything the Final Art reaches");
		MomentumRules.Temper thunder = MomentumRules.temper("thunder");
		assertTrue(MomentumRules.artFoe(1, 2, thunder) > MomentumRules.artFoe(1, 2, plain), "Thunder feeds on crowds");
	}

	@Test
	void anArtFromAfarCountsHalfButGalesCountsWhole() {
		MomentumRules.Temper plain = MomentumRules.Temper.PLAIN;
		assertEquals(1.0, MomentumRules.reach(2.5, plain), 1e-9);
		assertEquals(MomentumRules.RANGED_SHARE, MomentumRules.reach(MomentumRules.RANGED_FROM + 4, plain), 1e-9);
		assertEquals(1.0, MomentumRules.reach(12, MomentumRules.temper("gale")), 1e-9);
	}

	// ------------------------------------------------------------------ what knocks it down

	@Test
	void aHitTakenKnocksOffAShareByHowHeavyItWas() {
		MomentumRules.Temper plain = MomentumRules.Temper.PLAIN;
		double light = MomentumRules.loss(80, 2, 20, false, plain);
		double heavy = MomentumRules.loss(80, 12, 20, false, plain);
		assertTrue(heavy > light, "a heavier blow knocks off more");
		assertEquals((80 * MomentumRules.LOSS_MOST + MomentumRules.LOSS_FLAT), MomentumRules.loss(80, 1000, 20, false, plain), 1e-9, "never past the most");
		assertTrue(light >= 80 * MomentumRules.LOSS_LEAST, "always at least the least share");
		assertEquals(light * MomentumRules.GUARDED_LOSS, MomentumRules.loss(80, 2, 20, true, plain), 1e-9, "a held guard halves it");
		assertEquals(0, MomentumRules.loss(0, 5, 20, false, plain), 1e-9);
		assertEquals(3, MomentumRules.loss(3, 20, 20, false, plain), 1e-9, "never more than there is");
		// A typical blow (a zombie's three on twenty) at a high tier drops it about a fifth.
		double fall = MomentumRules.loss(80, 3, 20, false, plain);
		assertTrue(fall > 12 && fall < 24, "a zombie's blow at 80: " + fall);
	}

	@Test
	void stoneLosesLeastAndNothingThroughItsGuard() {
		MomentumRules.Temper stone = MomentumRules.temper("stone");
		assertEquals(0, MomentumRules.loss(80, 6, 20, true, stone), 1e-9);
		for (String method : METHODS) {
			if (!method.equals("stone")) {
				assertTrue(MomentumRules.loss(80, 6, 20, false, MomentumRules.temper(method)) > MomentumRules.loss(80, 6, 20, false, stone),
					"Stone gives least ground to a blow, not " + method);
			}
		}
	}

	// ------------------------------------------------------------------ the ebb

	@Test
	void itHoldsThroughAFightThenEbbs() {
		double ebb = MomentumRules.ebbPerTick(MomentumRules.Temper.PLAIN, 1.0);
		assertEquals(MomentumRules.EBB / 20.0, ebb, 1e-9);
		assertEquals(80, MomentumRules.current(80, 100, ebb, 0, 0, 100), 1e-9, "held");
		assertEquals(80, MomentumRules.current(80, 100, ebb, 0, 0, 60), 1e-9, "held before too");
		assertEquals(80 - ebb * 20, MomentumRules.current(80, 100, ebb, 0, 0, 120), 1e-9, "a second's ebb");
		assertEquals(0, MomentumRules.current(80, 100, ebb, 0, 0, 100000), 1e-9, "all gone in the end");
		// From the peak, out of a fight: gone from the peak within a second of the ebb, and all of it in under fifteen.
		assertFalse(MomentumRules.peak(MomentumRules.current(100, 0, ebb, 0, 0, 20)));
		assertEquals(0, MomentumRules.current(100, 0, ebb, 0, 0, 15 * 20), 1e-9);
		assertEquals(0, MomentumRules.ebbPerTick(MomentumRules.Temper.PLAIN, 0), 1e-9, "a server may stop the ebb");
	}

	@Test
	void anAwakeningsHoldKeepsItHighAndUnebbing() {
		double ebb = MomentumRules.ebbPerTick(MomentumRules.Temper.PLAIN, 1.0);
		// Held at 95 until tick 400: no ebb, never below the floor, even after a hit took it to 60.
		assertEquals(95, MomentumRules.current(60, 0, ebb, 95, 400, 300), 1e-9);
		assertEquals(100, MomentumRules.current(100, 0, ebb, 95, 400, 300), 1e-9, "no ebb while it holds");
		// After the hold, it ebbs from what it was.
		assertEquals(100 - ebb * 20, MomentumRules.current(100, 0, ebb, 95, 400, 420), 1e-9);
	}

	// ------------------------------------------------------------------ keeping it honest

	@Test
	void practiceNeverReachesThePeakOutsideTheArena() {
		assertFalse(MomentumRules.peak(MomentumRules.PRACTICE_CEILING));
		assertEquals(MomentumRules.PRACTICE_CEILING, MomentumRules.ceiling(true, false), 1e-9);
		assertEquals(MomentumRules.MAX, MomentumRules.ceiling(true, true), 1e-9, "the arena lets it be practised whole");
		assertEquals(MomentumRules.MAX, MomentumRules.ceiling(false, false), 1e-9);
		assertTrue(MomentumRules.PRACTICE_SHARE < 1);
	}

	@Test
	void eachFoeGivesOnlySoMuchFromBlows() {
		double zombie = MomentumRules.budget(20, false, false, false);
		double ravager = MomentumRules.budget(100, false, false, false);
		double boss = MomentumRules.budget(300, true, false, false);
		assertTrue(ravager > zombie && boss > ravager, zombie + " " + ravager + " " + boss);
		assertTrue(MomentumRules.budget(1, false, false, false) >= 12, "even the weakest gives a little");
		assertEquals(MomentumRules.PLAYER_BUDGET, MomentumRules.budget(20, false, true, false), 1e-9);
		assertEquals(MomentumRules.PLAYER_BUDGET, MomentumRules.budget(1000, false, false, true), 1e-9, "a dummy gives a strong foe's, never its health's");
		assertFalse(MomentumRules.peak(MomentumRules.budget(1000, false, false, false)), "one foe's blows alone never reach the peak");
		assertEquals(4, MomentumRules.fromBudget(4, 10, 20), 1e-9);
		assertEquals(2, MomentumRules.fromBudget(4, 18, 20), 1e-9);
		assertEquals(0, MomentumRules.fromBudget(4, 25, 20), 1e-9);
	}

	// ------------------------------------------------------------------ the methods' tempers

	@Test
	void everyMethodHasATemperFeedingOnTheStateItsOwnArtsLeave() {
		Set<Integer> favours = new HashSet<>();
		for (String method : METHODS) {
			MomentumRules.Temper t = MomentumRules.temper(method);
			assertNotSame(MomentumRules.Temper.PLAIN, t, method);
			assertNotEquals(0, t.favours(), method + " feeds on something");
			assertTrue(favours.add(t.favours()), method + " feeds on a state of its own");
			assertTrue(t.hitOn(false, t.favours()) > t.hitOn(false, 0), method + "'s blows build faster on a foe in that state");
			assertTrue(t.grace() >= 40 && t.grace() <= 200, method + " holds " + t.grace());
			assertTrue(t.ebb() > 0.3 && t.ebb() < 2, method + " ebbs " + t.ebb());
		}
		assertEquals(METHODS.size(), MomentumRules.TEMPERS.size());
		assertSame(MomentumRules.Temper.PLAIN, MomentumRules.temper("an_addon_method"));
		assertSame(MomentumRules.Temper.PLAIN, MomentumRules.temper(null));
	}

	@Test
	void eachMethodsMomentumMovesItsOwnWay() {
		Map<String, MomentumRules.Temper> t = MomentumRules.TEMPERS;
		// Ember burns hot and out: the most from blows, and the fastest ebb.
		assertEquals("ember", most(t, x -> x.hit()));
		assertEquals("ember", most(t, x -> x.ebb()));
		// Stone gives least ground and turns blows aside best.
		assertEquals("stone", least(t, x -> x.loss()));
		assertEquals("stone", most(t, x -> x.guard()));
		assertTrue(t.get("stone").steadfast());
		// Rime and Verdant hold it longest out of a fight (the slowest ebb); Hourglass waits longest before it ebbs at all.
		assertEquals(0.6, t.get("rime").ebb(), 1e-9);
		assertEquals(0.6, t.get("verdant").ebb(), 1e-9);
		assertEquals("hourglass", most(t, x -> x.grace()));
		// Gale moves: the most from steps, and arts from afar count whole.
		assertEquals("gale", most(t, x -> x.step()));
		assertTrue(t.get("gale").reaches());
		// Thunder feeds on crowds; Starlit on its arts; Crimson on blood, its own too.
		assertEquals("thunder", most(t, x -> x.chain()));
		assertEquals("starlit", most(t, x -> x.art()));
		assertTrue(t.get("crimson").bloodied());
		assertTrue(t.get("crimson").loss() < 1, "Crimson knocks off less than most");
	}

	private static String most(Map<String, MomentumRules.Temper> t, java.util.function.ToDoubleFunction<MomentumRules.Temper> f) {
		return t.entrySet().stream().max((a, b) -> Double.compare(f.applyAsDouble(a.getValue()), f.applyAsDouble(b.getValue()))).orElseThrow().getKey();
	}

	private static String least(Map<String, MomentumRules.Temper> t, java.util.function.ToDoubleFunction<MomentumRules.Temper> f) {
		return t.entrySet().stream().min((a, b) -> Double.compare(f.applyAsDouble(a.getValue()), f.applyAsDouble(b.getValue()))).orElseThrow().getKey();
	}

	// ------------------------------------------------------------------ how a fight plays out

	/**
	 * A fight, a tick at a time: a full swing every {@code swingEvery} ticks (each foe takes four, its budget permitting), an art every
	 * {@code artEvery} (its first foe and two more), a hit taken every {@code hitEvery} (0 never) of {@code damage} on twenty health.
	 * Returns the tick the peak was first reached, or -1.
	 */
	private static int fight(MomentumRules.Temper t, int ticks, int swingEvery, int artEvery, int hitEvery, double damage, boolean favoured) {
		double value = 0;
		double given = 0;
		int swings = 0;
		for (int tick = 1; tick <= ticks; tick++) {
			if (tick % swingEvery == 0) {
				double budget = MomentumRules.budget(24, false, false, false);
				double hit = MomentumRules.fromBudget(t.hitOn(false, favoured ? t.favours() : 0), given, budget);
				given += hit;
				value = Math.min(MomentumRules.MAX, value + hit);
				if (++swings % 4 == 0) {
					// The foe falls; the next is fresh.
					given = 0;
					value = Math.min(MomentumRules.MAX, value + MomentumRules.KILL);
				}
			}
			if (artEvery > 0 && tick % artEvery == 0) {
				for (int n = 1; n <= 3; n++) {
					value = Math.min(MomentumRules.MAX, value + MomentumRules.artFoe(0, n, t));
				}
			}
			if (hitEvery > 0 && tick % hitEvery == 0) {
				value -= MomentumRules.loss(value, damage, 20, false, t);
			}
			if (MomentumRules.peak(value)) {
				return tick;
			}
		}
		return -1;
	}

	@Test
	void aCleanFightReachesThePeakInTenOrFifteenSecondsAndAMessyOneDoesnt() {
		MomentumRules.Temper plain = MomentumRules.Temper.PLAIN;
		// Full swings every 0.65 s (a foe felled every fourth) and an art every 3 s, nothing taken: the peak in 8 to 25 seconds, never in a moment.
		int clean = fight(plain, 1200, 13, 60, 0, 0, false);
		assertTrue(clean > 8 * 20 && clean < 25 * 20, "a clean fight peaks at tick " + clean);
		// Blows alone, no arts: slower.
		int blows = fight(plain, 1200, 13, 0, 0, 0, false);
		assertTrue(blows == -1 || blows > clean, "blows alone peak at " + blows);
		// Struck every two seconds by a zombie: never there.
		assertEquals(-1, fight(plain, 1200, 13, 60, 40, 3, false), "a fight going badly never peaks");
		// A method fighting its own way (its foes in the state it feeds on) gets there sooner.
		for (String method : METHODS) {
			MomentumRules.Temper t = MomentumRules.temper(method);
			int own = fight(t, 1200, 13, 60, 0, 0, true);
			assertTrue(own > 0 && own <= clean + 60, method + " fighting its own way peaks at " + own + " (plain " + clean + ")");
		}
	}
}
