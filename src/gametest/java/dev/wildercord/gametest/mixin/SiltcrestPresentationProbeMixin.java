package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.wildlife.SiltcrestBittern;
import dev.wildercord.wildlife.SiltcrestPresentationProbe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only observes native boundaries. Original operations run exactly once with unchanged arguments/results. */
@Mixin(value=SiltcrestBittern.class,remap=false)
public abstract class SiltcrestPresentationProbeMixin {
 @Shadow private AbstractFish quarry;
 @Shadow private Vec3 committed;
 @Shadow private int epoch;
 @Shadow private int left;
 @Shadow private boolean pendingPreen;

 @WrapMethod(method="beginCoil(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/fish/AbstractFish;)Z",require=1,expect=1,allow=1)
 private boolean wildercord$coil(ServerLevel level,AbstractFish fish,Operation<Boolean> original) {
  boolean result=original.call(level,fish);if(result)wildercord$receipt("coil_admitted",null,null,null,null,null,false);return result;
 }
 @WrapMethod(method="strike(Lnet/minecraft/server/level/ServerLevel;)V",require=1,expect=1,allow=1)
 private void wildercord$strike(ServerLevel level,Operation<Void> original) {
  wildercord$receipt("strike_enter",null,null,null,null,null,false);boolean returned=false;
  try{original.call(level);returned=true;}finally{wildercord$receipt("strike_exit",null,null,null,null,null,!returned);}
 }
 @Inject(method="cancel(I)V",at=@At("HEAD"),require=1,expect=1,allow=1)
 private void wildercord$cancel(int pause,CallbackInfo ci){wildercord$receipt("cancel_enter",pause,null,null,null,null,false);}
 @WrapOperation(method="strike(Lnet/minecraft/server/level/ServerLevel;)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/animal/fish/AbstractFish;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"),require=1,expect=1,allow=1)
 private boolean wildercord$damage(AbstractFish fish,ServerLevel level,DamageSource damage,float amount,Operation<Boolean> original) {
  wildercord$receipt("damage_call",null,fish,damage,amount,null,false);Boolean result=null;
  try{result=original.call(fish,level,damage,amount);return result;}
  finally{wildercord$receipt("damage_return",null,fish,damage,amount,result,result==null);}
 }
 @Unique private void wildercord$receipt(String event,Integer pause,AbstractFish fish,DamageSource damage,Float amount,Boolean result,boolean threw) {
  SiltcrestPresentationProbe.observe((SiltcrestBittern)(Object)this,event,quarry,committed,epoch,left,pendingPreen,pause,fish,damage,amount,result,threw);
 }
}
