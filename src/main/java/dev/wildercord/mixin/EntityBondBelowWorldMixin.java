package dev.wildercord.mixin;

import dev.wildercord.aura.BondedBlades;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A bonded blade falling out of the world comes home to its swordsman instead of being lost (see {@link BondedBlades#fellOut}). */
@Mixin(Entity.class)
public abstract class EntityBondBelowWorldMixin {
	@Inject(method = "onBelowWorld", at = @At("HEAD"), cancellable = true)
	private void wildercord$home(CallbackInfo ci) {
		if ((Object) this instanceof ItemEntity item && BondedBlades.bonded(item.getItem()) && BondedBlades.fellOut(item)) {
			ci.cancel();
		}
	}
}
