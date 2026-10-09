package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.aura.StoneHingeReceipt;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.aura.world.SwordMaster;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Only a Master's released SWEEP or THRUST counts as melee for Stone Hinge; other Aura projections stay unclassified. */
@Mixin(SwordMaster.class)
public abstract class SwordMasterStoneHingeMixin {
	@Shadow private MastersRules.Move attack;
	@WrapOperation(method = "tickAttack", at = @At(value = "INVOKE",
		target = "Ldev/wildercord/aura/world/SwordMaster;projected(Lnet/minecraft/world/entity/LivingEntity;D)F"))
	private float wildercord$stoneHingeMelee(SwordMaster master, LivingEntity target, double damage, Operation<Float> original) {
		if (attack != MastersRules.Move.SWEEP && attack != MastersRules.Move.THRUST) return original.call(master, target, damage);
		return StoneHingeReceipt.master(master, target, () -> original.call(master, target, damage));
	}
}
