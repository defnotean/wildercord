package dev.wildercord.gametest.mixin;

import dev.wildercord.wildlife.Glimmerwing;
import dev.wildercord.wildlife.MoonreedSourceProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reads real search boundaries and flight state without calling AI or changing its inputs. */
@Mixin(value=Glimmerwing.class,remap=false)
public abstract class MoonreedFlightProbeMixin {
 @Shadow private BlockPos flower;
 @Shadow private Vec3 target;
 @Shadow private Vec3 lure;
 @Shadow private int retarget;
 @Shadow private int lureLeft;
 @Inject(method="seekLure(Lnet/minecraft/server/level/ServerLevel;)V",at=@At("HEAD"))
 private void wildercord$searchHead(ServerLevel level,CallbackInfo ci){MoonreedSourceProbe.search((Glimmerwing)(Object)this,false,flower,target,lure,retarget,lureLeft);}
 @Inject(method="seekLure(Lnet/minecraft/server/level/ServerLevel;)V",at=@At("RETURN"))
 private void wildercord$searchReturn(ServerLevel level,CallbackInfo ci){MoonreedSourceProbe.search((Glimmerwing)(Object)this,true,flower,target,lure,retarget,lureLeft);}
 @Inject(method="customServerAiStep(Lnet/minecraft/server/level/ServerLevel;)V",at=@At("RETURN"))
 private void wildercord$flightReturn(ServerLevel level,CallbackInfo ci){MoonreedSourceProbe.flight((Glimmerwing)(Object)this,flower,target,lure,retarget,lureLeft);}
}
