package dev.wildercord.cast;

/**
 * When a player may step into the practice room: not as a way out of a fight, a fall, a fire or a ride, and not again
 * within {@link #COOLDOWN_TICKS} of the last time. Leaving is always allowed.
 */
public final class PracticeRules {
	private PracticeRules() {}

	/** Ticks between two entries. */
	public static final int COOLDOWN_TICKS = 600;
	/** The tick of a player's last entry when they have never entered (halved so {@code now - NEVER} can't overflow). */
	public static final long NEVER = Long.MIN_VALUE / 2;

	/** Why a player can't enter now, or {@link #NONE}. */
	public enum Refusal { NONE, DUEL, HURT, FALLING, BURNING, RIDING, COOLDOWN }

	/** Why a player in this state can't enter the practice room; {@code now} and {@code lastEntered} are server ticks. */
	public static Refusal refusal(boolean inDuel, boolean hurtRecently, boolean onGround, boolean burning, boolean riding,
		long now, long lastEntered) {
		if (inDuel) {
			return Refusal.DUEL;
		}
		if (hurtRecently) {
			return Refusal.HURT;
		}
		if (riding) {
			return Refusal.RIDING;
		}
		if (!onGround) {
			return Refusal.FALLING;
		}
		if (burning) {
			return Refusal.BURNING;
		}
		if (now >= lastEntered && now - lastEntered < COOLDOWN_TICKS) {
			return Refusal.COOLDOWN;
		}
		return Refusal.NONE;
	}
}
