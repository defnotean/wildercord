package dev.wildercord.aura.world;

import dev.wildercord.aura.world.MasterAnimationRules.Joint;
import dev.wildercord.aura.world.MasterAnimationRules.Pose;
import java.util.ArrayList;
import java.util.List;

/**
 * The masters' hundred named sword techniques: short combos of one to four strikes. Each strike is one authored
 * motion (some mirrored into a backhand, spun into a whirl, carried forward by a lunge, quickened or made heavy),
 * so every technique is a distinct animated sequence. Every strike still has its own chamber and a locked aim
 * before it lands, and the whole combo ends in an open recovery. Shared by the server (timing, reach) and the client (pose).
 */
public final class MasterTechniques {
	private MasterTechniques() {}

	/** Ticks before the first strike lands, and before each later one. School tempo scales both. */
	public static final int FIRST_TELL = 12, FOLLOW_TELL = 8, MIN_TELL = 5;
	public static final double STEP = 2.0;
	/** Upper bound on a whole chain's damage, as a multiple of one technique hit. */
	public static final double MAX_SHARE = 1.75;

	/** How a strike's hit area is measured from the master's feet along its locked aim. */
	public enum Shape {
		/** A broad cut in front. */ ARC,
		/** A low cut: jumping clears it. */ ARC_LOW,
		/** A head-height cut: crouching ducks it. */ ARC_HIGH,
		/** A narrow line: sidestep it. */ LANE,
		/** All round the master. */ CIRCLE
	}

	/** One authored strike motion. */
	public record Primitive(String name, Pose chamber, Pose impact, Pose follow, Shape shape, double reach, double width, double damage) {}

	public record Strike(Primitive primitive, boolean mirrored, boolean spin, double step, boolean quick, boolean heavy, int tell, double damage) {
		public Shape shape() { return spin ? Shape.CIRCLE : primitive.shape(); }
		public double reach() { return spin ? Math.min(primitive.reach(), 3.75) : primitive.reach(); }

		Pose key(int index) {
			Pose base = index == 0 ? primitive.chamber() : index == 1 ? primitive.impact() : primitive.follow();
			float stance = base.stance() + (heavy ? .06F : 0) + (step > 0 ? .08F : 0);
			return mirrored ? new Pose(1, mirror(base.body()), mirror(base.head()), mirror(base.sword()), mirror(base.offhand()), stance, base.bladeTilt())
				: new Pose(1, base.body(), base.head(), base.sword(), base.offhand(), stance, base.bladeTilt());
		}

		/** Whether a target at this offset from the master's feet (along and across the locked aim) is struck. */
		public boolean hits(double forward, double side, double height, boolean crouching) {
			if (!Double.isFinite(forward) || !Double.isFinite(side) || !Double.isFinite(height) || Math.abs(height) > 2.5) return false;
			double reach = reach();
			return switch (shape()) {
				case CIRCLE -> forward * forward + side * side <= reach * reach;
				case LANE -> forward >= 0 && forward <= reach && Math.abs(side) <= primitive.width();
				case ARC, ARC_LOW, ARC_HIGH -> forward >= 0 && forward <= reach && Math.abs(side) <= Math.min(3.0, .8 + forward * .8)
					&& (shape() != Shape.ARC_LOW || height <= .6) && (shape() != Shape.ARC_HIGH || !crouching);
			};
		}
	}

	public record Technique(int id, String key, int school, List<Strike> strikes, int[] impacts, int recovery, double cost) {
		/** Tick (from the start) on which strike {@code index} lands. */
		public int impact(int index) { return impacts[index]; }
		public int lastImpact() { return impacts[impacts.length - 1]; }
		public int duration() { return lastImpact() + recovery; }
		/** How far the opening strike can reach, counting its lunge. */
		public double openingReach() { return strikes.getFirst().reach() + strikes.getFirst().step(); }
		public String translationKey() { return "boss.wildercord.master.technique." + key; }
	}

	private static final java.util.Map<String, Primitive> PRIMITIVES = new java.util.LinkedHashMap<>();
	private static final List<Technique> ALL = new ArrayList<>();
	/** ---- methods-b pack: techniques that hold reserved ids (349-417) and so never shift the positional ids of {@link #ALL}. */
	private static final java.util.Map<Integer, Technique> RESERVED = new java.util.LinkedHashMap<>();

	private static void primitive(String name, Pose[] keys, Shape shape, double reach, double width, double damage) {
		PRIMITIVES.put(name, new Primitive(name, keys[0], keys[1], keys[2], shape, reach, width, damage));
	}

	private static Pose p(Joint body, Joint head, Joint sword, Joint offhand, float stance, float tilt) {
		return new Pose(1, body, head, sword, offhand, stance, tilt);
	}

	private static Joint j(float x, float y, float z) { return new Joint(x, y, z); }

	static {
		// The masters' existing single strikes.
		primitive("SWEEP", MasterAnimationRules.keys(MasterAnimationRules.SWEEP), Shape.ARC, 4, 0, 1);
		primitive("THRUST", MasterAnimationRules.keys(MasterAnimationRules.THRUST), Shape.LANE, 6, .8, 1.05);
		primitive("RISING", MasterAnimationRules.keys(MasterAnimationRules.CRESCENT), Shape.LANE, 4.5, 1.1, 1);
		primitive("DROP", MasterAnimationRules.keys(MasterAnimationRules.BREAK_CAST), Shape.LANE, 4, .8, .95);
		primitive("LOW", MasterAnimationRules.keys(MasterAnimationRules.CINDER_WAKE), Shape.ARC_LOW, 4, 0, .95);
		primitive("PLUNGE", MasterAnimationRules.keys(-1), Shape.LANE, 3.5, .9, .95);
		primitive("DRIVE", MasterAnimationRules.keys(MasterAnimationRules.PURSUIT_BREAK), Shape.LANE, 4.5, .8, 1);
		primitive("REPLY", MasterAnimationRules.keys(MasterAnimationRules.CROSSWIND_REPRISE), Shape.LANE, 5, .75, 1);
		primitive("CLEAVE", MasterAnimationRules.keys(MasterAnimationRules.STONE_FRACTURE), Shape.LANE, 5, 1, 1.15);
		primitive("WHIRL", MasterAnimationRules.keys(MasterAnimationRules.KILN_RING), Shape.CIRCLE, 3.5, 0, 1);
		// New strikes. Same rig conventions: radians, negative sword X raises the blade arm forward.
		// High-right to low-left diagonal.
		primitive("FALLING", new Pose[] {
			p(j(-.08F, .45F, -.03F), j(.03F, 0, 0), j(-2.55F, .55F, -.55F), j(-.70F, -.20F, -.35F), .20F, 10),
			p(j(.20F, -.30F, .04F), j(-.08F, 0, 0), j(-1.05F, -.70F, -.25F), j(-.50F, .25F, -.45F), .20F, 0),
			p(j(.16F, -.55F, .04F), j(-.04F, 0, 0), j(-.55F, -.95F, -.15F), j(-.36F, .20F, -.40F), .20F, -5)},
			Shape.LANE, 4.5, 1.1, 1);
		// A flat cut at head height: crouch under it.
		primitive("HIGH", new Pose[] {
			p(j(-.02F, .70F, -.02F), j(.02F, 0, 0), j(-2.25F, .95F, -.80F), j(-.90F, -.30F, -.30F), .16F, 0),
			p(j(.05F, -.40F, .02F), j(-.02F, 0, 0), j(-2.05F, -1.00F, -.65F), j(-.62F, .28F, -.50F), .16F, 0),
			p(j(.02F, -.80F, .02F), j(0, 0, 0), j(-1.60F, -1.25F, -.45F), j(-.40F, .22F, -.42F), .16F, 0)},
			Shape.ARC_HIGH, 4.5, 0, 1);
		// A long, low lunge to the point.
		primitive("LUNGE", new Pose[] {
			p(j(-.15F, .40F, 0), j(.04F, 0, 0), j(-1.10F, .45F, -.08F), j(-1.00F, -.10F, -.30F), .38F, -72),
			p(j(.45F, -.12F, .02F), j(-.25F, 0, 0), j(-1.62F, -.02F, -.02F), j(.45F, .12F, -.35F), .52F, -84),
			p(j(.30F, -.18F, .02F), j(-.16F, 0, 0), j(-1.48F, -.06F, -.06F), j(.10F, .10F, -.30F), .46F, -76)},
			Shape.LANE, 7, .7, 1.05);
		// A vertical rising cut from the knee.
		primitive("UPPER", new Pose[] {
			p(j(.30F, .05F, 0), j(-.10F, 0, 0), j(.25F, .15F, .15F), j(-.90F, -.20F, -.30F), .30F, -15),
			p(j(-.12F, 0, 0), j(.05F, 0, 0), j(-2.35F, .05F, -.10F), j(-.50F, -.10F, -.40F), .24F, 5),
			p(j(-.18F, 0, 0), j(.08F, 0, 0), j(-2.85F, 0, -.08F), j(-.36F, -.08F, -.36F), .22F, 10)},
			Shape.LANE, 3.5, 1, 1);
		// A shove with the open hand while the blade waits back: short, light, fast.
		primitive("PALM", new Pose[] {
			p(j(-.05F, .35F, 0), j(.02F, 0, 0), j(-.60F, .50F, -.40F), j(-.60F, -.40F, -.20F), .22F, -20),
			p(j(.25F, -.25F, 0), j(-.10F, 0, 0), j(-.40F, .30F, -.50F), j(-1.65F, .15F, -.05F), .30F, -20),
			p(j(.18F, -.30F, 0), j(-.06F, 0, 0), j(-.45F, .25F, -.45F), j(-1.40F, .10F, -.10F), .28F, -20)},
			Shape.LANE, 2.75, 1, .6);

		school(MastersRules.EMBER, """
			kindling_cut SWEEP ~SWEEP
			ember_cross FALLING ~FALLING
			rising_flame LOW UPPER
			searing_lunge ^LUNGE
			hearth_breaker !CLEAVE
			cinder_flurry *SWEEP *~SWEEP *THRUST
			flare_step ^FALLING ~SWEEP
			scorch_line DRIVE THRUST
			wildfire_turn @SWEEP
			bellows_drive PALM ^LUNGE
			brand_and_burn HIGH LOW
			kiln_spiral WHIRL
			spark_scatter *THRUST *THRUST *THRUST
			blaze_crescent RISING ~FALLING
			coalfall UPPER !CLEAVE
			furnace_gate ~SWEEP SWEEP !THRUST
			smoke_feint *PALM ~HIGH
			ashen_reply REPLY ~SWEEP
			molten_arc @~SWEEP SWEEP
			pyre_crash ^^CLEAVE
			lantern_sweep LOW ~LOW
			flash_point *DROP ^LUNGE
			burning_wheel WHIRL @~SWEEP
			tinder_snap *FALLING *~FALLING *UPPER
			crown_of_flame HIGH ~HIGH !CLEAVE
			ember_rain PLUNGE PLUNGE
			firebrand_rush ^DRIVE ^THRUST
			forge_hammer !CLEAVE !CLEAVE
			dragon_coil LOW @SWEEP
			flicker_cut *~SWEEP *SWEEP
			slag_turn ~RISING @SWEEP
			sunset_draw RISING HIGH
			inferno_chain SWEEP ~FALLING THRUST !UPPER
			last_cinder ^FALLING ~FALLING @SWEEP !CLEAVE
			""");
		school(MastersRules.GALE, """
			zephyr_point ^THRUST
			crosswind_pair *THRUST *~THRUST
			gust_cut *SWEEP
			swallow_turn ~HIGH SWEEP
			jade_needle *THRUST *THRUST
			kite_string ^^LUNGE
			squall_line *SWEEP *~SWEEP *SWEEP
			updraft LOW UPPER
			feather_fall UPPER DROP
			tailwind_rush ^DRIVE *THRUST
			cyclone @SWEEP @~SWEEP
			hawk_dive ^PLUNGE
			reed_bend *LOW *HIGH
			whistling_reply REPLY REPLY
			skyward_cross RISING ~RISING
			gale_lattice *FALLING *~FALLING *THRUST
			drifting_palm PALM *THRUST
			vortex WHIRL
			sparrow_flurry *THRUST *~SWEEP *THRUST *SWEEP
			headwind HIGH ~HIGH
			shear ~SWEEP ^LUNGE
			dust_devil LOW @~SWEEP
			storm_eye *PALM WHIRL
			thousand_leaves *SWEEP *~SWEEP *SWEEP *~SWEEP
			needle_rain *DROP *DROP *THRUST
			wind_rider ^^DRIVE
			crane_wing ~RISING HIGH
			slipstream ^*FALLING ^*~FALLING
			tempest_spiral @SWEEP @SWEEP
			breeze_feint *HIGH ^LUNGE
			cloud_split UPPER !CLEAVE
			howling_cross FALLING ~FALLING !THRUST
			last_gale ^THRUST *~SWEEP @SWEEP ^LUNGE
			""");
		school(MastersRules.STONE, """
			boulder_drop !CLEAVE
			bedrock_sweep !SWEEP
			quarry_cut FALLING ~FALLING
			landslide LOW !LOW
			obelisk_thrust !THRUST
			cairn_breaker UPPER !CLEAVE
			mountain_turn !WHIRL
			ridge_line SWEEP !THRUST
			granite_palm PALM !CLEAVE
			avalanche_step ^CLEAVE
			fault_cross !FALLING !~FALLING
			iron_root LOW ~LOW !UPPER
			crag_hammer !DROP !DROP
			monolith_drive ^DRIVE
			gravel_spin @SWEEP
			tectonic_shift ~SWEEP SWEEP @!SWEEP
			keystone HIGH !CLEAVE
			cliff_face !RISING
			basalt_wall ~HIGH HIGH
			marble_reply !REPLY
			sediment PLUNGE !PLUNGE
			quake_lunge ^^LUNGE
			rockslide_chain SWEEP ~SWEEP SWEEP
			anvil_fall !UPPER !CLEAVE
			tor_spiral WHIRL !CLEAVE
			shale_split *FALLING !THRUST
			bastion PALM !SWEEP
			rift_open !LOW !RISING
			stone_rain DROP DROP DROP
			menhir !THRUST !THRUST
			deep_quarry ^CLEAVE @!SWEEP
			earthsplitter !CLEAVE ~FALLING !CLEAVE
			last_stone LOW !UPPER @SWEEP !CLEAVE
			""");
		// ---- masters-a pack: Rime, Thunder, Verdant and Hollow tables live in ElementalMasters.
		for (int school : ElementalMasters.SCHOOL_IDS) school(school, ElementalMasters.techniques(school));
		// ---- masters-b pack
		MastersPackB.techniques(MasterTechniques::school);
		// ---- methods-a pack
		MethodsAMasters.techniques(MasterTechniques::school);
	}

	/** Tempo per school: Gale is quickest, Stone slowest and heaviest. */
	public static double tempo(int school) {
		return school == MastersRules.GALE ? .85 : school == MastersRules.STONE ? 1.12 : 1;
	}

	private static void school(int school, String table) { school(school, table, 0); }

	/** With {@code firstId > 0} the table's techniques take ids from there (a reserved range); returns the next free id. */
	private static int school(int school, String table, int firstId) {
		int nextId = firstId;
		for (String line : table.strip().split("\n")) {
			String[] parts = line.strip().split("\\s+");
			int count = parts.length - 1;
			if (count < 1 || count > 4) throw new IllegalStateException("A technique has one to four strikes: " + line);
			double share = count == 1 ? .9 : count == 2 ? .6 : count == 3 ? .5 : .42;
			List<Strike> strikes = new ArrayList<>();
			int[] impacts = new int[count];
			int at = 0;
			boolean lastHeavy = false;
			for (int i = 0; i < count; i++) {
				String token = parts[i + 1];
				boolean mirrored = false, spin = false, quick = false, heavy = false;
				double step = 0;
				int c = 0;
				for (; c < token.length() && "~@*!^".indexOf(token.charAt(c)) >= 0; c++) {
					switch (token.charAt(c)) {
						case '~' -> mirrored = true;
						case '@' -> spin = true;
						case '*' -> quick = true;
						case '!' -> heavy = true;
						default -> step += STEP;
					}
				}
				Primitive primitive = PRIMITIVES.get(token.substring(c));
				if (primitive == null) throw new IllegalStateException("Unknown strike in " + line);
				if (primitive.name().equals("WHIRL")) spin = true;
				int tell = (i == 0 ? FIRST_TELL : FOLLOW_TELL) + (heavy ? 4 : 0) - (quick ? 3 : 0) + (step > 0 ? 2 : 0) + (spin ? 2 : 0);
				tell = Math.max(MIN_TELL, (int) Math.round(tell * tempo(school)));
				if (ElementalMasters.owns(school)) tell = ElementalMasters.tell(heavy, spin, step > 0); // ---- masters-a pack
				if (MastersPackB.owns(school)) tell = MastersPackB.tell(i, tell); // ---- masters-b pack
				if (MethodsAMasters.owns(school)) tell = MethodsAMasters.tell(i, tell); // ---- methods-a pack
				double damage = share * primitive.damage() * (heavy ? 1.3 : 1) * (quick ? .85 : 1);
				strikes.add(new Strike(primitive, mirrored, spin, step, quick, heavy, tell, damage));
				at += tell;
				impacts[i] = at;
				lastHeavy = heavy;
			}
			// Heavy openers can stack past what a single read should cost; keep every chain under the cap.
			double total = strikes.stream().mapToDouble(Strike::damage).sum();
			if (total > MAX_SHARE) strikes.replaceAll(strike -> new Strike(strike.primitive(), strike.mirrored(), strike.spin(), strike.step(),
				strike.quick(), strike.heavy(), strike.tell(), strike.damage() * MAX_SHARE / total));
			int recovery = 12 + 3 * (count - 1) + (lastHeavy ? 4 : 0)
				+ (school == MastersRules.STONE ? 3 : school == MastersRules.GALE ? -2 : 0);
			if (ElementalMasters.owns(school)) recovery = ElementalMasters.recovery(count); // ---- masters-a pack
			if (MastersPackB.owns(school)) recovery = MastersPackB.recovery(recovery); // ---- masters-b pack
			if (MethodsAMasters.owns(school)) recovery = MethodsAMasters.recovery(recovery); // ---- methods-a pack
			Technique technique = new Technique(firstId > 0 ? nextId++ : ALL.size() + 1, parts[0], school, List.copyOf(strikes), impacts, recovery,
				MastersRules.ATTACK_COST + 4 * (count - 1));
			if (firstId <= 0) ALL.add(technique);
			else if (RESERVED.putIfAbsent(technique.id(), technique) != null || technique.id() <= ALL.size())
				throw new IllegalStateException("Technique id " + technique.id() + " is taken"); // ---- methods-b pack
		}
		return nextId;
	}

	public static List<Technique> all() {
		if (RESERVED.isEmpty()) return List.copyOf(ALL);
		List<Technique> all = new ArrayList<>(ALL); // ---- methods-b pack
		all.addAll(RESERVED.values());
		return List.copyOf(all);
	}

	public static List<Technique> forSchool(int school) {
		return all().stream().filter(technique -> technique.school() == school).toList();
	}

	/** IDs run from 1; 0 and anything unknown are null. */
	public static Technique byId(int id) {
		return id >= 1 && id <= ALL.size() ? ALL.get(id - 1) : RESERVED.get(id); // ---- methods-b pack: or a reserved id
	}

	/** The classic-rig pose of a technique at this many ticks since it began; NONE outside it. */
	public static Pose sample(int id, float age) {
		Technique technique = byId(id);
		if (technique == null || !Float.isFinite(age) || age < 0 || age >= technique.duration()) return MasterAnimationRules.NONE;
		var strikes = technique.strikes();
		int n = strikes.size(), i = 0;
		while (i < n && age >= technique.impact(i)) i++;
		Pose pose;
		if (i < n) {
			Strike strike = strikes.get(i);
			float start = i == 0 ? 0 : technique.impact(i - 1), hit = technique.impact(i), length = hit - start;
			float chamberAt = start + length * .6F;
			if (age >= chamberAt) pose = strike.key(0).toward(strike.key(1), smooth((age - chamberAt) / (hit - chamberAt)));
			else if (i == 0) pose = strike.key(0);
			else {
				// Finish the last cut's follow-through, then gather into the next chamber.
				Strike previous = strikes.get(i - 1);
				float settle = start + length * .2F;
				pose = age < settle ? previous.key(1).toward(previous.key(2), smooth((age - start) / (settle - start)))
					: previous.key(2).toward(strike.key(0), smooth((age - settle) / (chamberAt - settle)));
			}
		} else {
			Strike last = strikes.get(n - 1);
			float hit = technique.lastImpact(), follow = hit + Math.min(4, technique.recovery() * .3F);
			pose = age < follow ? last.key(1).toward(last.key(2), smooth((age - hit) / (follow - hit))) : last.key(2);
		}
		float followEnd = technique.lastImpact() + Math.min(4, technique.recovery() * .3F);
		float weight = smooth(age / Math.max(1, technique.impact(0) * .5F))
			* (1 - smooth((age - followEnd) / Math.max(1, technique.duration() - followEnd)));
		return new Pose(weight, pose.body(), pose.head(), pose.sword(), pose.offhand(), pose.stance(), pose.bladeTilt(), yaw(technique, age));
	}

	/** Whole turns through each spinning strike; a completed turn is the same as none, so it never unwinds. */
	public static float yaw(Technique technique, float age) {
		float yaw = 0;
		for (int i = 0; i < technique.strikes().size(); i++) {
			Strike strike = technique.strikes().get(i);
			if (!strike.spin()) continue;
			float hit = technique.impact(i), start = hit - strike.tell() * .7F, end = hit + 2;
			float turn = smooth((age - start) / (end - start));
			if (turn > 0 && turn < 1) yaw += (strike.mirrored() ? 1 : -1) * (float) (Math.PI * 2) * turn;
		}
		return yaw;
	}

	private static Joint mirror(Joint joint) { return new Joint(joint.x(), -joint.y(), -joint.z()); }
	private static float clamp(float value) { return Float.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0; }
	private static float smooth(float value) { float t = clamp(value); return t * t * (3 - 2 * t); }

	// ---- methods-b pack: Echo, Dawn and Venom hold the reserved ids 349-417 (after methods-a's 283-348), whatever loads first.
	static {
		int id = MethodsBMasters.FIRST_TECHNIQUE;
		id = school(MethodsBMasters.ECHO, MethodsBMasters.ECHO_TABLE, id);
		id = school(MethodsBMasters.DAWN, MethodsBMasters.DAWN_TABLE, id);
		id = school(MethodsBMasters.VENOM, MethodsBMasters.VENOM_TABLE, id);
		if (id - 1 != MethodsBMasters.LAST_TECHNIQUE) throw new IllegalStateException("Echo, Dawn and Venom must fill ids 349-417, not to " + (id - 1));
	}
}
