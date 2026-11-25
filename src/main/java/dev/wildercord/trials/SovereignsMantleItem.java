package dev.wildercord.trials;

import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.party.Parties;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tri-Seal Sovereign Capstone chestplate: projects a 12-block party Resistance/Regeneration aura
 * while worn and unleashes Sovereign's Convergence when shift-used (or channeled through Trial relics).
 */
public final class SovereignsMantleItem extends AuraWorld.Lore {
	public static final int CONVERGENCE_COOLDOWN_TICKS = 400;
	public static final double AURA_RADIUS = 12.0;
	public static final double BURST_RADIUS = 8.0;

	public SovereignsMantleItem(Properties properties) {
		super("sovereigns_mantle", properties);
	}

	public static boolean isWearingMantle(Player player) {
		return player.getItemBySlot(EquipmentSlot.CHEST).is(CoopTrialContent.SOVEREIGNS_MANTLE);
	}

	public static void tickServer(MinecraftServer server) {
		if ((server.getTickCount() % 40) != 0) {
			return;
		}
		for (ServerPlayer wearer : server.getPlayerList().getPlayers()) {
			if (!wearer.isAlive() || wearer.isSpectator() || !isWearingMantle(wearer)) {
				continue;
			}
			ServerLevel level = wearer.level();
			for (ServerPlayer ally : level.players()) {
				if (ally.isAlive() && !ally.isSpectator()
					&& (ally == wearer || Parties.sameParty(wearer, ally))
					&& ally.distanceToSqr(wearer) <= AURA_RADIUS * AURA_RADIUS) {
					ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0, false, false));
					ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, false));
				}
			}
		}
	}

	/** Triggers Sovereign's Convergence from a worn chestplate when sneaking with a Trial relic. */
	public static boolean tryTriggerFromRelic(ServerLevel server, ServerPlayer player) {
		if (!player.isShiftKeyDown() || !isWearingMantle(player)) {
			return false;
		}
		return triggerConvergence(server, player, player.getItemBySlot(EquipmentSlot.CHEST));
	}

	public static boolean triggerConvergence(ServerLevel server, ServerPlayer player, ItemStack cooldownStack) {
		if (player.getCooldowns().isOnCooldown(cooldownStack)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.mantle.cooldown"));
			return false;
		}
		player.getCooldowns().addCooldown(cooldownStack, CONVERGENCE_COOLDOWN_TICKS);
		AABB box = player.getBoundingBox().inflate(BURST_RADIUS, 3.0, BURST_RADIUS);
		for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, box)) {
			if (target == player || target.isAlliedTo(player)) {
				continue;
			}
			if (target instanceof ServerPlayer other && Parties.sameParty(player, other)) {
				continue;
			}
			if (target.distanceToSqr(player) <= BURST_RADIUS * BURST_RADIUS) {
				target.hurtServer(server, server.damageSources().playerAttack(player), 14.0F);
				target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1, false, true));
				target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, true));
				Vec3 push = target.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
				double len = push.length();
				if (len > 0.01) {
					target.push(push.x / len * 0.75, 0.30, push.z / len * 0.75);
				}
			}
		}
		for (ServerPlayer ally : server.players()) {
			if (ally.isAlive() && !ally.isSpectator()
				&& (ally == player || Parties.sameParty(player, ally))
				&& ally.distanceToSqr(player) <= AURA_RADIUS * AURA_RADIUS) {
				ally.heal(6.0F);
				ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 0, false, true));
			}
		}
		server.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
		for (int i = 0; i < 16; i++) {
			double ang = (Math.PI * 2.0 * i) / 16.0;
			server.sendParticles(ParticleTypes.END_ROD,
				player.getX() + Math.cos(ang) * 3.0, player.getY() + 0.9, player.getZ() + Math.sin(ang) * 3.0,
				2, 0.15, 0.2, 0.15, 0.02);
		}
		server.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2F, 1.15F);
		player.sendOverlayMessage(Component.translatable("message.wildercord.mantle.convergence").withStyle(ChatFormatting.GOLD));
		return true;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) {
			if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
				return triggerConvergence(server, sp, held) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
			}
			return InteractionResult.SUCCESS;
		}
		return super.use(level, player, hand);
	}
}
