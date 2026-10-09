package dev.wildercord.gametest.galevault.mixin;

import dev.wildercord.aura.world.SwordMaster;
import dev.wildercord.gametest.galevault.GaleVaultProbe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SwordMaster.class)
public abstract class GaleVaultMasterMixin {
    @Inject(method = "setNoAi", at = @At("HEAD"))
    private void gale$noAi(boolean noAi, CallbackInfo ci) {
        var trial = GaleVaultProbe.current((SwordMaster) (Object) this);
        if (trial != null && noAi) trial.abort(GaleVaultProbe.Result.NO_AI);
    }
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void gale$fallResult(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        var trial = GaleVaultProbe.current((SwordMaster) (Object) this);
        if (trial != null && source.is(DamageTypes.FALL)) trial.fallDamage(cir.getReturnValue());
    }
}
