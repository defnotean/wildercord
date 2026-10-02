package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import static dev.wildercord.aura.StanceRules.Kind.BOSS;
import static dev.wildercord.aura.StanceRules.Kind.CREATURE;
import static dev.wildercord.aura.StanceRules.Kind.DUMMY;
import static dev.wildercord.aura.StanceRules.Kind.PLAYER;
import static dev.wildercord.aura.StanceRules.Kind.STURDY;
import static dev.wildercord.aura.StanceRules.Source.ART;
import static dev.wildercord.aura.StanceRules.Source.BARE;
import static dev.wildercord.aura.StanceRules.Source.BLOW;
import static dev.wildercord.aura.StanceRules.Source.CRITICAL;
import static dev.wildercord.aura.StanceRules.Source.GLANCE;
import static dev.wildercord.aura.StanceRules.Source.SLASH;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Stance and openings, the pure part ({@link StanceRules}): how much stance each kind of foe has, what wears it and how much, how
 * it comes back, how long an opening and the steadiness after last, what a finisher deals (never a one-shot on a player), and how a
 * fight with a weak foe, a strong one and a boss plays out over it.
 */
class StanceRulesTest {
	/** A diamond sword's coated full swing, about. */
	private static final double DIAMOND = 7.7;

	private static double wear(double amount, StanceRules.Source source, StanceRules.Kind kind) {
		return StanceRules.wear(amount, source, 1.0, false, 1.0, kind, 0.6, 1.0);
	}

	// ------------------------------------------------------------------ how much

	@Test
	void aWeakFoeHasAsMuchStanceAsHealthAndAStrongOneLess() {
		assertEquals(20, StanceRules.pool(CREATURE, 20, 0), 1e-9, "a zombie's");
		assertEquals(20, StanceRules.pool(CREATURE, 8, 0), 1e-9, "never under the least");
		assertTrue(StanceRules.pool(CREATURE, 100, 0) < 100, "a ravager's is less than its health");
		assertEquals(StanceRules.POOL_MOST, StanceRules.pool(CREATURE, 400, 0), 1e-9, "and never past the most");
		assertEquals(StanceRules.pool(CREATURE, 40, 0) * StanceRules.STURDY, StanceRules.pool(STURDY, 40, 0), 1e-9, "a sturdy foe a quarter more");
		assertEquals(StanceRules.PLAYER_POOL, StanceRules.pool(PLAYER, 20, 0), 1e-9);
		assertEquals(StanceRules.DUMMY_POOL, StanceRules.pool(DUMMY, 1000, 0), 1e-9, "a dummy's is fixed, for practice");
	}

	@Test
	void aBossHasMuchMoreAndGrowsSteadierEachBreak() {
		double wither = StanceRules.pool(BOSS, 300, 0);
		assertTrue(wither >= StanceRules.BOSS_POOL_LEAST && wither <= StanceRules.BOSS_POOL_MOST, "the wither's " + wither);
		assertTrue(wither > StanceRules.pool(CREATURE, 40, 0), "more than any ordinary creature's");
		assertEquals(StanceRules.BOSS_POOL_LEAST, StanceRules.pool(BOSS, 80, 0), 1e-9, "even a small boss has the least of a boss");
		assertTrue(StanceRules.pool(BOSS, 300, 1) > wither, "steadier after a break");
		assertEquals(wither * StanceRules.BOSS_GROWTH_MOST, StanceRules.pool(BOSS, 300, 50), 1e-9, "never more than twice");
		assertEquals(StanceRules.pool(CREATURE, 40, 0), StanceRules.pool(CREATURE, 40, 3), 1e-9, "only a boss grows");
	}

	// ------------------------------------------------------------------ what wears it

	@Test
	void artsWearMoreThanBlowsAndGlancesLittle() {
		double blow = wear(DIAMOND, BLOW, CREATURE);
		assertEquals(DIAMOND, blow, 1e-9, "a full swing wears what it was dealt at");
		assertTrue(wear(DIAMOND, CRITICAL, CREATURE) > blow, "a falling blow more");
		assertTrue(wear(DIAMOND, ART, CREATURE) > wear(DIAMOND, CRITICAL, CREATURE), "an art more still");
		assertTrue(wear(DIAMOND, GLANCE, CREATURE) < blow * 0.5, "a short swing or a sweep's other blows little");
		assertTrue(wear(DIAMOND, BARE, CREATURE) < blow, "a blade with no aura less");
		assertTrue(wear(DIAMOND, SLASH, CREATURE) < blow, "aura from afar less");
		assertEquals(0, StanceRules.wear(DIAMOND, BLOW, 1.0, false, 1.0, CREATURE, 0.6, 0), 1e-9, "a server may switch it off");
		assertEquals(0, wear(0, BLOW, CREATURE), 1e-9);
	}

	@Test
	void stoneWearsStanceTheMost() {
		double quake = StanceRules.artWeight(true, false);
		double hold = StanceRules.artWeight(false, true);
		assertTrue(quake > hold && hold > StanceRules.artWeight(false, false), "a quake most, a hold more than a plain art");
		double stoneArt = StanceRules.wear(DIAMOND, ART, StanceRules.artWeight(true, true), true, 1.0, CREATURE, 0.6, 1.0);
		double frost = StanceRules.wear(DIAMOND, ART, StanceRules.artWeight(false, true), false, 1.0, CREATURE, 0.6, 1.0);
		assertTrue(stoneArt > frost * 1.3, "Stone's quaking arts far more than a freeze: " + stoneArt + " " + frost);
		assertTrue(StanceRules.wear(DIAMOND, BLOW, 1.0, true, 1.0, CREATURE, 0.6, 1.0) > wear(DIAMOND, BLOW, CREATURE), "and its plain blows too");
		assertEquals(StanceRules.wear(DIAMOND, ART, 1.0, true, 1.0, CREATURE, 0.6, 1.0), wear(DIAMOND, ART, CREATURE), 1e-9,
			"its arts are weighed by their kind, not twice");
	}

	@Test
	void momentumWearsStanceFaster() {
		double peak = MomentumRules.stanceFactor(MomentumRules.PEAK_TIER);
		assertEquals(DIAMOND * peak, StanceRules.wear(DIAMOND, BLOW, 1.0, false, peak, CREATURE, 0.6, 1.0), 1e-9);
	}

	@Test
	void aBossTakesLessOfEveryWearAndAPerfectGuardLessOfItsStance() {
		assertEquals(wear(DIAMOND, BLOW, CREATURE) * StanceRules.BOSS_TOUGHNESS, wear(DIAMOND, BLOW, BOSS), 1e-9);
		double pool = 40;
		assertEquals(pool * StanceRules.GUARD_BREAK, StanceRules.guardBreak(CREATURE, pool, 1.0), 1e-9);
		assertTrue(3 * StanceRules.GUARD_BREAK >= 1, "three perfect guards break any ordinary foe");
		assertTrue(StanceRules.guardBreak(BOSS, pool, 1.0) < StanceRules.guardBreak(CREATURE, pool, 1.0));
		assertTrue(4 * StanceRules.BOSS_GUARD_BREAK < 1, "a boss takes more than four");
	}

	@Test
	void aPlayersStanceTakesPressureNeverAMoment() {
		// No single blow (or art) more than a third of it, whatever the blade; the PvP scale applies.
		double huge = StanceRules.wear(500, ART, 2.0, true, 1.45, PLAYER, 1.0, 5.0);
		assertEquals(StanceRules.PLAYER_POOL * StanceRules.PVP_BLOW_CAP, huge, 1e-9);
		assertTrue(StanceRules.PVP_BLOW_CAP < 0.5, "it takes at least three blows to break");
		assertEquals(DIAMOND * 0.6, wear(DIAMOND, BLOW, PLAYER), 1e-9, "under the PvP scale");
		assertTrue(StanceRules.PVP_ART_CAP < 1, "no art alone breaks a player");
		assertEquals(StanceRules.PLAYER_POOL * StanceRules.GUARD_BREAK, StanceRules.guardBreak(PLAYER, StanceRules.PLAYER_POOL, 1.0), 1e-9);
	}

	// ------------------------------------------------------------------ coming back

	@Test
	void itComesBackAfterAWhileQuickly() {
		double pool = 40;
		assertEquals(30, StanceRules.worn(30, 0, pool, CREATURE, StanceRules.regenDelay(CREATURE)), 1e-9, "not yet");
		double later = StanceRules.worn(30, 0, pool, CREATURE, StanceRules.regenDelay(CREATURE) + 20);
		assertEquals(30 - pool * StanceRules.regenShare(CREATURE), later, 1e-9, "a second's worth");
		assertEquals(0, StanceRules.worn(pool, 0, pool, CREATURE, StanceRules.regenDelay(CREATURE) + 5 * 20 + 1), 1e-9, "whole in five seconds after");
		assertTrue(StanceRules.regenDelay(BOSS) < StanceRules.regenDelay(CREATURE), "a boss recovers sooner");
		assertTrue(StanceRules.regenDelay(PLAYER) < StanceRules.regenDelay(CREATURE), "and a player");
		assertEquals(1.0, StanceRules.left(0, pool), 1e-9);
		assertEquals(0.25, StanceRules.left(30, pool), 1e-9);
		assertEquals(0, StanceRules.left(50, pool), 1e-9);
	}

	// ------------------------------------------------------------------ opened, steady

	@Test
	void anOpeningIsAMomentAndTheSteadinessAfterLonger() {
		for (StanceRules.Kind kind : StanceRules.Kind.values()) {
			assertTrue(StanceRules.openTicks(kind) >= 20 && StanceRules.openTicks(kind) <= 80, kind + " opened " + StanceRules.openTicks(kind));
			assertTrue(StanceRules.steadyTicks(kind) >= StanceRules.openTicks(kind), kind + " stands steady at least as long");
			assertEquals(kind, StanceRules.Kind.of(kind.ordinal()));
		}
		assertTrue(StanceRules.openTicks(BOSS) < StanceRules.openTicks(CREATURE), "a boss's opening is shorter");
		assertTrue(StanceRules.steadyTicks(BOSS) > 3 * StanceRules.openTicks(BOSS), "and it can't be opened again soon");
		assertTrue(StanceRules.openTicks(PLAYER) < StanceRules.openTicks(CREATURE));
		assertEquals(CREATURE, StanceRules.Kind.of(-1));
	}

	// ------------------------------------------------------------------ the finisher

	@Test
	void aFinisherDealsAShareOfWhatTheFoeHasLost() {
		// A ravager at 40 of 100: a third of its 60 lost, under four blades.
		assertEquals(60 * StanceRules.SHARE, StanceRules.finisher(CREATURE, 60, 100, 7, 0.6, 1.0), 1e-9);
		assertEquals(StanceRules.CAP * 7, StanceRules.finisher(CREATURE, 600, 1000, 7, 0.6, 1.0), 1e-9, "held to four blades");
		// A boss: a small share, a smaller cap.
		assertEquals(150 * StanceRules.BOSS_SHARE, StanceRules.finisher(BOSS, 150, 300, 8, 0.6, 1.0), 1e-9);
		assertTrue(StanceRules.finisher(BOSS, 150, 300, 8, 0.6, 1.0) < StanceRules.finisher(CREATURE, 150, 300, 8, 0.6, 1.0));
		assertEquals(StanceRules.BOSS_CAP * 8, StanceRules.finisher(BOSS, 1000, 2000, 8, 0.6, 1.0), 1e-9);
		assertEquals(0, StanceRules.finisher(CREATURE, 0, 20, 7, 0.6, 1.0), 1e-9, "nothing lost, nothing more");
		assertEquals(0, StanceRules.finisher(CREATURE, 10, 20, 7, 0.6, 0), 1e-9, "a server may switch it off");
	}

	@Test
	void aFinisherOnAPlayerIsNeverAOneShot() {
		// From full health it adds nothing at all; at worst, a few hearts, under the PvP scale and a quarter of their health.
		assertEquals(0, StanceRules.finisher(PLAYER, 0, 20, 20, 1.0, 3.0), 1e-9);
		double worst = StanceRules.finisher(PLAYER, 20, 20, 20, 10.0, 1.0);
		assertEquals(20 * StanceRules.PVP_FINISHER_HEALTH, worst, 1e-9);
		assertEquals(StanceRules.PVP_FINISHER * 0.6, StanceRules.finisher(PLAYER, 1000, 1000, 20, 0.6, 1.0), 1e-9);
		assertEquals(10 * StanceRules.PLAYER_SHARE, StanceRules.finisher(PLAYER, 10, 20, 7, 0.6, 1.0), 1e-9);
	}

	@Test
	void aFinisherGivesAuraBackMoreAtEachStage() {
		assertEquals(8, StanceRules.finisherAura(AuraRules.GLOW, false), 1e-9);
		assertEquals(16, StanceRules.finisherAura(AuraRules.SOVEREIGN, false), 1e-9);
		assertEquals(16 * AuraRules.PRACTICE_GAIN, StanceRules.finisherAura(AuraRules.SOVEREIGN, true), 1e-9, "a quarter of it in practice");
		assertTrue(StanceRules.STARLIT_AURA > 1, "Starlit's more");
	}

	// ------------------------------------------------------------------ named, described, voiced

	@Test
	void everyFinisherIsNamedDescribedAndVoiced() throws java.io.IOException {
		com.google.gson.JsonObject lang = read("/assets/wildercord/lang/en_us.json");
		java.util.Set<String> names = new java.util.HashSet<>();
		for (String id : dev.wildercord.aura.arts.Finishers.IDS) {
			String key = "aura.wildercord.finisher." + id;
			assertTrue(lang.has(key), key);
			assertTrue(lang.has(key + ".desc"), key + ".desc");
			assertTrue(names.add(lang.get(key).getAsString()), id + " has a name of its own");
		}
		assertEquals(11, dev.wildercord.aura.arts.Finishers.IDS.size(), "the ten methods' and the common one");
		for (String key : java.util.List.of("message.wildercord.aura.art.peak", "message.wildercord.aura.guard_broken", "message.wildercord.aura.opened",
				"aura.wildercord.banner.finisher", "toast.wildercord.aura.peak_momentum", "toast.wildercord.aura.finisher",
				"toast.wildercord.aura.stance_break", "aura.wildercord.technique.momentum", "aura.wildercord.technique.momentum.desc",
				"aura.wildercord.technique.finisher", "aura.wildercord.technique.finisher.desc", "screen.wildercord.aura.art_needs_peak",
				"screen.wildercord.aura.momentum_line", "screen.wildercord.aura.momentum_peak", "screen.wildercord.aura.finisher_when")) {
			assertTrue(lang.has(key), key);
		}
		com.google.gson.JsonObject kit = read("/assets/wildercord/kit_sounds.json").getAsJsonObject("events");
		java.util.List<String> sounds = new java.util.ArrayList<>(dev.wildercord.aura.arts.Finishers.SOUNDS);
		sounds.addAll(java.util.List.of("aura_momentum_rise", "aura_momentum_peak", "aura_stance_break", "aura_finisher"));
		for (String sound : sounds) {
			assertTrue(kit.has(sound), sound + " isn't in the feel kit: run python tools/feel/build.py --only aura");
		}
		assertEquals(10, dev.wildercord.aura.arts.Finishers.SOUNDS.size(), "a voice for each method's finisher");
	}

	private static com.google.gson.JsonObject read(String path) throws java.io.IOException {
		try (java.io.InputStream in = StanceRulesTest.class.getResourceAsStream(path)) {
			assertNotNull(in, path);
			return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	// ------------------------------------------------------------------ how fights play out

	/** Full diamond blows (and every {@code artEvery}th an art) on a foe until its stance breaks: how many it took. */
	private static int blowsToBreak(StanceRules.Kind kind, double health, int breaks, int artEvery, boolean stone) {
		double pool = StanceRules.pool(kind, health, breaks);
		double worn = 0;
		for (int n = 1; n <= 500; n++) {
			boolean art = artEvery > 0 && n % artEvery == 0;
			worn += StanceRules.wear(art ? DIAMOND * 1.8 : DIAMOND, art ? ART : BLOW, art && stone ? StanceRules.artWeight(true, false) : 1.0, stone, 1.0,
				kind, 0.6, 1.0);
			if (worn >= pool) {
				return n;
			}
		}
		return -1;
	}

	@Test
	void aZombieFallsToPlainBlowsBeforeItOpensButTechniqueOpensIt() {
		int toBreak = blowsToBreak(CREATURE, 20, 0, 0, false);
		int toKill = (int) Math.ceil(20 / DIAMOND);
		assertTrue(toBreak >= toKill, "plain blows fell a zombie (" + toKill + ") before they open it (" + toBreak + ")");
		// Two perfect guards and a blow open one.
		double pool = StanceRules.pool(CREATURE, 20, 0);
		assertTrue(2 * StanceRules.guardBreak(CREATURE, pool, 1.0) + DIAMOND >= pool);
	}

	@Test
	void aStrongFoeOpensPartWayThroughAFightAndStoneOpensItSooner() {
		int plain = blowsToBreak(CREATURE, 100, 0, 3, false);
		int toKill = (int) Math.ceil(100 / DIAMOND);
		assertTrue(plain < toKill * 0.8, "a ravager opens well before it falls (" + plain + " of " + toKill + ")");
		assertTrue(blowsToBreak(CREATURE, 100, 0, 3, true) < plain, "Stone opens it sooner");
	}

	@Test
	void aBossIsBreakableButNeverTrivial() {
		// The wither (300) with a diamond blade, an art every third blow.
		int first = blowsToBreak(BOSS, 300, 0, 3, false);
		int toKill = (int) Math.ceil(300 / DIAMOND);
		assertTrue(first >= 10 && first <= toKill / 2, "its first opening takes a real fight: " + first + " blows of " + toKill);
		int second = blowsToBreak(BOSS, 300, 1, 3, false);
		assertTrue(second > first, "the second takes more (" + second + ")");
		// What two finishers take off it, at most: well under half of it.
		double finishers = StanceRules.finisher(BOSS, 300 * 0.5, 300, 7, 0.6, 1.0) + StanceRules.finisher(BOSS, 300 * 0.85, 300, 7, 0.6, 1.0);
		assertTrue(finishers < 300 * 0.2, "two finishers take " + finishers + " of 300");
	}
}
