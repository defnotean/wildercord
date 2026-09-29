package dev.wildercord.cast;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The fused effects of frost that stay on a creature and answer what happens to it: Frostbloom (whatever
 * strikes the ally is frozen stiff), Geode (whatever strikes the ally is cut by crystal), Black Ice (one
 * that dies while brittle shatters) and Cryostasis (an ally sealed in ice, untouchable, and not again for
 * 10 seconds after). Each is kept by
 * the creature's UUID with the game time it ends, so a lost task, a death, a logout or a restart can't
 * leave one behind, and the listeners cost a map lookup while none are in play.
 */
final class FusedFrostWards {
	private FusedFrostWards() {}

	/** Frostbloom's frost, Geode's shards and Black Ice's shatter reach this far at most. */
	private static final double ANSWER_RANGE = 24.0;
	/** Black Ice's shatter: enemies within this many blocks of the one that died. */
	private static final double SHATTER_RADIUS = 3.0;

	/** A Frostbloom or Geode on an ally: whose, until when, and what it answers a blow with. */
	private static final class Answer {
		final LivingEntity ally;
		final Cast cast;
		final long until;
		final Consumer<Runnable> context;
		/** Frostbloom: how long a striker stays frozen stiff. */
		int slowTicks;
		/** Geode: how hard the shards cut. */
		double damage;
		/** Frostbloom: how many strikers have healed the ally so far (at most {@link FusedFrostRules#BLOOM_HEALS}). */
		int healed;

		Answer(LivingEntity ally, Cast cast, long until) {
			this.ally = ally;
			this.cast = cast;
			this.until = until;
			this.context = FusedFrost.context();
		}

		/** Past its time, or the ally is gone (dead, left, or into another world than the one it was cast in), or so is its caster. */
		boolean over(long now) {
			return now > until || ally.isRemoved() || !ally.isAlive() || ally.level() != cast.level || !cast.alive();
		}
	}

	/** Black Ice's brittle mark: the creature shatters if it dies before {@code until}. */
	private record Brittle(LivingEntity target, Cast cast, long until, double power, Consumer<Runnable> context) {}

	/** A Cryostasis: the ally, where it's held, until when, and its healing still to come. */
	private static final class Seal {
		final LivingEntity ally;
		final Cast cast;
		final Vec3 anchor;
		final long until;
		final float healEach;
		int healsLeft;
		long nextHeal;
		long lastDeflect;
		List<Display.BlockDisplay> shell = List.of();

		Seal(LivingEntity ally, Cast cast, long now, int ticks, double heal) {
			this.ally = ally;
			this.cast = cast;
			this.anchor = ally.position();
			this.until = now + ticks;
			this.healsLeft = FusedFrostRules.sealHeals(ticks);
			this.healEach = FusedFrostRules.sealHealEach(heal, healsLeft);
			this.nextHeal = now + FusedFrostRules.SEAL_HEAL_EVERY;
		}
	}

	/** Whether {@code entity} is sealed in a Cryostasis right now: nothing is cast from inside the ice. */
	public static boolean sealed(Entity entity) {
		Seal seal = SEALS.get(entity.getUUID());
		return seal != null && seal.ally == entity && entity.level().getGameTime() < seal.until;
	}

	private static final Map<UUID, Answer> BLOOMS = new HashMap<>();
	private static final Map<UUID, Answer> GEODES = new HashMap<>();
	private static final Map<UUID, Brittle> BRITTLE = new HashMap<>();
	private static final Map<UUID, Seal> SEALS = new HashMap<>();
	/**
	 * When each creature may be sealed in a Cryostasis again: 10 seconds after its last one ended (or a
	 * seal was refused it), so no caster can keep anyone untouchable by casting it over and over.
	 */
	private static final Map<UUID, Long> THAWING = new HashMap<>();
	/** Set while an answer is being dealt, so an answer never sets off another (two Geodes can't trade shards forever). */
	private static boolean answering;

	static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(FusedFrostWards::allowDamage);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> afterDamage(entity, source, blocked));
		ServerLivingEntityEvents.AFTER_DEATH.register(FusedFrostWards::afterDeath);
		ServerTickEvents.END_SERVER_TICK.register(FusedFrostWards::tick);
		// Every seal opens before the world is saved.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Seal seal : new ArrayList<>(SEALS.values())) {
				seal.shell.forEach(Entity::discard);
			}
			SEALS.clear();
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			BLOOMS.clear();
			GEODES.clear();
			BRITTLE.clear();
			SEALS.clear();
			THAWING.clear();
			answering = false;
		});
	}

	// ------------------------------------------------------------------ putting them on

	/** Frostbloom on {@code ally} for {@code ticks}; returns whether it's new (not a renewal of one it had). */
	static boolean frostbloom(Cast cast, LivingEntity ally, int ticks, int slowTicks) {
		boolean fresh = !running(BLOOMS, ally, cast.level.getGameTime());
		answer(BLOOMS, cast, ally, ticks).slowTicks = slowTicks;
		return fresh;
	}

	/** Geode on {@code ally} for {@code ticks}, its shards cutting for {@code damage}; returns whether it's new. */
	static boolean geode(Cast cast, LivingEntity ally, int ticks, double damage) {
		boolean fresh = !running(GEODES, ally, cast.level.getGameTime());
		answer(GEODES, cast, ally, ticks).damage = damage;
		return fresh;
	}

	private static boolean running(Map<UUID, Answer> answers, LivingEntity ally, long now) {
		Answer old = answers.get(ally.getUUID());
		return old != null && old.ally == ally && now <= old.until;
	}

	/** Puts on (or renews: the newest caster's, lasting whichever ends later) an answer. Never a second copy. */
	private static Answer answer(Map<UUID, Answer> answers, Cast cast, LivingEntity ally, int ticks) {
		long now = cast.level.getGameTime();
		Answer old = answers.get(ally.getUUID());
		long until = now + ticks;
		if (old != null && old.ally == ally) {
			until = Math.max(until, old.until);
		}
		Answer fresh = new Answer(ally, cast, until);
		answers.put(ally.getUUID(), fresh);
		return fresh;
	}

	/** Black Ice: {@code t} is brittle for {@code ticks}; if it dies in that time it shatters. Renewing never shortens it. */
	static void brittle(Cast cast, LivingEntity t, int ticks, double power) {
		long now = cast.level.getGameTime();
		long until = now + ticks;
		Brittle old = BRITTLE.get(t.getUUID());
		if (old != null && old.target() == t) {
			until = Math.max(until, old.until());
		}
		BRITTLE.put(t.getUUID(), new Brittle(t, cast, until, power, FusedFrost.context()));
	}

	/**
	 * Cryostasis: {@code ally} sealed in ice for {@code ticks}, unable to move or be hurt, healing
	 * {@code heal} over the time. One still running on the same ally can't be renewed or stacked, and once
	 * it ends the same creature can't be sealed again for 10 seconds: a seal asked for meanwhile fizzles
	 * (and the wait starts over from then), so it can't be chained into lasting invulnerability.
	 */
	static void cryostasis(Cast cast, LivingEntity ally, int ticks, double heal) {
		ServerLevel level = cast.level;
		long now = level.getGameTime();
		UUID id = ally.getUUID();
		Seal running = SEALS.get(id);
		boolean holding = running != null && running.ally == ally && now < running.until;
		long thawing = THAWING.getOrDefault(id, Long.MIN_VALUE);
		if (holding || !FusedFrostRules.maySeal(thawing, now)) {
			long until = FusedFrostRules.lockedUntil(holding ? FusedFrostRules.lockedUntil(thawing, running.until) : thawing, now);
			THAWING.put(id, until);
			FusedFrostVfx.cryostasisRefused(level, ally);
			int seconds = FusedFrostRules.secondsLeft(until, now);
			Casters.tell(cast.caster, ally == cast.caster
				? Component.translatableWithFallback("message.wildercord.cryostasis_wait_self", "Too soon to seal yourself in ice again (%ss)", seconds)
					.withColor(0x8CDCFF)
				: Component.translatableWithFallback("message.wildercord.cryostasis_wait", "Too soon to seal %s in ice again (%ss)", ally.getDisplayName(),
					seconds).withColor(0x8CDCFF));
			return;
		}
		Seal seal = new Seal(ally, cast, now, ticks, heal);
		SEALS.put(ally.getUUID(), seal);
		Spirits.hold(ally, ticks);
		ally.setDeltaMovement(Vec3.ZERO);
		ally.resetFallDistance();
		ally.needsSync = true;
		if (ally instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
		seal.shell = FusedFrostVfx.cryostasisSeal(level, ally, ticks);
	}

	// ------------------------------------------------------------------ events

	/** Cryostasis: nothing hurts a sealed ally (only what goes through everything: the void, /kill). */
	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (SEALS.isEmpty()) {
			return true;
		}
		Seal seal = SEALS.get(entity.getUUID());
		if (seal == null || seal.ally != entity || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
				|| !(entity.level() instanceof ServerLevel level) || level != seal.cast.level) {
			return true;
		}
		long now = level.getGameTime();
		// Past its time the ice holds nothing, even if the tick that opens it hasn't come yet.
		if (now >= seal.until) {
			return true;
		}
		if (now - seal.lastDeflect >= 5) {
			seal.lastDeflect = now;
			FusedFrostVfx.cryostasisDeflect(level, entity, source.getSourcePosition());
		}
		return false;
	}

	/** Frostbloom and Geode: whatever strikes the ally is answered. */
	private static void afterDamage(LivingEntity entity, DamageSource source, boolean blocked) {
		if (answering || blocked || BLOOMS.isEmpty() && GEODES.isEmpty()) {
			return;
		}
		Answer bloom = BLOOMS.get(entity.getUUID());
		Answer geode = GEODES.get(entity.getUUID());
		if (bloom == null && geode == null || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		// Crystal shards and thorns never answer each other, so two warded creatures can't trade blows forever.
		if (source.is(DamageTypes.THORNS)) {
			return;
		}
		LivingEntity attacker = attacker(source, entity);
		if (attacker == null) {
			return;
		}
		long now = level.getGameTime();
		answering = true;
		try {
			if (bloom != null && live(BLOOMS, bloom, entity, now) && punishable(bloom, attacker)) {
				attacker.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, bloom.slowTicks, 2, false, true), bloom.cast.caster);
				FusedFrostVfx.frostbloomStrike(level, entity, attacker);
				// Being struck is rewarded: each striker frozen heals the ally a point, up to four a cast.
				if (bloom.healed < FusedFrostRules.BLOOM_HEALS) {
					bloom.healed++;
					entity.heal(FusedFrostRules.BLOOM_HEAL);
				}
			}
			if (geode != null && live(GEODES, geode, entity, now) && punishable(geode, attacker)) {
				FusedFrostVfx.geodeShards(level, entity, attacker);
				geode.context.accept(() -> Effects.hurt(geode.cast, attacker, level.damageSources().thorns(entity), geode.damage));
			}
		} finally {
			answering = false;
		}
	}

	/** Whether an answer is still on (dropping it if not). */
	private static boolean live(Map<UUID, Answer> answers, Answer answer, LivingEntity ally, long now) {
		if (answer.ally != ally || answer.over(now)) {
			answers.remove(ally.getUUID(), answer);
			return false;
		}
		return true;
	}

	/** Only an enemy of the ally's caster, in reach, is answered; never the ally itself. */
	private static boolean punishable(Answer answer, LivingEntity attacker) {
		return attacker != answer.ally && attacker.level() == answer.ally.level() && attacker.distanceToSqr(answer.ally) <= ANSWER_RANGE * ANSWER_RANGE
			&& Targets.canHarm(answer.cast.caster, attacker);
	}

	/** Who struck: the creature behind the blow (an archer, a caster) or, failing that, the one that landed it. */
	private static LivingEntity attacker(DamageSource source, LivingEntity victim) {
		LivingEntity found = null;
		if (source.getEntity() instanceof LivingEntity causing) {
			found = causing;
		} else if (source.getDirectEntity() instanceof LivingEntity direct) {
			found = direct;
		}
		return found == null || found == victim || !found.isAlive() ? null : found;
	}

	/** Deaths: the dead lose their wards, and one dying brittle shatters. */
	private static void afterDeath(LivingEntity entity, DamageSource source) {
		UUID id = entity.getUUID();
		if (!BLOOMS.isEmpty()) {
			BLOOMS.remove(id);
		}
		if (!GEODES.isEmpty()) {
			GEODES.remove(id);
		}
		if (BRITTLE.isEmpty()) {
			return;
		}
		Brittle brittle = BRITTLE.remove(id);
		if (brittle == null || brittle.target() != entity || !(entity.level() instanceof ServerLevel level) || level.getGameTime() > brittle.until()) {
			return;
		}
		Vec3 at = entity.getBoundingBox().getCenter();
		// A tick later, clear of the death: a shatter that kills another brittle one sets that one off in turn.
		Scheduler.later(1, () -> brittle.context().accept(() -> shatter(brittle, level, at)));
	}

	/** Black Ice's shatter: shards of it into every enemy within 3 blocks, 4 damage each. */
	private static void shatter(Brittle brittle, ServerLevel level, Vec3 at) {
		Cast cast = brittle.cast();
		if (!cast.alive() || cast.level != level) {
			return;
		}
		FusedFrostVfx.blackIceShatter(level, at, SHATTER_RADIUS);
		int struck = 0;
		for (Entity e : level.getEntities((Entity) null, new AABB(at, at).inflate(SHATTER_RADIUS + 1), e -> Targets.canHarm(cast.caster, e))) {
			if (struck >= FusedFrost.MAX_IN_AREA) {
				break;
			}
			if (e == brittle.target() || e.getBoundingBox().getCenter().distanceTo(at) > SHATTER_RADIUS + e.getBbWidth() / 2) {
				continue;
			}
			LivingEntity t = (LivingEntity) e;
			if (!Statuses.claim(t, "shatter", 20)) {
				continue;
			}
			struck++;
			FusedFrostVfx.blackIceShard(level, at, t);
			Effects.hurt(cast, t, level.damageSources().indirectMagic(cast.caster, cast.caster), 4 * brittle.power());
			// The cold runs on: the survivors are brittle in their turn, so a chain of kills cascades.
			if (t.isAlive()) {
				brittle(cast, t, FusedFrostRules.CASCADE_TICKS, brittle.power());
			}
		}
	}

	// ------------------------------------------------------------------ ticking

	private static void tick(MinecraftServer server) {
		if (!SEALS.isEmpty()) {
			tickSeals();
		}
		int count = server.getTickCount();
		if (count % 20 == 0) {
			if (!BLOOMS.isEmpty()) {
				glow(BLOOMS, true);
			}
			if (!GEODES.isEmpty()) {
				glow(GEODES, false);
			}
			if (!BRITTLE.isEmpty()) {
				tickBrittle();
			}
		}
		if (count % 100 == 0 && !THAWING.isEmpty()) {
			long now = server.overworld().getGameTime();
			THAWING.values().removeIf(until -> FusedFrostRules.maySeal(until, now));
		}
	}

	/** A seal is over (however it ended): the same creature can't be sealed again for 10 seconds from {@code now}. */
	private static void thaw(Seal seal, long now) {
		THAWING.merge(seal.ally.getUUID(), FusedFrostRules.lockedUntil(Long.MIN_VALUE, now), Math::max);
	}

	/** Once a second: each ward's glimmer, and the ended ones dropped. */
	private static void glow(Map<UUID, Answer> answers, boolean bloom) {
		for (Iterator<Answer> it = answers.values().iterator(); it.hasNext(); ) {
			Answer answer = it.next();
			if (!(answer.ally.level() instanceof ServerLevel level) || answer.over(level.getGameTime())) {
				it.remove();
				continue;
			}
			if (bloom) {
				FusedFrostVfx.frostbloomAura(level, answer.ally);
			} else {
				FusedFrostVfx.geodeAura(level, answer.ally);
			}
		}
	}

	/** Once a second: the cracks on each brittle creature, and the ended marks dropped. */
	private static void tickBrittle() {
		for (Iterator<Brittle> it = BRITTLE.values().iterator(); it.hasNext(); ) {
			Brittle brittle = it.next();
			LivingEntity t = brittle.target();
			if (t.isRemoved() || !t.isAlive() || !(t.level() instanceof ServerLevel level) || level.getGameTime() > brittle.until()) {
				it.remove();
				continue;
			}
			FusedFrostVfx.brittle(level, t);
		}
	}

	/** Every tick: sealed allies held where they were sealed and healed; seals past their time opened. */
	private static void tickSeals() {
		List<Seal> opened = new ArrayList<>();
		for (Iterator<Seal> it = SEALS.values().iterator(); it.hasNext(); ) {
			Seal seal = it.next();
			LivingEntity ally = seal.ally;
			// Gone, or carried into another world (a portal, a command): the ice holds nothing there, and its
			// anchor is a place in the world it was cast in, never to be pulled to in this one.
			if (ally.isRemoved() || !ally.isAlive() || !(ally.level() instanceof ServerLevel level) || level != seal.cast.level) {
				it.remove();
				thaw(seal, ally.level().getGameTime());
				seal.shell.forEach(Entity::discard);
				continue;
			}
			long now = level.getGameTime();
			if (now >= seal.until || !seal.cast.alive()) {
				it.remove();
				thaw(seal, now);
				opened.add(seal);
				continue;
			}
			ally.setDeltaMovement(Vec3.ZERO);
			if (ally.position().distanceToSqr(seal.anchor) > 0.0025) {
				ally.teleportTo(level, seal.anchor.x, seal.anchor.y, seal.anchor.z, Set.<Relative>of(), ally.getYRot(), ally.getXRot(), false);
			}
			ally.resetFallDistance();
			ally.needsSync = true;
			if (now >= seal.nextHeal && seal.healsLeft > 0) {
				ally.heal(seal.healEach);
				seal.healsLeft--;
				seal.nextHeal += FusedFrostRules.SEAL_HEAL_EVERY;
			}
			if ((seal.until - now) % 10 == 0) {
				FusedFrostVfx.cryostasisTick(level, ally, (int) (seal.until - now));
			}
		}
		for (Seal seal : opened) {
			open(seal);
		}
	}

	/** A seal opens: the healing that's left if it ran its time, the ally let go, the ice cracking away. */
	private static void open(Seal seal) {
		LivingEntity ally = seal.ally;
		if (!(ally.level() instanceof ServerLevel level)) {
			seal.shell.forEach(Entity::discard);
			return;
		}
		if (level.getGameTime() >= seal.until && seal.healsLeft > 0 && seal.cast.alive()) {
			ally.heal(seal.healEach * seal.healsLeft);
			seal.healsLeft = 0;
		}
		Spirits.thawNow(ally);
		ally.resetFallDistance();
		FusedFrostVfx.cryostasisRelease(level, ally, seal.shell);
		if (level.getGameTime() >= seal.until && seal.cast.alive()) {
			burst(seal, level);
		}
	}

	/** The ice bursts outward: enemies within 3 blocks are thrown back, take 3, are slowed and left brittle. */
	private static void burst(Seal seal, ServerLevel level) {
		LivingEntity ally = seal.ally;
		Cast cast = seal.cast;
		Effects.asElement("frost", () -> {
			int struck = 0;
			for (Entity e : level.getEntities(ally, ally.getBoundingBox().inflate(FusedFrostRules.BURST_RADIUS), e -> Targets.canHarm(cast.caster, e))) {
				if (struck >= FusedFrost.MAX_IN_AREA || e.distanceTo(ally) > FusedFrostRules.BURST_RADIUS + e.getBbWidth() / 2) {
					continue;
				}
				struck++;
				LivingEntity t = (LivingEntity) e;
				if (!Spirits.isBoss(t)) {
					Vec3 away = Effects.horizontal(t.position().subtract(ally.position()), ally.getLookAngle());
					Effects.push(t, away.scale(FusedFrostRules.BURST_KNOCK).add(0, 0.3, 0));
				}
				Effects.hurt(cast, t, level.damageSources().source(DamageTypes.FREEZE, cast.caster), FusedFrostRules.BURST_DAMAGE);
				t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FusedFrostRules.BURST_SLOW_TICKS, 1, false, true), cast.caster);
				Reactions.mark(t, Reactions.Mark.FROZEN, 40);
			}
		});
	}
}
