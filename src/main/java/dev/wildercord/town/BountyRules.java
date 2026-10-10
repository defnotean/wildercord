package dev.wildercord.town;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Bounties and reputation at a Wayfarer Inn (0.12 "Tempering"), as plain numbers. The inn's bounty board offers each traveller
 * one bounty a day; turned in, it pays emeralds and reputation with the inn's keepers, and reputation opens their better trades
 * tier by tier. A stranger is offered hunts ("slay 5 Gloomstalkers within 256 blocks of here"); the better the keepers know you,
 * the more kinds open (0.13): gathering, escorting a traveller's pack llama, a named elite set loose near the board, a dungeon's
 * guardian, and once a week a great hunt for a great reward.
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

	/** Something a gathering bounty can ask for, and how many. */
	public record Goods(String id, int min, int max) {}

	public static final List<Goods> GOODS = List.of(
		new Goods("minecraft:leather", 6, 10),
		new Goods("minecraft:string", 8, 16),
		new Goods("minecraft:bone", 8, 16),
		new Goods("minecraft:gunpowder", 4, 8),
		new Goods("minecraft:spider_eye", 4, 8),
		new Goods("minecraft:feather", 8, 16),
		new Goods("minecraft:rabbit_hide", 4, 8),
		new Goods("minecraft:honeycomb", 3, 6),
		new Goods("minecraft:amethyst_shard", 6, 12),
		new Goods("minecraft:sweet_berries", 16, 32));

	/** The creatures a named elite can be: Wildercord's own monsters. */
	public static final List<String> ELITES = List.of("wildercord:bramblewalker", "wildercord:gloomstalker", "wildercord:geode_crawler",
		"wildercord:bog_witch_frog", "wildercord:thunderwing_harpy");
	/** The first halves and second halves of a named elite's name. */
	public static final List<String> NAME_FRONT = List.of("Grim", "Ash", "Thorn", "Hollow", "Rot", "Iron", "Black", "Moss", "Cinder", "Gloam");
	public static final List<String> NAME_BACK = List.of("jaw", "maw", "hide", "claw", "eye", "fang", "back", "tooth", "heart", "spine");
	/** How much tougher a named elite is than a tempered creature of its kind. */
	public static final double ELITE_HEALTH = 3.0;
	/** How far from the board a named elite is set loose (blocks). */
	public static final int ELITE_NEAR = 48, ELITE_FAR = 96;
	/** The threat a named elite is tempered to, at the least. */
	public static final int ELITE_THREAT_MIN = 6;

	/** A great hunt: how many it asks for, and what it pays. */
	public static final int GREAT_MIN = 12, GREAT_MAX = 20, GREAT_EMERALDS = 24, GREAT_REPUTATION = 20, GREAT_CRYSTALS = 2;
	/** A week, in days: one great hunt each. */
	public static final long WEEK = 7;

	/** What a bounty asks of you. */
	public enum Kind {
		/** Slay so many of one creature near the board. */
		HUNT("hunt", Tier.STRANGER),
		/** Bring the board so many of one thing. */
		GATHER("gather", Tier.KNOWN),
		/** Hunt down one named elite, set loose near the board. */
		ELITE("elite", Tier.FRIEND),
		/** Slay a dungeon's guardian, wherever it waits. */
		DUNGEON("dungeon", Tier.HONOURED),
		/** Once a week: a great hunt, for a great reward. */
		GREAT("great", Tier.KNOWN),
		/** Lead a traveller's pack llama safely to where they're bound. */
		ESCORT("escort", Tier.KNOWN);

		public final String id;
		/** The standing it's first offered at. */
		public final Tier opens;

		Kind(String id, Tier opens) {
			this.id = id;
			this.opens = opens;
		}

		public static Kind of(String id) {
			for (Kind kind : values()) {
				if (kind.id.equals(id)) return kind;
			}
			return HUNT;
		}
	}

	/** The travellers whose pack llamas an escort leads, and how far they're bound (blocks from the board). */
	public static final List<String> TRAVELLERS = List.of("Old Maren", "Tobin Reed", "Sister Ivy", "Hale the Tinker", "Wenna Brook",
		"Corin Ashford", "Pell the Peddler", "Yara Dunmore");
	public static final int ESCORT_NEAR = 160, ESCORT_FAR = 240;
	/** What an escort pays: emeralds for each this many blocks of road, on top of a base. */
	public static final int ESCORT_EMERALDS = 6, ESCORT_BLOCKS_PER_EMERALD = 40, ESCORT_REPUTATION = 10;
	/** What an escort names as its target: the pack llama. */
	public static final String ESCORT_TARGET = "minecraft:llama";

	/** What a dungeon bounty names as its target. */
	public static final String DUNGEON_GUARDIAN = "dungeon_guardian";
	/** What an elite bounty and a dungeon bounty pay. */
	public static final int ELITE_EMERALDS = 14, ELITE_REPUTATION = 12, DUNGEON_EMERALDS = 20, DUNGEON_REPUTATION = 15;

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

	/**
	 * An offered bounty: {@code needed} of {@code target} (a creature, or for a gathering an item), for emeralds and reputation.
	 * A named elite's {@code name} is what it's called, and an escort's is the traveller's (its {@code needed} is how many blocks
	 * of road); other kinds have none.
	 */
	public record Bounty(Kind kind, String target, int needed, int emeralds, int reputation, String name) {
		public Bounty(String target, int needed, int emeralds, int reputation) {
			this(Kind.HUNT, target, needed, emeralds, reputation, "");
		}
	}

	/** The week a day falls in. */
	public static long week(long day) {
		return Math.floorDiv(day, WEEK);
	}

	/**
	 * The bounty a board shows this traveller today: the same all day, different at each board. A traveller of {@code tier}
	 * who hasn't had this week's great hunt ({@code greatDue}) is offered that first.
	 */
	public static Bounty offer(UUID player, long day, long boardPos, Tier tier, boolean greatDue) {
		Random random = new Random(seed(player, day, boardPos) ^ 0x5DEECE66DL);
		if (greatDue && tier.ordinal() >= Kind.GREAT.opens.ordinal()) {
			Target target = TARGETS.get(random.nextInt(TARGETS.size()));
			return new Bounty(Kind.GREAT, target.id(), GREAT_MIN + random.nextInt(GREAT_MAX - GREAT_MIN + 1), GREAT_EMERALDS, GREAT_REPUTATION, "");
		}
		List<Kind> open = new java.util.ArrayList<>();
		for (Kind kind : Kind.values()) {
			if (kind != Kind.GREAT && tier.ordinal() >= kind.opens.ordinal()) open.add(kind);
		}
		// Hunts stay the common bounty; each further kind opened is offered a little less often.
		int[] weight = {6, 3, 2, 1, 0, 2};
		int total = 0;
		for (Kind kind : open) total += weight[kind.ordinal()];
		int roll = random.nextInt(total);
		Kind kind = Kind.HUNT;
		for (Kind k : open) {
			roll -= weight[k.ordinal()];
			if (roll < 0) {
				kind = k;
				break;
			}
		}
		return switch (kind) {
			case GATHER -> {
				Goods goods = GOODS.get(random.nextInt(GOODS.size()));
				int needed = goods.min() + random.nextInt(goods.max() - goods.min() + 1);
				int share = (needed - goods.min()) * 3 / Math.max(1, goods.max() - goods.min());
				yield new Bounty(Kind.GATHER, goods.id(), needed, 4 + share, REPUTATION_MIN + 1 + share, "");
			}
			case ELITE -> new Bounty(Kind.ELITE, ELITES.get(random.nextInt(ELITES.size())), 1, ELITE_EMERALDS, ELITE_REPUTATION,
				NAME_FRONT.get(random.nextInt(NAME_FRONT.size())) + NAME_BACK.get(random.nextInt(NAME_BACK.size())));
			case DUNGEON -> new Bounty(Kind.DUNGEON, DUNGEON_GUARDIAN, 1, DUNGEON_EMERALDS, DUNGEON_REPUTATION, "");
			case ESCORT -> {
				int distance = ESCORT_NEAR + random.nextInt(ESCORT_FAR - ESCORT_NEAR + 1);
				yield new Bounty(Kind.ESCORT, ESCORT_TARGET, distance, ESCORT_EMERALDS + distance / ESCORT_BLOCKS_PER_EMERALD, ESCORT_REPUTATION,
					TRAVELLERS.get(random.nextInt(TRAVELLERS.size())));
			}
			default -> offer(player, day, boardPos);
		};
	}

	/** The hunt a board would show this traveller today. */
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

	/** Whether a traveller who last had a great hunt in {@code lastWeek} (-1 for never) is due one on {@code today}. */
	public static boolean greatDue(long lastWeek, long today) {
		return lastWeek < week(today);
	}

	/** Whether the traveller can take another bounty today, having last turned one in on {@code lastDay} (-1 for never). */
	public static boolean ready(long lastDay, long today) {
		return lastDay < today;
	}
}
