package dev.wildercord.aura;

/**
 * Masters and disciples, the pure part. Shared by the server ({@code aura.Lineage}), the Aura page's Lineage tab and the unit tests; the
 * server-tunable numbers are also in the config's {@code aura} section.
 *
 * <ul>
 * <li><b>Taking a disciple</b>: a swordsman of {@link #MASTER_FROM} or above holds the breathing stance, and one {@link #GAP} stages or more
 * below them (or not yet breathing at all) kneels before them (sneaking close, the two facing each other). Both are told who asks whom; held
 * for {@link #CEREMONY_TICKS} the bond is sealed. A master keeps at most {@link #MAX_DISCIPLES}; a disciple has one master.</li>
 * <li><b>What a disciple gains</b>: their master's method (taught at the ceremony to one not yet breathing; to one breathing another way the
 * master's manual, to read if they choose), aura experience {@link #NEAR_GAIN} times as fast within {@link #NEAR} blocks of their master,
 * a lesson a day (kneeling before their master again: a part of a technique the master knows), and the master's trial: <b>besting their
 * master in a spar</b>, brought to one heart, makes a waiting breakthrough into any stage below the master's own ({@link #trialCounts}).</li>
 * <li><b>What a master gains</b>: a share ({@link #SHARE}) of the road each of their disciples walks, as it's walked: paid when a disciple
 * breaks through, kept for the master if they're away.</li>
 * <li><b>The end of it</b>: either may end the bond at any time from the Aura page (asked twice), and it ends with honour when a disciple
 * reaches their master's stage.</li>
 * </ul>
 */
public final class LineageRules {
	private LineageRules() {}

	/** A master is a swordsman of Form or above. */
	public static final int MASTER_FROM = AuraRules.FORM;
	/** A disciple stands at least this many stages below their master when the bond is made. */
	public static final int GAP = 2;
	/** The most disciples a master keeps by default (the server's {@code max_disciples}). */
	public static final int MAX_DISCIPLES = 3;

	// ------------------------------------------------------------------ the ceremony

	/** How long the master's stance must have settled before a kneeling disciple is noticed (ticks). */
	public static final int SETTLE_BEFORE = 20;
	/** The ceremony, from the kneeling noticed to the seal (ticks). */
	public static final int CEREMONY_TICKS = 160;
	/** Its parts: the asking (both told who asks whom), the binding (their colours braiding), the seal. */
	public static final int ASK_END = 40;
	public static final int BIND_END = 120;
	/** A disciple kneels within this many blocks of their master, the two facing each other (each look's flat dot with the way to the other). */
	public static final double REACH = 2.75;
	public static final double FACING = 0.5;
	/** A lesson (a disciple kneeling before their master again): its length, and how long before the next (a day). */
	public static final int LESSON_TICKS = 100;
	public static final int LESSON_REST = SparRules.DAY;

	/** The ceremony's part at {@code t} ticks in: 0 the asking, 1 the binding, 2 the seal. */
	public static int phase(int t) {
		return t < ASK_END ? 0 : t < BIND_END ? 1 : 2;
	}

	/** Why a master can't take a disciple now. */
	public enum Refusal {
		/** Masters and disciples are off on this server, or aura is. */
		OFF,
		/** The one asked is below Form. */
		MASTER_STAGE,
		/** The one kneeling isn't two stages below. */
		GAP,
		/** The master already keeps as many disciples as they may. */
		FULL,
		/** The one kneeling already has a master. */
		HAS_MASTER,
		/** The one kneeling is this master's disciple already. */
		ALREADY;

		/** Its line's language key. */
		public String key() {
			return "message.wildercord.aura.lineage.refused." + name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/**
	 * Why a master at {@code masterStage} with {@code disciples} disciples (of {@code most}) can't take one at {@code discipleStage} (0 for one
	 * not yet breathing), or null when they can, checked in this order.
	 */
	public static Refusal refusal(boolean on, int masterStage, int discipleStage, int disciples, int most, boolean alreadyTheirs, boolean hasMaster) {
		if (!on) {
			return Refusal.OFF;
		}
		if (alreadyTheirs) {
			return Refusal.ALREADY;
		}
		if (masterStage < MASTER_FROM) {
			return Refusal.MASTER_STAGE;
		}
		if (discipleStage > masterStage - GAP) {
			return Refusal.GAP;
		}
		if (hasMaster) {
			return Refusal.HAS_MASTER;
		}
		if (disciples >= Math.max(1, most)) {
			return Refusal.FULL;
		}
		return null;
	}

	// ------------------------------------------------------------------ what each gains

	/** Within this many blocks of their master (in the same world), a disciple learns faster. */
	public static final double NEAR = 24.0;
	/** How much faster (aura experience; the server's {@code disciple_gain}). */
	public static final double NEAR_GAIN = 1.25;
	/** The share of each road a disciple walks that their master earns (the server's {@code master_share}). */
	public static final double SHARE = 0.25;
	/** The trial a disciple makes by besting their master in a spar ({@code api.AuraApi#allowTrial}). */
	public static final String TRIAL = "master";

	/** A disciple's experience near their master: {@code xp} times the server's gain ({@code factor}); away, as it was. */
	public static double near(double xp, boolean near, double factor) {
		return near ? xp * Math.max(1.0, factor) : xp;
	}

	/** Whether the master's trial can make a breakthrough into {@code next}: only into a stage below the master's own (to stand beside them takes a trial of the world). */
	public static boolean trialCounts(int masterStage, int next) {
		return next < masterStage;
	}

	/** What a master earns as their disciple breaks through into {@code stage}: {@code share} of the road the disciple just walked. */
	public static double share(int stage, double share) {
		if (stage <= AuraRules.GLOW || share <= 0) {
			return 0;
		}
		return (AuraRules.threshold(stage) - AuraRules.threshold(stage - 1)) * share;
	}

	/** Whether a disciple at {@code discipleStage} has reached their master at {@code masterStage}: the bond ends with honour. */
	public static boolean graduates(int discipleStage, int masterStage) {
		return discipleStage >= masterStage;
	}

	/** Whether a lesson may be given at {@code now}, the last at {@code last} (game time; {@link Long#MIN_VALUE} for none yet). */
	public static boolean lessonReady(long last, long now) {
		return last == Long.MIN_VALUE || now - last >= LESSON_REST;
	}
}
