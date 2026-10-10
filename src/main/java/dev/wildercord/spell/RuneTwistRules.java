package dev.wildercord.spell;

import java.util.Locale;

/**
 * Flawed and corrupted runes (0.13), as plain numbers. A twisted rune is an effect rune found in a dungeon chest with a
 * twist cut into it. Learning it twists that rune everywhere it's threaded. A flawed rune is weaker but makes its spell
 * cheaper. A corrupted rune is stronger, at a price.
 */
public final class RuneTwistRules {
	private RuneTwistRules() {}

	public enum Twist {
		/** Weaker, but each flawed rune in a spell takes 15% off its price. */
		FLAWED(0.8, false),
		/** Stronger, and whoever it helps catches fire: a heal that burns. */
		SEARING(1.3, true),
		/** Stronger, and each cast it's part of costs the caster a heart. */
		BLOODLETTING(1.35, true),
		/** Much stronger, but now and then it fizzles where it lands. */
		VOLATILE(1.5, true),
		/** Stronger, and each cast it's part of leaves the caster hungry. */
		HUNGERING(1.3, true);

		/** The factor on the rune's power. */
		public final double power;
		/** Whether it's corrupted (stronger at a price) rather than flawed. */
		public final boolean corrupted;

		Twist(double power, boolean corrupted) {
			this.power = power;
			this.corrupted = corrupted;
		}

		public String id() {
			return name().toLowerCase(Locale.ROOT);
		}

		/** The twist with this id, or null for none or an unknown one. */
		public static Twist byId(String id) {
			if (id == null) return null;
			for (Twist twist : values()) {
				if (twist.id().equals(id)) return twist;
			}
			return null;
		}
	}

	/** The price factor for each flawed rune in a spell, and the least a spell can come down to. */
	public static final double FLAWED_COST = 0.85, FLAWED_FLOOR = 0.5;
	/** The chance a volatile rune fizzles where it lands. */
	public static final double VOLATILE_FIZZLE = 0.15;
	/** What bloodletting takes (health) and hungering takes (exhaustion), once a cast. */
	public static final float BLOOD_TOLL = 2.0F, HUNGER_TOLL = 4.0F;
	/** How long a searing rune sets those it helps alight (ticks). */
	public static final int SEARING_TICKS = 60;
	/** The chance in 100 a dungeon chest holds a twisted rune, and the tiers it's drawn from. */
	public static final int CHEST_CHANCE = 12, MIN_TIER = 1, MAX_TIER = 3;

	/** The factor on a spell's price for {@code flawed} distinct flawed runes in it. */
	public static double costFactor(int flawed) {
		return Math.max(FLAWED_FLOOR, Math.pow(FLAWED_COST, Math.max(0, flawed)));
	}

	/** The power factor of a rune with {@code twist} (null: untwisted). */
	public static double power(Twist twist) {
		return twist == null ? 1.0 : twist.power;
	}

	/** The twist a found rune carries for a {@code roll} in [0, 1): flawed half the time, else one corruption, evenly. */
	public static Twist roll(double roll) {
		if (roll < 0.5) return Twist.FLAWED;
		Twist[] all = Twist.values();
		int corrupted = all.length - 1;
		int pick = Math.min(corrupted - 1, (int) ((roll - 0.5) / 0.5 * corrupted));
		return all[1 + pick];
	}
}
