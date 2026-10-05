package dev.wildercord.aura.world;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Supplier;

/** Internal pursuit-only observation. Existing damage, guards, wards and post-hit restoration remain authoritative. */
public final class MasterHitReceipt {
	private MasterHitReceipt() {}
	private static final ThreadLocal<Pending> CURRENT = new ThreadLocal<>();

	record Result(float netHealthLost, float healthLost, float absorptionLost, boolean observed) {
		boolean damaging() { return observed && MasterPursuitRules.resolvedDamage(healthLost, absorptionLost); }
	}

	private static final class Pending {
		final SwordMaster master;
		final LivingEntity target;
		DamageSource source;
		float healthLost, absorptionLost;
		boolean observed, observing;
		Pending(SwordMaster master, LivingEntity target) { this.master = master; this.target = target; }
	}

	/** Bind the exact projected source before element reactions can trigger other nested damage. */
	static void source(AuraFighter master, LivingEntity target, DamageSource source) {
		Pending pending = CURRENT.get();
		if (pending != null && pending.master == master && pending.target == target && pending.source == null) pending.source = source;
	}

	/** An opaque token used only by the native Player.actuallyHurt observation hook. */
	public static final class Observation {
		private final Pending pending;
		private final float health, absorption;
		private Observation(Pending pending) {
			this.pending = pending;
			health = pending.target.getHealth(); absorption = pending.target.getAbsorptionAmount();
		}
	}

	/** Snapshot immediately before the matching native damage operation, after earlier element/ward reactions. */
	public static Observation begin(LivingEntity target, DamageSource source) {
		Pending pending = CURRENT.get();
		if (pending == null || pending.observed || pending.observing || target != pending.target || source != pending.source) return null;
		pending.observing = true;
		return new Observation(pending);
	}

	/** Capture before death prevention, totems or AFTER_DAMAGE healing can conceal a real wound. Never changes damage. */
	public static void finish(Observation observation) {
		if (observation == null) return;
		Pending pending = observation.pending;
		pending.observing = false;
		pending.observed = true;
		pending.healthLost = Math.max(0, observation.health - Math.max(0, pending.target.getHealth()));
		pending.absorptionLost = Math.max(0, observation.absorption - pending.target.getAbsorptionAmount());
	}

	/** Nested measurements have their own scope; rejected hits cannot inherit another receipt, even after exceptions. */
	static Result measure(SwordMaster master, LivingEntity target, Supplier<Float> damage) {
		Pending previous = CURRENT.get(), pending = new Pending(master, target);
		CURRENT.set(pending);
		try {
			float net = damage.get();
			return new Result(net, pending.healthLost, pending.absorptionLost, pending.observed);
		} finally {
			if (previous == null) CURRENT.remove();
			else CURRENT.set(previous);
		}
	}
}
