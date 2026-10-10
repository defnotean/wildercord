package dev.wildercord.wildlife;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

/** A Skyray Bridle, won from the Tenth Circle's tribulation: use it to call your skyray down and ride it (see {@link SkyMountRules}). */
public class SkyrayBridleItem extends Item {
	public SkyrayBridleItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
			if (BondedSkyray.call(server, serverPlayer) == null) {
				return InteractionResult.FAIL;
			}
			player.getCooldowns().addCooldown(stack, SkyMountRules.CALL_COOLDOWN);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("tooltip.wildercord.skyray_bridle").withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("tooltip.wildercord.skyray_bridle.ride").withStyle(ChatFormatting.DARK_AQUA));
	}
}
