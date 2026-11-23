package dev.wildercord.content;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** The grand reward for saving the sacred pig. It's exactly what it says it is. */
public class SacredPigLeftNippleItem extends Item {
	public SacredPigLeftNippleItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("tooltip.wildercord.sacred_pig_left_nipple").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		builder.accept(Component.translatable("tooltip.wildercord.sacred_pig_left_nipple.lore").withStyle(ChatFormatting.DARK_GRAY));
	}
}
