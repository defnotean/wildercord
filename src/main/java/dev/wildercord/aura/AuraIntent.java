package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.Spirits;
import dev.wildercord.cast.Targets;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Intent (Form, always on): a swordsman's presence presses on what's weaker. Once a second, every hostile creature within
 * {@link AuraRules#INTENT_RADIUS} blocks whose whole health is below yours (or, for anything that carries aura of its own, whose
 * stage is below yours) is slowed for a moment, and now and then falters, stopping where it stands. Bosses never feel it.
 * Another player it presses on (PvP on, and {@code aura.intent_pvp}) feels it as a dark vignette and a slight slow, modest by
 * design ({@code aura.intent_pvp_slow}: their speed, never Slowness), and only while it lasts.
 *
 * <p>It needs a blade in hand and aura enough to coat a blow: put the sword away and nothing is pressed on.</p>
 */
public final class AuraIntent {
	private AuraIntent() {}

	private static final Identifier SLOW = Wildercord.id("aura_intent");
	/** When each player Intent slowed lets go, by player. */
	private static final Map<UUID, Long> PRESSED = new HashMap<>();
	/** When each player last heard their Intent take hold (a low thrum, at most every few seconds). */
	private static final Map<UUID, Long> HEARD = new HashMap<>();
	private static final int HEARD_REST = 100;
	/** The vignette's colour: a dark, cold shadow at the edges, whoever's aura it is. */
	private static final int SHADOW = 0x1A1424;

	static void forget(UUID id) {
		HEARD.remove(id);
	}

	static void clear() {
		PRESSED.clear();
		HEARD.clear();
	}

	/** The stage of aura {@code entity} carries (a player's, or anything with an aura look, as the aura knights have), or -1. */
	public static int stageOf(LivingEntity entity) {
		if (entity instanceof Player player) {
			int stage = Aura.stage(player);
			return stage > AuraRules.NONE ? stage : -1;
		}
		AuraAttachments.Look look = entity.getAttached(AuraAttachments.LOOK);
		return look != null && look.stage() > AuraRules.NONE ? look.stage() : -1;
	}

	/** Whether Intent is pressing from {@code player} now: Form, aura working, a blade in hand and aura to coat a blow. */
	public static boolean active(ServerPlayer player) {
		return Aura.stage(player) >= AuraRules.FORM && Aura.coated(player) && !player.isSpectator();
	}

	/** Whether {@code player}'s Intent presses on {@code target}. */
	public static boolean presses(ServerPlayer player, LivingEntity target) {
		if (target == player || !target.isAlive() || !Targets.canHarm(player, target)) {
			return false;
		}
		if (target instanceof Player other) {
			if (other.isCreative() || other.isSpectator() || !Config.get().aura().heights().intentPvp()) {
				return false;
			}
		} else if (!(target instanceof Mob mob) || !(mob instanceof Enemy || mob.getTarget() == player) || Targets.playerPet(mob)) {
			return false;
		}
		return AuraRules.intimidated(Spirits.isBoss(target), stageOf(target), Aura.stage(player), target.getMaxHealth(), player.getMaxHealth());
	}

	/** Every tick, for a player with aura: once a second, their Intent presses. */
	static void tick(ServerPlayer player, long now) {
		if ((now + player.getId()) % AuraRules.INTENT_PERIOD == 0 && active(player)) {
			pulse(player);
		}
	}

	/** One press of {@code player}'s Intent on everything round them (also the tests, for a simulated player); returns how many it pressed on. */
	public static int pulse(ServerPlayer player) {
		long now = player.level().getGameTime();
		ServerLevel level = player.level();
		double r = AuraRules.INTENT_RADIUS;
		int pressed = 0;
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(r),
				e -> e.distanceToSqr(player) <= r * r)) {
			if (!presses(player, target)) {
				continue;
			}
			pressed++;
			if (target instanceof ServerPlayer other) {
				pressPlayer(other, now);
			} else if (target instanceof Mob mob) {
				pressCreature(player, mob);
			}
		}
		if (pressed > 0) {
			AuraVfx.intent(player, Aura.color(player));
			Long heard = HEARD.get(player.getUUID());
			if (heard == null || now - heard >= HEARD_REST) {
				HEARD.put(player.getUUID(), now);
				Aura.sound(player, "aura_intent", 0.5F, 1.0F);
			}
		}
		return pressed;
	}

	/** A weaker creature: slowed for a moment, and now and then it falters where it stands. */
	private static void pressCreature(ServerPlayer player, Mob mob) {
		mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, AuraRules.INTENT_SLOW_TICKS, 0, true, false), player);
		if (mob.getRandom().nextDouble() < AuraRules.INTENT_FALTER) {
			mob.getNavigation().stop();
			AuraVfx.falter(player.level(), mob, Aura.color(player));
		}
	}

	/** Another player: the vignette, and their speed taken down a little while it lasts. */
	private static void pressPlayer(ServerPlayer other, long now) {
		WildercordConfig.AuraHeights settings = Config.get().aura().heights();
		ScreenFx.tint(other, SHADOW, AuraRules.INTENT_VIGNETTE_TICKS);
		AttributeInstance speed = other.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null && settings.intentPvpSlow() > 0) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(SLOW, -settings.intentPvpSlow(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			PRESSED.put(other.getUUID(), now + AuraRules.INTENT_VIGNETTE_TICKS);
		}
	}

	/** Every tick: a pressed player's slow lets go once Intent stops reaching them. */
	static void release(MinecraftServer server) {
		for (Iterator<Map.Entry<UUID, Long>> it = PRESSED.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Long> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			boolean gone = player == null;
			if (gone || player.level().getGameTime() > entry.getValue()) {
				if (!gone) {
					AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
					if (speed != null) {
						speed.removeModifier(SLOW);
					}
				}
				it.remove();
			}
		}
	}

	/** Whether a player is slowed by someone's Intent now (the tests ask). */
	public static boolean slowed(Player player) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		return speed != null && speed.getModifier(SLOW) != null;
	}
}
