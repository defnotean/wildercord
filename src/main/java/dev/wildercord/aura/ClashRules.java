package dev.wildercord.aura;

/**
 * The clash, the pure part: when two strikes meet (two crescents in the air, an art loosed into an oncoming crescent, or an art answering
 * an art in the same breath), they lock for a moment into a struggle won on timing. Shared by the server ({@code aura.Clashes}), each
 * client's rhythm on the HUD and the unit tests.
 *
 * <p><b>The rhythm.</b> The lock takes hold for {@link #LOCK} ticks, then {@link #BEATS} beats come {@link #GAP} ticks apart, each one shown
 * to both sides as a ring closing on its mark. Each side presses (a swing of the blade) on each beat: within {@link #PERFECT} ticks of it is
 * perfect (two points), within {@link #GOOD} is good (one), and a beat with no press in its window is missed (none). A press that falls in
 * no window, or a second press in a window already answered, is a <b>fumble</b> and takes a point away, so hammering the button loses. The
 * higher score wins and its strike carries on; equal scores break both, as two crescents always used to. The whole struggle is
 * {@link #length()} ticks (under two seconds), and neither side is ever held: they can move, guard and step all the while.</p>
 *
 * <p><b>The Blade's edge.</b> A swordsman walking the Way of the Blade whose slash meets another's has an edge in the struggle rather than an
 * automatic win: each of its windows is {@link #EDGE} tick wider, and it wins an equal score.</p>
 *
 * <p><b>A swordsman of the world</b> (a duelist, a fallen knight) presses on a timing of its own, drawn once as the lock takes hold: each
 * beat it misses with a chance by its stage ({@link #npcMiss}) and otherwise presses off the beat by a normal spread ({@link #npcSpread}),
 * tighter at the higher stages; its presses show on the HUD as they land, and its blade flashes as it presses, so a player can read it.
 * A good player beats any of them more often than not.</p>
 */
public final class ClashRules {
	private ClashRules() {}

	// ------------------------------------------------------------------ the rhythm

	/** Ticks from the lock to the first beat: the strikes bind, the rings appear and the first one starts closing. */
	public static final int LOCK = 10;
	/** How many beats the struggle runs. */
	public static final int BEATS = 3;
	/** Ticks between beats (a steady pulse, a little under half a second). */
	public static final int GAP = 9;
	/** Within this many ticks of a beat, either side, a press is perfect. */
	public static final int PERFECT = 2;
	/** Within this many, good. */
	public static final int GOOD = 4;
	/** How much wider each window is for a side with the Blade's edge. */
	public static final int EDGE = 1;
	/** Ticks after the last window closes before the result (the last presses' word reaching the server). */
	public static final int SETTLE = 4;

	/** A grade: what a beat (or a stray press) was worth. */
	public enum Grade {
		/** A beat with no press in its window. */
		MISS(0),
		GOOD(1),
		PERFECT(2),
		/** A press in no window, or a second in a window already answered. */
		FUMBLE(-1);

		public final int points;

		Grade(int points) {
			this.points = points;
		}

		public static Grade of(int ordinal) {
			Grade[] all = values();
			return ordinal >= 0 && ordinal < all.length ? all[ordinal] : MISS;
		}
	}

	/** When beat {@code k} (0 the first) falls, in ticks from the lock. */
	public static int beat(int k) {
		return LOCK + Math.max(0, k) * GAP;
	}

	/** How wide a side's good window is, either side of its beat. */
	public static int window(boolean edge) {
		return GOOD + (edge ? EDGE : 0);
	}

	/** When the last window closes (a side with the edge), in ticks from the lock. */
	public static int lastClose() {
		return beat(BEATS - 1) + GOOD + EDGE;
	}

	/** The whole struggle, lock to result, in ticks. */
	public static int length() {
		return lastClose() + SETTLE;
	}

	/** The beat nearest {@code t} (ticks from the lock), whether or not {@code t} is in its window. */
	public static int nearest(int t) {
		int k = (int) Math.round((t - LOCK) / (double) GAP);
		return Math.max(0, Math.min(BEATS - 1, k));
	}

	/** The beat a press at {@code t} answers (the nearest, if {@code t} is in its window), or -1 for none (a fumble). */
	public static int beatFor(int t, boolean edge) {
		int k = nearest(t);
		return Math.abs(t - beat(k)) <= window(edge) ? k : -1;
	}

	/** What a press {@code offset} ticks off its beat is worth (a fumble outside every window). */
	public static Grade judge(int offset, boolean edge) {
		int off = Math.abs(offset);
		int e = edge ? EDGE : 0;
		if (off <= PERFECT + e) {
			return Grade.PERFECT;
		}
		return off <= GOOD + e ? Grade.GOOD : Grade.FUMBLE;
	}

	/** Whether beat {@code k}'s window has closed by {@code t} for a side (a beat still unanswered then is missed). */
	public static boolean closed(int k, int t, boolean edge) {
		return t > beat(k) + window(edge);
	}

	/** One side's tally: a grade a beat (null while unanswered) and the fumbles beside them. */
	public static final class Tally {
		private final Grade[] beats = new Grade[BEATS];
		private int fumbles;

		/**
		 * A press at {@code t} (ticks from the lock, after any allowance for the network): the grade it earned, which is a fumble if it falls in
		 * no window or its beat is already answered.
		 */
		public Grade press(int t, boolean edge) {
			int k = beatFor(t, edge);
			if (k < 0 || beats[k] != null) {
				fumbles++;
				return Grade.FUMBLE;
			}
			Grade grade = judge(t - ClashRules.beat(k), edge);
			beats[k] = grade;
			return grade;
		}

		/** Marks every beat whose window has closed by {@code t} and went unanswered as missed; returns the beats newly missed (a bit each). */
		public int close(int t, boolean edge) {
			int missed = 0;
			for (int k = 0; k < BEATS; k++) {
				if (beats[k] == null && closed(k, t, edge)) {
					beats[k] = Grade.MISS;
					missed |= 1 << k;
				}
			}
			return missed;
		}

		/** The grade of beat {@code k} (null while unanswered). */
		public Grade beat(int k) {
			return k >= 0 && k < BEATS ? beats[k] : null;
		}

		/** Sets beat {@code k}'s grade outright (a side of the world's own timing). */
		public void set(int k, Grade grade) {
			if (k >= 0 && k < BEATS && beats[k] == null) {
				beats[k] = grade;
			}
		}

		public int fumbles() {
			return fumbles;
		}

		/** Its score so far: each beat's points, less one a fumble. */
		public int score() {
			int s = -fumbles;
			for (Grade g : beats) {
				if (g != null) {
					s += g.points;
				}
			}
			return s;
		}

		/** Whether every beat has a grade. */
		public boolean done() {
			for (Grade g : beats) {
				if (g == null) {
					return false;
				}
			}
			return true;
		}
	}

	/** Who won a clash. */
	public enum Outcome { A, B, EVEN }

	/** The winner of scores {@code a} and {@code b}: the higher, or on equal scores the one side with the Blade's edge, else neither. */
	public static Outcome outcome(int a, int b, boolean edgeA, boolean edgeB) {
		if (a != b) {
			return a > b ? Outcome.A : Outcome.B;
		}
		if (edgeA != edgeB) {
			return edgeA ? Outcome.A : Outcome.B;
		}
		return Outcome.EVEN;
	}

	/**
	 * How the struggle leans, -1 (all {@code b}'s) to 1 (all {@code a}'s), from the scores so far: the meeting point drifts toward the side
	 * losing it, for everyone watching.
	 */
	public static double balance(int a, int b) {
		double span = Math.max(4, Math.abs(a) + Math.abs(b));
		return Math.max(-1, Math.min(1, (a - b) / span));
	}

	// ------------------------------------------------------------------ the network

	/** The most ticks a press is moved back for its sender's connection. */
	public static final int MAX_LAG = 6;

	/** How many ticks a press from a player whose round trip is {@code latencyMs} is taken to have come early (half the trip, held to {@link #MAX_LAG}). */
	public static int lag(int latencyMs) {
		return Math.max(0, Math.min(MAX_LAG, (int) Math.round(Math.max(0, latencyMs) / 100.0)));
	}

	// ------------------------------------------------------------------ the swordsmen of the world

	/** A press a swordsman of the world doesn't make (it misses the beat). */
	public static final int NPC_MISS = Integer.MIN_VALUE;
	private static final double[] NPC_SPREAD = {4.0, 4.0, 3.6, 3.2, 2.8, 2.4};
	private static final double[] NPC_MISS_CHANCE = {0.3, 0.3, 0.25, 0.2, 0.15, 0.12};

	/** How far off the beat a swordsman of the world at {@code stage} presses, one standard spread (ticks). */
	public static double npcSpread(int stage) {
		return NPC_SPREAD[AuraRules.clampStage(stage)];
	}

	/** How likely it is to miss a beat altogether at {@code stage}. */
	public static double npcMiss(int stage) {
		return NPC_MISS_CHANCE[AuraRules.clampStage(stage)];
	}

	/**
	 * Where a swordsman of the world at {@code stage} presses on a beat, in ticks off it, from {@code u} (a uniform draw: below its miss chance
	 * it misses) and {@code gaussian} (a standard normal draw): {@link #NPC_MISS} for a miss, and any press it would make outside its window is
	 * a miss too (it never fumbles).
	 */
	public static int npcOffset(int stage, double u, double gaussian) {
		if (u < npcMiss(stage)) {
			return NPC_MISS;
		}
		int off = (int) Math.round(gaussian * npcSpread(stage));
		return Math.abs(off) > GOOD ? NPC_MISS : off;
	}

	// ------------------------------------------------------------------ what comes of it

	/** The winning crescent flies on at this share of its harm (a little spent by the struggle). */
	public static final double CARRY = 0.8;
	/** The same two don't lock again for this long (ticks): meeting before then, their strikes break each other as of old. */
	public static final int REST = 60;
	/** An art loosed within this many ticks of being struck by a foe's art answers it: the two lock. */
	public static final int CROSS = 8;
	/** An art meets an oncoming crescent within this many blocks of the swordsman's eyes, ahead of them. */
	public static final double MEET_REACH = 4.0;
	/** An art answers an art only against a foe this close (blocks). */
	public static final double ANSWER_REACH = 8.0;
	/** Ahead: the look's flat dot with the way to the other at least this. */
	public static final double FACING = 0.5;
	/** A crescent comes on at its swordsman: its flight's flat dot with the way to them at least this. */
	public static final double ONCOMING = 0.3;
	/** When the first striker wins an answered art's clash, their art carries on: this share again of what it dealt. */
	public static final double ANSWER_CARRY = 0.5;
	/** The momentum a clash won builds (before the method's temper and the server's gain). */
	public static final double WIN_MOMENTUM = 10;

	/** What the winning crescent flies on at: its harm times the carry ({@code carry}, the server's {@code clash_carry}). */
	public static double carried(double damage, double carry) {
		return Math.max(0, damage) * Math.max(0, carry);
	}

	/** Whether two who clashed at {@code last} (game time) may lock again at {@code now}. */
	public static boolean rested(long last, long now) {
		return last == Long.MIN_VALUE || now - last >= REST;
	}

	/** How the kinds of clash are told apart (on the wire and in the hooks). */
	public enum Kind {
		/** Two crescents meeting in the air. */
		CRESCENTS,
		/** An art loosed into an oncoming crescent ({@code a} the art, {@code b} the crescent). */
		ART_CRESCENT,
		/** An art answering an art in the same breath ({@code a} the answer, {@code b} the first striker). */
		ARTS;

		public static Kind of(int ordinal) {
			Kind[] all = values();
			return ordinal >= 0 && ordinal < all.length ? all[ordinal] : CRESCENTS;
		}
	}
}
