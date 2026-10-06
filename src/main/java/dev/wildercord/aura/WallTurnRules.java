package dev.wildercord.aura;

/** Bounded movement and fresh-input rules shared by the server, readout and headless tests. */
public final class WallTurnRules {
	private WallTurnRules() {}
	public static final int COST = 20, BRACE_TICKS = 10, KICK_TICKS = 8, REST_TICKS = 120, RECOVERY_TICKS = 10;
	public static final double REACH = .8, DISTANCE = 4, RISE = 1.25, PROBE = .125;
	public static final int IDLE = 0, BRACE = 1, KICK = 2, FALL = 3, LAND = 4, ABORT = 5;
	public static final int PRESS = 0, RELEASE = 1, CANCEL = 2, EQUIP = 3, UNEQUIP = 4;

	public static boolean eligible(int stage, boolean galeClear) { return stage >= 5 && galeClear; }
	public static boolean canPay(double aura, long now, long readyAt, boolean used) {
		return Double.isFinite(aura) && aura >= COST && now >= readyAt && !used;
	}
	/** A path rises once and returns to the launch height. It never adds height on top of a rising endpoint. */
	public static double rise(int step) {
		return RISE * Math.sin(Math.PI * Math.clamp(step, 0, KICK_TICKS) / KICK_TICKS);
	}
	public static double distance(int step) { return DISTANCE * Math.clamp(step, 0, KICK_TICKS) / KICK_TICKS; }
	public static boolean phase(int phase) { return phase >= IDLE && phase <= ABORT; }
	public static boolean action(int action) { return action >= PRESS && action <= UNEQUIP; }

	/** Packets are edges from one body session, not a client assertion that a move was accepted. */
	public static final class Input {
		private long sequence;
		private long releasedAt = Long.MIN_VALUE;
		private boolean held;
		public boolean admit(long incoming, int action, long now) {
			if (!action(action) || incoming <= sequence || incoming - sequence > 1024) return false;
			sequence = incoming;
			if (action == CANCEL) { held = true; releasedAt = Long.MIN_VALUE; return true; }
			if (action == RELEASE) { held = false; releasedAt = now; return true; }
			if (action != PRESS) return true;
			if (held || releasedAt == now) return false;
			held = true;
			return true;
		}
	}
}
