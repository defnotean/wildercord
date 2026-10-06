package dev.wildercord.spell;

/** Permanent lesson names and their small, ordered reading contracts. */
public final class MasterStudyRules {
	private MasterStudyRules() {}

	public static final String RELAY = "study:relay";
	/** Verified Archive access, distinct from a portable book copy and the learned shape. */
	public static final String RELAY_COPIED = "study:relay_copied";
	public static final String RELAY_PRACTICE = "practice:relay";
	public static final String RELAY_COPY = "study:relay_copy";
	public static final String RELAY_REPLACEMENT = "study:relay_replacement";
	public static final String REWEAVE = "study:reweave";
	/** Verified Low Tide recovery; neither this receipt nor a book alone teaches the shape. */
	public static final String REWEAVE_COPIED = "study:reweave_copied";
	public static final String REWEAVE_COPY = "study:reweave_copy";
	public static final String REWEAVE_REPLACEMENT = "study:reweave_replacement";
	public static final String REWEAVE_PRACTICE = "practice:reweave";
	public static final int REWEAVE_CIRCLE = 12;
    public static final String EXCISE = "study:excise", EXCISE_COPIED = "study:excise_copied",
        EXCISE_COPY = "study:excise_copy", EXCISE_REPLACEMENT = "study:excise_replacement", EXCISE_PRACTICE = "practice:excise";
    public static final int EXCISE_CIRCLE = 16;
    public static boolean eligibleExcise(int activeCircles, boolean heartwood) { return activeCircles >= EXCISE_CIRCLE && heartwood; }
    public static boolean hasExciseLesson(boolean heartwood, boolean copied, boolean learned) { return heartwood || copied || learned; }
    public static boolean mayStudyExcise(int activeCircles, boolean heartwood, boolean copied) { return copied && eligibleExcise(activeCircles, heartwood); }
	public static final int PAGES = 3;
	public static final int READING_TICKS = 20 * 60 * 5;
	public static final double REACH = 5.5;

	public static boolean eligibleRelay(int activeCircles, boolean archivist) {
		return activeCircles >= Circles.ARCHMAGE && archivist;
	}

	public static boolean mayStudyRelay(int activeCircles, boolean archivist, boolean copied) {
		return copied && eligibleRelay(activeCircles, archivist);
	}

	public static boolean eligibleReweave(int activeCircles, boolean lowTide) {
		return activeCircles >= REWEAVE_CIRCLE && lowTide;
	}

	/** A victory recorded before the lesson was added is already enough to recover its pages. */
	public static boolean hasReweaveLesson(boolean lowTide, boolean copied, boolean learned) {
		return lowTide || copied || learned;
	}

	public static boolean mayStudyReweave(int activeCircles, boolean lowTide, boolean copied) {
		return copied && eligibleReweave(activeCircles, lowTide);
	}

	public static boolean liveReading(long elapsed) {
		return elapsed >= 0 && elapsed < READING_TICKS;
	}

	/** Only the next page, followed by the third page's learn action, can advance a live reading. */
	public static boolean nextPage(int current, int requested) {
		return current >= 0 && current < PAGES && requested == current + 1;
	}
}
