package dev.wildercord.cast;

/**
 * The fused effects of frost's pure rules, with no Minecraft types so they're unit-tested: how many
 * beats Frostbite's cold has and how deep each one slows, how Cryostasis spreads its healing over the
 * seal, how long an enemy must stand on a Rime Seal, and how soon Absolute Zero may freeze the same
 * creature solid again. {@link FusedFrost} and {@link FusedFrostWards} apply them.
 */
public final class FusedFrostRules {
	private FusedFrostRules() {}

	/** Ticks an enemy must stand on a Rime Seal before it freezes. */
	public static final int SEAL_STAND_TICKS = 20;
	/** Cryostasis heals once every this many ticks while the ice holds. */
	public static final int SEAL_HEAL_EVERY = 10;
	/** The longest a Cryostasis holds, however it's extended: 4 seconds, so untouchable never lasts long. */
	public static final int SEAL_MAX_TICKS = 80;

	/** Frostbloom: each striker it freezes heals the ally this much, up to this many times a cast. */
	public static final float BLOOM_HEAL = 1.0F;
	public static final int BLOOM_HEALS = 4;
	/** Black Ice: the survivors of a shatter are brittle for this long (3 seconds). */
	public static final int CASCADE_TICKS = 60;
	/** Cryostasis: the burst as the ice opens: how far, how hard it throws, what it deals and how long it slows. */
	public static final double BURST_RADIUS = 3.0;
	public static final double BURST_KNOCK = 1.2;
	public static final double BURST_DAMAGE = 3.0;
	public static final int BURST_SLOW_TICKS = 60;

	/** Frostbite's closing freeze, in seconds. */
	public static final double FROSTBITE_HOLD_SECONDS = 1.5;
	/** Blizzard: how far its whiteout walks each second, and what it bites for each second. */
	public static final double BLIZZARD_WALK = 1.5;
	public static final double BLIZZARD_BITE = 1.5;

	/** Absolute Zero on a creature showing {@code n} signs of cold (0 to 4): 2 damage and 2.5 for each (4.5, 7, 9.5, 12). */
	public static double zeroDamage(int n) {
		return 2.0 + 2.5 * Math.max(0, Math.min(4, n));
	}

	/** How long it holds a creature showing {@code n} signs of cold: 1 second and half a second for each (half as long on a player). */
	public static double zeroHoldSeconds(int n, boolean player) {
		double seconds = 1.0 + 0.5 * Math.max(0, Math.min(4, n));
		return player ? seconds / 2 : seconds;
	}

	/** Frostbite's beats: one a second for five seconds, times the duration (always at least one). */
	public static int frostbiteBeats(double duration) {
		return (int) Math.max(1, Math.round(5 * duration));
	}

	/**
	 * The Slowness amplifier Frostbite's cold has reached at beat {@code beat} (1 to {@code beats}):
	 * I, then II, then III, in about equal thirds, so the last beat is always III.
	 */
	public static int frostbiteStage(int beat, int beats) {
		if (beats <= 1) {
			return 2;
		}
		return Math.max(0, Math.min(2, 3 * beat / (beats + 1)));
	}

	/** How many times a Cryostasis of {@code ticks} heals: once every {@link #SEAL_HEAL_EVERY} ticks, at least once. */
	public static int sealHeals(int ticks) {
		return Math.max(1, ticks / SEAL_HEAL_EVERY);
	}

	/** What each of a Cryostasis's heals gives, so that together they come to {@code total}. */
	public static float sealHealEach(double total, int heals) {
		return (float) (total / Math.max(1, heals));
	}

	/** Whether an enemy first seen on a Rime Seal at {@code since} has stood there long enough at {@code now}. */
	public static boolean stoodLongEnough(int since, int now) {
		return now - since >= SEAL_STAND_TICKS;
	}

	/** After a Cryostasis ends (or is refused), the same creature can't be sealed again for this many ticks: 10 seconds. */
	public static final int SEAL_LOCKOUT_TICKS = 200;

	/**
	 * Until when a creature can't be sealed, after a Cryostasis on it ended (or was refused) at {@code at},
	 * given the wait it already had ({@code current}): 10 seconds from then, never shortening it.
	 */
	public static long lockedUntil(long current, long at) {
		return Math.max(current, at + SEAL_LOCKOUT_TICKS);
	}

	/** Whether a creature locked out until {@code lockedUntil} may be sealed at {@code now}. */
	public static boolean maySeal(long lockedUntil, long now) {
		return now >= lockedUntil;
	}

	/**
	 * After Absolute Zero freezes a creature solid, it can't freeze it solid again until this many ticks after the
	 * ice lets go: 3 seconds. Its first hit leaves the creature slowed, so without this every later hit (a Linger,
	 * a Zone's pulses, a second copy) would be a certain freeze and 7 damage.
	 */
	public static final int ZERO_LOCKOUT_TICKS = 60;

	/** Until when Absolute Zero can't freeze a creature solid again, having frozen it at {@code at} for {@code hold} ticks. */
	public static long zeroLockedUntil(long at, int hold) {
		return at + hold + ZERO_LOCKOUT_TICKS;
	}

	/** Whole seconds left (rounded up) until {@code lockedUntil}, for telling the caster. */
	public static int secondsLeft(long lockedUntil, long now) {
		return (int) Math.max(0, (lockedUntil - now + 19) / 20);
	}
}
