package dev.wildercord.wildlife;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

/**
 * Something a creature of the wild leaves behind. Its uses are ordinary recipes and brews (see
 * {@code tools/wildlife_art.py}); its tooltip carries a line of lore ({@code item.wildercord.<id>.lore}) and what it's
 * good for ({@code item.wildercord.<id>.use}).
 */
public class WildlifeItem extends Item {
	public WildlifeItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> wildercord$lines,TooltipFlag flag) {
		Consumer<Component> out = wildercord$lines::add;
		String key = "item." + BuiltInRegistries.ITEM.getKey(this).toLanguageKey();
		out.accept(Component.translatable(key + ".lore").withStyle(ChatFormatting.ITALIC).withColor(0xB8A8D8));
		out.accept(Component.translatable(key + ".use").withColor(0xB8B0C8));
	}
}
