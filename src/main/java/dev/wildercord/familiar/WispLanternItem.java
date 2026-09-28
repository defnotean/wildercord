package dev.wildercord.familiar;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

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
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer server)) {
			return InteractionResult.SUCCESS;
		}
		Bonds bonds = Familiars.get(server);
		if (bonds.bonds().isEmpty()) {
			server.sendOverlayMessage(Component.translatable("message.wildercord.wisp.lantern_empty").withStyle(ChatFormatting.GRAY));
			return InteractionResult.FAIL;
		}
		if (player.isShiftKeyDown()) {
			String from = bonds.out().isEmpty() ? bonds.last() : bonds.out();
			Bonds.Bond next = bonds.bonds().size() == 1 && bonds.out().isEmpty() ? bonds.bonds().getFirst() : bonds.after(from);
			if (next != null && !next.id().equals(bonds.out())) {
				Familiars.callOut(server, next);
			}
			return InteractionResult.SUCCESS;
		}
		if (!Familiars.recall(server, false)) {
			Bonds.Bond last = bonds.get(bonds.last());
			Familiars.callOut(server, last != null ? last : bonds.bonds().getFirst());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("tooltip.wildercord.wisp_lantern").withStyle(ChatFormatting.GRAY));
	}
}
