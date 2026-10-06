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
	private static final Map<MastersRules.Move, Integer> WIRE_IDS = Map.of(
		MastersRules.Move.SWEEP, 1, MastersRules.Move.THRUST, 2, MastersRules.Move.CRESCENT, 3,
		MastersRules.Move.BREAK_CAST, 4, MastersRules.Move.CINDER_WAKE, 5, MastersRules.Move.PURSUIT_BREAK, 6,
		MastersRules.Move.CROSSWIND_REPRISE, 7, MastersRules.Move.STONE_FRACTURE, 8, MastersRules.Move.KILN_RING, 9);

	@Test
	void freezesAllExistingWireAndStableIdsWithoutCountingSchoolVariantsAsAttacks() {
		assertEquals(9, CATALOG.authoredAttackCount(), "Eight existing IDs plus the proposed, not yet certified Kiln Ring");
		assertEquals(Set.of(MastersRules.Move.values()), WIRE_IDS.keySet(), "A new legacy enum needs deliberate catalog review");
		List<String> names = List.of("sweep", "thrust", "crescent", "break_cast", "cinder_wake", "pursuit_break", "crosswind_reprise", "stone_fracture", "kiln_ring");
		for (var entry : WIRE_IDS.entrySet()) {
			var definition = CATALOG.forMove(entry.getKey());
			assertEquals(entry.getValue().intValue(), definition.wireId());
			assertEquals(entry.getValue().intValue(), LegacyMasterMoves.wireId(entry.getKey()));
			assertEquals(entry.getValue().intValue(), entry.getKey().ordinal() + 1, "Adapter must match the still-live legacy protocol");
			assertEquals("wildercord:master/" + names.get(entry.getValue() - 1), definition.id());
			assertSame(definition, CATALOG.byWireId(entry.getValue()).orElseThrow());
			assertSame(definition, CATALOG.byId(definition.id()).orElseThrow());
		}
		assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), CATALOG.definitions().stream().map(MasterMoveCatalog.Definition::wireId).toList());
	}

	@Test
	void eachSchoolHasFiveSharedActionsAndOnlyItsOwnSignatures() {
		List<Set<Integer>> expected = List.of(Set.of(1, 2, 3, 4, 5, 6, 9), Set.of(1, 2, 3, 4, 6, 7), Set.of(1, 2, 3, 4, 6, 8));
		Set<String> union = new HashSet<>();
		int references = 0;
		for (int school = MastersRules.EMBER; school <= MastersRules.STONE; school++) {
			var available = CATALOG.forSchool(school);
			assertEquals(school == MastersRules.EMBER ? 7 : 6, available.size());
			assertEquals(expected.get(school), Set.copyOf(available.stream().map(MasterMoveCatalog.Definition::wireId).toList()));
			available.forEach(definition -> union.add(definition.id()));
			references += available.size();
		}
		assertEquals(19, references);
		assertEquals(9, union.size());
		assertTrue(CATALOG.forSchool(-1).isEmpty());
		assertTrue(CATALOG.forSchool(3).isEmpty());
		assertTrue(CATALOG.forSchool(Integer.MAX_VALUE).isEmpty());
	}

	@Test
	void preservesPaymentAndDamageAcrossLegacySchoolsAndPartySizes() {
		double[][] costs = {{16, 16, 16, 16, 24, 28, 24, 28, 28}, {16, 16, 16, 16, 24, 26, 24, 28, 28}, {16, 16, 16, 16, 24, 30, 24, 28, 28}};
		for (int school : new int[] {Integer.MIN_VALUE, -1, 0, 1, 2, 3, Integer.MAX_VALUE}) {
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
		int[] tells = {18, 22, 20, 20, 24, 22, 22, 32, 40};
		int[] recoveries = {20, 24, 24, 24, 56, 30, 32, 40, 48};
		int[] rendererIds = {MasterAnimationRules.SWEEP, MasterAnimationRules.THRUST, MasterAnimationRules.CRESCENT,
			MasterAnimationRules.BREAK_CAST, MasterAnimationRules.CINDER_WAKE, MasterAnimationRules.PURSUIT_BREAK,
			MasterAnimationRules.CROSSWIND_REPRISE, MasterAnimationRules.STONE_FRACTURE, MasterAnimationRules.KILN_RING};
		for (var entry : WIRE_IDS.entrySet()) {
			int wireId = entry.getValue();
			var move = entry.getKey();
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
		for (int id : new int[] {Integer.MIN_VALUE, -1, 0, 10, 255, MasterMoveCatalog.MAX_WIRE_ID, Integer.MAX_VALUE}) {
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
		assertEquals(9, snapshot.authoredAttackCount());
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
		for (Set<Integer> schools : List.of(Set.<Integer>of(), Set.of(-1), Set.of(3), Set.of(0, 3)))
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
