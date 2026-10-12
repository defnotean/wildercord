package dev.wildercord.monster;

/**
 * The Dunestinger's rules, apart from the game so they can be tested: how close someone has to come before a buried one
 * feels them, which of its two weapons it reaches for, and when, hurt, it goes back under the sand to come up somewhere
 * else.
 */
public final class DunestingerRules {
	private DunestingerRules() {}

	/** How close someone walking has to come before a buried one feels them through the sand. */
	public static final double WAKE_RANGE = 4.0;
	/** Running feet carry further through sand. */
	public static final double SPRINT_WAKE_RANGE = 7.0;
	/** A careful, crouching step barely carries at all. */
	public static final double SNEAK_WAKE_RANGE = 1.75;
	/** How far off the rest of a buried pack bursts up when one does. */
	public static final double PACK_RANGE = 12.0;

	/** Its pincers: close, quick, light. */
	public static final double PINCH_REACH = 2.0;
	public static final int PINCH_COOLDOWN = 18;
	public static final int PINCH_TICKS = 6;
	public static final float PINCH_DAMAGE = 3.0F;
	/** Its sting: the tail drawn back over its back for a moment first, then driven down. */
	public static final double STING_REACH = 2.8;
	public static final int STING_WINDUP = 10;
	public static final int STING_TICKS = 6;
	public static final int STING_COOLDOWN = 60;
	public static final float STING_DAMAGE = 4.0F;
	/** The venom: Poison II, for this long. */
	public static final int POISON_TICKS = 60;
	public static final int POISON_LEVEL = 1;

	/** Hurt below this share of its health, standing on sand, it goes under... */
	public static final float BURROW_HEALTH = 0.4F;
	/** ...for this long, out of reach of anything... */
	public static final int BURROW_TICKS = 40;
	/** ...and not again for this long after it comes back up. */
	public static final int BURROW_COOLDOWN = 200;
	/** With nothing to hunt for this long, on sand, it buries itself again to wait. */
	public static final int REBURY_TICKS = 120;

	/** How close someone has to come to wake a buried one, by how they're moving. */
	public static double wakeRange(boolean sprinting, boolean sneaking) {
		if (sneaking) {
			return SNEAK_WAKE_RANGE;
		}
		return sprinting ? SPRINT_WAKE_RANGE : WAKE_RANGE;
	}

	/** Whether someone this close, moving like this, wakes it. */
	public static boolean wakes(double distance, boolean sprinting, boolean sneaking) {
		return distance <= wakeRange(sprinting, sneaking);
	}

	/** What it does next at this distance: 2 the sting (drawn back first), 1 the pincers, 0 nothing yet. */
	public static int attack(double distance, long now, long pinchReady, long stingReady) {
		if (distance <= STING_REACH && now >= stingReady) {
			return 2;
		}
		if (distance <= PINCH_REACH && now >= pinchReady) {
			return 1;
		}
		return 0;
	}

	/** Whether it goes under to come up elsewhere: hurt enough, on sand, and not done so lately. */
	public static boolean burrows(float health, float maxHealth, boolean onSand, long now, long ready) {
		return onSand && maxHealth > 0 && health / maxHealth < BURROW_HEALTH && now >= ready;
	}
}
