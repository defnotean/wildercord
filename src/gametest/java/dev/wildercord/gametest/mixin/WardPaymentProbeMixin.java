package dev.wildercord.gametest.mixin;
import dev.wildercord.cast.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Wards.class)
public abstract class WardPaymentProbeMixin {
 @Inject(method="foresight",at=@At("HEAD"),remap=false)
 private static void foresight(Cast cast,LivingEntity target,int ticks,int charges,CallbackInfo ci){WardPaymentProbe.accept(cast);}
 @Inject(method="reflect(Ldev/wildercord/cast/Cast;Lnet/minecraft/world/entity/LivingEntity;ID)V",at=@At("HEAD"),remap=false)
 private static void reflect(Cast cast,LivingEntity target,int ticks,double fraction,CallbackInfo ci){WardPaymentProbe.accept(cast);}
}
