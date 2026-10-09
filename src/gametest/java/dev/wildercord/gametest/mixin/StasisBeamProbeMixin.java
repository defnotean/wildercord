package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.StasisDeliveryProbe;
import dev.wildercord.cast.Vfx;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Original Beam and clip each execute once; the probe records only their real arguments/results. */
@Mixin(value = CastEngine.class, remap = false)
public abstract class StasisBeamProbeMixin {
    @WrapMethod(method = "beam(Ldev/wildercord/cast/Cast;Ldev/wildercord/spell/SpellPlan$Group;Ldev/wildercord/spell/SpellPlan$Link;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Ldev/wildercord/cast/Vfx$Theme;)V",
        require = 1, expect = 1, allow = 1)
    private static void wildercord$beam(Cast cast, SpellPlan.Group group, SpellPlan.Link link, Vec3 from, Vec3 direction,
            Vfx.Theme theme, Operation<Void> original) {
        StasisDeliveryProbe.beam(cast, group, from, direction, () -> original.call(cast, group, link, from, direction, theme));
    }

    @WrapOperation(method = "beam", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"),
        require = 1, expect = 1, allow = 1)
    private static BlockHitResult wildercord$clip(ServerLevel level, ClipContext context, Operation<BlockHitResult> original) {
        BlockHitResult result = original.call(level, context);
        StasisDeliveryProbe.clipped(result);
        return result;
    }

    @Inject(method = "onHit(Ldev/wildercord/cast/Cast;Ldev/wildercord/spell/SpellPlan$Group;Ldev/wildercord/cast/Cast$Hit;Ldev/wildercord/spell/SpellPlan$Link;)V",
        at = @At("HEAD"), require = 1, expect = 1, allow = 1)
    private static void wildercord$hit(Cast cast, SpellPlan.Group group, Cast.Hit hit, SpellPlan.Link link, CallbackInfo ci) {
        StasisDeliveryProbe.hit(cast, hit);
    }
}
