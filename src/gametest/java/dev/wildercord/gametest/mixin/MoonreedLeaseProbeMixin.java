package dev.wildercord.gametest.mixin;

import dev.wildercord.wildlife.MoonreedSourceProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Records the real lease result; never opens, closes, or changes a lease itself. */
@Mixin(targets="dev.wildercord.wildlife.MoonreedAdmission",remap=false)
public abstract class MoonreedLeaseProbeMixin {
 @Inject(method="open(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Ldev/wildercord/wildlife/MoonreedAdmission;",at=@At("RETURN"))
 private static void wildercord$leaseReturn(ServerLevel level,BlockPos root,CallbackInfoReturnable<Object> cir){MoonreedSourceProbe.lease(level,root,cir.getReturnValue()!=null);}
}
