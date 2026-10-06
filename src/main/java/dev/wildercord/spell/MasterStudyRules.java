package dev.wildercord.spell;

/** Permanent lesson names and the small, ordered Archive reading contract. */
public final class MasterStudyRules {
	private MasterStudyRules() {}

	public static final String RELAY = "study:relay";
	/** Verified Archive access, distinct from a portable book copy and the learned shape. */
	public static final String RELAY_COPIED = "study:relay_copied";
	public static final String RELAY_PRACTICE = "practice:relay";
	public static final String RELAY_COPY = "study:relay_copy";
	public static final String RELAY_REPLACEMENT = "study:relay_replacement";
	public static final int PAGES = 3;
	public static final int READING_TICKS = 20 * 60 * 5;
	public static final double REACH = 5.5;

	public static boolean eligibleRelay(int activeCircles, boolean archivist) {
		return activeCircles >= Circles.ARCHMAGE && archivist;
	}

	public static boolean mayStudyRelay(int activeCircles, boolean archivist, boolean copied) {
		return copied && eligibleRelay(activeCircles, archivist);
	}

	/** Only the next page, followed by the third page's learn action, can advance a live reading. */
	public static boolean nextPage(int current, int requested) {
		return current >= 0 && current < PAGES && requested == current + 1;
	}
}
