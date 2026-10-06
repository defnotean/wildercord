package dev.wildercord.gametest.mixin;

import dev.wildercord.wildlife.Glimmerwing;
import dev.wildercord.wildlife.GlimmerwingLanternProbe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.LightLayer;
import dev.wildercord.wildlife.MoonreedSourceProbe;
import dev.wildercord.wildlife.MoonreedPriorityProbe;
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
 @WrapMethod(method="seekLure(Lnet/minecraft/server/level/ServerLevel;)V",require=1,expect=1,allow=1)
 private void wildercord$lanternSearch(ServerLevel level,Operation<Void> original){
  GlimmerwingLanternProbe.searchCall(level,(Glimmerwing)(Object)this,()->original.call(level));
 }
 @WrapOperation(method="seekLure(Lnet/minecraft/server/level/ServerLevel;)V",
  at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I",ordinal=1),require=1,expect=1,allow=1)
 private int wildercord$lanternSample(ServerLevel level,LightLayer layer,BlockPos at,Operation<Integer> original){
  return GlimmerwingLanternProbe.lightRead(level,(Glimmerwing)(Object)this,at,()->original.call(level,layer,at));
 }
 @Inject(method="seekLure(Lnet/minecraft/server/level/ServerLevel;)V",at=@At("HEAD"))
 private void wildercord$searchHead(ServerLevel level,CallbackInfo ci){MoonreedSourceProbe.search((Glimmerwing)(Object)this,false,flower,target,lure,retarget,lureLeft);MoonreedPriorityProbe.search((Glimmerwing)(Object)this,false,flower,lure,lureLeft);GlimmerwingLanternProbe.search(level,(Glimmerwing)(Object)this,false,flower,target,lure,retarget,lureLeft);}
 @Inject(method="seekLure(Lnet/minecraft/server/level/ServerLevel;)V",at=@At("RETURN"))
 private void wildercord$searchReturn(ServerLevel level,CallbackInfo ci){MoonreedSourceProbe.search((Glimmerwing)(Object)this,true,flower,target,lure,retarget,lureLeft);MoonreedPriorityProbe.search((Glimmerwing)(Object)this,true,flower,lure,lureLeft);GlimmerwingLanternProbe.search(level,(Glimmerwing)(Object)this,true,flower,target,lure,retarget,lureLeft);}
 @Inject(method="customServerAiStep(Lnet/minecraft/server/level/ServerLevel;)V",at=@At("RETURN"))
 private void wildercord$flightReturn(ServerLevel level,CallbackInfo ci){MoonreedSourceProbe.flight((Glimmerwing)(Object)this,flower,target,lure,retarget,lureLeft);GlimmerwingLanternProbe.flight(level,(Glimmerwing)(Object)this,flower,target,lure,retarget,lureLeft);}
}
