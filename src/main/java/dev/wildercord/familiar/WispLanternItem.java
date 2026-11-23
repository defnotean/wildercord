package dev.wildercord.familiar;

import net.minecraft.world.InteractionResultHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/**
 * The Wisp Lantern, where familiars rest. Use it to send the familiar that's out home, or to call
 * the last one out again; sneak and use it to call the next one (a familiar waiting at a Wellstone
 * leaves it and comes). The familiars themselves are kept on their owner, so any lantern is yours.
 * Its tooltip (drawn on the client) lists them.
 */
public class WispLanternItem extends Item {
	public WispLanternItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) { ItemStack mcUseStack = player.getItemInHand(hand);
		if (!(player instanceof ServerPlayer server)) {
			return InteractionResultHolder.success(mcUseStack);
		}
		Bonds bonds = Familiars.get(server);
		if (bonds.bonds().isEmpty()) {
			server.displayClientMessage(Component.translatable("message.wildercord.wisp.lantern_empty").withStyle(ChatFormatting.GRAY), true);
			return InteractionResultHolder.fail(mcUseStack);
		}
		if (player.isShiftKeyDown()) {
			String from = bonds.out().isEmpty() ? bonds.last() : bonds.out();
			Bonds.Bond next = bonds.bonds().size() == 1 && bonds.out().isEmpty() ? bonds.bonds().getFirst() : bonds.after(from);
			if (next != null && !next.id().equals(bonds.out())) {
				Familiars.callOut(server, next);
			}
			return InteractionResultHolder.success(mcUseStack);
		}
		if (!Familiars.recall(server, false)) {
			Bonds.Bond last = bonds.get(bonds.last());
			Familiars.callOut(server, last != null ? last : bonds.bonds().getFirst());
		}
		return InteractionResultHolder.success(mcUseStack);
	}

	@Override
	public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> wildercord$lines,TooltipFlag flag) {
		java.util.function.Consumer<Component> builder = wildercord$lines::add;
		builder.accept(Component.translatable("tooltip.wildercord.wisp_lantern").withStyle(ChatFormatting.GRAY));
	}
}
