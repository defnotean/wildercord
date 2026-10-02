package dev.wildercord.mixin;

import dev.wildercord.aura.BondedBlades;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A bonded blade at a grindstone: alone (or on top, mending with another weapon below it) it's ground as any weapon is and keeps its
 * bond; as the lower one, which the grindstone would melt into the upper, it isn't taken.
 */
@Mixin(GrindstoneMenu.class)
public abstract class BondedBladeMendingMixin {
	@Inject(method = "computeResult", at = @At("HEAD"), cancellable = true)
	private void wildercord$keepItsStory(ItemStack input, ItemStack additional, CallbackInfoReturnable<ItemStack> cir) {
		if (!input.isEmpty() && BondedBlades.bonded(additional)) {
			cir.setReturnValue(ItemStack.EMPTY);
		}
	}
}
