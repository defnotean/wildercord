package dev.wildercord.mixin;

import dev.wildercord.duel.Duels;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two duellists can strike and shoot each other once the fight is on, even with PvP or friendly fire
 * off. Vanilla asks this both ways round (the victim about its attacker when a blow lands, the
 * attacker about its target when an arrow or a wolf picks one), so the answer here is the same
 * either way; what a duellist may do to anyone else is settled in {@link Duels} where the harm lands.
 */
@Mixin(ServerPlayer.class)
public abstract class DuelHarmMixin {
	@Inject(method = "canHarmPlayer", at = @At("HEAD"), cancellable = true)
	private void wildercord$duel(Player other, CallbackInfoReturnable<Boolean> cir) {
		Boolean duel = Duels.between((ServerPlayer) (Object) this, other);
		if (duel != null) {
			cir.setReturnValue(duel);
		}
	}
}
