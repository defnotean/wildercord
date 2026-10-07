package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.aura.CounterSpellCapture;
import dev.wildercord.aura.RuneEtchings;
import dev.wildercord.cast.Cast;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Observes the inscription's original paid Cast and exact recipe, without replacing its effects. */
@Mixin(value = RuneEtchings.class, remap = false)
public abstract class CounterRuneCaptureMixin {
	@WrapMethod(method = "wake", require = 1, expect = 1, allow = 1)
	private static boolean wildercord$wake(ServerPlayer owner, LivingEntity target, float dealt, Operation<Boolean> original) {
		return CounterSpellCapture.wake(owner, target, () -> original.call(owner, target, dealt));
	}
	@WrapOperation(method = "wake", at = @At(value = "INVOKE", target = "Ldev/wildercord/cast/Effects;apply(Ldev/wildercord/cast/Cast;Ldev/wildercord/spell/SpellPlan$EffectNode;Ldev/wildercord/cast/Cast$Hit;)V"), require = 1, expect = 1, allow = 1)
	private static void wildercord$recipe(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, Operation<Void> original) {
		CounterSpellCapture.etching(cast, node, hit, () -> original.call(cast, node, hit));
	}
}
