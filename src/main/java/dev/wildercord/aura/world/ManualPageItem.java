package dev.wildercord.aura.world;

import dev.wildercord.aura.BreathingManualItem;
import dev.wildercord.aura.BreathingMethod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A page torn from a breathing manual, carried in a fallen knight's armour since the knight was a swordsman. Its method is the
 * same component a manual's is ({@code wildercord:breathing_method}); four pages of one method and a book bind into that
 * method's manual at a crafting table ({@link ManualPagesRecipe}).
 */
public class ManualPageItem extends Item {
	public ManualPageItem(Properties properties) {
		super(properties);
	}

	/** A page of {@code method}. */
	public static ItemStack of(Item page, String method) {
		ItemStack stack = new ItemStack(page);
		stack.set(BreathingManualItem.METHOD, method);
		return stack;
	}

	public static Optional<BreathingMethod> methodOf(ItemStack stack) {
		return BreathingManualItem.methodOf(stack);
	}

	@Override
	public Component getName(ItemStack stack) {
		return methodOf(stack).<Component>map(m -> Component.translatable("item.wildercord.manual_page.named", Component.translatable(m.nameKey())))
			.orElseGet(() -> Component.translatable("item.wildercord.manual_page"));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("item.wildercord.manual_page.lore").withStyle(ChatFormatting.ITALIC).withColor(0xC8B89A));
		methodOf(stack).ifPresent(method -> {
			if (!method.element().isEmpty()) {
				builder.accept(Component.translatable("tooltip.wildercord.breathing_manual.element",
					Component.translatable("element.wildercord." + method.element()).withColor(method.color())).withStyle(ChatFormatting.GRAY));
			}
		});
		builder.accept(Component.translatable("item.wildercord.manual_page.use", AuraWorldRules.PAGES_PER_MANUAL).withColor(0xB8D8A8));
	}
}
