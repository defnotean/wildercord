package dev.wildercord.aura;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The bonded blade, the pure part ({@link BladeRules}, {@link BladeBond}): its tiers and what each waits on, how a passed blade sleeps, the
 * ceremonies' timing, what every deed is worth and how fast a blade grows, its thirteen traits (each drawn from its own habit, leaned by
 * method and Way, weighed for balance and held for PvP), the names it suggests (original, clean, short, its own), its story's limits, and
 * the component's codecs.
 */
class BladeRulesTest {
	// ------------------------------------------------------------------ tiers

	@Test
	void tiersAskResonanceAndAStage() {
		assertEquals(AuraRules.EDGE, BladeRules.FROM, "a blade is bonded from Edge");
		assertEquals(List.of("none", "bonded", "named", "awakened", "soulforged"),
			List.of(BladeRules.tierId(0), BladeRules.tierId(1), BladeRules.tierId(2), BladeRules.tierId(3), BladeRules.tierId(4)));
		assertEquals(300, BladeRules.threshold(BladeRules.NAMED), 1e-9);
		assertEquals(1200, BladeRules.threshold(BladeRules.AWAKENED), 1e-9);
		assertEquals(3600, BladeRules.threshold(BladeRules.SOULFORGED), 1e-9);
		// Earned by resonance (and a boss for Soulforged), whatever the stage.
		assertEquals(BladeRules.BONDED, BladeRules.earned(0, 0));
		assertEquals(BladeRules.BONDED, BladeRules.earned(299.9, 0));
		assertEquals(BladeRules.NAMED, BladeRules.earned(300, 0));
		assertEquals(BladeRules.AWAKENED, BladeRules.earned(1200, 0));
		assertEquals(BladeRules.AWAKENED, BladeRules.earned(9999, 0), "Soulforged asks a boss felled by it");
		assertEquals(BladeRules.SOULFORGED, BladeRules.earned(3600, 1));
		// Each tier waits on its stage, as a breakthrough waits on its trial.
		assertEquals(BladeRules.NAMED, BladeRules.tier(1200, 0, AuraRules.EDGE), "Awakened waits on Form");
		assertEquals(BladeRules.AWAKENED, BladeRules.tier(1200, 0, AuraRules.FORM));
		assertEquals(BladeRules.AWAKENED, BladeRules.tier(5000, 3, AuraRules.FORM), "Soulforged waits on Sovereign");
		assertEquals(BladeRules.SOULFORGED, BladeRules.tier(5000, 3, AuraRules.SOVEREIGN));
		assertEquals(BladeRules.BONDED, BladeRules.tier(0, 0, AuraRules.GLOW), "a bonded blade is Bonded at least");
	}

	@Test
	void aPassedBladeSleepsUntilItsDiscipleGrowsIntoIt() {
		assertEquals(0, BladeRules.allowed(AuraRules.GLOW));
		assertEquals(0, BladeRules.allowed(AuraRules.FLOW));
		assertEquals(BladeRules.NAMED, BladeRules.allowed(AuraRules.EDGE));
		assertEquals(BladeRules.AWAKENED, BladeRules.allowed(AuraRules.FORM));
		assertEquals(BladeRules.SOULFORGED, BladeRules.allowed(AuraRules.SOVEREIGN));
		assertEquals(0, BladeRules.effective(BladeRules.SOULFORGED, AuraRules.FLOW), "a Flow disciple's Soulforged blade sleeps");
		assertEquals(BladeRules.NAMED, BladeRules.effective(BladeRules.SOULFORGED, AuraRules.EDGE), "at Edge it gives what Edge allows");
		assertEquals(BladeRules.AWAKENED, BladeRules.effective(BladeRules.AWAKENED, AuraRules.SOVEREIGN), "never more than it has");
		assertEquals(0, BladeRules.traitStrength(BladeRules.effective(BladeRules.SOULFORGED, AuraRules.EDGE)), "no trait before Awakened");
	}

	@Test
	void whatTheNextTierWaitsOn() {
		assertEquals(BladeRules.Waiting.RESONANCE, BladeRules.waiting(BladeRules.BONDED, 100, 0, AuraRules.EDGE));
		assertEquals(BladeRules.Waiting.NOTHING, BladeRules.waiting(BladeRules.BONDED, 300, 0, AuraRules.EDGE));
		assertEquals(BladeRules.Waiting.STAGE, BladeRules.waiting(BladeRules.NAMED, 1300, 0, AuraRules.EDGE));
		assertEquals(BladeRules.Waiting.BOSS, BladeRules.waiting(BladeRules.AWAKENED, 4000, 0, AuraRules.SOVEREIGN));
		assertEquals(BladeRules.Waiting.TOP, BladeRules.waiting(BladeRules.SOULFORGED, 4000, 2, AuraRules.SOVEREIGN));
		assertEquals(0.5, BladeRules.progress(150, BladeRules.BONDED), 1e-9);
		assertEquals(0.5, BladeRules.progress(750, BladeRules.NAMED), 1e-9);
		assertEquals(1.0, BladeRules.progress(99999, BladeRules.SOULFORGED), 1e-9);
		assertEquals(1.0, BladeRules.progress(5000, BladeRules.AWAKENED), 1e-9, "full while it waits on a stage or a boss");
	}

	@Test
	void theTiersGiftsAreSmallAndGrow() {
		assertEquals(1.0, BladeRules.hitGain(BladeRules.BONDED), 1e-9);
		assertEquals(1.1, BladeRules.hitGain(BladeRules.NAMED), 1e-9);
		assertEquals(1.1, BladeRules.hitGain(BladeRules.AWAKENED), 1e-9);
		assertEquals(1.2, BladeRules.hitGain(BladeRules.SOULFORGED), 1e-9);
		assertEquals(1.0, BladeRules.traitStrength(BladeRules.AWAKENED), 1e-9);
		assertEquals(1.5, BladeRules.traitStrength(BladeRules.SOULFORGED), 1e-9);
		assertEquals(0.775, BladeRules.scaled(0.85, 1.5), 1e-9, "a 15% discount half again as strong");
		assertEquals(1.18, BladeRules.scaled(1.12, 1.5), 1e-9);
		assertEquals(1.0, BladeRules.scaled(1.15, 0), 1e-9);
	}

	// ------------------------------------------------------------------ the ceremonies

	@Test
	void theBondCeremonyRunsInThreeParts() {
		assertEquals(200, BladeRules.BOND_TICKS, "ten seconds");
		assertEquals(0, BladeRules.phase(0));
		assertEquals(0, BladeRules.phase(BladeRules.KINDLE_END - 1));
		assertEquals(1, BladeRules.phase(BladeRules.KINDLE_END));
		assertEquals(2, BladeRules.phase(BladeRules.JOIN_END));
		assertEquals(3, BladeRules.phase(BladeRules.BOND_TICKS));
		assertTrue(BladeRules.SETTLE_BEFORE > 0 && BladeRules.SETTLE_BEFORE < 40, "a quick crouch never starts it, a real stance soon does");
		assertTrue(BladeRules.PASS_TICKS >= 100 && BladeRules.PASS_TICKS <= BladeRules.BOND_TICKS);
		assertTrue(BladeRules.PASS_REACH >= 2 && BladeRules.PASS_REACH <= 4, "a disciple kneels close");
		assertTrue(BladeRules.AFTER_CEREMONY >= 40, "the crossroads gives a ceremony room");
	}

	// ------------------------------------------------------------------ resonance

	@Test
	void deedsAreWorthWhatTheyAsk() {
		assertEquals(0, BladeRules.kill(0, 1, false), 1e-9, "nothing for no foe");
		assertEquals(1.0, BladeRules.kill(1.0, 1.0, false), 1e-9);
		assertEquals(0.5, BladeRules.kill(1.0, 0.5, false), 1e-9, "a farm's tenth zombie is worth less");
		assertEquals(2.0 + BladeRules.BOSS_KILL, BladeRules.kill(2.0, 1.0, true), 1e-9, "a boss is a deed");
		assertEquals(0.6, BladeRules.art(0, 1), 1e-9);
		assertEquals(3.0, BladeRules.art(4, 1), 1e-9, "the Final Art most");
		assertEquals(BladeRules.ART_MORE, BladeRules.art(2, 2), 1e-9);
		assertEquals(BladeRules.ART_MORE, BladeRules.art(2, 1 + BladeRules.ART_MORE_FOES), 1e-9);
		assertEquals(0, BladeRules.art(2, 2 + BladeRules.ART_MORE_FOES), 1e-9, "a crowd only adds so much");
		assertEquals(0, BladeRules.breakthrough(AuraRules.EDGE), 1e-9, "bonded from Edge: no Edge breakthrough to carry");
		assertTrue(BladeRules.breakthrough(AuraRules.SOVEREIGN) > BladeRules.breakthrough(AuraRules.FORM));
		// One foe only gives so much, a little more a stronger one, much more a boss; a finisher fits inside a foe's share.
		assertEquals(5.0, BladeRules.foeCap(0.5, false), 1e-9);
		assertEquals(8.0, BladeRules.foeCap(2.0, false), 1e-9);
		assertTrue(BladeRules.foeCap(2.0, true) > 4 * BladeRules.foeCap(2.0, false));
		assertTrue(BladeRules.FINISHER + BladeRules.art(3, 1) < BladeRules.foeCap(0.5, false));
		assertEquals(24000, BladeRules.PLAYER_KILL_REST, "one player's fall counts once a day");
	}

	/**
	 * How fast a blade grows for a swordsman fighting steadily with it (an hour of real fighting, by a model of what such an hour holds), so
	 * Named comes in the first evening, Awakened over a week of play, Soulforged as a long road's end.
	 */
	@Test
	void aBladeGrowsAtTheRightPace() {
		// An hour: a hundred worthy kills (worth 0.8, half of them a little stale), 120 arts landing on their first foe (by the slots' mix) and
		// 60 more foes, 20 finishers, 40 techniques, 25 stances broken, 15 perfect guards, 3 awakenings; capped per foe as the server caps.
		double hour = 100 * BladeRules.kill(0.8, 0.75, false)
			+ 40 * BladeRules.art(0, 1) + 35 * BladeRules.art(1, 1) + 25 * BladeRules.art(2, 1) + 18 * BladeRules.art(3, 1) + 2 * BladeRules.art(4, 1)
			+ 60 * BladeRules.ART_MORE + 20 * BladeRules.FINISHER + 40 * BladeRules.art(1, 1) + 25 * BladeRules.BROKEN + 15 * BladeRules.GUARD
			+ 3 * BladeRules.AWAKENING;
		assertTrue(hour > 180 && hour < 320, "an hour's resonance: " + hour);
		double named = BladeRules.threshold(BladeRules.NAMED) / hour;
		double awakened = (BladeRules.threshold(BladeRules.AWAKENED) - BladeRules.FORM_BREAKTHROUGH) / hour;
		double soulforged = (BladeRules.threshold(BladeRules.SOULFORGED) - BladeRules.FORM_BREAKTHROUGH - BladeRules.SOVEREIGN_BREAKTHROUGH
			- 2 * (BladeRules.BOSS_KILL + 2)) / hour;
		assertTrue(named > 0.8 && named < 2.5, "Named after " + named + " hours");
		assertTrue(awakened > 3 && awakened < 8, "Awakened after " + awakened + " hours");
		assertTrue(soulforged > 10 && soulforged < 22, "Soulforged after " + soulforged + " hours");
	}

	// ------------------------------------------------------------------ the history

	@Test
	void itRemembersTheArtsItPlaysMost() {
		Map<String, Integer> arts = new LinkedHashMap<>();
		for (int i = 0; i < BladeRules.MAX_ARTS + 5; i++) {
			for (int k = 0; k <= i; k++) {
				arts = BladeRules.countArt(arts, "art_" + i);
			}
		}
		assertEquals(BladeRules.MAX_ARTS, arts.size(), "it remembers the most played only");
		assertFalse(arts.containsKey("art_0"), "the least played went first");
		arts = BladeRules.countArt(arts, "brand_new");
		assertTrue(arts.containsKey("brand_new"), "the one just played is never the one let go");
		assertEquals(BladeRules.MAX_ARTS, arts.size());
		assertEquals("art_16", BladeRules.favourite(arts));
		assertEquals("", BladeRules.favourite(Map.of()));
		assertEquals("a", BladeRules.favourite(Map.of("b", 3, "a", 3)), "a tie goes by id, the same everywhere");
	}

	@Test
	void itsStoryKeepsItsLimits() {
		List<BladeBond.Deed> deeds = new ArrayList<>();
		for (int i = 0; i < BladeRules.MAX_DEEDS + 6; i++) {
			deeds.add(new BladeBond.Deed(BladeRules.DEED_BOSS, "entity.minecraft.wither", i));
		}
		BladeBond.History h = BladeBond.History.EMPTY.plus(Map.of(BladeRules.KILLS, 3), List.of("ember_sunfall"), deeds);
		assertEquals(BladeRules.MAX_DEEDS, h.deeds().size(), "the latest deeds");
		assertEquals(BladeRules.MAX_DEEDS + 5, h.deeds().getLast().day(), "the newest kept");
		assertEquals(3, h.count(BladeRules.KILLS));
		assertEquals(1, h.arts().get("ember_sunfall"));
		h = h.plus(Map.of(BladeRules.KILLS, 2), List.of(), List.of());
		assertEquals(5, h.count(BladeRules.KILLS), "counts add up");
		List<String> lineage = new ArrayList<>();
		for (int i = 0; i < BladeRules.MAX_LINEAGE + 3; i++) {
			lineage.add("Master" + i);
		}
		BladeBond.Who who = new BladeBond.Who(UUID.randomUUID(), UUID.randomUUID(), "Disciple", lineage);
		assertEquals(BladeRules.MAX_LINEAGE, who.lineage().size());
		assertEquals("Master" + (BladeRules.MAX_LINEAGE + 2), who.lineage().getLast(), "the latest master kept");
		assertTrue(BladeRules.COUNTS.containsAll(BladeRules.SHOWN_COUNTS));
		assertEquals(new HashSet<>(BladeRules.COUNTS).size(), BladeRules.COUNTS.size());
	}

	// ------------------------------------------------------------------ traits

	private static BladeRules.History history(Map<String, Integer> counts, Map<String, Integer> arts) {
		return new BladeRules.History(counts, arts, "", "");
	}

	/** A history that shows {@code trait}'s habit strongly and nothing else's. */
	private static BladeRules.History habit(String trait) {
		return switch (trait) {
			case BladeRules.WELL_WORN -> history(Map.of(BladeRules.ARTS, 140, BladeRules.KILLS, 100), Map.of("ember_sunfall", 120, "ember_backdraft", 20));
			case BladeRules.CLOSING_STROKE -> history(Map.of(BladeRules.FINISHERS, 80, BladeRules.KILLS, 100), Map.of());
			case BladeRules.SUNDERING_STEEL -> history(Map.of(BladeRules.BROKEN_STANCES, 100, BladeRules.KILLS, 100), Map.of());
			case BladeRules.RIPOSTE -> history(Map.of(BladeRules.GUARDS, 80, BladeRules.KILLS, 100), Map.of());
			case BladeRules.WIND_STEP -> history(Map.of(BladeRules.STEPS, 150, BladeRules.KILLS, 100), Map.of());
			case BladeRules.LONG_CRESCENT -> history(Map.of(BladeRules.SLASHES, 160, BladeRules.KILLS, 100), Map.of());
			case BladeRules.INKBOUND -> history(Map.of(BladeRules.TECHNIQUES, 160, BladeRules.KILLS, 100), Map.of());
			case BladeRules.SECOND_BLAZE -> history(Map.of(BladeRules.AWAKENINGS, 25, BladeRules.KILLS, 100), Map.of());
			case BladeRules.MOUNTAINFELLER -> history(Map.of(BladeRules.BOSSES, 5, BladeRules.STRONG, 40, BladeRules.KILLS, 100), Map.of());
			case BladeRules.GRAVEWARDEN -> history(Map.of(BladeRules.UNDEAD, 200, BladeRules.KILLS, 300), Map.of());
			case BladeRules.LAST_LIGHT -> history(Map.of(BladeRules.LOW, 50, BladeRules.KILLS, 300), Map.of());
			case BladeRules.MOONWAKE -> history(Map.of(BladeRules.NIGHT, 250, BladeRules.KILLS, 300), Map.of());
			case BladeRules.RALLYING_STEEL -> history(Map.of(BladeRules.ALLIED, 120, BladeRules.KILLS, 300), Map.of());
			default -> throw new AssertionError(trait);
		};
	}

	@Test
	void thirteenTraitsEachWithItsWordsAndGlyph() throws IOException {
		assertEquals(13, BladeRules.BUILT_IN_TRAITS.size());
		assertEquals(new HashSet<>(BladeRules.BUILT_IN_TRAITS).size(), BladeRules.BUILT_IN_TRAITS.size());
		JsonObject lang = lang();
		String art = Files.readString(Path.of("tools/blade_art.py"));
		for (String id : BladeRules.BUILT_IN_TRAITS) {
			BladeRules.Trait t = BladeRules.trait(id).orElseThrow();
			for (String suffix : List.of("", ".desc", ".short", ".habit")) {
				assertTrue(lang.has(t.nameKey() + suffix), id + " has " + (suffix.isEmpty() ? "a name" : suffix));
			}
			assertTrue(Files.exists(Path.of("src/main/resources/assets/wildercord/textures/gui/sprites/aura/blade/trait_" + id + ".png")), id + " has a glyph");
			assertTrue(art.contains("\"" + id + "\""), id + " is drawn by tools/blade_art.py");
			assertFalse(t.reason().isBlank(), id + " says why it's worth what it is");
		}
		for (int tier = BladeRules.BONDED; tier <= BladeRules.MAX_TIER; tier++) {
			String tid = BladeRules.tierId(tier);
			assertTrue(lang.has("aura.wildercord.blade.tier." + tid), tid + " has a name");
			assertTrue(lang.has("screen.wildercord.aura.blade.tier_gives." + tid), tid + " says what it gives");
			assertTrue(Files.exists(Path.of("src/main/resources/assets/wildercord/textures/gui/sprites/aura/blade/tier_" + tid + ".png")), tid + " has an emblem");
			if (tier > BladeRules.BONDED) {
				assertTrue(lang.has("message.wildercord.aura.blade.tier." + tid), tid + " is told");
				assertTrue(lang.has("message.wildercord.aura.blade.tier." + tid + ".how"), tid + " says what it means");
				assertTrue(lang.has("toast.wildercord.aura.blade_" + tid), tid + " goes into the Grimoire");
			}
		}
		for (String id : BladeRules.SHOWN_COUNTS) {
			assertTrue(lang.has("tooltip.wildercord.bonded_blade.count." + id), id + " is shown");
		}
		for (String kind : BladeRules.DEEDS) {
			assertTrue(lang.has("tooltip.wildercord.bonded_blade.deed." + kind), kind + " has its line");
		}
	}

	@Test
	void eachTraitIsDrawnFromItsOwnHabit() {
		for (String id : BladeRules.BUILT_IN_TRAITS) {
			List<String> offer = BladeRules.offer(habit(id), 42L);
			assertEquals(id, offer.getFirst(), "a blade that " + id + "'s habit shows offers it first (" + offer + ")");
		}
	}

	@Test
	void noHabitNoTrait() {
		BladeRules.History fresh = history(Map.of(), Map.of());
		for (BladeRules.Trait t : BladeRules.traits()) {
			assertEquals(0, BladeRules.weight(t, fresh), 1e-9, t.id() + " isn't read in a blade that has done nothing");
		}
		assertEquals(List.of(BladeRules.CLOSING_STROKE, BladeRules.SUNDERING_STEEL, BladeRules.RIPOSTE), BladeRules.offer(fresh, 1L),
			"a blade showing nothing offers three that suit any blade");
		// Just under a habit's least, it isn't read.
		assertEquals(0, BladeRules.weight(BladeRules.trait(BladeRules.RIPOSTE).orElseThrow(), history(Map.of(BladeRules.GUARDS, 19), Map.of())), 1e-9);
		assertTrue(BladeRules.weight(BladeRules.trait(BladeRules.RIPOSTE).orElseThrow(), history(Map.of(BladeRules.GUARDS, 20), Map.of())) > 0);
		// A share-based habit needs the share: undead are only a habit when they're much of what it fells.
		assertEquals(0, BladeRules.weight(BladeRules.trait(BladeRules.GRAVEWARDEN).orElseThrow(),
			history(Map.of(BladeRules.UNDEAD, 100, BladeRules.KILLS, 400), Map.of())), 1e-9);
	}

	@Test
	void theMethodAndTheWayLean() {
		// Stances broken and perfect guards alike: Stone leans to Sundering Steel, the Bulwark to Riposte.
		Map<String, Integer> both = Map.of(BladeRules.BROKEN_STANCES, 60, BladeRules.GUARDS, 50, BladeRules.KILLS, 100);
		assertEquals(BladeRules.SUNDERING_STEEL, BladeRules.offer(new BladeRules.History(both, Map.of(), "stone", ""), 3L).getFirst());
		assertEquals(BladeRules.RIPOSTE, BladeRules.offer(new BladeRules.History(both, Map.of(), "", WayRules.BULWARK), 3L).getFirst());
		// A lean never conjures a habit that isn't there.
		assertEquals(0, BladeRules.weight(BladeRules.trait(BladeRules.RALLYING_STEEL).orElseThrow(),
			new BladeRules.History(Map.of(), Map.of(), "verdant", WayRules.BANNER)), 1e-9);
		assertEquals(1.0, BladeRules.methodLean("addon:method", BladeRules.RIPOSTE), 1e-9, "an add-on's method leans no way");
	}

	@Test
	void anOfferIsThreeAndAlwaysTheSame() {
		Map<String, Integer> counts = new HashMap<>();
		for (String id : List.of(BladeRules.FINISHERS, BladeRules.BROKEN_STANCES, BladeRules.GUARDS, BladeRules.STEPS, BladeRules.SLASHES)) {
			counts.put(id, 200);
		}
		counts.put(BladeRules.KILLS, 300);
		BladeRules.History h = history(counts, Map.of());
		List<String> a = BladeRules.offer(h, 7L);
		assertEquals(BladeRules.OFFER, a.size());
		assertEquals(BladeRules.OFFER, new HashSet<>(a).size(), "three different");
		assertEquals(a, BladeRules.offer(h, 7L), "the same blade and history, the same offer");
		// Equal habits: which go first is the blade's own (its seed), not the order traits were registered in.
		Set<List<String>> seen = new HashSet<>();
		for (long seed = 0; seed < 40; seed++) {
			seen.add(BladeRules.offer(h, seed));
		}
		assertTrue(seen.size() > 1, "equal habits break their ties by the blade's own seed");
	}

	/**
	 * Balance: each trait is worth a small, even share of a swordsman's strength (the reasoning is beside each in {@link BladeRules}), and
	 * nothing that touches a player is worth more than a little; the damage bonuses stay well under the spell-defence cap.
	 */
	@Test
	void theTraitsAreBalancedAndFairToPlayers() {
		Map<String, double[]> worths = BladeRules.worths();
		assertEquals(13, worths.size());
		double most = 0;
		double least = 1;
		for (Map.Entry<String, double[]> e : worths.entrySet()) {
			double alone = e.getValue()[0];
			double pvp = e.getValue()[1];
			assertTrue(alone >= 0.025 && alone <= 0.045, e.getKey() + " worth " + alone + " of a swordsman: a small, even share");
			assertTrue(pvp <= 0.03 + 1e-9, e.getKey() + " against a player " + pvp);
			assertTrue(pvp <= alone + 1e-9, e.getKey() + " never more against a player");
			most = Math.max(most, alone);
			least = Math.min(least, alone);
		}
		assertTrue(most / least <= 1.8, "no trait far ahead of another (" + least + " to " + most + ")");
		// At Soulforged (half again), still small: no damage bonus past a fifth, no price under two thirds.
		double s = BladeRules.SOULFORGED_TRAIT;
		for (double damage : new double[] {BladeRules.RIPOSTE_DAMAGE, BladeRules.MOUNTAINFELLER_DAMAGE, BladeRules.GRAVEWARDEN_DAMAGE}) {
			assertTrue(BladeRules.scaled(damage, s) <= 1.23, "damage " + damage + " at Soulforged");
		}
		for (double price : new double[] {BladeRules.WELL_WORN_PRICE, BladeRules.WIND_STEP_PRICE, BladeRules.LONG_CRESCENT_PRICE, BladeRules.INKBOUND_PRICE,
				BladeRules.SECOND_BLAZE_REST}) {
			assertTrue(BladeRules.scaled(price, s) >= 0.62, "price or rest " + price + " at Soulforged");
		}
		assertTrue(BladeRules.scaled(BladeRules.LAST_LIGHT_HARM, s) >= 0.85, "Last Light never more than a seventh off");
		// Against a player Riposte and Sundering are half (BladeTraits), and inside the cap a Soulforged Riposte is still a small bonus.
		assertTrue(BladeRules.scaled(BladeRules.RIPOSTE_DAMAGE, s * 0.5) <= 1.12);
		assertTrue(BladeRules.scaled(BladeRules.SUNDERING_WEAR, s * 0.5) <= 1.1);
		assertEquals(0, worths.get(BladeRules.MOUNTAINFELLER)[1], 1e-9, "Mountainfeller never touches a player");
		assertEquals(0, worths.get(BladeRules.GRAVEWARDEN)[1], 1e-9, "Gravewarden never touches a player");
	}

	@Test
	void anAddonsTraitTakesNoBuiltInPlace() {
		assertThrows(IllegalArgumentException.class, () -> BladeRules.register(new BladeRules.Trait("riposte", h -> 1, 0.03, 0.03, "x")));
		BladeRules.Trait own = new BladeRules.Trait("testmod:steady_" + UUID.randomUUID().toString().substring(0, 8), h -> h.count(BladeRules.GUARDS) > 0 ? 0.5 : 0,
			0.03, 0.0, "a test's");
		BladeRules.register(own);
		assertThrows(IllegalArgumentException.class, () -> BladeRules.register(own));
		assertTrue(BladeRules.trait(own.id()).isPresent());
		assertFalse(BladeRules.BUILT_IN_TRAITS.contains(own.id()));
		assertFalse(BladeRules.worths().containsKey(own.id()), "the balance test weighs the mod's own");
	}

	// ------------------------------------------------------------------ names

	private static final List<String> ELEMENTS = List.of("fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood", "");
	private static final List<String> WAYS = List.of("", WayRules.BLADE, WayRules.BULWARK, WayRules.SHADOWSTEP, WayRules.BANNER);
	private static final List<String> BIOMES = List.of("minecraft:desert", "minecraft:birch_forest", "minecraft:snowy_taiga", "minecraft:cherry_grove",
		"minecraft:deep_dark", "minecraft:plains", "minecraft:the_void", "", "minecraft:nether_wastes", "minecraft:the_end");

	@Test
	void itsNamesAreItsOwnCleanAndShort() {
		Set<String> all = new HashSet<>();
		int checked = 0;
		for (int seed = 0; seed < 60; seed++) {
			for (String element : ELEMENTS) {
				for (String way : WAYS) {
					String biome = BIOMES.get(Math.floorMod(seed * 7 + element.hashCode(), BIOMES.size()));
					BladeRules.NameSeed ns = new BladeRules.NameSeed(element, way, seed % 3 == 0 ? "Sunfall" : "", biome,
						seed % 4 == 0 ? BladeRules.UNDEAD : "", seed * 977L + way.hashCode());
					for (int salt = 0; salt < 6; salt++) {
						String name = BladeRules.suggest(ns, salt);
						assertFalse(name.isEmpty());
						assertTrue(name.length() <= BladeRules.MAX_NAME, name + " too long");
						assertEquals(name, BladeRules.cleanName(name), name + " is clean");
						assertEquals(name, BladeRules.suggest(ns, salt), "the same seed and salt, the same name");
						String last = name.substring(name.lastIndexOf(' ') + 1);
						assertTrue(name.matches("[A-Z][a-z]+( (of|the|[A-Z][a-z]+))*") && Character.isUpperCase(last.charAt(0)), name + " reads as a name");
						all.add(name);
						checked++;
					}
				}
			}
		}
		assertTrue(all.size() > checked / 7, "plenty of names (" + all.size() + " of " + checked + ")");
	}

	@Test
	void itsNamesSpeakOfItsStory() {
		// An Ember blade bonded in a desert, its favourite art Sunfall: its names speak of fire, the dunes or the sun.
		BladeRules.NameSeed ember = new BladeRules.NameSeed("fire", WayRules.BLADE, "Sunfall", "minecraft:desert", "", 1234L);
		Set<String> words = Set.of("ember", "cinder", "ash", "kindle", "pyre", "soot", "flare", "dune", "dunes", "sunfall", "keen", "bright", "true");
		int speaking = 0;
		for (int salt = 0; salt < 30; salt++) {
			String name = BladeRules.suggest(ember, salt).toLowerCase(Locale.ROOT);
			if (words.stream().anyMatch(name::contains)) {
				speaking++;
			}
		}
		assertTrue(speaking >= 18, "most of its names speak of it (" + speaking + " of 30)");
		// Another salt, another name, mostly.
		Set<String> names = new HashSet<>();
		for (int salt = 0; salt < 12; salt++) {
			names.add(BladeRules.suggest(ember, salt));
		}
		assertTrue(names.size() >= 8, "asking again gives another (" + names + ")");
		assertEquals("Sunfall", BladeRules.artWord("sunfall"), "an art lends its name");
		assertEquals("Kindling", BladeRules.artWord("kindling_draw"), "its first word");
		assertEquals("", BladeRules.artWord("technique_1"), "a technique lends none");
		assertEquals("", BladeRules.artWord("first_art"), "nor a common art");
		assertEquals(BladeRules.UNDEAD, BladeRules.foeKind(Map.of(BladeRules.KILLS, 100, BladeRules.UNDEAD, 60)));
		assertEquals(BladeRules.BOSSES, BladeRules.foeKind(Map.of(BladeRules.KILLS, 100, BladeRules.BOSSES, 4, BladeRules.UNDEAD, 60)));
		assertEquals("", BladeRules.foeKind(Map.of(BladeRules.KILLS, 100, BladeRules.UNDEAD, 10)));
	}

	// ------------------------------------------------------------------ which weapons

	@Test
	void onlyOneWearingBladeBonds() {
		assertTrue(BladeRules.bondable(true, true, 1));
		assertFalse(BladeRules.bondable(false, true, 1), "not a bondable kind");
		assertFalse(BladeRules.bondable(true, false, 1), "a blade that never wears (an unbreakable one's fine as vanilla's: it still wears)");
		assertFalse(BladeRules.bondable(true, true, 2), "one at a time");
	}

	// ------------------------------------------------------------------ the component

	@Test
	void theBondTravelsWhole() {
		BladeBond b = sample();
		JsonElement json = BladeBond.CODEC.encodeStart(JsonOps.INSTANCE, b).getOrThrow();
		assertEquals(b, BladeBond.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(), "saved and read back the same");
		ByteBuf buf = Unpooled.buffer();
		BladeBond.STREAM_CODEC.encode(buf, b);
		assertEquals(b, BladeBond.STREAM_CODEC.decode(buf), "sent and received the same");
		BladeBond.Former former = new BladeBond.Former("Cinderwake", "Alex", BladeRules.AWAKENED);
		JsonElement fj = BladeBond.Former.CODEC.encodeStart(JsonOps.INSTANCE, former).getOrThrow();
		assertEquals(former, BladeBond.Former.CODEC.parse(JsonOps.INSTANCE, fj).getOrThrow());
		// A bond written before a field existed reads with its default.
		JsonObject old = json.getAsJsonObject().deepCopy();
		old.remove("history");
		assertEquals(BladeBond.History.EMPTY, BladeBond.CODEC.parse(JsonOps.INSTANCE, old).getOrThrow().history());
	}

	@Test
	void aBondReadsItself() {
		BladeBond b = sample();
		assertEquals("Cinderwake", b.shownName());
		assertEquals(b.seed(), sample().seed(), "its seed is its id's");
		assertTrue(b.ownedBy(b.owner()));
		assertFalse(b.ownedBy(UUID.randomUUID()));
		BladeBond forged = b.withGrowth(b.growth().withName("Ember‮Fang§l", 3));
		assertEquals("EmberFang", forged.shownName(), "a name stored forged is shown made safe");
		BladeRules.History habits = b.habits("ember", WayRules.BLADE);
		assertEquals("ember", habits.method());
		assertEquals(12, habits.count(BladeRules.KILLS));
		BladeBond fresh = BladeBond.fresh(UUID.randomUUID(), UUID.randomUUID(), "Sam", 0xF06E32, BladeBond.Origin.NOWHERE);
		assertEquals(BladeRules.BONDED, fresh.tier());
		assertEquals(0xF06E32, fresh.color());
		assertTrue(fresh.shownName().isEmpty());
	}

	private static BladeBond sample() {
		UUID id = UUID.fromString("0f8a3c5e-1234-4abc-9def-56789abcdef0");
		UUID owner = UUID.fromString("a1b2c3d4-0000-4000-8000-123456789abc");
		BladeBond.Who who = new BladeBond.Who(id, owner, "Alex", List.of("Old Master"));
		BladeBond.Growth growth = new BladeBond.Growth(0xF06E32, BladeRules.AWAKENED, 1324.5F, "Cinderwake", 12, BladeRules.RIPOSTE, "",
			List.of(BladeRules.RIPOSTE, BladeRules.CLOSING_STROKE, BladeRules.SUNDERING_STEEL), false);
		BladeBond.Origin origin = new BladeBond.Origin(3, "minecraft:desert", "minecraft:overworld", 120, 70, -340, "ember", "ceremony");
		BladeBond.History history = new BladeBond.History(Map.of(BladeRules.KILLS, 12, BladeRules.GUARDS, 40), Map.of("ember_sunfall", 9),
			List.of(new BladeBond.Deed(BladeRules.DEED_TIER, "named", 9), new BladeBond.Deed(BladeRules.DEED_BOSS, "entity.minecraft.wither", 11)));
		return new BladeBond(who, growth, origin, history);
	}

	private static JsonObject lang() throws IOException {
		try (InputStream in = BladeRulesTest.class.getResourceAsStream("/assets/wildercord/lang/en_us.json")) {
			if (in != null) {
				return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			}
		}
		return JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/wildercord/lang/en_us.json"))).getAsJsonObject();
	}
}
