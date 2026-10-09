package dev.wildercord.aura;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.CrimsonArts;
import dev.wildercord.aura.arts.DuneArts;
import dev.wildercord.aura.arts.IronArts;
import dev.wildercord.aura.arts.TideArts;
import dev.wildercord.aura.arts.EmberArts;
import dev.wildercord.aura.arts.GaleArts;
import dev.wildercord.aura.arts.HollowArts;
import dev.wildercord.aura.arts.HourglassArts;
import dev.wildercord.aura.arts.MethodArts;
import dev.wildercord.aura.arts.RimeArts;
import dev.wildercord.aura.arts.StarlitArts;
import dev.wildercord.aura.arts.StoneArts;
import dev.wildercord.aura.arts.ThunderArts;
import dev.wildercord.aura.arts.VerdantArts;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The breathing methods' arts, the pure parts: each art's price and rest, the balance between all fifty (by slot, by method, against
 * the techniques, and each method's own strength), the rules an art keeps against players and bosses (damage, holds, throws, drags,
 * silence, mending), the shapes they reach, and the framework that registers them ({@link AuraApi#registerArts}: slots, whose each
 * art is, the common arts stepping aside, conflicts, the Final Art's gate), with every art's name, description and voice really there.
 */
class ArtRulesTest {
	private static final List<String> METHODS = List.of("ember", "rime", "thunder", "gale", "stone", "verdant", "hollow", "starlit", "hourglass",
		"crimson", "tide", "iron", "dune");
	/** Echo, Dawn and Venom (methods-b pack): registered beside the ten, held to the same rules. */
	private static final List<String> PACK = List.of("echo", "dawn", "venom");
	private static final List<String> ALL = java.util.stream.Stream.concat(METHODS.stream(), PACK.stream()).toList();

	// ------------------------------------------------------------------ the arts, priced

	@Test
	void everyMethodHasFiveArtsOneASlot() {
		Set<String> ids = new HashSet<>();
		for (String method : ALL) {
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
		assertEquals(80, ArtRules.ARTS.size(), "thirteen methods and the methods-b pack's three, five arts each");
		assertEquals(List.copyOf(MethodArts.METHODS), METHODS, "every built-in method has its arts");
		assertEquals(dev.wildercord.aura.arts.MethodsBArts.METHODS, PACK, "and the pack's methods theirs");
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

	/** Each method's model sums: primary, area, control, reach, mend, aura, toll. */
	private static Map<String, double[]> sums() {
		Map<String, double[]> sums = new LinkedHashMap<>();
		for (ArtRules.Art art : ArtRules.ARTS) {
			double[] s = sums.computeIfAbsent(art.method(), k -> new double[7]);
			s[0] += art.primary();
			s[1] += art.area();
			s[2] += art.control();
			s[3] += art.reach();
			s[4] += art.mend();
			s[5] += art.aura();
			s[6] += art.toll();
		}
		return sums;
	}

	/** Whether {@code method} has the most of measure {@code i} of all ten (strictly), naming the runner-up when not. */
	private static void leads(Map<String, double[]> sums, String method, int i, String what) {
		for (Map.Entry<String, double[]> e : sums.entrySet()) {
			if (!e.getKey().equals(method)) {
				assertTrue(sums.get(method)[i] > e.getValue()[i], method + " " + what + " (" + String.format("%.2f", sums.get(method)[i]) + "), more than "
					+ e.getKey() + " (" + String.format("%.2f", e.getValue()[i]) + ")");
			}
		}
	}

	@Test
	void eachMethodLeadsInItsOwnThing() {
		// In the model's numbers: Ember hurts most, Rime holds most, Thunder reaches the most other foes, Gale reaches furthest,
		// Verdant mends most, Starlit gives the most aura back; Crimson hurts most after Ember, mends most after Verdant, and alone pays
		// in health; Hourglass holds most after Rime.
		Map<String, double[]> sums = sums();
		leads(sums, "ember", 0, "hurts the most");
		leads(sums, "rime", 2, "holds the most");
		leads(sums, "thunder", 1, "reaches the most other foes");
		leads(sums, "gale", 3, "reaches the furthest");
		leads(sums, "verdant", 4, "mends the most");
		leads(sums, "starlit", 5, "gives the most aura back");
		for (String method : ALL) {
			if (!method.equals("ember") && !method.equals("crimson")) {
				assertTrue(sums.get("crimson")[0] > sums.get(method)[0], "Crimson hurts more than everyone but Ember: " + method);
			}
			if (!method.equals("verdant") && !method.equals("crimson")) {
				assertTrue(sums.get("crimson")[4] > sums.get(method)[4], "Crimson mends (drinks) more than everyone but Verdant: " + method);
			}
			if (!method.equals("rime") && !method.equals("hourglass")) {
				assertTrue(sums.get("hourglass")[2] > sums.get(method)[2], "Hourglass holds more than everyone but Rime: " + method);
			}
			if (!method.equals("crimson")) {
				assertEquals(0, sums.get(method)[6], 1e-9, "only Crimson pays in health: " + method);
			}
			if (!method.equals("starlit")) {
				assertEquals(0, sums.get(method)[5], 1e-9, "only Starlit gives aura back: " + method);
			}
		}
		assertTrue(sums.get("crimson")[6] > 0, "Crimson Moon's price is in the model");
	}

	/** What each method's arts do that's its own: every one of its arts carries one of these, and another method borrows one at most once. */
	private static final Map<String, Set<ArtRules.Kind>> OWN = Map.ofEntries(
		Map.entry("ember", EnumSet.of(ArtRules.Kind.FIRE)),
		Map.entry("rime", EnumSet.of(ArtRules.Kind.FROST, ArtRules.Kind.FREEZE)),
		Map.entry("thunder", EnumSet.of(ArtRules.Kind.SHOCK, ArtRules.Kind.CHAIN)),
		Map.entry("gale", EnumSet.of(ArtRules.Kind.WIND)),
		Map.entry("stone", EnumSet.of(ArtRules.Kind.QUAKE, ArtRules.Kind.HARDEN)),
		Map.entry("verdant", EnumSet.of(ArtRules.Kind.ROOT, ArtRules.Kind.MEND)),
		Map.entry("hollow", EnumSet.of(ArtRules.Kind.PULL, ArtRules.Kind.SILENCE)),
		Map.entry("starlit", EnumSet.of(ArtRules.Kind.STAR, ArtRules.Kind.AURA)),
		Map.entry("hourglass", EnumSet.of(ArtRules.Kind.ECHO, ArtRules.Kind.REWIND, ArtRules.Kind.STILL, ArtRules.Kind.DRAG)),
		Map.entry("crimson", EnumSet.of(ArtRules.Kind.BLEED, ArtRules.Kind.DRINK, ArtRules.Kind.TOLL, ArtRules.Kind.FRENZY)),
		// ---- methods-a pack
		Map.entry("tide", EnumSet.of(ArtRules.Kind.CURRENT, ArtRules.Kind.SOAK)),
		Map.entry("iron", EnumSet.of(ArtRules.Kind.SUNDER, ArtRules.Kind.BULWARK)),
		Map.entry("dune", EnumSet.of(ArtRules.Kind.BLIND, ArtRules.Kind.SINK)),
		// ---- methods-b pack
		Map.entry("echo", EnumSet.of(ArtRules.Kind.RESOUND)),
		Map.entry("dawn", EnumSet.of(ArtRules.Kind.RADIANT, ArtRules.Kind.DAZZLE)),
		Map.entry("venom", EnumSet.of(ArtRules.Kind.TOXIN, ArtRules.Kind.WEAKEN)));

	@Test
	void eachMethodsArtsDoWhatsItsOwn() {
		assertEquals(Set.copyOf(ALL), OWN.keySet());
		for (ArtRules.Art art : ArtRules.ARTS) {
			Set<ArtRules.Kind> own = OWN.get(art.method());
			assertTrue(art.kinds().stream().anyMatch(own::contains), art.id() + " does something only " + art.method() + " does (" + art.kinds() + ")");
		}
		for (String method : ALL) {
			for (String other : ALL) {
				if (other.equals(method)) {
					continue;
				}
				long borrowed = ArtRules.of(other).stream().filter(a -> a.kinds().stream().anyMatch(OWN.get(method)::contains)).count();
				assertTrue(borrowed <= 1, other + " borrows " + method + "'s own " + OWN.get(method) + " in " + borrowed + " arts");
			}
		}
		// A few that must never be borrowed at all: fire, silence, a health price, aura given back, time held still.
		for (ArtRules.Kind sole : List.of(ArtRules.Kind.FIRE, ArtRules.Kind.SILENCE, ArtRules.Kind.TOLL, ArtRules.Kind.AURA, ArtRules.Kind.STILL,
				ArtRules.Kind.ROOT, ArtRules.Kind.FREEZE)) {
			Set<String> who = new HashSet<>();
			for (ArtRules.Art art : ArtRules.ARTS) {
				if (art.is(sole)) {
					who.add(art.method());
				}
			}
			assertEquals(1, who.size(), sole + " belongs to one method alone: " + who);
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

	// ------------------------------------------------------------------ mending, drinking and their price

	@Test
	void artsMendOneBodyOnlySoMuchSoFast() {
		assertEquals(ArtRules.MEND_CAP, ArtRules.mendRoom(0, 0), 1e-9, "an unmended body takes a whole bucket");
		assertEquals(0, ArtRules.mendRoom(ArtRules.MEND_CAP, 0), 1e-9, "a full bucket takes no more");
		assertEquals(ArtRules.MEND_CAP / 2, ArtRules.mendRoom(ArtRules.MEND_CAP, ArtRules.MEND_WINDOW / 2), 1e-9, "half drained, half again");
		assertEquals(ArtRules.MEND_CAP, ArtRules.mendRoom(ArtRules.MEND_CAP, ArtRules.MEND_WINDOW * 3), 1e-9, "drained away in time");
		assertEquals(4, ArtRules.mendLevel(4, -50), 1e-9, "time never runs backward into a fuller bucket");
		// Never outpacing a fight: at most a health a second for long, from every art together (Verdant's passive is its own).
		double steady = ArtRules.MEND_CAP / ArtRules.MEND_WINDOW * 20;
		assertTrue(steady <= 1.0 + 1e-9, "arts mend a body at most a health a second once the bucket is full (" + steady + ")");
		assertTrue(ArtRules.MEND_CAP <= 10.0, "no more than five hearts at once from arts");
		// Every mending an art does fits the bucket, even the grove's whole time.
		assertTrue(ArtRules.BLOSSOM_MEND + ArtRules.BLOSSOM_PULSE * ArtRules.BLOSSOM_TICKS / 20.0 <= ArtRules.MEND_CAP);
		assertTrue(ArtRules.GROVE_MEND * ArtRules.GROVE_TICKS / 20.0 <= ArtRules.MEND_CAP);
		assertTrue(ArtRules.ROOTED_MAX <= ArtRules.MEND_CAP && ArtRules.MOON_DRINK_MAX <= ArtRules.MEND_CAP && ArtRules.RAIN_DRINK_MAX <= ArtRules.MEND_CAP);
	}

	@Test
	void rootedParryMendsByWhatItCaught() {
		assertEquals(ArtRules.ROOTED_MIN, ArtRules.rootedMend(0), 1e-9, "a projectile caught still mends a little");
		assertEquals(6 * ArtRules.ROOTED_SHARE, ArtRules.rootedMend(6), 1e-9);
		assertEquals(ArtRules.ROOTED_MAX, ArtRules.rootedMend(40), 1e-9, "a huge blow mends no more than six");
	}

	@Test
	void aDrinkIsAShareHeldToItsCap() {
		assertEquals(3.0, ArtRules.drink(10, 0.3, 4), 1e-9);
		assertEquals(4.0, ArtRules.drink(100, 0.3, 4), 1e-9, "held to the cap");
		assertEquals(0, ArtRules.drink(-5, 0.3, 4), 1e-9);
		assertEquals(0, ArtRules.drink(5, 0.3, -1), 1e-9);
	}

	@Test
	void crimsonMoonNeverKillsItsSwordsman() {
		// A quarter of a full twenty, at full health.
		assertEquals(5.0, ArtRules.moonToll(20, 20), 1e-9);
		assertEquals(5.0, ArtRules.moonToll(9, 20), 1e-9, "the price is of the greatest health, so it costs the same hurt or whole");
		assertEquals(4.0 - ArtRules.MOON_FLOOR, ArtRules.moonToll(4, 20), 1e-9, "never past the floor");
		assertEquals(0, ArtRules.moonToll(ArtRules.MOON_FLOOR, 20), 1e-9, "at a heart it costs nothing more");
		assertEquals(0, ArtRules.moonToll(1, 20), 1e-9);
		for (double health = 0.5; health <= 40; health += 0.5) {
			for (double max : new double[] {20, 40}) {
				double left = Math.min(health, max) - ArtRules.moonToll(Math.min(health, max), max);
				assertTrue(left >= Math.min(Math.min(health, max), ArtRules.MOON_FLOOR) - 1e-9, "never below a heart (or where it began): " + health + " of " + max);
				assertTrue(left > 0, "never their life");
			}
		}
	}

	@Test
	void woundsFrenziesAndStoresAreHeld() {
		assertEquals(7, ArtRules.sanguineWound(7, 12), 1e-9, "Sanguine Parry's wound is no more than a weapon");
		assertEquals(3, ArtRules.sanguineWound(7, 3), 1e-9);
		assertEquals(0, ArtRules.frenzy(0), 1e-9);
		assertEquals(ArtRules.FRENZY_SPEED * ArtRules.FRENZY_STACKS, ArtRules.frenzy(99), 1e-9, "a frenzy has its most");
		assertTrue(ArtRules.frenzy(99) <= 0.2 + 1e-9, "never more than a fifth faster");
		assertEquals(0.5, ArtRules.stored(0, 1, 5), 1e-9);
		assertEquals(2.0, ArtRules.stored(1.8, 4, 2), 1e-9, "Thousand Moments stores only so much on one foe");
		assertEquals(ArtRules.NOVA_AURA_MAX, ArtRules.novaAura(99), 1e-9);
		assertTrue(ArtRules.NOVA_AURA_MAX <= ArtRules.SLOT_COST[4] / 2, "Nova gives back no more than half its price");
		assertEquals(0, ArtRules.novaAura(0), 1e-9);
	}

	@Test
	void silenceAndDragAreHeldForPlayersAndBosses() {
		assertEquals(60, ArtRules.silence(60, false, false));
		assertEquals(ArtRules.SILENCE_PLAYER_TICKS, ArtRules.silence(60, true, false), "a player only briefly");
		assertEquals(0, ArtRules.silence(60, false, true), "a boss is never silenced");
		assertTrue(ArtRules.SILENCE_PLAYER_TICKS <= dev.wildercord.cast.CastLock.PLAYER_LOCK_CAP, "no longer than any spell's seal on a player");
		assertTrue(ArtRules.SILENCE_REST > ArtRules.SILENCE_PLAYER_TICKS * 3, "a player silenced can't be kept silent");
		assertEquals(0.3, ArtRules.dragged(0.3, false, false), 1e-9);
		assertEquals(ArtRules.PVP_DRAG, ArtRules.dragged(0.3, true, false), 1e-9);
		assertEquals(0, ArtRules.dragged(0.3, false, true), 1e-9, "a boss is never dragged");
		assertTrue(ArtRules.PVP_DRAG < 0.13, "slower than a sprint: a player can always run out of a pull");
		// Every art that holds a player in place does it through the one hold, held to its cap.
		for (int ticks : new int[] {ArtRules.THORN_ROOT, ArtRules.ROOTED_ROOT, ArtRules.GROVE_ROOT, ArtRules.STOPPED_HOLD, ArtRules.THOUSAND_HOLD,
				ArtRules.NULL_HOLD, ArtRules.SANGUINE_HOLD}) {
			assertTrue(ArtRules.hold(ticks, true, false) <= ArtRules.PVP_HOLD_TICKS);
			assertEquals(0, ArtRules.hold(ticks, false, true));
		}
	}

	// ------------------------------------------------------------------ the framework

	@Test
	void eachMethodsArtsRegisterInTheirSlots() {
		Map<String, List<AuraApi.StringArt>> sets = new LinkedHashMap<>();
		sets.put(EmberArts.METHOD, EmberArts.arts());
		sets.put(RimeArts.METHOD, RimeArts.arts());
		sets.put(ThunderArts.METHOD, ThunderArts.arts());
		sets.put(GaleArts.METHOD, GaleArts.arts());
		sets.put(StoneArts.METHOD, StoneArts.arts());
		sets.put(VerdantArts.METHOD, VerdantArts.arts());
		sets.put(HollowArts.METHOD, HollowArts.arts());
		sets.put(StarlitArts.METHOD, StarlitArts.arts());
		sets.put(HourglassArts.METHOD, HourglassArts.arts());
		sets.put(CrimsonArts.METHOD, CrimsonArts.arts());
		// ---- methods-a pack
		sets.put(TideArts.METHOD, TideArts.arts());
		sets.put(IronArts.METHOD, IronArts.arts());
		sets.put(DuneArts.METHOD, DuneArts.arts());
		assertEquals(Set.copyOf(MethodArts.METHODS), sets.keySet());
		// ---- methods-b pack
		sets.put(dev.wildercord.aura.arts.EchoArts.METHOD, dev.wildercord.aura.arts.EchoArts.arts());
		sets.put(dev.wildercord.aura.arts.DawnArts.METHOD, dev.wildercord.aura.arts.DawnArts.arts());
		sets.put(dev.wildercord.aura.arts.VenomArts.METHOD, dev.wildercord.aura.arts.VenomArts.arts());
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
			assertFalse(AuraApi.hasArts("example:none"), "a method without arts of its own (an add-on's) plays the common arts");
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
		for (String sound : java.util.stream.Stream.concat(MethodArts.SOUNDS.stream(), dev.wildercord.aura.arts.MethodsBArts.SOUNDS.stream()).toList()) {
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
