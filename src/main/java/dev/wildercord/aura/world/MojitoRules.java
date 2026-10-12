package dev.wildercord.aura.world;

/**
 * Mojito, the misty grove's guardian: a black standard poodle grown as tall as three men, a greatsword held crosswise in his jaws
 * and mint-green magic welling up on both his flanks. He lies curled asleep before the grave in the grove, the sword laid before
 * his paws, until someone comes far enough in; then he gets up, takes the sword in his mouth, shakes out his coat, and the fight
 * is on. Sweeping sword cuts up close, a spin, a great leap onto anyone who backs off, and volleys of magic for anyone further;
 * wounded, he gets the zoomies. Above the Frost Wyrmling and below the Bell-Warden.
 */
public final class MojitoRules {
	private MojitoRules() {}

	/** Curled asleep before the grave, the sword laid in front of his paws. */
	public static final int DORMANT = 0;
	/** Getting up, taking up the sword and shaking out his coat; scripted, and untouchable while it lasts. */
	public static final int RISE = 1;
	/** On his feet, tracking. */
	public static final int WATCH = 2;
	/** Curling up again once the grove has gone quiet: the rise, played backwards. */
	public static final int BED = 3;
	/** The sword swept in a wide arc across the ground in front of him, his whole head swinging it; alternates sides. */
	public static final int SLASH = 4;
	/** A full turn on the spot, the blade sweeping round through everyone near him. */
	public static final int SPIN = 5;
	/** A great leap onto where his prey stood, the sword brought down overhead as he lands. */
	public static final int LEAP = 6;
	/** Magic welling up on both flanks, then loosed at his prey in {@link #BOLT_VOLLEYS} volleys. */
	public static final int BOLTS = 7;
	/** A quick hop back from someone crowding him, or out from under a blow. */
	public static final int HOP = 8;
	/** Spent after an attack, tongue out and panting: the opening. */
	public static final int RECOVER = 9;
	/**
	 * His signature once wounded, the zoomies: a play-bow, then {@link #ZOOM_TICKS} ticks racing round and round the grove, sword out,
	 * running down whoever is in his path and leaving a trail of magic behind him that stings whoever stands in it.
	 */
	public static final int ZOOMIES = 10;
	/** Flopped down flat after the zoomies, tongue lolling: the big opening. */
	public static final int FLOP = 11;

	/** How hard he is among the guardians (see {@link BossTier}). */
	public static final BossTier TIER = BossTier.HEROIC;
	public static final double HEALTH = TIER.health, ARMOUR = TIER.armour, SPEED = TIER.speed(0.3);

	/** Anyone in the grove this close to where he sleeps, horizontally, wakes him. */
	public static final double WAKE_RADIUS = 14;
	/** How far above or below his bed a player still counts as on the grove floor. */
	public static final double WAKE_HEIGHT = 6;
	public static final int RISE_TICKS = 50;
	/** When in his rise he takes up the sword, and when he shakes out his coat. */
	public static final int GRAB_AT = 20, SHAKE_AT = 32;

	/** The grove he keeps. He is put back if he strays more than this outside. */
	public static final double LEASH = 3;
	/** The boss bar shows to anyone within this of the grove, and goes past {@link #BAR_DROP}. */
	public static final double BAR_SHOW = 4, BAR_DROP = 12;
	/** Ticks between the kills he makes keeping his grove. */
	public static final int GUARD_EVERY = 10;
	/** With nobody to fight he walks back to his bed after this long, and curls up at {@link #RESET_TICKS}. */
	public static final int RETURN_TICKS = 100, RESET_TICKS = 200;
	/** Whether he was left alone for {@code away} ticks, long enough to have curled up again unseen. */
	public static boolean forgets(long away) {
		return away > RESET_TICKS;
	}

	/** How close he closes before he stops to face his prey: he is long, and so is the sword. */
	public static final double CLOSE = 5;

	// ---- the fight: a long reach, wide sweeps, and magic for anyone who keeps away.
	/** What each blow does before defences. */
	public static final double SLASH_DAMAGE = TIER.hit(18), SPIN_DAMAGE = TIER.hit(16), LEAP_DAMAGE = TIER.hit(22),
		BOLT_DAMAGE = TIER.hit(7), ZOOM_DAMAGE = TIER.hit(14);
	/** The share of what armour, Protection and Resistance would stop that his blows go through anyway. */
	public static final double PIERCE = 0.35;
	/** The slash: its tell, the rest after, and how far the blade reaches. */
	public static final int SLASH_TELL = 9, SLASH_RECOVER = 12;
	public static final double SLASH_REACH = 7;
	/** The spin: its tell, how long the turn takes, the rest after, the least time between two, and how far the blade reaches. */
	public static final int SPIN_TELL = 12, SPIN_TICKS = 8, SPIN_RECOVER = 18, SPIN_REST = 70;
	public static final double SPIN_REACH = 7;
	/** The leap: its tell (the crouch), how long he is in the air, the rest after, the least time between two, how far he leaps at
	 *  most, and how far round where he lands the sword hits. */
	public static final int LEAP_TELL = 14, LEAP_TICKS = 12, LEAP_RECOVER = 22, LEAP_REST = 140;
	public static final double LEAP_REACH = 20, LEAP_RADIUS = 4.5;
	/** The bolts: the tell while the magic wells up, how many volleys, the ticks between them, the rest after, the least time
	 *  between two; how fast a bolt flies, how long it lasts, and how close it has to pass to hit. */
	public static final int BOLT_TELL = 16, BOLT_VOLLEYS = 3, BOLT_EVERY = 9, BOLT_RECOVER = 16, BOLT_REST = 90, BOLT_LIFE = 50;
	public static final double BOLT_SPEED = 0.9, BOLT_RADIUS = 1.1;
	/** The hop back: how long it lasts, how fast, and the least time between two. */
	public static final int HOP_TICKS = 6, HOP_REST = 40;
	public static final double HOP_SPEED = 0.8;
	/** The zoomies: the play-bow before, how long he runs, how fast, the flop after, the least time between two, the least and most
	 *  radius of his circles, how often a struck player can be struck again; the trail, how long it lingers, how often it stings, for
	 *  how much, and how wide. */
	public static final int ZOOM_TELL = 18, ZOOM_TICKS = 90, ZOOM_FLOP = 70, ZOOM_REST = 420, ZOOM_REHIT = 20;
	public static final double ZOOM_SPEED = 0.95, ZOOM_MIN_RADIUS = 6, ZOOM_MAX_RADIUS = 12;
	public static final int TRAIL_TICKS = 70, TRAIL_EVERY = 10;
	public static final double TRAIL_DAMAGE = TIER.hit(3), TRAIL_RADIUS = 1.3;
	/** Ticks between his blows, fewer once he is wounded. */
	public static final int GAP = 10, GAP_WOUNDED = 6;

	/** The bar's name for what he is doing. */
	public static String key(int move) {
		return switch (move) {
			case SLASH -> "slash";
			case SPIN -> "spin";
			case LEAP -> "leap";
			case BOLTS -> "bolts";
			case HOP -> "hop";
			case RECOVER -> "recover";
			case ZOOMIES -> "zoomies";
			case FLOP -> "flop";
			default -> "watch";
		};
	}

	/**
	 * His choice of blow at {@code distance} from his prey, who stands {@code behind} him or not; -1 to keep closing. {@code roll} is
	 * 0-1. Anyone at his back or crowding him gets the spin, or he hops back off them; in reach of the sword, he slashes; anyone
	 * backing off, he leaps on; anyone keeping well away gets the magic.
	 */
	public static int choose(double distance, boolean behind, double roll, boolean maySpin, boolean mayLeap, boolean mayBolts, boolean mayHop) {
		if (behind && distance <= SPIN_REACH && maySpin) return SPIN;
		if (distance <= 2.5 && mayHop && roll < 0.35) return HOP;
		if (distance <= CLOSE && maySpin && roll < 0.25) return SPIN;
		if (distance <= SLASH_REACH - 0.5 && !behind) return SLASH;
		if (distance >= 9 && distance <= LEAP_REACH && mayLeap && (roll < 0.55 || !mayBolts)) return LEAP;
		if (distance >= 8 && mayBolts) return BOLTS;
		return -1;
	}
	/** Whether he starts the zoomies with his prey {@code distance} away: only wounded, rested, and not right on top of him. */
	public static boolean zooms(boolean wounded, long sinceZoom, double distance) {
		return wounded && sinceZoom >= ZOOM_REST && distance >= 3;
	}
	/** Whether a blow reaches someone {@code forward} ahead, {@code side} across and {@code height} above his feet, in his locked aim. */
	public static boolean hits(int move, double forward, double side, double height) {
		double flat = forward * forward + side * side;
		return switch (move) {
			case SLASH -> forward > -1.5 && flat <= SLASH_REACH * SLASH_REACH && height > -1.5 && height < 4.5;
			case SPIN -> flat <= SPIN_REACH * SPIN_REACH && height > -1.5 && height < 4.0;
			case ZOOMIES -> forward > -2.0 && forward <= 3.5 && Math.abs(side) <= 2.4 && height > -1.5 && height < 5.0;
			default -> false;
		};
	}
	/** Whether a leap landing at an offset of ({@code dx},{@code dy},{@code dz}) from the player hits them. */
	public static boolean lands(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= LEAP_RADIUS * LEAP_RADIUS && dy > -2.5 && dy < 2.5;
	}
	/** Where a leap at someone {@code distance} away comes down: on them, but never further than he can leap. */
	public static double leapLength(double distance) {
		return Math.max(0, Math.min(LEAP_REACH, distance - 1));
	}
	/** Whether a bolt at (bx,by,bz) touches someone standing at (px,py,pz) with height {@code tall}. */
	public static boolean boltHits(double bx, double by, double bz, double px, double py, double pz, double tall) {
		double cy = Math.max(py, Math.min(py + tall, by));
		double dx = bx - px, dy = by - cy, dz = bz - pz;
		return dx * dx + dy * dy + dz * dz <= BOLT_RADIUS * BOLT_RADIUS;
	}
	/** The radius he runs his circles at, given how far his prey stands from the middle of the grove. */
	public static double zoomRadius(double preyFromMiddle) {
		return Math.max(ZOOM_MIN_RADIUS, Math.min(ZOOM_MAX_RADIUS, preyFromMiddle));
	}
	/**
	 * What his coat leaves of a blow. The sword across his jaws turns some of a blow from the front; his flanks take it all; spent
	 * after an attack he is open; flopped down after the zoomies, wide open.
	 */
	public static double armour(int move, boolean frontal) {
		if (move == FLOP) return 1.5;
		if (move == RECOVER) return 1.3;
		if (move == ZOOMIES) return 0.8;
		return frontal ? 0.7 : 1.0;
	}
	/** Whether a blow arriving from {@code angle} degrees off his facing lands on his front. */
	public static boolean frontal(double angle) {
		return Math.abs(wrap(angle)) <= 50;
	}
	/** Whether someone {@code angle} degrees off his facing stands at his back. */
	public static boolean behind(double angle) {
		return Math.abs(wrap(angle)) >= 125;
	}
	private static double wrap(double angle) {
		return ((angle % 360) + 540) % 360 - 180;
	}
	/** Whether he hops back from a blow: only while he watches, not too often, and only some of the time. */
	public static boolean dodges(int move, long sinceHop, double roll) {
		return move == WATCH && sinceHop >= HOP_REST && roll < 0.3;
	}
	/** Whether he is wounded: at half health or below, he is quicker, and may get the zoomies. */
	public static boolean wounded(float health, float max) {
		return health <= max * 0.5F;
	}

	/** Whether a point lies inside a grove from (x0,y0,z0) to (x1,y1,z1), widened by {@code margin} on every side. */
	public static boolean within(double x, double y, double z, double x0, double y0, double z0, double x1, double y1, double z1, double margin) {
		return x >= x0 - margin && x <= x1 + 1 + margin && y >= y0 - margin && y <= y1 + 1 + margin && z >= z0 - margin && z <= z1 + 1 + margin;
	}

	/** Whether a player at this offset from his bed is close enough to wake him (and inside the grove, which the caller checks). */
	public static boolean wakes(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= WAKE_RADIUS * WAKE_RADIUS && Math.abs(dy) <= WAKE_HEIGHT;
	}

	/** The scripted states: he cannot be hurt, and he shows no bar. */
	public static boolean scripted(int move) {
		return move == DORMANT || move == RISE || move == BED;
	}

	/** How far up he is, 0 curled to 1 standing, {@code elapsed} ticks into a rise (or curling up, which runs it backwards). */
	public static float rise(int move, float elapsed) {
		float t = Math.max(0, Math.min(1, elapsed / RISE_TICKS));
		return switch (move) {
			case DORMANT -> 0;
			case RISE -> t;
			case BED -> 1 - t;
			default -> 1;
		};
	}

	/** Health he knits back this tick, {@code sinceCut} ticks after he was last hurt: a Master's regeneration. */
	public static float regen(float maxHealth, long sinceCut) {
		return MastersRules.regen(maxHealth, sinceCut);
	}
}
