package dev.wildercord.monster;

/**
 * Roaming overworld giants (0.13), as plain numbers. Now and then a giant wakes somewhere 48 to 96 blocks from a traveller:
 * one of the wilds' own monsters grown three times over, wearing the variant of where it walks. In the cold a Frost
 * Matriarch (a Gloomstalker who leads every pack in the snow), in the heat an Ash Colossus (a Bramblewalker gone to cinders),
 * and anywhere between an Elder Bramblewalker. It wanders far, shakes the ground with a stomp that throws whoever stands
 * close, shows a bar to everyone near it, never despawns, and drops its Giant's Heart when it falls.
 */
public final class GiantRules {
	private GiantRules() {}

	/** The giants there are, by the variant of the land they wake in. */
	public enum Kind {
		/** A Frost Gloomstalker, mother of the snow packs. */
		MATRIARCH(MonsterVariantRules.Variant.FROST),
		/** An Ash Bramblewalker. */
		COLOSSUS(MonsterVariantRules.Variant.ASH),
		/** A plain Bramblewalker, old as the woods. */
		ELDER(MonsterVariantRules.Variant.NONE);

		public final MonsterVariantRules.Variant variant;

		Kind(MonsterVariantRules.Variant variant) {
			this.variant = variant;
		}

		/** Its name's key: {@code monster.wildercord.giant.matriarch}. */
		public String key() {
			return "monster.wildercord.giant." + name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/** The giant that wakes in land of this temperature (snowy or not). */
	public static Kind kind(float temperature, boolean snowy) {
		return switch (MonsterVariantRules.of(temperature, snowy)) {
			case FROST -> Kind.MATRIARCH;
			case ASH -> Kind.COLOSSUS;
			case NONE -> Kind.ELDER;
		};
	}

	/** How often the wilds are checked for a waking giant (ticks), and the chance one wakes each time. */
	public static final int CHECK_TICKS = 20 * 60 * 10;
	public static final double CHANCE = 0.25;
	/** Where it wakes: this far from the traveller. */
	public static final int WAKE_NEAR = 48, WAKE_FAR = 96;
	/** No giant wakes within this many blocks of another. */
	public static final double APART = 192;

	/** How much bigger it is than its kind (added to its scale: three times as big), and how hard it is to shove. */
	public static final double SCALE = 2.0;
	public static final double KNOCKBACK_RESISTANCE = 0.9;
	/** How much tougher it is than a champion of its kind's tempering, and the least threat it's tempered to. */
	public static final double TOUGHNESS = 4.0;
	public static final int THREAT_MIN = 6;

	/** Its stomp: how often (ticks), how far it reaches, what it deals at its feet, and how hard it throws. */
	public static final int STOMP_INTERVAL = 100;
	public static final double STOMP_RADIUS = 6;
	public static final float STOMP_DAMAGE = 10;
	public static final double STOMP_THROW = 1.1;

	/** Whether it stomps on tick {@code now}: only in a fight, each giant on a beat of its own. */
	public static boolean stomps(boolean fighting, long now, int id) {
		return fighting && Math.floorMod(now + id, STOMP_INTERVAL) == 0;
	}

	/** What its stomp deals {@code distance} blocks away: all of it at its feet, half at the edge, nothing past it. */
	public static float stompDamage(double distance) {
		if (distance > STOMP_RADIUS) return 0;
		return (float) (STOMP_DAMAGE * (1 - 0.5 * Math.max(0, distance) / STOMP_RADIUS));
	}

	/** How often an idle giant sets off somewhere new (ticks), and how far it goes. */
	public static final int ROAM_INTERVAL = 600;
	public static final int ROAM_NEAR = 24, ROAM_FAR = 48;

	/** Whether it sets off somewhere new on tick {@code now}: only with nothing to fight. */
	public static boolean roams(boolean fighting, long now, int id) {
		return !fighting && Math.floorMod(now + id, ROAM_INTERVAL) == 0;
	}

	/** Its bar shows to everyone within this many blocks. */
	public static final double BAR_RANGE = 48;

	/** How many Giant's Hearts it drops: one, and a second one time in four. */
	public static int hearts(double roll) {
		return roll < 0.25 ? 2 : 1;
	}
}
