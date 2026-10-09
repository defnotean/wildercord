package dev.wildercord.aura.world;

/**
 * The geometry of the Echo, Dawn and Venom Masters' signatures. Each has one answer, shown before it lands:
 * <ul>
 * <li>Tolling Bell (Echo): a long lane rung twice. The second toll comes back down the first lane even if the Master has
 * turned. Answer: leave the lane.</li>
 * <li>Noon Glare (Dawn): light that burns whoever is looking at the Master, wherever they stand. Answer: turn away.</li>
 * <li>Serpent Coil (Venom): a ring round the Master with one gap at its side. Jumping does not clear it. Answer: leave
 * through the gap, or get outside the ring.</li>
 * </ul>
 * Every input is relative to the Master: {@code forward} along its locked aim, {@code side} to its left (+) or right (-),
 * {@code height} above its feet.
 */
public final class MethodsBSignatureRules {
	private MethodsBSignatureRules() {}

	public enum Signature { TOLL, GLARE, COIL }

	/** Tolling Bell: the lane's length and half width. Longer and a little wider than a plain thrust. */
	public static final double TOLL_REACH = 9.0, TOLL_HALF_WIDTH = 1.1;
	/** Nausea from a toll (ticks). */
	public static final int TOLL_REEL = 50;

	/** Noon Glare: its range, and how squarely a player must be looking at the Master to be caught (the dot of look and line). */
	public static final double GLARE_RANGE = 12.0, GLARE_FACING = 0.5;
	/** Blindness from the glare (ticks). */
	public static final int GLARE_BLIND = 40;

	/** Serpent Coil: the ring's radius and its gap's full width (degrees). */
	public static final double COIL_RADIUS = 5.5, COIL_GAP_DEGREES = 70;
	/** Poison from the coil (ticks). */
	public static final int COIL_POISON = 80;

	/** How high a signature reaches: no jump clears these, and only a long fall or a high ledge stays above them. */
	public static final double LOW = -1.5, HIGH = 2.5;

	private static boolean level(double height) {
		return height > LOW && height < HIGH;
	}

	public static boolean tollHits(double forward, double side, double height) {
		return level(height) && forward >= -0.5 && forward <= TOLL_REACH && Math.abs(side) <= TOLL_HALF_WIDTH;
	}

	/**
	 * @param facing the dot of the player's look with the unit line from the player's eyes to the Master's: 1 looking straight at
	 *               it, 0 side-on, -1 with their back to it
	 */
	public static boolean glareHits(double distance, double facing) {
		return Double.isFinite(distance) && Double.isFinite(facing) && distance <= GLARE_RANGE && facing > GLARE_FACING;
	}

	/** Where the coil's gap opens, in degrees from the aim toward the left (+90) or the right (-90). It alternates by cast. */
	public static double gapDegrees(long began) {
		return Math.floorMod(began, 2) == 0 ? 90 : -90;
	}

	public static boolean coilHits(double forward, double side, double height, double gapDegrees) {
		if (!level(height)) return false;
		double r = Math.hypot(forward, side);
		if (!(r <= COIL_RADIUS)) return false;
		if (r < 0.05) return true;
		double angle = Math.toDegrees(Math.atan2(side, forward));
		return Math.abs(wrap(angle - gapDegrees)) > COIL_GAP_DEGREES / 2;
	}

	/** An angle in degrees brought into -180..180. */
	static double wrap(double degrees) {
		double d = degrees % 360;
		if (d > 180) d -= 360;
		if (d < -180) d += 360;
		return d;
	}

	/** Whether the signature catches a player at this place, looking this squarely at the Master. */
	public static boolean hits(Signature signature, double forward, double side, double height, double facing, double gapDegrees) {
		return switch (signature) {
			case TOLL -> tollHits(forward, side, height);
			case GLARE -> glareHits(Math.sqrt(forward * forward + side * side + height * height), facing);
			case COIL -> coilHits(forward, side, height, gapDegrees);
		};
	}
}
