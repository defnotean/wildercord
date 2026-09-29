package dev.wildercord.mixin;

import dev.wildercord.cast.CraftedRunes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A creature under a Gash can't heal: every way health comes back (Regeneration, a potion, food, a
 * healing spell) goes through {@code heal}. Setting health outright (a death save) isn't healing, so it
 * still works.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHealMixin {
	@Inject(method = "heal(F)V", at = @At("HEAD"), cancellable = true)
	private void wildercord$gashed(float amount, CallbackInfo ci) {
		if (CraftedRunes.gashed((LivingEntity) (Object) this)) {
			ci.cancel();
		}
	}
}
