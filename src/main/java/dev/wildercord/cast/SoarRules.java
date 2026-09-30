package dev.wildercord.cast;

/**
 * The pure rules of Soar ({@link Soar}), with no Minecraft types so they're unit-tested: how long a flight
 * lasts, when its warning comes, how fast it flies, who it lifts, the rest the wings need afterwards, and what
 * becomes of a flight found on a player as they log in.
 *
 * <p>Soar is a burst of flight, not a way to live in the sky: one flight lasts its duration and no longer
 * (casting again mid-flight is refused), and when it ends, however it ends short of death, the wind won't lift
 * that player again for {@link #REST_TICKS}. Elytras and the long road keep their place.</p>
 */
public final class SoarRules {
	private SoarRules() {}

	/** A flight's length at duration 1 (Extend doubles it, Frugal takes 40% off): 20 seconds. */
	public static final int BASE_TICKS = 400;
	/** However it's lengthened, no flight lasts longer than this: 40 seconds, one Extend's worth. */
	public static final int MAX_TICKS = 800;
	/** The shortest flight there is, so a much shortened one still does something: a second. */
	public static final int MIN_TICKS = 20;
	/**
	 * After a flight ends (running out, grounded, set down by a ward or a new world, or run out while logged
	 * out: anything but death), the wings rest this long before Soar lifts that player again: 30 seconds.
	 */
	public static final int REST_TICKS = 600;
	/** The wind starts to fade this long before a flight ends: 3 seconds, time enough to think about the ground. */
	public static final int WARNING_TICKS = 60;
	/**
	 * Flying speed while soaring: three fifths of creative's, so it stays a stroll through the sky rather
	 * than a race (sprinting doubles it, and rising and sinking follow it, as in creative).
	 */
	public static final float FLY_SPEED = 0.03F;
	/** Vanilla's own flying speed, which a player has when nothing has changed it. */
	public static final float DEFAULT_FLY_SPEED = 0.05F;
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

	/** Whether a flight could be given to a player, and if not, why. */
	public enum Lift {
		/** Given. */
		LIFT,
		/** Creative or spectator: their own flight is never touched. */
		CREATIVE,
		/** Already soaring: a flight is never lengthened or started over mid-air. */
		ALREADY_SOARING,
		/** They can already fly by some other means, which isn't Soar's to give or take away. */
		ALREADY_FLIES,
		/** In a dungeon's warded arena or vault, where the ward stills the wind. */
		WARDED,
		/** Their wings are resting after a flight. */
		RESTING
	}

	/**
	 * Whether Soar lifts a player: never one in creative or spectator, never one already soaring (no renewing),
	 * never one who can already fly some other way, never inside a ward, and not while their wings rest.
	 */
	public static Lift lift(boolean creativeOrSpectator, boolean mayFly, boolean soaring, boolean warded, boolean resting) {
		if (creativeOrSpectator) {
			return Lift.CREATIVE;
		}
		if (soaring) {
			return Lift.ALREADY_SOARING;
		}
		if (mayFly) {
			return Lift.ALREADY_FLIES;
		}
		if (warded) {
			return Lift.WARDED;
		}
		if (resting) {
			return Lift.RESTING;
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

	/**
	 * Whether wings resting until {@code restUntil} still rest at {@code now}. A rest further off than any rest
	 * can be (another world's clock) isn't one.
	 */
	public static boolean resting(long now, long restUntil) {
		return now < restUntil && restUntil - now <= REST_TICKS + WARNING_TICKS;
	}

	/**
	 * When the wings rest until once a flight ends at {@code now}: {@link #REST_TICKS} on, or a rest already
	 * running if that's longer (a rest from another clock is forgotten).
	 */
	public static long restUntil(long now, long current) {
		long fresh = now + REST_TICKS;
		return resting(now, current) ? Math.max(current, fresh) : fresh;
	}

	/** Whole seconds left of a rest (at least 1 while it lasts, 0 once it's over). */
	public static int restSecondsLeft(long now, long restUntil) {
		return resting(now, restUntil) ? (int) Math.max(1, (restUntil - now + 19) / 20) : 0;
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

	/** How loud the soft gust is for a flier moving {@code speed} blocks a tick: quiet while hovering, fuller at speed. */
	public static float gustVolume(double speed) {
		return (float) (0.18 + 0.27 * Math.min(1.0, Math.max(0, speed) / 0.6));
	}

	private static boolean same(float a, float b) {
		return Math.abs(a - b) < 1.0E-5F;
	}
}
