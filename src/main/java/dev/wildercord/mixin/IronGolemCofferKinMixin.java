package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.monster.CofferMimic;
import dev.wildercord.monster.MimicRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.spongepowered.asm.mixin.Mixin;

/** Iron golems pass a tamed coffer by: golems take up with anything that is an Enemy, and a mimic stays a monster even after it is tame. */
@Mixin(IronGolem.class)
public abstract class IronGolemCofferKinMixin {
	@WrapMethod(method = "canAttack")
	private boolean wildercord$spareTamedCoffer(LivingEntity target, Operation<Boolean> original) {
		if (target instanceof CofferMimic coffer && MimicRules.sparesGolems(coffer.tamed())) return false;
		return original.call(target);
	}
}
