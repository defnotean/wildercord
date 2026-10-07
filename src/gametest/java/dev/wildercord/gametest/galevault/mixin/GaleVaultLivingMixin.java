package dev.wildercord.gametest.galevault.mixin;

import dev.wildercord.gametest.galevault.GaleVaultProbe;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
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
    @Inject(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V", at = @At("HEAD"))
    private void gale$nativeKnockback(double strength, double x, double z, DamageSource source, float damage, boolean force, CallbackInfo ci) {
        var trial = GaleVaultProbe.current((LivingEntity) (Object) this); if (trial != null) trial.knockedBack();
    }
}
