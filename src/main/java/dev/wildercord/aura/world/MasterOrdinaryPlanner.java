package dev.wildercord.aura.world;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Bounded encounter-local bridge to the pure planner. It only proposes ordinary attacks; the live executor owns
 * eligibility, payment and every warning/hit/recovery. Calling propose never changes accepted encounter state.
 */
public final class MasterOrdinaryPlanner {
	public static final String VERSION = "ordinary_v1";
	private static final List<MastersRules.Move> ORDINARY = List.of(MastersRules.Move.SWEEP, MastersRules.Move.THRUST, MastersRules.Move.CRESCENT);
	private static final MasterMovePlanner[][] PLANNERS = buildPlanners();
	private final int school;
	private MasterMovePlanner.State state;
	private long revision;
	private UUID target;
	private Set<UUID> roster = Set.of();

	/** Opaque, expiring proposal. A graph choice is never a permit to execute an attack. */
	public static final class Proposal {
		private final MasterOrdinaryPlanner owner;
		private final long revision;
		private final MasterMovePlanner planner;
		private final MasterMovePlanner.Proposal plan;
		private Proposal(MasterOrdinaryPlanner owner, MasterMovePlanner planner, MasterMovePlanner.Proposal plan) {
			this.owner = owner; this.revision = owner.revision; this.planner = planner; this.plan = plan;
		}
		public MastersRules.Move move() {
			return plan.choice().map(choice -> MasterMoveCatalog.legacy().byId(choice.moveId()).orElseThrow().legacyMove()).orElse(null);
		}
		public String graphId() { return graph(owner.school, plannerIndex(planner, owner.school)).id(); }
		public MasterMovePlanner.State before() { return plan.before(); }
	}

	public MasterOrdinaryPlanner(int school, long encounterSeed) {
		if (school < MastersRules.EMBER || school > MastersRules.STONE) throw new IllegalArgumentException("Unknown school");
		this.school = school;
		state = PLANNERS[school][0].initial(encounterSeed);
	}

	public MasterMovePlanner.State state() { return state; }
	public int school() { return school; }

	/** Native UUIDs supply one seed at roster lock, without consuming shared world RNG or consulting wall time. */
	public static long encounterSeed(UUID master, UUID challenger) {
		Objects.requireNonNull(master, "master"); Objects.requireNonNull(challenger, "challenger");
		return master.getMostSignificantBits() ^ Long.rotateLeft(master.getLeastSignificantBits(), 17)
			^ Long.rotateLeft(challenger.getMostSignificantBits(), 31) ^ challenger.getLeastSignificantBits();
	}

	/** A target/valid-roster change terminates the phrase and invalidates even an otherwise identical proposal. */
	public void observe(UUID target, Set<UUID> validRoster) {
		Objects.requireNonNull(validRoster, "validRoster");
		if (validRoster.size() > MastersRules.MAX_PARTICIPANTS) throw new IllegalArgumentException("Oversized roster");
		var copy = Set.copyOf(validRoster);
		if (!Objects.equals(this.target, target) || !roster.equals(copy)) {
			endPhrase(); this.target = target; roster = copy;
		}
	}

	public boolean contextMatches(UUID target, Set<UUID> validRoster) {
		return Objects.equals(this.target, target) && roster.equals(validRoster);
	}

	/** Preferred is the existing phase/sequence pattern result; it changes weights, never legal eligibility. */
	public Proposal propose(MastersRules.Move preferred, Set<MastersRules.Move> eligible, double aura) {
		int index = ORDINARY.indexOf(preferred);
		if (index < 0 || !ORDINARY.containsAll(eligible)) throw new IllegalArgumentException("Not an ordinary slot");
		var planner = PLANNERS[school][index];
		var ids = eligible.stream().map(move -> MasterMoveCatalog.legacy().forMove(move).id()).collect(Collectors.toUnmodifiableSet());
		return new Proposal(this, planner, planner.propose(state, new MasterMovePlanner.Eligibility(ids, aura)));
	}

	public boolean current(Proposal proposal) {
		return proposal != null && proposal.owner == this && proposal.revision == revision && proposal.plan.before().equals(state);
	}

	/** Call only at the executor's successful payment/start boundary, after a second live eligibility check. */
	public boolean admitted(Proposal proposal) {
		if (!current(proposal) || proposal.move() == null) return false;
		state = proposal.planner.resolve(state, proposal.plan, MasterMovePlanner.Outcome.STARTED);
		revision++;
		return true;
	}

	/** Neutral is one outcome, never a recursive redraw. The executor may retain its existing lawful fallback. */
	public boolean neutral(Proposal proposal) {
		if (!current(proposal) || proposal.move() != null) return false;
		state = proposal.planner.resolve(state, proposal.plan, MasterMovePlanner.Outcome.NEUTRAL);
		revision++;
		return true;
	}

	/** Higher-priority and fallback starts also enter authored-ID history, but cannot extend an ordinary phrase. */
	public void admittedExternal(MastersRules.Move move) {
		var definition = MasterMoveCatalog.legacy().forMove(move);
		if (!definition.availableIn(school) || state.successfulDecisions() == Long.MAX_VALUE)
			throw new IllegalArgumentException("Unavailable move or exhausted ordinal");
		var history = new ArrayList<>(state.history()); history.add(definition.id());
		if (history.size() > MasterMovePlanner.HISTORY_LIMIT) history.removeFirst();
		state = new MasterMovePlanner.State(state.encounterSeed(), state.successfulDecisions() + 1, "neutral", 0, history);
		revision++;
	}

	public void endPhrase() {
		state = PLANNERS[school][0].endPhrase(state);
		revision++;
	}

	/** Conservative spatial candidates only. Live lifecycle, priority, Aura and native line-of-sight checks remain outside. */
	public static Set<MastersRules.Move> spatialCandidates(double distance, double height) {
		if (!Double.isFinite(distance) || distance < 0 || !Double.isFinite(height)
			|| MastersRules.needsCrescent(distance, height)) return Set.of();
		var moves = EnumSet.of(MastersRules.Move.THRUST, MastersRules.Move.CRESCENT);
		if (distance <= 4) moves.add(MastersRules.Move.SWEEP);
		return Set.copyOf(moves);
	}

	public static MasterMoveGraph graph(int school, int preferredIndex) {
		if (school < 0 || school > 2 || preferredIndex < 0 || preferredIndex >= ORDINARY.size())
			throw new IllegalArgumentException("Invalid graph profile");
		var nodes = new ArrayList<MasterMoveGraph.Node>(); nodes.add(MasterMoveGraph.Node.neutral("neutral"));
		for (var move : ORDINARY) nodes.add(MasterMoveGraph.Node.action(key(move), MasterMoveCatalog.legacy().forMove(move).id()));
		var edges = new ArrayList<MasterMoveGraph.Edge>();
		for (var from : nodes) {
			if (school == MastersRules.STONE && from.moveId().isPresent()) continue;
			for (int i = 0; i < ORDINARY.size(); i++) {
				String to = key(ORDINARY.get(i));
				edges.add(new MasterMoveGraph.Edge(from.id() + "_" + to, from.id(), to,
					i == preferredIndex ? 9 : 3, MasterMoveGraph.Condition.ELIGIBLE));
			}
		}
		return new MasterMoveGraph("wildercord:master_graph/ordinary_" + school + "_" + key(ORDINARY.get(preferredIndex)) + "_v1", school, nodes, edges);
	}

	private static String key(MastersRules.Move move) { return move.name().toLowerCase(java.util.Locale.ROOT); }
	private static MasterMovePlanner[][] buildPlanners() {
		var result = new MasterMovePlanner[3][3];
		for (int school = 0; school < 3; school++) for (int preferred = 0; preferred < 3; preferred++)
			result[school][preferred] = new MasterMovePlanner(graph(school, preferred));
		return result;
	}
	private static int plannerIndex(MasterMovePlanner planner, int school) {
		for (int i = 0; i < 3; i++) if (PLANNERS[school][i] == planner) return i;
		throw new IllegalArgumentException("Unknown planner");
	}
}
