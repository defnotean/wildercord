package dev.wildercord.mixin;

import dev.wildercord.compat.MobEffectCompat;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.21.10 has no fabric mob-effect events: this stands in for {@code ServerMobEffectEvents.AFTER_ADD}. */
@Mixin(LivingEntity.class)
public abstract class MobEffectAddCompatMixin {
	@Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
		at = @At("RETURN"))
	private void wildercord$afterAdd(MobEffectInstance effect, Entity cause, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue()) {
			MobEffectCompat.fireAfterAdd(effect, (Entity) (Object) this);
		}
	}
}
