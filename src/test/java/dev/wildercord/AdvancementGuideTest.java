package dev.wildercord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Player-facing advancement totals and named rows stay tied to shipped data, including new regions. */
class AdvancementGuideTest {
	@Test
	void everyVisibleAdvancementAndRewardTotalAppearsInBothGuides() throws Exception {
		var language = json(Path.of("src/main/resources/assets/wildercord/lang/en_us.json"));
		var root = Path.of("src/main/resources/data/wildercord");
		var frames = new HashMap<String, Integer>();
		var items = new HashMap<String, Integer>();
		var names = new java.util.ArrayList<String>();
		int xp = 0, hidden = 0;
		try (var paths = Files.walk(root.resolve("advancement"))) {
			for (var path : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
				var advancement = json(path);
				if (!advancement.has("display")) continue;
				var display = advancement.getAsJsonObject("display");
				var title = display.getAsJsonObject("title");
				names.add(title.has("translate") ? language.get(title.get("translate").getAsString()).getAsString() : title.get("text").getAsString());
				frames.merge(display.has("frame") ? display.get("frame").getAsString() : "task", 1, Integer::sum);
				if (display.has("hidden") && display.get("hidden").getAsBoolean()) hidden++;
				if (!advancement.has("rewards")) continue;
				var rewards = advancement.getAsJsonObject("rewards");
				if (rewards.has("experience")) xp += rewards.get("experience").getAsInt();
				if (!rewards.has("loot")) continue;
				for (var loot : rewards.getAsJsonArray("loot")) {
					String id = loot.getAsString();
					assertTrue(id.startsWith("wildercord:advancement_reward/"), "Review new reward-table types before publishing totals");
					for (var pool : json(root.resolve("loot_table/" + id.substring("wildercord:".length()) + ".json")).getAsJsonArray("pools")) {
						var p = pool.getAsJsonObject();
						assertEquals(1, p.getAsJsonArray("entries").size(), "Only deterministic item rewards can be summed");
						var entry = p.getAsJsonArray("entries").get(0).getAsJsonObject();
						assertEquals("minecraft:item", entry.get("type").getAsString());
						int count = entry.has("modifier") ? entry.getAsJsonObject("modifier").get("count").getAsInt() : 1;
						items.merge(entry.get("name").getAsString(), p.get("rolls").getAsInt() * count, Integer::sum);
					}
				}
			}
		}
		for (String edition : new String[] {"wiki", "gitbook"}) {
			String text = Files.readString(Path.of(edition, "progression/advancements.md"));
			for (String name : names) assertTrue(text.contains("| **" + name + "** |"), edition + " omits " + name);
			assertTrue(text.contains(names.size() + " visible advancements, counting the root."));
			for (var pair : Map.of("Tasks", "task", "Goals", "goal", "Challenges", "challenge").entrySet()) row(text, pair.getKey(), frames.get(pair.getValue()));
			row(text, "Hidden", hidden);
			row(text, "Experience for all of them", xp);
			row(text, "Mana Crystals", items.get("wildercord:mana_crystal"));
			assertTrue(text.contains("| Blank Runes | " + items.get("wildercord:blank_rune") + " "));
		}
	}

	private static void row(String text, String label, int value) {
		assertTrue(text.contains("| " + label + " | " + String.format(Locale.ROOT, "%,d", value) + " |"), "Stale published total: " + label);
	}

	private static JsonObject json(Path path) throws Exception {
		return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
	}
}
