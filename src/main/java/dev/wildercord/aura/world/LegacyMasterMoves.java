package dev.wildercord.aura.world;

import java.util.Objects;

/**
 * Read-only compatibility adapter. Existing server and client paths remain unchanged until independent integration review.
 * This class describes legacy values; neither catalog availability nor a cost lookup authorizes starting an attack.
 */
public final class LegacyMasterMoves {
	private LegacyMasterMoves() {}

	public static int wireId(MastersRules.Move move) { return MasterMoveCatalog.legacy().forMove(move).wireId(); }
	public static int tellTicks(MastersRules.Move move) { return Objects.requireNonNull(move, "move").tell; }
	public static int recoveryTicks(MastersRules.Move move) { return Objects.requireNonNull(move, "move").recovery; }

	/** The current protocol draws the hit in the first recovery tick, not as an extra server tick. */
	public static int activeTicks(int wireId) { return MasterMoveCatalog.legacy().byWireId(wireId).isPresent() ? 1 : 0; }
	public static int tellTicks(int wireId) {
		return MasterMoveCatalog.legacy().byWireId(wireId).map(definition -> tellTicks(definition.legacyMove())).orElse(0);
	}
	public static int visualRecoveryTicks(int wireId) {
		return MasterMoveCatalog.legacy().byWireId(wireId).map(definition -> recoveryTicks(definition.legacyMove()) - 1).orElse(0);
	}

	/** Mirrors SwordMaster.beginAttack's existing debit without copying tuning numbers. */
	public static double auraCost(MastersRules.Move move, int discipline) {
		return switch (Objects.requireNonNull(move, "move")) {
			case CINDER_WAKE -> EmberWakeRules.COST;
			case PURSUIT_BREAK -> MasterPursuitRules.school(discipline).cost();
			case CROSSWIND_REPRISE -> GaleRepriseRules.COST;
			case STONE_FRACTURE -> StoneFractureRules.COST;
			case KILN_RING -> EmberKilnRules.COST;
			case STONE_FAULT_MARCH -> StoneMarchRules.COST;
			case SWEEP, THRUST, CRESCENT, BREAK_CAST, TECHNIQUE -> MastersRules.ATTACK_COST;
			// ---- masters-a pack
			case RIME_LATTICE, THUNDER_CHAIN, VERDANT_BLOOM, HOLLOW_PULL -> ElementalMasters.cost(move);
			case STARLIT_CONSTELLATION, HOURGLASS_REWIND, CRIMSON_FRENZY -> MastersPackB.cost(move); // ---- masters-b pack
			case TIDE_UNDERTOW_RING, IRON_ANVIL_VERDICT, DUNE_SHIFTING_SANDS -> MethodsAMasters.cost(move); // ---- methods-a pack
		};
	}

	/** Legacy base strike damage only. A Cinder Wake's later afterburn remains owned by EmberAfterburn. */
	public static double damage(MastersRules.Move move, int partySize, int discipline) {
		return MastersRules.damage(partySize, discipline, Objects.requireNonNull(move, "move"));
	}
}
