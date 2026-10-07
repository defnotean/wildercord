package dev.wildercord.gametest.stonehinge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.gametest.stonehinge.peer.StoneHingeNaturalMotion;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

/** Counts even direct, nested or extra doTick calls while the single natural dispatch is armed. */
@Mixin(ServerPlayer.class)
public abstract class StoneHingeNaturalStepMixin {
    @WrapMethod(method = "doTick()V", require = 1, expect = 1, allow = 1)
    private void stoneHinge$nativeStep(Operation<Void> original) {
        StoneHingeNaturalMotion.step((ServerPlayer) (Object) this, () -> original.call());
    }
}
