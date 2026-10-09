package dev.wildercord.mixin;

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
	@Inject(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V", at = @At("HEAD"), cancellable = true)
	private void wildercord$relayKnockback(double strength, double x, double z, DamageSource source, float multiplier, boolean force, CallbackInfo ci) {
		if (source instanceof RelayDamageSource relay && !relay.admits((LivingEntity) (Object) this)) ci.cancel();
	}
	@Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
	private void wildercord$relayAdmission(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
		if (source instanceof RelayDamageSource relay && !relay.admits((LivingEntity) (Object) this)) ci.cancel();
	}
}
