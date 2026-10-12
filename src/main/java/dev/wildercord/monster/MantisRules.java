package dev.wildercord.monster;

/**
 * The Bloomclaw Mantis's numbers and decisions, kept pure for its tests: who it notices through its stillness, when it
 * strikes, whether a strike catches, how long it holds on and nibbles, and what breaks its grip.
 */
public final class MantisRules {
	private MantisRules() {}

	/** It notices whoever comes this close, but a sneaking player only from much nearer. */
	public static final double NOTICE_RANGE = 12;
	public static final double SNEAK_NOTICE = 4;
	/** It strikes from this close: a short threat display (the tell), then its forelegs snap out. */
	public static final double STRIKE_RANGE = 3.0;
	public static final int WINDUP_TICKS = 12;
	public static final int STRIKE_TICKS = 6;
	/** Its strike catches whatever is this near, this squarely in front of it: the dot of its look and the way to them. */
	public static final double STRIKE_REACH = 3.4;
	public static final double STRIKE_CONE = 0.6;
	public static final float STRIKE_DAMAGE = 4.0F;
	public static final int STRIKE_COOLDOWN = 50;
	/** Caught, its prey is held in its forelegs this long at most, nibbled now and then. */
	public static final int CLUTCH_TICKS = 50;
	public static final int NIBBLE_EVERY = 10;
	public static final float NIBBLE_DAMAGE = 2.0F;
	/** One blow at least this hard, struck while it holds on, breaks its grip and staggers it. */
	public static final float BREAK_FREE = 3.0F;
	/** Missing, it stumbles; broken free of, it staggers longer. Either way blows land half again as hard. */
	public static final int STUMBLE_TICKS = 25, STAGGER_TICKS = 45;
	public static final float CAUGHT_OUT_HURT = 1.5F;

	/** Whether it notices someone this far away. */
	public static boolean notices(double distance, boolean sneaking) {
		return distance <= (sneaking ? SNEAK_NOTICE : NOTICE_RANGE);
	}

	/** Whether it begins its strike now. */
	public static boolean strikes(double distance, long now, long next) {
		return distance <= STRIKE_RANGE && now >= next;
	}

	/** Whether its forelegs catch someone this far away, at this dot of its look and the way to them. */
	public static boolean catches(double distance, double dot) {
		return distance <= STRIKE_REACH && dot >= STRIKE_CONE;
	}

	/** Whether it nibbles at what it holds on this tick of holding. */
	public static boolean nibbles(int held) {
		return held > 0 && held % NIBBLE_EVERY == 0;
	}

	/** Whether it lets go of its own accord. */
	public static boolean letsGo(int held) {
		return held >= CLUTCH_TICKS;
	}

	/** Whether a blow this hard breaks its grip. */
	public static boolean breaksFree(float damage) {
		return damage >= BREAK_FREE;
	}

	/** Damage it takes: more while it stumbles or staggers. */
	public static float hurt(float damage, boolean caughtOut) {
		return caughtOut ? damage * CAUGHT_OUT_HURT : damage;
	}
}
