package dev.wildercord.aura;

import dev.wildercord.api.AuraApi;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.Spirits;
import dev.wildercord.cast.Targets;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dominion (Sovereign): hold the Aura key and you claim the ground you stand on. A circle of your aura, {@link
 * AuraRules#DOMINION_RADIUS} blocks out from where you raised it, holds for a few seconds:
 * <ul>
 * <li>foes inside are slowed (creatures Slowness II, players Slowness I) and every blow they strike lands weaker;</li>
 * <li>each of your blows on a foe inside chains once to another foe inside, carrying part of it as aura off the blade;</li>
 * <li>while you stand in it your aura comes back twice as fast, with a trickle besides.</li>
 * </ul>
 * It's a big moment and costs like one: much aura and a long rest. Against other players it stays inside the PvP caps: the
 * weakening is scaled by the aura PvP scale, and the chain is projected aura, which meets their spell defences.
 */
public final class AuraDominion {
	private AuraDominion() {}

	/** A Dominion standing in the world. */
	record Field(UUID owner, ServerLevel level, Vec3 centre, double radius, long start, long until, int color) {
		boolean inside(Entity e) {
			if (e.level() != level) {
				return false;
			}
			double dx = e.getX() - centre.x;
			double dz = e.getZ() - centre.z;
			double reach = radius + e.getBbWidth() / 2;
			return dx * dx + dz * dz <= reach * reach && e.getY() > centre.y - 2.0 && e.getY() < centre.y + 4.0;
		}
	}

	private static final Map<UUID, Field> FIELDS = new HashMap<>();
	/** When each player's blows last chained, so a sweep's blows chain once between them. */
	private static final Map<UUID, Long> CHAINED = new HashMap<>();

	static void init() {
		// Aura flows back faster for whoever stands in their own Dominion.
		AuraApi.onGain((player, amount, source) -> {
			Field field = FIELDS.get(player.getUUID());
			return field != null && field.inside(player) ? amount * AuraRules.DOMINION_FLOW : amount;
		});
	}

	static void forget(UUID id) {
		Field field = FIELDS.remove(id);
		CHAINED.remove(id);
		if (field != null) {
			AuraVfx.dominionEnd(field.level(), field.centre(), field.radius(), field.color());
		}
	}

	static void clear() {
		FIELDS.clear();
		CHAINED.clear();
	}

	/** The Dominion {@code player} holds now, if any (the tests ask). */
	public static boolean active(Player player) {
		Field field = FIELDS.get(player.getUUID());
		return field != null && player.level().getGameTime() <= field.until();
	}

	/** Whether {@code entity} stands inside the Dominion {@code owner} holds now. */
	public static boolean inside(Player owner, Entity entity) {
		Field field = FIELDS.get(owner.getUUID());
		return field != null && owner.level().getGameTime() <= field.until() && field.inside(entity);
	}

	/** Ends the Dominion {@code player} holds, at once (its rest still runs). */
	public static void end(ServerPlayer player) {
		forget(player.getUUID());
		AuraPresence.Timers timers = AuraPresence.timers(player);
		long now = player.level().getGameTime();
		if (timers.dominionUntil() > now) {
			AuraPresence.timers(player, timers.dominion(timers.dominionReadyAt(), now));
		}
	}

	/** The technique: raise a Dominion where the player stands. */
	public static boolean raise(ServerPlayer player) {
		long now = player.level().getGameTime();
		if (!Aura.holdsWeapon(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.no_weapon").withColor(0xA89CC8));
			return false;
		}
		AuraPresence.Timers timers = AuraPresence.timers(player);
		if (now < timers.dominionReadyAt()) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.dominion_resting", (timers.dominionReadyAt() - now + 19) / 20)
				.withColor(0xA89CC8));
			return false;
		}
		WildercordConfig.AuraHeights settings = Config.get().aura().heights();
		AuraRules.Spend paid = Aura.spend(player, settings.dominionCost(), "dominion");
		if (paid.backlash()) {
			// Spent past empty: the ground won't answer; a short rest before trying again.
			AuraPresence.timers(player, timers.dominion(now + 40, timers.dominionUntil()));
			return false;
		}
		int ticks = settings.dominionTicks();
		ServerLevel level = player.level();
		Field field = new Field(player.getUUID(), level, player.position(), AuraRules.DOMINION_RADIUS, now, now + ticks, Aura.color(player));
		Field old = FIELDS.put(player.getUUID(), field);
		if (old != null) {
			AuraVfx.dominionEnd(old.level(), old.centre(), old.radius(), old.color());
		}
		AuraPresence.timers(player, timers.dominion(now + settings.dominionCooldownTicks(), now + ticks));
		Aura.sound(player, "aura_dominion", 1.4F, 1.0F);
		AuraVfx.dominionRise(level, field.centre(), field.radius(), field.color(), ticks);
		ScreenFx.shake(level, field.centre(), 0.35F, 14);
		player.sendOverlayMessage(Component.translatable("message.wildercord.aura.dominion").withColor(0xFF000000 | field.color()));
		Grimoire.unlock(player, "aura:dominion");
		return true;
	}

	/** Every tick: each Dominion presses on the foes in it, keeps its look, and ends on time (or when its owner is gone). */
	static void tick(MinecraftServer server) {
		for (Iterator<Map.Entry<UUID, Field>> it = FIELDS.entrySet().iterator(); it.hasNext(); ) {
			Field field = it.next().getValue();
			ServerPlayer owner = server.getPlayerList().getPlayer(field.owner());
			long now = field.level().getGameTime();
			if (owner == null || !owner.isAlive() || owner.level() != field.level() || now > field.until()) {
				it.remove();
				AuraVfx.dominionEnd(field.level(), field.centre(), field.radius(), field.color());
				dev.wildercord.cast.feel.Feels.sound(field.level(), field.centre().add(0, 1, 0), "aura_dominion_fade", 1.0F, 1.0F);
				if (owner != null) {
					AuraPresence.Timers timers = AuraPresence.timers(owner);
					if (timers.dominionUntil() > now) {
						AuraPresence.timers(owner, timers.dominion(timers.dominionReadyAt(), now));
					}
				}
				continue;
			}
			long age = now - field.start();
			if (age % 10 == 0) {
				press(owner, field);
			}
			if (age % 10 == 5 && field.inside(owner)) {
				Aura.gain(owner, AuraRules.DOMINION_TRICKLE / 2, "dominion");
			}
			AuraVfx.dominionPulse(field.level(), field.centre(), field.radius(), field.color(), age, field.until() - now);
		}
	}

	/** The foes inside: slowed, and players among them see the circle's colour at the edges of their sight. */
	private static void press(ServerPlayer owner, Field field) {
		for (LivingEntity e : foes(owner, field)) {
			boolean player = e instanceof Player;
			int level = player ? AuraRules.DOMINION_SLOW_PLAYER : Spirits.isBoss(e) ? 0 : AuraRules.DOMINION_SLOW_CREATURE;
			e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 15, level, true, true), owner);
			if (e instanceof ServerPlayer other) {
				ScreenFx.tint(other, field.color(), 15);
			}
		}
	}

	/** Every foe of {@code owner}'s inside their Dominion. */
	static List<LivingEntity> foes(ServerPlayer owner, Field field) {
		List<LivingEntity> out = new ArrayList<>();
		AABB box = new AABB(field.centre(), field.centre()).inflate(field.radius() + 1, 4, field.radius() + 1);
		for (LivingEntity e : field.level().getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e != owner && field.inside(e))) {
			if (Targets.canHarm(owner, e)) {
				out.add(e);
			}
		}
		return out;
	}

	/**
	 * A blow (or anything else harmful) about to reach {@code target}, from a foe standing in someone's Dominion: weaker. Called
	 * from {@code mixin.LivingEntityAuraMixin} for every harm; unchanged when its striker stands in no Dominion of a foe's.
	 */
	public static float weakened(LivingEntity target, DamageSource source, float damage) {
		if (FIELDS.isEmpty() || damage <= 0 || !(source.getEntity() instanceof LivingEntity striker) || striker == target) {
			return damage;
		}
		for (Field field : FIELDS.values()) {
			if (striker.level() != field.level() || field.level().getGameTime() > field.until() || !field.inside(striker)) {
				continue;
			}
			ServerPlayer owner = field.level().getServer().getPlayerList().getPlayer(field.owner());
			if (owner == null || owner == striker || !Targets.canHarm(owner, striker)) {
				continue;
			}
			double weaken = Config.get().aura().heights().dominionWeaken();
			return (float) AuraRules.dominionWeakened(damage, weaken, striker instanceof Player, Config.get().aura().pvpScale());
		}
		return damage;
	}

	/**
	 * A blow of {@code player}'s landed on {@code struck}: in their Dominion, it chains once to the nearest other foe inside,
	 * carrying part of what it took as aura off the blade (which meets a player's spell defences).
	 */
	static void chain(ServerPlayer player, LivingEntity struck, float taken) {
		Field field = FIELDS.get(player.getUUID());
		long now = player.level().getGameTime();
		if (field == null || now > field.until() || taken <= 0 || !field.inside(struck)) {
			return;
		}
		Long last = CHAINED.get(player.getUUID());
		if (last != null && last == now) {
			return;
		}
		LivingEntity next = null;
		double best = Double.MAX_VALUE;
		for (LivingEntity e : foes(player, field)) {
			if (e == struck) {
				continue;
			}
			double d = e.distanceToSqr(struck);
			if (d < best) {
				best = d;
				next = e;
			}
		}
		if (next == null) {
			return;
		}
		CHAINED.put(player.getUUID(), now);
		AuraVfx.dominionChain(player.level(), struck, next, field.color());
		Aura.sound(player, "aura_slash", 0.5F, 1.5F);
		AuraCombat.projected(player, next, Math.max(1.0, taken * AuraRules.DOMINION_CHAIN_SHARE) * Config.get().aura().damageScale(), false);
	}
}
