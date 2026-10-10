package dev.wildercord.spell;

import java.util.List;

/**
 * Heart Circles: rings of condensed mana a caster builds around their heart, from the 1st to
 * the 20th (the Master Heart). Every point of mana spent on spells condenses toward the next circle;
 * once enough has gathered (and, for some circles, a breakthrough has been earned), the caster
 * meditates to form it. Each circle deepens the heart; some bring a perk.
 *
 * <p>Pure numbers only, so the readout, the server and the tests agree. Conditions that need
 * the player (runes known, Cord worn, monsters and bosses slain) are checked in {@code player.Heart}.</p>
 */
public final class Circles {
	private Circles() {}

	public static final int MAX = 20;

	/**
	 * Mana condensed (see {@link CondenseRules}: mostly mana spent on spells that hurt hostile creatures) needed before circle
	 * {@code n} can form. Index 0 unused. Tempered in 0.12 to about 2.8 times the old figures: around a hundred hours of play
	 * to the Master Heart.
	 */
	private static final int[] CONDENSE = {0, 1000, 4000, 12000, 28000, 55000, 95000, 150000, 230000,
		330000, 460000, 620000, 810000, 1030000, 1280000, 1560000, 1870000, 2210000, 2580000, 2980000, 3420000};

	/** Per circle. */
	public static final int MANA_PER_CIRCLE = 15;
	public static final float REGEN_PER_CIRCLE = 0.5F;
	public static final double POWER_PER_CIRCLE = 0.015;

	/** Perks. */
	public static final int MANA_SKIN = 3;
	public static final double MANA_SKIN_SHARE = 0.2;
	public static final float MANA_SKIN_COST = 2.0F;
	public static final int FLOW = 5;
	public static final double FLOW_COOLDOWN = 0.85;
	public static final int OVERFLOW = 7;
	public static final double OVERFLOW_POWER = 1.15;
	public static final int ARCHMAGE = 8;
	public static final double ARCHMAGE_COST = 0.85;

	/** Meditation ticks it takes to form a circle once the heart is ready (taking damage starts it over). */
	public static final int FORM_TICKS = 200;

	/** Kinds of breakthrough a circle can need besides condensed mana. */
	public enum Need {
		/** Know this many runes. */
		RUNES,
		/** Wear a Cord of at least this tier (1 Copper, 2 Amethyst, 3 Echo). */
		CORD,
		/** Have defeated this many monsters with spells. */
		KILLS,
		/** Have helped slay a boss: the Wither, the Warden, an Elder Guardian, the Ender Dragon or the Archivist. */
		BOSS,
		/** Have set off this many different element reactions (any of them: see {@link Feats#REACTIONS}). */
		REACTIONS,
		/** Have slain this many Runebound, the monsters that cast spells. */
		RUNEBOUND,
		/** Have found this many secret spells. */
		SECRETS,
		/** Have done one particular feat (see {@link Feats}). */
		FEAT
	}

	/** @param feat for {@link Need#FEAT}: which one (a {@link Feats} id); otherwise empty */
	public record Requirement(Need need, int amount, String feat) {
		public Requirement(Need need, int amount) {
			this(need, amount, "");
		}

		public static Requirement feat(String feat) {
			return new Requirement(Need.FEAT, 1, feat);
		}
	}

	/**
	 * Every circle after the 1st needs a breakthrough as well as condensed mana. Early ones ask
	 * for knowledge; later ones for things you've actually done with magic.
	 */
	public static List<Requirement> requirements(int circle) {
		return switch (circle) {
			case 2 -> List.of(new Requirement(Need.RUNES, 10));
			case 3 -> List.of(new Requirement(Need.CORD, 1), new Requirement(Need.REACTIONS, 1));
			case 4 -> List.of(new Requirement(Need.KILLS, 40), new Requirement(Need.REACTIONS, 3));
			case 5 -> List.of(new Requirement(Need.RUNES, 35), new Requirement(Need.CORD, 2), Requirement.feat(Feats.LONG_SPELL_KILL));
			case 6 -> List.of(new Requirement(Need.KILLS, 150), new Requirement(Need.RUNEBOUND, 8), new Requirement(Need.REACTIONS, 5));
			case 7 -> List.of(new Requirement(Need.BOSS, 1), new Requirement(Need.SECRETS, 2), Requirement.feat(Feats.RHYTHM));
			case 8 -> List.of(new Requirement(Need.CORD, 3), new Requirement(Need.SECRETS, 4), Requirement.feat(Feats.ARCHIVIST));
			// The Archmage remains the original eighth-circle milestone. Beyond it, the same saved
			// counters and solo-achievable discoveries lead through all five later dungeon bosses.
			case 9 -> List.of(new Requirement(Need.RUNES, 60), new Requirement(Need.REACTIONS, 6));
			case 10 -> List.of(new Requirement(Need.KILLS, 250), Requirement.feat(Feats.CINDER_WARDEN));
			case 11 -> List.of(new Requirement(Need.SECRETS, 5), Requirement.feat(Feats.COMBINE));
			case 12 -> List.of(new Requirement(Need.RUNEBOUND, 20), Requirement.feat(Feats.TIDE_SCRIBE));
			case 13 -> List.of(new Requirement(Need.RUNES, 90), new Requirement(Need.REACTIONS, 8));
			case 14 -> List.of(new Requirement(Need.KILLS, 400), Requirement.feat(Feats.STAR_EATER));
			case 15 -> List.of(new Requirement(Need.SECRETS, 7), Requirement.feat(Feats.KNOT));
			case 16 -> List.of(new Requirement(Need.RUNEBOUND, 40), Requirement.feat(Feats.ROOT_GUARDIAN));
			case 17 -> List.of(new Requirement(Need.RUNES, 120), new Requirement(Need.REACTIONS, 10));
			case 18 -> List.of(new Requirement(Need.KILLS, 650), Requirement.feat(Feats.STORM_CONDUCTOR));
			case 19 -> List.of(new Requirement(Need.SECRETS, 9), new Requirement(Need.REACTIONS, 11));
			case 20 -> List.of(new Requirement(Need.KILLS, 1000), new Requirement(Need.RUNEBOUND, 100), new Requirement(Need.SECRETS, 10));
			default -> List.of();
		};
	}

	public static int condenseNeeded(int circle) {
		return circle <= 0 ? 0 : CONDENSE[Math.min(MAX, circle)];
	}

	/** Read old integer saves without migration; malformed values cannot grant out-of-range bonuses. */
	public static int count(int saved) {
		return Math.clamp(saved, 0, MAX);
	}

	/** Lifetime progress is never spent or wrapped back to a negative number. */
	public static int addCondensed(int saved, int earned) {
		return (int) Math.min(Integer.MAX_VALUE, (long) Math.max(0, saved) + Math.max(0, earned));
	}

	/** Keep the original eight rings' sizes, then fit the later rings close around the heart. */
	public static double ringRadius(int circle) {
		int ring = Math.max(0, count(circle) - 1);
		return 0.3 + 0.09 * Math.min(7, ring) + 0.035 * Math.max(0, ring - 7);
	}

	/** "1st", "2nd", "3rd", "4th"... */
	public static String ordinal(int n) {
		int mod100 = n % 100;
		String suffix = mod100 >= 11 && mod100 <= 13 ? "th" : switch (n % 10) {
			case 1 -> "st";
			case 2 -> "nd";
			case 3 -> "rd";
			default -> "th";
		};
		return n + suffix;
	}

	// ------------------------------------------------------------------ what circles and Cord enchantments do to spells

	public static final double POTENCY_PER_LEVEL = 0.08;
	public static final double CELERITY_PER_LEVEL = 0.08;
	public static final double THRIFT_PER_LEVEL = 0.07;
	public static final double PERSISTENCE_PER_LEVEL = 0.2;

	public static double power(int potency, int circles, boolean overflow) {
		double p = (1 + POTENCY_PER_LEVEL * potency) * (1 + POWER_PER_CIRCLE * count(circles));
		return overflow && circles >= OVERFLOW ? p * OVERFLOW_POWER : p;
	}

	public static double cost(int thrift, int circles) {
		return Math.max(0.2, 1 - THRIFT_PER_LEVEL * thrift) * (circles >= ARCHMAGE ? ARCHMAGE_COST : 1.0);
	}

	public static double cooldown(int celerity, int circles) {
		return Math.max(0.2, 1 - CELERITY_PER_LEVEL * celerity) * (circles >= FLOW ? FLOW_COOLDOWN : 1.0);
	}

	public static double duration(int persistence) {
		return 1 + PERSISTENCE_PER_LEVEL * persistence;
	}
}
