package dev.wildercord.aura.world;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static dev.wildercord.aura.world.MasterMoveGraph.Condition.ELIGIBLE;
import static dev.wildercord.aura.world.MasterMoveGraph.Condition.NEUTRAL_ONLY;
import static dev.wildercord.aura.world.MasterMovePlanner.Outcome.*;
import static org.junit.jupiter.api.Assertions.*;

class MasterMovePlannerTest {
	private static final String SWEEP = move("sweep"), THRUST = move("thrust"), CRESCENT = move("crescent");
	private static final MasterMovePlanner.Eligibility ALL = eligibility(SWEEP, THRUST, CRESCENT);

	@Test
	void fixedSeedGoldenReplaysFreezeSelectionAndNeutralBoundaries() {
		assertEquals("neutral_sweep,sweep_crescent,neutral,neutral_sweep,sweep_thrust,neutral,neutral_sweep,sweep_crescent,neutral,"
			+ "neutral_thrust,thrust_crescent,neutral,neutral_thrust,thrust_sweep,neutral,neutral_thrust,thrust_crescent,neutral",
			replay(graph(MastersRules.EMBER), 0x123456789abcdefL, 18));
		assertEquals("neutral_sweep,sweep_thrust,neutral,neutral_sweep,sweep_crescent,neutral,neutral_thrust,thrust_crescent,neutral,"
			+ "neutral_thrust,thrust_sweep,neutral,neutral_thrust,thrust_sweep,neutral,neutral_crescent,crescent_sweep,neutral",
			replay(graph(MastersRules.GALE), -1L, 18));
		assertEquals("neutral_sweep,neutral,neutral_crescent,neutral,neutral_sweep,neutral,neutral_thrust,neutral,neutral_sweep,neutral,neutral_thrust,neutral",
			replay(graph(MastersRules.STONE), Long.MIN_VALUE, 12));
	}

	@Test
	void insertionOrderAndFreshPlannerInstancesCannotChangeReplay() {
		for (int school = 0; school < 3; school++) {
			var original = graph(school);
			var nodes = new ArrayList<>(original.nodes());
			var edges = new ArrayList<>(original.edges());
			for (int shift = 0; shift < edges.size(); shift++) {
				Collections.rotate(nodes, 1);
				Collections.rotate(edges, 1);
				var shuffled = new MasterMoveGraph(original.id(), school, nodes, edges);
				assertEquals(original.nodes(), shuffled.nodes());
				assertEquals(original.edges(), shuffled.edges());
				for (long seed : new long[] {0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE})
					assertEquals(replay(original, seed, 80), replay(shuffled, seed, 80));
			}
		}
		var planner = new MasterMovePlanner(graph(0));
		var state = planner.initial(17);
		var reversed = new LinkedHashSet<>(List.of(CRESCENT, THRUST, SWEEP));
		assertEquals(planner.propose(state, ALL).choice(), planner.propose(state, new MasterMovePlanner.Eligibility(reversed, 100)).choice());
	}

	@Test
	void declinedAdmissionLeavesAllStateAndSnapshotsUnchangedWithoutRerolling() {
		var planner = new MasterMovePlanner(graph(0));
		var state = planner.initial(23);
		var proposal = planner.propose(state, ALL);
		for (int retry = 0; retry < 20; retry++) {
			assertSame(state, planner.resolve(state, proposal, DECLINED));
			assertEquals(proposal.choice(), planner.propose(state, ALL).choice());
			assertEquals(proposal.nextState(), planner.propose(state, ALL).nextState());
		}
		assertEquals(0, state.successfulDecisions());
		assertEquals(100, ALL.availableAura());
		assertEquals(Set.of(SWEEP, THRUST, CRESCENT), ALL.moveIds());
		assertTrue(state.history().isEmpty());
	}

	@Test
	void acceptedInterruptionCountsExactlyOnceAndStaleAcceptanceFails() {
		var planner = new MasterMovePlanner(graph(0));
		var before = planner.initial(8);
		var proposal = planner.propose(before, ALL);
		var started = planner.resolve(before, proposal, STARTED);
		var interrupted = planner.endPhrase(started);
		assertEquals(interrupted, planner.resolve(before, proposal, STARTED_INTERRUPTED));
		assertEquals(1, interrupted.successfulDecisions());
		assertEquals(List.of(proposal.choice().orElseThrow().moveId()), interrupted.history());
		assertEquals(0, interrupted.depth());
		assertEquals("neutral", interrupted.nodeId());
		assertSame(interrupted, planner.endPhrase(interrupted));
		assertThrows(IllegalArgumentException.class, () -> planner.resolve(started, proposal, STARTED));
		assertThrows(IllegalArgumentException.class, () -> planner.resolve(interrupted, proposal, STARTED_INTERRUPTED));
		var foreign = new MasterMovePlanner(graph(0));
		assertThrows(IllegalArgumentException.class, () -> foreign.resolve(before, proposal, STARTED));
	}

	@Test
	void cyclesAlwaysEndAtSchoolDepthAndCannotEraseHistory() {
		for (int school = 0; school < 3; school++) {
			var graph = graph(school);
			var planner = new MasterMovePlanner(graph);
			var state = planner.initial(93);
			int starts = 0, depth = 0;
			var expectedHistory = new ArrayList<String>();
			for (int attempt = 0; attempt < 1000; attempt++) {
				var proposal = planner.propose(state, ALL);
				if (depth == graph.maxDepth()) {
					assertTrue(proposal.choice().isEmpty());
					state = planner.resolve(state, proposal, NEUTRAL);
					depth = 0;
				} else {
					String chosen = proposal.choice().orElseThrow().moveId();
					expectedHistory.add(chosen);
					if (expectedHistory.size() > 4) expectedHistory.removeFirst();
					state = planner.resolve(state, proposal, STARTED);
					starts++;
					depth++;
				}
				assertEquals(starts, state.successfulDecisions());
				assertEquals(depth, state.depth());
				assertEquals(expectedHistory, state.history());
			}
		}
	}

	@Test
	void noEligibleOrAffordableEdgeProposesNeutralWithoutInventingAnAttack() {
		var planner = new MasterMovePlanner(graph(0));
		var initial = planner.initial(5);
		var started = planner.resolve(initial, planner.propose(initial, ALL), STARTED);
		for (var eligibility : List.of(eligibility(), new MasterMovePlanner.Eligibility(ALL.moveIds(), 15.99), eligibility(move("cinder_wake")))) {
			var proposal = planner.propose(started, eligibility);
			assertTrue(proposal.choice().isEmpty());
			assertSame(started, planner.resolve(started, proposal, DECLINED));
			var neutral = planner.resolve(started, proposal, NEUTRAL);
			assertEquals(started.history(), neutral.history());
			assertEquals(started.successfulDecisions(), neutral.successfulDecisions());
			assertEquals(0, neutral.depth());
			assertThrows(IllegalArgumentException.class, () -> planner.resolve(started, proposal, STARTED));
			assertThrows(IllegalArgumentException.class, () -> planner.resolve(started, proposal, STARTED_INTERRUPTED));
		}
		assertSame(initial, planner.resolve(initial, planner.propose(initial, eligibility()), NEUTRAL));
		assertThrows(IllegalArgumentException.class, () -> planner.resolve(initial, planner.propose(initial, ALL), NEUTRAL));
	}

	@Test
	void oneLegalChoiceMayRepeatIncludingAcrossNeutralAndHistoryRetainsDuplicates() {
		for (int school = 0; school < 3; school++) {
			var planner = new MasterMovePlanner(graph(school));
			var state = planner.initial(18);
			for (int i = 0; i < 30; i++) {
				var proposal = planner.propose(state, eligibility(SWEEP));
				if (proposal.choice().isPresent()) {
					assertEquals(SWEEP, proposal.choice().orElseThrow().moveId());
					state = planner.resolve(state, proposal, STARTED);
				} else state = planner.resolve(state, proposal, NEUTRAL);
			}
			assertEquals(List.of(SWEEP, SWEEP, SWEEP, SWEEP), state.history());
		}
	}

	@Test
	void immediateRepeatIsExcludedByAuthoredIdEvenAcrossDifferentNodes() {
		var nodes = List.of(MasterMoveGraph.Node.neutral("neutral"), action("sweep_a", SWEEP), action("sweep_b", SWEEP), action("thrust", THRUST));
		var edges = List.of(edge("a", "neutral", "sweep_a", 100), edge("b", "neutral", "sweep_b", 100), edge("c", "neutral", "thrust", 1));
		var planner = new MasterMovePlanner(new MasterMoveGraph(graphId(0), 0, nodes, edges));
		for (long seed = 0; seed < 100; seed++) {
			var state = new MasterMovePlanner.State(seed, 1, "neutral", 0, List.of(SWEEP));
			assertEquals(THRUST, planner.propose(state, eligibility(SWEEP, THRUST)).choice().orElseThrow().moveId());
		}
	}

	@Test
	void olderRecentMovesHaveHalfWeightWithPositiveFloorAndExpireAfterFourStarts() {
		var graph = new MasterMoveGraph(graphId(0), 0,
			List.of(MasterMoveGraph.Node.neutral("neutral"), action("sweep", SWEEP), action("thrust", THRUST)),
			List.of(edge("a", "neutral", "sweep", 9), edge("b", "neutral", "thrust", 1)));
		var planner = new MasterMovePlanner(graph);
		var history = List.of(SWEEP, THRUST, move("cinder_wake"), CRESCENT);
		Set<String> seen = new HashSet<>();
		for (long seed = 0; seed < 100; seed++) {
			var state = new MasterMovePlanner.State(seed, 4, "neutral", 0, history);
			var choice = planner.propose(state, eligibility(SWEEP, THRUST)).choice().orElseThrow();
			assertEquals(choice.moveId().equals(SWEEP) ? 4 : 1, choice.effectiveWeight());
			seen.add(choice.moveId());
		}
		assertEquals(Set.of(SWEEP, THRUST), seen);
		var state = new MasterMovePlanner.State(1, 4, "neutral", 0, history);
		state = planner.resolve(state, planner.propose(state, eligibility(THRUST)), STARTED);
		state = planner.endPhrase(state);
		assertEquals(List.of(THRUST, move("cinder_wake"), CRESCENT, THRUST), state.history());
		assertEquals(9, planner.propose(state, eligibility(SWEEP)).choice().orElseThrow().effectiveWeight());
	}

	@Test
	void eligibilitySnapshotsFreezeSetsAndRejectNonFiniteOrOutOfBoundsAura() {
		var source = new HashSet<>(ALL.moveIds());
		var eligibility = new MasterMovePlanner.Eligibility(source, 16);
		source.clear();
		assertEquals(ALL.moveIds(), eligibility.moveIds());
		assertThrows(UnsupportedOperationException.class, () -> eligibility.moveIds().clear());
		for (double aura : new double[] {Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, -1, 100.01})
			assertThrows(IllegalArgumentException.class, () -> new MasterMovePlanner.Eligibility(Set.of(SWEEP), aura));
		assertThrows(NullPointerException.class, () -> new MasterMovePlanner.Eligibility(null, 100));
		assertThrows(NullPointerException.class, () -> new MasterMovePlanner.Eligibility(new HashSet<>(Arrays.asList(SWEEP, null)), 100));
		assertThrows(IllegalArgumentException.class, () -> eligibility(move("unknown")));
		var planner = new MasterMovePlanner(graph(0));
		assertTrue(planner.propose(planner.initial(1), new MasterMovePlanner.Eligibility(Set.of(SWEEP), 16)).choice().isPresent());
		assertTrue(planner.propose(planner.initial(1), new MasterMovePlanner.Eligibility(Set.of(SWEEP), 0)).choice().isEmpty());
		assertThrows(IllegalArgumentException.class, () -> planner.propose(planner.initial(1), eligibility(move("stone_fracture"))));
	}

	@Test
	void everySchoolCostUsesLegacyFundingAndCallerCooldownExclusionsAreNeverOverridden() {
		for (int school = 0; school < 3; school++) {
			for (var definition : MasterMoveCatalog.legacy().forSchool(school)) {
				var planner = new MasterMovePlanner(new MasterMoveGraph(graphId(school), school,
					List.of(MasterMoveGraph.Node.neutral("neutral"), action("action", definition.id())),
					List.of(new MasterMoveGraph.Edge("entry", "neutral", "action", 1, NEUTRAL_ONLY))));
				var state = planner.initial(4);
				double cost = LegacyMasterMoves.auraCost(definition.legacyMove(), school);
				assertTrue(planner.propose(state, new MasterMovePlanner.Eligibility(Set.of(definition.id()), Math.nextDown(cost))).choice().isEmpty());
				assertEquals(definition.id(), planner.propose(state, new MasterMovePlanner.Eligibility(Set.of(definition.id()), cost)).choice().orElseThrow().moveId());
				assertTrue(planner.propose(state, new MasterMovePlanner.Eligibility(Set.of(), 100)).choice().isEmpty(), "Caller exclusions, including cooldowns, remain excluded");
			}
		}
	}

	@Test
	void historyInputsAndAllGraphCollectionsAreImmutableSnapshots() {
		var history = new ArrayList<>(List.of(SWEEP));
		var state = new MasterMovePlanner.State(1, 1, "neutral", 0, history);
		history.clear();
		assertEquals(List.of(SWEEP), state.history());
		assertThrows(UnsupportedOperationException.class, () -> state.history().clear());
		var original = graph(0);
		var nodes = new ArrayList<>(original.nodes());
		var edges = new ArrayList<>(original.edges());
		var graph = new MasterMoveGraph(original.id(), 0, nodes, edges);
		nodes.clear();
		edges.clear();
		assertEquals(original.nodes(), graph.nodes());
		assertEquals(original.edges(), graph.edges());
		assertThrows(UnsupportedOperationException.class, () -> graph.nodes().clear());
		assertThrows(UnsupportedOperationException.class, () -> graph.edges().clear());
	}

	@Test
	void rejectsMalformedStatesAndOrdinalOverflowRatherThanWrappingReplay() {
		var planner = new MasterMovePlanner(graph(0));
		assertThrows(IllegalArgumentException.class, () -> new MasterMovePlanner.State(0, -1, "neutral", 0, List.of()));
		assertThrows(IllegalArgumentException.class, () -> new MasterMovePlanner.State(0, 0, "neutral", 1, List.of()));
		assertThrows(IllegalArgumentException.class, () -> new MasterMovePlanner.State(0, 1, "neutral", -1, List.of(SWEEP)));
		assertThrows(IllegalArgumentException.class, () -> new MasterMovePlanner.State(0, 4, "neutral", 3, Collections.nCopies(4, SWEEP)));
		assertThrows(IllegalArgumentException.class, () -> new MasterMovePlanner.State(0, 2, "neutral", 0, List.of(SWEEP)));
		assertThrows(IllegalArgumentException.class, () -> new MasterMovePlanner.State(0, 5, "neutral", 0, Collections.nCopies(5, SWEEP)));
		assertThrows(IllegalArgumentException.class, () -> new MasterMovePlanner.State(0, 1, "neutral", 0, List.of(move("unknown"))));
		for (var state : List.of(
			new MasterMovePlanner.State(0, 0, "unknown", 0, List.of()),
			new MasterMovePlanner.State(0, 1, "neutral", 1, List.of(SWEEP)),
			new MasterMovePlanner.State(0, 1, "sweep", 0, List.of(SWEEP)),
			new MasterMovePlanner.State(0, 1, "sweep", 1, List.of(THRUST)),
			new MasterMovePlanner.State(0, 1, "neutral", 0, List.of(move("stone_fracture"))))) {
			assertThrows(IllegalArgumentException.class, () -> planner.propose(state, ALL));
			assertThrows(IllegalArgumentException.class, () -> planner.endPhrase(state));
		}
		var stone = new MasterMovePlanner(graph(2));
		assertThrows(IllegalArgumentException.class, () -> stone.propose(new MasterMovePlanner.State(0, 2, "sweep", 2, List.of(THRUST, SWEEP)), ALL));
		var exhausted = new MasterMovePlanner.State(0, Long.MAX_VALUE, "neutral", 0, Collections.nCopies(4, SWEEP));
		assertThrows(IllegalStateException.class, () -> planner.propose(exhausted, ALL));
		assertEquals(exhausted, planner.propose(exhausted, eligibility()).nextState());
	}

	@Test
	void rejectsMalformedIdsSchoolsWeightsAndNullInputs() {
		for (String id : List.of("", "neutral/attack", "Neutral", "../attack", "a".repeat(49))) {
			assertThrows(IllegalArgumentException.class, () -> MasterMoveGraph.Node.neutral(id));
			assertThrows(IllegalArgumentException.class, () -> edge(id, "neutral", "sweep", 1));
		}
		for (int weight : new int[] {Integer.MIN_VALUE, -1, 0, 101, Integer.MAX_VALUE})
			assertThrows(IllegalArgumentException.class, () -> edge("a", "neutral", "sweep", weight));
		for (int school : new int[] {-1, 3, Integer.MIN_VALUE, Integer.MAX_VALUE})
			assertThrows(IllegalArgumentException.class, () -> new MasterMoveGraph(graphId(0), school, graph(0).nodes(), graph(0).edges()));
		for (String id : List.of("", "ember", "wildercord:master_graph/Ember", "wildercord:master_graph/" + "a".repeat(100)))
			assertThrows(IllegalArgumentException.class, () -> new MasterMoveGraph(id, 0, graph(0).nodes(), graph(0).edges()));
		assertThrows(NullPointerException.class, () -> MasterMoveGraph.Node.neutral(null));
		assertThrows(IllegalArgumentException.class, () -> action("unknown", move("unknown")));
		assertThrows(NullPointerException.class, () -> new MasterMoveGraph.Edge("a", "neutral", "sweep", 1, null));
		assertThrows(NullPointerException.class, () -> new MasterMoveGraph(graphId(0), 0, null, List.of()));
		assertThrows(NullPointerException.class, () -> new MasterMoveGraph(graphId(0), 0, graph(0).nodes(), null));
		assertThrows(NullPointerException.class, () -> new MasterMoveGraph(graphId(0), 0, Arrays.asList(MasterMoveGraph.Node.neutral("neutral"), null), List.of()));
		assertThrows(NullPointerException.class, () -> new MasterMoveGraph(graphId(0), 0, graph(0).nodes(), Arrays.asList((MasterMoveGraph.Edge) null)));
	}

	@Test
	void rejectsMissingDuplicateUnreachableOrSchoolIncompatibleNodes() {
		var neutral = MasterMoveGraph.Node.neutral("neutral");
		var sweep = action("sweep", SWEEP);
		for (var nodes : List.of(List.<MasterMoveGraph.Node>of(), List.of(sweep), List.of(neutral, neutral),
			List.of(neutral, MasterMoveGraph.Node.neutral("other")), List.of(neutral, sweep),
			List.of(neutral, action("foreign", move("crosswind_reprise")))))
			assertThrows(IllegalArgumentException.class, () -> new MasterMoveGraph(graphId(0), 0, nodes, List.of()));
		var empty = new MasterMoveGraph(graphId(0), 0, List.of(neutral), List.of());
		var planner = new MasterMovePlanner(empty);
		assertTrue(planner.propose(planner.initial(0), ALL).choice().isEmpty());
	}

	@Test
	void rejectsDuplicateRoutesUnknownEndpointsNeutralCyclesAndReactiveMidPhraseEdges() {
		var nodes = List.of(MasterMoveGraph.Node.neutral("neutral"), action("sweep", SWEEP), action("thrust", THRUST));
		var a = edge("a", "neutral", "sweep", 1);
		var b = edge("b", "neutral", "thrust", 1);
		for (var edges : List.of(List.of(a, b, a), List.of(a, b, edge("a", "sweep", "thrust", 1)),
			List.of(a, b, edge("c", "neutral", "sweep", 1)), List.of(a, b, edge("c", "unknown", "sweep", 1)),
			List.of(a, b, edge("c", "sweep", "unknown", 1)), List.of(a, b, edge("c", "sweep", "neutral", 1)),
			List.of(a, b, new MasterMoveGraph.Edge("c", "sweep", "thrust", 1, NEUTRAL_ONLY))))
			assertThrows(IllegalArgumentException.class, () -> new MasterMoveGraph(graphId(0), 0, nodes, edges));
		for (String reactive : List.of(move("break_cast"), move("pursuit_break"))) {
			var reactiveNodes = List.of(MasterMoveGraph.Node.neutral("neutral"), action("reactive", reactive));
			assertThrows(IllegalArgumentException.class, () -> new MasterMoveGraph(graphId(0), 0, reactiveNodes,
				List.of(edge("a", "neutral", "reactive", 1))));
			var valid = new MasterMoveGraph(graphId(0), 0, reactiveNodes,
				List.of(new MasterMoveGraph.Edge("a", "neutral", "reactive", 1, NEUTRAL_ONLY)));
			var planner = new MasterMovePlanner(valid);
			assertEquals(reactive, planner.propose(planner.initial(1), eligibility(reactive)).choice().orElseThrow().moveId());
		}
	}

	@Test
	void acceptsExactGraphBoundsAndRejectsOneOverWithoutUnboundedSelection() {
		var nodes = new ArrayList<MasterMoveGraph.Node>();
		nodes.add(MasterMoveGraph.Node.neutral("neutral"));
		for (int i = 0; i < 15; i++) nodes.add(action("node_" + i, SWEEP));
		var edges = new ArrayList<MasterMoveGraph.Edge>();
		for (int i = 0; i < 15; i++) edges.add(edge("entry_" + i, "neutral", "node_" + i, 100));
		for (int from = 0; edges.size() < 64; from++)
			for (int to = 0; to < 15 && edges.size() < 64; to++)
				edges.add(edge("loop_" + from + "_" + to, "node_" + from, "node_" + to, 100));
		var graph = new MasterMoveGraph(graphId(0), 0, nodes, edges);
		assertEquals(16, graph.nodes().size());
		assertEquals(64, graph.edges().size());
		var planner = new MasterMovePlanner(graph);
		assertEquals(SWEEP, planner.propose(planner.initial(Long.MAX_VALUE), eligibility(SWEEP)).choice().orElseThrow().moveId());
		nodes.add(action("too_many", SWEEP));
		assertThrows(IllegalArgumentException.class, () -> new MasterMoveGraph(graphId(0), 0, nodes, edges));
		nodes.removeLast();
		edges.add(edge("too_many", "node_14", "node_14", 1));
		assertThrows(IllegalArgumentException.class, () -> new MasterMoveGraph(graphId(0), 0, nodes, edges));
		assertEquals(8, MasterMoveCatalog.legacy().authoredAttackCount(), "Nodes, edges and combinations do not add attacks");
	}

	private static String replay(MasterMoveGraph graph, long seed, int proposals) {
		var planner = new MasterMovePlanner(graph);
		var state = planner.initial(seed);
		var events = new ArrayList<String>();
		for (int i = 0; i < proposals; i++) {
			var proposal = planner.propose(state, ALL);
			events.add(proposal.choice().map(choice -> choice.edge().id()).orElse("neutral"));
			state = planner.resolve(state, proposal, proposal.choice().isPresent() ? STARTED : NEUTRAL);
		}
		return String.join(",", events);
	}

	private static MasterMoveGraph graph(int school) {
		var nodes = List.of(MasterMoveGraph.Node.neutral("neutral"), action("sweep", SWEEP), action("thrust", THRUST), action("crescent", CRESCENT));
		var edges = new ArrayList<MasterMoveGraph.Edge>();
		for (String from : List.of("neutral", "sweep", "thrust", "crescent")) {
			edges.add(edge(from + "_sweep", from, "sweep", 9));
			edges.add(edge(from + "_thrust", from, "thrust", 7));
			edges.add(edge(from + "_crescent", from, "crescent", 5));
		}
		return new MasterMoveGraph(graphId(school), school, nodes, edges);
	}
	private static String graphId(int school) { return "wildercord:master_graph/test_" + school + "_v1"; }
	private static String move(String name) { return "wildercord:master/" + name; }
	private static MasterMoveGraph.Node action(String node, String move) { return MasterMoveGraph.Node.action(node, move); }
	private static MasterMoveGraph.Edge edge(String id, String from, String to, int weight) { return new MasterMoveGraph.Edge(id, from, to, weight, ELIGIBLE); }
	private static MasterMovePlanner.Eligibility eligibility(String... moves) { return new MasterMovePlanner.Eligibility(Set.of(moves), 100); }
}
