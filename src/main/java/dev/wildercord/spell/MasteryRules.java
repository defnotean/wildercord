package dev.wildercord.spell;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spell mastery, the pure part: a spell grows with the one who casts it. A spell is its exact sequence of runes
 * (as it fires, Knots untied), and each player keeps a record for each sequence they use: experience, the rank it
 * has reached, the traits they chose for it and a few counts of where and how it was cast. Editing a spell makes it
 * a new spell, but the old record stays, so changing back brings it back.
 *
 * <p>Experience comes from casts that matter, not from casting: a hit on a real enemy, a heal or a ward on an ally
 * who needs it. Danger (low health, a crowd, a boss, a dungeon) and variety (a kind of foe the spell hasn't met
 * lately) are worth more; doing the same thing in the same place again and again is worth less and less; and the
 * training dummy and the practice arena only teach a spell so much. Shared by the server ({@code cast.Mastery}),
 * the Cord screen and the unit tests.</p>
 *
 * <p>The ranks are tuned so a spell used as a main attack reaches Master in about three hours of real play, Practised
 * in under half an hour and Mythic in about ten: a meaningful cast strikes one or two foes and is worth about 2.5
 * experience once repetition has had its say, and a player fighting with one spell lands about two a minute between
 * everything else they do ({@code MasteryRulesTest} works this through).</p>
 */
public final class MasteryRules {
	private MasteryRules() {}

	// ------------------------------------------------------------------ ranks

	public static final int MAX_RANK = 5;
	/** The ranks' names, from the first cast to the last. */
	private static final String[] NAMES = {"Kindled", "Practised", "Adept", "Master", "Mythic"};
	/** The experience (a running total) each rank needs. */
	private static final int[] THRESHOLDS = {0, 100, 350, 1000, 3000};
	/** The rank from which a spell can be inscribed onto a scroll with its traits, and from which its name is spoken when cast. */
	public static final int ADEPT = 3;
	public static final int MASTER = 4;
	public static final int MYTHIC = 5;
	/** The rank of a new record. */
	public static final int FIRST = 1;

	/** The rank {@code xp} experience has reached, 1 to {@link #MAX_RANK}. */
	public static int rank(double xp) {
		int rank = 1;
		for (int i = 1; i < THRESHOLDS.length; i++) {
			if (xp >= THRESHOLDS[i]) {
				rank = i + 1;
			}
		}
		return rank;
	}

	/** The experience {@code rank} needs (0 for the first). */
	public static int threshold(int rank) {
		return THRESHOLDS[Math.max(1, Math.min(MAX_RANK, rank)) - 1];
	}

	/** How far {@code xp} is from its rank to the next, 0 to 1 (1 at the last rank). */
	public static double progress(double xp) {
		int rank = rank(xp);
		if (rank >= MAX_RANK) {
			return 1.0;
		}
		double from = threshold(rank);
		return Math.max(0, Math.min(1, (xp - from) / (threshold(rank + 1) - from)));
	}

	/** A rank's name: Kindled, Practised, Adept, Master or Mythic. */
	public static String name(int rank) {
		return NAMES[Math.max(1, Math.min(MAX_RANK, rank)) - 1];
	}

	/** The trait slot a rank opens (rank II opens the first, V the fourth), or -1 for the first rank. */
	public static int slotOf(int rank) {
		return rank >= 2 && rank <= MAX_RANK ? rank - 2 : -1;
	}

	/** How many trait slots a record has: one each for ranks II to V. */
	public static final int SLOTS = 4;

	/** The rank whose trait goes in {@code slot}. */
	public static int rankOf(int slot) {
		return slot + 2;
	}

	// ------------------------------------------------------------------ records

	/** How many spells' records a player keeps: the most recently used. A spell threaded on the Cord is never forgotten. */
	public static final int MAX_RECORDS = 48;
	/** How many trait choices a record's offer holds. */
	public static final int OFFER = 3;
	/**
	 * Experience levels it costs to change your mind about a rank's trait once: re-roll the offer before choosing, or
	 * unbind the chosen trait to choose again. Each rank allows one change.
	 */
	public static final int CHANGE_LEVELS = 3;

	/** Records keyed longer than this are keyed by a hash instead (a sequence of very long Knots or weaves). */
	private static final int MAX_KEY = 512;

	/**
	 * A spell's identity: its rune ids (Knots already untied, as it fires), in order. Very long sequences are kept as a
	 * hash of themselves, which is as good for telling spells apart.
	 */
	public static String key(List<String> ids) {
		String joined = String.join(" ", ids);
		if (joined.length() <= MAX_KEY) {
			return joined;
		}
		return "#" + Long.toHexString(hash(joined)) + ":" + ids.size();
	}

	/** A stable 64-bit hash of {@code text} (FNV-1a), the same on every machine. */
	public static long hash(String text) {
		long h = 0xcbf29ce484222325L;
		for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
			h ^= b & 0xFF;
			h *= 0x100000001b3L;
		}
		return h;
	}

	// ------------------------------------------------------------------ experience

	/** Experience for striking a real foe once in a cast (before what the strike took off it). */
	public static final double STRIKE = 1.0;
	/** Experience for taking a foe's whole health in one cast; a share of it for less. */
	public static final double DAMAGE = 2.0;
	/** Experience for the strike that slays it. */
	public static final double KILL = 1.0;
	/** Experience for helping an ally who needed it (hurt, or in a fight), once per ally per cast. */
	public static final double HELP = 1.0;
	/** Experience for warding or healing yourself while in danger, once per cast. */
	public static final double SELF_HELP = 0.6;
	/** Experience for a heal, per point of health it truly restores to someone who was missing it, up to {@link #HEAL_MAX}. */
	public static final double HEAL_PER_POINT = 0.25;
	public static final double HEAL_MAX = 2.0;
	/** Experience for a spell that only moves you or works the world (mining, building, crossing), once per cast. */
	public static final double UTILITY = 0.35;
	/** The most one cast (its links, echoes and pulses together) can earn, after everything that multiplies it. */
	public static final double MAX_PER_CAST = 20.0;

	/** How much a kind of target is worth: a monster is 1, these are relative to it. */
	public static final double RUNEBOUND = 1.5;
	public static final double BOSS = 2.0;
	/** Another player, with PvP on: half, and a friend can't be farmed (it fades like any repetition). */
	public static final double PLAYER = 0.5;

	/**
	 * Experience for striking one foe in a cast.
	 *
	 * @param worth  what the foe is worth (1 a monster, {@link #RUNEBOUND}, {@link #BOSS}, {@link #PLAYER})
	 * @param share  the share of its full health the strike took, 0 to 1
	 * @param killed whether it slew it
	 */
	public static double strike(double worth, double share, boolean killed) {
		if (worth <= 0) {
			return 0;
		}
		return worth * (STRIKE + DAMAGE * Math.max(0, Math.min(1, share)) + (killed ? KILL : 0));
	}

	/** Experience for a heal restoring {@code restored} health to someone missing it. */
	public static double heal(double restored) {
		return Math.max(0, Math.min(HEAL_MAX, restored * HEAL_PER_POINT));
	}

	// ------------------------------------------------------------------ the moment

	/** Below this share of their health a caster is in danger. */
	public static final double LOW_HEALTH = 0.35;
	/** Foes within {@link #CROWD_RADIUS} blocks that make a crowd, and a horde. */
	public static final int CROWD = 4;
	public static final int HORDE = 8;
	public static final double CROWD_RADIUS = 16.0;
	/** How far a boss counts as being in the fight. */
	public static final double BOSS_RADIUS = 48.0;
	/** The most the moment can multiply a cast's experience by. */
	public static final double MAX_SITUATION = 3.0;
	/** A kind of foe this spell hasn't struck lately is worth this much more. */
	public static final double NOVELTY = 1.25;
	/** How many kinds of foe a spell remembers having struck lately. */
	public static final int REMEMBERED_KINDS = 12;

	/**
	 * How much the moment of a cast multiplies its experience: low health 1.5×, a crowd 1.3× (a horde 1.5×), a boss
	 * nearby 1.5×, inside a dungeon 1.25×, together at most {@link #MAX_SITUATION}.
	 */
	public static double situation(double healthShare, int foes, boolean boss, boolean dungeon) {
		double m = 1.0;
		if (healthShare < LOW_HEALTH) {
			m *= 1.5;
		}
		if (foes >= HORDE) {
			m *= 1.5;
		} else if (foes >= CROWD) {
			m *= 1.3;
		}
		if (boss) {
			m *= 1.5;
		}
		if (dungeon) {
			m *= 1.25;
		}
		return Math.min(MAX_SITUATION, m);
	}

	/** Whether a caster is in danger: low on health, or with a crowd around them. */
	public static boolean danger(double healthShare, int foes) {
		return healthShare < LOW_HEALTH || foes >= CROWD;
	}

	// ------------------------------------------------------------------ repetition

	/** How much each recent cast in the same place against the same kind of foe takes off the next. */
	public static final double REPEAT = 0.06;
	/** The least repetition leaves of a cast's worth. */
	public static final double REPEAT_FLOOR = 0.1;
	/** How long it takes a place to be half forgotten, in ticks (ten minutes). */
	public static final long REPEAT_HALF_LIFE = 20L * 60 * 10;
	/** The size of a place, in blocks: casting in the same 16-block cube counts as the same place. */
	public static final int PLACE = 16;

	/** What is left of a cast's worth after {@code recent} recent casts like it (in the same place, at the same kind of foe). */
	public static double repetition(double recent) {
		return Math.max(REPEAT_FLOOR, 1.0 / (1.0 + REPEAT * Math.max(0, recent)));
	}

	/** How many recent casts remain remembered after {@code ticks} have passed: half every {@link #REPEAT_HALF_LIFE}. */
	public static double forget(double recent, long ticks) {
		if (ticks <= 0) {
			return recent;
		}
		return recent * Math.pow(0.5, ticks / (double) REPEAT_HALF_LIFE);
	}

	// ------------------------------------------------------------------ practice

	/** The most a spell can learn from training dummies and the practice arena, in all: just over half the way to Practised. */
	public static final double PRACTICE_CAP = 60.0;
	/** Practice is worth half as much as the real thing. */
	public static final double PRACTICE_RATE = 0.5;

	/** How much of {@code xp} earned in practice a spell takes, having already learned {@code learned} that way. */
	public static double practice(double learned, double xp) {
		return Math.max(0, Math.min(PRACTICE_CAP - learned, xp * PRACTICE_RATE));
	}

	// ------------------------------------------------------------------ traits against the caps

	/** The most a spell's traits together may multiply a hit's damage by (they never stack into a one-shot). */
	public static final double MAX_TRAIT_POWER = 1.2;
	/** Against a player, a damage trait gives this share of its bonus. */
	public static final double PLAYER_TRAIT_SHARE = 0.5;
	/** The least a spell's traits together may bring its price and cooldown down to. */
	public static final double COST_FLOOR = 0.85;
	public static final double COOLDOWN_FLOOR = 0.85;
	/** The most a spell's traits may quicken its charge, or lengthen its reach. */
	public static final double MAX_CHARGE_SPEED = 1.25;
	public static final double MAX_RANGE = 1.25;

	/** A hit's damage bonus from traits giving {@code product} together, held to the cap and halved against a player. */
	public static double traitPower(double product, boolean againstPlayer) {
		double held = Math.max(1.0, Math.min(MAX_TRAIT_POWER, product));
		return againstPlayer ? 1.0 + (held - 1.0) * PLAYER_TRAIT_SHARE : held;
	}
}
