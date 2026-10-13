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
			"learned:way", "learned:lineage", "learned:perfect_guard", "learned:technique", "learned:reaction", "learned:field_guide")) {
			assertTrue(lang.has(LoreEntries.fixedKey(entry)), entry);
		}
		assertTrue(lang.has("key.wildercord.lore_journal"));
		assertTrue(lang.has("key.category.wildercord.lore"));
	}
}
