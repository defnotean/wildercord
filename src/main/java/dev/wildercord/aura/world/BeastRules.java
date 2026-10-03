package dev.wildercord.aura.world;

/** Readable attacks and bounded highland populations. These animals resist spells, not skilled blades. */
public final class BeastRules {
	private BeastRules() {}
	public static final int IDLE=0, WARN=1, CHARGE=2, LEAP=3, RECOVER=4, FORAGE=5, REST=6;
	public static final int WARNING=40, RECOVERY=50, SHED_INTERVAL=2400;
	public static boolean room(int nearby) { return nearby < 2; }
	public static boolean habitat(boolean enabled, boolean peaceful, boolean ground, boolean sky, int y, int sea, int nearby) {
		return enabled && !peaceful && ground && sky && y>=sea+12 && room(nearby);
	}
	public static double spell(boolean gale, boolean recovery) { return recovery ? (gale?.65:.4) : (gale?.2:.12); }
	public static boolean chargeHit(double forward,double side,double height) { return forward>=-.6 && forward<=2.4 && Math.abs(side)<1.25 && Math.abs(height)<1.8; }
	public static boolean leapHit(double distanceSquared,double height) { return distanceSquared<=2.25*2.25 && Math.abs(height)<2; }
	public static boolean wary(double distance,boolean sneaking,boolean food) { return distance<4 && !sneaking && !food; }
}
