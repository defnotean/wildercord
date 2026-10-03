package dev.wildercord.wildlife;

/** Distinct highland rhythms; meals suppress hunting, never defensive retaliation. */
public final class HighlandRules {
	private HighlandRules() {}
	public static final int MEAL_REST=1200;
	public static boolean wantsCover(boolean predator,long day,boolean rain) {
		long time=Math.floorMod(day,24000);
		return rain || (predator?time>=2000 && time<10000:time>=13000 && time<23000);
	}
	public static boolean hungry(long now,long fedUntil) {return now>=fedUntil;}
	public static long meal(long now) {return now<0 || now>Long.MAX_VALUE-MEAL_REST?Long.MAX_VALUE:now+MEAL_REST;}
}
