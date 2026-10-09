package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.aura.CounterSpellCapture;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.SpellDefence;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Binds the exact Cast to the final native non-player DamageSource received by the fixture body. */
@Mixin(value = SpellDefence.class, remap = false)
public abstract class CounterSpellDamageCaptureMixin {
	@WrapMethod(method = {
		"hurtAdmitted(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;FLdev/wildercord/cast/Cast;)Z",
		"hurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;FLdev/wildercord/cast/Cast;)Z"
	}, require = 2, expect = 2, allow = 2)
	private static boolean wildercord$cast(ServerLevel level, LivingEntity target, DamageSource source, float amount, Cast cast, Operation<Boolean> original) {
		return CounterSpellCapture.damage(cast, target, () -> original.call(level, target, source, amount, cast));
	}
	@WrapMethod(method = "hurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;FLjava/lang/Object;)Z", require = 1, expect = 1, allow = 1)
	private static boolean wildercord$identity(ServerLevel level, LivingEntity target, DamageSource source, float amount, Object identity, Operation<Boolean> original) {
		return CounterSpellCapture.identity(identity, () -> original.call(level, target, source, amount, identity));
	}
	@WrapOperation(method = "hurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;FLjava/lang/Object;)Z",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"), require = 1, expect = 1, allow = 1)
	private static boolean wildercord$nativeDamage(LivingEntity target, ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
		return CounterSpellCapture.nativeDamage(target, level, source, () -> original.call(target, level, source, amount));
	}
}
