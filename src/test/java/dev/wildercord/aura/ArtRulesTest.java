package dev.wildercord.aura;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.EmberArts;
import dev.wildercord.aura.arts.GaleArts;
import dev.wildercord.aura.arts.MethodArts;
import dev.wildercord.aura.arts.RimeArts;
import dev.wildercord.aura.arts.StoneArts;
import dev.wildercord.aura.arts.ThunderArts;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The breathing methods' arts, the pure parts: each art's price and rest, the balance between them (by slot, by method, and
 * against the techniques), the rules an art keeps against players and bosses, the shapes they reach, and the framework that
 * registers them ({@link AuraApi#registerArts}: slots, whose each art is, the common arts stepping aside, conflicts, the Final
 * Art's gate), with every art's name, description and voice really there.
 */
class ArtRulesTest {
	private static final List<String> METHODS = List.of("ember", "rime", "thunder", "gale", "stone");

	// ------------------------------------------------------------------ the arts, priced

	@Test
	void everyMethodHasFiveArtsOneASlot() {
		Set<String> ids = new HashSet<>();
		for (String method : METHODS) {
			List<ArtRules.Art> arts = ArtRules.of(method);
			assertEquals(5, arts.size(), method);
			for (int slot = 0; slot < 5; slot++) {
				ArtRules.Art art = arts.get(slot);
				assertEquals(slot, art.slot(), art.id() + " is in its slot's place");
				assertEquals(slot + AuraRules.GLOW, art.stage(), art.id());
				assertEquals(method, art.method());
				assertTrue(ids.add(art.id()), art.id() + " twice");
				assertSame(art, ArtRules.art(art.id()));
			}
		}
		assertEquals(25, ArtRules.ARTS.size());
		assertThrows(IllegalArgumentException.class, () -> ArtRules.art("no_such_art"));
	}

	@Test
	void everyArtCostsAndRestsAboutWhatItsSlotDoes() {
		for (ArtRules.Art art : ArtRules.ARTS) {
			double cost = ArtRules.SLOT_COST[art.slot()];
			int rest = ArtRules.SLOT_COOLDOWN[art.slot()];
			assertEquals(cost, art.cost(), 1e-9, art.id() + " costs its slot's price: a method is another answer, never a cheaper one");
			assertTrue(art.cooldown() >= rest * 0.8 && art.cooldown() <= rest * 1.2, art.id() + " rests about its slot's time (" + art.cooldown() + ")");
		}
		assertEquals(0, ArtRules.slot(AuraRules.GLOW));
		assertEquals(4, ArtRules.slot(AuraRules.SOVEREIGN));
		assertEquals(0, ArtRules.slot(-3), "held to the slots");
		assertEquals(4, ArtRules.slot(99));
	}

	@Test
	void everyArtIsWorthAboutWhatItsSlotIs() {
		for (ArtRules.Art art : ArtRules.ARTS) {
			double worth = ArtRules.power(art);
			double slot = ArtRules.SLOT_POWER[art.slot()];
			assertTrue(Math.abs(worth / slot - 1) <= ArtRules.POWER_SPREAD,
				art.id() + " is worth " + String.format("%.2f", worth) + " against its slot's " + slot);
		}
		for (int slot = 1; slot < 5; slot++) {
			assertTrue(ArtRules.SLOT_POWER[slot] >= ArtRules.SLOT_POWER[slot - 1], "a later stage's art is worth at least the one before");
		}
	}

	@Test
	void noMethodsFiveArtsOutweighAnothers() {
		Map<String, Double> totals = new HashMap<>();
		for (ArtRules.Art art : ArtRules.ARTS) {
			totals.merge(art.method(), ArtRules.power(art), Double::sum);
		}
		double low = totals.values().stream().mapToDouble(d -> d).min().orElseThrow();
		double high = totals.values().stream().mapToDouble(d -> d).max().orElseThrow();
		assertTrue(high / low <= 1.1, "the methods' arts in all within a tenth of each other: " + totals);
	}

	@Test
	void eachMethodHasItsOwnStrength() {
		// Ember hurts most and holds least; Rime holds most; Thunder reaches most foes; Gale and Stone sit between.
		Map<String, double[]> sums = new HashMap<>();
		for (ArtRules.Art art : ArtRules.ARTS) {
			double[] s = sums.computeIfAbsent(art.method(), k -> new double[3]);
			s[0] += art.primary();
			s[1] += art.area();
			s[2] += art.control();
		}
		for (String method : METHODS) {
			if (!method.equals("ember")) {
				assertTrue(sums.get("ember")[2] < sums.get(method)[2], "Ember holds the least: " + method);
			}
			if (!method.equals("rime") && !method.equals("stone")) {
				assertTrue(sums.get("rime")[2] > sums.get(method)[2], "Rime holds more than " + method);
			}
			if (!method.equals("thunder")) {
				assertTrue(sums.get("thunder")[1] >= sums.get(method)[1], "Thunder reaches the most other foes: " + method);
			}
		}
	}

	@Test
	void theArtsSitBesideTheTechniques() {
		ArtRules.Art first = ArtRules.ARTS.getFirst();
		assertTrue(first.cost() <= AuraRules.capacity(AuraRules.GLOW) / 3.0, "a Glow pool plays a First Art three times");
		for (ArtRules.Art art : ArtRules.ARTS) {
			assertTrue(art.cooldown() > AuraRules.SLASH_COOLDOWN, art.id() + " rests longer than the slash");
			if (art.slot() == 4) {
				assertTrue(art.cost() <= AuraRules.DOMINION_COST, art.id() + " costs no more than Dominion");
				assertTrue(art.cooldown() >= 20 * 20, art.id() + " is rare");
				assertTrue(art.cost() < AuraRules.capacity(AuraRules.SOVEREIGN) * StringRules.FINAL_POOL, "a full pool pays for " + art.id());
			} else {
				assertTrue(art.cost() < AuraRules.SLASH_COST, art.id() + " costs less than the slash");
			}
		}
		// A Fourth Art comes after a step: the pair costs about two slashes.
		assertEquals(AuraRules.STEP_COST + ArtRules.SLOT_COST[3], 22, 1e-9);
	}

	// ------------------------------------------------------------------ against players and bosses

	@Test
	void anArtNeverDealsOnePlayerMoreThanItsCap() {
		assertEquals(5.0, ArtRules.pvpLeft(5.0, 0), 1e-9);
		assertEquals(3.0, ArtRules.pvpLeft(5.0, ArtRules.PVP_ART_CAP - 3.0), 1e-9, "what's left of the cap");
		assertEquals(0.0, ArtRules.pvpLeft(5.0, ArtRules.PVP_ART_CAP), 1e-9, "nothing past it");
		assertEquals(0.0, ArtRules.pvpLeft(5.0, 99), 1e-9);
		assertTrue(ArtRules.PVP_ART_CAP < 20, "never a one-shot of a full-health player, armour or no");
	}

	@Test
	void holdsThrowsAndFireAreHeldForPlayersAndBosses() {
		assertEquals(40, ArtRules.hold(40, false, false));
		assertEquals(ArtRules.PVP_HOLD_TICKS, ArtRules.hold(40, true, false), "a player only briefly");
		assertEquals(10, ArtRules.hold(10, true, false));
		assertEquals(0, ArtRules.hold(40, false, true), "a boss never");
		assertEquals(0, ArtRules.hold(-5, false, false));
		assertEquals(1.2, ArtRules.thrown(1.2, false, false), 1e-9);
		assertEquals(ArtRules.PVP_THROW, ArtRules.thrown(1.2, true, false), 1e-9);
		assertEquals(0.0, ArtRules.thrown(1.2, false, true), 1e-9, "a boss is never thrown");
		assertEquals(100, ArtRules.ignite(100, false));
		assertEquals(ArtRules.PVP_IGNITE_TICKS, ArtRules.ignite(100, true));
		assertEquals(0, ArtRules.ignite(-1, true));
		assertTrue(ArtRules.PVP_HOLD_REST > ArtRules.PVP_HOLD_TICKS * 4, "a player held can't be held again at once (no lock)");
	}

	// ------------------------------------------------------------------ shapes and numbers

	@Test
	void conesLinesBlastsAndChains() {
		assertTrue(ArtRules.inCone(0, 3, 0, 1, 4.5, 75));
		assertFalse(ArtRules.inCone(3, 1, 0, 1, 4.5, 75), "too far to the side for a narrow cone");
		assertTrue(ArtRules.inCone(3, 1, 0, 1, 4.5, 160), "inside a wide one");
		double[] aa = ArtRules.alongAcross(1, 3, 0, 2);
		assertEquals(3, aa[0], 1e-9);
		assertEquals(1, aa[1], 1e-9);
		assertEquals(-2, ArtRules.alongAcross(0, -2, 0, 1)[0], 1e-9, "behind the start");
		assertEquals(2.2, ArtRules.falloff(2.2, 1.5, 0, 4.5), 1e-9);
		assertEquals(1.5, ArtRules.falloff(2.2, 1.5, 4.5, 4.5), 1e-9);
		assertEquals(1.5, ArtRules.falloff(2.2, 1.5, 9, 4.5), 1e-9, "held at the edge");
		assertEquals(1.85, ArtRules.falloff(2.2, 1.5, 2.25, 4.5), 1e-9);
		assertEquals(0.8, ArtRules.chain(0.8, 0.85, 0), 1e-9);
		assertEquals(0.8 * 0.85 * 0.85, ArtRules.chain(0.8, 0.85, 2), 1e-9);
		assertEquals(0, ArtRules.chain(0.8, 0.85, -1), 1e-9);
	}

	@Test
	void theSpearRunsLevelUnlessClearlyAimed() {
		assertEquals(0, ArtRules.spearTilt(-0.14), 1e-9, "a glance down at a foe ahead: level, so it doesn't plough into the ground");
		assertEquals(0, ArtRules.spearTilt(0.1), 1e-9);
		assertEquals(0.3, ArtRules.spearTilt(0.3), 1e-9, "aimed up at a flier: with the aim");
		assertEquals(-ArtRules.SPEAR_TILT, ArtRules.spearTilt(-0.9), 1e-9, "never steeper than thirty degrees");
	}

	@Test
	void crustsBuildToAFreezeAndFade() {
		assertEquals(1, ArtRules.crusts(0, Long.MIN_VALUE / 4, 100));
		assertEquals(2, ArtRules.crusts(1, 100, 150));
		assertEquals(ArtRules.CRUSTS_TO_FREEZE, ArtRules.crusts(2, 150, 200), "the third freezes");
		assertEquals(ArtRules.CRUSTS_TO_FREEZE, ArtRules.crusts(5, 150, 200), "never more than that");
		assertEquals(1, ArtRules.crusts(2, 100, 100 + ArtRules.CRUST_MEMORY + 1), "old crusts have melted");
	}

	@Test
	void backdraftThrowsTheCaughtBlowBackHeldToTheBlade() {
		double weapon = 7;
		assertEquals(weapon * ArtRules.BACKDRAFT_FACTOR, ArtRules.backdraft(weapon, 0), 1e-9, "a projectile caught: the blade's share alone");
		assertEquals(weapon * ArtRules.BACKDRAFT_FACTOR + 4, ArtRules.backdraft(weapon, 4), 1e-9);
		assertEquals(weapon * (ArtRules.BACKDRAFT_FACTOR + ArtRules.BACKDRAFT_CAUGHT_MAX), ArtRules.backdraft(weapon, 40), 1e-9,
			"a huge blow comes back no harder than the blade itself");
	}

	// ------------------------------------------------------------------ the framework

	@Test
	void eachMethodsArtsRegisterInTheirSlots() {
		Map<String, List<AuraApi.StringArt>> sets = Map.of(EmberArts.METHOD, EmberArts.arts(), RimeArts.METHOD, RimeArts.arts(),
			ThunderArts.METHOD, ThunderArts.arts(), GaleArts.METHOD, GaleArts.arts(), StoneArts.METHOD, StoneArts.arts());
		assertEquals(Set.copyOf(MethodArts.METHODS), sets.keySet());
		PlaceholderArts.register();
		try {
			for (Map.Entry<String, List<AuraApi.StringArt>> set : sets.entrySet()) {
				List<AuraApi.StringArt> registered = AuraApi.registerArts(set.getKey(), set.getValue());
				assertTrue(AuraApi.hasArts(set.getKey()));
				assertEquals(5, registered.size());
				List<AuraApi.StringArt> arts = AuraApi.arts(set.getKey());
				for (int i = 0; i < 5; i++) {
					AuraApi.StringArt art = arts.get(i);
					AuraApi.ArtSlot slot = AuraApi.ArtSlot.values()[i];
					assertEquals(slot, AuraApi.ArtSlot.of(art).orElseThrow(), art.id());
					assertEquals(slot.string, art.string(), art.id() + " is played on its slot's string");
					ArtRules.Art numbers = ArtRules.art(art.id());
					assertEquals(numbers.cost(), art.cost(), 1e-9, art.id());
					assertEquals(numbers.cooldown(), art.cooldownTicks(), art.id());
					assertEquals(set.getKey(), AuraApi.artMethod(art.id()));
					assertEquals(slot == AuraApi.ArtSlot.FINAL ? AuraApi.FINAL_GATE : AuraApi.ArtCondition.ALWAYS, art.condition(), art.id());
					// Within its method nothing gets in its way, and the other methods' arts and the common ones never do.
					assertTrue(AuraApi.conflicts(art.string(), art.id(), set.getKey()).isEmpty(),
						art.id() + " meets " + AuraApi.conflicts(art.string(), art.id(), set.getKey()));
				}
			}
			assertFalse(AuraApi.hasArts("verdant"), "the methods still to come play the common arts");
			assertEquals(PlaceholderArts.IDS, AuraApi.arts("verdant").stream().map(AuraApi.StringArt::id).toList());
			assertEquals(PlaceholderArts.IDS, AuraApi.arts("example:none").stream().map(AuraApi.StringArt::id).toList());
			assertEquals("", AuraApi.artMethod(PlaceholderArts.FIRST));
			// A common art still warns an art open to everyone.
			assertFalse(AuraApi.conflicts(PlaceholderArts.FIRST_STRING, "apitest:any").isEmpty());
		} finally {
			for (String method : sets.keySet()) {
				for (AuraApi.StringArt art : AuraApi.arts(method)) {
					AuraApi.unregisterString(art.id());
				}
			}
		}
		assertFalse(AuraApi.hasArts(EmberArts.METHOD), "taking out a method's arts takes out its set");
	}

	@Test
	void aMethodsArtsAreReplacedWholeAndTheFinalGateCanBeChanged() {
		try {
			AuraApi.registerArts("apitest:method", List.of(AuraApi.ArtSlot.FIRST.art("apitest:one", 5, 40, (p, c) -> true),
				AuraApi.ArtSlot.FINAL.art("apitest:end", 30, 400, (p, c) -> true)));
			assertEquals(List.of("apitest:one", "apitest:end"), AuraApi.arts("apitest:method").stream().map(AuraApi.StringArt::id).toList());
			assertSame(AuraApi.FINAL_GATE, AuraApi.string("apitest:end").orElseThrow().condition());
			AuraApi.registerArts("apitest:method", List.of(AuraApi.ArtSlot.SECOND.art("apitest:two", 5, 40, (p, c) -> true)));
			assertTrue(AuraApi.string("apitest:one").isEmpty(), "the old set goes");
			assertEquals(List.of("apitest:two"), AuraApi.arts("apitest:method").stream().map(AuraApi.StringArt::id).toList());
			assertThrows(IllegalArgumentException.class, () -> AuraApi.registerArts(" ", List.of()));
			// The gate every Final Art waits on: a full pool, until something changes it, in one place.
			assertEquals("message.wildercord.aura.art.full_pool", AuraApi.FINAL_GATE.hintKey());
			AuraApi.gateFinalArts(AuraApi.ArtCondition.of(p -> true, "apitest.gate"));
			assertEquals("apitest.gate", AuraApi.FINAL_GATE.hintKey());
		} finally {
			AuraApi.gateFinalArts(null);
			AuraApi.unregisterString("apitest:two");
			AuraApi.unregisterString("apitest:one");
			AuraApi.unregisterString("apitest:end");
		}
		assertEquals("message.wildercord.aura.art.full_pool", AuraApi.FINAL_GATE.hintKey(), "null puts the full pool back");
		assertFalse(AuraApi.hasArts("apitest:method"));
	}

	@Test
	void everyArtIsNamedDescribedAndVoiced() throws IOException {
		JsonObject lang = read("/assets/wildercord/lang/en_us.json");
		for (ArtRules.Art art : ArtRules.ARTS) {
			String key = "aura.wildercord.art." + art.id();
			assertTrue(lang.has(key), key);
			assertTrue(lang.has(key + ".desc"), key + ".desc");
		}
		for (String key : List.of("message.wildercord.aura.art.blocked", "toast.wildercord.aura.art", "screen.wildercord.grimoire.arts",
				"screen.wildercord.grimoire.art", "screen.wildercord.grimoire.art_unknown", "screen.wildercord.aura.art_other")) {
			assertTrue(lang.has(key), key);
		}
		JsonObject kit = read("/assets/wildercord/kit_sounds.json").getAsJsonObject("events");
		Set<String> voices = new HashSet<>();
		for (String sound : MethodArts.SOUNDS) {
			assertTrue(kit.has(sound), sound + " isn't in the feel kit: run python tools/feel/build.py --only aura");
			assertTrue(voices.add(sound), sound + " twice");
		}
		for (ArtRules.Art art : ArtRules.ARTS) {
			assertTrue(voices.contains("aura_art_" + art.id()), art.id() + " has a voice of its own");
		}
	}

	@Test
	void anArtsGrimoireEntryIsNamedForIt() {
		assertEquals("aura:art_kindling_draw", ArtRules.grimoireKey("kindling_draw"));
		assertEquals("aura:art_example.art", ArtRules.grimoireKey("example:art"));
		assertTrue(ArtRules.grimoireKey("x").startsWith(ArtRules.GRIMOIRE_ART));
		assertEquals(60, dev.wildercord.spell.Feats.reward(ArtRules.grimoireKey("kindling_draw")), "a smaller milestone than a breakthrough");
	}

	private static JsonObject read(String path) throws IOException {
		try (InputStream in = ArtRulesTest.class.getResourceAsStream(path)) {
			assertNotNull(in, path);
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}
}
