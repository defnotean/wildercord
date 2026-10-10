package dev.wildercord.monster;

/**
 * How the world answers the players in it: a hostile creature that comes into being near players is tempered to the strongest of
 * them, by their circles and aura stage ({@link #threat}). It has more health and hits harder, sometimes it's an elite, and a
 * boss grows with the party that comes for it. {@link Tempering} applies these.
 */
public final class TemperingRules {
	private TemperingRules() {}

	/** How far (blocks) a player's strength reaches the creatures that appear near them. */
	public static final double REACH = 64;
	/** How far (blocks) the players a boss counts as its challengers stand. */
	public static final double PARTY_REACH = 48;
	/** The most challengers a boss grows for. */
	public static final int PARTY_MAX = 6;
	/** Each aura stage weighs as much as this many circles. */
	public static final int STAGE_WEIGHT = 3;
	/** The highest threat anything is tempered to (circle 20 and Sovereign: 32). */
	public static final int MAX_THREAT = 32;

	public static final double HEALTH_PER_THREAT = 0.10;
	public static final double HEALTH_MAX = 4.0;
	public static final double DAMAGE_PER_THREAT = 0.05;
	public static final double DAMAGE_MAX = 2.5;
	/** Below this threat nothing is ever an elite: the first hours are the world as it was. */
	public static final int ELITE_FROM = 4;
	public static final double ELITE_BASE = 0.02;
	public static final double ELITE_PER_THREAT = 0.006;
	public static final double ELITE_MAX = 0.20;
	/** An elite's health on top of its tempering. */
	public static final double ELITE_HEALTH = 1.5;
	/** Each extra challenger adds this much of a boss's health. */
	public static final double BOSS_PER_PLAYER = 0.5;

	public static final double SWIFT_SPEED = 0.35;
	public static final double IRONHIDE_ARMOUR = 10;
	public static final double IRONHIDE_TOUGHNESS = 4;
	public static final double VAMPIRIC_DRAIN = 0.5;
	/** How many lesser copies a Splitting elite breaks into, and the share of its kind's health each keeps. */
	public static final int SPLIT_COUNT = 2;
	/** A boss with no phases of its own enrages once, at this share of its health: faster, harder, and briefly shielded. */
	public static final double ENRAGE_AT = 0.5;
	public static final double ENRAGE_SPEED = 0.25;
	public static final double ENRAGE_DAMAGE = 0.4;
	/** The shield it raises as it enrages, as a share of its health. */
	public static final double ENRAGE_SHIELD = 0.15;

	/** Whether a boss at {@code health} of {@code max} has fallen far enough to enrage. */
	public static boolean enrages(double health, double max) {
		return max > 0 && health > 0 && health <= max * ENRAGE_AT;
	}
	public static final double SPLIT_HEALTH = 0.5;
	public static final double BRUTAL_DAMAGE = 1.5;
	public static final double BRUTAL_KNOCKBACK = 1.0;

	/** The threat a player with {@code circles} and aura {@code stage} carries. */
	public static int threat(int circles, int stage) {
		return (int) Math.clamp((long) Math.max(0, circles) + (long) STAGE_WEIGHT * Math.max(0, stage), 0, MAX_THREAT);
	}

	public static double health(int threat) {
		return Math.min(HEALTH_MAX, 1 + HEALTH_PER_THREAT * Math.clamp(threat, 0, MAX_THREAT));
	}

	public static double damage(int threat) {
		return Math.min(DAMAGE_MAX, 1 + DAMAGE_PER_THREAT * Math.clamp(threat, 0, MAX_THREAT));
	}

	public static double eliteChance(int threat) {
		return threat < ELITE_FROM ? 0 : Math.min(ELITE_MAX, ELITE_BASE + ELITE_PER_THREAT * threat);
	}

	/** A boss's health for {@code players} challengers (one is the fight as built). */
	public static double bossParty(int players) {
		return 1 + BOSS_PER_PLAYER * (Math.clamp(players, 1, PARTY_MAX) - 1);
	}

	/** Experience an elite leaves beyond its own. */
	public static int eliteExperience(int threat) {
		return 10 + Math.clamp(threat, 0, MAX_THREAT);
	}

	/** The kinds of elite. */
	public enum Elite {
		/** Quick on its feet: a third faster. */
		SWIFT("Swift", 0x7FE0FF),
		/** Hide like iron: heavy armour. */
		IRONHIDE("Ironhide", 0xB8B8C8),
		/** Drinks what it draws: heals half the damage it deals. */
		VAMPIRIC("Vampiric", 0xC0203A),
		/** Hits like a falling tree: half again the damage, and it throws you. */
		BRUTAL("Brutal", 0xFF8A30),
		/** Falls apart into lesser copies of itself when it dies. */
		SPLITTING("Splitting", 0x8CE06A);

		public final String title;
		public final int color;

		Elite(String title, int color) {
			this.title = title;
			this.color = color;
		}

		public String key() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}

		public String tag() {
			return Tempering.ELITE_TAG + key();
		}
	}
}
