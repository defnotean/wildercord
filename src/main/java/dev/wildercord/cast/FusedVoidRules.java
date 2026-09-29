package dev.wildercord.cast;

/**
 * The pure rules of the fused effects of void, arcane and time ({@link FusedVoid}), with no Minecraft
 * types so they're unit-tested. Numbers are at power 1 and duration 1 and match the rune descriptions
 * in {@code Runes}.
 */
final class FusedVoidRules {
	private FusedVoidRules() {}

	// ---- Entropy: 0.5, 1, 1.5, 2 and 2.5 over five seconds.
	static final int ENTROPY_SECONDS = 5;
	static final double ENTROPY_STEP = 0.5;
	static final double ENTROPY_PEAK = 3.0;
	/** Armour it strips a wound, up to this many points, until it ends. */
	static final int ENTROPY_ARMOUR_MAX = 5;

	/** How many wounds (a second apart) an Entropy lasts at {@code duration}: five, longer with Extend, never none. */
	static int entropyWounds(double duration) {
		return Math.max(1, (int) Math.round(ENTROPY_SECONDS * duration));
	}

	/** The {@code step}th wound (1 = the first), at power 1: half a point more each second, up to 2.5. */
	static double entropyWound(int step) {
		return Math.min(ENTROPY_PEAK, ENTROPY_STEP * (Math.max(1, step) + 1));
	}

	// ---- Devour: 5 damage; a kill feeds 10 mana and 4 absorption.
	static final double DEVOUR_DAMAGE = 5.0;
	/** Devour bites 1 harder for each tenth of its prey's health that is gone, up to this much more. */
	static final double DEVOUR_EXTRA_MAX = 5.0;

	/** Devour's damage on prey with {@code health} of {@code max}: 5, and 1 more for every 10% it is missing (up to 5 more). */
	static double devourDamage(float health, float max) {
		double missing = max <= 0 ? 0 : Math.max(0.0, 1.0 - health / max);
		return DEVOUR_DAMAGE + Math.min(DEVOUR_EXTRA_MAX, Math.floor(missing * 10 + 1.0E-6));
	}
	static final float DEVOUR_MANA = 10.0F;
	static final float DEVOUR_ABSORPTION = 4.0F;
	/** How long the absorption lasts (the Absorption effect it rides on). */
	static final int DEVOUR_ABSORPTION_TICKS = 30 * 20;
	/** Kills one cast (and its echoes) may feed on. */
	static final int DEVOUR_FEEDS = 2;

	// ---- Timesteal: up to 2 good effects, at most 30 seconds of each.
	static final int TIMESTEAL_EFFECTS = 2;
	static final double TIMESTEAL_SECONDS = 30.0;

	/**
	 * Of an effect with {@code left} ticks to run ({@code Integer.MAX_VALUE} for an endless one), how
	 * many ticks Timesteal takes: all of it, at most {@code cap}.
	 */
	static int stolenTicks(int left, int cap) {
		return Math.max(0, Math.min(cap, left));
	}

	/**
	 * What a boss keeps of an effect Timesteal took {@code stolen} ticks of: the rest (a boss loses only
	 * the time taken, never the whole effect). Anyone else keeps nothing.
	 */
	static int keptTicks(int left, int stolen, boolean boss) {
		return boss && left != Integer.MAX_VALUE ? Math.max(0, left - stolen) : 0;
	}

	// ---- Hemomancy: 4 magic damage, 1 more per 2 health missing, up to 6 more.
	static final double HEMOMANCY_DAMAGE = 4.0;
	static final int HEMOMANCY_BONUS = 6;

	/** Hemomancy's extra damage for a caster on {@code health} of {@code max}: 1 for every 2 missing, up to 6. */
	static int hemomancyBonus(float max, float health) {
		double missing = Math.max(0.0, max - health);
		return (int) Math.min(HEMOMANCY_BONUS, Math.floor(missing / 2.0 + 1.0E-6));
	}

	// ---- Reckoning: 4 seconds of wounds counted; half of it due at once, at most 12.
	static final double RECKONING_SECONDS = 4.0;
	static final double RECKONING_SHARE = 0.5;
	static final double RECKONING_CAP = 12.0;
	/** The share of what comes due that heals whoever opened the ledger, and the most it heals. */
	static final double RECKONING_HEAL_SHARE = 0.5;
	static final float RECKONING_HEAL_MAX = 6.0F;

	static float reckoningHeal(double due) {
		return (float) Math.min(RECKONING_HEAL_MAX, Math.max(0.0, due) * RECKONING_HEAL_SHARE);
	}

	/** What a ledger of {@code owed} comes to at {@code power}: half of it, at most 12 (both scaled by power). */
	static double reckoningDue(double owed, double power) {
		return Math.min(RECKONING_CAP, Math.max(0.0, owed) * RECKONING_SHARE) * power;
	}

	// ---- Singularity: 2.5 seconds within 5 blocks, then 6 damage.
	static final int SINGULARITY_TICKS = 50;
	static final double SINGULARITY_RADIUS = 5.0;
	static final double SINGULARITY_DAMAGE = 6.0;
	/** Creatures one black hole can hold at once. */
	static final int SINGULARITY_CAUGHT = 12;
	/** Black holes one caster may have open at once (Split, Volley and Echo can't open a dozen). */
	static final int SINGULARITY_OPEN = 3;
	/** Projectiles one hole can swallow, each adding 1 damage to its burst. */
	static final int SINGULARITY_SWALLOW_MAX = 5;

	/**
	 * How hard a creature is flung out of the burst: less toward a wall close behind it ({@code room}
	 * blocks of it, looked for up to 4), so nothing is dashed into stone.
	 */
	static double flingStrength(double power, double room) {
		return 1.3 * Math.sqrt(Math.max(0.25, power)) * Math.max(0.25, Math.min(1.0, room / 4.0));
	}

	// ---- Prismatic Burst: 4 damage and 3 more per mark.
	static final double PRISMATIC_DAMAGE = 4.0;
	static final double PRISMATIC_PER_MARK = 3.0;
	/** Marks it counts at most (it uses up every one: burning, frozen, windswept, pulled, soaked, wet, cracked, shadowed, bleeding). */
	static final int PRISMATIC_MARKS = 6;

	/** Prismatic Burst's damage at power 1 on a target with {@code marks} elemental marks. */
	static double prismaticDamage(int marks) {
		return PRISMATIC_DAMAGE + PRISMATIC_PER_MARK * Math.max(0, Math.min(PRISMATIC_MARKS, marks));
	}

	// ---- Chronoshift: other spells 3 seconds sooner; Haste I and Speed I for 5 seconds.
	static final double CHRONOSHIFT_SECONDS = 3.0;
	static final double CHRONOSHIFT_BUFF_SECONDS = 5.0;
	/** Mana an ally spent in the last 5 seconds that Chronoshift gives back: this share, at most that much. */
	static final double CHRONOSHIFT_REFUND_SHARE = 0.3;
	static final float CHRONOSHIFT_REFUND_MAX = 30.0F;
	static final int CHRONOSHIFT_REFUND_WINDOW = 100;

	/** Ticks Chronoshift takes off a spell's cooldown at {@code power}: 3 seconds, more with power, never under half or over twice that. */
	static int chronoshiftTicks(double power) {
		return (int) Math.round(CHRONOSHIFT_SECONDS * 20 * Math.max(0.5, Math.min(2.0, power)));
	}

	/** When a spell ready at {@code readyAt} is ready after Chronoshift takes {@code by} ticks off it, at {@code now}: never before now. */
	static long shifted(long readyAt, long now, int by) {
		return readyAt <= now ? readyAt : Math.max(now, readyAt - by);
	}
}
