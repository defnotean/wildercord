package dev.wildercord.mixin;

import dev.wildercord.monster.Tempering;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** A tempered creature's blow lands harder on a player, before armour and Resistance take their share ({@link Tempering#onPlayer}). */
@Mixin(Player.class)
public abstract class TemperedBlowMixin {
	@ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
	private float wildercord$temperedBlow(float amount, ServerLevel level, DamageSource source) {
		return Tempering.onPlayer(source, amount);
	}
}
