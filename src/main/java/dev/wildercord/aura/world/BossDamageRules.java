package dev.wildercord.aura.world;

/**
 * How much harder a boss hits a player than its own numbers say: full diamond with Protection IV used to turn a boss's blow into
 * half a heart, so every boss's hit on a player lands {@link #MULTIPLIER} times as hard and goes through at least {@link #PIERCE}
 * of what armour, Protection and Resistance would stop.
 */
public final class BossDamageRules {
	private BossDamageRules(){}
	public static final float MULTIPLIER = 1.35f;
	public static final double PIERCE = 0.4;

	/** A boss's blow of {@code damage} on a player, before any defence. */
	public static float scaled(float damage) {
		return damage * MULTIPLIER;
	}

	/** The share of a defence a blow goes through: a boss's at least {@link #PIERCE}, whatever its own move already pierces. */
	public static double share(boolean boss, double own) {
		return boss ? Math.max(own, PIERCE) : own;
	}
}
