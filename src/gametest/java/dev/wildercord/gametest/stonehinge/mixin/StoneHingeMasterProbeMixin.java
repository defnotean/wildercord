package dev.wildercord.gametest.stonehinge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.aura.world.SwordMaster;
import dev.wildercord.gametest.stonehinge.StoneHingeImpulseProbe;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** A prototype provenance seam, not a production rule or a blanket Aura damage classification. */
@Mixin(SwordMaster.class)
public abstract class StoneHingeMasterProbeMixin {
	@Shadow private MastersRules.Move attack;
	@WrapOperation(method = "tickAttack", at = @At(value = "INVOKE",
		target = "Ldev/wildercord/aura/world/SwordMaster;projected(Lnet/minecraft/world/entity/LivingEntity;D)F"))
	private float stoneHinge$exactMelee(SwordMaster master, LivingEntity target, double damage, Operation<Float> original) {
		if (attack != MastersRules.Move.SWEEP && attack != MastersRules.Move.THRUST) return original.call(master, target, damage);
		return dev.wildercord.gametest.stonehinge.peer.StoneHingeNaturalRelease.observe(master, target,
            () -> StoneHingeImpulseProbe.masterMelee(master, target, () -> original.call(master, target, damage)));
	}
}
