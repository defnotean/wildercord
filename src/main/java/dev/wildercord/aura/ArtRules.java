package dev.wildercord.aura;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The breathing methods' arts, the pure part: each art's price, rest and numbers, the rules every art keeps against another
 * player and a boss, and the rough model the arts were balanced with. The arts themselves are in {@code aura.arts} (one class a
 * method); the framework that registers them is {@code aura.arts.MethodArts}.
 *
 * <h2>How the numbers were set</h2>
 * <p>Damage is written in the weapon's own damage ({@code W}: an iron sword's 6, a netherite one's 8), as the slash and the
 * placeholder arts are, so a better blade carries every art with it, and the server's {@code aura.damage_scale} and
 * {@code aura.art_damage} scale them all. Every art lands as projected aura (the slash's rules: armour applies, a player's spell
 * defences and the PvP scale apply, a totem saves).</p>
 *
 * <p>Each stage's art has a slot, and every method's art in a slot costs and rests about the same and is worth about the same
 * (see {@link #power}): the slot's string is the same for everyone, so a method is a different answer to the same question,
 * never a better one.</p>
 * <ul>
 * <li><b>First Art</b> (Glow), 6 aura, 3 s: the everyday combo finisher, worth about 1.3 W. A Glow pool (20) plays it three
 *     times.</li>
 * <li><b>Second Art</b> (Flow), 8 aura, 4 s: an aerial opener, about 1.8 W with its control.</li>
 * <li><b>Third Art</b> (Edge), 8 aura, 4 s: the counter. It needs a perfect guard first (the hardest thing to time in aura), so
 *     for its price it's worth the most of the four: about 1.9 W, and a hard answer to whoever struck.</li>
 * <li><b>Fourth Art</b> (Form), 10 aura, 5 s: the rush. It comes straight after an Aura Step (12 aura of its own), so the pair
 *     costs 22: about 2.15 W with the ground it covers.</li>
 * <li><b>Final Art</b> (Sovereign), 40 aura, 30 s, a full pool: the method's great moment, about 4.35 W, beside Dominion (40
 *     aura, 90 s).</li>
 * </ul>
 * <p>Against the techniques: Aura Slash is 12 aura every 2 s for 1.2 W down a 14-block line through six foes, so it stays the
 * reach weapon; an art does more in its moment for its string's effort, and rests longer. Against spells: a Final Art lands about
 * what a strong high-circle spell does (around 15 damage with a diamond blade, before defences), and every hold an art lays on a
 * player is shorter than the spell that does the same.</p>
 *
 * <h2>Fair to other players</h2>
 * <p>An art never deals one player more than {@link #PVP_ART_CAP} in all (after the PvP scale, before their armour and
 * defences), holds a player still at most {@link #PVP_HOLD_TICKS} and not again for {@link #PVP_HOLD_REST}, sets a player alight
 * for at most {@link #PVP_IGNITE_TICKS}, and throws a player no harder than {@link #PVP_THROW}. Teams and the pvp rule are
 * respected (a player the swordsman can't harm is never touched). Bosses are only ever slowed: never lifted, thrown, pulled or
 * held, as with every spell.</p>
 */
public final class ArtRules {
	private ArtRules() {}

	// ------------------------------------------------------------------ the slots

	/** What every method's art in a slot costs and rests, by default: the First Art to the Final Art. */
	public static final double[] SLOT_COST = {6.0, 8.0, 8.0, 10.0, 40.0};
	public static final int[] SLOT_COOLDOWN = {60, 80, 80, 100, 600};

	/** What every art's Grimoire entry begins with. */
	public static final String GRIMOIRE_ART = "aura:art_";

	/** The Grimoire entry a method's own art writes the first time it's played: {@code aura:art_<id>} (':' as '.'). */
	public static String grimoireKey(String artId) {
		return GRIMOIRE_ART + artId.replace(':', '.');
	}

	/** The slot (0 to 4) of an art opening at {@code stage}: the First Art at Glow to the Final Art at Sovereign. */
	public static int slot(int stage) {
		return Math.max(0, Math.min(4, AuraRules.clampStage(stage) - 1));
	}

	// ------------------------------------------------------------------ against players and bosses

	/** The most one art deals one player in all, after the PvP scale and before their armour and spell defences: never a one-shot. */
	public static final double PVP_ART_CAP = 8.0;
	/** The longest an art holds a player still (a freeze, a shake), and how long before another art's hold can take them. */
	public static final int PVP_HOLD_TICKS = 15;
	public static final int PVP_HOLD_REST = 80;
	/** The longest an art sets a player alight. */
	public static final int PVP_IGNITE_TICKS = 60;
	/** The hardest an art throws, lifts or pulls a player (an impulse in blocks a tick). */
	public static final double PVP_THROW = 0.6;

	/**
	 * What is left of an art's damage on one player after it has dealt {@code dealt} already: never past {@link #PVP_ART_CAP}.
	 *
	 * @param amount what the next hit would deal after the PvP scale
	 */
	public static double pvpLeft(double amount, double dealt) {
		return Math.max(0, Math.min(amount, PVP_ART_CAP - Math.max(0, dealt)));
	}

	/** How long a hold lasts on a target: as asked on a creature, {@link #PVP_HOLD_TICKS} at most on a player, nothing on a boss. */
	public static int hold(int ticks, boolean player, boolean boss) {
		if (boss || ticks <= 0) {
			return 0;
		}
		return player ? Math.min(ticks, PVP_HOLD_TICKS) : ticks;
	}

	/** How hard a throw lands on a target: as asked on a creature, {@link #PVP_THROW} at most on a player, nothing on a boss. */
	public static double thrown(double power, boolean player, boolean boss) {
		if (boss || power <= 0) {
			return 0;
		}
		return player ? Math.min(power, PVP_THROW) : power;
	}

	/** How long fire takes hold on a target: as asked, {@link #PVP_IGNITE_TICKS} at most on a player. */
	public static int ignite(int ticks, boolean player) {
		return player ? Math.min(Math.max(0, ticks), PVP_IGNITE_TICKS) : Math.max(0, ticks);
	}

	// ------------------------------------------------------------------ shapes

	/** Whether a point {@code dx, dz} from the apex lies in a cone {@code reach} long and {@code degrees} wide along {@code fx, fz} (flat). */
	public static boolean inCone(double dx, double dz, double fx, double fz, double reach, double degrees) {
		return StringRules.inArc(dx, dz, fx, fz, reach, degrees);
	}

	/**
	 * How far along and how far across a line from its start a point lies (flat): along is measured down {@code fx, fz}, across
	 * square to it. Returns {@code {along, across}}.
	 */
	public static double[] alongAcross(double dx, double dz, double fx, double fz) {
		double f = Math.sqrt(fx * fx + fz * fz);
		if (f < 1.0E-9) {
			return new double[] {0, Math.sqrt(dx * dx + dz * dz)};
		}
		double ux = fx / f;
		double uz = fz / f;
		return new double[] {dx * ux + dz * uz, Math.abs(dx * uz - dz * ux)};
	}

	/** A blast's damage at {@code distance} from its heart: {@code centre} there, easing to {@code edge} at {@code radius}. */
	public static double falloff(double centre, double edge, double distance, double radius) {
		if (radius <= 0) {
			return centre;
		}
		double t = Math.max(0, Math.min(1, distance / radius));
		return centre + (edge - centre) * t;
	}

	/** A chain's {@code n}th foe (0 the first): each one {@code keep} of the one before. */
	public static double chain(double first, double keep, int n) {
		return n < 0 ? 0 : first * Math.pow(Math.max(0, Math.min(1, keep)), n);
	}

	// ================================================================== Ember: fire that spreads and lingers

	/** Kindling Draw (I): a fast low draw-cut in front that sets its foes alight, and a line of fire running on ahead. */
	public static final double KINDLING_REACH = 3.5;
	public static final double KINDLING_DEGREES = 120;
	public static final double KINDLING_FACTOR = 0.55;
	public static final int KINDLING_IGNITE = 60;
	public static final int KINDLING_TARGETS = 4;
	/** The line of fire: from this far ahead, this long, this wide each side, burning this long, setting what stands in it alight. */
	public static final double KINDLING_LINE_FROM = 1.2;
	public static final double KINDLING_LINE = 5.0;
	public static final double KINDLING_LINE_HALF = 0.75;
	public static final int KINDLING_LINE_TICKS = 60;
	public static final int KINDLING_LINE_IGNITE = 40;

	/** Rising Cinders (II): a leaping slash that lifts the foes in front in a shower of embers, and cinders rain off them. */
	public static final double CINDERS_REACH = 3.3;
	public static final double CINDERS_DEGREES = 110;
	public static final double CINDERS_FACTOR = 0.7;
	public static final double CINDERS_LIFT = 0.62;
	public static final int CINDERS_IGNITE = 60;
	public static final int CINDERS_AIRBORNE = 30;
	public static final int CINDERS_TARGETS = 4;
	/** The cinders: this long after, off each foe lifted, onto the others this close. */
	public static final int CINDERS_RAIN_DELAY = 12;
	public static final double CINDERS_RAIN_RADIUS = 2.2;
	public static final double CINDERS_RAIN_FACTOR = 0.25;

	/** Backdraft (III): the caught blow thrown back as a gout of flame, the one who struck taking it whole. */
	public static final double BACKDRAFT_REACH = 4.5;
	public static final double BACKDRAFT_DEGREES = 75;
	public static final double BACKDRAFT_FACTOR = 0.75;
	/** The caught blow comes back too: up to the weapon's own damage of it. */
	public static final double BACKDRAFT_CAUGHT_MAX = 1.0;
	/** The rest of the cone takes this share of what the one who struck takes. */
	public static final double BACKDRAFT_SPLASH = 0.5;
	public static final int BACKDRAFT_IGNITE = 60;
	public static final double BACKDRAFT_THROW = 0.7;
	public static final int BACKDRAFT_TARGETS = 5;

	/** What Backdraft's foe takes, in damage: the weapon's share and the caught blow, the blow at most {@link #BACKDRAFT_CAUGHT_MAX} weapons. */
	public static double backdraft(double weapon, double caught) {
		return weapon * BACKDRAFT_FACTOR + Math.max(0, Math.min(caught, weapon * BACKDRAFT_CAUGHT_MAX));
	}

	/** Wildfire Rush (IV): a second dash, through foes, leaving the ground burning behind. */
	public static final double WILDFIRE_DISTANCE = 7.0;
	public static final int WILDFIRE_TICKS = 4;
	public static final double WILDFIRE_WIDTH = 1.4;
	public static final double WILDFIRE_FACTOR = 0.85;
	public static final int WILDFIRE_TARGETS = 5;
	public static final int WILDFIRE_IGNITE = 60;
	public static final int WILDFIRE_TRAIL_TICKS = 80;

	/** Sunfall (V): up into the air, a sun on the blade, down in a blazing arc; a blast where it lands and a ring of fire after. */
	public static final double SUNFALL_LEAP = 0.95;
	public static final int SUNFALL_RISE = 9;
	public static final double SUNFALL_DIVE = 1.6;
	/** How long the swordsman may take to come down before it lands where they are anyway. */
	public static final int SUNFALL_FALL_MAX = 30;
	public static final double SUNFALL_RADIUS = 4.5;
	public static final double SUNFALL_CENTRE = 2.2;
	public static final double SUNFALL_EDGE = 1.5;
	public static final int SUNFALL_TARGETS = 10;
	public static final int SUNFALL_IGNITE = 100;
	public static final double SUNFALL_THROW = 0.9;
	/** The ring of fire: this far out, this wide, standing this long, searing what's in it each period. */
	public static final double SUNFALL_RING = 5.0;
	public static final double SUNFALL_RING_BAND = 1.0;
	public static final int SUNFALL_RING_TICKS = 100;
	public static final int SUNFALL_RING_PERIOD = 10;
	public static final double SUNFALL_RING_FACTOR = 0.15;

	// ================================================================== Rime: slow, then freeze, then shatter

	/** Frostbite (I): a cut that crusts and slows; a third crust soon after freezes the foe solid. */
	public static final double FROSTBITE_REACH = 3.3;
	public static final double FROSTBITE_DEGREES = 120;
	public static final double FROSTBITE_FACTOR = 0.6;
	public static final int FROSTBITE_SLOW = 60;
	public static final int FROSTBITE_TARGETS = 4;
	/** Crusts build this long; this many freeze the foe for this long. */
	public static final int CRUST_MEMORY = 160;
	public static final int CRUSTS_TO_FREEZE = 3;
	public static final int FROSTBITE_FREEZE = 30;

	/** The crusts a foe carries after one more lands (at most {@link #CRUSTS_TO_FREEZE}): the old ones only if they're fresh. */
	public static int crusts(int had, long lastAt, long now) {
		int fresh = now - lastAt <= CRUST_MEMORY && now >= lastAt ? Math.max(0, had) : 0;
		return Math.min(CRUSTS_TO_FREEZE, fresh + 1);
	}

	/** Hailfall (II): a cut in front, and a cloud ahead that rains hail. */
	public static final double HAIL_CUT_REACH = 3.0;
	public static final double HAIL_CUT_FACTOR = 0.5;
	public static final double HAIL_AHEAD = 3.6;
	public static final double HAIL_RADIUS = 2.6;
	public static final int HAIL_STONES = 7;
	public static final int HAIL_TICKS = 18;
	public static final double HAIL_STONE_REACH = 1.15;
	public static final double HAIL_STONE_FACTOR = 0.28;
	/** The most hailstones that strike one foe. */
	public static final int HAIL_PER_FOE = 3;
	public static final int HAIL_SLOW = 30;

	/** Glacier Mirror (III): the one who struck frozen solid, and a mirror of ice raised that turns projectiles back. */
	public static final double MIRROR_FACTOR = 0.9;
	public static final int MIRROR_FREEZE = 40;
	public static final double MIRROR_CHILL_REACH = 3.0;
	public static final int MIRROR_CHILL = 40;
	public static final int MIRROR_TICKS = 50;

	/** Skate (IV): a glide along a path of ice laid as you go; frozen foes in the way shatter. */
	public static final double SKATE_DISTANCE = 8.0;
	public static final int SKATE_TICKS = 4;
	public static final double SKATE_WIDTH = 1.4;
	public static final double SKATE_FACTOR = 0.7;
	public static final double SKATE_SHATTER = 1.5;
	public static final double SKATE_SHARDS = 2.0;
	public static final double SKATE_SHARD_FACTOR = 0.3;
	public static final int SKATE_PATH_TICKS = 100;
	public static final int SKATE_TARGETS = 5;

	/** Winter's Hush (V): a cone that freezes everything in it, and a moment later shatters what's still frozen. */
	public static final double HUSH_REACH = 7.5;
	public static final double HUSH_DEGREES = 100;
	public static final double HUSH_FACTOR = 0.8;
	public static final int HUSH_FREEZE = 50;
	public static final int HUSH_TARGETS = 10;
	public static final int HUSH_SHATTER_DELAY = 30;
	public static final double HUSH_SHATTER = 1.7;
	public static final double HUSH_SHARDS = 2.2;
	public static final double HUSH_SHARD_FACTOR = 0.35;

	// ================================================================== Thunder: faster than the eye, from foe to foe

	/** Crackle (I): three cuts faster than the eye on one foe, each throwing a spark to another. */
	public static final double CRACKLE_REACH = 3.4;
	public static final int CRACKLE_CUTS = 3;
	public static final int CRACKLE_GAP = 2;
	public static final double CRACKLE_FACTOR = 0.27;
	public static final double CRACKLE_SPARK = 0.15;
	public static final double CRACKLE_SPARK_REACH = 4.0;

	/** Skyfall (II): a bolt called down on the foe (or the ground ahead) a moment after, arcing to two more. */
	public static final double SKYFALL_AHEAD = 4.0;
	public static final int SKYFALL_DELAY = 6;
	public static final double SKYFALL_RADIUS = 1.6;
	public static final double SKYFALL_FACTOR = 0.95;
	public static final int SKYFALL_SHOCK = 10;
	public static final int SKYFALL_ARCS = 2;
	public static final double SKYFALL_ARC_REACH = 4.5;
	public static final double SKYFALL_ARC_FACTOR = 0.35;

	/** Static Riposte (III): the one who struck, then lightning leaping on through the foes near. */
	public static final double RIPOSTE_FACTOR = 0.8;
	public static final int RIPOSTE_JUMPS = 4;
	public static final double RIPOSTE_KEEP = 0.85;
	public static final double RIPOSTE_REACH = 5.0;
	public static final int RIPOSTE_SHOCK = 8;

	/** Bolt Step (IV): blinking from foe to foe, a cut on each. */
	public static final int BOLT_FOES = 4;
	public static final double BOLT_REACH = 8.0;
	public static final int BOLT_GAP = 3;
	public static final double BOLT_FACTOR = 0.75;
	public static final int BOLT_SHOCK = 6;

	/** Heaven's Spear (V): a charge, then a lance of lightning down a line, and the sky answering on everything it ran through. */
	public static final int SPEAR_CHARGE = 14;
	public static final double SPEAR_LENGTH = 20.0;
	public static final double SPEAR_HALF = 0.8;
	public static final double SPEAR_FACTOR = 2.0;
	public static final int SPEAR_TARGETS = 8;
	public static final int SPEAR_SHOCK = 20;
	/** The lance runs level unless the swordsman clearly looks up or down (past this sine of their pitch, about twelve degrees). */
	public static final double SPEAR_AIM = 0.2;
	/** The most the lance tilts (the sine of thirty degrees). */
	public static final double SPEAR_TILT = 0.5;

	/** The sine of the lance's tilt for a look whose upward part is {@code lookY}: level for a glance down at a foe, the aim held to thirty degrees. */
	public static double spearTilt(double lookY) {
		if (Math.abs(lookY) < SPEAR_AIM) {
			return 0;
		}
		return Math.max(-SPEAR_TILT, Math.min(SPEAR_TILT, lookY));
	}

	/** How long a foe a Thunder art struck stays ionised, for a storm spell to conduct through. */
	public static final int IONISED_TICKS = 100;

	// ================================================================== Gale: reach, the air under them, the wind at your back

	/** Cutting Breeze (I): a wind blade flying on far past the sword's reach, pushing what it cuts. */
	public static final double BREEZE_RANGE = 10.0;
	public static final double BREEZE_SPEED = 2.0;
	public static final double BREEZE_WIDTH = 2.0;
	public static final double BREEZE_FACTOR = 0.5;
	public static final double BREEZE_PUSH = 0.8;
	public static final int BREEZE_TARGETS = 4;

	/** Updraft (II): the foes in front thrown high, you rising after them; they take more from your blade while they're up. */
	public static final double UPDRAFT_REACH = 3.3;
	public static final double UPDRAFT_DEGREES = 120;
	public static final double UPDRAFT_FACTOR = 0.65;
	public static final double UPDRAFT_LIFT = 0.95;
	public static final double UPDRAFT_SELF = 0.75;
	public static final int UPDRAFT_FLOAT = 30;
	public static final int UPDRAFT_AIRBORNE = 40;
	public static final int UPDRAFT_TARGETS = 4;
	/** While a foe Updraft threw is off the ground, your coated blows on it land this many times as hard (the juggle). */
	public static final double UPDRAFT_JUGGLE = 1.25;

	/** Eye of the Storm (III): a spinning cut all round, and for a while projectiles turn aside and foes are blown off you. */
	public static final double EYE_RADIUS = 3.2;
	public static final double EYE_FACTOR = 0.8;
	public static final double EYE_PUSH = 0.75;
	public static final int EYE_TARGETS = 6;
	public static final int EYE_TICKS = 60;
	public static final double EYE_GUST_RADIUS = 2.5;
	public static final double EYE_GUST = 0.4;

	/** Tailwind (IV): a long dash, foes shoved aside, allies near swept along faster. */
	public static final double TAILWIND_DISTANCE = 11.0;
	public static final int TAILWIND_TICKS = 5;
	public static final double TAILWIND_WIDTH = 1.6;
	public static final double TAILWIND_FACTOR = 0.6;
	public static final double TAILWIND_SHOVE = 0.8;
	public static final int TAILWIND_TARGETS = 5;
	public static final double TAILWIND_ALLIES = 5.0;
	public static final int TAILWIND_SPEED = 100;

	/** Hundred Winds (V): you become a whirlwind of cuts, drawing foes in and lifting them all at its end. */
	public static final double WINDS_RADIUS = 5.0;
	public static final int WINDS_TICKS = 60;
	public static final int WINDS_PERIOD = 5;
	public static final double WINDS_PULL = 0.22;
	public static final double WINDS_FACTOR = 0.22;
	public static final int WINDS_TARGETS = 8;
	public static final double WINDS_LIFT = 1.0;
	public static final double WINDS_FINISH = 0.5;

	// ================================================================== Stone: weight, footing and standing firm

	/** Rockbreaker (I): a heavy cut that shakes its foe's footing and cracks it, the shock spilling onto those beside it. */
	public static final double ROCK_REACH = 3.3;
	public static final double ROCK_FACTOR = 0.8;
	public static final int ROCK_SLOW = 30;
	public static final int ROCK_HOLD = 8;
	public static final double ROCK_SHOCK_RADIUS = 1.8;
	public static final double ROCK_SHOCK_FACTOR = 0.3;
	public static final int ROCK_CRACKED = 60;

	/** Avalanche (II): a slam where you land, and a shockwave rolling out over the ground. */
	public static final double AVALANCHE_RADIUS = 4.2;
	public static final int AVALANCHE_ROLL = 6;
	public static final double AVALANCHE_CENTRE = 0.9;
	public static final double AVALANCHE_EDGE = 0.5;
	public static final double AVALANCHE_THROW = 0.5;
	public static final int AVALANCHE_SLOW = 40;
	public static final int AVALANCHE_TARGETS = 8;

	/** Unmoved (III): the one who struck hurled back by their own blow, and you hardened like stone for a while. */
	public static final double UNMOVED_FACTOR = 0.9;
	public static final double UNMOVED_THROW = 1.6;
	public static final int UNMOVED_TICKS = 80;

	/** Landslide (IV): a heavy charge that carries the foes in front along, throwing them at its end; one against a wall is crushed. */
	public static final double LANDSLIDE_DISTANCE = 8.0;
	public static final int LANDSLIDE_TICKS = 8;
	public static final double LANDSLIDE_WIDTH = 1.6;
	public static final int LANDSLIDE_TARGETS = 4;
	public static final double LANDSLIDE_FACTOR = 0.9;
	public static final double LANDSLIDE_WALL = 1.5;
	public static final double LANDSLIDE_THROW = 1.0;
	public static final int LANDSLIDE_HOLD = 10;
	public static final int LANDSLIDE_WALL_HOLD = 20;

	/** Mountain Splitter (V): the ground split in a line ahead, stone rising along it in turn and throwing up whatever stands there. */
	public static final double SPLITTER_LENGTH = 15.0;
	public static final double SPLITTER_SPEED = 1.25;
	public static final double SPLITTER_SPACING = 1.5;
	public static final double SPLITTER_REACH = 1.4;
	public static final double SPLITTER_FACTOR = 2.1;
	public static final double SPLITTER_LIFT = 0.75;
	public static final int SPLITTER_HOLD = 15;
	public static final int SPLITTER_TARGETS = 10;

	// ================================================================== the arts, priced and weighed

	/**
	 * One art's price and rest, and what it's worth in the balance model ({@link #power}): {@code primary} is what its main foe
	 * takes (in weapon damage; fire counted at a seventh of a weapon a second, and a bonus it often earns at about a third of it),
	 * {@code area} what it may do to the others it can reach (in all, at a fair count of them), {@code control} the seconds it
	 * holds, slows, throws or turns a foe away (or hardens or speeds its swordsman or an ally), and {@code reach} how far from where
	 * its swordsman stood it reaches or carries them (blocks).
	 */
	public record Art(String id, String method, int slot, double cost, int cooldown, double primary, double area, double control, double reach) {
		public int stage() {
			return slot + AuraRules.GLOW;
		}
	}

	/** Fire, counted in weapon damage: vanilla fire burns a point a second, and a weapon is about seven. */
	static double fire(int ticks) {
		return ticks / 20.0 / 7.0;
	}

	public static final List<Art> ARTS = List.of(
		// Ember: damage now and fire after, and the least control.
		new Art("kindling_draw", "ember", 0, 6, 60, KINDLING_FACTOR + fire(KINDLING_IGNITE), 0.3, 0.0, KINDLING_LINE_FROM + KINDLING_LINE),
		new Art("rising_cinders", "ember", 1, 8, 80, CINDERS_FACTOR + fire(CINDERS_IGNITE), 0.5 + CINDERS_RAIN_FACTOR, 1.0, CINDERS_REACH),
		new Art("backdraft", "ember", 2, 8, 80, BACKDRAFT_FACTOR + 0.35 + fire(BACKDRAFT_IGNITE), 0.45, 0.5, BACKDRAFT_REACH),
		new Art("wildfire_rush", "ember", 3, 10, 100, WILDFIRE_FACTOR + fire(WILDFIRE_IGNITE), 0.8, 0.0, WILDFIRE_DISTANCE + 3),
		new Art("sunfall", "ember", 4, 40, 600, SUNFALL_CENTRE + fire(SUNFALL_IGNITE), 1.6, 1.5, SUNFALL_RING + 0.5),
		// Rime: less damage, the most hold.
		new Art("frostbite", "rime", 0, 6, 60, FROSTBITE_FACTOR, 0.4, 1.6, FROSTBITE_REACH),
		new Art("hailfall", "rime", 1, 8, 80, HAIL_CUT_FACTOR + 2 * HAIL_STONE_FACTOR, 0.5, 1.5, 5.0),
		new Art("glacier_mirror", "rime", 2, 8, 80, MIRROR_FACTOR, 0.2, 3.0, 3.0),
		new Art("skate", "rime", 3, 10, 100, SKATE_FACTOR + 0.3, 0.4, 1.6, SKATE_DISTANCE + 3),
		new Art("winters_hush", "rime", 4, 40, 600, HUSH_FACTOR + HUSH_SHATTER, 1.6, 2.5, HUSH_REACH),
		// Thunder: many foes, a little each, and interrupts.
		new Art("crackle", "thunder", 0, 6, 50, CRACKLE_CUTS * CRACKLE_FACTOR, CRACKLE_CUTS * CRACKLE_SPARK, 0.6, CRACKLE_REACH),
		new Art("skyfall", "thunder", 1, 8, 80, SKYFALL_FACTOR, SKYFALL_ARCS * SKYFALL_ARC_FACTOR, 0.5, SKYFALL_AHEAD + SKYFALL_RADIUS),
		new Art("static_riposte", "thunder", 2, 8, 80, RIPOSTE_FACTOR, 1.2, 1.2, RIPOSTE_REACH),
		new Art("bolt_step", "thunder", 3, 10, 100, BOLT_FACTOR, 1.05, 0.5, BOLT_REACH + 3),
		new Art("heavens_spear", "thunder", 4, 40, 600, SPEAR_FACTOR, 1.6, 1.0, SPEAR_LENGTH),
		// Gale: reach, and the ground taken from under them.
		new Art("cutting_breeze", "gale", 0, 6, 60, BREEZE_FACTOR, 0.4, 0.4, BREEZE_RANGE),
		new Art("updraft", "gale", 1, 8, 80, UPDRAFT_FACTOR + 0.3, 0.4, 1.8, 5.0),
		new Art("eye_of_the_storm", "gale", 2, 8, 80, EYE_FACTOR, 0.6, 2.5, EYE_RADIUS),
		new Art("tailwind", "gale", 3, 10, 100, TAILWIND_FACTOR, 0.4, 2.0, TAILWIND_DISTANCE + 3),
		new Art("hundred_winds", "gale", 4, 40, 600, WINDS_FACTOR * 8 + WINDS_FINISH, 1.6, 2.4, WINDS_RADIUS + 2),
		// Stone: heavy hits, footing taken, and standing firm.
		new Art("rockbreaker", "stone", 0, 6, 60, ROCK_FACTOR, ROCK_SHOCK_FACTOR, 1.2, ROCK_REACH),
		new Art("avalanche", "stone", 1, 8, 80, AVALANCHE_CENTRE, 0.9, 1.0, AVALANCHE_RADIUS),
		new Art("unmoved", "stone", 2, 8, 80, UNMOVED_FACTOR, 0.0, 3.6, 3.0),
		new Art("landslide", "stone", 3, 10, 100, LANDSLIDE_FACTOR + 0.2, 0.5, 0.8, LANDSLIDE_DISTANCE + 3),
		new Art("mountain_splitter", "stone", 4, 40, 600, SPLITTER_FACTOR, 2.0, 1.4, SPLITTER_LENGTH));

	private static final Map<String, Art> BY_ID = ARTS.stream().collect(Collectors.toUnmodifiableMap(Art::id, Function.identity()));

	/** An art's price and rest by its id ({@code kindling_draw}...); throws for one that isn't here. */
	public static Art art(String id) {
		Art art = BY_ID.get(id);
		if (art == null) {
			throw new IllegalArgumentException("no art " + id);
		}
		return art;
	}

	/** The arts of a method, First to Final. */
	public static List<Art> of(String method) {
		return ARTS.stream().filter(a -> a.method().equals(method)).toList();
	}

	/** What a block of reach past a sword's own (three blocks) is worth in the balance model, in weapon damage. */
	public static final double REACH_WORTH = 0.07;

	/**
	 * What an art is worth in the balance model: its main foe's damage, three fifths of what it may do to others (it doesn't
	 * always find them), a quarter of a weapon for each second of control, and {@link #REACH_WORTH} for each block it reaches past
	 * a sword. Rough, and meant to be: it's what keeps one method's answer from outgrowing another's.
	 */
	public static double power(Art art) {
		return art.primary() + 0.6 * art.area() + 0.25 * art.control() + Math.max(0, art.reach() - 3.0) * REACH_WORTH;
	}

	/** What an art in each slot should be worth (see {@link #power}), the First to the Final. */
	public static final double[] SLOT_POWER = {1.3, 1.8, 1.9, 2.15, 4.35};
	/** How far one art may stray from its slot's worth, as a share. */
	public static final double POWER_SPREAD = 0.12;
}
