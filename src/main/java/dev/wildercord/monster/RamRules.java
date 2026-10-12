package dev.wildercord.monster;

/**
 * The Cragcleaver Ram's rules, apart from the game so they can be tested: when it paws the ground and charges, when its
 * aim sets so the charge can be stepped aside from, how far it runs, how long a wall leaves it dazed, and its butt up close.
 */
public final class RamRules {
	private RamRules() {}

	/** It charges at prey this far off, no nearer (up close it butts) and no further. */
	public static final double CHARGE_MIN = 4.5;
	public static final double CHARGE_MAX = 18.0;
	/** It paws the ground this long first; its aim follows its prey till this many ticks before it goes, then sets. */
	public static final int PAW_TICKS = 34;
	public static final int AIM_SETS = 10;
	/** The charge: this fast, for at most this long, then not again for a while. */
	public static final double CHARGE_SPEED = 0.62;
	public static final int CHARGE_TICKS = 34;
	public static final int CHARGE_COOLDOWN = 110;
	/** What the charge does to whatever it hits, and how far it throws it. */
	public static final float CHARGE_DAMAGE = 9.0F;
	public static final double CHARGE_KNOCK = 1.9;
	/** Into a wall: its horns stuck, dazed this long, and blows land this much harder meanwhile. */
	public static final int DAZE_TICKS = 70;
	public static final float DAZED_HURT = 1.5F;
	/** The butt up close. */
	public static final double BUTT_REACH = 2.6;
	public static final int BUTT_COOLDOWN = 28;
	public static final double BUTT_KNOCK = 1.0;

	/** Whether it starts pawing for a charge: ready, far enough off but not too far, and a clear run at its prey. */
	public static boolean charges(double distance, long now, long ready, boolean clearRun) {
		return clearRun && now >= ready && distance >= CHARGE_MIN && distance <= CHARGE_MAX;
	}

	/** Whether its aim still follows its prey, {@code left} ticks before it goes. */
	public static boolean aiming(long left) {
		return left > AIM_SETS;
	}

	/** Whether the charge is spent: run its length, or slowed to nothing against something. */
	public static boolean spent(int run, double speed) {
		return run >= CHARGE_TICKS || (run > 4 && speed < CHARGE_SPEED * 0.25);
	}

	/** Whether running into the wall dazes it: only a hard hit, at its full run, not a stumble at the start. */
	public static boolean dazes(boolean wall, int run) {
		return wall && run > 3;
	}

	/** A blow while it is dazed lands harder. */
	public static float hurt(float damage, boolean dazed) {
		return dazed ? damage * DAZED_HURT : damage;
	}

	/** Whether it butts its prey up close. */
	public static boolean butts(double distance, long now, long ready) {
		return now >= ready && distance <= BUTT_REACH;
	}
}
