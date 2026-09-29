package dev.wildercord.mixin;

import dev.wildercord.cast.PlayerAffinities;
import net.minecraft.advancements.triggers.TameAnimalTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Taming feeds life. The game tells this advancement trigger whenever any animal is tamed (a wolf, a cat, a
 * parrot, a horse...), always with the player who tamed it, so it's the one place to hear of every kind.
 */
@Mixin(TameAnimalTrigger.class)
public abstract class TameAnimalTriggerMixin {
	@Inject(method = "trigger", at = @At("HEAD"))
	private void wildercord$tamed(ServerPlayer player, Animal animal, CallbackInfo ci) {
		PlayerAffinities.tamed(player);
	}
}
