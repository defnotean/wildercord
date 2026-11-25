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

/** Eclipse support relic: aligns starlight around allies for Swiftness II, Night Vision, Absorption, and healing. */
public final class AstrolabeDialItem extends AuraWorld.Lore {
	private static final int COOLDOWN_TICKS = 180;

	public AstrolabeDialItem(Properties properties) {
		super("astrolabe_dial", properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(held)) {
			if (player instanceof ServerPlayer sp) {
				sp.sendOverlayMessage(Component.translatable("message.wildercord.astrolabe_dial.cooldown", 9));
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
						ally.heal(5.0F);
						ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 180, 1, false, true));
						ally.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
						ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, mantleActive ? 200 : 120, 0, false, true));
						server.sendParticles(ParticleTypes.END_ROD, ally.getX(), ally.getY() + 1.2, ally.getZ(), 8, 0.35, 0.35, 0.35, 0.02);
					}
				}
			}
			server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.3F, 1.25F);
			if (player instanceof ServerPlayer sp) {
				sp.sendOverlayMessage(Component.translatable("message.wildercord.astrolabe_dial.pulse").withStyle(ChatFormatting.LIGHT_PURPLE));
			}
		}
		return InteractionResult.SUCCESS;
	}
}
