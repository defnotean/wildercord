package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.aura.StoneHingeReceipt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native mob melee provenance for Stone Hinge. Inert unless the target holds a paid catch. */
@Mixin(Mob.class)
public abstract class MobStoneHingeMixin {
	@WrapMethod(method = "doHurtTarget")
	private boolean wildercord$stoneHingeMelee(Entity target, Operation<Boolean> original) {
		return StoneHingeReceipt.melee((Mob) (Object) this, target, () -> original.call(target));
	}

	@WrapOperation(method = "doHurtTarget", at = @At(value = "INVOKE", target =
		"Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean wildercord$stoneHingeSource(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
		StoneHingeReceipt.meleeSource((Mob) (Object) this, target, source);
		return original.call(target, source, amount);
	}
}
