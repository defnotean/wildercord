package dev.wildercord.mixin;
import dev.wildercord.cast.PocketChestOwnership;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Draft only: root must add the mixin entry when the complete Pocket Current increment is promoted. */
@Mixin(BlockItem.class)
public abstract class NextChestPlacementMixin {
 @Inject(method="placeBlock",at=@At("RETURN"))
 private void wildercord$pocketOwner(BlockPlaceContext context,BlockState state,CallbackInfoReturnable<Boolean> result){
  if(result.getReturnValueZ() && context.getLevel() instanceof ServerLevel level && context.getPlayer() instanceof ServerPlayer player)
   PocketChestOwnership.placed(player,level,context.getClickedPos());
 }
}
