package dev.wildercord.duel;

import java.util.Locale;

/**
 * The spell-duel arena (0.13), as plain numbers. Two casters step up to an Arena Stone and fight a ranked duel round it,
 * on the duel's own rules: nobody dies and nothing is lost but rating. Ratings move by Elo, fall into ranks, and every
 * season (28 days) they're pulled halfway back to the start.
 */
public final class ArenaRules {
	private ArenaRules() {}

	/** Where a new caster starts, and how far one bout can move a rating. */
	public static final int START = 1000, K = 32;
	/** How far round the stone the bout may range (blocks) and how near it you must stand to step up. */
	public static final double RADIUS = 16.0, STEP_UP = 6.0;
	/** How long one who steps up waits for an opponent (ticks: a minute), and how long a bout may run (ticks: three minutes). */
	public static final int WAIT_TICKS = 1200, BOUT_TICKS = 3600;
	/** How long a season runs (days), and how many names the ladder shows. */
	public static final int SEASON_DAYS = 28, LADDER_SHOWN = 5;

	/** The ranks, from the rating each begins at. */
	public enum Rank {
		APPRENTICE(0, 0xA8A8A8),
		ADEPT(1100, 0x7FD8A8),
		DUELLIST(1200, 0x6AB8FF),
		ARCHMAGE(1350, 0xC08CFF),
		GRANDMASTER(1500, 0xFFC84A);

		public final int from;
		public final int color;

		Rank(int from, int color) {
			this.from = from;
			this.color = color;
		}

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** The rank a {@code rating} falls in. */
	public static Rank rank(int rating) {
		Rank found = Rank.APPRENTICE;
		for (Rank rank : Rank.values()) {
			if (rating >= rank.from) found = rank;
		}
		return found;
	}

	/** The chance Elo gives a caster rated {@code a} against one rated {@code b}. */
	public static double expected(int a, int b) {
		return 1.0 / (1.0 + Math.pow(10, (b - a) / 400.0));
	}

	/** How far {@code a}'s rating moves against {@code b} for a {@code score} (1 won, 0.5 even, 0 lost). */
	public static int change(int a, int b, double score) {
		return (int) Math.round(K * (score - expected(a, b)));
	}

	/** The season a game time falls in. */
	public static int season(long gameTime) {
		return (int) Math.max(0, gameTime / 24000L / SEASON_DAYS);
	}

	/** A rating carried into a new season: halfway back to the start. */
	public static int carried(int rating) {
		return START + (rating - START) / 2;
	}

	/** A rating carried across {@code seasons} season changes. */
	public static int carried(int rating, int seasons) {
		for (int i = 0; i < seasons && rating != START; i++) rating = carried(rating);
		return rating;
	}
}
