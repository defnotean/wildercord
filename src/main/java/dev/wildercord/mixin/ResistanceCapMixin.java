package dev.wildercord.mixin;

import dev.wildercord.cast.DefenceCaps;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Holds every Resistance a player is given to {@link DefenceCaps#resistance}: guards stack to Resistance II, dodges keep theirs. */
@Mixin(LivingEntity.class)
public abstract class ResistanceCapMixin {
	@ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
	private MobEffectInstance wildercord$capResistance(MobEffectInstance effect, MobEffectInstance same, Entity source) {
		if (!((Object) this instanceof Player) || effect == null || !effect.is(MobEffects.RESISTANCE)) {
			return effect;
		}
		int capped = DefenceCaps.resistance(effect.getAmplifier(), effect.getDuration());
		if (capped == effect.getAmplifier()) {
			return effect;
		}
		return new MobEffectInstance(MobEffects.RESISTANCE, effect.getDuration(), capped, effect.isAmbient(), effect.isVisible(), effect.showIcon());
	}
}
