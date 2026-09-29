package dev.wildercord.spell;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.wildercord.spell.Affinity.Verdict;
import dev.wildercord.spell.ClimateRules.Condition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Creature affinities and elemental climate: how weaknesses, resistances, immunity and reactions
 * combine, the Bestiary's Grimoire keys, the climate table and which conditions hold where, and the
 * generated affinity tags (no creature both weak to and resisting one element).
 */
class AffinityAndClimateTest {
	private static final double EPS = 1e-9;

	// ------------------------------------------------------------------ affinities

	@Test
	void weaknessResistanceAndImmunity() {
		assertEquals(Verdict.NONE, Affinity.judge(false, false, false, false));
		assertEquals(Verdict.WEAK, Affinity.judge(true, false, false, false));
		assertEquals(Verdict.RESISTED, Affinity.judge(false, true, false, false));
		assertEquals(Verdict.IMMUNE, Affinity.judge(false, false, true, false));
		assertEquals(1.5, Affinity.multiplier(Verdict.WEAK), EPS);
		assertEquals(0.5, Affinity.multiplier(Verdict.RESISTED), EPS);
		assertEquals(0.0, Affinity.multiplier(Verdict.IMMUNE), EPS);
		assertEquals(1.0, Affinity.multiplier(Verdict.NONE), EPS);
	}

	@Test
	void aReactionBreaksThroughAResistanceButNotAnImmunity() {
		// The Cinder Warden resists fire, but a Shatter set off on it lands in full.
		assertEquals(Verdict.NONE, Affinity.judge(false, true, false, true));
		assertEquals(Verdict.IMMUNE, Affinity.judge(false, false, true, true));
		// A weakness still counts with a reaction.
		assertEquals(Verdict.WEAK, Affinity.judge(true, false, false, true));
	}

	@Test
	void aRuneboundCarryingItsWeaknessCancelsIt() {
		// A stray (weak to fire) with a Fire Arc on its Cord: the two cancel.
		assertEquals(Verdict.NONE, Affinity.judge(true, true, false, false));
	}

	@Test
	void damageWithoutAnEffectCountsAsItsKind() {
		assertEquals("fire", Affinity.elementOf(true, false, false, false));
		assertEquals("frost", Affinity.elementOf(false, true, false, false));
		assertEquals("storm", Affinity.elementOf(false, false, true, false));
		assertEquals("void", Affinity.elementOf(false, false, false, true));
		assertEquals("", Affinity.elementOf(false, false, false, false));
	}

	// ------------------------------------------------------------------ the Bestiary

	@Test
	void bestiaryKeysReadBack() {
		String weak = Bestiary.key("minecraft:blaze", Bestiary.Kind.WEAK, "frost");
		assertEquals("bestiary:minecraft:blaze|weak|frost", weak);
		assertEquals(new Bestiary.Entry("minecraft:blaze", Bestiary.Kind.WEAK, "frost"), Bestiary.parse(weak).orElseThrow());
		assertEquals(new Bestiary.Entry("wildercord:cinder_warden", Bestiary.Kind.MET, ""), Bestiary.parse(Bestiary.metKey("wildercord:cinder_warden")).orElseThrow());
		assertEquals(Bestiary.metKey("minecraft:husk"), Bestiary.key("minecraft:husk", Bestiary.Kind.MET, "fire"));
		assertTrue(Bestiary.parse("feat:overcast").isEmpty());
		assertTrue(Bestiary.parse("bestiary:").isEmpty());
		assertTrue(Bestiary.parse("bestiary:minecraft:blaze|sleepy|frost").isEmpty());
		assertTrue(Bestiary.parse("bestiary:minecraft:blaze|met|frost").isEmpty());
		assertTrue(Bestiary.parse("bestiary:minecraft:blaze|weak").isEmpty());
	}

	@Test
	void theBestiaryListsWhatWasMetAndFound() {
		List<String> grimoire = List.of("reaction:shatter", Bestiary.metKey("minecraft:blaze"), Bestiary.key("minecraft:blaze", Bestiary.Kind.WEAK, "frost"),
			Bestiary.metKey("minecraft:husk"), Bestiary.key("minecraft:blaze", Bestiary.Kind.IMMUNE, "fire"), Bestiary.metKey("minecraft:blaze"));
		assertEquals(List.of("minecraft:blaze", "minecraft:husk"), Bestiary.met(grimoire));
		assertEquals(List.of("frost"), Bestiary.known(grimoire, "minecraft:blaze", Bestiary.Kind.WEAK));
		assertEquals(List.of("fire"), Bestiary.known(grimoire, "minecraft:blaze", Bestiary.Kind.IMMUNE));
		assertEquals(List.of(), Bestiary.known(grimoire, "minecraft:husk", Bestiary.Kind.WEAK));
	}

	@Test
	void onlyAWeaknessFoundIsAnnouncedAndRewarded() {
		String weak = Bestiary.key("minecraft:blaze", Bestiary.Kind.WEAK, "frost");
		assertFalse(Bestiary.quiet(weak));
		assertTrue(Bestiary.quiet(Bestiary.metKey("minecraft:blaze")));
		assertTrue(Bestiary.quiet(Bestiary.key("minecraft:blaze", Bestiary.Kind.RESISTS, "fire")));
		assertTrue(Bestiary.quiet(Bestiary.key("minecraft:snow_golem", Bestiary.Kind.IMMUNE, "frost")));
		assertFalse(Bestiary.quiet("feat:overcast"));
		assertEquals(Bestiary.WEAKNESS_REWARD, Feats.reward(weak));
		assertEquals(0, Feats.reward(Bestiary.metKey("minecraft:blaze")));
		assertEquals(0, Feats.reward(Bestiary.key("minecraft:blaze", Bestiary.Kind.RESISTS, "fire")));
		// Bestiary entries never count toward a full Grimoire.
		assertTrue(Feats.everyEntry().stream().noneMatch(k -> k.startsWith(Bestiary.PREFIX)));
	}

	// ------------------------------------------------------------------ climate

	@Test
	void theClimateTable() {
		assertEquals(1.20, ClimateRules.factor(Set.of(Condition.NETHER), "fire"), EPS);
		assertEquals(0.75, ClimateRules.factor(Set.of(Condition.NETHER), "frost"), EPS);
		assertEquals(1.00, ClimateRules.factor(Set.of(Condition.NETHER), "storm"), EPS);
		assertEquals(1.20, ClimateRules.factor(Set.of(Condition.END), "void"), EPS);
		assertEquals(1.25, ClimateRules.factor(Set.of(Condition.THUNDER), "storm"), EPS);
		assertEquals(0.90, ClimateRules.factor(Set.of(Condition.RAIN), "fire"), EPS);
		assertEquals(1.20, ClimateRules.factor(Set.of(Condition.SNOW), "frost"), EPS);
		assertEquals(0.90, ClimateRules.factor(Set.of(Condition.SNOW), "fire"), EPS);
		assertEquals(1.15, ClimateRules.factor(Set.of(Condition.HEAT), "fire"), EPS);
		assertEquals(0.90, ClimateRules.factor(Set.of(Condition.HEAT), "frost"), EPS);
		assertEquals(1.10, ClimateRules.factor(Set.of(Condition.NIGHT), "void"), EPS);
		assertEquals(1.10, ClimateRules.factor(Set.of(Condition.SUN), "life"), EPS);
		assertEquals(1.15, ClimateRules.factor(Set.of(Condition.DEEP), "earth"), EPS);
		assertEquals(1.15, ClimateRules.factor(Set.of(Condition.LEY), "arcane"), EPS);
		assertEquals(1.0, ClimateRules.factor(Set.of(), "fire"), EPS);
		assertEquals(1.0, ClimateRules.factor(Set.of(Condition.NETHER), ""), EPS);
	}

	@Test
	void conditionsStackButStayModest() {
		// A thunderstorm in the rain: storm crackles, fire gutters.
		Set<Condition> storm = EnumSet.of(Condition.THUNDER, Condition.RAIN, Condition.NIGHT);
		assertEquals(1.25, ClimateRules.factor(storm, "storm"), EPS);
		assertEquals(0.90, ClimateRules.factor(storm, "fire"), EPS);
		assertEquals(1.10, ClimateRules.factor(storm, "void"), EPS);
		// Every element stays within the bounds however things stack.
		Set<Condition> everything = EnumSet.allOf(Condition.class);
		for (String element : Affinity.ELEMENTS) {
			double factor = ClimateRules.factor(everything, element);
			assertTrue(factor >= ClimateRules.MIN && factor <= ClimateRules.MAX, element + " " + factor);
		}
		// Only the changed elements, favoured or hindered, in the Grimoire's order.
		Map<String, Double> nether = ClimateRules.factors(Set.of(Condition.NETHER));
		assertEquals(List.of("fire", "frost"), new ArrayList<>(nether.keySet()));
		assertTrue(ClimateRules.factors(Set.of()).isEmpty());
	}

	@Test
	void theGrimoireDescribesEachCondition() {
		assertEquals("fire +20%, frost -25%", ClimateRules.describe(Condition.NETHER, e -> e));
		assertEquals("Storm +25%", ClimateRules.describe(Condition.THUNDER, e -> Character.toUpperCase(e.charAt(0)) + e.substring(1)));
	}

	private static ClimateRules.Surroundings overworld(long time, boolean sky, boolean raining, boolean thunder, boolean rainOnThem, boolean cold,
			float temperature, boolean dry, int y, boolean ley) {
		return new ClimateRules.Surroundings("minecraft:overworld", true, time, sky, raining, thunder, rainOnThem, cold, temperature, dry, y, ley);
	}

	@Test
	void whereConditionsHold() {
		// A clear noon in the plains: sunlight.
		assertEquals(EnumSet.of(Condition.SUN), ClimateRules.conditions(overworld(6000, true, false, false, false, false, 0.8F, false, 70, false)));
		// Midnight under the sky: night. Under a roof: nothing.
		assertEquals(EnumSet.of(Condition.NIGHT), ClimateRules.conditions(overworld(18000, true, false, false, false, false, 0.8F, false, 70, false)));
		assertEquals(EnumSet.noneOf(Condition.class), ClimateRules.conditions(overworld(18000, false, false, false, false, false, 0.8F, false, 70, false)));
		// A rainy day is no sunlight; a thunderstorm needs the open sky.
		assertEquals(EnumSet.of(Condition.RAIN), ClimateRules.conditions(overworld(6000, true, true, false, true, false, 0.8F, false, 70, false)));
		assertEquals(EnumSet.of(Condition.THUNDER, Condition.RAIN), ClimateRules.conditions(overworld(6000, true, true, true, true, false, 0.8F, false, 70, false)));
		assertFalse(ClimateRules.conditions(overworld(6000, false, true, true, false, false, 0.8F, false, 70, false)).contains(Condition.THUNDER));
		// Snowy lands, and the desert (hot and never rains); a warm jungle that rains is neither.
		assertTrue(ClimateRules.conditions(overworld(6000, true, false, false, false, true, -0.5F, false, 70, false)).contains(Condition.SNOW));
		assertTrue(ClimateRules.conditions(overworld(6000, true, false, false, false, false, 2.0F, true, 70, false)).contains(Condition.HEAT));
		assertFalse(ClimateRules.conditions(overworld(6000, true, false, false, false, false, 0.95F, false, 70, false)).contains(Condition.HEAT));
		// Deep underground: below 0, in the Overworld only.
		assertEquals(EnumSet.of(Condition.DEEP), ClimateRules.conditions(overworld(6000, false, false, false, false, false, 0.8F, false, -20, false)));
		// A ley line, anywhere.
		assertTrue(ClimateRules.conditions(overworld(6000, false, false, false, false, false, 0.8F, false, 64, true)).contains(Condition.LEY));
	}

	@Test
	void theNetherAndTheEndAreTheirOwnClimate() {
		// Hot and dry, but it's the Nether that counts; no day or night, no depth.
		ClimateRules.Surroundings nether = new ClimateRules.Surroundings("minecraft:the_nether", false, 18000, false, false, false, false, false, 2.0F, true, -10, false);
		assertEquals(EnumSet.of(Condition.NETHER), ClimateRules.conditions(nether));
		ClimateRules.Surroundings end = new ClimateRules.Surroundings("minecraft:the_end", false, 6000, true, false, false, false, false, 0.5F, true, 60, false);
		assertEquals(EnumSet.of(Condition.END), ClimateRules.conditions(end));
	}

	@Test
	void conditionsTravelAsIds() {
		Set<Condition> here = EnumSet.of(Condition.RAIN, Condition.THUNDER);
		assertEquals(List.of("thunder", "rain"), ClimateRules.ids(here));
		assertEquals(here, ClimateRules.fromIds(List.of("rain", "thunder", "something_newer")));
		assertEquals(List.of(), ClimateRules.ids(Set.of()));
	}

	// ------------------------------------------------------------------ the generated table

	@Test
	void theAffinityTagsAreConsistent() throws IOException {
		Path dir = Path.of("src/main/resources/data/wildercord/tags/entity_type/affinity");
		Map<String, Set<String>> tags = new HashMap<>();
		try (Stream<Path> files = Files.list(dir)) {
			for (Path file : files.toList()) {
				String name = file.getFileName().toString().replace(".json", "");
				Set<String> values = new HashSet<>();
				for (JsonElement value : JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("values")) {
					values.add(value.getAsString());
				}
				tags.put(name, values);
				String element = name.substring(name.lastIndexOf('_') + 1);
				assertTrue(Affinity.ELEMENTS.contains(element), name);
				assertTrue(name.startsWith("weak_to_") || name.startsWith("resists_") || name.startsWith("immune_to_"), name);
			}
		}
		assertFalse(tags.isEmpty());
		for (String element : Affinity.ELEMENTS) {
			Set<String> weak = tags.getOrDefault("weak_to_" + element, Set.of());
			for (String other : List.of("resists_" + element, "immune_to_" + element)) {
				for (String creature : tags.getOrDefault(other, Set.of())) {
					assertFalse(weak.contains(creature), creature + " is both weak to and in " + other);
				}
			}
		}
		// The ones the in-game test and the wiki lean on.
		assertTrue(tags.get("weak_to_frost").contains("minecraft:blaze"));
		assertTrue(tags.get("resists_fire").contains("minecraft:hoglin"));
		assertTrue(tags.get("resists_fire").contains("wildercord:cinder_warden"));
		assertTrue(tags.get("weak_to_life").contains("#minecraft:undead"));
	}
}
