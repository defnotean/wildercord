package dev.wildercord.aura.world;

import dev.wildercord.aura.BreathingMethod;
import dev.wildercord.aura.BreathingMethods;

import java.util.Collection;
import java.util.List;

/**
 * The Starlit, Hourglass and Crimson Masters: their school ids, ordinary patterns, named techniques and signature
 * forms. Shared Master code calls in through short marked hooks; everything school-specific lives here.
 * Their tempo is the retuned one: every strike tells for 12 to 14 ticks, every chain recovers for 14 to 16.
 */
public final class MastersPackB {
	private MastersPackB() {}

	/** Wire values follow each method's index in BreathingMethods.BUILT_IN, so 3 to 6 stay free for other schools. */
	public static final int STARLIT = 7, HOURGLASS = 8, CRIMSON = 9;
	public static final List<Integer> SCHOOLS = List.of(STARLIT, HOURGLASS, CRIMSON);
	public static final int MIN_TELL = 12, MAX_TELL = 14, MIN_RECOVERY = 14, MAX_RECOVERY = 16;
	/** Each school keeps at least this many named techniques. */
	public static final int MIN_TECHNIQUES = 25;

	/** No signature or technique chain deals more to one challenger than this, however many of its beats they stand in. */
	public static final double CHAIN_CAP = MastersRules.TECHNIQUE_DAMAGE * MasterTechniques.MAX_SHARE;

	/** What one more beat may still deal to a challenger already dealt {@code dealt} by this form. */
	public static double capped(double dealt, double damage) {
		if (!Double.isFinite(dealt) || !Double.isFinite(damage) || damage <= 0) return 0;
		return Math.max(0, Math.min(damage, CHAIN_CAP - Math.max(0, dealt)));
	}

	public static boolean owns(int school) { return school == STARLIT || school == HOURGLASS || school == CRIMSON; }

	public static String id(int school) {
		return switch (school) {
			case STARLIT -> "starlit";
			case HOURGLASS -> "hourglass";
			case CRIMSON -> "crimson";
			default -> throw new IllegalArgumentException("Not a pack B school: " + school);
		};
	}

	/** -1 for any method these Masters do not teach. */
	public static int school(BreathingMethod method) {
		if (method == null) return -1;
		if (method.equals(BreathingMethods.STARLIT)) return STARLIT;
		if (method.equals(BreathingMethods.HOURGLASS)) return HOURGLASS;
		if (method.equals(BreathingMethods.CRIMSON)) return CRIMSON;
		return -1;
	}

	public static BreathingMethod method(int school) {
		return switch (school) {
			case STARLIT -> BreathingMethods.STARLIT;
			case HOURGLASS -> BreathingMethods.HOURGLASS;
			case CRIMSON -> BreathingMethods.CRIMSON;
			default -> throw new IllegalArgumentException("Not a pack B school: " + school);
		};
	}

	/** The ordinary rotation each school falls back to between its named techniques. */
	public static MastersRules.Move[] pattern(int school) {
		return switch (school) {
			case STARLIT -> new MastersRules.Move[] {MastersRules.Move.THRUST, MastersRules.Move.CRESCENT, MastersRules.Move.THRUST, MastersRules.Move.SWEEP};
			case HOURGLASS -> new MastersRules.Move[] {MastersRules.Move.SWEEP, MastersRules.Move.THRUST, MastersRules.Move.SWEEP, MastersRules.Move.CRESCENT};
			default -> new MastersRules.Move[] {MastersRules.Move.SWEEP, MastersRules.Move.SWEEP, MastersRules.Move.THRUST, MastersRules.Move.SWEEP};
		};
	}

	/** One signature per school, appended after the shared moves so every older wire id keeps its meaning. */
	public static MastersRules.Move signature(int school) {
		return switch (school) {
			case STARLIT -> MastersRules.Move.STARLIT_CONSTELLATION;
			case HOURGLASS -> MastersRules.Move.HOURGLASS_REWIND;
			case CRIMSON -> MastersRules.Move.CRIMSON_FRENZY;
			default -> null;
		};
	}

	public static boolean signature(MastersRules.Move move) {
		return move == MastersRules.Move.STARLIT_CONSTELLATION || move == MastersRules.Move.HOURGLASS_REWIND
			|| move == MastersRules.Move.CRIMSON_FRENZY;
	}

	public static int school(MastersRules.Move move) {
		return move == MastersRules.Move.STARLIT_CONSTELLATION ? STARLIT : move == MastersRules.Move.HOURGLASS_REWIND ? HOURGLASS
			: move == MastersRules.Move.CRIMSON_FRENZY ? CRIMSON : -1;
	}

	public static double cost(MastersRules.Move move) {
		return move == MastersRules.Move.STARLIT_CONSTELLATION ? StarlitConstellationRules.COST
			: move == MastersRules.Move.HOURGLASS_REWIND ? HourglassRewindRules.COST
			: move == MastersRules.Move.CRIMSON_FRENZY ? CrimsonFrenzyRules.COST : MastersRules.ATTACK_COST;
	}

	public static int cooldown(MastersRules.Move move) {
		return move == MastersRules.Move.STARLIT_CONSTELLATION ? StarlitConstellationRules.COOLDOWN
			: move == MastersRules.Move.HOURGLASS_REWIND ? HourglassRewindRules.COOLDOWN : CrimsonFrenzyRules.COOLDOWN;
	}

	/** Lang key of the plain answer to a strike of this shape. */
	public static String answer(MasterTechniques.Shape shape) {
		return "message.wildercord.master.answer." + switch (shape) {
			case ARC_LOW -> "jump";
			case ARC_HIGH -> "duck";
			case LANE -> "sidestep";
			case ARC, CIRCLE -> "clear";
		};
	}

	/** Lang key of the answer shown with an ordinary attack or a signature's opening banner. */
	public static String answer(MastersRules.Move move) {
		return switch (move) {
			case SWEEP -> answer(MasterTechniques.Shape.ARC);
			case THRUST, CRESCENT, BREAK_CAST -> answer(MasterTechniques.Shape.LANE);
			case STARLIT_CONSTELLATION -> "message.wildercord.master.constellation_hint";
			case HOURGLASS_REWIND -> "message.wildercord.master.rewind_hint";
			case CRIMSON_FRENZY -> "message.wildercord.master.frenzy_hint";
			default -> null;
		};
	}

	/** Every strike of these schools shows its answer for 12 to 14 ticks; chained strikes keep a longer floor than a fresh combo. */
	public static int tell(int index, int authored) {
		return Math.max(MIN_TELL, Math.min(MAX_TELL, authored + (index == 0 ? 0 : 4)));
	}

	public static int recovery(int authored) {
		return Math.max(MIN_RECOVERY, Math.min(MAX_RECOVERY, authored));
	}

	/** Techniques still allowed after the last few named ones; a school always keeps fresh choices. */
	public static List<MasterTechniques.Technique> fresh(List<MasterTechniques.Technique> options, Collection<Integer> recent) {
		return options.stream().filter(technique -> !recent.contains(technique.id())).toList();
	}

	@FunctionalInterface
	interface SchoolTable { void add(int school, String table); }

	/** Called once, at the end of MasterTechniques' table, so the older hundred keep ids 1 to 100. */
	static void techniques(SchoolTable table) {
		table.add(STARLIT, """
			first_light THRUST
			twin_stars THRUST ~THRUST
			falling_star FALLING ~DROP
			polaris ^LUNGE
			star_needle THRUST *THRUST
			night_arc SWEEP ~SWEEP
			comet_tail ^DRIVE THRUST
			meteor_drop UPPER !PLUNGE
			orbit WHIRL
			zenith RISING HIGH
			nadir LOW ~LOW
			starlit_gate HIGH LOW
			pleiades THRUST *THRUST *~THRUST
			dusk_cross FALLING ~FALLING
			moonrise LOW UPPER
			aurora_veil SWEEP ~HIGH
			sirius_point ^THRUST !LUNGE
			eclipse @SWEEP ~SWEEP
			nebula_turn WHIRL ~SWEEP
			quasar_lance ^^LUNGE
			starfall_rain DROP DROP *THRUST
			celestial_wheel @SWEEP @~SWEEP
			zodiac_chain THRUST ~FALLING LOW !UPPER
			dawn_star REPLY !THRUST
			lodestar PALM ^LUNGE
			last_constellation ^THRUST ~FALLING @SWEEP !LUNGE
			""");
		table.add(HOURGLASS, """
			grain_cut SWEEP
			second_hand THRUST THRUST
			pendulum SWEEP ~SWEEP
			tick_tock FALLING ~FALLING
			sandfall DROP DROP
			hour_hand !CLEAVE
			minute_hand *THRUST *THRUST *THRUST
			turning_glass @SWEEP
			long_hour ^LUNGE
			echo_step ^DRIVE ~SWEEP
			dial_sweep LOW ~LOW
			noon_bell HIGH !CLEAVE
			midnight_bell LOW HIGH
			sundial_turn WHIRL
			recurrence REPLY ~REPLY
			hourglass_flip UPPER ~DROP
			dune_clock SWEEP ~SWEEP SWEEP
			time_lapse PALM ^LUNGE
			paused_blade RISING !THRUST
			chime_spiral @SWEEP @~SWEEP
			sand_wake LOW UPPER
			measured_cross FALLING ~FALLING !THRUST
			eternal_turn WHIRL @~SWEEP
			borrowed_hour ^THRUST ~SWEEP
			clockwork_chain THRUST ~SWEEP LOW !UPPER
			last_grain ^FALLING ~FALLING @SWEEP !CLEAVE
			""");
		table.add(CRIMSON, """
			first_blood SWEEP ~SWEEP
			red_fang ^LUNGE
			vein_cut FALLING ~FALLING
			heart_strike !THRUST
			crimson_tide SWEEP ~SWEEP SWEEP
			blood_moon @SWEEP
			scarlet_rush ^DRIVE *THRUST
			iron_taste PALM !CLEAVE
			bleeding_edge LOW ~LOW
			wound_fan *SWEEP *~SWEEP *SWEEP
			red_rising LOW UPPER
			pulse_beat THRUST *THRUST
			hemorrhage UPPER !CLEAVE
			sanguine_wheel WHIRL
			rose_thorn HIGH LOW
			claret_cross FALLING ~FALLING !THRUST
			fever_drive ^^DRIVE
			vermilion_spiral @SWEEP @~SWEEP
			gore_gate ~SWEEP SWEEP !CLEAVE
			blood_rain PLUNGE PLUNGE
			ruby_reply REPLY ~SWEEP
			carmine_hook HIGH ~HIGH
			heartbeat_chain *THRUST *~SWEEP *THRUST *SWEEP
			red_harvest WHIRL !CLEAVE
			mortal_lunge ^THRUST !LUNGE
			last_drop LOW !UPPER @SWEEP !CLEAVE
			""");
	}
}
