package dev.wildercord.aura.world;

import dev.wildercord.cast.MoveDeck;

/**
 * The buried keeper's blows: where each reaches, how long it is shown before it falls, and what each asks of whoever fights it.
 * It opens with the old drill it was taught, sweep then thrust then guard; after that it chooses, and never the same blow twice
 * running. The sweep wants you off its front and the thrust off its line; the guard wants an axe or its flank. Get behind it and
 * the blade comes round low at your heels, to be jumped. Stood still and listening, it leaps on any footfall it hears, so stand
 * still or creep; kneeling, it calls the buried hands up beneath whoever stands still, so keep moving; and it charges with the
 * blade held crosswise, to be stepped aside from or led into a wall. Half broken, it kneels for its rite, throws back everyone
 * near, and fights on roused: quicker, its blows in pairs, and more hands. Direction always locks before damage.
 */
public final class SwordTombRules {
	private SwordTombRules() {}
	public static final int IDLE=0, SWEEP=1, THRUST=2, GUARD=3, RECOVER=4, BROKEN=5;
	/** Head cocked, the blade point lowered, a hand raised to the blindfold: listening for a footfall. */
	public static final int LISTEN=6;
	/** A leap at what it heard, the blade raised two-handed overhead, and down on the spot; then the blade stuck fast. */
	public static final int VERDICT=7;
	/** Kneeling with the blade plunged in the ground, calling the buried hands up beneath its prey, wave after wave. */
	public static final int HANDS=8;
	/** The blade trailed low behind it, then brought round in one full turn at ankle height. */
	public static final int HEEL=9;
	/** The blade held crosswise before it and a charge across the arena. */
	public static final int RAMPART=10;
	/** Charged into a wall and stuck there, shaking. */
	public static final int WALLED=11;
	/** Half broken: kneeling, head bowed over the pommel, then rising and throwing everyone back. Once. */
	public static final int RITE=12;
	/** Listened and heard nothing; at a loss, its guard down. */
	public static final int LOST=13;
	public static final int[] PICKS={SWEEP,THRUST,GUARD,HEEL,LISTEN,HANDS,RAMPART};
	/** The old drill it opens with, in order. */
	public static final int[] DRILL={SWEEP,THRUST,GUARD};
	public static final int MEMORY=5;
	public static final int WINDUP=36, ROUSED_WINDUP=24, CHAIN_WINDUP=16, GUARD_TICKS=44, RECOVER_TICKS=28, BROKEN_TICKS=50;
	public static final int REST=14, ROUSED_REST=6;
	public static final int HEEL_TELL=14, HEEL_SPIN=6, HEEL_RECOVER=24;
	public static final int LISTEN_TELL=16, LISTEN_TICKS=36, LISTEN_GRACE=4, LOST_TICKS=44;
	public static final int VERDICT_CROUCH=8, VERDICT_LEAP=10, VERDICT_STUCK=30;
	public static final int HANDS_TELL=14, HANDS_WAVES=3, HANDS_EVERY=14, HANDS_RISE=18;
	public static final int RAMPART_TELL=18, RAMPART_LOCK=5, RAMPART_TICKS=18, RAMPART_RECOVER=20, WALLED_TICKS=50;
	public static final int RITE_TICKS=36;
	public static final double ARENA_RADIUS=14;
	public static final double HEEL_RADIUS=4.2, HEEL_ANKLE=.6, VERDICT_RADIUS=2.2, GRASP_RADIUS=1.4, RAMPART_SPEED=.7, RAMPART_WIDTH=1.3, RITE_RADIUS=6;
	/** How far a footfall must carry in a tick to be heard, and how high a hop. A creeping step is never heard. */
	public static final double LOUD=.1, LEAP=.2;
	public static boolean hit(int move,double forward,double side,double vertical) {
		if(Math.abs(vertical)>2.4 || forward<0) return false;
		return move==SWEEP ? forward*forward+side*side<=25 && forward>=Math.abs(side)*.45
			: move==THRUST && forward<=7 && Math.abs(side)<=.75;
	}
	public static double damage(int move) {
		return switch(move){case THRUST->8;case VERDICT->9;case RAMPART->7;case RITE->4;default->6;};
	}
	public static double multiplier(int move,boolean front,boolean axe) {
		if(move==LOST || move==WALLED) return 1.5;
		if(move==BROKEN || move==RECOVER || move==HANDS || move==VERDICT) return 1.25;
		if(move==RITE) return .5;
		return move==GUARD && front && !axe?.35:1;
	}
	/** What each blow is worth to it now, by how far off its prey is and whether they have got behind it. */
	public static double[] weights(double distance,boolean behind) {
		var w=new double[PICKS.length];
		for(int i=0;i<PICKS.length;i++) w[i]=switch(PICKS[i]){
			case SWEEP->!behind && distance<=5?3:0;
			case THRUST->!behind && distance>=2 && distance<=7?2.5:0;
			case GUARD->!behind && distance<=6?1:0;
			case HEEL->distance<=HEEL_RADIUS?behind?4:1:0;
			case LISTEN->distance<=14?1.2:0;
			case HANDS->distance>=4?distance>7?3:1.2:0;
			case RAMPART->distance>=5?distance>7?2.5:1.5:0;
			default->0;
		};
		return w;
	}
	public static int choose(MoveDeck deck,double distance,boolean behind,double roll) { return deck.pick(PICKS,weights(distance,behind),roll); }
	/** Roused, the blow it runs straight into after this one, or -1: the thrust after the sweep, the heel-turn after the leap. */
	public static int chain(int move,double distance) {
		return move==SWEEP && distance<=7?THRUST : move==VERDICT && distance<=HEEL_RADIUS?HEEL : -1;
	}
	/** Whether it hears someone who moved this far across the ground and rose this far in a tick. */
	public static boolean heard(double flat,double rise,boolean sneaking) { return rise>LEAP || !sneaking && flat>LOUD; }
	/** Whether the heel-turn catches someone this far off: within its reach and still on the ground. */
	public static boolean heel(double dx,double dz,double dy) { return dx*dx+dz*dz<=HEEL_RADIUS*HEEL_RADIUS && dy<HEEL_ANKLE && dy>-1.5; }
	/**
	 * Whether the blade, starting straight behind it and come round {@code swept} degrees, has passed someone at {@code bearing}
	 * degrees from where it faces.
	 */
	public static boolean passed(double bearing,double swept) { return ((bearing-180)%360+360)%360<=swept; }
	/** Whether the charge runs into someone this far ahead of it, to one side and above. */
	public static boolean rampart(double forward,double side,double vertical) { return forward>=-.5 && forward<=2 && Math.abs(side)<=RAMPART_WIDTH && Math.abs(vertical)<2.4; }
	public static boolean gate(int actual,int required,boolean enabled,boolean weapon) {
		return enabled && weapon && actual>=required;
	}
}
