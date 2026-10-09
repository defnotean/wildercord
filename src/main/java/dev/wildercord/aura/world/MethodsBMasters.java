package dev.wildercord.aura.world;

import dev.wildercord.aura.MethodsBArtRules;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * The Echo, Dawn and Venom Sword Masters. Each fights on a built-in school's footing (its guard rhythm, tempo and ordinary
 * cuts) but with its own named techniques, its own signature (see {@link MethodsBSignatureRules}) and none of the base school's
 * signature openers. Their school ids are the reserved 13 to 15, after methods-a's Tide, Iron and Dune (10 to 12); their
 * first clears are those bits of the shared {@link MasterVictories} record.
 */
public final class MethodsBMasters {
	private MethodsBMasters() {}

	public static final int ECHO = 13, DAWN = 14, VENOM = 15;
	public static final List<Integer> SCHOOLS = List.of(ECHO, DAWN, VENOM);
	/** The reserved Master-technique id range: 23 techniques per school, Echo from 349, Dawn from 372, Venom from 395. */
	public static final int FIRST_TECHNIQUE = 349, LAST_TECHNIQUE = 417;

	public static boolean owns(int school) {
		return school >= ECHO && school <= VENOM;
	}

	/** The built-in school a pack Master fights on: Echo on Ember's, Dawn on Stone's (it guards after every attack), Venom on Gale's. */
	public static int base(int school) {
		return switch (school) {
			case DAWN -> MastersRules.STONE;
			case VENOM -> MastersRules.GALE;
			default -> MastersRules.EMBER;
		};
	}

	public static String id(int school) {
		return switch (school) {
			case ECHO -> MethodsBArtRules.ECHO;
			case DAWN -> MethodsBArtRules.DAWN;
			case VENOM -> MethodsBArtRules.VENOM;
			default -> "";
		};
	}

	/** The pack Master of a breathing method, or -1. */
	public static int forMethod(String method) {
		if (method == null) return -1;
		return switch (method) {
			case MethodsBArtRules.ECHO -> ECHO;
			case MethodsBArtRules.DAWN -> DAWN;
			case MethodsBArtRules.VENOM -> VENOM;
			default -> -1;
		};
	}

	public static Component schoolName(int school) {
		return Component.translatable("master.wildercord.school." + id(school));
	}

	public static String lessonKey(int school) {
		return "methods_b_" + id(school) + "_lesson";
	}

	/** The technique lesson a first clear teaches: Echo's wave, Dawn's ward, Venom's bind. */
	public static String reward(int school) {
		return switch (school) {
			case ECHO -> dev.wildercord.aura.TechniqueRules.WAVE;
			case DAWN -> dev.wildercord.aura.TechniqueRules.WARD;
			case VENOM -> dev.wildercord.aura.TechniqueRules.BIND;
			default -> "";
		};
	}

	/** The shared victory-record bit of a pack school ({@code 1 << school}, bits 13 to 15), or 0 for any other. */
	public static int bit(int school) {
		return owns(school) ? 1 << school : 0;
	}

	// ------------------------------------------------------------------ techniques
	// Two strikes each: a 12-tick tell (14 with a step or a spin), a 15-tick open recovery. The last line is the signature.

	public static final String ECHO_TABLE = """
		ringing_pair SWEEP ~SWEEP
		struck_bell FALLING ~FALLING
		low_hum LOW ~LOW
		overtone RISING *RISING
		tuning_fork THRUST *THRUST
		cadence ~SWEEP SWEEP
		reverb_turn @SWEEP ~SWEEP
		sounding_drop DROP ~DROP
		chime_point ^THRUST REPLY
		rolling_peal WHIRL @~SWEEP
		hush_cut HIGH ~HIGH
		counter_melody REPLY *~REPLY
		resonant_palm PALM ~PALM
		grace_note UPPER *DROP
		tremolo FALLING *~FALLING
		sympathetic_cut CLEAVE ~CLEAVE
		drumroll ^LUNGE *LUNGE
		dissonance LOW ~HIGH
		refrain DRIVE ~DRIVE
		breath_note PLUNGE *~PLUNGE
		descant HIGH *UPPER
		harmonic_wheel @THRUST ~WHIRL
		tolling_bell THRUST REPLY
		""";

	public static final String DAWN_TABLE = """
		first_ray RISING ~RISING
		sunbeam THRUST ~THRUST
		daybreak_cut LOW UPPER
		halo_turn WHIRL ~SWEEP
		morning_star FALLING *FALLING
		gilded_arc SWEEP ~FALLING
		lantern_thrust ^THRUST *THRUST
		zenith_drop HIGH DROP
		aurora_sweep ~SWEEP *SWEEP
		rose_petal PALM *~PALM
		glint UPPER ~UPPER
		sunspot PLUNGE ~RISING
		prism_cut CLEAVE *~SWEEP
		dawn_chorus @SWEEP *~SWEEP
		bright_reply REPLY ~THRUST
		solar_lunge ^LUNGE ~LUNGE
		meridian DROP *DROP
		corona @THRUST WHIRL
		radiant_step ^RISING *~RISING
		crown_of_morning LOW *HIGH
		white_gold DRIVE *~FALLING
		rosy_fingers ~FALLING ~UPPER
		noon_glare RISING HIGH
		""";

	public static final String VENOM_TABLE = """
		twin_fangs ^LUNGE *~LUNGE
		viper_bite THRUST *~THRUST
		asp_sweep LOW *LOW
		coil_and_strike WHIRL *THRUST
		adder_rise LOW *UPPER
		mamba_rush ^DRIVE *DRIVE
		sidewinder SWEEP *~SWEEP
		cobra_hood HIGH ~DROP
		pit_viper PLUNGE *THRUST
		constrictor @SWEEP @~SWEEP
		scale_shed ~SWEEP *LOW
		fang_and_tail THRUST ~LOW
		forked_tongue REPLY *REPLY
		hooded_reply REPLY ~RISING
		basilisk_gaze HIGH *THRUST
		venom_drip DROP *~DROP
		slither_cut ^LOW ~LOW
		rattlesnake FALLING *~FALLING
		hydra_snap PALM *THRUST
		jungle_strike CLEAVE *LOW
		nest_guard UPPER ~SWEEP
		king_cobra ^THRUST ~THRUST
		serpent_coil WHIRL ~WHIRL
		""";

	public static final Map<String, MethodsBSignatureRules.Signature> SIGNATURES = Map.of(
		"tolling_bell", MethodsBSignatureRules.Signature.TOLL,
		"noon_glare", MethodsBSignatureRules.Signature.GLARE,
		"serpent_coil", MethodsBSignatureRules.Signature.COIL);

	/** The signature a technique is, or null. */
	public static MethodsBSignatureRules.Signature signature(String key) {
		return key == null ? null : SIGNATURES.get(key);
	}

	/** A signature turns up three times as often as one plain technique. */
	public static int extraWeight(MasterTechniques.Technique technique) {
		return signature(technique.key()) != null ? 2 : 0;
	}

	/** The banner's second line: a signature's answer, or the answer to the technique's opening strike. */
	public static Component hint(MasterTechniques.Technique technique) {
		MethodsBSignatureRules.Signature signature = signature(technique.key());
		if (signature != null) return Component.translatable("message.wildercord.master.methods_b."
			+ signature.name().toLowerCase(java.util.Locale.ROOT) + "_hint");
		return Component.translatable("message.wildercord.master.methods_b.answer."
			+ technique.strikes().getFirst().shape().name().toLowerCase(java.util.Locale.ROOT));
	}

	// ------------------------------------------------------------------ the signatures in the world

	/** Shows where a signature will land, for {@code ticks}: the rung lane, the gathering sun, the coil with its gap. */
	static void warn(ServerLevel level, MethodsBSignatureRules.Signature signature, int strike, Vec3 feet, Vec3 aim, long began, int color,
		int ticks) {
		Vec3 side = new Vec3(-aim.z, 0, aim.x);
		switch (signature) {
			case TOLL -> {
				double w = MethodsBSignatureRules.TOLL_HALF_WIDTH;
				Vec3 end = feet.add(aim.scale(MethodsBSignatureRules.TOLL_REACH));
				Light.ray(level, feet, end, color, strike > 0 ? 0.14 : 0.08, ticks);
				Light.ray(level, feet.add(side.scale(w)), end.add(side.scale(w)), 0xE4E0F4, 0.04, ticks);
				Light.ray(level, feet.add(side.scale(-w)), end.add(side.scale(-w)), 0xE4E0F4, 0.04, ticks);
				Feels.sound(level, feet, "aura_echo_art", 0.9F, strike > 0 ? 1.3F : 1.0F);
			}
			case GLARE -> {
				Vec3 sun = feet.add(0, 3.2, 0);
				for (int i = 0; i < 8; i++) {
					double a = Math.PI * 2 * i / 8;
					Light.ray(level, sun, sun.add(Math.cos(a) * 1.6, -0.6, Math.sin(a) * 1.6), i % 2 == 0 ? 0xFFF4C8 : 0xFFC0D0, 0.06, ticks);
				}
				Light.ray(level, feet, sun, 0xFFFFFF, 0.1, ticks);
				Feels.sound(level, sun, "aura_dawn_art", 0.9F, 0.9F + 0.15F * strike);
			}
			case COIL -> {
				double gap = MethodsBSignatureRules.gapDegrees(began);
				double half = MethodsBSignatureRules.COIL_GAP_DEGREES / 2;
				for (int d = -180; d < 180; d += 20) {
					if (Math.abs(MethodsBSignatureRules.wrap(d - gap)) <= half) continue;
					Light.ray(level, feet, feet.add(rotate(aim, side, d).scale(MethodsBSignatureRules.COIL_RADIUS)), color, 0.06, ticks);
				}
				// The gap's edges, bright, so the way out reads at a glance.
				for (double edge : new double[] {gap - half, gap + half})
					Light.ray(level, feet, feet.add(rotate(aim, side, edge).scale(MethodsBSignatureRules.COIL_RADIUS + 1)), 0xFFFFFF, 0.1, ticks);
				Feels.sound(level, feet, "aura_venom_art", 0.9F, 0.9F + 0.1F * strike);
			}
		}
	}

	/** Whether {@code player} is caught, the Master at {@code origin} aiming along {@code aim}. */
	static boolean hits(MethodsBSignatureRules.Signature signature, SwordMaster master, ServerPlayer player, Vec3 origin, Vec3 aim, long began) {
		Vec3 side = new Vec3(-aim.z, 0, aim.x);
		Vec3 delta = player.position().subtract(origin);
		Vec3 line = master.getEyePosition().subtract(player.getEyePosition());
		double facing = line.lengthSqr() < 1e-6 ? 1 : player.getLookAngle().dot(line.normalize());
		return MethodsBSignatureRules.hits(signature, delta.dot(aim), delta.dot(side), delta.y, facing, MethodsBSignatureRules.gapDegrees(began));
	}

	/** What a signature leaves on a player it caught, beyond the cut. */
	static void afflict(MethodsBSignatureRules.Signature signature, SwordMaster master, ServerPlayer player) {
		switch (signature) {
			case TOLL -> player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, MethodsBSignatureRules.TOLL_REEL, 0, false, true), master);
			case GLARE -> player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, MethodsBSignatureRules.GLARE_BLIND, 0, false, true), master);
			case COIL -> player.addEffect(new MobEffectInstance(MobEffects.POISON, MethodsBSignatureRules.COIL_POISON, 0, false, true), master);
		}
	}

	/** {@code aim} turned {@code degrees} toward {@code side} (its left). */
	static Vec3 rotate(Vec3 aim, Vec3 side, double degrees) {
		double r = Math.toRadians(degrees);
		return aim.scale(Math.cos(r)).add(side.scale(Math.sin(r)));
	}
}
