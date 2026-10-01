package dev.wildercord.aura;

import dev.wildercord.cast.SpellDefenceRules;
import dev.wildercord.spell.MasteryRules;
import dev.wildercord.spell.Parry;

/**
 * Aura, the swordsman's path, the pure part: mana drawn into the body and out along a blade. A player who learns a
 * breathing method holds aura (a small pool that never decays) which fills from landing real blows and from the breathing
 * stance, and is spent on coated hits and on techniques. It climbs in stages, each a leap: a haze on the weapon (Glow), then
 * aura that flows (Flow), then a solid blade (Edge); Form and Sovereign follow in a later wave. Experience comes from
 * meaningful melee and fills toward each stage's threshold, where a breakthrough waits for a trial.
 *
 * <p>Shared by the server ({@link Aura} and friends), the HUD and the Aura page, and the unit tests. Every number a server
 * owner may want to change is also in the {@code aura} config section; the defaults live here.</p>
 *
 * <p>Tuned alongside spell mastery ({@link MasteryRules}), whose moment, repetition and practice rules this reuses: a player
 * who fights with a blade about a third of their play reaches Flow in about half an hour, Edge in about two hours, and the
 * later stages (when they open) in about six and fifteen. A kill of an ordinary monster in four full swings is worth about
 * 2.5 experience; meaningful fighting earns about 300 an hour once repetition has had its say.</p>
 */
public final class AuraRules {
	private AuraRules() {}

	// ------------------------------------------------------------------ stages

	public static final int NONE = 0;
	public static final int GLOW = 1;
	public static final int FLOW = 2;
	public static final int EDGE = 3;
	public static final int FORM = 4;
	public static final int SOVEREIGN = 5;
	public static final int MAX_STAGE = SOVEREIGN;
	/** The stages this wave opens (their breakthrough trials are built in); the stage registry can open more. */
	public static final int BUILT_IN_STAGES = EDGE;

	private static final String[] IDS = {"none", "glow", "flow", "edge", "form", "sovereign"};
	/** How much aura each stage holds: the stage's leap is in what it can do, and in how much it can do it. */
	private static final int[] CAPACITY = {0, 20, 40, 70, 110, 160};
	/** The experience (a running total) each stage's breakthrough needs: Glow comes with the method, then 150, 600, 1,800, 4,500. */
	private static final int[] THRESHOLD = {0, 0, 150, 600, 1800, 4500};

	/** A stage's id: none, glow, flow, edge, form or sovereign. */
	public static String id(int stage) {
		return IDS[clampStage(stage)];
	}

	public static int clampStage(int stage) {
		return Math.max(NONE, Math.min(MAX_STAGE, stage));
	}

	/** The aura {@code stage} holds by default (0 without a method). */
	public static int capacity(int stage) {
		return CAPACITY[clampStage(stage)];
	}

	/** The experience the breakthrough into {@code stage} needs by default. */
	public static int threshold(int stage) {
		return THRESHOLD[clampStage(stage)];
	}

	/**
	 * Experience after a gain: it fills toward the next stage's threshold and stops there until the breakthrough is made
	 * (a path climbs in leaps, never by drifting past one), so nothing is lost and nothing runs ahead.
	 *
	 * @param nextThreshold the next stage's threshold, or a negative number at the last stage (no cap)
	 */
	public static double fill(double xp, double gained, double nextThreshold) {
		double next = Math.max(0, xp) + Math.max(0, gained);
		return nextThreshold < 0 ? next : Math.min(next, nextThreshold);
	}

	/** How far {@code xp} is from {@code from} (this stage's threshold) to {@code to} (the next's), 0 to 1. */
	public static double progress(double xp, double from, double to) {
		if (to <= from) {
			return 1.0;
		}
		return Math.max(0, Math.min(1, (xp - from) / (to - from)));
	}

	/** Whether a breakthrough waits: the experience has reached the next stage's threshold. */
	public static boolean ready(double xp, double nextThreshold) {
		return nextThreshold >= 0 && xp >= nextThreshold - 1.0E-6;
	}

	/**
	 * Experience left after switching to another breathing method: back to the start of the current stage. The stage
	 * itself is kept (it was earned), but the road to the next breakthrough starts again in the new method's way.
	 */
	public static double afterSwitch(int stage, double stageThreshold) {
		return stage <= GLOW ? 0 : Math.max(0, stageThreshold);
	}

	// ------------------------------------------------------------------ gaining aura

	/** A blow counts as meaningful only at this share of a full swing or more: spam earns nothing. */
	public static final double FULL_SWING = 0.9;
	/** Aura a blow gives per point of damage it really took, and the most one blow can give. */
	public static final double HIT_GAIN = 0.4;
	public static final double MAX_HIT_GAIN = 4.0;
	/** A training dummy (or the practice arena) gives this share of a real blow's aura. */
	public static final double PRACTICE_GAIN = 0.25;

	/**
	 * Aura from a blow that took {@code taken} health.
	 *
	 * @param swing      the share of a full swing it was struck at (vanilla's attack strength)
	 * @param repetition what repetition leaves of it (the same kind of foe in the same place, see {@link MasteryRules#repetition})
	 * @param practice   whether it struck a training dummy or anything in the practice arena
	 */
	public static double hitGain(double taken, double swing, double repetition, boolean practice) {
		if (swing < FULL_SWING || taken <= 0) {
			return 0;
		}
		double gain = Math.min(MAX_HIT_GAIN, taken * HIT_GAIN) * Math.max(0, Math.min(1, repetition));
		return practice ? gain * PRACTICE_GAIN : gain;
	}

	/** The breathing stance: sneak and stand still this long (ticks) with an aura weapon before the breath takes hold. */
	public static final int SETTLE_TICKS = 20;
	/** Aura the stance draws in a second. */
	public static final double BREATH_GAIN = 1.5;
	/** A breath every two seconds: the beat the HUD's ring closes on. */
	public static final int BREATH_PERIOD = 40;
	/** Ticks either side of a beat that count as on it. */
	public static final int BEAT_WINDOW = 4;
	/** Letting sneak up for no longer than this (and pressing it again) is a breath, and keeps the stance. */
	public static final int BOB_TICKS = 10;
	/** Aura a breath on the beat draws in at once. */
	public static final double BEAT_GAIN = 3.0;

	/** The game time of the first beat after {@code now}, for a stance that settled at {@code settledAt}. */
	public static long nextBeat(long settledAt, long now) {
		if (now < settledAt) {
			return settledAt + BREATH_PERIOD;
		}
		long k = (now - settledAt) / BREATH_PERIOD + 1;
		return settledAt + k * BREATH_PERIOD;
	}

	/** Whether {@code now} is on a beat of a stance that settled at {@code settledAt} (the first beat is a period after settling). */
	public static boolean onBeat(long settledAt, long now) {
		if (now < settledAt + BREATH_PERIOD - BEAT_WINDOW) {
			return false;
		}
		long since = (now - settledAt) % BREATH_PERIOD;
		return since <= BEAT_WINDOW || BREATH_PERIOD - since <= BEAT_WINDOW;
	}

	// ------------------------------------------------------------------ spending aura

	/** Glow: what a coated blow adds by default (the config's coat_bonus), and what it costs. */
	public static final double COAT_BONUS = 0.10;
	public static final double COAT_COST = 0.5;

	/** Flow: the sweep's reach round the struck foe (vanilla's is 1 block), how far from you it reaches (vanilla's 3), and the share of the blow it carries on top of vanilla's. */
	public static final double FLOW_SWEEP_INFLATE = 1.6;
	public static final double FLOW_SWEEP_RANGE = 3.75;
	public static final double FLOW_SWEEP_SHARE = 0.4;

	/** Flow: Aura Guard. Raising it costs this; it holds while sneak is held, this long at most; it halves what it catches, paying this much aura a point. */
	public static final double GUARD_RAISE_COST = 2.0;
	public static final int GUARD_TICKS = 40;
	public static final double GUARD_SHARE = 0.5;
	public static final double GUARD_COST_PER_POINT = 0.6;
	/** Ticks after a guard drops before another can be raised (so the perfect moment can't be held open by tapping). */
	public static final int GUARD_REST = 20;
	/** The perfect guard: raised no more than this many ticks before the blow lands. The same timing as a parry ({@link Parry#WINDOW}). */
	public static final int PERFECT_TICKS = Parry.WINDOW;
	/** A perfectly guarded attacker staggers this long. */
	public static final int STAGGER_TICKS = 40;

	/** Whether a guard raised at {@code raisedAt} is still in its perfect moment at {@code now}: the parry's rule. */
	public static boolean perfect(long raisedAt, long now) {
		return Parry.timed(raisedAt, now);
	}

	/**
	 * What a held guard takes off a blow of {@code incoming}: {@code share} of it, as far as {@code aura} pays for at
	 * {@link #GUARD_COST_PER_POINT} a point.
	 */
	public static double guardAbsorb(double incoming, double aura, double share) {
		if (incoming <= 0 || aura <= 0) {
			return 0;
		}
		return Math.min(incoming * Math.max(0, Math.min(1, share)), aura / GUARD_COST_PER_POINT);
	}

	/** Edge: reach the blade adds (blocks of entity interaction range), and the share of a blow that goes through armour. */
	public static final double EDGE_REACH = 1.0;
	public static final double EDGE_PIERCE = 0.25;

	/**
	 * What a blow of {@code amount} must be dealt as so that, once armour has had its say, {@code pierce} of it went through
	 * unhindered: the unpierced part as it was, plus the pierced part lifted by the armour that will take it down again.
	 *
	 * @param armourLeaves the share of the blow armour will leave (from vanilla's armour formula for this blow), 0 to 1
	 */
	public static double pierced(double amount, double pierce, double armourLeaves) {
		if (amount <= 0 || pierce <= 0 || armourLeaves >= 1.0 - 1.0E-6) {
			return amount;
		}
		double leaves = Math.max(0.2, armourLeaves);
		double p = Math.max(0, Math.min(1, pierce));
		return amount * (1 - p) + amount * p / leaves;
	}

	/** Edge: Aura Slash. Its default price, cooldown (ticks), and strength as a share of the weapon's damage. */
	public static final double SLASH_COST = 12.0;
	public static final int SLASH_COOLDOWN = 40;
	public static final double SLASH_FACTOR = 1.2;
	/** How far it flies, how fast (blocks a tick), how wide it cuts, and how many foes it cuts at most. */
	public static final double SLASH_RANGE = 14.0;
	public static final double SLASH_SPEED = 1.6;
	public static final double SLASH_WIDTH = 3.0;
	public static final int SLASH_TARGETS = 6;

	/**
	 * What a slash deals: the weapon's damage times {@code factor}, and only the share of its price that was really paid
	 * (a slash spent past empty goes out weakened, and brings backlash).
	 */
	public static double slashDamage(double weaponDamage, double factor, double price, double paid) {
		if (weaponDamage <= 0 || price <= 0) {
			return 0;
		}
		double share = Math.max(0, Math.min(1, paid / price));
		return weaponDamage * Math.max(0, factor) * share;
	}

	/** How long backlash lasts by default (ticks): brief exhaustion, slowed and weakened. It never damages. */
	public static final int BACKLASH_TICKS = 60;
	/** A second backlash this soon after the first only renews it. */
	public static final int BACKLASH_REST = 20;

	/** What spending {@code cost} out of {@code aura} leaves, and whether it went past empty (backlash). */
	public record Spend(double left, double paid, boolean backlash) {}

	/** Spends {@code cost}: everything that's there pays what it can; anything short of the price is backlash. */
	public static Spend spend(double aura, double cost) {
		double have = Math.max(0, aura);
		double price = Math.max(0, cost);
		if (have >= price - 1.0E-9) {
			return new Spend(have - price, price, false);
		}
		return new Spend(0, have, true);
	}

	// ------------------------------------------------------------------ against players

	/** Against another player, an aura bonus (the coat, a weakness) is one more factor under the spell-defence cap. */
	public static double capBonus(double bonus, double cap) {
		return SpellDefenceRules.capBonus(bonus, cap);
	}

	// ------------------------------------------------------------------ experience

	/** Experience for a meaningful blow on a real foe, for the share of its whole health it took, and for the blow that slays it. */
	public static final double STRIKE = 0.25;
	public static final double DAMAGE = 1.0;
	public static final double KILL = 0.5;
	/** The most one blow can earn, after everything that multiplies it. */
	public static final double MAX_PER_STRIKE = 6.0;
	/** The most training dummies and the practice arena can teach, in all, and at what rate. */
	public static final double PRACTICE_CAP = 40.0;
	public static final double PRACTICE_RATE = 0.5;

	/**
	 * What a foe is worth to learn from: another player (PvP on) half, a boss twice, a Runebound one and a half, anything
	 * else by how much stronger it is than you (the square root of its health over yours, from half to twice).
	 */
	public static double worth(boolean player, boolean boss, boolean runebound, double foeMaxHealth, double ownMaxHealth) {
		if (player) {
			return MasteryRules.PLAYER;
		}
		if (boss) {
			return MasteryRules.BOSS;
		}
		double strength = Math.sqrt(Math.max(1, foeMaxHealth) / Math.max(1, ownMaxHealth));
		double worth = Math.max(0.5, Math.min(2.0, strength));
		return runebound ? Math.max(worth, MasteryRules.RUNEBOUND) : worth;
	}

	/** Experience for one meaningful blow (before the moment, repetition and the server's rate). */
	public static double strike(double worth, double share, boolean killed) {
		if (worth <= 0) {
			return 0;
		}
		return worth * (STRIKE + DAMAGE * Math.max(0, Math.min(1, share)) + (killed ? KILL : 0));
	}

	/** How much of {@code xp} earned in practice is learned, having already learned {@code learned} that way. */
	public static double practice(double learned, double xp) {
		return Math.max(0, Math.min(PRACTICE_CAP - learned, xp * PRACTICE_RATE));
	}

	// ------------------------------------------------------------------ breakthroughs

	/** The two trials a breakthrough can be made by. */
	public enum Trial { STILLNESS, STRONGER_FOE }

	/** Stillness: hold the breathing stance unbroken this long (ticks) at a place of power. */
	public static final int STILLNESS_TICKS = 600;
	/** A stronger foe has at least this many times your own health (bosses and Runebound always count). */
	public static final double STRONGER_HEALTH = 2.0;
	/** A stronger foe must fall within this long (ticks) of your first blow on it, to melee and aura alone. */
	public static final int TRIAL_WINDOW = 1200;

	/** Whether a foe is stronger than you, for the trial. */
	public static boolean stronger(boolean boss, boolean runebound, double foeMaxHealth, double ownMaxHealth) {
		return boss || runebound || foeMaxHealth >= STRONGER_HEALTH * Math.max(1, ownMaxHealth) - 1.0E-6;
	}

	// ------------------------------------------------------------------ the methods' flavours

	/** Ember: from Flow a coated blow may set the foe alight; from Edge it always does, for longer at each stage. */
	public static double emberChance(int stage) {
		return stage >= EDGE ? 1.0 : stage >= FLOW ? 0.15 : 0.0;
	}

	public static int emberTicks(int stage) {
		return stage >= EDGE ? 40 + 20 * (stage - EDGE) : stage >= FLOW ? 20 : 0;
	}

	/** Rime: a coated blow slows the foe, a little longer at each stage (ticks). */
	public static int rimeTicks(int stage) {
		return stage <= NONE ? 0 : 10 + 10 * stage;
	}

	/** Thunder: the chance a coated blow throws a spark to another foe, and how much of the blow it carries. */
	public static double thunderChance(int stage) {
		return stage <= NONE ? 0 : 0.04 + 0.04 * stage;
	}

	public static final double THUNDER_SHARE = 0.3;
	public static final double THUNDER_REACH = 5.0;

	/** Gale: a bit of speed while in a fight (struck or striking lately), more at each stage. */
	public static double galeSpeed(int stage) {
		return 0.04 * Math.max(0, stage);
	}

	/** How long after a blow (given or taken) a player counts as in a fight (ticks). */
	public static final int COMBAT_TICKS = 80;

	/** Stone: knockback resistance with an aura weapon in hand. */
	public static double stoneResistance(int stage) {
		return 0.12 * Math.max(0, stage);
	}

	/** Verdant: health a coated blow mends, at most every {@link #VERDANT_REST} ticks. */
	public static double verdantHeal(int stage) {
		return stage <= NONE ? 0 : 0.25 + 0.15 * stage;
	}

	public static final int VERDANT_REST = 10;

	/** Hollow: how hard foes near the struck one are drawn toward it, and from how far. */
	public static double hollowPull(int stage) {
		return 0.06 * Math.max(0, stage);
	}

	public static final double HOLLOW_RADIUS = 3.5;

	/** Starlit: aura comes this many times as fast (the stance and blows alike). */
	public static double starlitGain(int stage) {
		return 1.0 + 0.15 * Math.max(0, stage);
	}

	/** Hourglass: a little attack speed with an aura weapon in hand. */
	public static double hourglassSpeed(int stage) {
		return 0.025 * Math.max(0, stage);
	}

	/** Crimson: a coated blow drinks this share of what it took, at most {@link #CRIMSON_MAX} health, for {@link #CRIMSON_COST} more aura. */
	public static double crimsonLeech(int stage) {
		return stage <= NONE ? 0 : 0.05 + 0.05 * stage;
	}

	public static final double CRIMSON_COST = 1.0;
	public static final double CRIMSON_MAX = 2.0;

	// ------------------------------------------------------------------ colour

	/** The aura's colour at a stage: the method's colour, burning toward its highlight as it grows. */
	public static int color(int primary, int highlight, int stage) {
		double t = Math.max(0, Math.min(0.6, 0.12 * (stage - 1)));
		return mix(primary, highlight, t);
	}

	static int mix(int a, int b, double t) {
		int r = (int) Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int g = (int) Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int bl = (int) Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return (r << 16) | (g << 8) | bl;
	}
}
