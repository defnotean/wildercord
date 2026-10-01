package dev.wildercord.monster;

import java.util.List;

/**
 * The magical monsters' numbers and decisions, kept free of Minecraft so they're unit-tested: how often each spawns, how
 * long a Bramblewalker's vines hold, when a Gloomstalker shows itself, how much a curled Geode Crawler shrugs off, how a
 * Mana Ooze fills, grows and splits, and the arc a Bog Witch-Frog's bubble flies. The creatures themselves read these.
 */
public final class MonsterRules {
	private MonsterRules() {}

	/**
	 * The six, by their entity ids, with how often each turns up among a biome's other monsters and in what numbers. A
	 * zombie, a skeleton, a spider and a creeper each weigh about 100, so a weight of 10 is one spawn in fifty or so where it
	 * lives: a meeting now and then, never a crowd.
	 */
	public enum Kind {
		BRAMBLEWALKER("bramblewalker", 10, 1, 2),
		GLOOMSTALKER("gloomstalker", 8, 1, 1),
		THUNDERWING_HARPY("thunderwing_harpy", 10, 1, 2),
		GEODE_CRAWLER("geode_crawler", 10, 1, 2),
		BOG_WITCH_FROG("bog_witch_frog", 14, 1, 1),
		MANA_OOZE("mana_ooze", 7, 1, 2);

		public final String id;
		public final int weight;
		public final int min;
		public final int max;

		Kind(String id, int weight, int min, int max) {
			this.id = id;
			this.weight = weight;
			this.min = min;
			this.max = max;
		}
	}

	public static final List<Kind> ALL = List.of(Kind.values());

	// ------------------------------------------------------------------ spawning

	/** A spawn weight scaled by the server's {@code monsters.spawn_rate}: nothing at 0, never rounded away to nothing above it. */
	public static int weight(int base, double rate) {
		if (!(rate > 0) || base <= 0) {
			return 0;
		}
		return (int) Math.max(1, Math.round(base * Math.min(rate, 4.0)));
	}

	/** The deepest a Thunderwing Harpy's peak can be: they spawn only this high, where the mountains are bare rock and snow. */
	public static final int HARPY_MIN_Y = 90;
	/** Gloomstalkers and Mana Oozes come up no higher than this underground (the deep caves and the deep dark's edges). */
	public static final int DEEP_Y = 0;
	/** On a ley line, a Mana Ooze comes up this far. */
	public static final int LEY_OOZE_Y = 40;
	/** Geode Crawlers live below this. */
	public static final int CRAWLER_Y = 50;
	/** Away from amethyst, only this share of a Geode Crawler's spawns go ahead: most are found near geodes. */
	public static final double CRAWLER_AWAY_FROM_GEODES = 0.25;

	// ------------------------------------------------------------------ the Bramblewalker

	/** How long its vines hold, by difficulty (1 Easy, 2 Normal, 3 Hard): a breath on Easy, two seconds on Hard. */
	public static int rootTicks(int difficulty) {
		return switch (difficulty) {
			case 0, 1 -> 20;
			case 2 -> 30;
			default -> 40;
		};
	}

	/** How long it runs once the flames on it go out, still remembering them. */
	public static final int FLEE_AFTER_FIRE = 40;

	// ------------------------------------------------------------------ the Gloomstalker

	/** Light (0-15) from which a Gloomstalker can't hide: a torch's glow a few blocks off, or day. */
	public static final int REVEAL_LIGHT = 8;
	/** Within this many blocks of a player, it shows. */
	public static final double REVEAL_NEAR = 3.0;
	/** How long a light-bringing spell keeps it in view, in ticks. */
	public static final int SPELL_REVEAL = 120;
	/** How long any wound keeps it in view. */
	public static final int HURT_REVEAL = 40;

	/** Whether a Gloomstalker hides in the dark: unlit, unmarked, unhurt lately, nobody close, and not mid-leap. */
	public static boolean veiled(int light, boolean glowing, boolean revealed, double nearestPlayer, boolean acting) {
		return light < REVEAL_LIGHT && !glowing && !revealed && nearestPlayer > REVEAL_NEAR && !acting;
	}

	/** The elements whose light shows a Gloomstalker: fire's flame, storm's flash, arcane's starlight and life's bloom. */
	public static boolean revealing(String element) {
		return element.equals("fire") || element.equals("storm") || element.equals("arcane") || element.equals("life");
	}

	/** How opaque a Gloomstalker draws at a veil of 0 (in plain view) to 1 (hidden): never quite gone, a ripple in the dark. */
	public static float opacity(float veil) {
		return 1.0F - 0.88F * Math.max(0, Math.min(1, veil));
	}

	// ------------------------------------------------------------------ the Geode Crawler

	/** The share of a blow that reaches a curled Geode Crawler, unless it's the kind that cracks crystal. */
	public static final float CURLED_SHARE = 0.2F;
	/** A cracking blow (a mace, a pickaxe, a blast, a shock) lands a little harder than it would have, and breaks the curl. */
	public static final float CRACKING_SHARE = 1.25F;

	/** What a blow does to a curled Geode Crawler. */
	public static float curled(float amount, boolean cracking) {
		return amount * (cracking ? CRACKING_SHARE : CURLED_SHARE);
	}

	// ------------------------------------------------------------------ the Mana Ooze

	/** The biggest a Mana Ooze grows; full at this size, it splits. */
	public static final int MAX_OOZE = 4;

	/** A Mana Ooze's health by size. */
	public static int oozeHealth(int size) {
		return switch (Math.max(1, Math.min(MAX_OOZE, size))) {
			case 1 -> 6;
			case 2 -> 14;
			case 3 -> 24;
			default -> 36;
		};
	}

	/** How much spell damage it drinks before it grows (or, at its biggest, splits). */
	public static float oozeCapacity(int size) {
		return 8.0F * Math.max(1, size);
	}

	/** A Mana Ooze after drinking a spell: its size, how full it is now, and whether it burst into two. */
	public record Feed(int size, float fullness, boolean split) {}

	/**
	 * Drinks {@code amount} of spell damage: it fills, and full it grows a size (healed, and empty again); full at its
	 * biggest, it splits in two (the halves start empty).
	 */
	public static Feed feed(int size, float fullness, float amount) {
		int s = Math.max(1, Math.min(MAX_OOZE, size));
		float full = Math.max(0, fullness) + Math.max(0, amount);
		if (full < oozeCapacity(s)) {
			return new Feed(s, full, false);
		}
		if (s < MAX_OOZE) {
			return new Feed(s + 1, 0, false);
		}
		return new Feed(MAX_OOZE / 2, 0, true);
	}

	// ------------------------------------------------------------------ the Bog Witch-Frog's bubble

	/** How hard the world pulls a bubble down each tick. */
	public static final double BUBBLE_GRAVITY = 0.03;

	/** Ticks a bubble takes to reach something {@code distance} blocks away: slow enough to see coming, and to shoot down. */
	public static int bubbleFlight(double distance) {
		return (int) Math.max(12, Math.min(34, Math.round(distance / 0.5)));
	}

	/**
	 * The velocity that carries a bubble (moving {@code v} each tick, then pulled down by {@code gravity}) exactly
	 * {@code dx, dy, dz} in {@code ticks} ticks.
	 */
	public static double[] lob(double dx, double dy, double dz, double gravity, int ticks) {
		int t = Math.max(1, ticks);
		double vy = (dy + gravity * t * (t - 1) / 2.0) / t;
		return new double[] {dx / t, vy, dz / t};
	}
}
