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
	// 26.1.2 dispatches the default damage knockback straight from hurtServer into knockback(DDD);
	// the route, call, invocation and write scopes all nest around that single native call site.
	@WrapOperation(method = "hurtServer", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
	private void stoneHinge$defaultRoute(LivingEntity entity, double x, double y, double z, Operation<Void> original,
			@Local(argsOnly = true) DamageSource source) {
		StoneHingeImpulseProbe.defaultRoute(entity, source, () ->
			StoneHingeImpulseProbe.defaultCall(entity, source, () ->
				StoneHingeImpulseProbe.knockbackInvocation(entity, source, () -> {
					Vec3 before = entity.getDeltaMovement(); boolean grounded = entity.onGround();
					original.call(entity, x, y, z);
					StoneHingeImpulseProbe.nativeWrite(entity, source, before, entity.getDeltaMovement(), grounded);
				})));
	}

	@WrapMethod(method = "knockback(DDD)V")
	private void stoneHinge$invocation(double x, double y, double z, Operation<Void> original) {
		dev.wildercord.gametest.stonehinge.peer.StoneHingeNaturalMotion.contaminate((LivingEntity) (Object) this, "Intervening knockback during natural dispatch");
		original.call(x, y, z);
	}
}
