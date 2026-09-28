package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Wild magic: an overcast spell, paid for with a cracking Heart Circle instead of mana, sometimes
 * twists into something unexpected. The chance, the table of outcomes and the rune rewrites some of
 * them make are here, pure; {@code cast.WildSurge} makes each one happen.
 *
 * <p>To add an outcome: add a {@link Surge} (its weight, colour and whether it needs an ordinary
 * spell to rewrite), a {@code message.wildercord.surge.<id>} line in {@code tools/generate_assets.py},
 * and its case in {@code WildSurge.surge}.</p>
 */
public final class WildMagic {
	private WildMagic() {}

	/** The chance an overcast surges when it went no further past your mana than a whisker. */
	public static final double BASE_CHANCE = 0.20;
	/** The chance at most: an overcast of twice your full mana or more. */
	public static final double MAX_CHANCE = 0.50;
	/** How long a Free Recast waits for its cast. */
	public static final int FREE_RECAST_TICKS = 60;
	/** How far the Blink surge throws you: 3 to 6 blocks. */
	public static final double BLINK_MIN = 3.0;
	public static final double BLINK_MAX = 6.0;
	/** Everything within this many blocks of you is caught up in a surge that touches everyone near. */
	public static final double NEAR = 8.0;
	/** A Backfire hurts you for this much, but never below half a heart. */
	public static final float BACKFIRE = 4.0F;

	/**
	 * What an overcast spell can twist into. {@code weight} is how often (out of the total), {@code color}
	 * the colour of its line above the hotbar, and {@code rewrites} whether it changes the spell's runes
	 * (a secret spell, which is its exact runes, never gets those).
	 */
	public enum Surge {
		/** The spell goes off twice. */
		TWICE("twice", 10, 0xFFD870, false),
		/** Its element swaps for a random other one. */
		ELEMENT("element", 9, 0x9AE0FF, true),
		/** It's cast at double size: every radius it has, widened twice. */
		GRAND("grand", 8, 0xFF9A5A, true),
		/** It comes out as a harmless shower of butterflies of light and fireworks. */
		BUTTERFLIES("butterflies", 8, 0xFFA8E8, false),
		/** It heals everyone near instead, friend and foe. */
		HEAL_ALL("heal_all", 7, 0x8CF08C, false),
		/** It blinks you a few blocks away, then goes off. */
		BLINK("blink", 7, 0xC8A0FF, false),
		/** A burst of wisps comes with it, chasing whatever's near. */
		WISPS("wisps", 7, 0xB06CFF, false),
		/** Gravity flips around you for a moment: everything near floats up. */
		LEVITATE("levitate", 7, 0xE0F4FF, false),
		/** It goes off at a random creature nearby, whoever that is. */
		STRAY("stray", 6, 0xFFB050, false),
		/** Time slows: everything near, you included, is slowed for 2 seconds. */
		SLOW_TIME("slow_time", 6, 0xF2D98A, false),
		/** It backfires: a little damage to you, and nothing else. */
		BACKFIRE("backfire", 6, 0xFF6A5A, false),
		/** It goes off, and your next cast within 3 seconds is free and ignores cooldowns. */
		FREE_RECAST("free_recast", 7, 0x7AF0D8, false),
		/** It goes off, and a Shield of light settles on you for 10 seconds. */
		WARD("ward", 6, 0xF5B04A, false);

		public final String id;
		public final int weight;
		public final int color;
		public final boolean rewrites;

		Surge(String id, int weight, int color, boolean rewrites) {
			this.id = id;
			this.weight = weight;
			this.color = color;
			this.rewrites = rewrites;
		}

		/** The key of its line above the hotbar. */
		public String key() {
			return "message.wildercord.surge." + id;
		}
	}

	/**
	 * The chance an overcast surges: {@link #BASE_CHANCE}, plus 15 points for every full mana bar the
	 * spell went past the mana you had, up to {@link #MAX_CHANCE}.
	 */
	public static double chance(double mana, double cost, double maxMana) {
		double past = Math.max(0, cost - Math.max(0, mana)) / Math.max(1, maxMana);
		return Math.max(BASE_CHANCE, Math.min(MAX_CHANCE, BASE_CHANCE + 0.15 * past));
	}

	/**
	 * The outcome for a roll in [0, 1), by weight. With {@code rewritable} false (a secret spell), the
	 * outcomes that rewrite runes are left out.
	 */
	public static Surge pick(double roll, boolean rewritable) {
		int total = 0;
		for (Surge surge : Surge.values()) {
			if (rewritable || !surge.rewrites) {
				total += surge.weight;
			}
		}
		double at = Math.max(0, Math.min(0.999999, roll)) * total;
		Surge last = Surge.TWICE;
		for (Surge surge : Surge.values()) {
			if (rewritable || !surge.rewrites) {
				last = surge;
				at -= surge.weight;
				if (at < 0) {
					return surge;
				}
			}
		}
		return last;
	}

	/** The elements a spell can swap to: every one with a harmful effect rune (innate runes aside). */
	public static List<String> elements() {
		TreeSet<String> found = new TreeSet<>();
		for (RuneDef rune : Runes.all()) {
			if (swappable(rune)) {
				found.add(rune.element());
			}
		}
		return List.copyOf(found);
	}

	/** The element a spell's effects are mostly: its first harmful effect's, or "" for none. */
	public static String elementOf(List<RuneDef> runes) {
		for (RuneDef rune : runes) {
			if (swappable(rune)) {
				return rune.element();
			}
		}
		return "";
	}

	private static boolean swappable(RuneDef rune) {
		return rune.family() == RuneFamily.EFFECT && rune.kind() == EffectKind.HARMFUL && !rune.element().isEmpty() && Runes.common(rune);
	}

	/**
	 * The spell with every harmful effect of an element swapped for one of {@code element}'s: the one of
	 * the nearest tier, the {@code choice}-th of those (wrapping round). Everything else stays.
	 */
	public static List<RuneDef> swapElement(List<RuneDef> runes, String element, int choice) {
		List<RuneDef> candidates = new ArrayList<>();
		for (RuneDef rune : Runes.all()) {
			if (swappable(rune) && rune.element().equals(element)) {
				candidates.add(rune);
			}
		}
		if (candidates.isEmpty()) {
			return runes;
		}
		List<RuneDef> out = new ArrayList<>(runes.size());
		for (RuneDef rune : runes) {
			if (!swappable(rune) || rune.element().equals(element)) {
				out.add(rune);
				continue;
			}
			int nearest = Integer.MAX_VALUE;
			for (RuneDef c : candidates) {
				nearest = Math.min(nearest, Math.abs(c.tier() - rune.tier()));
			}
			List<RuneDef> best = new ArrayList<>();
			for (RuneDef c : candidates) {
				if (Math.abs(c.tier() - rune.tier()) == nearest) {
					best.add(c);
				}
			}
			out.add(best.get(Math.floorMod(choice, best.size())));
		}
		return out;
	}

	/**
	 * The spell at double size: two Widens right after every rune with a radius (a modifier attaches to
	 * the nearest rune before it that it can change, so they land on that one). {@code null} when
	 * nothing in it has a radius to widen.
	 */
	public static List<RuneDef> grand(List<RuneDef> runes) {
		List<RuneDef> out = new ArrayList<>(runes.size() + 4);
		boolean widened = false;
		for (RuneDef rune : runes) {
			out.add(rune);
			if (rune.family() != RuneFamily.MODIFIER && rune.traits().contains(Trait.RADIUS)) {
				out.add(Runes.WIDEN);
				out.add(Runes.WIDEN);
				widened = true;
			}
		}
		return widened ? out : null;
	}
}
