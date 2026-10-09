package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.aura.CounterSpellCapture;
import dev.wildercord.cast.AddonRunes;
import dev.wildercord.cast.Cast;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Records the actual native reaction Cast instead of trusting an ambient enclosing effect. */
@Mixin(value = AddonRunes.class, remap = false)
public abstract class CounterAddonCaptureMixin {
	@WrapMethod(method = "react(Ldev/wildercord/cast/Cast;Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/String;)D", require = 1, expect = 1, allow = 1)
	private static double wildercord$reaction(Cast cast, LivingEntity target, String element, Operation<Double> original) {
		return CounterSpellCapture.reaction(cast, target, element, () -> original.call(cast, target, element));
	}
}
