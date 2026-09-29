package dev.wildercord.mixin;

import dev.wildercord.cast.WorldMagic;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enderman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** An enderman a void spell struck is anchored for a few seconds: every way it teleports (fleeing, the rain, an arrow) fails. */
@Mixin(Enderman.class)
public abstract class EndermanMixin {
	@Inject(method = "teleport(DDD)Z", at = @At("HEAD"), cancellable = true)
	private void wildercord$anchored(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
		if (WorldMagic.anchored((Entity) (Object) this)) {
			cir.setReturnValue(false);
		}
	}
}
