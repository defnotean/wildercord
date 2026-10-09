package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.wildercord.aura.StoneHingeReceipt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Records the native knockback write for an armed Stone Hinge catch. The write itself always happens unchanged. */
@Mixin(LivingEntity.class)
public abstract class LivingStoneHingeMixin {
	@WrapOperation(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V"))
	private void wildercord$stoneHingeImpulse(LivingEntity entity, double x, double y, double z, Operation<Void> original,
			@Local(argsOnly = true) DamageSource source) {
		Vec3 before = entity.getDeltaMovement(); boolean grounded = entity.onGround();
		original.call(entity, x, y, z);
		StoneHingeReceipt.impulse(entity, source, before, entity.getDeltaMovement(), grounded);
	}
}
