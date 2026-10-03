package dev.wildercord.spell;

import java.util.List;

/**
 * Sub-categories inside each family, so the Codex can be browsed by what a rune is for.
 * Order here is the order the Cord screen shows them in.
 */
public final class RuneCategories {
	private RuneCategories() {}

	public static final List<String> SHAPE = List.of("personal", "direct", "projectile", "area", "lingering");
	public static final List<String> EFFECT = List.of("damage", "control", "support", "movement", "time", "world", "summon", "innate");
	public static final List<String> MODIFIER = List.of("power", "area", "timing", "projectile", "circle");
	public static final List<String> LINK = List.of("timing", "trigger", "reactive", "condition");
	public static final List<String> KNOT = List.of("knot");

	/** Categories add-ons added (see {@code dev.wildercord.api}), shown after the built-in ones. */
	private static final java.util.Map<RuneFamily, List<String>> ADDED = new java.util.concurrent.ConcurrentHashMap<>();

	public static List<String> of(RuneFamily family) {
		List<String> builtIn = switch (family) {
			case SHAPE -> SHAPE;
			case EFFECT -> EFFECT;
			case MODIFIER -> MODIFIER;
			case LINK -> LINK;
			case KNOT -> KNOT;
		};
		List<String> added = ADDED.get(family);
		if (added == null) {
			return builtIn;
		}
		List<String> all = new java.util.ArrayList<>(builtIn);
		all.addAll(added);
		return List.copyOf(all);
	}

	/** Adds a category to a family (a no-op if it's already there). Its Codex label is the lang key {@code category.wildercord.<family>.<category>}. */
	public static synchronized void add(RuneFamily family, String category) {
		if (of(family).contains(category)) {
			return;
		}
		List<String> added = new java.util.ArrayList<>(ADDED.getOrDefault(family, List.of()));
		added.add(category);
		ADDED.put(family, List.copyOf(added));
	}

	/** Sort position of a category within its family (unknown categories sort last). */
	public static int order(RuneDef rune) {
		int index = of(rune.family()).indexOf(rune.category());
		return index < 0 ? Integer.MAX_VALUE : index;
	}

	/** The built-in runes' categories. Add-on runes will declare their own. */
	static String categoryFor(String path, RuneFamily family) {
		return switch (family) {
			case SHAPE -> switch (path) {
				case "self", "orbit" -> "personal";
				case "touch", "beam", "barrage", "blitz", "ray", "lance", "sweep", "prism", "stream" -> "direct";
				case "bolt", "arc", "wave", "crescent", "orb", "spark", "wisp", "comet", "ricochet", "cluster" -> "projectile";
				// New runes (batch 2); Imprint waits where you stood, as a Mine does, so it's lingering.
				case "latch" -> "direct";
				case "glaive" -> "projectile";
				case "burst", "cone", "ring", "pillar", "rain", "nova", "constellation" -> "area";
				default -> "lingering";
			};
			case EFFECT -> switch (path) {
				case "push", "pull", "launch", "root", "freeze", "levitate", "gravity_well", "blind", "chill", "silence", "reveal",
					"decree", "weigh", "shackle", "bubble", "hex", "rend", "jolt", "banish", "cyclone",
					"echolocate", "undertow", "portalfall", "hush", "mire", "rootsnare", "starlight_tether", "sporebloom", "tidehook" -> "control";
				case "heal", "shield", "regrowth", "cleanse", "stoneskin", "empower", "haste", "swift", "night_eye", "feather_fall", "veil",
					"fireward", "nourish", "tidebreath", "leap", "infinity", "reversal", "reflect", "overdrive", "foresight", "restore",
					"barrier", "brace", "anchor", "bramble", "frostward", "cushion", "deflect", "haven",
					"remedy", "warcry", "treasure_sense", "shulkershell", "ashen_veil", "cinderheart", "manatide" -> "support";
				case "dash", "blink", "grapple", "swap", "zipper", "shadowstep", "tusk_charge", "warp_step", "current" -> "movement";
				case "stasis", "rewind", "accelerate", "time_skip" -> "time";
				case "light", "grow", "break", "harvest", "icepath", "basinfill", "collect", "excavate", "rampart", "chisel", "glimmer", "prune", "tunnel",
					"vein", "smelt", "fell", "span", "ancient_seed", "glowvine" -> "world";
				case "summon", "shades", "thunderbird" -> "summon";
				case "glacier", "nullify", "blizzard", "black_ice", "rime_seal", "absolute_zero", "magnetize", "heartstopper", "updraft", "recoil",
					"sinkhole", "fossilize", "timesteal" -> "control";
				case "bloom", "surge", "phoenix_pyre", "frostbloom", "cryostasis", "zephyr", "geode", "soulbond", "second_wind", "transfusion",
					"lifebloom" -> "support";
				case "warp", "skyglyph" -> "movement";
				case "chronoshift" -> "time";
				// New runes (batch 2); Spellbrand and Umbra are damage.
				case "gash", "flash_freeze", "drowse", "disarm" -> "control";
				case "searing_edge" -> "support";
				case "prospect", "galvanize" -> "world";
				case "prolong" -> "time";
				// Flight.
				case "soar", "wind_steps", "rime_causeway", "thunder_walk" -> "movement";
				case "strata_rise", "cinder_bulwark", "root_bulwark" -> "world";
				// Signature fusions (see Fusions.SIGNATURES); the rest of them are damage.
				case "seethe", "dust_devil", "malison" -> "control";
				case "stitchtime", "halo", "riposte" -> "support";
				case "bloomstep", "thunderstep" -> "movement";
				case "blood_thread", "kindling", "twin_star", "borrowed_time", "gale_mantle", "stoneform", "mirrorfrost", "fortune", "phantom",
					"stormheart" -> "innate";
				default -> "damage";
			};
			case MODIFIER -> path.endsWith("_circle") ? "circle" : switch (path) {
				case "amplify", "overcharge", "frugal", "vow", "blood_price", "execute", "trial_key", "kindled", "unstable" -> "power";
				case "widen", "focus", "split" -> "area";
				case "extend", "linger", "rapid" -> "timing";
				// New runes (batch 2): Kindred spreads an effect to more creatures.
				case "thirst" -> "power";
				case "kindred" -> "area";
				case "belated" -> "timing";
				default -> "projectile";
			};
			case LINK -> switch (path) {
				case "delay", "pulse", "echo" -> "timing";
				case "on_hurt", "on_low_health" -> "reactive";
				case "if_sneaking", "if_airborne", "combo", "if_wounded", "if_outnumbered", "if_wet" -> "condition";
				default -> "trigger";
			};
			case KNOT -> "knot";
		};
	}
}
