package dev.wildercord.aura.arts;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.feel.Feels;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * What an art leaves on a creature for a while, and what the rest of aura asks about it:
 * <ul>
 * <li><b>Glacier Mirror</b>'s ice: projectiles from in front turn back at whoever loosed them ({@link #deflection}, asked by
 * {@code AuraGuard.deflection} when no perfect guard turned it);</li>
 * <li><b>Eye of the Storm</b>'s wind: projectiles turn aside before they reach its swordsman, from any side;</li>
 * <li><b>Updraft</b>'s juggle: while a foe it threw is still in the air, its swordsman's coated blows on it land harder
 * ({@link #juggle}, asked by {@code AuraCombat.coat});</li>
 * <li><b>Unmoved</b>'s stone: its swordsman hardened (Resistance, and nothing knocks them back) until it wears off;</li>
 * <li><b>Frostbite</b>'s crusts on a foe, which freeze it at the third.</li>
 * </ul>
 */
public final class ArtWards {
	private ArtWards() {}

	private static final Map<UUID, Long> MIRROR = new HashMap<>();
	private static final Map<UUID, Long> EYE = new HashMap<>();
	private static final Map<UUID, Long> HARDENED = new HashMap<>();
	/** A foe Updraft threw: by the foe, its thrower and until when. */
	private record Juggle(UUID owner, long until) {}

	private static final Map<UUID, Juggle> JUGGLED = new HashMap<>();
	/** Frostbite's crusts on each foe: how many, and when the last landed. */
	private static final Map<UUID, long[]> CRUSTS = new HashMap<>();

	private static final Identifier UNMOVED = Wildercord.id("art_unmoved");

	static void init() {
		ServerTickEvents.END_SERVER_TICK.register(ArtWards::tick);
	}

	// ------------------------------------------------------------------ projectiles

	public static void mirror(ServerPlayer player, int ticks) {
		MIRROR.put(player.getUUID(), player.level().getGameTime() + ticks);
	}

	public static void eye(ServerPlayer player, int ticks) {
		EYE.put(player.getUUID(), player.level().getGameTime() + ticks);
	}

	public static boolean mirrored(Player player) {
		Long until = MIRROR.get(player.getUUID());
		return until != null && player.level().getGameTime() <= until;
	}

	public static boolean inEye(Player player) {
		Long until = EYE.get(player.getUUID());
		return until != null && player.level().getGameTime() <= until;
	}

	/**
	 * How an art's ward turns a projectile about to hit {@code player}, or null when none does: Glacier Mirror's ice sends one from
	 * in front back at whoever loosed it, a quarter faster (it's the swordsman's from then on); the Eye of the Storm's wind turns one
	 * from any side aside, slowed, past them.
	 */
	public static ProjectileDeflection deflection(ServerPlayer player, Projectile projectile) {
		if (mirrored(player) && dev.wildercord.aura.AuraGuard.facing(player, projectile.position())) {
			Entity shooter = projectile.getOwner();
			double speed = Math.min(3.0, Math.max(0.6, projectile.getDeltaMovement().length()) * 1.25);
			RimeArts.mirrorTurns(player, projectile.position());
			Scheduler.later(1, () -> {
				if (!projectile.isRemoved()) {
					projectile.setOwner(player);
				}
			});
			return (turned, by, random, power) -> {
				Vec3 at = turned.position();
				Vec3 aim = shooter != null && shooter.isAlive() && shooter.level() == turned.level() && shooter.distanceTo(player) < 32
					? shooter.getBoundingBox().getCenter().subtract(at) : player.getViewVector(1.0F);
				if (aim.lengthSqr() < 1.0E-4) {
					aim = player.getViewVector(1.0F);
				}
				turned.setDeltaMovement(aim.normalize().scale(speed));
				turned.needsSync = true;
			};
		}
		if (inEye(player)) {
			GaleArts.eyeTurns(player, projectile.position());
			return (turned, by, random, power) -> {
				Vec3 v = turned.getDeltaMovement();
				Vec3 flat = new Vec3(v.x, 0, v.z);
				if (flat.lengthSqr() < 1.0E-4) {
					flat = new Vec3(1, 0, 0);
				}
				// Swung round the swordsman, the way the wind turns: square to its path, and up a little.
				Vec3 side = new Vec3(-flat.z, 0, flat.x).normalize();
				double speed = Math.max(0.3, v.length() * 0.6);
				turned.setDeltaMovement(side.scale(speed).add(flat.normalize().scale(speed * 0.4)).add(0, 0.25, 0));
				turned.needsSync = true;
			};
		}
		return null;
	}

	// ------------------------------------------------------------------ the juggle

	/** {@code foe} was thrown by {@code owner}'s Updraft: their coated blows land harder while it's still in the air. */
	public static void juggled(ServerPlayer owner, LivingEntity foe, int ticks) {
		JUGGLED.put(foe.getUUID(), new Juggle(owner.getUUID(), owner.level().getGameTime() + ticks));
	}

	/** What {@code player}'s coated blow on {@code target} is multiplied by for the juggle: {@link ArtRules#UPDRAFT_JUGGLE} or 1. */
	public static double juggle(ServerPlayer player, LivingEntity target) {
		if (JUGGLED.isEmpty()) {
			return 1.0;
		}
		Juggle juggle = JUGGLED.get(target.getUUID());
		if (juggle == null || !juggle.owner().equals(player.getUUID()) || player.level().getGameTime() > juggle.until() || target.onGround()) {
			return 1.0;
		}
		return ArtRules.UPDRAFT_JUGGLE;
	}

	// ------------------------------------------------------------------ hardened

	/** {@code player} hardened like stone for {@code ticks}: Resistance I, and nothing knocks them back. */
	public static void harden(ServerPlayer player, int ticks) {
		HARDENED.put(player.getUUID(), player.level().getGameTime() + ticks);
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 0, false, true, true));
		AttributeInstance resist = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (resist != null) {
			resist.addOrUpdateTransientModifier(new AttributeModifier(UNMOVED, 1.0, AttributeModifier.Operation.ADD_VALUE));
		}
	}

	public static boolean hardened(Player player) {
		Long until = HARDENED.get(player.getUUID());
		return until != null && player.level().getGameTime() <= until;
	}

	private static void soften(ServerPlayer player) {
		AttributeInstance resist = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (resist != null && resist.getModifier(UNMOVED) != null) {
			resist.removeModifier(UNMOVED);
		}
	}

	// ------------------------------------------------------------------ crusts

	/** One more crust on {@code foe}: how many it carries now (fresh ones only, at most {@link ArtRules#CRUSTS_TO_FREEZE}). */
	public static int crust(LivingEntity foe) {
		long now = foe.level().getGameTime();
		long[] had = CRUSTS.get(foe.getUUID());
		int n = ArtRules.crusts(had == null ? 0 : (int) had[0], had == null ? Long.MIN_VALUE / 4 : had[1], now);
		if (n >= ArtRules.CRUSTS_TO_FREEZE) {
			CRUSTS.remove(foe.getUUID());
		} else {
			CRUSTS.put(foe.getUUID(), new long[] {n, now});
		}
		if (CRUSTS.size() > 256) {
			CRUSTS.values().removeIf(c -> now - c[1] > ArtRules.CRUST_MEMORY);
		}
		return n;
	}

	/** How many crusts {@code foe} carries now. */
	public static int crusts(LivingEntity foe) {
		long[] had = CRUSTS.get(foe.getUUID());
		return had == null || foe.level().getGameTime() - had[1] > ArtRules.CRUST_MEMORY ? 0 : (int) had[0];
	}

	// ------------------------------------------------------------------ upkeep

	private static void tick(MinecraftServer server) {
		if (HARDENED.isEmpty() && (server.getTickCount() % 100 != 0)) {
			return;
		}
		for (Iterator<Map.Entry<UUID, Long>> it = HARDENED.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Long> e = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
			if (player == null) {
				it.remove();
				continue;
			}
			long now = player.level().getGameTime();
			if (now > e.getValue() || !player.isAlive()) {
				soften(player);
				it.remove();
				Feels.sound(player.level(), player.position(), "earth_creak", 0.4F, 1.2F);
			} else if ((now + player.getId()) % 10 == 0) {
				StoneArts.hardenedLook(player);
			}
		}
		if (server.getTickCount() % 100 == 0) {
			long now = server.overworld().getGameTime();
			MIRROR.values().removeIf(until -> until < now - 200);
			EYE.values().removeIf(until -> until < now - 200);
			JUGGLED.values().removeIf(j -> j.until() < now - 200);
		}
	}

	static void forget(UUID id) {
		MIRROR.remove(id);
		EYE.remove(id);
		HARDENED.remove(id);
		JUGGLED.values().removeIf(j -> j.owner().equals(id));
	}

	static void clear() {
		MIRROR.clear();
		EYE.clear();
		HARDENED.clear();
		JUGGLED.clear();
		CRUSTS.clear();
	}
}
