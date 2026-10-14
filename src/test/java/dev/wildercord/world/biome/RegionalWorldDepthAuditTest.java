package dev.wildercord.world.biome;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import dev.wildercord.lore.LoreEntries;
import dev.wildercord.lore.LoreQuestRules;
import dev.wildercord.spell.Attunements;
import dev.wildercord.world.cartographer.CartographerRules;
import dev.wildercord.world.ferryman.FerrymanRules;
import dev.wildercord.world.hollowbell.HollowBellRules;
import dev.wildercord.world.kilnwidow.KilnWidowRules;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cross-region world-depth audit (BIO-05, POL-03, POL-04, POL-06, POL-08, LOR-06):
 * verifies that all four custom regional slices (Luminous Reedlands, Windscar Highlands,
 * Belowkeeper Hollows, and Emberwood Reach) compose commutatively, share parent attunements,
 * define valid structure sets with unique salts, bound their 3-chapter lore routes and boss
 * rules, and provide complete journal/dialogue localization.
 */
class RegionalWorldDepthAuditTest {
	private static final List<BiConsumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>>> ADAPTERS = List.of(
		ReedlandClimate::emit,
		WindscarClimate::emit,
		BelowkeeperClimate::emit,
		EmberwoodClimate::emit
	);

	@Test
	void allTwentyFourClimateAdapterPermutationsProduceIdenticalIdempotentPartitions() {
		var sampleBiomes = List.of(
			Biomes.SWAMP, Biomes.WINDSWEPT_HILLS, Biomes.DRIPSTONE_CAVES, Biomes.WOODED_BADLANDS,
			Biomes.FOREST, Biomes.PLAINS, Biomes.LUSH_CAVES, Biomes.BADLANDS
		);
		var point = new Climate.ParameterPoint(
			new Climate.Parameter(-10000, 10000),
			new Climate.Parameter(-10000, 10000),
			new Climate.Parameter(-10000, 10000),
			new Climate.Parameter(-10000, 10000),
			new Climate.Parameter(-10000, 10000),
			new Climate.Parameter(-10000, 10000),
			0
		);
		List<List<Integer>> perms = permutations(List.of(0, 1, 2, 3));
		assertEquals(24, perms.size());

		for (var biome : sampleBiomes) {
			var input = List.of(Pair.of(point, biome));
			List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> reference = applyOrder(input, perms.getFirst());
			for (var perm : perms) {
				var actual = applyOrder(input, perm);
				assertEquals(reference, actual, "Permutation " + perm + " differed for " + biome.identifier());
				var idempotent = applyOrder(actual, perm);
				assertEquals(reference, idempotent, "Idempotence failed for " + biome.identifier());
			}
		}
	}

	@Test
	void allFourRegionalBiomesShareParentAttunementGroupsWithoutDuplicatingRules() {
		assertEquals(15, Attunements.RULES.size(), "Regional biomes must share existing attunement rules rather than adding duplicate daily allowances");
		assertTrue(Attunements.byId("swamp").orElseThrow().biomes().contains("wildercord:luminous_reedlands"));
		assertTrue(Attunements.byId("peaks").orElseThrow().biomes().contains("wildercord:windscar_highlands"));
		assertTrue(Attunements.byId("dripstone_caves").orElseThrow().biomes().contains("wildercord:belowkeeper_hollows"));
		assertTrue(Attunements.byId("badlands").orElseThrow().biomes().contains("wildercord:emberwood_reach"));

		for (String biome : List.of(
			"wildercord:luminous_reedlands",
			"wildercord:windscar_highlands",
			"wildercord:belowkeeper_hollows",
			"wildercord:emberwood_reach"
		)) {
			assertTrue(Attunements.biomeHolds(biome), biome + " should support its shared regional attunement");
			long matchingRules = Attunements.RULES.stream().filter(r -> r.biomes().contains(biome)).count();
			assertEquals(1L, matchingRules, biome + " must belong to exactly one attunement rule");
		}
	}

	@Test
	void allEightRegionalStructuresAndSetsHaveValidSaltsSpacingAndBiomeTags() throws Exception {
		var regionalStructures = List.of(
			"water_reedkeeper_landing", "water_last_crossing",
			"highland_cairn_lodge", "highland_survey_ring",
			"belowkeeper_listening_station", "belowkeeper_bell_sanctum",
			"emberwood_charcoal_yard", "emberwood_kiln_court"
		);
		Set<Long> salts = new HashSet<>();
		for (String id : regionalStructures) {
			JsonObject structure = readJson("/data/wildercord/worldgen/structure/" + id + ".json");
			assertEquals("wildercord:" + id, structure.get("type").getAsString());
			assertTrue(structure.get("biomes").getAsString().startsWith("#wildercord:has_structure/"), id + " biome tag");

			JsonObject set = readJson("/data/wildercord/worldgen/structure_set/" + id + ".json");
			JsonObject placement = set.getAsJsonObject("placement");
			int spacing = placement.get("spacing").getAsInt();
			int separation = placement.get("separation").getAsInt();
			long salt = placement.get("salt").getAsLong();
			assertTrue(spacing > separation && separation > 0, id + " spacing/separation must be valid");
			assertTrue(salts.add(salt), id + " must have a unique structure-set salt");
		}
	}

	@Test
	void allFourRegionalThreeChapterRoutesPayFiniteRewardsAndNeverReplay() {
		var routes = List.of(
			List.of("drowned_road", "second_count", "last_boat_light"),
			List.of("paired_cairns", "ridge_orders", "cairn_beacon"),
			List.of("empty_bell_kept", "pressure_records", "resonance_shutter"),
			List.of("eleven_winters", "forge_records", "kiln_draft_lamp")
		);
		for (var route : routes) {
			int totalXp = 0, totalRunes = 0, totalShards = 0;
			for (int i = 0; i < route.size(); i++) {
				var quest = LoreQuestRules.byId(route.get(i));
				assertNotNull(quest, "Missing quest " + route.get(i));
				assertEquals(i == 0 ? "" : route.get(i - 1), quest.prerequisite(), "Ordered prerequisite for " + quest.id());
				totalXp += quest.xp();
				totalRunes += quest.runes();
				totalShards += quest.shards();
			}
			assertEquals(90, totalXp, "Each 3-chapter regional route awards 90 XP total");
			assertEquals(5, totalRunes, "Each 3-chapter regional route awards 5 Blank Runes total");
			assertEquals(1, totalShards, "Each 3-chapter regional route awards 1 Aura Shard total");
		}

		// Simulate advancing all 4 regional routes to completion, then verify idempotence.
		Set<String> allRegionalFacts = Set.of(
			LoreQuestRules.REEDKEEPER, LoreQuestRules.FERRYMAN_STORY, LoreQuestRules.BOATHOUSE, LoreQuestRules.COAST_LIGHT,
			LoreQuestRules.CAIRN_KEEPER, LoreQuestRules.CARTOGRAPHER_STORY, LoreQuestRules.RIDGE_SITE, LoreQuestRules.CAIRN_LAMP,
			LoreQuestRules.BELOWKEEPER_LISTENER, LoreQuestRules.HOLLOW_BELL_STORY, LoreQuestRules.DEEP_SITE, LoreQuestRules.BELOWKEEPER_SHUTTER,
			LoreQuestRules.KILN_KEEPER, LoreQuestRules.KILN_WIDOW_STORY, LoreQuestRules.FORGE_SITE, LoreQuestRules.EMBERWOOD_DRAFT
		);
		Map<String, Integer> stages = new HashMap<>();
		int completedCount = 0;
		for (int step = 0; step < 6; step++) {
			var adv = LoreQuestRules.advance(stages, allRegionalFacts);
			stages = adv.quests();
			completedCount += adv.completed().size();
		}
		assertEquals(12, completedCount, "All 12 chapters across the 4 regional routes complete exactly once");
		var replay = LoreQuestRules.advance(stages, allRegionalFacts);
		assertFalse(replay.changed(), "Completed regional routes must never replay on subsequent polls");
		assertTrue(replay.completed().isEmpty());
	}

	@Test
	void allRegionalJournalEntriesAndResidentDialoguesAreCategorizedAndTranslated() throws Exception {
		JsonObject lang = readJson("/assets/wildercord/lang/en_us.json");
		var places = List.of(
			"place:luminous_reedlands", "place:windscar_highlands", "place:belowkeeper_hollows", "place:emberwood_reach",
			"place:water_reedkeeper_landing", "place:water_last_crossing",
			"place:highland_cairn_lodge", "place:highland_survey_ring",
			"place:belowkeeper_listening_station", "place:belowkeeper_bell_sanctum",
			"place:emberwood_charcoal_yard", "place:emberwood_kiln_court"
		);
		var people = List.of(
			"person:reedkeeper", "person:cairn_keeper", "person:belowkeeper_listener", "person:kiln_keeper"
		);
		var learned = List.of(
			"learned:lighthouse_keeper_mark", "learned:scout_wind_mark", "learned:belowkeeper_shutter", "learned:emberwood_draft",
			"learned:reed_crowned_ferryman", "learned:glass_cartographer", "learned:hollow_bell", "learned:kiln_widow"
		);
		for (String entry : places) {
			assertEquals("places", LoreEntries.tab(entry));
			assertTranslated(lang, LoreEntries.fixedKey(entry) + ".title");
			assertTranslated(lang, LoreEntries.fixedKey(entry) + ".body");
		}
		for (String entry : people) {
			assertEquals("people", LoreEntries.tab(entry));
			assertTranslated(lang, LoreEntries.fixedKey(entry) + ".title");
			assertTranslated(lang, LoreEntries.fixedKey(entry) + ".body");
		}
		for (String entry : learned) {
			assertEquals("learned", LoreEntries.tab(entry));
			assertTranslated(lang, LoreEntries.fixedKey(entry) + ".title");
			assertTranslated(lang, LoreEntries.fixedKey(entry) + ".body");
		}

		var dialogues = Map.of(
			"reedkeeper", List.of("reeds", "lantern", "tide", "ferryman", "ledger_note", "lighthouse_lamp", "light_return", "ferryman_return"),
			"cairn_keeper", List.of("wind", "windreed", "cairns", "cartographer", "watch_orders", "cairn_light", "light_return", "cartographer_return"),
			"belowkeeper_listener", List.of("drip", "glowcap", "shutter", "hollow_bell", "deep_records", "station_gauge", "gauge_return", "bell_return"),
			"kiln_keeper", List.of("ash", "cinder_fern", "firebreaks", "kiln_widow", "forge_ledger", "kiln_draft", "draft_return", "widow_return")
		);
		for (var entry : dialogues.entrySet()) {
			assertTranslated(lang, "dialogue.wildercord.role." + entry.getKey());
			for (String line : entry.getValue()) {
				String fullLine = entry.getKey() + "." + line;
				assertEquals("talk", LoreEntries.tab("said:" + fullLine));
				assertTranslated(lang, LoreEntries.lineKey(fullLine));
			}
		}
	}

	@Test
	void allFourRegionalBossRuleContractsMatchPhaseSafetyAndResetInvariants() {
		assertEquals(180f, FerrymanRules.HEALTH);
		assertEquals(180f, CartographerRules.HEALTH);
		assertEquals(180f, HollowBellRules.HEALTH);
		assertEquals(180f, KilnWidowRules.HEALTH);

		assertEquals(200, FerrymanRules.QUIET_RESET);
		assertEquals(200, CartographerRules.QUIET_RESET);
		assertEquals(200, HollowBellRules.QUIET_RESET);
		assertEquals(200, KilnWidowRules.QUIET_RESET);

		assertEquals(1.75, FerrymanRules.SAFE_HALF_WIDTH);
		assertEquals(1.75, CartographerRules.SAFE_HALF_WIDTH);
		assertEquals(1.75, HollowBellRules.SAFE_HALF_WIDTH);
		assertEquals(1.75, KilnWidowRules.SAFE_HALF_WIDTH);

		assertTrue(FerrymanRules.second(90f, 180f) && !FerrymanRules.second(91f, 180f));
		assertTrue(CartographerRules.second(90f, 180f) && !CartographerRules.second(91f, 180f));
		assertTrue(HollowBellRules.second(90f, 180f) && !HollowBellRules.second(91f, 180f));
		assertTrue(KilnWidowRules.second(90f, 180f) && !KilnWidowRules.second(91f, 180f));
	}

	private static List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> applyOrder(
		List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> current, List<Integer> order) {
		var list = current;
		for (int index : order) {
			var next = new ArrayList<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>();
			var adapter = ADAPTERS.get(index);
			for (var item : list) adapter.accept(item, next::add);
			list = next;
		}
		return list;
	}

	private static List<List<Integer>> permutations(List<Integer> items) {
		if (items.size() <= 1) return List.of(items);
		var result = new ArrayList<List<Integer>>();
		for (int i = 0; i < items.size(); i++) {
			var rest = new ArrayList<>(items);
			int head = rest.remove(i);
			for (var sub : permutations(rest)) {
				var combined = new ArrayList<Integer>(items.size());
				combined.add(head);
				combined.addAll(sub);
				result.add(combined);
			}
		}
		return result;
	}

	private static JsonObject readJson(String resourcePath) throws Exception {
		try (var in = RegionalWorldDepthAuditTest.class.getResourceAsStream(resourcePath)) {
			assertNotNull(in, "Missing resource: " + resourcePath);
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static void assertTranslated(JsonObject lang, String key) {
		assertTrue(lang.has(key), "Missing translation key: " + key);
		String value = lang.get(key).getAsString();
		assertFalse(value.isBlank(), "Blank translation value for: " + key);
	}
}
