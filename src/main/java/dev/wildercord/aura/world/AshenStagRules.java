package dev.wildercord.aura.world;

/**
 * The Ashen Stag, the burnt glade's guardian: its states, its wake-up and the glade it keeps. It lies bedded in the ash before the
 * smouldering stump, facing the gate, until someone comes far enough into the glade; then it gets up, hindquarters first, bellows,
 * and the fight is on. Quicker on its feet than the Bell-Warden and lighter in the hand; a step below a Master, like the others.
 */
public final class AshenStagRules {
	private AshenStagRules() {}

	/** Bedded down in the ash before the stump. */
	public static final int DORMANT = 0;
	/** Getting up, hindquarters first, and bellowing once it is on its feet; scripted, and untouchable while it lasts. */
	public static final int RISE = 1;
	/** On its feet, tracking. */
	public static final int WATCH = 2;
	/** Bedding down again once the glade has gone quiet: the rise, played backwards. */
	public static final int BED = 3;
	/** Head dropped and antlers thrust up through whoever is in front of it, wound back {@link #GORE_TELL} ticks. */
	public static final int GORE = 4;
	/** Both hind hooves lashed out behind it at whoever stands at its back. */
	public static final int KICK = 5;
	/** Reared up on its hind legs and both forehooves brought down on the ground ahead of it. */
	public static final int STOMP = 6;
	/** Head down, antlers forward, and its whole weight run along a fixed line. */
	public static final int CHARGE = 7;
	/** A bound to one side out from under a blow; or, hurt too fast, a leap away back across the glade. */
	public static final int BOUND = 8;
	/** Spent after an attack, head hanging and sides heaving: the opening. */
	public static final int RECOVER = 9;
	/** A charge that ran into a trunk or the bank: its antlers driven into it, and it heaving to pull them free. The big opening. */
	public static final int STUCK = 10;
	/**
	 * Its signature once wounded, the Cinder Rush: {@link #RUSH_RUNS} charges back to back, wheeling and pawing between them, each one
	 * leaving a strip of smouldering embers along its track that burns whoever stands in it. A run into a trunk still sticks it fast.
	 */
	public static final int RUSH = 11;

	public static final double HEALTH = 380, ARMOUR = 8, SPEED = 0.3;

	/** Anyone in the glade this close to its bed, horizontally, wakes it: a few strides in past the gate. */
	public static final double WAKE_RADIUS = 12;
	/** How far above or below its bed a player still counts as on the glade floor. */
	public static final double WAKE_HEIGHT = 5;
	public static final int RISE_TICKS = 40;
	/** When in its rise it bellows: once its forelegs are under it. */
	public static final int BELLOW_AT = RISE_TICKS * 3 / 4;

	/** The glade it keeps, bank and all. It is put back if it strays more than this outside. */
	public static final double LEASH = 2;
	/** The boss bar shows to anyone within this of the glade, and goes past {@link #BAR_DROP}. */
	public static final double BAR_SHOW = 4, BAR_DROP = 10;
	/** Ticks between the kills it makes keeping its glade. */
	public static final int GUARD_EVERY = 10;
	/** With nobody to fight it walks back to its bed after this long, and beds down at {@link #RESET_TICKS}. */
	public static final int RETURN_TICKS = 100, RESET_TICKS = 200;
	/** Whether it was left alone for {@code away} ticks, long enough to have bedded down again unseen. */
	public static boolean forgets(long away){return away>RESET_TICKS;}

	/** How close it closes before it stops to face its prey. */
	public static final double CLOSE = 3.2;

	// ---- the fight: quick tells and quick feet, lighter blows than the Warden's, and a big opening when a charge goes wrong.
	/** What each blow does before defences. */
	public static final double GORE_DAMAGE = 18, KICK_DAMAGE = 20, STOMP_DAMAGE = 16, CHARGE_DAMAGE = 22;
	/** The share of what armour, Protection and Resistance would stop that its blows go through anyway. */
	public static final double PIERCE = 0.35;
	/** The gore: its tell, how long the antlers stay up, and the rest after. */
	public static final int GORE_TELL = 6, GORE_HOLD = 3, GORE_RECOVER = 12;
	/** The kick: its tell, the rest after, and the least time between two. */
	public static final int KICK_TELL = 5, KICK_RECOVER = 14, KICK_REST = 40;
	/** The stomp: its tell (the rear), its rest after, the least time between two; where the hooves land ahead of it, and how far round that they hit. */
	public static final int STOMP_TELL = 12, STOMP_RECOVER = 18, STOMP_REST = 80;
	public static final double STOMP_AHEAD = 2.6, STOMP_RADIUS = 3.0;
	/** The charge: its tell, how long it runs, its rest after, the least time between two, and how far it runs. */
	public static final int CHARGE_TELL = 12, CHARGE_TICKS = 10, CHARGE_RECOVER = 20, CHARGE_REST = 120;
	public static final double CHARGE_REACH = 13;
	/** The Cinder Rush: how many runs, the tell before the first and the wheel before each after, how long each run lasts, the rest
	 *  after, the least time between two; how far each run goes and what it does. */
	public static final int RUSH_RUNS = 3, RUSH_TELL = 16, RUSH_TURN = 9, RUSH_TICKS = 9, RUSH_RECOVER = 34, RUSH_REST = 320;
	public static final double RUSH_REACH = 12, RUSH_DAMAGE = 16;
	/** The ember strips a rush leaves: how long they smoulder, how often they burn whoever stands in them, for how much, and how wide. */
	public static final int EMBER_TICKS = 100, EMBER_EVERY = 10;
	public static final double EMBER_DAMAGE = 3, EMBER_WIDTH = 0.9;
	/** Whether it starts a Cinder Rush at its prey {@code distance} away: only wounded, rested, and with room to run. */
	public static boolean rushes(boolean wounded, long sinceRush, double distance) {
		return wounded && sinceRush >= RUSH_REST && distance >= 4 && distance <= RUSH_REACH + 4;
	}
	/** Whether (px,pz) stands on the ember strip from (ax,az) to (bx,bz). */
	public static boolean onStrip(double px, double pz, double ax, double az, double bx, double bz) {
		double dx = bx - ax, dz = bz - az, len = dx * dx + dz * dz;
		double t = len < 1e-9 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / len));
		double ox = px - (ax + dx * t), oz = pz - (az + dz * t);
		return ox * ox + oz * oz <= EMBER_WIDTH * EMBER_WIDTH;
	}

	/** How long its antlers stay stuck in whatever a charge ran it into. */
	public static final int STUCK_TICKS = 60;
	/** The bound aside: how long it lasts, how fast it goes, and the least time between two. */
	public static final int BOUND_TICKS = 6, BOUND_REST = 40;
	public static final double BOUND_SPEED = 0.65;
	/** The leap away: how much hurt, fading each tick by {@link #STING_FADE}, sends it off, how fast it goes, and the least time between two. */
	public static final double STING_AFTER = 40, STING_FADE = 0.97, LEAP_SPEED = 0.85;
	public static final int LEAP_TICKS = 8, LEAP_REST = 220;
	/** Ticks between its blows, fewer once it is wounded. */
	public static final int GAP = 9, GAP_WOUNDED = 5;

	/** The bar's name for what it is doing. */
	public static String key(int move) {
		return switch (move) {
			case GORE -> "gore";
			case KICK -> "kick";
			case STOMP -> "stomp";
			case CHARGE -> "charge";
			case BOUND -> "bound";
			case RECOVER -> "recover";
			case STUCK -> "stuck";
			case RUSH -> "rush";
			default -> "watch";
		};
	}

	/**
	 * Its choice of blow at {@code distance} from its prey, who stands {@code behind} it or not; -1 to keep closing. {@code roll} is
	 * 0-1. Anyone at its back gets both hind hooves; up close in front it gores, or rears and stomps; a little further it stomps;
	 * anyone keeping well away, it charges.
	 */
	public static int choose(double distance, boolean behind, double roll, boolean mayStomp, boolean mayCharge, boolean mayKick) {
		if (behind && distance <= CLOSE + 1 && mayKick) return KICK;
		if (distance <= CLOSE) return mayStomp && roll < 0.3 ? STOMP : GORE;
		if (distance <= STOMP_AHEAD + STOMP_RADIUS && mayStomp) return STOMP;
		if (distance >= 5 && distance <= CHARGE_REACH + 3 && mayCharge) return CHARGE;
		return -1;
	}
	/** Whether a blow reaches someone {@code forward} ahead, {@code side} across and {@code height} above its feet, in its locked aim. */
	public static boolean hits(int move, double forward, double side, double height) {
		return switch (move) {
			case GORE -> forward > -0.5 && forward <= 4.6 && Math.abs(side) <= 1.9 && height > -1.5 && height < 4.0;
			case KICK -> forward < 0.5 && forward >= -5.0 && Math.abs(side) <= 2.0 && height > -1.5 && height < 3.0;
			case CHARGE, RUSH -> forward > -1.5 && forward <= 3.2 && Math.abs(side) <= 2.0 && height > -1.5 && height < 4.5;
			case STOMP -> {
				double f = forward - STOMP_AHEAD;
				yield f * f + side * side <= STOMP_RADIUS * STOMP_RADIUS && height > -1.5 && height < 2.0;
			}
			default -> false;
		};
	}
	/**
	 * What its bark leaves of a blow. Its antlers and brow turn some of a blow from the front; its flanks take it all; spent after an
	 * attack it is open; with its antlers stuck in a trunk, wide open.
	 */
	public static double armour(int move, boolean frontal) {
		if (move == STUCK) return 1.5;
		if (move == RECOVER) return 1.3;
		return frontal ? 0.65 : 1.0;
	}
	/** Whether a blow arriving from {@code angle} degrees off its facing lands on its front. */
	public static boolean frontal(double angle) {
		return Math.abs(wrap(angle)) <= 50;
	}
	/** Whether someone {@code angle} degrees off its facing stands at its back. */
	public static boolean behind(double angle) {
		return Math.abs(wrap(angle)) >= 125;
	}
	private static double wrap(double angle) {
		return ((angle % 360) + 540) % 360 - 180;
	}
	/** Whether it bounds aside from a blow: only while it watches, not too often, and only some of the time. Skittish, from any side. */
	public static boolean dodges(int move, long sinceDodge, double roll) {
		return move == WATCH && sinceDodge >= BOUND_REST && roll < 0.35;
	}
	/** Whether the hurt it has taken lately, {@code stung}, sends it leaping away, {@code sinceLeap} ticks after it last did. */
	public static boolean leaps(int move, double stung, long sinceLeap) {
		return (move == WATCH || move == RECOVER) && stung >= STING_AFTER && sinceLeap >= LEAP_REST;
	}
	/** Whether it is wounded: at half health or below, it is quicker. */
	public static boolean wounded(float health, float max) {
		return health <= max * 0.5F;
	}

	/** Whether a point lies inside a glade from (x0,y0,z0) to (x1,y1,z1), widened by {@code margin} on every side. */
	public static boolean within(double x, double y, double z, double x0, double y0, double z0, double x1, double y1, double z1, double margin) {
		return x >= x0 - margin && x <= x1 + 1 + margin && y >= y0 - margin && y <= y1 + 1 + margin && z >= z0 - margin && z <= z1 + 1 + margin;
	}

	/** Whether a player at this offset from its bed is close enough to wake it (and inside the glade, which the caller checks). */
	public static boolean wakes(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= WAKE_RADIUS * WAKE_RADIUS && Math.abs(dy) <= WAKE_HEIGHT;
	}

	/** The scripted states: it cannot be hurt, and it shows no bar. */
	public static boolean scripted(int move) {
		return move == DORMANT || move == RISE || move == BED;
	}

	/** How far up it is, 0 bedded to 1 standing, {@code elapsed} ticks into a rise (or bedding down, which runs it backwards). */
	public static float rise(int move, float elapsed) {
		float t = Math.max(0, Math.min(1, elapsed / RISE_TICKS));
		return switch (move) {
			case DORMANT -> 0;
			case RISE -> t;
			case BED -> 1 - t;
			default -> 1;
		};
	}

	/** Health it knits back this tick, {@code sinceCut} ticks after it was last hurt: a Master's regeneration. */
	public static float regen(float maxHealth, long sinceCut) {
		return MastersRules.regen(maxHealth, sinceCut);
	}
}
