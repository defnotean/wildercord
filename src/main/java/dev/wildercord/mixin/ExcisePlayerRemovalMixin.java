package dev.wildercord.mixin;

import dev.wildercord.cast.ExciseCasting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** PlayerList.remove saves the body before removing it from the world. Persist paid cancellation first. */
@Mixin(PlayerList.class)
public abstract class ExcisePlayerRemovalMixin {
	@Inject(method = "remove", at = @At("HEAD"), require = 1, allow = 1)
	private void wildercord$retireExciseBeforeSave(ServerPlayer player, CallbackInfo ci) {
		ExciseCasting.retire(player);
	}
}
