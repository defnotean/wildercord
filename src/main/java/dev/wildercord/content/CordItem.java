package dev.wildercord.content;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** A Cord: worn in the Cord slot, it sets your sockets, spells and mana. */
public class CordItem extends Item {
	public final CordTier tier;

	public CordItem(CordTier tier, Properties properties) {
		super(properties);
		this.tier = tier;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("tooltip.wildercord.cord.stats", tier.sockets, tier.spells, tier.maxMana).withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("tooltip.wildercord.cord.tier", RuneItem.roman(tier.maxRuneTier)).withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("tooltip.wildercord.cord.wear").withStyle(ChatFormatting.DARK_AQUA));
	}
}
