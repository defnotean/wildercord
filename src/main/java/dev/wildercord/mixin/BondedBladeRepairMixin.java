package dev.wildercord.mixin;

import dev.wildercord.aura.BondedBlades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two weapons crafted together into one fresh one carry nothing over but their curses: a bonded blade is never one of them (its bond and
 * its story would be lost). It's mended at an anvil, a grindstone or with Mending instead.
 */
@Mixin(RepairItemRecipe.class)
public abstract class BondedBladeRepairMixin {
	@Inject(method = "canCombine", at = @At("HEAD"), cancellable = true)
	private static void wildercord$neverABondedBlade(ItemStack first, ItemStack second, CallbackInfoReturnable<Boolean> cir) {
		if (BondedBlades.bonded(first) || BondedBlades.bonded(second)) {
			cir.setReturnValue(false);
		}
	}
}
