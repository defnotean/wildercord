package dev.wildercord.wildlife;

/** Finite wetland observation and gathering. No breeding or automatic reward loop. */
public final class WetlandRules {
	private WetlandRules() {}
	public static final int PEARL_REST=2400, BROWSE_REST=1200, RESPONSE_REST=200, REFUGE_PAUSE=120, REFUGE_REST=1200;
	public static boolean room(int nearby) {return nearby<3;}
	public static boolean night(long time) {long t=Math.floorMod(time,24000);return t>=13000 && t<23000;}
	public static boolean ready(long now,long ready) {return now>=ready;}
	/** A stalking anchor goes stale once the quarry swims out of the one-cell ring its bank candidates surround. */
	public static boolean staleAnchor(int ox,int oz,int qx,int qz) {return Math.max(Math.abs(qx-ox),Math.abs(qz-oz))>1;}
	public static float glow(boolean night,boolean wet,boolean responding) {return responding?1:night && wet?.85F:wet?.35F:.12F;}
}
