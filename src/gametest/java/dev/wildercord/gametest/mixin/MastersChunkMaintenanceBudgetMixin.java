package dev.wildercord.gametest.mixin;

import dev.wildercord.aura.world.MasterChunkMaintenanceBudget;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.BooleanSupplier;

/** GameTest-only: preserve the ordinary invocation, tickets and callbacks, with a scoped admission allowance. */
@Mixin(ChunkMap.class)
public abstract class MastersChunkMaintenanceBudgetMixin {
	@Shadow @Final private ServerLevel level;

	@ModifyArg(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ChunkMap;processUnloads(Ljava/util/function/BooleanSupplier;)V"), index = 0)
	private BooleanSupplier wildercord$maintenanceBudget(BooleanSupplier original) {
		return MasterChunkMaintenanceBudget.supplement(level, original);
	}
}
