package dev.wildercord.wildlife;

/**
 * The brookbill duck's numbers and decisions, kept pure for its tests: the single-file line its ducklings paddle in, who a
 * duckling imprints on, when it up-ends to dabble, and when it shakes itself dry or flaps in the rain.
 */
public final class DuckRules {
	private DuckRules() {}

	/** How far a duckling looks for its mother, and the gap it keeps behind whoever is ahead of it in the line. */
	public static final double MOTHER_RANGE = 16;
	public static final double LINE_GAP = 0.9;
	/** Further back than this and it hurries to catch up. */
	public static final double LINE_HURRY = 3.0;
	/** A player this near when a duckling hatches from breeding is the first face it sees, and it imprints on them. */
	public static final double IMPRINT_RANGE = 5.0;
	/** Up-ended, dabbling: how long, how long between, and what a mouthful heals. */
	public static final int DABBLE_TICKS = 40, DABBLE_COOLDOWN = 240;
	public static final float DABBLE_HEAL = 1;
	/** A shake or a happy flap of the wings lasts this long; in the rain, now and then. */
	public static final int FLAP_TICKS = 16;
	public static final double RAIN_FLAP_CHANCE = 0.01;

	/** Where a duckling sits in its line, counting from the front, among its brood's ids sorted low to high. */
	public static int place(int id, int[] sortedIds) {
		for (int i = 0; i < sortedIds.length; i++) {
			if (sortedIds[i] == id) {
				return i;
			}
		}
		return sortedIds.length;
	}

	/** How fast it paddles after whoever is ahead of it: not at all once close, quickly once fallen behind. */
	public static double pace(double distance) {
		if (distance <= LINE_GAP) {
			return 0;
		}
		return distance > LINE_HURRY ? 1.4 : 1.0;
	}

	/** Whether a duckling imprints on a player: only a duckling, only on seeds, and only one not already imprinted. */
	public static boolean imprints(boolean baby, boolean seeds, boolean imprinted) {
		return baby && seeds && !imprinted;
	}

	/** Whether it up-ends to dabble: grown, afloat, idle, and its last dabble long enough ago. */
	public static boolean dabbles(boolean grown, boolean afloat, boolean idle, long sinceLast) {
		return grown && afloat && idle && (sinceLast < 0 || sinceLast >= DABBLE_COOLDOWN);
	}

	/** Whether it shakes itself off: just climbed out of the water onto land. */
	public static boolean shakes(boolean wasAfloat, boolean afloat, boolean grounded) {
		return wasAfloat && !afloat && grounded;
	}

	/** Whether it flaps happily in the rain, from a 0 to 1 roll: out on land in the rain, now and then. */
	public static boolean rejoices(boolean raining, boolean grounded, double roll) {
		return raining && grounded && roll < RAIN_FLAP_CHANCE;
	}
}
