package dev.wildercord.aura;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A sword string: a short run of swings, each of a kind (a {@link Token}), that a swordsman plays to set off an art. Ordinary
 * swings are the language: a plain swing, a full swing (the blade fully recovered), a low swing (crouching), a leaping swing
 * (in the air), a running swing (sprinting), a counter (the first swing straight after a perfect Aura Guard) and a step cut
 * (the first swing straight after an Aura Step). "swing swing low" is two swings of any kind and then a low one.
 *
 * <p>A swing can be several kinds at once (a full, low swing straight after a perfect guard is a full swing, a low swing and a
 * counter), so a string's token asks for one kind and any swing that has it fits: a plain {@link Token#SWING} fits every swing.
 * What a swing was is a set of bits, its <em>marks</em> ({@link Token#bit}). Timing (each swing within its window of the one
 * before) is the reader's ({@link StringReader}); this is only the grammar.</p>
 *
 * <p>Pure (no Minecraft types), shared by the server, the client's reader, the Aura page and the unit tests.</p>
 */
public record SwordString(List<Token> tokens) {
	/** The longest string there is: six swings. */
	public static final int MAX_LENGTH = 6;

	/** The kinds of swing a string is written in. */
	public enum Token {
		/** Any swing at all. */
		SWING("swing", 0),
		/** A full swing: the blade had recovered (vanilla's attack strength at nine tenths or more). */
		FULL("full", 1),
		/** A low swing: struck crouching. */
		LOW("low", 2),
		/** A leaping swing: struck in the air (jumping or falling, not swimming, climbing, riding or flying). */
		LEAP("leap", 2),
		/** A running swing: struck sprinting. */
		RUN("run", 2),
		/** A counter: the first swing within a moment of a perfect Aura Guard. */
		COUNTER("counter", 4),
		/** A step cut: the first swing within a moment of an Aura Step. */
		STEP("step", 4);

		/** How it's written ("swing", "full", "low", "leap", "run", "counter", "step"). */
		public final String id;
		/**
		 * How much it asks of the player: a plain swing nothing, a full swing a little (it waits on the blade), a low, leaping or
		 * running swing more, a counter or a step cut most (each needs a guard or a step first). When more than one string fits
		 * the same swings, the one that asks the most wins (see {@link StringReader#best}).
		 */
		public final int weight;

		Token(String id, int weight) {
			this.id = id;
			this.weight = weight;
		}

		/** Its bit in a swing's marks. */
		public int bit() {
			return 1 << ordinal();
		}

		/** Whether a swing with these marks fits this token. */
		public boolean fits(int marks) {
			return this == SWING || (marks & bit()) != 0;
		}

		/** Whether a swing that fits {@code other} always fits this too (a plain swing is fitted by anything; else only by itself). */
		public boolean coveredBy(Token other) {
			return this == SWING || this == other;
		}

		public static Optional<Token> byId(String id) {
			for (Token t : values()) {
				if (t.id.equals(id)) {
					return Optional.of(t);
				}
			}
			return Optional.empty();
		}

		/**
		 * The kind a swing with these marks is shown as: its most telling one (a counter or a step cut, then a leaping, low or
		 * running swing, then a full one, and a plain swing last).
		 */
		public static Token shown(int marks) {
			for (Token t : new Token[] {COUNTER, STEP, LEAP, LOW, RUN, FULL}) {
				if ((marks & t.bit()) != 0) {
					return t;
				}
			}
			return SWING;
		}

		/** The marks for these kinds together (every swing is a swing, so its bit is always there). */
		public static int marks(Token... kinds) {
			int marks = SWING.bit();
			for (Token t : kinds) {
				marks |= t.bit();
			}
			return marks;
		}
	}

	public SwordString {
		if (tokens == null || tokens.isEmpty()) {
			throw new IllegalArgumentException("a sword string needs at least one swing");
		}
		if (tokens.size() > MAX_LENGTH) {
			throw new IllegalArgumentException("a sword string has at most " + MAX_LENGTH + " swings (" + tokens.size() + " given)");
		}
		for (Token t : tokens) {
			if (t == null) {
				throw new IllegalArgumentException("a sword string can't hold a missing swing");
			}
		}
		tokens = List.copyOf(tokens);
	}

	public static SwordString of(Token... tokens) {
		return new SwordString(List.of(tokens));
	}

	/**
	 * A string written out: token ids separated by spaces or commas, as {@link #text} writes them ("swing swing low",
	 * "full, full, full, low").
	 *
	 * @throws IllegalArgumentException for an unknown token, or a string too short or too long
	 */
	public static SwordString parse(String text) {
		List<Token> tokens = new ArrayList<>();
		for (String part : (text == null ? "" : text).trim().toLowerCase(Locale.ROOT).split("[\\s,]+")) {
			if (part.isEmpty()) {
				continue;
			}
			tokens.add(Token.byId(part).orElseThrow(() -> new IllegalArgumentException("no such swing in a sword string: '" + part + "'")));
		}
		return new SwordString(tokens);
	}

	public int length() {
		return tokens.size();
	}

	public Token token(int i) {
		return tokens.get(i);
	}

	/** The swing that finishes it (the release). */
	public Token last() {
		return tokens.getLast();
	}

	/** How much the whole string asks: its tokens' weights together. */
	public int weight() {
		int w = 0;
		for (Token t : tokens) {
			w += t.weight;
		}
		return w;
	}

	/** Whether any of its swings is of this kind. */
	public boolean has(Token token) {
		return tokens.contains(token);
	}

	/** How it's written: token ids separated by spaces. */
	public String text() {
		StringBuilder out = new StringBuilder();
		for (Token t : tokens) {
			if (!out.isEmpty()) {
				out.append(' ');
			}
			out.append(t.id);
		}
		return out.toString();
	}

	/** Whether these swings' marks, oldest first, fit the string exactly (one swing a token, in order). */
	public boolean fits(int[] marks) {
		if (marks.length != tokens.size()) {
			return false;
		}
		for (int i = 0; i < marks.length; i++) {
			if (!tokens.get(i).fits(marks[i])) {
				return false;
			}
		}
		return true;
	}

	/** Whether the last swings of these, oldest first, fit the string: whether playing them just finished it. */
	public boolean endsIn(int[] marks) {
		int n = tokens.size();
		if (marks.length < n) {
			return false;
		}
		int from = marks.length - n;
		for (int i = 0; i < n; i++) {
			if (!tokens.get(i).fits(marks[from + i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether this string, played as it's written, finishes {@code shorter} on the way: whether swings of exactly the kinds this
	 * string asks for would complete {@code shorter} before this string's last swing, which cuts this one short (an art goes off
	 * the moment its string is played). "swing swing" cuts "swing swing low"; "swing swing low" doesn't cut "full full full low",
	 * since a full swing alone isn't low. Registering a string that a shorter one cuts is allowed (a player can still add a mark
	 * on purpose to avoid it) but almost always a mistake.
	 */
	public boolean cutBy(SwordString shorter) {
		int m = shorter.length();
		if (m >= length()) {
			return false;
		}
		// The shorter one completing on this one's k-th swing (k from m to length - 1, counting from 1).
		for (int k = m; k < length(); k++) {
			boolean all = true;
			for (int j = 0; j < m && all; j++) {
				all = shorter.token(j).coveredBy(tokens.get(k - m + j));
			}
			if (all) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return text();
	}
}
