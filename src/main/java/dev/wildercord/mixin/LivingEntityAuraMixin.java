package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.aura.AuraArmour;
import dev.wildercord.aura.AuraDominion;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.aura.WayBanner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Aura between a creature and what strikes it, wrapped round the whole of {@code hurtServer} (after a player's own checks,
 * difficulty and PvP, have had their say), in this order:
 * <ul>
 * <li>an Aura Step's untouchable moment (see {@link AuraStep}): the hit never lands;</li>
 * <li>a striker standing in a foe's Dominion (see {@link AuraDominion}): its blow lands weaker;</li>
 * <li>a body a Way steadies (see {@link WayBanner}: a Banner's cry, presence or sheltering Dominion, an unbroken Bulwark's awakening): what
 * a foe deals it lands weaker;</li>
 * <li>Aura Guard (see {@link AuraGuard}): a held guard takes its share off a blow or a projectile from in front, and a perfect
 * guard turns it aside whole;</li>
 * <li>aura armour (see {@link AuraArmour}): its share of whatever is left, paid for only if the hit really lands.</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAuraMixin {
	@WrapMethod(method = "hurtServer")
	private boolean wildercord$auraGuard(ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (AuraStep.untouchable(self, source)) {
			return false;
		}
		float weakened = dev.wildercord.aura.BladeTraits.harm(self, source, WayBanner.harm(self, source, AuraDominion.weakened(self, source, damage)));
		float through = AuraGuard.incoming(self, source, weakened);
		if (through < 0) {
			return false;
		}
		float armour = AuraArmour.share(self, source, through);
		boolean hurt = original.call(level, source, through - armour);
		if (hurt && armour > 0) {
			AuraArmour.paid(self, source, armour);
		}
		return hurt;
	}
}
