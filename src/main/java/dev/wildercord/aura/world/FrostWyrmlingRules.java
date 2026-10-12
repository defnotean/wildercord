package dev.wildercord.aura.world;

/**
 * The Frost Wyrmling, the Glacier Den's guardian: its states, its wake-up and the hall it keeps. A young ice drake, curled nose to
 * tail on the snow mound at the back of the hall over its hoard, until someone comes far enough in; then it uncurls, rises onto its
 * legs, throws its wings wide and roars, and the fight is on. Quick and snapping, with a breath that freezes; a step below a Master,
 * like the others.
 */
public final class FrostWyrmlingRules {
	private FrostWyrmlingRules() {}

	/** Curled up asleep on its mound, nose tucked against its tail and its wings folded over it. */
	public static final int DORMANT = 0;
	/** Uncurling, getting its legs under it, and roaring with its wings thrown wide; scripted, and untouchable while it lasts. */
	public static final int RISE = 1;
	/** On its feet, tracking. */
	public static final int WATCH = 2;
	/** Curling up again once the hall has gone quiet: the rise, played backwards. */
	public static final int BED = 3;
	/** Neck drawn back and the jaws snapped shut on whoever is in front of it, wound back {@link #BITE_TELL} ticks. */
	public static final int BITE = 4;
	/** Its tail swept round in a half circle behind and beside it, at whoever has got round its flank. */
	public static final int TAIL = 5;
	/** Wings raised and brought down in one great beat, throwing back everyone in front of it in a burst of snow. */
	public static final int WING = 6;
	/** Chest swelled and a cone of frost breathed along the ground ahead, swung slowly after its prey: it chills, slows and freezes. */
	public static final int BREATH = 7;
	/** A crouch, then a leap at its prey, landing claws-first where they stood. */
	public static final int POUNCE = 8;
	/** A hop to one side out from under a blow, wings flicking; or, hurt too fast, a beat of its wings away back across the hall. */
	public static final int BOUND = 9;
	/** Spent after an attack, head low and wings drooping, breath steaming: the opening. */
	public static final int RECOVER = 10;

	/**
	 * Its signature, wounded only: reared up on its hind legs with its wings thrown wide through a long tell, then brought down on
	 * its forefeet; frost cracks race out along the floor in lines from where it lands, one at each player and the rest fanned evenly
	 * round it, and ice lances burst up along each line behind the crack, flinging whoever stands on one into the air.
	 */
	public static final int LANCES = 11;

	public static final double HEALTH = 360, ARMOUR = 8, SPEED = 0.32;

	/** Anyone in the hall this close to its mound, horizontally, wakes it: well in past the crevasse mouth. */
	public static final double WAKE_RADIUS = 12;
	/** How far above or below its mound a player still counts as on the hall floor. */
	public static final double WAKE_HEIGHT = 5;
	public static final int RISE_TICKS = 44;
	/** When in its rise it roars: once it is up, as its wings go wide. */
	public static final int ROAR_AT = RISE_TICKS * 3 / 4;

	/** The hall it keeps. It is put back if it strays more than this outside. */
	public static final double LEASH = 2;
	/** The boss bar shows to anyone within this of the hall, and goes past {@link #BAR_DROP}. */
	public static final double BAR_SHOW = 4, BAR_DROP = 10;
	/** Ticks between the kills it makes keeping its hall. */
	public static final int GUARD_EVERY = 10;
	/** With nobody to fight it walks back to its mound after this long, and curls up at {@link #RESET_TICKS}. */
	public static final int RETURN_TICKS = 100, RESET_TICKS = 200;
	/** Whether it was left alone for {@code away} ticks, long enough to have curled up again unseen. */
	public static boolean forgets(long away){return away>RESET_TICKS;}

	/** How close it closes before it stops to face its prey. */
	public static final double CLOSE = 3.4;

	// ---- the fight: quick snapping blows, a breath to keep away from, and a pounce on anyone who does.
	/** What each blow does before defences. */
	public static final double BITE_DAMAGE = 17, TAIL_DAMAGE = 15, WING_DAMAGE = 8, POUNCE_DAMAGE = 20;
	/** The share of what armour, Protection and Resistance would stop that its blows go through anyway. */
	public static final double PIERCE = 0.35;
	/** The bite: its tell, how long its jaws stay shut, and the rest after. */
	public static final int BITE_TELL = 6, BITE_HOLD = 3, BITE_RECOVER = 12;
	/** The tail sweep: its tell, the rest after, and the least time between two. */
	public static final int TAIL_TELL = 7, TAIL_RECOVER = 14, TAIL_REST = 40;
	/** The wing beat: its tell, the rest after, the least time between two, and how hard it throws. */
	public static final int WING_TELL = 9, WING_RECOVER = 14, WING_REST = 90;
	public static final double WING_SHOVE = 2.4;
	/** The frost breath: its tell, how long it breathes, the rest after, the least time between two, how far it reaches; and how
	 *  often it bites, for how much, how long it slows, and how much frost it lays on. */
	public static final int BREATH_TELL = 14, BREATH_TICKS = 30, BREATH_RECOVER = 22, BREATH_REST = 140;
	public static final double BREATH_REACH = 9;
	public static final int BREATH_EVERY = 5, BREATH_SLOW = 60, BREATH_FROST = 45;
	public static final double BREATH_DAMAGE = 3;
	/** How fast, in degrees a tick, the breath swings after its prey. */
	public static final float BREATH_TURN = 3;
	/** The pounce: its tell (the crouch), how long it is in the air, the rest after, the least time between two, how far it leaps at
	 *  most, how high, and how far round where it lands it strikes. */
	public static final int POUNCE_TELL = 10, POUNCE_TICKS = 10, POUNCE_RECOVER = 22, POUNCE_REST = 110;
	public static final double POUNCE_REACH = 12, POUNCE_HEIGHT = 2.6, POUNCE_RADIUS = 2.6;

	/** The hop aside: how long it lasts, how fast it goes, and the least time between two. */
	public static final int BOUND_TICKS = 6, BOUND_REST = 50;
	public static final double BOUND_SPEED = 0.6;
	/** The beat away: how much hurt, fading each tick by {@link #STING_FADE}, sends it off, how fast it goes, and the least time between two. */
	public static final double STING_AFTER = 40, STING_FADE = 0.97, LEAP_SPEED = 0.8;
	public static final int LEAP_TICKS = 9, LEAP_REST = 220;
	/**
	 * Rime Lances: the rear (its tell); how long after the slam the lances begin to burst, behind the crack; how many lines it sends
	 * out in all; how far they run and how fast, in blocks a tick; how close to a lance someone must stand to be struck; what a lance
	 * does, and how high it flings; how long each lance stands; the rest after; and the least time between two.
	 */
	public static final int LANCE_TELL = 18, LANCE_WARN = 8, LANCE_LINES = 8, LANCE_HOLD = 24, LANCE_RECOVER = 28, LANCE_REST = 280;
	public static final double LANCE_REACH = 14, LANCE_SPEED = 1, LANCE_RADIUS = 0.95, LANCE_DAMAGE = 14, LANCE_LIFT = 0.8, LANCE_START = 1.5;
	/** How long the lances take to run out to their full reach once they begin. */
	public static final int LANCE_RUN = (int) Math.ceil((LANCE_REACH - LANCE_START) / LANCE_SPEED);

	/** Whether, wounded and rested, it rears for its lances at prey {@code distance} away. */
	public static boolean lances(boolean wounded, long sinceLances, double distance) {
		return wounded && sinceLances >= LANCE_REST && distance <= LANCE_REACH - 2;
	}
	/** How far out along its line the lances have burst, {@code sinceSlam} ticks after its forefeet came down; 0 before they begin. */
	public static double lanceFront(long sinceSlam) {
		if (sinceSlam < LANCE_WARN) return 0;
		return Math.min(LANCE_REACH, LANCE_START + (sinceSlam - LANCE_WARN) * LANCE_SPEED);
	}
	/**
	 * The yaws of the lines it sends out: one at each of {@code prey} (yaws, the first its target), and the rest fanned evenly round
	 * it from the first, skipping any that would run too close beside a line already aimed.
	 */
	public static float[] lines(float[] prey) {
		var out = new java.util.ArrayList<Float>();
		for (float p : prey) out.add(p);
		float first = prey.length > 0 ? prey[0] : 0;
		for (int i = 0; out.size() < Math.max(LANCE_LINES, prey.length) && i < LANCE_LINES; i++) {
			float y = first + i * 360F / LANCE_LINES;
			boolean near = false;
			for (float o : out) if (Math.abs(wrap(y - o)) < 360F / LANCE_LINES / 2) near = true;
			if (!near) out.add(y);
		}
		float[] arr = new float[out.size()];
		for (int i = 0; i < arr.length; i++) arr[i] = out.get(i);
		return arr;
	}
	/** Whether someone {@code along} out along a lance line and {@code across} beside it, {@code height} above its feet, stands on a lance that has burst out to {@code front}. */
	public static boolean lanced(double along, double across, double height, double front) {
		return along >= LANCE_START - LANCE_RADIUS && along <= front + 0.3 && Math.abs(across) <= LANCE_RADIUS && height > -1.5 && height < 2.5;
	}

	/**
	 * The Rimescale, taken up once the Wyrmling is dead: its bearer never freezes, and whatever hostile strikes them in melee is bitten
	 * by frost in return, slowed, chilled through and hurt a little; then the scale must rime over again, {@link #RIME_REST} ticks.
	 */
	public static final int RIME_REST = 100, RIME_SLOW = 80, RIME_FROST = 160;
	public static final double RIME_DAMAGE = 3;
	/** A living Wyrmling this close keeps anyone from the scale. */
	public static final double SCALE_GUARD = 48;

	/** Whether the scale bites back at a blow {@code sinceRime} ticks after it last did. */
	public static boolean rimes(long sinceRime) {
		return sinceRime >= RIME_REST;
	}

	/** Ticks between its blows, fewer once it is wounded. */
	public static final int GAP = 9, GAP_WOUNDED = 5;

	/** The bar's name for what it is doing. */
	public static String key(int move) {
		return switch (move) {
			case BITE -> "bite";
			case TAIL -> "tail";
			case WING -> "wing";
			case BREATH -> "breath";
			case POUNCE -> "pounce";
			case BOUND -> "bound";
			case RECOVER -> "recover";
			case LANCES -> "lances";
			default -> "watch";
		};
	}

	/**
	 * Its choice of blow at {@code distance} from its prey, who stands {@code angle} degrees off its facing; -1 to keep closing.
	 * {@code roll} is 0-1. Anyone round its flank or at its back gets the tail; up close in front it bites, or beats them back with
	 * its wings; a little way off it breathes frost; anyone keeping well away, it pounces on.
	 */
	public static int choose(double distance, double angle, double roll, boolean mayTail, boolean mayWing, boolean mayBreath, boolean mayPounce) {
		double off = Math.abs(wrap(angle));
		if (off >= 70 && distance <= CLOSE + 1.5 && mayTail) return TAIL;
		if (distance <= CLOSE) return mayWing && roll < 0.25 ? WING : BITE;
		if (distance >= 4 && distance <= BREATH_REACH - 1 && mayBreath && roll < 0.6) return BREATH;
		if (distance >= 5 && distance <= POUNCE_REACH + 1 && mayPounce) return POUNCE;
		if (distance >= 4 && distance <= BREATH_REACH - 1 && mayBreath) return BREATH;
		return -1;
	}
	/** Whether a blow reaches someone {@code forward} ahead, {@code side} across and {@code height} above its feet, in its locked aim. */
	public static boolean hits(int move, double forward, double side, double height) {
		return switch (move) {
			case BITE -> forward > -0.5 && forward <= 4.8 && Math.abs(side) <= 1.8 && height > -1.5 && height < 4.0;
			case TAIL -> forward <= 1.0 && forward * forward + side * side <= 5.2 * 5.2 && height > -1.5 && height < 2.5;
			case WING -> forward > -0.5 && forward <= 6.0 && Math.abs(side) <= 1.5 + Math.max(0, forward) * 0.8 && height > -1.5 && height < 4.0;
			case BREATH -> breathed(forward, side, height);
			default -> false;
		};
	}
	/** Whether someone {@code forward} ahead, {@code side} across and {@code height} above its feet stands in its breath. */
	public static boolean breathed(double forward, double side, double height) {
		return forward > 0.5 && forward <= BREATH_REACH && Math.abs(side) <= 0.6 + forward * 0.45 && height > -1.5 && height < 3.5;
	}
	/** Whether someone {@code dx}, {@code dz} from where it lands, and {@code height} above it, is struck by a pounce. */
	public static boolean crushed(double dx, double dz, double height) {
		return dx * dx + dz * dz <= POUNCE_RADIUS * POUNCE_RADIUS && height > -1.5 && height < 2.5;
	}
	/** How high it is through a pounce, {@code t} 0 to 1 from take-off to landing. */
	public static double arc(double t) {
		t = Math.max(0, Math.min(1, t));
		return 4 * POUNCE_HEIGHT * t * (1 - t);
	}
	/** How far it leaps at prey {@code distance} away: to where they stand, but no further than {@link #POUNCE_REACH}. */
	public static double leap(double distance) {
		return Math.max(0, Math.min(POUNCE_REACH, distance));
	}
	/**
	 * What its scales leave of a blow. Its horned head and the frost on its brow turn some of a blow from the front; its flanks take
	 * it all; spent after an attack it is open, and reared up for its lances its pale belly is bared to the front.
	 */
	public static double armour(int move, boolean frontal) {
		if (move == RECOVER) return 1.3;
		if (move == LANCES) return 1.15;
		return frontal ? 0.6 : 1.0;
	}
	/** Whether a blow arriving from {@code angle} degrees off its facing lands on its front. */
	public static boolean frontal(double angle) {
		return Math.abs(wrap(angle)) <= 50;
	}
	private static double wrap(double angle) {
		return ((angle % 360) + 540) % 360 - 180;
	}
	/** Whether it hops aside from a blow: only while it watches, not too often, and only some of the time. */
	public static boolean dodges(int move, long sinceDodge, double roll) {
		return move == WATCH && sinceDodge >= BOUND_REST && roll < 0.3;
	}
	/** Whether the hurt it has taken lately, {@code stung}, sends it beating away, {@code sinceLeap} ticks after it last did. */
	public static boolean leaps(int move, double stung, long sinceLeap) {
		return (move == WATCH || move == RECOVER) && stung >= STING_AFTER && sinceLeap >= LEAP_REST;
	}
	/** Whether it is wounded: at half health or below, it is quicker. */
	public static boolean wounded(float health, float max) {
		return health <= max * 0.5F;
	}

	/** Whether a point lies inside a hall from (x0,y0,z0) to (x1,y1,z1), widened by {@code margin} on every side. */
	public static boolean within(double x, double y, double z, double x0, double y0, double z0, double x1, double y1, double z1, double margin) {
		return x >= x0 - margin && x <= x1 + 1 + margin && y >= y0 - margin && y <= y1 + 1 + margin && z >= z0 - margin && z <= z1 + 1 + margin;
	}

	/** Whether a player at this offset from its mound is close enough to wake it (and inside the hall, which the caller checks). */
	public static boolean wakes(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= WAKE_RADIUS * WAKE_RADIUS && Math.abs(dy) <= WAKE_HEIGHT;
	}

	/** The scripted states: it cannot be hurt, and it shows no bar. */
	public static boolean scripted(int move) {
		return move == DORMANT || move == RISE || move == BED;
	}

	/** How far up it is, 0 curled to 1 standing, {@code elapsed} ticks into a rise (or curling up, which runs it backwards). */
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
