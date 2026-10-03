package dev.wildercord.wildlife;

import dev.wildercord.spell.Feats;
import dev.wildercord.spell.FieldGuide;
import dev.wildercord.wildlife.WildlifeRules.Garden;
import dev.wildercord.wildlife.WildlifeRules.Kind;
import dev.wildercord.wildlife.WildlifeRules.Stance;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WildlifeRulesTest {
 @Test void wetlandPollinatorsKeepTheirSharedAmbientAndCrowdingRules() {
  assertTrue(WildlifeRules.GLIMMERWING.biomes().containsAll(List.of("minecraft:swamp","minecraft:mangrove_swamp")));
  assertEquals(WildlifeRules.Pool.AMBIENT,WildlifeRules.GLIMMERWING.pool());
  assertTrue(WildlifeRules.roomFor(WildlifeRules.GLIMMERWING,WildlifeRules.GLIMMERWING.crowd()));
  assertFalse(WildlifeRules.roomFor(WildlifeRules.GLIMMERWING,WildlifeRules.GLIMMERWING.crowd()+1));
 }

	@Test
	void everyKindHasSaneSpawns() {
		Set<String> ids = new HashSet<>();
		for (Kind kind : WildlifeRules.ALL) {
			assertTrue(ids.add(kind.id()), "unique id " + kind.id());
			assertTrue(kind.weight() >= 1 && kind.weight() <= 12, kind.id() + " weighs like a vanilla entry");
			assertTrue(kind.minGroup() >= 1 && kind.minGroup() <= kind.maxGroup() && kind.maxGroup() <= 5, kind.id() + " groups");
			assertTrue(kind.chance() > 0 && kind.chance() <= 1, kind.id() + " chance");
			assertFalse(kind.biomes().isEmpty(), kind.id() + " lives somewhere");
			assertEquals(kind.biomes().size(), new HashSet<>(kind.biomes()).size(), kind.id() + " lists each biome once");
			for (String biome : kind.biomes()) {
				assertTrue(biome.matches("minecraft:[a-z_]+"), biome);
			}
			assertSame(kind, WildlifeRules.kind(kind.id()));
		}
		assertThrows(IllegalArgumentException.class, () -> WildlifeRules.kind("zombie"));
	}

	@Test
	void theRareOnesStayRare() {
		// The stag spawns alone, never with another nearby, and only a third of good tries; the skyray rarer still.
		assertEquals(1, WildlifeRules.LUMEN_STAG.maxGroup());
		assertTrue(WildlifeRules.roomFor(WildlifeRules.LUMEN_STAG, 0));
		assertFalse(WildlifeRules.roomFor(WildlifeRules.LUMEN_STAG, 1));
		assertTrue(WildlifeRules.LUMEN_STAG.weight() <= 2);
		assertTrue(WildlifeRules.SKYRAY.chance() < WildlifeRules.LUMEN_STAG.chance());
		assertFalse(WildlifeRules.roomFor(WildlifeRules.SKYRAY, 2), "two skyrays in one sky at most");
		assertTrue(WildlifeRules.SKYRAY.crowdRange() >= 96, "and they keep far apart");
		// A swarm has room to gather, but not to flood a forest.
		assertTrue(WildlifeRules.roomFor(WildlifeRules.GLIMMERWING, 8));
		assertFalse(WildlifeRules.roomFor(WildlifeRules.GLIMMERWING, 12));
	}

	@Test
	void theMultiplierScalesChanceBelowOneAndWeightAboveIt() {
		Kind hare = WildlifeRules.RIMEHARE;
		assertEquals(hare.weight(), WildlifeRules.weight(hare, 1));
		assertEquals(hare.weight(), WildlifeRules.weight(hare, 0.25), "fewer spawns come from the chance, not the weight");
		assertEquals(hare.weight() * 3, WildlifeRules.weight(hare, 3));
		assertEquals(1.0, WildlifeRules.chance(hare, 1), 1e-9);
		assertEquals(0.25, WildlifeRules.chance(hare, 0.25), 1e-9);
		assertEquals(1.0, WildlifeRules.chance(hare, 5), 1e-9, "never above every try");
		assertEquals(0, WildlifeRules.chance(hare, 0), 1e-9);
		Kind stag = WildlifeRules.LUMEN_STAG;
		assertEquals(stag.chance() / 2, WildlifeRules.chance(stag, 0.5), 1e-9);
		assertEquals(stag.chance(), WildlifeRules.chance(stag, 4), 1e-9, "a rare one's rarity holds above 1");
		assertTrue(WildlifeRules.weight(stag, 0.01) >= 1);
	}

	@Test
	void antlersBurnWithTheMoon() {
		float full = WildlifeRules.antlerGlow(0, 1);
		float half = WildlifeRules.antlerGlow(2, 1);
		float fresh = WildlifeRules.antlerGlow(4, 1);
		assertEquals(1.0F, full, 1e-6F);
		assertTrue(full > half && half > fresh, full + " > " + half + " > " + fresh);
		assertEquals(WildlifeRules.antlerGlow(2, 1), WildlifeRules.antlerGlow(6, 1), 1e-6F, "waxing and waning match");
		assertEquals(0.35F, fresh, 0.01F);
		// By day they're faint whatever the moon.
		assertEquals(WildlifeRules.antlerGlow(0, 0), WildlifeRules.antlerGlow(4, 0), 1e-6F);
		assertTrue(WildlifeRules.antlerGlow(0, 0) < fresh);
		assertEquals(WildlifeRules.antlerGlow(8, 1), full, 1e-6F, "phases wrap");
	}

	@Test
	void nightComesWithTheDarkeningSky() {
		assertEquals(0, WildlifeRules.night(0), 1e-6);
		assertEquals(1, WildlifeRules.night(11), 1e-6);
		float dusk = WildlifeRules.night(6);
		assertTrue(dusk > 0.3F && dusk < 0.7F, "dusk is in between: " + dusk);
		assertTrue(WildlifeRules.glimmerGlow(1) > WildlifeRules.glimmerGlow(0));
		assertTrue(WildlifeRules.glimmerGlow(0) > 0, "they still glow a little by day");
	}

	@Test
	void aStagSheddsOnceADay() {
		assertEquals(0, WildlifeRules.day(23999));
		assertEquals(1, WildlifeRules.day(24000));
		assertEquals(-1, WildlifeRules.day(-1));
		assertTrue(WildlifeRules.mayShed(-1, 0));
		assertFalse(WildlifeRules.mayShed(5, 5));
		assertTrue(WildlifeRules.mayShed(5, 6));
	}

	@Test
	void aStagTrustsOnlyCalmSneakingPlayers() {
		assertEquals(Stance.IGNORE, WildlifeRules.stagStance(30, false, 0.3, false));
		assertEquals(Stance.FLEE, WildlifeRules.stagStance(10, false, 0, false), "walking up openly scares it");
		assertEquals(Stance.FLEE, WildlifeRules.stagStance(10, true, 0.3, false), "rushing in, even sneaking, scares it");
		assertEquals(Stance.WATCH, WildlifeRules.stagStance(10, true, 0.05, false));
		assertEquals(Stance.TRUST, WildlifeRules.stagStance(2, true, 0.02, false));
		assertEquals(Stance.FLEE, WildlifeRules.stagStance(2, true, 0.02, true), "once frightened, it trusts nobody");
		assertTrue(WildlifeRules.STAG_TRUST_TICKS >= 40, "trust takes a moment");
	}

	@Test
	void tortoiseGardensComeFromTheirBiome() {
		assertEquals(Garden.SWAMP, WildlifeRules.garden("minecraft:swamp"));
		assertEquals(Garden.MANGROVE, WildlifeRules.garden("minecraft:mangrove_swamp"));
		assertEquals(Garden.JUNGLE, WildlifeRules.garden("minecraft:bamboo_jungle"));
		assertNull(WildlifeRules.garden("minecraft:plains"), "elsewhere a tortoise keeps the garden it has");
		for (String biome : WildlifeRules.MOSSBACK_TORTOISE.biomes()) {
			assertNotNull(WildlifeRules.garden(biome), "every tortoise biome grows a garden: " + biome);
		}
		assertEquals(Garden.SWAMP, Garden.byId(0));
		assertEquals(Garden.JUNGLE, Garden.byId(-1), "a bad saved id still names a garden");
		assertTrue(WildlifeRules.SHELL_GUARD < 1);
	}

	@Test
	void intervalsStayInTheirWindows() {
		assertEquals(12000, WildlifeRules.scuteInterval(0));
		assertEquals(24000, WildlifeRules.scuteInterval(1));
		assertEquals(24000, WildlifeRules.scuteInterval(7), "clamped");
		assertEquals(9000, WildlifeRules.membraneInterval(0));
		assertEquals(18000, WildlifeRules.membraneInterval(1));
	}

	@Test
	void aCinderfoxBitesFireWeakFoesHarder() {
		assertEquals(4F, WildlifeRules.sparkBite(4, false), 1e-6F);
		assertEquals(6F, WildlifeRules.sparkBite(4, true), 1e-6F);
	}

	@Test
	void skyraysCruiseHighAndLeanIntoTurns() {
		assertEquals(64 + WildlifeRules.SKYRAY_LOW, WildlifeRules.cruise(64, 0), 1e-9);
		assertEquals(64 + WildlifeRules.SKYRAY_HIGH, WildlifeRules.cruise(64, 1), 1e-9);
		assertEquals(WildlifeRules.SKYRAY_CEILING, WildlifeRules.cruise(180, 0.5), 1e-9, "never above the ceiling");
		assertEquals(0, WildlifeRules.bank(0), 1e-6);
		assertTrue(WildlifeRules.bank(2) > 0 && WildlifeRules.bank(-2) < 0);
		assertEquals(0.7F, WildlifeRules.bank(90), 1e-6F, "never rolls over");
	}

	@Test
	void rimeharesBoltUnlessTempted() {
		assertFalse(WildlifeRules.hareBolts(20, false, true), "too far to notice");
		assertTrue(WildlifeRules.hareBolts(6, false, false));
		assertFalse(WildlifeRules.hareBolts(6, true, false), "berries held out calm it");
		assertTrue(WildlifeRules.hareBolts(6, true, true), "but not if you run at it");
	}

	@Test
	void theFieldGuideListsEveryKindOfWildlife() {
		List<FieldGuide.Entry> wildlife = FieldGuide.group(FieldGuide.Group.WILDLIFE);
		for (Kind kind : WildlifeRules.ALL) {
			String type = "wildercord:" + kind.id();
			assertTrue(wildlife.stream().anyMatch(e -> e.type().equals(type)), type + " is in the guide");
		}
		FieldGuide.Entry stag = FieldGuide.byType("wildercord:lumen_stag").orElseThrow();
		assertEquals("creature:wildercord:lumen_stag", stag.key());
		assertEquals("guide.wildercord.lumen_stag", stag.noteKey());
		assertEquals("guide.wildercord.lumen_stag.hint", stag.hintKey());
		assertEquals(stag, FieldGuide.entry(stag.key()).orElseThrow());
		assertTrue(FieldGuide.entry("creature:minecraft:nothing").isEmpty());
		assertFalse(FieldGuide.isKey("creature:"));
		assertEquals(FieldGuide.REWARD, Feats.reward(stag.key()), "meeting a creature condenses a little mana");
	}

	@Test
	void theGuideListsWhatWasMetInOrder() {
		List<String> grimoire = List.of("reaction:shatter", "creature:wildercord:skyray", "creature:wildercord:rimehare",
			"creature:wildercord:skyray", "creature:minecraft:unknown");
		List<FieldGuide.Entry> met = FieldGuide.met(grimoire);
		assertEquals(List.of("wildercord:skyray", "wildercord:rimehare"), met.stream().map(FieldGuide.Entry::type).toList());
	}

	@Test
	void otherCreaturesJoinTheSameGuide() {
		FieldGuide.Entry monster = new FieldGuide.Entry("wildercord:test_monster", FieldGuide.Group.MONSTER, 0xFF0000);
		FieldGuide.add(monster);
		FieldGuide.add(new FieldGuide.Entry("wildercord:test_monster", FieldGuide.Group.WILDLIFE, 0x00FF00));
		assertEquals(monster, FieldGuide.byType("wildercord:test_monster").orElseThrow(), "the first entry for a type stands");
		assertTrue(FieldGuide.group(FieldGuide.Group.MONSTER).contains(monster));
		assertFalse(FieldGuide.group(FieldGuide.Group.WILDLIFE).contains(monster));
	}
}
