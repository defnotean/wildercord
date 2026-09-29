package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.List;

/**
 * The limits on the runes of the world that the server enforces (what a burn gives back, how much
 * mana Manaburn can take, when a lingering effect pulses, how often a land gives its rune), pure so
 * they can be tested. {@code cast.ExplorerEffects} and {@code cast.Attunement} apply them.
 */
public final class ExplorerNumbers {
	private ExplorerNumbers() {}

	/** Soulfire: mana given back for each point of damage a burn actually dealt. */
	public static final double SOULFIRE_REFUND = 0.35;
	/** Soulfire: the most mana one cast gives back, however many burns it lit. */
	public static final double SOULFIRE_REFUND_MAX = 5.0;

	/**
	 * The mana a Soulfire burn gives back: a share of the damage it really dealt (nothing for a burn
	 * that was blocked, or that the target shrugged off), never past the cast's {@link #SOULFIRE_REFUND_MAX}.
	 *
	 * @param dealt   the health the burn took
	 * @param already what this cast has given back so far
	 */
	public static double soulfireRefund(double dealt, double already) {
		if (!(dealt > 0)) {
			return 0;
		}
		return Math.max(0, Math.min(dealt * SOULFIRE_REFUND, SOULFIRE_REFUND_MAX - Math.max(0, already)));
	}

	/** Manaburn: the mana a spellcaster loses to one full-strength hit. */
	public static final double MANABURN_DRAIN = 20.0;
	/** Manaburn: the most one cast can take from any one spellcaster. */
	public static final double MANABURN_DRAIN_MAX = 20.0;

	/**
	 * The mana a Manaburn hit takes: {@link #MANABURN_DRAIN} times the hit's power (which carries the
	 * shape's strength, so each of a Barrage's many hits takes little) and the PvP scale, never past
	 * {@link #MANABURN_DRAIN_MAX} from one target over the whole cast.
	 *
	 * @param already what this cast has taken from this target so far
	 */
	public static double manaburnDrain(double power, double pvpScale, double already) {
		double drain = MANABURN_DRAIN * Math.max(0, power) * Math.max(0, pvpScale);
		return Math.max(0, Math.min(drain, MANABURN_DRAIN_MAX - Math.max(0, already)));
	}

	/**
	 * When a lingering effect of {@code ticks} pulses every {@code every} ticks: the first at once, then
	 * one each {@code every}, the last before the effect ends. So five seconds of once a second is five
	 * pulses (0, 20, 40, 60, 80), not six. Always at least one.
	 */
	public static List<Integer> pulses(int ticks, int every) {
		int step = Math.max(1, every);
		List<Integer> out = new ArrayList<>();
		int t = 0;
		do {
			out.add(t);
			t += step;
		} while (t < ticks);
		return out;
	}

	/** Manatide: mana a second while it flows. */
	public static final int MANATIDE_PER_SECOND = 3;
	/** Manatide: the longest one drink flows, however Extend lengthens it: 10 seconds, so 30 mana at most. */
	public static final int MANATIDE_MOST_TICKS = 200;
	/** Manatide: how long a player waits between drinks (from the start of one to the next): a minute. */
	public static final int MANATIDE_WAIT = 1200;

	/**
	 * How long a Manatide of {@code ticks} really flows: never past {@link #MANATIDE_MOST_TICKS}. With four
	 * Extends it ran 160 seconds, nearly 500 mana for 46, and outlasted the minute between drinks, so streams
	 * overlapped.
	 */
	public static int manatideTicks(int ticks) {
		return Math.max(1, Math.min(ticks, MANATIDE_MOST_TICKS));
	}

	/** Attunement: a land gives each player its rune once an in-game day (in game ticks). */
	public static final long ATTUNE_REST = 24000L;

	/**
	 * How long before a land will give its rune again to someone who last attuned there at game time
	 * {@code last} (null for never): 0 when it's ready.
	 */
	public static long attuneRestLeft(Long last, long now) {
		if (last == null || now < last) {
			return 0;
		}
		return Math.max(0, last + ATTUNE_REST - now);
	}
}
