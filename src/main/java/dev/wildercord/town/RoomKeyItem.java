package dev.wildercord.town;

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

/** The Inn Cook's Room Key: use it at a Wayfarer Inn to rent a room there. See {@link InnRooms}. */
public class RoomKeyItem extends Item {
	public RoomKeyItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer server) {
			if (!atInn(server)) {
				server.sendOverlayMessage(Component.translatable("message.wildercord.room.no_inn").withStyle(ChatFormatting.RED));
				return InteractionResult.FAIL;
			}
			InnRooms.rent(server);
			if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	/** Whether {@code player} stands at an inn: near one of its keepers (a caravan doesn't let rooms). */
	static boolean atInn(ServerPlayer player) {
		return !player.level().getEntitiesOfClass(WayfarerKeeper.class, player.getBoundingBox().inflate(InnRoomRules.NEAR_KEEPER),
			keeper -> keeper.role() != WayfarerKeeper.Role.CARAVANEER).isEmpty();
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
		out.accept(Component.translatable("tooltip.wildercord.room_key", InnRoomRules.RENT_DAYS).withStyle(ChatFormatting.GRAY));
	}
}
