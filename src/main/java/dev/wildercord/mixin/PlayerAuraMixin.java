package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.aura.AuraCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Aura in a player's blows (see {@link AuraCombat}): each blow is wrapped round the very call that hurts its foe, so a coated
 * blow lands at its coated strength and what it really took (killing blows too) is answered with aura and experience; a
 * spear's thrust likewise; and with Flow every aura weapon sweeps, wider and further, drawn as the aura's own sweep rather than
 * vanilla's grey crescent.
 */
@Mixin(Player.class)
public abstract class PlayerAuraMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void wildercord$auraSwing(Entity target, CallbackInfo ci) {
		AuraCombat.swing((Player) (Object) this);
	}

	@WrapOperation(method = "attack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean wildercord$auraBlow(Entity target, DamageSource source, float damage, Operation<Boolean> original) {
		return AuraCombat.blow((Player) (Object) this, target, source, damage, true, amount -> original.call(target, source, amount));
	}

	@Inject(method = "stabAttack", at = @At("HEAD"))
	private void wildercord$auraThrustBegins(EquipmentSlot slot, Entity target, float baseDamage, boolean dealsDamage, boolean dealsKnockback,
			boolean dismounts, CallbackInfoReturnable<Boolean> cir) {
		AuraCombat.swing((Player) (Object) this);
	}

	@WrapOperation(method = "stabAttack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean wildercord$auraThrust(Entity target, DamageSource source, float damage, Operation<Boolean> original) {
		return AuraCombat.blow((Player) (Object) this, target, source, damage, true, amount -> original.call(target, source, amount));
	}

	@WrapOperation(method = "doSweepAttack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean wildercord$auraSweepBlow(LivingEntity target, ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
		return AuraCombat.blow((Player) (Object) this, target, source, damage, false, amount -> original.call(target, level, source, amount));
	}

	@Inject(method = "isSweepAttack", at = @At("RETURN"), cancellable = true)
	private void wildercord$flowSweeps(boolean fullStrength, boolean critical, boolean knockback, CallbackInfoReturnable<Boolean> cir) {
		Player self = (Player) (Object) this;
		if (cir.getReturnValueZ() || !fullStrength || critical || knockback || !self.onGround() || !AuraCombat.flowSweeps(self)) {
			return;
		}
		double max = self.getSpeed() * 2.5;
		if (self.getKnownMovement().horizontalDistanceSqr() < max * max) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "doSweepAttack", at = @At("HEAD"))
	private void wildercord$flowSweepSeen(Entity entity, float baseDamage, DamageSource source, float strength, CallbackInfo ci) {
		AuraCombat.sweep((Player) (Object) this);
	}

	/** A Flow sweep is drawn as the aura's own wide trail (see {@code AuraFx}): vanilla's grey sweep crescent is left out under it. */
	@WrapOperation(method = "doSweepAttack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
	private int wildercord$flowSweepParticle(ServerLevel level, net.minecraft.core.particles.ParticleOptions particle, double x, double y, double z,
			int count, double dx, double dy, double dz, double speed, Operation<Integer> original) {
		if (AuraCombat.flowSweeps((Player) (Object) this)) {
			return 0;
		}
		return original.call(level, particle, x, y, z, count, dx, dy, dz, speed);
	}

	@WrapOperation(method = "doSweepAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"))
	private AABB wildercord$flowSweepWide(AABB box, double x, double y, double z, Operation<AABB> original) {
		Player self = (Player) (Object) this;
		return original.call(box, AuraCombat.sweepInflate(self, x), y, AuraCombat.sweepInflate(self, z));
	}

	@ModifyConstant(method = "doSweepAttack", constant = @Constant(doubleValue = 9.0))
	private double wildercord$flowSweepReach(double vanilla) {
		return AuraCombat.sweepRangeSq((Player) (Object) this, vanilla);
	}
}
