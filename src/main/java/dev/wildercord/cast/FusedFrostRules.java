package dev.wildercord.cast;

/**
 * The fused effects of frost's pure rules, with no Minecraft types so they're unit-tested: how many
 * beats Frostbite's cold has and how deep each one slows, how Cryostasis spreads its healing over the
 * seal, and how long an enemy must stand on a Rime Seal. {@link FusedFrost} and {@link FusedFrostWards}
 * apply them.
 */
public final class FusedFrostRules {
	private FusedFrostRules() {}

	/** Ticks an enemy must stand on a Rime Seal before it freezes. */
	public static final int SEAL_STAND_TICKS = 20;
	/** Cryostasis heals once every this many ticks while the ice holds. */
	public static final int SEAL_HEAL_EVERY = 10;

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

	/** Whole seconds left (rounded up) until {@code lockedUntil}, for telling the caster. */
	public static int secondsLeft(long lockedUntil, long now) {
		return (int) Math.max(0, (lockedUntil - now + 19) / 20);
	}
}
