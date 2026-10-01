package dev.wildercord.mixin;

import dev.wildercord.cast.CraftedRunes;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.PlayerAffinities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every way health comes back (Regeneration, a potion, food, a healing spell) goes through {@code heal}:
 * <ul>
 *   <li>a creature under a Gash can't heal at all (setting health outright, as a death save does, isn't
 *       healing, so that still works);</li>
 *   <li>healing others feeds life: health a player's spell restores to someone else (an ally, a pet) while
 *       it's being applied counts toward the caster's life affinity, by what it actually restored;</li>
 *   <li>and toward the spell's mastery (see {@code cast.Mastery}), which also counts healing yourself in danger.</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHealMixin {
	@Inject(method = "heal", at = @At("HEAD"), cancellable = true)
	private void wildercord$heal(float amount, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (CraftedRunes.gashed(self)) {
			ci.cancel();
			return;
		}
		if (amount > 0 && !self.level().isClientSide() && Effects.applying() instanceof ServerPlayer healer && healer != self && self.isAlive()) {
			PlayerAffinities.healed(healer, self, Math.min(amount, self.getMaxHealth() - self.getHealth()));
		}
		if (amount > 0 && !self.level().isClientSide() && Effects.applyingCast() != null && self.isAlive()) {
			dev.wildercord.cast.Mastery.healed(Effects.applyingCast(), self, Math.min(amount, self.getMaxHealth() - self.getHealth()));
		}
	}
}
