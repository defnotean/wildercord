package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Awakening, the pure part ({@link AwakeningRules}): when a swordsman may awaken and why not, how long it lasts and what feeds it,
 * what it gives (and how little of it against a player), the spent state and the rest after it, the input's charge, the look's
 * curve, and the Sovereign's awakened Dominion. Its names, descriptions and voices are checked against the generated assets.
 */
class AwakeningRulesTest {
	private static final List<String> METHODS = List.of("ember", "rime", "thunder", "gale", "stone", "verdant", "hollow", "starlit", "hourglass",
		"crimson");

	// ------------------------------------------------------------------ when

	@Test
	void aFullPoolIsNineteenTwentiethsOfIt() {
		assertTrue(AwakeningRules.full(70, 70));
		assertTrue(AwakeningRules.full(66.5, 70), "a coated swing's price never stands in the way");
		assertFalse(AwakeningRules.full(66.4, 70));
		assertFalse(AwakeningRules.full(0, 0), "no pool is never full");
		assertTrue(AwakeningRules.full(152, 160));
	}

	@Test
	void theRefusalsComeInOrder() {
		// Everything in place: no refusal.
		assertNull(AwakeningRules.refusal(true, AuraRules.EDGE, false, false, false, true, 70, 70, true, 60, 50));
		assertEquals(AwakeningRules.Refusal.OFF, AwakeningRules.refusal(false, AuraRules.EDGE, true, true, true, false, 0, 70, true, 0, 50));
		assertEquals(AwakeningRules.Refusal.STAGE, AwakeningRules.refusal(true, AuraRules.FLOW, false, false, false, true, 40, 40, true, 100, 50));
		assertEquals(AwakeningRules.Refusal.AWAKENED, AwakeningRules.refusal(true, AuraRules.FORM, true, false, true, true, 110, 110, true, 100, 50));
		assertEquals(AwakeningRules.Refusal.SPENT, AwakeningRules.refusal(true, AuraRules.FORM, false, true, true, true, 0, 110, true, 0, 50));
		assertEquals(AwakeningRules.Refusal.RESTING, AwakeningRules.refusal(true, AuraRules.FORM, false, false, true, true, 110, 110, true, 100, 50));
		assertEquals(AwakeningRules.Refusal.NO_WEAPON, AwakeningRules.refusal(true, AuraRules.FORM, false, false, false, false, 110, 110, true, 100, 50));
		assertEquals(AwakeningRules.Refusal.POOL, AwakeningRules.refusal(true, AuraRules.FORM, false, false, false, true, 90, 110, true, 100, 50));
		assertEquals(AwakeningRules.Refusal.MOMENTUM, AwakeningRules.refusal(true, AuraRules.FORM, false, false, false, true, 110, 110, true, 49.9, 50));
		// Momentum before the pool: short of it, a lone tap looses the slash, which spends the pool; the momentum is what's really missing.
		assertEquals(AwakeningRules.Refusal.MOMENTUM, AwakeningRules.refusal(true, AuraRules.EDGE, false, false, false, true, 58, 70, true, 0, 50));
		// Where the server has momentum off, a full pool alone is enough.
		assertNull(AwakeningRules.refusal(true, AuraRules.FORM, false, false, false, true, 110, 110, false, 0, 50));
		// Its message keys are the refusal's name.
		assertEquals("message.wildercord.aura.awaken.momentum", AwakeningRules.Refusal.MOMENTUM.key());
	}

	@Test
	void itOpensAtEdgeAndAsksForTheSecondTier() {
		assertEquals(AuraRules.EDGE, AwakeningRules.FROM);
		assertEquals(2, MomentumRules.tier(AwakeningRules.MOMENTUM), "a fight going well, short of the peak");
		assertTrue(AwakeningRules.MOMENTUM < MomentumRules.PEAK, "it asks less than the peak (it gives the peak)");
	}

	// ------------------------------------------------------------------ how long

	@Test
	void itLastsLongerAtEachStage() {
		assertEquals(0, AwakeningRules.ticks(AuraRules.GLOW, 1));
		assertEquals(0, AwakeningRules.ticks(AuraRules.FLOW, 1));
		int edge = AwakeningRules.ticks(AuraRules.EDGE, 1);
		int form = AwakeningRules.ticks(AuraRules.FORM, 1);
		int sovereign = AwakeningRules.ticks(AuraRules.SOVEREIGN, 1);
		assertEquals(240, edge, "twelve seconds at Edge");
		assertEquals(320, form, "sixteen at Form");
		assertEquals(400, sovereign, "twenty at Sovereign");
		assertTrue(edge < form && form < sovereign);
		assertEquals(480, AwakeningRules.ticks(AuraRules.SOVEREIGN, 1.2), "times the server's duration");
		assertEquals(20, AwakeningRules.ticks(AuraRules.EDGE, 0), "never less than a second");
		assertEquals(400, AwakeningRules.ticks(99, 1), "a stage past the last counts as the last");
	}

	@Test
	void finishersFeedItUpToALimit() {
		int fed = 0;
		int finishers = 0;
		while (AwakeningRules.extend(fed) > 0) {
			fed += AwakeningRules.extend(fed);
			finishers++;
		}
		assertEquals(AwakeningRules.MAX_EXTEND, fed);
		assertEquals(4, finishers, "a second each for four finishers");
		assertEquals(0, AwakeningRules.extend(AwakeningRules.MAX_EXTEND));
		assertEquals(10, AwakeningRules.extend(AwakeningRules.MAX_EXTEND - 10), "the last one only what's left");
	}

	// ------------------------------------------------------------------ what it gives

	@Test
	void artsCostNothingByDefault() {
		assertEquals(0, AwakeningRules.price(30, AwakeningRules.ART_PRICE), 1e-9);
		assertEquals(7.5, AwakeningRules.price(30, 0.25), 1e-9, "a server can make it little instead");
		assertEquals(30, AwakeningRules.price(30, 5), 1e-9, "never more than the price");
		assertEquals(0, AwakeningRules.price(-3, 1), 1e-9);
	}

	@Test
	void itsBlowsAreHarderAndOnlyALittleAgainstAPlayer() {
		double creature = AwakeningRules.damage(AwakeningRules.DAMAGE, false);
		double player = AwakeningRules.damage(AwakeningRules.DAMAGE, true);
		assertEquals(1.15, creature, 1e-9);
		assertEquals(1.075, player, 1e-9);
		// Against a player it's one more bonus under their cap and the PvP scale (0.6 by default): a few percent in the end.
		double pvpScale = 0.6;
		assertTrue((player - 1) * pvpScale <= 0.05, "at most five percent more against a player");
		assertEquals(1.0, AwakeningRules.damage(0, true), 1e-9);
		assertTrue(AwakeningRules.SPEED <= 0.1 + 1e-9 && AwakeningRules.ATTACK_SPEED <= 0.1 + 1e-9, "a little faster, never a sprint's worth");
	}

	@Test
	void itHoldsMomentumAtThePeak() {
		assertTrue(MomentumRules.peak(AwakeningRules.HOLD));
		// The Final Art's release spends some, but the hold keeps it at the peak.
		double after = MomentumRules.current(AwakeningRules.HOLD - MomentumRules.FINAL_SPEND, 0, 0.4, AwakeningRules.HOLD, 1000, 500);
		assertTrue(MomentumRules.peak(after), "held at the peak through the Final Art's release (" + after + ")");
	}

	// ------------------------------------------------------------------ after: a real cost

	@Test
	void theSpentTimeIsARealCostAndTheRestLonger() {
		int longest = AwakeningRules.ticks(AuraRules.SOVEREIGN, 1) + AwakeningRules.MAX_EXTEND;
		assertTrue(AwakeningRules.SPENT_TICKS >= longest, "spent at least as long as the longest awakening burns");
		assertTrue(AwakeningRules.COOLDOWN_TICKS > AwakeningRules.SPENT_TICKS * 3, "and the rest far longer than that");
		assertEquals(0, AwakeningRules.SPENT_SLOW, "slowed as Slowness I slows");
	}

	@Test
	void theFinalArtComesAtMostOncePerAwakening() {
		int longest = AwakeningRules.ticks(AuraRules.SOVEREIGN, 1) + AwakeningRules.MAX_EXTEND;
		assertTrue(StringRules.FINAL_COOLDOWN > longest, "its rest outlasts even a fed Sovereign's awakening");
		assertEquals(ArtRules.SLOT_COOLDOWN[4], StringRules.FINAL_COOLDOWN, "every method's Final Art rests the same");
	}

	@Test
	void thePhasesFollowFromTheTimes() {
		int awakened = AwakeningRules.Phase.AWAKENED.ordinal();
		int spent = AwakeningRules.Phase.SPENT.ordinal();
		assertTrue(AwakeningRules.awakened(awakened, 500, 499));
		assertFalse(AwakeningRules.awakened(awakened, 500, 500), "it ends as its time comes");
		assertFalse(AwakeningRules.awakened(spent, 500, 100));
		assertTrue(AwakeningRules.spent(spent, 900, 899));
		assertFalse(AwakeningRules.spent(spent, 900, 900));
		assertFalse(AwakeningRules.spent(awakened, 900, 100));
		// Ended on time: spent from its end, ready a rest after it.
		AwakeningRules.Ending onTime = AwakeningRules.ended(1000, 0, 600, 3600, 1000);
		assertEquals(AwakeningRules.Phase.SPENT, onTime.phase());
		assertEquals(1600, onTime.spentUntil());
		assertEquals(4600, onTime.readyAt());
		// Back long after (it ran out while they were away): no longer spent, the rest still counted from its end.
		AwakeningRules.Ending late = AwakeningRules.ended(1000, 0, 600, 3600, 2000);
		assertEquals(AwakeningRules.Phase.RESTING, late.phase());
		assertEquals(4600, late.readyAt());
		// Fed by finishers, the ready time written as it began moved on with it: never sooner than that.
		assertEquals(5000, AwakeningRules.ended(1000, 5000, 600, 3600, 1000).readyAt());
		assertEquals(AwakeningRules.Phase.AWAKENED, AwakeningRules.Phase.of(1));
		assertEquals(AwakeningRules.Phase.NONE, AwakeningRules.Phase.of(42));
	}

	// ------------------------------------------------------------------ the input

	@Test
	void theChargeFillsOnceTheSecondPressIsHeld() {
		assertEquals(0, AwakeningRules.charge(0));
		assertEquals(0, AwakeningRules.charge(AwakeningRules.CHARGE_SHOWS), "a quick second press (a double tap) shows nothing");
		assertTrue(AwakeningRules.charge(AwakeningRules.CHARGE_SHOWS + 1) > 0);
		assertEquals(1, AwakeningRules.charge(AwakeningRules.HOLD_TICKS), 1e-6);
		assertEquals(1, AwakeningRules.charge(AwakeningRules.HOLD_TICKS + 30), 1e-6);
		float last = 0;
		for (int t = 0; t <= AwakeningRules.HOLD_TICKS; t++) {
			float c = AwakeningRules.charge(t);
			assertTrue(c >= last, "it only fills");
			last = c;
		}
		// Deliberate, never a slip: well over a quarter of a second, under a second.
		assertTrue(AwakeningRules.HOLD_TICKS >= 10 && AwakeningRules.HOLD_TICKS <= 20);
		assertTrue(AwakeningRules.CHARGE_SHOWS < 5, "it shows before a hold would have gone (5 ticks)");
	}

	// ------------------------------------------------------------------ the look

	@Test
	void theFormGathersBurstsSettlesAndGutters() {
		int life = 400;
		assertEquals(0, AwakeningRules.form(-1, life), "nothing before it");
		assertTrue(AwakeningRules.form(AwakeningRules.BURST_AT - 1, life) <= 0.25F, "drawn in, a little, before the burst");
		assertTrue(AwakeningRules.form(AwakeningRules.BURST_AT, life) >= 1.0F, "it bursts into its form");
		float most = 0;
		for (int t = 0; t < AwakeningRules.RISE; t++) {
			most = Math.max(most, AwakeningRules.form(t, life - t));
		}
		assertTrue(most > 1.05F && most <= 1.25F, "a surge past its form at the burst (" + most + ")");
		assertEquals(1.0F, AwakeningRules.form(AwakeningRules.RISE + 10, 200), 1e-6, "settled");
		float gutter = AwakeningRules.form(300, AwakeningRules.GUTTER / 2.0);
		assertTrue(gutter < 0.8F && gutter > 0.4F, "guttering at the end (" + gutter + ")");
		assertEquals(0, AwakeningRules.form(300, 0), "gone once it ends");
		assertTrue(AwakeningRules.GLOW > 0.3F && AwakeningRules.SPENT_GLOW < 0.5F);
	}

	// ------------------------------------------------------------------ the Sovereign's Dominion

	@Test
	void anAwakenedDominionIsStrongerAndEachMethodHasItsOwn() {
		assertTrue(AwakeningRules.Sovereign.RADIUS > 1 && AwakeningRules.Sovereign.TIME > 1);
		assertTrue(AuraRules.DOMINION_WEAKEN + AwakeningRules.Sovereign.WEAKEN < 0.9, "never past the weakening's own cap");
		assertTrue(AwakeningRules.Sovereign.FLOW > AuraRules.DOMINION_FLOW);
		assertEquals(AwakeningRules.Sovereign.STARLIT_FLOW, AwakeningRules.Sovereign.flow("starlit"), 1e-9, "Starlit's aura flows back fastest");
		assertEquals(AwakeningRules.Sovereign.FLOW, AwakeningRules.Sovereign.flow("ember"), 1e-9);
		assertEquals(2, AwakeningRules.Sovereign.CHAINS);
		Set<AwakeningRules.Flavour> seen = new HashSet<>();
		Set<String> ids = new HashSet<>();
		for (String method : METHODS) {
			AwakeningRules.Flavour f = AwakeningRules.Flavour.of(method);
			assertNotEquals(AwakeningRules.Flavour.PLAIN, f, method + " has its own");
			assertEquals(method, f.method);
			assertTrue(seen.add(f), method + "'s is its own");
			assertTrue(ids.add(f.id));
		}
		assertEquals(AwakeningRules.Flavour.PLAIN, AwakeningRules.Flavour.of("someone:else"), "an add-on's method raises the plain one");
		assertEquals(AwakeningRules.Flavour.PLAIN, AwakeningRules.Flavour.of(null));
		assertEquals(11, AwakeningRules.Flavour.values().length);
		// Holds under the player caps the arts already keep.
		assertTrue(AwakeningRules.Sovereign.RIME_FREEZE >= ArtRules.PVP_HOLD_TICKS, "a player is held only as long as any art may (the kit cuts it)");
		assertTrue(AwakeningRules.Sovereign.THUNDER_BOLT < 1 && AwakeningRules.Sovereign.STARLIT_STAR < 1 && AwakeningRules.Sovereign.CRIMSON_BLEED < 1,
			"its own strikes are small, a beat at a time");
	}

	// ------------------------------------------------------------------ named, described, voiced

	@Test
	void everythingIsNamedDescribedAndVoiced() throws java.io.IOException {
		com.google.gson.JsonObject lang = read("/assets/wildercord/lang/en_us.json");
		Set<String> names = new HashSet<>();
		for (AwakeningRules.Flavour f : AwakeningRules.Flavour.values()) {
			assertTrue(lang.has(f.nameKey()), f.nameKey());
			assertTrue(lang.has(f.nameKey() + ".desc"), f.nameKey() + ".desc");
			assertTrue(names.add(lang.get(f.nameKey()).getAsString()), f.id + " has a name of its own");
		}
		for (AwakeningRules.Refusal why : AwakeningRules.Refusal.values()) {
			assertTrue(lang.has(why.key()), why.key());
		}
		for (String key : List.of("aura.wildercord.technique.awaken", "aura.wildercord.technique.awaken.desc", "aura.wildercord.awakening",
				"screen.wildercord.aura.key_tap_hold", "screen.wildercord.aura.awakened_left", "screen.wildercord.aura.spent_left",
				"screen.wildercord.aura.awakening_rests", "screen.wildercord.aura.awakening_ready", "screen.wildercord.aura.awakening_pool",
				"screen.wildercord.aura.awakening_momentum", "screen.wildercord.aura.awakening_waits", "message.wildercord.aura.spent",
				"message.wildercord.aura.recovered", "toast.wildercord.aura.awakening", "toast.wildercord.aura.sovereign_dominion",
				"aura.wildercord.sovereign.kicker")) {
			assertTrue(lang.has(key), key);
		}
		com.google.gson.JsonObject kit = read("/assets/wildercord/kit_sounds.json").getAsJsonObject("events");
		for (String sound : AwakeningFx.SOUNDS) {
			assertTrue(kit.has(sound), sound + " isn't in the feel kit: run python tools/feel/build.py --only aura");
		}
		for (String method : METHODS) {
			assertTrue(AwakeningFx.SOUNDS.contains(AwakeningFx.voice(method)), method + " has its own voice");
		}
		assertEquals("aura_awaken_steel", AwakeningFx.voice("someone:else"));
	}

	private static com.google.gson.JsonObject read(String path) throws java.io.IOException {
		try (java.io.InputStream in = AwakeningRulesTest.class.getResourceAsStream(path)) {
			assertNotNull(in, path);
			return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}
}
