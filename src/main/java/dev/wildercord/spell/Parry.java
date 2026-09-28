package dev.wildercord.spell;

/**
 * Parrying: a Shield raised at the last moment turns a spell back. The timing and the numbers live
 * here, pure, so they can be tested; {@code cast.Shields} applies them.
 *
 * <p>A Shield parries a spell when it was raised no more than {@link #WINDOW} ticks before the spell
 * reached it, or while that spell was already on its way and close (inside the range where a Shield's
 * circles appear). A parry ignores what the spell weighs: even one that would shatter the Shield is
 * turned. The Shield is spent either way.</p>
 */
public final class Parry {
	private Parry() {}

	/** How long before a spell arrives a Shield can go up and still parry it: 7 ticks, about a third of a second. */
	public static final int WINDOW = 7;
	/** A reflected bolt flies back this much faster than it came. */
	public static final double REFLECT_SPEED = 1.25;
	/** A counter-burst hits for this fraction of what the parried spell was worth. */
	public static final double COUNTER = 0.5;
	/** The most a counter-burst can deal (before the caster's power and PvP scaling). */
	public static final double MAX_COUNTER = 12.0;
	/** A counter-burst reaches the parried spell's caster this far away at most. */
	public static final double COUNTER_RANGE = 32.0;

	/**
	 * Whether a Shield raised at {@code raisedAt} (a game time; negative for never) parries a spell that
	 * reaches it at {@code now}: it went up no more than {@link #WINDOW} ticks before.
	 */
	public static boolean timed(long raisedAt, long now) {
		return raisedAt >= 0 && now >= raisedAt && now - raisedAt <= WINDOW;
	}

	/**
	 * Whether a spell reaching a Shield at {@code now} is parried.
	 *
	 * @param primed the Shield went up while this spell was already on its way, close by
	 */
	public static boolean parries(long raisedAt, long now, boolean primed) {
		return primed || timed(raisedAt, now);
	}

	/**
	 * What a counter-burst deals, before the parried caster's own power: half of what a spell of this
	 * mana weight is roughly worth (4 plus 0.4 per mana), and never more than {@link #MAX_COUNTER}.
	 */
	public static double counter(double weight) {
		return Math.min(MAX_COUNTER, COUNTER * (4 + 0.4 * Math.max(0, weight)));
	}
}
