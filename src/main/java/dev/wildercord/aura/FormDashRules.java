package dev.wildercord.aura;

/**
 * Pure numbers for the two grounded field forms that share the one Master-form slot with Wall Turn.
 * Cinder Lunge (Ember) closes a gap behind a planted tell and ends in one cut; Reed Slip (Gale) gives
 * a short off-line step that keeps facing. Neither has invulnerability, and both end in an exposed recovery.
 */
public final class FormDashRules {
	private FormDashRules() {}
	public static final int CINDER_LUNGE = 2, REED_SLIP = 3;
	/** A compile-time constant, so decoding a saved record never has to initialise FormDash first. Bits 5-10 are the moves pack's; 4 stays unused. */
	public static final int KNOWN = 1 << CINDER_LUNGE | 1 << REED_SLIP | 1 << 5 | 1 << 6 | 1 << 7 | 1 << 8 | 1 << 9 | 1 << 10;
	/** Phases on the wire. IDLE matches Wall Turn's so an empty view reads the same. */
	public static final int IDLE = 0, SET = 1, LUNGE = 2, CUT = 3, SLIP = 4, STALL = 5, RECOVER = 6, ABORT = 7;
	public static final int CINDER_COST = 25, CINDER_SET_TICKS = 5, CINDER_TICKS = 6, CINDER_REST = 100, CINDER_RECOVERY = 16, STALL_RECOVERY = 24;
	public static final double CINDER_DISTANCE = 5, CUT_REACH = 2.6, CUT_HALF = .9, CUT_SCALE = 1.25;
	public static final int REED_COST = 12, REED_TICKS = 3, REED_REST = 50, REED_RECOVERY = 14;
	public static final double REED_DISTANCE = 2.5;
	/** A half-slab or stair riser may be climbed during travel; anything taller ends it. */
	public static final double STEP = .5625;
	public static final int MAX_TICKS = 30;
	/** The longest rest and recovery one use can owe; a loaded record never holds more than this from now. */
	public static final int MAX_REST = Math.max(Math.max(CINDER_REST, REED_REST), 120), MAX_RECOVERY = Math.max(STALL_RECOVERY, Math.max(CINDER_RECOVERY, REED_RECOVERY));
	/** Footing is checked at least this often along a stride, so a one-block gap cannot be stepped over. */
	public static final double FOOTING_PROBE = .25;

	public static boolean form(int form) { return form == CINDER_LUNGE || form == REED_SLIP || form >= AIR_STEP && form <= SPELL_CUT; }
	public static boolean phase(int phase) { return phase >= IDLE && phase <= MISS; }
	/** Cinder Lunge and Plunging Strike need a Sovereign; the rest are Form-stage lessons. Each needs its school's Master clear. */
	public static boolean eligible(int form, int stage, boolean cleared) {
		return cleared && form(form) && stage >= (form == CINDER_LUNGE || form == PLUNGE ? AuraRules.SOVEREIGN : AuraRules.FORM);
	}
	public static int school(int form) {
		return switch (form) {
			case CINDER_LUNGE, RIPOSTE, SPELL_CUT -> dev.wildercord.aura.world.MastersRules.EMBER;
			case PLUNGE, GUARD_BREAK -> dev.wildercord.aura.world.MastersRules.STONE;
			default -> dev.wildercord.aura.world.MastersRules.GALE;
		};
	}
	public static int cost(int form) {
		return switch (form) {
			case CINDER_LUNGE -> CINDER_COST; case AIR_STEP -> AIR_STEP_COST; case PLUNGE -> PLUNGE_COST; case RIPOSTE -> RIPOSTE_COST;
			case SHOVE -> SHOVE_COST; case GUARD_BREAK -> GUARD_BREAK_COST; case SPELL_CUT -> dev.wildercord.spell.SpellCutRules.COUNTER_COST;
			default -> REED_COST;
		};
	}
	/** The slot's shared rest. Follow-ups are gated by the parry window and Spell Cut by its recovery instead. */
	public static int rest(int form) {
		return switch (form) { case CINDER_LUNGE -> CINDER_REST; case REED_SLIP -> REED_REST; case AIR_STEP -> AIR_STEP_REST; case PLUNGE -> PLUNGE_REST; default -> 0; };
	}
	public static int recovery(int form, boolean stalled) {
		if (stalled) return form == REED_SLIP ? REED_RECOVERY : STALL_RECOVERY;
		return switch (form) {
			case CINDER_LUNGE -> CINDER_RECOVERY; case RIPOSTE -> RIPOSTE_RECOVERY; case SHOVE -> SHOVE_RECOVERY; case GUARD_BREAK -> GUARD_BREAK_RECOVERY;
			case AIR_STEP -> AIR_LAND_RECOVERY; case PLUNGE -> PLUNGE_RECOVERY; default -> REED_RECOVERY;
		};
	}
	public static int travelTicks(int form) {
		return switch (form) { case CINDER_LUNGE -> CINDER_TICKS; case REED_SLIP -> REED_TICKS; case RIPOSTE -> RIPOSTE_TICKS; case AIR_STEP -> AIR_STEP_TICKS; default -> 0; };
	}
	public static double distance(int form) {
		return switch (form) { case CINDER_LUNGE -> CINDER_DISTANCE; case REED_SLIP -> REED_DISTANCE; case RIPOSTE -> RIPOSTE_DISTANCE; default -> 0; };
	}
	/** Even pacing: the per-tick stretch stays well under the swept-step limit. */
	public static double stride(int form) { return travelTicks(form) == 0 ? 0 : distance(form) / travelTicks(form); }
	public static boolean canPay(int form, double aura, long now, long readyAt, long recoveryUntil) {
		return form(form) && now >= readyAt && now >= recoveryUntil && aura >= cost(form);
	}
	/** Field forms only ever send press, release, cancel, equip and unequip; Stone Hinge's equip action is refused here. */
	public static WallTurnRules.Input input() { return new WallTurnRules.Input(WallTurnRules.UNEQUIP); }
	public static int bit(int form) { return form(form) ? 1 << form : 0; }
	/** Only these sit in the one Master-form slot; follow-ups and Spell Cut are learned answers that need no slot. */
	public static boolean slot(int form) { return form == CINDER_LUNGE || form == REED_SLIP || form == AIR_STEP || form == PLUNGE; }

	/**
	 * Reed Slip's world-space lane from the facing yaw (degrees) and the client's move intent. Any forward share
	 * is removed so the slip never closes distance; no intent at all steps straight back. Returns {x, z}.
	 */
	public static double[] slip(float yaw, double intentX, double intentZ) {
		double rad = Math.toRadians(yaw), fx = -Math.sin(rad), fz = Math.cos(rad);
		double x = intentX, z = intentZ, length = Math.sqrt(x * x + z * z);
		if (!Double.isFinite(length) || length < 1.0E-3) return new double[] {-fx, -fz};
		x /= length; z /= length;
		double ahead = x * fx + z * fz;
		if (ahead > 0) { x -= fx * ahead; z -= fz * ahead; }
		length = Math.sqrt(x * x + z * z);
		return length < 1.0E-3 ? new double[] {-fx, -fz} : new double[] {x / length, z / length};
	}

	// ---- moves pack
	/** Aerial slot forms (Gale, Stone), parry follow-ups (one per school), and the Spell Cut counter (Ember). */
	public static final int AIR_STEP = 5, PLUNGE = 6, RIPOSTE = 7, SHOVE = 8, GUARD_BREAK = 9, SPELL_CUT = 10;
	/** Phases on the wire after ABORT: aerial travel and its three distinct ends, a follow-up's blow, and a spell cut or miss. */
	public static final int RISE = 8, DIVE = 9, LAND = 10, SPLASH = 11, STRIKE = 12, SEVER = 13, MISS = 14;
	public static final int AIR_STEP_COST = 15, AIR_STEP_TICKS = 3, AIR_STEP_REST = 60, AIR_LAND_RECOVERY = 8, SPLASH_RECOVERY = 6;
	public static final double AIR_STEP_RISE = .45, AIR_STEP_DRIFT = .3;
	/** An aerial form that has not landed after this long ends as a timeout, owing the stalled recovery. */
	public static final int AIR_TIMEOUT = 60;
	public static final int PLUNGE_COST = 20, PLUNGE_SET_TICKS = 3, PLUNGE_TICKS = 16, PLUNGE_REST = 120, PLUNGE_RECOVERY = 20;
	public static final double PLUNGE_STRIDE = 1.25, PLUNGE_MIN_DROP = 2, PLUNGE_RADIUS = 2.5, PLUNGE_SCALE = .9;
	/** A perfect guard opens this many ticks in which the form key answers with a learned follow-up. */
	public static final int FOLLOW_WINDOW = 16;
	public static final int RIPOSTE_COST = 12, RIPOSTE_SET_TICKS = 3, RIPOSTE_TICKS = 3, RIPOSTE_RECOVERY = 12;
	public static final double RIPOSTE_DISTANCE = 2.4, RIPOSTE_SCALE = 1.1;
	public static final int SHOVE_COST = 10, SHOVE_SET_TICKS = 3, SHOVE_RECOVERY = 10, SHOVE_WEAKNESS = 60;
	public static final double SHOVE_PUSH = 1.1;
	public static final int GUARD_BREAK_COST = 16, GUARD_BREAK_SET_TICKS = 6, GUARD_BREAK_RECOVERY = 18, GUARD_BREAK_SLOW = 40;
	public static final double GUARD_BREAK_SCALE = 1.0, FOLLOW_REACH = 2.8;

	public static boolean aerial(int form) { return form == AIR_STEP || form == PLUNGE; }
	public static boolean followUp(int form) { return form == RIPOSTE || form == SHOVE || form == GUARD_BREAK; }
	/** The still tell before travel or a blow; zero for Reed Slip and Air Step, which answer at once. */
	public static int setTicks(int form) {
		return switch (form) {
			case CINDER_LUNGE -> CINDER_SET_TICKS; case RIPOSTE -> RIPOSTE_SET_TICKS; case SHOVE -> SHOVE_SET_TICKS;
			case GUARD_BREAK -> GUARD_BREAK_SET_TICKS; case PLUNGE -> PLUNGE_SET_TICKS; default -> 0;
		};
	}
	/** Grounded forms that end in a cut once they reach a foe. */
	public static boolean cuts(int form) { return form == CINDER_LUNGE || form == RIPOSTE; }
	public static double cutScale(int form) { return form == RIPOSTE ? RIPOSTE_SCALE : CUT_SCALE; }
	/** The phase a grounded travel ends on. */
	public static int endPhase(int form, boolean stalled) {
		return stalled ? STALL : cuts(form) ? CUT : followUp(form) ? STRIKE : RECOVER;
	}
	/**
	 * The follow-up the player's move intent asks for: forward lunges (Riposte), sideways shoves (Disarm Shove), and still or
	 * back plants a Guard Break. An unlearned choice falls to the next learned one in that order, so one lesson always answers.
	 */
	public static int pick(int learned, double forward, double side) {
		int wanted = Double.isFinite(forward) && forward > .3 ? RIPOSTE : Double.isFinite(side) && Math.abs(side) > .3 ? SHOVE : GUARD_BREAK;
		int[] order = {RIPOSTE, SHOVE, GUARD_BREAK};
		for (int i = 0; i < 3; i++) { int form = order[(wanted - RIPOSTE + i) % 3]; if ((learned & bit(form)) != 0) return form; }
		return 0;
	}
	/** Air Step and Plunging Strike start only off the ground and out of water, once per time in the air. */
	public static boolean airborne(boolean onGround, boolean inFluid, boolean spent) { return !onGround && !inFluid && !spent; }
	/** How an aerial form ended: safe footing, water, or nothing in time. IDLE means still in the air. */
	public static int landing(boolean footing, boolean water, int ticks, int limit) {
		return footing ? LAND : water ? SPLASH : ticks >= limit ? STALL : IDLE;
	}
	/** The phase the set gives way to: Plunging Strike dives, a lunge travels, and the planted follow-ups strike at once. */
	public static int travelPhase(int form) { return form == PLUNGE ? DIVE : travelTicks(form) > 0 ? LUNGE : IDLE; }
	public static int travelSpan(int form) { return form == PLUNGE ? PLUNGE_TICKS : travelTicks(form); }
	public static int landingRecovery(int form, int phase) {
		return phase == LAND ? recovery(form, false) : phase == SPLASH ? SPLASH_RECOVERY : STALL_RECOVERY;
	}
}
