package dev.wildercord.aura.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MasterTechniquesTest {
	@Test
	void aHundredDistinctTechniquesAcrossTheThreeSchools() {
		var all = MasterTechniques.all();
		// 100 established techniques, 104 for Rime, Thunder, Verdant and Hollow (masters-a pack), 78 for Starlit, Hourglass and Crimson (masters-b pack),
		// 66 for Tide, Iron and Dune (methods-a pack, ids 283-348) and 69 for Echo, Dawn and Venom (methods-b pack, ids 349-417).
		assertEquals(417, all.size());
		assertEquals(417, all.stream().map(MasterTechniques.Technique::key).distinct().count());
		assertEquals(100, all.stream().filter(technique -> technique.school() <= MastersRules.STONE).count());
		for (int school : new int[] {MastersRules.EMBER, MastersRules.GALE, MastersRules.STONE})
			assertTrue(MasterTechniques.forSchool(school).size() >= 33, "school " + school);
		// No two techniques of a school are the same sequence of strikes.
		for (int school : new int[] {MastersRules.EMBER, MastersRules.GALE, MastersRules.STONE}) {
			Set<String> shapes = new HashSet<>();
			for (var technique : MasterTechniques.forSchool(school)) {
				String signature = technique.strikes().stream().map(strike -> strike.primitive().name() + strike.mirrored()
					+ strike.spin() + strike.step() + strike.quick() + strike.heavy()).toList().toString();
				assertTrue(shapes.add(signature), technique.key() + " repeats another technique");
			}
		}
	}

	@Test
	void everyStrikeIsTelegraphedAndEveryChainEndsOpen() {
		for (var technique : MasterTechniques.all()) {
			int previous = 0;
			for (int i = 0; i < technique.strikes().size(); i++) {
				assertTrue(technique.impact(i) - previous >= MasterTechniques.MIN_TELL, technique.key());
				previous = technique.impact(i);
			}
			assertTrue(technique.impact(0) >= 8, technique.key() + " opens with a readable chamber");
			assertTrue(technique.recovery() >= 10, technique.key());
			assertTrue(technique.cost() >= MastersRules.ATTACK_COST && technique.cost() + MastersRules.GUARD_COST <= MastersRules.AURA_MAX);
			double total = technique.strikes().stream().mapToDouble(MasterTechniques.Strike::damage).sum();
			assertTrue(total > 0 && total <= MasterTechniques.MAX_SHARE + 1e-9, technique.key() + " total damage share " + total);
		}
	}

	@Test
	void posesAreFiniteAndChangeThroughTheCombo() {
		for (var technique : MasterTechniques.all()) {
			assertEquals(0, MasterTechniques.sample(technique.id(), -1).weight());
			assertEquals(0, MasterTechniques.sample(technique.id(), technique.duration()).weight());
			var chamber = MasterTechniques.sample(technique.id(), technique.impact(0) * .5F);
			var impact = MasterTechniques.sample(technique.id(), technique.impact(0));
			assertTrue(impact.weight() > .99F, technique.key());
			assertNotEquals(chamber.sword(), impact.sword(), technique.key());
			for (float age = 0; age < technique.duration(); age += .5F) {
				var pose = MasterTechniques.sample(technique.id(), age);
				assertTrue(Float.isFinite(pose.weight()) && Float.isFinite(pose.sword().x()) && Float.isFinite(pose.rootYaw())
					&& Math.abs(pose.rootYaw()) <= Math.PI * 4.01, technique.key() + " at " + age);
			}
		}
		assertNull(MasterTechniques.byId(0));
		// ---- methods-a pack holds 283-348; methods-b pack: Echo, Dawn and Venom hold the reserved ids 349-417.
		assertNotNull(MasterTechniques.byId(348));
		assertNotNull(MasterTechniques.byId(349));
		assertNotNull(MasterTechniques.byId(417));
		assertNull(MasterTechniques.byId(418));
		assertEquals(0, MasterTechniques.sample(0, 5).weight());
	}

	@Test
	void shapesHaveTheirOwnCounterplay() {
		var low = MasterTechniques.byId(MasterTechniques.all().stream().filter(t -> t.key().equals("lantern_sweep")).findFirst().orElseThrow().id());
		var lowStrike = low.strikes().getFirst();
		assertTrue(lowStrike.hits(2, 0, 0, false));
		assertFalse(lowStrike.hits(2, 0, 1, false), "jumping clears a low cut");
		var high = MasterTechniques.all().stream().filter(t -> t.key().equals("headwind")).findFirst().orElseThrow().strikes().getFirst();
		assertTrue(high.hits(2, 0, 0, false));
		assertFalse(high.hits(2, 0, 0, true), "crouching ducks a high cut");
		var whirl = MasterTechniques.all().stream().filter(t -> t.key().equals("vortex")).findFirst().orElseThrow().strikes().getFirst();
		assertTrue(whirl.hits(-2, 0, 0, false), "a whirl reaches behind");
		assertFalse(whirl.hits(-4, 0, 0, false));
		var thrust = MasterTechniques.all().stream().filter(t -> t.key().equals("zephyr_point")).findFirst().orElseThrow().strikes().getFirst();
		assertFalse(thrust.hits(3, 1.5, 0, false), "sidestepping a thrust works");
	}

	@Test
	void everyTechniqueHasAName() throws Exception {
		var lang = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/wildercord/lang/en_us.json"))).getAsJsonObject();
		for (var technique : MasterTechniques.all()) assertTrue(lang.has(technique.translationKey()), technique.key());
		assertTrue(lang.has("boss.wildercord.master.technique"));
	}
}
