package dev.wildercord.aura.world;

import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.aura.TechniqueRules;

import java.util.List;

/**
 * The Tide, Iron and Dune Masters (the methods-a pack): their school ids, ordinary patterns, named techniques, signature
 * forms and first-clear lessons. Shared Master code calls in through short marked hooks; everything school-specific lives here.
 * Their tempo is the retuned one: every strike tells for 12 to 14 ticks, every chain recovers for 14 to 16.
 */
public final class MethodsAMasters {
	private MethodsAMasters() {}

	/** Ten to twelve, clear of the original three and of the other packs' schools. */
	public static final int TIDE = 10, IRON = 11, DUNE = 12;
	public static final List<Integer> SCHOOLS = List.of(TIDE, IRON, DUNE);
	public static final int MIN_TELL = 12, MAX_TELL = 14, MIN_RECOVERY = 14, MAX_RECOVERY = 16;
	/** Each school keeps at least this many named techniques. */
	public static final int MIN_TECHNIQUES = 20;

	/** No signature deals more to one challenger than this, however many of its beats they stand in. */
	public static final double CHAIN_CAP = MastersRules.TECHNIQUE_DAMAGE * MasterTechniques.MAX_SHARE;

	/** What one more beat may still deal to a challenger already dealt {@code dealt} by this form. */
	public static double capped(double dealt, double damage) {
		if (!Double.isFinite(dealt) || !Double.isFinite(damage) || damage <= 0) return 0;
		return Math.max(0, Math.min(damage, CHAIN_CAP - Math.max(0, dealt)));
	}

	public static int cooldown(MastersRules.Move move) { return signature(move) ? MethodsASignatureRules.COOLDOWN : 0; }

	public static boolean owns(int school) { return school == TIDE || school == IRON || school == DUNE; }

	public static String id(int school) {
		return switch (school) {
			case TIDE -> "tide";
			case IRON -> "iron";
			case DUNE -> "dune";
			default -> throw new IllegalArgumentException("Not a methods-a school: " + school);
		};
	}

	/** -1 for any method these Masters do not teach. */
	public static int school(BreathingMethod method) {
		if (method == null) return -1;
		if (method.equals(BreathingMethods.TIDE)) return TIDE;
		if (method.equals(BreathingMethods.IRON)) return IRON;
		if (method.equals(BreathingMethods.DUNE)) return DUNE;
		return -1;
	}

	public static BreathingMethod method(int school) {
		return switch (school) {
			case TIDE -> BreathingMethods.TIDE;
			case IRON -> BreathingMethods.IRON;
			case DUNE -> BreathingMethods.DUNE;
			default -> throw new IllegalArgumentException("Not a methods-a school: " + school);
		};
	}

	/** The ordinary rotation each school falls back to between its named techniques. */
	public static MastersRules.Move[] pattern(int school) {
		return switch (school) {
			// Tide rolls in and out: wide cuts, then a reaching volley.
			case TIDE -> new MastersRules.Move[] {MastersRules.Move.SWEEP, MastersRules.Move.SWEEP, MastersRules.Move.CRESCENT, MastersRules.Move.THRUST};
			// Iron presses straight in.
			case IRON -> new MastersRules.Move[] {MastersRules.Move.THRUST, MastersRules.Move.THRUST, MastersRules.Move.SWEEP, MastersRules.Move.THRUST};
			default -> new MastersRules.Move[] {MastersRules.Move.CRESCENT, MastersRules.Move.SWEEP, MastersRules.Move.THRUST, MastersRules.Move.SWEEP};
		};
	}

	/** One signature per school, appended after the shared moves so every older wire id keeps its meaning. */
	public static MastersRules.Move signature(int school) {
		return switch (school) {
			case TIDE -> MastersRules.Move.TIDE_UNDERTOW_RING;
			case IRON -> MastersRules.Move.IRON_ANVIL_VERDICT;
			case DUNE -> MastersRules.Move.DUNE_SHIFTING_SANDS;
			default -> null;
		};
	}

	public static boolean signature(MastersRules.Move move) {
		return move == MastersRules.Move.TIDE_UNDERTOW_RING || move == MastersRules.Move.IRON_ANVIL_VERDICT
			|| move == MastersRules.Move.DUNE_SHIFTING_SANDS;
	}

	public static int school(MastersRules.Move move) {
		return move == MastersRules.Move.TIDE_UNDERTOW_RING ? TIDE : move == MastersRules.Move.IRON_ANVIL_VERDICT ? IRON
			: move == MastersRules.Move.DUNE_SHIFTING_SANDS ? DUNE : -1;
	}

	public static double cost(MastersRules.Move move) {
		return signature(move) ? MethodsASignatureRules.COST : MastersRules.ATTACK_COST;
	}

	/** The plain answer shown under the signature's name when it opens. */
	public static String hint(MastersRules.Move move) {
		return move == MastersRules.Move.TIDE_UNDERTOW_RING ? "message.wildercord.master.undertow_hint"
			: move == MastersRules.Move.IRON_ANVIL_VERDICT ? "message.wildercord.master.anvil_hint"
			: move == MastersRules.Move.DUNE_SHIFTING_SANDS ? "message.wildercord.master.sands_hint" : null;
	}

	/** First-clear lesson: an existing horizontal technique part, like the original three. */
	public static String reward(int school) {
		return switch (school) {
			case TIDE -> TechniqueRules.WAVE;
			case IRON -> TechniqueRules.PIERCE;
			case DUNE -> TechniqueRules.BIND; // never WARD: that part is Way-only
			default -> "";
		};
	}

	/** Every strike of these schools shows its answer for 12 to 14 ticks; chained strikes keep a longer floor than a fresh combo. */
	public static int tell(int index, int authored) {
		return Math.max(MIN_TELL, Math.min(MAX_TELL, authored + (index == 0 ? 0 : 4)));
	}

	public static int recovery(int authored) {
		return Math.max(MIN_RECOVERY, Math.min(MAX_RECOVERY, authored));
	}

	@FunctionalInterface
	interface SchoolTable { void add(int school, String table); }

	/** Called once, after masters-a and masters-b, so Tide, Iron and Dune hold ids 283 to 348. Keys are unique across every school. */
	static void techniques(SchoolTable table) {
		table.add(TIDE, TIDE_TABLE);
		table.add(IRON, IRON_TABLE);
		table.add(DUNE, DUNE_TABLE);
	}

	static final String TIDE_TABLE = """
		tide_cut SWEEP
		ebb_and_flow SWEEP ~SWEEP
		spring_tide ^LUNGE
		breakwater !CLEAVE
		foam_needle THRUST *THRUST
		undertow_hook LOW ~LOW
		rising_swell RISING HIGH
		crashing_surf FALLING !DROP
		whirl_of_the_deep WHIRL
		neap_reply REPLY ~SWEEP
		coral_lance ^THRUST !LUNGE
		spindrift *SWEEP *~SWEEP *SWEEP
		tidal_bore ^DRIVE THRUST
		kelp_snare LOW UPPER
		moon_pull HIGH ~LOW
		riptide_turn @SWEEP ~SWEEP
		salt_spray THRUST ~FALLING
		harbour_gate SWEEP ~SWEEP !CLEAVE
		deep_current PALM ^LUNGE
		storm_surge @SWEEP @~SWEEP
		leviathan_coil WHIRL !CLEAVE
		last_tide ^THRUST ~FALLING LOW !UPPER
		""";
	static final String IRON_TABLE = """
		iron_cut !SWEEP
		hammer_and_tongs CLEAVE ~CLEAVE
		bellows_thrust ^THRUST
		quench_drop UPPER !DROP
		rivet_line THRUST THRUST
		ingot_press !PLUNGE
		temper_turn @SWEEP
		slag_sweep LOW ~LOW
		plate_breaker PALM !CLEAVE
		mail_cutter SWEEP ~SWEEP SWEEP
		crucible WHIRL
		tongs_hook HIGH ~LOW
		forge_lance ^^LUNGE
		annealing_reply REPLY !THRUST
		spark_shower *THRUST *~THRUST *THRUST
		cold_iron FALLING ~FALLING
		rampart_breaker ~SWEEP !CLEAVE
		smelter_rise LOW UPPER
		riveted_cross FALLING ~FALLING !THRUST
		blast_furnace @SWEEP @~SWEEP
		steel_verdict WHIRL ~SWEEP
		last_ingot ^THRUST ~SWEEP LOW !CLEAVE
		""";
	static final String DUNE_TABLE = """
		grit_cut SWEEP
		sirocco THRUST ~THRUST
		mirage_step ^LUNGE
		dune_crest RISING HIGH
		sand_lash *SWEEP *~SWEEP
		dust_needle THRUST *THRUST
		scarab_hook LOW ~LOW
		sinking_cut LOW !DROP
		haboob WHIRL
		oasis_reply REPLY ~SWEEP
		dry_lightning ^DRIVE *THRUST
		caravan_line SWEEP ~SWEEP SWEEP
		dune_slide FALLING ~LOW
		scorpion_tail HIGH !PLUNGE
		dervish_turn @SWEEP
		desert_wind PALM ^LUNGE
		shifting_dune LOW UPPER
		sunbaked_cross FALLING ~FALLING !THRUST
		simoom_spiral @SWEEP @~SWEEP
		buried_gate ~SWEEP SWEEP !CLEAVE
		erg_storm WHIRL !CLEAVE
		last_dune LOW !UPPER @SWEEP !CLEAVE
		""";
}
