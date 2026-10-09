package dev.wildercord.gametest.mixin;

import dev.wildercord.aura.arts.ArtWardsHardeningTest;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Passive native setter observation for the explicitly armed lifetime fixture only. */
@Mixin(ServerPlayer.class)
public abstract class ReleasedOwnerSetterProbeMixin {
	@Inject(method = "setServerLevel(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("HEAD"), require = 1, expect = 1, allow = 1)
	private void wildercord$setterHead(ServerLevel destination, CallbackInfo callback) {
		ArtWardsHardeningTest.observeSetter((ServerPlayer) (Object) this, true);
	}
	@Inject(method = "setServerLevel(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("TAIL"), require = 1, expect = 1, allow = 1)
	private void wildercord$setterTail(ServerLevel destination, CallbackInfo callback) {
		ArtWardsHardeningTest.observeSetter((ServerPlayer) (Object) this, false);
	}
}
