package dev.wildercord.aura;

/** The original Unmoved/Null caught and nearest geometry; struck deliberately has no range or LOS predicate. */
public final class OriginalCounterRules {
	private OriginalCounterRules() {}
	public static boolean caught(double distanceSquared) { return distanceSquared <= 36; }
	public static boolean nearest(double x, double y, double z, double lookX, double lookZ, double width) {
		return Math.abs(y) <= 2.2 && ArtRules.inCone(x, z, lookX, lookZ, 4 + width / 2, 120);
	}
}
