package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.monster.CofferMimic;
import dev.wildercord.monster.MimicRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import org.spongepowered.asm.mixin.Mixin;

/** Iron golems pass a tamed coffer by: golems take up with anything that is an Enemy, and a mimic stays a monster even after it is tame. */
@Mixin(IronGolem.class)
public abstract class IronGolemCofferKinMixin {
	// 1.21.11 folded canAttack into the target selector lambda (yarn name method_6498):
	// it keeps Enemy targets and drops creepers, which is the same seam.
	@WrapMethod(method = "method_6498")
	private static boolean wildercord$spareTamedCoffer(LivingEntity target, ServerLevel level, Operation<Boolean> original) {
		if (target instanceof CofferMimic coffer && MimicRules.sparesGolems(coffer.tamed())) return false;
		return original.call(target, level);
	}
}
