package dev.wildercord.monster;

/**
 * Biome variants and packs (0.12 "Tempering"), as plain numbers. A Bramblewalker or Gloomstalker born somewhere freezing
 * comes out Frost (tougher, and its blows slow and chill), one born somewhere scorched comes out Ash (fireproof, harder
 * hitting, and its blows burn). Gloomstalkers hunt in packs: one that finds prey calls the rest of its kind nearby onto it,
 * and the first of a spawned group may be its Alpha, bigger and stronger, whose call carries further.
 */
public final class MonsterVariantRules {
	private MonsterVariantRules() {}

	public enum Variant {
		NONE("", 0xFFFFFF),
		FROST("frost", 0xB8DCFF),
		ASH("ash", 0x9A8C84);

		public final String id;
		/** What the model is tinted (RGB). */
		public final int tint;

		Variant(String id, int tint) {
			this.id = id;
			this.tint = tint;
		}

		public static Variant of(int ordinal) {
			return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : NONE;
		}
	}

	/** At or below this base temperature (or wherever it snows) a variant comes out Frost. */
	public static final float FROST_BELOW = 0.15F;
	/** At or above this base temperature (deserts, badlands, savannas, the Nether) it comes out Ash. */
	public static final float ASH_ABOVE = 1.9F;

	/** Frost: health up this share; its blows slow (Slowness I) and chill (frozen ticks) for this long. */
	public static final double FROST_HEALTH = 0.25;
	public static final int FROST_SLOW_TICKS = 60;
	public static final int FROST_CHILL_TICKS = 100;
	/** Ash: damage up this share; its blows set the target alight this many seconds. */
	public static final double ASH_DAMAGE = 0.2;
	public static final float ASH_BURN_SECONDS = 3;

	/** A pack's call: how far a hunter's kin hear it, and an Alpha's. */
	public static final double PACK_RANGE = 16;
	public static final double ALPHA_RANGE = 28;
	/** The chance the first of a pack is its Alpha, and what being one adds (health, damage, size). */
	public static final double ALPHA_CHANCE = 0.5;
	public static final double ALPHA_HEALTH = 0.6;
	public static final double ALPHA_DAMAGE = 0.3;
	public static final double ALPHA_SCALE = 0.2;

	/** The variant a creature born where it's this warm (snowy or not) comes out as. */
	public static Variant of(float temperature, boolean snowy) {
		if (snowy || temperature <= FROST_BELOW) {
			return Variant.FROST;
		}
		if (temperature >= ASH_ABOVE) {
			return Variant.ASH;
		}
		return Variant.NONE;
	}

	/** How far a pack hunter's call carries. */
	public static double callRange(boolean alpha) {
		return alpha ? ALPHA_RANGE : PACK_RANGE;
	}
}
