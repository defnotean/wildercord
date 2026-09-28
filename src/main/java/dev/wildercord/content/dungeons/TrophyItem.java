package dev.wildercord.content.dungeons;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/**
 * A dungeon boss's trophy: one of a kind, kept rather than spent. Held up (used), it lends a little
 * of its keeper's nature for a while, then rests for five minutes.
 */
public class TrophyItem extends Item {
	/** One effect it grants: which, for how long, how strong. */
	public record Boon(Holder<MobEffect> effect, int ticks, int amplifier) {}

	private static final int REST = 20 * 60 * 5;

	private final List<Boon> boons;
	private final SoundEvent sound;

	public TrophyItem(Properties properties, SoundEvent sound, Boon... boons) {
		super(properties);
		this.sound = sound;
		this.boons = List.of(boons);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			for (Boon boon : boons) {
				player.addEffect(new MobEffectInstance(boon.effect(), boon.ticks(), boon.amplifier()));
			}
			level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 0.8F, 1.2F);
			player.getCooldowns().addCooldown(stack, REST);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		builder.accept(Component.translatable(getDescriptionId() + ".use").withStyle(ChatFormatting.DARK_AQUA));
	}
}
