package dev.wildercord.aura.world;

/**
 * Crimson signature: the master pays blood to open a four-beat frenzy (low, high, lane, ring) and heals only on the
 * beats that land. A clean read of all four leaves it bled, healed by nothing and open for longer.
 */
public final class CrimsonFrenzyRules {
	private CrimsonFrenzyRules() {}

	public static final int[] BEATS = {13, 25, 37, 49};
	public static final MasterTechniques.Shape[] SHAPES = {MasterTechniques.Shape.ARC_LOW, MasterTechniques.Shape.ARC_HIGH,
		MasterTechniques.Shape.LANE, MasterTechniques.Shape.CIRCLE};
	public static final double[] REACH = {4, 4.5, 5, 3.5};
	public static final int TELL = 49, RECOVERY = 16, MISS_RECOVERY = 10, LOCK = 6, COOLDOWN = 200, WARNING_REFRESH = 3;
	public static final double COST = 32, DAMAGE = 15, LANE_WIDTH = .8, PRICE = .03, HEAL = .05, MAX_DISTANCE = 4, MAX_HEIGHT = 1.5;

	/** The price must never be the blow that ends the master's own trial. */
	public static boolean eligible(int school, int sequence, double distance, double height, double aura, double health, double maxHealth,
		long now, long readyAt) {
		return school == MastersPackB.CRIMSON && Math.floorMod(sequence, 4) == 3 && now >= readyAt
			&& Double.isFinite(aura) && aura >= COST + MastersRules.GUARD_COST && aura <= MastersRules.AURA_MAX
			&& Double.isFinite(distance) && distance <= MAX_DISTANCE && Double.isFinite(height) && Math.abs(height) <= MAX_HEIGHT
			&& Double.isFinite(health) && Double.isFinite(maxHealth) && maxHealth > 0 && health > price(maxHealth) * 2;
	}

	public static double price(double maxHealth) { return Double.isFinite(maxHealth) && maxHealth > 0 ? maxHealth * PRICE : 0; }
	public static double heal(double maxHealth) { return Double.isFinite(maxHealth) && maxHealth > 0 ? maxHealth * HEAL : 0; }

	/** Every beat dodged leaves the master open longer; a single landed beat removes the reward. */
	public static int recovery(int landed) { return RECOVERY + (landed == 0 ? MISS_RECOVERY : 0); }

	/** Jump the low cut, duck the high one, sidestep the lane, get clear of the ring. */
	public static boolean hits(int beat, double forward, double side, double height, boolean crouching) {
		if (beat < 0 || beat >= BEATS.length || !Double.isFinite(forward) || !Double.isFinite(side) || !Double.isFinite(height)
			|| Math.abs(height) > 2.5) return false;
		double reach = REACH[beat];
		return switch (SHAPES[beat]) {
			case CIRCLE -> forward * forward + side * side <= reach * reach;
			case LANE -> forward >= 0 && forward <= reach && Math.abs(side) <= LANE_WIDTH;
			case ARC, ARC_LOW, ARC_HIGH -> forward >= 0 && forward <= reach && Math.abs(side) <= Math.min(3.0, .8 + forward * .8)
				&& (SHAPES[beat] != MasterTechniques.Shape.ARC_LOW || height <= .6)
				&& (SHAPES[beat] != MasterTechniques.Shape.ARC_HIGH || !crouching);
		};
	}
}
