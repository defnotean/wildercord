package dev.wildercord.aura.world;

import dev.wildercord.cast.MoveDeck;

/**
 * The Ashen Stag, the burnt glade's guardian: its states, its wake-up and the glade it keeps. It lies bedded in the ash before the
 * smouldering stump, facing the gate, until someone comes far enough into the glade; then it gets up, hindquarters first, bellows,
 * and the fight is on. Quicker on its feet than the Bell-Warden and lighter in the hand; a step below a Master, like the others.
 * <p>
 * It never makes the same move twice running while it has another it could make: it tosses whoever is in front of it on a twist of
 * its antlers, bucks embers out behind it, rears and stamps a ring of fire out across the ash, charges and skids round at the end,
 * rakes cinders down on anyone keeping away, calls a herd of ember deer across the glade, hides in a pall of smoke and stalks out of
 * it, and locks its antlers on whoever stands in front of it. Half beaten it kindles: fire climbs its antlers for good, and it comes
 * on quicker, its moves in pairs, and most of them grown worse.
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
	/** Head low and twisted, tines scraping sparks off the ash, then wrenched up through whoever stands square in front of it. */
	public static final int TOSS = 4;
	/** Tail flagged and head to its knees, then both hind hooves lashed out behind it, kicking a fan of embers after them. */
	public static final int BUCK = 5;
	/** Reared up tall on its hind legs, belly bare, then both forehooves stamped down ahead, a ring of fire running out from them. */
	public static final int REAR = 6;
	/** Pawing the ash with its head down, then its whole weight run along a fixed line, a burning wake left behind it. */
	public static final int CHARGE = 7;
	/** A bound to one side out from under a blow; or, hurt too fast, a leap away back across the glade. */
	public static final int BOUND = 8;
	/** Spent after an attack, head hanging and sides heaving: the opening. */
	public static final int RECOVER = 9;
	/** A charge that ran into a trunk or the bank: its antlers driven into it, and it heaving to pull them free. The big opening. */
	public static final int STUCK = 10;
	/**
	 * Kindled, its charge becomes the Cinder Rush: {@link #RUSH_RUNS} charges back to back, wheeling and pawing between them, each
	 * leaving a strip of embers smouldering along its track; the last skids round. A run into a trunk still sticks it fast.
	 */
	public static final int RUSH = 11;
	/** Antlers dragged side to side through the ash, flinging cinders that come down burning where its prey is going. */
	public static final int RAKE = 12;
	/** A long bellow calling a herd of ember deer across the glade in lanes, with gaps between them to stand in. */
	public static final int STAMPEDE = 13;
	/** Its antlers shaken until smoke pours off them, a blinding pall left where it stood; then it stalks out of it. */
	public static final int SMOKE = 14;
	/** Prowling low out of its smoke, its glow dimmed, until it is close enough to lunge. */
	public static final int STALK = 15;
	/** Antlers levelled and a forehoof scraping, then a short lunge that locks its antlers on whoever it catches, grinding them. */
	public static final int LOCK = 16;
	/** The end of a charge: hooves dug in, sliding and wheeling round, its flank turned. */
	public static final int SKID = 17;
	/** Its antlers wrenched free of a lock by force, and it staggering, head swinging: wide open. */
	public static final int STAGGER = 18;
	/** Half beaten, once: reared and bellowing to the sky while fire climbs its antlers, then down in a blast and a great ring of fire. */
	public static final int KINDLE = 19;
	/** The lunge out of a stalk: antlers flared, a snort, and a short hard spring at whoever is nearest. */
	public static final int LUNGE = 20;

	/** How hard it is among the guardians (see {@link BossTier}). */
	public static final BossTier TIER = BossTier.VETERAN;
	public static final double HEALTH = TIER.health, ARMOUR = TIER.armour, SPEED = TIER.speed(0.3);

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
	/** The share of what armour, Protection and Resistance would stop that its blows go through anyway. */
	public static final double PIERCE = 0.35;

	// ---- the moves: each with its own tell, its own way out of it, and some with an opening after.
	/** The toss: its tell (shorter run on from another move), how far it turns a tick through it, how early the aim locks, how long
	 *  the antlers stay up, the rest after; and what it does. Get beside it. */
	public static final int TOSS_TELL = 14, TOSS_TELL_CHAINED = 9, TOSS_LOCK = 6, TOSS_HOLD = 4, TOSS_RECOVER = 14;
	public static final double TOSS_TURN = 30, TOSS_DAMAGE = TIER.hit(13);
	/** The buck: its tell, the second tell kindled when it wheels and bucks again, the rest after; what the hooves and the embers do,
	 *  how far the embers carry and how wide they fan, and how far it wheels for the second. Get beside its flank. */
	public static final int BUCK_TELL = 12, BUCK_TELL_CHAINED = 8, BUCK_RETELL = 8, BUCK_RECOVER = 14;
	public static final double BUCK_DAMAGE = TIER.hit(17), FAN_DAMAGE = TIER.hit(7), FAN_REACH = 6.5, FAN_ANGLE = 35, BUCK_PIVOT = 70;
	/** The rear: its tell, how early the aim locks, the second stamp kindled, the rest after; where the hooves come down ahead of it,
	 *  how far round that they crush and for how much. Jump the ring that runs out after. */
	public static final int REAR_TELL = 16, REAR_TELL_CHAINED = 11, REAR_LOCK = 4, REAR_AGAIN = 12, REAR_RECOVER = 16;
	public static final double REAR_AHEAD = 2.2, REAR_RADIUS = 2.6, REAR_DAMAGE = TIER.hit(15);
	/** The ring of fire a stamp sends out: how fast it runs, how far, how high it stands and what it does. */
	public static final double WAVE_SPEED = 0.7, WAVE_REACH = 11, WAVE_HEIGHT = 0.75, WAVE_DAMAGE = TIER.hit(9);
	/** The charge: its tell, how early the aim locks, how long it runs and how far; what it does. Sidestep, and keep off its wake. */
	public static final int CHARGE_TELL = 14, CHARGE_TELL_CHAINED = 10, CHARGE_LOCK = 4, CHARGE_TICKS = 11;
	public static final double CHARGE_REACH = 13, CHARGE_DAMAGE = TIER.hit(20);
	/** The skid at the end of a charge: how long it lasts, and for how much of it it is still sliding. */
	public static final int SKID_TICKS = 24, SKID_SLIDE = 8;
	/** The Cinder Rush: how many runs, the tell before the first and the wheel before each after, how long each run lasts, how far each
	 *  goes and what it does; and how long its strips smoulder. */
	public static final int RUSH_RUNS = 3, RUSH_TELL = 16, RUSH_TURN = 9, RUSH_TICKS = 9, RUSH_TRAIL = 140;
	public static final double RUSH_REACH = 12, RUSH_DAMAGE = TIER.hit(16);
	/** The embers a charge leaves: how long they smoulder, how often they burn whoever stands in them, for how much, and how wide. */
	public static final int EMBER_TICKS = 100, EMBER_EVERY = 10;
	public static final double EMBER_DAMAGE = TIER.hit(3), EMBER_WIDTH = 0.9;
	/** The rake: its tell, how long it rakes, how often it flings a cinder, the rest after; how far a cinder carries, how long it is in
	 *  the air, how wide it bursts and for how much, and how long the patch it leaves smoulders. Step off the closing rings. */
	public static final int RAKE_TELL = 12, RAKE_TICKS = 30, RAKE_EVERY = 6, RAKE_RECOVER = 12, CINDER_FLIGHT = 20, PATCH_TICKS = 40;
	public static final double CINDER_RANGE = 14, CINDER_RADIUS = 1.8, CINDER_DAMAGE = TIER.hit(8);
	/** The herd: its tell, how long it stands calling them on after, the rest after, when the second herd comes kindled, how far apart
	 *  their deer set off down a lane; the lanes' spacing and half width, how deep the herd runs, how far either side they come from,
	 *  how fast they run and what they do. Stand in the gaps. */
	public static final int HERD_TELL = 18, HERD_HOLD = 20, HERD_RECOVER = 10, HERD_AGAIN = 30, HERD_STAGGER = 7;
	public static final double LANE_SPACING = 5, LANE_HALF = 1.25, HERD_DEPTH = 16, HERD_WIDTH = 18, DEER_SPEED = 1.1, DEER_DAMAGE = TIER.hit(10);
	/** The smoke: its tell, how long the pall hangs, how wide, how long it blinds and how often it blinds again. Get out of it. */
	public static final int SMOKE_TELL = 12, SMOKE_TICKS = 80, SMOKE_BLIND = 50, SMOKE_EVERY = 10;
	public static final double SMOKE_RADIUS = 7;
	/** The stalk out of the smoke: how long it prowls at most, and how close it gets before it lunges. */
	public static final int STALK_TICKS = 50;
	public static final double STALK_NEAR = 5;
	/** The lunge: its tell, how early the aim locks, how long it springs and how far, the second tell kindled, the rest after; and what
	 *  it does. Sidestep on the snort. */
	public static final int LUNGE_TELL = 10, LUNGE_LOCK = 3, LUNGE_TICKS = 4, LUNGE_RETELL = 7, LUNGE_RECOVER = 16;
	public static final double LUNGE_REACH = 5, LUNGE_DAMAGE = TIER.hit(16);
	/** The lock: its tell, how early the aim locks, how long the lunge, how long it holds at most, how often it grinds, how many blows
	 *  to its front break it, the rest after a throw and after a miss, and the stagger when it breaks; how far the lunge goes, where it
	 *  holds its prey, how far they can get before it lets go, what the catch, the grind and the throw do. Dodge sideways, or break it. */
	public static final int LOCK_TELL = 16, LOCK_LOCK = 3, LOCK_TICKS = 4, LOCK_HOLD = 50, LOCK_GRIND = 12, LOCK_BREAK = 2,
		LOCK_RECOVER = 14, LOCK_MISS = 20, STAGGER_TICKS = 40;
	public static final double LOCK_REACH = 3.5, LOCK_AHEAD = 2.6, LOCK_SLIP = 6, LOCK_DAMAGE = TIER.hit(8), GRIND_DAMAGE = TIER.hit(3),
		THROW_DAMAGE = TIER.hit(14);
	/** The kindling: its tell, the rest after, how far round the blast reaches and for how much, and the great ring it sends out. */
	public static final int KINDLE_TELL = 30, KINDLE_RECOVER = 20;
	public static final double KINDLE_RADIUS = 6, KINDLE_DAMAGE = TIER.hit(12), KINDLE_WAVE_SPEED = 0.8, KINDLE_WAVE_REACH = 14;

	/** How long its antlers stay stuck in whatever a charge ran it into. */
	public static final int STUCK_TICKS = 60;
	/** The bound aside: how long it lasts, how fast it goes, and the least time between two. */
	public static final int BOUND_TICKS = 6, BOUND_REST = 40;
	public static final double BOUND_SPEED = 0.65;
	/** The leap away: how much hurt, fading each tick by {@link #STING_FADE}, sends it off, how fast it goes, and the least time between two. */
	public static final double STING_AFTER = 40, STING_FADE = 0.97, LEAP_SPEED = 0.85;
	public static final int LEAP_TICKS = 8, LEAP_REST = 220;
	/** Ticks between its moves, fewer once it is kindled; how many it runs on into one after another kindled, and the extra rest after. */
	public static final int GAP = 10, GAP_KINDLED = 5, CHAIN_MOST = 2, CHAIN_REST = 10;

	/** The bar's name for what it is doing. */
	public static String key(int move) {
		return switch (move) {
			case TOSS -> "toss";
			case BUCK -> "buck";
			case REAR -> "rear";
			case CHARGE -> "charge";
			case BOUND -> "bound";
			case RECOVER -> "recover";
			case STUCK -> "stuck";
			case RUSH -> "rush";
			case RAKE -> "rake";
			case STAMPEDE -> "herd";
			case SMOKE -> "smoke";
			case STALK -> "stalk";
			case LOCK -> "lock";
			case SKID -> "skid";
			case STAGGER -> "stagger";
			case KINDLE -> "kindle";
			case LUNGE -> "lunge";
			default -> "watch";
		};
	}

	/** The moves it picks among. Kindled, a picked charge is a Cinder Rush. */
	public static final int[] PICKS = {TOSS, BUCK, REAR, CHARGE, RAKE, STAMPEDE, SMOKE, LOCK};
	/** The least time between two of {@code move}, shorter once it is {@code kindled}. */
	public static int rest(int move, boolean kindled) {
		int r = switch (move) {
			case TOSS -> 30;
			case BUCK -> 40;
			case REAR -> 80;
			case CHARGE -> 110;
			case RAKE -> 140;
			case STAMPEDE -> 420;
			case SMOKE -> 320;
			case LOCK -> 220;
			default -> 0;
		};
		return kindled ? r * 2 / 3 : r;
	}
	/**
	 * How much it wants each of {@link #PICKS} with its prey {@code distance} away, standing {@code behind} it or not, their feet
	 * {@code height} above its own, {@code since} holding the ticks since it last made each move (by move). Up close in front the toss
	 * and the lock, the rear a little further; its hind hooves for anyone at its back; the charge and the rake for anyone keeping away,
	 * the herd for anyone not right on top of it, the smoke for anyone near. Anyone up out of reach of its antlers gets cinders. Nothing
	 * for a move still resting.
	 */
	public static double[] weights(double distance, boolean behind, double height, long[] since, boolean kindled) {
		double[] w = new double[PICKS.length];
		boolean up = height > 2;
		for (int i = 0; i < PICKS.length; i++) {
			int m = PICKS[i];
			if (since[m] < rest(m, kindled)) continue;
			double d = distance;
			w[i] = switch (m) {
				case TOSS -> up || behind || d > 4.4 ? 0 : d <= CLOSE ? 3 : 1.5;
				case BUCK -> up || !behind ? 0 : d <= 4 ? 4 : d <= FAN_REACH ? 2 : 0;
				case REAR -> up ? 0.3 : behind ? 0.8 : d <= 5 ? 2.5 : d <= 11 ? 1.5 : 0;
				case CHARGE -> up || d < 5 || d > CHARGE_REACH + 3 ? 0 : (d >= 9 ? 3 : 1.5) * (behind ? 0.5 : 1);
				case RAKE -> up ? 4 : d >= 4 && d <= CINDER_RANGE ? (d >= 8 ? 2.5 : 1.5) : 0;
				case STAMPEDE -> up || d < 3 ? 0 : kindled ? 2 : 1.5;
				case SMOKE -> up ? 1 : d <= 9 ? 1.2 : 0;
				case LOCK -> up || behind || d < 2 || d > 6 ? 0 : 2.2;
				default -> 0;
			};
		}
		return w;
	}
	/** Its next move, by {@link #weights} and what it has been doing lately; -1 to keep closing. {@code roll} is 0-1. */
	public static int choose(MoveDeck deck, double distance, boolean behind, double height, long[] since, boolean kindled, double roll) {
		return deck.pick(PICKS, weights(distance, behind, height, since, kindled), roll);
	}
	/** Kindled, the move it runs straight on into after {@code move}, its prey {@code distance} away; -1 for none. {@code roll} is 0-1. */
	public static int chain(int move, double distance, double roll) {
		return switch (move) {
			case TOSS -> roll < 0.45 ? REAR : -1;
			case BUCK -> distance <= 4.4 && roll < 0.5 ? TOSS : -1;
			case REAR -> distance >= 6 ? (roll < 0.5 ? CHARGE : -1) : roll < 0.4 ? TOSS : -1;
			case RAKE -> roll < 0.4 ? REAR : -1;
			case STAMPEDE -> roll < 0.5 ? RAKE : -1;
			default -> -1;
		};
	}
	/** Whether a charge it sets off on is a Cinder Rush: kindled, and not run on into from another move. */
	public static boolean rushes(boolean kindled, boolean chained) {
		return kindled && !chained;
	}
	/** Whether it kindles now: down to half health for the first time. */
	public static boolean kindles(float health, float max, boolean kindled) {
		return !kindled && wounded(health, max);
	}
	/** Whether {@code blows} to its front while its antlers are locked on someone wrench them free. */
	public static boolean breaks(int blows) {
		return blows >= LOCK_BREAK;
	}

	/** Whether a blow reaches someone {@code forward} ahead, {@code side} across and {@code height} above its feet, in its locked aim. */
	public static boolean hits(int move, double forward, double side, double height) {
		return switch (move) {
			case TOSS -> forward > -0.5 && forward <= 4.4 && Math.abs(side) <= 1.3 && height > -1.5 && height < 3.5;
			case BUCK -> forward < 0.5 && forward >= -3.5 && Math.abs(side) <= 2.0 && height > -1.5 && height < 3.0;
			case CHARGE, RUSH -> forward > -1.5 && forward <= 3.2 && Math.abs(side) <= 2.0 && height > -1.5 && height < 4.5;
			case LOCK -> forward > -0.5 && forward <= 3.2 && Math.abs(side) <= 1.3 && height > -1.5 && height < 3.0;
			case LUNGE -> forward > -1.0 && forward <= 3.4 && Math.abs(side) <= 1.6 && height > -1.5 && height < 4.0;
			default -> false;
		};
	}
	/** Whether the embers a buck kicks out behind it catch someone {@code forward} ahead (so, behind it), {@code side} across, {@code height} up. */
	public static boolean fans(double forward, double side, double height) {
		if (forward > -0.5 || height <= -1.5 || height >= 3.0) return false;
		return forward * forward + side * side <= FAN_REACH * FAN_REACH && Math.toDegrees(Math.atan2(Math.abs(side), -forward)) <= FAN_ANGLE;
	}
	/** Whether the forehooves of a rear, coming down {@link #REAR_AHEAD} ahead, crush someone {@code forward} ahead, {@code side} across. */
	public static boolean crushes(double forward, double side, double height) {
		double f = forward - REAR_AHEAD;
		return f * f + side * side <= REAR_RADIUS * REAR_RADIUS && height > -1.5 && height < 2.0;
	}
	/** Whether a ring of fire running out from {@code from} to {@code to} this tick passes someone {@code distance} out, feet {@code height} up. */
	public static boolean crosses(double distance, double from, double to, double height) {
		return distance > from - 0.5 && distance <= to + 0.5 && height > -1.5 && height < WAVE_HEIGHT;
	}
	/** Whether a cinder bursting at an offset of ({@code dx},{@code dy},{@code dz}) from someone burns them. */
	public static boolean cinders(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= CINDER_RADIUS * CINDER_RADIUS && dy > -1.5 && dy < 2.5;
	}
	/** Whether the blast of its kindling, at an offset of ({@code dx},{@code dy},{@code dz}), reaches someone. */
	public static boolean blasts(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= KINDLE_RADIUS * KINDLE_RADIUS && dy > -1.5 && dy < 3.0;
	}
	/** Whether someone at an offset of ({@code dx},{@code dy},{@code dz}) from the middle of its smoke is inside it. */
	public static boolean smothered(double dx, double dy, double dz) {
		return dx * dx + dz * dz <= SMOKE_RADIUS * SMOKE_RADIUS && Math.abs(dy) < 4;
	}
	/**
	 * The lanes a herd runs down, as distances along its facing: one through {@code offset} (where its prey stands, or for the second
	 * herd half a lane on from there) and every {@link #LANE_SPACING} either side of it, as deep as the herd runs.
	 */
	public static double[] lanes(double offset) {
		int lo = (int) Math.ceil((-HERD_DEPTH - offset) / LANE_SPACING), hi = (int) Math.floor((HERD_DEPTH - offset) / LANE_SPACING);
		double[] out = new double[Math.max(0, hi - lo + 1)];
		for (int n = lo; n <= hi; n++) out[n - lo] = offset + n * LANE_SPACING;
		return out;
	}
	/** Whether someone {@code forward} along the herd's facing stands in the lane down {@code centre}. */
	public static boolean inLane(double forward, double centre) {
		return Math.abs(forward - centre) <= LANE_HALF + 0.3;
	}
	/** How far across a deer running {@code dir} (1 or -1) has come {@code ticks} after it set off. */
	public static double lateral(int dir, double ticks) {
		return dir * (-HERD_WIDTH + ticks * DEER_SPEED);
	}
	/** Whether a deer {@code ticks} after it set off has run clean across. */
	public static boolean through(double ticks) {
		return ticks * DEER_SPEED > HERD_WIDTH * 2;
	}
	/**
	 * Whether a deer running down the lane at {@code centre}, from {@code from} to {@code to} across this tick, tramples someone
	 * {@code across} and {@code forward} along the herd, feet {@code height} above the herd's ground.
	 */
	public static boolean trampled(double across, double from, double to, double forward, double centre, double height) {
		return across >= Math.min(from, to) - 1.2 && across <= Math.max(from, to) + 1.2 && inLane(forward, centre) && height > -1.5 && height < 2.5;
	}

	/**
	 * What its bark leaves of a blow. Its antlers and brow turn some of a blow from the front; its flanks take it all. Spent it is
	 * open, stuck or staggering wide open; skidding, its flank is turned. Some moves leave its front {@code open}: its throat with its
	 * antlers locked on someone, its belly reared up, its chest after a toss, its brow down in the ash raking, its throat bellowing.
	 */
	public static double armour(int move, boolean frontal, boolean open) {
		if (move == STUCK || move == STAGGER) return 1.5;
		if (move == RECOVER) return 1.3;
		if (move == SKID) return frontal ? 1.0 : 1.4;
		if (!frontal) return 1.0;
		return switch (move) {
			case LOCK -> open ? 1.5 : 0.65;
			case REAR -> open ? 1.3 : 0.65;
			case TOSS -> open ? 1.2 : 0.65;
			case RAKE -> 1.15;
			case STAMPEDE -> 1.25;
			default -> 0.65;
		};
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
	/** Whether (px,pz) stands on the ember strip from (ax,az) to (bx,bz). */
	public static boolean onStrip(double px, double pz, double ax, double az, double bx, double bz) {
		double dx = bx - ax, dz = bz - az, len = dx * dx + dz * dz;
		double t = len < 1e-9 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / len));
		double ox = px - (ax + dx * t), oz = pz - (az + dz * t);
		return ox * ox + oz * oz <= EMBER_WIDTH * EMBER_WIDTH;
	}

	/** Whether it bounds aside from a blow: only while it watches, not too often, and only some of the time. Skittish, from any side. */
	public static boolean dodges(int move, long sinceDodge, double roll) {
		return move == WATCH && sinceDodge >= BOUND_REST && roll < 0.35;
	}
	/** Whether the hurt it has taken lately, {@code stung}, sends it leaping away, {@code sinceLeap} ticks after it last did. */
	public static boolean leaps(int move, double stung, long sinceLeap) {
		return (move == WATCH || move == RECOVER) && stung >= STING_AFTER && sinceLeap >= LEAP_REST;
	}
	/** Whether it is wounded: at half health or below, and it kindles. */
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

	/**
	 * The Cinder Antler, taken up once the stag is dead: after {@link #ANTLER_SPRINT} ticks of sprinting, the first thing hostile you
	 * strike is gored the way the stag gores, thrown back {@link #ANTLER_SHOVE} and set burning {@link #ANTLER_BURN} ticks; then the
	 * antler cools {@link #ANTLER_RECHARGE} ticks before it will again.
	 */
	public static final int ANTLER_SPRINT = 30, ANTLER_RECHARGE = 400, ANTLER_BURN = 100;
	public static final double ANTLER_SHOVE = 1.4;
	/** A living stag this close keeps anyone from the antler. */
	public static final double ANTLER_GUARD = 48;

	/** Whether a blow struck after {@code sprinted} ticks of sprinting, {@code since} ticks after the last gore, gores. */
	public static boolean gores(int sprinted, long since) {
		return sprinted >= ANTLER_SPRINT && since >= ANTLER_RECHARGE;
	}

	/** Health it knits back this tick, {@code sinceCut} ticks after it was last hurt: a Master's regeneration. */
	public static float regen(float maxHealth, long sinceCut) {
		return MastersRules.regen(maxHealth, sinceCut);
	}
}
