package dev.wildercord.monster;

/**
 * The Cackling Hyena's rules, apart from the game so they can be tested: how wide its pack circles, when one of them darts
 * in (one at a time, and sooner at a back turned), when the pack grows bold, and when a hurt one slinks off.
 */
public final class HyenaRules {
	private HyenaRules() {}

	/** How wide the pack circles its prey, and how close once the prey is badly hurt. */
	public static final double ORBIT = 6.0;
	public static final double BOLD_ORBIT = 3.5;
	/** How far round the circle it trots between steps (radians). */
	public static final double ORBIT_STEP = 0.4;
	/** Below this share of its health, the prey is weak and the pack grows bold. */
	public static final float BOLD_HEALTH = 0.5F;

	/** How far off it can dart from. */
	public static final double DART_RANGE = 8.0;
	/** Between one dart and its next... */
	public static final int DART_COOLDOWN = 70;
	/** ...or half that, bold. */
	public static final int BOLD_COOLDOWN = 35;
	/** How long it waits for a back to be turned before it darts in at a face anyway. */
	public static final int PATIENCE = 50;
	/** The crouch before the dart: a tell. */
	public static final int DART_WINDUP = 10;
	/** How long the dart lasts, and how close it has to come to bite. */
	public static final int DART_TICKS = 12;
	public static final double BITE_REACH = 1.7;
	public static final double DART_SPEED = 0.85;
	/** The bite hamstrings: Slowness I, for this long. */
	public static final int HAMSTRING_TICKS = 50;
	/** A missed dart leaves it stumbling, open to a blow. */
	public static final int STUMBLE_TICKS = 20;
	/** After a dart, it falls back out to the circle. */
	public static final int RETREAT_TICKS = 30;

	/** Hurt below this share of its health, it slinks off a while... */
	public static final float COWER_HEALTH = 0.3F;
	public static final int COWER_TICKS = 60;
	/** ...and not again for this long. */
	public static final int COWER_COOLDOWN = 300;

	/** Whether someone looking along {@code dot} (their look against the way to the hyena) has their back to it. */
	public static boolean backTurned(double dot) {
		return dot < 0.2;
	}

	/** Whether the prey is weak enough for the pack to close in. */
	public static boolean bold(float health, float maxHealth) {
		return maxHealth > 0 && health / maxHealth <= BOLD_HEALTH;
	}

	public static double orbit(boolean bold) {
		return bold ? BOLD_ORBIT : ORBIT;
	}

	public static int cooldown(boolean bold) {
		return bold ? BOLD_COOLDOWN : DART_COOLDOWN;
	}

	/**
	 * Whether it darts in now: in range, its own dart ready, no packmate darting already, and the prey's back turned (or it has
	 * waited long enough not to care).
	 */
	public static boolean darts(double distance, long now, long ready, boolean backTurned, boolean packmateDarting) {
		if (distance > DART_RANGE || packmateDarting || now < ready) {
			return false;
		}
		return backTurned || now >= ready + PATIENCE;
	}

	/** Whether a hurt one slinks off: hurt enough, and not done so lately. */
	public static boolean cowers(float health, float maxHealth, long now, long ready) {
		return maxHealth > 0 && health / maxHealth < COWER_HEALTH && now >= ready;
	}
}
