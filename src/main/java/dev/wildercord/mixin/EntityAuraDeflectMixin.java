package dev.wildercord.mixin;

import dev.wildercord.aura.AuraGuard;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A perfect Aura Guard turns a projectile before it hits (see {@link AuraGuard#deflection}), through the same deflection a
 * breeze turns arrows with, so the projectile flies back instead of landing.
 */
@Mixin(Entity.class)
public abstract class EntityAuraDeflectMixin {
	@Inject(method = "deflection", at = @At("HEAD"), cancellable = true)
	private void wildercord$auraDeflect(Projectile projectile, CallbackInfoReturnable<ProjectileDeflection> cir) {
		ProjectileDeflection turned = AuraGuard.deflection((Entity) (Object) this, projectile);
		if (turned != null) {
			cir.setReturnValue(turned);
		}
	}
}
