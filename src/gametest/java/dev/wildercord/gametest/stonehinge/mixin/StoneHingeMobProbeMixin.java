package dev.wildercord.gametest.stonehinge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.gametest.stonehinge.StoneHingeImpulseProbe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mob.class)
public abstract class StoneHingeMobProbeMixin {
	@WrapOperation(method = "doHurtTarget", at = @At(value = "INVOKE", target =
		"Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean stoneHinge$nativeMelee(Entity target, ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
		if (!(target instanceof LivingEntity living)) return original.call(target, level, source, amount);
		return StoneHingeImpulseProbe.nativeMelee((Mob) (Object) this, living, source, () -> original.call(target, level, source, amount));
	}
}
