package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.aura.arts.ReleasedArtOwner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Generation barrier around actual world assignment, before reentrant Fabric AFTER-level listeners run. */
@Mixin(ServerPlayer.class)
public abstract class ReleasedArtOwnerLevelMixin {
	@WrapMethod(method = "setServerLevel(Lnet/minecraft/server/level/ServerLevel;)V")
	private void wildercord$retireDepartingGeneration(ServerLevel destination, Operation<Void> original) {
		try (var change = ReleasedArtOwner.changingWorld((ServerPlayer) (Object) this, destination)) {
			original.call(destination);
		}
	}
	@Inject(method = "setServerLevel(Lnet/minecraft/server/level/ServerLevel;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setLevel(Lnet/minecraft/world/level/Level;)V", shift = At.Shift.AFTER),
		require = 1, expect = 1, allow = 1)
	private void wildercord$assignedGeneration(ServerLevel destination, CallbackInfo callback) {
		ReleasedArtOwner.assignedWorld((ServerPlayer) (Object) this);
	}

}
