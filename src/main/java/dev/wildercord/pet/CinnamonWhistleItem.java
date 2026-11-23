package dev.wildercord.pet;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Every copy resolves the same server-owned companion and shared saved cooldown. */
public final class CinnamonWhistleItem extends Item {
	public CinnamonWhistleItem(Properties properties) { super(properties); }
	@Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) { ItemStack mcUseStack = player.getItemInHand(hand);
		if (player instanceof ServerPlayer owner && player.getItemInHand(hand).is(this)) CinnamonCompanion.whistle(owner);
		return InteractionResultHolder.success(mcUseStack);
	}
}
