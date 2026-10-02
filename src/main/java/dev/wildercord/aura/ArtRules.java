package dev.wildercord.aura;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * <p>All ten methods' fifty arts are weighed with the same model ({@link #power}): what an art mends counts like what it deals to
 * the others it reaches (three fifths: it doesn't always find a wound), aura it gives back a tenth of a W each, and the health an
 * art costs its own swordsman (Crimson Moon) is taken off whole. Each method also leads in its own thing, in the model's numbers
 * or in the kinds of thing its arts do ({@link Kind}); {@code ArtRulesTest} holds them all to it.</p>
 *
 * <h2>Fair to other players</h2>
 * <p>An art never deals one player more than {@link #PVP_ART_CAP} in all (after the PvP scale, before their armour and
 * defences), holds a player still at most {@link #PVP_HOLD_TICKS} and not again for {@link #PVP_HOLD_REST} (a root, a stopped
 * moment and a freeze are all holds), sets a player alight for at most {@link #PVP_IGNITE_TICKS}, throws a player no harder than
 * {@link #PVP_THROW}, drags one no faster than {@link #PVP_DRAG} (slower than a sprint, so a pull can always be run out of), and
 * silences one at most {@link #SILENCE_PLAYER_TICKS}, not again for {@link #SILENCE_REST}. Teams and the pvp rule are respected (a player
 * the swordsman can't harm is never touched). Bosses are only ever slowed: never lifted, thrown, pulled, held or silenced, as with
 * every spell.</p>
 *
 * <h2>Mending and drinking</h2>
 * <p>Whatever an art mends (Verdant's mending, Crimson's drinking), one body takes from all arts together at most {@link #MEND_CAP}
 * at once and {@link #MEND_CAP} over {@link #MEND_WINDOW} after ({@link #mendRoom}: a bucket that drains as time passes), so arts
 * buy time in a fight and never outpace it. What an art mends is written in health (half-hearts), not in W: a better blade doesn't
 * mend more, and {@code aura.art_damage} doesn't touch it.</p>
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

	/**
	 * The fastest an art drags a player that it pulls every tick or two (Collapse's well, Event Horizon), in blocks a tick: under a
	 * sprint's 0.13, so a player can always run out of a pull. (A single draw, a lash or a cut that pulls once, is a throw.)
	 */
	public static final double PVP_DRAG = 0.08;

	/** How fast a steady pull drags a target: as asked on a creature, {@link #PVP_DRAG} at most on a player, nothing on a boss. */
	public static double dragged(double speed, boolean player, boolean boss) {
		if (boss || speed <= 0) {
			return 0;
		}
		return player ? Math.min(speed, PVP_DRAG) : speed;
	}

	/**
	 * Hollow's silence on a player: their spells, their arts and the Aura key's techniques (all but the guard: a silenced swordsman may
	 * still defend) stop for at most this long, and no art silences the same player again for {@link #SILENCE_REST}.
	 */
	public static final int SILENCE_PLAYER_TICKS = 30;
	public static final int SILENCE_REST = 100;

	/** How long a silence lasts on a target: as asked on a creature, {@link #SILENCE_PLAYER_TICKS} at most on a player, nothing on a boss. */
	public static int silence(int ticks, boolean player, boolean boss) {
		if (boss || ticks <= 0) {
			return 0;
		}
		return player ? Math.min(ticks, SILENCE_PLAYER_TICKS) : ticks;
	}

	// ------------------------------------------------------------------ mending and drinking

	/** The most health arts mend one body at once, and how long that much takes to drain away again (so the steady most is a health a second). */
	public static final double MEND_CAP = 10.0;
	public static final int MEND_WINDOW = 200;

	/** What is left of the mending bucket {@code since} ticks after it held {@code level}: it drains at {@link #MEND_CAP} over {@link #MEND_WINDOW}. */
	public static double mendLevel(double level, long since) {
		return Math.max(0, Math.max(0, level) - Math.max(0, since) * MEND_CAP / MEND_WINDOW);
	}

	/** How much more health arts may mend a body whose bucket held {@code level} {@code since} ticks ago. */
	public static double mendRoom(double level, long since) {
		return Math.max(0, MEND_CAP - mendLevel(level, since));
	}

	/** What a drink gives back: {@code share} of what was {@code dealt}, never more than {@code cap}. */
	public static double drink(double dealt, double share, double cap) {
		return Math.max(0, Math.min(cap, Math.max(0, dealt) * Math.max(0, share)));
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
	/** (The balance pass across all fifty: 2 s to 2.5 s. The counter that needs the hardest timing held less than its slot's others.) */
	public static final int MIRROR_FREEZE = 50;
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
	/** (The balance pass: 0.35 to 0.4; it was the lightest Second Art by the model.) */
	public static final double SKYFALL_ARC_FACTOR = 0.4;

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
	/** (The balance pass: 20 to 18 blocks, so Gale, the method of reach, reaches furthest in all; still the longest single shape.) */
	public static final double SPEAR_LENGTH = 18.0;
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
	/** (The balance pass: 10 to 12 blocks, and a touch lighter, 0.5 to 0.45: Gale cuts from further than a sword should.) */
	public static final double BREEZE_RANGE = 12.0;
	public static final double BREEZE_SPEED = 2.0;
	public static final double BREEZE_WIDTH = 2.0;
	public static final double BREEZE_FACTOR = 0.45;
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
	/** (The balance pass: 3.2 to 3.5 round.) */
	public static final double EYE_RADIUS = 3.5;
	public static final double EYE_FACTOR = 0.8;
	public static final double EYE_PUSH = 0.75;
	public static final int EYE_TARGETS = 6;
	public static final int EYE_TICKS = 60;
	public static final double EYE_GUST_RADIUS = 2.5;
	public static final double EYE_GUST = 0.4;

	/** Tailwind (IV): a long dash, foes shoved aside, allies near swept along faster. */
	/** (The balance pass: 11 to 13 blocks.) */
	public static final double TAILWIND_DISTANCE = 13.0;
	public static final int TAILWIND_TICKS = 5;
	public static final double TAILWIND_WIDTH = 1.6;
	public static final double TAILWIND_FACTOR = 0.6;
	public static final double TAILWIND_SHOVE = 0.8;
	public static final int TAILWIND_TARGETS = 5;
	public static final double TAILWIND_ALLIES = 5.0;
	public static final int TAILWIND_SPEED = 100;

	/** Hundred Winds (V): you become a whirlwind of cuts, drawing foes in and lifting them all at its end. */
	/** (The balance pass: 5 to 5.5 round and each cut 0.22 to 0.24; it was the lightest Final Art by the model.) */
	public static final double WINDS_RADIUS = 5.5;
	public static final int WINDS_TICKS = 60;
	public static final int WINDS_PERIOD = 5;
	public static final double WINDS_PULL = 0.22;
	public static final double WINDS_FACTOR = 0.24;
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

	/**
	 * Unmoved (III): the one who struck hurled back by their own blow, and you hardened like stone for a while. (The balance pass:
	 * 0.9 to 1.0. Stone hits hardest of all on one foe, and its counter was its slot's lightest.)
	 */
	public static final double UNMOVED_FACTOR = 1.0;
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

	// ================================================================== Verdant: mending and binding

	/** Thorn Lash (I): a lash of thorned vine flicked out past the sword's reach; the first foe it catches is rooted, and pricked while held. */
	public static final double THORN_REACH = 4.5;
	public static final double THORN_DEGREES = 70;
	public static final double THORN_FACTOR = 0.45;
	public static final int THORN_TARGETS = 3;
	public static final int THORN_ROOT = 30;
	/** The thorns: this much every half second while the root holds, this many times. */
	public static final double THORN_PRICK = 0.07;
	public static final int THORN_PRICKS = 3;

	/** Blossom Fall (II): a falling cut, and a carpet of blossom where it lands that mends you and your allies and slows foes in it. */
	public static final double BLOSSOM_REACH = 3.3;
	public static final double BLOSSOM_DEGREES = 120;
	public static final double BLOSSOM_FACTOR = 0.75;
	public static final int BLOSSOM_TARGETS = 4;
	/** The blossom: this far ahead, this wide, mending this much at once and this much each second it stands. */
	public static final double BLOSSOM_AHEAD = 1.8;
	public static final double BLOSSOM_RADIUS = 3.5;
	public static final double BLOSSOM_MEND = 2.0;
	public static final int BLOSSOM_TICKS = 80;
	public static final double BLOSSOM_PULSE = 1.0;
	public static final int BLOSSOM_SLOW = 30;

	/** Rooted Parry (III): roots seize whoever struck, thorns burst round you, and you mend by what you caught. */
	public static final double ROOTED_FACTOR = 0.95;
	public static final int ROOTED_ROOT = 30;
	public static final double ROOTED_SHARE = 0.75;
	public static final double ROOTED_MIN = 3.0;
	public static final double ROOTED_MAX = 6.0;
	public static final double ROOTED_THORNS = 2.5;
	public static final double ROOTED_THORN_FACTOR = 0.3;

	/** What Rooted Parry mends: {@link #ROOTED_SHARE} of the caught blow, never less than {@link #ROOTED_MIN} nor more than {@link #ROOTED_MAX}. */
	public static double rootedMend(double caught) {
		return Math.max(ROOTED_MIN, Math.min(ROOTED_MAX, Math.max(0, caught) * ROOTED_SHARE));
	}

	/** Wild Growth (IV): a second rush, the foes in the way cut and snagged; brambles left along it slow and prick foes and mend allies. */
	public static final double WILD_DISTANCE = 7.0;
	public static final int WILD_TICKS = 5;
	public static final double WILD_WIDTH = 1.4;
	public static final double WILD_FACTOR = 0.65;
	public static final int WILD_TARGETS = 5;
	public static final int WILD_SNAG = 10;
	/** The brambles: standing this long; each second this much to a foe in them, and this much mended to an ally. */
	public static final int WILD_FIELD = 100;
	public static final double WILD_PRICK = 0.05;
	public static final double WILD_MEND = 1.0;

	/** Grove's Heart (V): the blade planted, roots bursting under every foe near, and a grove standing a while that mends allies and binds foes. */
	public static final double GROVE_RADIUS = 6.0;
	public static final double GROVE_FACTOR = 1.5;
	public static final int GROVE_TARGETS = 10;
	public static final int GROVE_ROOT = 50;
	/** The grove: standing this long; each second this much mended to an ally in it, and this much to a foe (slowed while it stays). */
	public static final int GROVE_TICKS = 160;
	public static final double GROVE_MEND = 1.0;
	public static final double GROVE_PRICK = 0.05;

	// ================================================================== Hollow: drawing in, silencing, stepping through

	/** Void Cut (I): a cut that tears a hole at the blade's end and draws the foes in front to you, shadowed and off balance. */
	public static final double VOID_REACH = 5.5;
	public static final double VOID_DEGREES = 70;
	public static final double VOID_FACTOR = 0.6;
	public static final int VOID_TARGETS = 3;
	/** The draw: once, this hard, to rest about this far in front of you; then slowed a moment. */
	public static final double VOID_DRAW = 0.95;
	public static final double VOID_STOP = 1.6;
	public static final int VOID_SLOW = 20;

	/** Collapse (II): a falling cut that opens a well ahead, dragging everything near into it, then collapsing on what it caught. */
	public static final double COLLAPSE_AHEAD = 2.5;
	public static final double COLLAPSE_PULL = 4.5;
	public static final int COLLAPSE_TICKS = 16;
	public static final double COLLAPSE_DRAG = 0.3;
	public static final double COLLAPSE_RADIUS = 2.2;
	public static final double COLLAPSE_CENTRE = 0.8;
	public static final double COLLAPSE_EDGE = 0.55;
	public static final int COLLAPSE_TARGETS = 8;

	/** Null Parry (III): the caught blow swallowed by the void, whoever struck cut and silenced, the foes near shoved off and nicked. */
	public static final double NULL_FACTOR = 1.0;
	/** The silence on a creature (a player's is held to {@link #SILENCE_PLAYER_TICKS}). */
	public static final int NULL_SILENCE = 60;
	public static final int NULL_HOLD = 8;
	public static final double NULL_PULSE = 3.0;
	public static final double NULL_PULSE_FACTOR = 0.25;
	public static final double NULL_SHOVE = 0.5;

	/** Rift Step (IV): through a rift to behind the foe ahead and a cut in its back; the rifts' edges drag in and cut those near them. */
	public static final double RIFT_REACH = 8.0;
	/** The foe it goes through: the nearest in this cone ahead (narrow, so it's the one you face, not one off to the side). */
	public static final double RIFT_DEGREES = 70;
	/** You come out of the far rift this far past the foe's back. */
	public static final double RIFT_BEHIND = 0.9;
	public static final double RIFT_FACTOR = 1.0;
	public static final double RIFT_EDGE = 2.5;
	public static final double RIFT_EDGE_FACTOR = 0.35;
	public static final int RIFT_EDGE_TARGETS = 4;
	public static final double RIFT_DRAW = 0.45;
	public static final int RIFT_SLOW = 30;

	/** Event Horizon (V): a black sphere cut into the air ahead, dragging in everything near, grinding what it holds, then crushing it. */
	public static final double HORIZON_AHEAD = 4.5;
	public static final double HORIZON_HEIGHT = 1.3;
	public static final double HORIZON_PULL = 7.0;
	public static final int HORIZON_TICKS = 40;
	public static final double HORIZON_DRAG = 0.3;
	/** The grip: whatever is this close to the sphere is slowed hard and ground for this much every few ticks. */
	public static final double HORIZON_CORE = 2.5;
	public static final int HORIZON_GRIP_PERIOD = 5;
	public static final double HORIZON_GRIP = 0.12;
	public static final double HORIZON_CRUSH_RADIUS = 3.5;
	public static final double HORIZON_CRUSH = 1.4;
	public static final double HORIZON_CRUSH_EDGE = 0.9;
	public static final int HORIZON_TARGETS = 12;

	// ================================================================== Starlit: stars that mark and burst, and aura given back

	/** How long a star an art sets on a foe lasts, for the next Starlit art to burst. */
	public static final int STAR_TICKS = 100;

	/** Star Needle (I): a thrust that looses three darts of starlight; each that strikes gives aura back and sets a star on its foe. */
	public static final int NEEDLE_DARTS = 3;
	public static final double NEEDLE_RANGE = 9.0;
	public static final double NEEDLE_SPEED = 1.5;
	/** Degrees between the darts as they leave, and how hard each turns toward the foe nearest its path, a tick. */
	public static final double NEEDLE_SPREAD = 8.0;
	public static final double NEEDLE_SEEK = 0.35;
	public static final double NEEDLE_FACTOR = 0.22;
	public static final double NEEDLE_AURA = 0.5;

	/** Meteor Shower (II): stars brought down over a circle ahead, the last a great one; each foe struck gives aura back, the great star stars them. */
	public static final double METEOR_AHEAD = 4.0;
	public static final double METEOR_RADIUS = 3.0;
	public static final int METEOR_STARS = 5;
	public static final int METEOR_TICKS = 18;
	public static final double METEOR_REACH = 1.4;
	public static final double METEOR_FACTOR = 0.25;
	public static final int METEOR_PER_FOE = 3;
	public static final double METEOR_GREAT = 0.4;
	public static final double METEOR_GREAT_REACH = 2.0;
	public static final double METEOR_AURA = 1.0;
	public static final int METEOR_AURA_FOES = 4;

	/** Constellation Guard (III): whoever struck cut and set with stars that burst one by one a moment later, each giving aura back. */
	public static final double CONSTELLATION_FACTOR = 0.6;
	public static final int CONSTELLATION_STARS = 4;
	public static final int CONSTELLATION_DELAY = 30;
	public static final int CONSTELLATION_GAP = 4;
	public static final double CONSTELLATION_BURST = 0.2;
	public static final double CONSTELLATION_AURA = 0.75;
	/** Every other foe this near that already carries a star bursts at once, this hard; a star whose foe fell bursts where it fell, on those this near. */
	public static final double CONSTELLATION_REACH = 6.0;
	public static final double CONSTELLATION_STARRED = 0.35;
	public static final double CONSTELLATION_SPLASH = 2.0;

	/** Comet Dash (IV): a second rush trailing stars, the foes in the way cut; a moment later the trail bursts star by star, each foe caught giving aura back. */
	public static final double COMET_DISTANCE = 8.0;
	public static final int COMET_TICKS = 4;
	public static final double COMET_WIDTH = 1.4;
	public static final double COMET_FACTOR = 0.6;
	public static final int COMET_TARGETS = 5;
	public static final int COMET_DELAY = 12;
	public static final double COMET_BURST_REACH = 1.5;
	public static final double COMET_BURST = 0.45;
	public static final double COMET_AURA = 1.0;
	public static final int COMET_AURA_FOES = 5;

	/** Nova (V): starlight gathered on the blade, then a ring of it bursting out; starred foes burst with it, and aura comes back for each foe struck. */
	public static final int NOVA_GATHER = 12;
	public static final double NOVA_RADIUS = 7.0;
	public static final double NOVA_CENTRE = 1.9;
	public static final double NOVA_EDGE = 1.3;
	public static final int NOVA_TARGETS = 12;
	public static final double NOVA_STARRED = 0.4;
	public static final double NOVA_THROW = 0.6;
	public static final double NOVA_AURA = 2.5;
	public static final double NOVA_AURA_MAX = 20.0;

	/** The aura Nova gives back for {@code foes} struck: {@link #NOVA_AURA} each, at most {@link #NOVA_AURA_MAX} (half what it cost). */
	public static double novaAura(int foes) {
		return Math.min(NOVA_AURA_MAX, Math.max(0, foes) * NOVA_AURA);
	}

	// ================================================================== Hourglass: echoes, rewinds, moments held still

	/** Echo Cut (I): a cut, and an afterimage left where you stood that repeats it a moment later. */
	public static final double ECHO_REACH = 3.3;
	public static final double ECHO_DEGREES = 120;
	public static final double ECHO_FACTOR = 0.5;
	public static final int ECHO_TARGETS = 4;
	public static final int ECHO_DELAY = 12;
	public static final double ECHO_REPEAT = 0.5;
	/** The echo's cut reaches a little further than the cut (a foe the first knocked back a step is still in it). */
	public static final double ECHO_REPEAT_REACH = 4.0;

	/** Rewind Leap (II): a falling cut that drags its foes in time, then time snaps you back to where you leapt from. */
	public static final double REWIND_REACH = 3.3;
	public static final double REWIND_DEGREES = 120;
	public static final double REWIND_FACTOR = 0.95;
	public static final int REWIND_TARGETS = 4;
	public static final int REWIND_DRAG = 40;
	public static final int REWIND_DELAY = 6;
	/** The snap back goes at most this far, to where you left the ground no longer ago than this. */
	public static final double REWIND_MAX = 10.0;
	public static final int REWIND_MEMORY = 50;

	/** Stopped Moment (III): whoever struck held still in time, then flung back by the moment's snap as time starts again; those near dragged. */
	public static final double STOPPED_FACTOR = 0.8;
	public static final int STOPPED_HOLD = 50;
	public static final double STOPPED_SNAP = 0.25;
	public static final double STOPPED_THROW = 0.6;
	public static final double STOPPED_DRAG_REACH = 3.0;
	public static final int STOPPED_DRAG = 20;

	/** Blur (IV): a rush faster than any, the foes in the way cut; for a while time drags round you: foes and projectiles slowed, you quickened. */
	public static final double BLUR_DISTANCE = 7.0;
	public static final int BLUR_TICKS = 3;
	public static final double BLUR_WIDTH = 1.4;
	public static final double BLUR_FACTOR = 0.7;
	public static final int BLUR_TARGETS = 5;
	public static final double BLUR_RADIUS = 5.0;
	public static final int BLUR_FIELD = 60;
	/** A projectile entering the drag goes on at this share of its speed. */
	public static final double BLUR_ARROW = 0.35;

	/** Thousand Moments (V): time stopped round you; your cuts gather on every foe held, and land all at once when it moves again. */
	public static final double THOUSAND_RADIUS = 7.0;
	public static final int THOUSAND_HOLD = 50;
	public static final int THOUSAND_CUTS = 10;
	public static final double THOUSAND_FACTOR = 2.2;
	public static final int THOUSAND_TARGETS = 10;
	/** A share of each of your own blows on a stopped foe is stored and lands again with the rest, up to this many weapons a foe. */
	public static final double THOUSAND_STORE = 0.5;
	public static final double THOUSAND_STORE_MAX = 1.0;

	/** What Thousand Moments holds on a foe after one more blow that took {@code taken}: a share of it on top of {@code had}, held to {@code max}. */
	public static double stored(double had, double taken, double max) {
		return Math.min(Math.max(0, max), Math.max(0, had) + Math.max(0, taken) * THOUSAND_STORE);
	}

	// ================================================================== Crimson: bleeding, drinking, frenzy and the price of it

	/** How often a wound an art opens bleeds, and how much harder it bleeds while its bearer is on the move (as the Bleed spell's does). */
	public static final int BLEED_PERIOD = 10;
	public static final double BLEED_MOVING = 1.5;

	/** Bloodletting (I): a deep cut that opens a wound; it bleeds three seconds, and you drink a share of what it bleeds. */
	public static final double BLOOD_REACH = 3.3;
	public static final double BLOOD_DEGREES = 90;
	public static final double BLOOD_FACTOR = 0.55;
	public static final int BLOOD_TARGETS = 3;
	public static final int BLOOD_BLEEDS = 6;
	public static final double BLOOD_BLEED = 0.08;
	public static final double BLOOD_DRINK = 0.25;

	/** Red Rain (II): a falling cut that bursts where it lands, and a red rain there a while: foes bleed, and you drink from all of it. */
	public static final double RAIN_AHEAD = 2.0;
	public static final double RAIN_RADIUS = 3.5;
	public static final double RAIN_FACTOR = 0.7;
	public static final int RAIN_TARGETS = 6;
	public static final int RAIN_TICKS = 40;
	public static final double RAIN_BLEED = 0.08;
	public static final double RAIN_DRINK = 0.3;
	public static final double RAIN_DRINK_MAX = 4.0;

	/** Sanguine Parry (III): the caught blow turned into a wound in whoever struck, bleeding its worth back out; you drink half of all of it. */
	public static final double SANGUINE_FACTOR = 0.7;
	public static final double SANGUINE_WOUND_MAX = 1.0;
	public static final int SANGUINE_BLEEDS = 6;
	public static final double SANGUINE_DRINK = 0.5;
	public static final int SANGUINE_HOLD = 8;
	public static final double SANGUINE_SPRAY = 2.5;
	public static final double SANGUINE_SPRAY_FACTOR = 0.25;

	/** The wound Sanguine Parry opens, in damage in all: the caught blow, held to {@link #SANGUINE_WOUND_MAX} weapons. */
	public static double sanguineWound(double weapon, double caught) {
		return Math.max(0, Math.min(caught, weapon * SANGUINE_WOUND_MAX));
	}

	/** Frenzy (IV): a second rush through foes; each cut, and each blow of yours that lands after, adds to a frenzy that quickens your blade. */
	public static final double FRENZY_DISTANCE = 7.0;
	public static final int FRENZY_TICKS = 4;
	public static final double FRENZY_WIDTH = 1.4;
	public static final double FRENZY_FACTOR = 0.7;
	public static final int FRENZY_TARGETS = 5;
	public static final int FRENZY_STACKS = 4;
	public static final double FRENZY_SPEED = 0.05;
	public static final int FRENZY_TIME = 100;

	/** How much faster a frenzy of {@code stacks} swings the blade (a share of its attack speed): {@link #FRENZY_SPEED} each, up to {@link #FRENZY_STACKS}. */
	public static double frenzy(int stacks) {
		return FRENZY_SPEED * Math.max(0, Math.min(FRENZY_STACKS, stacks));
	}

	/** Crimson Moon (V): a great arc of blood-light that opens wounds in every foe before you and drinks deeply from them, paid in your own health. */
	public static final double MOON_RADIUS = 6.0;
	public static final double MOON_DEGREES = 180;
	public static final double MOON_FACTOR = 2.5;
	public static final int MOON_TARGETS = 10;
	public static final int MOON_BLEEDS = 6;
	public static final double MOON_BLEED = 0.1;
	public static final double MOON_DRINK = 0.5;
	public static final double MOON_DRINK_MAX = 10.0;
	/** The price: a quarter of the swordsman's greatest health, taken straight from their health (not dealt as damage). */
	public static final double MOON_TOLL = 0.25;
	/** Crimson Moon never leaves its swordsman with less health than this (a heart), so it can never kill them. */
	public static final double MOON_FLOOR = 2.0;

	/**
	 * What Crimson Moon takes from a swordsman with {@code health} of {@code max}: {@link #MOON_TOLL} of their greatest health, never
	 * past {@link #MOON_FLOOR} (a swordsman already down to a heart pays nothing more). Never negative, and never their life.
	 */
	public static double moonToll(double health, double max) {
		return Math.max(0, Math.min(Math.max(0, max) * MOON_TOLL, health - MOON_FLOOR));
	}

	// ================================================================== the arts, priced and weighed

	/**
	 * The kinds of thing an art does, a word each: what tells one method's answer from another's. Each method has kinds of its own
	 * that every one of its arts carries and that another method borrows at most once ({@code ArtRulesTest}), so a later art that
	 * takes another method's signature is caught.
	 */
	public enum Kind {
		/** Sets alight. */
		FIRE,
		/** Slows with frost, crusts. */
		FROST,
		/** Freezes solid (the mod's freeze, for a mage's Shatter). */
		FREEZE,
		/** Lightning: interrupts, ionises, a twitch of a hold. */
		SHOCK,
		/** Leaps from foe to foe. */
		CHAIN,
		/** Wind: pushes, turns projectiles, carries allies along. */
		WIND,
		/** Throws a foe up. */
		LIFT,
		/** Throws a foe back. */
		THROW,
		/** Stuns a foe a moment. */
		HOLD,
		/** Slows a foe. */
		SLOW,
		/** Shakes, cracks or splits the ground. */
		QUAKE,
		/** Hardens its own swordsman. */
		HARDEN,
		/** Roots a foe where it stands (it can still strike). */
		ROOT,
		/** Mends its swordsman or their allies (life's own mending, not a drink). */
		MEND,
		/** Draws foes in toward a point. */
		PULL,
		/** Silences a foe's spells and abilities. */
		SILENCE,
		/** Sets stars on foes for a later art to burst. */
		STAR,
		/** Gives aura back. */
		AURA,
		/** Strikes again a moment later. */
		ECHO,
		/** Puts its swordsman back where they were. */
		REWIND,
		/** Holds foes still in time. */
		STILL,
		/** Drags foes in time, or slows the world round its swordsman. */
		DRAG,
		/** Opens wounds that bleed. */
		BLEED,
		/** Mends its swordsman by what it deals. */
		DRINK,
		/** Costs its own swordsman health. */
		TOLL,
		/** Quickens its swordsman's blade as it lands. */
		FRENZY,
		/** Rushes or blinks its swordsman on. */
		MOVE,
		/** Leaves something on the ground a while. */
		FIELD,
		/** Turns or slows projectiles. */
		WARD
	}

	/**
	 * One art's price and rest, and what it's worth in the balance model ({@link #power}): {@code primary} is what its main foe
	 * takes (in weapon damage; fire counted at a seventh of a weapon a second, a wound's bleeding at what it bleeds, and a bonus it
	 * often earns at about a third of it), {@code area} what it may do to the others it can reach (in all, at a fair count of them),
	 * {@code control} the seconds it holds, roots, slows, throws, silences or turns a foe away (or hardens or speeds its swordsman or
	 * an ally), {@code reach} how far from where its swordsman stood it reaches or carries them (blocks), {@code mend} what it mends
	 * its swordsman and their allies (in W: a W is about seven health), {@code aura} the aura it gives back (at a fair count of foes),
	 * {@code toll} the health it costs its own swordsman (in W), and {@code kinds} what it does, in a word each.
	 */
	public record Art(String id, String method, int slot, double cost, int cooldown, double primary, double area, double control, double reach,
			double mend, double aura, double toll, Set<Kind> kinds) {
		public Art {
			kinds = kinds == null || kinds.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(kinds));
		}

		public int stage() {
			return slot + AuraRules.GLOW;
		}

		/** Whether it does {@code kind}. */
		public boolean is(Kind kind) {
			return kinds.contains(kind);
		}

		/** The same art resting {@code ticks} (a lighter art may rest a little less than its slot). */
		Art rests(int ticks) {
			return new Art(id, method, slot, cost, ticks, primary, area, control, reach, mend, aura, toll, kinds);
		}

		/** The same art mending {@code w} (in W). */
		Art mends(double w) {
			return new Art(id, method, slot, cost, cooldown, primary, area, control, reach, w, aura, toll, kinds);
		}

		/** The same art giving back {@code amount} aura. */
		Art gives(double amount) {
			return new Art(id, method, slot, cost, cooldown, primary, area, control, reach, mend, amount, toll, kinds);
		}

		/** The same art costing its swordsman {@code w} of health (in W). */
		Art costs(double w) {
			return new Art(id, method, slot, cost, cooldown, primary, area, control, reach, mend, aura, w, kinds);
		}
	}

	/** An art at its slot's price and rest. */
	private static Art art(String id, String method, int slot, double primary, double area, double control, double reach, Kind... kinds) {
		return new Art(id, method, slot, SLOT_COST[slot], SLOT_COOLDOWN[slot], primary, area, control, reach, 0, 0, 0, Set.of(kinds));
	}

	/** Fire, counted in weapon damage: vanilla fire burns a point a second, and a weapon is about seven. */
	static double fire(int ticks) {
		return ticks / 20.0 / 7.0;
	}

	/** Health, counted in weapon damage (a W is about seven). */
	static double health(double amount) {
		return amount / 7.0;
	}

	public static final List<Art> ARTS = List.of(
		// Ember: damage now and fire after.
		art("kindling_draw", "ember", 0, KINDLING_FACTOR + fire(KINDLING_IGNITE), 0.3, 0.0, KINDLING_LINE_FROM + KINDLING_LINE, Kind.FIRE, Kind.FIELD),
		art("rising_cinders", "ember", 1, CINDERS_FACTOR + fire(CINDERS_IGNITE), 0.5 + CINDERS_RAIN_FACTOR, 1.0, CINDERS_REACH, Kind.FIRE, Kind.LIFT),
		art("backdraft", "ember", 2, BACKDRAFT_FACTOR + 0.35 + fire(BACKDRAFT_IGNITE), 0.45, 0.5, BACKDRAFT_REACH, Kind.FIRE, Kind.THROW),
		art("wildfire_rush", "ember", 3, WILDFIRE_FACTOR + fire(WILDFIRE_IGNITE), 0.8, 0.0, WILDFIRE_DISTANCE + 3, Kind.FIRE, Kind.MOVE, Kind.FIELD),
		art("sunfall", "ember", 4, SUNFALL_CENTRE + fire(SUNFALL_IGNITE), 1.6, 1.5, SUNFALL_RING + 0.5, Kind.FIRE, Kind.THROW, Kind.FIELD),
		// Rime: less damage, the most hold.
		art("frostbite", "rime", 0, FROSTBITE_FACTOR, 0.4, 1.6, FROSTBITE_REACH, Kind.FROST, Kind.FREEZE),
		art("hailfall", "rime", 1, HAIL_CUT_FACTOR + 2 * HAIL_STONE_FACTOR, 0.5, 1.5, 5.0, Kind.FROST),
		art("glacier_mirror", "rime", 2, MIRROR_FACTOR, 0.2, MIRROR_FREEZE / 20.0 + 1.0, 3.0, Kind.FREEZE, Kind.FROST, Kind.WARD),
		art("skate", "rime", 3, SKATE_FACTOR + 0.3, 0.4, 1.6, SKATE_DISTANCE + 3, Kind.FROST, Kind.FREEZE, Kind.MOVE, Kind.FIELD),
		art("winters_hush", "rime", 4, HUSH_FACTOR + HUSH_SHATTER, 1.6, 2.5, HUSH_REACH, Kind.FREEZE),
		// Thunder: many foes, a little each, and interrupts.
		art("crackle", "thunder", 0, CRACKLE_CUTS * CRACKLE_FACTOR, CRACKLE_CUTS * CRACKLE_SPARK, 0.6, CRACKLE_REACH, Kind.SHOCK, Kind.CHAIN).rests(50),
		art("skyfall", "thunder", 1, SKYFALL_FACTOR, SKYFALL_ARCS * SKYFALL_ARC_FACTOR, 0.5, SKYFALL_AHEAD + SKYFALL_RADIUS, Kind.SHOCK, Kind.CHAIN),
		art("static_riposte", "thunder", 2, RIPOSTE_FACTOR, 1.2, 1.2, RIPOSTE_REACH, Kind.SHOCK, Kind.CHAIN),
		art("bolt_step", "thunder", 3, BOLT_FACTOR, 1.05, 0.5, BOLT_REACH + 3, Kind.SHOCK, Kind.MOVE),
		art("heavens_spear", "thunder", 4, SPEAR_FACTOR, 1.6, 1.0, SPEAR_LENGTH, Kind.SHOCK),
		// Gale: reach, and the ground taken from under them.
		art("cutting_breeze", "gale", 0, BREEZE_FACTOR, 0.4, 0.4, BREEZE_RANGE, Kind.WIND),
		art("updraft", "gale", 1, UPDRAFT_FACTOR + 0.3, 0.4, 1.8, 5.0, Kind.WIND, Kind.LIFT),
		art("eye_of_the_storm", "gale", 2, EYE_FACTOR, 0.6, 2.5, EYE_RADIUS, Kind.WIND, Kind.THROW, Kind.WARD),
		art("tailwind", "gale", 3, TAILWIND_FACTOR, 0.4, 2.0, TAILWIND_DISTANCE + 3, Kind.WIND, Kind.MOVE),
		art("hundred_winds", "gale", 4, WINDS_FACTOR * 8 + WINDS_FINISH, 1.6, 2.4, WINDS_RADIUS + 2, Kind.WIND, Kind.PULL, Kind.LIFT),
		// Stone: heavy hits, footing taken, and standing firm.
		art("rockbreaker", "stone", 0, ROCK_FACTOR, ROCK_SHOCK_FACTOR, 1.2, ROCK_REACH, Kind.QUAKE, Kind.HOLD, Kind.SLOW),
		art("avalanche", "stone", 1, AVALANCHE_CENTRE, 0.9, 1.0, AVALANCHE_RADIUS, Kind.QUAKE, Kind.THROW, Kind.SLOW),
		art("unmoved", "stone", 2, UNMOVED_FACTOR, 0.0, 3.6, 3.0, Kind.HARDEN, Kind.THROW, Kind.HOLD),
		art("landslide", "stone", 3, LANDSLIDE_FACTOR + 0.2, 0.5, 0.8, LANDSLIDE_DISTANCE + 3, Kind.QUAKE, Kind.MOVE, Kind.THROW, Kind.HOLD),
		art("mountain_splitter", "stone", 4, SPLITTER_FACTOR, 2.0, 1.4, SPLITTER_LENGTH, Kind.QUAKE, Kind.LIFT, Kind.HOLD),
		// Verdant: the least damage, the most mending, and roots.
		art("thorn_lash", "verdant", 0, THORN_FACTOR + THORN_PRICKS * THORN_PRICK, 0.35, THORN_ROOT / 20.0, THORN_REACH, Kind.ROOT),
		art("blossom_fall", "verdant", 1, BLOSSOM_FACTOR, 0.45, 0.5, BLOSSOM_AHEAD + 1.7, Kind.MEND, Kind.SLOW, Kind.FIELD)
			.mends(health(BLOSSOM_MEND + 4 * BLOSSOM_PULSE) + 0.04),
		art("rooted_parry", "verdant", 2, ROOTED_FACTOR, ROOTED_THORN_FACTOR, ROOTED_ROOT / 20.0, 3.0, Kind.ROOT, Kind.MEND).mends(health(4.9)),
		art("wild_growth", "verdant", 3, WILD_FACTOR + 3 * WILD_PRICK, 0.45, 1.2, WILD_DISTANCE + 3, Kind.MOVE, Kind.ROOT, Kind.SLOW, Kind.MEND, Kind.FIELD)
			.mends(health(2.8)),
		art("groves_heart", "verdant", 4, GROVE_FACTOR + 8 * GROVE_PRICK, 1.4, 3.0, GROVE_RADIUS, Kind.ROOT, Kind.MEND, Kind.SLOW, Kind.FIELD)
			.mends(health(8.4)),
		// Hollow: drawing foes in, silencing them, stepping through.
		art("void_cut", "hollow", 0, VOID_FACTOR, 0.35, 1.2, VOID_REACH, Kind.PULL, Kind.SLOW),
		art("collapse", "hollow", 1, COLLAPSE_CENTRE, 0.75, 1.0, COLLAPSE_AHEAD + COLLAPSE_PULL, Kind.PULL),
		art("null_parry", "hollow", 2, NULL_FACTOR, NULL_PULSE_FACTOR, 3.2, 3.0, Kind.SILENCE, Kind.HOLD, Kind.THROW),
		art("rift_step", "hollow", 3, RIFT_FACTOR, 0.6, 1.0, RIFT_REACH + 3, Kind.MOVE, Kind.PULL, Kind.SLOW),
		art("event_horizon", "hollow", 4, HORIZON_TICKS / HORIZON_GRIP_PERIOD * HORIZON_GRIP + HORIZON_CRUSH, 1.6, 2.5, HORIZON_AHEAD + HORIZON_PULL,
			Kind.PULL, Kind.SLOW),
		// Starlit: stars that burst, and aura given back.
		art("star_needle", "starlit", 0, NEEDLE_DARTS * NEEDLE_FACTOR, 0.2, 0.0, NEEDLE_RANGE, Kind.STAR, Kind.AURA).gives(NEEDLE_DARTS * NEEDLE_AURA),
		art("meteor_shower", "starlit", 1, METEOR_PER_FOE * METEOR_FACTOR + METEOR_GREAT, 0.6, 0.0, METEOR_AHEAD + 1.5, Kind.STAR, Kind.AURA)
			.gives(2 * METEOR_AURA),
		art("constellation_guard", "starlit", 2, CONSTELLATION_FACTOR + CONSTELLATION_STARS * CONSTELLATION_BURST, 0.25, 0.0, 3.0, Kind.STAR, Kind.AURA)
			.gives(CONSTELLATION_STARS * CONSTELLATION_AURA),
		art("comet_dash", "starlit", 3, COMET_FACTOR + COMET_BURST, 0.7, 0.0, COMET_DISTANCE + 3, Kind.MOVE, Kind.STAR, Kind.AURA).gives(2 * COMET_AURA),
		art("nova", "starlit", 4, NOVA_CENTRE + NOVA_STARRED, 1.6, 0.5, NOVA_RADIUS, Kind.AURA, Kind.STAR, Kind.THROW).gives(novaAura(3) + 0.5),
		// Hourglass: echoes, rewinds, and moments held still.
		art("echo_cut", "hourglass", 0, ECHO_FACTOR + ECHO_REPEAT, 0.45, 0.0, ECHO_REACH, Kind.ECHO),
		art("rewind_leap", "hourglass", 1, REWIND_FACTOR, 0.5, REWIND_DRAG / 20.0, REWIND_REACH, Kind.REWIND, Kind.DRAG),
		art("stopped_moment", "hourglass", 2, STOPPED_FACTOR + STOPPED_SNAP, 0.1, STOPPED_HOLD / 20.0 + 0.8, 3.0, Kind.STILL, Kind.THROW, Kind.DRAG),
		art("blur", "hourglass", 3, BLUR_FACTOR, 0.45, 2.5, BLUR_DISTANCE + 3, Kind.MOVE, Kind.DRAG, Kind.FIELD, Kind.WARD),
		art("thousand_moments", "hourglass", 4, THOUSAND_FACTOR + 0.3, 1.6, THOUSAND_HOLD / 20.0, THOUSAND_RADIUS, Kind.STILL, Kind.ECHO),
		// Crimson: bleeding and drinking, the most damage after Ember's, and a price.
		art("bloodletting", "crimson", 0, BLOOD_FACTOR + BLOOD_BLEEDS * BLOOD_BLEED, 0.25, 0.0, BLOOD_REACH, Kind.BLEED, Kind.DRINK).mends(0.12),
		art("red_rain", "crimson", 1, RAIN_FACTOR + 4 * RAIN_BLEED, 0.7, 0.0, RAIN_AHEAD + RAIN_RADIUS, Kind.BLEED, Kind.DRINK, Kind.FIELD).mends(0.4),
		art("sanguine_parry", "crimson", 2, SANGUINE_FACTOR + 0.6, SANGUINE_SPRAY_FACTOR - 0.05, SANGUINE_HOLD / 20.0, 3.0, Kind.BLEED, Kind.DRINK, Kind.HOLD)
			.mends(0.6),
		art("frenzy", "crimson", 3, FRENZY_FACTOR + 0.6, 0.5, 0.0, FRENZY_DISTANCE + 3, Kind.MOVE, Kind.FRENZY),
		art("crimson_moon", "crimson", 4, MOON_FACTOR + MOON_BLEEDS * MOON_BLEED, 1.6, 0.0, MOON_RADIUS, Kind.BLEED, Kind.DRINK, Kind.TOLL)
			.mends(health(8.0)).costs(health(5.0)));

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
	/** What a point of aura given back is worth in the balance model, in weapon damage. */
	public static final double AURA_WORTH = 0.1;

	/**
	 * What an art is worth in the balance model: its main foe's damage, three fifths of what it may do to others and of what it
	 * mends (it doesn't always find them, or a wound), a quarter of a weapon for each second of control, {@link #REACH_WORTH} for
	 * each block it reaches past a sword, {@link #AURA_WORTH} for each point of aura it gives back, less whatever health it costs
	 * its own swordsman. Rough, and meant to be: it's what keeps one method's answer from outgrowing another's.
	 */
	public static double power(Art art) {
		return art.primary() + 0.6 * (art.area() + art.mend()) + 0.25 * art.control() + Math.max(0, art.reach() - 3.0) * REACH_WORTH
			+ art.aura() * AURA_WORTH - art.toll();
	}

	/** What an art in each slot should be worth (see {@link #power}), the First to the Final. */
	public static final double[] SLOT_POWER = {1.3, 1.8, 1.9, 2.15, 4.35};
	/** How far one art may stray from its slot's worth, as a share. */
	public static final double POWER_SPREAD = 0.12;
}
