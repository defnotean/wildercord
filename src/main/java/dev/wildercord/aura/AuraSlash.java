package dev.wildercord.aura;

import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Targets;
import dev.wildercord.config.Config;
import dev.wildercord.config.WildercordConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Aura Slash (Edge): tap the Aura key and the blade looses a crescent of aura that flies ahead at chest height, cutting each
 * foe in its path once (six at most) for the weapon's damage times the slash's factor, in the method's element. It's the Crescent
 * shape's flight and look in the aura's colour, without a spell: it costs aura and has a short cooldown, and spent past empty it
 * goes out weakened, with backlash. Against players it meets the spell defences ({@link AuraCombat#projected}).
 */
public final class AuraSlash {
	private AuraSlash() {}

	/** The technique: loose a slash. */
	public static boolean loose(ServerPlayer player) {
		long now = player.level().getGameTime();
		if (!Aura.holdsWeapon(player)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.no_weapon").withColor(0xA89CC8));
			return false;
		}
		AuraAttachments.State state = Aura.state(player);
		if (now < state.slashReadyAt()) {
			return false;
		}
		WildercordConfig.AuraSettings settings = Config.get().aura();
		double price = settings.slashCost();
		AuraRules.Spend paid = Aura.spend(player, price, "slash");
		Aura.state(player, Aura.state(player).slashReady(now + settings.slashCooldownTicks()));
		double weapon = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		double damage = AuraRules.slashDamage(weapon, settings.slashDamage(), Math.max(price, 1.0E-6), price <= 0 ? 1 : paid.paid())
			* settings.damageScale();
		if (damage <= 0) {
			// A technique failing at nothing: only the backlash.
			return false;
		}
		fly(player, damage, paid.backlash());
		return true;
	}

	/** The crescent's flight: a step a tick, cutting what it passes, stopped by a solid block. */
	static void fly(ServerPlayer player, double damage, boolean weak) {
		ServerLevel level = player.level();
		Vec3 aim = player.getViewVector(1.0F);
		Vec3 flat = new Vec3(aim.x, aim.y * 0.6, aim.z).normalize();
		Vec3 side = flat.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 sideF = side;
		Vec3 origin = player.getEyePosition().subtract(0, 0.45, 0);
		int color = Aura.color(player);
		int steps = (int) Math.ceil(AuraRules.SLASH_RANGE / AuraRules.SLASH_SPEED);
		Set<UUID> hit = new HashSet<>();
		boolean[] stopped = {false};
		player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		Aura.sound(player, "aura_slash", 1.0F, weak ? 0.8F : 1.0F);
		AuraVfx.slashStart(player, origin, flat, sideF, color);
		for (int t = 0; t < steps; t++) {
			int tick = t;
			double d = 0.8 + AuraRules.SLASH_SPEED * (t + 1);
			Scheduler.later(t + 1, () -> {
				if (stopped[0] || !player.isAlive() || player.level() != level) {
					return;
				}
				Vec3 front = origin.add(flat.scale(d));
				BlockPos pos = BlockPos.containing(front);
				if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
					stopped[0] = true;
					AuraVfx.slashEnd(level, front.subtract(flat.scale(0.4)), flat, color);
					return;
				}
				AuraVfx.slashStep(level, front, flat, sideF, color, tick, weak);
				double width = AuraRules.SLASH_WIDTH;
				for (Entity e : level.getEntities(player, new AABB(front, front).inflate(width / 2 + 1, 2.0, width / 2 + 1),
						e -> e instanceof LivingEntity && e.isAlive() && !hit.contains(e.getUUID()))) {
					if (hit.size() >= AuraRules.SLASH_TARGETS) {
						break;
					}
					Vec3 rel = e.getBoundingBox().getCenter().subtract(front);
					if (Math.abs(rel.dot(flat)) > AuraRules.SLASH_SPEED / 2 + 0.8 || Math.abs(rel.dot(sideF)) > width / 2 + e.getBbWidth() / 2
						|| Math.abs(rel.y) > 1.6) {
						continue;
					}
					hit.add(e.getUUID());
					if (Targets.canHarm(player, e)) {
						AuraCombat.projected(player, (LivingEntity) e, damage, true);
						AuraVfx.slashCut(level, e.getBoundingBox().getCenter(), flat, color);
					}
				}
			});
		}
	}
}
