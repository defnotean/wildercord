package dev.wildercord.cast;

import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Magic that stays on a creature and answers what happens to it: Stasis (holds damage until
 * time moves again), Reversal (cheats one death), Reflect, Foresight (dodges), Infinity
 * (stops projectiles), plus the short position history that Rewind reads. None of it is
 * saved: every ward lasts seconds, and a restart simply ends them.
 */
public final class Wards {
	private Wards() {}

	private static final class Stasis {
		final LivingEntity target;
		final LivingEntity caster;
		final Vec3 anchor;
		/** The world the anchor is in. */
		final net.minecraft.world.level.Level level;
		final boolean hadNoGravity;
		long until;
		float stored;
		int hits;
		DamageSource source;

		Stasis(LivingEntity target, LivingEntity caster, long until) {
			this.target = target;
			this.caster = caster;
			this.anchor = target.position();
			this.level = target.level();
			this.hadNoGravity = target.isNoGravity();
			this.until = until;
		}
	}

	private record Reflect(long until, double fraction) {}

	private static final class Sight {
		long until;
		int charges;
	}

	private static final class Infinity {
		final LivingEntity who;
		final LivingEntity caster;
		long until;

		Infinity(LivingEntity who, LivingEntity caster, long until) {
			this.who = who;
			this.caster = caster;
			this.until = until;
		}
	}

	private record Held(Projectile projectile, UUID holder, boolean hadNoGravity) {}

	private record Snapshot(ResourceKey<Level> dimension, Vec3 pos, float health, float yRot, float xRot, long time) {}

	private static final Map<UUID, Stasis> STASIS = new HashMap<>();
	private static final Map<UUID, Long> REVERSAL = new HashMap<>();
	private static final Map<UUID, Reflect> REFLECT = new HashMap<>();
	private static final Map<UUID, Sight> FORESIGHT = new HashMap<>();
	private static final Map<UUID, Infinity> INFINITY = new HashMap<>();
	private static final Map<UUID, Held> HELD = new HashMap<>();
	private static final Map<UUID, ArrayDeque<Snapshot>> HISTORY = new HashMap<>();
	/** Snapshots every half second, kept for 7 seconds. */
	private static final int HISTORY_SIZE = 14;
	private static boolean reflecting;

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(Wards::allowDamage);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> reflect(entity, source, damage));
		ServerLivingEntityEvents.ALLOW_DEATH.register(Wards::allowDeath);
		// Rewind never reaches back past a death (a quick respawn could land between two snapshots).
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer) {
				HISTORY.remove(entity.getUUID());
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(Wards::tick);
		Techniques.init();
		// Held things let go before the world is saved, and anything saved while held (a player who
		// logged out in Stasis) gets its gravity back when it loads.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Stasis held : new ArrayList<>(STASIS.values())) {
				unhold(held.target, held.hadNoGravity);
			}
			for (Held held : new ArrayList<>(HELD.values())) {
				letGo(held.holder());
			}
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity.hasAttached(WildercordAttachments.HELD_GRAVITY) && !STASIS.containsKey(entity.getUUID()) && !HELD.containsKey(entity.getUUID())) {
				unhold(entity, false);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			STASIS.clear();
			REVERSAL.clear();
			REFLECT.clear();
			FORESIGHT.clear();
			INFINITY.clear();
			HELD.clear();
			HISTORY.clear();
		});
	}

	// ------------------------------------------------------------------ casting

	/** Stasis: holds the target still in time. Bosses are only slowed. */
	static void stasis(Cast cast, LivingEntity t, int ticks) {
		if (Shields.stops(cast, t, cast.caster.getEyePosition())) {
			return;
		}
		long now = cast.level.getGameTime();
		if (cast.once("stasis")) {
			Fx.sound(cast.level, cast.caster.position(), SoundEvents.BELL_BLOCK, 1.0F, 0.5F);
			Fx.sound(cast.level, cast.caster.position(), SoundEvents.BEACON_DEACTIVATE, 0.8F, 0.6F);
		}
		TechniqueVfx.stasisStart(cast.level, t);
		if (Spirits.isBoss(t)) {
			t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 4, false, true));
			return;
		}
		Stasis held = STASIS.get(t.getUUID());
		if (held != null && held.target == t) {
			held.until = Math.max(held.until, now + ticks);
			Spirits.hold(t, (int) (held.until - now));
			return;
		}
		STASIS.put(t.getUUID(), new Stasis(t, cast.caster, now + ticks));
		hold(t);
		t.setDeltaMovement(Vec3.ZERO);
		Spirits.hold(t, ticks);
	}

	static void reversal(Cast cast, LivingEntity t, int ticks) {
		REVERSAL.merge(t.getUUID(), cast.level.getGameTime() + ticks, Math::max);
		TechniqueVfx.reversalMark(cast.level, t);
	}

	static void reflect(Cast cast, LivingEntity t, int ticks, double fraction) {
		REFLECT.put(t.getUUID(), new Reflect(cast.level.getGameTime() + ticks, fraction));
		TechniqueVfx.reflectMark(cast.level, t);
	}

	static void foresight(Cast cast, LivingEntity t, int ticks, int charges) {
		Sight sight = new Sight();
		sight.until = cast.level.getGameTime() + ticks;
		sight.charges = charges;
		FORESIGHT.put(t.getUUID(), sight);
		TechniqueVfx.foresightMark(cast.level, t);
	}

	static void infinity(Cast cast, LivingEntity t, int ticks) {
		long until = cast.level.getGameTime() + ticks;
		Infinity existing = INFINITY.get(t.getUUID());
		if (existing != null && existing.who == t) {
			existing.until = Math.max(existing.until, until);
		} else {
			INFINITY.put(t.getUUID(), new Infinity(t, cast.caster, until));
		}
		TechniqueVfx.infinityStart(cast.level, t);
	}

	/** Rewind: back to where the player was about five seconds ago, with that health if it was more. */
	static void rewind(Cast cast, LivingEntity t) {
		if (!(t instanceof ServerPlayer player)) {
			return;
		}
		ArrayDeque<Snapshot> history = HISTORY.get(player.getUUID());
		if (history == null || history.isEmpty()) {
			return;
		}
		long now = cast.level.getGameTime();
		Snapshot pick = null;
		for (Snapshot snapshot : history) {
			if (now - snapshot.time() >= 100) {
				pick = snapshot;
			}
		}
		if (pick == null) {
			pick = history.peekFirst();
		}
		if (!pick.dimension().equals(player.level().dimension())) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.rewind_dimension"));
			return;
		}
		// Later snapshots are forgotten, so rewinding again goes further back.
		while (!history.isEmpty() && history.peekLast() != pick) {
			history.pollLast();
		}
		Vec3 from = player.position();
		player.teleportTo(player.level(), pick.pos().x, pick.pos().y, pick.pos().z, Set.<Relative>of(), pick.yRot(), pick.xRot(), false);
		player.setDeltaMovement(Vec3.ZERO);
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
		player.resetFallDistance();
		player.clearFire();
		if (pick.health() > player.getHealth()) {
			player.setHealth(Math.min(player.getMaxHealth(), pick.health()));
		}
		TechniqueVfx.rewind(cast.level, from, pick.pos());
	}

	// ------------------------------------------------------------------ events

	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !(entity.level() instanceof ServerLevel level)) {
			return true;
		}
		Stasis held = STASIS.get(entity.getUUID());
		if (held != null && held.target == entity) {
			// Every hit on a stopped target waits for time to move again; show that it landed.
			held.stored += amount;
			held.hits++;
			held.source = source;
			TechniqueVfx.stasisStore(level, entity, held.hits);
			if (!held.caster.isRemoved()) {
				Casters.tell(held.caster, Component.translatable("message.wildercord.time_held", Math.round(held.stored), held.hits).withColor(0xF2D98A));
			}
			return false;
		}
		Sight sight = FORESIGHT.get(entity.getUUID());
		if (sight != null && source.getEntity() != null && source.getEntity() != entity) {
			if (sight.until < level.getGameTime()) {
				FORESIGHT.remove(entity.getUUID());
				return true;
			}
			dodge(level, entity, source);
			if (--sight.charges <= 0) {
				FORESIGHT.remove(entity.getUUID());
			}
			return false;
		}
		return true;
	}

	/** Foresight: a sidestep away from the attack, if there's room. The hit misses either way. */
	private static void dodge(ServerLevel level, LivingEntity entity, DamageSource source) {
		Vec3 from = source.getSourcePosition() != null ? source.getSourcePosition() : source.getEntity().position();
		Vec3 toward = Effects.horizontal(entity.position().subtract(from), entity.getLookAngle());
		Vec3 side = new Vec3(-toward.z, 0, toward.x);
		Vec3 start = entity.position();
		for (double dist : new double[] {1.6, 1.0}) {
			for (int sign : new int[] {1, -1}) {
				Vec3 spot = start.add(side.scale(sign * dist));
				if (level.noCollision(entity, entity.getBoundingBox().move(spot.subtract(start)))) {
					entity.teleportTo(level, spot.x, spot.y, spot.z, Set.<Relative>of(), entity.getYRot(), entity.getXRot(), false);
					TechniqueVfx.dodge(level, entity, start, spot);
					return;
				}
			}
		}
		TechniqueVfx.dodge(level, entity, start, start);
	}

	private static void reflect(LivingEntity entity, DamageSource source, float damage) {
		if (reflecting || REFLECT.isEmpty() || damage <= 0) {
			return;
		}
		Reflect ward = REFLECT.get(entity.getUUID());
		if (ward == null || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		if (ward.until() < level.getGameTime()) {
			REFLECT.remove(entity.getUUID());
			return;
		}
		if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == entity || !attacker.isAlive()) {
			return;
		}
		reflecting = true;
		try {
			Effects.readyToHurt(attacker);
			attacker.hurtServer(level, level.damageSources().thorns(entity), (float) (damage * ward.fraction()));
			TechniqueVfx.reflect(level, entity, attacker);
		} finally {
			reflecting = false;
		}
	}

	private static boolean allowDeath(LivingEntity entity, DamageSource source, float amount) {
		if (REVERSAL.isEmpty() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !(entity.level() instanceof ServerLevel level)) {
			return true;
		}
		Long until = REVERSAL.remove(entity.getUUID());
		if (until == null || until < level.getGameTime()) {
			return true;
		}
		entity.setHealth(entity.getMaxHealth() * 0.5F);
		List<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> bad = new ArrayList<>();
		for (MobEffectInstance effect : entity.getActiveEffects()) {
			if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
				bad.add(effect.getEffect());
			}
		}
		bad.forEach(entity::removeEffect);
		entity.clearFire();
		entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true));
		entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 1, false, true));
		TechniqueVfx.reversal(level, entity);
		if (entity instanceof ServerPlayer player) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.reversal").withColor(0x6EDC64));
		}
		return false;
	}

	// ------------------------------------------------------------------ ticking

	private static void tick(MinecraftServer server) {
		if (!STASIS.isEmpty()) {
			tickStasis();
		}
		if (!INFINITY.isEmpty() || !HELD.isEmpty()) {
			tickInfinity();
		}
		if (server.getTickCount() % 10 == 0) {
			record(server);
		}
		if (server.getTickCount() % 200 == 0) {
			long now = server.overworld().getGameTime();
			REVERSAL.values().removeIf(until -> until < now);
			REFLECT.values().removeIf(ward -> ward.until() < now);
			FORESIGHT.values().removeIf(sight -> sight.until < now);
		}
	}

	private static void tickStasis() {
		List<Stasis> ended = new ArrayList<>();
		for (Iterator<Stasis> it = STASIS.values().iterator(); it.hasNext(); ) {
			Stasis held = it.next();
			LivingEntity t = held.target;
			if (t.isRemoved() || !t.isAlive() || !(t.level() instanceof ServerLevel level)) {
				it.remove();
				unhold(t, held.hadNoGravity);
				continue;
			}
			long now = level.getGameTime();
			// Carried to another world (a portal it stood in): time moves again, rather than pulling it to the old
			// anchor's coordinates in the new one.
			if (now >= held.until || level != held.level) {
				it.remove();
				ended.add(held);
				continue;
			}
			t.setDeltaMovement(Vec3.ZERO);
			if (t.position().distanceToSqr(held.anchor) > 0.0025) {
				t.teleportTo(level, held.anchor.x, held.anchor.y, held.anchor.z, Set.<Relative>of(), t.getYRot(), t.getXRot(), false);
			}
			t.needsSync = true;
			if (now % 4 == 0) {
				TechniqueVfx.stasisTick(level, t, (int) (held.until - now));
			}
		}
		// Released after the sweep: the held damage goes back through the damage events.
		for (Stasis held : ended) {
			release(held);
		}
	}

	private static void release(Stasis held) {
		LivingEntity t = held.target;
		unhold(t, held.hadNoGravity);
		Spirits.thawNow(t);
		if (!(t.level() instanceof ServerLevel level)) {
			return;
		}
		TechniqueVfx.timeResumes(level, t, held.stored);
		if (held.stored > 0) {
			DamageSource source = held.source != null ? held.source : level.damageSources().indirectMagic(held.caster, held.caster);
			Effects.readyToHurt(t);
			t.hurtServer(level, source, held.stored);
			// Everything held lands at once, and it hits like it.
			Vec3 away = Effects.horizontal(t.position().subtract(held.caster.position()), held.caster.getLookAngle());
			Effects.push(t, away.scale(Math.min(2.0, 0.4 + held.hits * 0.15)).add(0, 0.35, 0));
			if (!held.caster.isRemoved()) {
				Casters.tell(held.caster, Component.translatable("message.wildercord.time_resumes", Math.round(held.stored), held.hits).withColor(0xF2D98A));
			}
		}
	}

	private static void tickInfinity() {
		for (Iterator<Infinity> it = INFINITY.values().iterator(); it.hasNext(); ) {
			Infinity ward = it.next();
			LivingEntity who = ward.who;
			if (who.isRemoved() || !who.isAlive() || !(who.level() instanceof ServerLevel level) || level.getGameTime() >= ward.until) {
				it.remove();
				letGo(who.getUUID());
				continue;
			}
			long now = level.getGameTime();
			Vec3 centre = who.getBoundingBox().getCenter();
			for (Projectile p : level.getEntitiesOfClass(Projectile.class, new AABB(centre, centre).inflate(4.5))) {
				Entity owner = p.getOwner();
				if (owner == who || (owner != null && Targets.isAlly(ward.caster, owner))) {
					continue;
				}
				Vec3 to = centre.subtract(p.position());
				double distance = to.length();
				boolean held = HELD.containsKey(p.getUUID());
				if (distance > 4.0 || (!held && p.getDeltaMovement().dot(to) <= 0)) {
					continue;
				}
				if (!held) {
					HELD.put(p.getUUID(), new Held(p, who.getUUID(), p.isNoGravity()));
					TechniqueVfx.infinityHalt(level, p.position());
				}
				p.setDeltaMovement(p.getDeltaMovement().scale(distance < 2.4 ? 0.0 : 0.35));
				hold(p);
				p.needsSync = true;
			}
			if (now % 4 == 0) {
				for (Entity e : level.getEntities(who, who.getBoundingBox().inflate(1.6), e -> Targets.canHarm(ward.caster, e))) {
					Vec3 away = Effects.horizontal(e.position().subtract(who.position()), who.getLookAngle());
					Effects.push((LivingEntity) e, away.scale(0.45).add(0, 0.1, 0));
				}
			}
			if (now % 8 == 0) {
				TechniqueVfx.infinityShell(level, centre);
			}
		}
		HELD.values().removeIf(held -> held.projectile().isRemoved());
	}

	/** Stops gravity for something a spell holds, remembering (in a saved attachment) what it was. */
	private static void hold(Entity e) {
		if (!e.hasAttached(WildercordAttachments.HELD_GRAVITY)) {
			e.setAttached(WildercordAttachments.HELD_GRAVITY, e.isNoGravity());
		}
		e.setNoGravity(true);
	}

	/** Gives back the gravity it had before it was held ({@code fallback} if that wasn't recorded). */
	private static void unhold(Entity e, boolean fallback) {
		Boolean had = e.removeAttached(WildercordAttachments.HELD_GRAVITY);
		e.setNoGravity(had != null ? had : fallback);
	}

	/** Drops every projectile an Infinity was holding: they fall as they would have. */
	private static void letGo(UUID holder) {
		for (Iterator<Held> it = HELD.values().iterator(); it.hasNext(); ) {
			Held held = it.next();
			if (held.holder().equals(holder)) {
				it.remove();
				if (!held.projectile().isRemoved()) {
					unhold(held.projectile(), held.hadNoGravity());
					held.projectile().setDeltaMovement(0, -0.05, 0);
					held.projectile().needsSync = true;
				}
			}
		}
	}

	private static void record(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive()) {
				// Dead: nothing from before the death is kept, so Rewind can't carry the respawned back to it.
				HISTORY.remove(player.getUUID());
				continue;
			}
			ArrayDeque<Snapshot> history = HISTORY.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
			history.addLast(new Snapshot(player.level().dimension(), player.position(), player.getHealth(), player.getYRot(), player.getXRot(),
				player.level().getGameTime()));
			while (history.size() > HISTORY_SIZE) {
				history.pollFirst();
			}
		}
		HISTORY.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
	}
}
