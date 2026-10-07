package dev.wildercord.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.CounterSpellCapture;
import dev.wildercord.aura.ResonantRules;
import dev.wildercord.aura.ResonantStrikes;
import dev.wildercord.cast.Cast;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.UUID;
import java.util.function.BiPredicate;

/** Native ledger pair, actual resource payment and consequent damage stay one transparent invocation. */
@Mixin(value = ResonantStrikes.class, remap = false)
public abstract class CounterResonanceCaptureMixin {
	@WrapMethod(method = "offer", require = 1, expect = 1, allow = 1)
	private static boolean wildercord$offer(ServerPlayer incoming, LivingEntity target, String element, float taken, boolean blade, Cast cast, Operation<Boolean> original) {
		return CounterSpellCapture.offer(incoming, target, blade, cast, () -> original.call(incoming, target, element, taken, blade, cast));
	}
	@WrapOperation(method = "offer", at = @At(value = "INVOKE", target = "Ldev/wildercord/aura/ResonantRules$Ledger;offer(Ljava/util/UUID;Ldev/wildercord/aura/ResonantRules$Hit;Ljava/util/function/BiPredicate;)Ldev/wildercord/aura/ResonantRules$Pair;"), require = 1, expect = 1, allow = 1)
	private static ResonantRules.Pair wildercord$pair(ResonantRules.Ledger ledger, UUID target, ResonantRules.Hit hit, BiPredicate<ResonantRules.Hit, ResonantRules.Hit> allowed, Operation<ResonantRules.Pair> original) {
		return CounterSpellCapture.ledger(target, hit, () -> original.call(ledger, target, hit, allowed));
	}
	@WrapOperation(method = "offer", at = @At(value = "INVOKE", target = "Ldev/wildercord/aura/Aura;spend(Lnet/minecraft/server/level/ServerPlayer;DLjava/lang/String;)Ldev/wildercord/aura/AuraRules$Spend;"), require = 1, expect = 1, allow = 1)
	private static AuraRules.Spend wildercord$payment(ServerPlayer player, double cost, String reason, Operation<AuraRules.Spend> original) {
		return CounterSpellCapture.spend(player, cost, reason, () -> original.call(player, cost, reason));
	}
	@WrapOperation(method = "offer", at = @At(value = "INVOKE", target = "Ldev/wildercord/cast/SpellDefence;resonantHurt(Ldev/wildercord/cast/Cast;Lnet/minecraft/world/entity/LivingEntity;F)V"), require = 1, expect = 1, allow = 1)
	private static void wildercord$resonance(Cast cast, LivingEntity target, float amount, Operation<Void> original) {
		CounterSpellCapture.resonant(cast, target, () -> original.call(cast, target, amount));
	}
}
