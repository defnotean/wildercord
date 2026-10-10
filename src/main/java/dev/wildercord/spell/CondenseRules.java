package dev.wildercord.spell;

/**
 * How mana condenses toward the next Heart Circle (0.12 "Tempering"): pure numbers, so the server and the tests agree.
 *
 * <ul>
 * <li>Only a {@link #AIR_SHARE tenth} of mana spent condenses at once. The rest waits, and condenses only if one of the
 * caster's spells hurts a hostile creature within {@link #PENDING_TICKS} ticks: spells cast into the air or at a dummy
 * barely build a heart.</li>
 * <li>Spell kills of monsters count toward breakthroughs, but a farm's steady stream counts for less: past
 * {@link #KILL_FREE} kills inside {@link #KILL_WINDOW_TICKS}, each further kill counts with chance {@link #KILL_FARMED}.</li>
 * <li>A boss's breakthrough goes only to those who dealt it at least {@link #BOSS_SHARE} of its health.</li>
 * <li>Old saves are re-tempered: circles beyond what their condensed mana now pays for are given back ({@link #retemper}).</li>
 * </ul>
 */
public final class CondenseRules {
	private CondenseRules() {}

	/** Share of mana spent that condenses straight away, whatever the spell hit. */
	public static final double AIR_SHARE = 0.1;
	/** How long the rest waits for a spell to hurt a hostile creature. */
	public static final int PENDING_TICKS = 200;
	/** The most mana that can wait at once (a long barrage into the air does not bank a fortune for one later hit). */
	public static final float PENDING_CAP = 600;

	public static final int KILL_WINDOW_TICKS = 1200;
	public static final int KILL_FREE = 12;
	public static final double KILL_FARMED = 0.15;

	public static final double BOSS_SHARE = 0.02;

	/** What of {@code mana} condenses now; the rest is {@link #pending}. */
	public static float immediate(float mana) {
		return !Float.isFinite(mana) || mana <= 0 ? 0 : (float) (mana * AIR_SHARE);
	}

	/** What of {@code mana} waits for a hostile hit. */
	public static float pending(float mana) {
		return !Float.isFinite(mana) || mana <= 0 ? 0 : mana - immediate(mana);
	}

	/** Adds {@code more} to what's waiting, held to {@link #PENDING_CAP}. */
	public static float addPending(float waiting, float more) {
		return Math.min(PENDING_CAP, Math.max(0, waiting) + Math.max(0, more));
	}

	/** Chance a spell kill counts, given how many already counted inside the window. */
	public static double killChance(int recentKills) {
		return recentKills < KILL_FREE ? 1.0 : KILL_FARMED;
	}

	/** Whether {@code dealt} damage on a boss of {@code maxHealth} earns its breakthrough. */
	public static boolean bossCredit(double dealt, double maxHealth) {
		return maxHealth > 0 && dealt >= maxHealth * BOSS_SHARE;
	}

	/** The circles an older save keeps under today's thresholds: the most, up to what it had, its condensed mana pays for. */
	public static int retemper(int circles, int condensed) {
		int n = Circles.count(circles);
		while (n > 0 && condensed < Circles.condenseNeeded(n)) {
			n--;
		}
		return n;
	}
}
