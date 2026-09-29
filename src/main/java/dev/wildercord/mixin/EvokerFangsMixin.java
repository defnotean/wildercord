package dev.wildercord.mixin;

import dev.wildercord.cast.ExplorerEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A Fangs spell's evoker fangs bite as the spell does, through the mod's spell damage and only what their
 * caster may harm (vanilla's bite anything off their owner's team, past Shields and the PvP scale): see
 * {@link ExplorerEffects#bite}.
 */
@Mixin(EvokerFangs.class)
public abstract class EvokerFangsMixin {
	@Inject(method = "dealDamageTo", at = @At("HEAD"), cancellable = true)
	private void wildercord$friendlyFire(LivingEntity target, CallbackInfo ci) {
		if (!ExplorerEffects.bite((EvokerFangs) (Object) this, target)) {
			ci.cancel();
		}
	}
}
