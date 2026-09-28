package dev.wildercord.cast;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Friendly fire is off by design: who counts as an ally, and who may be harmed. For a player
 * that's themselves, their pets and their team. For a Runebound it's every other monster: its
 * spells hit players, their pets, golems and whatever it's hunting, never its own kind.
 */
public final class Targets {
	private Targets() {}

	public static boolean isAlly(LivingEntity caster, Entity entity) {
		if (entity == caster) {
			return true;
		}
		if (!(caster instanceof Player)) {
			return entity instanceof Enemy && !(entity instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player);
		}
		if (entity instanceof OwnableEntity ownable && ownable.getOwner() == caster) {
			return true;
		}
		return caster.isAlliedTo(entity);
	}

	/** Harmful effects: living, not you, not an ally, and respecting the pvp game rule. */
	public static boolean canHarm(LivingEntity caster, Entity entity) {
		if (!(entity instanceof LivingEntity living) || !living.isAlive() || entity instanceof ArmorStand) {
			return false;
		}
		// A duel overrides the rest: the two duellists may hurt each other, and nobody else may hurt either.
		Boolean duel = dev.wildercord.duel.Duels.canHarm(caster, entity);
		if (duel != null) {
			return duel;
		}
		if (isAlly(caster, entity)) {
			return false;
		}
		if (entity instanceof Player player) {
			boolean pvp = !(caster instanceof Player) || ((ServerLevel) caster.level()).isPvpAllowed();
			return pvp && !player.isCreative() && !player.isSpectator();
		}
		if (!(caster instanceof Player)) {
			return entity instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player
				|| entity instanceof AbstractGolem
				|| caster instanceof Mob mob && mob.getTarget() == entity
				|| entity instanceof net.minecraft.world.entity.decoration.Mannequin;
		}
		return true;
	}

	/** Helpful effects: only you and your allies. */
	public static boolean canHelp(LivingEntity caster, Entity entity) {
		return entity instanceof LivingEntity living && living.isAlive() && isAlly(caster, entity);
	}
}
