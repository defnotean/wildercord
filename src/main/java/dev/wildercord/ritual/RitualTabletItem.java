package dev.wildercord.ritual;

import dev.wildercord.ritual.RitualRules.Ritual;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * A Ritual Tablet: sneak-use to choose a ritual, hold use to channel it. When the channel completes, the ritual is worked
 * and its cost taken from every caster in the circle (see {@link Rituals}).
 */
public class RitualTabletItem extends Item {
	public RitualTabletItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown()) {
			if (player instanceof ServerPlayer server) {
				Ritual next = Rituals.ritual(stack).next();
				Rituals.setRitual(stack, next);
				server.sendOverlayMessage(Component.translatable("message.wildercord.ritual.chosen",
					Component.translatable("ritual.wildercord." + next.id), next.minCircle).withColor(0xB8A8FF));
				server.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.0F);
			}
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer server) {
			Rituals.Result result = Rituals.check(server.level(), server, Rituals.ritual(stack));
			if (result != Rituals.Result.DONE) {
				refuse(server, Rituals.ritual(stack), result);
				return InteractionResult.FAIL;
			}
		}
		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	private static void refuse(ServerPlayer player, Ritual ritual, Rituals.Result result) {
		String key = "message.wildercord.ritual." + result.name().toLowerCase(java.util.Locale.ROOT);
		player.sendOverlayMessage(Component.translatable(key, ritual.minCircle, new ItemStack(Rituals.reagent(ritual)).getHoverName()).withStyle(ChatFormatting.GRAY));
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return Rituals.ritual(stack).channel;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
		if (!(level instanceof ServerLevel server) || remaining % 4 != 0) return;
		int total = getUseDuration(stack, entity);
		double progress = 1.0 - (double) remaining / total;
		double radius = 3.0 - progress * 1.5;
		double angle = (total - remaining) * 0.35;
		for (int i = 0; i < 4; i++) {
			double a = angle + i * Math.PI / 2;
			server.sendParticles(ParticleTypes.ENCHANT, entity.getX() + Math.cos(a) * radius, entity.getY() + 0.1,
				entity.getZ() + Math.sin(a) * radius, 2, 0.05, 0.05, 0.05, 0.0);
		}
		if (remaining % 20 == 0) {
			server.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F,
				0.6F + (float) progress);
		}
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof ServerPlayer player && level instanceof ServerLevel server) {
			Ritual ritual = Rituals.ritual(stack);
			Rituals.Result result = Rituals.perform(server, player, ritual);
			if (result != Rituals.Result.DONE) refuse(player, ritual, result);
		}
		return stack;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		Ritual ritual = Rituals.ritual(stack);
		builder.accept(Component.translatable("tooltip.wildercord.ritual_tablet.set", Component.translatable("ritual.wildercord." + ritual.id))
			.withStyle(ChatFormatting.LIGHT_PURPLE));
		builder.accept(Component.translatable("ritual.wildercord." + ritual.id + ".desc").withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("tooltip.wildercord.ritual_tablet.cost", (int) ritual.cost, ritual.minCircle,
			new ItemStack(Rituals.reagent(ritual)).getHoverName()).withStyle(ChatFormatting.DARK_AQUA));
		builder.accept(Component.translatable("tooltip.wildercord.ritual_tablet.use").withStyle(ChatFormatting.DARK_GRAY));
	}
}
