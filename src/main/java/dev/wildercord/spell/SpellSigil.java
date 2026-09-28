package dev.wildercord.spell;

/**
 * How a spell is written as a magic circle, so the circle can be read. From the outside in:
 * <ul>
 *   <li>a frame, a heavy ring and a fine one, with short rays outside it on the star's points;</li>
 *   <li>a band of script: the spell's own rune emblems, in order, again and again around it;</li>
 *   <li>a patterned band in the spell's first effect's ring pattern (its element at a glance);</li>
 *   <li>a star polygon with a point for every rune, and on each point a roundel: that rune's own
 *       ring pattern around its emblem, in casting order around from the top;</li>
 *   <li>an inner ring, and in the centre the first rune's emblem as the seal.</li>
 * </ul>
 * Every rune's pattern and emblem is its own (drawn by {@code tools/circle_art.py}), so anyone who
 * has learned them can read a spell off its circle. The circle's size doesn't depend on the spell:
 * a longer spell gets more star points and smaller roundels, never a bigger circle.
 *
 * <p>Radii and widths here are fractions of the circle's radius.
 */
public final class SpellSigil {
	private SpellSigil() {}

	/** A circle writes out at most this many runes (a Cord's longest spell). */
	public static final int MAX_RUNES = 16;

	/** The frame: its heavy outer ring and the fine ring just inside it. */
	public static final float FRAME = 1.0F;
	public static final float FRAME_INNER = 0.965F;
	/** How far the rays on the star's points reach outside the frame. */
	public static final float RAYS = 1.08F;
	/** The script band: its middle and its height. */
	public static final float SCRIPT = 0.895F;
	public static final float SCRIPT_HEIGHT = 0.11F;
	/** The line under the script. */
	public static final float SCRIPT_INNER = 0.835F;
	/** The patterned band. */
	public static final float PATTERN = 0.79F;
	public static final float PATTERN_HEIGHT = 0.065F;
	/** The circle the star's points (and the roundels) sit on. */
	public static final float STAR = 0.56F;
	/** The inner ring, and the ring around the seal. */
	public static final float INNER = 0.3F;
	public static final float MEDALLION = 0.2F;
	/** The seal's width. */
	public static final float SEAL = 0.3F;
	/** Line widths: the frame's heavy ring, and every other line. */
	public static final float HEAVY = 0.03F;
	public static final float FINE = 0.012F;

	/** How many points the star has for a spell of {@code runes} runes (never fewer than it has runes). */
	public static int points(int runes) {
		int n = Math.max(1, Math.min(runes, MAX_RUNES));
		// Short spells get a hexagram or an octagram with their roundels spread around it.
		return n <= 3 ? 6 : n == 4 ? 8 : n;
	}

	/** How many points along each line of the star jumps: {p/q}. Always a real star (q > 1, 2q < p). */
	public static int step(int points) {
		if (points == 8) {
			return 3;
		}
		return Math.max(2, Math.min((points - 1) / 2, Math.round(points * 0.3F)));
	}

	/** The star point rune {@code i} of {@code runes} sits on: evenly spread, the first at the top. */
	public static int pointOf(int i, int runes) {
		int p = points(runes);
		int n = Math.max(1, Math.min(runes, MAX_RUNES));
		return Math.round(i * p / (float) n) % p;
	}

	/** A roundel's radius for a spell of {@code runes} runes: as big as fits between neighbours, at most 0.15. */
	public static float roundel(int runes) {
		int p = points(runes);
		// Neighbouring points are 2·STAR·sin(π/p) apart; leave a little air between roundels.
		return (float) Math.min(0.15, STAR * Math.sin(Math.PI / p) * 0.82);
	}
}
