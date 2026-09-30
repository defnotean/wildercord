package dev.wildercord.cast;

/**
 * The pure rules of Soar ({@link Soar}), with no Minecraft types so they're unit-tested: how long a flight
 * lasts, when its warning comes, how fast it flies, who it lifts, how long a grounding hit keeps the wind
 * away, and what becomes of a flight found on a player as they log in.
 */
public final class SoarRules {
	private SoarRules() {}

	/** A flight's length at duration 1 (Extend doubles it, Frugal takes 40% off): 20 seconds. */
	public static final int BASE_TICKS = 400;
	/** However it's lengthened, no flight lasts longer than this: a minute and a half. */
	public static final int MAX_TICKS = 1800;
	/** The shortest flight there is, so a much shortened one still does something: a second. */
	public static final int MIN_TICKS = 20;
	/** The wind starts to fade this long before a flight ends: 3 seconds, time enough to think about the ground. */
	public static final int WARNING_TICKS = 60;
	/**
	 * Flying speed while soaring: three fifths of creative's, so it stays a stroll through the sky rather
	 * than a race (sprinting doubles it, and rising and sinking follow it, as in creative).
	 */
	public static final float FLY_SPEED = 0.03F;
	/** Vanilla's own flying speed, which a player has when nothing has changed it. */
	public static final float DEFAULT_FLY_SPEED = 0.05F;
	/** A grounding hit (a pull, Weigh, Downdraft) keeps the wind from lifting that player again for 3 seconds. */
	public static final int GROUNDED_TICKS = 60;
	/**
	 * The gentle descent after a flight guards its faller this long at most: longer than a slow fall from the
	 * top of the world to the bottom, and a limit so nothing can hold it open forever.
	 */
	public static final int DESCENT_TICKS = 1200;
	/** How often a flier's place is checked against the dungeons' wards: a lookup of structures, so not every tick. */
	public static final int WARD_CHECK_TICKS = 10;
	/** A soft gust while flying, about this often (a third either way, so it never ticks like a clock). */
	public static final int GUST_TICKS = 50;

	/** How long a flight cast at {@code duration} (Extend, Frugal, the caster's circles...) lasts, in ticks. */
	public static int flightTicks(double duration) {
		double ticks = BASE_TICKS * Math.max(0, duration);
		return (int) Math.max(MIN_TICKS, Math.min(MAX_TICKS, Math.round(ticks)));
	}

	/**
	 * When a flight of {@code ticks} cast at {@code now} ends. On someone already soaring until {@code until}
	 * it's the later of the two: casting again never cuts a flight short.
	 */
	public static long renewed(long now, long until, boolean soaring, int ticks) {
		long fresh = now + ticks;
		return soaring ? Math.max(until, fresh) : fresh;
	}

	/** Whether a flight could be given to a player, and if not, why. */
	public enum Lift {
		/** Given (or renewed). */
		LIFT,
		/** Creative or spectator: their own flight is never touched. */
		CREATIVE,
		/** They can already fly by some other means, which isn't Soar's to give or take away. */
		ALREADY_FLIES,
		/** In a dungeon's warded arena or vault, where the ward stills the wind. */
		WARDED,
		/** A grounding hit knocked them out of the air a moment ago. */
		GROUNDED
	}

	/**
	 * Whether Soar lifts a player: never one in creative or spectator, never one who can already fly unless
	 * that flight is Soar's own ({@code soaring}, renewed), never inside a ward, and not while grounded.
	 */
	public static Lift lift(boolean creativeOrSpectator, boolean mayFly, boolean soaring, boolean warded, boolean grounded) {
		if (creativeOrSpectator) {
			return Lift.CREATIVE;
		}
		if (mayFly && !soaring) {
			return Lift.ALREADY_FLIES;
		}
		if (warded) {
			return Lift.WARDED;
		}
		if (grounded) {
			return Lift.GROUNDED;
		}
		return Lift.LIFT;
	}

	/** Whether the wind should start fading now: once, in the last {@link #WARNING_TICKS} of a flight still going. */
	public static boolean warnNow(long now, long until, boolean warned) {
		return !warned && now < until && until - now <= WARNING_TICKS;
	}

	/**
	 * Whether a flight's end (or a descent's) is too far off to be real: further than any flight can last. A
	 * player carried over from another world keeps that world's clock, and a flight from it can't be trusted.
	 */
	public static boolean stale(long now, long until) {
		return until - now > Math.max(MAX_TICKS, DESCENT_TICKS) + WARNING_TICKS;
	}

	/** What becomes of a flight found on a player logging in (after a logout, a restart or a crash). */
	public enum Login {
		/** Still time left: the flight goes on where it was, hovering if they're in the air. */
		RESUME,
		/** Over while they were away (or not to be trusted): whatever was left of it goes, and they're let down gently. */
		DESCEND
	}

	public static Login onLogin(long now, long until, boolean falling) {
		if (falling || stale(now, until) || now >= until) {
			return Login.DESCEND;
		}
		return Login.RESUME;
	}

	/**
	 * The flying speed to remember as the one from before a flight: what the player has, unless it's
	 * Soar's own (left over from a flight that was never tidied away), which can't have been theirs.
	 */
	public static float priorSpeed(float current) {
		return same(current, FLY_SPEED) ? DEFAULT_FLY_SPEED : current;
	}

	/** The flying speed to put back when a flight ends: the one from before, unless something else has changed it since. */
	public static float restoredSpeed(float current, float before) {
		return same(current, FLY_SPEED) ? before : current;
	}

	/** Whether a player grounded until {@code groundedUntil} is still grounded at {@code now}. */
	public static boolean grounded(long now, long groundedUntil) {
		return now < groundedUntil;
	}

	/** Whole seconds left of being grounded (at least 1 while it lasts, 0 once it's over). */
	public static int groundedSecondsLeft(long now, long groundedUntil) {
		return grounded(now, groundedUntil) ? (int) Math.max(1, (groundedUntil - now + 19) / 20) : 0;
	}

	/** How loud the soft gust is for a flier moving {@code speed} blocks a tick: quiet while hovering, fuller at speed. */
	public static float gustVolume(double speed) {
		return (float) (0.18 + 0.27 * Math.min(1.0, Math.max(0, speed) / 0.6));
	}

	private static boolean same(float a, float b) {
		return Math.abs(a - b) < 1.0E-5F;
	}
}
