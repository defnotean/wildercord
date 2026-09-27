package dev.wildercord.cast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

/** Friendly fire is off by design: who counts as an ally, and who may be harmed. */
public final class Targets {
	private Targets() {}

	public static boolean isAlly(ServerPlayer caster, Entity entity) {
		if (entity == caster) {
			return true;
		}
		if (entity instanceof OwnableEntity ownable && ownable.getOwner() == caster) {
			return true;
		}
		return caster.isAlliedTo(entity);
	}

	/** Harmful effects: living, not you, not an ally, and respecting the pvp game rule. */
	public static boolean canHarm(ServerPlayer caster, Entity entity) {
		if (!(entity instanceof LivingEntity living) || !living.isAlive() || entity instanceof ArmorStand) {
			return false;
		}
		if (isAlly(caster, entity)) {
			return false;
		}
		if (entity instanceof Player player) {
			return caster.level().isPvpAllowed() && !player.isCreative() && !player.isSpectator();
		}
		return true;
	}

	/** Helpful effects: only you and your allies. */
	public static boolean canHelp(ServerPlayer caster, Entity entity) {
		return entity instanceof LivingEntity living && living.isAlive() && isAlly(caster, entity);
	}
}
