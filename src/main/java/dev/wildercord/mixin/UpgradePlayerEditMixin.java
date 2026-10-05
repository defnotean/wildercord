package dev.wildercord.mixin;

import dev.wildercord.world.upgrade.UpgradeEdits;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Successful future block placements, creative and survival alike, are exclusion evidence. */
@Mixin(BlockItem.class)
public abstract class UpgradePlayerEditMixin {
	@Inject(method="placeBlock",at=@At("RETURN"))
	private void wildercord$rememberSuccessfulEdit(BlockPlaceContext context,BlockState state,CallbackInfoReturnable<Boolean> result) {
		if(result.getReturnValueZ() && context.getPlayer()!=null && context.getLevel() instanceof ServerLevel level)
			UpgradeEdits.successfulPlayerEdit(level,context.getClickedPos());
	}
}
