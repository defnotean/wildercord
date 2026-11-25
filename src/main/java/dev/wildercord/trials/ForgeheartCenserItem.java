package dev.wildercord.trials;

import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.party.Parties;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Crucible support relic: releases a warm foundry ember wave that heals and grants Strength & Fire Resistance. */
public final class ForgeheartCenserItem extends AuraWorld.Lore {
	private static final int COOLDOWN_TICKS = 180;

	public ForgeheartCenserItem(Properties properties) {
		super("forgeheart_censer", properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(held)) {
			if (player instanceof ServerPlayer sp) {
				sp.sendOverlayMessage(Component.translatable("message.wildercord.forgeheart_censer.cooldown", 9));
			}
			return InteractionResult.FAIL;
		}
		if (level instanceof ServerLevel server) {
			player.getCooldowns().addCooldown(held, COOLDOWN_TICKS);
			boolean mantleActive = SovereignsMantleItem.isWearingMantle(player);
			if (mantleActive && player instanceof ServerPlayer sp) {
				SovereignsMantleItem.tryTriggerFromRelic(server, sp);
			}
			for (ServerPlayer ally : server.players()) {
				if (ally == player || Parties.sameParty(player, ally)) {
					if (ally.distanceToSqr(player) <= 14.0 * 14.0) {
						ally.heal(4.0F);
						ally.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 240, 0, false, true));
						ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 160, 0, false, true));
						if (mantleActive) {
							ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0, false, true));
						}
						server.sendParticles(ParticleTypes.FLAME, ally.getX(), ally.getY() + 1.0, ally.getZ(), 8, 0.35, 0.35, 0.35, 0.03);
					}
				}
			}
			server.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.1F, 0.85F);
			if (player instanceof ServerPlayer sp) {
				sp.sendOverlayMessage(Component.translatable("message.wildercord.forgeheart_censer.pulse").withStyle(ChatFormatting.GOLD));
			}
		}
		return InteractionResult.SUCCESS;
	}
}
