package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * What a player may know of their world's magic, and nothing more: the one place that decides what leaves the server.
 * A client is sent these views, never the world's resonances or quirks themselves.
 *
 * <ul>
 *   <li>A resonance the player <b>found</b>: everything (its runes, riddle, twist, colour, and who found it first).</li>
 *   <li>One whose riddle they've <b>read</b> (a Torn Page): its name and riddle, never its runes or its twist.</li>
 *   <li>One somebody else found, which the world <b>announced</b>: its name and its finder, nothing else.</li>
 *   <li>Anything else: not even that it exists, beyond how many the world holds in all.</li>
 * </ul>
 *
 * <p>Quirks go the same way: a player is told only those already in their Grimoire.</p>
 */
public final class ResonanceLore {
	private ResonanceLore() {}

	/**
	 * One resonance as a player may see it. Fields a player mayn't know are empty.
	 *
	 * @param runes  its rune ids, only if they found it
	 * @param riddle only if they found it or read it
	 * @param twist  its twist's id, only if they found it
	 * @param finder who found it first in this world ("" if nobody, or if the world doesn't say)
	 */
	public record View(String id, String name, String riddle, List<String> runes, String twist, int color, boolean found, boolean hinted,
			String finder) {
		public View {
			runes = List.copyOf(runes);
		}

		/** Whether these are exactly its runes: only ever true of one the player found. */
		public boolean matchesIds(List<String> ids) {
			return found && runes.equals(ids);
		}
	}

	/** One quirk a player has met: its id and what the Grimoire says. */
	public record QuirkView(String id, String text) {}

	/**
	 * What a player with {@code grimoire} may know of {@code world}'s resonances, given who found which first
	 * ({@code finders}: resonance id to the finder's name; only resonances somebody has found are in it).
	 */
	public static List<View> views(List<Resonance> world, Collection<String> grimoire, Map<String, String> finders) {
		List<View> views = new ArrayList<>();
		for (Resonance resonance : world) {
			boolean found = grimoire.contains(resonance.key());
			boolean hinted = grimoire.contains(resonance.hintKey());
			String finder = finders.getOrDefault(resonance.id(), "");
			if (found) {
				views.add(new View(resonance.id(), resonance.name(), resonance.riddle(), resonance.runes(), resonance.twist(), resonance.color(), true,
					hinted, finder));
			} else if (hinted) {
				views.add(new View(resonance.id(), resonance.name(), resonance.riddle(), List.of(), "", resonance.color(), false, true, finder));
			} else if (!finder.isEmpty()) {
				views.add(new View(resonance.id(), resonance.name(), "", List.of(), "", resonance.color(), false, false, finder));
			}
		}
		return List.copyOf(views);
	}

	/** The quirks of {@code world} already in {@code grimoire}. */
	public static List<QuirkView> quirks(List<RuneQuirks.Quirk> world, Collection<String> grimoire) {
		List<QuirkView> views = new ArrayList<>();
		for (RuneQuirks.Quirk quirk : world) {
			if (grimoire.contains(quirk.key())) {
				views.add(new QuirkView(quirk.id(), quirk.text()));
			}
		}
		return List.copyOf(views);
	}
}
