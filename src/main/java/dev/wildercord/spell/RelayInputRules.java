package dev.wildercord.spell;

/** Fresh cast edges, independent of keyboard repeat and duplicate network delivery. */
public final class RelayInputRules {
	private RelayInputRules() {}
	public static final int DOWN = 0, UP = 1, CANCEL = 2;

	public static final class Edges {
		private long nonce = -1, downTick = Long.MIN_VALUE;
		private boolean held;
		public boolean accept(int action, long nextNonce, long tick) {
			if (action < DOWN || action > CANCEL || nextNonce < 0 || nextNonce <= nonce) return false;
			nonce = nextNonce;
			if (action == CANCEL) { held = false; return true; }
			if (action == UP) { if (!held) return false; held = false; return true; }
			if (held || tick <= downTick) return false;
			held = true;
			downTick = tick;
			return true;
		}
	}
}
