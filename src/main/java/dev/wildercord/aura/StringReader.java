package dev.wildercord.aura;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

/**
 * Reads sword strings out of a player's swings as they come: the detector. Each swing is given (when, its marks, how long its
 * blade takes to recover) and the reader answers with what it meant: it {@linkplain Landed landed} on a string still being
 * played, {@linkplain Completed completed} one, {@linkplain Refused completed} one that can't go now, or came too late to finish
 * the one that just ran out ({@linkplain Fumbled a fumble}). Left alone past its window a string {@linkplain Lapsed lapses}
 * quietly ({@link #tick}).
 *
 * <ul>
 * <li><b>Windows.</b> Each swing must come within {@link StringRules#window} of the one before: the base window counted from the
 *     moment the blade was ready again. A perfect guard or an Aura Step ({@link #cue}) in the middle of a string keeps it going,
 *     and marks the first swing after it, within its own moment, as a counter or a step cut.</li>
 * <li><b>Matching.</b> After every swing, each candidate string whose tokens the last swings fit is complete. The one that asks
 *     the most wins ({@link #best}): the heaviest last token (the release), then the heaviest string, then the longest, then the
 *     highest stage. Of those that can go now (the caller's {@code usable}) the best goes; if none can, the best is refused. A
 *     completed (or refused) string uses its swings up: the next string starts afresh.</li>
 * <li><b>Fumbles.</b> A swing that would have finished a string whose last token isn't a plain swing (a deliberate release), had
 *     it come in time, and comes no more than {@link StringRules#LATE_GRACE} late, is a fumble.</li>
 * </ul>
 *
 * <p>Pure (no Minecraft types): the client keeps one for its player; the unit tests drive it with plain numbers.</p>
 *
 * @param <T> what a string sets off (an art), known by its string and stage
 */
public final class StringReader<T extends StringReader.Spelled> {
	/** What a reader matches: a string, and the stage it opens at. */
	public interface Spelled {
		SwordString string();

		int stage();
	}

	/**
	 * A swing as the reader saw it.
	 *
	 * @param at      the game time it was struck
	 * @param marks   what kind of swing it was ({@link SwordString.Token#bit}s; the counter and step cut ones added by the reader)
	 * @param recover how long its blade takes to be ready for a full swing again (ticks)
	 */
	public record Stroke(long at, int marks, int recover) {}

	/** Something that happened between swings that a string can be written around. */
	public enum Cue {
		/** A perfect Aura Guard: the first swing after it is a counter. */
		GUARD,
		/** An Aura Step: the first swing after it is a step cut. */
		STEP;

		/** How long after it the first swing still counts (ticks). */
		public int window() {
			return this == GUARD ? StringRules.COUNTER_TICKS : StringRules.STEP_CUT_TICKS;
		}

		/** The token a swing in its moment fits. */
		public SwordString.Token token() {
			return this == GUARD ? SwordString.Token.COUNTER : SwordString.Token.STEP;
		}
	}

	/** What a swing (or a quiet moment) meant. */
	public sealed interface Event permits Landed, Completed, Refused, Fumbled, Lapsed {}

	/** A swing that went on a string still being played (or began one). */
	public record Landed(Stroke stroke) implements Event {}

	/** A string played to its end, with an art that can go now: {@code strokes} are its swings, oldest first. */
	public record Completed<T>(T art, List<Stroke> strokes) implements Event {}

	/** A string played to its end whose art can't go now (resting, too little aura, its condition unmet), and none other could. */
	public record Refused<T>(T art, List<Stroke> strokes) implements Event {}

	/** A swing that would have finished {@code art}'s string had it come in time: {@code strokes} are the string's, the late one last. */
	public record Fumbled<T>(T art, List<Stroke> strokes) implements Event {}

	/** A string left unfinished past its window, letting go quietly. */
	public record Lapsed(List<Stroke> strokes) implements Event {}

	private static final long NEVER = Long.MIN_VALUE / 4;

	private int baseWindow;
	private final Deque<Stroke> chain = new ArrayDeque<>();
	/** When the string's last move was (a swing, or a guard or step in the middle of it), and how long the next swing has. */
	private long lastAt = NEVER;
	private int nextWindow;
	/** When each cue last came, while the swing it marks hasn't come yet. */
	private long guardAt = NEVER;
	private long stepAt = NEVER;
	/** The string that ran out last, and when its window closed: for a late swing's fumble. */
	private List<Stroke> lapsed = List.of();
	private long lapsedAt = NEVER;

	public StringReader(int baseWindow) {
		this.baseWindow = Math.max(1, baseWindow);
	}

	/** Changes the base window (the server's setting can change at a reload). */
	public void window(int ticks) {
		this.baseWindow = Math.max(1, ticks);
	}

	public int window() {
		return baseWindow;
	}

	/** The swings of the string being played, oldest first. */
	public List<Stroke> chain() {
		return List.copyOf(chain);
	}

	/** When the string being played runs out (the game time after which a swing comes too late), or {@link Long#MIN_VALUE} with none. */
	public long deadline() {
		return chain.isEmpty() ? Long.MIN_VALUE : lastAt + nextWindow;
	}

	/** How long the next swing had, from the string's last move (ticks): the window the deadline was counted with. */
	public int nextWindow() {
		return chain.isEmpty() ? 0 : nextWindow;
	}

	/** Forgets everything: the string, the cues, a string that ran out. */
	public void reset() {
		chain.clear();
		lastAt = NEVER;
		nextWindow = 0;
		guardAt = NEVER;
		stepAt = NEVER;
		lapsed = List.of();
		lapsedAt = NEVER;
	}

	/** The marks the cues give a swing struck at {@code now}, without using them up. */
	public int cueMarks(long now) {
		int marks = 0;
		if (guardAt != NEVER && now - guardAt <= Cue.GUARD.window() && now >= guardAt) {
			marks |= SwordString.Token.COUNTER.bit();
		}
		if (stepAt != NEVER && now - stepAt <= Cue.STEP.window() && now >= stepAt) {
			marks |= SwordString.Token.STEP.bit();
		}
		return marks;
	}

	/**
	 * A perfect guard or an Aura Step at {@code now}: the first swing within its moment gets its mark, and a string being played
	 * stays open at least that long.
	 */
	public void cue(Cue cue, long now) {
		if (cue == Cue.GUARD) {
			guardAt = now;
		} else {
			stepAt = now;
		}
		if (!chain.isEmpty() && now - lastAt <= nextWindow) {
			long left = lastAt + nextWindow - now;
			nextWindow = (int) Math.max(left, cue.window());
			lastAt = now;
		}
	}

	/** Every tick: a string past its window lapses (returned once), and old cues and a long-gone lapsed string are let go. */
	public Event tick(long now) {
		if (guardAt != NEVER && now - guardAt > Cue.GUARD.window()) {
			guardAt = NEVER;
		}
		if (stepAt != NEVER && now - stepAt > Cue.STEP.window()) {
			stepAt = NEVER;
		}
		if (!lapsed.isEmpty() && now > lapsedAt + StringRules.LATE_GRACE) {
			lapsed = List.of();
		}
		if (!chain.isEmpty() && now - lastAt > nextWindow) {
			List<Stroke> gone = lapse();
			return new Lapsed(gone);
		}
		return null;
	}

	private List<Stroke> lapse() {
		List<Stroke> gone = List.copyOf(chain);
		lapsed = gone;
		lapsedAt = lastAt + nextWindow;
		chain.clear();
		return gone;
	}

	/**
	 * A swing at {@code now}.
	 *
	 * @param marks      what kind of swing it was (the counter and step cut marks are the reader's to add)
	 * @param recover    how long its blade takes to recover (ticks)
	 * @param candidates the strings the player has (their stage reached, open to them), in registration order
	 * @param usable     whether a string's art can go now (resting, aura, its condition)
	 */
	public Event stroke(long now, int marks, int recover, List<T> candidates, Predicate<T> usable) {
		int cued = cueMarks(now);
		if ((cued & SwordString.Token.COUNTER.bit()) != 0) {
			guardAt = NEVER;
		}
		if ((cued & SwordString.Token.STEP.bit()) != 0) {
			stepAt = NEVER;
		}
		Stroke stroke = new Stroke(now, marks | cued | SwordString.Token.SWING.bit(), Math.max(0, recover));
		if (!chain.isEmpty() && now - lastAt > nextWindow) {
			lapse();
		}
		List<Stroke> late = null;
		if (chain.isEmpty() && !lapsed.isEmpty() && now <= lapsedAt + StringRules.LATE_GRACE) {
			late = lapsed;
		}
		lapsed = List.of();
		chain.addLast(stroke);
		while (chain.size() > StringRules.CHAIN) {
			chain.removeFirst();
		}
		lastAt = now;
		nextWindow = StringRules.window(baseWindow, stroke.recover());

		List<Stroke> played = List.copyOf(chain);
		List<T> matches = matching(candidates, played);
		if (!matches.isEmpty()) {
			List<T> ready = new ArrayList<>();
			for (T t : matches) {
				if (usable == null || usable.test(t)) {
					ready.add(t);
				}
			}
			chain.clear();
			if (!ready.isEmpty()) {
				T art = best(ready);
				return new Completed<>(art, tail(played, art.string().length()));
			}
			T art = best(matches);
			return new Refused<>(art, tail(played, art.string().length()));
		}
		if (late != null) {
			List<Stroke> attempt = new ArrayList<>(late);
			attempt.add(stroke);
			List<T> meant = new ArrayList<>();
			for (T t : matching(candidates, attempt)) {
				SwordString s = t.string();
				// A deliberate release on a string of two or more: the late swing alone (a lone counter) isn't a fumble.
				if (s.length() >= 2 && s.last().weight > 0) {
					meant.add(t);
				}
			}
			if (!meant.isEmpty()) {
				chain.clear();
				T art = best(meant);
				return new Fumbled<>(art, tail(attempt, art.string().length()));
			}
		}
		return new Landed(stroke);
	}

	/** The candidates whose strings the end of {@code played} (oldest first) completes, in the candidates' order. */
	public static <T extends Spelled> List<T> matching(List<T> candidates, List<Stroke> played) {
		int[] marks = new int[played.size()];
		for (int i = 0; i < marks.length; i++) {
			marks[i] = played.get(i).marks();
		}
		List<T> out = new ArrayList<>();
		for (T t : candidates) {
			if (t != null && t.string() != null && t.string().endsIn(marks)) {
				out.add(t);
			}
		}
		return out;
	}

	/**
	 * Of strings that all fit the same swings, the one that asks the most: the heaviest last token (a counter or a step cut beats
	 * a low, leaping or running swing, which beats a full one, which beats a plain one), then the heaviest string, then the
	 * longest, then the highest stage; the first given wins a tie.
	 */
	public static <T extends Spelled> T best(List<T> matches) {
		T best = null;
		for (T t : matches) {
			if (best == null || compare(t, best) > 0) {
				best = t;
			}
		}
		return best;
	}

	/** Positive when {@code a} asks more than {@code b} (see {@link #best}). */
	public static int compare(Spelled a, Spelled b) {
		SwordString x = a.string();
		SwordString y = b.string();
		int c = Integer.compare(x.last().weight, y.last().weight);
		if (c == 0) {
			c = Integer.compare(x.weight(), y.weight());
		}
		if (c == 0) {
			c = Integer.compare(x.length(), y.length());
		}
		if (c == 0) {
			c = Integer.compare(a.stage(), b.stage());
		}
		return c;
	}

	private static List<Stroke> tail(List<Stroke> strokes, int n) {
		return List.copyOf(strokes.subList(Math.max(0, strokes.size() - n), strokes.size()));
	}
}
