package dev.wildercord.cast.packs;

import dev.wildercord.cast.FxSupportAccess;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Spirits;
import dev.wildercord.cast.Targets;
import dev.wildercord.player.Mana;
import dev.wildercord.spell.WardRules;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the support pack leaves behind in the world: its places (a mist, a well, a sanctuary, a blastward...), the marks
 * it puts on creatures (Grace, Guardlink, Hexguard, a pacified mob...) and the hooks that read them. Nothing here is
 * saved: a restart clears it all, like the other spells' lingering parts.
 */
public final class WardState {
	private WardState() {}

	/** A place one of the runes keeps for a while. */
	static final class Zone {
		final String kind;
		final ServerLevel level;
		final Vec3 center;
		final double radius;
		final long until;
		final LivingEntity caster;
		final double power;
		final List<Mob> held = new ArrayList<>();

		Zone(String kind, ServerLevel level, Vec3 center, double radius, long until, LivingEntity caster, double power) {
			this.kind = kind;
			this.level = level;
			this.center = center;
			this.radius = radius;
			this.until = until;
			this.caster = caster;
			this.power = power;
		}

		boolean holds(Level at, double x, double y, double z) {
			return at == level && center.distanceToSqr(x, y, z) <= radius * radius;
		}
	}

	/** A mark on one creature until a game time, with whatever it needs. */
	static final class Mark {
		long until;
		double value;
		int left;
		LivingEntity other;
		Vec3 point;

		Mark(long until) {
			this.until = until;
		}
	}

	record Kept(ResourceKey<Level> dimension, BlockPos pos) {}

	record Keeper(long until, UUID owner) {}

	static final List<Zone> ZONES = new ArrayList<>();
	static final Map<UUID, Mark> AFTERCARE = new HashMap<>();
	static final Map<UUID, Mark> GRACE = new HashMap<>();
	static final Map<UUID, Long> GRACE_SPENT = new HashMap<>();
	static final Map<UUID, Mark> GUARDLINK = new HashMap<>();
	static final Map<UUID, Mark> HEXGUARD = new HashMap<>();
	static final Map<UUID, Mark> IRONHOLD = new HashMap<>();
	static final Map<UUID, Mark> EVADE = new HashMap<>();
	static final Map<UUID, Mark> HEARTHGUARD = new HashMap<>();
	static final Map<UUID, Mark> AEGIS = new HashMap<>();
	static final Map<UUID, Long> AEGIS_SPENT = new HashMap<>();
	static final Map<UUID, Mark> STAUNCH = new HashMap<>();
	static final Map<UUID, Mark> SHIELDWALL = new HashMap<>();
	static final Map<UUID, Mark> WITHDRAWN = new HashMap<>();
	static final Map<UUID, Mark> FAITHFUL = new HashMap<>();
	static final Map<UUID, Long> FAITHFUL_SPENT = new HashMap<>();
	/** Mobs that may pick no target (pacified, lured, spooked: the mark's point is where to walk, if any). */
	static final Map<Mob, Mark> CALMED = new HashMap<>();
	/** Mobs that may only pick their taunter. */
	static final Map<Mob, Mark> TAUNTED = new HashMap<>();
	static final Map<Kept, Keeper> KEEPSAFE = new HashMap<>();

	/** Set while Guardlink's share lands on a guardian, so it never passes on again. */
	private static boolean redirecting;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(WardState::tick);
		ServerLivingEntityEvents.ALLOW_DEATH.register(WardState::allowDeath);
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> mayBreak(world, player, pos));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> leave(handler.player));
	}

	private static List<Map<UUID, Mark>> marks() {
		return List.of(AFTERCARE, GRACE, GUARDLINK, HEXGUARD, IRONHOLD, EVADE, HEARTHGUARD, AEGIS, STAUNCH, SHIELDWALL, WITHDRAWN, FAITHFUL);
	}

	/** A player left: their places end and every mark on or tied to them goes (the save rests stay, so a relog can't reset them). */
	static void leave(LivingEntity player) {
		UUID id = player.getUUID();
		ZONES.removeIf(z -> z.caster == player);
		for (Map<UUID, Mark> map : marks()) {
			map.remove(id);
			map.values().removeIf(m -> m.other == player);
		}
		TAUNTED.values().removeIf(m -> m.other == player);
	}

	/** Every {@link #SWEEP} ticks: marks, rests and keepsakes whose time is over go, so nothing waits on a lookup to leave. */
	static final int SWEEP = 200;

	static void sweep(long now) {
		for (Map<UUID, Mark> map : marks()) {
			map.values().removeIf(m -> now > m.until);
		}
		GRACE_SPENT.values().removeIf(at -> !WardRules.resting(at, now, WardRules.SAVE_REST));
		FAITHFUL_SPENT.values().removeIf(at -> !WardRules.resting(at, now, WardRules.SAVE_REST));
		AEGIS_SPENT.values().removeIf(at -> !WardRules.resting(at, now, WardRules.AEGIS_REST));
		KEEPSAFE.values().removeIf(k -> now > k.until());
	}

	static void clear() {
		ZONES.clear();
		for (Map<UUID, ?> map : List.<Map<UUID, ?>>of(AFTERCARE, GRACE, GRACE_SPENT, GUARDLINK, HEXGUARD, IRONHOLD, EVADE, HEARTHGUARD, AEGIS,
				AEGIS_SPENT, STAUNCH, SHIELDWALL, WITHDRAWN, FAITHFUL, FAITHFUL_SPENT)) {
			map.clear();
		}
		CALMED.clear();
		TAUNTED.clear();
		KEEPSAFE.clear();
		redirecting = false;
	}

	static long now(Entity e) {
		return e.level().getGameTime();
	}

	static Mark mark(Map<UUID, Mark> map, LivingEntity e, int ticks) {
		Mark m = new Mark(now(e) + ticks);
		map.put(e.getUUID(), m);
		return m;
	}

	static Mark live(Map<UUID, Mark> map, Entity e) {
		if (map.isEmpty()) {
			return null;
		}
		Mark m = map.get(e.getUUID());
		if (m != null && now(e) > m.until) {
			map.remove(e.getUUID());
			return null;
		}
		return m;
	}

	public static boolean has(String what, Entity e) {
		return switch (what) {
			case "aftercare" -> live(AFTERCARE, e) != null;
			case "grace" -> live(GRACE, e) != null;
			case "guardlink" -> live(GUARDLINK, e) != null;
			case "hexguard" -> live(HEXGUARD, e) != null;
			case "ironhold" -> live(IRONHOLD, e) != null;
			case "evade" -> live(EVADE, e) != null;
			case "hearthguard" -> live(HEARTHGUARD, e) != null;
			case "aegis" -> live(AEGIS, e) != null;
			case "staunch" -> live(STAUNCH, e) != null;
			case "faithful" -> live(FAITHFUL, e) != null;
			case "calmed" -> e instanceof Mob mob && calmed(mob) != null;
			case "taunted" -> e instanceof Mob mob && TAUNTED.containsKey(mob);
			default -> false;
		};
	}

	static Mark calmed(Mob mob) {
		Mark m = CALMED.get(mob);
		if (m != null && now(mob) > m.until) {
			CALMED.remove(mob);
			return null;
		}
		return m;
	}

	/** Keeps a mob from choosing anyone for {@code ticks} (walking toward {@code walkTo} when given). */
	static void calm(Mob mob, int ticks, Vec3 walkTo) {
		Mark m = new Mark(now(mob) + ticks);
		m.point = walkTo;
		CALMED.put(mob, m);
		forget(mob);
	}

	static void forget(Mob mob) {
		mob.setTarget(null);
		if (mob instanceof net.minecraft.world.entity.NeutralMob neutral) {
			neutral.stopBeingAngry();
		}
		mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
		mob.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
	}

	static void zone(Zone zone) {
		ZONES.add(zone);
	}

	// ------------------------------------------------------------------ hooks

	/** Whether {@code mob} may take {@code target} (a pacified, lured or spooked mob none; a taunted one only its taunter). */
	public static boolean refusesTarget(Mob mob, LivingEntity target) {
		if (target == null || mob.level().isClientSide()) {
			return false;
		}
		if (calmed(mob) != null) {
			return true;
		}
		Mark taunt = TAUNTED.get(mob);
		if (taunt != null && now(mob) <= taunt.until && taunt.other != null && taunt.other.isAlive() && target != taunt.other) {
			return true;
		}
		return live(WITHDRAWN, target) != null;
	}

	/** Hexguard and Staunch: whether {@code instance} is turned away from {@code e}. */
	public static boolean refusesEffect(LivingEntity e, MobEffectInstance instance) {
		if (e.level().isClientSide() || HEXGUARD.isEmpty() && STAUNCH.isEmpty()) {
			return false;
		}
		if ((instance.is(MobEffects.POISON) || instance.is(MobEffects.WITHER)) && live(STAUNCH, e) != null) {
			return true;
		}
		if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && live(HEXGUARD, e) != null) {
			HEXGUARD.remove(e.getUUID());
			if (e.level() instanceof ServerLevel level) {
				WardVfx.refuse(level, e, 0x9E7BFF);
			}
			return true;
		}
		return false;
	}

	private static boolean bypasses(DamageSource source) {
		return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	/**
	 * What of {@code damage} reaches {@code self} after the pack's wards (negative: none, the blow never lands). Evade turns a
	 * melee blow, Aegis a missile; then Aegis, Hearthguard, Bellward and Citadel cut it, and Ironhold caps it.
	 */
	public static float incoming(LivingEntity self, DamageSource source, float damage) {
		if (damage <= 0 || bypasses(source) || !(self.level() instanceof ServerLevel level)) {
			return damage;
		}
		Entity attacker = source.getEntity();
		Entity direct = source.getDirectEntity();
		if (direct instanceof LivingEntity && direct == attacker && live(EVADE, self) != null) {
			EVADE.remove(self.getUUID());
			Vec3 away = FxSupportAccess.horizontal(self.position().subtract(direct.position()), self.getLookAngle());
			Vec3 aside = new Vec3(-away.z, 0, away.x).scale(0.7).add(away.scale(0.3)).add(0, 0.2, 0);
			self.setDeltaMovement(self.getDeltaMovement().add(aside));
			self.needsSync = true;
			if (self instanceof ServerPlayer player) {
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
			}
			WardVfx.evade(level, self);
			return -1;
		}
		Mark aegis = live(AEGIS, self);
		if (aegis != null && direct instanceof Projectile) {
			WardVfx.refuse(level, self, 0xFFE7A0);
			return -1;
		}
		float out = damage;
		if (aegis != null) {
			out *= (float) WardRules.AEGIS_TAKES;
		}
		if (attacker instanceof Enemy && (live(HEARTHGUARD, self) != null || self instanceof AbstractVillager && inZone("bellward", self))) {
			out *= (float) WardRules.VILLAGE_TAKES;
		}
		for (Zone z : ZONES) {
			if (z.kind.equals("citadel") && z.holds(level, self.getX(), self.getY(), self.getZ()) && level.getGameTime() <= z.until
					&& Targets.isAlly(z.caster, self)) {
				out *= (float) WardRules.CITADEL_TAKES;
				break;
			}
		}
		Mark iron = live(IRONHOLD, self);
		if (iron != null) {
			out = WardRules.ironhold(out, iron.value);
		}
		return out;
	}

	/** Guardlink: the share of {@code damage} its guardian will take, if the link holds (it breaks here when it can't). */
	public static float guardShare(LivingEntity self, DamageSource source, float damage) {
		if (redirecting || damage <= 0 || bypasses(source) || GUARDLINK.isEmpty()) {
			return 0;
		}
		Mark link = live(GUARDLINK, self);
		if (link == null) {
			return 0;
		}
		LivingEntity guardian = link.other;
		if (guardian == null || !guardian.isAlive() || guardian.level() != self.level() || guardian.distanceTo(self) > WardRules.GUARDLINK_RANGE
				|| guardian.getHealth() <= WardRules.GUARDLINK_FLOOR || guardian == source.getEntity()) {
			GUARDLINK.remove(self.getUUID());
			return 0;
		}
		return WardRules.redirect(damage, guardian.getHealth());
	}

	/** After a blow landed on {@code self}: Guardlink sends its share on, Aftercare starts mending, a pacified mob wakes. */
	public static void landed(LivingEntity self, DamageSource source, float dealt, float shared) {
		if (!(self.level() instanceof ServerLevel level)) {
			return;
		}
		if (shared > 0) {
			Mark link = GUARDLINK.get(self.getUUID());
			LivingEntity guardian = link == null ? null : link.other;
			if (guardian != null) {
				WardVfx.tether(level, self, guardian, 0xC9A36B);
				Scheduler.later(1, () -> {
					if (guardian.isAlive() && guardian.level() instanceof ServerLevel at) {
						redirecting = true;
						try {
							guardian.hurtServer(at, at.damageSources().magic(), shared);
						} finally {
							redirecting = false;
						}
					}
				});
			}
		}
		Mark care = live(AFTERCARE, self);
		if (care != null && care.left > 0 && dealt > 0) {
			care.left--;
			double heal = care.value;
			Scheduler.later(20, () -> {
				if (self.isAlive()) {
					self.heal((float) heal);
					WardVfx.mend(self.level() instanceof ServerLevel at ? at : level, self, 0x9BD36A, 3);
				}
			});
		}
		if (self instanceof Mob mob && source.getEntity() instanceof LivingEntity && calmed(mob) != null) {
			CALMED.remove(mob);
		}
	}

	/** Whether a monster may spawn here (Sanctuary and Accord say no). */
	public static boolean forbidsSpawn(Level level, double x, double y, double z) {
		if (ZONES.isEmpty()) {
			return false;
		}
		for (Zone zone : ZONES) {
			if ((zone.kind.equals("sanctuary") || zone.kind.equals("accord")) && zone.holds(level, x, y, z) && level.getGameTime() <= zone.until) {
				return true;
			}
		}
		return false;
	}

	/** Whether an explosion must leave this block (Blastward, Citadel, Keepsafe). */
	public static boolean sparesBlock(Level level, BlockPos pos) {
		if (ZONES.isEmpty() && KEEPSAFE.isEmpty()) {
			return false;
		}
		for (Zone zone : ZONES) {
			if ((zone.kind.equals("blastward") || zone.kind.equals("citadel")) && zone.holds(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
					&& level.getGameTime() <= zone.until) {
				return true;
			}
		}
		Keeper keeper = KEEPSAFE.get(new Kept(level.dimension(), pos));
		return keeper != null && level.getGameTime() <= keeper.until();
	}

	/** Whether any ward could spare a block, so an explosion needn't check each one. */
	public static boolean anyBlockWards() {
		return !KEEPSAFE.isEmpty() || ZONES.stream().anyMatch(z -> z.kind.equals("blastward") || z.kind.equals("citadel"));
	}

	private static boolean mayBreak(Level world, Player player, BlockPos pos) {
		if (KEEPSAFE.isEmpty()) {
			return true;
		}
		Kept key = new Kept(world.dimension(), pos);
		Keeper keeper = KEEPSAFE.get(key);
		if (keeper == null) {
			return true;
		}
		if (world.getGameTime() > keeper.until()) {
			KEEPSAFE.remove(key);
			return true;
		}
		if (player.getUUID().equals(keeper.owner()) || player.isCreative()) {
			KEEPSAFE.remove(key);
			return true;
		}
		MinecraftServer server = world.getServer();
		ServerPlayer owner = server == null ? null : server.getPlayerList().getPlayer(keeper.owner());
		if (owner != null && dev.wildercord.party.Parties.sameParty(owner, player)) {
			KEEPSAFE.remove(key);
			return true;
		}
		if (world instanceof ServerLevel level) {
			WardVfx.refuse(level, Vec3.atCenterOf(pos), 0xC9A36B);
		}
		return false;
	}

	private static boolean inZone(String kind, Entity e) {
		for (Zone zone : ZONES) {
			if (zone.kind.equals(kind) && zone.holds(e.level(), e.getX(), e.getY(), e.getZ()) && e.level().getGameTime() <= zone.until) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ death saves

	private static boolean allowDeath(LivingEntity e, DamageSource source, float amount) {
		if (GRACE.isEmpty() && FAITHFUL.isEmpty() || bypasses(source) || !(e.level() instanceof ServerLevel level)) {
			return true;
		}
		long now = level.getGameTime();
		Mark grace = live(GRACE, e);
		if (grace != null && !Spirits.isBoss(e) && dev.wildercord.cast.DeathsDoor.resting(e) <= 0) {
			GRACE.remove(e.getUUID());
			Long spent = GRACE_SPENT.get(e.getUUID());
			if (spent == null || !WardRules.resting(spent, now, WardRules.SAVE_REST)) {
				FxSupportAccess.saved(e);
				GRACE_SPENT.put(e.getUUID(), now);
				e.setHealth(Math.min(e.getMaxHealth(), 2.0F));
				e.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 2, false, true));
				WardVfx.saved(level, e, 0xFFE7A0);
				return false;
			}
		}
		Mark faithful = live(FAITHFUL, e);
		if (faithful != null && dev.wildercord.cast.DeathsDoor.resting(e) <= 0) {
			FAITHFUL.remove(e.getUUID());
			Long spent = FAITHFUL_SPENT.get(e.getUUID());
			if (spent == null || !WardRules.resting(spent, now, WardRules.SAVE_REST)) {
				FxSupportAccess.saved(e);
				FAITHFUL_SPENT.put(e.getUUID(), now);
				e.setHealth(1.0F);
				e.clearFire();
				LivingEntity owner = faithful.other;
				if (owner != null && owner.isAlive() && owner.level() == level) {
					e.teleportTo(owner.getX(), owner.getY(), owner.getZ());
					e.setDeltaMovement(Vec3.ZERO);
					if (e instanceof Mob mob) {
						forget(mob);
					}
				}
				WardVfx.saved(level, e, 0xFFB3D9);
				return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ the tick

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % SWEEP == 0) {
			sweep(server.overworld().getGameTime());
		}
		tickMobs();
		if (ZONES.isEmpty()) {
			return;
		}
		Iterator<Zone> it = ZONES.iterator();
		while (it.hasNext()) {
			Zone zone = it.next();
			long now = zone.level.getGameTime();
			if (now > zone.until || zone.level.getServer() != server) {
				it.remove();
				continue;
			}
			tickZone(zone, now);
		}
	}

	private static void tickMobs() {
		if (!CALMED.isEmpty()) {
			Iterator<Map.Entry<Mob, Mark>> it = CALMED.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Mob, Mark> entry = it.next();
				Mob mob = entry.getKey();
				Mark m = entry.getValue();
				if (!mob.isAlive() || now(mob) > m.until) {
					it.remove();
					continue;
				}
				if (mob.getTarget() != null) {
					forget(mob);
				}
				if (m.point != null && now(mob) % 10 == 0) {
					mob.getNavigation().moveTo(m.point.x, m.point.y, m.point.z, m.value > 0 ? m.value : 1.0);
				}
			}
		}
		if (!TAUNTED.isEmpty()) {
			Iterator<Map.Entry<Mob, Mark>> it = TAUNTED.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Mob, Mark> entry = it.next();
				Mob mob = entry.getKey();
				Mark m = entry.getValue();
				if (!mob.isAlive() || now(mob) > m.until || m.other == null || !m.other.isAlive() || m.other.level() != mob.level()) {
					it.remove();
					continue;
				}
				if (mob.getTarget() != m.other) {
					mob.setTarget(m.other);
				}
			}
		}
	}

	private static void tickZone(Zone zone, long now) {
		ServerLevel level = zone.level;
		boolean second = (now - zone.until) % 20 == 0;
		AABB box = new AABB(zone.center, zone.center).inflate(zone.radius);
		switch (zone.kind) {
			case "mending_mist" -> {
				if (second) {
					for (LivingEntity e : allies(zone, box)) {
						e.heal((float) zone.power);
					}
					WardVfx.mist(level, zone.center, zone.radius, 0xA8E4FF);
				}
			}
			case "hearthglow" -> {
				if (second) {
					for (LivingEntity e : allies(zone, box)) {
						e.setTicksFrozen(0);
						MobEffectInstance regen = e.getEffect(MobEffects.REGENERATION);
						if (regen == null || regen.getAmplifier() == 0 && regen.getDuration() < 30) {
							e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, true));
						}
					}
					WardVfx.glow(level, zone.center, zone.radius, 0xFFB45A);
				}
			}
			case "manawell" -> {
				if (second) {
					for (LivingEntity e : allies(zone, box)) {
						if (e instanceof ServerPlayer player) {
							Mana.restore(player, 2.0F);
						}
					}
					WardVfx.well(level, zone.center, zone.radius, 0x8FB8FF);
				}
			}
			case "bellward" -> {
				if (second) {
					for (Entity e : level.getEntities((Entity) null, box, x -> x instanceof Enemy && x instanceof LivingEntity && x.isAlive())) {
						if (zone.holds(level, e.getX(), e.getY(), e.getZ())) {
							((LivingEntity) e).addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
						}
					}
				}
			}
			case "sanctuary", "accord" -> {
				if (zone.kind.equals("sanctuary") && now % 10 == 0) {
					for (Entity e : level.getEntities((Entity) null, box, x -> x instanceof Enemy && x instanceof Mob && x.isAlive())) {
						if (!Spirits.isBoss(e) && zone.holds(level, e.getX(), e.getY(), e.getZ())) {
							Vec3 out = FxSupportAccess.horizontal(e.position().subtract(zone.center), new Vec3(1, 0, 0));
							FxSupportAccess.push((LivingEntity) e, out.scale(0.35).add(0, 0.1, 0));
						}
					}
				}
				if (second) {
					WardVfx.ring(level, zone.center, zone.radius, zone.kind.equals("accord") ? 0xFFF4C8 : 0xE8C6FF);
				}
			}
			case "arrowveil", "citadel" -> {
				for (Entity e : level.getEntities((Entity) null, box, x -> x instanceof Projectile)) {
					Projectile missile = (Projectile) e;
					Entity owner = missile.getOwner();
					if (!zone.holds(level, e.getX(), e.getY(), e.getZ()) || owner != null && Targets.isAlly(zone.caster, owner)) {
						continue;
					}
					if (zone.kind.equals("citadel") && owner != null && zone.holds(level, owner.getX(), owner.getY(), owner.getZ())) {
						continue;
					}
					if (missile instanceof AbstractHurtingProjectile) {
						WardVfx.refuse(level, missile.position(), 0xD8F4FF);
						missile.discard();
					} else if (missile.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
						missile.setDeltaMovement(0, Math.min(0, missile.getDeltaMovement().y) * 0.2, 0);
						missile.needsSync = true;
						WardVfx.refuse(level, missile.position(), 0xD8F4FF);
					}
				}
				if (second) {
					WardVfx.ring(level, zone.center, zone.radius, zone.kind.equals("citadel") ? 0xC9A36B : 0xD8F4FF);
				}
			}
			case "corral" -> {
				zone.held.removeIf(mob -> !mob.isAlive() || mob.level() != level);
				for (Mob mob : zone.held) {
					double d = mob.position().distanceTo(zone.center);
					if (d > zone.radius) {
						Vec3 back = FxSupportAccess.horizontal(zone.center.subtract(mob.position()), new Vec3(1, 0, 0));
						FxSupportAccess.push(mob, back.scale(0.4).add(0, 0.1, 0));
					}
				}
				if (second) {
					WardVfx.ring(level, zone.center, zone.radius, 0xBFE8D0);
				}
			}
			case "blastward" -> {
				if (second && (now - zone.until) % 60 == 0) {
					WardVfx.ring(level, zone.center, zone.radius, 0xC9A36B);
				}
			}
			default -> {}
		}
	}

	private static List<LivingEntity> allies(Zone zone, AABB box) {
		List<LivingEntity> out = new ArrayList<>();
		for (Entity e : zone.level.getEntities((Entity) null, box, x -> x instanceof LivingEntity && x.isAlive())) {
			if (zone.holds(zone.level, e.getX(), e.getY(), e.getZ()) && Targets.canHelp(zone.caster, e)) {
				out.add((LivingEntity) e);
			}
		}
		return out;
	}
}
