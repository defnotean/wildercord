package dev.wildercord.trials;

import dev.wildercord.aura.world.AuraWorld;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Vaelkhor's halberd: high-reach polearm whose right-click unleashes a sweeping Leyline Cleave. */
public final class SovereignHalberdItem extends AuraWorld.Lore {
	public SovereignHalberdItem(Properties properties) {
		super("sovereign_halberd", properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(held)) {
			return InteractionResult.FAIL;
		}
		if (level instanceof ServerLevel server) {
			player.getCooldowns().addCooldown(held, 40);
			if (!player.getAbilities().instabuild) {
				held.hurtAndBreak(2, player, hand.asEquipmentSlot());
			}
			boolean crowned = SovereignsMantleItem.isWearingMantle(player);
			if (crowned && player instanceof ServerPlayer sp) {
				SovereignsMantleItem.tryTriggerFromRelic(server, sp);
			}
			float yaw = player.getYRot();
			float damage = crowned ? 15.0F : 12.0F;
			AABB box = player.getBoundingBox().inflate(5.2, 1.8, 5.2);
			for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, box)) {
				if (target == player || target.isAlliedTo(player)) {
					continue;
				}
				Vec3 d = target.position().subtract(player.position());
				if (VaelkhorRules.inCleaveArc(d.x, d.z, yaw, 5.2, Math.toRadians(65.0))) {
					target.hurtServer(server, server.damageSources().playerAttack(player), damage);
					Vec3 push = d.multiply(1.0, 0.0, 1.0);
					double len = push.length();
					if (len > 0.01) {
						target.push(push.x / len * 0.55, 0.22, push.z / len * 0.55);
					}
				}
			}
			double baseRad = Math.toRadians(yaw);
			for (int step = -3; step <= 3; step++) {
				double ang = baseRad + Math.toRadians(step * 18.0);
				double fx = -Math.sin(ang);
				double fz = Math.cos(ang);
				server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
					player.getX() + fx * 3.0, player.getY() + 1.05, player.getZ() + fz * 3.0,
					2, 0.12, 0.12, 0.12, 0.02);
				server.sendParticles(ParticleTypes.END_ROD,
					player.getX() + fx * 3.6, player.getY() + 1.1, player.getZ() + fz * 3.6,
					1, 0.08, 0.08, 0.08, 0.02);
			}
			server.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.1F, 0.75F);
		}
		return InteractionResult.SUCCESS;
	}
}
