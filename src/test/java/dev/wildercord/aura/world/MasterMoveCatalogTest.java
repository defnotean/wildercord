package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MasterMoveCatalogTest {
	private static final MasterMoveCatalog CATALOG = MasterMoveCatalog.legacy();
	private static final Map<MastersRules.Move, Integer> WIRE_IDS = Map.ofEntries(
		Map.entry(MastersRules.Move.SWEEP, 1),
		Map.entry(MastersRules.Move.THRUST, 2),
		Map.entry(MastersRules.Move.CRESCENT, 3),
		Map.entry(MastersRules.Move.BREAK_CAST, 4),
		Map.entry(MastersRules.Move.CINDER_WAKE, 5),
		Map.entry(MastersRules.Move.PURSUIT_BREAK, 6),
		Map.entry(MastersRules.Move.CROSSWIND_REPRISE, 7),
		Map.entry(MastersRules.Move.STONE_FRACTURE, 8),
		Map.entry(MastersRules.Move.KILN_RING, 9),
		Map.entry(MastersRules.Move.STONE_FAULT_MARCH, 10),
		Map.entry(MastersRules.Move.TECHNIQUE, 11),
		Map.entry(MastersRules.Move.RIME_LATTICE, 12),
		Map.entry(MastersRules.Move.THUNDER_CHAIN, 13),
		Map.entry(MastersRules.Move.VERDANT_BLOOM, 14),
		Map.entry(MastersRules.Move.HOLLOW_PULL, 15),
		// ---- masters-b pack
		Map.entry(MastersRules.Move.STARLIT_CONSTELLATION, 16),
		Map.entry(MastersRules.Move.HOURGLASS_REWIND, 17),
		Map.entry(MastersRules.Move.CRIMSON_FRENZY, 18),
		// ---- methods-a pack
		Map.entry(MastersRules.Move.TIDE_UNDERTOW_RING, 19),
		Map.entry(MastersRules.Move.IRON_ANVIL_VERDICT, 20),
		Map.entry(MastersRules.Move.DUNE_SHIFTING_SANDS, 21));

	@Test
	void freezesAllExistingWireAndStableIdsWithoutCountingSchoolVariantsAsAttacks() {
		assertEquals(21, CATALOG.authoredAttackCount(), "Nine established IDs, Stone Fault March, the shared technique chain, four elemental, three pack B and three methods-a signatures");
		assertEquals(Set.of(MastersRules.Move.values()), WIRE_IDS.keySet(), "A new legacy enum needs deliberate catalog review");
		List<String> names = List.of("sweep", "thrust", "crescent", "break_cast", "cinder_wake", "pursuit_break", "crosswind_reprise", "stone_fracture", "kiln_ring", "stone_fault_march", "technique",
			"rime_lattice", "thunder_chain", "verdant_bloom", "hollow_pull", "starlit_constellation", "hourglass_rewind", "crimson_frenzy",
			"tide_undertow_ring", "iron_anvil_verdict", "dune_shifting_sands");
		for (var entry : WIRE_IDS.entrySet()) {
			var definition = CATALOG.forMove(entry.getKey());
			assertEquals(entry.getValue().intValue(), definition.wireId());
			assertEquals(entry.getValue().intValue(), LegacyMasterMoves.wireId(entry.getKey()));
			assertEquals(entry.getValue().intValue(), entry.getKey().ordinal() + 1, "Adapter must match the still-live legacy protocol");
			assertEquals("wildercord:master/" + names.get(entry.getValue() - 1), definition.id());
			assertSame(definition, CATALOG.byWireId(entry.getValue()).orElseThrow());
			assertSame(definition, CATALOG.byId(definition.id()).orElseThrow());
		}
		assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21), CATALOG.definitions().stream().map(MasterMoveCatalog.Definition::wireId).toList());
	}

	@Test
	void eachSchoolHasSixSharedActionsAndOnlyItsOwnSignatures() {
		List<Set<Integer>> expected = List.of(Set.of(1, 2, 3, 4, 5, 6, 9, 11), Set.of(1, 2, 3, 4, 6, 7, 11), Set.of(1, 2, 3, 4, 6, 8, 10, 11));
		Set<String> union = new HashSet<>();
		int references = 0;
		for (int school = MastersRules.EMBER; school <= MastersRules.STONE; school++) {
			var available = CATALOG.forSchool(school);
			assertEquals(school == MastersRules.GALE ? 7 : 8, available.size());
			assertEquals(expected.get(school), Set.copyOf(available.stream().map(MasterMoveCatalog.Definition::wireId).toList()));
			available.forEach(definition -> union.add(definition.id()));
			references += available.size();
		}
		assertEquals(23, references);
		assertEquals(11, union.size(), "Ember, Gale and Stone use eleven of the catalog's ids");
		// Each elemental school shares the six common actions and owns exactly its one signature.
		for (int school = MastersRules.RIME; school <= MastersRules.HOLLOW; school++) {
			var available = CATALOG.forSchool(school);
			assertEquals(Set.of(1, 2, 3, 4, 6, 11, 9 + school), Set.copyOf(available.stream().map(MasterMoveCatalog.Definition::wireId).toList()));
		}
		// ---- masters-b pack: Starlit, Hourglass and Crimson likewise (wire ids 16 to 18).
		for (int school = MastersPackB.STARLIT; school <= MastersPackB.CRIMSON; school++) {
			var available = CATALOG.forSchool(school);
			assertEquals(Set.of(1, 2, 3, 4, 6, 11, 9 + school), Set.copyOf(available.stream().map(MasterMoveCatalog.Definition::wireId).toList()));
		}
		// ---- methods-a pack: Tide, Iron and Dune likewise (wire ids 19 to 21).
		for (int school = MethodsAMasters.TIDE; school <= MethodsAMasters.DUNE; school++) {
			var available = CATALOG.forSchool(school);
			assertEquals(Set.of(1, 2, 3, 4, 6, 11, 9 + school), Set.copyOf(available.stream().map(MasterMoveCatalog.Definition::wireId).toList()));
		}
		assertTrue(CATALOG.forSchool(-1).isEmpty());
		assertTrue(CATALOG.forSchool(MastersRules.SCHOOLS).isEmpty());
		assertTrue(CATALOG.forSchool(Integer.MAX_VALUE).isEmpty());
	}

	@Test
	void preservesPaymentAndDamageAcrossLegacySchoolsAndPartySizes() {
		double[] ember = {16, 16, 16, 16, 24, 28, 24, 28, 28, 30, 16, 26, 26, 26, 26, 30, 28, 32, 28, 28, 28};
		// Rime, Thunder, Verdant, Hollow, Starlit, Hourglass, Crimson, Tide, Iron and Dune pay Ember's Pursuit price (the pursuit table's default school).
		double[][] costs = {ember, {16, 16, 16, 16, 24, 26, 24, 28, 28, 30, 16, 26, 26, 26, 26, 30, 28, 32, 28, 28, 28},
			{16, 16, 16, 16, 24, 30, 24, 28, 28, 30, 16, 26, 26, 26, 26, 30, 28, 32, 28, 28, 28}, ember, ember, ember, ember, ember, ember, ember,
			ember, ember, ember}; // ---- methods-a pack
		for (int school : new int[] {Integer.MIN_VALUE, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, MastersRules.SCHOOLS, Integer.MAX_VALUE}) {
			for (var entry : WIRE_IDS.entrySet()) {
				MastersRules.Move move = entry.getKey();
				assertEquals(costs[MastersRules.discipline(school)][entry.getValue() - 1], LegacyMasterMoves.auraCost(move, school));
				for (int party : new int[] {-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, Integer.MAX_VALUE}) {
					assertEquals(MastersRules.damage(party, school, move), LegacyMasterMoves.damage(move, party, school));
				}
			}
		}
		// A Wake's later second hit is not merged into the legacy base-strike damage.
		assertEquals(26, LegacyMasterMoves.damage(MastersRules.Move.CINDER_WAKE, 1, MastersRules.EMBER));
		assertEquals(26, EmberWakeRules.AFTERBURN_DAMAGE);
	}

	@Test
	void retainsTheHitTickInsideRecoveryAndEveryExistingAnimationBoundary() {
		int[] tells = {12, 14, 14, 12, 24, 22, 22, 32, 40, 32, 0, 20, 36, 30, 40, 32, 27, 49, 28, 26, 27};
		int[] recoveries = {14, 16, 16, 14, 46, 22, 24, 30, 48, 64, 0, 48, 28, 16, 16, 16, 16, 16, 16, 16, 16};
		int[] rendererIds = {MasterAnimationRules.SWEEP, MasterAnimationRules.THRUST, MasterAnimationRules.CRESCENT,
			MasterAnimationRules.BREAK_CAST, MasterAnimationRules.CINDER_WAKE, MasterAnimationRules.PURSUIT_BREAK,
			MasterAnimationRules.CROSSWIND_REPRISE, MasterAnimationRules.STONE_FRACTURE, MasterAnimationRules.KILN_RING, MasterAnimationRules.STONE_FAULT_MARCH, -1,
			ElementalMasterAnimation.RIME_LATTICE, ElementalMasterAnimation.THUNDER_CHAIN, ElementalMasterAnimation.VERDANT_BLOOM,
			ElementalMasterAnimation.HOLLOW_PULL,
			MastersPackBAnimation.STARLIT_CONSTELLATION, MastersPackBAnimation.HOURGLASS_REWIND, MastersPackBAnimation.CRIMSON_FRENZY,
			MethodsAMasterAnimation.TIDE_UNDERTOW_RING, MethodsAMasterAnimation.IRON_ANVIL_VERDICT, MethodsAMasterAnimation.DUNE_SHIFTING_SANDS}; // ---- methods-a pack
		for (var entry : WIRE_IDS.entrySet()) {
			int wireId = entry.getValue();
			var move = entry.getKey();
			// A technique's clip is a sequence owned by MasterTechniques, not one tell/recovery boundary.
			if (move == MastersRules.Move.TECHNIQUE) continue;
			assertEquals(wireId, rendererIds[wireId - 1]);
			assertEquals(tells[wireId - 1], LegacyMasterMoves.tellTicks(move));
			assertEquals(recoveries[wireId - 1], LegacyMasterMoves.recoveryTicks(move));
			assertEquals(move.tell, LegacyMasterMoves.tellTicks(wireId));
			assertEquals(1, LegacyMasterMoves.activeTicks(wireId));
			assertEquals(move.recovery, LegacyMasterMoves.activeTicks(wireId) + LegacyMasterMoves.visualRecoveryTicks(wireId));
			for (float age = -1; age <= move.tell + move.recovery + 1; age += .5F) {
				assertEquals(MasterAnimationRules.sample(move.ordinal() + 1, age, move.tell, 1, move.recovery - 1),
					MasterAnimationRules.sample(wireId, age, LegacyMasterMoves.tellTicks(wireId), LegacyMasterMoves.activeTicks(wireId),
						LegacyMasterMoves.visualRecoveryTicks(wireId)));
			}
		}
	}

	@Test
	void unknownWireIdsAndIdleNeverAliasAnAttack() {
		for (int id : new int[] {Integer.MIN_VALUE, -1, 0, 22, 255, MasterMoveCatalog.MAX_WIRE_ID, Integer.MAX_VALUE}) {
			assertTrue(CATALOG.byWireId(id).isEmpty());
			assertEquals(0, LegacyMasterMoves.tellTicks(id));
			assertEquals(0, LegacyMasterMoves.activeTicks(id));
			assertEquals(0, LegacyMasterMoves.visualRecoveryTicks(id));
		}
		assertTrue(CATALOG.byId("wildercord:master/unknown").isEmpty());
	}

	@Test
	void snapshotsInputsAndExposesOnlyImmutableCollections() {
		Set<Integer> schools = new HashSet<>(Set.of(MastersRules.EMBER));
		var definition = copy(CATALOG.definitions().getFirst(), "wildercord:master/example", 20, MastersRules.Move.SWEEP, schools);
		schools.clear();
		assertTrue(definition.availableIn(MastersRules.EMBER));
		assertThrows(UnsupportedOperationException.class, () -> definition.schools().add(MastersRules.GALE));
		var input = new ArrayList<>(CATALOG.definitions());
		var snapshot = new MasterMoveCatalog(input);
		input.clear();
		assertEquals(21, snapshot.authoredAttackCount());
		assertThrows(UnsupportedOperationException.class, () -> snapshot.definitions().clear());
		assertThrows(UnsupportedOperationException.class, () -> snapshot.forSchool(MastersRules.GALE).clear());
	}

	@Test
	void canonicalOrderDoesNotDependOnInputOrSchoolSetIteration() {
		var input = new ArrayList<>(CATALOG.definitions());
		for (int shift = 0; shift < input.size(); shift++) {
			Collections.rotate(input, 1);
			var snapshot = new MasterMoveCatalog(input);
			assertEquals(CATALOG.definitions(), snapshot.definitions());
			for (int school = 0; school < 3; school++) assertEquals(CATALOG.forSchool(school), snapshot.forSchool(school));
		}
		Collections.reverse(input);
		assertEquals(CATALOG.definitions(), new MasterMoveCatalog(input).definitions());
	}

	@Test
	void rejectsDuplicateIdentityOnEveryAxis() {
		var first = CATALOG.definitions().getFirst();
		var schools = first.schools();
		assertThrows(IllegalArgumentException.class, () -> new MasterMoveCatalog(List.of(first, first)));
		assertThrows(IllegalArgumentException.class, () -> new MasterMoveCatalog(List.of(first,
			copy(first, first.id(), 20, MastersRules.Move.THRUST, schools))));
		assertThrows(IllegalArgumentException.class, () -> new MasterMoveCatalog(List.of(first,
			copy(first, "wildercord:master/other", first.wireId(), MastersRules.Move.THRUST, schools))));
		assertThrows(IllegalArgumentException.class, () -> new MasterMoveCatalog(List.of(first,
			copy(first, "wildercord:master/other", 20, MastersRules.Move.SWEEP, schools))));
	}

	@Test
	void rejectsMalformedDefinitionsAndUnboundedCatalogInputs() {
		var first = CATALOG.definitions().getFirst();
		for (String id : List.of("", "sweep", "wildercord:master/../sweep", "wildercord:master/SWEEP", "wildercord:master/" + "a".repeat(100)))
			assertThrows(IllegalArgumentException.class, () -> copy(first, id, 1, first.legacyMove(), first.schools()));
		for (int id : new int[] {Integer.MIN_VALUE, -1, 0, MasterMoveCatalog.MAX_WIRE_ID + 1, Integer.MAX_VALUE})
			assertThrows(IllegalArgumentException.class, () -> copy(first, first.id(), id, first.legacyMove(), first.schools()));
		for (Set<Integer> schools : List.of(Set.<Integer>of(), Set.of(-1), Set.of(MastersRules.SCHOOLS), Set.of(0, MastersRules.SCHOOLS)))
			assertThrows(IllegalArgumentException.class, () -> copy(first, first.id(), 1, first.legacyMove(), schools));
		assertThrows(IllegalArgumentException.class, () -> new MasterMoveCatalog(List.of()));
		assertThrows(IllegalArgumentException.class, () -> new MasterMoveCatalog(Collections.nCopies(MasterMoveCatalog.MAX_DEFINITIONS + 1, first)));
		assertThrows(NullPointerException.class, () -> new MasterMoveCatalog(null));
		assertThrows(NullPointerException.class, () -> new MasterMoveCatalog(java.util.Arrays.asList(first, null)));
		assertThrows(NullPointerException.class, () -> copy(first, null, 1, first.legacyMove(), first.schools()));
		assertThrows(NullPointerException.class, () -> copy(first, first.id(), 1, null, first.schools()));
		assertThrows(NullPointerException.class, () -> copy(first, first.id(), 1, first.legacyMove(), null));
		assertThrows(NullPointerException.class, () -> new MasterMoveCatalog.Definition(first.id(), 1, first.legacyMove(), first.schools(), null));
	}

	private static MasterMoveCatalog.Definition copy(MasterMoveCatalog.Definition source, String id, int wireId,
		MastersRules.Move move, Set<Integer> schools) {
		return new MasterMoveCatalog.Definition(id, wireId, move, schools, source.executionFamily());
	}
}
