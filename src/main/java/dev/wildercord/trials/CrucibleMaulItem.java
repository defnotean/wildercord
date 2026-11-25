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

/** Kharvok's greathammer: right-click delivers a 4.8-block Anvil Shatter ground slam with fiery knock-up. */
public final class CrucibleMaulItem extends AuraWorld.Lore {
	public CrucibleMaulItem(Properties properties) {
		super("crucible_maul", properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(held)) {
			return InteractionResult.FAIL;
		}
		if (level instanceof ServerLevel server) {
			player.getCooldowns().addCooldown(held, 50);
			if (!player.getAbilities().instabuild) {
				held.hurtAndBreak(2, player, hand.asEquipmentSlot());
			}
			boolean mantleActive = SovereignsMantleItem.isWearingMantle(player);
			if (mantleActive && player instanceof ServerPlayer sp) {
				SovereignsMantleItem.tryTriggerFromRelic(server, sp);
			}
			float dmg = mantleActive ? 16.0F : 13.0F;
			AABB box = player.getBoundingBox().inflate(4.8, 2.0, 4.8);
			for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, box)) {
				if (target == player || target.isAlliedTo(player)) continue;
				if (target.distanceToSqr(player) <= 4.8 * 4.8) {
					target.hurtServer(server, server.damageSources().playerAttack(player), dmg);
					target.push(0.0, 0.42, 0.0);
				}
			}
			for (int i = 0; i < 16; i++) {
				double ang = (Math.PI * 2.0 * i) / 16.0;
				server.sendParticles(ParticleTypes.FLAME,
					player.getX() + Math.cos(ang) * 2.8, player.getY() + 0.35, player.getZ() + Math.sin(ang) * 2.8,
					2, 0.1, 0.1, 0.1, 0.03);
			}
			server.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 0.5, player.getZ(), 10, 1.4, 0.2, 1.4, 0.05);
			server.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.1F, 0.65F);
		}
		return InteractionResult.SUCCESS;
	}
}
