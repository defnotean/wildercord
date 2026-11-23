package dev.wildercord.wildlife;

/**
 * Pure domain rules and geometry helpers for the forest White Rabbit and its Hollow Ancient Tree
 * burrow portal (random cave, treasure vault, or all bosses at once). Kept free of Minecraft types
 * so it can be unit tested directly.
 */
public final class WhiteRabbitRules {
	private WhiteRabbitRules() {}

	/** Entity scoreboard tag applied to bosses summoned by a White Rabbit boss-rush portal. */
	public static final String BOSS_RUSH_TAG = "wildercord_rabbit_boss_rush";

	/** Notice and guiding distances (in blocks). */
	public static final double NOTICE_RANGE = 14.0;
	public static final double LEAD_HOP_RANGE = 7.0;
	public static final double WAIT_FOR_PLAYER_RANGE = 10.5;
	public static final double ARRIVAL_RANGE = 3.2;
	public static final double DIVE_PLAYER_RANGE = 11.0;

	/** Distance range from the rabbit where its Hollow Tree spawns. */
	public static final int TREE_MIN_DIST = 16;
	public static final int TREE_MAX_DIST = 30;

	/** Minimum cooldown between portal hops for a player (2 seconds). */
	public static final long PORTAL_COOLDOWN_TICKS = 40L;

	/** Total number of distinct Wildercord bosses summoned in the ALL_BOSSES outcome. */
	public static final int ALL_BOSSES_COUNT = 17;

	/** Possible destinations behind a Hollow Tree's burrow portal. */
	public enum Outcome {
		RANDOM_CAVE("random_cave"),
		TREASURE_VAULT("treasure_vault"),
		ALL_BOSSES("all_bosses");

		private final String id;

		Outcome(String id) {
			this.id = id;
		}

		public String id() {
			return id;
		}

		public static Outcome fromId(String id) {
			for (Outcome value : values()) {
				if (value.id.equals(id)) {
					return value;
				}
			}
			return RANDOM_CAVE;
		}
	}

	/** Weighted outcome roll in [0.0, 1.0]: 40% random cave, 35% treasure vault, 25% all bosses. */
	public static Outcome rollOutcome(double unitRoll) {
		double r = Math.clamp(unitRoll, 0.0, 1.0);
		if (r < 0.40) {
			return Outcome.RANDOM_CAVE;
		}
		if (r < 0.75) {
			return Outcome.TREASURE_VAULT;
		}
		return Outcome.ALL_BOSSES;
	}

	/** Whether the rabbit should keep bounding toward the tree (close enough to guide, not yet there). */
	public static boolean shouldAdvance(double playerDist, double treeDist) {
		return treeDist > ARRIVAL_RANGE && playerDist <= WAIT_FOR_PLAYER_RANGE;
	}

	/** Whether the rabbit should pause beside a bush/path and look back for the lagging player. */
	public static boolean shouldWaitForPlayer(double playerDist, double treeDist) {
		return treeDist > ARRIVAL_RANGE && playerDist > WAIT_FOR_PLAYER_RANGE;
	}

	/** Whether the rabbit has reached the hole and the follower is close enough to watch it dive in. */
	public static boolean shouldDiveIn(double playerDist, double treeDist) {
		return treeDist <= ARRIVAL_RANGE && playerDist <= DIVE_PLAYER_RANGE;
	}

	/** Computes [dx, dz] rounded ring coordinates around the arena center for evenly spacing bosses. */
	public static int[] bossRingOffset(int index, int total, double radius) {
		if (total <= 0) {
			return new int[] { 0, 0 };
		}
		double angle = (Math.PI * 2.0 * index) / total;
		int dx = (int) Math.round(Math.cos(angle) * radius);
		int dz = (int) Math.round(Math.sin(angle) * radius);
		return new int[] { dx, dz };
	}
}
