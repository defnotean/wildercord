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

	/** A mirror ward: it cracks with every blow it returns (each costs a second of its time, and it holds at most {@link #REFLECTIONS}). */
	private static final class Reflect {
		long until;
		final double fraction;
		int left = REFLECTIONS;

		Reflect(long until, double fraction) {
			this.until = until;
			this.fraction = fraction;
		}

		long until() {
			return until;
		}

		double fraction() {
			return fraction;
		}
	}

	/** Blows a Reflect returns before it shatters. */
	public static final int REFLECTIONS = 6;

	private static final class Sight {
		long until;
		int charges;
		/** The press that put it up: a Zone, Pulse or Echo of the same cast only lengthens it, never refills it. */
		int cast;
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
	/** The most damage one Foresight dodge turns away. */
	static final float FORESIGHT_CAP = 12.0F;
	/** How far Infinity's slowing reaches, and how hard it slows at a distance (amplifier: 1 is Slowness II, 5 is VI). */
	static final double ZENO_REACH = 5.0;

	static int zenoLevel(double distance) {
		return distance <= 2.0 ? 5 : distance <= 3.0 ? 4 : distance <= 4.0 ? 3 : 1;
	}
	private static boolean softening;

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
		VoidTime.init();
		// Held things let go before the world is saved, and anything saved while held (a player who
		// logged out in Stasis) gets its gravity back when it loads. So does the copy of a held creature or
		// arrow carried to another world (a portal): it's a new entity there, which nothing holds.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Stasis held : new ArrayList<>(STASIS.values())) {
				unhold(held.target, held.hadNoGravity);
			}
			for (Held held : new ArrayList<>(HELD.values())) {
				letGo(held.holder());
			}
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity.hasAttached(WildercordAttachments.HELD_GRAVITY) && !holds(entity)) {
				unhold(entity, false);
			}
		});
		// The copy made in the other world keeps the held one's "no gravity" but not always the note of what it had
		// before, so it's given back from the original, which knows.
		net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents.AFTER_ENTITY_CHANGE_LEVEL.register((original, copy, from, to) -> {
			Stasis stasis = STASIS.get(original.getUUID());
			Held held = HELD.get(original.getUUID());
			Boolean noted = original.getAttached(WildercordAttachments.HELD_GRAVITY);
			if (noted == null && (stasis == null || stasis.target != original) && (held == null || held.projectile() != original)) {
				return;
			}
			boolean had = noted != null ? noted : stasis != null && stasis.target == original ? stasis.hadNoGravity : held.hadNoGravity();
			copy.removeAttached(WildercordAttachments.HELD_GRAVITY);
			copy.setNoGravity(had);
			// The original's saved attachments may be copied over after this; the note goes once they have been.
			Scheduler.later(1, () -> {
				if (!copy.isRemoved() && !holds(copy)) {
					copy.removeAttached(WildercordAttachments.HELD_GRAVITY);
				}
			});
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
			dev.wildercord.cast.feel.Feels.sound(cast.level, cast.caster.position(), "time_stop", 1.0F, 1.0F);
		}
		TechniqueVfx.stasisStart(cast.level, t);
		// The column of frozen sand is drawn once and lasts the hold (a renewal by a repeating shape does not redraw it).
		if (!STASIS.containsKey(t.getUUID())) {
			TimeFx.stasisColumn(cast.level, t, ticks);
		}
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
		int resting = DeathsDoor.resting(t);
		if (resting > 0) {
			// Death was cheated a moment ago: it won't be again yet (see DeathsDoor).
			Casters.tell(cast.caster, Component.translatableWithFallback("message.wildercord.deaths_door",
				"Death was cheated too recently: nothing turns it back again for %s s", resting).withColor(0x6EDC64));
			return;
		}
		REVERSAL.merge(t.getUUID(), cast.level.getGameTime() + ticks, Math::max);
		TechniqueVfx.reversalMark(cast.level, t);
	}

	/** Starmaw eats the wards on {@code t} (Foresight, Reflect, Reversal, Infinity, Riposte, Anchor); how many it took. */
	static int devourWards(LivingEntity t) {
		int taken = 0;
		UUID id = t.getUUID();
		taken += FORESIGHT.remove(id) != null ? 1 : 0;
		taken += REFLECT.remove(id) != null ? 1 : 0;
		taken += REVERSAL.remove(id) != null ? 1 : 0;
		if (INFINITY.remove(id) != null) {
			letGo(id);
			taken++;
		}
		taken += SignatureWards.devourGuard(t) ? 1 : 0;
		taken += VoidTime.unanchor(t) ? 1 : 0;
		return taken;
	}

	static void reflect(Cast cast, LivingEntity t, int ticks, double fraction) {
		REFLECT.put(t.getUUID(), new Reflect(cast.level.getGameTime() + ticks, fraction));
		TechniqueVfx.reflectMark(cast.level, t);
	}

	static void foresight(Cast cast, LivingEntity t, int ticks, int charges) {
		Sight old = FORESIGHT.get(t.getUUID());
		long until = cast.level.getGameTime() + ticks;
		if (old != null && old.cast == cast.id() && old.until >= cast.level.getGameTime() - 1) {
			old.until = Math.max(old.until, until);
			return;
		}
		Sight sight = new Sight();
		sight.until = until;
		sight.charges = charges;
		sight.cast = cast.id();
		FORESIGHT.put(t.getUUID(), sight);
		TechniqueVfx.foresightMark(cast.level, t, charges);
		// The sight running out: a ring closes on the head and a tick.
		TimeFx.endingLater(cast.level, t, ticks, TimeFx.gold(), "time_tick", 0.6F);
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
		// Never back into lava, a wall or thin air: time will not put you somewhere that would kill you.
		if (!Effects.safeSpot(player.level(), player, pick.pos())) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.rewind_unsafe"));
			return;
		}
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
		Sight sight = softening ? null : FORESIGHT.get(entity.getUUID());
		if (sight != null && sight.charges > 0 && source.getEntity() != null && source.getEntity() != entity) {
			if (sight.until < level.getGameTime()) {
				FORESIGHT.remove(entity.getUUID());
				return true;
			}
			dodge(level, entity, source);
			if (--sight.charges <= 0) {
				// Spent, it stays (resting) until its time is up so the same cast cannot put it up again.
				sight.until = Math.min(sight.until, level.getGameTime() + 5);
			}
			// A step aside stops a blow of up to 12; a bigger one only loses that much.
			if (amount > FORESIGHT_CAP && !softening) {
				softening = true;
				try {
					entity.hurtServer(level, source, amount - FORESIGHT_CAP);
				} finally {
					softening = false;
				}
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
			SpellDefence.hurt(level, attacker, level.damageSources().indirectMagic(entity, entity), (float) (damage * ward.fraction()));
			ward.until -= 20;
			if (--ward.left <= 0) {
				REFLECT.remove(entity.getUUID(), ward);
			}
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
		if (until == null || until < level.getGameTime() || DeathsDoor.resting(entity) > 0) {
			return true;
		}
		DeathsDoor.saved(entity, false);
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
		// Its hold (Spirits.hold, renewed as the Stasis was) lets go on its own schedule: a thaw forced here would
		// also end a longer Freeze on the same creature.
		if (!(t.level() instanceof ServerLevel level)) {
			return;
		}
		TechniqueVfx.timeResumes(level, t, held.stored);
		if (held.stored > 0) {
			DamageSource source = held.source != null ? held.source : level.damageSources().indirectMagic(held.caster, held.caster);
			Effects.readyToHurt(t);
			// What was held already met a player's spell defences as it was stored; it lands as one blow, so the spellguard answers it.
			SpellDefence.guarded(t, () -> t.hurtServer(level, source, held.stored));
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
				// Zeno's bubble: the closer a hostile thing comes, the slower it moves (Slowness II at 5 blocks up to VI within 2).
				for (Entity e : level.getEntities(who, who.getBoundingBox().inflate(ZENO_REACH), e -> e instanceof LivingEntity && Targets.canHarm(ward.caster, e))) {
					double d = e.getBoundingBox().getCenter().distanceTo(centre);
					if (d <= ZENO_REACH) {
						((LivingEntity) e).addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 8, zenoLevel(d), false, false), ward.caster);
					}
				}
			}
			if (now % 8 == 0) {
				TechniqueVfx.infinityShell(level, centre);
			}
		}
		HELD.values().removeIf(held -> held.projectile().isRemoved());
	}

	/** Whether a spell holds this very entity (not an earlier copy of it with the same id, left in another world). */
	private static boolean holds(Entity e) {
		Stasis stasis = STASIS.get(e.getUUID());
		Held held = HELD.get(e.getUUID());
		return stasis != null && stasis.target == e || held != null && held.projectile() == e;
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
