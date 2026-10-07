package dev.wildercord.gametest.galevault.mixin;

import dev.wildercord.gametest.galevault.GaleVaultProbe;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class GaleVaultEntityMixin {
    @Inject(method = "move", at = @At("HEAD"))
    private void gale$beforeMove(MoverType type, Vec3 delta, CallbackInfo ci) {
        var trial = GaleVaultProbe.current((Entity) (Object) this); if (trial != null) trial.beforeMove(type, delta);
    }
    @Inject(method = "move", at = @At("RETURN"))
    private void gale$afterMove(MoverType type, Vec3 delta, CallbackInfo ci) {
        var trial = GaleVaultProbe.current((Entity) (Object) this); if (trial != null) trial.afterMove();
    }
    @Inject(method = "remove", at = @At("HEAD"))
    private void gale$removed(Entity.RemovalReason reason, CallbackInfo ci) {
        var trial = GaleVaultProbe.current((Entity) (Object) this);
        if (trial != null) trial.abort(GaleVaultProbe.Result.REMOVED);
    }
}
