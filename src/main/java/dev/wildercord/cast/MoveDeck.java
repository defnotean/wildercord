package dev.wildercord.cast;

/**
 * A boss's memory of its last few moves, so that it picks among what it can do rather than doing one thing over and over. Each
 * move it could make right now comes with a weight (nothing or less meaning it can't); a move it made lately counts for less the
 * more often it made it, one it hasn't made in a while counts for more, and the move it just made is out unless there is nothing
 * else. Kept free of the game so it can be tested.
 */
public final class MoveDeck {
	/** What each time a move was made lately leaves of its weight, and what a move not made lately has its weight raised by. */
	public static final double REPEAT = 0.35, FRESH = 1.5;
	private final int[] recent;
	private int count, head;

	/** A deck remembering the last {@code memory} moves. */
	public MoveDeck(int memory) {
		recent = new int[Math.max(1, memory)];
	}

	/** Remember {@code move} as made. */
	public void played(int move) {
		recent[head] = move;
		head = (head + 1) % recent.length;
		if (count < recent.length) count++;
	}

	/** The move made last, or -1. */
	public int last() {
		return count == 0 ? -1 : recent[(head - 1 + recent.length) % recent.length];
	}

	/** How often {@code move} was made among those remembered. */
	public int repeats(int move) {
		int n = 0;
		for (int i = 0; i < count; i++) if (recent[i] == move) n++;
		return n;
	}

	/** Forget everything, as at the start of a fight. */
	public void clear() {
		count = 0;
		head = 0;
	}

	/** What {@code weight} for {@code move} comes to after what was made lately, the last move aside. */
	public double weigh(int move, double weight) {
		if (weight <= 0) return 0;
		int n = repeats(move);
		return n == 0 ? weight * FRESH : weight * Math.pow(REPEAT, n);
	}

	/**
	 * Which of {@code moves} to make, each with the weight at the same place in {@code weights}, {@code roll} being 0-1; -1 if none
	 * of them can be made.
	 */
	public int pick(int[] moves, double[] weights, double roll) {
		int open = 0, last = last();
		boolean other = false;
		for (int i = 0; i < moves.length; i++) if (weights[i] > 0) {
			open++;
			if (moves[i] != last) other = true;
		}
		if (open == 0) return -1;
		double[] w = new double[moves.length];
		double total = 0;
		for (int i = 0; i < moves.length; i++) {
			if (weights[i] <= 0 || (other && moves[i] == last)) continue;
			w[i] = weigh(moves[i], weights[i]);
			total += w[i];
		}
		double r = Math.max(0, Math.min(roll, 0.999999)) * total;
		int chosen = -1;
		for (int i = 0; i < moves.length; i++) {
			if (w[i] <= 0) continue;
			chosen = moves[i];
			r -= w[i];
			if (r < 0) break;
		}
		return chosen;
	}
}
