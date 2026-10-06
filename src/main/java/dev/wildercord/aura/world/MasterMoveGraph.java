package dev.wildercord.aura.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Bounded, immutable planning data. No live Master reads this graph. Nodes do not add authored attacks. */
public final class MasterMoveGraph {
	public static final int MAX_NODES = 16, MAX_EDGES = 64, MAX_WEIGHT = 100;

	/** Eligibility is always required. Reactive openings may additionally require a neutral phrase. */
	public enum Condition { ELIGIBLE, NEUTRAL_ONLY }

	public record Node(String id, Optional<String> moveId) {
		public Node {
			checkKey(id);
			Objects.requireNonNull(moveId, "moveId");
			moveId.ifPresent(move -> {
				if (MasterMoveCatalog.legacy().byId(move).isEmpty()) throw new IllegalArgumentException("Unknown move: " + move);
			});
		}
		public static Node neutral(String id) { return new Node(id, Optional.empty()); }
		public static Node action(String id, String moveId) { return new Node(id, Optional.of(moveId)); }
	}

	/** Edges enter actions. Returning to neutral is an explicit planner outcome, never a depth-resetting edge. */
	public record Edge(String id, String from, String to, int weight, Condition condition) {
		public Edge {
			checkKey(id);
			checkKey(from);
			checkKey(to);
			Objects.requireNonNull(condition, "condition");
			if (weight < 1 || weight > MAX_WEIGHT) throw new IllegalArgumentException("Invalid edge weight: " + weight);
		}
	}

	private final String id;
	private final int school;
	private final List<Node> nodes;
	private final List<Edge> edges;
	private final Map<String, Node> byId;
	private final Node neutral;

	/** The version belongs in the stable graph ID (for example, wildercord:master_graph/ember_v1). */
	public MasterMoveGraph(String id, int school, List<Node> nodes, List<Edge> edges) {
		Objects.requireNonNull(id, "id");
		if (id.length() > 96 || !id.matches("wildercord:master_graph/[a-z][a-z0-9_]*"))
			throw new IllegalArgumentException("Invalid graph ID: " + id);
		if (school < MastersRules.EMBER || school > MastersRules.STONE) throw new IllegalArgumentException("Unknown school");
		Objects.requireNonNull(nodes, "nodes");
		Objects.requireNonNull(edges, "edges");
		if (nodes.isEmpty() || nodes.size() > MAX_NODES || edges.size() > MAX_EDGES)
			throw new IllegalArgumentException("Graph exceeds node/edge bounds");
		var orderedNodes = new ArrayList<>(List.copyOf(nodes));
		var orderedEdges = new ArrayList<>(List.copyOf(edges));
		orderedNodes.sort(Comparator.comparing(Node::id));
		orderedEdges.sort(Comparator.comparing(Edge::id));
		Map<String, Node> ids = new HashMap<>();
		Node root = null;
		for (Node node : orderedNodes) {
			if (ids.putIfAbsent(node.id(), node) != null) throw new IllegalArgumentException("Duplicate node: " + node.id());
			if (node.moveId().isEmpty()) {
				if (root != null) throw new IllegalArgumentException("Multiple neutral nodes");
				root = node;
			} else if (!MasterMoveCatalog.legacy().byId(node.moveId().orElseThrow()).orElseThrow().availableIn(school)) {
				throw new IllegalArgumentException("Move is unavailable in graph school");
			}
		}
		if (root == null) throw new IllegalArgumentException("Missing neutral node");
		var edgeIds = new HashSet<String>();
		var routes = new HashSet<List<String>>();
		for (Edge edge : orderedEdges) {
			Node from = ids.get(edge.from()), to = ids.get(edge.to());
			if (!edgeIds.add(edge.id()) || !routes.add(List.of(edge.from(), edge.to())))
				throw new IllegalArgumentException("Duplicate edge identity or route: " + edge.id());
			if (from == null || to == null) throw new IllegalArgumentException("Unknown edge endpoint");
			if (to.moveId().isEmpty()) throw new IllegalArgumentException("Neutral is an outcome, not an edge target");
			if (edge.condition() == Condition.NEUTRAL_ONLY && from != root)
				throw new IllegalArgumentException("Neutral-only edge leaves an action");
			var move = MasterMoveCatalog.legacy().byId(to.moveId().orElseThrow()).orElseThrow().legacyMove();
			if ((move == MastersRules.Move.PURSUIT_BREAK || move == MastersRules.Move.BREAK_CAST)
				&& edge.condition() != Condition.NEUTRAL_ONLY)
				throw new IllegalArgumentException("Reactive opening requires a neutral-only edge");
		}
		// At most MAX_NODES passes; disconnected authoring mistakes cannot silently become dormant content.
		var reached = new HashSet<String>();
		reached.add(root.id());
		for (int pass = 0; pass < orderedNodes.size(); pass++)
			for (Edge edge : orderedEdges) if (reached.contains(edge.from())) reached.add(edge.to());
		if (reached.size() != orderedNodes.size()) throw new IllegalArgumentException("Unreachable graph node");
		this.id = id;
		this.school = school;
		this.nodes = List.copyOf(orderedNodes);
		this.edges = List.copyOf(orderedEdges);
		byId = Map.copyOf(ids);
		neutral = root;
	}

	public String id() { return id; }
	public int school() { return school; }
	public int maxDepth() { return school == MastersRules.STONE ? 1 : 2; }
	public List<Node> nodes() { return nodes; }
	/** ASCII edge-ID order is the replay contract, independent of insertion or map order. */
	public List<Edge> edges() { return edges; }
	public Node neutral() { return neutral; }
	public Optional<Node> node(String id) { return Optional.ofNullable(byId.get(Objects.requireNonNull(id, "id"))); }

	private static void checkKey(String key) {
		Objects.requireNonNull(key, "key");
		if (key.length() > 48 || !key.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("Invalid graph key: " + key);
	}
}
