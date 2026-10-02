package dev.wildercord.duel;

import java.util.UUID;

/**
 * A duel between two players, as a plain state machine (no Minecraft types, so it's unit-tested):
 * a challenge that waits for an answer, a countdown, the fight, and how it ended. The server
 * ({@link Duels}) feeds it the clock and what happened; it decides who won.
 */
public final class DuelRules {
	private DuelRules() {}

	/** How long a challenge waits for an answer: 30 seconds. */
	public static final int CHALLENGE_TICKS = 600;
	/** The countdown before the first blow counts: 3 seconds. */
	public static final int COUNTDOWN_TICKS = 60;
	/** How far from where the duel began either duellist may go: 40 blocks. */
	public static final double ARENA_RADIUS = 40.0;
	/** A duel nobody wins in 5 minutes is a draw. */
	public static final int MAX_FIGHT_TICKS = 6000;
	/** How long a challenger waits before challenging again: 10 seconds. */
	public static final int CHALLENGE_COOLDOWN_TICKS = 200;
	/** Hurt by anything this recently (10 seconds), a player can't start a duel. */
	public static final int HURT_TICKS = 200;
	/** In a fight with another player this recently (30 seconds), a player can't start a duel. */
	public static final int PVP_TICKS = 600;
	/** After a duel ends, how long before either of its duellists can start another: 30 seconds. */
	public static final int DUEL_COOLDOWN_TICKS = 600;
	/** When something never happened. */
	public static final long NEVER = Long.MIN_VALUE;

	public enum Phase { COUNTDOWN, FIGHTING, OVER }

	/**
	 * How a duel ended. Every ending but a draw and an interruption has a winner and a loser. An
	 * interrupted duel (someone else struck one of the duellists) counts for nobody.
	 */
	public enum Ending { KNOCKOUT, LEFT_AREA, LOGGED_OFF, DIED, DRAW, INTERRUPTED }

	/** Why a player can't start a duel right now, or {@link #NONE}. */
	public enum Refusal { NONE, HURT, PVP, COOLDOWN }

	/**
	 * Whether a player may start a duel: not hurt in the last {@link #HURT_TICKS}, not fighting another
	 * player in the last {@link #PVP_TICKS}, and not in a duel that ended in the last
	 * {@link #DUEL_COOLDOWN_TICKS}. Times are server ticks, {@link #NEVER} for never.
	 */
	public static Refusal ready(long now, long lastHurt, long lastPvp, long lastDuel) {
		if (within(now, lastPvp, PVP_TICKS)) {
			return Refusal.PVP;
		}
		if (within(now, lastHurt, HURT_TICKS)) {
			return Refusal.HURT;
		}
		if (within(now, lastDuel, DUEL_COOLDOWN_TICKS)) {
			return Refusal.COOLDOWN;
		}
		return Refusal.NONE;
	}

	/** Whether {@code then} was less than {@code ticks} before {@code now}. */
	public static boolean within(long now, long then, int ticks) {
		return then != NEVER && now >= then && now - then < ticks;
	}

	/**
	 * What a duellist's health is put back to when the duel ends: the harm their opponent did them
	 * ({@code taken}) is given back, never past what they had when it began, and never over the most
	 * they can have; anything else that hurt them meanwhile stays, and gains made meanwhile are kept.
	 * A duel never heals past where it found you, and never heals what it didn't do.
	 */
	public static float restored(float now, float before, float max, float taken) {
		return Math.min(max, Math.max(now, Math.min(before, now + taken)));
	}

	/**
	 * How recently the opponent must have struck a duellist (this tick or the one before) for harm landing with no
	 * spell's name on it, an arrow's poison or a flaming blade's fire, to count as theirs.
	 */
	public static final int STRUCK_TICKS = 2;

	/** Whether harm landing at {@code now} with no spell's name on it is the opponent's: they struck at {@code struck}, just now. */
	public static boolean byOpponent(long now, long struck) {
		return within(now, struck, STRUCK_TICKS);
	}

	/**
	 * Whether a duel's end takes an effect (or fire) off a duellist: harmful, laid on them by their opponent, and not
	 * something they had when it began. Harm from anything else (a monster's poison, lava, a potion) is left as it is.
	 */
	public static boolean undone(boolean harmful, boolean hadBefore, boolean byOpponent) {
		return harmful && !hadBefore && byOpponent;
	}

	/** An effect a duellist had when the duel began, put back with the time the duel took off it (-1 lasts forever). */
	public static int remaining(int duration, long elapsed) {
		return duration < 0 ? duration : (int) Math.max(0, duration - elapsed);
	}

	/**
	 * The terms a duel is fought on: how far from where it began either duellist may go, the countdown, how long it may run before it's a draw,
	 * the health that ends it ({@code knockoutAt}: 0 when only a blow that would kill does, as in a duel; a spar ends at one heart), the health a
	 * duellist brought down is left on, and whether it goes into the duel record. A duel's are {@link #DUEL}.
	 */
	public record Terms(double radius, int countdownTicks, int maxFightTicks, float knockoutAt, float leftOn, boolean recorded) {
		public static final Terms DUEL = new Terms(ARENA_RADIUS, COUNTDOWN_TICKS, MAX_FIGHT_TICKS, 0F, 1.0F, true);

		public Terms {
			radius = Math.max(1, radius);
			countdownTicks = Math.max(0, countdownTicks);
			maxFightTicks = Math.max(1, maxFightTicks);
			knockoutAt = Math.max(0, knockoutAt);
			leftOn = Math.max(1.0F, Math.max(leftOn, knockoutAt));
		}

		/** Whether a duellist left on {@code health} by their opponent has lost (never on these terms when only a killing blow ends it). */
		public boolean knockedOut(float health) {
			return knockoutAt > 0 && health <= knockoutAt + 1.0E-4F;
		}

		/** Whether a point is outside the ground that began at the centre (horizontal distance only). */
		public boolean outside(double centreX, double centreZ, double x, double z) {
			return DuelRules.outside(centreX, centreZ, x, z, radius);
		}
	}

	/** A challenge from one player to another, made at {@code made} (game time). */
	public record Challenge(UUID from, UUID to, long made) {
		public boolean expired(long now) {
			return now - made > CHALLENGE_TICKS;
		}
	}

	/** One duel. Mutable: the server keeps one per pair of duellists. */
	public static final class Duel {
		public final UUID a;
		public final UUID b;
		public final long start;
		public final Terms terms;
		private Phase phase = Phase.COUNTDOWN;
		private UUID winner;
		private UUID loser;
		private Ending ending;

		public Duel(UUID a, UUID b, long start) {
			this(a, b, start, Terms.DUEL);
		}

		public Duel(UUID a, UUID b, long start, Terms terms) {
			if (a.equals(b)) {
				throw new IllegalArgumentException("a player can't duel themselves");
			}
			this.a = a;
			this.b = b;
			this.start = start;
			this.terms = terms == null ? Terms.DUEL : terms;
		}

		public Phase phase() {
			return phase;
		}

		public boolean involves(UUID player) {
			return a.equals(player) || b.equals(player);
		}

		public UUID opponent(UUID player) {
			return a.equals(player) ? b : b.equals(player) ? a : null;
		}

		/** The countdown's second still to come (3, 2, 1), or 0 once the fight has begun. */
		public int countdown(long now) {
			long left = start + terms.countdownTicks() - now;
			return left <= 0 ? 0 : (int) ((left + 19) / 20);
		}

		/**
		 * Moves the clock on: the countdown gives way to the fight, and a fight that runs too long
		 * ends in a draw. Returns true when the phase changed.
		 */
		public boolean tick(long now) {
			if (phase == Phase.COUNTDOWN && now >= start + terms.countdownTicks()) {
				phase = Phase.FIGHTING;
				return true;
			}
			if (phase == Phase.FIGHTING && now - start - terms.countdownTicks() >= terms.maxFightTicks()) {
				end(null, null, Ending.DRAW);
				return true;
			}
			return false;
		}

		/** Whether a blow between the two duellists counts right now (not during the countdown, not after). */
		public boolean fighting() {
			return phase == Phase.FIGHTING;
		}

		/** One duellist was brought down to their last health by the other. */
		public void knockout(UUID loser) {
			if (phase == Phase.FIGHTING && involves(loser)) {
				end(opponent(loser), loser, Ending.KNOCKOUT);
			}
		}

		/** Someone else struck one of the duellists: the duel is off, and counts for nobody. */
		public void interrupt() {
			if (phase != Phase.OVER) {
				end(null, null, Ending.INTERRUPTED);
			}
		}

		/** One duellist gave the duel up: left the area, logged off or died to something else. Counts in the countdown too. */
		public void forfeit(UUID quitter, Ending why) {
			if (phase != Phase.OVER && involves(quitter)) {
				end(opponent(quitter), quitter, why);
			}
		}

		private void end(UUID winner, UUID loser, Ending ending) {
			this.phase = Phase.OVER;
			this.winner = winner;
			this.loser = loser;
			this.ending = ending;
		}

		public UUID winner() {
			return winner;
		}

		public UUID loser() {
			return loser;
		}

		public Ending ending() {
			return ending;
		}
	}

	/** Whether a point is outside the arena that began at the centre (horizontal distance only). */
	public static boolean outside(double centreX, double centreZ, double x, double z) {
		return outside(centreX, centreZ, x, z, ARENA_RADIUS);
	}

	/** Whether a point is more than {@code radius} from the centre (horizontal distance only). */
	public static boolean outside(double centreX, double centreZ, double x, double z, double radius) {
		double dx = x - centreX;
		double dz = z - centreZ;
		return dx * dx + dz * dz > radius * radius;
	}

	/** How long a duel on {@code terms} has been fought at {@code now} (ticks; 0 during the countdown). */
	public static long fought(Duel duel, long now) {
		return Math.max(0, now - duel.start - duel.terms.countdownTicks());
	}
}
