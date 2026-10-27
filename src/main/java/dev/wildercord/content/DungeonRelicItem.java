package dev.wildercord.content;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Reusable dungeon relics that give players a small, tactical spell without a Cord. */
public class DungeonRelicItem extends Item {
	public enum Kind { ROOT, STORM }

	private final Kind kind;

	public DungeonRelicItem(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("item.wildercord." + (kind == Kind.ROOT ? "rootbound_relic" : "stormglass_relic") + ".desc")
			.withStyle(ChatFormatting.GRAY));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.FAIL;
		}
		if (!(level instanceof ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
			var caster = dev.wildercord.data.CasterData.get(sp);
			caster.setMana(Math.min(caster.getMaxMana(sp), caster.getMana() + 25), sp);
			var aura = dev.wildercord.aura.AuraData.get(sp);
			if (aura.isAwakened()) {
				aura.setAura(Math.min(aura.maxAura(sp), aura.aura() + 20));
				aura.sync(sp);
			}
		}
		if (kind == Kind.ROOT) {
			player.heal(6);
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 160, 0));
			for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(6), m -> m instanceof Enemy)) {
				mob.hurtServer(server, server.damageSources().indirectMagic(player, player), 4.0F);
				mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 2), player);
			}
			dev.wildercord.cast.Fx.sendParticles(server, ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 32, 2, 1, 2, 0.1);
			level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1, 0.75F);
		} else {
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, 240, 1));
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 240, 0));
			for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(6), m -> m instanceof Enemy)) {
				mob.hurtServer(server, server.damageSources().indirectMagic(player, player), 5.0F);
				mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0), player);
				var away = mob.position().subtract(player.position());
				if (away.horizontalDistanceSqr() > 0.01) {
					mob.push(away.x / Math.sqrt(away.horizontalDistanceSqr()) * 1.35, 0.5, away.z / Math.sqrt(away.horizontalDistanceSqr()) * 1.35);
				}
			}
			dev.wildercord.cast.Fx.sendParticles(server, ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 24, 2, 1, 2, 0.08);
			level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1, 1.25F);
		}
		player.getCooldowns().addCooldown(stack, 20 * 20);
		return InteractionResult.SUCCESS;
	}
}
