package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve vanilla saving and its failure, while exposing the otherwise discarded cause in native tests. */
@Mixin(PlayerDataStorage.class)
public abstract class PlayerSaveDiagnosticsMixin {
	@Inject(method = "save", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"), require = 1, allow = 1)
	private void wildercord$saveFailure(Player player, CallbackInfo ci, @Local Exception failure) {
		dev.wildercord.Wildercord.LOGGER.error("Native player save failed for " + player.getUUID(), failure);
	}
}
