package dev.wildercord.mixin;

import dev.wildercord.cast.Effects;
import dev.wildercord.cast.PlayerAffinities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Healing others feeds life: health a player's spell restores to someone else (an ally, a pet) while it's
 * being applied counts toward the caster's life affinity, by what it actually restored.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHealMixin {
	@Inject(method = "heal", at = @At("HEAD"))
	private void wildercord$healedBySpell(float amount, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (amount > 0 && !self.level().isClientSide() && Effects.applying() instanceof ServerPlayer healer && healer != self && self.isAlive()) {
			PlayerAffinities.healed(healer, self, Math.min(amount, self.getMaxHealth() - self.getHealth()));
		}
	}
}
