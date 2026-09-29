package dev.wildercord.mixin;

import dev.wildercord.cast.ExplorerEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A Fangs spell's evoker fangs bite only what their caster may harm (vanilla's bite anything off their
 * owner's team, a pet or a friend walking onto them): see {@link ExplorerEffects#mayBite}.
 */
@Mixin(EvokerFangs.class)
public abstract class EvokerFangsMixin {
	@Inject(method = "dealDamageTo", at = @At("HEAD"), cancellable = true)
	private void wildercord$friendlyFire(LivingEntity target, CallbackInfo ci) {
		if (!ExplorerEffects.mayBite((EvokerFangs) (Object) this, target)) {
			ci.cancel();
		}
	}
}
