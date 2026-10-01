package dev.wildercord.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.wildercord.world.ResidueRules.Kind;
import dev.wildercord.world.ResidueRules.Placement;
import dev.wildercord.world.ResidueRules.Refusal;
import dev.wildercord.world.ResidueRules.Site;
import dev.wildercord.world.ResidueRules.Source;
import dev.wildercord.world.ResidueRules.Spot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** When big magic leaves a residue, how much, for how long, and the ground it may (and may never) take. */
class ResidueRulesTest {
	private static final double MIN = ResidueRules.MIN_SPELL_COST;

	@Test
	void everyElementHasItsOwnMark() {
		Set<String> elements = new HashSet<>();
		Set<String> reagents = new HashSet<>();
		for (Kind kind : Kind.values()) {
			assertTrue(elements.add(kind.element), "two marks for " + kind.element);
			assertTrue(reagents.add(kind.reagent), "two marks give " + kind.reagent);
			assertEquals(kind, ResidueRules.kindFor(kind.element).orElseThrow());
			assertEquals(kind, Kind.byPath(kind.path).orElseThrow());
			assertTrue(kind.lifetime >= 1200, kind + " lasts at least a minute");
		}
		assertEquals(10, elements.size(), "all ten elements leave a mark");
		assertTrue(ResidueRules.kindFor("").isEmpty(), "a rune with no element leaves nothing");
		assertTrue(ResidueRules.kindFor(null).isEmpty());
		assertEquals(Kind.EVERFROST, ResidueRules.kindFor("Frost").orElseThrow());
		// Everfrost holds about a day; a void scar closes within minutes.
		assertEquals(24000, Kind.EVERFROST.lifetime);
		assertTrue(Kind.VOID_SCAR.lifetime < Kind.EVERFROST.lifetime);
	}

	@Test
	void onlyStrongMagicLeavesAResidue() {
		// A small spell leaves nothing: a fire bolt (about 12 mana), a burst of fire (18).
		assertEquals(0, ResidueRules.strength(Source.SPELL, 12, MIN, false));
		assertEquals(0, ResidueRules.strength(Source.SPELL, 29.9, MIN, false));
		// At the threshold, and twice it.
		assertEquals(1.0, ResidueRules.strength(Source.SPELL, 30, MIN, false), 1e-9);
		assertEquals(2.0, ResidueRules.strength(Source.SPELL, 60, MIN, false), 1e-9);
		// However costly, it reads no stronger than the cap.
		assertEquals(ResidueRules.MAX_STRENGTH, ResidueRules.strength(Source.SPELL, 1000, MIN, false), 1e-9);
		// The server's threshold moves it.
		assertEquals(0, ResidueRules.strength(Source.SPELL, 30, 60, false));
		assertEquals(1.0, ResidueRules.strength(Source.SPELL, 15, 15, false), 1e-9);
	}

	@Test
	void anOvercastAlwaysLeavesOneAndABossLeavesABiggerOne() {
		assertEquals(1.5, ResidueRules.strength(Source.SPELL, 10, MIN, true), 1e-9, "a cheap spell paid with a cracked circle still counts");
		assertEquals(2.5, ResidueRules.strength(Source.SPELL, 60, MIN, true), 1e-9);
		assertEquals(ResidueRules.BOSS_STRENGTH, ResidueRules.strength(Source.BOSS, 5, MIN, false), 1e-9);
		assertEquals(2.0, ResidueRules.strength(Source.BOSS, 60, MIN, false), 1e-9);
		assertEquals(ResidueRules.REACTION_STRENGTH, ResidueRules.strength(Source.REACTION, 0, MIN, false), 1e-9);
		assertTrue(ResidueRules.REACTION_CHANCE > 0 && ResidueRules.REACTION_CHANCE < 1, "only sometimes for a reaction");
	}

	@Test
	void strongerMagicLeavesMoreAndFurtherAndLonger() {
		assertEquals(0, ResidueRules.cells(0.5));
		assertEquals(1, ResidueRules.cells(1.0));
		assertEquals(2, ResidueRules.cells(1.5));
		assertEquals(3, ResidueRules.cells(2.0));
		assertEquals(5, ResidueRules.cells(3.0));
		assertEquals(5, ResidueRules.cells(99), "never more than five blocks from one cast");
		assertEquals(2, ResidueRules.spread(1.0));
		assertEquals(3, ResidueRules.spread(2.5));
		assertEquals(3, ResidueRules.spread(99));
		assertEquals(24000, ResidueRules.lifetime(Kind.EVERFROST, 1.0, 1.0));
		assertEquals(36000, ResidueRules.lifetime(Kind.EVERFROST, 3.0, 1.0), "the strongest last half again as long");
		assertEquals(36000, ResidueRules.lifetime(Kind.EVERFROST, 9.0, 1.0));
		assertEquals(48000, ResidueRules.lifetime(Kind.EVERFROST, 1.0, 2.0), "the server's multiplier");
		assertEquals(1200, ResidueRules.lifetime(Kind.VOID_SCAR, 1.0, 0.01), "never under a minute");
		assertTrue(ResidueRules.rest(Source.BOSS) > ResidueRules.rest(Source.SPELL));
	}

	private static Site site(Spot here, Spot below, Spot above) {
		return new Site(here, below, above, true, false, false, true, false, true, true);
	}

	@Test
	void aResidueNeverReplacesAnythingBuilt() {
		// Natural ground under open air: both kinds go.
		assertNull(ResidueRules.judge(Placement.COVER, site(Spot.NATURAL, Spot.NATURAL, Spot.OPEN)));
		assertNull(ResidueRules.judge(Placement.REST, site(Spot.OPEN, Spot.NATURAL, Spot.OPEN)));
		// A built block (planks, bricks, glass...) is never taken, and nothing rests on one.
		assertEquals(Refusal.BUILT, ResidueRules.judge(Placement.COVER, site(Spot.OTHER, Spot.NATURAL, Spot.OPEN)));
		assertEquals(Refusal.BUILT, ResidueRules.judge(Placement.REST, site(Spot.OPEN, Spot.OTHER, Spot.OPEN)));
		assertEquals(Refusal.BUILT, ResidueRules.judge(Placement.REST, site(Spot.OTHER, Spot.NATURAL, Spot.OPEN)));
		// A block with contents, a spell's passing block or another residue: never.
		assertEquals(Refusal.BUILT, ResidueRules.judge(Placement.COVER,
			new Site(Spot.NATURAL, Spot.NATURAL, Spot.OPEN, true, false, true, true, false, true, true)));
		assertEquals(Refusal.TEMPORARY, ResidueRules.judge(Placement.REST,
			new Site(Spot.OPEN, Spot.NATURAL, Spot.OPEN, true, true, false, true, false, true, true)));
		// Ground under something (a COVER residue needs the sky over it) and air over nothing.
		assertEquals(Refusal.COVERED, ResidueRules.judge(Placement.COVER, site(Spot.NATURAL, Spot.NATURAL, Spot.OTHER)));
		assertEquals(Refusal.NO_GROUND, ResidueRules.judge(Placement.REST, site(Spot.OPEN, Spot.OPEN, Spot.OPEN)));
		assertEquals(Refusal.NO_GROUND, ResidueRules.judge(Placement.REST,
			new Site(Spot.OPEN, Spot.NATURAL, Spot.OPEN, false, false, false, true, false, true, true)));
	}

	@Test
	void aResidueAsksPermissionAndKeepsOutOfWards() {
		Site base = site(Spot.NATURAL, Spot.NATURAL, Spot.OPEN);
		assertEquals(Refusal.PROTECTED, ResidueRules.judge(Placement.COVER,
			new Site(base.here(), base.below(), base.above(), true, false, false, false, false, true, true)));
		assertEquals(Refusal.WARDED, ResidueRules.judge(Placement.COVER,
			new Site(base.here(), base.below(), base.above(), true, false, false, true, true, true, true)));
		assertEquals(Refusal.UNLOADED, ResidueRules.judge(Placement.COVER,
			new Site(base.here(), base.below(), base.above(), true, false, false, true, false, false, true)));
		assertEquals(Refusal.OUTSIDE_WORLD, ResidueRules.judge(Placement.COVER,
			new Site(base.here(), base.below(), base.above(), true, false, false, true, false, true, false)));
		// A claim mod is asked only about a block the residue could really take: built ground is refused before.
		assertEquals(Refusal.BUILT, ResidueRules.judge(Placement.COVER,
			new Site(Spot.OTHER, Spot.NATURAL, Spot.OPEN, true, false, false, false, false, true, true)));
	}

	@Test
	void theNaturalGroundTagHoldsNothingBuilt() throws IOException {
		Path tag = Path.of("src/main/resources/data/wildercord/tags/block/residue_ground.json");
		JsonArray values = JsonParser.parseString(Files.readString(tag)).getAsJsonObject().getAsJsonArray("values");
		Set<String> entries = new HashSet<>();
		values.forEach(v -> entries.add(v.isJsonObject() ? v.getAsJsonObject().get("id").getAsString() : v.getAsString()));
		// Grass, dirt, moss and mud come in through vanilla's own substrate tag.
		assertTrue(entries.contains("minecraft:grass_block") || entries.contains("#minecraft:substrate_overworld"), "grass is natural ground");
		assertTrue(entries.contains("minecraft:sand") || entries.contains("#minecraft:sand"));
		for (String built : List.of("minecraft:oak_planks", "minecraft:cobblestone", "minecraft:stone_bricks", "minecraft:glass", "minecraft:bricks",
				"minecraft:smooth_stone", "#minecraft:planks", "#minecraft:logs", "minecraft:white_wool", "minecraft:polished_andesite", "minecraft:farmland",
				"minecraft:dirt_path")) {
			assertFalse(entries.contains(built), built + " is something players build with, never residue ground");
		}
	}

	@Test
	void aScarClosesThroughItsStagesAndASeedlingNeverOutlivesItsParent() {
		assertEquals(0.0, ResidueRules.age(100, 1100, 100), 1e-9);
		assertEquals(0.5, ResidueRules.age(100, 1100, 600), 1e-9);
		assertEquals(1.0, ResidueRules.age(100, 1100, 5000), 1e-9);
		assertEquals(0, ResidueRules.stage(0, 400, 0));
		assertEquals(1, ResidueRules.stage(0, 400, 100));
		assertEquals(3, ResidueRules.stage(0, 400, 399));
		assertEquals(3, ResidueRules.stage(0, 400, 9999));
		assertEquals(5000, ResidueRules.seedlingDue(5000, 1000, 24000), "a seedling fades with its parent");
		assertEquals(3000, ResidueRules.seedlingDue(50000, 1000, 2000));
		assertTrue(ResidueRules.BLOOM_GENERATIONS >= 1 && ResidueRules.BLOOM_GENERATIONS <= 3, "wildblooms spread only a little");
	}
}
