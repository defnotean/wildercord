package dev.wildercord.mixin;

import dev.wildercord.world.upgrade.WorldUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Persist an edit fence before protected region mutation, including a later identical replacement (ABA). */
@Mixin(LevelChunk.class)
public abstract class UpgradeChunkWriteMixin {
	@Inject(method="setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Lnet/minecraft/world/level/block/state/BlockState;",at=@At("HEAD"),cancellable=true)
	private void wildercord$upgradeFence(BlockPos point,BlockState replacement,int flags,CallbackInfoReturnable<BlockState> result) {
		var chunk=(LevelChunk)(Object)this;
		if(chunk.getLevel() instanceof ServerLevel level && !chunk.getBlockState(point).equals(replacement)
				&& !WorldUpgrades.beforeExternalWrite(level,point,replacement))result.setReturnValue(null);
	}
}
