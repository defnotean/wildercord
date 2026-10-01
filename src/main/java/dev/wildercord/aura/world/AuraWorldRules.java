package dev.wildercord.aura.world;

import dev.wildercord.aura.AuraRules;
import dev.wildercord.cast.SpellDefenceRules;
import dev.wildercord.spell.Parry;

/**
 * The world of aura, the pure part: the wandering duelists who teach breathing methods, the fallen knights who haunt old
 * places, the aura-forged gear, and the clash of two slashes in the air. Every number a duel, a knight or a forged piece
 * uses is here, so the server, the tests and the guide agree; the server owner's {@code aura_world} settings change the
 * main ones.
 *
 * <p>Tuned against a player of the same stage: a duelist scales its stage to its challenger's and fights with what that
 * stage brings (the coat, the guard, the slash, the dash once Aura Step exists), with every attack telegraphed long enough
 * to answer. A knight is a dungeon's threat rather than a teacher: it hits harder, guards more and slashes from further, and
 * its rank follows where it haunts.</p>
 */
public final class AuraWorldRules {
	private AuraWorldRules() {}

	// ------------------------------------------------------------------ duelists

	/**
	 * The stage a duelist fights at against a challenger at {@code playerStage}: the same, never below Glow (a challenger
	 * without a method meets a Glow duelist), never above the highest stage open on this server.
	 */
	public static int duelStage(int playerStage, int highestOpen) {
		int top = Math.max(AuraRules.GLOW, Math.min(AuraRules.MAX_STAGE, highestOpen));
		return Math.max(AuraRules.GLOW, Math.min(top, playerStage));
	}

	/** A duelist's health at a stage: 30 at Glow, ten more a stage. */
	public static double duelistHealth(int stage) {
		return 30 + 10 * (AuraRules.clampStage(Math.max(AuraRules.GLOW, stage)) - 1);
	}

	/** Ticks between a duelist's blows: a little quicker at each stage. */
	public static int duelistAttackTicks(int stage) {
		return 28 - 2 * Math.max(AuraRules.GLOW, AuraRules.clampStage(stage));
	}

	/** A duelist yields once a blow would leave it this share of its health or less. It never dies in a duel. */
	public static final double YIELD_SHARE = 0.15;

	/** The health a duelist yields at. */
	public static double yieldHealth(double maxHealth) {
		return Math.max(1, maxHealth * YIELD_SHARE);
	}

	/** Whether a blow of {@code damage} on a duelist at {@code health} makes it yield. */
	public static boolean yields(double health, double damage, double maxHealth) {
		return health - Math.max(0, damage) <= yieldHealth(maxHealth) + 1.0E-6;
	}

	/** A duelist's slash as a share of its weapon's damage (a player's is 1.2, and goes through no spell defences of the duelist's). */
	public static double duelistSlashFactor(int stage) {
		return stage >= AuraRules.SOVEREIGN ? 1.0 : stage >= AuraRules.FORM ? 0.9 : 0.8;
	}

	/** How long a duelist holds its blade high before a slash (the tell): shorter at higher stages. */
	public static int duelistSlashWindup(int stage) {
		return Math.max(12, 18 - 2 * (Math.max(AuraRules.EDGE, stage) - AuraRules.EDGE));
	}

	/** Ticks between a duelist's slashes. */
	public static int duelistSlashCooldown(int stage) {
		return stage >= AuraRules.SOVEREIGN ? 70 : stage >= AuraRules.FORM ? 85 : 100;
	}

	/** A duelist's crescent: slower than a player's, so it can be stepped aside from. */
	public static final double DUELIST_SLASH_SPEED = 1.3;
	public static final double DUELIST_SLASH_RANGE = 12.0;
	public static final double DUELIST_SLASH_WIDTH = 2.6;

	/** The chance a duelist raises its guard right after a blow lands on it (it learns your rhythm). */
	public static double duelistGuardChance(int stage) {
		return stage < AuraRules.FLOW ? 0 : Math.min(0.6, 0.35 + 0.1 * (stage - AuraRules.FLOW));
	}

	/** How far a duelist's dash (from Form, once Aura Step exists) carries it, its tell and its rest. */
	public static final double DASH_DISTANCE = 4.0;
	public static final int DASH_WINDUP = 6;
	public static final int DASH_COOLDOWN = 80;

	/** Aura experience a duelist's lesson gives its victor: a little, more at higher stages. */
	public static double duelXp(int stage) {
		return 20 + 15 * Math.max(AuraRules.GLOW, AuraRules.clampStage(stage));
	}

	/** How long a second use has to accept a duelist's challenge (ticks). */
	public static final int CHALLENGE_TICKS = 200;
	/** How long a duelist rests after beating someone before it takes another challenge. */
	public static final int REST_TICKS = 600;
	/** How long a duelist bows before it leaves its victor (ticks). */
	public static final int BOW_TICKS = 50;

	// ------------------------------------------------------------------ guards (duelists and knights alike)

	/** A guard held by a duelist or a knight: how long, its perfect moment (a parry's), and its rest after. */
	public static final int MOB_GUARD_TICKS = 30;
	public static final int MOB_PERFECT_TICKS = Parry.WINDOW;
	public static final int MOB_GUARD_REST = 40;
	/** What a held mob guard takes off a blow from in front. */
	public static final double MOB_GUARD_SHARE = 0.5;
	/** A player whose blow a mob's perfect guard turns: thrown back, slowed and weakened this long. */
	public static final int MOB_STAGGER_TICKS = 30;

	/** Whether a guard raised at {@code raisedAt} is in its perfect moment at {@code now}. */
	public static boolean perfect(long raisedAt, long now) {
		return raisedAt >= 0 && now >= raisedAt && now - raisedAt <= MOB_PERFECT_TICKS;
	}

	/** What a held mob guard lets through of a blow from in front. */
	public static double throughGuard(double damage) {
		return Math.max(0, damage) * (1 - MOB_GUARD_SHARE);
	}

	// ------------------------------------------------------------------ fallen knights

	/** Where a knight haunts decides its rank: a dungeon's spawner room 1, a stronghold 2, an ancient city or an expedition 3. */
	public static int knightRank(boolean deep, boolean stronghold) {
		return deep ? 3 : stronghold ? 2 : 1;
	}

	public static int clampRank(int rank) {
		return Math.max(1, Math.min(3, rank));
	}

	public static double knightHealth(int rank) {
		return 30 + 10 * clampRank(rank);
	}

	public static double knightArmour(int rank) {
		return 6 + 2 * clampRank(rank);
	}

	/** A knight's slash before the game's difficulty scaling (aura from a monster is scaled like any monster's harm). */
	public static double knightSlash(int rank) {
		return 4 + clampRank(rank);
	}

	public static int knightSlashWindup(int rank) {
		return 22 - 2 * clampRank(rank);
	}

	public static int knightSlashCooldown(int rank) {
		return 160 - 20 * clampRank(rank);
	}

	/** A knight's crescent: slower still, and it flies a little further. */
	public static final double KNIGHT_SLASH_SPEED = 1.1;
	public static final double KNIGHT_SLASH_RANGE = 14.0;
	public static final double KNIGHT_SLASH_WIDTH = 3.0;
	/** The aim fixes this long before a slash leaves the blade: moving aside then still works. */
	public static final int SLASH_LOCK = 6;
	/** After its slash a knight is open this long: it can't guard. */
	public static final int KNIGHT_RECOVER = 20;
	/** A knight's guard broken by an axe: dazed this long, and it takes this much more while dazed. */
	public static final int KNIGHT_BROKEN_TICKS = 50;
	public static final double KNIGHT_BROKEN_BONUS = 1.25;
	/** The chance a knight braces its guard when its blow is resting and a foe is close. */
	public static final double KNIGHT_GUARD_CHANCE = 0.3;

	/** Manual pages a knight drops (one, and one more by luck with Looting), and the chance of an Aura Shard. */
	public static final int PAGES = 1;
	public static final double SHARD_CHANCE = 0.25;
	public static final double SHARD_LOOTING = 0.08;
	/** Pages of one method bound with a book into its manual. */
	public static final int PAGES_PER_MANUAL = 4;

	// ------------------------------------------------------------------ spawning

	/** How often (ticks) a duelist may wander in near a player, and the chance each time at a spawn rate of 1. */
	public static final int DUELIST_PERIOD = 1200;
	public static final double DUELIST_CHANCE = 0.03;
	/** How long a duelist stays before it moves on (ticks): twenty minutes. */
	public static final int DUELIST_STAY = 24000;
	/** No duelist comes within this many blocks of another. */
	public static final double DUELIST_APART = 160;

	/** How often (ticks) a knight may rise near a player in a haunted place, and the chance each time at a rate of 1. */
	public static final int KNIGHT_PERIOD = 200;
	public static final double KNIGHT_CHANCE = 0.12;
	/** How far round a player knights are counted toward the most allowed. */
	public static final double KNIGHT_CROWD = 48;

	/** A spawn chance scaled by a rate, held between 0 and 1. */
	public static double chance(double base, double rate) {
		return Math.max(0, Math.min(1, base * Math.max(0, rate)));
	}

	// ------------------------------------------------------------------ aura-forged gear

	/** The three aura-forged weapons and what each does for aura. */
	public enum Forged {
		/** A sword: more aura from every blow. */
		LUMENEDGE("lumenedge"),
		/** A spear: a stronger slash that flies further and wider. */
		SKYREND_GLAIVE("skyrend_glaive"),
		/** An axe: a cheaper guard. */
		BULWARK_MAUL("bulwark_maul");

		public final String id;

		Forged(String id) {
			this.id = id;
		}

		public static Forged byId(String id) {
			for (Forged forged : values()) {
				if (forged.id.equals(id)) {
					return forged;
				}
			}
			return null;
		}
	}

	/** Lumenedge: a blow's aura, times this. */
	public static final double LUMENEDGE_GAIN = 1.5;
	/** Skyrend Glaive: a slash's damage, times this; and how much further and wider it flies, with two more foes cut. */
	public static final double SKYREND_SLASH = 1.6;
	public static final double SKYREND_REACH = 1.3;
	public static final int SKYREND_TARGETS = 2;
	/** Bulwark Maul: what the guard costs (raised and held), times this. */
	public static final double BULWARK_GUARD_COST = 0.6;

	/** The Breath Sash: aura capacity times this (rounded down), the stance settling in this many ticks, its breath times this. */
	public static final double SASH_CAPACITY = 1.25;
	public static final int SASH_SETTLE = 10;
	public static final double SASH_BREATH = 1.5;

	/** A stage's capacity with the sash on. */
	public static int sashCapacity(int capacity, double factor) {
		return capacity <= 0 ? 0 : (int) Math.floor(capacity * Math.max(1, factor) + 1.0E-6);
	}

	// ------------------------------------------------------------------ the clash, and slashes against players

	/**
	 * Whether two crescents flying this tick meet: the nearest the two fronts come to each other as both move from where they
	 * were to where they are is within {@code reach}. Points are {x, y, z}.
	 */
	public static boolean meets(double[] fromA, double[] toA, double[] fromB, double[] toB, double reach) {
		double[] d0 = {fromB[0] - fromA[0], fromB[1] - fromA[1], fromB[2] - fromA[2]};
		double[] dv = {(toB[0] - fromB[0]) - (toA[0] - fromA[0]), (toB[1] - fromB[1]) - (toA[1] - fromA[1]), (toB[2] - fromB[2]) - (toA[2] - fromA[2])};
		double vv = dv[0] * dv[0] + dv[1] * dv[1] + dv[2] * dv[2];
		double t = vv < 1.0E-9 ? 0 : Math.max(0, Math.min(1, -(d0[0] * dv[0] + d0[1] * dv[1] + d0[2] * dv[2]) / vv));
		double x = d0[0] + dv[0] * t;
		double y = d0[1] + dv[1] * t;
		double z = d0[2] + dv[2] * t;
		return x * x + y * y + z * z <= reach * reach;
	}

	/** How near two crescents must come to clash: most of their half-widths together. */
	public static double clashReach(double widthA, double widthB) {
		return 0.6 * (widthA + widthB) / 2 + 0.4;
	}

	/** How far a clash's burst throws creatures, and from how far. */
	public static final double CLASH_RADIUS = 2.5;
	public static final double CLASH_PUSH = 0.6;

	/**
	 * A slash landing on a player, as a share of its damage before their own defences: its bonuses (the element, a forged
	 * glaive) held to the spell-defence cap, then the aura PvP scale. The worked numbers are in DESIGN.md's PvP review.
	 */
	public static double slashAgainstPlayer(double bonus, double cap, double pvpScale) {
		return SpellDefenceRules.capBonus(bonus, cap) * Math.max(0, pvpScale);
	}
}
