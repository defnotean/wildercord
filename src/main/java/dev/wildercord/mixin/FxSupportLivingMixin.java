package dev.wildercord.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.wildercord.cast.packs.WardState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The support pack's wards between a creature and what strikes it (see {@link WardState#incoming}: Evade, Aegis,
 * Hearthguard, Bellward, Citadel, Ironhold, then Guardlink's share) and its refusals of harmful effects (Hexguard,
 * Staunch). Wraps {@code hurtServer} beside the aura's own wrap; neither depends on running first.
 */
@Mixin(LivingEntity.class)
public abstract class FxSupportLivingMixin {
	@WrapMethod(method = "hurtServer")
	private boolean wildercord$fxSupportWards(ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
		LivingEntity self = (LivingEntity) (Object) this;
		float through = WardState.incoming(self, source, damage);
		if (through < 0) {
			return false;
		}
		float shared = WardState.guardShare(self, source, through);
		boolean hurt = original.call(level, source, through - shared);
		if (hurt) {
			WardState.landed(self, source, through - shared, shared);
		}
		return hurt;
	}

	@Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
	private void wildercord$fxSupportRefuse(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
		if (WardState.refusesEffect((LivingEntity) (Object) this, effect)) {
			cir.setReturnValue(false);
		}
	}
}
