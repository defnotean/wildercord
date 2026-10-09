package dev.wildercord.gametest.mixin;

import dev.wildercord.cast.ExcisePlayableTest;
import dev.wildercord.cast.NativeZoneEmitters;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes the exact local cut before its caller publishes completion; never changes the result. */
@Mixin(value = NativeZoneEmitters.class, remap = false)
public abstract class ExciseCutProbeMixin {
    @Inject(method = "cut(Lnet/minecraft/server/level/ServerPlayer;Ldev/wildercord/cast/NativeZoneEmitters$Emitter;)Z",
        at = @At("RETURN"), require = 2, expect = 2, allow = 2)
    private static void wildercord$observeCut(ServerPlayer player, @Coerce Object emitter, CallbackInfoReturnable<Boolean> result) {
        if (result.getReturnValueZ()) ExcisePlayableTest.observeNativeCut(player, emitter);
    }
}
