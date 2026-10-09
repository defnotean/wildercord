package dev.wildercord.spell;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The hearth pack: 17 links and 36 modifiers for everyday magic (mining, farming, fishing, travel, care), most of them
 * as useful away from a fight as in one. This class holds their pure rules (what each can attach to, what it refuses,
 * how a spell reads with it, and its numbers) so they can be tested without a world; {@code cast.HearthLinks} and
 * {@code cast.HearthModifiers} carry them out.
 *
 * <p>Each hearth modifier attaches to the closest rune on its left that it can change, like any other. Rather than
 * give old runes new traits, the traits below are worked out from what an effect is (see {@link #derived}):
 * a world effect breaks or shapes blocks, a harmful or moving one strikes creatures, a helpful one aids them, a fire
 * one burns. At most one of each hearth modifier per effect.
 */
public final class HearthLinkRules {
	private HearthLinkRules() {}

	// ------------------------------------------------------------------ derived traits

	/** An effect that harms or moves creatures (harmful and movement effects). */
	public static final String HARMS = "hearth_harms";
	/** An effect that helps creatures. */
	public static final String HELPS = "hearth_helps";
	/** A world effect: one that breaks, grows or shapes blocks. */
	public static final String WORLD = "hearth_world";
	/** An effect that moves creatures. */
	public static final String MOVES = "hearth_moves";
	/** A fire effect. */
	public static final String BURNS = "hearth_burns";

	/** Whether {@code rune} has one of the derived hearth traits (asked by {@link RuneDef#has}). */
	public static boolean derived(RuneDef rune, String trait) {
		if (trait == null || !trait.startsWith("hearth_") || rune.family() != RuneFamily.EFFECT) {
			return false;
		}
		return switch (trait) {
			case HARMS -> rune.kind() == EffectKind.HARMFUL || rune.kind() == EffectKind.MOVEMENT;
			case HELPS -> rune.kind() == EffectKind.HELPFUL;
			case WORLD -> rune.kind() == EffectKind.WORLD;
			case MOVES -> rune.kind() == EffectKind.MOVEMENT;
			case BURNS -> "fire".equals(rune.element());
			default -> false;
		};
	}

	// ------------------------------------------------------------------ the roster

	/** The hearth modifiers by path, and the trait each needs. */
	public static final Map<String, String> MODIFIERS = Map.ofEntries(
		Map.entry("tidy", WORLD), Map.entry("replanting", WORLD), Map.entry("kilned", WORLD), Map.entry("silken", WORLD),
		Map.entry("windfall", WORLD), Map.entry("veinfollow", WORLD), Map.entry("timbering", WORLD), Map.entry("level_ground", WORLD),
		Map.entry("gentle", HARMS), Map.entry("sparing", HARMS), Map.entry("culling", HARMS), Map.entry("headhunting", HARMS),
		Map.entry("hallowed", HARMS), Map.entry("tapering", HARMS), Map.entry("pooled", HARMS), Map.entry("soothing", HARMS),
		Map.entry("bountiful", HARMS), Map.entry("fetching", HARMS), Map.entry("cushioned", MOVES), Map.entry("damp", BURNS),
		Map.entry("inward", HELPS), Map.entry("selfless", HELPS), Map.entry("triage", HELPS), Map.entry("mending", HELPS),
		Map.entry("nourishing", HELPS), Map.entry("purifying", HELPS), Map.entry("matchmaking", HELPS),
		Map.entry("magnetic", Trait.FRUGAL), Map.entry("sowing", Trait.FRUGAL), Map.entry("furrowing", Trait.FRUGAL),
		Map.entry("fertile", Trait.FRUGAL), Map.entry("torchset", Trait.FRUGAL), Map.entry("ore_sensing", Trait.FRUGAL),
		Map.entry("fleecing", Trait.FRUGAL), Map.entry("sunlit", Trait.FRUGAL), Map.entry("steady", Trait.FRUGAL));

	/** The hearth conditions: the rest of the spell fires only if they hold, from where the spell already is. */
	public static final Set<String> CONDITIONS = Set.of("if_night", "if_day", "if_raining", "if_underground", "if_alone", "if_near_ally",
		"if_unhurt", "if_holding_tool", "if_brimming", "if_in_fields");

	/** The hearth watchers: the rest of the spell waits for something you do, then fires where it happened. */
	public static final Set<String> WATCHERS = Set.of("on_mine", "on_harvest", "on_catch", "on_sprint", "on_splash", "on_mount", "on_wake");

	/** Modifiers that each decide what a broken block drops: one per effect. */
	public static final Set<String> DROP_RULES = Set.of("kilned", "silken", "windfall");

	public static boolean ownsModifier(RuneDef rune) {
		return rune.family() == RuneFamily.MODIFIER && builtIn(rune) && MODIFIERS.containsKey(rune.path());
	}

	public static boolean ownsLink(RuneDef rune) {
		return rune.family() == RuneFamily.LINK && builtIn(rune) && (CONDITIONS.contains(rune.path()) || WATCHERS.contains(rune.path()));
	}

	public static boolean ownsLink(String id) {
		return id.startsWith("wildercord:") && (CONDITIONS.contains(id.substring(11)) || WATCHERS.contains(id.substring(11)));
	}

	/** A hearth condition keeps the spell's current shape after it, like If Wet; a watcher starts from where it was set off. */
	public static boolean keepsShape(RuneDef link) {
		return link.family() == RuneFamily.LINK && builtIn(link) && CONDITIONS.contains(link.path());
	}

	public static boolean isWatcher(String id) {
		return id.startsWith("wildercord:") && WATCHERS.contains(id.substring(11));
	}

	private static boolean builtIn(RuneDef rune) {
		return rune.id().startsWith("wildercord:");
	}

	// ------------------------------------------------------------------ placement

	/**
	 * Why {@code modifier} can't go where it was written, or null if it can (or isn't a hearth modifier).
	 *
	 * @param targetMods the modifiers already on the rune it would attach to, or null if nothing on its left can take it
	 */
	public static String refusal(RuneDef modifier, List<RuneDef> targetMods) {
		if (!ownsModifier(modifier)) {
			return null;
		}
		String path = modifier.path();
		if (targetMods == null) {
			return modifier.name() + " does nothing here: it needs " + needsPhrase(MODIFIERS.get(path)) + " on its left.";
		}
		for (RuneDef other : targetMods) {
			if (!ownsModifier(other) || other.path().equals(path)) {
				continue;
			}
			String o = other.path();
			if (DROP_RULES.contains(path) && DROP_RULES.contains(o)) {
				return modifier.name() + " and " + other.name() + " both decide what a block drops: only one per effect.";
			}
			if (path.equals("inward") && o.equals("selfless") || path.equals("selfless") && o.equals("inward")) {
				return modifier.name() + " and " + other.name() + " pull opposite ways: only one per effect.";
			}
			if (path.equals("steady") && MODIFIERS.get(o).equals(WORLD) || o.equals("steady") && MODIFIERS.get(path).equals(WORLD)) {
				return "Steady stops the effect changing any block, so " + (path.equals("steady") ? other.name() : modifier.name())
					+ " would have nothing to work on.";
			}
		}
		return null;
	}

	/** What a hearth modifier needs on its left, in words. */
	public static String needsPhrase(String trait) {
		return switch (trait) {
			case WORLD -> "a world effect that breaks blocks (like Break, Harvest or Excavate)";
			case HARMS -> "an effect that strikes creatures (like Harm or Push)";
			case HELPS -> "a helpful effect (like Heal or Swift)";
			case MOVES -> "an effect that moves creatures (like Push or Leap)";
			case BURNS -> "a fire effect (like Fire or Ember)";
			default -> "an effect";
		};
	}

	// ------------------------------------------------------------------ readout

	/** The readout line above what a hearth link fires, or null for any other link. */
	public static String header(String linkId) {
		if (!linkId.startsWith("wildercord:")) {
			return null;
		}
		return switch (linkId.substring(11)) {
			case "if_night" -> "If it's night:";
			case "if_day" -> "If it's day:";
			case "if_raining" -> "If rain or snow is falling on you:";
			case "if_underground" -> "If there's no open sky above you:";
			case "if_alone" -> "If no other player or monster is within " + trim(ALONE_RADIUS) + " blocks:";
			case "if_near_ally" -> "If an ally is within " + trim(ALLY_RADIUS) + " blocks:";
			case "if_unhurt" -> "If you're at full health:";
			case "if_holding_tool" -> "If you're holding a tool:";
			case "if_brimming" -> "If more than half your mana is left:";
			case "if_in_fields" -> "If farmland is within " + FIELDS_RADIUS + " blocks:";
			case "on_mine" -> "At the next block you mine (" + seconds(watchTicks("on_mine")) + "):";
			case "on_harvest" -> "At the next ripe crop you pick (" + seconds(watchTicks("on_harvest")) + "):";
			case "on_catch" -> "At your next catch (" + seconds(watchTicks("on_catch")) + "):";
			case "on_sprint" -> "When you next start sprinting (" + seconds(watchTicks("on_sprint")) + "):";
			case "on_splash" -> "Where you next enter water (" + seconds(watchTicks("on_splash")) + "):";
			case "on_mount" -> "At what you next ride (" + seconds(watchTicks("on_mount")) + "):";
			case "on_wake" -> "When you next wake from a bed (" + seconds(watchTicks("on_wake")) + "):";
			default -> null;
		};
	}

	/** What the hearth modifiers on {@code e} add to its readout, in order. */
	public static List<String> phrases(SpellPlan.EffectNode e) {
		List<String> out = new ArrayList<>();
		for (RuneDef mod : e.mods) {
			if (!ownsModifier(mod) || out.contains(phrase(mod.path()))) {
				continue;
			}
			out.add(phrase(mod.path()));
		}
		return out;
	}

	static String phrase(String path) {
		return switch (path) {
			case "tidy" -> "drops to your pack";
			case "replanting" -> "replants crops";
			case "kilned" -> "drops smelted";
			case "silken" -> "silk touch";
			case "windfall" -> "fortune " + WINDFALL_LEVEL;
			case "veinfollow" -> "follows ore veins (+" + VEIN_BLOCKS + ")";
			case "timbering" -> "fells trees (+" + TIMBER_LOGS + ")";
			case "level_ground" -> "never below your feet";
			case "gentle" -> "spares animals and villagers";
			case "sparing" -> "spares players and pets";
			case "culling" -> "monsters only, x" + trim(CULLING_POWER);
			case "headhunting" -> "healthiest only, x" + trim(HEADHUNT_POWER);
			case "hallowed" -> "undead only, x" + trim(HALLOWED_POWER);
			case "tapering" -> "x" + trim(taper(0)) + " first, less each after";
			case "pooled" -> "x" + trim(POOL_TOTAL) + " shared out";
			case "soothing" -> "calms, x" + trim(SOOTHING_POWER);
			case "bountiful" -> "x" + BOUNTY_XP + " experience";
			case "fetching" -> "loot to your feet";
			case "cushioned" -> "no fall damage " + seconds(CUSHION_TICKS);
			case "damp" -> "lights no blocks";
			case "inward" -> "you only, x" + trim(INWARD_POWER);
			case "selfless" -> "others only, x" + trim(SELFLESS_POWER);
			case "triage" -> "most hurt only, x" + trim(TRIAGE_POWER);
			case "mending" -> "mends " + MEND_POINTS + " durability";
			case "nourishing" -> "feeds " + NOURISH_FOOD;
			case "purifying" -> "lifts an ailment";
			case "matchmaking" -> "animals fall in love";
			case "magnetic" -> "pulls items in " + trim(MAGNET_RADIUS) + " blocks";
			case "sowing" -> "sows seeds";
			case "furrowing" -> "tills the soil";
			case "fertile" -> "grows crops";
			case "torchset" -> "sets a torch in the dark";
			case "ore_sensing" -> "shows ores in " + ORE_RADIUS + " blocks";
			case "fleecing" -> "shears sheep";
			case "sunlit" -> "x" + trim(SUNLIT_BRIGHT) + " in sunlight, x" + trim(SUNLIT_SHADE) + " elsewhere";
			case "steady" -> "changes no blocks";
			default -> path;
		};
	}

	// ------------------------------------------------------------------ numbers

	public static final double ALONE_RADIUS = 16;
	public static final double ALLY_RADIUS = 8;
	public static final int FIELDS_RADIUS = 4;
	public static final double BRIMMING_SHARE = 0.5;
	/** How many watchers of the hearth pack one caster may have waiting at once; the oldest gives way. */
	public static final int MAX_WATCHES = 8;

	/** How long a hearth watcher waits, in ticks. */
	public static int watchTicks(String path) {
		return switch (path) {
			case "on_mine", "on_splash", "on_mount" -> 600;
			case "on_harvest" -> 1200;
			case "on_catch" -> 2400;
			case "on_sprint" -> 300;
			case "on_wake" -> 12000;
			default -> 0;
		};
	}

	/** How far around where an effect lands its block modifiers look (a cube this many blocks out). */
	public static final int BLOCK_REACH = 4;
	public static final int WINDFALL_LEVEL = 3;
	public static final int VEIN_BLOCKS = 8;
	public static final int TIMBER_LOGS = 24;
	public static final double CULLING_POWER = 1.2;
	public static final double HEADHUNT_POWER = 1.6;
	public static final double HALLOWED_POWER = 2.0;
	public static final double POOL_TOTAL = 2.0;
	public static final double SOOTHING_POWER = 0.7;
	public static final int BOUNTY_XP = 2;
	public static final int CUSHION_TICKS = 200;
	public static final double INWARD_POWER = 1.4;
	public static final double SELFLESS_POWER = 1.3;
	public static final double TRIAGE_POWER = 1.6;
	public static final int MEND_POINTS = 10;
	public static final int NOURISH_FOOD = 3;
	public static final double MAGNET_RADIUS = 6;
	public static final int SOW_RADIUS = 3;
	public static final int FURROW_RADIUS = 2;
	public static final int FERTILE_RADIUS = 3;
	public static final int ORE_RADIUS = 8;
	public static final int ORE_GLOW_TICKS = 200;
	public static final double SUNLIT_BRIGHT = 2.0;
	public static final double SUNLIT_SHADE = 0.5;
	/** The block light at or below which Torchset calls a place dark. */
	public static final int DARK = 7;

	/** Tapering: the power for the {@code index}-th creature reached (0 first): 1.5, then a quarter less each time. */
	public static double taper(int index) {
		return 1.5 * Math.pow(0.75, Math.max(0, index));
	}

	/** Pooled: each of {@code count} creatures takes an equal share of double power. */
	public static double pooled(int count) {
		return count <= 0 ? 0 : POOL_TOTAL / count;
	}

	/** Sunlit's factor: doubled under open daylight sky, halved anywhere else. */
	public static double sunlit(boolean inSunlight) {
		return inSunlight ? SUNLIT_BRIGHT : SUNLIT_SHADE;
	}

	/** Mending: the item's damage after one mend. */
	public static int mended(int damage) {
		return Math.max(0, damage - MEND_POINTS);
	}

	/** If Brimming: whether {@code mana} out of {@code max} is more than half. */
	public static boolean brimming(double mana, double max) {
		return max > 0 && mana > max * BRIMMING_SHARE;
	}

	private static String seconds(int ticks) {
		return ticks % 1200 == 0 && ticks >= 1200 ? ticks / 1200 + (ticks == 1200 ? " minute" : " minutes") : trim(ticks / 20.0) + "s";
	}

	private static String trim(double v) {
		return Math.abs(v - Math.rint(v)) < 1e-6 ? Long.toString(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
	}
}
