package dev.wildercord.aura.world;

/**
 * Hourglass signature: one long thrust down a locked lane, then the hourglass turns and the same cut replays from
 * where the master first stood. The spent lane stays marked: sidestep the first cut and do not step back into it.
 */
public final class HourglassRewindRules {
	private HourglassRewindRules() {}

	public static final int STRIKE = 13, REPLAY = 27, TELL = REPLAY, RECOVERY = 16, LOCK = 6;
	public static final int COOLDOWN = 150, WARNING_REFRESH = 3;
	public static final double COST = 28, DAMAGE = 27, REACH = 6, HALF_WIDTH = .9, MIN_DISTANCE = 1.5, MAX_HEIGHT = 1.5;

	public static boolean eligible(int school, int sequence, double distance, double height, double aura, long now, long readyAt) {
		return school == MastersPackB.HOURGLASS && Math.floorMod(sequence, 4) == 2 && now >= readyAt
			&& Double.isFinite(aura) && aura >= COST + MastersRules.GUARD_COST && aura <= MastersRules.AURA_MAX
			&& Double.isFinite(distance) && distance >= MIN_DISTANCE && distance <= REACH - .5
			&& Double.isFinite(height) && Math.abs(height) <= MAX_HEIGHT;
	}

	/** The replay warns for 12 to 14 ticks, like the first cut, so both halves read the same. */
	public static int replayTell() { return REPLAY - STRIKE; }

	/** Both the cut and its replay measure the same frozen lane. */
	public static boolean hits(double forward, double side, double height) {
		return Double.isFinite(forward) && Double.isFinite(side) && Double.isFinite(height) && Math.abs(height) <= 2.5
			&& forward >= 0 && forward <= REACH && Math.abs(side) <= HALF_WIDTH;
	}
}
