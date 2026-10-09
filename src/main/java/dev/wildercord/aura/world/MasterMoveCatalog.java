package dev.wildercord.aura.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Authored inventory used by the bounded ordinary planner and compatibility lookups, including isolated candidates.
 * Catalog membership never grants live admission, damage, native acceptance or a new rendering path.
 * School references, parameter variants and combinations do not create additional authored attack IDs.
 */
public final class MasterMoveCatalog {
	public static final int MAX_DEFINITIONS = 256;
	public static final int MAX_WIRE_ID = 32767;

	/** Closed references to existing code, never class names, commands or data-driven effect programs. */
	public enum ExecutionFamily { MELEE, CRESCENT, CINDER_WAKE, PURSUIT, REPRISE, FRACTURE, KILN, STONE_MARCH, TECHNIQUE,
		// ---- masters-a pack
		SIGNATURE,
		// ---- masters-b pack
		PACK_B_SIGNATURE,
		// ---- methods-a pack
		METHODS_A_SIGNATURE }

	/** Timing, payment, warnings, counterplay and presentation remain owned by the existing implementation. */
	public record Definition(String id, int wireId, MastersRules.Move legacyMove, Set<Integer> schools,
		ExecutionFamily executionFamily) {
		public Definition {
			Objects.requireNonNull(id, "id");
			Objects.requireNonNull(legacyMove, "legacyMove");
			Objects.requireNonNull(executionFamily, "executionFamily");
			if (id.length() > 96 || !id.matches("wildercord:master/[a-z][a-z0-9_]*"))
				throw new IllegalArgumentException("Invalid Master move ID: " + id);
			if (wireId < 1 || wireId > MAX_WIRE_ID) throw new IllegalArgumentException("Invalid Master wire ID: " + wireId);
			schools = Set.copyOf(schools);
			if (schools.isEmpty() || schools.stream().anyMatch(school -> !knownSchool(school)))
				throw new IllegalArgumentException("A Master move needs known schools");
		}

		/** Availability is descriptive only; it does not grant admission or broaden the lawful target roster. */
		public boolean availableIn(int school) { return schools.contains(school); }
	}

	private static final Set<Integer> ALL_SCHOOLS = Set.of(MastersRules.EMBER, MastersRules.GALE, MastersRules.STONE,
		// ---- masters-a pack
		MastersRules.RIME, MastersRules.THUNDER, MastersRules.VERDANT, MastersRules.HOLLOW,
		MastersPackB.STARLIT, MastersPackB.HOURGLASS, MastersPackB.CRIMSON, // ---- masters-b pack: the shared moves are theirs too
		MethodsAMasters.TIDE, MethodsAMasters.IRON, MethodsAMasters.DUNE, // ---- methods-a pack: the shared moves are theirs too
		MethodsBMasters.ECHO, MethodsBMasters.DAWN, MethodsBMasters.VENOM); // ---- methods-b pack
	private static final MasterMoveCatalog LEGACY = new MasterMoveCatalog(List.of(
		new Definition("wildercord:master/sweep", 1, MastersRules.Move.SWEEP, ALL_SCHOOLS, ExecutionFamily.MELEE),
		new Definition("wildercord:master/thrust", 2, MastersRules.Move.THRUST, ALL_SCHOOLS, ExecutionFamily.MELEE),
		new Definition("wildercord:master/crescent", 3, MastersRules.Move.CRESCENT, ALL_SCHOOLS, ExecutionFamily.CRESCENT),
		new Definition("wildercord:master/break_cast", 4, MastersRules.Move.BREAK_CAST, ALL_SCHOOLS, ExecutionFamily.MELEE),
		new Definition("wildercord:master/cinder_wake", 5, MastersRules.Move.CINDER_WAKE, Set.of(MastersRules.EMBER), ExecutionFamily.CINDER_WAKE),
		new Definition("wildercord:master/pursuit_break", 6, MastersRules.Move.PURSUIT_BREAK, ALL_SCHOOLS, ExecutionFamily.PURSUIT),
		new Definition("wildercord:master/crosswind_reprise", 7, MastersRules.Move.CROSSWIND_REPRISE, Set.of(MastersRules.GALE), ExecutionFamily.REPRISE),
		new Definition("wildercord:master/stone_fracture", 8, MastersRules.Move.STONE_FRACTURE, Set.of(MastersRules.STONE), ExecutionFamily.FRACTURE),
		new Definition("wildercord:master/kiln_ring", 9, MastersRules.Move.KILN_RING, Set.of(MastersRules.EMBER), ExecutionFamily.KILN),
		new Definition("wildercord:master/stone_fault_march", 10, MastersRules.Move.STONE_FAULT_MARCH, Set.of(MastersRules.STONE), ExecutionFamily.STONE_MARCH),
		new Definition("wildercord:master/technique", 11, MastersRules.Move.TECHNIQUE, ALL_SCHOOLS, ExecutionFamily.TECHNIQUE),
		// ---- masters-a pack
		new Definition("wildercord:master/rime_lattice", 12, MastersRules.Move.RIME_LATTICE, Set.of(MastersRules.RIME), ExecutionFamily.SIGNATURE),
		new Definition("wildercord:master/thunder_chain", 13, MastersRules.Move.THUNDER_CHAIN, Set.of(MastersRules.THUNDER), ExecutionFamily.SIGNATURE),
		new Definition("wildercord:master/verdant_bloom", 14, MastersRules.Move.VERDANT_BLOOM, Set.of(MastersRules.VERDANT), ExecutionFamily.SIGNATURE),
		new Definition("wildercord:master/hollow_pull", 15, MastersRules.Move.HOLLOW_PULL, Set.of(MastersRules.HOLLOW), ExecutionFamily.SIGNATURE),
		// ---- masters-b pack
		new Definition("wildercord:master/starlit_constellation", 16, MastersRules.Move.STARLIT_CONSTELLATION, Set.of(MastersPackB.STARLIT), ExecutionFamily.PACK_B_SIGNATURE),
		new Definition("wildercord:master/hourglass_rewind", 17, MastersRules.Move.HOURGLASS_REWIND, Set.of(MastersPackB.HOURGLASS), ExecutionFamily.PACK_B_SIGNATURE),
		new Definition("wildercord:master/crimson_frenzy", 18, MastersRules.Move.CRIMSON_FRENZY, Set.of(MastersPackB.CRIMSON), ExecutionFamily.PACK_B_SIGNATURE),
		// ---- methods-a pack: wire ids 19-21 (ordinal + 1, like every move before it).
		new Definition("wildercord:master/tide_undertow_ring", 19, MastersRules.Move.TIDE_UNDERTOW_RING,
			Set.of(MethodsAMasters.TIDE), ExecutionFamily.METHODS_A_SIGNATURE),
		new Definition("wildercord:master/iron_anvil_verdict", 20, MastersRules.Move.IRON_ANVIL_VERDICT,
			Set.of(MethodsAMasters.IRON), ExecutionFamily.METHODS_A_SIGNATURE),
		new Definition("wildercord:master/dune_shifting_sands", 21, MastersRules.Move.DUNE_SHIFTING_SANDS,
			Set.of(MethodsAMasters.DUNE), ExecutionFamily.METHODS_A_SIGNATURE)));

	private final List<Definition> definitions;
	private final Map<String, Definition> byId;
	private final Map<Integer, Definition> byWireId;
	private final Map<MastersRules.Move, Definition> byMove;

	/** Package-local construction supports validation without exposing runtime registration. */
	MasterMoveCatalog(List<Definition> source) {
		Objects.requireNonNull(source, "source");
		if (source.isEmpty() || source.size() > MAX_DEFINITIONS) throw new IllegalArgumentException("Invalid Master catalog size");
		var ordered = new ArrayList<>(List.copyOf(source));
		ordered.sort(Comparator.comparingInt(Definition::wireId).thenComparing(Definition::id));
		Map<String, Definition> ids = new HashMap<>();
		Map<Integer, Definition> wireIds = new HashMap<>();
		Map<MastersRules.Move, Definition> moves = new EnumMap<>(MastersRules.Move.class);
		for (Definition definition : ordered) {
			if (ids.putIfAbsent(definition.id(), definition) != null
				|| wireIds.putIfAbsent(definition.wireId(), definition) != null
				|| moves.putIfAbsent(definition.legacyMove(), definition) != null)
				throw new IllegalArgumentException("Duplicate Master move identity: " + definition.id());
		}
		definitions = List.copyOf(ordered);
		byId = Map.copyOf(ids);
		byWireId = Map.copyOf(wireIds);
		byMove = Map.copyOf(moves);
	}

	public static MasterMoveCatalog legacy() { return LEGACY; }
	public List<Definition> definitions() { return definitions; }
	/** Structural ID count, not a native-certification or delivery claim. */
	public int authoredAttackCount() { return definitions.size(); }
	public Optional<Definition> byId(String id) { return Optional.ofNullable(byId.get(Objects.requireNonNull(id, "id"))); }

	/** Includes idle (0), invalid and unsupported IDs; none is silently mapped to a damaging action. */
	public Optional<Definition> byWireId(int wireId) { return Optional.ofNullable(byWireId.get(wireId)); }

	public Definition forMove(MastersRules.Move move) {
		Definition definition = byMove.get(Objects.requireNonNull(move, "move"));
		if (definition == null) throw new IllegalArgumentException("Uncatalogued Master move: " + move);
		return definition;
	}

	/** Canonical order is wire ID, independent of declaration order or set/map iteration order. */
	public List<Definition> forSchool(int school) {
		return knownSchool(school) ? definitions.stream().filter(definition -> definition.availableIn(school)).toList() : List.of();
	}

	private static boolean knownSchool(int school) { return MastersRules.knownSchool(school); } // ---- masters-a pack
}
