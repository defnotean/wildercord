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
		if (kind == Kind.ROOT) {
			player.heal(4);
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
			for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(5), m -> m instanceof Enemy)) {
				mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 1), player);
			}
			server.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 32, 2, 1, 2, 0.1);
			level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1, 0.75F);
		} else {
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 1));
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
			for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(5), m -> m instanceof Enemy)) {
				var away = mob.position().subtract(player.position());
				if (away.horizontalDistanceSqr() > 0.01) {
					mob.push(away.x / Math.sqrt(away.horizontalDistanceSqr()) * 1.1, 0.45, away.z / Math.sqrt(away.horizontalDistanceSqr()) * 1.1);
				}
			}
			server.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 24, 2, 1, 2, 0.08);
			level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1, 1.25F);
		}
		player.getCooldowns().addCooldown(stack, 20 * 30);
		return InteractionResult.SUCCESS;
	}
}
