package dev.wildercord.aura;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sparring, the pure part: two swordsmen trying their blades on each other on grounds marked by cloth standards, where nobody dies or loses anything. Shared
 * by the server ({@code aura.Spars}), the HUD, the Aura page and the unit tests; the server-tunable numbers are also in the config's
 * {@code aura} section.
 *
 * <ul>
 * <li><b>The challenge</b>: a <b>salute</b>, sneaking and using the blade on another swordsman (a deliberate gesture: nothing an ordinary
 * swing or the Aura key does). They have {@link #OFFER_TICKS} to salute back, and that's the only way a spar ever begins: nobody can be made
 * to spar.</li>
 * <li><b>The ring</b>: four cloth standards {@link #RING_RADIUS} blocks round the point between them; a count-in of {@link #COUNT_TICKS}; then
 * blades and aura only (spells, shots and pets don't reach a sparring partner). It ends when either is brought to <b>one heart</b>
 * ({@link #KNOCKOUT}: held there, never killed), or steps out of the ring (walking out is always free, so nobody can be trapped in it; the
 * one who left loses), or after {@link #MAX_FIGHT_TICKS} (even).</li>
 * <li><b>Afterwards</b> both are put back as they began: the health and harm their partner dealt, the aura they spent (unless they awakened:
 * an awakening is always paid for), and nothing built in the ring outlasts it (momentum starts and ends at nothing, an awakening ends).</li>
 * <li><b>What it teaches</b>: both earn aura experience, the winner more ({@link #xp}), from a spar that really was one (fought at least
 * {@link #MIN_FIGHT_TICKS}, each landing a blow), and only {@link #DAILY} spars a day count for any one pair. A spar grows no blade, ranks no
 * technique and teaches no mastery.</li>
 * </ul>
 */
public final class SparRules {
	private SparRules() {}

	// ------------------------------------------------------------------ the challenge

	/** A salute (and the answering salute) reaches a swordsman this close (blocks); the game's own reach for using something on them is about 3. */
	public static final double REACH = 4.5;
	/** How long a salute waits to be answered (ticks). */
	public static final int OFFER_TICKS = 400;
	/** A salute renewed this soon (ticks) is the same challenge, and doesn't ring again. */
	public static final int RESALUTE = 100;
	/** Each must have at least this share of their health to begin (a spar starting at two hearts would be over at the first blow). */
	public static final double READY_SHARE = 0.5;

	/** Why a spar can't begin now (or a salute be made), checked in this order. */
	public enum Refusal {
		/** Sparring is off on this server, or aura is. */
		OFF,
		/** One of them hasn't learned a breathing method. */
		NO_METHOD,
		/** One of them holds no blade. */
		NO_WEAPON,
		/** In creative or spectating: nothing can hurt them, so there's nothing to spar. */
		UNHURTABLE,
		/** Too far apart, or not in the same world. */
		TOO_FAR,
		/** One of them is already in a spar or a duel. */
		BUSY,
		/** One of them is awakened (let it burn out first). */
		AWAKENED,
		/** One of them has less than half their health. */
		HURT;

		/** Its line's language key. */
		public String key() {
			return "message.wildercord.aura.spar.refused." + name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/**
	 * Why a spar between two swordsmen can't begin, or null when it can: {@code on} (sparring and aura on), then for the two of them (the
	 * arrays, one entry each) a method learned, a blade in hand, able to be hurt, then the distance between them, busy, awakened, and their
	 * health as a share of the most they can have.
	 */
	public static Refusal refusal(boolean on, boolean[] method, boolean[] weapon, boolean[] hurtable, double apart, boolean sameWorld, boolean[] busy,
			boolean[] awakened, double[] health) {
		if (!on) {
			return Refusal.OFF;
		}
		if (!all(method)) {
			return Refusal.NO_METHOD;
		}
		if (!all(weapon)) {
			return Refusal.NO_WEAPON;
		}
		if (!all(hurtable)) {
			return Refusal.UNHURTABLE;
		}
		if (!sameWorld || apart > REACH) {
			return Refusal.TOO_FAR;
		}
		if (any(busy)) {
			return Refusal.BUSY;
		}
		if (any(awakened)) {
			return Refusal.AWAKENED;
		}
		for (double h : health) {
			if (h < READY_SHARE - 1.0E-6) {
				return Refusal.HURT;
			}
		}
		return null;
	}

	private static boolean all(boolean[] values) {
		for (boolean v : values) {
			if (!v) {
				return false;
			}
		}
		return true;
	}

	private static boolean any(boolean[] values) {
		for (boolean v : values) {
			if (v) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ the ring

	/** The ring's radius by default (blocks; the server's {@code spar_ring_radius}). */
	public static final double RING_RADIUS = 7.0;
	public static final double MIN_RADIUS = 4.0;
	public static final double MAX_RADIUS = 16.0;
	/** The count-in: three seconds. */
	public static final int COUNT_TICKS = 60;
	/** A spar nobody has won in two minutes is even. */
	public static final int MAX_FIGHT_TICKS = 2400;
	/** Brought to this health (one heart) by their partner, a swordsman has lost: held there, never lower. */
	public static final float KNOCKOUT = 2.0F;
	/** Within this many blocks of the ring's edge, the swordsman's own screen says so. */
	public static final double EDGE_WARNING = 1.5;
	/** How long the winner's moment lingers on both screens after the end (ticks). */
	public static final int OUTCOME_TICKS = 70;
	/** How often the ring is drawn again (ticks), and how long each drawing lasts. */
	public static final int RING_EVERY = 10;
	public static final int RING_LIFE = 14;

	/** Whether ({@code x}, {@code z}) is outside a ring of {@code radius} round ({@code cx}, {@code cz}) (only the flat distance counts). */
	public static boolean outside(double cx, double cz, double x, double z, double radius) {
		double dx = x - cx;
		double dz = z - cz;
		return dx * dx + dz * dz > radius * radius;
	}

	/** How far ({@code x}, {@code z}) is inside the ring's edge (blocks; negative outside). */
	public static double inside(double cx, double cz, double x, double z, double radius) {
		return radius - Math.hypot(x - cx, z - cz);
	}

	/** The count-in's second still to come at {@code t} ticks after the spar began (3, 2, 1), or 0 once it's on. */
	public static int count(long t) {
		long left = COUNT_TICKS - t;
		return left <= 0 ? 0 : (int) ((left + 19) / 20);
	}

	// ------------------------------------------------------------------ what it teaches

	/** A spar counts only after this long fighting (ticks): ten seconds, and each having landed a blow. */
	public static final int MIN_FIGHT_TICKS = 200;
	/** How many spars a day count for any one pair (the server's {@code spars_per_day}). */
	public static final int DAILY = 3;
	/** A day, in the server's ticks (it doesn't skip ahead when everyone sleeps). */
	public static final int DAY = 24000;
	/** What a counted spar teaches, as a share of the road from the swordsman's stage to the next: the winner, the loser, an even spar. */
	public static final double XP_WIN = 0.015;
	public static final double XP_LOSS = 0.01;
	public static final double XP_EVEN = 0.01;
	/** Never less than this (experience) for a counted spar, so a Glow swordsman learns something too. */
	public static final double XP_FLOOR = 2.0;

	/** How a spar ended for one of them. */
	public enum Result { WON, LOST, EVEN }

	/** The day ({@code gameTime} / {@link #DAY}). */
	public static long day(long gameTime) {
		return Math.floorDiv(gameTime, DAY);
	}

	/**
	 * How much a partner of {@code theirs} stage teaches a swordsman of {@code mine}: one at their own stage or above teaches in full (a quarter
	 * more a stage above, half again at most), one a stage below a little over half, and one further below only a quarter.
	 */
	public static double partner(int mine, int theirs) {
		if (theirs >= mine) {
			return Math.min(1.5, 1.0 + 0.25 * (theirs - mine));
		}
		return theirs == mine - 1 ? 0.6 : 0.25;
	}

	/** The road from {@code stage} to the next (experience; nothing at the last stage). */
	public static double road(int stage) {
		if (stage <= AuraRules.NONE || stage >= AuraRules.MAX_STAGE) {
			return 0;
		}
		return AuraRules.threshold(stage + 1) - AuraRules.threshold(stage);
	}

	/**
	 * The aura experience a counted spar teaches a swordsman at {@code stage} whose road to the next is {@code road}, against a partner at
	 * {@code partnerStage}, for {@code result}, times the server's {@code spar_xp} ({@code multiplier}).
	 */
	public static double xp(int stage, double road, int partnerStage, Result result, double multiplier) {
		if (stage <= AuraRules.NONE || multiplier <= 0) {
			return 0;
		}
		double share = switch (result) {
			case WON -> XP_WIN;
			case LOST -> XP_LOSS;
			case EVEN -> XP_EVEN;
		};
		return Math.max(XP_FLOOR, Math.max(0, road) * share) * partner(stage, partnerStage) * multiplier;
	}

	/** Whether a spar that ended after {@code fought} ticks of fighting, with both having landed a blow or not, was a spar at all. */
	public static boolean real(long fought, boolean bothStruck) {
		return fought >= MIN_FIGHT_TICKS && bothStruck;
	}

	/**
	 * Each swordsman's sparring record: wins, losses and evens for good, and how many spars counted today with each partner (forgotten when
	 * the day turns).
	 */
	public record Log(int wins, int losses, int evens, long day, Map<UUID, Integer> today) {
		public static final Log NONE = new Log(0, 0, 0, Long.MIN_VALUE, Map.of());

		public Log {
			today = today == null ? Map.of() : Map.copyOf(today);
		}

		/** How many spars with {@code partner} have counted on {@code day}. */
		public int counted(UUID partner, long day) {
			return day != this.day ? 0 : today.getOrDefault(partner, 0);
		}

		/** One more counted with {@code partner} on {@code day} (a new day starts afresh). */
		public Log count(UUID partner, long day) {
			Map<UUID, Integer> next = new HashMap<>(day == this.day ? today : Map.of());
			next.merge(partner, 1, Integer::sum);
			return new Log(wins, losses, evens, day, next);
		}

		/** The record with {@code result} added. */
		public Log record(Result result) {
			return switch (result) {
				case WON -> new Log(wins + 1, losses, evens, day, today);
				case LOST -> new Log(wins, losses + 1, evens, day, today);
				case EVEN -> new Log(wins, losses, evens + 1, day, today);
			};
		}

		/** Spars fought to an end in all. */
		public int total() {
			return wins + losses + evens;
		}
	}

	/** Whether another spar with this partner today still counts: fewer than {@code daily} have. */
	public static boolean counts(int countedToday, int daily) {
		return countedToday < Math.max(0, daily);
	}
}
