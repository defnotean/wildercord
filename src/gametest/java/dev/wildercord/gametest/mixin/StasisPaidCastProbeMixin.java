package dev.wildercord.gametest.mixin;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Mastery;
import dev.wildercord.cast.StasisMechanicsProbe;
import dev.wildercord.spell.RuneDef;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

/** Observes the Cast actually made by the paid fixture call; does not construct or change a cast. */
@Mixin(value = Mastery.class, remap = false)
public abstract class StasisPaidCastProbeMixin {
    @Inject(method = "onCast(Lnet/minecraft/server/level/ServerPlayer;ILjava/util/List;Ldev/wildercord/cast/Cast;I)V",
        at = @At("HEAD"), require = 1, expect = 1, allow = 1)
    private static void wildercord$paid(ServerPlayer owner, int slot, List<RuneDef> runes, Cast cast, int spent, CallbackInfo ci) {
        StasisMechanicsProbe.observePaidCast(owner, slot, runes, cast);
    }
}
