package dev.wildercord.aura;

import java.util.Locale;

/**
 * Awakening, the pure part: a swordsman from Edge upward, their pool full and their momentum high, lets everything they hold go at
 * once. For a while (longer at each stage) they're <b>awakened</b>: the body's aura blazes and the eyes burn, arts cost nothing
 * ({@link #ART_PRICE}), momentum holds at its peak, and they're a little faster ({@link #SPEED}, {@link #ATTACK_SPEED}) and hit a
 * little harder ({@link #DAMAGE}, half that against a player). Each finisher landed while it lasts feeds it a second more
 * ({@link #FINISHER_EXTEND}, up to {@link #MAX_EXTEND}). When it ends what aura is left burns away and they're <b>spent</b>: slowed,
 * unable to gather aura at all, momentum emptied, for {@link #SPENT_TICKS}; the next awakening waits {@link #COOLDOWN_TICKS} from the
 * end. At Sovereign a Dominion raised while awakened is the method's own ({@link Sovereign}).
 *
 * <p>The input is the Aura key tapped, then pressed again at once and held ({@link #HOLD_TICKS}): a fifth way beside the tap (the
 * slash), the press while sneaking (the guard), the double tap (the step) and the hold (Dominion). A quick second press is still a
 * double tap; only a held one awakens.</p>
 *
 * <p>Shared by the server ({@code aura.Awakening}), the client (the key, the HUD, the body's aura) and the unit tests. Every number
 * a server owner may want to change is also in the config's {@code aura} section; the defaults live here.</p>
 */
public final class AwakeningRules {
	private AwakeningRules() {}

	// ------------------------------------------------------------------ when

	/** The stage awakening opens at. */
	public static final int FROM = AuraRules.EDGE;
	/** A pool this full (a share of its capacity) counts as full: a coated swing's own price never stands in the way. */
	public static final double POOL = 0.95;
	/** The momentum it asks for by default (the second tier): a fight going well. */
	public static final double MOMENTUM = 50.0;

	/** Whether {@code aura} out of {@code capacity} is a full pool. */
	public static boolean full(double aura, double capacity) {
		return capacity > 0 && aura >= capacity * POOL - 1.0E-6;
	}

	// ------------------------------------------------------------------ the input

	/**
	 * The Aura key tapped, then pressed again within the double tap's moment and held this long (ticks) awakens. Let go sooner and
	 * it was a double tap (quickly) or nothing at all (after the hold began).
	 */
	public static final int HOLD_TICKS = 14;
	/** A second press held this long (ticks) is no longer a tap: the charge shows from here. */
	public static final int CHARGE_SHOWS = 4;

	/** How far a held second press has come toward awakening (0 to 1), {@code held} ticks in. */
	public static float charge(double held) {
		if (held <= CHARGE_SHOWS) {
			return 0;
		}
		return (float) Math.max(0, Math.min(1, (held - CHARGE_SHOWS) / (HOLD_TICKS - CHARGE_SHOWS)));
	}

	// ------------------------------------------------------------------ how long

	/** How long it lasts at each stage (ticks): 12 seconds at Edge, 16 at Form, 20 at Sovereign. */
	private static final int[] TICKS = {0, 0, 0, 240, 320, 400};

	/** How long an awakening at {@code stage} lasts (ticks), times the server's {@code awakening_duration}; 0 below Edge. */
	public static int ticks(int stage, double scale) {
		int base = TICKS[AuraRules.clampStage(stage)];
		return base <= 0 ? 0 : Math.max(20, (int) Math.round(base * Math.max(0, scale)));
	}

	/** A finisher landed while awakened feeds it this many ticks more, and all its finishers together at most this many. */
	public static final int FINISHER_EXTEND = 20;
	public static final int MAX_EXTEND = 80;

	/** What a finisher adds now to an awakening already fed {@code extended} ticks. */
	public static int extend(int extended) {
		return Math.max(0, Math.min(FINISHER_EXTEND, MAX_EXTEND - Math.max(0, extended)));
	}

	// ------------------------------------------------------------------ after

	/** How long a swordsman is spent once it ends (ticks): slowed, gathering no aura at all. */
	public static final int SPENT_TICKS = 600;
	/** How long from its end before the next awakening (ticks): three minutes (the spent time runs inside it). */
	public static final int COOLDOWN_TICKS = 3600;
	/** The slow while spent: Slowness I. */
	public static final int SPENT_SLOW = 0;

	// ------------------------------------------------------------------ while it lasts

	/** What an art costs while awakened, as a share of its price (nothing, by default). */
	public static final double ART_PRICE = 0.0;
	/** How much faster: on foot (a share of base speed) and with the blade (a share of attack speed). */
	public static final double SPEED = 0.10;
	public static final double ATTACK_SPEED = 0.10;
	/** How much harder a coated blow lands, and the share of that left against another player (before their cap and the PvP scale). */
	public static final double DAMAGE = 0.15;
	public static final double PVP_SHARE = 0.5;
	/** The momentum it holds at: the peak. */
	public static final double HOLD = MomentumRules.PEAK;

	/** An art's price while awakened: {@code share} of it. */
	public static double price(double price, double share) {
		return Math.max(0, price) * Math.max(0, Math.min(1, share));
	}

	/** What a coated blow is multiplied by while awakened: {@code damage} more, against a player only {@link #PVP_SHARE} of it. */
	public static double damage(double damage, boolean againstPlayer) {
		return 1.0 + Math.max(0, damage) * (againstPlayer ? PVP_SHARE : 1.0);
	}

	// ------------------------------------------------------------------ the phases

	/** Where a swordsman's awakening stands: none yet, awakened, spent, or resting until the next. */
	public enum Phase {
		NONE, AWAKENED, SPENT, RESTING;

		public static Phase of(int ordinal) {
			Phase[] all = values();
			return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
		}
	}

	/** Whether an awakening in {@code phase} that runs until {@code until} burns at {@code now}. */
	public static boolean awakened(int phase, long until, long now) {
		return phase == Phase.AWAKENED.ordinal() && now < until;
	}

	/** Whether a swordsman whose awakening is in {@code phase}, spent until {@code spentUntil}, is spent at {@code now}. */
	public static boolean spent(int phase, long spentUntil, long now) {
		return phase == Phase.SPENT.ordinal() && now < spentUntil;
	}

	/**
	 * Where an awakening that ran until {@code until} stands once it has ended, seen at {@code now} (a swordsman who left mid-awakening
	 * comes back to what's left of it): spent until {@code until + spent} (or already resting if that has passed), and ready again
	 * {@code cooldown} after its end (never sooner than {@code readyAt}, what was written as it began).
	 */
	public record Ending(Phase phase, long spentUntil, long readyAt) {}

	public static Ending ended(long until, long readyAt, int spent, int cooldown, long now) {
		long spentUntil = until + Math.max(0, spent);
		long ready = Math.max(readyAt, until + Math.max(0, cooldown));
		return new Ending(now < spentUntil ? Phase.SPENT : Phase.RESTING, spentUntil, ready);
	}

	/** Why a swordsman can't awaken now (the first that applies, in this order). */
	public enum Refusal {
		/** The server has awakening off. */
		OFF,
		/** Below Edge. */
		STAGE,
		/** Awakened already. */
		AWAKENED,
		/** Still spent from the last. */
		SPENT,
		/** The last one's rest still runs. */
		RESTING,
		/** No aura weapon in hand. */
		NO_WEAPON,
		/**
		 * Momentum short of what it asks for. Asked before the pool: short of it, a lone tap of the Aura key goes at once (the slash),
		 * which spends the pool, and "your pool isn't full" would hide what the swordsman really lacks.
		 */
		MOMENTUM,
		/** The pool isn't full. */
		POOL;

		public String key() {
			return "message.wildercord.aura.awaken." + name().toLowerCase(Locale.ROOT);
		}
	}

	/**
	 * Why a swordsman can't awaken, or null when they can: {@code momentum} counts only where momentum works on the server
	 * ({@code momentumOn}); {@code needed} is the server's {@code awakening_momentum}.
	 */
	public static Refusal refusal(boolean on, int stage, boolean awakened, boolean spent, boolean resting, boolean weapon, double aura, double capacity,
			boolean momentumOn, double momentum, double needed) {
		if (!on) {
			return Refusal.OFF;
		}
		if (stage < FROM) {
			return Refusal.STAGE;
		}
		if (awakened) {
			return Refusal.AWAKENED;
		}
		if (spent) {
			return Refusal.SPENT;
		}
		if (resting) {
			return Refusal.RESTING;
		}
		if (!weapon) {
			return Refusal.NO_WEAPON;
		}
		if (momentumOn && momentum < needed - 1.0E-6) {
			return Refusal.MOMENTUM;
		}
		if (!full(aura, capacity)) {
			return Refusal.POOL;
		}
		return null;
	}

	// ------------------------------------------------------------------ the look

	/** The transformation: the body's aura gathers for {@link #BURST_AT} ticks, bursts, and has risen to its awakened form by {@link #RISE}. */
	public static final int BURST_AT = 6;
	public static final int RISE = 26;
	/** In its last this many ticks the awakened aura gutters, flickering down, so everyone sees it's ending. */
	public static final int GUTTER = 40;
	/** What awakening adds to the body's aura's strength (on top of a fight's and momentum's). */
	public static final float GLOW = 0.55F;
	/** What a spent swordsman's body aura is left at (a fraction of its usual strength): embers. */
	public static final float SPENT_GLOW = 0.3F;

	/**
	 * How far into its awakened form the body's aura is (0 to about 1.25), {@code age} ticks after it began with {@code left} ticks to
	 * go: drawn in until the burst (a little), surging past its form at the burst, settling at 1 by {@link #RISE}, and guttering in the
	 * last {@link #GUTTER} ticks (the flicker is the client's; this is its ceiling).
	 */
	public static float form(double age, double left) {
		if (age < 0 || left <= 0) {
			return 0;
		}
		float f;
		if (age < BURST_AT) {
			// Drawn in: a little, gathering.
			f = (float) (0.25 * age / BURST_AT);
		} else if (age < RISE) {
			// The burst, then settling into its form: a surge past it that eases back.
			double t = (age - BURST_AT) / (RISE - BURST_AT);
			f = (float) (1.0 + 0.25 * Math.sin(Math.PI * Math.min(1, t * 1.6)) * (1 - t));
			f = Math.max(f, (float) Math.min(1, 0.4 + 2.5 * t));
		} else {
			f = 1.0F;
		}
		if (left < GUTTER) {
			f *= (float) (0.35 + 0.65 * left / GUTTER);
		}
		return f;
	}

	// ------------------------------------------------------------------ the Sovereign's Dominion

	/**
	 * A Dominion raised while awakened (Sovereign): wider ({@link #RADIUS} times), longer ({@link #TIME} times), its foes weaker
	 * ({@link #WEAKEN} more), its chain leaping to {@link #CHAINS} foes, aura flowing back {@link #FLOW} times as fast, and shaped by
	 * the method: each of the ten has its own ({@link Flavour}).
	 */
	public static final class Sovereign {
		private Sovereign() {}

		public static final double RADIUS = 1.5;
		public static final double TIME = 1.5;
		public static final double WEAKEN = 0.1;
		public static final int CHAINS = 2;
		public static final double FLOW = 2.5;

		/** How often each flavour's pulse comes (ticks). */
		public static final int PULSE = 20;

		/** Ember: foes inside are set alight each pulse (a player at most the art cap). */
		public static final int EMBER_BURN = 60;
		/** Rime: raised, each foe inside freezes for a moment (a player held to the cap); after, they're chilled hard. */
		public static final int RIME_FREEZE = 40;
		public static final int RIME_CHILL = 2;
		/** Thunder: each pulse a bolt falls on a foe inside for this share of the weapon, and the shock holds it a moment. */
		public static final double THUNDER_BOLT = 0.45;
		public static final int THUNDER_SHOCK = 8;
		/** Gale: every other pulse an updraft lifts the foes inside; the owner standing inside turns shots aside. */
		public static final double GALE_LIFT = 0.55;
		public static final int GALE_AIRBORNE = 30;
		/** Stone: the owner standing inside is hardened (Resistance I, unmovable); each pulse wears the stance of foes inside. */
		public static final double STONE_STANCE = 4.0;
		/** Verdant: raised, foes inside are rooted; each pulse the owner and allies inside are mended. */
		public static final int VERDANT_ROOT = 40;
		public static final double VERDANT_MEND = 1.0;
		/** Hollow: raised, foes inside are silenced; every other tick they're drawn toward its heart. */
		public static final int HOLLOW_SILENCE = 60;
		public static final double HOLLOW_DRAG = 0.1;
		/** Starlit: each pulse a star falls on a foe inside for this share of the weapon (a starred one's bursts for more); aura flows faster. */
		public static final double STARLIT_STAR = 0.3;
		public static final double STARLIT_FLOW = 3.0;
		/** Hourglass: raised, foes inside are held still a moment; time drags inside (creatures slowed hard, shots slowed). */
		public static final int HOURGLASS_HOLD = 30;
		public static final int HOURGLASS_SLOW = 3;
		/** Crimson: every other pulse foes inside bleed for this share of the weapon; the owner's blows on foes inside drink a share. */
		public static final double CRIMSON_BLEED = 0.25;
		public static final double CRIMSON_DRINK = 0.25;

		/** The aura's flow inside for {@code methodId}'s awakened Dominion. */
		public static double flow(String methodId) {
			return Flavour.of(methodId) == Flavour.STARLIT ? STARLIT_FLOW : FLOW;
		}
	}

	/**
	 * The ten methods' Dominions raised while awakened, each named and shaped by its method (language keys
	 * {@code aura.wildercord.sovereign.<id>} and {@code .desc}). A method without its own raises the plain one ({@link #PLAIN}): only
	 * stronger.
	 */
	public enum Flavour {
		EMBER("ember", "throne_of_cinders"),
		RIME("rime", "court_of_winter"),
		THUNDER("thunder", "seat_of_storms"),
		GALE("gale", "windward_ground"),
		STONE("stone", "unmoving_mountain"),
		VERDANT("verdant", "wildwood_court"),
		HOLLOW("hollow", "sunken_hall"),
		STARLIT("starlit", "field_of_stars"),
		HOURGLASS("hourglass", "stilled_hour"),
		CRIMSON("crimson", "crimson_court"),
		PLAIN("", "sovereign_ground"),
		// ---- methods-a pack
		TIDE("tide", "drowning_tide"),
		IRON("iron", "anvil_court"),
		DUNE("dune", "shifting_sea");

		public final String method;
		public final String id;

		Flavour(String method, String id) {
			this.method = method;
			this.id = id;
		}

		public String nameKey() {
			return "aura.wildercord.sovereign." + id;
		}

		/** The flavour of {@code methodId}'s awakened Dominion ({@link #PLAIN} for a method without one). */
		public static Flavour of(String methodId) {
			for (Flavour f : values()) {
				if (!f.method.isEmpty() && f.method.equals(methodId)) {
					return f;
				}
			}
			return PLAIN;
		}
	}
}
