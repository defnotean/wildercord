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
	/** Moonpetal's strength under the moon: 1.4 at full moon (phase 0), 1.0 at the quarters, 0.7 at new moon; and 25% more at night under open sky. */
	public static double moonFactor(int phase, boolean nightUnderSky) {
		double[] byPhase = {1.4, 1.2, 1.0, 0.85, 0.7, 0.85, 1.0, 1.2};
		return byPhase[Math.floorMod(phase, 8)] * (nightUnderSky ? 1.25 : 1.0);
	}

	/** The boon an ailment turns into, or null (Remedy). */
	public static <T> T boonFor(T ailment, java.util.Map<T, T> pairs) {
		return pairs.get(ailment);
	}

	/** A Manatide gives back this share of every spell cast under it, and 30 mana at most. */
	public static final double MANATIDE_SHARE = 0.25;
	public static final int MANATIDE_MOST = 30;

	/** What a Manatide returns for a spell that cost {@code spent}, given {@code already} returned: a quarter, never past the most. */
	public static double manatideRefund(double spent, double already) {
		return Math.max(0, Math.min(spent * MANATIDE_SHARE, MANATIDE_MOST - already));
	}

	/**
	 * How long a Manatide of {@code ticks} really flows: never past {@link #MANATIDE_MOST_TICKS}. With four
	 * Extends it ran 160 seconds, nearly 500 mana for 46, and outlasted the minute between drinks, so streams
	 * overlapped.
	 */
	public static int manatideTicks(int ticks) {
		return Math.max(1, Math.min(ticks, MANATIDE_MOST_TICKS));
	}

	/** Tidehook: the tugs that reel a target in, and the ticks between them. */
	public static final int TIDEHOOK_TUGS = 3;
	public static final int TIDEHOOK_TUG_EVERY = 7;
	/** Tidehook: how close to the caster (in blocks, across the ground) the reel stops pulling. */
	public static final double TIDEHOOK_REST = 2.0;
	/** Tidehook: each tug's hop, so a target on the ground leaves it (and its friction) like a fish flapping on a line. */
	public static final double TIDEHOOK_LIFT = 0.3;
	/** Tidehook: the hardest one tug pulls (blocks a tick across the ground). */
	public static final double TIDEHOOK_TUG_MAX = 1.0;

	/**
	 * How hard one Tidehook tug pulls a target {@code distance} blocks from the caster (across the ground): nothing
	 * once it's within {@link #TIDEHOOK_REST}, harder the further out it is, never past {@link #TIDEHOOK_TUG_MAX}. A
	 * target ten blocks out comes in about five, then three, then the last one or two.
	 */
	public static double tidehookTug(double distance) {
		if (!(distance > TIDEHOOK_REST)) {
			return 0;
		}
		return Math.min(TIDEHOOK_TUG_MAX, 0.2 + (distance - TIDEHOOK_REST) * 0.13);
	}

	/** Current: how long the current holds its rider at speed, in ticks. */
	public static final int CURRENT_TICKS = 6;
	/** Current: the rider's speed while it holds them, in blocks a tick, at normal power. */
	public static final double CURRENT_SPEED = 1.5;
	/** Current: the longest the rider is kept from fall damage waiting to land, in ticks. */
	public static final int CURRENT_GUARD_TICKS = 100;
	/**
	 * Current: how long the rider must stay in water to count as come down, in ticks: long enough that a surge up out
	 * of the sea has left it (and would otherwise land on the shore with its fall damage back).
	 */
	public static final int CURRENT_SETTLE_TICKS = 10;

	/**
	 * Current's speed at {@code power}: the square root of the power (so Amplify carries further, but not half as
	 * far again), between 0.6 and 1.6 times {@link #CURRENT_SPEED}. At normal power it carries about 15 blocks: 9
	 * while it holds, and the rest as the water or the air slows the rider.
	 */
	public static double currentSpeed(double power) {
		return CURRENT_SPEED * Math.max(0.6, Math.min(1.6, Math.sqrt(Math.max(0, power))));
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
