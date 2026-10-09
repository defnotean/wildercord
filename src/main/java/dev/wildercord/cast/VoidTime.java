package dev.wildercord.cast;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Small pieces of state shared by the void and time runes: the once-per-window guard that keeps a lingering
 * effect from stacking under Zone, Pulse, Linger and Echo, what Anchor holds fast, Veil's ambush and Shadowstep's
 * backstab, Hush's pockets, and the darkness Eclipse makes. Nothing here is saved: everything lasts seconds and a
 * restart ends it.
 */
public final class VoidTime {
	private VoidTime() {}

	private record Landed(long at) {}

	private static final Map<String, Landed> LANDED = new HashMap<>();

	/**
	 * True the first time in {@code every} ticks that this cast's {@code rune} lands on {@code target}: a
	 * lingering effect (a cloud, a well, a rift) laid again by a repeating shape (Zone, Pulse, Linger, Echo) on a
	 * creature it already holds does nothing more, as Magma and Tempest do not.
	 */
	public static boolean once(Cast cast, String rune, Entity target, int every) {
		long now = cast.level.getGameTime();
		String key = rune + ":" + cast.id() + ":" + target.getUUID();
		Landed last = LANDED.get(key);
		if (last != null && now >= last.at() && now - last.at() < every) {
			return false;
		}
		if (LANDED.size() > 512) {
			LANDED.values().removeIf(l -> now - l.at() > 600 || l.at() > now);
		}
		LANDED.put(key, new Landed(now));
		return true;
	}

	/** Same, for a place: true the first time in {@code every} ticks this caster's {@code rune} is laid within 2 blocks of {@code at}. */
	public static boolean onceAt(Cast cast, String rune, Vec3 at, int every) {
		long now = cast.level.getGameTime();
		String key = rune + "@" + cast.id() + ":" + Math.round(at.x / 2) + "," + Math.round(at.y / 2) + "," + Math.round(at.z / 2);
		Landed last = LANDED.get(key);
		if (last != null && now >= last.at() && now - last.at() < every) {
			return false;
		}
		LANDED.put(key, new Landed(now));
		return true;
	}

	// ------------------------------------------------------------------ Anchor

	private static final Map<UUID, Long> ANCHORED = new HashMap<>();

	/** Anchor: nothing a spell does can move {@code t} for {@code ticks}. */
	static void anchor(LivingEntity t, int ticks) {
		if (t instanceof net.minecraft.server.level.ServerPlayer player) dev.wildercord.aura.MasterForms.cancel(player);
		long now = t.level().getGameTime();
		ANCHORED.merge(t.getUUID(), now + ticks, Math::max);
		if (ANCHORED.size() > 256) {
			ANCHORED.values().removeIf(u -> u < now);
		}
	}

	/** Ends Anchor's hold on {@code t} (Starmaw devours it); whether it held. */
	static boolean unanchor(LivingEntity t) {
		return ANCHORED.remove(t.getUUID()) != null;
	}

	/** Whether Anchor holds {@code t} fast right now: no push, pull, banish, swap or portal moves it. */
	public static boolean anchored(Entity t) {
		if (ANCHORED.isEmpty()) {
			return false;
		}
		Long until = ANCHORED.get(t.getUUID());
		return until != null && until >= t.level().getGameTime();
	}

	// ------------------------------------------------------------------ Veil's ambush and Shadowstep's backstab

	private static final Map<UUID, Long> VEILED = new HashMap<>();
	private static final Map<UUID, long[]> STABS = new HashMap<>();
	private static final Map<UUID, UUID> STAB_TARGET = new HashMap<>();
	public static final double AMBUSH = 1.5;
	public static final double BACKSTAB = 1.5;
	public static final int BACKSTAB_TICKS = 60;

	static void veiled(LivingEntity t, int ticks) {
		long now = t.level().getGameTime();
		VEILED.put(t.getUUID(), now + ticks);
		if (VEILED.size() > 256) {
			VEILED.values().removeIf(u -> u < now);
		}
	}

	static void backstab(LivingEntity caster, LivingEntity target) {
		long now = caster.level().getGameTime();
		STABS.put(caster.getUUID(), new long[] {now + BACKSTAB_TICKS});
		STAB_TARGET.put(caster.getUUID(), target.getUUID());
	}

	/**
	 * The multiplier an opening blow gets: the first damage a veiled caster deals (it ends the veil), or the first
	 * blow on the creature a Shadowstep just put them behind.
	 */
	static double opener(Cast cast, LivingEntity target) {
		if (VEILED.isEmpty() && STABS.isEmpty()) {
			return 1.0;
		}
		LivingEntity caster = cast.caster;
		long now = cast.level.getGameTime();
		double bonus = 1.0;
		Long until = VEILED.get(caster.getUUID());
		if (until != null) {
			VEILED.remove(caster.getUUID());
			if (until >= now && caster.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)) {
				caster.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
				bonus *= AMBUSH;
				Vfx.emit(cast.level, net.minecraft.core.particles.ParticleTypes.SQUID_INK, target.getBoundingBox().getCenter(), 8, 0.3, 0.02);
			}
		}
		long[] stab = STABS.get(caster.getUUID());
		if (stab != null && target.getUUID().equals(STAB_TARGET.get(caster.getUUID()))) {
			STABS.remove(caster.getUUID());
			STAB_TARGET.remove(caster.getUUID());
			if (stab[0] >= now) {
				bonus *= BACKSTAB;
				Vfx.emit(cast.level, net.minecraft.core.particles.ParticleTypes.CRIT, target.getBoundingBox().getCenter(), 8, 0.3, 0.2);
			}
		}
		return bonus;
	}

	private static boolean stabbing;

	/** Melee reads the backstab too: a blow struck by hand on the creature a Shadowstep put you behind lands half again as hard. */
	private static void meleeOpener(LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float damage) {
		if (STABS.isEmpty() || stabbing || !(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer attacker)
				|| source.getDirectEntity() != attacker || !(target.level() instanceof ServerLevel level)) {
			return;
		}
		long[] stab = STABS.get(attacker.getUUID());
		if (stab == null || !target.getUUID().equals(STAB_TARGET.get(attacker.getUUID()))) {
			return;
		}
		STABS.remove(attacker.getUUID());
		STAB_TARGET.remove(attacker.getUUID());
		if (stab[0] >= level.getGameTime() && target.isAlive()) {
			stabbing = true;
			try {
				Effects.readyToHurt(target);
				target.hurtServer(level, level.damageSources().playerAttack(attacker), (float) (damage * (BACKSTAB - 1.0)));
			} finally {
				stabbing = false;
			}
			Vfx.emit(level, net.minecraft.core.particles.ParticleTypes.CRIT, target.getBoundingBox().getCenter(), 8, 0.3, 0.2);
		}
	}

	// ------------------------------------------------------------------ Chronoshift's ledger of mana spent

	private static final Map<UUID, java.util.ArrayDeque<long[]>> SPENT = new HashMap<>();

	/** Notes mana a player just spent on a spell, for Chronoshift to turn back. */
	public static void spent(net.minecraft.server.level.ServerPlayer player, int mana) {
		if (mana <= 0) {
			return;
		}
		long now = player.level().getGameTime();
		java.util.ArrayDeque<long[]> log = SPENT.computeIfAbsent(player.getUUID(), k -> new java.util.ArrayDeque<>());
		log.addLast(new long[] {now, mana});
		while (!log.isEmpty() && now - log.peekFirst()[0] > 200) {
			log.removeFirst();
		}
	}

	/** Takes what {@code share} of the mana spent in the last {@code window} ticks comes to (at most {@code max}), forgetting it. */
	static float turnBack(net.minecraft.server.level.ServerPlayer player, double share, float max, int window) {
		java.util.ArrayDeque<long[]> log = SPENT.get(player.getUUID());
		if (log == null) {
			return 0;
		}
		long now = player.level().getGameTime();
		double total = 0;
		for (java.util.Iterator<long[]> it = log.iterator(); it.hasNext(); ) {
			long[] entry = it.next();
			// Not what was paid this very tick (that is the Chronoshift's own price).
			if (entry[0] < now && now - entry[0] <= window) {
				total += entry[1];
				it.remove();
			}
		}
		return (float) Math.min(max, total * share);
	}

	private static final Map<UUID, Long> SKIPPED = new HashMap<>();

	/** Time Skip's skipped second: true (and locks it for 5 s) if the caster has not skipped in the last 5 s. */
	static boolean skipReady(LivingEntity caster) {
		long now = caster.level().getGameTime();
		Long until = SKIPPED.get(caster.getUUID());
		if (until != null && until > now) {
			return false;
		}
		SKIPPED.put(caster.getUUID(), now + 100);
		return true;
	}

	// ------------------------------------------------------------------ Accelerate's tempo

	/** Charge fills this much faster, and Bolts and Arcs fly this much faster, while time runs hurried. */
	public static final double HURRY_CHARGE = 1.4;
	public static final double HURRY_BOLT = 1.5;

	/** Whether {@code who} is running ahead of the clock: Accelerate's Haste III is on them (the client sees it too). */
	public static boolean hurried(net.minecraft.world.entity.Entity who) {
		if (!(who instanceof LivingEntity living)) {
			return false;
		}
		net.minecraft.world.effect.MobEffectInstance haste = living.getEffect(net.minecraft.world.effect.MobEffects.HASTE);
		return haste != null && haste.getAmplifier() >= 2;
	}

	// ------------------------------------------------------------------ Hush

	private record Pocket(ServerLevel level, Vec3 at, double radius, long until, UUID caster) {}

	private static final List<Pocket> POCKETS = new ArrayList<>();

	/** Hush opens a pocket of silence: enemy casters inside it cannot cast until it closes. */
	static void hush(Cast cast, Vec3 at, double radius, int ticks) {
		long now = cast.level.getGameTime();
		POCKETS.removeIf(p -> p.until() < now);
		if (POCKETS.size() < 64) {
			POCKETS.add(new Pocket(cast.level, at, radius, now + ticks, cast.caster.getUUID()));
		}
	}

	/**
	 * Whether {@code who} stands in a Hush cast by someone who may harm them: a spell they cast fails. (The seam for
	 * a general Silenced state: whoever owns that can call this from the same place.)
	 */
	public static boolean hushed(LivingEntity who) {
		if (POCKETS.isEmpty()) {
			return false;
		}
		long now = who.level().getGameTime();
		for (Pocket p : POCKETS) {
			if (p.until() >= now && p.level() == who.level() && p.at().distanceTo(who.position()) <= p.radius() + who.getBbWidth() / 2
					&& !p.caster().equals(who.getUUID())) {
				Entity owner = p.level().getEntity(p.caster());
				if (owner != null && Targets.canHarm((LivingEntity) owner, who)) {
					return true;
				}
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ Eclipse's darkness

	/** How long an Eclipse's darkness lasts after a pulse (a little longer than the pulse gap). */
	public static final int DARK_TICKS = 25;
	private static final Map<UUID, Long> DARKENED = new HashMap<>();

	static void darken(LivingEntity t) {
		long now = t.level().getGameTime();
		DARKENED.put(t.getUUID(), now + DARK_TICKS);
		if (DARKENED.size() > 256) {
			DARKENED.values().removeIf(u -> u < now);
		}
	}

	/** Whether light counts as dim at {@code t}: an Eclipse is over it. */
	public static boolean darkened(Entity t) {
		Long until = DARKENED.get(t.getUUID());
		return until != null && until >= t.level().getGameTime();
	}

	// ------------------------------------------------------------------ Blind, Hex and Wither's hold on a mob or a wound

	/** Tag on Shades' hounds: their bites leave Shadowed. */
	public static final String SHADE_TAG = "wildercord.shade";
	private static final Map<UUID, Long> WITHERED = new HashMap<>();

	static void withered(LivingEntity t, int ticks) {
		long now = t.level().getGameTime();
		WITHERED.put(t.getUUID(), now + ticks);
		if (WITHERED.size() > 256) {
			WITHERED.values().removeIf(u -> u < now);
		}
	}

	/** A blinded monster swings at the nearest creature beside it (never you or your side), or at nothing, for {@code ticks}. */
	static void lashOut(Cast cast, net.minecraft.world.entity.Mob mob, int ticks) {
		if (Spirits.isBoss(mob)) {
			mob.setTarget(null);
			return;
		}
		for (int t = 0; t < ticks; t += 20) {
			Scheduler.later(Math.max(1, t), () -> {
				if (!mob.isAlive() || mob.level() != cast.level) {
					return;
				}
				net.minecraft.world.entity.Mob next = null;
				double best = 36.0;
				for (net.minecraft.world.entity.Mob other : cast.level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, mob.getBoundingBox().inflate(6.0),
						m -> m != mob && m.isAlive() && Targets.canHarm(cast.caster, m))) {
					double d = other.distanceToSqr(mob);
					if (d < best) {
						best = d;
						next = other;
					}
				}
				mob.setTarget(next);
			});
		}
	}

	/** Hex's price: the hexed monster fixes on whoever hexed it for {@code ticks}. */
	static void fixate(Cast cast, net.minecraft.world.entity.Mob mob, int ticks) {
		if (Spirits.isBoss(mob) || cast.caster == mob) {
			return;
		}
		for (int t = 0; t < ticks; t += 20) {
			Scheduler.later(Math.max(1, t), () -> {
				if (mob.isAlive() && mob.level() == cast.level && cast.caster.isAlive() && mob.getTarget() != cast.caster) {
					mob.setTarget(cast.caster);
				}
			});
		}
	}

	private static void afterDamage(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float damage) {
		if (!WITHERED.isEmpty()) {
			Long until = WITHERED.get(entity.getUUID());
			if (until != null && source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker && attacker != entity
					&& until >= entity.level().getGameTime()) {
				// The rot spreads to whatever strikes it in melee.
				attacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 60, 0, false, true));
			}
		}
		if (source.getEntity() instanceof net.minecraft.world.entity.animal.wolf.Wolf wolf && wolf.entityTags().contains(SHADE_TAG)) {
			Reactions.mark(entity, Reactions.Mark.SHADOWED, 100);
		}
	}

	static void init() {
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, damage, blocked) -> {
			if (damage > 0) {
				meleeOpener(entity, source, damage);
				afterDamage(entity, source, damage);
			}
		});
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SPENT.remove(handler.player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			LANDED.clear();
			ANCHORED.clear();
			VEILED.clear();
			STABS.clear();
			STAB_TARGET.clear();
			POCKETS.clear();
			WITHERED.clear();
			SKIPPED.clear();
			SPENT.clear();
			DARKENED.clear();
		});
	}
}
