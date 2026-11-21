package dev.wildercord.cast;

/**
 * The bosses' pure rules (the three dungeon bosses and the Archivist), with no Minecraft types so
 * they're unit-tested: which phase a boss's health puts it in, and how far one blow may take it
 * (never past the start of its next phase, so no burst of damage skips a phase or kills it from its
 * first).
 */
public final class BossRules {
	private BossRules() {}

	/** Ticks a stranded Tide Scribe can't be stranded again after it breaks free. */
	public static final int STRAND_IMMUNE_TICKS = 100;
	/** Minimum duration (1.0s) for a temporary hold/immobilization on a boss. */
	public static final int BOSS_HOLD_MIN_TICKS = 20;
	/** Maximum duration (1.75s) a boss can be held before forcibly breaking out. */
	public static final int BOSS_HOLD_MAX_TICKS = 35;
	/** Ticks (5.0s) a boss resists follow-up holds/crowds-control after breaking free. */
	public static final int BOSS_HOLD_IMMUNE_TICKS = 100;

	/** Active hold and post-breakout unstoppable immunity window for a boss. */
	public record HoldWindow(long heldUntil, long immuneUntil) {}

	/** Clamp any requested boss hold duration to a brief 1–1.75s window. */
	public static int bossHoldTicks(int requestedTicks) {
		if (requestedTicks <= 0) {
			return 0;
		}
		return Math.clamp(requestedTicks, BOSS_HOLD_MIN_TICKS, BOSS_HOLD_MAX_TICKS);
	}

	/** Whether a boss is currently inside its temporary hold window. */
	public static boolean isBossHeld(HoldWindow window, long now) {
		return window != null && now < window.heldUntil();
	}

	/** Whether a boss has broken free and is inside its post-hold immunity window. */
	public static boolean isBossHoldImmune(HoldWindow window, long now) {
		return window != null && !isBossHeld(window, now) && now < window.immuneUntil();
	}

	/** Whether a fresh hold window may start (false while already held or immune). */
	public static boolean canHoldBoss(HoldWindow window, long now) {
		return window == null || now >= Math.max(window.heldUntil(), window.immuneUntil());
	}

	/** Create a new capped hold window starting at {@code now} followed by breakout immunity. */
	public static HoldWindow beginBossHold(long now, int requestedTicks) {
		int duration = bossHoldTicks(requestedTicks);
		long heldUntil = now + duration;
		long immuneUntil = duration > 0 ? heldUntil + BOSS_HOLD_IMMUNE_TICKS : now;
		return new HoldWindow(heldUntil, immuneUntil);
	}
	/** Ticks without an active challenger in the arena before a boss resets to full health. */
	public static final int WIPE_RESET_TICKS = 200;
	/** Short re-engagement pause after a full-party wipe reset. */
	public static final int WIPE_COOLDOWN_TICKS = 100;

	/** Whether an engaged boss has waited long enough with no challenger present to reset to phase 1 and full health. */
	public static boolean shouldReset(boolean engaged, boolean challengerPresent, int emptyTicks) {
		return engaged && !challengerPresent && emptyTicks >= WIPE_RESET_TICKS;
	}

	/** The phase (1 to 3) a boss is in at {@code health} of {@code max}: the second at two thirds, the third at one third. */
	public static int phaseFor(float health, float max) {
		return health > max * 2 / 3 ? 1 : health > max / 3 ? 2 : 3;
	}

	/** The health a boss in {@code phase} can't be taken below by one blow: where its next phase starts (0 in the last). */
	public static float floor(int phase, float max) {
		return phase <= 1 ? max * 2 / 3 : phase == 2 ? max / 3 : 0;
	}

	/**
	 * Its health after a blow took it from {@code before} to {@code after}, in {@code phase}: held at
	 * the start of the next phase if the blow would carry it past (or kill it before its last phase).
	 */
	public static float capped(int phase, float max, float before, float after) {
		float floor = floor(phase, max);
		return floor > 0 && before > floor && after < floor ? floor : after;
	}
}
