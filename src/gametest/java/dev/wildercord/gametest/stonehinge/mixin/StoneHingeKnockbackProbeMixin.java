package dev.wildercord.gametest.stonehinge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.wildercord.gametest.stonehinge.StoneHingeImpulseProbe;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class StoneHingeKnockbackProbeMixin {
	@WrapOperation(method = "hurtServer", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/LivingEntity;dealDefaultKnockback(Lnet/minecraft/world/damagesource/DamageSource;FZ)V"))
	private void stoneHinge$defaultRoute(LivingEntity entity, DamageSource source, float damage, boolean blocked, Operation<Void> original) {
		StoneHingeImpulseProbe.defaultRoute(entity, source, () -> original.call(entity, source, damage, blocked));
	}

	@WrapOperation(method = "dealDefaultKnockback", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDDLnet/minecraft/world/damagesource/DamageSource;F)V"))
	private void stoneHinge$defaultCall(LivingEntity entity, double strength, double x, double z, DamageSource source, float damage, Operation<Void> original) {
		StoneHingeImpulseProbe.defaultCall(entity, source, () -> original.call(entity, strength, x, z, source, damage));
	}

	@WrapMethod(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V")
	private void stoneHinge$invocation(double strength, double x, double z, DamageSource source, float damage, boolean force, Operation<Void> original) {
        dev.wildercord.gametest.stonehinge.peer.StoneHingeNaturalMotion.contaminate((LivingEntity) (Object) this, "Intervening knockback during natural dispatch");
		StoneHingeImpulseProbe.knockbackInvocation((LivingEntity) (Object) this, source,
			() -> original.call(strength, x, z, source, damage, force));
	}

	@WrapOperation(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V"))
	private void stoneHinge$nativeWrite(LivingEntity entity, double x, double y, double z, Operation<Void> original,
			@Local(argsOnly = true) DamageSource source) {
		Vec3 before = entity.getDeltaMovement(); boolean grounded = entity.onGround();
		original.call(entity, x, y, z);
		StoneHingeImpulseProbe.nativeWrite(entity, source, before, entity.getDeltaMovement(), grounded);
	}
}
