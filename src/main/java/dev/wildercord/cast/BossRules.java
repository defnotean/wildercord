package dev.wildercord.cast;

/**
 * The dungeon bosses' pure rules, with no Minecraft types so they're unit-tested: which phase a
 * boss's health puts it in, and how far one blow may take it (never past the start of its next
 * phase, so no burst of damage skips a phase or kills it from its first).
 */
public final class BossRules {
	private BossRules() {}

	/** Ticks a stranded Tide Scribe can't be stranded again after it breaks free. */
	public static final int STRAND_IMMUNE_TICKS = 100;

	/** The phase (1 to 3) a boss is in at {@code health} of {@code max}: the second at two thirds, the third at one third. */
	public static int phaseFor(float health, float max) {
		return health > max * 2 / 3 ? 1 : health > max / 3 ? 2 : 3;
	}

	/** The health a boss in {@code phase} can't be taken below by one blow: where its next phase starts (0 in the last). */
	public static float floor(int phase, float max) {
		return phase <= 1 ? max * 2 / 3 : phase == 2 ? max / 3 : 0;
	}

	/**
	 * Its health after a blow took it from {@code before} to {@code after}, in {@code phase}: held at
	 * the start of the next phase if the blow would carry it past (or kill it before its last phase).
	 */
	public static float capped(int phase, float max, float before, float after) {
		float floor = floor(phase, max);
		return floor > 0 && before > floor && after < floor ? floor : after;
	}
}
