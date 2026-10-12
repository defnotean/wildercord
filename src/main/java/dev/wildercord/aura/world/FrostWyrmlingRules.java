package dev.wildercord.aura.world;

import dev.wildercord.cast.MoveDeck;

/**
 * The Frost Wyrmling, the Glacier Den's guardian: its states, its wake-up and the hall it keeps. A young ice drake, curled nose to
 * tail on the snow mound at the back of the hall over its hoard, until someone comes far enough in; then it uncurls, rises onto its
 * legs, throws its wings wide and roars, and the fight is on. Quick and playful and cruel the way young things are: it snaps, lashes,
 * beats up gales, sweeps its breath across the hall, bounds about, dives from the dome, toboggans down ice it breathes for itself and
 * brings the icicles down off the ceiling. Half beaten it shuts itself in a shell of ice and bursts out of it fiercer. A step below a
 * Master, like the others.
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
	/** Neck coiled back low in an S with the jaws steaming open, then a lunge and a snap; fierce, it snaps twice. */
	public static final int BITE = 4;
	/** Crouched and looking back over its shoulder, tail raised, then whipped round low through everything but its front. */
	public static final int TAIL = 5;
	/** Wings mantled forward and cupped, then beaten down again and again, each beat a gale of snow that shoves everyone before it. */
	public static final int GALE = 6;
	/** Head drawn back and chest aglow while ice pillars burst up near its prey; then a beam of frost swept across the hall. */
	public static final int BREATH = 7;
	/** A stalking crouch, rump wiggling, then a string of bounding hops, each landing claws-first where its prey stood. */
	public static final int POUNCE = 8;
	/** A hop to one side out from under a blow, wings flicking; or, hurt too fast, a beat of its wings away back across the hall. */
	public static final int BOUND = 9;
	/** Spent after an attack, head low and wings drooping, breath steaming: the opening. */
	public static final int RECOVER = 10;
	/**
	 * Its signature, fierce only: reared up on its hind legs with its wings thrown wide through a long tell, then brought down on
	 * its forefeet; frost cracks race out along the floor in lines from where it lands, one at each player and the rest fanned evenly
	 * round it, and ice lances burst up along each line behind the crack, flinging whoever stands on one into the air.
	 */
	public static final int LANCES = 11;
	/** Up off the floor on its wings to hang under the dome, a frost line drawn under it at its prey; then a dive down the line and a skid. */
	public static final int DIVE = 12;
	/** A glazed strip of ice breathed out along the floor at its prey, then its back turned to it, down on its belly and tobogganing tail first down it. */
	public static final int SLIDE = 13;
	/** Sprawled after a dive or a slide: belly-flat (side 0) or rolled onto its side with its legs paddling (side -1 or 1). The big opening. */
	public static final int SPRAWL = 15;
	/** Head reared to the dome with a roar that shakes icicles loose, each falling on a frost mark by its prey. */
	public static final int ICEFALL = 14;
	/** Half beaten, once: wrapped in its wings inside a shell of ice that closes over it, then bursting out of it in a ring of frost. */
	public static final int SHELL = 16;
	/** Shaken after bursting out of its shell, swaying, wide open. */
	public static final int DAZED = 17;

	/** How hard it is among the guardians (see {@link BossTier}). */
	public static final BossTier TIER = BossTier.ELDER;
	public static final double HEALTH = TIER.health, ARMOUR = TIER.armour, SPEED = TIER.speed(0.32);

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
	/** The share of what armour, Protection and Resistance would stop that its blows go through anyway. */
	public static final double PIERCE = 0.35;

	/** The snap: its tell, the fierce second snap's tell, how long its jaws stay shut, the rest after, how far it lunges and reaches, and what it does. */
	public static final int BITE_TELL = 10, BITE_AGAIN = 6, BITE_HOLD = 3, BITE_RECOVER = 14;
	public static final double BITE_LUNGE = 1.5, BITE_REACH = 5.5, BITE_DAMAGE = TIER.hit(16);
	/** The lash: its tell (shorter fierce), how long the sweep takes, the rest after, how far and how low it reaches, how far round, and what it does. */
	public static final int TAIL_TELL = 12, TAIL_TELL_FIERCE = 9, TAIL_TICKS = 5, TAIL_RECOVER = 18;
	public static final double TAIL_REACH = 5.5, TAIL_HEIGHT = 0.75, TAIL_ARC = 270, TAIL_DAMAGE = TIER.hit(15);
	/** The gale: its tell, how many beats (more fierce) and how far apart, the rest after, how far a beat carries, what it does and how hard it shoves; and the share of the shove someone crouching takes. */
	public static final int GALE_TELL = 14, GALE_BEATS = 3, GALE_BEATS_FIERCE = 4, GALE_EVERY = 6, GALE_RECOVER = 16;
	public static final double GALE_REACH = 8, GALE_DAMAGE = TIER.hit(5), GALE_SHOVE = 1.6, GALE_CROUCH = 0.25;
	/**
	 * The breath: its tell, how long one pass across takes, the rest after, how far it reaches, how wide the beam is and how far it
	 * swings each way (degrees), what it does, and how long it slows and how much frost it lays on; the pillars it raises, how tall
	 * and wide, how far beside its prey, and how close behind one counts as sheltered.
	 */
	public static final int BREATH_TELL = 16, BREATH_TICKS = 24, BREATH_RECOVER = 26, BREATH_SLOW = 60, BREATH_FROST = 60;
	public static final double BREATH_REACH = 10, BREATH_WIDTH = 12, BREATH_SWING = 70, BREATH_DAMAGE = TIER.hit(11);
	public static final float PILLAR_HEIGHT = 3.5F, PILLAR_WIDTH = 1.4F;
	public static final double PILLAR_SIDE = 2.5, PILLAR_COVER = 0.9;
	/** The bounds: the crouch, how many hops (more fierce), ticks on the ground between and in the air for each, how high and far each goes, and how wide and hard each landing and the last land. */
	public static final int POUNCE_TELL = 12, HOPS = 3, HOPS_FIERCE = 4, HOP_GAP = 4, HOP_TICKS = 10, POUNCE_RECOVER = 26;
	public static final int HOP_CYCLE = HOP_GAP + HOP_TICKS;
	public static final double HOP_HEIGHT = 1.8, HOP_REACH = 6, HOP_RADIUS = 2.0, HOP_DAMAGE = TIER.hit(12), LAST_RADIUS = 2.8, LAST_DAMAGE = TIER.hit(17);
	/**
	 * The dive: the crouch, the climb, and the whole tell (the hover after the climb); how long before the dive its aim is fixed; how
	 * high it climbs; how long the dive and the skid after take; how far it dives at most and how far it skids; how close it passes
	 * to strike, and what it does; and how long it sprawls after.
	 */
	public static final int DIVE_CROUCH = 4, DIVE_RISE = 10, DIVE_TELL = 24, DIVE_LOCK = 6, DIVE_TICKS = 8, DIVE_SKID = 8, DIVE_SPRAWL = 30;
	public static final double DIVE_HEIGHT = 5, DIVE_REACH = 12, DIVE_MIN = 3, DIVE_SKID_REACH = 4, DIVE_RADIUS = 1.6, DIVE_DAMAGE = TIER.hit(18);
	/** The slide: the tell (breathing the strip, then turning round), when in it it turns, how long the slide takes, how far, how wide the strip and how close it passes to strike, what it does, and how long it sprawls after. */
	public static final int SLIDE_TELL = 16, SLIDE_TURN = 8, SLIDE_TICKS = 12, SLIDE_SPRAWL = 32;
	public static final double SLIDE_REACH = 12, SLIDE_WIDTH = 2.4, SLIDE_RADIUS = 1.8, SLIDE_DAMAGE = TIER.hit(14);
	/** The icefall: the tell, when it marks the floor, how many marks per player (more fierce), ticks between falls, how long each falls, how close to strike, what it does, the rest after, and how high it looks for the ceiling. */
	public static final int ICEFALL_TELL = 18, MARK_AT = 4, MARKS = 3, MARKS_FIERCE = 5, ICEFALL_EVERY = 3, ICICLE_FALL = 8, ICEFALL_RECOVER = 16;
	public static final double ICICLE_RADIUS = 1.4, ICICLE_DAMAGE = TIER.hit(13), ICEFALL_CEILING = 9;
	/** Its shell: the tell, when the burst is warned of, the cocoon's radius and shards, the burst's reach, damage and shove; the ring after, its speed, reach, damage and height; and how long it stands dazed. */
	public static final int SHELL_TELL = 40, SHELL_WARN = 10, SHELL_SHARDS = 8, DAZED_TICKS = 40;
	public static final double SHELL_RADIUS = 1.8, BURST_RADIUS = 3.5, BURST_DAMAGE = TIER.hit(14), BURST_SHOVE = 1.6;
	public static final double RING_SPEED = 0.8, RING_REACH = 14, RING_DAMAGE = TIER.hit(10), RING_HEIGHT = 0.75, RING_LIFT = 0.3;

	/** The hop aside: how long it lasts, how fast it goes, and the least time between two. */
	public static final int BOUND_TICKS = 6, BOUND_REST = 50;
	public static final double BOUND_SPEED = 0.6;
	/** The beat away: how much hurt, fading each tick by {@link #STING_FADE}, sends it off, how fast it goes, and the least time between two. */
	public static final double STING_AFTER = 40, STING_FADE = 0.97, LEAP_SPEED = 0.8;
	public static final int LEAP_TICKS = 9, LEAP_REST = 220;
	/**
	 * Rime Lances: the rear (its tell); how long after the slam the lances begin to burst, behind the crack; how many lines it sends
	 * out in all; how far they run and how fast, in blocks a tick; how close to a lance someone must stand to be struck; what a lance
	 * does, and how high it flings; how long each lance stands; and the rest after.
	 */
	public static final int LANCE_TELL = 18, LANCE_WARN = 8, LANCE_LINES = 8, LANCE_HOLD = 24, LANCE_RECOVER = 28;
	public static final double LANCE_REACH = 14, LANCE_SPEED = 1, LANCE_RADIUS = 0.95, LANCE_DAMAGE = TIER.hit(14), LANCE_LIFT = 0.8, LANCE_START = 1.5;
	/** How long the lances take to run out to their full reach once they begin. */
	public static final int LANCE_RUN = (int) Math.ceil((LANCE_REACH - LANCE_START) / LANCE_SPEED);

	/**
	 * The Rimescale, taken up once the Wyrmling is dead: its bearer never freezes, and whatever hostile strikes them in melee is bitten
	 * by frost in return, slowed, chilled through and hurt a little; then the scale must rime over again, {@link #RIME_REST} ticks.
	 */
	public static final int RIME_REST = 100, RIME_SLOW = 80, RIME_FROST = 160;
	public static final double RIME_DAMAGE = TIER.hit(3);
	/** A living Wyrmling this close keeps anyone from the scale. */
	public static final double SCALE_GUARD = 48;
	/** Whether the scale bites back at a blow {@code sinceRime} ticks after it last did. */
	public static boolean rimes(long sinceRime){return sinceRime>=RIME_REST;}

	/** Ticks between its moves, fewer once it is fierce; how many moves it runs together fierce, and the extra rest after a run. */
	public static final int GAP = 9, GAP_FIERCE = 5, CHAIN_MOST = 2, CHAIN_REST = 10;
	/** How many moves back it remembers, so it doesn't fall into the same few. */
	public static final int MEMORY = 5;

	/** The moves it picks among. */
	public static final int[] PICKS = {BITE, TAIL, GALE, BREATH, POUNCE, DIVE, SLIDE, ICEFALL, LANCES};
	/** The least time between two of {@code move}, a quarter less once it is fierce. */
	public static int rest(int move, boolean fierce){
		int r = switch (move) {
			case BITE -> 20; case TAIL -> 40; case GALE -> 90; case BREATH -> 140; case POUNCE -> 110;
			case DIVE -> 200; case SLIDE -> 160; case ICEFALL -> 180; case LANCES -> 280;
			default -> 0;
		};
		return fierce ? r * 3 / 4 : r;
	}
	/**
	 * How much it wants each of {@link #PICKS} with its prey {@code distance} away, {@code angle} degrees off its facing and
	 * {@code height} above its feet, each move {@code since} ticks after it last made it (by move); 0 for a move that doesn't suit or is
	 * still resting. Its jaws and wings for anyone close in front, its tail for anyone round its flank or behind, breath and slide a
	 * little way off, bounds, dives and icicles for anyone keeping away, and its lances once it is fierce.
	 */
	public static double[] weights(double distance, double angle, double height, long[] since, boolean fierce){
		double[] w = new double[PICKS.length];
		double off = Math.abs(wrap(angle));
		boolean level = height < 2.5;
		for (int i = 0; i < PICKS.length; i++) {
			int m = PICKS[i];
			if (since[m] < rest(m, fierce)) continue;
			w[i] = switch (m) {
				case BITE -> distance <= BITE_REACH && off < 45 && level ? (distance <= 3.5 ? 3 : 1.5) : 0;
				case TAIL -> distance <= TAIL_REACH && level ? (off >= 100 ? 4 : off >= 60 ? 2.5 : 0.4) : 0;
				case GALE -> distance <= 6 && off < 60 ? (distance <= 3.5 ? 2 : 1.5) : 0;
				case BREATH -> distance >= 4 && distance <= BREATH_REACH && off <= 90 ? 2.5 : 0;
				case POUNCE -> distance >= 5 && distance <= 16 && level ? (distance >= 8 ? 2.5 : 1.5) : 0;
				case DIVE -> distance >= 6 && distance <= 14 && level ? (distance >= 9 ? 3 : 1.5) : 0;
				case SLIDE -> distance >= 5 && distance <= SLIDE_REACH && height < 2 ? 2 : 0;
				case ICEFALL -> !level ? 3 : distance >= 3 ? (distance >= 8 ? 2 : 1) : 0;
				case LANCES -> fierce && distance <= LANCE_REACH - 2 ? 2.5 : 0;
				default -> 0;
			};
		}
		return w;
	}
	/** Its next move, drawn from {@code deck} with {@code roll} 0-1; -1 to keep closing. */
	public static int choose(MoveDeck deck, double distance, double angle, double height, long[] since, boolean fierce, double roll){
		return deck.pick(PICKS, weights(distance, angle, height, since, fierce), roll);
	}
	/**
	 * Fierce, the move it runs straight into after {@code move} with its prey {@code distance} away, {@code roll} 0-1; -1 for none.
	 * A snap runs into a lash or a gale, a lash into a snap or a slide, a gale into a dive or its breath, its breath into a gale or a
	 * bound, its bounds into a lash, and the icicles into bounds or a snap. A dive or a slide always ends sprawled.
	 */
	public static int chain(int move, double distance, double roll){
		return switch (move) {
			case BITE -> distance > TAIL_REACH ? -1 : roll < 0.5 ? TAIL : roll < 0.8 ? GALE : -1;
			case TAIL -> distance <= BITE_REACH ? (roll < 0.6 ? BITE : -1) : distance <= SLIDE_REACH ? (roll < 0.6 ? SLIDE : -1) : -1;
			case GALE -> distance >= 6 ? (roll < 0.5 ? DIVE : roll < 0.85 ? BREATH : -1) : distance >= 4 ? (roll < 0.6 ? BREATH : -1) : -1;
			case BREATH -> distance <= 6 ? (roll < 0.5 ? GALE : -1) : (roll < 0.4 ? POUNCE : -1);
			case POUNCE -> distance <= TAIL_REACH && roll < 0.6 ? TAIL : -1;
			case ICEFALL -> distance >= 5 ? (roll < 0.5 ? POUNCE : -1) : distance <= BITE_REACH && roll < 0.5 ? BITE : -1;
			default -> -1;
		};
	}
	/** Whether it shuts itself in its shell: once, the first time it is half beaten. */
	public static boolean shells(float health, float max, boolean fierce){return !fierce && wounded(health, max);}

	/** The bar's name for what it is doing. */
	public static String key(int move){
		return switch (move) {
			case BITE -> "bite"; case TAIL -> "tail"; case GALE -> "gale"; case BREATH -> "breath"; case POUNCE -> "pounce";
			case BOUND -> "bound"; case RECOVER -> "recover"; case LANCES -> "lances"; case DIVE -> "dive"; case SLIDE -> "slide";
			case ICEFALL -> "icefall"; case SPRAWL -> "sprawl"; case SHELL -> "shell"; case DAZED -> "dazed";
			default -> "watch";
		};
	}

	/** Whether its snap reaches someone {@code forward} ahead, {@code side} across and {@code height} above its feet, in its locked aim. */
	public static boolean bitten(double forward, double side, double height){
		return forward > -0.5 && forward <= BITE_REACH && Math.abs(side) <= 1.8 && height > -1.5 && height < 4.0;
	}
	/** Where its tail points, degrees off its facing (unwrapped, mirrored by {@code side}), {@code tick} into the lash. */
	public static double tail(int side, double tick){
		return side * (45 + TAIL_ARC * Math.min(1, Math.max(0, tick / TAIL_TICKS)));
	}
	/**
	 * Whether its tail, sweeping toward {@code side}, passes over someone {@code angle} degrees off its facing, {@code flat} away and
	 * {@code height} above its feet, between the tick before {@code tick} and {@code tick}. It sweeps low: someone in the air clears it.
	 */
	public static boolean lashed(int side, int tick, double angle, double flat, double height){
		double u = ((side * angle) % 360 + 360) % 360, a = side * tail(side, tick - 1), b = side * tail(side, tick);
		return u >= a - 12 && u <= b + 12 && flat >= 0.8 && flat <= TAIL_REACH && height > -1.5 && height < TAIL_HEIGHT;
	}
	/** Whether a beat of its wings catches someone {@code forward} ahead, {@code side} across and {@code height} above its feet. */
	public static boolean gusted(double forward, double side, double height){
		return forward > 0.5 && forward <= GALE_REACH && Math.abs(side) <= 1.2 + forward * 0.55 && height > -1.5 && height < 4.0;
	}
	/** Where its breath points, degrees off its aim, {@code tick} after it begins breathing toward {@code dir}; fierce, it sweeps back the other way after. */
	public static double beam(int dir, double tick){
		if (tick <= BREATH_TICKS) return dir * (-BREATH_SWING + 2 * BREATH_SWING * Math.max(0, tick) / BREATH_TICKS);
		return dir * (BREATH_SWING - 2 * BREATH_SWING * Math.min(BREATH_TICKS, tick - BREATH_TICKS) / BREATH_TICKS);
	}
	/** Whether someone {@code off} degrees off the beam, {@code flat} away and {@code height} above its feet, stands in its breath. */
	public static boolean breathed(double off, double flat, double height){
		double o = Math.abs(wrap(off));
		boolean in = o <= BREATH_WIDTH || o < 90 && Math.sin(Math.toRadians(o)) * flat <= 0.8;
		return in && flat > 0.8 && flat <= BREATH_REACH && height > -1.5 && height < 3.5;
	}
	/** Whether a pillar at ({@code px}, {@code pz}) stands between its jaws and someone at ({@code tx}, {@code tz}), both from its jaws. */
	public static boolean sheltered(double tx, double tz, double px, double pz){
		double len = tx * tx + tz * tz;
		if (len < 1e-6) return false;
		double t = (px * tx + pz * tz) / len;
		if (t <= 0 || t >= 1) return false;
		double cx = tx * t - px, cz = tz * t - pz;
		return cx * cx + cz * cz <= PILLAR_COVER * PILLAR_COVER;
	}
	/** How many hops it makes in a string. */
	public static int hops(boolean fierce){return fierce ? HOPS_FIERCE : HOPS;}
	/** How high it is through its hops, {@code sinceTell} ticks after its crouch ends: on the ground between, up in an arc in each. */
	public static double hop(double sinceTell){
		if (sinceTell < 0) return 0;
		double c = sinceTell % HOP_CYCLE;
		if (c < HOP_GAP) return 0;
		double t = (c - HOP_GAP) / HOP_TICKS;
		return 4 * HOP_HEIGHT * t * (1 - t);
	}
	/** Whether someone {@code dx}, {@code dz} from a spot and {@code height} above it is within {@code radius} of it. */
	public static boolean lands(double dx, double dz, double height, double radius){
		return dx * dx + dz * dz <= radius * radius && height > -1.5 && height < 2.5;
	}
	/** How far it dives at prey {@code distance} away: down onto them, but no nearer than {@link #DIVE_MIN} or further than {@link #DIVE_REACH}. */
	public static double dive(double distance){return Math.max(DIVE_MIN, Math.min(DIVE_REACH, distance));}
	/** Whether a ring running out from {@code from} to {@code to} this tick passes someone {@code distance} out and {@code height} up. */
	public static boolean crosses(double distance, double from, double to, double height){
		return distance > from - 0.5 && distance <= to + 0.5 && height > -1.5 && height < RING_HEIGHT;
	}
	/** How far out along its line the lances have burst, {@code sinceSlam} ticks after its forefeet came down; 0 before they begin. */
	public static double lanceFront(long sinceSlam){
		if (sinceSlam < LANCE_WARN) return 0;
		return Math.min(LANCE_REACH, LANCE_START + (sinceSlam - LANCE_WARN) * LANCE_SPEED);
	}
	/**
	 * The yaws of the lines it sends out: one at each of {@code prey} (yaws, the first its target), and the rest fanned evenly round
	 * it from the first, skipping any that would run too close beside a line already aimed.
	 */
	public static float[] lines(float[] prey){
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
	public static boolean lanced(double along, double across, double height, double front){
		return along >= LANCE_START - LANCE_RADIUS && along <= front + 0.3 && Math.abs(across) <= LANCE_RADIUS && height > -1.5 && height < 2.5;
	}

	/**
	 * What its scales leave of a blow. Its horned head and the frost on its brow turn some of a blow from the front; its flanks take
	 * it all. Spent, sprawled or dazed it is open; reared for its lances or the icicles its pale belly is bared; and shut in its shell
	 * the ice takes nearly everything.
	 */
	public static double armour(int move, boolean frontal){
		return switch (move) {
			case RECOVER -> 1.3; case SPRAWL -> 1.35; case DAZED -> 1.4; case LANCES -> 1.15; case ICEFALL -> 1.0; case SHELL -> 0.2;
			default -> frontal ? 0.6 : 1.0;
		};
	}
	/** Whether a blow arriving from {@code angle} degrees off its facing lands on its front. */
	public static boolean frontal(double angle){return Math.abs(wrap(angle)) <= 50;}
	/** {@code angle} brought into -180 to 180. */
	public static double wrap(double angle){return ((angle % 360) + 540) % 360 - 180;}
	/** Whether it hops aside from a blow: only while it watches, not too often, and only some of the time. */
	public static boolean dodges(int move, long sinceDodge, double roll){return move == WATCH && sinceDodge >= BOUND_REST && roll < 0.3;}
	/** Whether the hurt it has taken lately, {@code stung}, sends it beating away, {@code sinceLeap} ticks after it last did. */
	public static boolean leaps(int move, double stung, long sinceLeap){
		return (move == WATCH || move == RECOVER) && stung >= STING_AFTER && sinceLeap >= LEAP_REST;
	}
	/** Whether it is half beaten. */
	public static boolean wounded(float health, float max){return health <= max * 0.5F;}

	/** Whether a point lies inside a hall from (x0,y0,z0) to (x1,y1,z1), widened by {@code margin} on every side. */
	public static boolean within(double x, double y, double z, double x0, double y0, double z0, double x1, double y1, double z1, double margin){
		return x >= x0 - margin && x <= x1 + 1 + margin && y >= y0 - margin && y <= y1 + 1 + margin && z >= z0 - margin && z <= z1 + 1 + margin;
	}
	/** Whether a player at this offset from its mound is close enough to wake it (and inside the hall, which the caller checks). */
	public static boolean wakes(double dx, double dy, double dz){
		return dx * dx + dz * dz <= WAKE_RADIUS * WAKE_RADIUS && Math.abs(dy) <= WAKE_HEIGHT;
	}
	/** The scripted states: it cannot be hurt, and it shows no bar. */
	public static boolean scripted(int move){return move == DORMANT || move == RISE || move == BED;}
	/** How far up it is, 0 curled to 1 standing, {@code elapsed} ticks into a rise (or curling up, which runs it backwards). */
	public static float rise(int move, float elapsed){
		float t = Math.max(0, Math.min(1, elapsed / RISE_TICKS));
		return switch (move) {
			case DORMANT -> 0;
			case RISE -> t;
			case BED -> 1 - t;
			default -> 1;
		};
	}
	/** Health it knits back this tick, {@code sinceCut} ticks after it was last hurt: a Master's regeneration. */
	public static float regen(float maxHealth, long sinceCut){return MastersRules.regen(maxHealth, sinceCut);}
}
