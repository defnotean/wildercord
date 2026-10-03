package dev.wildercord.spell;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Attunement: meditate (sneak and stand still) with a Blank Rune in hand in the right biome, under
 * the right conditions, and after {@link #SECONDS} seconds the blank drinks in the land's own rune.
 * The rules are pure data (a biome and a condition over a {@link Place}), so they're unit-tested; the
 * server fills a {@code Place} in from the world and runs the ritual (see {@code cast.Attunement}).
 */
public final class Attunements {
	private Attunements() {}

	/** How long the meditation must hold. */
	public static final int SECONDS = 20;

	/**
	 * What the server knows about where a caster meditates.
	 *
	 * @param biome        the biome's id, e.g. {@code minecraft:cherry_grove}
	 * @param dimension    the dimension's id, e.g. {@code minecraft:the_nether}
	 * @param y            the caster's height
	 * @param timeOfDay    the day clock, 0-23999 (6000 is noon, 18000 midnight)
	 * @param moonPhase    0 for a full moon up to 7
	 * @param precipitation what falls on the caster: "rain", "snow" or "none"
	 * @param openSky      whether the caster can see the sky
	 * @param nearSculk    whether sculk is within a few blocks
	 */
	public record Place(String biome, String dimension, int y, long timeOfDay, int moonPhase, String precipitation, boolean openSky, boolean nearSculk) {
		public boolean night() {
			return timeOfDay >= 13000 && timeOfDay < 23000;
		}

		/** Around noon: two hours either side of it. */
		public boolean noon() {
			return timeOfDay >= 4000 && timeOfDay <= 8000;
		}
	}

	/**
	 * One attunement.
	 *
	 * @param id     the Grimoire key's name ({@code attune:<id>}), which is also the {@link RuneSources} id's suffix
	 * @param biomes the biome ids it may happen in
	 * @param riddle the hint the Grimoire shows until it's found
	 * @param needs  what the Grimoire says once it's found, e.g. "under a full moon"
	 */
	public record Rule(String id, RuneDef rune, Set<String> biomes, Predicate<Place> when, String riddle, String needs) {
		public boolean matches(Place place) {
			return biomes.contains(place.biome()) && when.test(place);
		}

		public String key() {
			return "attune:" + id;
		}
	}

	private static final Predicate<Place> ALWAYS = p -> true;

	public static final List<Rule> RULES = List.of(
		new Rule("cherry_grove", Runes.MOONPETAL, Set.of("minecraft:cherry_grove"), p -> p.night() && p.moonPhase() == 0 && p.openSky(),
			"Pink boughs hold their breath beneath a moon that is whole.", "at night, under a full moon"),
		new Rule("ice_spikes", Runes.HOARFROST, Set.of("minecraft:ice_spikes"), p -> p.precipitation().equals("snow"),
			"Among the frozen spears, wait while the sky lets its snow fall.", "while snow falls"),
		new Rule("deep_dark", Runes.HUSH, Set.of("minecraft:deep_dark"), Place::nearSculk,
			"Where the listening moss grows, be quieter than it.", "beside sculk"),
		new Rule("mushroom_fields", Runes.SPOREBLOOM, Set.of("minecraft:mushroom_fields"), ALWAYS,
			"On the island where the ground is fungus, and no monster walks.", "anywhere"),
		new Rule("badlands", Runes.SUNSCORCH, Set.of("minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands"),
			p -> p.noon() && p.openSky(), "In the red canyons, when the sun stands at its highest.", "at noon, under open sky"),
		new Rule("swamp", Runes.MIRE, Set.of("minecraft:swamp"), p -> p.precipitation().equals("rain"),
			"The murky marsh speaks only while the rain beats down on it.", "in the rain"),
		new Rule("lush_caves", Runes.GLOWVINE, Set.of("minecraft:lush_caves"), ALWAYS,
			"Beneath the green, where berries glow on hanging vines.", "anywhere"),
		new Rule("mangrove_swamp", Runes.ROOTSNARE, Set.of("minecraft:mangrove_swamp"), ALWAYS,
			"Where the trees stand on their own tangled roots in the water.", "anywhere"),
		new Rule("dripstone_caves", Runes.STALACTITE, Set.of("minecraft:dripstone_caves"), ALWAYS,
			"Under stone teeth that drip, slowly, forever.", "anywhere"),
		new Rule("peaks", Runes.SUMMIT_WIND, Set.of("minecraft:jagged_peaks", "minecraft:frozen_peaks", "minecraft:stony_peaks",
			"minecraft:snowy_slopes", "minecraft:grove", "minecraft:windswept_hills",
			"minecraft:windswept_gravelly_hills", "minecraft:windswept_forest"), p -> p.y() >= 200,
			"Climb until the mountain has nothing left above you but wind.", "at height 200 or higher"),
		new Rule("soul_sand_valley", Runes.SOULFIRE, Set.of("minecraft:soul_sand_valley"), ALWAYS,
			"In the valley of sighing sand, where the fires burn blue.", "anywhere"),
		new Rule("warped_forest", Runes.WARP_STEP, Set.of("minecraft:warped_forest"), ALWAYS,
			"Among the teal fungus, where the tall wanderers walk in peace.", "anywhere"),
		new Rule("crimson_forest", Runes.BLOOD_MOSS, Set.of("minecraft:crimson_forest"), ALWAYS,
			"In the red wood of the burning world, where the moss is the colour of blood.", "anywhere"),
		new Rule("basalt_deltas", Runes.BASALT_SURGE, Set.of("minecraft:basalt_deltas"), ALWAYS,
			"Where grey pillars and ash fall into lakes of fire.", "anywhere"),
		new Rule("outer_end", Runes.STARLIGHT_TETHER,
			Set.of("minecraft:end_highlands", "minecraft:end_midlands", "minecraft:small_end_islands", "minecraft:end_barrens"), ALWAYS,
			"Far past the dragon's island, on the rocks that drift among the stars.", "anywhere"));

	/** The attunement that happens at {@code place}, if any. */
	public static Optional<Rule> match(Place place) {
		for (Rule rule : RULES) {
			if (rule.matches(place)) {
				return Optional.of(rule);
			}
		}
		return Optional.empty();
	}

	/** Whether any attunement could happen in this biome, whatever the conditions. */
	public static boolean biomeHolds(String biome) {
		for (Rule rule : RULES) {
			if (rule.biomes().contains(biome)) {
				return true;
			}
		}
		return false;
	}

	public static Optional<Rule> byId(String id) {
		for (Rule rule : RULES) {
			if (rule.id().equals(id)) {
				return Optional.of(rule);
			}
		}
		return Optional.empty();
	}
}
