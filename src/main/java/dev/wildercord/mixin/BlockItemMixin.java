package dev.wildercord.mixin;

import dev.wildercord.cast.Imbuing;
import dev.wildercord.content.WildercordComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An imbued block item, placed: the block becomes a glyph holding what the item held. */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Inject(method = "placeBlock", at = @At("RETURN"))
	private void wildercord$placeImbued(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && context.getLevel() instanceof ServerLevel level && context.getPlayer() instanceof ServerPlayer player
				&& context.getItemInHand().has(WildercordComponents.IMBUED)) {
			Imbuing.placed(player, level, context.getClickedPos(), context.getClickedFace(), context.getItemInHand().get(WildercordComponents.IMBUED));
		}
	}
}
