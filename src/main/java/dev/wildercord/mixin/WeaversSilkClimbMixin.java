package dev.wildercord.mixin;

import dev.wildercord.aura.world.WeaversSilk;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Weaver's Silk holds its wearer to a wall as a ladder would, so they climb it the way the Hollow Matron does. */
@Mixin(LivingEntity.class)
public abstract class WeaversSilkClimbMixin {
	@Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
	private void wildercord$weaversSilk(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player player && WeaversSilk.clings(player)) cir.setReturnValue(true);
	}
}
