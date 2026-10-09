package dev.wildercord.aura.world;

import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.aura.TechniqueRules;

import java.util.Collection;
import java.util.List;

/**
 * The Rime, Thunder, Verdant and Hollow Sword Masters: school identities, their named-technique tables and the
 * pure facts the shared Masters registries ask about them. Everything here is server/client neutral.
 */
public final class ElementalMasters {
	private ElementalMasters() {}

	public static final int RIME = 3, THUNDER = 4, VERDANT = 5, HOLLOW = 6;
	public static final List<Integer> SCHOOL_IDS = List.of(RIME, THUNDER, VERDANT, HOLLOW);
	/** First-clear bits these schools own in {@link MasterVictoryRules.Progress}. */
	public static final int VICTORY_MASK = (1 << RIME) | (1 << THUNDER) | (1 << VERDANT) | (1 << HOLLOW);

	/** Technique timing for these schools: every strike is warned for 12 to 14 ticks and every chain ends 14 to 16 ticks open. */
	public static final int MIN_TELL = 12, MAX_TELL = 14, MIN_RECOVERY = 14, MAX_RECOVERY = 16;
	/** The shared exchange pause after the Master spends its Aura. */
	public static final int EXCHANGE_PAUSE = MastersRules.BREATH_TICKS;
	/** How many recent techniques a Master refuses to repeat (mirrors SwordMaster's recent-technique window). */
	public static final int RECENT = 6;
	/** Every signature shares this cooldown once admitted. */
	public static final int SIGNATURE_COOLDOWN = 140;

	public static boolean owns(int school) { return school >= RIME && school <= HOLLOW; }

	public static String name(int school) {
		return switch (school) {
			case RIME -> "rime";
			case THUNDER -> "thunder";
			case VERDANT -> "verdant";
			case HOLLOW -> "hollow";
			default -> "";
		};
	}

	public static BreathingMethod method(int school) {
		return switch (school) {
			case RIME -> BreathingMethods.RIME;
			case THUNDER -> BreathingMethods.THUNDER;
			case VERDANT -> BreathingMethods.VERDANT;
			case HOLLOW -> BreathingMethods.HOLLOW;
			default -> null;
		};
	}

	/** The school a teacher of this breathing method introduces, or -1 when another table owns it. */
	public static int schoolOf(BreathingMethod method) {
		for (int school : SCHOOL_IDS) if (method(school).equals(method)) return school;
		return -1;
	}

	public static int schoolOf(String id) {
		for (int school : SCHOOL_IDS) if (name(school).equals(id)) return school;
		return -1;
	}

	/** The one-line counterplay lesson key suffix, e.g. {@code rime_lesson}. */
	public static String lesson(int school) { return owns(school) ? name(school) + "_lesson" : null; }

	/** Existing horizontal technique parts taught on a first clear. */
	public static String reward(int school) {
		return switch (school) {
			case RIME -> TechniqueRules.WARD;
			case THUNDER -> TechniqueRules.PIERCE;
			case VERDANT -> TechniqueRules.RALLY;
			case HOLLOW -> TechniqueRules.BIND;
			default -> "";
		};
	}

	public static MastersRules.Move signatureMove(int school) {
		return switch (school) {
			case RIME -> MastersRules.Move.RIME_LATTICE;
			case THUNDER -> MastersRules.Move.THUNDER_CHAIN;
			case VERDANT -> MastersRules.Move.VERDANT_BLOOM;
			case HOLLOW -> MastersRules.Move.HOLLOW_PULL;
			default -> null;
		};
	}

	public static boolean signature(MastersRules.Move move) {
		return move == MastersRules.Move.RIME_LATTICE || move == MastersRules.Move.THUNDER_CHAIN
			|| move == MastersRules.Move.VERDANT_BLOOM || move == MastersRules.Move.HOLLOW_PULL;
	}

	public static int schoolOf(MastersRules.Move move) {
		return switch (move) {
			case RIME_LATTICE -> RIME;
			case THUNDER_CHAIN -> THUNDER;
			case VERDANT_BLOOM -> VERDANT;
			case HOLLOW_PULL -> HOLLOW;
			default -> -1;
		};
	}

	public static double cost(MastersRules.Move move) {
		return switch (move) {
			case RIME_LATTICE -> RimeLatticeRules.COST;
			case THUNDER_CHAIN -> ThunderChainRules.COST;
			case VERDANT_BLOOM -> VerdantBloomRules.COST;
			case HOLLOW_PULL -> HollowPullRules.COST;
			default -> MastersRules.ATTACK_COST;
		};
	}

	/** Total ticks from admission to the end of the signature's open recovery. */
	public static int end(MastersRules.Move move) {
		return switch (move) {
			case RIME_LATTICE -> RimeLatticeRules.END;
			case THUNDER_CHAIN -> ThunderChainRules.END;
			case VERDANT_BLOOM -> VerdantBloomRules.END;
			case HOLLOW_PULL -> HollowPullRules.END;
			default -> move.tell + move.recovery;
		};
	}

	/** Ages (ticks after admission) at which the signature resolves a hit: one per pulse, rod or collapse. */
	public static int[] beats(MastersRules.Move move) {
		return switch (move) {
			case RIME_LATTICE -> new int[] {RimeLatticeRules.freezeAt(0), RimeLatticeRules.freezeAt(1), RimeLatticeRules.freezeAt(2)};
			case THUNDER_CHAIN -> new int[] {ThunderChainRules.strikeAt(0), ThunderChainRules.strikeAt(1), ThunderChainRules.strikeAt(2)};
			case VERDANT_BLOOM -> new int[] {VerdantBloomRules.TELL};
			case HOLLOW_PULL -> new int[] {HollowPullRules.TELL};
			default -> new int[] {move.tell};
		};
	}

	/** The age of the signature's last resolving hit; its open recovery follows. */
	public static int lastEvent(MastersRules.Move move) { int[] beats = beats(move); return beats[beats.length - 1]; }

	/** Highest total damage one challenger can take from one signature. */
	public static double cap(MastersRules.Move move) {
		return switch (move) {
			case RIME_LATTICE -> RimeLatticeRules.CAP;
			case THUNDER_CHAIN -> ThunderChainRules.DAMAGE;
			case VERDANT_BLOOM -> VerdantBloomRules.DAMAGE;
			case HOLLOW_PULL -> HollowPullRules.DAMAGE;
			default -> move.damage;
		};
	}

	public static String hintKey(MastersRules.Move move) {
		int school = schoolOf(move);
		return school < 0 ? null : "message.wildercord.master." + name(school) + "_hint";
	}

	/** Every signature occupies the second slot of each four-move phrase, after its readiness and Aura are restored. */
	public static boolean eligible(int school, int sequence, double aura, long now, long readyAt, double cost) {
		return owns(school) && Math.floorMod(sequence, 4) == 1 && now >= readyAt
			&& Double.isFinite(aura) && aura >= cost && aura <= MastersRules.AURA_MAX;
	}

	/** A strike's warning: 12 ticks, one more for a heavy blow and one more for a turn or a lunge. */
	public static int tell(boolean heavy, boolean spin, boolean step) {
		return Math.max(MIN_TELL, Math.min(MAX_TELL, MIN_TELL + (heavy ? 1 : 0) + (spin || step ? 1 : 0)));
	}

	/** The open recovery after a chain of {@code count} strikes. */
	public static int recovery(int count) { return MIN_RECOVERY + Math.max(0, Math.min(2, count - 1)); }

	/**
	 * The techniques a Master of this school could open with: in reach, affordable with a guard in reserve and not one of
	 * the last {@link #RECENT}. Mirrors SwordMaster.beginTechnique's filter.
	 */
	public static List<MasterTechniques.Technique> options(int school, double distance, double aura, Collection<Integer> recent) {
		return MasterTechniques.forSchool(school).stream()
			.filter(combo -> distance <= combo.openingReach() && !recent.contains(combo.id())
				&& aura >= combo.cost() + MastersRules.GUARD_COST)
			.toList();
	}

	/** Each school's 26 named techniques, in the shared notation of {@link MasterTechniques}. */
	public static String techniques(int school) {
		return switch (school) {
			case RIME -> """
				frost_needle THRUST
				hoarfrost_cut SWEEP ~SWEEP
				glacier_drop !CLEAVE
				lattice_cross FALLING ~FALLING
				icicle_rain DROP DROP DROP
				snowdrift_sweep LOW ~LOW
				permafrost LOW !UPPER
				frozen_lane ^THRUST ~SWEEP
				white_out *SWEEP *~SWEEP *THRUST
				cold_snap *THRUST *THRUST
				sleet_turn @SWEEP
				floe_split UPPER ~FALLING
				crystal_point ^LUNGE
				blizzard_wheel WHIRL
				shiver_palm PALM THRUST
				hailstone_drop PLUNGE !PLUNGE
				thin_ice HIGH LOW
				winter_reply REPLY ~REPLY
				icefall RISING !CLEAVE
				frostbite_flurry *FALLING *~FALLING *SWEEP *~SWEEP
				polar_drive DRIVE THRUST
				glaze_cut ~HIGH HIGH
				snowblind *PALM @~SWEEP
				cold_current LOW @SWEEP
				icebound_gate ~SWEEP SWEEP !THRUST
				last_frost ^FALLING ~SWEEP @SWEEP !CLEAVE
				""";
			case THUNDER -> """
				thunderclap !CLEAVE
				lightning_step ^THRUST
				spark_jab *THRUST *~THRUST
				bolt_lunge ^^LUNGE
				storm_front *SWEEP *~SWEEP *SWEEP
				fork_cut FALLING ~FALLING
				static_palm PALM ^LUNGE
				rolling_thunder SWEEP @~SWEEP
				arc_flash *DROP *THRUST
				skybreaker UPPER !CLEAVE
				ozone_cut HIGH ~SWEEP
				charged_reply REPLY THRUST
				thunderhead WHIRL !CLEAVE
				volt_drive ^DRIVE
				crackle_flurry *THRUST *~SWEEP *THRUST *~SWEEP
				ground_strike PLUNGE
				storm_rising LOW UPPER
				squall_bolt RISING ^THRUST
				flash_cross *FALLING *~FALLING
				tempest_crown HIGH ~HIGH
				thunder_wheel @SWEEP @~SWEEP
				discharge !DROP !THRUST
				stormcaller ~RISING RISING
				blinding_arc *HIGH LOW
				galvanic_rush ^DRIVE ^THRUST
				last_thunder ^THRUST *FALLING @SWEEP !CLEAVE
				""";
			case VERDANT -> """
				sprout_cut UPPER
				bramble_sweep SWEEP ~SWEEP
				thornline THRUST THRUST
				vine_lash *SWEEP *~SWEEP
				root_snare LOW ~LOW
				bloom_turn @~SWEEP
				oak_breaker !CLEAVE
				willow_bend ~HIGH LOW
				canopy_fall FALLING !CLEAVE
				seedfall DROP DROP
				greenwood_palm PALM SWEEP
				moss_step ^FALLING
				ivy_coil LOW @SWEEP
				petal_flurry *THRUST *~SWEEP *SWEEP
				heartwood !THRUST
				sapling_rise RISING UPPER
				briar_wheel WHIRL ~SWEEP
				grove_reply REPLY !CLEAVE
				fern_unfurl ~SWEEP SWEEP ~SWEEP
				overgrowth HIGH ~HIGH !UPPER
				pollen_drift *PALM *THRUST
				blossom_rain PLUNGE PLUNGE
				timber_drive ^DRIVE ~SWEEP
				wild_bloom @SWEEP !CLEAVE
				spring_lunge ^LUNGE
				last_bloom LOW ~FALLING @SWEEP !UPPER
				""";
			case HOLLOW -> """
				void_point THRUST
				null_cut ~SWEEP
				hollow_drop !DROP
				event_horizon WHIRL
				abyss_thrust ^^THRUST
				umbral_cross FALLING ~FALLING
				gravity_well !PLUNGE
				dark_matter *THRUST *THRUST *THRUST
				eclipse_cut HIGH !CLEAVE
				singularity @SWEEP @SWEEP
				nether_palm PALM !THRUST
				riftwalk ^FALLING ~FALLING
				shade_flurry *SWEEP *~SWEEP *THRUST *~SWEEP
				void_reply REPLY REPLY
				collapse UPPER !CLEAVE
				starless_lunge ^LUNGE
				empty_hand *PALM ^LUNGE
				penumbra LOW HIGH
				dusk_drive DRIVE !THRUST
				silent_wheel WHIRL @~SWEEP
				vacuum_cut RISING ~RISING
				nightfall !FALLING
				oblivion_gate ~SWEEP SWEEP @!SWEEP
				black_star DROP ^THRUST
				abyssal_coil LOW @~SWEEP
				last_void ^THRUST ~FALLING @SWEEP !CLEAVE
				""";
			default -> "";
		};
	}
}
