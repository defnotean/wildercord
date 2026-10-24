package dev.wildercord.mixin;

import dev.wildercord.aura.world.BossDamage;
import dev.wildercord.aura.world.GuardianBite;
import dev.wildercord.aura.world.SwordMaster;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A Master's cut goes partly through armour, and through Protection and Resistance (see {@link SwordMaster#pierce}); so does a
 * guardian's blow (see {@link GuardianBite}) and any boss's blow on a player (see {@link BossDamage#pierce}).
 */
@Mixin(LivingEntity.class)
public abstract class MasterPierceMixin {
	@Inject(method = "getDamageAfterArmorAbsorb", at = @At("RETURN"), cancellable = true)
	private void wildercord$pierceArmour(DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(BossDamage.pierce((LivingEntity) (Object) this, source, damage, SwordMaster.pierce(damage, cir.getReturnValueF())));
	}

	@Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true)
	private void wildercord$pierceProtection(DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(BossDamage.pierce((LivingEntity) (Object) this, source, damage, SwordMaster.pierce(damage, cir.getReturnValueF())));
	}
}
