package dev.wildercord.mixin;

import dev.wildercord.party.Parties;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reject hostile effects before they mutate an ally; helpful buffs and self-costs still apply. */
@Mixin(LivingEntity.class)
public abstract class PartyEffectMixin {
	@Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
		at = @At("HEAD"), cancellable = true)
	private void wildercord$partyEffect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
		if (effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) return;
		LivingEntity target = (LivingEntity) (Object) this;
		if (source != null ? Parties.blocksHarm(source, target) : Parties.blocksCurrentHarm(target)) {
			cir.setReturnValue(false);
		}
	}
}
