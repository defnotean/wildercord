package dev.wildercord.spell;

/**
 * The numbers of the wayfarer's runes (the fx-explore pack: readings, bearings, structure senses, trade,
 * brewing and decoration helpers, Nether and End footing), kept apart from the world so they can be tested.
 * Their behaviour is in {@code dev.wildercord.cast.WayfarerEffects}.
 */
public final class WayfarerRules {
	private WayfarerRules() {}

	/** The rest (ticks) every structure sense shares after one finds something or comes up empty. */
	public static final int LOCATE_REST = 600;
	/** How far a structure sense searches (in the structure's own placement cells, as /locate counts), and its cap. */
	public static final int LOCATE_CELLS = 12, LOCATE_CELLS_MAX = 24;
	/** The compass, clockwise from north, as the direction.wildercord.* keys name it. */
	public static final String[] COMPASS = {"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"};
	/** Area runes: their base reach and the cap Radius can't pass. */
	public static final double AREA = 6, AREA_MAX = 10;
	/** Blocks one area rune may change at most (also bounded by the cast's block budget). */
	public static final int MAX_EDITS = 24;
	/** Dye Wash and Checker Dye: blocks one dye covers, and blocks changed at most. */
	public static final int BLOCKS_PER_DYE = 8, MAX_DYED = 16;
	/** Potion Steep: the most it adds to one effect (ticks), and the length it never steeps past. */
	public static final int STEEP_ADD_MAX = 900, STEEP_CAP = 9600;
	/** Lava Crust: base half-width and its cap. */
	public static final int CRUST = 2, CRUST_MAX = 4;
	/** Steady Brush: blocks at once, strokes each, and ticks between strokes (vanilla's own brush rhythm). */
	public static final int BRUSH_BLOCKS = 3, BRUSH_STROKES = 10, BRUSH_EVERY = 10;
	/** The armor stand poses Stand Pose steps through. */
	public static final int POSES = 4;

	/** A structure sense's search, grown by Radius and capped. */
	public static int locateCells(double radius) {
		return (int) Math.max(1, Math.min(LOCATE_CELLS_MAX, Math.round(LOCATE_CELLS * radius)));
	}

	/** Whether a structure sense is resting: it last searched at {@code last} (or never, {@link Long#MIN_VALUE}). */
	public static boolean resting(long last, long now) {
		return last != Long.MIN_VALUE && now - last < LOCATE_REST && now >= last;
	}

	/** Which way {@code dx, dz} points, as an index into {@link #COMPASS} (north is -z). */
	public static int compass(double dx, double dz) {
		double angle = Math.toDegrees(Math.atan2(dx, -dz));
		return Math.floorMod((int) Math.round(angle / 45.0), 8);
	}

	/** A distance told roughly: to the nearest 10 under 100 blocks, else to the nearest 50; never 0. */
	public static long rough(double blocks) {
		if (blocks < 100) {
			return Math.max(10, Math.round(blocks / 10.0) * 10);
		}
		return Math.round(blocks / 50.0) * 50;
	}

	/** Nights until the next full moon from moon phase {@code phase} (0 is full). */
	public static int nightsToFull(int phase) {
		return Math.floorMod(8 - phase, 8);
	}

	/** Whether the sun is up at {@code clock} (in ticks of the day). */
	public static boolean day(long clock) {
		return Math.floorMod(clock, 24000L) < 12000L;
	}

	/** Ticks until dusk (by day) or dawn (by night) at {@code clock}. */
	public static long untilTurn(long clock) {
		long t = Math.floorMod(clock, 24000L);
		return t < 12000L ? 12000L - t : 24000L - t;
	}

	/** Ticks told as whole real minutes, never fewer than one. */
	public static long minutes(long ticks) {
		return Math.max(1, Math.round(ticks / 1200.0));
	}

	/** The clock time told as an hour and minute, 6:00 at sunrise (tick 0), as a 24-hour "hh:mm". */
	public static String hour(long clock) {
		long t = Math.floorMod(clock + 6000L, 24000L);
		long h = t / 1000L, m = (t % 1000L) * 60L / 1000L;
		return String.format(java.util.Locale.ROOT, "%02d:%02d", h, m);
	}

	/** Where an Overworld coordinate falls in the Nether, and back. */
	public static int toNether(double c) {
		return (int) Math.floor(c / 8.0);
	}

	public static long toOverworld(double c) {
		return (long) Math.floor(c * 8.0);
	}

	/** Dye for {@code blocks} recoloured blocks. */
	public static int dyesFor(int blocks) {
		return blocks <= 0 ? 0 : (blocks + BLOCKS_PER_DYE - 1) / BLOCKS_PER_DYE;
	}

	/** Blocks {@code dyes} can recolour, capped. */
	public static int dyeable(int dyes) {
		return Math.max(0, Math.min(MAX_DYED, dyes * BLOCKS_PER_DYE));
	}

	/** Checker Dye's squares: the blocks whose coordinates add up even. */
	public static boolean checker(int x, int y, int z) {
		return Math.floorMod(x + y + z, 2) == 0;
	}

	/** A good effect with {@code remaining} ticks left, steeped: a quarter longer, at most 45 seconds more, never past 8 minutes. */
	public static int steeped(int remaining) {
		if (remaining <= 0 || remaining >= STEEP_CAP) {
			return remaining;
		}
		return Math.min(STEEP_CAP, remaining + Math.min(STEEP_ADD_MAX, remaining / 4));
	}

	/** A beacon's own reach (vanilla: 10 a level plus 10) and its reach while swelled. */
	public static int beaconReach(int levels) {
		return levels <= 0 ? 0 : levels * 10 + 10;
	}

	public static double swelledReach(int levels) {
		return beaconReach(levels) * 1.5;
	}

	/** The day number of the clock: a villager restocks by spell once in it. */
	public static long dayOf(long clock) {
		return Math.floorDiv(clock, 24000L);
	}

	/** The tag a villager wears for the day a spell restocked it. */
	public static String restockTag(long day) {
		return "wildercord.restocked." + day;
	}

	/** The pose after {@code current} (-1 for none of them). */
	public static int nextPose(int current) {
		return Math.floorMod(current + 1, POSES);
	}

	/** An area rune's reach: its base grown by Radius, capped. */
	public static double area(double base, double radius, double cap) {
		return Math.max(1, Math.min(cap, base * radius));
	}

	/** Lava Crust's half-width. */
	public static int crust(double radius) {
		return (int) Math.max(1, Math.min(CRUST_MAX, Math.round(CRUST * radius)));
	}

	/** Void Step: when the block {@code ring} steps from the centre goes (the edge first), for a platform lasting {@code ticks}. */
	public static int crumble(int ring, int ticks) {
		return ring == 0 ? ticks * 3 / 2 : ticks;
	}

	/** A shelf count told against the full 15. */
	public static int shelves(int counted) {
		return Math.max(0, Math.min(15, counted));
	}

	/** Whether an anvil refuses to work it: any job adds at least a level to the repair cost, and vanilla refuses 40 or more. */
	public static boolean tooCostly(int repairCost) {
		return repairCost >= 39;
	}
}
