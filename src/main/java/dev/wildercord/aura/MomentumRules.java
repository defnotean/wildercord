package dev.wildercord.aura;

import java.util.Map;

/**
 * Momentum, the pure part: a swordsman's meter from 0 to {@link #MAX} that a clean fight fills. Clean hits (full swings that land
 * on a real foe), arts that land, perfect guards and Aura Steps taken through an attack build it; a hit taken knocks a share off;
 * out of a fight it ebbs away. It climbs through tiers ({@link #tier}) that make every art cheaper ({@link #priceFactor}) and
 * stronger ({@link #strength}) and wear foes' stance faster ({@link #stanceFactor}), and at its peak ({@link #PEAK}) the Final Art
 * opens. Playing the Final Art spends {@link #FINAL_SPEND} of it.
 *
 * <p>Each breathing method has its own temper ({@link Temper}): Ember burns hot and burns out, Rime and Verdant hold theirs
 * longest, Stone barely loses any to a blow, Crimson feeds on being hurt, Gale's steps and Thunder's crowds feed it, and every
 * method builds fastest on blows against foes in the state its own arts leave them in (burning, frozen, ionised, thrown up,
 * cracked, rooted, silenced, starred, stopped in time, bleeding: {@link #FAVOURED}).</p>
 *
 * <p>What keeps it honest: weak swings and sweeps give nothing; a foe that can't fight back gives nothing (no mind of its own,
 * riding a boat or a cart); each foe gives only so much from blows ({@link #budget}) until its stance is broken; training dummies
 * and the practice arena teach at {@link #PRACTICE_SHARE} and only up to {@link #PRACTICE_CEILING} (the arena alone lets it reach
 * the peak, and leaving it empties it); an art landing far away ({@link #RANGED_FROM}) counts half (Gale's whole); and it ebbs
 * after a few seconds out of a fight, so it can't be carried in from anywhere.</p>
 *
 * <p>Shared by the server ({@code aura.Momentum}), the HUD and the unit tests. The ebb is worked out on both sides from the same
 * numbers ({@link #current}), so the server only sends a change, never a tick.</p>
 */
public final class MomentumRules {
	private MomentumRules() {}

	// ------------------------------------------------------------------ the meter

	public static final double MAX = 100.0;
	/** The tiers' thresholds: 25, 50 and 75; and the peak, which opens the Final Art. */
	public static final double[] TIERS = {25.0, 50.0, 75.0};
	public static final double PEAK = 95.0;
	/** The highest tier: the peak. */
	public static final int PEAK_TIER = 4;

	/** The tier of {@code value}: 0 below the first threshold, 1 to 3 at each, {@link #PEAK_TIER} at the peak. */
	public static int tier(double value) {
		if (value >= PEAK - 1.0E-9) {
			return PEAK_TIER;
		}
		int tier = 0;
		for (double t : TIERS) {
			if (value >= t - 1.0E-9) {
				tier++;
			}
		}
		return tier;
	}

	/** Whether {@code value} is at the peak. */
	public static boolean peak(double value) {
		return tier(value) >= PEAK_TIER;
	}

	/** {@code value} held to the meter. */
	public static double clamp(double value) {
		return Math.max(0, Math.min(MAX, value));
	}

	// ------------------------------------------------------------------ what each tier gives

	/** What an art costs at each tier, as a share of its price: 10%, 15%, 20%, then a quarter off at the peak. */
	private static final double[] PRICE = {1.0, 0.9, 0.85, 0.8, 0.75};
	/** How hard an art strikes at each tier: 5% harder a tier, a fifth at the peak (another player's cap still holds). */
	private static final double[] STRENGTH = {1.0, 1.05, 1.1, 1.15, 1.2};
	/** How much faster a swordsman's blows and arts wear a foe's stance at each tier. */
	private static final double[] STANCE = {1.0, 1.1, 1.2, 1.3, 1.45};

	public static double priceFactor(int tier) {
		return PRICE[clampTier(tier)];
	}

	public static double strength(int tier) {
		return STRENGTH[clampTier(tier)];
	}

	public static double stanceFactor(int tier) {
		return STANCE[clampTier(tier)];
	}

	/** An art's price at {@code tier}. */
	public static double price(double cost, int tier) {
		return Math.max(0, cost) * priceFactor(tier);
	}

	private static int clampTier(int tier) {
		return Math.max(0, Math.min(PEAK_TIER, tier));
	}

	/** What playing the Final Art spends (its release: the meter falls back from the peak, never below nothing). */
	public static final double FINAL_SPEND = 40.0;

	// ------------------------------------------------------------------ what builds it

	/** A clean hit: a full swing (with a blade, by a swordsman) that hurt a real foe; a critical one more. */
	public static final double CLEAN_HIT = 3.5;
	public static final double CRITICAL_HIT = 5.0;
	/** An art landing (its first foe hurt), by slot: the First Art to the Final Art (whose release builds nothing: it spends). */
	public static final double[] ART = {6.0, 8.0, 10.0, 10.0, 0.0};
	/** Each foe after the first an art hurts, up to {@link #ART_EXTRA_FOES} of them (Thunder's temper counts more). */
	public static final double ART_EXTRA = 1.5;
	public static final int ART_EXTRA_FOES = 3;
	/** A perfect guard against a blow, and against a projectile or a spell (no foe within the blade's reach to answer). */
	public static final double PERFECT_GUARD = 12.0;
	public static final double PERFECT_DEFLECT = 6.0;
	/** An Aura Step taken through an attack (its untouchable moment turned a real blow or shot aside), once a step. */
	public static final double STEP_THROUGH = 10.0;
	/** Breaking a foe's stance, and the finisher after it. */
	public static final double BREAK = 8.0;
	public static final double FINISHER = 14.0;
	/** A worthy foe felled. */
	public static final double KILL = 3.0;

	/** What a clean hit builds before the temper: a critical one more. */
	public static double cleanHit(boolean critical) {
		return critical ? CRITICAL_HIT : CLEAN_HIT;
	}

	/** What an art in {@code slot} (0 to 4) landing builds before the temper. */
	public static double art(int slot) {
		return ART[Math.max(0, Math.min(ART.length - 1, slot))];
	}

	/** What the {@code n}th foe an art hurts (1 for the first) adds, before the temper: the art itself for the first, then a little each. */
	public static double artFoe(int slot, int n, Temper temper) {
		if (n <= 0 || art(slot) <= 0) {
			return 0;
		}
		if (n == 1) {
			return art(slot) * temper.art();
		}
		return n - 1 <= ART_EXTRA_FOES ? ART_EXTRA * temper.chain() : 0;
	}

	/** An art landing this far (blocks) from its swordsman, or further, counts {@link #RANGED_SHARE} (Gale's temper counts it whole). */
	public static final double RANGED_FROM = 6.0;
	public static final double RANGED_SHARE = 0.5;

	/** What reach leaves of an art's momentum: whole up close (or for a temper that reaches), {@link #RANGED_SHARE} from afar. */
	public static double reach(double distance, Temper temper) {
		return distance >= RANGED_FROM && !temper.reaches() ? RANGED_SHARE : 1.0;
	}

	// ------------------------------------------------------------------ what knocks it down

	/** A hit taken knocks off at least this share of what's there, more for a heavier blow, at most {@link #LOSS_MOST}, and a little more. */
	public static final double LOSS_LEAST = 0.12;
	public static final double LOSS_MOST = 0.4;
	public static final double LOSS_FLAT = 2.0;
	/** A blow caught on a held guard knocks off this share of what it otherwise would. */
	public static final double GUARDED_LOSS = 0.5;

	/**
	 * What a hit taken knocks off {@code value}: a share by how heavy it was ({@code damage} of {@code maxHealth}), less through a held
	 * guard, by the temper. Never more than there is.
	 */
	public static double loss(double value, double damage, double maxHealth, boolean guarded, Temper temper) {
		if (value <= 0 || damage <= 0) {
			return 0;
		}
		double share = Math.max(LOSS_LEAST, Math.min(LOSS_MOST, LOSS_LEAST + 0.6 * damage / Math.max(1, maxHealth)));
		double lost = (value * share + LOSS_FLAT) * temper.loss() * (guarded ? GUARDED_LOSS : 1.0);
		if (guarded && temper.steadfast()) {
			lost = 0;
		}
		return Math.max(0, Math.min(value, lost));
	}

	/** Crimson's temper: a hit taken while at or under half health builds this much (after what it knocks off). */
	public static final double BLOODIED = 3.0;
	public static final double BLOODIED_AT = 0.5;

	// ------------------------------------------------------------------ the ebb

	/** How long after the last blow given or taken (ticks) before it starts to ebb, and how fast it ebbs then (a second). */
	public static final int GRACE = 80;
	public static final double EBB = 8.0;

	/** What it ebbs a tick, for {@code temper}, times the server's {@code momentum_ebb}. */
	public static double ebbPerTick(Temper temper, double ebbScale) {
		return EBB / 20.0 * temper.ebb() * Math.max(0, ebbScale);
	}

	/**
	 * What {@code value} is at {@code now}: held until {@code heldUntil}, then ebbing {@code ebbPerTick}; and while a hold lasts
	 * ({@code floorUntil}, an awakening's) never below {@code floor} and not ebbing at all. The server writes the four when one
	 * changes; both sides read it this way.
	 */
	public static double current(double value, long heldUntil, double ebbPerTick, double floor, long floorUntil, long now) {
		long held = Math.max(heldUntil, floorUntil);
		double v = value;
		if (now > held) {
			v -= Math.max(0, ebbPerTick) * (now - held);
		}
		if (now <= floorUntil) {
			v = Math.max(v, floor);
		}
		return clamp(v);
	}

	// ------------------------------------------------------------------ keeping it honest

	/** On training dummies and in the practice arena it builds this share, and only to this (a tier short of the peak). */
	public static final double PRACTICE_SHARE = 0.6;
	public static final double PRACTICE_CEILING = 74.0;

	/** Where it may rise to from what was struck: the practice ceiling for a dummy outside the arena, the meter's top anywhere else. */
	public static double ceiling(boolean practiceTarget, boolean inArena) {
		return practiceTarget && !inArena ? PRACTICE_CEILING : MAX;
	}

	/**
	 * What blows on one foe may build in all, until its stance is broken: a little for a weak one, more for a strong one, most for a
	 * boss (a long fight), and a player or a dummy as an ordinary strong foe. Perfect guards, steps and arts aren't counted against it.
	 */
	public static double budget(double maxHealth, boolean boss, boolean player, boolean dummy) {
		if (boss) {
			return BOSS_BUDGET;
		}
		if (player || dummy) {
			return PLAYER_BUDGET;
		}
		return Math.max(12.0, Math.min(40.0, 10.0 + 0.5 * Math.max(0, maxHealth)));
	}

	public static final double BOSS_BUDGET = 60.0;
	public static final double PLAYER_BUDGET = 30.0;
	/** A foe's budget is forgotten this long (ticks) after the last blow on it (a player's sooner). */
	public static final int BUDGET_MEMORY = 400;
	public static final int PLAYER_BUDGET_MEMORY = 200;

	/** What is left of a blow's momentum on a foe that has given {@code given} of its {@code budget} already. */
	public static double fromBudget(double amount, double given, double budget) {
		return Math.max(0, Math.min(amount, budget - Math.max(0, given)));
	}

	// ------------------------------------------------------------------ the methods' tempers

	/** The states a foe can be in that a method's blows feed on (bits, combined). */
	public static final int BURNING = 1;
	public static final int CHILLED = 1 << 1;
	public static final int IONISED = 1 << 2;
	public static final int AIRBORNE = 1 << 3;
	public static final int CRACKED = 1 << 4;
	public static final int ROOTED = 1 << 5;
	public static final int SILENCED = 1 << 6;
	public static final int SHADOWED = 1 << 7;
	public static final int STARRED = 1 << 8;
	public static final int STOPPED = 1 << 9;
	public static final int BLEEDING = 1 << 10;

	/** What a blow on a foe in a state the temper favours builds: this much more. */
	public static final double FAVOURED = 1.4;

	/**
	 * How a method's momentum moves: how much each kind of play builds ({@code hit}, {@code art}, {@code guard}, {@code step}), what
	 * each foe after an art's first adds ({@code chain}), how much a hit taken knocks off ({@code loss}), how long it holds out of a
	 * fight ({@code grace} ticks) and how fast it ebbs after ({@code ebb}), the foe states its blows feed on ({@code favours}, bits),
	 * whether an art from afar counts whole ({@code reaches}), whether a guarded hit costs nothing ({@code steadfast}) and whether
	 * being hurt while bloodied builds it ({@code bloodied}).
	 */
	public record Temper(double hit, double art, double guard, double step, double chain, double loss, int grace, double ebb, int favours,
			boolean reaches, boolean steadfast, boolean bloodied) {
		/** A method without a temper of its own: everything as the rules give it. */
		public static final Temper PLAIN = new Temper(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, GRACE, 1.0, 0, false, false, false);

		/** Whether its blows feed on a foe in {@code states}. */
		public boolean favoured(int states) {
			return (favours & states) != 0;
		}

		/** What a clean hit on a foe in {@code states} builds. */
		public double hitOn(boolean critical, int states) {
			return cleanHit(critical) * hit * (favoured(states) ? FAVOURED : 1.0);
		}
	}

	/**
	 * The built-in methods' tempers. Ember burns hot and out (more from blows, most on burning foes, ebbs fastest); Rime holds still
	 * (feeds on frozen foes and perfect guards, ebbs slowest with Verdant); Thunder feeds on crowds (each foe an art reaches adds
	 * double); Gale on movement (steps through attacks, arts from afar whole, foes in the air); Stone barely gives ground (a hit
	 * takes half as much, a guarded one nothing, perfect guards build most); Verdant is patient (holds longest after Hourglass, ebbs
	 * slowest, feeds on rooted foes); Hollow feeds on silenced and shadowed foes and its arts; Starlit on starred foes and its arts;
	 * Hourglass holds the moment (longest before it ebbs, feeds on foes stopped in time); Crimson feeds on blood (bleeding foes, and
	 * its own: a hit taken while bloodied builds it, and knocks off least after Stone).
	 */
	public static final Map<String, Temper> TEMPERS = Map.of(
		"ember", new Temper(1.15, 1.0, 0.9, 1.0, 1.0, 1.1, 60, 1.4, BURNING, false, false, false),
		"rime", new Temper(0.9, 1.1, 1.25, 0.9, 1.0, 0.9, 100, 0.6, CHILLED, false, false, false),
		"thunder", new Temper(1.0, 1.0, 1.0, 1.1, 2.0, 1.0, 60, 1.25, IONISED, false, false, false),
		"gale", new Temper(1.0, 1.0, 0.9, 1.5, 1.0, 1.0, 80, 1.0, AIRBORNE, true, false, false),
		"stone", new Temper(0.9, 1.0, 1.4, 0.8, 1.0, 0.5, 100, 0.8, CRACKED, false, true, false),
		"verdant", new Temper(0.9, 1.15, 1.1, 1.0, 1.0, 0.8, 120, 0.6, ROOTED, false, false, false),
		"hollow", new Temper(1.0, 1.15, 1.0, 1.0, 1.0, 1.0, 80, 1.0, SILENCED | SHADOWED, false, false, false),
		"starlit", new Temper(1.0, 1.2, 1.0, 1.0, 1.0, 1.0, 80, 1.0, STARRED, false, false, false),
		"hourglass", new Temper(1.0, 1.0, 1.0, 1.1, 1.0, 0.9, 160, 0.7, STOPPED, false, false, false),
		"crimson", new Temper(1.1, 1.0, 0.9, 1.0, 1.0, 0.6, 60, 1.3, BLEEDING, false, false, true));

	/** A method's temper ({@link Temper#PLAIN} for one without). */
	public static Temper temper(String methodId) {
		return methodId == null ? Temper.PLAIN : TEMPERS.getOrDefault(methodId, Temper.PLAIN);
	}
}
