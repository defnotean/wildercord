package dev.wildercord.cast;

/**
 * The pure rules of standing up to spells, kept free of Minecraft so the unit tests can reach them: what a player's armour
 * turns aside, what Warding and Warded take off, how far a hit's bonuses may multiply it against a player, and when the
 * spellguard holds. {@link SpellDefence} applies them.
 *
 * <p>Every layer multiplies, so none of them alone (and no stack of enchantments) makes anyone immune.</p>
 */
public final class SpellDefenceRules {
	private SpellDefenceRules() {}

	// ------------------------------------------------------------------ armour

	/**
	 * How much of armour's usual worth counts against a spell that armour doesn't stop at all (vanilla's magic, frost,
	 * a sonic boom): the default for defence.armour_rate. Just over half, so full netherite turns aside 35 to 40% of an
	 * everyday spell and still about a third of a huge one.
	 */
	public static final double ARMOUR_RATE = 0.55;

	/**
	 * The share of a spell hit of {@code damage} that {@code armour} points and {@code toughness} turn aside, at {@code rate}
	 * of what they'd turn from a blade. This is vanilla's armour formula (20 points count at most, each 4%, a fifth of them
	 * always counts, and toughness keeps more of it against big hits), scaled.
	 */
	public static double armourShare(double damage, double armour, double toughness, double rate) {
		if (armour <= 0 || rate <= 0) {
			return 0;
		}
		double toughnessDivisor = 2.0 + toughness / 4.0;
		double counted = clamp(armour - Math.max(0, damage) / toughnessDivisor, armour * 0.2, 20.0);
		return Math.min(1.0, rate) * counted / 25.0;
	}

	// ------------------------------------------------------------------ Warding

	/** Warding's protection points per level: twice Protection's, like the other protections against one kind of harm. */
	public static final int WARDING_POINTS = 2;
	/** Vanilla's cap on protection from enchantments: 20 points, each 4%, so 80% at most. Warding shares it with Protection. */
	public static final double ENCHANTMENT_CAP = 20.0;
	/** The highest level of Warding. */
	public static final int WARDING_MAX_LEVEL = 4;

	/**
	 * What Warding multiplies a spell hit by, on top of what the game's own enchantment protection will take off it
	 * ({@code otherPoints}: Protection's, 0 when the hit ignores enchantments). The two share the one cap, so a full set of
	 * Warding IV, or Warding on top of Protection, reaches 80% and never more.
	 *
	 * @param wardingLevels the Warding levels on every piece of armour, added up
	 */
	public static double wardingFactor(double otherPoints, int wardingLevels) {
		if (wardingLevels <= 0) {
			return 1.0;
		}
		double before = Math.min(ENCHANTMENT_CAP, Math.max(0, otherPoints));
		double after = Math.min(ENCHANTMENT_CAP, before + wardingLevels * WARDING_POINTS);
		return (1.0 - after / 25.0) / (1.0 - before / 25.0);
	}

	/** The share of a spell hit enchantments take off: Protection's points and Warding's together, within the cap. */
	public static double enchantmentShare(double otherPoints, int wardingLevels) {
		return Math.min(ENCHANTMENT_CAP, Math.max(0, otherPoints) + Math.max(0, wardingLevels) * WARDING_POINTS) / 25.0;
	}

	// ------------------------------------------------------------------ Warded

	/** Warded: a fifth of spell damage off per level. */
	public static final double WARDED_PER_LEVEL = 0.2;
	/** However strong the effect (a command can give any level), it takes off 80% at most. */
	public static final double WARDED_MAX = 0.8;

	/** The share of a spell hit Warded takes off at {@code level} (the effect's amplifier plus one; 0 without it). */
	public static double wardedShare(int level) {
		return level <= 0 ? 0 : Math.min(WARDED_MAX, level * WARDED_PER_LEVEL);
	}

	// ------------------------------------------------------------------ bonuses

	/** The most a hit's bonuses together (execute, reactions, affinities, backstabs...) may multiply it against a player: the default for defence.max_bonus. */
	public static final double MAX_BONUS = 2.5;

	/** A hit's bonuses together, held to {@code cap}; bonuses that weaken it (a resistance, water on fire) are never lifted. */
	public static double capBonus(double bonus, double cap) {
		if (Double.isNaN(bonus)) {
			return 1.0;
		}
		return Math.min(bonus, Math.max(1.0, cap));
	}

	/**
	 * As {@link #capBonus(double, double)}, for a hit whose power already carries what the cast's
	 * performance added ({@code performance}: an overchannel stage, a release on the beat, a traced
	 * glyph). That share counts toward the cap like any other bonus, so the two together never multiply
	 * a spell against a player past it. The answer still multiplies the hit as it is (performance in it).
	 */
	public static double capBonus(double bonus, double performance, double cap) {
		if (Double.isNaN(performance) || performance <= 1.0) {
			return capBonus(bonus, cap);
		}
		return capBonus(Double.isNaN(bonus) ? performance : bonus * performance, cap) / performance;
	}

	// ------------------------------------------------------------------ the spellguard

	/** The share of full health a player must have for the spellguard to hold: the default for defence.spellguard_health. */
	public static final double GUARD_HEALTH = 0.8;
	/** How long the spellguard takes to come back once it has held, in seconds: the default for defence.spellguard_recharge_seconds. */
	public static final int GUARD_RECHARGE = 60;
	/** What the spellguard leaves a player on: one heart. */
	public static final float GUARD_LEAVES = 2.0F;

	/**
	 * Whether the spellguard turns aside a spell hit that would kill: it's on, it's charged (see {@link #guardSeconds}), and
	 * the hit found the player at {@code threshold} of their health or more.
	 *
	 * @param heldAt the game time it last held, or null if it never has
	 */
	public static boolean guardHolds(boolean enabled, float healthBefore, float maxHealth, double threshold, long now, Long heldAt, int rechargeSeconds) {
		if (!enabled || maxHealth <= 0 || healthBefore <= GUARD_LEAVES) {
			return false;
		}
		return guardSeconds(now, heldAt, rechargeSeconds) == 0 && healthBefore >= maxHealth * threshold - 1e-4;
	}

	/**
	 * Whole seconds until the spellguard is back (0: it's ready). It counts from when it held, so a server that shortens the
	 * recharge shortens a running one too, and a time "in the future" (the clock turned back) doesn't hold it for ever.
	 */
	public static int guardSeconds(long now, Long heldAt, int rechargeSeconds) {
		if (heldAt == null || heldAt > now || now - heldAt >= rechargeSeconds * 20L) {
			return 0;
		}
		return (int) Math.max(1, (rechargeSeconds * 20L - (now - heldAt) + 19) / 20);
	}

	// ------------------------------------------------------------------ together

	/** The share of a spell hit that several layers taking off {@code shares} each leave off together (they multiply). */
	public static double combined(double... shares) {
		double left = 1.0;
		for (double share : shares) {
			left *= 1.0 - clamp(share, 0, 1);
		}
		return 1.0 - left;
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
