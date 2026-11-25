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

/** Supportive trial relic: chimes a 14-block resonance that cleanses, heals, and fortifies nearby party allies. */
public final class LeylineBellCharmItem extends AuraWorld.Lore {
	private static final int COOLDOWN_TICKS = 160;

	public LeylineBellCharmItem(Properties properties) {
		super("leyline_bell_charm", properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(held)) {
			if (player instanceof ServerPlayer sp) {
				sp.sendOverlayMessage(Component.translatable("message.wildercord.bell_charm.cooldown", 8));
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
						ally.heal(6.0F);
						ally.removeEffect(MobEffects.WEAKNESS);
						ally.removeEffect(MobEffects.SLOWNESS);
						ally.removeEffect(MobEffects.WITHER);
						ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true));
						ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 0, false, true));
						if (mantleActive) {
							ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0, false, true));
						}
						server.sendParticles(ParticleTypes.NOTE, ally.getX(), ally.getEyeY() + 0.3, ally.getZ(), 6, 0.3, 0.2, 0.3, 0.1);
					}
				}
			}
			for (int i = 0; i < 12; i++) {
				double ang = (Math.PI * 2.0 * i) / 12.0;
				server.sendParticles(ParticleTypes.END_ROD,
					player.getX() + Math.cos(ang) * 2.4, player.getY() + 1.0, player.getZ() + Math.sin(ang) * 2.4,
					1, 0.08, 0.12, 0.08, 0.01);
			}
			server.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.3F, 1.35F);
			if (player instanceof ServerPlayer sp) {
				sp.sendOverlayMessage(Component.translatable("message.wildercord.bell_charm.chime").withStyle(ChatFormatting.AQUA));
			}
		}
		return InteractionResult.SUCCESS;
	}
}
