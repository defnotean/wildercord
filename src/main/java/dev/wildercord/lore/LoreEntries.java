package dev.wildercord.lore;

import java.util.List;
import java.util.Locale;

/**
 * Journal entry ids and conversation line keys, pure so they can be tested.
 *
 * <p>Entry ids: {@code place:<id>}, {@code learned:<id>} (fixed text), {@code method:<method>}, {@code stage:<stage>},
 * {@code form:<form>}, {@code duelist:<method>} (met), {@code won:<method>} (duel won), {@code master:<method>} (met),
 * {@code victory:<school>} (trial won) and {@code said:<line>} (a conversation line, kept so it can be read again).
 */
public final class LoreEntries {
	private LoreEntries() {}

	/** Every method with its own lines: the ten first breaths and the six newer ones. Any other id uses the shared lines. */
	public static final List<String> METHODS = List.of("ember", "rime", "thunder", "gale", "stone", "verdant", "hollow", "starlit",
		"hourglass", "crimson", "tide", "iron", "dune", "echo", "dawn", "venom");
	public static final String ANY = "any";
	public static final List<String> KINDS = List.of("greet", "hint", "victory");
	public static final List<String> ROLES = List.of("duelist", "master");

	/** A method id as it appears in keys: lower case, a namespace's colon as a dot. */
	public static String clean(String method) {
		return method == null ? ANY : method.toLowerCase(Locale.ROOT).replace(':', '.');
	}

	/** The method whose lines are spoken: its own if it has them, else the shared ones. */
	public static String voice(String method) {
		String clean = clean(method);
		return METHODS.contains(clean) ? clean : ANY;
	}

	/** The id stored after {@code said:}: {@code duelist.ember.greet}. */
	public static String line(String role, String method, String kind) {
		return role + "." + voice(method) + "." + kind;
	}

	public static String lineKey(String line) {
		return "dialogue.wildercord." + line;
	}

	/** The journal entry a Grimoire discovery writes, or null if it has none. */
	public static String fromGrimoire(String key) {
		if (key.startsWith("aura:battlefield")) return "place:memorial";
		return switch (key) {
			case "aura:sword_tomb" -> "place:tomb";
			case "aura:tournament" -> "place:tournament";
			case "aura:sleeping_blade" -> "place:sleeping_blade";
			case "aura:crossroads" -> "place:crossroads";
			case "aura:way" -> "learned:way";
			case "aura:lineage" -> "learned:lineage";
			case "aura:perfect_guard" -> "learned:perfect_guard";
			case "aura:technique" -> "learned:technique";
			default -> key.startsWith("reaction:") ? "learned:reaction" : key.startsWith("creature:") ? "learned:field_guide" : null;
		};
	}

	/** Entries with fixed text: their language key. */
	public static String fixedKey(String entry) {
		return "journal.wildercord.entry." + entry.replace(':', '.');
	}

	/** The tab an entry is listed under. */
	public static String tab(String entry) {
		if (entry.startsWith("said:")) return "talk";
		if (entry.startsWith("place:")) return "places";
		if (entry.startsWith("duelist:") || entry.startsWith("master:") || entry.startsWith("won:") || entry.startsWith("victory:")) return "people";
		return "learned";
	}
}
