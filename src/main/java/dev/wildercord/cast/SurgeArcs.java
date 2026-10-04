package dev.wildercord.cast;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Surge's static discharge: while the surge lasts, a blow the charged creature lands with its own hand arcs on to the
 * nearest other enemy within 4 blocks for 2 (at most once every 1.5 seconds, and at most 4 arcs to a surge), the way
 * Searing Edge sets what it strikes alight. Fire has a weapon buff; storm's is this.
 */
final class SurgeArcs {
	private SurgeArcs() {}

	static final double DAMAGE = 2.0;
	static final double REACH = 4.0;
	static final int GAP_TICKS = 30;
	static final int MAX_ARCS = 4;

	private static final class Charge {
		Cast cast;
		double power;
		long until;
		long last = Long.MIN_VALUE / 2;
		int arcs;
	}

	private static final Map<UUID, Charge> CHARGED = new HashMap<>();

	/** Surge landed on {@code t}: for {@code ticks} its blows arc. A second Surge renews the first (arcs refill, never stack). */
	static void charge(Cast cast, LivingEntity t, double power, int ticks) {
		Charge c = new Charge();
		c.cast = cast;
		c.power = power;
		long now = cast.level.getGameTime();
		c.until = now + ticks;
		// A charged creature that never strikes is only dropped here: so the table can't grow for ever.
		if (CHARGED.size() > 256) {
			CHARGED.values().removeIf(old -> old.until < now);
		}
		CHARGED.put(t.getUUID(), c);
	}

	/** A blow landed on {@code victim}: arcs on if its striker is charged. */
	static void onBlow(ServerLevel level, LivingEntity victim, DamageSource source, boolean blocked) {
		if (CHARGED.isEmpty() || blocked || !(source.getEntity() instanceof LivingEntity striker) || source.getDirectEntity() != striker
				|| !(source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO))) {
			return;
		}
		Charge c = CHARGED.get(striker.getUUID());
		long now = level.getGameTime();
		if (c == null) {
			return;
		}
		if (now > c.until || c.arcs >= MAX_ARCS || !c.cast.alive()) {
			CHARGED.remove(striker.getUUID(), c);
			return;
		}
		if (now - c.last < GAP_TICKS || c.cast.level != level) {
			return;
		}
		LivingEntity next = null;
		double best = REACH * REACH;
		for (Entity e : level.getEntities(victim, victim.getBoundingBox().inflate(REACH), e -> e instanceof LivingEntity && Targets.canHarm(c.cast.caster, e))) {
			double d = e.distanceToSqr(victim);
			if (e != striker && d < best) {
				best = d;
				next = (LivingEntity) e;
			}
		}
		if (next == null) {
			return;
		}
		c.last = now;
		c.arcs++;
		LivingEntity target = next;
		Vfx.shockArc(level, victim.getBoundingBox().getCenter(), target.getBoundingBox().getCenter());
		Effects.asElement("storm", () -> Effects.lingering(() -> Effects.hurt(c.cast, target,
			level.damageSources().source(DamageTypes.LIGHTNING_BOLT, c.cast.caster), DAMAGE * c.power * Reactions.storm(c.cast, target))));
	}

	static void clear() {
		CHARGED.clear();
	}
}
