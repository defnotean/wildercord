package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.aura.world.BossDamage;
import dev.wildercord.aura.world.GuardianBite;
import dev.wildercord.aura.world.SwordMaster;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * A Master's cut goes partly through armour, and through Protection and Resistance (see {@link SwordMaster#pierce}); so does a
 * guardian's blow (see {@link GuardianBite}) and any boss's blow on a player (see {@link BossDamage#pierce}).
 * <p>
 * The whole method is wrapped because vanilla overwrites its {@code damage} parameter before returning, so an injector at
 * RETURN would see what the defence left as the blow it started from, and nothing would go through.
 */
@Mixin(LivingEntity.class)
public abstract class MasterPierceMixin {
	@WrapMethod(method = "getDamageAfterArmorAbsorb")
	private float wildercord$pierceArmour(DamageSource source, float damage, Operation<Float> original) {
		return pierced(source, damage, original.call(source, damage));
	}

	@WrapMethod(method = "getDamageAfterMagicAbsorb")
	private float wildercord$pierceProtection(DamageSource source, float damage, Operation<Float> original) {
		return pierced(source, damage, original.call(source, damage));
	}

	private float pierced(DamageSource source, float before, float after) {
		return BossDamage.pierce((LivingEntity) (Object) this, source, before, SwordMaster.pierce(before, after));
	}
}
