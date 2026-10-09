package dev.wildercord.lore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The discovery quests, as a pure state machine. Each quest is UNKNOWN until its trigger fact is seen,
 * then ACTIVE (the clue is in the journal), then DONE once its objective fact is seen (the reward is paid
 * exactly once, on that step). Every fact can still be earned later, and a fact earned before the clue
 * completes the quest on the next step, so no quest can get stuck.
 */
public final class LoreQuestRules {
	private LoreQuestRules() {}

	public static final int UNKNOWN = 0, ACTIVE = 1, DONE = 2;

	// Facts, derived from the Grimoire, the journal and the player's state (see facts()).
	public static final String MEMORIAL = "memorial";
	public static final String TOMB = "tomb";
	public static final String TOURNAMENT = "tournament";
	public static final String FIELD_GUIDE = "field_guide";
	public static final String REACTION = "reaction";
	public static final String PERFECT_GUARD = "perfect_guard";
	public static final String DUEL_WON = "duel_won";
	public static final String TALK_EMBER = "talk_ember";
	public static final String TALK_GALE = "talk_gale";
	public static final String TALK_STONE = "talk_stone";
	public static final String TRAINED = "trained";
	public static final String MASTER_MET = "master_met";

	/** A quest: what starts it, what finishes it, and what it pays (vanilla experience, Aura Shards, Blank Runes, Aura experience). */
	public record Quest(String id, String trigger, String objective, int xp, int shards, int runes, int auraXp) {
		public String key(String part) {
			return "journal.wildercord.quest." + id + "." + part;
		}
	}

	public static final List<Quest> ALL = List.of(
		new Quest("scout_wall", MEMORIAL, TALK_GALE, 30, 2, 0, 40),
		new Quest("kiln_gate", TALK_EMBER, TRAINED, 30, 2, 0, 60),
		new Quest("keeper_openings", TOMB, PERFECT_GUARD, 40, 3, 0, 60),
		new Quest("warden_threshold", FIELD_GUIDE, REACTION, 30, 0, 2, 0),
		new Quest("different_hands", TOURNAMENT, DUEL_WON, 30, 2, 1, 40),
		new Quest("masters_ledger", TALK_STONE, MASTER_MET, 50, 3, 1, 80));

	public static Quest byId(String id) {
		for (Quest quest : ALL) if (quest.id().equals(id)) return quest;
		return null;
	}

	/** One step for one quest. At most one stage per step, so the clue is always seen before the reward. */
	public static int step(Quest quest, int stage, Set<String> facts) {
		if (stage >= DONE) return DONE;
		if (stage <= UNKNOWN) return facts.contains(quest.trigger()) ? ACTIVE : UNKNOWN;
		return facts.contains(quest.objective()) ? DONE : ACTIVE;
	}

	/** The result of a step over every quest: the new stages, and which quests were clued or completed by it. */
	public record Advance(Map<String, Integer> quests, List<Quest> clued, List<Quest> completed) {
		public boolean changed() {
			return !clued.isEmpty() || !completed.isEmpty();
		}
	}

	public static Advance advance(Map<String, Integer> stages, Set<String> facts) {
		Map<String, Integer> next = new HashMap<>(stages);
		List<Quest> clued = new ArrayList<>(), completed = new ArrayList<>();
		for (Quest quest : ALL) {
			int before = stages.getOrDefault(quest.id(), UNKNOWN);
			int after = step(quest, before, facts);
			if (after == before) continue;
			next.put(quest.id(), after);
			if (after == ACTIVE) clued.add(quest);
			else if (after == DONE) completed.add(quest);
		}
		return new Advance(next, clued, completed);
	}

	/** The oldest active quest in list order, for a teacher's repeated hint; null if none is active. */
	public static Quest firstActive(Map<String, Integer> stages) {
		for (Quest quest : ALL) if (stages.getOrDefault(quest.id(), UNKNOWN) == ACTIVE) return quest;
		return null;
	}

	/**
	 * Facts from what is already recorded: Grimoire keys, journal entries, and whether the player is breathing
	 * at a training ground right now. Old characters get every fact they have already earned.
	 */
	public static Set<String> facts(Collection<String> grimoire, Collection<String> journal, boolean trainingNow) {
		Set<String> facts = new HashSet<>();
		for (String key : grimoire) {
			if (key.startsWith("aura:battlefield")) facts.add(MEMORIAL);
			else if (key.equals("aura:sword_tomb")) facts.add(TOMB);
			else if (key.equals("aura:tournament")) facts.add(TOURNAMENT);
			else if (key.startsWith("creature:")) facts.add(FIELD_GUIDE);
			else if (key.startsWith("reaction:")) facts.add(REACTION);
			else if (key.equals("aura:perfect_guard")) facts.add(PERFECT_GUARD);
			else if (key.equals("aura:duelist")) facts.add(DUEL_WON);
		}
		for (String entry : journal) {
			switch (entry) {
				case "place:memorial" -> facts.add(MEMORIAL);
				case "place:tomb" -> facts.add(TOMB);
				case "place:tournament" -> facts.add(TOURNAMENT);
				case "place:training" -> facts.add(TRAINED);
				case "duelist:ember" -> facts.add(TALK_EMBER);
				case "duelist:gale" -> facts.add(TALK_GALE);
				case "duelist:stone" -> facts.add(TALK_STONE);
				default -> {
					if (entry.startsWith("master:") || entry.startsWith("victory:")) facts.add(MASTER_MET);
					else if (entry.startsWith("won:")) facts.add(DUEL_WON);
				}
			}
		}
		if (trainingNow) facts.add(TRAINED);
		return facts;
	}
}
