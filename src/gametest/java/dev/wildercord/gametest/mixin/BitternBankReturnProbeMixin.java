package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.wildlife.EcologyReturnProbe;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="dev.wildercord.wildlife.BitternBankReturnGoal",remap=false)
public abstract class BitternBankReturnProbeMixin {
 @Inject(method="canUse()Z",at=@At("RETURN"),require=1,expect=1,allow=1)
 private void wildercord$admission(CallbackInfoReturnable<Boolean> cir){EcologyReturnProbe.goal(this,"admission",cir.getReturnValue());}
 @Inject(method="canContinueToUse()Z",at=@At("RETURN"),require=1,expect=1,allow=1)
 private void wildercord$continuation(CallbackInfoReturnable<Boolean> cir){EcologyReturnProbe.goal(this,"continue",cir.getReturnValue());}
 @Inject(method="start()V",at=@At("TAIL"),require=1,expect=1,allow=1)
 private void wildercord$start(CallbackInfo ci){EcologyReturnProbe.goal(this,"start",null);}
 @Inject(method="tick()V",at=@At("RETURN"),require=1)
 private void wildercord$tick(CallbackInfo ci){EcologyReturnProbe.goal(this,"tick",null);}
 @Inject(method="stop()V",at=@At("HEAD"),require=1,expect=1,allow=1)
 private void wildercord$stop(CallbackInfo ci){EcologyReturnProbe.goal(this,"stop",null);}
 @WrapOperation(method="tick()V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/navigation/PathNavigation;moveTo(Lnet/minecraft/world/level/pathfinder/Path;D)Z"),require=1,expect=1,allow=1)
 private boolean wildercord$move(PathNavigation navigation,Path path,double speed,Operation<Boolean> original){return EcologyReturnProbe.move(this,navigation,path,speed,original);}
}
