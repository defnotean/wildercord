package dev.wildercord.spell;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;

/** The deliberately small, Minecraft-free contract of the Archive's Relay lesson. */
public final class RelayRules {
	private RelayRules() {}

	public static final String ID = "wildercord:relay";
	public static final int BASE_MANA = 24;
	public static final double STRENGTH = 0.9;
	public static final int REST_TICKS = 160;
	public static final int FOCUS_TICKS = 80;
	public static final int WARN_TICKS = 6;
	public static final int RECOVERY_TICKS = 10;
	public static final double MAX_PATH = 16;
	public static final double PLACE_RANGE = 8;
	public static final String GRAMMAR_PROBLEM = "Relay needs exactly Relay then Harm, Frost or Shock in one active spell: no modifiers, links, extra groups, Knots or woven runes.";
	public static final String STORAGE_PROBLEM = "Relay is an active Archive lesson: it cannot be stored, imbued, inscribed on a scroll or sustained as a passive.";

	/** Only the two original registered runes are accepted; no expanded or altered definitions. */
	public static boolean valid(List<RuneDef> runes) {
		return runes.size() == 2 && runes.getFirst().equals(Runes.RELAY)
			&& (runes.get(1).equals(Runes.HARM) || runes.get(1).equals(Runes.FROST) || runes.get(1).equals(Runes.SHOCK));
	}

	/** A non-Relay spell has no Relay grammar problem. */
	public static String problem(List<RuneDef> runes) {
		return contains(runes) && !valid(runes) ? GRAMMAR_PROBLEM : null;
	}

	public static boolean contains(List<RuneDef> runes) {
		return containsIds(runes.stream().map(RuneDef::id).toList());
	}

	/** A bounded saved row must not lose Relay (or forbidden suffixes) and become another castable spell. */
	public static List<String> boundedIds(List<String> ids, int maximum) {
		if (maximum < 0) throw new IllegalArgumentException("Negative rune limit");
		if (ids.size() <= maximum) return List.copyOf(ids);
		if (containsIds(ids)) return maximum == 0 ? List.of() : List.of(ID);
		return List.copyOf(ids.subList(0, maximum));
	}

	/**
	 * Checks original ids before any unknown-rune filtering. Knot decoding does not resolve its
	 * siblings or stop at the compiler's nesting limit, so an unknown sibling or overdeep wrapper
	 * cannot hide Relay. Woven ids are inspected too, including forged weaves containing a shape.
	 * Unreadable/oversized composites fail closed: callers must refuse them, not assume absence.
	 */
	public static boolean containsIds(List<String> ids) {
		ArrayDeque<String> pending = new ArrayDeque<>();
		for (String id : ids) {
			if (id == null) return true;
			pending.add(id);
		}
		int remaining = Knots.MAX_ID_LENGTH * 8;
		while (!pending.isEmpty()) {
			String id = pending.removeFirst();
			if (ID.equals(id)) return true;
			boolean knot = Knots.isKnot(id);
			boolean woven = WovenRunes.isWoven(id);
			if (!knot && !woven) continue;
			if (id.length() > Knots.MAX_ID_LENGTH || (remaining -= id.length()) < 0) return true;
			String prefix = knot ? Knots.PREFIX : WovenRunes.PREFIX;
			byte[] bytes = Knots.decode(id.substring(prefix.length()));
			if (bytes == null) return true;
			String body = new String(bytes, StandardCharsets.UTF_8);
			if (knot && body.contains("|")) body = body.substring(0, body.indexOf('|'));
			if (body.isEmpty()) return true;
			for (String part : body.split(knot ? "," : "\n", -1)) {
				if (part.isEmpty()) return true;
				pending.addLast(knot && !part.contains(":") ? "wildercord:" + part : part);
			}
		}
		return false;
	}
}
