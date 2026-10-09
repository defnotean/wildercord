package dev.wildercord.aura.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildercord.aura.TechniqueRules;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MethodsBMastersTest {
	@Test
	void schoolsSitClearOfTheBuiltInThreeAndMapBothWays() {
		for (int school : MethodsBMasters.SCHOOLS) {
			assertTrue(MethodsBMasters.owns(school));
			assertTrue(school > MastersRules.STONE);
			assertEquals(school, MethodsBMasters.forMethod(MethodsBMasters.id(school)));
			int base = MethodsBMasters.base(school);
			assertTrue(base >= MastersRules.EMBER && base <= MastersRules.STONE);
			assertFalse(MethodsBMasters.reward(school).isEmpty());
			assertTrue(TechniqueRules.allParts().contains(MethodsBMasters.reward(school)), MethodsBMasters.reward(school));
		}
		assertEquals(MastersRules.EMBER, MethodsBMasters.base(MethodsBMasters.ECHO));
		assertEquals(MastersRules.STONE, MethodsBMasters.base(MethodsBMasters.DAWN));
		assertEquals(MastersRules.GALE, MethodsBMasters.base(MethodsBMasters.VENOM));
		for (int school = MastersRules.EMBER; school <= MastersRules.STONE; school++) {
			assertFalse(MethodsBMasters.owns(school));
			assertEquals(0, MethodsBMasters.bit(school));
		}
		assertEquals(-1, MethodsBMasters.forMethod("ember"));
		assertEquals(-1, MethodsBMasters.forMethod(null));
		// Reserved school ids 13 to 15, each clear its own bit of the shared record.
		assertEquals(java.util.List.of(13, 14, 15), MethodsBMasters.SCHOOLS);
		assertEquals(0b111 << 13, MethodsBMasters.bit(MethodsBMasters.ECHO) | MethodsBMasters.bit(MethodsBMasters.DAWN) | MethodsBMasters.bit(MethodsBMasters.VENOM));
		assertEquals(MastersRules.SCHOOLS, MethodsBMasters.VENOM + 1);
		for (int school : MethodsBMasters.SCHOOLS) {
			assertTrue(MastersRules.knownSchool(school));
			assertEquals(MethodsBMasters.reward(school), MasterVictoryRules.reward(school));
			var cleared = MasterVictoryRules.Progress.NONE.withClear(school);
			assertEquals(1 << school, cleared.schools());
			assertTrue(cleared.cleared(school));
		}
	}

	@Test
	void eachMasterHasTwentyTechniquesAndASignatureAllTelegraphed() {
		Set<String> signatures = new HashSet<>();
		for (int school : MethodsBMasters.SCHOOLS) {
			var techniques = MasterTechniques.forSchool(school);
			long plain = techniques.stream().filter(t -> MethodsBMasters.signature(t.key()) == null).count();
			assertTrue(plain >= 20, "school " + school + " has " + plain);
			var own = techniques.stream().filter(t -> MethodsBMasters.signature(t.key()) != null).toList();
			assertEquals(1, own.size(), "school " + school);
			signatures.add(own.getFirst().key());
			Set<String> shapes = new HashSet<>();
			for (var technique : techniques) {
				// The first tell reads at 12 to 14 ticks; the chain ends in a 14 to 16 tick open recovery.
				assertTrue(technique.impact(0) >= 12 && technique.impact(0) <= 14, technique.key() + " tells " + technique.impact(0));
				assertTrue(technique.recovery() >= 14 && technique.recovery() <= 16, technique.key() + " recovers " + technique.recovery());
				assertTrue(technique.impact(1) - technique.impact(0) >= MasterTechniques.MIN_TELL, technique.key());
				assertTrue(technique.cost() + MastersRules.GUARD_COST <= MastersRules.AURA_MAX);
				String shape = technique.strikes().stream().map(strike -> strike.primitive().name() + strike.mirrored()
					+ strike.spin() + strike.step() + strike.quick() + strike.heavy()).toList().toString();
				assertTrue(shapes.add(shape), technique.key() + " repeats another technique");
			}
		}
		assertEquals(Set.of("tolling_bell", "noon_glare", "serpent_coil"), signatures);
		assertNull(MethodsBMasters.signature("ringing_pair"));
		assertNotNull(MethodsBMasters.signature("serpent_coil"));
	}

	@Test
	void signaturesTurnUpMoreOftenThanAPlainTechnique() {
		for (var technique : MasterTechniques.all()) {
			int extra = MethodsBMasters.extraWeight(technique);
			assertEquals(MethodsBMasters.signature(technique.key()) != null ? 2 : 0, extra, technique.key());
		}
	}

	@Test
	void theTollsSecondStrikeIsALaneSoItsAimMatters() {
		var toll = MasterTechniques.forSchool(MethodsBMasters.ECHO).stream().filter(t -> t.key().equals("tolling_bell")).findFirst().orElseThrow();
		assertEquals(2, toll.strikes().size());
		var coil = MasterTechniques.forSchool(MethodsBMasters.VENOM).stream().filter(t -> t.key().equals("serpent_coil")).findFirst().orElseThrow();
		assertEquals(MasterTechniques.Shape.CIRCLE, coil.strikes().getFirst().shape());
		// The coil reaches past a plain whirl, so its opening reach admits it at the same range.
		assertTrue(MethodsBSignatureRules.COIL_RADIUS >= coil.openingReach());
	}
}
