package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.aura.CounterSpellCapture;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.Effects;
import dev.wildercord.spell.SpellPlan;
import org.spongepowered.asm.mixin.Mixin;

/** Exact current effect node prevents nested add-on effects borrowing an outer inscription recipe. */
@Mixin(value = Effects.class, remap = false)
public abstract class CounterEffectCaptureMixin {
	@WrapMethod(method = "apply(Ldev/wildercord/cast/Cast;Ldev/wildercord/spell/SpellPlan$EffectNode;Ldev/wildercord/cast/Cast$Hit;D)V", require = 1, expect = 1, allow = 1)
	private static void wildercord$effect(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, double power, Operation<Void> original) {
		CounterSpellCapture.effect(cast, node, () -> original.call(cast, node, hit, power));
	}
}
