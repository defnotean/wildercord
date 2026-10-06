package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.wildercord.aura.world.MastersRules.Move.*;
import static org.junit.jupiter.api.Assertions.*;

class MasterOrdinaryPlannerRulesTest {
	private static final Set<MastersRules.Move> ALL = Set.of(SWEEP, THRUST, CRESCENT);
	private static final UUID TARGET = new UUID(0, 1), ALLY = new UUID(0, 2);

	@Test void replayIsExactAcrossNewInstancesAndPhaseProfiles() {
		for (int school = 0; school < 3; school++) {
			assertEquals(replay(school, 51), replay(school, 51));
		}
		assertEquals("CRESCENT,THRUST,neutral,SWEEP,THRUST,neutral,SWEEP,CRESCENT,neutral,THRUST,SWEEP,neutral", replay(MastersRules.EMBER, 51));
		assertEquals("THRUST,SWEEP,neutral,CRESCENT,THRUST,neutral,CRESCENT,THRUST,neutral,SWEEP,THRUST,neutral", replay(MastersRules.GALE, 51));
		assertEquals("CRESCENT,neutral,SWEEP,neutral,THRUST,neutral,CRESCENT,neutral,THRUST,neutral,CRESCENT,neutral", replay(MastersRules.STONE, 51));
	}

	@Test void proposalDoesNotPayAdvanceHistoryOrRerollAndAdmissionIsSingleUse() {
		var adapter = new MasterOrdinaryPlanner(0, 9);
		adapter.observe(TARGET, Set.of(TARGET));
		var before = adapter.state(); var proposal = adapter.propose(SWEEP, ALL, 100);
		for (int i = 0; i < 10; i++) {
			assertSame(before, adapter.state());
			assertEquals(proposal.move(), adapter.propose(SWEEP, ALL, 100).move());
		}
		assertTrue(adapter.admitted(proposal));
		var admitted = adapter.state();
		assertEquals(1, admitted.successfulDecisions());
		assertEquals(List.of(id(proposal.move())), admitted.history());
		assertFalse(adapter.admitted(proposal)); assertFalse(adapter.neutral(proposal));
		assertSame(admitted, adapter.state());
	}

	@Test void interruptionGuardObstructionAndContextChangesPreserveHistoryAndExpireTickets() {
		var adapter = new MasterOrdinaryPlanner(0, 7);
		adapter.observe(TARGET, Set.of(TARGET));
		assertTrue(adapter.admitted(adapter.propose(SWEEP, ALL, 100)));
		var accepted = adapter.state(); var stale = adapter.propose(THRUST, ALL, 84);
		adapter.endPhrase();
		assertFalse(adapter.admitted(stale));
		assertEquals(accepted.history(), adapter.state().history());
		assertEquals(accepted.successfulDecisions(), adapter.state().successfulDecisions());
		assertEquals(0, adapter.state().depth());
		var targetChange = adapter.propose(SWEEP, ALL, 84);
		adapter.observe(ALLY, Set.of(TARGET, ALLY));
		assertFalse(adapter.admitted(targetChange));
		var rosterChange = adapter.propose(SWEEP, ALL, 84);
		adapter.observe(ALLY, Set.of(ALLY));
		assertFalse(adapter.admitted(rosterChange));
		assertEquals(accepted.history(), adapter.state().history());
		assertTrue(adapter.contextMatches(ALLY, Set.of(ALLY)));
		assertFalse(adapter.contextMatches(ALLY, Set.of(TARGET, ALLY)));
	}

	@Test void evenNeutralBoundaryInvalidatesIdenticalStateAndForeignTickets() {
		var adapter = new MasterOrdinaryPlanner(0, 3);
		var before = adapter.state(); var ticket = adapter.propose(SWEEP, ALL, 100);
		adapter.endPhrase();
		assertEquals(before, adapter.state()); assertFalse(adapter.current(ticket));
		assertFalse(adapter.admitted(ticket)); assertSame(before, adapter.state());
		var foreign = new MasterOrdinaryPlanner(0, 3).propose(SWEEP, ALL, 100);
		assertFalse(adapter.admitted(foreign)); assertSame(before, adapter.state());
	}

	@Test void lawfulAlternativeExcludesImmediateRepeatAcrossProfileAndGuardChanges() {
		for (int school = 0; school < 3; school++) for (int seed = 0; seed < 50; seed++) {
			var adapter = new MasterOrdinaryPlanner(school, seed);
			adapter.admittedExternal(SWEEP);
			var proposal = adapter.propose(SWEEP, ALL, 84);
			assertNotEquals(SWEEP, proposal.move()); assertTrue(adapter.admitted(proposal));
			var history = adapter.state().history(); adapter.endPhrase();
			assertEquals(history, adapter.state().history());
			assertNotEquals(proposal.move(), adapter.propose(THRUST, ALL, 68).move());
		}
	}

	@Test void oneLegalMoveCanRepeatAndNeutralNeverRedraws() {
		for (int school = 0; school < 3; school++) {
			var adapter = new MasterOrdinaryPlanner(school, 23);
			for (int i = 0; i < (school == 2 ? 1 : 2); i++) {
				var proposal = adapter.propose(SWEEP, Set.of(SWEEP), 100);
				assertEquals(SWEEP, proposal.move()); assertTrue(adapter.admitted(proposal));
			}
			var before = adapter.state(); var neutral = adapter.propose(SWEEP, ALL, 100);
			assertNull(neutral.move()); assertFalse(adapter.admitted(neutral)); assertSame(before, adapter.state());
			assertTrue(adapter.neutral(neutral)); assertEquals(before.history(), adapter.state().history());
			assertEquals(0, adapter.state().depth());
			assertFalse(adapter.neutral(neutral));
		}
	}

	@Test void priorityAndFallbackStartsCountOnceWithoutExtendingTheOrdinaryPhrase() {
		var adapter = new MasterOrdinaryPlanner(MastersRules.EMBER, -9);
		for (var move : List.of(CINDER_WAKE, SWEEP, THRUST, KILN_RING, CRESCENT, PURSUIT_BREAK)) {
			adapter.admittedExternal(move); assertEquals(0, adapter.state().depth());
		}
		assertEquals(6, adapter.state().successfulDecisions());
		assertEquals(List.of(id(THRUST), id(KILN_RING), id(CRESCENT), id(PURSUIT_BREAK)), adapter.state().history());
		var before = adapter.state(); assertThrows(IllegalArgumentException.class, () -> adapter.admittedExternal(STONE_FRACTURE));
		assertSame(before, adapter.state());
	}

	@Test void geometryAuraAndAllowlistCannotInventEligibility() {
		assertEquals(ALL, MasterOrdinaryPlanner.spatialCandidates(4, 2.5));
		assertEquals(Set.of(THRUST, CRESCENT), MasterOrdinaryPlanner.spatialCandidates(4.001, -2.5));
		for (double distance : new double[] {-1, 6.001, Double.NaN, Double.POSITIVE_INFINITY})
			assertTrue(MasterOrdinaryPlanner.spatialCandidates(distance, 0).isEmpty());
		for (double height : new double[] {2.501, -2.501, Double.NaN, Double.NEGATIVE_INFINITY})
			assertTrue(MasterOrdinaryPlanner.spatialCandidates(3, height).isEmpty());
		var adapter = new MasterOrdinaryPlanner(0, 1);
		assertNull(adapter.propose(SWEEP, ALL, 15.99).move()); assertNull(adapter.propose(SWEEP, Set.of(), 100).move());
		for (double aura : new double[] {-1, 100.1, Double.NaN, Double.POSITIVE_INFINITY})
			assertThrows(IllegalArgumentException.class, () -> adapter.propose(SWEEP, ALL, aura));
		assertThrows(IllegalArgumentException.class, () -> adapter.propose(CINDER_WAKE, ALL, 100));
		assertThrows(IllegalArgumentException.class, () -> adapter.propose(SWEEP, Set.of(KILN_RING), 100));
		assertTrue(adapter.state().history().isEmpty());
	}

	@Test void graphsAndObservedCombinationsDoNotAddAuthoredAttacks() {
		int profiles = 0, edges = 0; var routes = new HashSet<String>();
		for (int school = 0; school < 3; school++) for (int preferred = 0; preferred < 3; preferred++) {
			var graph = MasterOrdinaryPlanner.graph(school, preferred); profiles++; edges += graph.edges().size();
			assertEquals(4, graph.nodes().size()); assertEquals(school == 2 ? 1 : 2, graph.maxDepth());
			for (var edge : graph.edges()) routes.add(school + ":" + edge.from() + ":" + edge.to());
		}
		assertEquals(9, profiles); assertEquals(81, edges); assertEquals(27, routes.size());
		assertEquals(10, MasterMoveCatalog.legacy().authoredAttackCount());
	}

	@Test void seedUsesOnlyFixedEncounterIdentity() {
		assertEquals(131073L, MasterOrdinaryPlanner.encounterSeed(new UUID(0, 1), TARGET));
		assertNotEquals(MasterOrdinaryPlanner.encounterSeed(TARGET, ALLY), MasterOrdinaryPlanner.encounterSeed(ALLY, TARGET));
	}

	private static String replay(int school, long seed) {
		var adapter = new MasterOrdinaryPlanner(school, seed); var events = new ArrayList<String>();
		adapter.observe(TARGET, Set.of(TARGET));
		adapter.admittedExternal(MastersRules.move(school, 0, 0, 3));
		for (int i = 0; i < 12; i++) {
			var preferred = MastersRules.move(school, i + 1, i % 3, 3);
			var ticket = adapter.propose(preferred, ALL, 100);
			if (ticket.move() == null) { events.add("neutral"); assertTrue(adapter.neutral(ticket)); }
			else { events.add(ticket.move().name()); assertTrue(adapter.admitted(ticket)); }
		}
		return String.join(",", events);
	}
	private static String id(MastersRules.Move move) { return MasterMoveCatalog.legacy().forMove(move).id(); }
}
