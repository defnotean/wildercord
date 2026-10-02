package dev.wildercord.mixin;

import dev.wildercord.aura.BondedBlades;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A dead swordsman's own bonded blade, about to be dropped some other way than the death's own drop (the cursor's blade, put down as a
 * menu closes on the dead body): kept for the next body instead, as the death kept the rest.
 */
@Mixin(LivingEntity.class)
public abstract class BondedBladeDropMixin {
	@Inject(method = "drop", at = @At("HEAD"), cancellable = true)
	private void wildercord$keepFromDead(ItemStack stack, boolean thrownFromHand, Prediction prediction, CallbackInfoReturnable<ItemEntity> cir) {
		if (BondedBlades.bonded(stack) && BondedBlades.keepFromDead((LivingEntity) (Object) this, stack)) {
			cir.setReturnValue(null);
		}
	}
}
