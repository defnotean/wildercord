package dev.wildercord.mixin;

import dev.wildercord.aura.BondedBlades;
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
@Mixin(net.minecraft.world.entity.player.Player.class)
public abstract class BondedBladeDropMixin {
	@Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
	private void wildercord$keepFromDead(ItemStack stack, boolean dropAround, boolean includeThrowerName, CallbackInfoReturnable<ItemEntity> cir) {
		if ((Object) this instanceof LivingEntity bearer && BondedBlades.bonded(stack) && BondedBlades.keepFromDead(bearer, stack)) {
			cir.setReturnValue(null);
		}
	}
}
