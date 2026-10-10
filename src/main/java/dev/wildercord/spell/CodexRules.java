package dev.wildercord.spell;

import java.util.Map;
import java.util.TreeMap;

/**
 * The codex bestiary (0.13): the field guide's tally of every creature a player has slain. Each kind of creature in the
 * {@link FieldGuide} counts its kills, and the count earns it a rank that tells the player more about it: slain once, its
 * tally shows; studied after ten, its health, armour and bite; mastered after thirty, its name is written in gold. It is
 * knowledge only: no rank makes anyone hit harder, so the 0.12 pace is untouched. Pure, shared by the server (which counts)
 * and the Grimoire page (which reads).
 */
public final class CodexRules {
	private CodexRules() {}

	public static final int STUDIED = 10;
	public static final int MASTERED = 30;
	/** A tally stops here: past it there is nothing more to learn, and the number stays small in the save. */
	public static final int CAP = 9999;
	/** At most this many kinds are tallied, so a modpack full of creatures can't grow a save without end. */
	public static final int MAX_KINDS = 512;

	public enum Rank {
		UNSLAIN("unslain"), SLAIN("slain"), STUDIED("studied"), MASTERED("mastered");

		/** Its language key's last part ({@code screen.wildercord.grimoire.codex_<key>}). */
		public final String key;

		Rank(String key) {
			this.key = key;
		}
	}

	public static Rank rank(int kills) {
		if (kills >= MASTERED) return Rank.MASTERED;
		if (kills >= STUDIED) return Rank.STUDIED;
		return kills > 0 ? Rank.SLAIN : Rank.UNSLAIN;
	}

	/** Kills still wanted for the next rank, or 0 at the last. */
	public static int toNext(int kills) {
		if (kills >= MASTERED) return 0;
		return (kills >= STUDIED ? MASTERED : kills > 0 ? STUDIED : 1) - Math.max(0, kills);
	}

	/** The tally with one more kill of {@code type}. Only creatures in the field guide count, and none past the cap. */
	public static Map<String, Integer> slay(Map<String, Integer> tally, String type) {
		if (FieldGuide.byType(type).isEmpty()) return tally;
		int now = tally.getOrDefault(type, 0);
		if (now >= CAP || (now == 0 && tally.size() >= MAX_KINDS)) return tally;
		Map<String, Integer> next = new TreeMap<>(tally);
		next.put(type, now + 1);
		return next;
	}

	/** Whether this kill just earned a new rank (worth telling the player). */
	public static boolean rankedUp(int before, int after) {
		return rank(before) != rank(after);
	}
}
