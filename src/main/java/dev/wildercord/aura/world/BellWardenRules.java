package dev.wildercord.aura.world;

/**
 * The Drowned Bell-Warden, the sunken chapel's guardian: its states, its wake-up and the hall it keeps. It kneels in vigil before
 * the fallen bell-frame with its own cracked bell over its head, until someone walks far enough up the nave; then it rises, tolls
 * and the fight is on. Starting tuning values; a step below a Master, like the serpent.
 */
public final class BellWardenRules {
	private BellWardenRules() {}

	/** Kneeling in vigil before the bell-frame. */
	public static final int DORMANT = 0;
	/** Getting up off its knees, tolling as it stands; scripted, and untouchable while it lasts. */
	public static final int RISE = 1;
	/** On its feet, tracking. */
	public static final int WATCH = 2;
	/** Kneeling again once the chapel has gone quiet: the rise, played backwards. */
	public static final int KNEEL = 3;
	/** A sweep of the clapper across its front, wound back {@link #SWING_TELL} ticks. */
	public static final int SWING = 4;
	/** Chained off a swing: the clapper brought down overhand on whoever is right in front of it. */
	public static final int HAMMER = 5;
	/** Both fists raised and brought down on the floor ahead of it; everyone near where they land is hit. */
	public static final int SLAM = 6;
	/** Its bell dropped like a ram and its whole weight run along a fixed line. */
	public static final int CHARGE = 7;
	/** It heaves itself back out from under a blow. */
	public static final int BACKSTEP = 8;
	/** Spent after an attack, its bell tipped back and its chest bare: the opening. */
	public static final int RECOVER = 9;
	/** Hurt too fast, it hunches down under its bell, which takes nearly everything. */
	public static final int SHELL = 10;
	/** It strikes its own bell, and a ring of force rolls out along the floor from its feet: jump it, or be swept. */
	public static final int TOLL = 11;

	/** How hard it is among the guardians (see {@link BossTier}). */
	public static final BossTier TIER = BossTier.DREAD;
	public static final double HEALTH = TIER.health, ARMOUR = TIER.armour, SPEED = TIER.speed(0.24);

	/** Anyone in the hall this close to its vigil, horizontally, wakes it: about halfway up the nave. */
	public static final double WAKE_RADIUS = 18;
	/** How far above or below its knees a player still counts as on the chapel floor. */
	public static final double WAKE_HEIGHT = 5;
	public static final int RISE_TICKS = 46;
	/** When in its rise the bell tolls: as it straightens. */
	public static final int TOLL_AT = RISE_TICKS / 2;

	/** The hall it keeps is the chapel, nave and apse. It is put back if it strays more than this outside. */
	public static final double LEASH = 2;
	/** The boss bar shows to anyone within this of the hall, and goes past {@link #BAR_DROP}. */
	public static final double BAR_SHOW = 4, BAR_DROP = 10;
	/** Ticks between the kills it makes keeping its hall. */
	public static final int GUARD_EVERY = 10;
	/** With nobody to fight it walks back to its vigil after this long, and kneels at {@link #RESET_TICKS}. */
	public static final int RETURN_TICKS = 100, RESET_TICKS = 200;
	/** Whether it was left alone for {@code away} ticks, long enough to have knelt again unseen. */
	public static boolean forgets(long away){return away>RESET_TICKS;}

	/** How close it closes before it stops to face its prey. */
	public static final double CLOSE = 3.5;

	// ---- the fight: tells a touch slower than the serpent's, since it hits harder and wider, and a real opening after each blow.
	/** What each blow does before defences. */
	public static final double SWING_DAMAGE = TIER.hit(20), HAMMER_DAMAGE = TIER.hit(24), SLAM_DAMAGE = TIER.hit(18), CHARGE_DAMAGE = TIER.hit(24);
	/** The share of what armour, Protection and Resistance would stop that its blows go through anyway: dead weight, not edges. */
	public static final double PIERCE = 0.4;
	/** A swing's tell, the hammer's tell after it, how long the blow stays out, and the rest after each. */
	public static final int SWING_TELL = 5, HAMMER_TELL = 4, SWING_HOLD = 3, SWING_RECOVER = 12, HAMMER_RECOVER = 18;
	/** The slam: its tell, its rest after, the least time between two; where the fists land ahead of its feet, and how far round that they hit. */
	public static final int SLAM_TELL = 9, SLAM_RECOVER = 20, SLAM_REST = 90;
	public static final double SLAM_AHEAD = 2.6, SLAM_RADIUS = 3.2;
	/** The charge: its tell, how long it runs, its rest after, the least time between two, and how far it runs. */
	public static final int CHARGE_TELL = 10, CHARGE_TICKS = 8, CHARGE_RECOVER = 24, CHARGE_REST = 140;
	public static final double CHARGE_REACH = 9;
	/** The backstep: how long it lasts, how fast it goes, and the least time between two. */
	public static final int BACKSTEP_TICKS = 5, BACKSTEP_REST = 40;
	public static final double BACKSTEP_SPEED = 0.6;
	/** The shell: how long it stays under, the least time between two, and how much hurt, fading each tick by {@link #STING_FADE}, sends it under. */
	public static final int SHELL_TICKS = 40, SHELL_REST = 220;
	public static final double SHELL_AFTER = 45, STING_FADE = 0.97;
	/**
	 * The toll: what the ring does, its tell, the tell of each toll after the first, its rest after the last, and the least time
	 * between two tollings it chooses (coming up out of its shell it always tolls).
	 */
	public static final double TOLL_DAMAGE = TIER.hit(16);
	public static final int TOLL_TELL = 12, TOLL_AGAIN = 10, TOLL_RECOVER = 26, TOLL_REST = 200;
	/** How far off it chooses to toll from. */
	public static final double TOLL_FROM = 10;
	/** The ring: how fast it rolls out, how far it goes, how thick it is, and how high off the floor it reaches. */
	public static final double WAVE_SPEED = 0.55, WAVE_REACH = 13, WAVE_BAND = 0.8, WAVE_HEIGHT = 0.9;
	/** Ticks between its blows, fewer once it is wounded. */
	public static final int GAP = 10, GAP_WOUNDED = 6;

	/** The bar's name for what it is doing. */
	public static String key(int move) {
		return switch (move) {
			case SWING -> "swing";
			case HAMMER -> "hammer";
			case SLAM -> "slam";
			case CHARGE -> "charge";
			case BACKSTEP -> "backstep";
			case RECOVER -> "recover";
			case SHELL -> "shell";
			case TOLL -> "toll";
			default -> "watch";
		};
	}

	/**
	 * Its choice of blow at {@code distance} from its prey; -1 to keep closing. {@code roll} is 0-1. Up close it swings, or pounds the
	 * floor; a little further it pounds the floor; anyone keeping well away, it charges. Now and then, near or far, it tolls.
	 */
	public static int choose(double distance, double roll, boolean maySlam, boolean mayCharge, boolean mayToll) {
		if (mayToll && roll >= 0.75 && distance <= TOLL_FROM) return TOLL;
		if (distance <= CLOSE) return maySlam && roll < 0.3 ? SLAM : SWING;
		if (distance <= SLAM_AHEAD + SLAM_RADIUS && maySlam) return SLAM;
		if (distance >= 5 && distance <= CHARGE_REACH + 3 && mayCharge) return CHARGE;
		return -1;
	}
	/** Whether a blow reaches someone {@code forward} ahead, {@code side} across and {@code height} above its feet, in its locked aim. */
	public static boolean hits(int move, double forward, double side, double height) {
		return switch (move) {
			case SWING -> forward > -0.5 && forward <= 4.8 && Math.abs(side) <= 3.0 && height > -1.5 && height < 4.0;
			case HAMMER -> forward > 0 && forward <= 4.6 && Math.abs(side) <= 1.6 && height > -1.5 && height < 4.0;
			case CHARGE -> forward > -1.5 && forward <= 2.8 && Math.abs(side) <= 2.0 && height > -1.5 && height < 4.5;
			case SLAM -> {
				double f = forward - SLAM_AHEAD;
				yield f * f + side * side <= SLAM_RADIUS * SLAM_RADIUS && height > -1.5 && height < 2.0;
			}
			default -> false;
		};
	}
	/** Whether it follows a swing with the hammer: half the time, always once it is wounded. */
	public static boolean hammers(double roll, boolean wounded) {
		return wounded || roll < 0.5;
	}
	/**
	 * What its bell and hide leave of a blow. Facing you, the bell turns some of it; behind, little; spent after an attack its chest is
	 * bare; under its bell, almost nothing gets through.
	 */
	public static double armour(int move, boolean frontal) {
		if (move == SHELL) return 0.15;
		if (move == RECOVER) return 1.35;
		return frontal ? 0.6 : 0.9;
	}
	/** Whether a blow arriving from {@code angle} degrees off its facing lands on its front. */
	public static boolean frontal(double angle) {
		double a = Math.abs(((angle % 360) + 540) % 360 - 180);
		return a <= 60;
	}
	/** Whether it steps back out of a blow: only a frontal one, only while it watches, not too often, and only some of the time. */
	public static boolean dodges(int move, boolean frontal, long sinceDodge, double roll) {
		return move == WATCH && frontal && sinceDodge >= BACKSTEP_REST && roll < 0.4;
	}
	/** Whether the hurt it has taken lately, {@code stung}, sends it under its bell, {@code sinceShell} ticks after it last went under. */
	public static boolean shells(int move, double stung, long sinceShell) {
		return (move == WATCH || move == RECOVER) && stung >= SHELL_AFTER && sinceShell >= SHELL_REST;
	}
	/** How many times it tolls in a row: once, twice once it is wounded, three times near the end. */
	public static int tolls(float health, float max) {
		return health <= max * 0.25F ? 3 : health <= max * 0.5F ? 2 : 1;
	}
	/** How far out a ring has rolled {@code age} ticks after the toll. */
	public static double waveRadius(long age) {
		return age * WAVE_SPEED;
	}
	/** Whether a ring has rolled out past its reach. */
	public static boolean waveDone(long age) {
		return waveRadius(age) > WAVE_REACH;
	}
	/** Whether a ring of {@code radius} catches someone {@code distance} across the floor from where it was struck and {@code height} above it. */
	public static boolean waveHits(double distance, double radius, double height) {
		return Math.abs(distance - radius) <= WAVE_BAND && height > -1.5 && height < WAVE_HEIGHT;
	}
	/**
	 * The Bell Shard, touched once the Warden is dead: a blow that leaves you at {@link #KNELL_BELOW} of your health or under makes it
	 * toll, once every {@link #KNELL_RECHARGE} ticks: everything hostile within {@link #KNELL_RADIUS} thrown back {@link #KNELL_SHOVE},
	 * and Absorption for {@link #KNELL_SHIELD} ticks and Resistance for {@link #KNELL_GUARD_TICKS}.
	 */
	public static final float KNELL_BELOW = 0.3F;
	public static final int KNELL_RECHARGE = 3600, KNELL_SHIELD = 160, KNELL_GUARD_TICKS = 60;
	public static final double KNELL_RADIUS = 6, KNELL_SHOVE = 1.6;
	/** A living Warden this close keeps anyone from the shard. */
	public static final double SHARD_GUARD = 48;

	/** Whether the shard tolls for someone left at {@code health} of {@code max}, {@code since} ticks after it last did. */
	public static boolean knells(float health, float max, long since) {
		return health > 0 && health <= max * KNELL_BELOW && since >= KNELL_RECHARGE;
	}
	/** Whether it is wounded: at half health or below, it is quicker and hammers after every swing. */
	public static boolean wounded(float health, float max) {
		return health <= max * 0.5F;
	}

	/** Whether a point lies inside a hall from (x0,y0,z0) to (x1,y1,z1), widened by {@code margin} on every side. */
	public static boolean within(double x, double y, double z, double x0, double y0, double z0, double x1, double y1, double z1, double margin) {
		return x >= x0 - margin && x <= x1 + 1 + margin && y >= y0 - margin && y <= y1 + 1 + margin && z >= z0 - margin && z <= z1 + 1 + margin;
	}

	/** Whether a player at this offset from its vigil is close enough to wake it (and inside the hall, which the caller checks). */
	public static boolean wakes(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= WAKE_RADIUS * WAKE_RADIUS && Math.abs(dy) <= WAKE_HEIGHT;
	}

	/** The scripted states: it cannot be hurt, and it shows no bar. */
	public static boolean scripted(int move) {
		return move == DORMANT || move == RISE || move == KNEEL;
	}

	/** How far up it is, 0 kneeling to 1 standing, {@code elapsed} ticks into a rise (or a kneel, which runs it backwards). */
	public static float rise(int move, float elapsed) {
		float t = Math.max(0, Math.min(1, elapsed / RISE_TICKS));
		return switch (move) {
			case DORMANT -> 0;
			case RISE -> t;
			case KNEEL -> 1 - t;
			default -> 1;
		};
	}

	/** Health it knits back this tick, {@code sinceCut} ticks after it was last hurt: a Master's regeneration. */
	public static float regen(float maxHealth, long sinceCut) {
		return MastersRules.regen(maxHealth, sinceCut);
	}
}
