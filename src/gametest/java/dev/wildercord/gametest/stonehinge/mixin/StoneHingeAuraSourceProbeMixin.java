package dev.wildercord.gametest.stonehinge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.aura.world.AuraFighter;
import dev.wildercord.gametest.stonehinge.StoneHingeImpulseProbe;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AuraFighter.class)
public abstract class StoneHingeAuraSourceProbeMixin {
	@WrapOperation(method = "projected", at = @At(value = "INVOKE", target =
		"Ldev/wildercord/aura/world/MasterHitReceipt;source(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)V"))
	private void stoneHinge$bind(LivingEntity attacker, LivingEntity target, DamageSource source, Operation<Void> original) {
		original.call(attacker, target, source);
		StoneHingeImpulseProbe.createdSource(attacker, target, source);
	}
}
