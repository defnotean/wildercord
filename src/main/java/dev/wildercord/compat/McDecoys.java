package dev.wildercord.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

/**
 * 1.21.8 has no mannequin entity (it arrives with 1.21.9), so the void afterimage that baits enemies
 * stands on an armour stand: it still holds a name, health and a glow, and mobs still target it.
 */
public final class McDecoys {
	private McDecoys() {}

	/** A body to pose as the caster, or null when one cannot be placed. */
	public static LivingEntity afterimageBody(ServerLevel level, Player caster) {
		ArmorStand body = new ArmorStand(level);
		body.snapTo(caster.getX(), caster.getY(), caster.getZ(), caster.getYRot(), caster.getXRot());
		body.setYHeadRot(caster.getYHeadRot());
		body.setNoGravity(true);
		body.setNoBasePlate(true);
		body.setShowArms(true);
		body.setPersistenceRequired();
		body.setInvulnerable(true);
		return body;
	}

	/** Whether an entity is one of the mod's afterimage bodies. */
	public static boolean isAfterimageBody(LivingEntity entity) {
		return entity instanceof ArmorStand;
	}
}
