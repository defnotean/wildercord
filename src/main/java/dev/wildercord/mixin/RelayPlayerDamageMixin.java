package dev.wildercord.mixin;

import dev.wildercord.cast.RelayDamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Players override the native health-write method and need the same opt-in receipt check. */
@Mixin(Player.class)
public abstract class RelayPlayerDamageMixin {
	@Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
	private void wildercord$relayAdmission(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
		if (source instanceof RelayDamageSource relay && !relay.admits((Player) (Object) this)) ci.cancel();
	}
}
