package dev.wildercord.gametest.galevault.mixin;

import dev.wildercord.gametest.galevault.GaleVaultProbe;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One exact owned body only: isolate goals, navigation and queued move/look/jump controls, not physics. */
@Mixin(Mob.class)
public abstract class GaleVaultMobMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void gale$isolateInputs(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (!GaleVaultProbe.owns(mob)) return;
        mob.getNavigation().stop();
        mob.setJumping(false);
        mob.xxa = 0; mob.yya = 0; mob.zza = 0;
        ci.cancel();
    }
}
