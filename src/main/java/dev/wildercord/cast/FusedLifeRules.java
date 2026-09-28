package dev.wildercord.cast;

/**
 * The pure rules of the fused effects of life and blood ({@link FusedLife}), with no Minecraft types so
 * they're unit-tested: Second Wind's lockout and what it leaves, what Transfusion gives, and whether a
 * blood price can be paid.
 */
public final class FusedLifeRules {
	private FusedLifeRules() {}

	/** Second Wind: once it has saved a creature, no new one takes on it for this long (a minute). */
	public static final int SECOND_WIND_LOCKOUT_TICKS = 1200;

	/** Transfusion never takes the giver below this. */
	public static final float TRANSFUSION_FLOOR = 2.0F;

	/**
	 * Whether a creature a Second Wind saved at game time {@code savedAt} is still locked out of a new one
	 * at {@code now} ({@code savedAt} below 0: never saved).
	 */
	public static boolean lockedOut(long savedAt, long now) {
		return savedAt >= 0 && now - savedAt < SECOND_WIND_LOCKOUT_TICKS;
	}

	/** Whole seconds left of a lockout (at least 1 while it lasts, 0 once it's over). */
	public static int lockoutSecondsLeft(long savedAt, long now) {
		if (!lockedOut(savedAt, now)) {
			return 0;
		}
		return (int) Math.max(1, (SECOND_WIND_LOCKOUT_TICKS - (now - savedAt) + 19) / 20);
	}

	/** The health a Second Wind leaves: 4 at power 1, never under 1 or over the creature's most. */
	public static float secondWindHealth(double power, float maxHealth) {
		return (float) Math.max(1.0, Math.min(maxHealth, 4 * power));
	}

	/**
	 * What Transfusion gives (the ally heals twice this): up to 4 at power 1, no more than half of what the
	 * ally is missing (so none is wasted), and never so much the giver drops below {@link #TRANSFUSION_FLOOR}
	 * ({@code free}, a creative player, gives without paying). 0 when there's nothing to give.
	 */
	public static double transfusionGift(double power, float giverHealth, float missing, boolean free) {
		double spare = free ? Double.MAX_VALUE : giverHealth - TRANSFUSION_FLOOR;
		double give = Math.min(4 * power, Math.min(missing / 2.0, spare));
		return give < 0.25 ? 0 : give;
	}

	/** Whether {@code health} can pay a blood price of {@code cost} and keep something (never the last of it). */
	public static boolean canPayBlood(float health, float cost) {
		return health > cost;
	}
}
