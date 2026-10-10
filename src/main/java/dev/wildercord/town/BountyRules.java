package dev.wildercord.town;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Bounties and reputation at a Wayfarer Inn (0.12 "Tempering"), as plain numbers. The inn's bounty board offers each traveller
 * one hunt a day ("slay 5 Gloomstalkers within 256 blocks of here"); kills count only near the board it was taken from. Turned
 * in, it pays emeralds and reputation with the inn's keepers, and reputation opens their better trades tier by tier.
 */
public final class BountyRules {
	private BountyRules() {}

	/** A creature a bounty can name: how many it asks for, and the extra emeralds a harder kill adds. */
	public record Target(String id, int min, int max, int bonus) {}

	public static final List<Target> TARGETS = List.of(
		new Target("wildercord:bramblewalker", 3, 6, 1),
		new Target("wildercord:gloomstalker", 3, 5, 2),
		new Target("wildercord:geode_crawler", 3, 6, 1),
		new Target("wildercord:mana_ooze", 4, 8, 0),
		new Target("wildercord:bog_witch_frog", 3, 5, 1),
		new Target("wildercord:thunderwing_harpy", 3, 4, 2),
		new Target("minecraft:zombie", 5, 8, 0),
		new Target("minecraft:skeleton", 4, 8, 0),
		new Target("minecraft:spider", 4, 8, 0),
		new Target("minecraft:creeper", 3, 6, 1));

	/** How far from its board a bounty's kills count (blocks, across the ground). */
	public static final double RANGE = 256;
	/** What a bounty pays, at most. */
	public static final int EMERALDS_MAX = 8, REPUTATION_MIN = 5, REPUTATION_MAX = 10;
	/** How long a shown offer can be taken by clicking the board again (ticks). */
	public static final int ACCEPT_TICKS = 200;
	/** One bounty turned in per traveller per day (in ticks). */
	public static final long DAY = 24000;

	/** How well the keepers know you; each tier opens more of their wares. */
	public enum Tier {
		STRANGER("stranger", 0),
		KNOWN("known", 15),
		FRIEND("friend", 40),
		HONOURED("honoured", 80);

		public final String id;
		public final int needs;

		Tier(String id, int needs) {
			this.id = id;
			this.needs = needs;
		}

		public static Tier of(int reputation) {
			Tier tier = STRANGER;
			for (Tier t : values()) {
				if (reputation >= t.needs) tier = t;
			}
			return tier;
		}

		/** The next tier up, or null at the top. */
		public Tier next() {
			return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
		}
	}

	/** An offered bounty: kill {@code needed} of {@code target}, for emeralds and reputation. */
	public record Bounty(String target, int needed, int emeralds, int reputation) {}

	/** The bounty a board shows this traveller today: the same all day, different at each board. */
	public static Bounty offer(UUID player, long day, long boardPos) {
		Random random = new Random(seed(player, day, boardPos));
		Target target = TARGETS.get(random.nextInt(TARGETS.size()));
		int needed = target.min() + random.nextInt(target.max() - target.min() + 1);
		int emeralds = Math.min(EMERALDS_MAX, needed + target.bonus());
		int reputation = Math.min(REPUTATION_MAX, REPUTATION_MIN + (needed - 3) + target.bonus());
		return new Bounty(target.id(), needed, emeralds, reputation);
	}

	static long seed(UUID player, long day, long boardPos) {
		return player.getMostSignificantBits() * 31 ^ player.getLeastSignificantBits() ^ day * 0x9E3779B97F4A7C15L ^ boardPos * 0xC2B2AE3D27D4EB4FL;
	}

	/** Whether a kill this far (across the ground) from the board counts. */
	public static boolean near(double dx, double dz) {
		return dx * dx + dz * dz <= RANGE * RANGE;
	}

	/** Whether the traveller can take another bounty today, having last turned one in on {@code lastDay} (-1 for never). */
	public static boolean ready(long lastDay, long today) {
		return lastDay < today;
	}
}
