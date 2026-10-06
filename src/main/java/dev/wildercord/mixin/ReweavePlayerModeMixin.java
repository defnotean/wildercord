package dev.wildercord.mixin;

import dev.wildercord.cast.ReweaveFields;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Latch an actual spectator transition, including a same-callback return before the field's next tick. */
@Mixin(ServerPlayer.class)
public abstract class ReweavePlayerModeMixin {
    @Inject(method = "setGameMode", at = @At("RETURN"))
    private void wildercord$reweaveSpectator(GameType mode, CallbackInfoReturnable<Boolean> result) {
        if (Boolean.TRUE.equals(result.getReturnValue()) && ((ServerPlayer)(Object)this).isSpectator())
            ReweaveFields.spectatorEntered((ServerPlayer)(Object)this);
    }
}
