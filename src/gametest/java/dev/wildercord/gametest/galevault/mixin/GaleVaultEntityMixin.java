package dev.wildercord.gametest.galevault.mixin;

import dev.wildercord.gametest.galevault.GaleVaultProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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
    @WrapOperation(method = "checkFallDamage", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/Block;fallOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;D)V"))
    private void gale$originalFallOn(Block block, Level sourceLevel, BlockState state, BlockPos pos, Entity entity, double argument, Operation<Void> original) throws Throwable {
        var trial = GaleVaultProbe.current((Entity) (Object) this);
        if (trial == null) { original.call(block, sourceLevel, state, pos, entity, argument); return; }
        trial.fallTrace.callback(() -> original.call(block, sourceLevel, state, pos, entity, argument),
            invocation -> trial.beforeOriginalFallOn(invocation, block, sourceLevel, state, pos, entity, argument), trial::afterOriginalFallOn);
    }
    @WrapMethod(method = "checkFallDamage")
    private void gale$originalFallCheck(double actualY, boolean ground, BlockState state, BlockPos pos, Operation<Void> original) throws Throwable {
        var trial = GaleVaultProbe.current((Entity) (Object) this);
        if (trial == null) { original.call(actualY, ground, state, pos); return; }
        trial.fallTrace.scope(() -> original.call(actualY, ground, state, pos),
            receipt -> trial.afterOriginalFallCheck(receipt, actualY, ground, state, pos));
    }
    @Inject(method = "remove", at = @At("HEAD"))
    private void gale$removed(Entity.RemovalReason reason, CallbackInfo ci) {
        var trial = GaleVaultProbe.current((Entity) (Object) this);
        if (trial != null) trial.abort(GaleVaultProbe.Result.REMOVED);
    }
}
