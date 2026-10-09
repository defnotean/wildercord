package dev.wildercord.gametest.mixin;

import dev.wildercord.wildlife.Glimmerwing;
import dev.wildercord.wildlife.MoonreedBlock;
import dev.wildercord.wildlife.MoonreedSourceProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes only the ordinary three-argument production entity-source overload. */
@Mixin(value=MoonreedBlock.class,remap=false)
public abstract class MoonreedPollinationProbeMixin {
 @Inject(method="pollinate(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Ldev/wildercord/wildlife/Glimmerwing;)Z",at=@At("HEAD"))
 private static void wildercord$pollinateHead(ServerLevel level,BlockPos root,Glimmerwing moth,CallbackInfoReturnable<Boolean> cir){MoonreedSourceProbe.pollination(moth,level,root,null);}
 @Inject(method="pollinate(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Ldev/wildercord/wildlife/Glimmerwing;)Z",at=@At("RETURN"))
 private static void wildercord$pollinateReturn(ServerLevel level,BlockPos root,Glimmerwing moth,CallbackInfoReturnable<Boolean> cir){MoonreedSourceProbe.pollination(moth,level,root,cir.getReturnValueZ());}
}
