package dev.wildercord.aura.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Pure, unconnected proposal engine. It neither admits attacks nor owns clocks, cooldowns, recovery, Aura or damage.
 * The future executor must recheck every live precondition and only commit a proposal after one successful start.
 */
public final class MasterMovePlanner {
	public static final int HISTORY_LIMIT = 4;

	/** Oldest to newest authored IDs, including repeats. The ordinal counts successful starts, not completed hits. */
	public record State(long encounterSeed, long successfulDecisions, String nodeId, int depth, List<String> history) {
		public State {
			Objects.requireNonNull(nodeId, "nodeId");
			Objects.requireNonNull(history, "history");
			if (successfulDecisions < 0 || depth < 0 || depth > 2 || depth > successfulDecisions
				|| history.size() != Math.min(successfulDecisions, HISTORY_LIMIT))
				throw new IllegalArgumentException("Invalid decision, depth or history bounds");
			history = List.copyOf(history);
			for (String move : history) if (MasterMoveCatalog.legacy().byId(move).isEmpty())
				throw new IllegalArgumentException("Unknown history move: " + move);
		}
	}

	/**
	 * A frozen set of caller-approved candidates, after cooldown/roster/terrain/priority checks. Aura is an additional
	 * conservative filter using legacy costs, not a reservation or debit. The planner never adds a missing candidate.
	 */
	public record Eligibility(Set<String> moveIds, double availableAura) {
		public Eligibility {
			Objects.requireNonNull(moveIds, "moveIds");
			if (moveIds.size() > MasterMoveCatalog.legacy().authoredAttackCount())
				throw new IllegalArgumentException("Eligibility exceeds catalog size");
			if (!Double.isFinite(availableAura) || availableAura < 0 || availableAura > MastersRules.AURA_MAX)
				throw new IllegalArgumentException("Invalid Aura snapshot");
			moveIds = Set.copyOf(moveIds);
			for (String move : moveIds) if (MasterMoveCatalog.legacy().byId(move).isEmpty())
				throw new IllegalArgumentException("Unknown eligible move: " + move);
		}
	}

	public enum Outcome { DECLINED, STARTED, STARTED_INTERRUPTED, NEUTRAL }
	public record Choice(MasterMoveGraph.Edge edge, String moveId, int effectiveWeight) {}

	/** Constructor is private: only this planner can issue proposals against its immutable graph. */
	public static final class Proposal {
		private final MasterMoveGraph graph;
		private final State before, nextState;
		private final Optional<Choice> choice;
		private Proposal(MasterMoveGraph graph, State before, State nextState, Optional<Choice> choice) {
			this.graph = graph;
			this.before = before;
			this.nextState = nextState;
			this.choice = choice;
		}
		public State before() { return before; }
		/** Tentative state only; use resolve with the current state after runtime admission. */
		public State nextState() { return nextState; }
		public Optional<Choice> choice() { return choice; }
	}

	private final MasterMoveGraph graph;
	public MasterMovePlanner(MasterMoveGraph graph) { this.graph = Objects.requireNonNull(graph, "graph"); }
	public State initial(long encounterSeed) { return new State(encounterSeed, 0, graph.neutral().id(), 0, List.of()); }

	/** One pass over at most 64 edges, one bounded draw. No RNG state, redraw loop, search or future observations. */
	public Proposal propose(State state, Eligibility eligibility) {
		validate(state);
		Objects.requireNonNull(eligibility, "eligibility");
		for (String id : eligibility.moveIds()) {
			if (!MasterMoveCatalog.legacy().byId(id).orElseThrow().availableIn(graph.school()))
				throw new IllegalArgumentException("Eligible move is unavailable in graph school");
		}
		if (state.depth() == graph.maxDepth()) return neutral(state);
		var eligible = new ArrayList<Choice>();
		for (var edge : graph.edges()) {
			if (!edge.from().equals(state.nodeId())
				|| edge.condition() == MasterMoveGraph.Condition.NEUTRAL_ONLY && state.depth() != 0) continue;
			String id = graph.node(edge.to()).orElseThrow().moveId().orElseThrow();
			var definition = MasterMoveCatalog.legacy().byId(id).orElseThrow();
			if (eligibility.moveIds().contains(id)
				&& LegacyMasterMoves.auraCost(definition.legacyMove(), graph.school()) <= eligibility.availableAura())
				eligible.add(new Choice(edge, id, edge.weight()));
		}
		if (eligible.isEmpty()) return neutral(state);
		String last = state.history().isEmpty() ? null : state.history().getLast();
		if (eligible.stream().anyMatch(choice -> !choice.moveId().equals(last)))
			eligible.removeIf(choice -> choice.moveId().equals(last));
		var weighted = eligible.stream().map(choice -> new Choice(choice.edge(), choice.moveId(),
			state.history().contains(choice.moveId()) ? Math.max(1, choice.effectiveWeight() / 2) : choice.effectiveWeight())).toList();
		int total = weighted.stream().mapToInt(Choice::effectiveWeight).sum();
		long hash = mix64(state.encounterSeed() ^ 0x9e3779b97f4a7c15L * (graph.school() + 1L)
			^ 0xd1b54a32d192ed03L * state.successfulDecisions());
		// ASCII IDs plus a NUL separator (forbidden in IDs); overflow is specified Java long arithmetic.
		for (Choice choice : weighted) {
			for (int i = 0; i < choice.edge().id().length(); i++) hash = (hash ^ choice.edge().id().charAt(i)) * 0x100000001b3L;
			hash *= 0x100000001b3L;
		}
		int ticket = (int) Long.remainderUnsigned(mix64(hash), total);
		for (Choice choice : weighted) {
			if (ticket < choice.effectiveWeight()) {
				if (state.successfulDecisions() == Long.MAX_VALUE) throw new IllegalStateException("Decision ordinal exhausted");
				var history = new ArrayList<>(state.history());
				history.add(choice.moveId());
				if (history.size() > HISTORY_LIMIT) history.removeFirst();
				State next = new State(state.encounterSeed(), state.successfulDecisions() + 1, choice.edge().to(), state.depth() + 1, history);
				return new Proposal(graph, state, next, Optional.of(choice));
			}
			ticket -= choice.effectiveWeight();
		}
		throw new AssertionError("Bounded weighted choice did not resolve");
	}

	/** Stale proposals fail closed. A decline returns the exact original state, including seed, ordinal and history. */
	public State resolve(State current, Proposal proposal, Outcome outcome) {
		validate(current);
		Objects.requireNonNull(proposal, "proposal");
		Objects.requireNonNull(outcome, "outcome");
		if (proposal.graph != graph || !proposal.before.equals(current)) throw new IllegalArgumentException("Stale or foreign proposal");
		if (outcome == Outcome.DECLINED) return current;
		if (outcome == Outcome.NEUTRAL) {
			if (proposal.choice.isPresent()) throw new IllegalArgumentException("An attack proposal cannot be accepted as neutral");
			return proposal.nextState;
		}
		if (proposal.choice.isEmpty()) throw new IllegalArgumentException("A neutral proposal cannot start an attack");
		return outcome == Outcome.STARTED_INTERRUPTED ? endPhrase(proposal.nextState) : proposal.nextState;
	}

	/** Guard, breathing, obstruction, interruption, roster or target changes end a phrase, never erase accepted history. */
	public State endPhrase(State state) {
		validate(state);
		return state.depth() == 0 ? state : new State(state.encounterSeed(), state.successfulDecisions(), graph.neutral().id(), 0, state.history());
	}

	private Proposal neutral(State state) { return new Proposal(graph, state, endPhrase(state), Optional.empty()); }

	private void validate(State state) {
		Objects.requireNonNull(state, "state");
		var node = graph.node(state.nodeId()).orElseThrow(() -> new IllegalArgumentException("Unknown state node"));
		if (state.depth() > graph.maxDepth() || (state.depth() == 0) != node.moveId().isEmpty()
			|| state.depth() > 0 && !node.moveId().orElseThrow().equals(state.history().getLast()))
			throw new IllegalArgumentException("State does not match graph depth or current move");
		for (String id : state.history()) if (!MasterMoveCatalog.legacy().byId(id).orElseThrow().availableIn(graph.school()))
			throw new IllegalArgumentException("History move is unavailable in graph school");
	}

	/** SplitMix64 finalizer, kept explicit so replay does not depend on a JDK random-generator implementation. */
	private static long mix64(long value) {
		value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
		value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
		return value ^ (value >>> 31);
	}
}
