package dev.wildercord.aura.world;

/** Geometry and timing of the keeper's physical attacks. Direction locks before damage. */
public final class SwordTombRules {
	private SwordTombRules() {}
	public static final int IDLE=0, SWEEP=1, THRUST=2, GUARD=3, RECOVER=4, BROKEN=5;
	public static final int WINDUP=36, GUARD_TICKS=44, RECOVER_TICKS=28, BROKEN_TICKS=50;
	public static final double ARENA_RADIUS=14;
	public static boolean hit(int move,double forward,double side,double vertical) {
		if(Math.abs(vertical)>2.4 || forward<0) return false;
		return move==SWEEP ? forward*forward+side*side<=25 && forward>=Math.abs(side)*.45
			: move==THRUST && forward<=7 && Math.abs(side)<=.75;
	}
	public static double damage(int move) { return move==THRUST?8:6; }
	public static double multiplier(int move,boolean front,boolean axe) {
		if(move==BROKEN || move==RECOVER) return 1.25;
		return move==GUARD && front && !axe?.35:1;
	}
	public static boolean gate(int actual,int required,boolean enabled,boolean weapon) {
		return enabled && weapon && actual>=required;
	}
}
