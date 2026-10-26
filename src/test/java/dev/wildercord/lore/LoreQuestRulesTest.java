package dev.wildercord.lore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.wildercord.lore.LoreQuestRules.*;
import static org.junit.jupiter.api.Assertions.*;

/** The discovery quests' state machine, the facts they read, and the text every quest and line needs. */
class LoreQuestRulesTest {
	private static Quest quest(String id) {
		Quest quest = byId(id);
		assertNotNull(quest, id);
		return quest;
	}

	@Test
	void sixQuestsWithDistinctIdsAndRealRewards() {
		assertTrue(ALL.size() >= 6);
		Set<String> ids = new HashSet<>();
		for (Quest quest : ALL) {
			assertTrue(ids.add(quest.id()), "unique id " + quest.id());
			assertNotEquals(quest.trigger(), quest.objective(), quest.id());
			assertTrue(quest.xp() > 0, "vanilla experience helps a brand-new character: " + quest.id());
			assertTrue(quest.shards() + quest.runes() > 0, "an item reward helps an existing character: " + quest.id());
		}
		assertNull(byId("nope"));
	}

	@Test
	void stepMovesOneStageAtATime() {
		Quest q = quest("keeper_openings");
		assertEquals(UNKNOWN, step(q, UNKNOWN, Set.of()));
		assertEquals(UNKNOWN, step(q, UNKNOWN, Set.of(PERFECT_GUARD)), "the objective alone does not skip the clue");
		assertEquals(ACTIVE, step(q, UNKNOWN, Set.of(TOMB, PERFECT_GUARD)), "the clue comes first even when both are met");
		assertEquals(ACTIVE, step(q, ACTIVE, Set.of(TOMB)));
		assertEquals(DONE, step(q, ACTIVE, Set.of(PERFECT_GUARD)));
		assertEquals(DONE, step(q, DONE, Set.of()), "done stays done");
		assertEquals(DONE, step(q, 7, Set.of()), "a future stage number is treated as done");
		assertEquals(UNKNOWN, step(q, -3, Set.of()), "a negative stage is treated as unknown");
	}

	@Test
	void noSoftlockEveryActiveQuestCompletesOnceItsObjectiveIsSeen() {
		for (Quest quest : ALL) {
			Map<String, Integer> stages = new HashMap<>();
			if (!quest.prerequisite().isEmpty()) stages.put(quest.prerequisite(), DONE);
			Advance first = advance(stages, Set.of(quest.trigger(), quest.objective()));
			assertTrue(first.clued().contains(quest), quest.id());
			Advance second = advance(first.quests(), Set.of(quest.objective()));
			assertTrue(second.completed().contains(quest), quest.id());
			assertEquals(DONE, second.quests().get(quest.id()));
			Advance third = advance(second.quests(), Set.of(quest.trigger(), quest.objective()));
			assertFalse(third.completed().contains(quest), "the reward is paid once: " + quest.id());
		}
	}

	@Test
	void scoutChaptersRequirePreviousCompletionAndNeverCascadeWithinAPoll() {
		Set<String> facts = Set.of(MEMORIAL, TALK_GALE, WATCHTOWER, STANDING_STONES, SLEEPING_BLADE);
		Map<String, Integer> stages = Map.of();
		List<String> sequence = List.of("scout_wall", "last_cairn", "road_before_roads", "oath_at_rest");
		Set<String> paid = new HashSet<>();
		for (String id : sequence) {
			Advance clue = advance(stages, facts);
			assertEquals(List.of(quest(id)), clue.clued(), "one readable chapter, even on an experienced save");
			assertTrue(clue.completed().isEmpty());
			Advance done = advance(clue.quests(), facts);
			assertEquals(List.of(quest(id)), done.completed());
			assertTrue(done.clued().isEmpty(), "the next chapter waits for another poll");
			assertTrue(paid.add(id));
			stages = done.quests();
		}
		assertFalse(advance(stages, facts).changed(), "completed chain never pays again");
	}

	@Test
	void earlyDiscoveryIsRememberedButCannotSkipAChapter() {
		Set<String> facts = facts(List.of("aura:sleeping_blade"), List.of("place:travel_watchtower",
			"place:travel_standing_stones"), false);
		assertTrue(facts.containsAll(Set.of(WATCHTOWER, STANDING_STONES, SLEEPING_BLADE)));
		assertFalse(advance(Map.of(), facts).changed(), "regional visits alone do not invent the scout's story");
		Advance active = advance(Map.of("road_before_roads", DONE), facts);
		assertTrue(active.clued().contains(quest("oath_at_rest")));
		assertTrue(advance(active.quests(), facts).completed().contains(quest("oath_at_rest")), "earlier blade visit counts");
		assertTrue(advance(Map.of("oath_at_rest", ACTIVE), Set.of(SLEEPING_BLADE)).completed().contains(quest("oath_at_rest")),
			"an active chapter can recover even if an old save lacks its prerequisite");
	}

	@Test
	void drownedRoadKeepsEarlierVisitsButRequiresTheReedkeepersWholeAccount() {
		Set<String> places = facts(List.of(), List.of("place:water_pier_boathouse", "place:water_lighthouse",
			"learned:coast_light", "said:reedkeeper.crossing"), false);
		assertFalse(advance(Map.of(), places).changed(), "lamps and visits alone cannot invent the boatman's account");
		Set<String> heard = new HashSet<>(places);
		heard.addAll(facts(List.of(), List.of("person:reedkeeper"), false));
		Advance first = advance(Map.of(), heard);
		assertEquals(List.of(quest("drowned_road")), first.clued());
		assertFalse(advance(first.quests(), heard).changed(), "greeting and crossing are not the final Ferryman story");
		heard.addAll(facts(List.of(), List.of("said:reedkeeper.ferryman"), false));
		Map<String, Integer> stages = first.quests();
		for (String id : List.of("drowned_road", "second_count", "last_boat_light")) {
			if (!id.equals("drowned_road")) {
				Advance clue = advance(stages, heard);
				assertEquals(List.of(quest(id)), clue.clued());
				assertTrue(clue.completed().isEmpty());
				stages = clue.quests();
			}
			Advance done = advance(stages, heard);
			assertEquals(List.of(quest(id)), done.completed(), "earlier discoveries count in order");
			assertTrue(done.clued().isEmpty(), "no chapter cascade in one poll");
			stages = done.quests();
		}
		assertFalse(advance(stages, heard).changed(), "a completed coastal route never pays again");
		assertFalse(facts(List.of(), List.of("place:water_lighthouse"), false).contains(COAST_LIGHT), "a dark tower is not a working lamp");
	}

	@Test
	void chapterGraphHasExistingAcyclicPrerequisites() {
		for (Quest quest : ALL) {
			Set<String> chain = new HashSet<>();
			Quest cursor = quest;
			while (!cursor.prerequisite().isEmpty()) {
				assertTrue(chain.add(cursor.id()), "cycle at " + cursor.id());
				cursor = byId(cursor.prerequisite());
				assertNotNull(cursor, "all prerequisite quests exist");
			}
		}
	}

	@Test
	void advanceReportsOnlyChanges() {
		Advance none = advance(Map.of(), Set.of());
		assertFalse(none.changed());
		Advance clue = advance(Map.of(), Set.of(TOURNAMENT));
		assertEquals(List.of(quest("different_hands")), clue.clued());
		assertTrue(clue.completed().isEmpty());
		assertEquals(quest("different_hands"), firstActive(clue.quests()));
		assertNull(firstActive(Map.of()));
	}

	@Test
	void factsComeFromGrimoireJournalAndTraining() {
		Set<String> facts = facts(List.of("aura:battlefield_broken_line", "aura:sword_tomb", "aura:tournament", "creature:minecraft:blaze",
			"reaction:shatter", "aura:perfect_guard", "aura:duelist"), List.of(), false);
		assertTrue(facts.containsAll(Set.of(MEMORIAL, TOMB, TOURNAMENT, FIELD_GUIDE, REACTION, PERFECT_GUARD, DUEL_WON)));
		assertFalse(facts.contains(TRAINED));
		Set<String> journal = facts(List.of(), List.of("duelist:ember", "duelist:gale", "duelist:stone", "master:ember", "place:training"), false);
		assertTrue(journal.containsAll(Set.of(TALK_EMBER, TALK_GALE, TALK_STONE, MASTER_MET, TRAINED)));
		assertTrue(facts(List.of(), List.of("victory:gale"), false).contains(MASTER_MET), "a Master already beaten counts as met");
		assertTrue(facts(List.of(), List.of("won:rime"), false).contains(DUEL_WON));
		assertTrue(facts(List.of(), List.of(), true).contains(TRAINED));
		assertTrue(facts(List.of("hint:x", "aura:way"), List.of("said:duelist.ember.greet"), false).isEmpty());
	}

	@Test
	void entriesAndLinesAreStable() {
		assertEquals("place:memorial", LoreEntries.fromGrimoire("aura:battlefield_last_shelter"));
		assertEquals("place:tomb", LoreEntries.fromGrimoire("aura:sword_tomb"));
		assertNull(LoreEntries.fromGrimoire("hint:sunfall"));
		assertEquals("duelist.ember.greet", LoreEntries.line("duelist", "ember", "greet"));
		assertEquals("master.any.hint", LoreEntries.line("master", "addon:frost_lotus", "hint"), "unknown methods use the shared lines");
		assertEquals("addon.frost_lotus", LoreEntries.clean("Addon:Frost_Lotus"));
		assertEquals("talk", LoreEntries.tab("said:master.gale.victory"));
		assertEquals("people", LoreEntries.tab("victory:stone"));
		assertEquals("places", LoreEntries.tab("place:tomb"));
		assertEquals("learned", LoreEntries.tab("form:wall_turn"));
		assertEquals(16, LoreEntries.METHODS.size());
	}

	@Test
	void everyKeyHasGeneratedEnglish() throws IOException {
		JsonObject lang = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/wildercord/lang/en_us.json"),
			StandardCharsets.UTF_8)).getAsJsonObject();
		for (Quest quest : ALL) {
			for (String part : List.of("title", "clue", "objective", "hint", "done")) {
				String text = lang.has(quest.key(part)) ? lang.get(quest.key(part)).getAsString() : "";
				assertFalse(text.isBlank(), quest.key(part));
				assertTrue(text.length() <= 140, "short text: " + quest.key(part));
			}
		}
		for (String method : new java.util.ArrayList<>(List.of(LoreEntries.ANY)) {{ addAll(LoreEntries.METHODS); }}) {
			for (String role : LoreEntries.ROLES) for (String kind : LoreEntries.KINDS) {
				String key = LoreEntries.lineKey(role + "." + method + "." + kind);
				assertTrue(lang.has(key), key);
				assertTrue(lang.get(key).getAsString().length() <= 90, "a line fits one chat line or two: " + key);
			}
		}
		for (String entry : List.of("place:memorial", "place:tomb", "place:tournament", "place:sleeping_blade", "place:crossroads", "place:training",
			"place:highland_cairn_lodge", "place:highland_survey_ring", "person:cairn_keeper",
			"place:belowkeeper_listening_station", "place:belowkeeper_bell_sanctum", "person:belowkeeper_listener",
			"place:emberwood_charcoal_yard", "place:emberwood_kiln_court", "person:kiln_keeper",
			"learned:way", "learned:lineage", "learned:perfect_guard", "learned:technique", "learned:reaction", "learned:field_guide",
			"learned:scout_wind_mark", "learned:glass_cartographer", "learned:belowkeeper_shutter", "learned:hollow_bell",
			"learned:emberwood_draft", "learned:kiln_widow")) {
			assertTrue(lang.has(LoreEntries.fixedKey(entry)), entry);
		}
		assertTrue(lang.has("key.wildercord.lore_journal"));
		assertTrue(lang.has("key.category.wildercord.lore"));
	}

	@Test
	void highlandCairnRouteKeepsEarlierVisitsAndNeverCascadesWithinAPoll() {
		Set<String> places = facts(List.of(), List.of("place:travel_watchtower", "place:highland_cairn_lodge",
			"learned:scout_wind_mark", "said:cairn_keeper.cairns"), false);
		assertFalse(advance(Map.of(), places).changed(), "lamps and visits alone cannot invent the Cairn Keeper's account");
		Set<String> heard = new HashSet<>(places);
		heard.addAll(facts(List.of(), List.of("person:cairn_keeper"), false));
		Advance first = advance(Map.of(), heard);
		assertEquals(List.of(quest("paired_cairns")), first.clued());
		assertFalse(advance(first.quests(), heard).changed(), "greeting and cairns are not the final survey ring story");
		heard.addAll(facts(List.of(), List.of("said:cairn_keeper.cartographer"), false));
		Map<String, Integer> stages = first.quests();
		for (String id : List.of("paired_cairns", "ridge_orders", "cairn_beacon")) {
			if (!id.equals("paired_cairns")) {
				Advance clue = advance(stages, heard);
				assertEquals(List.of(quest(id)), clue.clued());
				assertTrue(clue.completed().isEmpty());
				stages = clue.quests();
			}
			Advance done = advance(stages, heard);
			assertEquals(List.of(quest(id)), done.completed(), "earlier highland discoveries count in order");
			assertTrue(done.clued().isEmpty(), "no chapter cascade in one poll");
			stages = done.quests();
		}
		assertFalse(advance(stages, heard).changed(), "a completed highland route never pays again");
		assertFalse(facts(List.of(), List.of("place:highland_cairn_lodge"), false).contains(CAIRN_LAMP), "an unlit cairn lodge is not a lit cairn lamp");
	}

	@Test
	void belowkeeperListeningRouteKeepsEarlierVisitsAndNeverCascadesWithinAPoll() {
		Set<String> places = facts(List.of(), List.of("place:mine_collapsed_delve", "place:belowkeeper_listening_station",
			"learned:belowkeeper_shutter", "said:belowkeeper_listener.pressure"), false);
		assertFalse(advance(Map.of(), places).changed(), "shutters and delve visits alone cannot invent the Belowkeeper Listener's account");
		Set<String> heard = new HashSet<>(places);
		heard.addAll(facts(List.of(), List.of("person:belowkeeper_listener"), false));
		Advance first = advance(Map.of(), heard);
		assertEquals(List.of(quest("empty_bell_kept")), first.clued());
		assertFalse(advance(first.quests(), heard).changed(), "greeting and pressure are not the final Bell Sanctum story");
		heard.addAll(facts(List.of(), List.of("said:belowkeeper_listener.hollow_bell"), false));
		Map<String, Integer> stages = first.quests();
		for (String id : List.of("empty_bell_kept", "pressure_records", "resonance_shutter")) {
			if (!id.equals("empty_bell_kept")) {
				Advance clue = advance(stages, heard);
				assertEquals(List.of(quest(id)), clue.clued());
				assertTrue(clue.completed().isEmpty());
				stages = clue.quests();
			}
			Advance done = advance(stages, heard);
			assertEquals(List.of(quest(id)), done.completed(), "earlier delve and shutter discoveries count in order");
			assertTrue(done.clued().isEmpty(), "no chapter cascade in one poll");
			stages = done.quests();
		}
		assertFalse(advance(stages, heard).changed(), "a completed belowkeeper route never pays again");
		assertFalse(facts(List.of(), List.of("place:belowkeeper_listening_station"), false).contains(BELOWKEEPER_SHUTTER), "an unlit listening station is not a lit resonance shutter");
	}

	@Test
	void emberwoodKilnRouteKeepsEarlierVisitsAndNeverCascadesWithinAPoll() {
		Set<String> places = facts(List.of(), List.of("place:mine_forge_hall", "place:emberwood_charcoal_yard",
			"learned:emberwood_draft", "said:kiln_keeper.firebreaks"), false);
		assertFalse(advance(Map.of(), places).changed(), "draft lamps and forge visits alone cannot invent the Kiln Keeper's account");
		Set<String> heard = new HashSet<>(places);
		heard.addAll(facts(List.of(), List.of("person:kiln_keeper"), false));
		Advance first = advance(Map.of(), heard);
		assertEquals(List.of(quest("eleven_winters")), first.clued());
		assertFalse(advance(first.quests(), heard).changed(), "greeting and firebreaks are not the final Kiln Court story");
		heard.addAll(facts(List.of(), List.of("said:kiln_keeper.kiln_widow"), false));
		Map<String, Integer> stages = first.quests();
		for (String id : List.of("eleven_winters", "forge_records", "kiln_draft_lamp")) {
			if (!id.equals("eleven_winters")) {
				Advance clue = advance(stages, heard);
				assertEquals(List.of(quest(id)), clue.clued());
				assertTrue(clue.completed().isEmpty());
				stages = clue.quests();
			}
			Advance done = advance(stages, heard);
			assertEquals(List.of(quest(id)), done.completed(), "earlier forge and draft discoveries count in order");
			assertTrue(done.clued().isEmpty(), "no chapter cascade in one poll");
			stages = done.quests();
		}
		assertFalse(advance(stages, heard).changed(), "a completed emberwood route never pays again");
		assertFalse(facts(List.of(), List.of("place:emberwood_charcoal_yard"), false).contains(EMBERWOOD_DRAFT), "an unlit charcoal yard is not a lit kiln draft lamp");
	}

	@Test
	void fourLightsQuestNeedsEveryLampThenEveryKeeper() {
		List<String> lamps = List.of("learned:coast_light", "learned:scout_wind_mark", "learned:belowkeeper_shutter", "learned:emberwood_draft");
		for (String missing : lamps) {
			List<String> three = lamps.stream().filter(l -> !l.equals(missing)).toList();
			assertFalse(facts(List.of(), three, false).contains(FOUR_LIGHTS), "three lamps are not four");
		}
		Set<String> lit = facts(List.of(), lamps, false);
		assertTrue(lit.contains(FOUR_LIGHTS));
		Quest quest = quest("four_lights");
		assertEquals(ACTIVE, step(quest, UNKNOWN, lit));
		List<String> answers = lightAnswers();
		assertEquals(4, answers.size());
		for (int i = 0; i < answers.size() - 1; i++) {
			List<String> heard = new java.util.ArrayList<>(lamps);
			heard.addAll(answers.subList(0, i + 1));
			assertFalse(facts(List.of(), heard, false).contains(LIGHTS_ANSWERED), "every keeper must answer");
		}
		List<String> all = new java.util.ArrayList<>(lamps);
		all.addAll(answers);
		assertEquals(DONE, step(quest, ACTIVE, facts(List.of(), all, false)));
		assertTrue(quest.shards() > 0 && quest.xp() > 0);
	}
}
