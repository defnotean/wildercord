package dev.wildercord.gametest.galevault.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.gametest.galevault.GaleVaultProbe;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe the real call chain. The original travel, gravity and post-travel body pushing remain intact. */
@Mixin(LivingEntity.class)
public abstract class GaleVaultLivingMixin {
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void gale$beforeTick(CallbackInfo ci) {
        var trial = GaleVaultProbe.current((LivingEntity) (Object) this); if (trial != null) trial.beforeTick();
    }
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void gale$afterTick(CallbackInfo ci) {
        var trial = GaleVaultProbe.current((LivingEntity) (Object) this); if (trial != null) trial.afterTick();
    }
    @Inject(method = "travel", at = @At("HEAD"))
    private void gale$nativeTravel(Vec3 input, CallbackInfo ci) {
        var trial = GaleVaultProbe.current((LivingEntity) (Object) this); if (trial != null) trial.beforeTravel();
    }
    @Inject(method = "travel", at = @At("TAIL"))
    private void gale$afterTravel(Vec3 input, CallbackInfo ci) {
        var trial = GaleVaultProbe.current((LivingEntity) (Object) this); if (trial != null) trial.afterTravel();
    }
    @WrapMethod(method = "knockback(DDD)V")
    private void gale$nativeKnockback(double x, double y, double z, Operation<Void> original) {
        var trial = GaleVaultProbe.current((LivingEntity) (Object) this); if (trial != null) trial.knockedBack();
        original.call(x, y, z);
    }
}
