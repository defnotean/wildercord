package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.RuneBolt;
import dev.wildercord.party.Parties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;

/** Keep a vanilla projectile's shooter attached to fire and status mutations made during impact. */
@Mixin(Projectile.class)
public abstract class PartyProjectileMixin {
	@WrapMethod(method = "onHit")
	private void wildercord$partyProjectile(HitResult result, Operation<Void> original) {
		Projectile projectile = (Projectile) (Object) this;
		// A logged-out shooter's UUID still owns the arrow. With no living source to scope,
		// stop its impact before ignition/status side effects and consume it instead of looping
		// the same collision every tick. Potion and RuneBolt support payloads are left alone.
		if (!projectile.level().isClientSide() && projectile instanceof AbstractArrow
				&& projectile.getOwner() == null && result instanceof EntityHitResult hit
				&& Parties.blocksHarm(projectile, hit.getEntity())) {
			projectile.discard();
			return;
		}
		// Spell effects already carry a real Cast. A source-only scope must never erase that cast's duel rules.
		if (projectile.level().isClientSide() || projectile instanceof RuneBolt || Effects.applyingCast() != null
				|| !(projectile.getOwner() instanceof LivingEntity owner)) {
			original.call(result);
			return;
		}
		Effects.withSource(owner, () -> { original.call(result); });
	}
}
