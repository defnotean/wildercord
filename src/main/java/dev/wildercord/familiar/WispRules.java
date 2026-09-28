package dev.wildercord.familiar;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The rules for wisps and familiars, with no Minecraft in them: taming progress, the food each
 * element takes, levels and what each level gives. The server, the lantern's tooltip and the unit
 * tests all read the same numbers.
 *
 * <p>A wild wisp answers only to its own element. Strike it with a spell of that element (or feed
 * it the matching food) {@link #TAMING_HITS} times, each at least {@link #MIN_GAP_TICKS} apart and
 * none more than {@link #FORGET_TICKS} after the last, and it bonds with you. Magic of any other
 * element spooks it and it forgets everything; so does someone else trying.</p>
 */
public final class WispRules {
	private WispRules() {}

	/** The elements wisps come in, in the order the lantern lists them. */
	public static final List<String> ELEMENTS = List.of("fire", "frost", "storm", "wind", "earth", "life", "void", "arcane");

	/** Offerings of its element before a wild wisp bonds. */
	public static final int TAMING_HITS = 3;
	/** One cast can't count twice: offerings closer than this are ignored (a Zone's pulses, a Volley). */
	public static final int MIN_GAP_TICKS = 10;
	/** A wisp forgets a half-finished taming after this long (a minute). */
	public static final int FORGET_TICKS = 1200;
	/** Familiars one player can keep bonded at once (out, in the lantern, or waiting at a Wellstone). */
	public static final int MAX_BONDS = 12;

	/** Experience a familiar needs for each level: level 1 from the start, then 40 and 150. */
	private static final int[] LEVEL_XP = {0, 40, 150};
	public static final int MAX_LEVEL = LEVEL_XP.length;

	/** What an offering did. */
	public enum Outcome { IGNORED, PROGRESS, BONDED, SPOOKED }

	/** A wild wisp's taming so far: who is taming it, how many offerings, and when the last one was. */
	public record Taming(String player, int hits, long last) {
		public static final Taming NONE = new Taming("", 0, 0);
	}

	public record Step(Taming taming, Outcome outcome) {}

	/**
	 * A player offers a wisp magic (or food) of {@code offered}. Returns the wisp's new taming state
	 * and what happened.
	 */
	public static Step offer(Taming taming, String player, String offered, String element, long now) {
		if (offered == null || offered.isEmpty()) {
			return new Step(taming, Outcome.IGNORED);
		}
		if (!offered.equals(element)) {
			return new Step(Taming.NONE, Outcome.SPOOKED);
		}
		boolean fresh = !taming.player().equals(player) || taming.hits() == 0 || now - taming.last() > FORGET_TICKS;
		if (!fresh && now - taming.last() < MIN_GAP_TICKS) {
			return new Step(taming, Outcome.IGNORED);
		}
		int hits = fresh ? 1 : taming.hits() + 1;
		Taming next = new Taming(player, hits, now);
		return new Step(next, hits >= TAMING_HITS ? Outcome.BONDED : Outcome.PROGRESS);
	}

	/** Whether a half-finished taming has been forgotten by now. */
	public static boolean forgotten(Taming taming, long now) {
		return taming.hits() > 0 && now - taming.last() > FORGET_TICKS;
	}

	// ------------------------------------------------------------------ food

	/** The food each element's wisp takes (item ids), as an alternative to a spell. */
	private static final Map<String, String> FOOD = Map.of(
		"fire", "minecraft:blaze_powder",
		"frost", "minecraft:snowball",
		"storm", "minecraft:glowstone_dust",
		"wind", "minecraft:feather",
		"earth", "minecraft:clay_ball",
		"life", "minecraft:glow_berries",
		"void", "minecraft:ender_pearl",
		"arcane", "minecraft:amethyst_shard");

	/** The item id a wisp of {@code element} will eat. */
	public static String food(String element) {
		return FOOD.getOrDefault(element, "");
	}

	/** The element whose wisps eat {@code itemId}, or "" if none do. */
	public static String foodElement(String itemId) {
		for (Map.Entry<String, String> entry : FOOD.entrySet()) {
			if (entry.getValue().equals(itemId)) {
				return entry.getKey();
			}
		}
		return "";
	}

	// ------------------------------------------------------------------ levels

	/** A familiar's level (1 to {@link #MAX_LEVEL}) for its experience. */
	public static int level(int xp) {
		int level = 1;
		for (int i = 1; i < LEVEL_XP.length; i++) {
			if (xp >= LEVEL_XP[i]) {
				level = i + 1;
			}
		}
		return level;
	}

	/** Experience at which the next level comes, or -1 at the top level. */
	public static int nextLevelAt(int xp) {
		int level = level(xp);
		return level >= MAX_LEVEL ? -1 : LEVEL_XP[level];
	}

	/** Experience for a monster slain together: 1, or 20 for a boss. */
	public static int killXp(boolean boss) {
		return boss ? 20 : 1;
	}

	/** Extra mana regeneration while the familiar is out and near: +10%, +15%, +20%. */
	public static float regenBonus(int level) {
		return switch (clamp(level)) {
			case 1 -> 0.10F;
			case 2 -> 0.15F;
			default -> 0.20F;
		};
	}

	/** Ticks between the familiar's little spells: 12, 10 and 8 seconds. */
	public static int helpInterval(int level) {
		return switch (clamp(level)) {
			case 1 -> 240;
			case 2 -> 200;
			default -> 160;
		};
	}

	/** How strong its little spells are. */
	public static double helpPower(int level) {
		return switch (clamp(level)) {
			case 1 -> 1.0;
			case 2 -> 1.3;
			default -> 1.6;
		};
	}

	/** Whether these bonded elements include every element (the Menagerie feat). */
	public static boolean menagerie(Collection<String> bonded) {
		return bonded.containsAll(ELEMENTS);
	}

	private static int clamp(int level) {
		return Math.max(1, Math.min(MAX_LEVEL, level));
	}
}
