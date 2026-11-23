package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.wildercord.cast.RelayDamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Revalidate a paid remote hit after native pre-damage callbacks, before health and armour writes. */
@Mixin(LivingEntity.class)
public abstract class RelayLivingDamageMixin {
	@WrapOperation(method = "hurt", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
	private void wildercord$relayKnockback(LivingEntity entity, double x, double y, double z, Operation<Void> original,
			@Local(argsOnly = true) DamageSource source) {
		if (source instanceof RelayDamageSource relay && !relay.admits(entity)) return;
		original.call(entity, x, y, z);
	}
	@Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
	private void wildercord$relayAdmission(DamageSource source, float amount, CallbackInfo ci) {
		if (source instanceof RelayDamageSource relay && !relay.admits((LivingEntity) (Object) this)) ci.cancel();
	}
}
