package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.aura.AuraCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Aura in a player's blows (see {@link AuraCombat}): each blow is wrapped round the very call that hurts its foe, so a coated
 * blow lands at its coated strength and what it really took (killing blows too) is answered with aura and experience; and with
 * Flow every aura weapon sweeps, wider and further, drawn as the aura's own sweep rather than vanilla's grey crescent.
 * (1.21.10 inlines the sweep into {@code Player.attack} and has no spear thrust: the sweep wraps target attack itself, and a
 * Flow sweep on a weapon vanilla wouldn't sweep with draws the aura crescent without extra damage.)
 */
@Mixin(Player.class)
public abstract class PlayerAuraMixin {
	@Unique private boolean wildercord$fullStrength;
	@Unique private boolean wildercord$swept;

	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void wildercord$auraSwing(Entity target, CallbackInfo ci) {
		wildercord$fullStrength = false;
		wildercord$swept = false;
		if ((Object) this instanceof net.minecraft.server.level.ServerPlayer player && ((dev.wildercord.cast.ExciseCasting.blocking(player) || dev.wildercord.cast.LessonPackCasting.blocking(player)) || dev.wildercord.aura.MastersArts.committed(player))) { ci.cancel(); return; }
		if ((Object) this instanceof net.minecraft.server.level.ServerPlayer player) {
			dev.wildercord.aura.SwordStrings.attackBegins(player);
			wildercord$fullStrength = player.getAttackStrengthScale(0F) >= 0.999F;
		}
		AuraCombat.swing((Player) (Object) this);
	}

	@WrapOperation(method = "attack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean wildercord$auraBlow(Entity target, DamageSource source, float damage, Operation<Boolean> original) {
		if ((Object) this instanceof net.minecraft.server.level.ServerPlayer player) dev.wildercord.aura.MasterForms.cancel(player);
		return AuraCombat.blow((Player) (Object) this, target, source, damage, true, amount -> original.call(target, source, amount));
	}

	@WrapOperation(method = "attack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
	private void wildercord$auraSweepBlow(LivingEntity target, DamageSource source, float damage, Operation<Void> original) {
		wildercord$swept = true;
		AuraCombat.sweep((Player) (Object) this);
		AuraCombat.blow((Player) (Object) this, target, source, damage, false, amount -> { original.call(target, source, amount); return true; });
	}

	/** Flow sweeps with every aura weapon: where vanilla's inlined sweep did not run, the aura crescent is drawn on its own. */
	@Inject(method = "attack", at = @At("RETURN"))
	private void wildercord$flowSweeps(Entity target, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (!(self instanceof ServerPlayer) || wildercord$swept || !wildercord$fullStrength || !self.onGround() || !AuraCombat.flowSweeps(self)) {
			return;
		}
		double max = self.getSpeed() * 2.5;
		if (self.getKnownMovement().horizontalDistanceSqr() < max * max) {
			self.sweepAttack();
			AuraCombat.sweep(self);
		}
	}

	/** A Flow sweep is drawn as the aura's own wide trail (see {@code AuraFx}): vanilla's grey sweep crescent is left out under it. */
	@WrapOperation(method = "attack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
	private int wildercord$flowSweepParticle(ServerLevel level, net.minecraft.core.particles.ParticleOptions particle, double x, double y, double z,
			int count, double dx, double dy, double dz, double speed, Operation<Integer> original) {
		if (AuraCombat.flowSweeps((Player) (Object) this)) {
			return 0;
		}
		return original.call(level, particle, x, y, z, count, dx, dy, dz, speed);
	}

	@WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"))
	private AABB wildercord$flowSweepWide(AABB box, double x, double y, double z, Operation<AABB> original) {
		Player self = (Player) (Object) this;
		return original.call(box, AuraCombat.sweepInflate(self, x), y, AuraCombat.sweepInflate(self, z));
	}

	@ModifyConstant(method = "attack", constant = @Constant(doubleValue = 9.0))
	private double wildercord$flowSweepReach(double vanilla) {
		return AuraCombat.sweepRangeSq((Player) (Object) this, vanilla);
	}
}
