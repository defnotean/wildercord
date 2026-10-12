package dev.wildercord.monster;

/**
 * The Snapjaw Turtle's rules, apart from the game so they can be tested: when it lunges from its lurk, how long it holds
 * on once its jaws close and what makes it let go, where its shell turns a blow, and when it pulls into its shell.
 */
public final class SnapjawRules {
	private SnapjawRules() {}

	/** Lurking, it snaps at anything that comes this close, after a twitch this long, and not again for a while after. */
	public static final double SNAP_REACH = 2.6;
	public static final int SNAP_WINDUP = 4;
	public static final int SNAP_COOLDOWN = 50;
	/** Out of its lurk it bites at this reach, this often. */
	public static final double BITE_REACH = 2.2;
	public static final int BITE_COOLDOWN = 30;
	/** Its jaws hold on this long, worrying for this much every so often. */
	public static final int LATCH_TICKS = 60;
	public static final int WORRY_EVERY = 20;
	public static final float WORRY_DAMAGE = 2.0F;
	/** This much harm while it holds on makes it let go. */
	public static final float LETGO_DAMAGE = 6.0F;
	/** Its prey gets this far off (pulled away or gone) and it lets go. */
	public static final double LATCH_SLIP = 3.5;
	/** Its shell takes a blow from above or behind down to this. */
	public static final float SHELL_FACTOR = 0.3F;
	/** Blows from this far above its back count as from above. */
	public static final double ABOVE = 0.6;
	/** A blow whose direction meets its facing this low (or lower) counts as from behind. */
	public static final double BEHIND_DOT = -0.25;
	/** This hurt, it pulls into its shell this long, all blows taken down to this, and not again for a while after. */
	public static final float WITHDRAW_FRACTION = 0.35F;
	public static final int WITHDRAW_TICKS = 80;
	public static final int WITHDRAW_COOLDOWN = 300;
	public static final float WITHDRAWN_FACTOR = 0.15F;
	/** Out of the water, hunting nothing, it heads back in after this long. */
	public static final int DRY_TICKS = 100;

	/** Lurking still, its prey this close, ready: it snaps. */
	public static boolean snaps(double distance, long now, long ready, boolean latched) {
		return !latched && now >= ready && distance <= SNAP_REACH;
	}

	/** Holding on: it lets go once its time's up, once it's been hurt enough, or once its prey's pulled away. */
	public static boolean letsGo(int held, float harm, double distance) {
		return held >= LATCH_TICKS || harm >= LETGO_DAMAGE || distance > LATCH_SLIP;
	}

	/** Whether it worries its prey this tick of holding on. */
	public static boolean worries(int held) {
		return held > 0 && held % WORRY_EVERY == 0;
	}

	/**
	 * Whether a blow lands on its shell: from well above its back, or from behind. {@code dot} is the blow's direction from
	 * the turtle (towards whoever struck) against its facing, both flat and unit length.
	 */
	public static boolean onShell(double heightAboveBack, double dot) {
		return heightAboveBack >= ABOVE || dot <= BEHIND_DOT;
	}

	/** What a blow does, after its shell or its pulling in. */
	public static float damage(float damage, boolean onShell, boolean withdrawn) {
		if (withdrawn) {
			return damage * WITHDRAWN_FACTOR;
		}
		return onShell ? damage * SHELL_FACTOR : damage;
	}

	/** Badly hurt and ready: it pulls into its shell. */
	public static boolean withdraws(float health, float max, long now, long ready) {
		return now >= ready && health <= max * WITHDRAW_FRACTION;
	}
}
