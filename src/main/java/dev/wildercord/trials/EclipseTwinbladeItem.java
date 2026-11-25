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

/** Nyxara's twinblades: right-click lunges forward along your gaze, severing foes in an astral slash. */
public final class EclipseTwinbladeItem extends AuraWorld.Lore {
	public EclipseTwinbladeItem(Properties properties) {
		super("eclipse_twinblade", properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(held)) {
			return InteractionResult.FAIL;
		}
		if (level instanceof ServerLevel server) {
			player.getCooldowns().addCooldown(held, 36);
			if (!player.getAbilities().instabuild) {
				held.hurtAndBreak(2, player, hand.asEquipmentSlot());
			}
			boolean mantleActive = SovereignsMantleItem.isWearingMantle(player);
			if (mantleActive && player instanceof ServerPlayer sp) {
				SovereignsMantleItem.tryTriggerFromRelic(server, sp);
			}
			Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
			Vec3 dir = look.lengthSqr() > 1.0e-4 ? look.normalize() : new Vec3(0.0, 0.0, 1.0);
			player.push(dir.x * 0.95, 0.14, dir.z * 0.95);
			player.hurtMarked = true;

			float dmg = mantleActive ? 15.0F : 12.0F;
			AABB box = player.getBoundingBox().inflate(4.5, 1.8, 4.5);
			for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, box)) {
				if (target == player || target.isAlliedTo(player)) continue;
				Vec3 d = target.position().subtract(player.position());
				if (VaelkhorRules.inCleaveArc(d.x, d.z, player.getYRot(), 4.5, Math.toRadians(65.0))) {
					target.hurtServer(server, server.damageSources().playerAttack(player), dmg);
				}
			}
			for (int step = 1; step <= 5; step++) {
				server.sendParticles(ParticleTypes.WITCH,
					player.getX() + dir.x * step * 0.8, player.getY() + 1.0, player.getZ() + dir.z * step * 0.8,
					3, 0.25, 0.2, 0.25, 0.02);
			}
			server.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.1F, 1.25F);
		}
		return InteractionResult.SUCCESS;
	}
}
