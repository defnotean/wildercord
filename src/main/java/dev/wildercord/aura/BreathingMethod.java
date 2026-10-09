package dev.wildercord.aura;

/**
 * A breathing method: the way a player draws mana into their body, and so what their aura is. It gives the aura its
 * element (for creature affinities and the element reactions, where a blade can set them off) and its colour, and one
 * small passive flavour that grows with each stage.
 *
 * <p>Pure data. The ten built in are {@link BreathingMethods}; add-ons register more through {@code api.AuraApi}. Its name,
 * lore and flavour line are language keys: {@code aura.wildercord.method.<id>} and its {@code .lore} and {@code .flavour}.</p>
 *
 * @param id        the method's id: a plain word for the built-in ones ("ember"), {@code namespace:path} for an add-on's
 * @param element   the element its aura carries (one of the ten, or "" for none)
 * @param color     its aura's colour, 0xRRGGBB
 * @param highlight the colour it burns toward as it grows
 * @param flavour   its passive
 */
public record BreathingMethod(String id, String element, int color, int highlight, Flavour flavour) {
	/** The passives a method can give; {@link AuraRules} has their numbers by stage. */
	public enum Flavour {
		/** Ember: coated blows set foes alight (a chance from Flow, always from Edge). */
		IGNITE,
		/** Rime: coated blows slow. */
		CHILL,
		/** Thunder: a chance to throw a spark to another foe. */
		SPARK,
		/** Gale: a bit of speed while in a fight. */
		GALE,
		/** Stone: knockback resistance. */
		STONE,
		/** Verdant: a coated blow mends a little. */
		MEND,
		/** Hollow: foes near the one struck are drawn toward it. */
		PULL,
		/** Starlit: aura comes faster. */
		STARLIT,
		/** Hourglass: a little attack speed. */
		HASTE,
		/** Crimson: coated blows drink a little, for more aura. */
		LEECH,
		/** An add-on's method with a passive of its own (or none). */
		NONE,
		// ---- methods-a pack
		/** Tide: coated blows push the foe back and soak it. */
		CURRENT,
		/** Iron: coated blows crack armour for a moment. */
		FORGE,
		/** Dune: coated blows may throw grit in the eyes. */
		GRIT
	}

	public BreathingMethod {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("a breathing method needs an id");
		}
		element = element == null ? "" : element;
		flavour = flavour == null ? Flavour.NONE : flavour;
		color &= 0xFFFFFF;
		highlight &= 0xFFFFFF;
	}

	/** Its aura's colour at {@code stage}. */
	public int color(int stage) {
		return AuraRules.color(color, highlight, stage);
	}

	/** The language key of its name ("Ember Breath"). */
	public String nameKey() {
		return "aura.wildercord.method." + id.replace(':', '.');
	}
}
