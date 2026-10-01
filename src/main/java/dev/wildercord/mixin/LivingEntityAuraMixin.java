package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.aura.AuraGuard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Aura Guard (see {@link AuraGuard}) between a guarding player and what strikes them: a held guard takes its share off a blow
 * or a projectile from in front, and a perfect guard turns it aside whole (the hit never lands). Wrapped round the whole of
 * {@code hurtServer}, after a player's own checks (difficulty, PvP) have had their say.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAuraMixin {
	@WrapMethod(method = "hurtServer")
	private boolean wildercord$auraGuard(ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
		float through = AuraGuard.incoming((LivingEntity) (Object) this, source, damage);
		if (through < 0) {
			return false;
		}
		return original.call(level, source, through);
	}
}
