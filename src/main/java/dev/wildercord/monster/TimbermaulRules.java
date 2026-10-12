package dev.wildercord.monster;

import dev.wildercord.cast.MoveDeck;

/**
 * The Timbermaul Bear's numbers and decisions, kept pure for its tests. It stands up and huffs at whoever comes near; back off
 * and it lets them be. Fighting, it closes in and never makes the same move twice running: a paw raised to one side and raked
 * across its front (back off, or get behind it), its jaws gaping and a lunging snap down a short lane (step aside), reared up
 * high and crushed down on a fan before it (get out of the fan, or strike it hard while it rears and topple it), a crouch with
 * its haunches wiggling and a pounce on a marked spot (leave the spot), its head down and pawing, then a charge along a line
 * (step aside, or lead it into a tree and leave it dazed), a mossy log torn up out of the forest floor and sent rolling down a
 * wide lane (jump it), and at whoever stands behind it, a spray of earth and stones dug up and kicked back (get out from behind
 * it). Half beaten it rears and roars and fights on roused: quicker, its moves in pairs, and a hug that pins whoever it falls on.
 */
public final class TimbermaulRules {
	private TimbermaulRules() {}

	/** Its step among the elites. */
	public static final EliteTier TIER = EliteTier.CHAMPION;

	/** Ambling about, or closing in between moves. */
	public static final int IDLE = 0;
	/** Stood up tall, huffing at whoever came near: a warning. */
	public static final int BLUFF = 1;
	/** A forepaw raised high to one side, then raked across its front. */
	public static final int RAKE = 2;
	/** Its jaws gaping, then a lunging snap down a short lane. */
	public static final int SNAP = 3;
	/** Reared up high, then crushed down on a fan before it. */
	public static final int CRUSH = 4;
	/** Knocked off its hind legs while reared: on its side, legs paddling. */
	public static final int TOPPLED = 5;
	/** Crouched, its haunches wiggling, then a pounce on a marked spot. */
	public static final int POUNCE = 6;
	/** Sprawled on its belly after a pounce or a hug that found nobody. */
	public static final int SPRAWLED = 7;
	/** Its head down and pawing the ground, then a charge along a line. */
	public static final int CHARGE = 8;
	/** Run into a tree or a wall: dazed, shaking its head. */
	public static final int DAZED = 9;
	/** Claws dug into the forest floor, hauling up a log, then sending it rolling down a lane. */
	public static final int LOG = 10;
	/** Its haunches up and front paws scrabbling, then earth and stones kicked out behind it. */
	public static final int DIG = 11;
	/** Reared with its forelegs flung wide, then falling forward onto whoever stands before it. Roused only. */
	public static final int HUG = 12;
	/** Lying on whoever it hugged, mauling them. */
	public static final int PINNED = 13;
	/** Half beaten: reared and roaring, throwing everyone back. Once. */
	public static final int ROAR = 14;
	/** Every pose it has. */
	public static final int MOVES = ROAR + 1;
	/** The moves it chooses among while fighting. */
	public static final int[] PICKS = {RAKE, SNAP, CRUSH, POUNCE, CHARGE, LOG, DIG, HUG};
	public static final int MEMORY = 4;

	/** It notices whoever comes this near, or only this near from someone sneaking, and gives up on prey this far off. */
	public static final double NOTICE_RANGE = 10, NOTICE_SNEAKING = 5, GIVE_UP = 28;
	/** Its warning: stood up this long, charging whoever comes this near, letting be whoever backs off this far (and staying calm a while). */
	public static final int BLUFF_TICKS = 50, CALM_TICKS = 600;
	public static final double CROWD = 5, LET_GO = 14;
	/** How fast it closes in, calm and roused, and the pause between moves while it does. */
	public static final double CHASE = 1.15, ROUSED_CHASE = 1.35;
	public static final int REST = 16, ROUSED_REST = 8;

	/** The rake: its paw raised this long, swept across its front over this long; roused, a backhand the other way after. */
	public static final int RAKE_TELL = 14, ROUSED_RAKE_TELL = 10, RAKE_SWEEP = 6, RAKE_BACK_TELL = 7, RAKE_RECOVER = 10;
	public static final double RAKE_REACH = 3.6, RAKE_DAMAGE = 8, RAKE_THROW = 0.9;
	/** The snap: jaws gaping this long, its lane locked this long before, the lunge; roused, a second snap after. */
	public static final int SNAP_TELL = 12, ROUSED_SNAP_TELL = 9, SNAP_LOCK = 3, SNAP_LUNGE = 5, SNAP_AGAIN_TELL = 7, SNAP_RECOVER = 12;
	public static final double SNAP_LENGTH = 5, SNAP_WIDTH = 0.9, SNAP_STRIDE = 0.6, SNAP_DAMAGE = 9;
	/** The crush: reared this long, then down on a fan before it; roused, it rears again at once and comes down once more. */
	public static final int CRUSH_TELL = 20, ROUSED_CRUSH_TELL = 14, CRUSH_AGAIN_TELL = 10, CRUSH_RECOVER = 14;
	public static final double CRUSH_RADIUS = 5, CRUSH_ARC = 0.34, CRUSH_DAMAGE = 11, CRUSH_LIFT = 0.35;
	public static final int CRUSH_SLOW = 60;
	/** A blow this hard while it rears topples it, and it lies this long. */
	public static final double TOPPLE_BLOW = 6;
	public static final int TOPPLED_TICKS = 50;
	/** The pounce: crouched this long, its spot following its prey until this long before, the flight, and sprawled after a miss. */
	public static final int POUNCE_TELL = 16, ROUSED_POUNCE_TELL = 12, POUNCE_AGAIN_TELL = 8, POUNCE_LOCK = 5, POUNCE_FLIGHT = 12;
	public static final int SPRAWLED_TICKS = 34, POUNCE_RECOVER = 10;
	public static final double POUNCE_NEAR = 3, POUNCE_REACH = 10, POUNCE_ARC = 2.5, POUNCE_RADIUS = 2.2, POUNCE_DAMAGE = 10;
	/** The charge: pawing this long, its line locked this long before, then a run this fast for at most this long. */
	public static final int CHARGE_TELL = 18, ROUSED_CHARGE_TELL = 12, CHARGE_AGAIN_TELL = 8, CHARGE_LOCK = 6, CHARGE_RUN = 22;
	public static final int DAZED_TICKS = 44, CHARGE_RECOVER = 14;
	public static final double CHARGE_SPEED = 0.7, CHARGE_LENGTH = 15, CHARGE_WIDTH = 1.4, CHARGE_DAMAGE = 10;
	/** The log: claws dug in this long, then the log rolls down a lane this wide at this pace; roused, a second one after. */
	public static final int LOG_TELL = 18, ROUSED_LOG_TELL = 14, LOG_LOCK = 4, LOG_AGAIN = 14, LOG_RECOVER = 12;
	public static final double LOG_LENGTH = 16, LOG_WIDTH = 2.5, LOG_SPEED = 0.4, LOG_CLEAR = 0.6, LOG_DAMAGE = 7;
	/** The dig: scrabbling this long, then kicking earth back down a cone for this long; roused, a wider cone. */
	public static final int DIG_TELL = 14, ROUSED_DIG_TELL = 10, DIG_SPRAY = 10, DIG_RECOVER = 8;
	public static final double DIG_RANGE = 7, DIG_CONE = -0.5, ROUSED_DIG_CONE = -0.2, DIG_DAMAGE = 4;
	/** The hug: reared wide this long, falling over this long onto a spot this far ahead; whoever it pins is held and mauled. */
	public static final int HUG_TELL = 16, HUG_FALL = 6, HUG_HOLD = 50, HUG_MAUL_EVERY = 15;
	public static final double HUG_AHEAD = 2.4, HUG_RADIUS = 1.8, HUG_MAUL = 4, HUG_BREAK = 10, HUG_THROW = 1.1;
	/** The roar at half health: reared this long, then everyone this near thrown back and weakened. */
	public static final int ROAR_TELL = 14, ROAR_TICKS = 34, ROAR_WEAKNESS = 100;
	public static final double ROAR_RADIUS = 7, ROAR_PUSH = 1.3;

	/** Whether it notices someone this far off. */
	public static boolean notices(double distance, boolean sneaking) {
		return distance <= (sneaking ? NOTICE_SNEAKING : NOTICE_RANGE);
	}

	/** Whether someone warned has come too near, and it charges. */
	public static boolean crowds(double distance) {
		return distance <= CROWD;
	}

	/** Whether someone warned has backed off far enough to be let be. */
	public static boolean letsGo(double distance) {
		return distance >= LET_GO;
	}

	/**
	 * What each move is worth to it now, by how far off its prey is, whether its prey stands behind it, whether two or more crowd
	 * it, and whether it is roused.
	 */
	public static double[] weights(double distance, boolean behind, boolean crowded, boolean roused) {
		var w = new double[PICKS.length];
		for (int i = 0; i < PICKS.length; i++) w[i] = switch (PICKS[i]) {
			case RAKE -> distance <= RAKE_REACH + 0.4 ? 3 : 0;
			case SNAP -> distance >= 2 && distance <= SNAP_LENGTH + 0.5 ? 2.5 : 0;
			case CRUSH -> distance <= CRUSH_RADIUS - 0.5 ? crowded ? 3 : 2 : 0;
			case POUNCE -> distance >= POUNCE_NEAR + 1 && distance <= POUNCE_REACH ? distance > 6 ? 3 : 2 : 0;
			case CHARGE -> distance >= 6 && distance <= CHARGE_LENGTH + 1 ? distance > 9 ? 3 : 2 : 0;
			case LOG -> distance >= 4 && distance <= LOG_LENGTH - 2 ? 2 : 0;
			case DIG -> behind && distance <= DIG_RANGE - 1 ? 4 : 0;
			case HUG -> roused && distance <= HUG_AHEAD + HUG_RADIUS ? 2.5 : 0;
			default -> 0;
		};
		return w;
	}

	/** Which move it makes next, or -1 to keep closing in: when nothing reaches, or only what it just did. */
	public static int choose(MoveDeck deck, double distance, boolean behind, boolean crowded, boolean roused, double roll) {
		int move = deck.pick(PICKS, weights(distance, behind, crowded, roused), roll);
		return move == deck.last() ? -1 : move;
	}

	/** Whether someone at this dot of its facing and the way to them stands behind it. */
	public static boolean behind(double dot) {
		return dot < -0.2;
	}

	/**
	 * Whether the rake strikes someone this far off (past its reach) and this many degrees off its facing (to its right is more),
	 * its paw raised on one side ({@code from}, 1 its right, -1 its left) and swept so many degrees across from it.
	 */
	public static boolean rakes(double across, double off, int from, double swept) {
		double side = from * off;
		return across <= RAKE_REACH && side <= 90 && side >= 90 - swept;
	}

	/** How far across its front the rake's paw has swept, so far into the sweep. */
	public static double swept(long into) {
		return Math.max(0, Math.min(180, 180.0 * (into + 1) / RAKE_SWEEP));
	}

	/** Whether something this far along a lane and this far to the side of it lies in it. */
	public static boolean inLane(double along, double across, double length, double width) {
		return along >= -0.5 && along <= length && Math.abs(across) <= width;
	}

	/** Whether the crush comes down on someone this far off, at this dot of its facing and the way to them. */
	public static boolean inFan(double across, double dot) {
		return across <= CRUSH_RADIUS && dot >= CRUSH_ARC;
	}

	/** Whether a blow this hard topples it, reared up as it is or not. */
	public static boolean topples(double blow, boolean reared) {
		return reared && blow >= TOPPLE_BLOW;
	}

	/** Whether someone stands within a ring this wide about a point, this far across the ground and this far above or below. */
	public static boolean inRing(double across, double rise, double radius) {
		return across <= radius && Math.abs(rise) < 2.5;
	}

	/** Where the pounce comes down, its prey this far off: on them, but no nearer than it can leap and no farther than it can reach. */
	public static double pounceLength(double distance) {
		return Math.max(POUNCE_NEAR, Math.min(POUNCE_REACH, distance));
	}

	/**
	 * Whether a rolling log, this far down its lane, bowls over someone this far down the lane, this far to the side of it and
	 * this far above the ground it rolls on: not if they jumped it.
	 */
	public static boolean bowls(double along, double across, double rise, double rolled) {
		return Math.abs(along - rolled) <= 0.7 && Math.abs(across) <= LOG_WIDTH && rise < LOG_CLEAR && rise > -1.5;
	}

	/** Whether the kicked earth catches someone this far off, at this dot of its facing and the way to them. */
	public static boolean inSpray(double across, double dot, boolean roused) {
		return across <= DIG_RANGE && dot <= (roused ? ROUSED_DIG_CONE : DIG_CONE);
	}

	/** Whether the roar at half health is due: not yet roused and worn down to half. */
	public static boolean roars(boolean roused, double health, double max) {
		return !roused && health <= max * 0.5;
	}

	/** Damage it takes: more while it lies open, a little more lying on someone, less while it roars. */
	public static float hurt(float damage, int move) {
		return damage * (float) switch (move) {
			case TOPPLED, DAZED, SPRAWLED -> 1.5;
			case PINNED -> 1.25;
			case ROAR -> 0.5;
			default -> 1;
		};
	}
}
