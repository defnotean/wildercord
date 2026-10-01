package dev.wildercord.content;

import dev.wildercord.cast.SoulWeaving;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Sneaking bypasses block interaction, so imprinting belongs to the held item. */
public final class BlankRuneItem extends Item {
	public BlankRuneItem(Properties properties) { super(properties); }
	@Override public InteractionResult useOn(UseOnContext context) {
		var player=context.getPlayer();
		if(player!=null && player.isShiftKeyDown()
				&& context.getLevel().getBlockState(context.getClickedPos()).is(WildercordBlocks.FUSION_ALTAR)) {
			if(player instanceof ServerPlayer server) SoulWeaving.imprint(server,context.getItemInHand());
			return InteractionResult.SUCCESS;
		}
		return super.useOn(context);
	}
}
