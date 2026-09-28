package dev.wildercord.mixin;

import dev.wildercord.duel.Duels;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Two duellists can strike and shoot each other once the fight is on, even with PvP or friendly fire off. */
@Mixin(ServerPlayer.class)
public abstract class DuelHarmMixin {
	@Inject(method = "canHarmPlayer", at = @At("HEAD"), cancellable = true)
	private void wildercord$duel(Player target, CallbackInfoReturnable<Boolean> cir) {
		Boolean duel = Duels.canHarm((ServerPlayer) (Object) this, target);
		if (duel != null) {
			cir.setReturnValue(duel);
		}
	}
}
