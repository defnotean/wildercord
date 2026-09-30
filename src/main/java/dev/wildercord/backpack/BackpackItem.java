package dev.wildercord.backpack;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * A backpack: use it to open it, or wear it in the Backpack slot and open it with the backpack key. What's in
 * it is kept in the item (see {@link Backpacks}). The game's own list of a container's items is hidden from
 * its tooltip, which lists them itself, between how full it is and how to open it.
 */
public class BackpackItem extends Item {
	public final BackpackTier tier;

	public BackpackItem(BackpackTier tier, Properties properties) {
		super(properties);
		this.tier = tier;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer server) {
			Backpacks.open(server, Backpacks.Place.hand(server, hand));
		}
		return InteractionResult.CONSUME;
	}

	/** Like a shulker box, it can't go in another container item: no backpack in a bundle, a shulker box or another backpack. */
	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}

	/** Burnt up or blown apart, it spills what it held, as a shulker box does. */
	@Override
	public void onDestroyed(ItemEntity entity) {
		ItemContainerContents contents = entity.getItem().set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		if (contents != null) {
			ItemUtils.onContainerDestroyed(entity, contents.nonEmptyItemCopyStream());
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		int used = Backpacks.used(stack);
		builder.accept(Component.translatable(used >= tier.slots() ? "tooltip.wildercord.backpack.full" : "tooltip.wildercord.backpack.fill", used, tier.slots())
			.withStyle(used >= tier.slots() ? ChatFormatting.GOLD : ChatFormatting.GRAY));
		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
		if (contents != null) {
			// The game's own list (five items and "and N more"), in the middle where it reads best.
			contents.addToTooltip(context, line -> builder.accept(line.copy().withStyle(ChatFormatting.GRAY)), flag, stack);
		}
		builder.accept(Component.translatable("tooltip.wildercord.backpack.open", Component.keybind("key.wildercord.open_backpack"))
			.withStyle(ChatFormatting.DARK_AQUA));
	}
}
