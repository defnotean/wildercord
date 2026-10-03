package dev.wildercord.spell;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static dev.wildercord.spell.Runes.*;
import static org.junit.jupiter.api.Assertions.*;

/** The runes of the world: found only in particular places, never crafted, and Attunement's rules. */
class WorldRunesTest {
	private static final Path RESOURCES = Path.of("src/main/resources");

	private static Attunements.Place place(String biome, long time, int moon, String falling, boolean sky, boolean sculk, int y) {
		String dimension = biome.contains("end_") ? "minecraft:the_end" : "minecraft:overworld";
		return new Attunements.Place(biome, dimension, y, time, moon, falling, sky, sculk);
	}

	private static Attunements.Place place(String biome) {
		return place(biome, 6000, 3, "none", true, false, 70);
	}

	private static RuneDef attuned(Attunements.Place place) {
		return Attunements.match(place).map(Attunements.Rule::rune).orElse(null);
	}

	@Test
	void thereAreManyAndTheyAreSpreadAcrossTheWorld() {
		List<RuneDef> world = RuneSources.runes();
		assertTrue(world.size() >= 40, "only " + world.size() + " runes of the world");
		assertEquals(world.size(), new HashSet<>(world).size());
		long effects = world.stream().filter(r -> r.family() == RuneFamily.EFFECT).count();
		assertTrue(effects > world.size() / 2, "mostly effects");
		// Every family a rune can be threaded as (a Knot is tied at the Fusion Altar, never found).
		for (RuneFamily family : List.of(RuneFamily.SHAPE, RuneFamily.EFFECT, RuneFamily.MODIFIER, RuneFamily.LINK)) {
			assertTrue(world.stream().anyMatch(r -> r.family() == family), "a " + family + " among them");
		}
		for (RuneDef rune : world) {
			assertFalse(RuneSources.sourcesOf(rune).isEmpty(), rune.name() + " has nowhere to be found");
			assertFalse(Runes.innate(rune), rune.name());
			assertTrue(Runes.get(rune.id()).isPresent(), rune.name());
		}
	}

	@Test
	void noneOfThemCanBeCrafted() {
		for (RuneDef rune : RuneSources.runes()) {
			Path recipe = RESOURCES.resolve("data/wildercord/recipe/rune_" + rune.path() + ".json");
			assertFalse(Files.exists(recipe), rune.name() + " must not have a crafting recipe");
		}
	}

	@Test
	void everyOneHasItsArtTextAndWhereItsFound() throws Exception {
		String lang = Files.readString(RESOURCES.resolve("assets/wildercord/lang/en_us.json"));
		for (RuneDef rune : RuneSources.runes()) {
			String path = rune.path();
			assertTrue(Files.exists(RESOURCES.resolve("assets/wildercord/textures/item/rune/" + path + ".png")), path + " icon");
			assertTrue(Files.exists(RESOURCES.resolve("assets/wildercord/textures/particle/circle/" + path + "_band.png")), path + " ring");
			assertTrue(Files.exists(RESOURCES.resolve("assets/wildercord/textures/particle/circle/" + path + "_mark.png")), path + " emblem");
			assertTrue(lang.contains("\"rune.wildercord." + path + "\""), path + " name");
			assertTrue(lang.contains("\"rune.wildercord." + path + ".found\": \"Found: "), path + " where it's found");
			assertFalse(lang.contains("\"rune.wildercord." + path + ".craft\""), path + " must not say how to craft it");
			if (rune.tier() >= 3) {
				assertTrue(Files.exists(RESOURCES.resolve("assets/wildercord/textures/item/rune/" + path + ".png.mcmeta")), path + " is animated");
			}
		}
	}

	@Test
	void theirNamesAreTheirOwn() {
		// The Fusion Altar's runes, its signature ones included.
		Set<String> fusion = new HashSet<>();
		for (RuneDef rune : java.util.stream.Stream.concat(Runes.FUSED.stream(), Runes.SIGNATURE.stream()).toList()) {
			fusion.add(rune.path());
			fusion.add(rune.name().toLowerCase(Locale.ROOT));
		}
		Set<String> names = new HashSet<>();
		for (RuneDef rune : Runes.all()) {
			assertTrue(names.add(rune.name().toLowerCase(Locale.ROOT)), "two runes are called " + rune.name());
		}
		for (RuneDef rune : RuneSources.runes()) {
			assertFalse(fusion.contains(rune.path()), rune.name() + " clashes with a fusion rune");
			assertFalse(fusion.contains(rune.name().toLowerCase(Locale.ROOT)), rune.name() + " clashes with a fusion rune");
		}
	}

	@Test
	void theDungeonsBossesAndEventsHaveTheirOwn() {
		for (String dungeon : List.of("ember_sanctum", "astral_observatory", "drowned_scriptorium")) {
			int n = RuneSources.forSource(dungeon).size();
			assertTrue(n >= 2 && n <= 4, dungeon + " has " + n);
		}
		for (String boss : List.of("cinder_warden", "star_eater", "tide_scribe")) {
			assertEquals(1, RuneSources.forSource(boss).size(), boss);
			assertEquals(4, RuneSources.forSource(boss).getFirst().tier(), boss + " drops a Tier IV rune");
		}
		for (String event : List.of("starfall", "rift", "mana_storm")) {
			int n = RuneSources.forSource(event).size();
			assertTrue(n >= 1 && n <= 2, event + " has " + n);
		}
		assertTrue(RuneSources.forSource("nowhere").isEmpty());
		assertFalse(RuneSources.forSource("archive").isEmpty());
		assertFalse(RuneSources.forSource("runebound_adept").isEmpty());
	}

	@Test
	void fishingHasTwoRunesOfItsOwn() {
		assertEquals(List.of(TIDEHOOK, CURRENT), RuneSources.forSource("fishing"));
		assertEquals(List.of(RuneSources.FISHING), RuneSources.sourcesOf(TIDEHOOK), "Tidehook is found nowhere else");
		assertEquals(List.of(RuneSources.FISHING), RuneSources.sourcesOf(CURRENT), "nor is Current");
		assertEquals(EffectKind.HARMFUL, TIDEHOOK.kind());
		assertEquals(EffectKind.MOVEMENT, CURRENT.kind(), "Current always moves its caster");
		assertEquals("control", TIDEHOOK.category());
		assertEquals("movement", CURRENT.category());
		// A hook of water soaks what it reels in: it never ices over the water it lands in.
		assertEquals(WorldRules.Interaction.NONE, WorldRules.of(TIDEHOOK));
		assertEquals(WorldRules.Interaction.NONE, WorldRules.of(CURRENT));
		assertTrue(TIDEHOOK.traits().contains(Trait.POWER) && TIDEHOOK.traits().contains(Trait.LINGER));
		assertTrue(CURRENT.traits().contains(Trait.POWER));
	}

	@Test
	void everyAttunementIsASource() {
		Set<String> ids = new HashSet<>();
		for (Attunements.Rule rule : Attunements.RULES) {
			assertTrue(ids.add(rule.id()), rule.id());
			assertEquals(List.of(rule.rune()), RuneSources.forSource("attunement:" + rule.id()), rule.id());
			assertFalse(rule.riddle().isBlank(), rule.id());
			assertTrue(rule.key().startsWith("attune:"));
		}
		for (RuneSources.Source source : RuneSources.all()) {
			if (source.id().startsWith("attunement:")) {
				assertTrue(Attunements.byId(source.id().substring("attunement:".length())).isPresent(), source.id());
			}
		}
		assertTrue(Attunements.RULES.size() >= 12);
	}

	@Test
	void attunementNeedsTheRightMoment() {
		// Cherry grove: only at night, under a full moon, under the sky.
		assertEquals(MOONPETAL, attuned(place("minecraft:cherry_grove", 18000, 0, "none", true, false, 90)));
		assertNull(attuned(place("minecraft:cherry_grove", 6000, 0, "none", true, false, 90)), "not by day");
		assertNull(attuned(place("minecraft:cherry_grove", 18000, 4, "none", true, false, 90)), "not under a new moon");
		assertNull(attuned(place("minecraft:cherry_grove", 18000, 0, "none", false, false, 90)), "not under a roof");
		// Ice spikes: in falling snow.
		assertEquals(HOARFROST, attuned(place("minecraft:ice_spikes", 6000, 3, "snow", true, false, 70)));
		assertNull(attuned(place("minecraft:ice_spikes")));
		// Deep dark: beside sculk.
		assertEquals(HUSH, attuned(place("minecraft:deep_dark", 6000, 3, "none", false, true, -40)));
		assertNull(attuned(place("minecraft:deep_dark", 6000, 3, "none", false, false, -40)));
		// Badlands (any kind): at noon, under open sky.
		for (String badlands : List.of("minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands")) {
			assertEquals(SUNSCORCH, attuned(place(badlands, 6000, 3, "none", true, false, 70)), badlands);
		}
		assertNull(attuned(place("minecraft:badlands", 12000, 3, "none", true, false, 70)), "not at dusk");
		assertNull(attuned(place("minecraft:badlands", 6000, 3, "none", false, false, 40)), "not underground");
		// Swamp: in the rain.
		assertEquals(MIRE, attuned(place("minecraft:swamp", 6000, 3, "rain", true, false, 63)));
		assertNull(attuned(place("minecraft:swamp")));
		// Peaks: above height 200.
		assertEquals(SUMMIT_WIND, attuned(place("minecraft:jagged_peaks", 6000, 3, "none", true, false, 210)));
		assertEquals(SUMMIT_WIND, attuned(place("minecraft:frozen_peaks", 6000, 3, "snow", true, false, 200)));
		assertNull(attuned(place("minecraft:jagged_peaks", 6000, 3, "none", true, false, 150)));
		for (String mountain : List.of("minecraft:jagged_peaks", "minecraft:frozen_peaks", "minecraft:stony_peaks",
			"minecraft:snowy_slopes", "minecraft:grove", "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest")) {
			assertEquals(SUMMIT_WIND, attuned(place(mountain, 6000, 3, "none", true, false, 254)), mountain);
			assertEquals(SUMMIT_WIND, attuned(place(mountain, 6000, 3, "none", true, false, 200)), mountain);
			assertNull(attuned(place(mountain, 6000, 3, "none", true, false, 199)), mountain);
		}
		assertNull(attuned(place("minecraft:plains", 6000, 3, "none", true, false, 254)), "a plains tower is not a mountain");
	}

	@Test
	void someLandsAlwaysHoldTheirRune() {
		assertEquals(SPOREBLOOM, attuned(place("minecraft:mushroom_fields")));
		assertEquals(GLOWVINE, attuned(place("minecraft:lush_caves", 18000, 0, "none", false, false, 20)));
		assertEquals(ROOTSNARE, attuned(place("minecraft:mangrove_swamp")));
		assertEquals(STALACTITE, attuned(place("minecraft:dripstone_caves", 0, 0, "none", false, false, 10)));
		assertEquals(SOULFIRE, attuned(place("minecraft:soul_sand_valley", 0, 0, "none", false, false, 40)));
		assertEquals(WARP_STEP, attuned(place("minecraft:warped_forest")));
		assertEquals(BLOOD_MOSS, attuned(place("minecraft:crimson_forest")));
		assertEquals(BASALT_SURGE, attuned(place("minecraft:basalt_deltas")));
		for (String outer : List.of("minecraft:end_highlands", "minecraft:end_midlands", "minecraft:small_end_islands", "minecraft:end_barrens")) {
			assertEquals(STARLIGHT_TETHER, attuned(place(outer)), outer);
		}
		// The dragon's island and ordinary lands hold nothing.
		assertNull(attuned(place("minecraft:the_end")));
		assertNull(attuned(place("minecraft:plains")));
		assertTrue(Attunements.biomeHolds("minecraft:cherry_grove"));
		assertFalse(Attunements.biomeHolds("minecraft:plains"));
	}

	@Test
	void theTimeOfDayIsReadTheWayTheGameCountsIt() {
		assertTrue(place("minecraft:plains", 18000, 0, "none", true, false, 64).night());
		assertFalse(place("minecraft:plains", 6000, 0, "none", true, false, 64).night());
		assertTrue(place("minecraft:plains", 6000, 0, "none", true, false, 64).noon());
		assertFalse(place("minecraft:plains", 0, 0, "none", true, false, 64).noon());
	}

	@Test
	void theirModifiersChangeTheNumbers() {
		SpellCompiler.Compiled keyed = SpellCompiler.compile(List.of(BOLT, HARM, TRIAL_KEY));
		assertEquals(1, keyed.attachedTo()[2]);
		assertEquals(1.6, SpellNumbers.trialKeyBonus(keyed.root().groups.getFirst().effects.getFirst()), 1e-9);
		assertTrue(keyed.lines().getFirst().contains("x1.6 at full health"), keyed.lines().getFirst());
		assertEquals((3 + 8 * 1.1) * 1.0 + 8 * 1.1 * 0.3, keyed.cost(), 1e-9);

		SpellPlan.EffectNode kindled = SpellCompiler.compile(List.of(BOLT, HARM, KINDLED)).root().groups.getFirst().effects.getFirst();
		assertEquals(1.3, SpellNumbers.power(kindled), 1e-9);
		assertEquals(4, SpellNumbers.kindledSeconds(kindled));
		assertEquals(0, SpellNumbers.kindledSeconds(SpellCompiler.compile(List.of(BOLT, HARM)).root().groups.getFirst().effects.getFirst()));

		SpellPlan.EffectNode unstable = SpellCompiler.compile(List.of(BOLT, HARM, UNSTABLE)).root().groups.getFirst().effects.getFirst();
		assertEquals(0.5, SpellNumbers.unstableSwing(unstable, 0.0), 1e-9);
		assertEquals(1.0, SpellNumbers.unstableSwing(unstable, 0.5), 1e-9);
		assertEquals(2.0, SpellNumbers.unstableSwing(unstable, 1.0), 1e-9);
		SpellPlan.EffectNode steady = SpellCompiler.compile(List.of(BOLT, HARM)).root().groups.getFirst().effects.getFirst();
		assertEquals(1.0, SpellNumbers.unstableSwing(steady, 0.0), 1e-9);
		// Kindled, Trial Key and Unstable only fit something with power.
		assertEquals(SpellCompiler.UNATTACHED, SpellCompiler.compile(List.of(SELF, NIGHT_EYE, KINDLED)).attachedTo()[2]);
	}

	@Test
	void theirShapesReadClearly() {
		assertEquals("A vortex's eye (1.8 blocks, drags in from 5 blocks, 3s): Harm", SpellCompiler.compile(List.of(VORTEX, HARM)).lines().getFirst());
		assertEquals("A vortex's eye (1.8 blocks, drags in from 5 blocks, 6s): Harm", SpellCompiler.compile(List.of(VORTEX, EXTEND, HARM)).lines().getFirst());
		assertEquals("A tripwire (up to 12 blocks, springs 2.5 blocks): Harm", SpellCompiler.compile(List.of(SNARE, HARM)).lines().getFirst());
		assertEquals("Up to 5 enemies within 18 blocks: Harm", SpellCompiler.compile(List.of(CONSTELLATION, WIDEN, HARM)).lines().getFirst());
		assertEquals("area", CONSTELLATION.category());
		assertEquals("lingering", VORTEX.category());
		assertEquals("lingering", SNARE.category());
	}

	@Test
	void theirConditionsGuardTheRest() {
		SpellCompiler.Compiled wounded = SpellCompiler.compile(List.of(SELF, SWIFT, IF_WOUNDED, HEAL));
		assertTrue(wounded.warnings().isEmpty(), wounded.warnings().toString());
		assertEquals(List.of("You: Swift", "If you're below half health:", "  You: Heal"), wounded.lines());
		assertEquals("If 3 or more enemies are within 8 blocks:", SpellCompiler.compile(List.of(IF_OUTNUMBERED, BURST, HARM)).lines().getFirst());
		assertEquals("If you're in water or rain:", SpellCompiler.compile(List.of(IF_WET, SELF, SWIFT)).lines().getFirst());
		// Like If Sneaking, what follows keeps the segment's own shape: at the start of a spell, you.
		SpellPlan.Segment root = SpellCompiler.compile(List.of(BOLT, HARM, IF_WET, SHOCK)).root();
		assertEquals(SELF, root.link.next.groups.getFirst().shape);
		for (RuneDef link : List.of(IF_WOUNDED, IF_OUTNUMBERED, IF_WET)) {
			assertEquals("condition", link.category(), link.name());
		}
	}

	@Test
	void theyAreFiledWhereTheyBelong() {
		for (RuneDef rune : List.of(REMEDY, WARCRY, TREASURE_SENSE, SHULKERSHELL, ASHEN_VEIL, CINDERHEART, MANATIDE)) {
			assertEquals("support", rune.category(), rune.name());
			assertEquals(EffectKind.HELPFUL, rune.kind(), rune.name());
		}
		for (RuneDef rune : List.of(TUSK_CHARGE, WARP_STEP)) {
			assertEquals("movement", rune.category(), rune.name());
			assertEquals(EffectKind.MOVEMENT, rune.kind(), rune.name());
		}
		for (RuneDef rune : List.of(ANCIENT_SEED, GLOWVINE)) {
			assertEquals("world", rune.category(), rune.name());
			assertEquals(EffectKind.WORLD, rune.kind(), rune.name());
		}
		for (RuneDef rune : List.of(TRIAL_KEY, KINDLED, UNSTABLE)) {
			assertEquals("power", rune.category(), rune.name());
		}
		assertEquals("control", HUSH.category());
		assertEquals("damage", RESONANT_SHRIEK.category());
		assertEquals("Opening Arcane Bolt", SpellNames.auto(List.of(BOLT, HARM, TRIAL_KEY)));
	}

	@Test
	void theyCostWhatTheirTierIsWorth() {
		for (RuneDef rune : RuneSources.runes()) {
			if (rune.family() != RuneFamily.EFFECT) {
				continue;
			}
			double[] range = switch (rune.tier()) {
				case 1 -> new double[] {2, 8};
				case 2 -> new double[] {5, 14};
				case 3 -> new double[] {10, 24};
				default -> new double[] {25, 40};
			};
			assertTrue(rune.cost() >= range[0] && rune.cost() <= range[1], rune.name() + " costs " + rune.cost() + " at Tier " + rune.tier());
		}
	}

	@Test
	void noneCanBeSustainedAsAPassiveUnlessHarmless() {
		for (RuneDef rune : RuneSources.runes()) {
			assertFalse(Passives.allowed(rune), rune.name() + " was never meant to be a passive");
		}
	}
}
